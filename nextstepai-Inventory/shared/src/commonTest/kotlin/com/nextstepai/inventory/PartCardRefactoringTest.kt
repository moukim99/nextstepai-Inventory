package com.nextstepai.inventory

import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartTable
import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockItemTable
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.repository.PartRepository
import com.nextstepai.inventory.repository.StockRepository
import com.nextstepai.inventory.ui.PartViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PartCardRefactoringTest {

    @Test
    fun testStockBreakdownAndReservations() {
        val part = Part(
            id = 101L,
            name = "متحكم ESP32",
            ipn = "MCU-ESP32",
            minimumStock = 10.0,
            totalInStock = 30.0,
            allocatedToBuildOrders = 12.0,
            allocatedToSalesOrders = 10.0
        )

        // available = 30 - 12 - 10 = 8.0
        assertEquals(8.0, part.availableStock)
        // 8.0 < 10.0 minimumStock -> isLowStock == true
        assertTrue(part.isLowStock)
        assertEquals(12.0, part.allocatedToBuildOrders)
        assertEquals(10.0, part.allocatedToSalesOrders)
    }

    @Test
    fun testPartViewModelStockItemsLoadingAndTransfer() {
        val partTable = PartTable()
        partTable.clearAll()
        val stockTable = StockItemTable()
        stockTable.clearAll()

        val partRepo = PartRepository(partTable)
        val stockRepo = StockRepository(stockTable)

        val loc1 = stockRepo.addLocation(StockLocation(name = "المستودع الرئيسي > الممر 1 > الرف A"))
        val loc2 = stockRepo.addLocation(StockLocation(name = "المستودع الفرعي > الرف B"))

        val part = partRepo.addPart(
            Part(
                id = 9999L,
                name = "مقاومة 10K فريدة",
                ipn = "RES-10K-UNIQUE",
                minimumStock = 50.0,
                totalInStock = 200.0
            )
        )

        val item1 = stockRepo.addStockItem(
            StockItem(
                partId = part.id,
                locationId = loc1.id,
                quantity = 150.0,
                batch = "BATCH-001"
            )
        )

        val viewModel = PartViewModel(
            repository = partRepo,
            stockRepository = stockRepo
        )

        val state = viewModel.uiState.value
        val initialItems = state.stockItems.filter { it.partId == part.id }
        assertEquals(1, initialItems.size)

        // Perform transfer
        viewModel.transferStockItem(
            itemId = item1.id,
            sourceLocationId = loc1.id,
            targetLocationId = loc2.id,
            quantity = 50.0,
            reason = "نقل داخلي"
        )

        val updatedState = viewModel.uiState.value
        val partItems = updatedState.stockItems.filter { it.partId == part.id }
        assertEquals(2, partItems.size)
        assertTrue(partItems.any { it.locationId == loc1.id && it.quantity == 100.0 })
        assertTrue(partItems.any { it.locationId == loc2.id && it.quantity == 50.0 })
    }

    @Test
    fun testMinimumStockAlertNotTriggeredWhenStockIsSufficient() {
        val part = Part(
            id = 201L,
            name = "مكثف 100nF",
            minimumStock = 20.0,
            totalInStock = 100.0,
            allocatedToBuildOrders = 10.0
        )

        assertEquals(90.0, part.availableStock)
        assertFalse(part.isLowStock)
    }

    @Test
    fun testStockProgressBarLogicAndConditions() {
        // 1. Visibility condition
        val partWithoutUnits = Part(name = "قطعة بدون وحدة", units = "", minimumStock = 10.0)
        assertFalse(partWithoutUnits.units.isNotBlank() && partWithoutUnits.minimumStock > 0.0)

        val partWithoutMinStock = Part(name = "قطعة بدون حد أدنى", units = "pcs", minimumStock = 0.0)
        assertFalse(partWithoutMinStock.units.isNotBlank() && partWithoutMinStock.minimumStock > 0.0)

        val validPart = Part(name = "قطعة صالحة", units = "pcs", minimumStock = 200.0, totalInStock = 150.0)
        assertTrue(validPart.units.isNotBlank() && validPart.minimumStock > 0.0)

        // 2. Case A: Shortage State (75%)
        val shortagePart = Part(name = "قطعة نقص", units = "pcs", minimumStock = 200.0, totalInStock = 150.0)
        assertTrue(shortagePart.totalInStock < shortagePart.minimumStock)
        val shortageRatio = shortagePart.totalInStock / shortagePart.minimumStock
        assertEquals(0.75, shortageRatio)
        val shortagePct = (shortageRatio * 100).toInt()
        assertEquals(75, shortagePct)

        // 3. Case B: Safe State (+32%)
        val safePart = Part(name = "قطعة أمان", units = "pcs", minimumStock = 200.0, totalInStock = 264.0)
        assertTrue(safePart.totalInStock >= safePart.minimumStock)
        val surplusPct = (((safePart.totalInStock - safePart.minimumStock) / safePart.minimumStock) * 100).toInt()
        assertEquals(32, surplusPct)

        // 4. Case C: Overstock State (+15%)
        val overstockPart = Part(name = "قطعة تكدس", units = "pcs", minimumStock = 200.0, maximumStock = 500.0, totalInStock = 575.0)
        val maxStock = overstockPart.maximumStock!!
        assertTrue(maxStock > 0.0 && overstockPart.totalInStock > maxStock)
        val overstockPct = (((overstockPart.totalInStock - maxStock) / maxStock) * 100).toInt()
        assertEquals(15, overstockPct)
    }
}
