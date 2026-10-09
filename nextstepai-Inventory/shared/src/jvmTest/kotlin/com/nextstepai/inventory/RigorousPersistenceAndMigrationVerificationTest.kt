package com.nextstepai.inventory

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nextstepai.inventory.data.*
import com.nextstepai.inventory.data.db.*
import com.nextstepai.inventory.repository.CompanyRepository
import com.nextstepai.inventory.repository.PartRepository
import com.nextstepai.inventory.repository.StockLocationRepository
import com.nextstepai.inventory.repository.StockRepository
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * اختبارات تحقق قاطعة وهندسية دقيقة لـ PR #3:
 * 1. استمرارية المعرفات الرقمية والعلاقات الهرمية لمواقع التخزين (StockLocation) عبر إعادة التشغيل على ملف حقيقي.
 * 2. ترحيل قواعد البيانات القديمة (Legacy DB Migration) بدون أي انهيار وبدون فقدان ذرة واحدة من البيانات السابقة.
 * 3. التحقق من سياسة الإغلاق عند الفشل (Fail-Closed Policy) في حال غياب الجداول الأساسية.
 */
class RigorousPersistenceAndMigrationVerificationTest {

    @Test
    fun testRealPersistenceAndColdRestartWithNumericIds() {
        val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
        val tempDbFile = File(System.getProperty("java.io.tmpdir"), "test_cold_restart_$runToken.db")
        if (tempDbFile.exists()) tempDbFile.delete()
        tempDbFile.deleteOnExit()

        try {
            SqliteDatabaseManager.setCustomDatabasePath(tempDbFile.absolutePath)

            val partRepo = PartRepository()
            val locationRepo = StockLocationRepository()
            val stockRepo = StockRepository(locationRepository = locationRepo)

            // 1. إنشاء صنف جديد
            val part = partRepo.addPart(
                Part(
                    name = "متحكم دقيق STM32-$runToken",
                    description = "وحدة معالجة مركزية صناعية",
                    units = "pcs",
                    minimumStock = 10.0
                )
            )
            assertTrue(part.id > 0, "يجب أن يحصل الصنف على معرف رقمي موجب")

            // 2. إنشاء موقع جذر وموقع فرعي وموقع حفيد
            val rootLoc = locationRepo.addLocation(
                StockLocation(
                    name = "المستودع الرئيسي $runToken",
                    locationType = "WAREHOUSE",
                    structural = true
                )
            )
            assertTrue(rootLoc.id > 0, "يجب أن يحصل الموقع الجذر على معرف رقمي")

            val childLoc = locationRepo.addLocation(
                StockLocation(
                    name = "الممر 05-$runToken",
                    parentId = rootLoc.id,
                    locationType = "ZONE",
                    structural = true
                )
            )
            assertTrue(childLoc.id > rootLoc.id, "يجب أن يكون ترقيم الموقع الفرعي تصاعدياً")
            assertEquals(rootLoc.id, childLoc.parentId)

            val shelfLoc = locationRepo.addLocation(
                StockLocation(
                    name = "الرف B2-$runToken",
                    parentId = childLoc.id,
                    locationType = "SHELF",
                    structural = false,
                    customCapacity = 500.0
                ).withCapacityUnit("pcs")
            )
            assertTrue(shelfLoc.id > childLoc.id, "يجب أن يكون ترقيم الرف تصاعدياً")
            assertEquals(childLoc.id, shelfLoc.parentId)

            // 3. إضافة وحدة مخزنية مرتبطة بالرف والصنف
            val stockItem = stockRepo.addStockItem(
                StockItem(
                    partId = part.id,
                    locationId = shelfLoc.id,
                    quantity = 150.0,
                    batch = "BATCH-$runToken",
                    packaging = "Tray"
                )
            )
            assertTrue(stockItem.id > 0, "يجب أن تحصل الوحدة المخزنية على معرف رقمي")
            assertEquals(shelfLoc.id, stockItem.locationId)
            assertEquals(part.id, stockItem.partId)

            // 4. إغلاق قاعدة البيانات بالكامل لمحاكاة إغلاق التطبيق (Cold Shutdown)
            SqliteDatabaseManager.closeDatabase()

            // 5. فحص ملف SQLite المادي مباشرة بدون أي وسيط
            val driver = BundledSQLiteDriver()
            val rawConn = driver.open(tempDbFile.absolutePath)
            try {
                // التأكد من وجود المواقع مع معرفاتها الرقمية وعلاقاتها في جدول stock_locations الفعلي
                var shelfFoundInRawDb = false
                rawConn.prepare("SELECT id, parentId, name, uuid FROM stock_locations WHERE id = ?").use { stmt ->
                    stmt.bindLong(1, shelfLoc.id)
                    if (stmt.step()) {
                        shelfFoundInRawDb = true
                        assertEquals(shelfLoc.id, stmt.getLong(0), "معرف الرف غير مطابق في الملف المادي")
                        assertEquals(childLoc.id, stmt.getLong(1), "معرف الأب للرف غير مطابق في الملف المادي")
                        assertEquals(shelfLoc.name, stmt.getText(2), "اسم الرف غير مطابق")
                        assertTrue(stmt.getText(3).isNotBlank(), "يجب أن يكون UUID محفوظاً")
                    }
                }
                assertTrue(shelfFoundInRawDb, "الرف غير موجود في ملف SQLite المادي على القرص!")

                // التأكد من ارتباط الوحدة المخزنية بالرف
                var itemFoundInRawDb = false
                rawConn.prepare("SELECT id, partId, locationId, quantity FROM stock_items WHERE id = ?").use { stmt ->
                    stmt.bindLong(1, stockItem.id)
                    if (stmt.step()) {
                        itemFoundInRawDb = true
                        assertEquals(part.id, stmt.getLong(1), "partId غير مطابق في الملف المادي")
                        assertEquals(shelfLoc.id, stmt.getLong(2), "locationId غير مطابق في الملف المادي")
                        assertEquals(150.0, stmt.getDouble(3), "quantity غير مطابقة في الملف المادي")
                    }
                }
                assertTrue(itemFoundInRawDb, "الوحدة المخزنية غير موجودة في ملف SQLite المادي على القرص!")
            } finally {
                rawConn.close()
            }

            // 6. إعادة فتح التطبيق بحاويات ومستودعات جديدة كلياً (Cold Restart)
            val freshLocationRepo = StockLocationRepository()
            val freshStockRepo = StockRepository(locationRepository = freshLocationRepo)
            val freshPartRepo = PartRepository()

            val reloadedLocations = freshLocationRepo.getLocations()
            val reloadedRoot = reloadedLocations.find { it.id == rootLoc.id }
            val reloadedChild = reloadedLocations.find { it.id == childLoc.id }
            val reloadedShelf = reloadedLocations.find { it.id == shelfLoc.id }

            assertNotNull(reloadedRoot, "فقدان الموقع الجذر بعد إعادة التشغيل")
            assertNotNull(reloadedChild, "فقدان الموقع الفرعي بعد إعادة التشغيل")
            assertNotNull(reloadedShelf, "فقدان الرف بعد إعادة التشغيل")

            assertEquals(rootLoc.id, reloadedRoot.id)
            assertEquals(childLoc.id, reloadedChild.id)
            assertEquals(shelfLoc.id, reloadedShelf.id)

            assertEquals(null, reloadedRoot.parentId, "الموقع الجذر لا يجب أن يكون له أب")
            assertEquals(rootLoc.id, reloadedChild.parentId, "فقدان ارتباط الموقع الفرعي بأبيه بعد إعادة التشغيل")
            assertEquals(childLoc.id, reloadedShelf.parentId, "فقدان ارتباط الرف بأبيه بعد إعادة التشغيل")

            // التحقق من المسار الهرمي المتسلسل الكامل
            val fullPath = reloadedShelf.getFullHierarchyPath(reloadedLocations)
            val expectedPath = "${rootLoc.name} > ${childLoc.name} > ${shelfLoc.name}"
            assertEquals(expectedPath, fullPath, "المسار الهرمي تلف بعد إعادة التشغيل")

            // التحقق من استرجاع الوحدة المخزنية وارتباطها بالرف والصنف
            val reloadedItems = freshStockRepo.getStockItems()
            val reloadedItem = reloadedItems.find { it.id == stockItem.id }
            assertNotNull(reloadedItem, "فقدان الوحدة المخزنية بعد إعادة التشغيل")
            assertEquals(shelfLoc.id, reloadedItem.locationId, "تلف رابط الرف للوحدة المخزنية بعد إعادة التشغيل")
            assertEquals(part.id, reloadedItem.partId, "تلف رابط الصنف للوحدة المخزنية بعد إعادة التشغيل")
            assertEquals(150.0, reloadedItem.quantity)

            // التحقق من حساب نسبة إشغال الرف
            val allParts = freshPartRepo.getParts()
            val itemsInShelf = reloadedItems.filter { it.locationId == reloadedShelf.id }
            val (pctText, previewText) = reloadedShelf.getOccupancySummary(itemsInShelf, allParts)
            assertTrue(pctText.contains("30%"), "فشل احتساب إشغال الرف: $pctText")
            assertTrue(previewText.contains("150"), "فشل عرض محتويات الرف: $previewText")

        } finally {
            SqliteDatabaseManager.closeDatabase()
            SqliteDatabaseManager.setCustomDatabasePath(null)
            runCatching { tempDbFile.delete() }
        }
    }

