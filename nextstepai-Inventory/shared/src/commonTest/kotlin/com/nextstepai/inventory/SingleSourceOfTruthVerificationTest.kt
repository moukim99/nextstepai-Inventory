package com.nextstepai.inventory

import com.nextstepai.inventory.data.*
import com.nextstepai.inventory.data.db.*
import com.nextstepai.inventory.repository.CompanyRepository
import com.nextstepai.inventory.repository.PartRepository
import com.nextstepai.inventory.sync.SyncStatus
import java.io.File
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*
import kotlin.time.Clock

/**
 * اختبارات التحقق الصارمة للـ 7 ملاحظات الخاصة بـ Single Source of Truth في SQLite:
 * 1. [P1] addStockToPart() يفشل ويعيد null عند غياب السجل في SQLite دون نجاح وهمي في الذاكرة.
 * 2. [P1] CompanyDao.delete() يُنشئ Tombstone بحالة PENDING وزيادة الـ version للشركات المتزامنة.
 * 3. [P1] SqliteNumericIdAllocator آمن 100% ومتزامن دون أي تكرار تحت ضغط التزامن المتعدد (Concurrency).
 * 4. [P1] الحفاظ الكامل على معرّفات UUIDv7 النصية غير الرقمية في دورة حياة القطع دون تحويلها لـ 0.
 * 5. [P1] الكيانات التابعة للقطع (المرفقات، الملاحظات، الأسعار، التفضيلات، الفحوصات، المعاملات، الصلات) محفوظة ومسترجعة من SQLite.
 * 6. [P2] عمليات التعديل والحذف تُرجع false صريحاً عند عدم وجود السجل في SQLite.
 * 7. [P2] فرض قيد فرادة اسم الشركة في SQLite ومنع تكرار الأسماء النشطة.
 */
class SingleSourceOfTruthVerificationTest {

    private lateinit var tempDbFile: File

    @BeforeTest
    fun setUp() {
        val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
        tempDbFile = File(System.getProperty("java.io.tmpdir"), "test_ssot_$runToken.db")
        if (tempDbFile.exists()) tempDbFile.delete()
        tempDbFile.deleteOnExit()
        SqliteDatabaseManager.setCustomDatabasePath(tempDbFile.absolutePath)
        SqliteDatabaseManager.getConnection()
    }

    @AfterTest
    fun tearDown() {
        SqliteDatabaseManager.closeDatabase()
        SqliteDatabaseManager.setCustomDatabasePath(null)
        runCatching { tempDbFile.delete() }
    }

    @Test
    fun testFinding1AddStockToPartStrictPersistence() {
        val partRepo = PartRepository()

        // 1. تحديث قطعة غير موجودة يجب أن يعيد null ولا ينجح وهمياً
        val resultNonExistent = partRepo.addStockToPart(999999L, 10.0)
        assertNull(resultNonExistent, "addStockToPart يجب أن يعيد null إذا لم تكن القطعة موجودة في SQLite")

        val resultNonExistentUuid = partRepo.addStockToPartByUuid("part-non-existent-999", 10.0)
        assertNull(resultNonExistentUuid, "addStockToPartByUuid يجب أن يعيد null إذا لم تكن القطعة موجودة")

        // 2. تحديث قطعة موجودة فعلياً يجب أن يحدثها في SQLite
        val part = partRepo.addPart(
            Part(
                name = "قطعة تجربة رصيد المخزون",
                ipn = "STOCK-VERIFY-001",
                totalInStock = 5.0
            )
        )
        val updated = partRepo.addStockToPart(part.id, 15.0)
        assertNotNull(updated)
        assertEquals(20.0, updated.totalInStock)

        // التحقق من مستودع جديد لقراءة القيمة من SQLite مباشرة
        val freshRepo = PartRepository()
        val readFromDb = freshRepo.getPartById(part.id)
        assertNotNull(readFromDb)
        assertEquals(20.0, readFromDb.totalInStock)
    }

    @Test
    fun testFinding2CompanyDeletionProducesTombstone() {
        val companyRepo = CompanyRepository()
        val companyDao = CompanyDao()

        // إنشاء شركة جديدة
        val company = companyRepo.addCompany(
            Company(
                name = "شركة تومبستون للاختبار",
                description = "اختبار مزامنة الحذف",
                isSupplier = true
            )
        )

        // محاكاة مزامنة سابقة بجعل الحالة SYNCED
        companyDao.updateSyncStatusForUuids(listOf(company.effectiveUuid), SyncStatus.SYNCED)

        // حذف الشركة عبر المستودع
        val deleted = companyRepo.deleteCompany(company.id)
        assertTrue(deleted, "حذف الشركة يجب أن ينجح في SQLite")

        // التحقق من قائمة الشركات المعلّقة للمزامنة
        val pending = companyDao.getPendingSyncCompanies(SyncStatus.PENDING)
        val tombstone = pending.find { it.uuid == company.effectiveUuid }
        assertNotNull(tombstone, "الشركة المحذوفة يجب أن تظهر في getPendingSyncCompanies بحالة PENDING")
        assertTrue(tombstone.isDeleted, "يجب أن يكون الحذف منطقياً (isDeleted = true)")
        assertEquals(SyncStatus.PENDING, tombstone.syncStatus, "حالة المزامنة يجب أن تتحول إلى PENDING")
        assertTrue(tombstone.version > 1, "يجب زيادة رقم الإصدار version لضمان قبول التومبستون في الخادم")
    }

