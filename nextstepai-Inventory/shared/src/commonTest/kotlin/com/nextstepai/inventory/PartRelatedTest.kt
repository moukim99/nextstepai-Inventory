package com.nextstepai.inventory

import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartRelatedTable
import com.nextstepai.inventory.data.PartTable
import com.nextstepai.inventory.repository.PartRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PartRelatedTest {

    @Test
    fun testSelfRelationPrevention() {
        val partTable = PartTable()
        val relatedTable = PartRelatedTable(partTable)

        val part = partTable.insertPart(Part(name = "قطعة شريحة"))

        // محاولة ربط القطعة بنفسها يجب أن يفشل (Self-Relation Prevention)
        assertFailsWith<IllegalArgumentException> {
            relatedTable.insertPartRelated(part1Id = part.id, part2Id = part.id)
        }
    }

    @Test
    fun testBidirectionalSymmetricUniqueness() {
        val partTable = PartTable()
        val relatedTable = PartRelatedTable(partTable)

        val p1 = partTable.insertPart(Part(name = "قطعة أصلية A"))
        val p2 = partTable.insertPart(Part(name = "قطعة شبيهة B"))

        // 1. إضافة العلاقة (A, B)
        val rel1 = relatedTable.insertPartRelated(p1.id, p2.id)
        assertTrue(rel1.id > 0)

        // 2. محاولة إضافة نفس العلاقة بنفس الاتجاه (A, B) يجب أن تفشل
        assertFailsWith<IllegalArgumentException> {
            relatedTable.insertPartRelated(p1.id, p2.id)
        }

        // 3. محاولة إضافة العلاقة في الاتجاه العكسي (B, A) يجب أن تفشل لقيد التماثل
        assertFailsWith<IllegalArgumentException> {
            relatedTable.insertPartRelated(p2.id, p1.id)
        }
    }

    @Test
    fun testBidirectionalRetrieval() {
        val partTable = PartTable()
        val relatedTable = PartRelatedTable(partTable)

        val p1 = partTable.insertPart(Part(name = "مستشعر حرارة A"))
        val p2 = partTable.insertPart(Part(name = "مستشعر حرارة B"))

        relatedTable.insertPartRelated(p1.id, p2.id)

        // جلب القطع ذات الصلة لـ p1 ينبغي أن يرجع p2
        val relatedForP1 = relatedTable.getRelatedPartsForPart(p1.id, partTable.getAllParts())
        assertEquals(1, relatedForP1.size)
        assertEquals(p2.id, relatedForP1.first().relatedPart.id)

        // جلب القطع ذات الصلة لـ p2 ينبغي أن يرجع p1 برغم التماثل التبادلي
        val relatedForP2 = relatedTable.getRelatedPartsForPart(p2.id, partTable.getAllParts())
        assertEquals(1, relatedForP2.size)
        assertEquals(p1.id, relatedForP2.first().relatedPart.id)
    }

    @Test
    fun testCascadeDeletionOnPartRemove() {
        val partTable = PartTable()
        val relatedTable = PartRelatedTable(partTable)

        val p1 = partTable.insertPart(Part(name = "مكثف 10uF"))
        val p2 = partTable.insertPart(Part(name = "مكثف 10uF بديل"))

        relatedTable.insertPartRelated(p1.id, p2.id)

        // مسح p1 ينبغي أن يحذف الارتباط متتابعاً (CASCADE)
        relatedTable.cascadeDeleteForPart(p1.id)

        val relatedForP2AfterDelete = relatedTable.getRelatedPartsForPart(p2.id, partTable.getAllParts())
        assertTrue(relatedForP2AfterDelete.isEmpty())
    }
}
