package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockItemTable
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.db.StockItemDao
import com.nextstepai.inventory.data.db.StockItemEntity
import com.nextstepai.inventory.data.db.StockLocationDao
import com.nextstepai.inventory.data.db.StockLocationEntity
import com.nextstepai.inventory.media.ImageProcessor
import com.nextstepai.inventory.media.ProcessedImage
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock

/**
 * المستودع (Repository) المسؤول عن إدارة المخزون الفعلي ومواقع التخزين الهيكلية (StockLocation)
 * وتجزئة الكميات والجرد والمزامنة المجمعة مع السحابة وضغط الصور.
 */
class StockRepository(
    private val stockTable: StockItemTable = StockItemTable(),
    private val stockDao: StockItemDao = StockItemDao(),
    private val locationDao: StockLocationDao = StockLocationDao(),
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
     * إضافة وحدة مخزنية جديدة.
     */
    fun addStockItem(item: StockItem): StockItem {
        val inserted = stockTable.insertStockItem(item)
        runBlocking {
            stockDao.insertOrUpdate(inserted.toEntity())
        }
        return inserted
    }

    /**
     * تجزئة كمية مخزنية إلى تشغيلة أصل وفرع (Split Stock).
     */
    fun splitStockItem(parentId: Long, splitQuantity: Double): StockItem {
        val child = stockTable.splitStockItem(parentId, splitQuantity)
        runBlocking {
            stockDao.insertOrUpdate(child.toEntity())
        }
        return child
    }

    /**
     * تنفيذ عملية الجرد الفعلي (Stocktake).
     */
    fun performStocktake(stockId: Long, userId: Long, stocktakeDate: String = "2025-02-15"): StockItem {
        val updated = stockTable.performStocktake(stockId, userId, stocktakeDate)
        runBlocking {
            stockDao.insertOrUpdate(updated.toEntity())
        }
        return updated
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

    private fun StockLocation.toEntity(): StockLocationEntity {
        return StockLocationEntity(
            uuid = "location-$id",
            locationId = id,
            name = name,
            description = description,
            parentId = parentId,
            structural = structural,
            external = external,
            locationTypeId = locationTypeId,
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
