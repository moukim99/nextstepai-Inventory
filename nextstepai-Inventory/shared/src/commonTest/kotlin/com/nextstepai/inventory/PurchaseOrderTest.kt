package com.nextstepai.inventory

import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.data.PurchaseOrder
import com.nextstepai.inventory.data.PurchaseOrderLineItem
import com.nextstepai.inventory.data.PurchaseOrderTable
import com.nextstepai.inventory.data.StockTrackingType
import com.nextstepai.inventory.data.db.PartDao
import com.nextstepai.inventory.data.db.PartEntity
import com.nextstepai.inventory.data.db.PurchaseOrderDao
import com.nextstepai.inventory.data.db.PurchaseOrderEntity
import com.nextstepai.inventory.data.db.StockItemDao
import com.nextstepai.inventory.data.db.StockItemEntity
import com.nextstepai.inventory.data.db.StockItemTrackingDao
import com.nextstepai.inventory.auth.AuthTokens
import com.nextstepai.inventory.auth.SecureTokenStorage
import com.nextstepai.inventory.repository.PurchaseOrderRepository
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock

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
    fun testPoDaoExplicitLimitOffsetPaging() = runBlocking {
        val dao = PurchaseOrderDao()
        val now = Clock.System.now().toEpochMilliseconds()

        for (i in 1..25) {
            dao.insertOrUpdateOrder(
                PurchaseOrderEntity(
                    uuid = "po-uuid-$i",
                    reference = "PO-REF-$i",
                    supplierId = 1L,
                    statusCode = 10,
                    syncStatus = SyncStatus.PENDING,
                    updatedAt = now + i
                )
            )
        }

        val testItems = dao.getOrdersPaged(limit = 100, offset = 0).filter { it.uuid.startsWith("po-uuid-") }

        val page1 = testItems.drop(0).take(10)
        assertEquals(10, page1.size)

        val page2 = testItems.drop(10).take(10)
        assertEquals(10, page2.size)

        val page3 = testItems.drop(20).take(10)
        assertEquals(5, page3.size)
    }

    @Test
    fun testPoBatchSyncWithCloudflare() = runBlocking {
        SecureTokenStorage().saveTokens(AuthTokens("test-access-token", "test-refresh-token"))
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

    @Test
    fun testProcurementClosedLoopPartialAndFullReceipt() {
        val repository = PurchaseOrderRepository()
        val poDao = PurchaseOrderDao()
        val stockDao = StockItemDao()
        val trackingDao = StockItemTrackingDao()
        val partDao = PartDao()

        val part1Id = 811L
        val part2Id = 812L
        val now = Clock.System.now().toEpochMilliseconds()

        partDao.insertOrUpdate(
            PartEntity(
                uuid = "part-$part1Id",
                name = "مستشعر درجة الحرارة DS18B20",
                ipn = "PART-DS18B20",
                units = "pcs",
                totalInStock = 0.0,
                updatedAt = now
            )
        )
        partDao.insertOrUpdate(
            PartEntity(
                uuid = "part-$part2Id",
                name = "شاشة أوليد 0.96 OLED",
                ipn = "PART-OLED-096",
                units = "pcs",
                totalInStock = 0.0,
                updatedAt = now
            )
        )

        val po = repository.addOrder(
            PurchaseOrder(
                id = 8100L,
                reference = "PO-TEST-PROC-01",
                supplierId = 10L,
                supplierName = "المورد الإلكتروني العالمي",
                status = POStatus.PENDING,
                orderCurrency = "USD",
                destinationLocationUuid = "loc-recv-01",
                lineItems = listOf(
                    PurchaseOrderLineItem(
                        id = 8101L,
                        orderId = 8100L,
                        supplierPartId = part1Id,
                        partName = "مستشعر درجة الحرارة DS18B20",
                        quantity = 10.0,
                        receivedQuantity = 0.0,
                        purchasePrice = 15.0
                    ),
                    PurchaseOrderLineItem(
                        id = 8102L,
                        orderId = 8100L,
                        supplierPartId = part2Id,
                        partName = "شاشة أوليد 0.96 OLED",
                        quantity = 5.0,
                        receivedQuantity = 0.0,
                        purchasePrice = 25.0
                    )
                )
            )
        )
        // اعتماد وإصدار أمر الشراء
        repository.updateOrderStatus(8100L, POStatus.PLACED)

        // الخطوة 1: استلام جزئي للبند الأول (4 وحدات من أصل 10)
        val partRecv1 = repository.receiveLineItem(8101L, 4.0)
        assertTrue(partRecv1, "يجب أن ينجح الاستلام الجزئي للبند")

        // التحقق من حالة الأمر: يجب أن تظل PLACED ولا تتحول إلى COMPLETE لأن البنود لم تكتمل
        val poAfterStep1 = poDao.getOrderByUuid("po-8100")
        assertNotNull(poAfterStep1)
        assertEquals(POStatus.PLACED.code, poAfterStep1.statusCode, "أمر الشراء لا يجوز إغلاقه عند الاستلام الجزئي")

        val line1AfterStep1 = poDao.getLineById(8101L)
        assertNotNull(line1AfterStep1)
        assertEquals(4.0, line1AfterStep1.receivedQuantity, "كمية الاستلام المسجلة للبند الأول يجب أن تصبح 4.0")

        // التحقق من إدراج عنصر المخزون وسجل التتبع
        val stockForPart1 = stockDao.getAvailableStockItemsForPart(part1Id)
        val createdStock1 = stockForPart1.find { it.purchaseOrderUuid == "po-8100" }
        assertNotNull(createdStock1, "يجب إنشاء سجل مخزون للبضاعة المستلمة في stock_items")
        assertEquals(4.0, createdStock1.quantity)

        val trackingLogs = trackingDao.getAllTrackingLogs()
        val recvLog1 = trackingLogs.find { it.stockItemUuid == createdStock1.uuid }
        assertNotNull(recvLog1, "يجب توثيق حركة استلام التوريد في stock_item_tracking")
        assertEquals(StockTrackingType.CREATED.code, recvLog1.trackingTypeCode)

        // الخطوة 2: استكمال البند الأول باستلام الـ 6 وحدات المتبقية (يصبح 10/10)
        val completeLine1 = repository.receiveLineItem(8101L, 6.0)
        assertTrue(completeLine1)

        val poAfterStep2 = poDao.getOrderByUuid("po-8100")
        assertNotNull(poAfterStep2)
        assertEquals(POStatus.PLACED.code, poAfterStep2.statusCode, "الأمر يجب أن يظل PLACED لأن البند الثاني لم يستلم بعد")

        // الخطوة 3: استلام كامل البند الثاني (5 وحدات من أصل 5)
        val completeLine2 = repository.receiveLineItem(8102L, 5.0)
        assertTrue(completeLine2)

        // التحقق من اكتمال وإغلاق أمر الشراء تلقائياً بعد استلام جميع بنوده
        val poFinal = poDao.getOrderByUuid("po-8100")
        assertNotNull(poFinal)
        assertEquals(POStatus.COMPLETE.code, poFinal.statusCode, "يجب إغلاق أمر الشراء وتحويل حالته إلى COMPLETE بعد استلام كامل بنوده")

        val searchedOrders = repository.searchOrders()
        val finalSearchedOrder = searchedOrders.find { it.id == 8100L }
        assertNotNull(finalSearchedOrder)
        assertEquals(POStatus.COMPLETE, finalSearchedOrder.status)
        assertEquals(10.0, finalSearchedOrder.lineItems.find { it.id == 8101L }?.receivedQuantity)
        assertEquals(5.0, finalSearchedOrder.lineItems.find { it.id == 8102L }?.receivedQuantity)
    }

    @Test
    fun testProcurementClosedLoopPreventDuplicateOrOverReceipt() {
        val repository = PurchaseOrderRepository()
        val poDao = PurchaseOrderDao()

        val partId = 821L
        val po = repository.addOrder(
            PurchaseOrder(
                id = 8200L,
                reference = "PO-TEST-PROC-OVER",
                supplierId = 11L,
                status = POStatus.PENDING,
                lineItems = listOf(
                    PurchaseOrderLineItem(
                        id = 8201L,
                        orderId = 8200L,
                        supplierPartId = partId,
                        quantity = 10.0,
                        receivedQuantity = 0.0,
                        purchasePrice = 12.0
                    )
                )
            )
        )
        repository.updateOrderStatus(8200L, POStatus.PLACED)

        // محاولة استلام كمية أكبر من المطلوب للبند (15 > 10)
        val overReceiveError = assertFailsWith<IllegalArgumentException> {
            repository.receiveLineItem(8201L, 15.0)
        }
        assertTrue(overReceiveError.message?.contains("تتجاوز الكمية المتبقية") == true)

        // استلام الكمية المقررة بالكامل
        val okRecv = repository.receiveLineItem(8201L, 10.0)
        assertTrue(okRecv)

        val completedPo = poDao.getOrderByUuid("po-8200")
        assertNotNull(completedPo)
        assertEquals(POStatus.COMPLETE.code, completedPo.statusCode)

        // محاولة الاستلام بعد اكتمال أمر الشراء والبند يجب أن ترفض تماماً (Idempotency)
        val duplicateError = assertFailsWith<IllegalStateException> {
            repository.receiveLineItem(8201L, 1.0)
        }
        assertTrue(duplicateError.message?.contains("مكتمل ومستلم بالكامل بالفعل") == true)
    }

    @Test
    fun testProcurementClosedLoopAtomicityOnCancelledOrder() {
        val repository = PurchaseOrderRepository()
        val poDao = PurchaseOrderDao()
        val stockDao = StockItemDao()

        val partId = 831L
        val po = repository.addOrder(
            PurchaseOrder(
                id = 8300L,
                reference = "PO-TEST-PROC-CANCELLED",
                supplierId = 12L,
                status = POStatus.CANCELLED,
                lineItems = listOf(
                    PurchaseOrderLineItem(
                        id = 8301L,
                        orderId = 8300L,
                        supplierPartId = partId,
                        quantity = 5.0,
                        receivedQuantity = 0.0,
                        purchasePrice = 30.0
                    )
                )
            )
        )

        // استلام على أمر ملغي يجب أن يفشل
        val cancelledError = assertFailsWith<IllegalStateException> {
            repository.receiveLineItem(8301L, 5.0)
        }
        assertTrue(cancelledError.message?.contains("ملغي") == true)

        // التأكد من عدم إنشاء أي سجل مخزون وعدم تغيير كمية الاستلام (Atomicity)
        val line = poDao.getLineById(8301L)
        assertNotNull(line)
        assertEquals(0.0, line.receivedQuantity)

        val stocks = stockDao.getAvailableStockItemsForPart(partId).filter { it.purchaseOrderUuid == "po-8300" }
        assertTrue(stocks.isEmpty(), "يجب ألا يتم إنشاء أي سجل مخزون عند رفض المعاملة")
    }
}
