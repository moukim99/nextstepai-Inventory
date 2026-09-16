package com.nextstepai.inventory

import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.repository.StockRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StockLocationTypeTest {

    @Test
    fun testPredefinedLocationTypesAssignment() {
        val locWarehouse = StockLocation(name = "مستودع الخامات", locationType = "WAREHOUSE")
        assertEquals("WAREHOUSE", locWarehouse.locationType)

        val locShelf = StockLocation(name = "رف A1", locationType = "SHELF")
        assertEquals("SHELF", locShelf.locationType)

        val locBin = StockLocation(name = "درج B12", locationType = "BIN")
        assertEquals("BIN", locBin.locationType)
    }

    @Test
    fun testRepositoryAddAndRetrieveLocationWithType() {
        val repository = StockRepository()

        val newLoc = repository.addLocation(
            StockLocation(
                name = "قسم التجميع الإلكتروني",
                description = "منطقة تجميع الدوائر المطبوعة",
                locationType = "ZONE"
            )
        )

        assertTrue(newLoc.id > 0)
        val retrieved = repository.getLocations().find { it.id == newLoc.id }
        assertEquals("قسم التجميع الإلكتروني", retrieved?.name)
        assertEquals("ZONE", retrieved?.locationType)
    }
}
