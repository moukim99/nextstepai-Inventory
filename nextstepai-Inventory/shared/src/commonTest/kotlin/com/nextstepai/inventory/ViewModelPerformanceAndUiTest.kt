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

    @OptIn(ExperimentalTime::class)
    @Test
    fun testStockViewModelLocationFilterPerformance() {
        val viewModel = StockViewModel()

        val duration = measureTime {
            viewModel.setFilterBottomSheetOpen(true)
            assertTrue(viewModel.uiState.value.isFilterBottomSheetOpen)

            val locs = viewModel.uiState.value.locations
            if (locs.isNotEmpty()) {
                val locId = locs.first().id
                viewModel.applyLocationFilters(setOf(locId))
                assertEquals(setOf(locId), viewModel.uiState.value.selectedLocationIds)
                assertFalse(viewModel.uiState.value.isFilterBottomSheetOpen)

                viewModel.toggleLocationFilter(locId)
                assertTrue(viewModel.uiState.value.selectedLocationIds.isEmpty())

                viewModel.filterByLocation(locId)
                assertEquals(setOf(locId), viewModel.uiState.value.selectedLocationIds)

                viewModel.clearLocationFilters()
                assertTrue(viewModel.uiState.value.selectedLocationIds.isEmpty())
            } else {
                viewModel.clearLocationFilters()
            }
        }

        assertTrue(duration.inWholeMilliseconds < 200, "استغرق فلتر المستودعات في StockViewModel زماً أطول من المتوقع: ${duration.inWholeMilliseconds} ms")
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testBomViewModelPerformanceAndSequentialAdd() {
        val viewModel = BomViewModel()

        val duration = measureTime {
            // 1. اختبار السرعة للتصفية والبحث
            viewModel.onSearchQueryChanged("مقاومة")
            viewModel.onSearchQueryChanged("")
            viewModel.setFilterSheetOpen(true)
            viewModel.applyFilters(mandatory = true, optional = false, consumable = false, allowVariants = false)
            viewModel.resetFilters()

            // 2. اختبار سرعة تسلسل الحفظ والمتابعة (Save & Continue)
            val parentParts = viewModel.uiState.value.parentParts
            val components = viewModel.uiState.value.allParts.filter { it.component }

            if (parentParts.isNotEmpty() && components.isNotEmpty()) {
                val parentId = parentParts.first().id
                val subPartId = components.first().id

                // إضافة مكون بتدفق الحفظ والمتابعة (closeDialog = false)
                viewModel.addBomItem(
                    subPartId = subPartId,
                    quantity = 2.0,
                    reference = "R10, R11",
                    optional = false,
                    consumable = false,
                    allowVariants = false,
                    inherited = false,
                    note = "اختبار أداء الإدخال المتتابع",
                    unit = "pcs",
                    parentPartIdOverride = parentId,
                    closeDialog = false
                )

                assertTrue(viewModel.uiState.value.isAddBomDialogOpen)
                assertNotNull(viewModel.uiState.value.successMessage)

                // إضافة مكون بتدفق الحفظ وإنهاء (closeDialog = true)
                viewModel.addBomItem(
                    subPartId = subPartId,
                    quantity = 1.0,
                    reference = "U5",
                    optional = false,
                    consumable = false,
                    allowVariants = false,
                    inherited = false,
                    note = "اختبار الإغلاق",
                    unit = "pcs",
                    parentPartIdOverride = parentId,
                    closeDialog = true
                )

                assertFalse(viewModel.uiState.value.isAddBomDialogOpen)
            }
        }

        assertTrue(duration.inWholeMilliseconds < 300, "استغرق أداء وحفظ BomViewModel زماً أطول من المتوقع: ${duration.inWholeMilliseconds} ms")
    }
}
