package com.nextstepai.inventory

import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockItemTable
import com.nextstepai.inventory.data.StockTrackingType
import com.nextstepai.inventory.data.db.StockItemTrackingDao
import com.nextstepai.inventory.data.db.StockItemTrackingEntity
import com.nextstepai.inventory.repository.StockRepository
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

class StockItemTrackingTest {

    @Test
    fun testAutomaticTrackingLogGenerationOnActions() {
        val stockTable = StockItemTable()

        // 1. إضافة قطعة جديدة تفرض توليد حركة CREATED
        val item = stockTable.insertStockItem(
            StockItem(
                partId = 15L,
                quantity = 100.0,
                notes = "إضافة شحنة توريد"
            )
        )

        var logs = stockTable.getTrackingForStockItem(item.id)
        assertEquals(1, logs.size)
        assertEquals(StockTrackingType.CREATED, logs.first().trackingType)
        assertEquals("إنشاء وحدة مخزنية جديدة", logs.first().label)

        // 2. تجزئة الكمية تفرض توليد حركة SPLIT للأب و CREATED للفرع
        val child = stockTable.splitStockItem(item.id, 20.0)

        logs = stockTable.getTrackingForStockItem(item.id)
        assertEquals(2, logs.size)
        assertEquals(StockTrackingType.SPLIT, logs.last().trackingType)

        val childLogs = stockTable.getTrackingForStockItem(child.id)
        assertEquals(1, childLogs.size)
        assertEquals(StockTrackingType.CREATED, childLogs.first().trackingType)

        // 3. الجرد الميداني يفرض توليد حركة COUNT
        stockTable.performStocktake(item.id, userId = 55L, stocktakeDate = "2025-02-15")

        logs = stockTable.getTrackingForStockItem(item.id)
        assertEquals(3, logs.size)
        assertEquals(StockTrackingType.COUNT, logs.last().trackingType)
        assertEquals(55L, logs.last().userId)
    }

    @Test
    fun testAll8SchemaFieldsAndDaoPersistence() = runBlocking {
        val dao = StockItemTrackingDao()
        val now = Clock.System.now().toEpochMilliseconds()

        val entity = StockItemTrackingEntity(
            uuid = "track-uuid-8-fields",
            trackingId = 99L,
            stockItemId = 200L,
            date = "2025-02-15 14:30:00",
            trackingTypeCode = StockTrackingType.MOVE.code,
            userId = 12L,
            label = "نقل من المستودع الرئيسي إلى الرف A1",
            notes = "تم النقل بواسطة رافعة التخزين",
            deltas = "{\"location\":[1,2]}",
            syncStatus = SyncStatus.PENDING,
            updatedAt = now
        )

        dao.insertOrUpdate(entity)

        val loadedList = dao.getTrackingForStockItem(200L)
        val loaded = loadedList.firstOrNull { it.uuid == "track-uuid-8-fields" }

        assertTrue(loaded != null)
        assertEquals(99L, loaded.trackingId)
        assertEquals(200L, loaded.stockItemId)
        assertEquals("2025-02-15 14:30:00", loaded.date)
        assertEquals(StockTrackingType.MOVE.code, loaded.trackingTypeCode)
        assertEquals(12L, loaded.userId)
        assertEquals("نقل من المستودع الرئيسي إلى الرف A1", loaded.label)
        assertEquals("تم النقل بواسطة رافعة التخزين", loaded.notes)
        assertEquals("{\"location\":[1,2]}", loaded.deltas)
    }

    @Test
    fun testRepositoryTrackingLogsRetrieval() {
        val repository = StockRepository()

        val item = repository.addStockItem(
            StockItem(
                partId = 30L,
                quantity = 10.0
            )
        )

        val logs = repository.getTrackingForStockItem(item.id)
        assertTrue(logs.isNotEmpty())
        assertEquals(item.id, logs.first().stockItemId)
    }
}
