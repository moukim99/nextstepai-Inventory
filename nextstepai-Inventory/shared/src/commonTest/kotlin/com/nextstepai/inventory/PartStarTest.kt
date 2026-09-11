package com.nextstepai.inventory

import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartStarTable
import com.nextstepai.inventory.data.PartTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PartStarTest {

    @Test
    fun testPartStarToggleActionAndUniqueConstraint() {
        val partTable = PartTable()
        val starTable = PartStarTable(partTable)

        val part = partTable.insertPart(Part(name = "قطعة مميزة للمتابعة"))

        // 1. التمييز لأول مرة ينبغي أن يعود بـ true
        val isStarred1 = starTable.toggleStarForPart(part.id, userId = 1L)
        assertTrue(isStarred1)
        assertTrue(starTable.isPartStarred(part.id, userId = 1L))

        // 2. النقر مجدداً ينبغي أن يلغي التفضيل ويعود بـ false (Toggle Behavior)
        val isStarred2 = starTable.toggleStarForPart(part.id, userId = 1L)
        assertFalse(isStarred2)
        assertFalse(starTable.isPartStarred(part.id, userId = 1L))
    }

    @Test
    fun testStarredPartIdsRetrievalAndCascadeDelete() {
        val partTable = PartTable()
        val starTable = PartStarTable(partTable)

        val p1 = partTable.insertPart(Part(name = "قطعة 1"))
        val p2 = partTable.insertPart(Part(name = "قطعة 2"))

        starTable.toggleStarForPart(p1.id, userId = 1L)
        starTable.toggleStarForPart(p2.id, userId = 1L)

        val starredIds = starTable.getStarredPartIdsForUser(1L)
        assertTrue(starredIds.contains(p1.id))
        assertTrue(starredIds.contains(p2.id))

        // الحذف المتتابع CASCADE عند مسح القطعة الأصيلة
        starTable.cascadeDeleteForPart(p1.id)
        assertFalse(starTable.isPartStarred(p1.id, userId = 1L))
        assertTrue(starTable.isPartStarred(p2.id, userId = 1L))
    }
}
