package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.data.PurchaseOrder
import com.nextstepai.inventory.data.PurchaseOrderLineItem
import com.nextstepai.inventory.data.PurchaseOrderTable
import com.nextstepai.inventory.data.db.PurchaseOrderDao
import com.nextstepai.inventory.data.db.PurchaseOrderEntity
import com.nextstepai.inventory.data.db.PurchaseOrderLineEntity
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock

import com.nextstepai.inventory.data.db.getRoomDatabase

/**
 * المستودع (Repository) المسؤول عن إدارة أوامر الشراء (Purchase Order) وبنودها والمزامنة الدفعية.
 */
class PurchaseOrderRepository(
    private val orderTable: PurchaseOrderTable = PurchaseOrderTable(),
    private val orderDao: PurchaseOrderDao = PurchaseOrderDao(),
    private val batchSyncService: BatchSyncService = BatchSyncService()
) {
    /**
     * جلب قائمة أوامر الشراء المتاحة مجزأة صفحات (LIMIT & OFFSET) لمنع التحميل الكامل في الذاكرة.
     */
    suspend fun getOrdersPaged(
        supplierId: Long? = null,
        status: POStatus? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<PurchaseOrderEntity> {
        return orderDao.getOrdersPaged(supplierId = supplierId, statusCode = status?.code, limit = limit, offset = offset)
    }

    /**
     * البحث والفلترة في قائمة أوامر الشراء.
     */
    fun searchOrders(
        query: String = "",
        supplierId: Long? = null,
        status: POStatus? = null
    ): List<PurchaseOrder> {
        return orderTable.searchOrders(query = query, supplierId = supplierId, status = status)
    }

    /**
     * إضافة أمر شراء جديد مع حفظ الكيان القابل للمزامنة.
     */
    fun addOrder(order: PurchaseOrder): PurchaseOrder {
        val inserted = orderTable.insertOrder(order)
        runBlocking {
            orderDao.insertOrUpdateOrder(
                PurchaseOrderEntity(
                    uuid = "po-${inserted.id}",
                    reference = inserted.reference,
                    supplierId = inserted.supplierId,
                    supplierName = inserted.supplierName,
                    statusCode = inserted.status.code,
                    description = inserted.description,
                    orderCurrency = inserted.orderCurrency,
                    targetDate = inserted.targetDate,
                    totalCost = inserted.totalCost,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return inserted
    }

    /**
     * إضافة بند داخل أمر شراء.
     */
    fun addLineItem(item: PurchaseOrderLineItem): PurchaseOrderLineItem {
        val inserted = orderTable.insertLineItem(item)
        runBlocking {
            orderDao.insertOrUpdateLine(
                PurchaseOrderLineEntity(
                    uuid = "po-line-${inserted.id}",
                    orderUuid = "po-${inserted.orderId}",
                    supplierPartId = inserted.supplierPartId,
                    quantity = inserted.quantity,
                    receivedQuantity = inserted.receivedQuantity,
                    purchasePrice = inserted.purchasePrice,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return inserted
    }

    /**
     * استلام كمية من بند أمر شراء.
     */
    fun receiveLineItem(lineItemId: Long, qty: Double): Boolean {
        return orderTable.receiveLineItem(lineItemId, qty)
    }

    /**
     * تغيير حالة أمر الشراء (Placed / Complete / Cancelled).
     */
    fun updateOrderStatus(orderId: Long, status: POStatus): Boolean {
        return orderTable.updateOrderStatus(orderId, status)
    }

    /**
     * تنفيذ المزامنة الدفعية (Batch Sync) مع Cloudflare Worker.
     */
    suspend fun syncPendingOrders(): Int {
        val pending = orderDao.getPendingSyncOrders(limit = 50, offset = 0)
        if (pending.isEmpty()) return 0

        val payloads = pending.map { entity ->
            SyncPayload(
                uuid = entity.uuid,
                entityType = "PurchaseOrder",
                payloadJson = "{\"reference\":\"${entity.reference}\",\"supplierId\":${entity.supplierId},\"totalCost\":${entity.totalCost}}",
                isDeleted = entity.isDeleted,
                updatedAt = entity.updatedAt
            )
        }

        val response = batchSyncService.performBatchSync(payloads, Clock.System.now().toEpochMilliseconds() - 86400000)
        orderDao.updateSyncStatusForUuids(response.acceptedUuids, SyncStatus.SYNCED)
        return response.acceptedUuids.size
    }
}
