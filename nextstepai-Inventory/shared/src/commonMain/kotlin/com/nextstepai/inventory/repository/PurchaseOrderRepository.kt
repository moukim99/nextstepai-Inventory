package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.data.PurchaseOrder
import com.nextstepai.inventory.data.PurchaseOrderLineItem
import com.nextstepai.inventory.data.PurchaseOrderTable
import com.nextstepai.inventory.data.db.PurchaseOrderDao
import com.nextstepai.inventory.data.db.PurchaseOrderEntity
import com.nextstepai.inventory.data.db.PurchaseOrderLineEntity
import com.nextstepai.inventory.data.db.PartDao
import com.nextstepai.inventory.data.db.StockItemDao
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
    private val stockDao: StockItemDao = StockItemDao(),
    private val partDao: PartDao = PartDao(),
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
     * البحث والفلترة في قائمة أوامر الشراء من قاعدة البيانات الدائمة (SQLite).
     */
    fun searchOrders(
        query: String = "",
        supplierId: Long? = null,
        status: POStatus? = null
    ): List<PurchaseOrder> {
        val entities = runBlocking {
            orderDao.getOrdersPaged(
                supplierId = supplierId,
                statusCode = status?.code,
                limit = 500,
                offset = 0
            )
        }
        if (entities.isNotEmpty()) {
            val allTableOrders = orderTable.getAllOrders()
            var result = entities.mapIndexed { index, entity ->
                val matchingTableOrder = allTableOrders.find { it.reference.equals(entity.reference, ignoreCase = true) }
                val numericId = entity.uuid.removePrefix("po-").toLongOrNull() ?: matchingTableOrder?.id ?: (index + 1L)
                val lineItems = matchingTableOrder?.lineItems ?: emptyList()
                PurchaseOrder(
                    id = numericId,
                    reference = entity.reference,
                    supplierId = entity.supplierId,
                    supplierName = entity.supplierName,
                    status = POStatus.fromCode(entity.statusCode),
                    description = entity.description,
                    orderCurrency = entity.orderCurrency,
                    targetDate = entity.targetDate,
                    sourceType = entity.sourceType,
                    sourceReferenceUuid = entity.sourceReferenceUuid,
                    destinationLocationUuid = entity.destinationLocationUuid,
                    lineItems = lineItems
                )
            }
            if (query.isNotBlank()) {
                result = result.filter {
                    it.reference.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.supplierName.contains(query, ignoreCase = true)
                }
            }
            return result
        }
        return emptyList()
    }

    /**
     * إضافة أمر شراء جديد مع حفظ الكيان القابل للمزامنة.
     */
    fun addOrder(order: PurchaseOrder): PurchaseOrder {
        val inserted = orderTable.insertOrder(order)
        order.lineItems.forEach { line ->
            orderTable.insertLineItem(line.copy(orderId = inserted.id))
        }
        val finalOrder = orderTable.getAllOrders().find { it.id == inserted.id } ?: inserted
        runBlocking {
            orderDao.insertOrUpdateOrder(
                PurchaseOrderEntity(
                    uuid = "po-${finalOrder.id}",
                    reference = finalOrder.reference,
                    supplierId = finalOrder.supplierId,
                    supplierName = finalOrder.supplierName,
                    statusCode = finalOrder.status.code,
                    description = finalOrder.description,
                    orderCurrency = finalOrder.orderCurrency,
                    targetDate = finalOrder.targetDate,
                    totalCost = finalOrder.totalCost,
                    sourceType = finalOrder.sourceType,
                    sourceReferenceUuid = finalOrder.sourceReferenceUuid,
                    destinationLocationUuid = finalOrder.destinationLocationUuid,
                    syncStatus = SyncStatus.PENDING
                )
            )
            finalOrder.lineItems.forEach { line ->
                orderDao.insertOrUpdateLine(
                    PurchaseOrderLineEntity(
                        uuid = "po-line-${line.id}",
                        orderUuid = "po-${finalOrder.id}",
                        supplierPartId = line.supplierPartId,
                        quantity = line.quantity,
                        receivedQuantity = line.receivedQuantity,
                        purchasePrice = line.purchasePrice,
                        syncStatus = SyncStatus.PENDING
                    )
                )
            }
        }
        return finalOrder
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
     * استلام كمية من بند أمر شراء وتحديث رصيد المخزون التلقائي (PO Stock Receiving Flow).
     */
    fun receiveLineItem(lineItemId: Long, qty: Double): Boolean {
        val success = orderTable.receiveLineItem(lineItemId, qty)
        if (success) {
            val line = orderTable.getAllOrders().flatMap { it.lineItems }.find { it.id == lineItemId }
            if (line != null) {
                val order = orderTable.getAllOrders().find { it.id == line.orderId }
                runBlocking {
                    orderDao.insertOrUpdateLine(
                        PurchaseOrderLineEntity(
                            uuid = "po-line-${line.id}",
                            orderUuid = "po-${line.orderId}",
                            supplierPartId = line.supplierPartId,
                            quantity = line.quantity,
                            receivedQuantity = line.receivedQuantity,
                            purchasePrice = line.purchasePrice,
                            syncStatus = SyncStatus.PENDING
                        )
                    )

                    partDao.addStockToPart(line.supplierPartId, qty)

                    stockDao.insertStockItemForPO(
                        partId = line.supplierPartId,
                        qty = qty,
                        purchasePrice = line.purchasePrice,
                        currency = order?.orderCurrency ?: "USD",
                        batch = order?.reference ?: "PO-RECV"
                    )

                    if (order != null) {
                        orderDao.insertOrUpdateOrder(
                            PurchaseOrderEntity(
                                uuid = "po-${order.id}",
                                reference = order.reference,
                                supplierId = order.supplierId,
                                supplierName = order.supplierName,
                                statusCode = order.status.code,
                                description = order.description,
                                orderCurrency = order.orderCurrency,
                                targetDate = order.targetDate,
                                totalCost = order.totalCost,
                                sourceType = order.sourceType,
                                sourceReferenceUuid = order.sourceReferenceUuid,
                                destinationLocationUuid = order.destinationLocationUuid,
                                syncStatus = SyncStatus.PENDING
                            )
                        )
                    }
                }
            }
        }
        return success
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
