package com.nextstepai.inventory

import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartPricingTable
import com.nextstepai.inventory.data.PartTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PartPricingTest {

    @Test
    fun testPartPricingRecalculation() {
        val partTable = PartTable()
        val pricingTable = PartPricingTable(partTable)

        val purchaseablePart = partTable.insertPart(
            Part(
                name = "قطعة قابلة للشراء",
                purchaseable = true,
                minimumStock = 100.0
            )
        )

        val pricing = pricingTable.recalculatePricingForPart(purchaseablePart)
        assertNotNull(pricing)
        assertEquals(purchaseablePart.id, pricing.partId)
        assertNotNull(pricing.purchaseCostMin)
        assertNotNull(pricing.purchaseCostMax)
        assertTrue(pricing.purchaseCostMin!! <= pricing.purchaseCostMax!!)
        assertTrue(pricing.overallMin!! <= pricing.overallMax!!)
    }

    @Test
    fun testBomCostCalculationForAssemblyPart() {
        val partTable = PartTable()
        val pricingTable = PartPricingTable(partTable)

        val assemblyPart = partTable.insertPart(
            Part(
                name = "منتج مجمع جديد",
                assembly = true
            )
        )

        val bomItems = listOf(
            BomItem(partId = assemblyPart.id, subPartId = 10L, quantity = 3.0),
            BomItem(partId = assemblyPart.id, subPartId = 20L, quantity = 2.0)
        )

        val pricing = pricingTable.recalculatePricingForPart(assemblyPart, bomItems)
        assertNotNull(pricing)
        assertNotNull(pricing.bomCostMin)
        assertNotNull(pricing.bomCostMax)
        assertTrue(pricing.bomCostMin!! > 0)
        assertTrue(pricing.bomCostMin!! <= pricing.bomCostMax!!)
    }

    @Test
    fun testCascadeDeletePartPricing() {
        val partTable = PartTable()
        val pricingTable = PartPricingTable(partTable)

        val part = partTable.insertPart(Part(name = "قطعة للتسعير والتنظيف"))
        pricingTable.recalculatePricingForPart(part)

        assertNotNull(pricingTable.getPricingForPart(part.id))

        // الحذف المتتابع CASCADE
        pricingTable.cascadeDeleteForPart(part.id)
        assertTrue(pricingTable.getPricingForPart(part.id) == null)
    }
}
