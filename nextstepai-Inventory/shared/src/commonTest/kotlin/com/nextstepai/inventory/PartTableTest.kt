package com.nextstepai.inventory

import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartTable
import com.nextstepai.inventory.repository.PartRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PartTableTest {

    @Test
    fun testPartStockCalculationAndLowStockAlert() {
        val part = Part(
            name = "مكثف 100uF",
            ipn = "CAP-100U",
            minimumStock = 20.0,
            totalInStock = 25.0,
            allocatedToBuildOrders = 5.0,
            allocatedToSalesOrders = 2.0
        )

        // الصافي = 25 - 5 - 2 = 18.0
        assertEquals(18.0, part.availableStock)
        // 18.0 < 20.0 => تنبيه انخفاض المخزون
        assertTrue(part.isLowStock)
    }

    @Test
    fun testInsertAndSearchPart() {
        val partTable = PartTable()
        val repository = PartRepository(partTable)

        val newPart = repository.addPart(
            Part(
                name = "مفتاح تشغيل شريحة Relay",
                ipn = "SW-RELAY-01",
                description = "مفتاح تتابع للتحكم في الأحمال",
                keywords = "relay switch power control",
                minimumStock = 5.0,
                totalInStock = 50.0
            )
        )

        assertTrue(newPart.id > 0)

        val searchResult = repository.searchParts(query = "SW-RELAY")
        assertEquals(1, searchResult.size)
        assertEquals("SW-RELAY-01", searchResult.first().ipn)
    }

    @Test
    fun testTemplateVariantValidation() {
        val partTable = PartTable()

        // إنشاء قطعة ليست قالباً (isTemplate = false)
        val regularPart = partTable.insertPart(
            Part(
                name = "عنصر عادي",
                isTemplate = false
            )
        )

        // محاولة ربط قطعة مشتقة بقطعة ليست قالباً يجب أن يرمي استثناء (IllegalArgumentException)
        assertFailsWith<IllegalArgumentException> {
            partTable.insertPart(
                Part(
                    name = "عنصر فرعي غير صالح",
                    variantOfId = regularPart.id
                )
            )
        }
    }

    @Test
    fun testPartsSummaryCalculation() {
        val repository = PartRepository()
        val summary = repository.getPartsSummary()

        assertTrue(summary.totalParts > 0)
        assertTrue(summary.activeParts > 0)
        assertNotNull(summary.lowStockParts)
    }
}
