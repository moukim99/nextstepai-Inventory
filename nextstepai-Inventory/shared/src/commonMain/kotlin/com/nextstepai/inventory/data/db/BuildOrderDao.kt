package com.nextstepai.inventory.data.db

import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات أوامر التصنيع والإنتاج (BuildOrderDao) مع استعلامات مجزأة صريحة (LIMIT & OFFSET).
 */
class BuildOrderDao {
    private val buildEntities = mutableListOf<BuildOrderEntity>()

    /**
     * استعلام صريح محدّد الصفحات (Explicit LIMIT / OFFSET Query).
     */
    fun getBuildOrdersPaged(
        partId: Long? = null,
        statusCode: Int? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<BuildOrderEntity> {
        return buildEntities
            .filter { entity ->
                !entity.isDeleted &&
                        (partId == null || entity.partId == partId) &&
                        (statusCode == null || entity.statusCode == statusCode)
            }
            .sortedByDescending { it.updatedAt }
            .drop(offset)
            .take(limit)
    }

    /**
     * جلب السجلات المعلّقة للمزامنة الدفعية.
     */
    fun getPendingSyncBuilds(limit: Int = 50, offset: Int = 0): List<BuildOrderEntity> {
        return buildEntities
            .filter { it.syncStatus == SyncStatus.PENDING_PUSH && !it.isDeleted }
            .sortedBy { it.updatedAt }
            .drop(offset)
            .take(limit)
    }

    /**
     * إدراج أو تحديث أمر تصنيع.
     */
    fun insertOrUpdate(entity: BuildOrderEntity) {
        val index = buildEntities.indexOfFirst { it.uuid == entity.uuid }
        if (index != -1) {
            buildEntities[index] = entity
        } else {
            buildEntities.add(entity)
        }
    }

    /**
     * تحديث حالة المزامنة لأوامر التصنيع المرفوعة بالباتش.
     */
    fun updateSyncStatusForUuids(uuids: List<String>, newStatus: SyncStatus) {
        for (i in buildEntities.indices) {
            if (buildEntities[i].uuid in uuids) {
                buildEntities[i] = buildEntities[i].copy(syncStatus = newStatus)
            }
        }
    }
}
