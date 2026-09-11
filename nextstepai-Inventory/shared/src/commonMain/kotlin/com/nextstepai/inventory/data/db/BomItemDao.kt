package com.nextstepai.inventory.data.db

import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول للبيانات (BomItemDao) للاستعلام عن بنود BOM باستخدام استعلامات التجزئة الصريحة (LIMIT & OFFSET).
 */
class BomItemDao {
    private val bomEntities = mutableListOf<BomItemEntity>()

    /**
     * استعلام مجزأ صريح بالـ LIMIT و OFFSET لمنع تحميل جدول BOM كاملاً في الذاكرة.
     */
    fun getBomItemsPaged(partId: Long? = null, limit: Int = 20, offset: Int = 0): List<BomItemEntity> {
        return bomEntities
            .filter { !it.isDeleted && (partId == null || it.partId == partId) }
            .sortedByDescending { it.updatedAt }
            .drop(offset)
            .take(limit)
    }

    /**
     * جلب السجلات المعلّقة للمزامنة الدفعية.
     */
    fun getPendingSyncBomItems(limit: Int = 50, offset: Int = 0): List<BomItemEntity> {
        return bomEntities
            .filter { it.syncStatus == SyncStatus.PENDING_PUSH && !it.isDeleted }
            .sortedBy { it.updatedAt }
            .drop(offset)
            .take(limit)
    }

    /**
     * إدراج أو تحديث بند قائمة مواد.
     */
    fun insertOrUpdate(entity: BomItemEntity) {
        val index = bomEntities.indexOfFirst { it.uuid == entity.uuid }
        if (index != -1) {
            bomEntities[index] = entity
        } else {
            bomEntities.add(entity)
        }
    }

    /**
     * تحديث حالة المزامنة لأسماء UUID المقبولة.
     */
    fun updateSyncStatusForUuids(uuids: List<String>, newStatus: SyncStatus) {
        for (i in bomEntities.indices) {
            if (bomEntities[i].uuid in uuids) {
                bomEntities[i] = bomEntities[i].copy(syncStatus = newStatus)
            }
        }
    }
}
