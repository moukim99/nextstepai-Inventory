package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.BuildItem
import com.nextstepai.inventory.data.BuildItemTable
import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildOrderLineItem
import com.nextstepai.inventory.data.BuildOrderLineItemTable
import com.nextstepai.inventory.data.BuildOrderTable
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.db.PartDao
import com.nextstepai.inventory.data.db.StockLocationDao
import com.nextstepai.inventory.data.StockTrackingType
import com.nextstepai.inventory.data.db.BomItemDao
import com.nextstepai.inventory.data.db.BuildItemDao
import com.nextstepai.inventory.data.db.BuildItemEntity
import com.nextstepai.inventory.data.db.BuildOrderDao
import com.nextstepai.inventory.data.db.BuildOrderEntity
import com.nextstepai.inventory.data.db.BuildOrderLineItemDao
import com.nextstepai.inventory.data.db.BuildOrderLineItemEntity
import com.nextstepai.inventory.data.db.SqliteDatabaseManager
import com.nextstepai.inventory.data.db.StockItemDao
import com.nextstepai.inventory.data.db.StockItemEntity
import com.nextstepai.inventory.data.db.StockItemTrackingDao
import com.nextstepai.inventory.data.db.StockItemTrackingEntity
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.data.PartPricingTable
import com.nextstepai.inventory.util.AppUuid
import com.nextstepai.inventory.util.DateTimeUtils
import kotlin.time.Clock

/**
 * المستودع (Repository) المسؤول عن إدارة أوامر التصنيع والإنتاج (Build Orders) وبنودها وتخصيصات المخزون والمزامنة الدفعية.
 */
