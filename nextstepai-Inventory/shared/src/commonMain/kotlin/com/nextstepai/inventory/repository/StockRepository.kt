package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockItemAttachment
import com.nextstepai.inventory.data.StockItemTable
import com.nextstepai.inventory.data.StockItemTestResult
import com.nextstepai.inventory.data.StockItemTracking
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.StockTrackingType
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
import com.nextstepai.inventory.media.ImageProcessor
import com.nextstepai.inventory.media.ProcessedImage
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.util.DateTimeUtils
import kotlinx.coroutines.runBlocking
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
    private val batchSyncService: BatchSyncService = BatchSyncService(),
    private val imageProcessor: ImageProcessor = ImageProcessor(maxDimension = 1024, compressionQuality = 85)
) {
    /**
     * جلب سجلات المخزون الفعلي مقسمة صفحات (LIMIT & OFFSET) لمنع التحميل الكامل في الذاكرة.
     */
    suspend fun getStockItemsPaged(
        partId: Long? = null,
        locationId: Long? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<StockItemEntity> {
        return stockDao.getStockItemsPaged(partId = partId, locationId = locationId, limit = limit, offset = offset)
    }

    /**
     * جلب كافة السجلات المخزنية الفعلية.
     */
    fun getStockItems(): List<StockItem> = stockTable.getAllStockItems()

    /**
     * جلب كافة مواقع التخزين المتاحة.
     */
    fun getLocations(): List<StockLocation> = stockTable.getAllLocations()



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
     * إضافة وحدة مخزنية جديدة وتسجيل حركة الإنشاء آلياً.
     */
    fun addStockItem(item: StockItem): StockItem {
        val inserted = stockTable.insertStockItem(item)
        runBlocking {
            stockDao.insertOrUpdate(inserted.toEntity())
            val trackings = stockTable.getTrackingForStockItem(inserted.id)
            trackings.forEach { trackingDao.insertOrUpdate(it.toEntity()) }
        }
        return inserted
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
