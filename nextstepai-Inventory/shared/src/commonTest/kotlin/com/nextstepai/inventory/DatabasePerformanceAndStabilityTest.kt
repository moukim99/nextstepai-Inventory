package com.nextstepai.inventory

import com.nextstepai.inventory.data.*
import com.nextstepai.inventory.data.db.*
import com.nextstepai.inventory.repository.CompanyRepository
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

/**
 * اختبارات الاستقرار والسرعة والأداء عالي الكثافة لقاعدة البيانات والطبقات المحلية (Database Performance & Stability Benchmark).
 */
class DatabasePerformanceAndStabilityTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun testDatabaseInitializationAndConnectionSpeed() {
        val duration = measureTime {
            val conn = SqliteDatabaseManager.getConnection()
            assertNotNull(conn)
        }
        // التأكد من أن عملية الاتصال وتهيئة جميع الجداول والفهارس تتم في أقل من 500 ملي ثانية
        assertTrue(duration.inWholeMilliseconds < 500, "استغرق اتصال قاعدة البيانات وتهيئة الجداول زماً أطول من المتوقع: ${duration.inWholeMilliseconds} ms")
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testHighVolumeCompanyInsertionAndQueryLatency() = runBlocking {
        val companyDao = CompanyDao()
        val count = 100
        val now = Clock.System.now().toEpochMilliseconds()

        val insertDuration = measureTime {
            for (i in 1..count) {
                companyDao.insertOrUpdate(
                    CompanyEntity(
                        uuid = "perf-company-$i",
                        name = "شركة الأداء العالي رقم $i",
                        description = "توصيف أداء واختبار السرعة",
                        website = "https://perf$i.example.com",
                        phone = "05000000$i",
                        email = "info@perf$i.com",
                        isSupplier = i % 2 == 0,
                        isManufacturer = i % 3 == 0,
                        isCustomer = true,
                        active = true,
                        currency = "SAR",
                        notes = "اختبار الضغط $i",
                        syncStatus = SyncStatus.PENDING,
                        updatedAt = now + i
                    )
                )
            }
        }

        // اختبار سرعة الاستعلام للصفحات (Paged Query Latency)
        var pagedCompanies: List<CompanyEntity>
        val queryDuration = measureTime {
            pagedCompanies = companyDao.getCompaniesPaged(limit = 50, offset = 0)
        }

        assertEquals(50, pagedCompanies.size)
        assertTrue(insertDuration.inWholeMilliseconds < 2000, "استغرقت عملية إدخال $count شركة وقتاً أطول من المتوقع: ${insertDuration.inWholeMilliseconds} ms")
        assertTrue(queryDuration.inWholeMilliseconds < 100, "استغرق استعلام الصفحة وقتاً أطول من المتوقع: ${queryDuration.inWholeMilliseconds} ms")
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testSubEntitiesQueryPerformanceAndIndexes() = runBlocking {
        val contactDao = ContactDao()
        val addressDao = AddressDao()
        val supDao = SupplierPartDao()
        val priceDao = SupplierPriceBreakDao()

        val compUuid = "perf-comp-main"
        val now = Clock.System.now().toEpochMilliseconds()

        val subEntityDuration = measureTime {
            // إدراج 50 جهة اتصال وعنوان وقطعة مورد وسعر
            for (i in 1..50) {
                contactDao.insertOrUpdate(
                    ContactEntity(
                        uuid = "perf-contact-$i",
                        companyUuid = compUuid,
                        name = "مسؤول تواصل $i",
                        phone = "05555555$i",
                        email = "contact$i@test.com",
                        role = "مدير المبيعات $i",
                        syncStatus = SyncStatus.PENDING,
                        updatedAt = now
                    )
                )

                addressDao.insertOrUpdate(
                    AddressEntity(
                        uuid = "perf-address-$i",
                        companyUuid = compUuid,
                        title = "فرع رقم $i",
                        isPrimary = (i == 1),
                        line1 = "شارع $i",
                        city = "الرياض",
                        country = "السعودية",
                        syncStatus = SyncStatus.PENDING,
                        updatedAt = now
                    )
                )

                supDao.insertOrUpdate(
                    SupplierPartEntity(
                        uuid = "perf-sup-part-$i",
                        partUuid = "part-100",
                        supplierUuid = compUuid,
                        sku = "PERF-SKU-$i",
                        syncStatus = SyncStatus.PENDING,
                        updatedAt = now
                    )
                )

                priceDao.insertOrUpdate(
                    SupplierPriceBreakEntity(
                        uuid = "perf-price-$i",
                        supplierPartUuid = "perf-sup-part-$i",
                        quantity = i * 10.0,
                        price = 100.0 / i,
                        priceCurrency = "SAR",
                        syncStatus = SyncStatus.PENDING,
                        updatedAt = now
                    )
                )
            }
        }

        // قياس استرجاع البيانات المجهزة عبر الفهارس المخصصة (Indexed Query Speed)
        val contacts: List<ContactEntity>
        val addresses: List<AddressEntity>
        val supParts: List<SupplierPartEntity>

        val fetchDuration = measureTime {
            contacts = contactDao.getContactsForCompany(compUuid)
            addresses = addressDao.getAddressesForCompany(compUuid)
            supParts = supDao.getForCompany(compUuid)
        }

        assertEquals(50, contacts.size)
        assertEquals(50, addresses.size)
        assertEquals(50, supParts.size)
        assertTrue(subEntityDuration.inWholeMilliseconds < 2500, "إدراج الكيانات الفرعية استغرق زمن: ${subEntityDuration.inWholeMilliseconds} ms")
        assertTrue(fetchDuration.inWholeMilliseconds < 100, "استرجاع الكيانات الفرعية المبوّبة عبر الفهارس استغرق زمن: ${fetchDuration.inWholeMilliseconds} ms")
    }

    @Test
    fun testRepositoryStabilityAndBusinessLogicConstraints() {
        val repo = CompanyRepository()

        // اختبار استقرار العمليات المتتالية والتكامل للـ Repository
        val company = repo.addCompany(
            Company(name = "شركة الاختبار والضغوطات", isSupplier = true, isManufacturer = true)
        )

        val mfgPart = repo.addManufacturerPart(
            ManufacturerPart(partId = 1, manufacturerId = company.id, mpn = "STABILITY-MPN-01")
        )

        val supPart = repo.addSupplierPart(
            SupplierPart(partId = 1, supplierId = company.id, sku = "STABILITY-SKU-01", manufacturerPartId = mfgPart.id)
        )

        repo.addPriceBreak(SupplierPriceBreak(supplierPartId = supPart.id, quantity = 1.0, price = 50.0))
        repo.addPriceBreak(SupplierPriceBreak(supplierPartId = supPart.id, quantity = 10.0, price = 45.0))
        repo.addPriceBreak(SupplierPriceBreak(supplierPartId = supPart.id, quantity = 100.0, price = 38.0))

        val bestPrice1 = repo.getBestPriceForQuantity(supPart.id, 5.0)
        assertEquals(50.0, bestPrice1?.price)

        val bestPrice15 = repo.getBestPriceForQuantity(supPart.id, 15.0)
        assertEquals(45.0, bestPrice15?.price)

        val bestPrice200 = repo.getBestPriceForQuantity(supPart.id, 200.0)
        assertEquals(38.0, bestPrice200?.price)
    }
}
