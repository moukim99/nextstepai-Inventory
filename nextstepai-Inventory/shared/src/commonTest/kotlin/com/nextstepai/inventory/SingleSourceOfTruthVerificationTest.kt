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
}
