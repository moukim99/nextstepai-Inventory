package com.nextstepai.inventory.domain

import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.db.PartInternalPriceEntity
import kotlin.test.*

class PartPricingCalculatorTest {

    private fun createBasePart(
        id: Long = 100L,
        uuid: String = "part-100",
        purchaseable: Boolean = true,
        assembly: Boolean = false,
        minimumStock: Double = 0.0
    ): Part = Part(
        id = id,
        uuid = uuid,
        name = "Test Resistor",
        ipn = "RES-100",
        description = "Description",
        units = "pcs",
        minimumStock = minimumStock,
        totalInStock = 50.0,
        purchaseable = purchaseable,
        assembly = assembly
    )

    @Test
    fun testPurchaseablePartWithZeroMinimumStockUsesDefaultFallback() {
        val part = createBasePart(purchaseable = true, minimumStock = 0.0)
        val pricing = PartPricingCalculator.calculate(part)

        assertEquals(1.25, pricing.purchaseCostMin)
        assertEquals(1.25 * 1.35, pricing.purchaseCostMax)
        assertEquals(1.25, pricing.overallMin)
        assertEquals(1.25 * 1.35, pricing.overallMax)
        assertEquals("USD", pricing.currency)
    }

    @Test
    fun testPurchaseablePartWithPositiveMinimumStockUsesStockFactor() {
        val part = createBasePart(purchaseable = true, minimumStock = 100.0)
        val pricing = PartPricingCalculator.calculate(part)

        val expectedMin = 100.0 * 0.05 // 5.0
        val expectedMax = expectedMin * 1.35 // 6.75
        assertEquals(expectedMin, pricing.purchaseCostMin)
        assertEquals(expectedMax, pricing.purchaseCostMax)
        assertEquals(expectedMin, pricing.overallMin)
        assertEquals(expectedMax, pricing.overallMax)
    }

    @Test
    fun testNonPurchaseablePartHasNullPurchaseCosts() {
        val part = createBasePart(purchaseable = false)
        val pricing = PartPricingCalculator.calculate(part)

        assertNull(pricing.purchaseCostMin)
        assertNull(pricing.purchaseCostMax)
        assertEquals(0.0, pricing.overallMin)
        assertEquals(0.0, pricing.overallMax)
    }

    @Test
    fun testAssemblyWithBomItemsCalculatesBomCostRange() {
        val part = createBasePart(purchaseable = false, assembly = true)
        val bomItems = listOf(
            BomItem(id = 1L, uuid = "bom-1", partId = 100L, subPartId = 1L, quantity = 2.0),
            BomItem(id = 2L, uuid = "bom-2", partId = 100L, subPartId = 2L, quantity = 3.0)
        )
        val pricing = PartPricingCalculator.calculate(part, bomItems = bomItems)

        val expectedTotalQty = 5.0
        val expectedBomMin = expectedTotalQty * 2.50 // 12.50
        val expectedBomMax = expectedTotalQty * 3.80 // 19.00
        assertEquals(expectedBomMin, pricing.bomCostMin)
        assertEquals(expectedBomMax, pricing.bomCostMax)
        assertEquals(expectedBomMin, pricing.overallMin)
        assertEquals(expectedBomMax, pricing.overallMax)
    }

    @Test
    fun testNonAssemblyIgnoresBomItems() {
        val part = createBasePart(purchaseable = false, assembly = false)
        val bomItems = listOf(
            BomItem(id = 1L, uuid = "bom-1", partId = 100L, subPartId = 1L, quantity = 10.0)
        )
        val pricing = PartPricingCalculator.calculate(part, bomItems = bomItems)

        assertNull(pricing.bomCostMin)
        assertNull(pricing.bomCostMax)
    }

    @Test
    fun testInternalPricesRangeAndCurrencySelection() {
        val part = createBasePart(purchaseable = false)
        val internalPrices = listOf(
            PartInternalPriceEntity(id = 1L, partId = 100L, price = 12.0, currency = "EUR"),
            PartInternalPriceEntity(id = 2L, partId = 100L, price = 8.5, currency = "EUR"),
            PartInternalPriceEntity(id = 3L, partId = 100L, price = 15.0, currency = "EUR")
        )

        val pricing = PartPricingCalculator.calculate(part, internalPrices = internalPrices)

        assertEquals(8.5, pricing.internalCostMin)
        assertEquals(15.0, pricing.internalCostMax)
        assertEquals(8.5, pricing.overallMin)
        assertEquals(15.0, pricing.overallMax)
        assertEquals("EUR", pricing.currency)
    }

