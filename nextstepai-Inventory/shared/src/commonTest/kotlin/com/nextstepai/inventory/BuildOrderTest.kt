package com.nextstepai.inventory

import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildOrderLineItem
import com.nextstepai.inventory.data.BuildOrderTable
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.db.BuildOrderDao
import com.nextstepai.inventory.data.db.BuildOrderEntity
import com.nextstepai.inventory.auth.AuthTokens
import com.nextstepai.inventory.auth.SecureTokenStorage
import com.nextstepai.inventory.data.StockTrackingType
import com.nextstepai.inventory.data.db.BomItemDao
import com.nextstepai.inventory.data.db.BomItemEntity
import com.nextstepai.inventory.data.db.StockItemDao
import com.nextstepai.inventory.data.db.StockItemEntity
import com.nextstepai.inventory.data.db.StockItemTrackingDao
import com.nextstepai.inventory.repository.BuildOrderRepository
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.util.AppUuid
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class BuildOrderTest {

    @Test
    fun testUniqueBuildReferenceConstraint() {
        val table = BuildOrderTable()

        table.insertBuild(
            BuildOrder(reference = "BO-UNIQUE-100", partId = 3L)
        )

        assertFailsWith<IllegalArgumentException> {
            table.insertBuild(
                BuildOrder(reference = "BO-UNIQUE-100", partId = 3L)
            )
        }
    }

    @Test
    fun testStartProductionAndOutputCompletion() {
        val table = BuildOrderTable()

        val build = table.insertBuild(
            BuildOrder(
                reference = "BO-TEST-PROGRESS",
                partId = 3L,
                quantity = 100.0,
                completedQuantity = 0.0,
                status = BuildStatus.PENDING
            )
        )

        // بدء التصنيع الفعلي
        val started = table.startProduction(build.id)
        assertTrue(started)

        // توريد 50 وحدة من أصل 100
        table.completeBuildOutput(build.id, 50.0)

        val inProgress = table.getAllBuilds().find { it.id == build.id }!!
        assertEquals(50.0, inProgress.completionPercentage.toDouble(), 0.1)

        // إنهاء الـ 50 وحدة المتبقية لإغلاق الأمر
        table.completeBuildOutput(build.id, 50.0)

        val completed = table.getAllBuilds().find { it.id == build.id }!!
        assertEquals(BuildStatus.COMPLETE, completed.status)
        assertEquals(100f, completed.completionPercentage)
    }

    @Test
    fun testBuildDaoExplicitLimitOffsetPaging() = runBlocking {
        val dao = BuildOrderDao()
        val now = Clock.System.now().toEpochMilliseconds()

        for (i in 1..25) {
            dao.insertOrUpdate(
                BuildOrderEntity(
                    uuid = "build-uuid-$i",
                    reference = "BO-REF-$i",
                    partId = 3L,
                    quantity = 10.0,
                    syncStatus = SyncStatus.PENDING,
                    updatedAt = now + i
                )
            )
        }

        val testItems = dao.getBuildOrdersPaged(limit = 100, offset = 0).filter { it.uuid.startsWith("build-uuid-") }

        val page1 = testItems.drop(0).take(10)
        assertEquals(10, page1.size)

        val page2 = testItems.drop(10).take(10)
        assertEquals(10, page2.size)

        val page3 = testItems.drop(20).take(10)
        assertEquals(5, page3.size)
    }

    @Test
    fun testBuildBatchSyncWithCloudflare() = runBlocking {
        SecureTokenStorage().saveTokens(AuthTokens("test-access-token", "test-refresh-token"))
        val repository = BuildOrderRepository()

        repository.addBuildOrder(
            BuildOrder(
                reference = "BO-SYNC-777",
                partId = 3L,
                quantity = 25.0
            )
        )

        val syncedCount = repository.syncPendingBuilds()
        assertTrue(syncedCount > 0)
    }

    @Test
    fun testLineItemStockAllocationAndConsumption() {
        val repository = BuildOrderRepository()

        val lineItem = repository.addLineItem(
            BuildOrderLineItem(
                buildId = 10L,
                bomItemId = 201L,
                subPartId = 15L,
                subPartName = "حساس الحرارة المتقدم DHT22",
                quantity = 100.0,
                allocatedQuantity = 0.0,
                consumedQuantity = 0.0
            )
        )

        // تخصيص حجز 50 وحدة من المخزون
        val allocated = repository.allocateStock(lineItem.id, 50.0)
        assertTrue(allocated)

        val updatedItems = repository.getLineItemsForBuild(10L)
        val itemAfterAlloc = updatedItems.find { it.id == lineItem.id }!!
        assertEquals(50.0, itemAfterAlloc.allocatedQuantity)
        assertEquals(50f, itemAfterAlloc.allocationPercentage)

        // استهلاك 30 وحدة في عملية الإنتاج
        val consumed = repository.consumeStock(lineItem.id, 30.0)
        assertTrue(consumed)

        val itemAfterConsume = repository.getLineItemsForBuild(10L).find { it.id == lineItem.id }!!
        assertEquals(30.0, itemAfterConsume.consumedQuantity)
    }

    @Test
    fun testAutoAllocatePerformanceAndConsistency() {
        val repository = BuildOrderRepository()

        val build = repository.addBuildOrder(
            BuildOrder(
                reference = "BO-AUTO-ALLOC-001",
                partId = 5L,
                quantity = 10.0
            )
        )

        repository.addLineItem(
            BuildOrderLineItem(
                buildId = build.id,
                bomItemId = 301L,
                subPartId = 20L,
                subPartName = "معالج شريحة ESP32-WROOM",
                quantity = 10.0,
                allocatedQuantity = 0.0
            )
        )

        // تنفيذ التخصيص التلقائي ذو الأداء العالي (Auto-Allocate)
        val startTime = Clock.System.now().toEpochMilliseconds()
        val count = repository.autoAllocateBuildOrder(build.id)
        val duration = Clock.System.now().toEpochMilliseconds() - startTime

        assertTrue(count > 0)
        assertTrue(duration < 200, "يجب أن ينتهي التخصيص الأوتوماتيكي في أقل من 200 مللي ثانية للحفاظ على استقرار التطبيق")

        val allocatedBuildItems = repository.getBuildItemsForBuild(build.id)
        assertTrue(allocatedBuildItems.isNotEmpty())
        assertEquals(10.0, allocatedBuildItems.sumOf { it.quantity })
    }

    @Test
    fun testHighVolumeBuildOrderQueryPerformanceAndStability() {
        val repository = BuildOrderRepository()

        // اختبار استقرار النظام وحجم الذاكرة عند معالجة حجم كبير من البيانات (Bulk Data Stress Test)
        val startTime = Clock.System.now().toEpochMilliseconds()
        for (i in 1..100) {
            repository.addBuildOrder(
                BuildOrder(
                    reference = "BO-STRESS-$i",
                    partId = (1..5).random().toLong(),
                    quantity = (10..500).random().toDouble(),
                    status = BuildStatus.IN_PRODUCTION,
                    issuedBy = "مختبر الأداء الذكي",
                    responsible = "فريق الجودة واختبارات الاستقرار"
                )
            )
        }
        val insertDuration = Clock.System.now().toEpochMilliseconds() - startTime

        // التحقق من أن سرعة إدراج البيانات سريعة جداً ولا تتسبب في تجميد الواجهة (Main Thread Blocking)
        assertTrue(insertDuration < 2500, "إدراج 100 سجل يجب أن يتم في أقل من 2500 مللي ثانية")

        // التصفية والبحث تحت الضغط العالي
        val searchStartTime = Clock.System.now().toEpochMilliseconds()
        val results = repository.searchBuilds(query = "STRESS", status = BuildStatus.IN_PRODUCTION)
        val searchDuration = Clock.System.now().toEpochMilliseconds() - searchStartTime

        assertTrue(results.size >= 100)
        assertTrue(searchDuration < 50, "استعلام البحث والتصفية المجرى على الفهارس المخصصة يجب أن يتم في أقل من 50 مللي ثانية")
    }

    @Test
    fun testManufacturingClosedLoopSuccess() {
        val repository = BuildOrderRepository()
        val stockDao = StockItemDao()
        val bomDao = BomItemDao()
        val trackingDao = StockItemTrackingDao()
        val now = Clock.System.now().toEpochMilliseconds()

        val parentPartId = 910L
        val subPart1Id = 911L
        val subPart2Id = 912L

        // 1. إضافة متطلبات قائمة المواد BOM في SQLite
        bomDao.insertOrUpdate(
            BomItemEntity(
                uuid = "bom-test-911",
                partId = parentPartId,
                subPartId = subPart1Id,
                quantity = 2.0,
                syncStatus = SyncStatus.SYNCED
            )
        )
        bomDao.insertOrUpdate(
            BomItemEntity(
                uuid = "bom-test-912",
                partId = parentPartId,
                subPartId = subPart2Id,
                quantity = 1.0,
                syncStatus = SyncStatus.SYNCED
            )
        )

        // 2. توفير أرصدة أولية في المخزون للمكونات
        stockDao.insertOrUpdate(
            StockItemEntity(
                uuid = "stock-comp-911",
                partUuid = "part-$subPart1Id",
                locationUuid = "loc-002",
                quantity = 50.0,
                statusCode = 10,
                updatedAt = now
            )
        )
        stockDao.insertOrUpdate(
            StockItemEntity(
                uuid = "stock-comp-912",
                partUuid = "part-$subPart2Id",
                locationUuid = "loc-003",
                quantity = 25.0,
                statusCode = 10,
                updatedAt = now
            )
        )

        // 3. إنشاء أمر تصنيع لـ 10 وحدات
        val bo = repository.addBuildOrder(
            BuildOrder(
                id = 9100L,
                reference = "BO-TEST-CLOSEDLOOP-01",
                title = "أمر تصنيع مغلق الدورة لاختبار المخزون",
                partId = parentPartId,
                partName = "لوحة تجميعية 910",
                quantity = 10.0,
                destinationLocationId = 4L,
                batch = "BATCH-910-FINAL"
            )
        )

        // 4. تنفيذ التوريد الكامل لأمر التصنيع (10 وحدات)
        val result = repository.completeBuildOutput(bo.id, 10.0)
        assertTrue(result, "عملية التوريد المغلقة يجب أن تنجح")

        // 5. التحقق من خصم كميات المكونات المستهلكة
        val updatedStocks911 = stockDao.getAvailableStockItemsForPart(subPart1Id)
        val remainingQty911 = updatedStocks911.sumOf { it.quantity }
        assertEquals(30.0, remainingQty911, 0.001, "يجب خصم 20 وحدة (2 * 10) من رصيد المكون 911")

        val updatedStocks912 = stockDao.getAvailableStockItemsForPart(subPart2Id)
        val remainingQty912 = updatedStocks912.sumOf { it.quantity }
        assertEquals(15.0, remainingQty912, 0.001, "يجب خصم 10 وحدات (1 * 10) من رصيد المكون 912")

        // 6. التحقق من إدراج المنتج النهائي في المخزون
        val finishedGoods = stockDao.getAvailableStockItemsForPart(parentPartId)
        val finishedItem = finishedGoods.find { it.buildUuid == "build-${bo.id}" || it.batch == "BATCH-910-FINAL" }
        assertNotNull(finishedItem, "يجب إنشاء سجل للمنتج النهائي في جدول المخزون stock_items")
        assertEquals(10.0, finishedItem.quantity, "كمية المنتج النهائي يجب أن تطابق الكمية المصنعة")
        assertEquals("loc-4", finishedItem.locationUuid)

        // 7. التحقق من توثيق سجلات التتبع
        val allTracking = trackingDao.getAllTrackingLogs()
        val finishedTracking = allTracking.find { it.stockItemUuid == finishedItem.uuid }
        assertNotNull(finishedTracking, "يجب توثيق قيد توريد المنتج النهائي في stock_item_tracking")
        assertEquals(StockTrackingType.CREATED.code, finishedTracking.trackingTypeCode)

        // 8. التحقق من إغلاق أمر التصنيع
        val closedBo = BuildOrderDao().getBuildOrderById(bo.id)
        assertNotNull(closedBo)
        assertEquals(BuildStatus.COMPLETE.code, closedBo.statusCode, "حالة أمر التصنيع يجب أن تصبح COMPLETE")
        assertEquals(10.0, closedBo.completedQuantity, "الكمية المنجزة يجب أن تصبح 10.0")
    }

    @Test
    fun testManufacturingClosedLoopAtomicityAndRollbackOnShortage() {
        val repository = BuildOrderRepository()
        val stockDao = StockItemDao()
        val bomDao = BomItemDao()
        val now = Clock.System.now().toEpochMilliseconds()

        val parentPartId = 920L
        val subPartId = 921L

        bomDao.insertOrUpdate(
            BomItemEntity(
                uuid = "bom-test-921",
                partId = parentPartId,
                subPartId = subPartId,
                quantity = 5.0,
                syncStatus = SyncStatus.SYNCED
            )
        )

        // توفير 20 وحدة فقط في المخزون (عجز بمقدار 30 وحدة)
        stockDao.insertOrUpdate(
            StockItemEntity(
                uuid = "stock-comp-921-scarce",
                partUuid = "part-$subPartId",
                locationUuid = "loc-002",
                quantity = 20.0,
                statusCode = 10,
                updatedAt = now
            )
        )

        val bo = repository.addBuildOrder(
            BuildOrder(
                id = 9200L,
                reference = "BO-TEST-SHORTAGE-02",
                partId = parentPartId,
                quantity = 10.0
            )
        )

        // محاولة التوريد يجب أن تفشل استباقياً وترمي استثناء بسبب نقص المخزون
        val error = assertFailsWith<IllegalStateException> {
            repository.completeBuildOutput(bo.id, 10.0)
        }
        assertTrue(error.message?.contains("رصيد المخزون غير كافٍ") == true)

        // التحقق من الذرية والتراجع (Rollback): لا خصم، ولا منتج نهائي، وحالة الأمر لم تتغير
        val remainingStock = stockDao.getAvailableStockItemsForPart(subPartId).sumOf { it.quantity }
        assertEquals(20.0, remainingStock, "رصيد المخزون يجب ألا يتأثر عند فشل عملية التوريد (Atomicity)")

        val finishedGoods = stockDao.getAvailableStockItemsForPart(parentPartId)
        assertTrue(finishedGoods.isEmpty(), "يجب ألا يتم إنشاء أي منتج نهائي عند فشل المعاملة")

        val unchangedBo = BuildOrderDao().getBuildOrderById(bo.id)
        assertNotNull(unchangedBo)
        assertEquals(0.0, unchangedBo.completedQuantity)
        assertEquals(BuildStatus.PENDING.code, unchangedBo.statusCode)
    }

    @Test
    fun testManufacturingClosedLoopIdempotency() {
        val repository = BuildOrderRepository()
        val stockDao = StockItemDao()
        val bomDao = BomItemDao()
        val now = Clock.System.now().toEpochMilliseconds()

        val parentPartId = 930L
        val subPartId = 931L

        bomDao.insertOrUpdate(
            BomItemEntity(
                uuid = "bom-test-931",
                partId = parentPartId,
                subPartId = subPartId,
                quantity = 1.0,
                syncStatus = SyncStatus.SYNCED
            )
        )

        stockDao.insertOrUpdate(
            StockItemEntity(
                uuid = "stock-comp-931",
                partUuid = "part-$subPartId",
                locationUuid = "loc-002",
                quantity = 100.0,
                statusCode = 10,
                updatedAt = now
            )
        )

        val bo = repository.addBuildOrder(
            BuildOrder(
                id = 9300L,
                reference = "BO-TEST-IDEMPOTENT-03",
                partId = parentPartId,
                quantity = 5.0
            )
        )

        // التوريد الأول الناجح
        val firstResult = repository.completeBuildOutput(bo.id, 5.0)
        assertTrue(firstResult)

        // المحاولة الثانية بعد اكتمال الأمر يجب أن ترفض تماماً (Idempotency)
        val error = assertFailsWith<IllegalStateException> {
            repository.completeBuildOutput(bo.id, 5.0)
        }
        assertTrue(error.message?.contains("مكتمل بالفعل") == true)

        // التأكد من أن الرصيد لم يخصم مرة ثانية (100 - 5 = 95)
        val stockAfter = stockDao.getAvailableStockItemsForPart(subPartId).sumOf { it.quantity }
        assertEquals(95.0, stockAfter, "لا يجوز خصم المخزون مرتين عند إعادة محاولة توريد أمر مكتمل")
    }
}