    @Test
    fun testFinding3ConcurrentNumericIdAllocationIsThreadSafe() {
        val threadCount = 8
        val allocationsPerThread = 25
        val totalAllocations = threadCount * allocationsPerThread
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val allocatedIds = Collections.synchronizedList(mutableListOf<Long>())

        for (i in 0 until threadCount) {
            executor.submit {
                try {
                    for (j in 0 until allocationsPerThread) {
                        val id = SqliteNumericIdAllocator.nextId("parts", "part-")
                        allocatedIds.add(id)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "مهلة تخصيص المعرفات المتزامنة انتهت")
        executor.shutdown()

        assertEquals(totalAllocations, allocatedIds.size)
        val uniqueIds = allocatedIds.toSet()
        assertEquals(totalAllocations, uniqueIds.size, "يجب ألا يكون هناك أي تصادم أو تكرار في المعرفات المخصصة متزامناً")
    }

    @Test
    fun testNumericIdAllocatorAccountsForCustomUuidRows() {
        val conn = SqliteDatabaseManager.getConnection()
        assertEquals(1L, PartDao().getPartByUuid("part-1")?.id, "يجب حفظ المعرف الرقمي لبيانات القطع الأولية في SQLite")
        assertEquals(5L, PartDao().getPartByUuid("part-5")?.id, "يجب أن تحتفظ كل القطع الأولية بمعرفها الرقمي")
        conn.prepare("INSERT INTO parts (uuid, id, name) VALUES ('custom-part-with-high-id', 50000, 'custom id row')").use { it.step() }
        conn.prepare("INSERT INTO companies (uuid, id, name) VALUES ('custom-company-with-high-id', 60000, 'custom company row')").use { it.step() }

        val nextPartId = SqliteNumericIdAllocator.nextId("parts", "part-")
        val nextCompanyId = SqliteNumericIdAllocator.nextId("companies", "company-")

        assertTrue(nextPartId > 50000L, "يجب ألا يتكرر رقم قطعة موجودة بمعرّف UUID مخصص")
        assertTrue(nextCompanyId > 60000L, "يجب ألا يتكرر رقم شركة موجودة بمعرّف UUID مخصص")
    }

    @Test
    fun testDatabaseEnforcesPartIdUniquenessConstraint() {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("INSERT INTO parts (uuid, id, name) VALUES ('unique-test-part-1', 77777, 'Unique Part 1')").use { it.step() }

        assertFailsWith<Throwable>("يجب أن ترفض قاعدة البيانات إدراج قطعة بمعرف رقمي مكرر عبر قيد idx_parts_unique_id") {
            conn.prepare("INSERT INTO parts (uuid, id, name) VALUES ('unique-test-part-2', 77777, 'Duplicate Part ID')").use { it.step() }
        }
    }

    @Test
    fun testDatabaseEnforcesCompanyIdUniquenessConstraint() {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("INSERT INTO companies (uuid, id, name) VALUES ('unique-test-comp-1', 88888, 'Unique Comp 1')").use { it.step() }

        assertFailsWith<Throwable>("يجب أن ترفض قاعدة البيانات إدراج شركة بمعرف رقمي مكرر عبر قيد idx_companies_unique_id") {
            conn.prepare("INSERT INTO companies (uuid, id, name) VALUES ('unique-test-comp-2', 88888, 'Duplicate Comp ID')").use { it.step() }
        }
    }

    @Test
    fun testAtomicMigrationRollsBackOnFailureAndRecoversOnRetry() {
        val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
        val tempFile = File(System.getProperty("java.io.tmpdir"), "atomic_mig_test_$runToken.db")
        tempFile.deleteOnExit()

        try {
            // Seed a raw DB with duplicated active company names to trigger ensureCompanyNameUniqueIndex failure
            val driver = androidx.sqlite.driver.bundled.BundledSQLiteDriver()
            val raw = driver.open(tempFile.absolutePath)
            raw.prepare("CREATE TABLE companies (uuid TEXT PRIMARY KEY, id INTEGER DEFAULT 0, name TEXT, isDeleted INTEGER DEFAULT 0);").use { it.step() }
            raw.prepare("INSERT INTO companies (uuid, name, isDeleted) VALUES ('c1', 'Conflicting Corp', 0)").use { it.step() }
            raw.prepare("INSERT INTO companies (uuid, name, isDeleted) VALUES ('c2', 'Conflicting Corp', 0)").use { it.step() }
            raw.close()

            SqliteDatabaseManager.setCustomDatabasePath(tempFile.absolutePath)

            // Opening via manager must fail closed atomically
            assertFailsWith<IllegalStateException>("يجب أن تفشل التهيئة لوجود تعارض في البيانات يمنع الفهرس الفريد") {
                SqliteDatabaseManager.getConnection()
            }

            // Verify original data is preserved and not left corrupted
            val rawCheck = driver.open(tempFile.absolutePath)
            var count = 0L
            rawCheck.prepare("SELECT COUNT(*) FROM companies").use { stmt ->
                if (stmt.step()) count = stmt.getLong(0)
            }
            assertEquals(2L, count, "البيانات الأصلية يجب أن تظل محفوظة ولم تُفقد بعد التراجع الذري")

            // Heal the conflict
            rawCheck.prepare("UPDATE companies SET name = 'Conflicting Corp Healed' WHERE uuid = 'c2'").use { it.step() }
            rawCheck.close()

            // Re-open: manager should now succeed completely and apply migration
            SqliteDatabaseManager.closeDatabase()
            val healedConn = SqliteDatabaseManager.getConnection()
            assertNotNull(healedConn)
            assertTrue(SqliteDatabaseManager.isDatabaseOpen())
        } finally {
            SqliteDatabaseManager.closeDatabase()
            SqliteDatabaseManager.setCustomDatabasePath(tempDbFile.absolutePath)
            runCatching { tempFile.delete() }
        }
    }

    @Test
    fun testMultiThreadedConcurrentAllocationsMaintainStrictUniquenessAndContiguity() {
        val threadCount = 8
        val allocationsPerThread = 30
        val totalAllocations = threadCount * allocationsPerThread
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val allocatedIds = Collections.synchronizedList(mutableListOf<Long>())

        val startBaseId = SqliteNumericIdAllocator.nextId("parts", "part-")
        allocatedIds.add(startBaseId)

        for (i in 0 until threadCount) {
            executor.submit {
                try {
                    for (j in 0 until allocationsPerThread) {
                        val id = SqliteNumericIdAllocator.nextId("parts", "part-")
                        allocatedIds.add(id)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(15, TimeUnit.SECONDS), "مهلة اختبار التزامن المتعدد انتهت")
        executor.shutdown()

        assertEquals(totalAllocations + 1, allocatedIds.size)
        val uniqueIds = allocatedIds.toSet()
        assertEquals(totalAllocations + 1, uniqueIds.size, "كل المعرفات يجب أن تكون فريدة تماماً دون أي تكرار")

        val sorted = allocatedIds.sorted()
        for (k in 1 until sorted.size) {
            assertEquals(sorted[k - 1] + 1, sorted[k], "تسلسل المعرفات يجب أن يكون متتالياً وصارماً")
        }
    }

    @Test
    fun testFinding4NonNumericUuidv7Preservation() {
        val partRepo = PartRepository()
        val customUuid = "018f3a5b-7c8d-7e9f-a0b1-c2d3e4f5a6b7"

        val part = Part(
            uuid = customUuid,
            name = "قطعة بمعرف UUIDv7 خاص",
            ipn = "UUIDV7-IPN-001",
            description = "اختبار الحفاظ على هوية السجل"
        )
        val inserted = partRepo.addPart(part)
        assertEquals(customUuid, inserted.effectiveUuid, "يجب الحفاظ على الـ UUIDv7 الأصلي")
        assertTrue(inserted.id > 0L, "يجب تخصيص معرّف رقمي متوافق حتى عندما يكون UUID غير رقمي")

        // قراءة القطعة بواسطة UUID الخاص بها
        val fetched = partRepo.getPartByUuid(customUuid)
        assertNotNull(fetched, "يجب العثور على القطعة باستخدام UUID الأصلي")
        assertEquals(customUuid, fetched.effectiveUuid)

        // تحديث القطعة
        val updated = partRepo.updatePart(fetched.copy(description = "وصف محدث للـ UUIDv7"))
        assertTrue(updated, "تحديث القطعة ذات المعرف النصي يجب أن ينجح")

        val fetchedAfterUpdate = partRepo.getPartByUuid(customUuid)
        assertNotNull(fetchedAfterUpdate)
        assertEquals("وصف محدث للـ UUIDv7", fetchedAfterUpdate.description)

        // اختبار إعادة الفتح: يجب أن يبقى الربط بين UUID والـ numeric ID ثابتاً.
        val persistedId = fetchedAfterUpdate.id
        SqliteDatabaseManager.closeDatabase()
        SqliteDatabaseManager.getConnection()
        val freshRepo = PartRepository()
        val fetchedAfterRestart = freshRepo.getPartByUuid(customUuid)
        assertNotNull(fetchedAfterRestart)
        assertEquals(persistedId, fetchedAfterRestart.id)
        assertEquals(customUuid, freshRepo.getPartById(persistedId)?.effectiveUuid)

        // حذف القطعة
        val deleteResult = partRepo.deletePartByUuid(customUuid)
        assertTrue(deleteResult.isSuccess, "حذف القطعة ذات المعرف النصي يجب أن ينجح")

        // التأكد من عدم وجودها بعد الحذف
        assertNull(freshRepo.getPartByUuid(customUuid), "يجب ألا تظهر القطعة المحذوفة في القراءات")
    }

    @Test
    fun testFinding5PartSubEntitiesPersistInSqliteAcrossReopen() {
        val partRepo = PartRepository()

        // إنشاء قطعتين لاختبار الصلات والبيانات التابعة
        val part1 = partRepo.addPart(Part(name = "قطعة رئيسية للكيانات التابعة", ipn = "SUBENT-01", salable = true))
        val part2 = partRepo.addPart(Part(name = "قطعة ثانوية للربط التبادلي", ipn = "SUBENT-02"))

        // 1. المرفقات
        val att = partRepo.addPartAttachment(
            PartAttachment(
                partId = part1.id,
                link = "https://example.com/datasheet.pdf",
                comment = "ورقة البيانات الفنية"
            )
        )
        assertTrue(att.id > 0L)

        // 2. الملاحظات
        partRepo.saveOrUpdatePartNotes(part1.id, "ملاحظات تفصيلية محفوظة في SQLite")

        // 3. الأسعار الداخلية
        val internalPrice = partRepo.addPartInternalPrice(part1.id, quantity = 10.0, price = 2.50, currency = "USD")
        assertTrue(internalPrice.id > 0L)

        // 4. أسعار البيع
        val salePrice = partRepo.addPartSalePrice(part1, quantity = 5.0, price = 4.00, currency = "USD")
        assertTrue(salePrice.id > 0L)

        // 5. التفضيل بنجمة
        partRepo.togglePartStar(part1.id, userId = 1L)

        // 6. قوالب الفحص
        val testTpl = partRepo.addPartTestTemplate(
            PartTestTemplate(
                partId = part1.id,
                testName = "فحص الجهد الكهربائي",
                required = true
            )
        )
        assertTrue(testTpl.id > 0L)

        // 7. المعاملات الفنية
        val param = partRepo.addPartParameter(part1.id, templateId = 1L, data = "5V")
        assertTrue(param.id > 0L)

        // 8. الصلة التبادلية
        val related = partRepo.addPartRelated(part1.id, part2.id)
        assertTrue(related.id > 0L)

        // إغلاق قاعدة البيانات وإعادة فتحها للتحقق من الاستمرارية
        SqliteDatabaseManager.closeDatabase()
        SqliteDatabaseManager.getConnection()

        val freshPartRepo = PartRepository()

        // التحقق من استرجاع المرفقات
        val savedAtts = freshPartRepo.getPartAttachments(part1.id)
        assertEquals(1, savedAtts.size)
        assertEquals("ورقة البيانات الفنية", savedAtts.first().comment)

        // التحقق من استرجاع الملاحظات
        val savedNotes = freshPartRepo.getPartNotes(part1.id)
        assertNotNull(savedNotes)
        assertEquals("ملاحظات تفصيلية محفوظة في SQLite", savedNotes.notes)

        // التحقق من استرجاع الأسعار الداخلية
        val savedInternalPrices = freshPartRepo.getPartInternalPrices(part1.id)
        assertEquals(1, savedInternalPrices.size)
        assertEquals(2.50, savedInternalPrices.first().price)

        // التحقق من استرجاع أسعار البيع
        val savedSalePrices = freshPartRepo.getPartSalePrices(part1.id)
        assertEquals(1, savedSalePrices.size)
        assertEquals(4.00, savedSalePrices.first().price)

        // التحقق من استرجاع التفضيل بنجمة
        assertTrue(freshPartRepo.isPartStarred(part1.id, userId = 1L))

        // التحقق من استرجاع قوالب الفحص
        val savedTests = freshPartRepo.getPartTestTemplates(part1.id)
        assertEquals(1, savedTests.size)
        assertEquals("فحص الجهد الكهربائي", savedTests.first().testName)

        // التحقق من استرجاع المعاملات الفنية
        val savedParams = freshPartRepo.getPartParameters(part1.id)
        assertTrue(savedParams.isNotEmpty())
        assertEquals("5V", savedParams.first().data)

        // التحقق من استرجاع الصلات التبادلية
        val savedRelated = freshPartRepo.getRelatedPartsForPart(part1.id)
        assertEquals(1, savedRelated.size)
        assertEquals(part2.id, savedRelated.first().relatedPart.id)
    }

    @Test
    fun testFinding6DeleteAndUpdateReturnFalseOnNonExistent() {
        val partRepo = PartRepository()
        val companyRepo = CompanyRepository()

        // 1. تحديث قطعة غير موجودة
        val updatePartNonExistent = partRepo.updatePart(Part(id = 999999L, name = "قطعة غير موجودة"))
        assertFalse(updatePartNonExistent, "تحديث قطعة غير موجودة يجب ألا ينجح ويجب أن يعيد false")

        // 2. حذف شركة غير موجودة
        val deleteCompanyNonExistent = companyRepo.deleteCompany(999999L)
        assertFalse(deleteCompanyNonExistent, "حذف شركة غير موجودة يجب ألا يعيد true")

        // 3. حذف جهة اتصال غير موجودة
        val deleteContactNonExistent = companyRepo.deleteContact(999999L)
        assertFalse(deleteContactNonExistent, "حذف جهة اتصال غير موجودة يجب أن يعيد false")

        // 4. حذف عنوان غير موجود
        val deleteAddressNonExistent = companyRepo.deleteAddress(999999L)
        assertFalse(deleteAddressNonExistent, "حذف عنوان غير موجود يجب أن يعيد false")

        // 5. حذف مرفق قطعة غير موجود
        val deleteAttNonExistent = partRepo.deletePartAttachment(999999L)
        assertFalse(deleteAttNonExistent, "حذف مرفق غير موجود يجب أن يعيد false")

        // 6. حذف سعر داخلي غير موجود
        val deleteIPriceNonExistent = partRepo.deletePartInternalPrice(999999L)
        assertFalse(deleteIPriceNonExistent, "حذف شريحة سعرية غير موجودة يجب أن يعيد false")

        // 7. حذف سعر بيع غير موجود
        val deleteSPriceNonExistent = partRepo.deletePartSalePrice(999999L)
        assertFalse(deleteSPriceNonExistent, "حذف سعر بيع غير موجود يجب أن يعيد false")

        // 8. حذف قالب فحص غير موجود
        val deleteTestNonExistent = partRepo.deletePartTestTemplate(999999L)
        assertFalse(deleteTestNonExistent, "حذف قالب فحص غير موجود يجب أن يعيد false")

        // 9. حذف صلة ربط غير موجودة
        val deleteRelatedNonExistent = partRepo.deletePartRelated(999999L)
        assertFalse(deleteRelatedNonExistent, "حذف صلة غير موجودة يجب أن يعيد false")
    }

    @Test
    fun testStaleAttachmentCacheCannotResurrectDeletedRows() {
        val partRepo = PartRepository()
        val part = partRepo.addPart(Part(name = "قطعة اختبار عدم استعادة المرفقات المحذوفة"))
        val attachment = partRepo.addPartAttachment(
            PartAttachment(partId = part.id, comment = "مرفق يجب أن يختفي")
        )

        assertEquals(1, partRepo.getPartAttachments(part.id).size)
        assertTrue(PartAttachmentDao().delete("part-att-${attachment.id}"))

        // PartAttachmentTable ما زال يحتوي نسخة الذاكرة القديمة، لكن DAO الفارغ هو المرجع الوحيد.
        assertTrue(
            partRepo.getPartAttachments(part.id).isEmpty(),
            "يجب ألا يعيد المستودع مرفقاً حُذف من SQLite بسبب وجود نسخة قديمة في الذاكرة"
        )
    }

    @Test
    fun testDeletedParameterRowsCannotReturnFromLegacyMemoryCache() {
        val repo = PartRepository()
        val part = repo.addPart(Part(name = "قطعة اختبار معاملات SQLite فقط"))
        val saved = repo.addPartParameter(part.id, templateId = 1L, data = "777")
        assertTrue(repo.getPartParameters(part.id).any { it.id == saved.id })

        assertTrue(PartParameterDao().delete("part-param-${saved.id}"))
        assertTrue(
            repo.getPartParameters(part.id).isEmpty(),
            "يجب ألا يعيد المستودع معاملات تقنية حُذفت من SQLite من نسخة الذاكرة"
        )
    }

    @Test
    fun testCategoryTemplateReadsDoNotFallBackToSeededMemoryRows() {
        val repo = PartRepository()
        val categoryId = repo.getCategories().first().id
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("UPDATE part_category_parameter_templates SET isDeleted = 1").use { it.step() }
        conn.prepare("UPDATE part_parameter_templates SET isDeleted = 1").use { it.step() }

        assertTrue(
            repo.getCategoryParameterTemplates(categoryId).isEmpty(),
            "يجب أن تكون نتيجة DAO الفارغة فارغة حتى لو كانت الجداول القديمة تحتوي قوالب تجريبية في الذاكرة"
        )
        assertTrue(repo.getAllParameterTemplates().isEmpty())
    }

    @Test
    fun testPricingRecalculationUsesCurrentSQLitePricesNotStaleMemory() {
        val repo = PartRepository()
        val part = repo.addPart(
            Part(name = "قطعة اختبار إعادة حساب السعر", purchaseable = false)
        )
        val savedPrice = repo.addPartInternalPrice(part.id, quantity = 1.0, price = 12.5)
        assertEquals(12.5, repo.getPartInternalPrices(part.id).single().price)

        // إزالة السجل من SQLite مباشرة مع ترك PartInternalPriceTable دون تحديث.
        assertTrue(PartInternalPriceDao().delete("part-iprice-${savedPrice.id}"))
        assertTrue(repo.getPartInternalPrices(part.id).isEmpty())

        val pricing = repo.recalculatePartPricing(part)
        assertNull(pricing.internalCostMin)
        assertNull(pricing.internalCostMax)
    }

    @Test
    fun testCustomCompanyUuidKeepsNumericIdentityAndChildRelationsAfterRestart() {
        val companyRepo = CompanyRepository()
        val customUuid = "018f3a5b-7c8d-7e9f-a0b1-c2d3e4f5a6c8"
        val company = companyRepo.addCompany(
            Company(uuid = customUuid, name = "شركة بمعرف UUID مخصص للاختبار")
        )
        assertTrue(company.id > 0L)
        assertEquals(customUuid, company.effectiveUuid)

        val contact = companyRepo.addContact(
            Contact(companyId = company.id, name = "جهة اتصال اختبار", phone = "000")
        )
        assertTrue(contact.id > 0L)

        SqliteDatabaseManager.closeDatabase()
        SqliteDatabaseManager.getConnection()

        val freshRepo = CompanyRepository()
        val companyAfterRestart = freshRepo.getCompanyById(company.id)
        assertNotNull(companyAfterRestart)
        assertEquals(customUuid, companyAfterRestart.effectiveUuid)
        val contacts = freshRepo.getContactsForCompany(company.id)
        assertEquals(1, contacts.size)
        assertEquals(company.id, contacts.single().companyId)
        assertEquals("جهة اتصال اختبار", contacts.single().name)
    }

    @Test
    fun testPartCascadeDeleteRollsBackWhenDependentDeleteFails() {
        val partRepo = PartRepository()
        val part = partRepo.addPart(Part(name = "قطعة اختبار التراجع الذري"))
        val attachment = partRepo.addPartAttachment(
            PartAttachment(partId = part.id, comment = "يجب أن يبقى بعد التراجع")
        )

        // إجبار إحدى خطوات cascade على الفشل بعد بدء المعاملة وبعد حذف المرفقات منطقياً.
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("DROP TABLE part_notes").use { it.step() }
        val result = partRepo.deletePartByUuid(part.effectiveUuid)
        assertTrue(result.isFailure, "يجب إرجاع فشل واضح عندما تفشل إزالة إحدى التوابع")

        // إعادة إنشاء الجدول لتمكين فحص الحالة بعد rollback.
        SqliteDatabaseSchema.createTables(conn)
        assertNotNull(partRepo.getPartByUuid(part.effectiveUuid), "يجب أن يتراجع حذف القطعة الأساسية")
        assertEquals(
            attachment.id,
            partRepo.getPartAttachments(part.id).single().id,
            "يجب أن يتراجع حذف المرفق التابع مع العملية الذرية"
        )
    }

    @Test
    fun testCompanyUniqueIndexMigrationFailsClosedOnLegacyDuplicates() {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("DROP INDEX IF EXISTS idx_companies_unique_name").use { it.step() }
        conn.prepare("INSERT INTO companies (uuid, id, name, isDeleted) VALUES ('legacy-dup-1', 90001, ' Legacy Duplicate ', 0)").use { it.step() }
        conn.prepare("INSERT INTO companies (uuid, id, name, isDeleted) VALUES ('legacy-dup-2', 90002, 'legacy duplicate', 0)").use { it.step() }

        val failure = assertFailsWith<IllegalStateException> {
            SqliteDatabaseSchema.createTables(conn)
        }
        assertTrue(failure.message.orEmpty().contains("duplicate active company names", ignoreCase = true))
    }

    @Test
    fun testFinding7CompanyNameUniquenessConstraint() {
        val companyRepo = CompanyRepository()
        val companyName = "شركة التقنية الموحدة"

        // إضافة شركة أولى
        val c1 = companyRepo.addCompany(
            Company(name = companyName, description = "النسخة الأصلية")
        )
        assertNotNull(c1)

        // محاولة إضافة شركة ثانية بنفس الاسم بالضبط يجب أن تفشل مع استثناء
        assertFailsWith<IllegalArgumentException> {
            companyRepo.addCompany(
                Company(name = companyName, description = "نسخة مكررة بنفس الاسم")
            )
        }

        // محاولة إضافة شركة ثانية بنفس الاسم مع اختلاف الأحرف الكبيرة/الصغيرة أو مسافات إضافية
        assertFailsWith<IllegalArgumentException> {
            companyRepo.addCompany(
                Company(name = "  $companyName  ", description = "نسخة مكررة مع مسافات")
            )
        }

        // حذف الشركة الأولى يجب أن يسمح بإعادة استخدام الاسم للشركات الجديدة
        val deleted = companyRepo.deleteCompany(c1.id)
        assertTrue(deleted)

        val c2 = companyRepo.addCompany(
            Company(name = companyName, description = "النسخة بعد حذف الأولى")
        )
        assertNotNull(c2)
        assertEquals(companyName, c2.name)
    }

    @Test
    fun testLegacyMigrationHealsDuplicateNumericIdsAndPreservesChildRelations() {
        val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
        val tempFile = File(System.getProperty("java.io.tmpdir"), "legacy_heal_test_$runToken.db")
        tempFile.deleteOnExit()

        try {
            val driver = androidx.sqlite.driver.bundled.BundledSQLiteDriver()
            val raw = driver.open(tempFile.absolutePath)

            // Create schema manually with duplicate IDs to simulate legacy state before index enforcement
            raw.prepare("""
                CREATE TABLE parts (
                    uuid TEXT PRIMARY KEY NOT NULL,
                    id INTEGER NOT NULL DEFAULT 0,
                    name TEXT NOT NULL,
                    isDeleted INTEGER NOT NULL DEFAULT 0
                );
            """.trimIndent()).use { it.step() }
            raw.prepare("""
                CREATE TABLE bom_items (
                    uuid TEXT PRIMARY KEY NOT NULL,
                    partUuid TEXT NOT NULL,
                    subPartUuid TEXT NOT NULL DEFAULT '',
                    partId INTEGER NOT NULL DEFAULT 0,
                    subPartId INTEGER NOT NULL DEFAULT 0
                );
            """.trimIndent()).use { it.step() }
            raw.prepare("""
                CREATE TABLE stock_items (
                    uuid TEXT PRIMARY KEY NOT NULL,
                    partUuid TEXT NOT NULL,
                    partId INTEGER NOT NULL DEFAULT 0,
                    isDeleted INTEGER NOT NULL DEFAULT 0
                );
            """.trimIndent()).use { it.step() }
            raw.prepare("""
                CREATE TABLE part_pricing (
                    uuid TEXT PRIMARY KEY NOT NULL,
                    partUuid TEXT NOT NULL UNIQUE,
                    currency TEXT NOT NULL DEFAULT 'USD',
                    overallMin REAL,
                    overallMax REAL
                );
            """.trimIndent()).use { it.step() }

            // Insert two colliding active parts with identical id = 10
            raw.prepare("INSERT INTO parts (uuid, id, name, isDeleted) VALUES ('part-collision-1', 10, 'Part 1', 0)").use { it.step() }
            raw.prepare("INSERT INTO parts (uuid, id, name, isDeleted) VALUES ('part-collision-2', 10, 'Part 2', 0)").use { it.step() }

            // Insert child records for both
            raw.prepare("INSERT INTO bom_items (uuid, partUuid, partId) VALUES ('bom-c1', 'part-collision-1', 10)").use { it.step() }
            raw.prepare("INSERT INTO bom_items (uuid, partUuid, partId) VALUES ('bom-c2', 'part-collision-2', 10)").use { it.step() }
            raw.prepare("INSERT INTO stock_items (uuid, partUuid, partId) VALUES ('stock-c1', 'part-collision-1', 10)").use { it.step() }
            raw.prepare("INSERT INTO stock_items (uuid, partUuid, partId) VALUES ('stock-c2', 'part-collision-2', 10)").use { it.step() }
            raw.prepare("INSERT INTO part_pricing (uuid, partUuid, overallMin, overallMax) VALUES ('pricing-c1', 'part-collision-1', 10.0, 20.0)").use { it.step() }
            raw.prepare("INSERT INTO part_pricing (uuid, partUuid, overallMin, overallMax) VALUES ('pricing-c2', 'part-collision-2', 15.0, 25.0)").use { it.step() }
            raw.close()

            SqliteDatabaseManager.setCustomDatabasePath(tempFile.absolutePath)
            val healedConn = SqliteDatabaseManager.getConnection()

            // Verify both parts have distinct IDs
            var id1 = 0L
            var id2 = 0L
            healedConn.prepare("SELECT id FROM parts WHERE uuid = 'part-collision-1'").use {
                if (it.step()) id1 = it.getLong(0)
            }
            healedConn.prepare("SELECT id FROM parts WHERE uuid = 'part-collision-2'").use {
                if (it.step()) id2 = it.getLong(0)
            }
            assertEquals(10L, id1, "القطعة الأولى الأصلية تحتفظ بمعرفها الرقمي 10")
            assertTrue(id2 > 10L, "القطعة المكررة تم حل تعارضها وأخذت معرفاً جديداً أكبر من 10")
            assertNotEquals(id1, id2, "المعرفات يجب أن تكون مختلفة ومميزة تماماً")

            // Verify child relations were cascaded properly
            var childBomPartId2 = 0L
            var childStockPartId2 = 0L
            var childPricingPartId1 = 0L
            var childPricingPartId2 = 0L
            healedConn.prepare("SELECT partId FROM bom_items WHERE partUuid = 'part-collision-2'").use {
                if (it.step()) childBomPartId2 = it.getLong(0)
            }
            healedConn.prepare("SELECT partId FROM stock_items WHERE partUuid = 'part-collision-2'").use {
                if (it.step()) childStockPartId2 = it.getLong(0)
            }
            healedConn.prepare("SELECT partId FROM part_pricing WHERE partUuid = 'part-collision-1'").use {
                if (it.step()) childPricingPartId1 = it.getLong(0)
            }
            healedConn.prepare("SELECT partId FROM part_pricing WHERE partUuid = 'part-collision-2'").use {
                if (it.step()) childPricingPartId2 = it.getLong(0)
            }
            assertEquals(id2, childBomPartId2, "بند BOM التابع للقطعة الثانية تم تحديثه لمعرف القطعة الجديد")
            assertEquals(id2, childStockPartId2, "عنصر المخزون التابع للقطعة الثانية تم تحديثه لمعرف القطعة الجديد")
            assertEquals(10L, childPricingPartId1, "سعر القطعة الأولى تم ربطه بالمعرف الأصلي 10")
            assertEquals(id2, childPricingPartId2, "سعر القطعة الثانية المكررة تم تحديث معرفه إلى المعرف الجديد المستحدث")
        } finally {
            SqliteDatabaseManager.closeDatabase()
            SqliteDatabaseManager.setCustomDatabasePath(tempDbFile.absolutePath)
            runCatching { tempFile.delete() }
        }
    }

    @Test
    fun testLegacyMigrationExplicitlyUpgradesPartPricing() {
        val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
        val tempFile = File(System.getProperty("java.io.tmpdir"), "legacy_pricing_migration_$runToken.db")
        tempFile.deleteOnExit()

        try {
            val driver = androidx.sqlite.driver.bundled.BundledSQLiteDriver()
            val raw = driver.open(tempFile.absolutePath)

            // Create legacy parts table and legacy part_pricing table (without id, without partId)
            raw.prepare("""
                CREATE TABLE parts (
                    uuid TEXT PRIMARY KEY NOT NULL,
                    id INTEGER NOT NULL DEFAULT 0,
                    name TEXT NOT NULL,
                    isDeleted INTEGER NOT NULL DEFAULT 0
                );
            """.trimIndent()).use { it.step() }

            raw.prepare("""
                CREATE TABLE part_pricing (
                    uuid TEXT PRIMARY KEY NOT NULL,
                    partUuid TEXT NOT NULL UNIQUE,
                    currency TEXT NOT NULL DEFAULT 'USD',
                    overallMin REAL,
                    overallMax REAL
                );
            """.trimIndent()).use { it.step() }

            raw.prepare("INSERT INTO parts (uuid, id, name, isDeleted) VALUES ('part-leg-1', 101, 'Legacy Resistor', 0)").use { it.step() }
            raw.prepare("INSERT INTO parts (uuid, id, name, isDeleted) VALUES ('part-leg-2', 102, 'Legacy Capacitor', 0)").use { it.step() }
            raw.prepare("INSERT INTO part_pricing (uuid, partUuid, overallMin, overallMax) VALUES ('pp-leg-1', 'part-leg-1', 1.5, 3.0)").use { it.step() }
            raw.prepare("INSERT INTO part_pricing (uuid, partUuid, overallMin, overallMax) VALUES ('pp-leg-2', 'part-leg-2', 2.0, 5.0)").use { it.step() }
            raw.close()

            // Open via SqliteDatabaseManager - should migrate part_pricing and backfill partId
            SqliteDatabaseManager.setCustomDatabasePath(tempFile.absolutePath)
            val migratedConn = SqliteDatabaseManager.getConnection()

            // Verify part_pricing has partId column and has been populated properly
            var pp1PartId = 0L
            var pp2PartId = 0L
            migratedConn.prepare("SELECT partId FROM part_pricing WHERE uuid = 'pp-leg-1'").use {
                if (it.step()) pp1PartId = it.getLong(0)
            }
            migratedConn.prepare("SELECT partId FROM part_pricing WHERE uuid = 'pp-leg-2'").use {
                if (it.step()) pp2PartId = it.getLong(0)
            }
            assertEquals(101L, pp1PartId, "ترحيل part_pricing قام بربط القطعة 101 بنجاح")
            assertEquals(102L, pp2PartId, "ترحيل part_pricing قام بربط القطعة 102 بنجاح")

            // Verify column existence via PRAGMA
            val columns = mutableSetOf<String>()
            migratedConn.prepare("PRAGMA table_info(part_pricing)").use { stmt ->
                while (stmt.step()) columns.add(stmt.getText(1))
            }
            assertTrue("id" in columns, "عمود id موجود في part_pricing بعد الترحيل")
            assertTrue("partId" in columns, "عمود partId موجود في part_pricing بعد الترحيل")
        } finally {
            SqliteDatabaseManager.closeDatabase()
            SqliteDatabaseManager.setCustomDatabasePath(tempDbFile.absolutePath)
            runCatching { tempFile.delete() }
        }
    }

    @Test
    fun testLegacyMigrationRollsBackAtomicallyOnFailure() {
        val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
        val tempFile = File(System.getProperty("java.io.tmpdir"), "legacy_rollback_test_$runToken.db")
        tempFile.deleteOnExit()

        try {
            val driver = androidx.sqlite.driver.bundled.BundledSQLiteDriver()
            val raw = driver.open(tempFile.absolutePath)

            // 1. Create a legacy database with parts and legacy part_pricing
            raw.prepare("""
                CREATE TABLE parts (
                    uuid TEXT PRIMARY KEY NOT NULL,
                    id INTEGER NOT NULL DEFAULT 0,
                    name TEXT NOT NULL,
                    isDeleted INTEGER NOT NULL DEFAULT 0
                );
            """.trimIndent()).use { it.step() }

            raw.prepare("""
                CREATE TABLE part_pricing (
                    uuid TEXT PRIMARY KEY NOT NULL,
                    partUuid TEXT NOT NULL UNIQUE,
                    currency TEXT NOT NULL DEFAULT 'USD',
                    overallMin REAL,
                    overallMax REAL
                );
            """.trimIndent()).use { it.step() }

            raw.prepare("INSERT INTO parts (uuid, id, name, isDeleted) VALUES ('part-fail-1', 501, 'Part Before Rollback', 0)").use { it.step() }
            raw.prepare("INSERT INTO part_pricing (uuid, partUuid, overallMin, overallMax) VALUES ('pp-fail-1', 'part-fail-1', 9.9, 19.9)").use { it.step() }

            // 2. Inject an intentional failure: attach a trigger to part_pricing that aborts any UPDATE
            raw.prepare("""
                CREATE TRIGGER fail_on_pricing_update 
                BEFORE UPDATE ON part_pricing 
                BEGIN 
                    SELECT RAISE(ABORT, 'Intentional failure during part_pricing migration update'); 
                END;
            """.trimIndent()).use { it.step() }
            raw.close()

            SqliteDatabaseManager.setCustomDatabasePath(tempFile.absolutePath)

            // 3. Opening via SqliteDatabaseManager MUST fail and trigger atomic ROLLBACK
            assertFailsWith<IllegalStateException>("يجب أن يفشل فتح قاعدة البيانات ويتراجع ذرياً عند حدوث خطأ أثناء الترحيل") {
                SqliteDatabaseManager.getConnection()
            }

            // 4. Verify that the rollback was clean and uncommitted DDL was reverted from schema
            val rawCheck = driver.open(tempFile.absolutePath)
            var partCount = 0L
            rawCheck.prepare("SELECT COUNT(*) FROM parts WHERE uuid = 'part-fail-1'").use {
                if (it.step()) partCount = it.getLong(0)
            }
            assertEquals(1L, partCount, "البيانات الأصلية لجدول القطع لم تُمس بعد التراجع الذري")

            var pricingCount = 0L
            rawCheck.prepare("SELECT COUNT(*) FROM part_pricing WHERE uuid = 'pp-fail-1'").use {
                if (it.step()) pricingCount = it.getLong(0)
            }
            assertEquals(1L, pricingCount, "البيانات الأصلية لجدول الأسعار لم تُمس بعد التراجع الذري")

            // Verify via PRAGMA table_info that uncommitted migration columns were rolled back completely
            val columnsAfterRollback = mutableSetOf<String>()
            rawCheck.prepare("PRAGMA table_info(part_pricing)").use { stmt ->
                while (stmt.step()) columnsAfterRollback.add(stmt.getText(1))
            }
            assertFalse("partId" in columnsAfterRollback, "عمود partId غير المعتمد يجب ألا يبقى في المخطط بعد التراجع الذري")
            assertFalse("id" in columnsAfterRollback, "عمود id غير المعتمد يجب ألا يبقى في المخطط بعد التراجع الذري")

            // 5. Heal the simulated failure by removing the failing trigger
            rawCheck.prepare("DROP TRIGGER fail_on_pricing_update;").use { it.step() }
            rawCheck.close()

            // 6. Re-attempt opening: migration should now succeed, commit columns, and backfill relations
            SqliteDatabaseManager.closeDatabase()
            val recoveredConn = SqliteDatabaseManager.getConnection()
            assertNotNull(recoveredConn)
            assertTrue(SqliteDatabaseManager.isDatabaseOpen())

            val columnsAfterRecovery = mutableSetOf<String>()
            recoveredConn.prepare("PRAGMA table_info(part_pricing)").use { stmt ->
                while (stmt.step()) columnsAfterRecovery.add(stmt.getText(1))
            }
            assertTrue("partId" in columnsAfterRecovery, "بعد التعافي، عمود partId تم اعتماده في المخطط بنجاح")
            assertTrue("id" in columnsAfterRecovery, "بعد التعافي، عمود id تم اعتماده في المخطط بنجاح")

            var recoveredPartId = 0L
            recoveredConn.prepare("SELECT partId FROM part_pricing WHERE uuid = 'pp-fail-1'").use {
                if (it.step()) recoveredPartId = it.getLong(0)
            }
            assertEquals(501L, recoveredPartId, "بعد زوال سبب الفشل، تم الترحيل بنجاح وربط معرّف القطعة 501")
        } finally {
            SqliteDatabaseManager.closeDatabase()
            SqliteDatabaseManager.setCustomDatabasePath(tempDbFile.absolutePath)
            runCatching { tempFile.delete() }
        }
    }

    @Test
    fun testMigrationHaltsWhenEssentialTableIsMissing() {
        val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
        val tempFile = File(System.getProperty("java.io.tmpdir"), "essential_table_test_$runToken.db")
        tempFile.deleteOnExit()

        try {
            val driver = androidx.sqlite.driver.bundled.BundledSQLiteDriver()
            val conn = driver.open(tempFile.absolutePath)

            // Essential table missing (unquoted, quoted, qualified, and uppercase): must throw IllegalStateException
            val err1 = assertFailsWith<IllegalStateException>("الترحيل يجب أن يتوقف إذا كان الجدول الأساسي مفقوداً") {
                SqliteDatabaseMigrations.addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN testCol INTEGER NOT NULL DEFAULT 0;")
            }
            assertTrue(err1.message.orEmpty().contains("essential table 'parts'", ignoreCase = true))

            val err2 = assertFailsWith<IllegalStateException>("الترحيل يجب أن يتوقف إذا كان الجدول الأساسي المقتبس مفقوداً") {
                SqliteDatabaseMigrations.addColumnIfMissing(conn, "ALTER TABLE \"companies\" ADD COLUMN testCol INTEGER NOT NULL DEFAULT 0;")
            }
            assertTrue(err2.message.orEmpty().contains("essential table 'companies'", ignoreCase = true))

            val err3 = assertFailsWith<IllegalStateException>("الترحيل يجب أن يتوقف إذا كان الجدول الأساسي المؤهل مفقوداً") {
                SqliteDatabaseMigrations.addColumnIfMissing(conn, "ALTER TABLE main.[part_pricing] ADD COLUMN testCol INTEGER NOT NULL DEFAULT 0;")
            }
            assertTrue(err3.message.orEmpty().contains("essential table 'part_pricing'", ignoreCase = true))

            val errUppercase = assertFailsWith<IllegalStateException>("الترحيل يجب أن يتوقف إذا صيغ اسم الجدول الأساسي بأحرف كبيرة Case-Insensitive") {
                SqliteDatabaseMigrations.addColumnIfMissing(conn, "ALTER TABLE \"PARTS\" ADD COLUMN testCol INTEGER NOT NULL DEFAULT 0;")
            }
            assertTrue(errUppercase.message.orEmpty().contains("essential table 'PARTS'", ignoreCase = true))

            // Unclassified / unknown table missing: must halt migration (fail-closed)
            val errUnclassified = assertFailsWith<IllegalStateException>("الترحيل يجب أن يتوقف إذا كان الجدول غير مصنف Fail-Closed") {
                SqliteDatabaseMigrations.addColumnIfMissing(conn, "ALTER TABLE unclassified_table_xyz ADD COLUMN testCol INTEGER NOT NULL DEFAULT 0;")
            }
            assertTrue(errUnclassified.message.orEmpty().contains("unclassified table 'unclassified_table_xyz'", ignoreCase = true))

            // Explicitly classified optional table missing: must safely ignore without throwing
            SqliteDatabaseMigrations.registerOptionalTable("test_optional_table")
            try {
                SqliteDatabaseMigrations.addColumnIfMissing(conn, "ALTER TABLE test_optional_table ADD COLUMN testCol INTEGER NOT NULL DEFAULT 0;")
            } finally {
                SqliteDatabaseMigrations.unregisterOptionalTable("test_optional_table")
            }
            conn.close()

            // Verify extractTableName unit tests
            assertEquals("parts", SqliteDatabaseMigrations.extractTableName("ALTER TABLE parts ADD COLUMN c INT"))
            assertEquals("parts", SqliteDatabaseMigrations.extractTableName("ALTER TABLE \"parts\" ADD COLUMN c INT"))
            assertEquals("parts", SqliteDatabaseMigrations.extractTableName("ALTER TABLE `parts` ADD COLUMN c INT"))
            assertEquals("parts", SqliteDatabaseMigrations.extractTableName("ALTER TABLE [parts] ADD COLUMN c INT"))
            assertEquals("parts", SqliteDatabaseMigrations.extractTableName("ALTER TABLE main.\"parts\" ADD COLUMN c INT"))
            assertEquals("parts", SqliteDatabaseMigrations.extractTableName("ALTER TABLE \"main\".[parts] ADD COLUMN c INT"))
            assertNull(SqliteDatabaseMigrations.extractTableName("SELECT * FROM parts"))
        } finally {
            runCatching { tempFile.delete() }
        }
    }

    @Test
    fun testIndependentConnectionsConcurrentAllocationSerializesWithoutCollisions() {
        val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
        val multiConnDb = File(System.getProperty("java.io.tmpdir"), "multi_conn_$runToken.db")
        multiConnDb.deleteOnExit()

        try {
            SqliteDatabaseManager.setCustomDatabasePath(multiConnDb.absolutePath)
            val initConn = SqliteDatabaseManager.getConnection() // Initialize schema and tables
            initConn.prepare("INSERT OR REPLACE INTO id_sequences (table_name, last_id) VALUES ('parts', 0);").use { it.step() }
            SqliteDatabaseManager.closeDatabase()

            val driver = androidx.sqlite.driver.bundled.BundledSQLiteDriver()
            val conn1 = driver.open(multiConnDb.absolutePath)
            val conn2 = driver.open(multiConnDb.absolutePath)
            conn1.prepare("PRAGMA busy_timeout = 10000;").use { it.step() }
            conn2.prepare("PRAGMA busy_timeout = 10000;").use { it.step() }

            val totalPerConn = 25
            val latch = CountDownLatch(2)
            val allocatedConn1 = Collections.synchronizedList(mutableListOf<Long>())
            val allocatedConn2 = Collections.synchronizedList(mutableListOf<Long>())

            val thread1 = Thread {
                try {
                    for (i in 0 until totalPerConn) {
                        val id = SqliteNumericIdAllocator.nextId("parts", "part-", connection = conn1)
                        allocatedConn1.add(id)
                    }
                } finally {
                    latch.countDown()
                }
            }

            val thread2 = Thread {
                try {
                    for (i in 0 until totalPerConn) {
                        val id = SqliteNumericIdAllocator.nextId("parts", "part-", connection = conn2)
                        allocatedConn2.add(id)
                    }
                } finally {
                    latch.countDown()
                }
            }

            thread1.start()
            thread2.start()

            assertTrue(latch.await(15, TimeUnit.SECONDS), "انتهت مهلة التزامن بين اتصالين مستقلين")
            conn1.close()
            conn2.close()

            assertEquals(totalPerConn, allocatedConn1.size)
            assertEquals(totalPerConn, allocatedConn2.size)

            val allAllocated = (allocatedConn1 + allocatedConn2).toSet()
            assertEquals(totalPerConn * 2, allAllocated.size, "لا يجوز حدوث أي تصادم بين معرفات الاتصالين المستقلين")
        } finally {
            SqliteDatabaseManager.closeDatabase()
            SqliteDatabaseManager.setCustomDatabasePath(tempDbFile.absolutePath)
            runCatching { multiConnDb.delete() }
        }
    }
}
