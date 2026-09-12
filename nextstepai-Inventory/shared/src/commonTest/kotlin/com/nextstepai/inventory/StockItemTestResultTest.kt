package com.nextstepai.inventory

import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockItemTable
import com.nextstepai.inventory.data.StockItemTestResult
import com.nextstepai.inventory.data.db.StockItemTestResultDao
import com.nextstepai.inventory.data.db.StockItemTestResultEntity
import com.nextstepai.inventory.repository.StockRepository
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock

class StockItemTestResultTest {

    @Test
    fun testAddAndRetrieveTestResults() {
        val stockTable = StockItemTable()

        val item = stockTable.insertStockItem(
            StockItem(
                partId = 10L,
                quantity = 1.0,
                serial = "QA-SN-1001"
            )
        )

        val result1 = stockTable.addTestResult(
            StockItemTestResult(
                stockItemId = item.id,
                test = "فحص الجهد الكهربائي",
                result = true,
                value = "5.02V - Pass",
                notes = "القياس مستقر ضمن الحدود المقبولة"
            )
        )

        val result2 = stockTable.addTestResult(
            StockItemTestResult(
                stockItemId = item.id,
                test = "اختبار العزل الكهربائي",
                result = false,
                value = "0.5 MOhm - Fail",
                notes = "تسريب حاد في التيار"
            )
        )

        val results = stockTable.getTestResultsForStockItem(item.id)
        assertEquals(2, results.size)
        assertTrue(results.any { it.id == result1.id && it.result })
        assertTrue(results.any { it.id == result2.id && !it.result })
    }

    @Test
    fun testAddTestResultForNonExistentStockItemFails() {
        val stockTable = StockItemTable()

        assertFailsWith<IllegalArgumentException> {
            stockTable.addTestResult(
                StockItemTestResult(
                    stockItemId = 99999L,
                    test = "فحص عشوائي"
                )
            )
        }
    }

    @Test
    fun testAll11SchemaFieldsAndDaoPersistence() = runBlocking {
        val dao = StockItemTestResultDao()
        val now = Clock.System.now().toEpochMilliseconds()

        val entity = StockItemTestResultEntity(
            uuid = "test-result-uuid-11-fields",
            resultId = 88L,
            stockItemId = 300L,
            templateId = 15L,
            test = "فحص الأبعاد الفنية والهندسية",
            result = true,
            value = "12.05mm x 5.00mm",
            attachment = "attachment_doc_88.pdf",
            notes = "تمت المعايرة باستخدام الميكرومتر الإلكتروني",
            date = "2025-02-15",
            userId = 101L,
            metadata = "{\"tolerance\":\"+/-0.05\"}",
            syncStatus = SyncStatus.PENDING,
            updatedAt = now
        )

        dao.insertOrUpdate(entity)

        val loadedList = dao.getTestResultsForStockItem(300L)
        val loaded = loadedList.firstOrNull { it.uuid == "test-result-uuid-11-fields" }

        assertTrue(loaded != null)
        assertEquals(88L, loaded.resultId)
        assertEquals(300L, loaded.stockItemId)
        assertEquals(15L, loaded.templateId)
        assertEquals("فحص الأبعاد الفنية والهندسية", loaded.test)
        assertTrue(loaded.result)
        assertEquals("12.05mm x 5.00mm", loaded.value)
        assertEquals("attachment_doc_88.pdf", loaded.attachment)
        assertEquals("تمت المعايرة باستخدام الميكرومتر الإلكتروني", loaded.notes)
        assertEquals("2025-02-15", loaded.date)
        assertEquals(101L, loaded.userId)
        assertEquals("{\"tolerance\":\"+/-0.05\"}", loaded.metadata)
    }

    @Test
    fun testRepositoryAddAndRetrieveTestResults() {
        val repository = StockRepository()

        val item = repository.addStockItem(
            StockItem(
                partId = 50L,
                quantity = 1.0,
                serial = "REPO-QA-200"
            )
        )

        val addedResult = repository.addTestResult(
            StockItemTestResult(
                stockItemId = item.id,
                test = "فحص التردد والاستجابة",
                result = true,
                value = "2.4 GHz - OK"
            )
        )

        assertTrue(addedResult.id > 0)
        val loaded = repository.getTestResultsForStockItem(item.id)
        assertEquals(1, loaded.size)
        assertEquals("فحص التردد والاستجابة", loaded.first().test)
    }
}
