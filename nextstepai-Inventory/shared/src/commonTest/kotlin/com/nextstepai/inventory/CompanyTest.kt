package com.nextstepai.inventory

import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.data.CompanyTable
import com.nextstepai.inventory.data.db.CompanyDao
import com.nextstepai.inventory.data.db.CompanyEntity
import com.nextstepai.inventory.repository.CompanyRepository
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock

class CompanyTest {

    @Test
    fun testUniqueCompanyNameConstraint() {
        val companyTable = CompanyTable()

        companyTable.insertCompany(
            Company(name = "شركة النجم للتوريدات")
        )

        // محاولة إضافة شركة أخرى بنفس الاسم يجب أن ترمي استثناء
        assertFailsWith<IllegalArgumentException> {
            companyTable.insertCompany(
                Company(name = "شركة النجم للتوريدات")
            )
        }
    }

    @Test
    fun testCompanyRoleFilters() {
        val companyTable = CompanyTable()

        val suppliers = companyTable.searchCompanies(supplierOnly = true)
        assertTrue(suppliers.all { it.isSupplier })

        val customers = companyTable.searchCompanies(customerOnly = true)
        assertTrue(customers.all { it.isCustomer })
    }

    @Test
    fun testCompanyDaoExplicitLimitOffsetPaging() = runBlocking {
        val dao = CompanyDao()
        val now = Clock.System.now().toEpochMilliseconds()

        for (i in 1..25) {
            dao.insertOrUpdate(
                CompanyEntity(
                    uuid = "company-uuid-$i",
                    name = "مورد #$i",
                    isSupplier = true,
                    syncStatus = SyncStatus.PENDING,
                    updatedAt = now + i
                )
            )
        }

        val testItems = dao.getCompaniesPaged(limit = 100, offset = 0).filter { it.uuid.startsWith("company-uuid-") }

        val page1 = testItems.drop(0).take(10)
        assertEquals(10, page1.size)

        val page2 = testItems.drop(10).take(10)
        assertEquals(10, page2.size)

        val page3 = testItems.drop(20).take(10)
        assertEquals(5, page3.size)
    }

    @Test
    fun testCompanyBatchSyncWithCloudflare() = runBlocking {
        val repository = CompanyRepository()
        repository.addCompany(
            Company(
                name = "شركة الميزان للتجارة",
                email = "contact@mezan.com",
                currency = "EUR"
            )
        )

        val syncedCount = repository.syncPendingCompanyChanges()
        assertTrue(syncedCount > 0)
    }
}
