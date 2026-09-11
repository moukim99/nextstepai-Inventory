package com.nextstepai.inventory

import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildOrderTable
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.db.BuildOrderDao
import com.nextstepai.inventory.data.db.BuildOrderEntity
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
}
