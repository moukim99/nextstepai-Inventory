package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockStatus
import com.nextstepai.inventory.data.StockItemAttachment
import com.nextstepai.inventory.data.StockItemTestResult
import com.nextstepai.inventory.data.StockItemTracking
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.StockLocationType
import com.nextstepai.inventory.data.labelImagePath
import com.nextstepai.inventory.data.withLabelSnapshot
import com.nextstepai.inventory.data.isPrimary
import com.nextstepai.inventory.data.withPrimary
import com.nextstepai.inventory.data.IntermediateNodeSpec
import com.nextstepai.inventory.data.StockTrackingType
import com.nextstepai.inventory.data.db.NotificationHistoryDao
import com.nextstepai.inventory.data.db.NotificationHistoryEntity
import com.nextstepai.inventory.data.db.PartDao
import com.nextstepai.inventory.data.db.StockItemAttachmentDao
import com.nextstepai.inventory.data.db.StockItemAttachmentEntity
import com.nextstepai.inventory.data.db.StockItemDao
import com.nextstepai.inventory.data.db.StockItemEntity
import com.nextstepai.inventory.data.db.StockItemTestResultDao
import com.nextstepai.inventory.data.db.StockItemTestResultEntity
import com.nextstepai.inventory.data.db.StockItemTrackingDao
import com.nextstepai.inventory.data.db.StockItemTrackingEntity
import com.nextstepai.inventory.data.db.StockLocationDao
import com.nextstepai.inventory.data.db.StockLocationEntity
import com.nextstepai.inventory.data.db.SqliteDatabaseManager
import com.nextstepai.inventory.data.db.SqliteNumericIdAllocator
import com.nextstepai.inventory.data.db.StockLocationTypeDao
import com.nextstepai.inventory.media.ImageProcessor
import com.nextstepai.inventory.media.ProcessedImage
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.util.AppUuid
import com.nextstepai.inventory.util.DateTimeUtils
import java.io.File
import kotlin.time.Clock

/**
 * تمثيل بند فردي في جلسة الجرد الميداني والمقارنة بين الرصيد الدفتري والفعلي.
 */
data class StocktakeItem(
    val stockItemId: Long,
    val stockItemUuid: String,
    val partId: Long,
    val partName: String,
    val locationId: Long?,
    val locationUuid: String? = null,
    val locationName: String = "",
    val batch: String = "",
    val bookQuantity: Double,
    val countedQuantity: Double,
    val notes: String = ""
) {
    val variance: Double get() = countedQuantity - bookQuantity
    val isMatched: Boolean get() = kotlin.math.abs(variance) < 0.0001
    val isSurplus: Boolean get() = variance > 0.0001
    val isShortage: Boolean get() = variance < -0.0001
}

/**
 * طلب اعتماد وتسوية فرق الجرد لوحدة مخزنية.
 */
data class StocktakeReconciliationRequest(
    val stockItemId: Long,
    val stockItemUuid: String? = null,
    val countedQuantity: Double,
    val reason: String,
    val notes: String = "",
    val userId: Long = 1L,
    val stocktakeDate: String = DateTimeUtils.getCurrentDate()
)

/**
 * ملخص نتائج وتدقيق جلسة الجرد المخزني.
 */
data class StocktakeSessionResult(
    val totalItemsCounted: Int,
    val matchedCount: Int,
    val surplusCount: Int,
    val shortageCount: Int,
    val netVariance: Double,
    val items: List<StocktakeItem>
)

/**
 * المستودع (Repository) المسؤول عن إدارة المخزون الفعلي ومواقع التخزين وسجلات التتبع
 * ونتائج فحوصات الجودة ومرفقات المخزون (StockItemAttachment) وتجزئة الكميات والجرد والمزامنة مع السحابة.
 */
