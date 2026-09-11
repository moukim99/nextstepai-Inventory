package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockItemTable
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.db.StockItemDao
import com.nextstepai.inventory.data.db.StockItemEntity
import com.nextstepai.inventory.media.ImageProcessor
import com.nextstepai.inventory.media.ProcessedImage
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock

import com.nextstepai.inventory.data.db.getRoomDatabase

/**
 * المستودع (Repository) المسؤول عن إدارة المخزون الفعلي (Stock Management) وتجزئة الكميات
 * والمزامنة المجمعة مع السحابة وضغط الصور.
 */
class StockRepository(
    private val stockTable: StockItemTable = StockItemTable(),
    private val stockDao: StockItemDao = StockItemDao(),
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
     * جلب مواقع التخزين المتاحة.
     */
    fun getLocations(): List<StockLocation> = stockTable.getAllLocations()

    /**
     * إضافة وحدة مخزنية جديدة.
     */
    fun addStockItem(item: StockItem): StockItem {
        val inserted = stockTable.insertStockItem(item)
        runBlocking {
            stockDao.insertOrUpdate(
                StockItemEntity(
                    uuid = "stock-${inserted.id}",
                    partId = inserted.partId,
                    locationId = inserted.locationId,
                    quantity = inserted.quantity,
                    serial = inserted.serial,
                    batch = inserted.batch,
                    statusCode = inserted.status.code,
                    packaging = inserted.packaging,
                    expiryDate = inserted.expiryDate,
                    notes = inserted.notes,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return inserted
    }

    /**
     * تجزئة كمية مخزنية إلى تشغيلة أصل وفرع (Split Stock).
     */
    fun splitStockItem(parentId: Long, splitQuantity: Double): StockItem {
        val child = stockTable.splitStockItem(parentId, splitQuantity)
        runBlocking {
            stockDao.insertOrUpdate(
                StockItemEntity(
                    uuid = "stock-${child.id}",
                    partId = child.partId,
                    locationId = child.locationId,
                    quantity = child.quantity,
                    serial = child.serial,
                    batch = child.batch,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return child
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
}
