package com.nextstepai.inventory

import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockItemTable
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.db.StockLocationDao
import com.nextstepai.inventory.data.db.StockLocationEntity
import com.nextstepai.inventory.repository.StockRepository
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock

class StockLocationTest {

    @Test
    fun testTreeMetricsAndHierarchy() {
        val stockTable = StockItemTable()

        val root = stockTable.insertLocation(
            StockLocation(
                name = "المستودع الرئيسي",
                description = "مستودع الخامات والمكونات",
                structural = true
            )
        )
        assertEquals(0, root.level)
        assertTrue(root.structural)

        val shelf = stockTable.insertLocation(
            StockLocation(
                name = "الرف A1",
                description = "رف المكونات الإلكترونية",
                parentId = root.id,
                structural = false
            )
        )
        assertEquals(1, shelf.level)
        assertEquals(root.id, shelf.parentId)
    }

    @Test
    fun testDuplicateNameUnderSameParentFails() {
        val stockTable = StockItemTable()

        val root = stockTable.insertLocation(name = "مستودع ب")

        stockTable.insertLocation(
            StockLocation(
                name = "ممر 1",
                parentId = root.id
            )
        )

        assertFailsWith<IllegalArgumentException> {
            stockTable.insertLocation(
                StockLocation(
                    name = "ممر 1",
                    parentId = root.id
                )
            )
        }
    }

    @Test
    fun testStructuralLocationPreventsDirectStockItemAssignment() {
        val stockTable = StockItemTable()

        val structLoc = stockTable.insertLocation(
            StockLocation(
                name = "مستودع هيكلي فقط",
                structural = true
            )
        )

        assertFailsWith<IllegalArgumentException> {
            stockTable.insertStockItem(
                StockItem(
                    partId = 1L,
                    locationId = structLoc.id,
                    quantity = 10.0
                )
            )
        }
    }

    @Test
    fun testAll15SchemaFieldsAndDaoPersistence() = runBlocking {
        val dao = StockLocationDao()
        val now = Clock.System.now().toEpochMilliseconds()

        val entity = StockLocationEntity(
            uuid = "loc-uuid-15-fields",
            locationId = 50L,
            name = "صندوق التخزين B12",
            description = "صندوق شفاف غير موصل للكهرباء",
            parentId = 10L,
            structural = false,
            external = true,
            locationTypeId = 3L,
            ownerId = 7L,
            icon = "box",
            customIcon = "custom_icon_b12.png",
            level = 2,
            lft = 4,
            rght = 5,
            treeId = 1,
            metadata = "{\"temp_controlled\":true}",
            syncStatus = SyncStatus.PENDING,
            updatedAt = now
        )

        dao.insertOrUpdate(entity)

        val allLocations = dao.getAllLocations()
        val loaded = allLocations.firstOrNull { it.uuid == "loc-uuid-15-fields" }

        assertTrue(loaded != null)
        assertEquals(50L, loaded.locationId)
        assertEquals("صندوق التخزين B12", loaded.name)
        assertEquals("صندوق شفاف غير موصل للكهرباء", loaded.description)
        assertEquals(10L, loaded.parentId)
        assertEquals(false, loaded.structural)
        assertEquals(true, loaded.external)
        assertEquals(3L, loaded.locationTypeId)
        assertEquals(7L, loaded.ownerId)
        assertEquals("box", loaded.icon)
        assertEquals("custom_icon_b12.png", loaded.customIcon)
        assertEquals(2, loaded.level)
        assertEquals(4, loaded.lft)
        assertEquals(5, loaded.rght)
        assertEquals(1, loaded.treeId)
    }

    @Test
    fun testRepositoryAddAndRetrieveLocations() {
        val repository = StockRepository()

        val newLoc = repository.addLocation(
            StockLocation(
                name = "مستودع المنتجات الصادرة",
                description = "منطقة الشحن والاستلام الخارجي",
                external = true
            )
        )

        assertTrue(newLoc.id > 0)
        val retrieved = repository.getLocations().find { it.id == newLoc.id }
        assertEquals("مستودع المنتجات الصادرة", retrieved?.name)
        assertTrue(retrieved?.external == true)
    }
}
