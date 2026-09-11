package com.nextstepai.inventory.data.db

import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات المخزون الفعلي (StockItemDao) للتصفح بطلب مجزأ (LIMIT & OFFSET).
 */
class StockItemDao {
    private val stockEntities = mutableListOf<StockItemEntity>()

    /**
     * استعلام مجزأ صريح مع التقييد بالصفحات (Explicit LIMIT / OFFSET Query).
     */
    fun getStockItemsPaged(
        partId: Long? = null,
        locationId: Long? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<StockItemEntity> {
        return stockEntities
            .filter { entity ->
                !entity.isDeleted &&
                        (partId == null || entity.partId == partId) &&
                        (locationId == null || entity.locationId == locationId)
            }
            .sortedByDescending { it.updatedAt }
            .drop(offset)
            .take(limit)
    }

    /**
     * جلب السجلات المعلّقة المرفوعة بالمزامنة المجمعة.
     */
    fun getPendingSyncStockItems(limit: Int = 50, offset: Int = 0): List<StockItemEntity> {
        return stockEntities
            .filter { it.syncStatus == SyncStatus.PENDING_PUSH && !it.isDeleted }
            .sortedBy { it.updatedAt }
            .drop(offset)
            .take(limit)
    }

    /**
     * إدراج أو تحديث سجل مخزني.
     */
    fun insertOrUpdate(entity: StockItemEntity) {
        val index = stockEntities.indexOfFirst { it.uuid == entity.uuid }
        if (index != -1) {
            stockEntities[index] = entity
        } else {
            stockEntities.add(entity)
        }
    }

    /**
     * تحديث حالة المزامنة لمعرفات المقبولة في الباتش.
     */
    fun updateSyncStatusForUuids(uuids: List<String>, newStatus: SyncStatus) {
        for (i in stockEntities.indices) {
            if (stockEntities[i].uuid in uuids) {
                stockEntities[i] = stockEntities[i].copy(syncStatus = newStatus)
            }
        }
    }
}
