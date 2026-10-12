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
import com.nextstepai.inventory.data.db.SqliteDatabaseManager
import com.nextstepai.inventory.repository.StockLocationRepository
import com.nextstepai.inventory.repository.StockRepository
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
            name = "صندوق التخزين B12",
            description = "صندوق شفاف غير موصل للكهرباء",
            parentUuid = "loc-10",
            structural = false,
            external = true,
            locationType = "SHELF",
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
        assertEquals("loc-uuid-15-fields", loaded.uuid)
        assertEquals("صندوق التخزين B12", loaded.name)
        assertEquals("صندوق شفاف غير موصل للكهرباء", loaded.description)
        assertEquals("loc-10", loaded.parentUuid)
        assertEquals(false, loaded.structural)
        assertEquals(true, loaded.external)
        assertEquals("SHELF", loaded.locationType)
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
    fun testDaoCountAndFindPrimaryQueries() {
        runBlocking {
            val dao = StockLocationDao()
        val now = Clock.System.now().toEpochMilliseconds()

        val siteEntity = StockLocationEntity(
            uuid = "site-primary-test-1",
            name = "الموقع الأخير الرئيسي",
            locationType = "SITE",
            metadata = "{\"isPrimary\":true}",
            updatedAt = now
        )
        dao.insertOrUpdate(siteEntity)

        val whEntity = StockLocationEntity(
            uuid = "wh-primary-test-1",
            name = "المستودع الأساسي الرئيسي",
            locationType = "WAREHOUSE",
            parentUuid = "site-primary-test-1",
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

        dao.softDeleteLocation("site-primary-test-1")
        dao.softDeleteLocation("wh-primary-test-1")
        }
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
        repo.getLocations().filter { it.locationType.equals("SITE", ignoreCase = true) }.forEach {
            runCatching { repo.deleteLocation(it.id) }
        }

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

        runCatching { repo.deleteLocation(site2.id) }
    }

    @Test
    fun testSyncConflictResolutionByTimestamp() {
        val repo = StockRepository()
        repo.getLocations().filter { it.locationType.equals("SITE", ignoreCase = true) }.forEach {
            runCatching { repo.deleteLocation(it.id) }
        }

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

        runCatching { repo.deleteLocation(site1.id) }
        runCatching { repo.deleteLocation(site2.id) }
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

    @Test
    fun testLocationDaoPrioritizedIdentifierResolution() {
        val dao = StockLocationDao()

        // 1. فحص المطابقة المباشرة بالـ UUID الصريح
        val loc002 = dao.getLocationByUuid("loc-002")
        kotlin.test.assertNotNull(loc002)
        assertEquals("loc-002", loc002.uuid)
        assertEquals(false, loc002.structural)

        // 2. فحص المطابقة بالصيغة المختصرة غير المبطنة
        val locShort = dao.getLocationByUuid("loc-2")
        kotlin.test.assertNotNull(locShort)
        assertEquals("loc-002", locShort.uuid)

        // 3. فحص المطابقة بالمعرف الرقمي id
        val locById = dao.getLocationById(2L)
        kotlin.test.assertNotNull(locById)
        assertEquals("loc-002", locById.uuid)

        // 4. فحص عدم وجود تداخل أو نتائج خاطئة لمعرفات غير موجودة
        val nonExistentByUuid = dao.getLocationByUuid("loc-non-existent-999")
        kotlin.test.assertNull(nonExistentByUuid)

        val nonExistentById = dao.getLocationById(999999L)
        kotlin.test.assertNull(nonExistentById)
    }

    @Test
    fun testLocationDaoNumericIdPriorityOverLocationIdConflict() {
        val dao = StockLocationDao()
        val conn = SqliteDatabaseManager.getConnection()

        val targetId = 7777L
        val locWithIdUuid = "loc-target-id-7777"
        val locWithLocIdUuid = "loc-target-locid-7777"

        fun cleanup() {
            conn.prepare("DELETE FROM stock_locations WHERE uuid IN (?, ?)").use { stmt ->
                stmt.bindText(1, locWithIdUuid)
                stmt.bindText(2, locWithLocIdUuid)
                stmt.step()
            }
        }

        // تنظيف مسبق لأي بيانات قديمة
        cleanup()

        try {
            // إنشاء سجلين بتعارض متعمد بين عمود id وعمود locationId مباشرة عبر SQL
            conn.prepare("""
                INSERT INTO stock_locations (uuid, id, locationId, name, structural)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()).use { stmt ->
                // السجل 1: id = 7777, locationId = 8888
                stmt.bindText(1, locWithIdUuid)
                stmt.bindLong(2, targetId)
                stmt.bindLong(3, 8888L)
                stmt.bindText(4, "موقع مع id صريح")
                stmt.bindLong(5, 0L)
                stmt.step()
            }

            conn.prepare("""
                INSERT INTO stock_locations (uuid, id, locationId, name, structural)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()).use { stmt ->
                // السجل 2: id = 9999, locationId = 7777
                stmt.bindText(1, locWithLocIdUuid)
                stmt.bindLong(2, 9999L)
                stmt.bindLong(3, targetId)
                stmt.bindText(4, "موقع مع locationId مطابق")
                stmt.bindLong(5, 0L)
                stmt.step()
            }

            // التحقق من أن الاستعلام عن targetId يختار دائماً السجل الذي يملك id = targetId وليس locationId
            val result = dao.getLocationById(targetId)
            kotlin.test.assertNotNull(result)
            assertEquals(locWithIdUuid, result.uuid, "يجب إعطاء الأولوية الصريحة لعمود id على عمود locationId عند التعارض")
            assertEquals(targetId, result.id)
        } finally {
            // تنظيف بعد الاختبار حتمي حتى لو فشل أحد التأكيدات
            cleanup()
        }
    }

    @Test
    fun testUnparseableUuidWithoutStoredNumericIdDoesNotReceiveIdOneOrCollide() {
        val repo = StockLocationRepository()
        val conn = SqliteDatabaseManager.getConnection()

        val unparseableUuid = "unparseable-uuid-no-numeric-id"
        val storedIdUuid = "custom-uuid-with-stored-id"
        val paddedUuid = "loc-078"
        val customStoredId = 8899L
        val paddedNumericId = 78L

        fun cleanup() {
            conn.prepare("DELETE FROM stock_locations WHERE uuid IN (?, ?, ?) OR id IN (?, ?)").use { stmt ->
                stmt.bindText(1, unparseableUuid)
                stmt.bindText(2, storedIdUuid)
                stmt.bindText(3, paddedUuid)
                stmt.bindLong(4, customStoredId)
                stmt.bindLong(5, paddedNumericId)
                stmt.step()
            }
        }

        cleanup()

        try {
            // 1. سجل غير قابل للتحليل ولا يحمل معرفاً رقمياً مخزناً (id = 0)
            conn.prepare("""
                INSERT INTO stock_locations (uuid, id, locationId, name, structural, isDeleted)
                VALUES (?, 0, 0, ?, ?, 0)
            """.trimIndent()).use { stmt ->
                stmt.bindText(1, unparseableUuid)
                stmt.bindText(2, "سجل غير محلول بدون معرف")
                stmt.bindLong(3, 0L)
                stmt.step()
            }

            // 2. سجل يحمل UUID نصي عشوائي لكن يحمل معرفاً رقمياً مخزناً صريحاً
            conn.prepare("""
                INSERT INTO stock_locations (uuid, id, locationId, name, structural, isDeleted)
                VALUES (?, ?, ?, ?, ?, 0)
            """.trimIndent()).use { stmt ->
                stmt.bindText(1, storedIdUuid)
                stmt.bindLong(2, customStoredId)
                stmt.bindLong(3, customStoredId)
                stmt.bindText(4, "سجل بمعرف رقمي مخزن صريح")
                stmt.bindLong(5, 0L)
                stmt.step()
            }

            // 3. سجل يحمل UUID رقمي مبطن دون عمود id
            conn.prepare("""
                INSERT INTO stock_locations (uuid, id, locationId, name, structural, isDeleted)
                VALUES (?, 0, 0, ?, ?, 0)
            """.trimIndent()).use { stmt ->
                stmt.bindText(1, paddedUuid)
                stmt.bindText(2, "سجل مبطن")
                stmt.bindLong(3, 0L)
                stmt.step()
            }

            val locations = repo.getLocations()

            // التأكد من أن السجل غير المحلول لم يحصل على ID = 1 ولم يدرج نهائياً في قائمة المواقع الصالحة
            val unresolvedInList = locations.find { it.uuid == unparseableUuid }
            assertNull(unresolvedInList, "السجلات غير المحلولة بدون معرف رقمي مخزن يجب استبعادها من getLocations")

            // التأكد من أن أياً من المواقع التي تحمل المعرف 1 (إن وجدت) لا ترتبط بالـ UUID غير المحلول
            val loc1 = locations.find { it.id == 1L }
            if (loc1 != null) {
                assertTrue(loc1.uuid != unparseableUuid, "المعرف 1 يجب ألا يعود للسجل غير المحلول")
            }

            // السجل ذو المعرف الرقمي المخزن الصريح يجب أن يحتفظ بهويته
            val storedLoc = locations.find { it.uuid == storedIdUuid }
            assertNotNull(storedLoc)
            assertEquals(customStoredId, storedLoc.id, "يجب الحفاظ على الهوية الرقمية المخزنة للسجل")

            // السجل المبطن يجب أن يفك تشفيره إلى 78
            val paddedLoc = locations.find { it.uuid == paddedUuid }
            assertNotNull(paddedLoc)
            assertEquals(paddedNumericId, paddedLoc.id)

            // التأكد من أن deleteLocationInternal يرفض المعرفات غير الموجبة
            assertFalse(repo.deleteLocationInternal(0L), "حذف معرف 0 يجب أن يرجع false")
            assertFalse(repo.deleteLocationInternal(-1L), "حذف معرف سالب يجب أن يرجع false")

            // محاولة حذف الموقع 1 لا يجب أن تؤثر على السجل غير المحلول
            repo.deleteLocationInternal(1L)

            // التأكد من أن السجل غير المحلول لم يتأثر ولم يحذف عبر حذف المعرف 1
            var unparseableDeleted = 0L
            conn.prepare("SELECT isDeleted FROM stock_locations WHERE uuid = ?").use { stmt ->
                stmt.bindText(1, unparseableUuid)
                if (stmt.step()) unparseableDeleted = stmt.getLong(0)
            }
            assertEquals(0L, unparseableDeleted, "السجل غير المحلول يجب ألا يتم حذفه عند حذف الموقع 1")

            // حذف السجل ذي الهوية المخزنة الصريحة عبر معرفه الرقمي ينجح
            assertTrue(repo.deleteLocationInternal(customStoredId), "حذف الموقع الصريح بالمعرف المخزن يجب أن ينجح")
            assertNull(repo.getLocations().find { it.id == customStoredId })
        } finally {
            cleanup()
        }
    }

    @Test
    fun testAddLocationPrimaryDemotionAtomicityOnFailure() {
        val repo = StockLocationRepository()
        val conn = SqliteDatabaseManager.getConnection()

        val primaryName = "موقع رئيسي أصلي ذرية"
        val abortName = "ABORT_PRIMARY_TARGET"

        // 1. إضافة موقع رئيسي أولي
        val originalPrimary = repo.addLocation(
            StockLocation(
                name = primaryName,
                locationType = "SITE",
                metadata = "{\"isPrimary\":true}"
            )
        )
        assertTrue(originalPrimary.isPrimary, "يجب أن يكون الموقع الأصلي رئيساً")

        try {
            // تفعيل زناد مؤقت لتعطيل إدراج الموقع الجديد وإثارة خطأ
            conn.prepare("""
                CREATE TEMP TRIGGER IF NOT EXISTS trigger_abort_add_primary 
                BEFORE INSERT ON stock_locations 
                WHEN NEW.name = '$abortName'
                BEGIN 
                    SELECT RAISE(ABORT, 'Simulated failure during location insert'); 
                END;
            """.trimIndent()).use { it.step() }

            // محاولة إضافة موقع رئيسي جديد مما يتطلب تنزيل رتبة الموقع الأصلي، لكن الإدراج الثاني سيفشل
            assertFailsWith<Throwable> {
                repo.addLocation(
                    StockLocation(
                        name = abortName,
                        locationType = "SITE",
                        metadata = "{\"isPrimary\":true}"
                    )
                )
            }

            // التحقق من التراجع الذري: لم يتم تنزيل رتبة الموقع الأصلي وظل رئيساً
            val currentPrimary = repo.getLocations().find { it.id == originalPrimary.id }
            assertNotNull(currentPrimary)
            assertTrue(currentPrimary.isPrimary, "الموقع الأصلي يجب أن يظل رئيساً بعد تراجع المعاملة الذرية")

            // الموقع المستهدف لم يتم إنشاؤه
            assertNull(repo.getLocations().find { it.name == abortName })
        } finally {
            runCatching {
                conn.prepare("DROP TRIGGER IF EXISTS trigger_abort_add_primary;").use { it.step() }
            }
            repo.deleteLocationInternal(originalPrimary.id)
        }
    }

    @Test
    fun testAddLocationWithIntermediatesAtomicityOnFailure() {
        val repo = StockLocationRepository()
        val conn = SqliteDatabaseManager.getConnection()

        val rootWh = repo.addLocation(StockLocation(name = "مستودع اختبار التراجع الهرمي", locationType = "WAREHOUSE"))
        val inter1Name = "INTERMEDIATE_FAIL_ZONE"
        val inter2Name = "INTERMEDIATE_FAIL_AISLE"
        val targetName = "ABORT_FAIL_SHELF"

        try {
            // تفعيل زناد مؤقت لتعطيل إدراج الموقع النهائي
            conn.prepare("""
                CREATE TEMP TRIGGER IF NOT EXISTS trigger_abort_intermediates 
                BEFORE INSERT ON stock_locations 
                WHEN NEW.name = '$targetName'
                BEGIN 
                    SELECT RAISE(ABORT, 'Simulated failure on target shelf insert'); 
                END;
            """.trimIndent()).use { it.step() }

            val targetShelf = StockLocation(
                name = targetName,
                locationType = "SHELF",
                parentId = rootWh.id
            )

            val intermediates = listOf(
                IntermediateNodeSpec(locationType = "ZONE", name = inter1Name),
                IntermediateNodeSpec(locationType = "AISLE", name = inter2Name)
            )

            // محاولة إنشاء السلسلة الهرمية مع الفشل المتعمد عند الموقع النهائي
            assertFailsWith<Throwable> {
                repo.addLocationWithIntermediates(targetShelf, intermediates)
            }

            // التحقق من التراجع التام: لا توجد أي عقد وسيطة متبقية في قاعدة البيانات
            val allLocs = repo.getLocations()
            assertNull(allLocs.find { it.name == inter1Name }, "الطبقة الوسيطة 1 يجب أن تتراجع تماماً")
            assertNull(allLocs.find { it.name == inter2Name }, "الطبقة الوسيطة 2 يجب أن تتراجع تماماً")
            assertNull(allLocs.find { it.name == targetName }, "الموقع النهائي يجب ألا يتم إنشاؤه")
        } finally {
            runCatching {
                conn.prepare("DROP TRIGGER IF EXISTS trigger_abort_intermediates;").use { it.step() }
            }
            repo.deleteLocationInternal(rootWh.id)
        }
    }
}


