package com.nextstepai.inventory

import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockItemTable
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.contactPerson
import com.nextstepai.inventory.data.contactPhone
import com.nextstepai.inventory.data.withContactInfo
import com.nextstepai.inventory.data.isPrimary
import com.nextstepai.inventory.data.withPrimary
import com.nextstepai.inventory.data.withCapacityUnit
import com.nextstepai.inventory.data.CompletenessTone
import com.nextstepai.inventory.data.calculateCompleteness
import com.nextstepai.inventory.data.IntermediateNodeSpec
import com.nextstepai.inventory.data.getFullHierarchyPath
import com.nextstepai.inventory.ui.StockViewModel
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
            locationType = "SHELF",
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
        assertEquals("SHELF", loaded.locationType)
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

    @Test
    fun testExternalLocationContactInfoMetadata() {
        val loc = StockLocation(
            name = "مستودع مقاول خارجي",
            external = true,
            address = "المنطقة الصناعية - وهران"
        ).withContactInfo("محمد العربي", "0550123456")

        assertEquals("محمد العربي", loc.contactPerson)
        assertEquals("0550123456", loc.contactPhone)

        val updatedLoc = loc.withContactInfo("علي محمود", "0660112233")
        assertEquals("علي محمود", updatedLoc.contactPerson)
        assertEquals("0660112233", updatedLoc.contactPhone)
    }

    @Test
    fun testFullHierarchyPathMultiTier() {
        val stockTable = StockItemTable()

        val root = stockTable.insertLocation(
            StockLocation(name = "المستودع الرئيسي", structural = true, locationType = "WAREHOUSE")
        )
        val zone = stockTable.insertLocation(
            StockLocation(name = "المنطقة A", parentId = root.id, structural = true, locationType = "ZONE")
        )
        val rack = stockTable.insertLocation(
            StockLocation(name = "الممر 03", parentId = zone.id, structural = true, locationType = "RACK")
        )
        val shelf = stockTable.insertLocation(
            StockLocation(name = "الرف B1", parentId = rack.id, structural = false, locationType = "SHELF")
        )

        val allLocs = stockTable.getAllLocations()

        assertEquals("المستودع الرئيسي", root.getFullHierarchyPath(allLocs))
        assertEquals("المستودع الرئيسي > المنطقة A", zone.getFullHierarchyPath(allLocs))
        assertEquals("المستودع الرئيسي > المنطقة A > الممر 03", rack.getFullHierarchyPath(allLocs))
        assertEquals("المستودع الرئيسي > المنطقة A > الممر 03 > الرف B1", shelf.getFullHierarchyPath(allLocs))
    }

    @Test
    fun testQuickCreateParentLocationLogic() {
        val viewModel = StockViewModel()

        val root = viewModel.quickCreateParentLocation(
            name = "المستودع المركزي",
            locationType = "WAREHOUSE",
            parentId = null
        )
        assertTrue(root.id > 0)
        assertTrue(root.structural)
        assertEquals(0, root.level)

        val zone = viewModel.quickCreateParentLocation(
            name = "المنطقة الشرقية",
            locationType = "ZONE",
            parentId = root.id
        )
        assertTrue(zone.id > 0)
        assertTrue(zone.structural)
        assertEquals(root.id, zone.parentId)
        assertEquals(1, zone.level)

        val updatedLocations = viewModel.uiState.value.locations
        val foundZone = updatedLocations.find { it.id == zone.id }
        assertTrue(foundZone != null)
        assertEquals("المستودع المركزي > المنطقة الشرقية", foundZone.getFullHierarchyPath(updatedLocations))
    }

    @Test
    fun testPrimaryLocationMetadataExtension() {
        val loc = StockLocation(name = "موقع تجريبي", locationType = "SITE")
        assertEquals(false, loc.isPrimary)

        val primaryLoc = loc.withPrimary(true)
        assertEquals(true, primaryLoc.isPrimary)
        assertTrue(primaryLoc.metadata.contains("\"isPrimary\":true"))

        val unprimaryLoc = primaryLoc.withPrimary(false)
        assertEquals(false, unprimaryLoc.isPrimary)
        assertTrue(unprimaryLoc.metadata.contains("\"isPrimary\":false"))
    }

    @Test
    fun testLocationDataCompletenessScore() {
        val basicLoc = StockLocation(name = "موقع تجريبي بدون عنوان أو مشرف")
        assertEquals(0, basicLoc.calculateCompleteness().percentage)
        assertEquals(CompletenessTone.RED, basicLoc.calculateCompleteness().colorTone)

        val linkedLoc = StockLocation(
            name = "مرفق بالهيكل",
            parentId = 10L,
            address = "الجزائر العاصمة"
        )
        assertEquals(60, linkedLoc.calculateCompleteness().percentage)
        assertEquals(CompletenessTone.ORANGE, linkedLoc.calculateCompleteness().colorTone)

        val supervisedLoc = linkedLoc.copy(ownerId = 5L)
        assertEquals(70, supervisedLoc.calculateCompleteness().percentage)
        assertEquals(CompletenessTone.ORANGE, supervisedLoc.calculateCompleteness().colorTone)

        val fullyConfiguredLoc = supervisedLoc
            .withCapacityUnit("كغ")
            .copy(customCapacity = 500.0)
        assertEquals(100, fullyConfiguredLoc.calculateCompleteness().percentage)
        assertEquals(CompletenessTone.GREEN, fullyConfiguredLoc.calculateCompleteness().colorTone)
    }

    @Test
    fun testDaoCountAndFindPrimaryQueries() = runBlocking {
        val dao = StockLocationDao()
        val now = Clock.System.now().toEpochMilliseconds()

        val siteEntity = StockLocationEntity(
            uuid = "site-primary-test-1",
            locationId = 101L,
            name = "الموقع الأخير الرئيسي",
            locationType = "SITE",
            metadata = "{\"isPrimary\":true}",
            updatedAt = now
        )
        dao.insertOrUpdate(siteEntity)

        val whEntity = StockLocationEntity(
            uuid = "wh-primary-test-1",
            locationId = 102L,
            name = "المستودع الأساسي الرئيسي",
            locationType = "WAREHOUSE",
            parentUuid = "site-primary-test-1",
            parentId = 101L,
            metadata = "{\"isPrimary\":true}",
            updatedAt = now
        )
        dao.insertOrUpdate(whEntity)

        val siteCount = dao.getRootSitesCount()
        assertTrue(siteCount > 0)

        val whCount = dao.getWarehouseCount()
        assertTrue(whCount > 0)

        val primarySite = dao.findPrimaryLocation("SITE")
        assertTrue(primarySite != null)
        assertEquals("site-primary-test-1", primarySite.uuid)

        val primaryWh = dao.findPrimaryLocation("WAREHOUSE")
        assertTrue(primaryWh != null)
        assertEquals("wh-primary-test-1", primaryWh.uuid)
    }

    @Test
    fun testFirstLocationAutoPrimaryLogic() {
        val viewModel = StockViewModel()
        val loc = viewModel.addLocation(
            name = "أول موقع أساسي في النظام",
            locationType = "SITE",
            isPrimary = true
        )
        assertTrue(loc.isPrimary)
    }

    @Test
    fun testSoftDeleteGuardReassignsPrimaryToOldestRemainingLocation() {
        val repo = StockRepository()

        val site1 = repo.addLocation(
            StockLocation(name = "الموقع الأول الأساسي", locationType = "SITE").withPrimary(true)
        )
        val site2 = repo.addLocation(
            StockLocation(name = "الموقع الثاني الفرعي", locationType = "SITE").withPrimary(false)
        )

        assertTrue(site1.isPrimary)

        repo.deleteLocation(site1.id)

        val updatedSite2 = repo.getLocations().find { it.id == site2.id }
        assertTrue(updatedSite2 != null)
        assertTrue(updatedSite2.isPrimary, "يجب أن يصبح الموقع الثاني النشط هو الأساسي تلقائياً لمنع حالة Zero Primary State")
    }

    @Test
    fun testSyncConflictResolutionByTimestamp() {
        val repo = StockRepository()

        val site1 = repo.addLocation(
            StockLocation(name = "موقع 1 قديم", locationType = "SITE").withPrimary(true)
        )
        val site2 = repo.addLocation(
            StockLocation(name = "موقع 2 أحدث", locationType = "SITE").withPrimary(true)
        )

        val allLocs = repo.getLocations()
        val refreshed1 = allLocs.find { it.id == site1.id }
        val refreshed2 = allLocs.find { it.id == site2.id }

        assertTrue(refreshed2?.isPrimary == true, "الموقع الأحدث يحافظ على صفة الأساسي")
        assertTrue(refreshed1?.isPrimary == false, "الموقع الأقدم يُسحب منه صفة الأساسي لتجنب التعيين المزدوج")
    }

    @Test
    fun testAtomicCreationWithIntermediateLayers() {
        val repo = StockRepository()

        val warehouse = repo.addLocation(
            StockLocation(name = "مستودع المكونات الرئيسي", locationType = "WAREHOUSE")
        )

        val targetShelf = StockLocation(
            name = "الرف B5",
            locationType = "SHELF",
            parentId = warehouse.id,
            customCapacity = 500.0
        )

        val intermediates = listOf(
            IntermediateNodeSpec(locationType = "ZONE", name = "المنطقة الشرقية"),
            IntermediateNodeSpec(locationType = "AISLE", name = "الممر 02")
        )

        val createdShelf = repo.addLocationWithIntermediates(targetShelf, intermediates)

        assertTrue(createdShelf.id > 0)

        val allLocs = repo.getLocations()
        val createdAisle = allLocs.find { it.id == createdShelf.parentId }
        assertTrue(createdAisle != null)
        assertEquals("الممر 02", createdAisle?.name)
        assertEquals("AISLE", createdAisle?.locationType)
        assertTrue(createdAisle?.structural == true)

        val createdZone = allLocs.find { it.id == createdAisle?.parentId }
        assertTrue(createdZone != null)
        assertEquals("المنطقة الشرقية", createdZone?.name)
        assertEquals("ZONE", createdZone?.locationType)
        assertTrue(createdZone?.structural == true)
        assertEquals(warehouse.id, createdZone?.parentId)

        assertEquals("مستودع المكونات الرئيسي > المنطقة الشرقية > الممر 02 > الرف B5", createdShelf.getFullHierarchyPath(allLocs))
    }

    @Test
    fun testDuplicateLocationDetectionAndAutoCollisionPrevention() {
        val repo = StockRepository()

        val wh = repo.addLocation(StockLocation(name = "مستودع الاختبار المركزي", locationType = "WAREHOUSE"))

        repo.addLocation(StockLocation(name = "الرف A1", parentId = wh.id, address = "المستودع المركزي > الرف A1"))

        val isDupName = repo.isLocationDuplicateUnderSameParent("الرف A1", wh.id)
        assertTrue(isDupName)

        val isNotDupName = repo.isLocationDuplicateUnderSameParent("الرف A2", wh.id)
        assertTrue(!isNotDupName)

        val uniqueName = repo.generateUniqueLocationName("الرف A1", wh.id)
        assertEquals("الرف A1-01", uniqueName)
    }

    @Test
    fun testBulkSequentialGenerationWithCollisionSkip() {
        val repo = StockRepository()
        val viewModel = StockViewModel(repo)

        val wh = repo.addLocation(StockLocation(name = "مستودع التسلسل", locationType = "WAREHOUSE"))

        // إضافة مسبقة لـ "رف 12" للتحقق من التخطي
        repo.addLocation(StockLocation(name = "رف 12", parentId = wh.id))

        // إدخال "رف 07" مع طلب 10 مواقع
        val startName = "رف 07"
        val count = 10

        val match = Regex("""^(.*?)(?:[\s\-_]*)(\d+)$""").find(startName.trim())
        val basePrefix = if (match != null) match.groupValues[1].trim() else startName.trim()
        val rawStartNum = match?.groupValues?.get(2)?.toIntOrNull() ?: 1
        val rawDigitsLength = match?.groupValues?.get(2)?.length ?: 2
        val finalPadding = if (rawDigitsLength < 2 && (rawStartNum + count - 1) >= 10) 2 else rawDigitsLength
        val separator = if (basePrefix.endsWith("-") || basePrefix.endsWith("_")) "" else " "

        val generatedNames = mutableListOf<String>()
        var currentNum = rawStartNum

        while (generatedNames.size < count) {
            val formattedNum = currentNum.toString().padStart(finalPadding, '0')
            val candidateName = if (basePrefix.isBlank()) formattedNum else "$basePrefix$separator$formattedNum"

            val isDuplicate = repo.isLocationDuplicateUnderSameParent(
                name = candidateName,
                parentId = wh.id
            )

            if (!isDuplicate) {
                generatedNames.add(candidateName)
            }
            currentNum++
        }

        assertEquals(10, generatedNames.size)
        assertEquals("رف 07", generatedNames.first())
        assertEquals("رف 17", generatedNames.last())
        assertTrue(!generatedNames.contains("رف 12"), "يجب أن يتم تخطي رف 12 الموجود مسبقاً")

        val insertedList = viewModel.addSequentialLocations(
            generatedNames = generatedNames,
            parentId = wh.id,
            locationType = "SHELF",
            baseAddress = "مستودع التسلسل > رف 07"
        )

        assertEquals(10, insertedList.size)
        assertTrue(repo.getLocations().any { it.name == "رف 17" })

        // Cleanup test data to maintain test isolation across shared in-memory database
        insertedList.forEach { repo.deleteLocation(it.id) }
        repo.getLocations().filter { it.parentId == wh.id }.forEach { repo.deleteLocation(it.id) }
        repo.deleteLocation(wh.id)
    }
}


