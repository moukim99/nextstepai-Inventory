package com.nextstepai.inventory

import com.nextstepai.inventory.data.StockItemTable
import com.nextstepai.inventory.data.StockLocationType
import com.nextstepai.inventory.data.db.StockLocationTypeDao
import com.nextstepai.inventory.data.db.StockLocationTypeEntity
import com.nextstepai.inventory.repository.StockRepository
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock

class StockLocationTypeTest {

    @Test
    fun testEffectiveIconPrecedence() {
        val defaultType = StockLocationType(
            name = "رف",
            icon = "shelf",
            customIcon = ""
        )
        assertEquals("shelf", defaultType.effectiveIcon)

        val customType = StockLocationType(
            name = "غرفة نظيفة",
            icon = "cleanroom",
            customIcon = "custom_cleanroom_icon.png"
        )
        assertEquals("custom_cleanroom_icon.png", customType.effectiveIcon)
    }

    @Test
    fun testUniqueNameConstraint() {
        val stockTable = StockItemTable()

        stockTable.insertLocationType(
            StockLocationType(
                name = "حاوية شحن"
            )
        )

        assertFailsWith<IllegalArgumentException> {
            stockTable.insertLocationType(
                StockLocationType(
                    name = "حاوية شحن"
                )
            )
        }
    }

    @Test
    fun testAll6SchemaFieldsAndDaoPersistence() = runBlocking {
        val dao = StockLocationTypeDao()
        val now = Clock.System.now().toEpochMilliseconds()

        val entity = StockLocationTypeEntity(
            uuid = "type-uuid-6-fields",
            typeId = 15L,
            name = "منطقة تبريد وتجميد",
            description = "مستودعات مخصصة للمواد الحساسة للحرارة",
            icon = "freezer",
            customIcon = "custom_freezer_icon.png",
            metadata = "{\"min_temp\":-20}",
            syncStatus = SyncStatus.PENDING,
            updatedAt = now
        )

        dao.insertOrUpdate(entity)

        val allTypes = dao.getAllLocationTypes()
        val loaded = allTypes.firstOrNull { it.uuid == "type-uuid-6-fields" }

        assertTrue(loaded != null)
        assertEquals(15L, loaded.typeId)
        assertEquals("منطقة تبريد وتجميد", loaded.name)
        assertEquals("مستودعات مخصصة للمواد الحساسة للحرارة", loaded.description)
        assertEquals("freezer", loaded.icon)
        assertEquals("custom_freezer_icon.png", loaded.customIcon)
        assertEquals("{\"min_temp\":-20}", loaded.metadata)
    }

    @Test
    fun testRepositoryAddAndRetrieveLocationTypes() {
        val repository = StockRepository()

        val newType = repository.addLocationType(
            StockLocationType(
                name = "شاحنة توصيل خارجية",
                description = "مواقع نقل وتوزيع متنقلة",
                icon = "truck"
            )
        )

        assertTrue(newType.id > 0)
        val retrieved = repository.getLocationTypes().find { it.id == newType.id }
        assertEquals("شاحنة توصيل خارجية", retrieved?.name)
        assertEquals("truck", retrieved?.icon)
    }
}
