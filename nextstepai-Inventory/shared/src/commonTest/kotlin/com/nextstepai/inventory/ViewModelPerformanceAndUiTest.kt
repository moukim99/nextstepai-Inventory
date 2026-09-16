package com.nextstepai.inventory

import com.nextstepai.inventory.ui.*
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

/**
 * اختبارات الأداء والسرعة لطبقة نماذج العرض والمنطق التجاري (ViewModel Performance & UI Logic Benchmark).
 */
class ViewModelPerformanceAndUiTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun testCompanyViewModelSearchAndFilterPerformance() {
        val viewModel = CompanyViewModel()

        val searchDuration = measureTime {
            viewModel.onSearchQueryChanged("تقنية")
            viewModel.setRoleFilter(CompanyRoleFilter.SUPPLIER_ONLY)
            viewModel.onSearchQueryChanged("")
            viewModel.setRoleFilter(CompanyRoleFilter.ALL)
        }

        val state = viewModel.uiState.value
        assertNotNull(state.companies)
        assertTrue(searchDuration.inWholeMilliseconds < 200, "استغرق التصفية والبحث في نموذج العرض وقتاً أطول من المتوقع: ${searchDuration.inWholeMilliseconds} ms")
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testCompanyViewModelCrudOperationsSpeed() {
        val viewModel = CompanyViewModel()

        val duration = measureTime {
            viewModel.addCompany(
                name = "شركة النخبة التقنية للتوريد",
                description = "مورد أداء عالي",
                website = "https://elite.com",
                phone = "0500000000",
                email = "info@elite.com",
                address = "الرياض",
                contact = "م. عبد العزيز",
                isSupplier = true,
                isManufacturer = true,
                isCustomer = false,
                currency = "SAR"
            )

            val addedCompany = viewModel.uiState.value.companies.find { it.name == "شركة النخبة التقنية للتوريد" }
            assertNotNull(addedCompany)

            viewModel.setSelectedCompany(addedCompany)
            viewModel.addContact("م. عبد العزيز", "0590000000", "abdulaziz@elite.com", "مدير المبيعات")
            viewModel.addAddress(
                title = "الفرع اللوجستي",
                isPrimary = true,
                line1 = "شارع الأمل",
                line2 = "المبنى 12",
                postalCode = "11564",
                city = "الرياض",
                province = "الرياض",
                country = "السعودية",
                shippingNotes = "التسليم صباحاً"
            )
        }

        val state = viewModel.uiState.value
        assertTrue(state.companyContacts.any { it.name == "م. عبد العزيز" })
        assertTrue(state.companyAddresses.any { it.title == "الفرع اللوجستي" })
        assertTrue(duration.inWholeMilliseconds < 300, "استغرقت عمليات التحديث والإضافة في نموذج العرض وقتاً أطول من المتوقع: ${duration.inWholeMilliseconds} ms")
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testPartViewModelSearchAndFilterPerformance() {
        val viewModel = PartViewModel()

        val duration = measureTime {
            viewModel.onSearchQueryChanged("ESP32")
            viewModel.onCategorySelected(null)
            viewModel.onSearchQueryChanged("")
        }

        val state = viewModel.uiState.value
        assertNotNull(state.parts)
        assertTrue(duration.inWholeMilliseconds < 200, "استغرق استعلام وتصفية القطع في PartViewModel زماً أطول من المتوقع: ${duration.inWholeMilliseconds} ms")
    }
}