class StockRepository(
    private val stockDao: StockItemDao = StockItemDao(),
    private val locationDao: StockLocationDao = StockLocationDao(),
    private val trackingDao: StockItemTrackingDao = StockItemTrackingDao(),
    private val testResultDao: StockItemTestResultDao = StockItemTestResultDao(),
    private val attachmentDao: StockItemAttachmentDao = StockItemAttachmentDao(),
    private val notificationDao: NotificationHistoryDao = NotificationHistoryDao(),
    private val partDao: PartDao = PartDao(),
    val locationRepository: StockLocationRepository = StockLocationRepository(locationDao = locationDao),
    private val batchSyncService: BatchSyncService = BatchSyncService(),
    private val imageProcessor: ImageProcessor = ImageProcessor(maxDimension = 1024, compressionQuality = 85)
) {
    /**
     * جلب سجلات المخزون الفعلي مقسمة صفحات (LIMIT & OFFSET) لمنع التحميل الكامل في الذاكرة.
     */
    suspend fun getStockItemsPaged(
        partUuid: String? = null,
        locationUuid: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<StockItemEntity> {
        return stockDao.getStockItemsPaged(
            partUuid = partUuid,
            locationUuid = locationUuid,
            limit = limit,
            offset = offset
        )
    }

    suspend fun getStockItemByUuid(uuid: String): StockItemEntity? {
        return stockDao.getStockItemByUuid(uuid)
    }

    /**
     * جلب كافة السجلات المخزنية الفعلية من قاعدة البيانات الدائمة (SQLite).
     */
    fun getStockItems(): List<StockItem> {
        val entities = runCatching { stockDao.getStockItemsPaged(limit = 1000, offset = 0) }.getOrDefault(emptyList())
        return entities.mapIndexed { index, entity ->
            val parsedId = entity.uuid.removePrefix("stock-").toLongOrNull() ?: (index + 1L)
            StockItem(
                id = parsedId,
                partId = entity.partUuid.removePrefix("part-").toLongOrNull() ?: 1L,
                locationId = entity.locationUuid?.takeIf { it.isNotBlank() }?.removePrefix("location-")?.removePrefix("loc-")?.toLongOrNull(),
                quantity = entity.quantity,
                serial = entity.serial,
                batch = entity.batch,
                status = StockStatus.fromCode(entity.statusCode),
                packaging = entity.packaging,
                purchasePrice = entity.purchasePrice,
                purchasePriceCurrency = entity.purchasePriceCurrency,
                purchaseOrderId = entity.purchaseOrderUuid?.removePrefix("po-")?.toLongOrNull(),
                supplierPartId = entity.supplierPartUuid.takeIf { it.isNotBlank() }?.removePrefix("sup-p-")?.toLongOrNull(),
                salesOrderId = entity.salesOrderUuid?.removePrefix("so-")?.toLongOrNull(),
                customerId = entity.customerUuid.takeIf { it.isNotBlank() }?.removePrefix("comp-")?.toLongOrNull(),
                buildId = entity.buildUuid?.removePrefix("bo-")?.removePrefix("build-")?.toLongOrNull(),
                isBuilding = entity.isBuilding,
                parentId = entity.parentStockItemUuid?.removePrefix("stock-")?.toLongOrNull(),
                expiryDate = entity.expiryDate,
                stocktakeDate = entity.stocktakeDate,
                stocktakeUserId = entity.stocktakeUserUuid?.removePrefix("usr-")?.toLongOrNull(),
                reviewNeeded = entity.reviewNeeded,
                deleteOnDeplete = entity.deleteOnDeplete,
                link = entity.link,
                notes = entity.notes,
                metadata = entity.metadata
            )
        }
    }

    /**
     * جلب كافة مواقع التخزين المتاحة من SQLite مع السقوط الآمن على الجدول المحلي.
     */
    fun getLocations(): List<StockLocation> = locationRepository.getLocations()

    /**
     * جلب كافة أنواع وقوالب مواقع التخزين المتاحة مع مواصفاتها الهندسية.
     */
    fun getLocationTypes(): List<StockLocationType> = locationRepository.getLocationTypes()

    /**
     * حساب توليد المسار الهرمي الكامل التراكمي للموقع من الجذر حتى النهاية.
     */
    fun getFullPathForLocation(locationId: Long?, separator: String = " / "): String =
        locationRepository.getFullPathForLocation(locationId, separator)



    /**
     * جلب سجلات الحركات والتتبع لوحدة مخزنية محددة.
     */
    fun getTrackingForStockItem(stockItemId: Long): List<StockItemTracking> {
        val entities = trackingDao.getTrackingForStockItem("stock-$stockItemId")
        return entities.mapIndexed { index, entity ->
            val parsedId = entity.uuid.removePrefix("track-").toLongOrNull() ?: (index + 1L)
            val parsedUserId = entity.userUuid?.removePrefix("usr-")?.toLongOrNull()
            StockItemTracking(
                id = parsedId,
                stockItemId = stockItemId,
                trackingType = StockTrackingType.fromCode(entity.trackingTypeCode),
                date = DateTimeUtils.formatEpochMillisToDate(entity.createdAt),
                userId = parsedUserId,
                label = entity.label,
                notes = entity.notes,
                deltas = entity.deltas
            )
        }
    }

    /**
     * جلب نتائج فحوص الجودة والقياسات الفنية لوحدة مخزنية محددة.
     */
    fun getTestResultsForStockItem(stockItemId: Long): List<StockItemTestResult> {
        val entities = testResultDao.getTestResultsForStockItem(stockItemId)
        return entities.map { entity ->
            StockItemTestResult(
                id = entity.resultId,
                stockItemId = entity.stockItemId,
                templateId = entity.templateId,
                test = entity.test,
                result = entity.result,
                value = entity.value,
                attachment = entity.attachment,
                notes = entity.notes,
                date = entity.date,
                userId = entity.userId,
                metadata = entity.metadata
            )
        }
    }

    /**
     * جلب كافة المستندات والمرفقات المرتبطة بعنصر مخزني محدد.
     */
    fun getAttachmentsForStockItem(stockItemId: Long): List<StockItemAttachment> {
        val entities = attachmentDao.getAttachmentsForStockItem(stockItemId)
        return entities.map { entity ->
            StockItemAttachment(
                id = entity.attachmentId,
                stockItemId = entity.stockItemId,
                attachment = entity.attachment,
                link = entity.link,
                comment = entity.comment,
                uploadDate = entity.uploadDate,
                userId = entity.userId,
                metadata = entity.metadata
            )
        }
    }

    /**
     * إضافة مرفق أو شهادة جديدة لعنصر مخزني (StockItemAttachment).
     */
    fun addStockItemAttachment(attachmentItem: StockItemAttachment): StockItemAttachment {
        val allocatedId = if (attachmentItem.id <= 0L) {
            runCatching {
                SqliteNumericIdAllocator.nextId("stock_item_attachments", "attachment-")
            }.getOrElse { Clock.System.now().toEpochMilliseconds() }
        } else {
            attachmentItem.id
        }
        val saved = attachmentItem.copy(id = allocatedId)
        attachmentDao.insertOrUpdate(saved.toEntity())
        return saved
    }

    /**
     * حذف مرفق مخزني محدد.
     */
    fun deleteStockItemAttachment(id: Long): Boolean {
        return attachmentDao.deleteAttachment("attachment-$id")
    }

    /**
     * تسجيل نتيجة فحص جودة واختبار فني جديد (StockItemTestResult).
     */
    fun addTestResult(testResult: StockItemTestResult): StockItemTestResult {
        val allocatedId = if (testResult.id <= 0L) {
            runCatching {
                SqliteNumericIdAllocator.nextId("stock_item_test_results", "test-result-")
            }.getOrElse { Clock.System.now().toEpochMilliseconds() }
        } else {
            testResult.id
        }
        val saved = testResult.copy(id = allocatedId)
        testResultDao.insertOrUpdate(saved.toEntity())
        return saved
    }

    /**
     * كاشف التكرار الميداني للهرمية (Auto-Collision & Duplicate Guard):
     */
    fun isLocationDuplicateUnderSameParent(
        name: String,
        parentId: Long?,
        excludeId: Long? = null,
        address: String? = null
    ): Boolean = locationRepository.isLocationDuplicateUnderSameParent(name, parentId, excludeId, address)

    /**
     * خوارزمية منع التكرار البرمجي آلياً (Auto-Collision Prevention Algorithm):
     */
    fun generateUniqueLocationName(baseName: String, parentId: Long?, excludeId: Long? = null): String =
        locationRepository.generateUniqueLocationName(baseName, parentId, excludeId)

    /**
     * إضافة أو تحديث موقع تخزيني جديد في الشجرة الهرمية لمواقع التخزين (StockLocation).
     */
    fun addLocation(location: StockLocation): StockLocation = locationRepository.addLocation(location)

    /**
     * إنشاء سلسلة هرمية ذرية للموقع المستهدف مع كافة طبقاته الوسيطة المفقودة (Atomic Transaction).
     */
    fun addLocationWithIntermediates(
        targetLocation: StockLocation,
        intermediates: List<IntermediateNodeSpec>
    ): StockLocation = locationRepository.addLocationWithIntermediates(targetLocation, intermediates)

    /**
     * تحديث بيانات موقع تخزيني قائم في الشجرة الهرمية لمواقع التخزين (StockLocation).
     */
    fun updateLocation(location: StockLocation): StockLocation = locationRepository.updateLocation(location)

    /**
     * حذف موقع تخزيني حذفاً مرناً (Soft Delete) بعد إجراء الفحوصات الأمنية الثلاثية
     */
    fun deleteLocation(locationId: Long): Boolean {
        // الفحص الأول: خلو الموقع من العناصر المخزنة
        val itemsInLoc = getStockItems().filter { it.locationId == locationId }
        if (itemsInLoc.isNotEmpty()) {
            throw IllegalArgumentException("لا يمكن حذف الموقع لأنه يحتوي على ${itemsInLoc.size} قطعة مادية مخزنة. يُرجى نقل أو إفراغ المواد أولاً.")
        }

        // الفحص الثاني: خلو الموقع من الأرفف والمواقع الفرعية
        val childLocs = getLocations().filter { it.parentId == locationId }
        if (childLocs.isNotEmpty()) {
            throw IllegalArgumentException("لا يمكن حذف الموقع لأنه يحتوي على ${childLocs.size} موقع فرعي تابع له. يُرجى نقل أو إعادة تعيين المواقع الفرعية أولاً.")
        }

        // الفحص الثالث: عدم ارتباط الموقع بأوامر بناء أو شراء معلقة
        val buildOrdersReferenced = runCatching {
            BuildOrderRepository().searchBuilds().filter {
                it.takeFromLocationId == locationId || it.destinationLocationId == locationId
            }
        }.getOrDefault(emptyList())
        if (buildOrdersReferenced.isNotEmpty()) {
            throw IllegalArgumentException("لا يمكن حذف الموقع لارتباطه بـ ${buildOrdersReferenced.size} أمر تصنيع وبناء نشط.")
        }

        val purchaseOrdersReferenced = runCatching {
            PurchaseOrderRepository().searchOrders().filter {
                it.destinationLocationId == locationId || it.destinationLocationUuid == "location-$locationId"
            }
        }.getOrDefault(emptyList())
        if (purchaseOrdersReferenced.isNotEmpty()) {
            throw IllegalArgumentException("لا يمكن حذف الموقع لارتباطه بـ ${purchaseOrdersReferenced.size} أمر شراء معلق كوجهة تسليم.")
        }

        return locationRepository.deleteLocationInternal(locationId)
    }

    /**
     * أرشفة وحفظ لقطة صورة الملصق المادية وتحديث بيانات الأرشفة لجدول الموقع.
     */
    fun saveLocationLabelSnapshot(locationId: Long, snapshotData: String): StockLocation? =
        locationRepository.saveLocationLabelSnapshot(locationId, snapshotData)

    /**
     * إضافة دفعة مواقع تخزينية متسلسلة جديدة (Bulk Location Generator).
     */
    fun addBatchLocations(locations: List<StockLocation>): List<StockLocation> =
        locationRepository.addBatchLocations(locations)

    /**
     * إضافة وحدة مخزنية جديدة وتسجيل حركة الإنشاء آلياً.
     */
    fun addStockItem(item: StockItem): StockItem {
        val allocatedId = if (item.id <= 0L) {
            runCatching {
                SqliteNumericIdAllocator.nextId("stock_items", "stock-")
            }.getOrElse { (getStockItems().maxOfOrNull { it.id } ?: 0L) + 1L }
        } else {
            item.id
        }
        val inserted = item.copy(id = allocatedId)
        stockDao.insertOrUpdate(inserted.toEntity())
        val trackingLabel = if (item.batch.startsWith("BATCH-") || item.link.isNotBlank()) {
            "استلام شحنة مخزنية (Stock In)"
        } else {
            "إنشاء وحدة مخزنية جديدة"
        }
        val trackingNotes = if (item.notes.isNotBlank() && item.link.isNotBlank()) {
            "شحنة [${item.link}]: ${item.notes}"
        } else if (item.notes.isNotBlank()) {
            item.notes
        } else {
            "الكمية الأولية: ${item.quantity} ${item.packaging}"
        }
        val userUuid = item.stocktakeUserId?.let { "usr-$it" }

        val tracking = StockItemTrackingEntity(
            uuid = AppUuid.generate(),
            stockItemUuid = "stock-$allocatedId",
            userUuid = userUuid,
            trackingTypeCode = StockTrackingType.CREATED.code,
            label = trackingLabel,
            notes = trackingNotes,
            deltas = "{\"quantity\": ${item.quantity}}",
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
        trackingDao.insertOrUpdate(tracking)
        checkAndInsertLowStockNotification(inserted.partId)
        return inserted
    }

    private fun checkAndInsertLowStockNotification(partId: Long) {
        runCatching {
            val allStock = getStockItems().filter { it.partId == partId }
            val currentTotalStock = allStock.sumOf { it.quantity }
            val parts = partDao.getPartsPaged(limit = 1000, offset = 0)
            val part = parts.find { it.uuid == "part-$partId" || it.uuid.removePrefix("part-").toLongOrNull() == partId }
            if (part != null && part.minimumStock > 0.0 && currentTotalStock <= part.minimumStock) {
                val now = Clock.System.now().toEpochMilliseconds()
                val notif = NotificationHistoryEntity(
                    uuid = AppUuid.generate(),
                    title = "⚠️ تنبيه انخفاض المخزون: ${part.name}",
                    message = "وصل الرصيد الفعلي لـ '${part.name}' إلى $currentTotalStock ${part.units}، وهو أقل من أو يساوي الحد الأدنى المحدد (${part.minimumStock} ${part.units}).",
                    notificationType = "LOW_STOCK_ALERT",
                    targetEntityUuid = part.uuid,
                    scheduledDate = now,
                    isTriggered = true
                )
                notificationDao.insertOrUpdate(notif)
            }
        }
    }

    /**
     * تجزئة كمية مخزنية إلى تشغيلة أصل وفرع وتسجيل حركات التجزئة.
     * يدعم [onBeforeCommit] لحقن فحص أو خطأ اختباري قبل تأكيد المعاملة للتأكد من التراجع الذري التام (Atomic Rollback).
     */
    fun splitStockItem(
        parentId: Long,
        splitQuantity: Double,
        onBeforeCommit: (() -> Unit)? = null
    ): StockItem {
        require(splitQuantity > 0.0) { "كمية التجزئة يجب أن تكون أكبر من الصفر" }

        val childId = SqliteNumericIdAllocator.nextId("stock_items", "stock-")
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("BEGIN IMMEDIATE;").use { it.step() }
        try {
            val parentEntity = stockDao.getStockItemById(parentId)
                ?: throw IllegalArgumentException("الوحدة المخزنية الأصلية غير موجودة (#$parentId)")
            require(parentEntity.quantity >= splitQuantity) { "الكمية المطلوبة للتجزئة أكبر من الرصيد المتوفر" }

            val now = Clock.System.now().toEpochMilliseconds()

            val newParentQty = parentEntity.quantity - splitQuantity
            stockDao.updateStockItemQuantity(parentEntity.uuid, newParentQty, now)

            val childEntity = parentEntity.copy(
                uuid = "stock-$childId",
                quantity = splitQuantity,
                parentStockItemUuid = parentEntity.uuid,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
            stockDao.insertOrUpdate(childEntity)

            val parentTracking = StockItemTrackingEntity(
                uuid = AppUuid.generate(),
                stockItemUuid = parentEntity.uuid,
                trackingTypeCode = StockTrackingType.SPLIT.code,
                label = "تجزئة مخزنية (اقتطاع كمية)",
                notes = "تم اقتطاع $splitQuantity لإنشاء الوحدة #$childId",
                deltas = "{\"splitQuantity\": $splitQuantity, \"remainingQuantity\": $newParentQty}",
                createdAt = now
            )
            val childTracking = StockItemTrackingEntity(
                uuid = AppUuid.generate(),
                stockItemUuid = childEntity.uuid,
                trackingTypeCode = StockTrackingType.SPLIT.code,
                label = "وحدة مجزأة جديدة من الأصل",
                notes = "مجزأة من الوحدة #${parentEntity.uuid}",
                deltas = "{\"initialQuantity\": $splitQuantity, \"parentUuid\": \"${parentEntity.uuid}\"}",
                createdAt = now
            )
            trackingDao.insertOrUpdate(parentTracking)
            trackingDao.insertOrUpdate(childTracking)

            onBeforeCommit?.invoke()

            conn.prepare("COMMIT;").use { it.step() }

            return StockItem(
                id = childId,
                partId = childEntity.partUuid.removePrefix("part-").toLongOrNull() ?: 1L,
                locationId = childEntity.locationUuid?.removePrefix("loc-")?.toLongOrNull(),
                quantity = childEntity.quantity,
                serial = childEntity.serial,
                batch = childEntity.batch,
                status = StockStatus.fromCode(childEntity.statusCode),
                packaging = childEntity.packaging,
                purchasePrice = childEntity.purchasePrice,
                purchasePriceCurrency = childEntity.purchasePriceCurrency,
                notes = childEntity.notes,
                link = childEntity.link,
                parentId = parentId
            )
        } catch (e: Throwable) {
            runCatching { conn.prepare("ROLLBACK;").use { it.step() } }
            throw e
        }
    }

    /**
     * تنفيذ النقل المخزني السريع (Quick Stock Transfer) ونقل الكمية كلياً أو جزئياً وتوثيق المعاملة في سجل الحركات.
     * مع تطبيق الضمانات الذرية:
     * 1. منع النقل إلى نفس الموقع.
     * 2. منع النقل إلى موقع هيكلي أو غير موجود.
     * 3. التحقق من وجود موقع المصدر والوجهة.
     * 4. التحقق من توفر الكمية المطلوبة داخل المعاملة.
     * 5. ثبات إجمالي الكمية بين المصدر والهدف.
     */
    fun transferStockItem(
        itemId: Long,
        sourceLocationId: Long?,
        targetLocationId: Long,
        quantityToTransfer: Double,
        reason: String,
        notes: String = ""
    ): Boolean {
        require(quantityToTransfer > 0.0) { "كمية النقل يجب أن تكون أكبر من الصفر" }
        require(sourceLocationId != targetLocationId) { "لا يمكن نقل المواد إلى نفس موقع التخزين الحالي" }

        // التحقق المسبق من وجود موقع الوجهة وأنه غير هيكلي
        val targetLocEntity = locationDao.getLocationById(targetLocationId)
            ?: locationDao.getLocationByUuid("loc-$targetLocationId")
        val targetLocDomain = if (targetLocEntity == null) locationRepository.getLocations().find { it.id == targetLocationId } else null

        if (targetLocEntity == null && targetLocDomain == null) {
            throw IllegalArgumentException("موقع الوجهة غير موجود: $targetLocationId")
        }

        val isStructural = targetLocEntity?.structural ?: (targetLocDomain?.structural ?: false)
        val locName = targetLocEntity?.name ?: targetLocDomain?.name ?: targetLocationId.toString()

        if (isStructural) {
            throw IllegalArgumentException("لا يمكن نقل مواد مباشرة إلى موقع هيكلي ('$locName')")
        }

        // التحقق من وجود موقع المصدر إن حُدد
        if (sourceLocationId != null) {
            val sourceLocEntity = locationDao.getLocationById(sourceLocationId)
                ?: locationDao.getLocationByUuid("loc-$sourceLocationId")
            val sourceLocDomain = if (sourceLocEntity == null) locationRepository.getLocations().find { it.id == sourceLocationId } else null
            if (sourceLocEntity == null && sourceLocDomain == null) {
                throw IllegalArgumentException("موقع المصدر غير موجود: $sourceLocationId")
            }
        }

        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("BEGIN IMMEDIATE;").use { it.step() }
        try {
            val sourceEntity = stockDao.getStockItemById(itemId)
                ?: throw IllegalArgumentException("العنصر المخزني المرتبط بطلب النقل غير موجود (#$itemId)")
            val currentQty = sourceEntity.quantity

            if (quantityToTransfer > currentQty) {
                throw IllegalArgumentException("الكمية المطلوب نقلها ($quantityToTransfer) أكبر من الكمية المتاحة في السجل الحالي ($currentQty)")
            }

            val now = Clock.System.now().toEpochMilliseconds()
            val targetLocationUuid = "loc-$targetLocationId"
            val sourceUuid = sourceEntity.uuid
            val partUuid = sourceEntity.partUuid
            val batch = sourceEntity.batch
            val statusCode = sourceEntity.statusCode

            if (quantityToTransfer == currentQty) {
                // نقل كامل: تعديل موقع السجل الحالي
                stockDao.updateStockItemLocation(sourceUuid, targetLocationUuid, now)
            } else {
                // نقل جزئي: خصم الكمية من المصدر
                stockDao.updateStockItemQuantity(sourceUuid, currentQty - quantityToTransfer, now)

                // البحث عن سجل متطابق في الموقع الجديد
                val existingTarget = stockDao.findStockItemByPartAndLocation(partUuid, targetLocationUuid, batch, statusCode)
                if (existingTarget != null) {
                    stockDao.updateStockItemQuantity(existingTarget.uuid, existingTarget.quantity + quantityToTransfer, now)
                } else {
                    val nextTargetId = runCatching {
                        SqliteNumericIdAllocator.nextId("stock_items", "stock-")
                    }.getOrElse { (getStockItems().maxOfOrNull { it.id } ?: 0L) + 1L }
                    val newTarget = sourceEntity.copy(
                        uuid = "stock-$nextTargetId",
                        locationUuid = targetLocationUuid,
                        quantity = quantityToTransfer,
                        updatedAt = now,
                        syncStatus = SyncStatus.PENDING
                    )
                    stockDao.insertOrUpdate(newTarget)
                }
            }

            // توثيق حركة النقل في سجل التتبع
            val reasonText = "نقل من موقع #${sourceLocationId ?: "بدون"} إلى #$targetLocationId | السبب: $reason${if (notes.isNotBlank()) " ($notes)" else ""}"
            trackingDao.insertOrUpdate(
                StockItemTrackingEntity(
                    uuid = AppUuid.generate(),
                    stockItemUuid = sourceUuid,
                    trackingTypeCode = StockTrackingType.MOVE.code,
                    label = if (quantityToTransfer == currentQty) "نقل موقع التخزين بالكامل" else "نقل جزئي للرصيد المخزني",
                    notes = reasonText,
                    deltas = "{\"sourceLocationId\": ${sourceLocationId ?: "null"}, \"targetLocationId\": $targetLocationId, \"transferredQuantity\": $quantityToTransfer}",
                    createdAt = now
                )
            )

            conn.prepare("COMMIT;").use { it.step() }
            return true
        } catch (e: Throwable) {
            runCatching { conn.prepare("ROLLBACK;").use { it.step() } }
            throw e
        }
    }

    /**
     * إنشاء جلسة جرد مخزني ميداني (Stocktake Session) لموقع محدد أو صنف محدد أو لكامل المخزون.
     */
    fun createStocktakeSession(locationId: Long? = null, partId: Long? = null): List<StocktakeItem> {
        val targetLocationUuid = locationId?.let { "loc-$it" }
        val targetPartUuid = partId?.let { "part-$it" }

        val dbItems = stockDao.getStockItemsPaged(
            partUuid = targetPartUuid,
            locationUuid = targetLocationUuid,
            limit = 1000,
            offset = 0
        )
        val allParts = runCatching { partDao.getPartsPaged(limit = 1000) }.getOrDefault(emptyList())
        val partNameMap = allParts.associate { (it.uuid.removePrefix("part-").toLongOrNull() ?: 0L) to it.name }
        val allLocations = locationRepository.getLocations()
        val locNameMap = allLocations.associate { it.id to it.name }

        if (dbItems.isNotEmpty()) {
            return dbItems.mapIndexed { index, entity ->
                val numericId = entity.uuid.removePrefix("stock-").toLongOrNull() ?: (index + 1L)
                val numericPartId = entity.partUuid.removePrefix("part-").toLongOrNull() ?: 1L
                val numericLocId = entity.locationUuid?.removePrefix("loc-")?.removePrefix("location-")?.toLongOrNull()
                val partName = partNameMap[numericPartId] ?: "قطعة #$numericPartId"
                val locName = if (numericLocId != null) locNameMap[numericLocId] ?: "موقع #$numericLocId" else "مستودع عام"

                StocktakeItem(
                    stockItemId = numericId,
                    stockItemUuid = entity.uuid,
                    partId = numericPartId,
                    partName = partName,
                    locationId = numericLocId,
                    locationUuid = entity.locationUuid,
                    locationName = locName,
                    batch = entity.batch,
                    bookQuantity = entity.quantity,
                    countedQuantity = entity.quantity,
                    notes = entity.notes
                )
            }
        }

        return emptyList()
    }

    /**
     * حساب وتدقيق الفروقات لجلسة الجرد الميداني قبل الاعتماد (Stocktake Audit).
     */
    fun calculateStocktakeAudit(items: List<StocktakeItem>): StocktakeSessionResult {
        val totalCounted = items.size
        val matched = items.count { it.isMatched }
        val surplus = items.count { it.isSurplus }
        val shortage = items.count { it.isShortage }
        val netVar = items.sumOf { it.variance }
        return StocktakeSessionResult(
            totalItemsCounted = totalCounted,
            matchedCount = matched,
            surplusCount = surplus,
            shortageCount = shortage,
            netVariance = netVar,
            items = items
        )
    }

    /**
     * اعتماد وتسوية الفارق لوحدة مخزنية ذرياً (Stocktake Reconciliation Atomic Transaction).
     * 1. منع الكميات السالبة.
     * 2. منع المعالجة بسجلات مخترعة أو غير موجودة.
     * 3. تعديل رصيد المخزون الفعلي في stock_items مع تحديث تاريخ الجرد والمستخدم.
     * 4. تعديل الرصيد التراكمي في parts بقيمة الفارق (الفعلي - الدفتري).
     * 5. توثيق حركة التسوية في stock_item_tracking بنوع COUNT أو ADJUST مع السبب والملاحظات.
     */
    fun reconcileStocktake(request: StocktakeReconciliationRequest): Boolean {
        require(request.countedQuantity >= 0.0) { "الكمية المجرودة الفعلية لا يمكن أن تكون سالبة" }
        require(request.reason.isNotBlank()) { "سبب تسوية الجرد إلزامي" }

        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("BEGIN IMMEDIATE;").use { it.step() }
        try {
            val entity = if (!request.stockItemUuid.isNullOrBlank()) {
                stockDao.getStockItemByUuid(request.stockItemUuid)
            } else {
                stockDao.getStockItemById(request.stockItemId)
            }

            if (entity == null) {
                throw IllegalArgumentException("سجل المخزون غير موجود (#${request.stockItemId})")
            }

            val itemUuid = entity.uuid
            val partUuid = entity.partUuid
            val currentBookQty = entity.quantity
            val difference = request.countedQuantity - currentBookQty

            val now = Clock.System.now().toEpochMilliseconds()
            val userUuid = "usr-${request.userId}"

            // 1. تحديث رصيد الوحدة المخزنية وبيانات الجرد
            stockDao.updateStocktakeReconciliation(
                uuid = itemUuid,
                newQuantity = request.countedQuantity,
                stocktakeDate = request.stocktakeDate,
                stocktakeUserUuid = userUuid,
                updatedAt = now,
                deleteOnDeplete = entity.deleteOnDeplete
            )

            // 2. تعديل الرصيد الإجمالي للصنف في جدول parts إذا وجد فارق
            if (kotlin.math.abs(difference) > 0.0001) {
                val adjusted = partDao.adjustTotalInStockByUuid(partUuid, difference)
                if (!adjusted) {
                    throw IllegalStateException("فشل تعديل رصيد الصنف التراكمي: $partUuid")
                }
            }

            // 3. توثيق حركة التسوية في سجل التتبع
            val trackingType = if (kotlin.math.abs(difference) < 0.0001) StockTrackingType.COUNT else StockTrackingType.ADJUST
            val trackingUuid = AppUuid.generate()
            trackingDao.insertOrUpdate(
                StockItemTrackingEntity(
                    uuid = trackingUuid,
                    stockItemUuid = itemUuid,
                    trackingTypeCode = trackingType.code,
                    label = if (kotlin.math.abs(difference) < 0.0001) "جرد مطابق (Stock Count Matched)" else "تسوية جرد فعلي (Stocktake Audit)",
                    notes = "السبب: ${request.reason}${if (request.notes.isNotBlank()) " | " + request.notes else ""}",
                    deltas = "{\"bookQuantity\": $currentBookQty, \"countedQuantity\": ${request.countedQuantity}, \"variance\": $difference, \"reason\": \"${request.reason}\"}",
                    createdAt = now
                )
            )

            conn.prepare("COMMIT;").use { it.step() }
            return true
        } catch (e: Throwable) {
            runCatching { conn.prepare("ROLLBACK;").use { it.step() } }
            throw e
        }
    }

    /**
     * اعتماد مجمع لدفعات تسوية الجرد (Batch Stocktake Reconciliation).
     */
    fun reconcileStocktakeBatch(requests: List<StocktakeReconciliationRequest>): Int {
        var count = 0
        for (req in requests) {
            if (reconcileStocktake(req)) {
                count++
            }
        }
        return count
    }

    /**
     * تنفيذ عملية الجرد الفعلي (Stocktake) وتسجيل حركة الجرد.
     */
    fun performStocktake(stockId: Long, userId: Long, stocktakeDate: String = DateTimeUtils.getCurrentDate()): StockItem {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("BEGIN IMMEDIATE;").use { it.step() }
        try {
            val entity = stockDao.getStockItemById(stockId)
                ?: throw IllegalArgumentException("الوحدة المخزنية غير موجودة (#$stockId)")

            val now = Clock.System.now().toEpochMilliseconds()
            val userUuid = "usr-$userId"

            stockDao.updateStocktakeReconciliation(
                uuid = entity.uuid,
                newQuantity = entity.quantity,
                stocktakeDate = stocktakeDate,
                stocktakeUserUuid = userUuid,
                updatedAt = now,
                deleteOnDeplete = entity.deleteOnDeplete
            )

            val trackingUuid = AppUuid.generate()
            trackingDao.insertOrUpdate(
                StockItemTrackingEntity(
                    uuid = trackingUuid,
                    stockItemUuid = entity.uuid,
                    trackingTypeCode = StockTrackingType.COUNT.code,
                    label = "جرد فعلي ميداني",
                    notes = "",
                    deltas = "{\"quantity\":[${entity.quantity},${entity.quantity}],\"stocktake_date\":\"$stocktakeDate\"}",
                    userUuid = userUuid,
                    createdAt = now
                )
            )

            conn.prepare("COMMIT;").use { it.step() }

            val updatedEntity = stockDao.getStockItemById(stockId) ?: entity
            val parsedId = updatedEntity.uuid.removePrefix("stock-").toLongOrNull() ?: stockId
            return StockItem(
                id = parsedId,
                partId = updatedEntity.partUuid.removePrefix("part-").toLongOrNull() ?: 1L,
                locationId = updatedEntity.locationUuid?.takeIf { it.isNotBlank() }?.removePrefix("location-")?.removePrefix("loc-")?.toLongOrNull(),
                quantity = updatedEntity.quantity,
                serial = updatedEntity.serial,
                batch = updatedEntity.batch,
                status = StockStatus.fromCode(updatedEntity.statusCode),
                packaging = updatedEntity.packaging,
                purchasePrice = updatedEntity.purchasePrice,
                purchasePriceCurrency = updatedEntity.purchasePriceCurrency,
                purchaseOrderId = updatedEntity.purchaseOrderUuid?.removePrefix("po-")?.toLongOrNull(),
                supplierPartId = updatedEntity.supplierPartUuid.takeIf { it.isNotBlank() }?.removePrefix("sup-p-")?.toLongOrNull(),
                salesOrderId = updatedEntity.salesOrderUuid?.removePrefix("so-")?.toLongOrNull(),
                customerId = updatedEntity.customerUuid.takeIf { it.isNotBlank() }?.removePrefix("comp-")?.toLongOrNull(),
                buildId = updatedEntity.buildUuid?.removePrefix("bo-")?.removePrefix("build-")?.toLongOrNull(),
                isBuilding = updatedEntity.isBuilding,
                parentId = updatedEntity.parentStockItemUuid?.removePrefix("stock-")?.toLongOrNull(),
                expiryDate = updatedEntity.expiryDate,
                stocktakeDate = stocktakeDate,
                stocktakeUserId = userId,
                reviewNeeded = false,
                deleteOnDeplete = updatedEntity.deleteOnDeplete,
                link = updatedEntity.link,
                notes = updatedEntity.notes,
                metadata = updatedEntity.metadata
            )
        } catch (e: Throwable) {
            runCatching { conn.prepare("ROLLBACK;").use { it.step() } }
            throw e
        }
    }

    /**
     * تسجيل حركة تتبع مخصصة لوحدة مخزنية.
     */
    fun recordTracking(
        stockItemId: Long,
        trackingType: StockTrackingType,
        userId: Long? = null,
        label: String = trackingType.label,
        notes: String = "",
        deltas: String = "{}"
    ): StockItemTracking {
        val now = Clock.System.now().toEpochMilliseconds()
        val trackingUuid = AppUuid.generate()
        val userUuid = userId?.let { "usr-$it" }
        trackingDao.insertOrUpdate(
            StockItemTrackingEntity(
                uuid = trackingUuid,
                stockItemUuid = "stock-$stockItemId",
                trackingTypeCode = trackingType.code,
                label = label,
                notes = notes,
                deltas = deltas,
                userUuid = userUuid,
                createdAt = now
            )
        )
        return StockItemTracking(
            id = trackingUuid.hashCode().toLong(),
            stockItemId = stockItemId,
            date = DateTimeUtils.getCurrentDateTime(),
            trackingType = trackingType,
            userId = userId,
            label = label,
            notes = notes,
            deltas = deltas
        )
    }

    /**
     * ضغط ومعالجة صورة ملصق أو باركود الوحدة المخزنية لمنع حفظ الصور الضخمة.
     */
    fun processStockImage(rawBytes: ByteArray, width: Int, height: Int): ProcessedImage {
        return imageProcessor.processAndCompressProductImage(rawBytes, width, height)
    }

    /**
     * المزامنة الدفعية للسجلات المخزنية المعلّقة (Batch Sync Request).
     */
    suspend fun syncPendingStockChanges(): Int {
        val pending = stockDao.getPendingSyncStockItems(limit = 50, offset = 0)
        if (pending.isEmpty()) return 0

        val payloads = pending.map { entity ->
            SyncPayload(
                uuid = entity.uuid,
                entityType = "StockItem",
                payloadJson = "{\"partUuid\":\"${entity.partUuid}\",\"quantity\":${entity.quantity},\"serial\":\"${entity.serial}\"}",
                isDeleted = entity.isDeleted,
                updatedAt = entity.updatedAt
            )
        }

        val response = batchSyncService.performBatchSync(payloads, Clock.System.now().toEpochMilliseconds() - 86400000)
        stockDao.updateSyncStatusForUuids(response.acceptedUuids, SyncStatus.SYNCED)
        return response.acceptedUuids.size
    }

    private fun StockItemAttachment.toEntity(): StockItemAttachmentEntity {
        return StockItemAttachmentEntity(
            uuid = "attachment-$id",
            attachmentId = id,
            stockItemId = stockItemId,
            stockItemUuid = "stock-$stockItemId",
            attachment = attachment,
            link = link,
            comment = comment,
            uploadDate = uploadDate,
            userId = userId,
            metadata = metadata,
            syncStatus = SyncStatus.PENDING
        )
    }

    private fun StockItemTestResult.toEntity(): StockItemTestResultEntity {
        return StockItemTestResultEntity(
            uuid = "test-result-$id",
            resultId = id,
            stockItemId = stockItemId,
            stockItemUuid = "stock-$stockItemId",
            templateId = templateId,
            test = test,
            result = result,
            value = value,
            attachment = attachment,
            notes = notes,
            date = date,
            userId = userId,
            metadata = metadata,
            syncStatus = SyncStatus.PENDING
        )
    }

    private fun StockItemTracking.toEntity(): StockItemTrackingEntity {
        return StockItemTrackingEntity(
            uuid = AppUuid.generate(),
            stockItemUuid = "stock-$stockItemId",
            trackingTypeCode = trackingType.code,
            label = label,
            notes = notes,
            deltas = deltas,
            userUuid = userId?.let { "usr-$it" },
            createdAt = Clock.System.now().toEpochMilliseconds(),
            syncStatus = SyncStatus.PENDING
        )
    }

    private fun StockItem.toEntity(): StockItemEntity {
        return StockItemEntity(
            uuid = "stock-$id",
            partUuid = "part-$partId",
            locationUuid = "loc-$locationId",
            quantity = quantity,
            serial = serial,
            batch = batch,
            statusCode = status.code,
            packaging = packaging,
            purchasePrice = purchasePrice,
            purchasePriceCurrency = purchasePriceCurrency,
            purchaseOrderUuid = purchaseOrderId?.let { "po-$it" },
            supplierPartUuid = supplierPartId?.let { "sup-p-$it" } ?: "",
            salesOrderUuid = salesOrderId?.let { "so-$it" },
            customerUuid = customerId?.let { "comp-$it" } ?: "",
            buildUuid = buildId?.let { "bo-$it" },
            isBuilding = isBuilding,
            parentStockItemUuid = parentId?.let { "stock-$it" },
            expiryDate = expiryDate,
            stocktakeDate = stocktakeDate,
            stocktakeUserUuid = stocktakeUserId?.let { "usr-$it" },
            reviewNeeded = reviewNeeded,
            deleteOnDeplete = deleteOnDeplete,
            link = link,
            notes = notes,
            metadata = metadata,
            syncStatus = SyncStatus.PENDING
        )
    }
}

internal fun StockLocation.toEntity(): StockLocationEntity {
    return StockLocationEntity(
        uuid = if (uuid.isNotBlank() && !uuid.startsWith("location-")) uuid else "loc-$id",
        id = id,
        parentId = parentId,
        ownerId = ownerId,
        name = name,
        description = description,
        parentUuid = parentId?.let { "loc-$it" },
        structural = structural,
        external = external,
        locationType = locationType,
        icon = icon,
        customIcon = customIcon,
        address = address,
        customCapacity = customCapacity,
        isBulkGenerated = isBulkGenerated,
        level = level,
        lft = lft,
        rght = rght,
        treeId = treeId,
        metadata = metadata,
        syncStatus = SyncStatus.PENDING
    )
}

