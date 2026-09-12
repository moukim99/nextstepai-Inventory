package com.nextstepai.inventory

import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockItemAttachment
import com.nextstepai.inventory.data.StockItemTable
import com.nextstepai.inventory.data.db.StockItemAttachmentDao
import com.nextstepai.inventory.data.db.StockItemAttachmentEntity
import com.nextstepai.inventory.repository.StockRepository
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock

class StockItemAttachmentTest {

    @Test
    fun testAddAttachmentRequiresFileOrLink() {
        val stockTable = StockItemTable()

        val item = stockTable.insertStockItem(
            StockItem(
                partId = 10L,
                quantity = 10.0
            )
        )

        // فشل الإضافة في حال كان الملف والرابط فارغين معاً
        assertFailsWith<IllegalArgumentException> {
            stockTable.addStockItemAttachment(
                StockItemAttachment(
                    stockItemId = item.id,
                    attachment = null,
                    link = null,
                    comment = "شهادة فارغة"
                )
            )
        }

        // نجاح الإضافة عند إدخال ملف محلي
        val fileAtt = stockTable.addStockItemAttachment(
            StockItemAttachment(
                stockItemId = item.id,
                attachment = "/docs/coc_certificate_101.pdf"
            )
        )
        assertEquals("coc_certificate_101.pdf", fileAtt.comment)

        // نجاح الإضافة عند إدخال رابط خارجي
        val linkAtt = stockTable.addStockItemAttachment(
            StockItemAttachment(
                stockItemId = item.id,
                link = "https://example.com/docs/invoice.pdf",
                comment = "فاتورة المورد"
            )
        )
        assertEquals("فاتورة المورد", linkAtt.comment)

        val attachments = stockTable.getAttachmentsForStockItem(item.id)
        assertEquals(2, attachments.size)
    }

    @Test
    fun testAll8SchemaFieldsAndDaoPersistence() = runBlocking {
        val dao = StockItemAttachmentDao()
        val now = Clock.System.now().toEpochMilliseconds()

        val entity = StockItemAttachmentEntity(
            uuid = "att-uuid-8-fields",
            attachmentId = 44L,
            stockItemId = 150L,
            attachment = "/docs/test_report_150.pdf",
            link = "https://drive.google.com/file/150",
            comment = "شهادة جودة ومطابقة CoC",
            uploadDate = "2025-02-15",
            userId = 99L,
            metadata = "{\"file_size\":\"2.4MB\"}",
            syncStatus = SyncStatus.PENDING,
            updatedAt = now
        )

        dao.insertOrUpdate(entity)

        val loadedList = dao.getAttachmentsForStockItem(150L)
        val loaded = loadedList.firstOrNull { it.uuid == "att-uuid-8-fields" }

        assertTrue(loaded != null)
        assertEquals(44L, loaded.attachmentId)
        assertEquals(150L, loaded.stockItemId)
        assertEquals("/docs/test_report_150.pdf", loaded.attachment)
        assertEquals("https://drive.google.com/file/150", loaded.link)
        assertEquals("شهادة جودة ومطابقة CoC", loaded.comment)
        assertEquals("2025-02-15", loaded.uploadDate)
        assertEquals(99L, loaded.userId)
        assertEquals("{\"file_size\":\"2.4MB\"}", loaded.metadata)
    }

    @Test
    fun testRepositoryAddAndDeleteAttachment() {
        val repository = StockRepository()

        val item = repository.addStockItem(
            StockItem(
                partId = 12L,
                quantity = 5.0
            )
        )

        val att = repository.addStockItemAttachment(
            StockItemAttachment(
                stockItemId = item.id,
                attachment = "/uploads/shipping_doc.pdf",
                comment = "بوليصة الشحن"
            )
        )

        assertTrue(att.id > 0)
        assertEquals(1, repository.getAttachmentsForStockItem(item.id).size)

        val deleted = repository.deleteStockItemAttachment(att.id)
        assertTrue(deleted)
        assertEquals(0, repository.getAttachmentsForStockItem(item.id).size)
    }
}
