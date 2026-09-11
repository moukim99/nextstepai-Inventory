package com.nextstepai.inventory

import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.data.PurchaseOrder
import com.nextstepai.inventory.data.PurchaseOrderLineItem
import com.nextstepai.inventory.data.PurchaseOrderTable
import com.nextstepai.inventory.data.db.PurchaseOrderDao
import com.nextstepai.inventory.data.db.PurchaseOrderEntity
import com.nextstepai.inventory.repository.PurchaseOrderRepository
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PurchaseOrderTest {

    @Test
    fun testUniquePoReferenceConstraint() {
        val table = PurchaseOrderTable()

        table.insertOrder(
            PurchaseOrder(reference = "PO-UNIQUE-100", supplierId = 1L)
        )

        assertFailsWith<IllegalArgumentException> {
            table.insertOrder(
                PurchaseOrder(reference = "PO-UNIQUE-100", supplierId = 2L)
            )
        }
    }

    @Test
    fun testTotalCostAndStateLocking() {
        val table = PurchaseOrderTable()

        val order = table.insertOrder(
            PurchaseOrder(reference = "PO-TEST-200", supplierId = 1L, status = POStatus.PENDING)
        )

        table.insertLineItem(
            PurchaseOrderLineItem(orderId = order.id, supplierPartId = 1L, quantity = 10.0, purchasePrice = 5.0)
        )
        table.insertLineItem(
            PurchaseOrderLineItem(orderId = order.id, supplierPartId = 2L, quantity = 2.0, purchasePrice = 20.0)
        )

        val updatedOrder = table.getAllOrders().find { it.id == order.id }!!
        assertEquals(90.0, updatedOrder.totalCost) // (10*5) + (2*20) = 90.0

        // الاعتماد للطلب
        table.updateOrderStatus(order.id, POStatus.PLACED)

        // محاولة إضافة بند لأمر معتمد يرمي استثناء
        assertFailsWith<IllegalArgumentException> {
            table.insertLineItem(
                PurchaseOrderLineItem(orderId = order.id, supplierPartId = 3L, quantity = 1.0, purchasePrice = 10.0)
            )
        }
    }

    @Test
    fun testAutoOrderCompletionOnFullReceipt() {
        val table = PurchaseOrderTable()

        val order = table.insertOrder(
            PurchaseOrder(reference = "PO-AUTO-REC", supplierId = 1L, status = POStatus.PENDING)
        )

        val line = table.insertLineItem(
            PurchaseOrderLineItem(orderId = order.id, supplierPartId = 1L, quantity = 10.0, receivedQuantity = 0.0)
        )

        // استلام الكامل
        table.receiveLineItem(line.id, 10.0)

        val completedOrder = table.getAllOrders().find { it.id == order.id }!!
        assertEquals(POStatus.COMPLETE, completedOrder.status)
    }

    @Test
    fun testPoDaoExplicitLimitOffsetPaging() {
        val dao = PurchaseOrderDao()

        for (i in 1..25) {
            dao.insertOrUpdateOrder(
                PurchaseOrderEntity(
                    uuid = "po-uuid-$i",
                    reference = "PO-REF-$i",
                    supplierId = 1L,
                    statusCode = 10,
                    syncStatus = SyncStatus.PENDING_PUSH,
                    updatedAt = System.currentTimeMillis() + i
                )
            )
        }

        val page1 = dao.getOrdersPaged(limit = 10, offset = 0)
        assertEquals(10, page1.size)

        val page2 = dao.getOrdersPaged(limit = 10, offset = 10)
        assertEquals(10, page2.size)

        val page3 = dao.getOrdersPaged(limit = 10, offset = 20)
        assertEquals(5, page3.size)
    }

    @Test
    fun testPoBatchSyncWithCloudflare() = runBlocking {
        val repository = PurchaseOrderRepository()

        repository.addOrder(
            PurchaseOrder(
                reference = "PO-SYNC-888",
                supplierId = 1L,
                description = "اختبار مزامنة دفعية"
            )
        )

        val syncedCount = repository.syncPendingOrders()
        assertTrue(syncedCount > 0)
    }
}
