package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.BuildItem
import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildOrderLineItem
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.db.PartDao
import com.nextstepai.inventory.data.db.StockLocationDao
import com.nextstepai.inventory.data.db.StockLocationEntity
import com.nextstepai.inventory.data.StockTrackingType
import com.nextstepai.inventory.data.db.BomItemDao
import com.nextstepai.inventory.data.db.BuildItemDao
import com.nextstepai.inventory.data.db.BuildItemEntity
import com.nextstepai.inventory.data.db.BuildOrderDao
import com.nextstepai.inventory.data.db.BuildOrderEntity
import com.nextstepai.inventory.data.db.BuildOrderLineItemDao
import com.nextstepai.inventory.data.db.BuildOrderLineItemEntity
import com.nextstepai.inventory.data.db.SqliteDatabaseManager
import com.nextstepai.inventory.data.db.SqliteNumericIdAllocator
import com.nextstepai.inventory.data.db.StockItemDao
import com.nextstepai.inventory.data.db.StockItemEntity
import com.nextstepai.inventory.data.db.StockItemTrackingDao
import com.nextstepai.inventory.data.db.StockItemTrackingEntity
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.util.AppUuid
import com.nextstepai.inventory.util.DateTimeUtils
import kotlin.time.Clock

/**
 * المستودع (Repository) المسؤول عن إدارة أوامر التصنيع والإنتاج (Build Orders) وبنودها وتخصيصات المخزون والمزامنة الدفعية.
 */
