package com.nextstepai.inventory

import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockItemTable
import com.nextstepai.inventory.data.db.StockItemDao
import com.nextstepai.inventory.data.db.StockItemEntity
import com.nextstepai.inventory.repository.StockRepository
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StockItemTest {

    @Test
    fun testSerialForcesQuantityToOne() {
        val stockTable = StockItemTable()

        val item = stockTable.insertStockItem(
            StockItem(
                partId = 10L,
                quantity = 50.0, // سيتم إجبارها إلى 1.0 بسبب وجود الرقم التسلسلي
                serial = "TEST-SN-999"
            )
        )

        assertEquals(1.0, item.quantity)
        assertEquals("TEST-SN-999", item.serial)
    }

    @Test
    fun testDuplicateSerialThrowsException() {
        val stockTable = StockItemTable()

        stockTable.insertStockItem(
            StockItem(
                partId = 10L,
                serial = "DUP-SN-001"
            )
        )

        assertFailsWith<IllegalArgumentException> {
            stockTable.insertStockItem(
                StockItem(
                    partId = 10L,
                    serial = "DUP-SN-001"
                )
            )
        }
    }

    @Test
    fun testSplitStockItem() {
        val stockTable = StockItemTable()

        val parent = stockTable.insertStockItem(
            StockItem(
                partId = 5L,
                quantity = 100.0
            )
        )

        // تجزئة 30 وحدة من أصل 100
        val child = stockTable.splitStockItem(parent.id, 30.0)

        assertEquals(30.0, child.quantity)
        assertEquals(parent.id, child.parentId)

        val updatedParent = stockTable.getAllStockItems().find { it.id == parent.id }
        assertEquals(70.0, updatedParent?.quantity)
    }

    @Test
    fun testStockDaoExplicitLimitOffsetPaging() {
        val dao = StockItemDao()

        for (i in 1..25) {
            dao.insertOrUpdate(
                StockItemEntity(
                    uuid = "stock-uuid-$i",
                    partId = 1L,
                    quantity = 10.0,
                    syncStatus = SyncStatus.PENDING_PUSH,
                    updatedAt = System.currentTimeMillis() + i
                )
            )
        }

        val page1 = dao.getStockItemsPaged(limit = 10, offset = 0)
        assertEquals(10, page1.size)

        val page2 = dao.getStockItemsPaged(limit = 10, offset = 10)
        assertEquals(10, page2.size)

        val page3 = dao.getStockItemsPaged(limit = 10, offset = 20)
        assertEquals(5, page3.size)
    }

    @Test
    fun testStockBatchSyncWithCloudflare() = runBlocking {
        val repository = StockRepository()
        repository.addStockItem(
            StockItem(
                partId = 1L,
                quantity = 5.0,
                serial = "SYNC-SN-123"
            )
        )

        val syncedCount = repository.syncPendingStockChanges()
        assertTrue(syncedCount > 0)
    }
}