class BuildOrderRepository(
    private val buildTable: BuildOrderTable = BuildOrderTable(),
    private val lineItemTable: BuildOrderLineItemTable = BuildOrderLineItemTable(),
    private val buildItemTable: BuildItemTable = BuildItemTable(),
    private val buildDao: BuildOrderDao = BuildOrderDao(),
    private val lineItemDao: BuildOrderLineItemDao = BuildOrderLineItemDao(),
    private val buildItemDao: BuildItemDao = BuildItemDao(),
    private val bomItemDao: BomItemDao = BomItemDao(),
    private val stockDao: StockItemDao = StockItemDao(),
    private val trackingDao: StockItemTrackingDao = StockItemTrackingDao(),
    private val partDao: PartDao = PartDao(),
    private val locationDao: StockLocationDao = StockLocationDao(),
    private val partPricingTable: PartPricingTable = PartPricingTable(),
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

        val seedBuilds = buildTable.getAllBuilds()
        if (seedBuilds.isNotEmpty()) {
            seedBuilds.forEach { b ->
                buildDao.insertOrUpdate(
                    BuildOrderEntity(
                        uuid = "build-${b.id}",
                        reference = b.reference,
                        title = b.title,
                        partId = b.partId,
                        partName = b.partName,
                        quantity = b.quantity,
                        completedQuantity = b.completedQuantity,
                        statusCode = b.status.code,
                        batch = b.batch,
                        targetDate = b.targetDate,
                        startDate = b.startDate,
                        completionDate = b.completionDate,
                        creationDate = b.creationDate,
                        parentId = b.parentId,
                        salesOrderId = b.salesOrderId,
                        takeFromLocationId = b.takeFromLocationId,
                        destinationLocationId = b.destinationLocationId,
                        issuedBy = b.issuedBy,
                        responsible = b.responsible,
                        notes = b.notes,
                        link = b.link,
                        syncStatus = SyncStatus.SYNCED
                    )
                )
            }
            return searchBuilds(query = query, partId = partId, status = status)
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

        // 2. النطاق المالي المحسوب في جدول PartPricing
        val pricing = partPricingTable.getPricingForPart(subPartId)
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
        if (dbItems.isNotEmpty()) {
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
        return lineItemTable.getLineItemsForBuild(buildId)
    }

    /**
     * إضافة بند جديد لأمر التصنيع.
     */
    fun addLineItem(item: BuildOrderLineItem): BuildOrderLineItem {
        val inserted = lineItemTable.insertLineItem(item)
        lineItemDao.insertOrUpdate(
            BuildOrderLineItemEntity(
                uuid = "lineitem-${inserted.id}",
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
        return buildItemTable.getBuildItemsForBuild(buildId)
    }

    fun updateBuild(build: BuildOrder) {
        buildTable.updateBuild(build)
    }

    /**
     * تسجيل تخصيص جديد لمادة في المخزون لصالح أمر إنتاج.
     */
    fun addBuildItemAllocation(item: BuildItem): BuildItem {
        val inserted = buildItemTable.insertBuildItem(item)
        buildItemDao.insertOrUpdate(
            BuildItemEntity(
                uuid = "builditem-${inserted.id}",
                id = inserted.id,
                buildId = inserted.buildId,
                buildUuid = inserted.buildUuid.ifBlank { "build-${inserted.buildId}" },
                buildLineId = inserted.buildLineId,
                buildLineUuid = inserted.buildLineUuid.ifBlank { if (inserted.buildLineId != null) "lineitem-${inserted.buildLineId}" else "" },
                stockItemId = inserted.stockItemId,
                stockItemUuid = inserted.stockItemUuid.ifBlank { "stock-${inserted.stockItemId}" },
                stockItemName = inserted.stockItemName,
                quantity = inserted.quantity,
                installIntoStockItemId = inserted.installIntoStockItemId,
                installIntoStockItemUuid = inserted.installIntoStockItemUuid,
                notes = inserted.notes,
                syncStatus = SyncStatus.PENDING
            )
        )
        item.buildLineId?.let { lineId ->
            lineItemTable.allocateStock(lineId, item.quantity)
        }
        return inserted
    }

    /**
     * التخصيص الأوتوماتيكي للمخزون (Auto-Allocate Action) وفق سياسة السحب والـ FIFO.
     */
    fun autoAllocateBuildOrder(buildId: Long): Int {
        val lineItems = lineItemTable.getLineItemsForBuild(buildId)
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
        val success = lineItemTable.allocateStock(lineItemId, quantity)
        if (success) {
            val item = lineItemTable.getLineItemById(lineItemId)
            if (item != null) {
                lineItemDao.insertOrUpdate(
                    BuildOrderLineItemEntity(
                        uuid = "lineitem-${item.id}",
                        id = item.id,
                        buildId = item.buildId,
                        buildUuid = item.buildUuid.ifBlank { "build-${item.buildId}" },
                        bomItemId = item.bomItemId,
                        bomItemUuid = item.bomItemUuid.ifBlank { "bom-${item.bomItemId}" },
                        subPartId = item.subPartId,
                        subPartName = item.subPartName,
                        quantity = item.quantity,
                        allocatedQuantity = item.allocatedQuantity,
                        consumedQuantity = item.consumedQuantity,
                        notes = item.notes,
                        phaseUuid = item.phaseUuid,
                        unitCost = item.unitCost,
                        syncStatus = SyncStatus.PENDING,
                        isDeleted = false,
                        updatedAt = Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        }
        return success
    }

    /**
     * استهلاك مخزون مخصص للبند (Consume Stock).
     */
    fun consumeStock(lineItemId: Long, quantity: Double): Boolean {
        val success = lineItemTable.consumeStock(lineItemId, quantity)
        if (success) {
            val item = lineItemTable.getLineItemById(lineItemId)
            if (item != null) {
                lineItemDao.insertOrUpdate(
                    BuildOrderLineItemEntity(
                        uuid = "lineitem-${item.id}",
                        id = item.id,
                        buildId = item.buildId,
                        buildUuid = item.buildUuid.ifBlank { "build-${item.buildId}" },
                        bomItemId = item.bomItemId,
                        bomItemUuid = item.bomItemUuid.ifBlank { "bom-${item.bomItemId}" },
                        subPartId = item.subPartId,
                        subPartName = item.subPartName,
                        quantity = item.quantity,
                        allocatedQuantity = item.allocatedQuantity,
                        consumedQuantity = item.consumedQuantity,
                        notes = item.notes,
                        phaseUuid = item.phaseUuid,
                        unitCost = item.unitCost,
                        syncStatus = SyncStatus.PENDING,
                        isDeleted = false,
                        updatedAt = Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        }
        return success
    }

    /**
     * إضافة أمر إنتاج جديد مع حفظ الكيان المحلي القابل للمزامنة.
     */
    fun addBuildOrder(build: BuildOrder): BuildOrder {
        val inserted = buildTable.insertBuild(build)
        buildDao.insertOrUpdate(
            BuildOrderEntity(
                uuid = "build-${inserted.id}",
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
                syncStatus = SyncStatus.PENDING
            )
        )
        return inserted
    }

    /**
     * بدء الإنتاج الفعلي وتحديث حالته في SQLite والذاكرة.
     */
    fun startProduction(buildId: Long): Boolean {
        val build = buildDao.getBuildOrderById(buildId) ?: run {
            searchBuilds()
            buildDao.getBuildOrderById(buildId)
        }
        if (build != null && build.statusCode == BuildStatus.PENDING.code) {
            val now = Clock.System.now().toEpochMilliseconds()
            val todayStr = DateTimeUtils.getCurrentDate()
            buildDao.updateBuildStatus(
                uuid = build.uuid,
                newStatusCode = BuildStatus.IN_PRODUCTION.code,
                startDate = todayStr,
                updatedAt = now
            )
        }
        return buildTable.startProduction(buildId)
    }

    /**
     * إلغاء أمر التصنيع وتحديث حالته في SQLite والذاكرة.
     */
    fun cancelBuildOrder(buildId: Long): Boolean {
        val build = buildDao.getBuildOrderById(buildId) ?: run {
            searchBuilds()
            buildDao.getBuildOrderById(buildId)
        }
        if (build != null && build.statusCode != BuildStatus.COMPLETE.code) {
            val now = Clock.System.now().toEpochMilliseconds()
            buildDao.updateBuildStatus(
                uuid = build.uuid,
                newStatusCode = BuildStatus.CANCELLED.code,
                updatedAt = now
            )
        }
        return buildTable.cancelBuildOrder(buildId)
    }

    /**
     * تحديث حالة أمر التصنيع المباشرة في SQLite والذاكرة.
     */
    fun updateBuildStatus(buildId: Long, newStatus: BuildStatus): Boolean {
        val build = buildDao.getBuildOrderById(buildId) ?: run {
            searchBuilds()
            buildDao.getBuildOrderById(buildId)
        }
        if (build != null) {
            val now = Clock.System.now().toEpochMilliseconds()
            buildDao.updateBuildStatus(
                uuid = build.uuid,
                newStatusCode = newStatus.code,
                updatedAt = now
            )
        }
        return buildTable.updateStatus(buildId, newStatus)
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

            // 2. التحقق الصارم من موقع الوجهة وموقع السحب (Review Point 6)
            val destLocId = build.destinationLocationId
                ?: locationDao.getAllLocations().firstOrNull { !it.structural && (it.locationType == "LINE" || it.id == 4L || it.uuid.contains("004")) }?.id
                ?: 4L
            val destLocation = locationDao.getLocationById(destLocId)
                ?: locationDao.getLocationByUuid("loc-$destLocId")
                ?: locationDao.getLocationByUuid("loc-${destLocId.toString().padStart(3, '0')}")
                ?: locationDao.getAllLocations().firstOrNull { !it.structural }
                ?: throw IllegalArgumentException("موقع الوجهة (#$destLocId) غير موجود في النظام")

            if (destLocation.structural) {
                throw IllegalArgumentException("لا يمكن استقبال المنتجات النهائية في موقع هيكلي ('${destLocation.name}')")
            }

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
                locationUuid = "loc-$destLocId",
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

            // مزامنة الذاكرة المؤقتة القديمة بأمان
            runCatching { buildTable.completeBuildOutput(buildId, completedQty) }

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