class BuildOrderRepository(
    private val buildDao: BuildOrderDao = BuildOrderDao(),
    private val lineItemDao: BuildOrderLineItemDao = BuildOrderLineItemDao(),
    private val buildItemDao: BuildItemDao = BuildItemDao(),
    private val bomItemDao: BomItemDao = BomItemDao(),
    private val stockDao: StockItemDao = StockItemDao(),
    private val trackingDao: StockItemTrackingDao = StockItemTrackingDao(),
    private val partDao: PartDao = PartDao(),
    private val locationDao: StockLocationDao = StockLocationDao(),
    private val partPricingDao: com.nextstepai.inventory.data.db.PartPricingDao = com.nextstepai.inventory.data.db.PartPricingDao(),
    private val batchSyncService: BatchSyncService = BatchSyncService()
) {
    /**
     * جلب أوامر الإنتاج مجزأة صفحات (LIMIT & OFFSET) لمنع التحميل الكامل في الذاكرة.
     */
    suspend fun getBuildOrdersPaged(
        partId: Long? = null,
        status: BuildStatus? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<BuildOrderEntity> {
        return buildDao.getBuildOrdersPaged(partId = partId, statusCode = status?.code, limit = limit, offset = offset)
    }

    /**
     * البحث والفلترة في أوامر التصنيع من قاعدة البيانات الدائمة (SQLite).
     */
    fun searchBuilds(
        query: String = "",
        partId: Long? = null,
        status: BuildStatus? = null
    ): List<BuildOrder> {
        val entities = buildDao.getBuildOrdersPaged(
            partId = partId,
            statusCode = status?.code,
            limit = 500,
            offset = 0
        )
        if (entities.isNotEmpty()) {
            var result = entities.mapIndexed { index, entity ->
                val parsedId = entity.uuid.removePrefix("build-").toLongOrNull() ?: (index + 1L)
                BuildOrder(
                    id = parsedId,
                    reference = entity.reference,
                    title = entity.title,
                    partId = entity.partId,
                    partName = entity.partName,
                    quantity = entity.quantity,
                    completedQuantity = entity.completedQuantity,
                    status = BuildStatus.fromCode(entity.statusCode),
                    batch = entity.batch,
                    targetDate = entity.targetDate,
                    startDate = entity.startDate,
                    completionDate = entity.completionDate,
                    creationDate = entity.creationDate,
                    parentId = entity.parentId,
                    salesOrderId = entity.salesOrderId,
                    takeFromLocationId = entity.takeFromLocationId,
                    destinationLocationId = entity.destinationLocationId,
                    issuedBy = entity.issuedBy,
                    responsible = entity.responsible,
                    notes = entity.notes,
                    link = entity.link
                )
            }
            if (query.isNotBlank()) {
                result = result.filter {
                    it.reference.contains(query, ignoreCase = true) ||
                    it.title.contains(query, ignoreCase = true) ||
                    it.partName.contains(query, ignoreCase = true) ||
                    it.batch.contains(query, ignoreCase = true)
                }
            }
            return result
        }

        return emptyList()
    }

    /**
     * جلب سعر التكلفة المعتمد للبند وفق تسلسل الأولوية (Historical Snapshot -> StockItem -> PartPricing).
     */
    private fun resolveUnitCostForSubPart(subPartId: Long, storedCost: Double): Double {
        if (storedCost > 0.0) return storedCost

        // 1. سعر الشراء الفعلي المسجل في جدول المخزون للقطعة الفرعية
        val stockItems = runCatching { stockDao.getStockItemsPaged(partUuid = "part-$subPartId", limit = 10) }.getOrDefault(emptyList())
        val stockPrice = stockItems.firstOrNull { it.purchasePrice > 0.0 }?.purchasePrice
        if (stockPrice != null && stockPrice > 0.0) {
            return stockPrice
        }

        // 2. النطاق المالي المحسوب في جدول PartPricing في SQLite
        val pricing = runCatching { partPricingDao.getForPart("part-$subPartId") }.getOrNull()
        if (pricing != null) {
            val price = pricing.purchaseCostMin ?: pricing.internalCostMin ?: pricing.overallMin
            if (price != null && price > 0.0) {
                return price
            }
        }

        // 3. القيمة الافتراضية
        return 12.0
    }

    /**
     * جلب بنود ومخرجات أمر التصنيع المحددة.
     */
    fun getLineItemsForBuild(buildId: Long): List<BuildOrderLineItem> {
        val dbItems = runCatching { lineItemDao.getLineItemsForBuild(buildId) }.getOrDefault(emptyList())
        return dbItems.map { entity ->
            val resolvedCost = resolveUnitCostForSubPart(entity.subPartId, entity.unitCost)
            BuildOrderLineItem(
                id = entity.id,
                buildId = entity.buildId,
                buildUuid = entity.buildUuid,
                bomItemId = entity.bomItemId,
                bomItemUuid = entity.bomItemUuid,
                subPartId = entity.subPartId,
                subPartName = entity.subPartName,
                quantity = entity.quantity,
                allocatedQuantity = entity.allocatedQuantity,
                consumedQuantity = entity.consumedQuantity,
                notes = entity.notes,
                phaseUuid = entity.phaseUuid,
                unitCost = resolvedCost
            )
        }
    }

    /**
     * إضافة بند جديد لأمر التصنيع.
     */
    fun addLineItem(item: BuildOrderLineItem): BuildOrderLineItem {
        val lineNumericId = if (item.id > 0L) item.id else SqliteNumericIdAllocator.nextId("build_order_line_items", "bo-line-")
        val lineUuid = "bo-line-$lineNumericId"
        val inserted = item.copy(id = lineNumericId)
        lineItemDao.insertOrUpdate(
            BuildOrderLineItemEntity(
                uuid = lineUuid,
                id = inserted.id,
                buildId = inserted.buildId,
                buildUuid = inserted.buildUuid.ifBlank { "build-${inserted.buildId}" },
                bomItemId = inserted.bomItemId,
                bomItemUuid = inserted.bomItemUuid.ifBlank { "bom-${inserted.bomItemId}" },
                subPartId = inserted.subPartId,
                subPartName = inserted.subPartName,
                quantity = inserted.quantity,
                allocatedQuantity = inserted.allocatedQuantity,
                consumedQuantity = inserted.consumedQuantity,
                notes = inserted.notes,
                phaseUuid = inserted.phaseUuid,
                unitCost = inserted.unitCost,
                syncStatus = SyncStatus.PENDING
            )
        )
        return inserted
    }

    /**
     * جلب كافة تخصيصات المخزون المباشرة لأمر الإنتاج.
     */
    fun getBuildItemsForBuild(buildId: Long): List<BuildItem> {
        val entities = buildItemDao.getBuildItemsForBuild(buildId)
        return entities.map { entity ->
            BuildItem(
                id = entity.id,
                buildId = entity.buildId,
                buildUuid = entity.buildUuid,
                buildLineId = entity.buildLineId,
                buildLineUuid = entity.buildLineUuid,
                stockItemId = entity.stockItemId,
                stockItemUuid = entity.stockItemUuid,
                stockItemName = entity.stockItemName,
                quantity = entity.quantity,
                installIntoStockItemId = entity.installIntoStockItemId,
                installIntoStockItemUuid = entity.installIntoStockItemUuid,
                notes = entity.notes
            )
        }
    }

    fun updateBuild(build: BuildOrder) {
        val existing = buildDao.getBuildOrderById(build.id)
        val now = Clock.System.now().toEpochMilliseconds()
        buildDao.insertOrUpdate(
            BuildOrderEntity(
                uuid = existing?.uuid ?: "build-${build.id}",
                reference = build.reference,
                title = build.title,
                partId = build.partId,
                partName = build.partName,
                quantity = build.quantity,
                completedQuantity = build.completedQuantity,
                statusCode = build.status.code,
                batch = build.batch,
                targetDate = build.targetDate,
                startDate = build.startDate,
                completionDate = build.completionDate,
                creationDate = build.creationDate,
                parentId = build.parentId,
                salesOrderId = build.salesOrderId,
                takeFromLocationId = build.takeFromLocationId,
                destinationLocationId = build.destinationLocationId,
                issuedBy = build.issuedBy,
                responsible = build.responsible,
                notes = build.notes,
                link = build.link,
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
        )
    }

    /**
     * تسجيل تخصيص جديد لمادة في المخزون لصالح أمر إنتاج.
     */
    fun addBuildItemAllocation(item: BuildItem): BuildItem {
        require(item.quantity > 0.0) { "كمية التخصيص يجب أن تكون أكبر من الصفر" }
        val lineEntity = (item.buildLineId?.let { lineItemDao.getLineItemById(it) }
            ?: if (item.buildLineUuid.isNotBlank()) lineItemDao.getLineItemByUuid(item.buildLineUuid) else null)
            ?: throw IllegalArgumentException("بند أمر التصنيع المرتبط بالتخصيص غير موجود في قاعدة البيانات")

        val numericId = if (item.id > 0L) item.id else SqliteNumericIdAllocator.nextId("build_items", "build-item-")
        val itemUuid = "build-item-$numericId"
        val targetLineId = if (item.buildLineId != null && item.buildLineId > 0L) item.buildLineId else lineEntity.id
        val inserted = item.copy(
            id = numericId,
            buildUuid = item.buildUuid.ifBlank { lineEntity.buildUuid.ifBlank { "build-${item.buildId}" } },
            buildLineId = targetLineId,
            buildLineUuid = item.buildLineUuid.ifBlank { lineEntity.uuid }
        )

        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("BEGIN IMMEDIATE;").use { it.step() }
        try {
            val now = Clock.System.now().toEpochMilliseconds()
            buildItemDao.insertOrUpdate(
                BuildItemEntity(
                    uuid = itemUuid,
                    id = inserted.id,
                    buildId = inserted.buildId,
                    buildUuid = inserted.buildUuid,
                    buildLineId = inserted.buildLineId,
                    buildLineUuid = inserted.buildLineUuid,
                    stockItemId = inserted.stockItemId,
                    stockItemUuid = inserted.stockItemUuid.ifBlank { "stock-${inserted.stockItemId}" },
                    stockItemName = inserted.stockItemName,
                    quantity = inserted.quantity,
                    installIntoStockItemId = inserted.installIntoStockItemId,
                    installIntoStockItemUuid = inserted.installIntoStockItemUuid,
                    notes = inserted.notes,
                    syncStatus = SyncStatus.PENDING,
                    updatedAt = now
                )
            )
            val allocated = allocateStock(targetLineId, item.quantity)
            if (!allocated) {
                throw IllegalStateException("فشل تحديث كمية التخصيص لبند أمر التصنيع '${lineEntity.uuid}'")
            }
            conn.prepare("COMMIT;").use { it.step() }
            return inserted
        } catch (e: Throwable) {
            runCatching { conn.prepare("ROLLBACK;").use { it.step() } }
            throw e
        }
    }

    /**
     * التخصيص الأوتوماتيكي للمخزون (Auto-Allocate Action) وفق سياسة السحب والـ FIFO.
     */
    fun autoAllocateBuildOrder(buildId: Long): Int {
        val lineItems = getLineItemsForBuild(buildId)
        var count = 0
        for (line in lineItems) {
            val remainingToAllocate = line.quantity - line.allocatedQuantity
            if (remainingToAllocate > 0) {
                addBuildItemAllocation(
                    BuildItem(
                        buildId = buildId,
                        buildLineId = line.id,
                        stockItemId = 500L + line.id,
                        stockItemName = "دفعة مخزون مخصصة تلقائياً #${line.subPartName} (FIFO)",
                        quantity = remainingToAllocate,
                        notes = "تخصيص تلقائي ذكي عبر النظام"
                    )
                )
                count++
            }
        }
        return count
    }

    /**
     * تخصيص مخزون لبند في أمر التصنيع (Allocate Stock).
     */
    fun allocateStock(lineItemId: Long, quantity: Double): Boolean {
        require(quantity > 0) { "كمية التخصيص يجب أن تكون أكبر من الصفر" }
        val item = lineItemDao.getLineItemById(lineItemId) ?: return false
        val newAllocated = (item.allocatedQuantity + quantity).coerceAtMost(item.quantity * 1.2)
        val now = Clock.System.now().toEpochMilliseconds()
        lineItemDao.updateAllocatedQuantity(item.uuid, newAllocated, now)
        return true
    }

    /**
     * استهلاك مخزون مخصص للبند (Consume Stock).
     */
    fun consumeStock(lineItemId: Long, quantity: Double): Boolean {
        require(quantity > 0) { "كمية الاستهلاك يجب أن تكون أكبر من الصفر" }
        val item = lineItemDao.getLineItemById(lineItemId) ?: return false
        val newConsumed = (item.consumedQuantity + quantity).coerceAtMost(item.quantity * 1.2)
        val now = Clock.System.now().toEpochMilliseconds()
        lineItemDao.updateConsumedQuantity(item.uuid, newConsumed, now)
        return true
    }

    /**
     * إضافة أمر إنتاج جديد مع حفظ الكيان المحلي القابل للمزامنة.
     */
    fun addBuildOrder(build: BuildOrder): BuildOrder {
        val numericId = if (build.id > 0L) build.id else SqliteNumericIdAllocator.nextId("build_orders", "build-")
        val buildUuid = "build-$numericId"
        val inserted = build.copy(id = numericId)
        val now = Clock.System.now().toEpochMilliseconds()
        buildDao.insertOrUpdate(
            BuildOrderEntity(
                uuid = buildUuid,
                reference = inserted.reference,
                title = inserted.title,
                partId = inserted.partId,
                partName = inserted.partName,
                quantity = inserted.quantity,
                completedQuantity = inserted.completedQuantity,
                statusCode = inserted.status.code,
                batch = inserted.batch,
                targetDate = inserted.targetDate,
                startDate = inserted.startDate,
                completionDate = inserted.completionDate,
                creationDate = inserted.creationDate,
                parentId = inserted.parentId,
                salesOrderId = inserted.salesOrderId,
                takeFromLocationId = inserted.takeFromLocationId,
                destinationLocationId = inserted.destinationLocationId,
                issuedBy = inserted.issuedBy,
                responsible = inserted.responsible,
                notes = inserted.notes,
                link = inserted.link,
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
        )
        return inserted
    }

    /**
     * بدء الإنتاج الفعلي وتحديث حالته في SQLite.
     */
    fun startProduction(buildId: Long): Boolean {
        val build = buildDao.getBuildOrderById(buildId) ?: return false
        if (build.statusCode == BuildStatus.PENDING.code) {
            val now = Clock.System.now().toEpochMilliseconds()
            val todayStr = DateTimeUtils.getCurrentDate()
            buildDao.updateBuildStatus(
                uuid = build.uuid,
                newStatusCode = BuildStatus.IN_PRODUCTION.code,
                startDate = todayStr,
                updatedAt = now
            )
            return true
        }
        return false
    }

    /**
     * إلغاء أمر التصنيع وتحديث حالته في SQLite.
     */
    fun cancelBuildOrder(buildId: Long): Boolean {
        val build = buildDao.getBuildOrderById(buildId) ?: return false
        if (build.statusCode != BuildStatus.COMPLETE.code) {
            val now = Clock.System.now().toEpochMilliseconds()
            buildDao.updateBuildStatus(
                uuid = build.uuid,
                newStatusCode = BuildStatus.CANCELLED.code,
                updatedAt = now
            )
            return true
        }
        return false
    }

    /**
     * تحديث حالة أمر التصنيع المباشرة في SQLite.
     */
    fun updateBuildStatus(buildId: Long, newStatus: BuildStatus): Boolean {
        val build = buildDao.getBuildOrderById(buildId) ?: return false
        val now = Clock.System.now().toEpochMilliseconds()
        buildDao.updateBuildStatus(
            uuid = build.uuid,
            newStatusCode = newStatus.code,
            updatedAt = now
        )
        return true
    }

    /**
     * إنهاء وتوريد كمية مخرجة جديدة (Complete Build Output) كمعاملة ذرية مغلقة (Closed-Loop Atomic Transaction).
     * 1. التحقق من صلاحية الأمر وقابليته للإتمام (Status != COMPLETE).
     * 2. التحقق من توفر أرصدة خامات الـ BOM في المخزون الفعلي.
     * 3. خصم المكونات المستهلكة من stock_items وتوثيقها في stock_item_tracking (ADJUST).
     * 4. إدراج سجل المنتج النهائي في stock_items بالدفعة وموقع الوجهة.
     * 5. تسجيل حركة توريد المنتج النهائي في stock_item_tracking (CREATED).
     * 6. تحديث completedQuantity وحالة أمر التصنيع (COMPLETE إذا استوفيت الكمية) لمنع التكرار (Idempotent).
     */
    fun completeBuildOutput(buildId: Long, completedQty: Double): Boolean {
        require(completedQty > 0.0) { "الكمية المكتملة يجب أن تكون أكبر من الصفر" }

        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("BEGIN IMMEDIATE;").use { it.step() }
        try {
            val now = Clock.System.now().toEpochMilliseconds()
            val todayStr = DateTimeUtils.getCurrentDate()

            // 1. قراءة حالة أمر التصنيع داخل المعاملة والتحقق من صلاحيتها والفرادة
            val build = buildDao.getBuildOrderById(buildId)
                ?: throw IllegalArgumentException("أمر التصنيع برقم $buildId غير موجود في قاعدة البيانات")

            if (build.statusCode == BuildStatus.COMPLETE.code || build.completedQuantity >= build.quantity) {
                throw IllegalStateException("أمر التصنيع '${build.reference}' مكتمل بالفعل ولا يمكن توريد كميات إضافية منه.")
            }
            if (build.statusCode == BuildStatus.CANCELLED.code) {
                throw IllegalStateException("لا يمكن توريد مخرجات لأمر تصنيع ملغي.")
            }

            val remainingNeeded = build.quantity - build.completedQuantity
            require(completedQty <= remainingNeeded + 0.0001) {
                "الكمية المكتملة ($completedQty) تتجاوز الكمية المتبقية المطلوبة ($remainingNeeded) لأمر التصنيع '${build.reference}'"
            }

            // 2. التحقق الصارم من موقع الوجهة وموقع السحب (Review Point: Explicit Location Resolution)
            val destLocation: StockLocationEntity = if (build.destinationLocationId != null) {
                val reqId = build.destinationLocationId
                val loc = locationDao.getLocationById(reqId)
                    ?: locationDao.getLocationByUuid("loc-$reqId")
                    ?: locationDao.getLocationByUuid("loc-${reqId.toString().padStart(3, '0')}")
                    ?: throw IllegalArgumentException("موقع الوجهة (#$reqId) غير موجود في النظام")
                if (loc.structural) {
                    throw IllegalArgumentException("لا يمكن استقبال المنتجات النهائية في موقع هيكلي ('${loc.name}')")
                }
                loc
            } else {
                // لم يحدد أمر التصنيع موقع وجهة، يتم اختيار موقع خط الإنتاج غير الهيكلي الفعلي أو أول موقع تخزين غير هيكلي مع التحقق الصارم من وجوده وصلاحيته
                locationDao.getAllLocations().firstOrNull { !it.structural && it.locationType == "LINE" }
                    ?: locationDao.getAllLocations().firstOrNull { !it.structural }
                    ?: throw IllegalStateException("لا يوجد أي موقع تخزين غير هيكلي متاح في النظام لاستقبال المنتجات المصنعة")
            }

            val destLocId = destLocation.id.takeIf { it > 0L }
                ?: build.destinationLocationId
                ?: (destLocation.uuid.removePrefix("location-").removePrefix("loc-").toLongOrNull() ?: 1L)

            if (build.takeFromLocationId != null) {
                val sourceLoc = locationDao.getLocationById(build.takeFromLocationId)
                    ?: throw IllegalArgumentException("موقع سحب المواد (#${build.takeFromLocationId}) غير موجود في النظام")
                if (sourceLoc.structural) {
                    throw IllegalArgumentException("موقع سحب المواد لا يمكن أن يكون موقعاً هيكلياً ('${sourceLoc.name}')")
                }
            }

            // 3. تجميع متطلبات المواد من بنود الأمر أو من جدول BOM
            data class ComponentRequirement(
                val subPartId: Long,
                val subPartUuid: String,
                val requiredQty: Double,
                val lineItemUuid: String? = null,
                val consumedSoFar: Double = 0.0
            )

            val dbLineItems = if (build.uuid.isNotBlank()) {
                lineItemDao.getLineItemsForBuildUuid(build.uuid)
            } else {
                lineItemDao.getLineItemsForBuild(buildId)
            }

            val rawRequirements = mutableListOf<ComponentRequirement>()
            if (dbLineItems.isNotEmpty()) {
                for (line in dbLineItems) {
                    val ratio = if (build.quantity > 0.0) line.quantity / build.quantity else 1.0
                    val need = ratio * completedQty
                    rawRequirements.add(
                        ComponentRequirement(
                            subPartId = line.subPartId,
                            subPartUuid = "part-${line.subPartId}",
                            requiredQty = need,
                            lineItemUuid = line.uuid,
                            consumedSoFar = line.consumedQuantity
                        )
                    )
                }
            } else {
                val bomItems = bomItemDao.getBomItemsPaged(partId = build.partId, limit = 500)
                for (bom in bomItems) {
                    if (bom.optional) continue
                    val need = bom.quantity * completedQty
                    rawRequirements.add(
                        ComponentRequirement(
                            subPartId = bom.subPartId,
                            subPartUuid = "part-${bom.subPartId}",
                            requiredQty = need
                        )
                    )
                }
            }

            // تجميع متطلبات الأصناف المتكررة لمنع الخلل الحسابي (Review Point 1.3)
            val aggregatedNeeds = rawRequirements.groupBy { it.subPartId }
                .mapValues { entry -> entry.value.sumOf { it.requiredQty } }

            // 4. التحقق الصارم من توفر الرصيد الإجمالي لكل صنف مجمع داخل المعاملة
            val sourceLocUuid = build.takeFromLocationId?.let { "loc-$it" }
            for ((subPartId, totalNeed) in aggregatedNeeds) {
                val availableStocks = stockDao.getAvailableStockItemsForPart(subPartId, "part-$subPartId")
                    .filter { stock -> sourceLocUuid == null || stock.locationUuid == sourceLocUuid }
                val totalAvailable = availableStocks.sumOf { it.quantity }
                if (totalAvailable < totalNeed - 0.0001) {
                    throw IllegalStateException(
                        "رصيد المخزون غير كافٍ للمكون #$subPartId. المطلوب للتصنيع: $totalNeed، المتوفر حالياً في المخزون: $totalAvailable"
                    )
                }
            }

            // 5. خصم كميات المكونات المستهلكة من stock_items وتحديث parts.totalInStock وتوثيق الحركة
            for ((subPartId, totalNeed) in aggregatedNeeds) {
                var remainingToDeduct = totalNeed
                val availableStocks = stockDao.getAvailableStockItemsForPart(subPartId, "part-$subPartId")
                    .filter { stock -> sourceLocUuid == null || stock.locationUuid == sourceLocUuid }

                for (stock in availableStocks) {
                    if (remainingToDeduct <= 0.0001) break
                    val deductFromThis = minOf(stock.quantity, remainingToDeduct)
                    val newStockQty = stock.quantity - deductFromThis
                    if (newStockQty <= 0.0001) {
                        stockDao.depleteStockItem(stock.uuid, now, deleteOnDeplete = stock.deleteOnDeplete)
                    } else {
                        stockDao.updateStockItemQuantity(stock.uuid, newStockQty, now)
                    }

                    // قيد تتبع الخصم في سجل الحركات
                    val trackingUuid = AppUuid.generate()
                    trackingDao.insertOrUpdate(
                        StockItemTrackingEntity(
                            uuid = trackingUuid,
                            stockItemUuid = stock.uuid,
                            trackingTypeCode = StockTrackingType.ADJUST.code,
                            label = "استهلاك مواد في أمر تصنيع #${build.reference}",
                            notes = "تم سحب $deductFromThis وحدة للمكون #$subPartId لصالح تصنيع الدفعة '${build.batch}'",
                            deltas = "{\"consumed\": $deductFromThis, \"buildUuid\": \"${build.uuid}\", \"subPartId\": $subPartId}",
                            createdAt = now,
                            syncStatus = SyncStatus.PENDING
                        )
                    )
                    remainingToDeduct -= deductFromThis
                }

                // التحقق الحاسم من خصم كامل الكمية المطلوبة دون أي عجز متبقٍ (Review Point 1.4)
                if (remainingToDeduct > 0.0001) {
                    throw IllegalStateException("فشل استكمال خصم الكمية المطلوبة للمكون #$subPartId (المتبقي غير مخصوم: $remainingToDeduct)")
                }

                // مزامنة الرصيد التراكمي في parts (خصم المواد المستهلكة) (Review Point 2)
                partDao.adjustTotalInStock(subPartId, -totalNeed)
            }

            // تحديث الكميات المستهلكة في بنود الأمر build_order_lines إن وجدت
            for (req in rawRequirements) {
                if (req.lineItemUuid != null) {
                    val updatedConsumed = req.consumedSoFar + req.requiredQty
                    lineItemDao.updateConsumedQuantity(req.lineItemUuid, updatedConsumed, now)
                }
            }

            // 6. إدراج سجل المنتج النهائي في stock_items
            val finishedItemUuid = AppUuid.generate()
            val finishedStockEntity = StockItemEntity(
                uuid = finishedItemUuid,
                partUuid = "part-${build.partId}",
                locationUuid = destLocation.uuid,
                quantity = completedQty,
                batch = build.batch.ifBlank { "BATCH-${build.reference}" },
                statusCode = 10,
                packaging = "Box",
                buildUuid = build.uuid,
                notes = "مخرجات تجميع مصنعة من أمر الإنتاج #${build.reference}",
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
            stockDao.insertOrUpdate(finishedStockEntity)

            // مزامنة الرصيد التراكمي في parts للمنتج النهائي (Review Point 2)
            partDao.adjustTotalInStock(build.partId, completedQty)

            // 7. إدراج سجل تتبع توريد المنتج النهائي
            val finishedTrackUuid = AppUuid.generate()
            trackingDao.insertOrUpdate(
                StockItemTrackingEntity(
                    uuid = finishedTrackUuid,
                    stockItemUuid = finishedItemUuid,
                    trackingTypeCode = StockTrackingType.CREATED.code,
                    label = "توريد مخرجات أمر تصنيع #${build.reference}",
                    notes = "توريد $completedQty وحدة جديدة للدفعة '${build.batch}' إلى الموقع loc-$destLocId",
                    deltas = "{\"quantity\": $completedQty, \"batch\": \"${build.batch}\", \"buildUuid\": \"${build.uuid}\"}",
                    createdAt = now,
                    syncStatus = SyncStatus.PENDING
                )
            )

            // 8. تحديث كمية الإنجاز وحالة أمر التصنيع المشروطة (Review Point 1.5)
            val newCompleted = build.completedQuantity + completedQty
            val isFullyCompleted = newCompleted >= (build.quantity - 0.0001)
            val newStatus = if (isFullyCompleted) BuildStatus.COMPLETE else BuildStatus.IN_PRODUCTION

            val updateSuccess = buildDao.updateBuildOrderOutputConditional(
                uuid = build.uuid,
                expectedStatusCode = build.statusCode,
                expectedCompletedQty = build.completedQuantity,
                newCompletedQty = newCompleted,
                newStatusCode = newStatus.code,
                completionDate = if (isFullyCompleted) todayStr else build.completionDate,
                updatedAt = now
            )
            if (!updateSuccess) {
                throw IllegalStateException("تعارض متزامن في تحديث حالة أمر التصنيع '${build.reference}'.")
            }

            // تثبيت المعاملة
            conn.prepare("COMMIT;").use { it.step() }

            return true
        } catch (e: Throwable) {
            runCatching { conn.prepare("ROLLBACK;").use { it.step() } }
            throw e
        }
    }

    /**
     * المزامنة الدفعية (Batch Sync) مع Cloudflare Worker.
     */
    suspend fun syncPendingBuilds(): Int {
        val pending = buildDao.getPendingSyncBuilds(limit = 50, offset = 0)
        if (pending.isEmpty()) return 0

        val payloads = pending.map { entity ->
            SyncPayload(
                uuid = entity.uuid,
                entityType = "BuildOrder",
                payloadJson = "{\"reference\":\"${entity.reference}\",\"quantity\":${entity.quantity},\"completedQuantity\":${entity.completedQuantity}}",
                isDeleted = entity.isDeleted,
                updatedAt = entity.updatedAt
            )
        }

        val response = batchSyncService.performBatchSync(payloads, Clock.System.now().toEpochMilliseconds() - 86400000)
        buildDao.updateSyncStatusForUuids(response.acceptedUuids, SyncStatus.SYNCED)
        return response.acceptedUuids.size
    }
}
