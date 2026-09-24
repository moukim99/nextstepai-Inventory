package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockStatus
import com.nextstepai.inventory.data.StockItemAttachment
import com.nextstepai.inventory.data.StockItemTable
import com.nextstepai.inventory.data.StockItemTestResult
import com.nextstepai.inventory.data.StockItemTracking
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.StockLocationType
import com.nextstepai.inventory.data.labelImagePath
import com.nextstepai.inventory.data.withLabelSnapshot
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
import com.nextstepai.inventory.data.db.StockLocationTypeDao
import com.nextstepai.inventory.media.ImageProcessor
import com.nextstepai.inventory.media.ProcessedImage
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.util.DateTimeUtils
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.time.Clock

/**
 * المستودع (Repository) المسؤول عن إدارة المخزون الفعلي ومواقع التخزين وسجلات التتبع
 * ونتائج فحوصات الجودة ومرفقات المخزون (StockItemAttachment) وتجزئة الكميات والجرد والمزامنة مع السحابة.
 */
class StockRepository(
    private val stockTable: StockItemTable = StockItemTable(),
    private val stockDao: StockItemDao = StockItemDao(),
    private val locationDao: StockLocationDao = StockLocationDao(),
    private val trackingDao: StockItemTrackingDao = StockItemTrackingDao(),
    private val testResultDao: StockItemTestResultDao = StockItemTestResultDao(),
    private val attachmentDao: StockItemAttachmentDao = StockItemAttachmentDao(),
    private val notificationDao: NotificationHistoryDao = NotificationHistoryDao(),
    private val partDao: PartDao = PartDao(),
    private val batchSyncService: BatchSyncService = BatchSyncService(),
    private val imageProcessor: ImageProcessor = ImageProcessor(maxDimension = 1024, compressionQuality = 85)
) {
    /**
     * جلب سجلات المخزون الفعلي مقسمة صفحات (LIMIT & OFFSET) لمنع التحميل الكامل في الذاكرة.
     */
    suspend fun getStockItemsPaged(
        partId: Long? = null,
        partUuid: String? = null,
        locationId: Long? = null,
        locationUuid: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<StockItemEntity> {
        return stockDao.getStockItemsPaged(
            partId = partId,
            partUuid = partUuid,
            locationId = locationId,
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
        val entities = runBlocking { stockDao.getStockItemsPaged(limit = 1000, offset = 0) }
        return entities.mapIndexed { index, entity ->
            val parsedId = entity.uuid.removePrefix("stock-").toLongOrNull() ?: (index + 1L)
            StockItem(
                id = parsedId,
                partId = entity.partId,
                locationId = entity.locationId ?: 1L,
                quantity = entity.quantity,
                serial = entity.serial,
                batch = entity.batch,
                status = StockStatus.fromCode(entity.statusCode),
                packaging = entity.packaging,
                purchasePrice = entity.purchasePrice,
                expiryDate = entity.expiryDate,
                stocktakeDate = entity.stocktakeDate,
                notes = entity.notes
            )
        }
    }

    /**
     * جلب كافة مواقع التخزين المتاحة من SQLite مع السقوط الآمن على الجدول المحلي.
     */
    fun getLocations(): List<StockLocation> {
        val entities = runCatching { runBlocking { locationDao.getAllLocations() } }.getOrDefault(emptyList())
        if (entities.isNotEmpty()) {
            return entities.map { entity ->
                StockLocation(
                    id = entity.locationId,
                    name = entity.name,
                    description = entity.description,
                    parentId = entity.parentId,
                    structural = entity.structural,
                    external = entity.external,
                    locationType = entity.locationType,
                    ownerId = entity.ownerId,
                    icon = entity.icon,
                    customIcon = entity.customIcon,
                    address = entity.address,
                    customCapacity = entity.customCapacity,
                    isBulkGenerated = entity.isBulkGenerated,
                    level = entity.level,
                    lft = entity.lft,
                    rght = entity.rght,
                    treeId = entity.treeId,
                    metadata = entity.metadata
                )
            }
        }
        return stockTable.getAllLocations()
    }

    /**
     * جلب كافة أنواع وقوالب مواقع التخزين المتاحة مع مواصفاتها الهندسية.
     */
    fun getLocationTypes(): List<StockLocationType> {
        val locationTypeDao = StockLocationTypeDao()
        val entities = runCatching { runBlocking { locationTypeDao.getAllLocationTypes() } }.getOrDefault(emptyList())
        return entities.map { entity ->
            StockLocationType(
                id = entity.typeId,
                name = entity.name,
                description = entity.description,
                icon = entity.icon,
                customIcon = entity.customIcon,
                length = entity.length,
                width = entity.width,
                height = entity.height,
                maxWeight = entity.maxWeight,
                maxVolume = entity.maxVolume,
                metadata = entity.metadata
            )
        }
    }

    /**
     * حساب توليد المسار الهرمي الكامل التراكمي للموقع من الجذر حتى النهاية.
     */
    fun getFullPathForLocation(locationId: Long?, separator: String = " / "): String {
        return stockTable.getFullPathForLocation(locationId, separator)
    }



    /**
     * جلب سجلات الحركات والتتبع لوحدة مخزنية محددة.
     */
    fun getTrackingForStockItem(stockItemId: Long): List<StockItemTracking> {
        return stockTable.getTrackingForStockItem(stockItemId)
    }

    /**
     * جلب نتائج فحوص الجودة والقياسات الفنية لوحدة مخزنية محددة.
     */
    fun getTestResultsForStockItem(stockItemId: Long): List<StockItemTestResult> {
        return stockTable.getTestResultsForStockItem(stockItemId)
    }

    /**
     * جلب كافة المستندات والمرفقات المرتبطة بعنصر مخزني محدد.
     */
    fun getAttachmentsForStockItem(stockItemId: Long): List<StockItemAttachment> {
        return stockTable.getAttachmentsForStockItem(stockItemId)
    }

    /**
     * إضافة مرفق أو شهادة جديدة لعنصر مخزني (StockItemAttachment).
     */
    fun addStockItemAttachment(attachmentItem: StockItemAttachment): StockItemAttachment {
        val inserted = stockTable.addStockItemAttachment(attachmentItem)
        runBlocking {
            attachmentDao.insertOrUpdate(inserted.toEntity())
        }
        return inserted
    }

    /**
     * حذف مرفق مخزني محدد.
     */
    fun deleteStockItemAttachment(id: Long): Boolean {
        val deleted = stockTable.deleteStockItemAttachment(id)
        if (deleted) {
            runBlocking {
                attachmentDao.deleteAttachment("attachment-$id")
            }
        }
        return deleted
    }

    /**
     * تسجيل نتيجة فحص جودة واختبار فني جديد (StockItemTestResult).
     */
    fun addTestResult(testResult: StockItemTestResult): StockItemTestResult {
        val inserted = stockTable.addTestResult(testResult)
        runBlocking {
            testResultDao.insertOrUpdate(inserted.toEntity())
        }
        return inserted
    }

    /**
     * إضافة أو تحديث موقع تخزيني جديد في الشجرة الهرمية لمواقع التخزين (StockLocation).
     */
    fun addLocation(location: StockLocation): StockLocation {
        val inserted = stockTable.insertLocation(location)
        runBlocking {
            locationDao.insertOrUpdate(inserted.toEntity())
        }
        return inserted
    }

    /**
     * تحديث بيانات موقع تخزيني قائم في الشجرة الهرمية لمواقع التخزين (StockLocation).
     */
    fun updateLocation(location: StockLocation): StockLocation {
        return addLocation(location)
    }

    /**
     * حذف موقع تخزيني حذفاً مرناً (Soft Delete) بعد إجراء الفحوصات الأمنية الثلاثية:
     * 1. التأكد من عدم وجود عناصر ومواد مخزنة داخل الموقع.
     * 2. التأكد من عدم وجود مواقع وأرفف فرعية (Child Locations) تابعة له.
     * 3. التأكد من عدم ارتباطه بأوامر إنتاج وتصنيع نشطة أو أوامر شراء معلقة.
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

        val targetLoc = getLocations().find { it.id == locationId }
        val removedFromTable = stockTable.deleteLocation(locationId)
        runBlocking {
            locationDao.softDeleteLocation(locationId)
            targetLoc?.labelImagePath?.let { path ->
                runCatching {
                    val file = File(path)
                    if (file.exists()) file.delete()
                }
            }
        }
        return removedFromTable
    }

    /**
     * أرشفة وحفظ لقطة صورة الملصق المادية وتحديث بيانات الأرشفة لجدول الموقع.
     */
    fun saveLocationLabelSnapshot(locationId: Long, snapshotData: String): StockLocation? {
        val targetLoc = getLocations().find { it.id == locationId } ?: return null
        val genAt = Clock.System.now().toEpochMilliseconds()
        val imagePath = "files/labels/locations/loc_${targetLoc.effectiveUuid}.webp"
        val updatedLoc = targetLoc.withLabelSnapshot(imagePath, genAt, snapshotData)
        return updateLocation(updatedLoc)
    }

    /**
     * إضافة دفعة مواقع تخزينية متسلسلة جديدة (Bulk Location Generator).
     */
    fun addBatchLocations(locations: List<StockLocation>): List<StockLocation> {
        val insertedList = stockTable.insertBatchLocations(locations)
        runBlocking {
            locationDao.insertBatchLocations(insertedList.map { it.toEntity() })
        }
        return insertedList
    }

    /**
     * إضافة وحدة مخزنية جديدة وتسجيل حركة الإنشاء آلياً.
     */
    fun addStockItem(item: StockItem): StockItem {
        val inserted = stockTable.insertStockItem(item)
        runBlocking {
            stockDao.insertOrUpdate(inserted.toEntity())
            val trackings = stockTable.getTrackingForStockItem(inserted.id)
            trackings.forEach { trackingDao.insertOrUpdate(it.toEntity()) }
            checkAndInsertLowStockNotification(inserted.partId)
        }
        return inserted
    }

    private suspend fun checkAndInsertLowStockNotification(partId: Long) {
        runCatching {
            val allStock = stockTable.getAllStockItems().filter { it.partId == partId }
            val currentTotalStock = allStock.sumOf { it.quantity }
            val parts = partDao.getPartsPaged(limit = 1000, offset = 0)
            val part = parts.find { it.id == partId }
            if (part != null && part.minimumStock > 0.0 && currentTotalStock <= part.minimumStock) {
                val now = Clock.System.now().toEpochMilliseconds()
                val notif = NotificationHistoryEntity(
                    uuid = "low-stock-${part.id}-$now",
                    title = "⚠️ تنبيه انخفاض المخزون: ${part.name}",
                    message = "وصل الرصيد الفعلي لـ '${part.name}' إلى $currentTotalStock ${part.units}، وهو أقل من أو يساوي الحد الأدنى المحدد (${part.minimumStock} ${part.units}).",
                    notificationType = "LOW_STOCK_ALERT",
                    targetEntityUuid = "part-${part.id}",
                    scheduledDate = now,
                    isTriggered = true
                )
                notificationDao.insertOrUpdate(notif)
            }
        }
    }

    /**
     * تجزئة كمية مخزنية إلى تشغيلة أصل وفرع وتسجيل حركات التجزئة.
     */
    fun splitStockItem(parentId: Long, splitQuantity: Double): StockItem {
        val child = stockTable.splitStockItem(parentId, splitQuantity)
        runBlocking {
            stockDao.insertOrUpdate(child.toEntity())
            val parentTrackings = stockTable.getTrackingForStockItem(parentId)
            val childTrackings = stockTable.getTrackingForStockItem(child.id)
            (parentTrackings + childTrackings).forEach { trackingDao.insertOrUpdate(it.toEntity()) }
        }
        return child
    }

    /**
     * تنفيذ عملية الجرد الفعلي (Stocktake) وتسجيل حركة الجرد.
     */
    fun performStocktake(stockId: Long, userId: Long, stocktakeDate: String = DateTimeUtils.getCurrentDate()): StockItem {
        val updated = stockTable.performStocktake(stockId, userId, stocktakeDate)
        runBlocking {
            stockDao.insertOrUpdate(updated.toEntity())
            val trackings = stockTable.getTrackingForStockItem(stockId)
            trackings.forEach { trackingDao.insertOrUpdate(it.toEntity()) }
        }
        return updated
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
        val tracking = stockTable.recordTracking(stockItemId, trackingType, userId, label, notes, deltas)
        runBlocking {
            trackingDao.insertOrUpdate(tracking.toEntity())
        }
        return tracking
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
                payloadJson = "{\"partId\":${entity.partId},\"quantity\":${entity.quantity},\"serial\":\"${entity.serial}\"}",
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
            uuid = "tracking-$id",
            trackingId = id,
            stockItemId = stockItemId,
            stockItemUuid = "stock-$stockItemId",
            date = date,
            trackingTypeCode = trackingType.code,
            userId = userId,
            label = label,
            notes = notes,
            deltas = deltas,
            syncStatus = SyncStatus.PENDING
        )
    }



    private fun StockLocation.toEntity(): StockLocationEntity {
        return StockLocationEntity(
            uuid = "location-$id",
            locationId = id,
            name = name,
            description = description,
            parentId = parentId,
            parentUuid = parentId?.let { "location-$it" },
            structural = structural,
            external = external,
            locationType = locationType,
            ownerId = ownerId,
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

    private fun StockItem.toEntity(): StockItemEntity {
        return StockItemEntity(
            uuid = "stock-$id",
            partId = partId,
            partUuid = "part-$partId",
            locationId = locationId,
            locationUuid = locationId?.let { "location-$it" },
            quantity = quantity,
            serial = serial,
            batch = batch,
            statusCode = status.code,
            packaging = packaging,
            purchasePrice = purchasePrice,
            purchasePriceCurrency = purchasePriceCurrency,
            purchaseOrderId = purchaseOrderId,
            supplierPartId = supplierPartId,
            salesOrderId = salesOrderId,
            customerId = customerId,
            buildId = buildId,
            isBuilding = isBuilding,
            parentId = parentId,
            parentUuid = parentId?.let { "stock-$it" },
            expiryDate = expiryDate,
            stocktakeDate = stocktakeDate,
            stocktakeUserId = stocktakeUserId,
            reviewNeeded = reviewNeeded,
            deleteOnDeplete = deleteOnDeplete,
            link = link,
            notes = notes,
            metadata = metadata,
            syncStatus = SyncStatus.PENDING
        )
    }
}
