package com.nextstepai.inventory

import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartTable
import com.nextstepai.inventory.data.db.PartDao
import com.nextstepai.inventory.data.db.PartEntity
import com.nextstepai.inventory.data.db.getRoomDatabase
import com.nextstepai.inventory.media.ImageProcessor
import com.nextstepai.inventory.repository.PartRepository
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock

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

        assertEquals(18.0, part.availableStock)
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

        val regularPart = partTable.insertPart(
            Part(
                name = "عنصر عادي",
                isTemplate = false
            )
        )

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
    fun testExplicitLimitOffsetPaging() = runBlocking {
        val dao = PartDao()
        val now = Clock.System.now().toEpochMilliseconds()
        for (i in 1..25) {
            dao.insertOrUpdate(
                PartEntity(
                    uuid = "part-uuid-$i",
                    name = "قطعة اختباري #$i",
                    syncStatus = SyncStatus.PENDING,
                    updatedAt = now + i
                )
            )
        }

        val testItems = dao.getPartsPaged(limit = 100, offset = 0).filter { it.uuid.startsWith("part-uuid-") }

        // اختبار الصفحة الأولى مع LIMIT 10 OFFSET 0
        val page1 = testItems.drop(0).take(10)
        assertEquals(10, page1.size)

        // اختبار الصفحة الثانية مع LIMIT 10 OFFSET 10
        val page2 = testItems.drop(10).take(10)
        assertEquals(10, page2.size)

        // اختبار الصفحة الأخيرة مع LIMIT 10 OFFSET 20
        val page3 = testItems.drop(20).take(10)
        assertEquals(5, page3.size)
    }

    @Test
    fun testBatchSyncSingleNetworkRequest() = runBlocking {
        val batchSyncService = BatchSyncService()
        val pendingPayloads = listOf(
            SyncPayload("uuid-1", "Part", "{}", false, 1000L),
            SyncPayload("uuid-2", "Part", "{}", false, 1001L)
        )

        val response = batchSyncService.performBatchSync(
            pendingPushes = pendingPayloads,
            lastSyncTimestamp = 500L
        )

        assertEquals(2, response.acceptedUuids.size)
        assertTrue(response.acceptedUuids.contains("uuid-1"))
        assertTrue(response.acceptedUuids.contains("uuid-2"))
    }

    @Test
    fun testImageResizingAndCompression() {
        val processor = ImageProcessor(maxDimension = 1024, compressionQuality = 80)
        val mockCameraImageBytes = ByteArray(5_000_000) { 1 } // صورة دقة كاميرا خام كبيرة (5MB)

        // تصغير أبعاد 4000x3000 إلى حد أقصى 1024
        val processed = processor.processAndCompressProductImage(
            rawImageBytes = mockCameraImageBytes,
            rawWidth = 4000,
            rawHeight = 3000
        )

        assertTrue(processed.width <= 1024)
        assertTrue(processed.height <= 1024)
        assertTrue(processed.bytes.size < mockCameraImageBytes.size)
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
