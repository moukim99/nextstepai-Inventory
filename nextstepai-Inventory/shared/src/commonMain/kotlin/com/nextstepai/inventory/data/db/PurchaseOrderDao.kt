package com.nextstepai.inventory.data.db

import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات أوامر الشراء (PurchaseOrderDao) للتصفح بطلب مجزأ صريح (LIMIT & OFFSET).
 */
class PurchaseOrderDao {
    private val orderEntities = mutableListOf<PurchaseOrderEntity>()
    private val lineEntities = mutableListOf<PurchaseOrderLineEntity>()

    /**
     * استعلام مجزأ صريح مع التقييد بالصفحات (Explicit LIMIT / OFFSET Query) لأوامر الشراء.
     */
    fun getOrdersPaged(
        supplierId: Long? = null,
        statusCode: Int? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<PurchaseOrderEntity> {
        return orderEntities
            .filter { entity ->
                !entity.isDeleted &&
                        (supplierId == null || entity.supplierId == supplierId) &&
                        (statusCode == null || entity.statusCode == statusCode)
            }
            .sortedByDescending { it.updatedAt }
            .drop(offset)
            .take(limit)
    }

    /**
     * جلب بنود أمر شراء محدد بأسلوب التجزئة.
     */
    fun getLinesForOrderPaged(
        orderUuid: String,
        limit: Int = 50,
        offset: Int = 0
    ): List<PurchaseOrderLineEntity> {
        return lineEntities
            .filter { !it.isDeleted && it.orderUuid == orderUuid }
            .sortedBy { it.updatedAt }
            .drop(offset)
            .take(limit)
    }

    /**
     * جلب أوامر الشراء المعلّقة للمزامنة الدفعية المجمعة.
     */
    fun getPendingSyncOrders(limit: Int = 50, offset: Int = 0): List<PurchaseOrderEntity> {
        return orderEntities
            .filter { it.syncStatus == SyncStatus.PENDING_PUSH && !it.isDeleted }
            .sortedBy { it.updatedAt }
            .drop(offset)
            .take(limit)
    }

    /**
     * إدراج أو تحديث أمر شراء.
     */
    fun insertOrUpdateOrder(entity: PurchaseOrderEntity) {
        val index = orderEntities.indexOfFirst { it.uuid == entity.uuid }
        if (index != -1) {
            orderEntities[index] = entity
        } else {
            orderEntities.add(entity)
        }
    }

    /**
     * إدراج أو تحديث بند أمر شراء.
     */
    fun insertOrUpdateLine(entity: PurchaseOrderLineEntity) {
        val index = lineEntities.indexOfFirst { it.uuid == entity.uuid }
        if (index != -1) {
            lineEntities[index] = entity
        } else {
            lineEntities.add(entity)
        }
    }

    /**
     * تحديث حالة المزامنة لأوامر الشراء المرفوعة بالباتش.
     */
    fun updateSyncStatusForUuids(uuids: List<String>, newStatus: SyncStatus) {
        for (i in orderEntities.indices) {
            if (orderEntities[i].uuid in uuids) {
                orderEntities[i] = orderEntities[i].copy(syncStatus = newStatus)
            }
        }
    }
}
