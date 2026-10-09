package com.nextstepai.inventory

import com.nextstepai.inventory.data.*
import com.nextstepai.inventory.data.db.*
import com.nextstepai.inventory.repository.CompanyRepository
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.Clock

class CompanyEntitiesTest {

    @Test
    fun testManufacturerPartUniqueConstraintAndRole() {
        val mfgTable = ManufacturerPartTable()

        // إضافة قطعة مصنّع أولى
        val p1 = mfgTable.insertManufacturerPart(
            ManufacturerPart(partId = 10, manufacturerId = 2, mpn = "ESP32-WROOM-32D"),
            isManufacturerCompany = true
        )
        assertEquals(10, p1.partId)

        // محاولة تكرار نفس MPN لنفس المصنّع يجب أن تفشل
        assertFailsWith<IllegalArgumentException> {
            mfgTable.insertManufacturerPart(
                ManufacturerPart(partId = 11, manufacturerId = 2, mpn = "esp32-wroom-32d"),
                isManufacturerCompany = true
            )
        }

        // محاولة ربط شركة ليست مصنّعاً يجب أن تفشل
        assertFailsWith<IllegalArgumentException> {
            mfgTable.insertManufacturerPart(
                ManufacturerPart(partId = 12, manufacturerId = 3, mpn = "OTHER-MPN"),
                isManufacturerCompany = false
            )
        }
    }

    @Test
    fun testSupplierPartUniqueConstraintAndRole() {
        val supTable = SupplierPartTable()

        // إضافة قطعة مورد أولى
        val sp1 = supTable.insertSupplierPart(
            SupplierPart(partId = 10, supplierId = 1, sku = "DIGI-ESP32-01"),
            isSupplierCompany = true
        )
        assertEquals("DIGI-ESP32-01", sp1.sku)

        // محاولة تكرار نفس SKU لنفس المورد يجب أن تفشل
        assertFailsWith<IllegalArgumentException> {
            supTable.insertSupplierPart(
                SupplierPart(partId = 10, supplierId = 1, sku = "digi-esp32-01"),
                isSupplierCompany = true
            )
        }

        // محاولة إضافة قطعة مورد لشركة ليست مورداً يجب أن تفشل
        assertFailsWith<IllegalArgumentException> {
            supTable.insertSupplierPart(
                SupplierPart(partId = 10, supplierId = 5, sku = "NEW-SKU"),
                isSupplierCompany = false
            )
        }
    }

    @Test
    fun testSupplierPriceBreakTiersAndBestPriceCalculation() {
        val priceTable = SupplierPriceBreakTable()

        priceTable.insertPriceBreak(SupplierPriceBreak(supplierPartId = 1, quantity = 1.0, price = 5.0))
        priceTable.insertPriceBreak(SupplierPriceBreak(supplierPartId = 1, quantity = 10.0, price = 4.2))
        priceTable.insertPriceBreak(SupplierPriceBreak(supplierPartId = 1, quantity = 100.0, price = 3.5))

        // محاولة إضافة شريحة سعر مكررة لنفس الكمية يجب أن تفشل
        assertFailsWith<IllegalArgumentException> {
            priceTable.insertPriceBreak(SupplierPriceBreak(supplierPartId = 1, quantity = 10.0, price = 4.0))
        }

        // اختبار السعر للكمية 1 -> يختار شريحة 1.0 بسعر 5.0
        val priceFor1 = priceTable.getBestPriceForQuantity(supplierPartId = 1, orderQty = 1.0)
        assertNotNull(priceFor1)
        assertEquals(5.0, priceFor1.price)

        // اختبار السعر للكمية 25 -> يختار شريحة 10.0 بسعر 4.2
        val priceFor25 = priceTable.getBestPriceForQuantity(supplierPartId = 1, orderQty = 25.0)
        assertNotNull(priceFor25)
        assertEquals(4.2, priceFor25.price)

        // اختبار السعر للكمية 500 -> يختار شريحة 100.0 بسعر 3.5
        val priceFor500 = priceTable.getBestPriceForQuantity(supplierPartId = 1, orderQty = 500.0)
        assertNotNull(priceFor500)
        assertEquals(3.5, priceFor500.price)
    }