    @Test
    fun testOverallMinAndMaxCombinesAllSourcesCorrectly() {
        val part = createBasePart(purchaseable = true, minimumStock = 40.0, assembly = true)
        // purchaseMin = 40 * 0.05 = 2.0, purchaseMax = 2.0 * 1.35 = 2.70
        val bomItems = listOf(
            BomItem(id = 1L, uuid = "bom-1", partId = 100L, subPartId = 1L, quantity = 1.0)
        )
        // bomMin = 1.0 * 2.50 = 2.50, bomMax = 1.0 * 3.80 = 3.80
        val internalPrices = listOf(
            PartInternalPriceEntity(id = 1L, partId = 100L, price = 1.5, currency = "USD"),
            PartInternalPriceEntity(id = 2L, partId = 100L, price = 4.2, currency = "USD")
        )
        // internalMin = 1.5, internalMax = 4.2

        val pricing = PartPricingCalculator.calculate(part, bomItems, internalPrices)

        assertEquals(2.0, pricing.purchaseCostMin)
        assertEquals(2.70, pricing.purchaseCostMax)
        assertEquals(2.50, pricing.bomCostMin)
        assertEquals(3.80, pricing.bomCostMax)
        assertEquals(1.5, pricing.internalCostMin)
        assertEquals(4.2, pricing.internalCostMax)

        // allMins = [2.0, 2.50, 1.5] -> min is 1.5
        assertEquals(1.5, pricing.overallMin)
        // allMaxs = [2.70, 3.80, 4.2] -> max is 4.2
        assertEquals(4.2, pricing.overallMax)
    }

    @Test
    fun testMultiCurrencyPicksFirstInternalPriceCurrencyOrFallback() {
        val part = createBasePart(purchaseable = false)
        val mixedPrices = listOf(
            PartInternalPriceEntity(id = 1L, partId = 100L, price = 10.0, currency = "GBP"),
            PartInternalPriceEntity(id = 2L, partId = 100L, price = 15.0, currency = "EUR")
        )
        val pricing = PartPricingCalculator.calculate(part, internalPrices = mixedPrices, defaultCurrency = "USD")
        assertEquals("GBP", pricing.currency, "يجب اعتماد عملة أول شريحة سعرية داخلية مسجلة")

        val emptyPricesPricing = PartPricingCalculator.calculate(part, internalPrices = emptyList(), defaultCurrency = "JPY")
        assertEquals("JPY", emptyPricesPricing.currency, "عند غياب الأسعار الداخلية يتم اعتماد العملة الافتراضية المحددة")
    }

    @Test
    fun testLegacyParityForFractionalStockAndAssemblyCombinations() {
        // Case 1: fractional minimumStock (0.5 pcs)
        val fractionalPart = createBasePart(purchaseable = true, minimumStock = 0.5)
        val fractionalPricing = PartPricingCalculator.calculate(fractionalPart)
        assertEquals(0.5 * 0.05, fractionalPricing.purchaseCostMin)
        assertEquals(0.5 * 0.05 * 1.35, fractionalPricing.purchaseCostMax)

        // Case 2: assembly = true but empty bom items -> bom costs must be null
        val emptyBomAssembly = createBasePart(purchaseable = false, assembly = true)
        val emptyBomPricing = PartPricingCalculator.calculate(emptyBomAssembly, bomItems = emptyList())
        assertNull(emptyBomPricing.bomCostMin)
        assertNull(emptyBomPricing.bomCostMax)

        // Case 3: assembly = false but bomItems passed -> bom costs must be null
        val nonAssemblyWithBom = createBasePart(purchaseable = false, assembly = false)
        val nonAssemblyPricing = PartPricingCalculator.calculate(
            nonAssemblyWithBom,
            bomItems = listOf(BomItem(id = 1L, partId = 100L, subPartId = 2L, quantity = 5.0))
        )
        assertNull(nonAssemblyPricing.bomCostMin)
        assertNull(nonAssemblyPricing.bomCostMax)
    }
}
