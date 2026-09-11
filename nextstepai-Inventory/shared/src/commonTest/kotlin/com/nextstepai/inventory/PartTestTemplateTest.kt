package com.nextstepai.inventory

import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartTable
import com.nextstepai.inventory.data.PartTestTemplate
import com.nextstepai.inventory.data.PartTestTemplateTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PartTestTemplateTest {

    @Test
    fun testBlankNameValidation() {
        val partTable = PartTable()
        val testTable = PartTestTemplateTable(partTable)

        val part = partTable.insertPart(Part(name = "لوحة دوائر مطبوعة"))

        // اسم فحص فارغ ينبغي أن يفشل
        assertFailsWith<IllegalArgumentException> {
            testTable.insertTestTemplate(
                PartTestTemplate(
                    partId = part.id,
                    testName = "   "
                )
            )
        }
    }

    @Test
    fun testUniqueConstraintOnPartAndTestName() {
        val partTable = PartTable()
        val testTable = PartTestTemplateTable(partTable)

        val part = partTable.insertPart(Part(name = "وحدة استشعار"))

        val t1 = testTable.insertTestTemplate(
            PartTestTemplate(
                partId = part.id,
                testName = "اختبار الضغط العالي",
                required = true,
                requiresValue = true
            )
        )
        assertTrue(t1.id > 0)

        // محاولة إضافة فحص بنفس الاسم لنفس القطعة ينبغي أن يفشل (unique_together)
        assertFailsWith<IllegalArgumentException> {
            testTable.insertTestTemplate(
                PartTestTemplate(
                    partId = part.id,
                    testName = "اختبار الضغط العالي"
                )
            )
        }
    }

    @Test
    fun testGetTestTemplatesForPartAndCascadeDelete() {
        val partTable = PartTable()
        val testTable = PartTestTemplateTable(partTable)

        val part1 = partTable.insertPart(Part(name = "قطعة اختبورية A"))
        val part2 = partTable.insertPart(Part(name = "قطعة اختبورية B"))

        testTable.insertTestTemplate(
            PartTestTemplate(partId = part1.id, testName = "فحص الاستمرارية الكهربائية")
        )
        testTable.insertTestTemplate(
            PartTestTemplate(partId = part1.id, testName = "فحص الحمل الحراري")
        )
        testTable.insertTestTemplate(
            PartTestTemplate(partId = part2.id, testName = "فحص الإشارة الرقمية")
        )

        val testsForPart1 = testTable.getTestTemplatesForPart(part1.id)
        assertEquals(2, testsForPart1.size)

        val testsForPart2 = testTable.getTestTemplatesForPart(part2.id)
        assertEquals(1, testsForPart2.size)

        // الحذف المتتابع CASCADE لـ part1
        testTable.cascadeDeleteForPart(part1.id)

        val testsForPart1AfterDelete = testTable.getTestTemplatesForPart(part1.id)
        assertTrue(testsForPart1AfterDelete.isEmpty())

        // التأكد من أن part2 لم تتأثر
        assertEquals(1, testTable.getTestTemplatesForPart(part2.id).size)
    }
}