    @Test
    fun testPriceBreakDefaultCompanyCurrencyFallback() {
        val priceTable = SupplierPriceBreakTable()

        // عند ترك حقل العملة فارغاً يتم تطبيق العملة الافتراضية للشركة (مثل SAR)
        val pb = priceTable.insertPriceBreak(
            SupplierPriceBreak(supplierPartId = 10, quantity = 5.0, price = 15.0, priceCurrency = ""),
            defaultCompanyCurrency = "SAR"
        )
        assertEquals("SAR", pb.priceCurrency)
    }

    @Test
    fun testPrimaryAddressResetLogic() {
        val addressTable = AddressTable()

        val addr1 = addressTable.insertAddress(
            Address(companyId = 1, title = "الفرع الأول", isPrimary = true, line1 = "شارع المطار")
        )
        assertTrue(addr1.isPrimary)

        // إضافة عنوان ثاني وتحديده كرئيسي
        val addr2 = addressTable.insertAddress(
            Address(companyId = 1, title = "المستودع الرئيسي", isPrimary = true, line1 = "المنطقة الصناعية")
        )
        assertTrue(addr2.isPrimary)

        // التأكد من أن العنوان الأول لم يعد رئيساً
        val primary = addressTable.getPrimaryAddressForCompany(1)
        assertNotNull(primary)
        assertEquals("المستودع الرئيسي", primary.title)
        assertEquals(addr2.id, primary.id)

        val allAddresses = addressTable.getAddressesForCompany(1)
        val nonPrimary = allAddresses.find { it.id == addr1.id }
        assertNotNull(nonPrimary)
        assertFalse(nonPrimary.isPrimary)
    }

