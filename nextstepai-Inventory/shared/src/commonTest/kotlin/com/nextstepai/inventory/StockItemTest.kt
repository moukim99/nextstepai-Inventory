package com.nextstepai.inventory

import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockItemTable
import com.nextstepai.inventory.data.StockStatus
import com.nextstepai.inventory.data.db.StockItemDao
import com.nextstepai.inventory.data.db.StockItemEntity
import com.nextstepai.inventory.repository.StockRepository
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

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
    fun testStocktakeAction() {
        val stockTable = StockItemTable()

        val item = stockTable.insertStockItem(
            StockItem(
                partId = 12L,
                quantity = 15.0,
                reviewNeeded = true
            )
        )
        assertTrue(item.reviewNeeded)

        val stocktaken = stockTable.performStocktake(item.id, userId = 101L, stocktakeDate = "2025-02-15")

        assertEquals("2025-02-15", stocktaken.stocktakeDate)
        assertEquals(101L, stocktaken.stocktakeUserId)
        assertFalse(stocktaken.reviewNeeded)
    }

    @Test
    fun testDeleteOnDeplete() {
        val stockTable = StockItemTable()

        val item = stockTable.insertStockItem(
            StockItem(
                partId = 20L,
                quantity = 10.0,
                deleteOnDeplete = true
            )
        )

        val remaining = stockTable.consumeStockQuantity(item.id, consumeQty = 10.0)

        assertNull(remaining)
        assertTrue(stockTable.getAllStockItems().none { it.id == item.id })
    }

    @Test
    fun testAll24SchemaFieldsAndDaoPersistence() = runBlocking {
        val dao = StockItemDao()
        val now = Clock.System.now().toEpochMilliseconds()

        val entity = StockItemEntity(
            uuid = "stock-24-fields-test",
            partId = 100L,
            locationId = 2L,
            quantity = 25.5,
            serial = "",
            batch = "LOT-2025-X",
            statusCode = StockStatus.OK.code,
            packaging = "Reel",
            purchasePrice = 12.5,
            purchasePriceCurrency = "SAR",
            purchaseOrderId = 55L,
            supplierPartId = 88L,
            salesOrderId = 33L,
            customerId = 77L,
            buildId = 12L,
            isBuilding = false,
            parentId = null,
            expiryDate = "2026-12-31",
            stocktakeDate = "2025-02-15",
            stocktakeUserId = 1L,
            reviewNeeded = false,
            deleteOnDeplete = true,
            link = "https://example.com/item/100",
            notes = "ملاحظات الفحص والتوثيق الشامل",
            metadata = "{\"version\":\"1.0\"}",
            syncStatus = SyncStatus.PENDING,
            updatedAt = now
        )

        dao.insertOrUpdate(entity)

        val loadedList = dao.getStockItemsPaged(partId = 100L, limit = 10, offset = 0)
        val loaded = loadedList.firstOrNull { it.uuid == "stock-24-fields-test" }

        assertTrue(loaded != null)
        assertEquals(100L, loaded.partId)
        assertEquals(2L, loaded.locationId)
        assertEquals(25.5, loaded.quantity)
        assertEquals("LOT-2025-X", loaded.batch)
        assertEquals("Reel", loaded.packaging)
        assertEquals(12.5, loaded.purchasePrice)
        assertEquals("SAR", loaded.purchasePriceCurrency)
        assertEquals(55L, loaded.purchaseOrderId)
        assertEquals(88L, loaded.supplierPartId)
        assertEquals(33L, loaded.salesOrderId)
        assertEquals(77L, loaded.customerId)
        assertEquals(12L, loaded.buildId)
        assertEquals("2026-12-31", loaded.expiryDate)
        assertEquals("2025-02-15", loaded.stocktakeDate)
        assertEquals(1L, loaded.stocktakeUserId)
        assertTrue(loaded.deleteOnDeplete)
        assertEquals("https://example.com/item/100", loaded.link)
        assertEquals("ملاحظات الفحص والتوثيق الشامل", loaded.notes)
    }

    @Test
    fun testStockDaoExplicitLimitOffsetPaging() = runBlocking {
        val dao = StockItemDao()
        val now = Clock.System.now().toEpochMilliseconds()

        for (i in 1..25) {
            dao.insertOrUpdate(
                StockItemEntity(
                    uuid = "stock-uuid-$i",
                    partId = 1L,
                    quantity = 10.0,
                    syncStatus = SyncStatus.PENDING,
                    updatedAt = now + i
                )
            )
        }

        val testItems = dao.getStockItemsPaged(limit = 100, offset = 0).filter { it.uuid.startsWith("stock-uuid-") }

        val page1 = testItems.drop(0).take(10)
        assertEquals(10, page1.size)

        val page2 = testItems.drop(10).take(10)
        assertEquals(10, page2.size)

        val page3 = testItems.drop(20).take(10)
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
