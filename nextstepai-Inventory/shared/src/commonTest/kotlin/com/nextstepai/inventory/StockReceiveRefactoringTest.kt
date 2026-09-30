package com.nextstepai.inventory

import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartTable
import com.nextstepai.inventory.data.StockItemTable
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.repository.PartRepository
import com.nextstepai.inventory.repository.StockRepository
import com.nextstepai.inventory.ui.PartViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StockReceiveRefactoringTest {

    @Test
    fun testStockReceivePipelineAndBatchAutoGeneration() {
        val partTable = PartTable()
        partTable.clearAll()
        val stockTable = StockItemTable()
        stockTable.clearAll()

        val partRepo = PartRepository(partTable)
        val stockRepo = StockRepository(stockTable)

        val loc = stockRepo.addLocation(StockLocation(name = "المستودع الرئيسي > رف A"))

        val part = partRepo.addPart(
            Part(
                id = 5001L,
                name = "مستشعر درجة الحرارة DHT22",
                ipn = "SENS-DHT22",
                minimumStock = 10.0,
                totalInStock = 25.0
            )
        )

        val viewModel = PartViewModel(
            repository = partRepo,
            stockRepository = stockRepo
        )

        // Execute quick stock receive with reference doc, notes, receipt date, and responsible user
        viewModel.receiveStockItem(
            partId = part.id,
            locationId = loc.id,
            quantity = 50.0,
            packaging = "صندوق أجهزة",
            referenceDoc = "PO-2025-99",
            notes = "شحنة واردة من المورد الرئيسي",
            receiptDate = "2025-05-20",
            receivedByUserId = 4L
        )

        // 1. Check updated part cumulative stock balance
        val updatedPart = partRepo.getPartById(part.id)
        assertNotNull(updatedPart)
        assertEquals(75.0, updatedPart.totalInStock)

        // 2. Check created stock item in stock table
        val stockItems = stockRepo.getStockItems().filter { it.partId == part.id }
        assertEquals(1, stockItems.size)
        val stockItem = stockItems.first()

        assertEquals(50.0, stockItem.quantity)
        assertEquals(loc.id, stockItem.locationId)
        assertEquals("صندوق أجهزة", stockItem.packaging)
        assertEquals("PO-2025-99", stockItem.link)
        assertEquals("شحنة واردة من المورد الرئيسي", stockItem.notes)
        assertEquals("2025-05-20", stockItem.stocktakeDate)
        assertEquals(4L, stockItem.stocktakeUserId)

        // Verify batch code auto-generation format (BATCH-YYYYMMDD-XXXX)
        assertTrue(stockItem.batch.startsWith("BATCH-"), "Batch code should start with BATCH-")
        assertTrue(stockItem.batch.contains("PO2025"), "Batch code should contain sanitized ref fragment")

        // 3. Check tracking log record creation
        val trackingLogs = stockRepo.getTrackingForStockItem(stockItem.id)
        assertEquals(1, trackingLogs.size)
        val log = trackingLogs.first()
        assertEquals(4L, log.userId)
        assertEquals("استلام شحنة مخزنية (Stock In)", log.label)
        assertTrue(log.notes.contains("PO-2025-99"))
    }
}
