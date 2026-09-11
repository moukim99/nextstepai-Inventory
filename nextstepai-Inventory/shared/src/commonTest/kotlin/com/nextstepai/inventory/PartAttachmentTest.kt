package com.nextstepai.inventory

import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartAttachment
import com.nextstepai.inventory.data.PartAttachmentTable
import com.nextstepai.inventory.data.PartTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PartAttachmentTest {

    @Test
    fun testSourceCheckConstraint() {
        val partTable = PartTable()
        val attTable = PartAttachmentTable(partTable)

        val part = partTable.insertPart(Part(name = "لوحة دوائر مطبوعة"))

        // محاولة إنشاء مرفق بدون ملف وبدون رابط ينبغي أن تطلق استثناء
        assertFailsWith<IllegalArgumentException> {
            attTable.insertAttachment(
                PartAttachment(
                    partId = part.id,
                    attachment = null,
                    link = null,
                    comment = "مرفق غير صالح"
                )
            )
        }
    }

    @Test
    fun testAutoCommentGenerationAndValidInsert() {
        val partTable = PartTable()
        val attTable = PartAttachmentTable(partTable)

        val part = partTable.insertPart(Part(name = "متحكم ESP32"))

        // 1. إضافة ملف بدون ملاحظة يدوية -> ينبغي أن يستخرج اسم الملف افتراضياً
        val attFile = attTable.insertAttachment(
            PartAttachment(
                partId = part.id,
                attachment = "/uploads/datasheet_esp32.pdf"
            )
        )
        assertEquals("datasheet_esp32.pdf", attFile.comment)

        // 2. إضافة رابط بدون ملاحظة يدوية -> ينبغي أن يستخدم الرابط افتراضياً
        val attLink = attTable.insertAttachment(
            PartAttachment(
                partId = part.id,
                link = "https://www.espressif.com/spec"
            )
        )
        assertEquals("https://www.espressif.com/spec", attLink.comment)
    }

    @Test
    fun testGetAttachmentsForPartAndCascadeDelete() {
        val partTable = PartTable()
        val attTable = PartAttachmentTable(partTable)

        val part1 = partTable.insertPart(Part(name = "قطعة أصلية A"))
        val part2 = partTable.insertPart(Part(name = "قطعة أصلية B"))

        attTable.insertAttachment(PartAttachment(partId = part1.id, attachment = "file1.pdf"))
        attTable.insertAttachment(PartAttachment(partId = part1.id, link = "http://link1.com"))
        attTable.insertAttachment(PartAttachment(partId = part2.id, attachment = "file2.pdf"))

        val attsForPart1 = attTable.getAttachmentsForPart(part1.id)
        assertEquals(2, attsForPart1.size)

        // الحذف المتتابع CASCADE عند مسح part1
        attTable.cascadeDeleteForPart(part1.id)

        val attsForPart1AfterDelete = attTable.getAttachmentsForPart(part1.id)
        assertTrue(attsForPart1AfterDelete.isEmpty())

        // التأكد من عدم تأثر part2
        assertEquals(1, attTable.getAttachmentsForPart(part2.id).size)
    }
}