    @Test
    fun testLegacyDatabaseMigrationPreservesPreExistingDataWithoutLoss() {
        val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
        val legacyDbFile = File(System.getProperty("java.io.tmpdir"), "test_legacy_db_$runToken.db")
        if (legacyDbFile.exists()) legacyDbFile.delete()
        legacyDbFile.deleteOnExit()

        try {
            // 1. بناء قاعدة بيانات قديمة تماماً تحاكي إصداراً قديماً من التطبيق بدون عمود uuid
            val driver = BundledSQLiteDriver()
            val preMigrationConn = driver.open(legacyDbFile.absolutePath)
            try {
                preMigrationConn.prepare("""
                    CREATE TABLE parts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        description TEXT,
                        ipn TEXT
                    );
                """.trimIndent()).use { it.step() }

                preMigrationConn.prepare("""
                    CREATE TABLE companies (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        description TEXT
                    );
                """.trimIndent()).use { it.step() }

                preMigrationConn.prepare("""
                    CREATE TABLE stock_locations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        description TEXT,
                        parentId INTEGER
                    );
                """.trimIndent()).use { it.step() }

                preMigrationConn.prepare("""
                    CREATE TABLE stock_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        partId INTEGER,
                        locationId INTEGER,
                        quantity REAL
                    );
                """.trimIndent()).use { it.step() }

                preMigrationConn.prepare("""
                    CREATE TABLE part_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        parentId INTEGER
                    );
                """.trimIndent()).use { it.step() }

                preMigrationConn.prepare("""
                    INSERT INTO parts (id, name, description, ipn)
                    VALUES (101, 'مقاومة قديمة 10K', 'مقاومة كربونية دقيقة', 'RES-10K');
                """.trimIndent()).use { it.step() }

                preMigrationConn.prepare("""
                    INSERT INTO companies (id, name, description)
                    VALUES (202, 'شركة التوريد المتقدمة', 'مورد معتمد تاريخي');
                """.trimIndent()).use { it.step() }

                preMigrationConn.prepare("""
                    INSERT INTO stock_locations (id, name, description, parentId)
                    VALUES (301, 'مستودع قديم A', 'المبنى الغربي', NULL);
                """.trimIndent()).use { it.step() }

                preMigrationConn.prepare("""
                    INSERT INTO stock_locations (id, name, description, parentId)
                    VALUES (302, 'رف قديم 01', 'مستوى أرضي', 301);
                """.trimIndent()).use { it.step() }

                preMigrationConn.prepare("""
                    INSERT INTO part_categories (id, name, parentId)
                    VALUES (11, 'مكونات إلكترونية', NULL);
                """.trimIndent()).use { it.step() }

                preMigrationConn.prepare("""
                    INSERT INTO stock_items (id, partId, locationId, quantity)
                    VALUES (501, 101, 302, 45.0);
                """.trimIndent()).use { it.step() }
            } finally {
                preMigrationConn.close()
            }

            // 2. الآن نفتح هذه القاعدة القديمة بواسطة SqliteDatabaseManager عبر مسار الإنتاج الفعلي
            SqliteDatabaseManager.setCustomDatabasePath(legacyDbFile.absolutePath)
            val migratedConn = SqliteDatabaseManager.getConnection()
            assertNotNull(migratedConn, "فشل تهيئة وترقية قاعدة البيانات القديمة")

            // 3. التحقق المباشر من أن الأعمدة تمت إضافتها وأن السجلات القديمة مُلئت معرّفاتها بنجاح
            val partRepo = PartRepository()
            val companyRepo = CompanyRepository()
            val locationRepo = StockLocationRepository()
            val stockRepo = StockRepository(locationRepository = locationRepo)

            val parts = partRepo.getParts()
            val migratedPart = parts.find { it.id == 101L }
            assertNotNull(migratedPart, "تم فقدان الصنف القديم أثناء الترحيل!")
            assertEquals("مقاومة قديمة 10K", migratedPart.name)
            assertEquals("RES-10K", migratedPart.ipn)
            assertEquals("part-101", migratedPart.uuid)

            val companies = companyRepo.getCompanies()
            val migratedCompany = companies.find { it.id == 202L }
            assertNotNull(migratedCompany, "تم فقدان الشركة القديمة أثناء الترحيل!")
            assertEquals("شركة التوريد المتقدمة", migratedCompany.name)
            assertEquals("company-202", migratedCompany.uuid)

            val locations = locationRepo.getLocations()
            val rootLoc = locations.find { it.id == 301L }
            val childLoc = locations.find { it.id == 302L }

            assertNotNull(rootLoc, "تم فقدان المستودع القديم الجذر أثناء الترحيل!")
            assertNotNull(childLoc, "تم فقدان الرف القديم أثناء الترحيل!")
            assertEquals("مستودع قديم A", rootLoc.name)
            assertEquals("رف قديم 01", childLoc.name)
            assertEquals(301L, childLoc.parentId, "تم فقدان العلاقة الهرمية بين المواقع القديمة أثناء الترحيل!")
            assertEquals("loc-301", rootLoc.uuid)
            assertEquals("loc-302", childLoc.uuid)

            // التحقق من استرجاع الوحدة المخزنية القديمة وارتباطها بالصنف والرف
            val stockItems = stockRepo.getStockItems()
            val migratedStockItem = stockItems.find { it.id == 501L }
            assertNotNull(migratedStockItem, "تم فقدان الوحدة المخزنية القديمة أثناء الترحيل!")
            assertEquals(101L, migratedStockItem.partId)
            assertEquals(302L, migratedStockItem.locationId)
            assertEquals(45.0, migratedStockItem.quantity)

            // 4. التحقق من أن إضافة صنف وموقع جديد الآن يحترم الترقيم التصاعدي دون تصادم
            val newPart = partRepo.addPart(Part(name = "مكثف جديد 100uF"))
            assertTrue(newPart.id > 101L, "يجب أن يكون ترقيم الصنف الجديد أعلى من الأصناف القديمة")

            val newLoc = locationRepo.addLocation(StockLocation(name = "رف جديد 02", parentId = rootLoc.id))
            assertTrue(newLoc.id > 302L, "يجب أن يكون ترقيم الموقع الجديد أعلى من المواقع القديمة")
            assertEquals(301L, newLoc.parentId)

            // 5. محاكاة إغلاق التطبيق وإعادة فتحه للتحقق من ثبات الترحيل وعدم تكرار العمليات (Idempotency)
            SqliteDatabaseManager.closeDatabase()

            val freshLocationRepo = StockLocationRepository()
            val freshStockRepo = StockRepository(locationRepository = freshLocationRepo)
            val freshPartRepo = PartRepository()

            val postRestartLocations = freshLocationRepo.getLocations()
            val postRestartItems = freshStockRepo.getStockItems()
            val postRestartParts = freshPartRepo.getParts()

            assertEquals(3, postRestartLocations.size, "يجب أن تبقى المواقع الثلاثة كما هي بعد إعادة التشغيل")
            assertEquals(1, postRestartItems.size, "يجب أن تبقى الوحدة المخزنية كما هي بعد إعادة التشغيل")
            assertEquals(2, postRestartParts.size, "يجب أن يبقى الصنفان كما هما بعد إعادة التشغيل")

        } finally {
            SqliteDatabaseManager.closeDatabase()
            SqliteDatabaseManager.setCustomDatabasePath(null)
            runCatching { legacyDbFile.delete() }
        }
    }

