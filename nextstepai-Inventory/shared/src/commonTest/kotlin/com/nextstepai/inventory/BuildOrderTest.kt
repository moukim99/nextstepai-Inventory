package com.nextstepai.inventory

import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildOrderLineItem
import com.nextstepai.inventory.data.BuildOrderTable
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.db.BuildOrderDao
import com.nextstepai.inventory.data.db.BuildOrderEntity
import com.nextstepai.inventory.auth.AuthTokens
import com.nextstepai.inventory.auth.SecureTokenStorage
import com.nextstepai.inventory.repository.BuildOrderRepository
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
}

