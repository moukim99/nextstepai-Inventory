package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.repository.CompanyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CompanyRoleFilter {
    ALL,
    SUPPLIER_ONLY,
    MANUFACTURER_ONLY,
    CUSTOMER_ONLY
}

/**
 * حالة واجهة شاشة الشركات والعلاقات التجارية (Company UI State).
 */
data class CompanyUiState(
    val companies: List<Company> = emptyList(),
    val searchQuery: String = "",
    val roleFilter: CompanyRoleFilter = CompanyRoleFilter.ALL,
    val selectedCompany: Company? = null,
    val isAddCompanyDialogOpen: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * نموذج العرض (ViewModel) لشاشة إدارة الشركات والعلاقات التجارية (Company Management).
 */
class CompanyViewModel(
    private val repository: CompanyRepository = CompanyRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanyUiState())
    val uiState: StateFlow<CompanyUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val filter = _uiState.value.roleFilter
        val list = repository.searchCompanies(
            query = _uiState.value.searchQuery,
            supplierOnly = filter == CompanyRoleFilter.SUPPLIER_ONLY,
            manufacturerOnly = filter == CompanyRoleFilter.MANUFACTURER_ONLY,
            customerOnly = filter == CompanyRoleFilter.CUSTOMER_ONLY
        )
        _uiState.update { it.copy(companies = list) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadData()
    }

    fun setRoleFilter(filter: CompanyRoleFilter) {
        _uiState.update { it.copy(roleFilter = filter) }
        loadData()
    }

    fun setSelectedCompany(company: Company?) {
        _uiState.update { it.copy(selectedCompany = company) }
    }

    fun setAddDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddCompanyDialogOpen = isOpen, errorMessage = null) }
    }

    fun addCompany(
        name: String,
        description: String,
        website: String,
        phone: String,
        email: String,
        address: String,
        contact: String,
        isSupplier: Boolean,
        isManufacturer: Boolean,
        isCustomer: Boolean,
        currency: String
    ) {
        try {
            val c = Company(
                name = name,
                description = description,
                website = website,
                phone = phone,
                email = email,
                address = address,
                contact = contact,
                isSupplier = isSupplier,
                isManufacturer = isManufacturer,
                isCustomer = isCustomer,
                currency = currency.ifBlank { "USD" }
            )
            repository.addCompany(c)
            _uiState.update {
                it.copy(
                    isAddCompanyDialogOpen = false,
                    errorMessage = null,
                    successMessage = "تم تسجيل الشركة '${name}' بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }
}