    @Test
    fun testOpeningSeverelyIncompleteDatabaseViaActualProductionPathCreatesMissingTablesSafely() {
        val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
        val partialDbFile = File(System.getProperty("java.io.tmpdir"), "test_partial_db_$runToken.db")
        if (partialDbFile.exists()) partialDbFile.delete()
        partialDbFile.deleteOnExit()

        try {
            // إنشاء قاعدة بيانات تحتوي على جدولين فقط من أصل 39 جدولاً
            val driver = BundledSQLiteDriver()
            val initialConn = driver.open(partialDbFile.absolutePath)
            try {
                initialConn.prepare("""
                    CREATE TABLE parts (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL
                    );
                """.trimIndent()).use { it.step() }

                initialConn.prepare("""
                    INSERT INTO parts (id, name) VALUES (1, 'جزء أولي');
                """.trimIndent()).use { it.step() }
            } finally {
                initialConn.close()
            }

            // فتح القاعدة عبر المسار الفعلي للإنتاج
            SqliteDatabaseManager.setCustomDatabasePath(partialDbFile.absolutePath)
            val conn = SqliteDatabaseManager.getConnection()
            assertNotNull(conn, "فشل فتح قاعدة البيانات الناقصة عبر مسار الإنتاج")

            // التأكد من أن جميع المستودعات تستطيع العمل فوراً دون خطأ 'no such table'
            val partRepo = PartRepository()
            val locationRepo = StockLocationRepository()
            val stockRepo = StockRepository(locationRepository = locationRepo)
            val companyRepo = CompanyRepository()

            // 1. القراءة من الجداول القديمة التي رُقّيت
            val parts = partRepo.getParts()
            assertEquals(1, parts.size)
            assertEquals("جزء أولي", parts[0].name)

            // 2. القراءة والكتابة في الجداول التي أنشئت تلقائياً
            val newCompany = companyRepo.addCompany(Company(name = "شركة حديثة $runToken"))
            assertTrue(newCompany.id > 0, "فشل إضافة شركة في جدول أُنشئ حديثاً")

            val newLocation = locationRepo.addLocation(StockLocation(name = "مستودع حديث $runToken"))
            assertTrue(newLocation.id > 0, "فشل إضافة موقع في جدول أُنشئ حديثاً")

            val newItem = stockRepo.addStockItem(StockItem(partId = parts[0].id, locationId = newLocation.id, quantity = 10.0))
            assertTrue(newItem.id > 0, "فشل إضافة مادة مخزنية في جدول أُنشئ حديثاً")

            // 3. إغلاق وإعادة فتح للتأكد من الاستقرار التام
            SqliteDatabaseManager.closeDatabase()
            val reconnected = SqliteDatabaseManager.getConnection()
            assertNotNull(reconnected)

            val reloadedParts = partRepo.getParts()
            val reloadedCompanies = companyRepo.getCompanies()
            assertEquals(1, reloadedParts.size)
            assertEquals(1, reloadedCompanies.size)

        } finally {
            SqliteDatabaseManager.closeDatabase()
            SqliteDatabaseManager.setCustomDatabasePath(null)
            runCatching { partialDbFile.delete() }
        }
    }

    @Test
    fun testMigrationFailClosedOnMissingEssentialTable() {
        val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
        val tempDbFile = File(System.getProperty("java.io.tmpdir"), "test_fail_closed_$runToken.db")
        if (tempDbFile.exists()) tempDbFile.delete()
        tempDbFile.deleteOnExit()

        val driver = BundledSQLiteDriver()
        val rawConn = driver.open(tempDbFile.absolutePath)
        try {
            // محاولة تشغيل ترحيل على جدول أساسي غير موجود أصلاً في قاعدة غير مكتملة
            val ex = assertFailsWith<IllegalStateException> {
                SqliteDatabaseMigrations.addColumnIfMissing(
                    rawConn,
                    "ALTER TABLE parts ADD COLUMN non_existent_col TEXT;"
                )
            }
            assertTrue(
                ex.message?.contains("required essential table 'parts' is missing") == true,
                "يجب أن تفشل الترقية بسياسة fail-closed واضحة عند غياب جدول أساسي: ${ex.message}"
            )
        } finally {
            rawConn.close()
            runCatching { tempDbFile.delete() }
        }
    }
}
