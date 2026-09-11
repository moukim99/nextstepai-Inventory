package com.nextstepai.inventory

import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartNotesTable
import com.nextstepai.inventory.data.PartTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PartNotesTest {

    @Test
    fun testSaveAndEditPartNotesWithAuditTrail() {
        val partTable = PartTable()
        val notesTable = PartNotesTable(partTable)

        val part = partTable.insertPart(Part(name = "لوحة دوائر مطبوعة الرئيسية"))

        // 1. إنشاء ملاحظة جديدة أولية
        val initialNote = notesTable.saveOrUpdateNotes(
            partId = part.id,
            newNotes = "الملاحظة الأولى: فحص الأبعاد الهندسية قبل التجميع.",
            userId = 1L,
            currentTimestamp = "2025-02-15 10:00:00"
        )

        assertEquals("الملاحظة الأولى: فحص الأبعاد الهندسية قبل التجميع.", initialNote.notes)
        assertEquals("2025-02-15 10:00:00", initialNote.updatedAt)
        assertEquals(1L, initialNote.userId)

        // 2. تحديث الملاحظة القائمة ببيانات جديدة ومستخدم آخر
        val updatedNote = notesTable.saveOrUpdateNotes(
            partId = part.id,
            newNotes = "تم التحديث: إضافة تعليمات التغليف ضد الشحنات الساكنة ESD.",
            userId = 2L,
            currentTimestamp = "2025-02-15 15:30:00"
        )

        assertEquals("تم التحديث: إضافة تعليمات التغليف ضد الشحنات الساكنة ESD.", updatedNote.notes)
        assertEquals("2025-02-15 15:30:00", updatedNote.updatedAt)
        assertEquals(2L, updatedNote.userId)
    }

    @Test
    fun testGetNotesForPartAndCascadeDelete() {
        val partTable = PartTable()
        val notesTable = PartNotesTable(partTable)

        val part1 = partTable.insertPart(Part(name = "متحكم ESP32 A"))
        val part2 = partTable.insertPart(Part(name = "متحكم ESP32 B"))

        notesTable.saveOrUpdateNotes(partId = part1.id, newNotes = "ملاحظات قطعة 1")
        notesTable.saveOrUpdateNotes(partId = part2.id, newNotes = "ملاحظات قطعة 2")

        val note1 = notesTable.getNotesForPart(part1.id)
        assertNotNull(note1)
        assertEquals("ملاحظات قطعة 1", note1.notes)

        // الحذف المتتابع CASCADE لـ part1
        notesTable.cascadeDeleteForPart(part1.id)

        val note1AfterDelete = notesTable.getNotesForPart(part1.id)
        assertNull(note1AfterDelete)

        // التأكد من عدم تأثر part2
        val note2 = notesTable.getNotesForPart(part2.id)
        assertNotNull(note2)
        assertEquals("ملاحظات قطعة 2", note2.notes)
    }
}