    @Test
    fun testSqlitePrimaryAddressPartialUniqueIndex() = runBlocking {
        val addressDao = AddressDao()
        val compUuid = "company-partial-index-test"
        val now = Clock.System.now().toEpochMilliseconds()

        // حفظ عنوان أول ورئيسي عبر الـ DAO
        addressDao.insertOrUpdate(
            AddressEntity(
                uuid = "addr-p-1",
                companyUuid = compUuid,
                title = "المستودع الأول",
                isPrimary = true,
                line1 = "شارع العليا",
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
        )

        // إضافة عنوان ثاني كرئيسي عبر الـ DAO يصفر العنوان الأول آلياً ويمنع تكرار isPrimary = 1
        addressDao.insertOrUpdate(
            AddressEntity(
                uuid = "addr-p-2",
                companyUuid = compUuid,
                title = "المقر العام",
                isPrimary = true,
                line1 = "طريق الملك عبدالله",
                syncStatus = SyncStatus.PENDING,
                updatedAt = now + 100
            )
        )

        val addresses = addressDao.getAddressesForCompany(compUuid)
        assertEquals(2, addresses.size)
        val primaryCount = addresses.count { it.isPrimary }
        assertEquals(1, primaryCount)
        assertEquals("المقر العام", addresses.first { it.isPrimary }.title)
    }

    @Test
    fun testCompanySelfParentValidationConstraint() {
        val companyTable = CompanyTable()

        val comp = companyTable.insertCompany(Company(name = "شركة العالمية للحلول"))
        assertEquals(comp.id, comp.id)

        // محاولة تحديث الشركة واختيار نفسها كشركة أم يجب أن يرمي استثناء
        assertFailsWith<IllegalArgumentException> {
            companyTable.updateCompany(comp.copy(parentId = comp.id))
        }
    }

    @Test
    fun testSqliteCompositeUniqueIndexes() = runBlocking {
        val mfgDao = ManufacturerPartDao()
        val supDao = SupplierPartDao()

        val compUuid = "company-unique-test"
        val now = Clock.System.now().toEpochMilliseconds()

        // ManufacturerPart Unique Constraint (manufacturerUuid + mpn)
        mfgDao.insertOrUpdate(
            ManufacturerPartEntity(
                uuid = "mfg-u-1",
                partUuid = "part-1",
                manufacturerUuid = compUuid,
                mpn = "MPN-UNIQUE-01",
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
        )

        // SupplierPart Unique Constraint (supplierUuid + sku)
        supDao.insertOrUpdate(
            SupplierPartEntity(
                uuid = "sup-u-1",
                partUuid = "part-1",
                supplierUuid = compUuid,
                sku = "SKU-UNIQUE-01",
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
        )

        val mfgList = mfgDao.getForCompany(compUuid)
        assertEquals(1, mfgList.size)
        assertEquals("MPN-UNIQUE-01", mfgList.first().mpn)

        val supList = supDao.getForCompany(compUuid)
        assertEquals(1, supList.size)
        assertEquals("SKU-UNIQUE-01", supList.first().sku)
    }

    @Test
    fun testManufacturerPartParameterUniqueConstraintAndDao() = runBlocking {
        val paramTable = ManufacturerPartParameterTable()
        val paramDao = ManufacturerPartParameterDao()

        // إضافة معامل فني أول
        paramTable.insertParameter(
            ManufacturerPartParameter(manufacturerPartId = 1, name = "Voltage", value = "5V", units = "V")
        )

        // محاولة إضافة نفس المعامل لنفس قطعة المصنّع يجب أن تفشل
        assertFailsWith<IllegalArgumentException> {
            paramTable.insertParameter(
                ManufacturerPartParameter(manufacturerPartId = 1, name = "voltage", value = "3.3V", units = "V")
            )
        }

        // اختبار الـ DAO
        paramDao.insertOrUpdate(
            ManufacturerPartParameterEntity(
                uuid = "param-1",
                manufacturerPartUuid = "mfg-part-1",
                name = "Resistance",
                value = "10k",
                units = "Ohm",
                syncStatus = SyncStatus.PENDING
            )
        )
        val list = paramDao.getForManufacturerPart("mfg-part-1")
        assertEquals(1, list.size)
        assertEquals("Resistance", list.first().name)
    }

    @Test
    fun testCompanyAttachmentsTableAndDao() = runBlocking {
        val repo = CompanyRepository()
        val compAttDao = CompanyAttachmentDao()

        val company = repo.addCompany(Company(name = "مؤسسة الأفق للتجارة ${Clock.System.now().toEpochMilliseconds()}"))
        val att = repo.addCompanyAttachment(
            CompanyAttachment(companyId = company.id, attachmentPath = "/docs/cr.pdf", comment = "السجل التجاري")
        )
        assertEquals("السجل التجاري", att.comment)

        val list = repo.getAttachmentsForCompany(company.id)
        assertEquals(1, list.size)

        // DAO Persistence
        compAttDao.insertOrUpdate(
            CompanyAttachmentEntity(
                uuid = "comp-att-2",
                companyUuid = "company-99",
                attachmentPath = "/docs/tax.pdf",
                comment = "الشهادة الضريبية",
                uploadDate = Clock.System.now().toEpochMilliseconds(),
                syncStatus = SyncStatus.PENDING
            )
        )
        val daoList = compAttDao.getForCompany("company-99")
        assertEquals(1, daoList.size)
        assertEquals("الشهادة الضريبية", daoList.first().comment)
    }

    @Test
    fun testValidateSupplierForPurchaseOrderConstraint() {
        val repo = CompanyRepository()
        val token = Clock.System.now().toEpochMilliseconds()

        // إضافة شركة كمصنّع فقط (ليست مورداً)
        val nonSupplier = repo.addCompany(
            Company(name = "مصنع الأمل التقني $token", isSupplier = false, isManufacturer = true)
        )

        // التحقق من أن اختيار هذه الشركة لأمر الشراء يرمي استثناء
        assertFailsWith<IllegalArgumentException> {
            repo.validateSupplierForPurchaseOrder(nonSupplier.id)
        }

        // إضافة شركة كمورد
        val supplier = repo.addCompany(
            Company(name = "مؤسسة التوريد الحديثة $token", isSupplier = true)
        )
        assertTrue(repo.validateSupplierForPurchaseOrder(supplier.id))
    }

    @Test
    fun testManufacturerPartAttachmentTableAndDao() = runBlocking {
        val repo = CompanyRepository()
        val mfgAttDao = ManufacturerPartAttachmentDao()

        val mfgCompany = repo.addCompany(Company(name = "STMicroelectronics ${Clock.System.now().toEpochMilliseconds()}", isManufacturer = true))
        val mfgPart = repo.addManufacturerPart(
            ManufacturerPart(partId = 1, manufacturerId = mfgCompany.id, mpn = "STM32F407VGT6")
        )

        // إضافة مرفق عبر الـ Repository
        val att = repo.addManufacturerPartAttachment(
            ManufacturerPartAttachment(
                manufacturerPartId = mfgPart.id,
                attachmentPath = "/datasheets/stm32f407.pdf",
                comment = "STM32F4 Datasheet Rev 8"
            )
        )
        assertNotNull(att)
        assertEquals("STM32F4 Datasheet Rev 8", att.comment)

        val attachments = repo.getAttachmentsForManufacturerPart(mfgPart.id)
        assertEquals(1, attachments.size)

        // التحقق عبر الـ DAO لقطعة أسطوانية منفصلة
        mfgAttDao.insertOrUpdate(
            ManufacturerPartAttachmentEntity(
                uuid = "mfg-att-test-2",
                manufacturerPartUuid = "mfg-part-dao-unique-99",
                attachmentPath = "/path/to/cert.pdf",
                comment = "RoHS Certificate",
                uploadDate = Clock.System.now().toEpochMilliseconds(),
                syncStatus = SyncStatus.PENDING
            )
        )

        val daoList = mfgAttDao.getForManufacturerPart("mfg-part-dao-unique-99")
        assertEquals(1, daoList.size)
        assertEquals("RoHS Certificate", daoList.first().comment)
    }

    @Test
    fun testRepositoryIntegrationForContactsAndAddresses() {
        val repo = CompanyRepository()
        val company = repo.addCompany(
            Company(name = "شركة الأمل البرمجية ${Clock.System.now().toEpochMilliseconds()}", isSupplier = true, isManufacturer = true, website = "https://alamal.com", notes = "ملاحظات هامة")
        )
        assertEquals("https://alamal.com", company.website)
        assertEquals("ملاحظات هامة", company.notes)

        // إضافة جهة اتصال
        repo.addContact(
            Contact(companyId = company.id, name = "م. خالد سعيد", role = "مدير المشتريات", email = "khalid@alamal.com")
        )
        val contacts = repo.getContactsForCompany(company.id)
        assertTrue(contacts.any { it.name == "م. خالد سعيد" })

        // إضافة قطعة مصنع
        val mfgPart = repo.addManufacturerPart(
            ManufacturerPart(partId = 5, manufacturerId = company.id, mpn = "AML-MCU-01")
        )
        val mfgParts = repo.getManufacturerPartsForCompany(company.id)
        assertTrue(mfgParts.any { it.mpn == "AML-MCU-01" })

        // إضافة قطعة مورد وشرائح أسعار
        val supPart = repo.addSupplierPart(
            SupplierPart(partId = 5, supplierId = company.id, sku = "SUP-AML-01", manufacturerPartId = mfgPart.id)
        )
        repo.addPriceBreak(SupplierPriceBreak(supplierPartId = supPart.id, quantity = 50.0, price = 12.0))

        val bestPrice = repo.getBestPriceForQuantity(supPart.id, 100.0)
        assertNotNull(bestPrice)
        assertEquals(12.0, bestPrice.price)

        // حذف قطعة المورد
        val deleted = repo.deleteSupplierPart(supPart.id)
        assertTrue(deleted)
        assertTrue(repo.getSupplierPartsForCompany(company.id).isEmpty())
    }

    @Test
    fun testDaosPersistenceForSubEntities() = runBlocking {
        val contactDao = ContactDao()
        val addressDao = AddressDao()
        val mfgDao = ManufacturerPartDao()
        val supDao = SupplierPartDao()
        val priceDao = SupplierPriceBreakDao()

        val compUuid = "company-test-uuid-99"
        val now = Clock.System.now().toEpochMilliseconds()

        // Contact DAO
        contactDao.insertOrUpdate(
            ContactEntity(
                uuid = "contact-test-1",
                companyUuid = compUuid,
                name = "أحمد المسعود",
                phone = "0501234567",
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
        )
        val contacts = contactDao.getContactsForCompany(compUuid)
        assertTrue(contacts.any { it.name == "أحمد المسعود" })

        // Address DAO
        addressDao.insertOrUpdate(
            AddressEntity(
                uuid = "address-test-1",
                companyUuid = compUuid,
                title = "المقر الإقليمي",
                isPrimary = true,
                line1 = "طريق الملك فهد",
                city = "الرياض",
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
        )
        val addresses = addressDao.getAddressesForCompany(compUuid)
        assertTrue(addresses.any { it.isPrimary && it.title == "المقر الإقليمي" })

        // ManufacturerPart DAO
        mfgDao.insertOrUpdate(
            ManufacturerPartEntity(
                uuid = "mfg-part-test-1",
                partUuid = "part-1",
                manufacturerUuid = compUuid,
                mpn = "MPN-TEST-99",
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
        )
        val mfgList = mfgDao.getForCompany(compUuid)
        assertTrue(mfgList.any { it.mpn == "MPN-TEST-99" })

        // SupplierPart DAO
        supDao.insertOrUpdate(
            SupplierPartEntity(
                uuid = "sup-part-test-1",
                partUuid = "part-1",
                supplierUuid = compUuid,
                sku = "SKU-TEST-99",
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
        )
        val supList = supDao.getForCompany(compUuid)
        assertTrue(supList.any { it.sku == "SKU-TEST-99" })

        // SupplierPriceBreak DAO
        priceDao.insertOrUpdate(
            SupplierPriceBreakEntity(
                uuid = "price-break-test-1",
                supplierPartUuid = "sup-part-test-1",
                quantity = 100.0,
                price = 2.75,
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
        )
        val priceList = priceDao.getForSupplierPart("sup-part-test-1")
        assertEquals(1, priceList.size)
        assertEquals(2.75, priceList.first().price)
    }

    @Test
    fun testCompanyBankAccountTableAndDao() {
        val table = CompanyBankAccountTable()

        val acc1 = table.insertBankAccount(
            CompanyBankAccount(
                companyId = 1L,
                bankName = "الراجحي",
                accountName = "شركة التقنيات المتقدمة",
                iban = "SA1122334455667788990000",
                currency = "SAR",
                isPrimary = true
            )
        )
        assertEquals(1L, acc1.id)
        assertTrue(acc1.isPrimary)

        val acc2 = table.insertBankAccount(
            CompanyBankAccount(
                companyId = 1L,
                bankName = "البنك الأهلي",
                accountName = "شركة التقنيات المتقدمة",
                iban = "SA9988776655443322110000",
                currency = "USD",
                isPrimary = true
            )
        )
        val accounts = table.getBankAccountsForCompany(1L)
        assertEquals(2, accounts.size)

        val updatedAcc1 = accounts.find { it.id == acc1.id }
        val updatedAcc2 = accounts.find { it.id == acc2.id }
        assertFalse(updatedAcc1!!.isPrimary)
        assertTrue(updatedAcc2!!.isPrimary)

        val deleted = table.deleteBankAccount(acc1.id)
        assertTrue(deleted)
        assertEquals(1, table.getBankAccountsForCompany(1L).size)
    }
}
