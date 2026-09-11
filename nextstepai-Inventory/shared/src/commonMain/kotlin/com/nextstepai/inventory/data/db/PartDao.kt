package com.nextstepai.inventory.data.db

import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول للبيانات (PartDao) للاستعلام من قاعدة البيانات المحلية باستعلامات مجزأة صريحة (Explicit LIMIT/OFFSET Queries).
 * يُمنع تحميل الجدول كاملاً في الذاكرة لشاشات القوائم.
 */
class PartDao {
    private val entities = mutableListOf<PartEntity>()

    /**
     * استعلام مجزأ صريح بالـ LIMIT و OFFSET لمنع تحميل الكيان كاملاً في الذاكرة.
     */
    fun getPartsPaged(limit: Int = 20, offset: Int = 0): List<PartEntity> {
        return entities
            .filter { !it.isDeleted }
            .sortedByDescending { it.updatedAt }
            .drop(offset)
            .take(limit)
    }

    /**
     * جلب السجلات المعلّقة للمزامنة الدفعية المجمعة (Batch Push).
     */
    fun getPendingSyncParts(limit: Int = 50, offset: Int = 0): List<PartEntity> {
        return entities
            .filter { it.syncStatus == SyncStatus.PENDING_PUSH && !it.isDeleted }
            .sortedBy { it.updatedAt }
            .drop(offset)
            .take(limit)
    }

    /**
     * حفظ أو إدراج كيان مجزأ.
     */
    fun insertOrUpdate(entity: PartEntity) {
        val index = entities.indexOfFirst { it.uuid == entity.uuid }
        if (index != -1) {
            entities[index] = entity
        } else {
            entities.add(entity)
        }
    }

    /**
     * تحديث حالة المزامنة الدفعية لأسماء UUID المقبولة.
     */
    fun updateSyncStatusForUuids(uuids: List<String>, newStatus: SyncStatus) {
        for (i in entities.indices) {
            if (entities[i].uuid in uuids) {
                entities[i] = entities[i].copy(syncStatus = newStatus)
            }
        }
    }

    /**
     * الحصول على العدد الإجمالي للسجلات غير المحذوفة لحساب الصفحات.
     */
    fun getActiveCount(): Int {
        return entities.count { !it.isDeleted }
    }
}
