package com.nextstepai.inventory

import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartSalePriceTable
import com.nextstepai.inventory.data.PartTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PartSalePriceTest {

    @Test
    fun testSalableConstraintValidation() {
        val partTable = PartTable()
        val salePriceTable = PartSalePriceTable(partTable)

        val nonSalablePart = partTable.insertPart(Part(name = "مادة خام غير قابلة للبيع", salable = false))

        // محاولة إضافة شريحة سعر بيع لقطعة غير قابلة للبيع ينبغي أن ترمي خبراً مستثنى
        assertFailsWith<IllegalArgumentException> {
            salePriceTable.insertSalePrice(
                part = nonSalablePart,
                quantity = 1.0,
                price = 25.0
            )
        }
    }

    @Test
    fun testQuantityAndUniqueConstraintValidation() {
        val partTable = PartTable()
        val salePriceTable = PartSalePriceTable(partTable)

        val salablePart = partTable.insertPart(Part(name = "قطعة تجارية قابلة للبيع", salable = true))

        salePriceTable.insertSalePrice(part = salablePart, quantity = 1.0, price = 50.0)
        salePriceTable.insertSalePrice(part = salablePart, quantity = 10.0, price = 40.0)

        // محاولة إضافة كمية مكررة نفس القيمة ينبغي أن ترمي خبراً مستثنى
        assertFailsWith<IllegalArgumentException> {
            salePriceTable.insertSalePrice(part = salablePart, quantity = 10.0, price = 38.0)
        }

        val tiers = salePriceTable.getSalePricesForPart(salablePart.id)
        assertEquals(2, tiers.size)
        assertEquals(1.0, tiers[0].quantity)
        assertEquals(10.0, tiers[1].quantity)
    }

    @Test
    fun testBestSalePriceTierSelection() {
        val partTable = PartTable()
        val salePriceTable = PartSalePriceTable(partTable)

        val salablePart = partTable.insertPart(Part(name = "جهاز إلكتروني", salable = true))

        salePriceTable.insertSalePrice(part = salablePart, quantity = 1.0, price = 100.0)
        salePriceTable.insertSalePrice(part = salablePart, quantity = 5.0, price = 90.0)
        salePriceTable.insertSalePrice(part = salablePart, quantity = 20.0, price = 75.0)

        // للطلب بكمية 3: الشريحة الأفضل هي كمية 1 بسعر 100
        val tierFor3 = salePriceTable.getBestSalePriceForQuantity(salablePart.id, 3.0)
        assertNotNull(tierFor3)
        assertEquals(100.0, tierFor3.price)

        // للطلب بكمية 12: الشريحة الأفضل هي كمية 5 بسعر 90
        val tierFor12 = salePriceTable.getBestSalePriceForQuantity(salablePart.id, 12.0)
        assertNotNull(tierFor12)
        assertEquals(90.0, tierFor12.price)

        // للطلب بكمية 50: الشريحة الأفضل هي كمية 20 بسعر 75
        val tierFor50 = salePriceTable.getBestSalePriceForQuantity(salablePart.id, 50.0)
        assertNotNull(tierFor50)
        assertEquals(75.0, tierFor50.price)
    }

    @Test
    fun testCascadeDeletePartSalePrice() {
        val partTable = PartTable()
        val salePriceTable = PartSalePriceTable(partTable)

        val salablePart = partTable.insertPart(Part(name = "قطعة تجارية للمسح", salable = true))
        salePriceTable.insertSalePrice(part = salablePart, quantity = 1.0, price = 15.0)

        assertTrue(salePriceTable.getSalePricesForPart(salablePart.id).isNotEmpty())

        salePriceTable.cascadeDeleteForPart(salablePart.id)
        assertTrue(salePriceTable.getSalePricesForPart(salablePart.id).isEmpty())
    }
}
