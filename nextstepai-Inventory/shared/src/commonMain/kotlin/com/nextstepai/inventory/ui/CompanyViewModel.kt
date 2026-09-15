package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import com.nextstepai.inventory.data.*
import com.nextstepai.inventory.repository.CompanyRepository
import com.nextstepai.inventory.repository.PartRepository
import com.nextstepai.inventory.repository.PurchaseOrderRepository
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

enum class CompanyDetailTab {
    INFO,
    CONTACTS,
    ADDRESSES,
    BANK_ACCOUNTS,
    MANUFACTURER_PARTS,
    SUPPLIER_PARTS
}

/**
 * إحصائيات حقيقية ديناميكية مأخوذة من جداول قاعدة البيانات للشركة.
 */
data class CompanyStats(
    val supplierPartsCount: Int = 0,
    val manufacturerPartsCount: Int = 0,
    val ordersCount: Int = 0,
    val primaryAddress: String = ""
)

/**
 * حالة واجهة شاشة الشركات والعلاقات التجارية (Company UI State).
 */
data class CompanyUiState(
    val companies: List<Company> = emptyList(),
    val companyStatsMap: Map<Long, CompanyStats> = emptyMap(),
    val totalSuppliersCount: Int = 0,
    val totalManufacturersCount: Int = 0,
    val totalCustomersCount: Int = 0,
    val searchQuery: String = "",
    val roleFilter: CompanyRoleFilter = CompanyRoleFilter.ALL,
    val selectedCompany: Company? = null,
    val activeDetailTab: CompanyDetailTab = CompanyDetailTab.INFO,
    val companyContacts: List<Contact> = emptyList(),
    val companyAddresses: List<Address> = emptyList(),
    val companyAttachments: List<CompanyAttachment> = emptyList(),
    val companyBankAccounts: List<CompanyBankAccount> = emptyList(),
    val companyLegalRecord: CompanyLegalRecord? = null,
    val companyManufacturerParts: List<ManufacturerPart> = emptyList(),
    val selectedManufacturerPart: ManufacturerPart? = null,
    val selectedManufacturerPartAttachments: List<ManufacturerPartAttachment> = emptyList(),
    val selectedManufacturerPartParameters: List<ManufacturerPartParameter> = emptyList(),
    val companySupplierParts: List<SupplierPart> = emptyList(),
    val selectedSupplierPart: SupplierPart? = null,
    val supplierPartPriceBreaks: List<SupplierPriceBreak> = emptyList(),
    val allParts: List<Part> = emptyList(),
    val selectedCountries: Set<String> = emptySet(),
    val selectedScope: String = "ALL",
    val isFilterBottomSheetOpen: Boolean = false,
    val isAddCompanyDialogOpen: Boolean = false,
    val companyToEdit: Company? = null,
    val isAddContactDialogOpen: Boolean = false,
    val isAddAddressDialogOpen: Boolean = false,
    val isAddCompanyAttachmentDialogOpen: Boolean = false,
    val isAddBankAccountDialogOpen: Boolean = false,
    val isEditLegalRecordDialogOpen: Boolean = false,
    val isAddManufacturerPartDialogOpen: Boolean = false,
    val isAddManufacturerPartParameterDialogOpen: Boolean = false,
    val isAddManufacturerPartAttachmentDialogOpen: Boolean = false,
    val isAddSupplierPartDialogOpen: Boolean = false,
    val isAddPriceBreakDialogOpen: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

fun String.normalizeArabic(): String = this
    .replace("[أإآ]".toRegex(), "ا")
    .replace("ة", "ه")
    .replace("ى", "ي")
    .trim()
    .lowercase()

/**
 * نموذج العرض (ViewModel) لشاشة إدارة الشركات والعلاقات التجارية (Company Management).
 */
class CompanyViewModel(
    private val repository: CompanyRepository = CompanyRepository(),
    private val poRepository: PurchaseOrderRepository = PurchaseOrderRepository(),
    private val partRepository: PartRepository = PartRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanyUiState())
    val uiState: StateFlow<CompanyUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val filter = _uiState.value.roleFilter
        var list = repository.searchCompanies(
            query = _uiState.value.searchQuery,
            supplierOnly = filter == CompanyRoleFilter.SUPPLIER_ONLY,
            manufacturerOnly = filter == CompanyRoleFilter.MANUFACTURER_ONLY,
            customerOnly = filter == CompanyRoleFilter.CUSTOMER_ONLY
        )

        val allCompanies = repository.getCompanies()
        val supCount = allCompanies.count { it.isSupplier }
        val mfgCount = allCompanies.count { it.isManufacturer }
        val custCount = allCompanies.count { it.isCustomer }

        val statsMap = list.associate { company ->
            val supParts = repository.getSupplierPartsForCompany(company.id).size
            val mfgParts = repository.getManufacturerPartsForCompany(company.id).size
            val orders = poRepository.searchOrders("", supplierId = company.id).size
            val addrs = repository.getAddressesForCompany(company.id)
            val primaryAddr = addrs.find { it.isPrimary } ?: addrs.firstOrNull()
            val addrText = primaryAddr?.let {
                val cityStr = it.city.ifBlank { it.line1 }
                if (cityStr.isNotBlank()) {
                    if (it.country.isNotBlank()) "$cityStr، ${it.country}" else cityStr
                } else company.address
            } ?: company.address

            company.id to CompanyStats(
                supplierPartsCount = supParts,
                manufacturerPartsCount = mfgParts,
                ordersCount = orders,
                primaryAddress = addrText
            )
        }

        // Apply country & scope filters in memory (Zero DB changes)
        val selectedCountries = _uiState.value.selectedCountries
        val selectedScope = _uiState.value.selectedScope

        if (selectedCountries.isNotEmpty()) {
            list = list.filter { comp ->
                val stats = statsMap[comp.id]
                val addrNorm = ((stats?.primaryAddress ?: "") + " " + comp.address).normalizeArabic()
                selectedCountries.any { country ->
                    val cNorm = country.normalizeArabic()
                    addrNorm.contains(cNorm) ||
                    (country.contains("السعودية") && (addrNorm.contains("الرياض") || addrNorm.contains("جده") || addrNorm.contains("السعوديه") || comp.phone.startsWith("+966"))) ||
                    (country.contains("الصين") && (addrNorm.contains("الصين") || addrNorm.contains("شنغهاي") || comp.phone.startsWith("+86")))
                }
            }
        }

        if (selectedScope == "DOMESTIC") {
            list = list.filter { comp ->
                val stats = statsMap[comp.id]
                val addrStr = (stats?.primaryAddress ?: "") + " " + comp.address
                addrStr.contains("السعودية", ignoreCase = true) || addrStr.contains("الرياض", ignoreCase = true) || comp.phone.startsWith("+966")
            }
        } else if (selectedScope == "GLOBAL") {
            list = list.filter { comp ->
                val stats = statsMap[comp.id]
                val addrStr = (stats?.primaryAddress ?: "") + " " + comp.address
                !addrStr.contains("السعودية", ignoreCase = true) && !addrStr.contains("الرياض", ignoreCase = true) && !comp.phone.startsWith("+966")
            }
        }

        val allParts = partRepository.getParts()

        _uiState.update {
            it.copy(
                companies = list,
                companyStatsMap = statsMap,
                totalSuppliersCount = supCount,
                totalManufacturersCount = mfgCount,
                totalCustomersCount = custCount,
                allParts = allParts
            )
        }
    }

    fun setFilterBottomSheetOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isFilterBottomSheetOpen = isOpen) }
    }

    fun applyCountryFilters(countries: Set<String>, scope: String) {
        _uiState.update {
            it.copy(
                selectedCountries = countries,
                selectedScope = scope,
                isFilterBottomSheetOpen = false
            )
        }
        loadData()
    }

    fun clearCountryFilters() {
        _uiState.update {
            it.copy(
                selectedCountries = emptySet(),
                selectedScope = "ALL"
            )
        }
        loadData()
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
        _uiState.update {
            it.copy(
                selectedCompany = company,
                activeDetailTab = CompanyDetailTab.INFO,
                selectedManufacturerPart = null,
                selectedManufacturerPartAttachments = emptyList(),
                selectedManufacturerPartParameters = emptyList(),
                selectedSupplierPart = null,
                errorMessage = null
            )
        }
        if (company != null) {
            loadCompanyDetails(company.id)
        }
    }

    fun setDetailTab(tab: CompanyDetailTab) {
        _uiState.update { it.copy(activeDetailTab = tab) }
    }

    fun openAddCompanyDialog() {
        _uiState.update { it.copy(isAddCompanyDialogOpen = true, companyToEdit = null, errorMessage = null) }
    }

    fun openEditCompanyDialog(company: Company) {
        _uiState.update { it.copy(isAddCompanyDialogOpen = true, companyToEdit = company, errorMessage = null) }
    }

    fun setAddDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddCompanyDialogOpen = isOpen, companyToEdit = if (!isOpen) null else it.companyToEdit, errorMessage = null) }
    }

    fun setAddContactDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddContactDialogOpen = isOpen, errorMessage = null) }
    }

    fun setAddAddressDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddAddressDialogOpen = isOpen, errorMessage = null) }
    }

    fun setAddCompanyAttachmentDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddCompanyAttachmentDialogOpen = isOpen, errorMessage = null) }
    }

    fun setAddBankAccountDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddBankAccountDialogOpen = isOpen, errorMessage = null) }
    }

    fun setEditLegalRecordDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isEditLegalRecordDialogOpen = isOpen, errorMessage = null) }
    }

    fun setAddManufacturerPartDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddManufacturerPartDialogOpen = isOpen, errorMessage = null) }
    }

    fun setAddManufacturerPartParameterDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddManufacturerPartParameterDialogOpen = isOpen, errorMessage = null) }
    }

    fun setAddManufacturerPartAttachmentDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddManufacturerPartAttachmentDialogOpen = isOpen, errorMessage = null) }
    }

    fun setAddSupplierPartDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddSupplierPartDialogOpen = isOpen, errorMessage = null) }
    }

    fun setAddPriceBreakDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddPriceBreakDialogOpen = isOpen, errorMessage = null) }
    }

    fun setSelectedManufacturerPart(part: ManufacturerPart?) {
        _uiState.update { it.copy(selectedManufacturerPart = part) }
        if (part != null) {
            val attachments = repository.getAttachmentsForManufacturerPart(part.id)
            val params = repository.getParametersForManufacturerPart(part.id)
            _uiState.update {
                it.copy(
                    selectedManufacturerPartAttachments = attachments,
                    selectedManufacturerPartParameters = params
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    selectedManufacturerPartAttachments = emptyList(),
                    selectedManufacturerPartParameters = emptyList()
                )
            }
        }
    }

    fun setSelectedSupplierPart(part: SupplierPart?) {
        _uiState.update { it.copy(selectedSupplierPart = part) }
        if (part != null) {
            val breaks = repository.getPriceBreaksForSupplierPart(part.id)
            _uiState.update { it.copy(supplierPartPriceBreaks = breaks) }
        } else {
            _uiState.update { it.copy(supplierPartPriceBreaks = emptyList()) }
        }
    }

    private fun loadCompanyDetails(companyId: Long) {
        val contacts = repository.getContactsForCompany(companyId)
        val addresses = repository.getAddressesForCompany(companyId)
        val attachments = repository.getAttachmentsForCompany(companyId)
        val bankAccounts = repository.getBankAccountsForCompany(companyId)
        val legalRecord = repository.getLegalRecordForCompany(companyId)
        val mfgParts = repository.getManufacturerPartsForCompany(companyId)
        val supParts = repository.getSupplierPartsForCompany(companyId)
        _uiState.update {
            it.copy(
                companyContacts = contacts,
                companyAddresses = addresses,
                companyAttachments = attachments,
                companyBankAccounts = bankAccounts,
                companyLegalRecord = legalRecord,
                companyManufacturerParts = mfgParts,
                companySupplierParts = supParts
            )
        }
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
        currency: String,
        parentId: Long? = null
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
                currency = currency.ifBlank { "USD" },
                parentId = parentId
            )
            repository.addCompany(c)
            _uiState.update {
                it.copy(
                    isAddCompanyDialogOpen = false,
                    companyToEdit = null,
                    errorMessage = null,
                    successMessage = "تم تسجيل الشركة '${name}' بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun updateCompany(
        id: Long,
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
        currency: String,
        parentId: Long? = null
    ) {
        try {
            val existing = repository.getCompanyById(id) ?: return
            val updated = existing.copy(
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
                currency = currency.ifBlank { "USD" },
                parentId = parentId
            )
            repository.updateCompany(updated)
            _uiState.update {
                it.copy(
                    isAddCompanyDialogOpen = false,
                    companyToEdit = null,
                    selectedCompany = updated,
                    errorMessage = null,
                    successMessage = "تم تحديث بيانات الشركة '${name}' بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun addCompanyAttachment(
        documentType: String,
        attachmentPath: String,
        link: String,
        comment: String,
        expiryDate: String = "",
        notifyOnExpiry: Boolean = true,
        notificationDaysBefore: Int = 30
    ) {
        val currentCompany = _uiState.value.selectedCompany ?: return
        try {
            val att = CompanyAttachment(
                companyId = currentCompany.id,
                documentType = documentType.ifBlank { "سجل تجاري" },
                attachmentPath = attachmentPath,
                link = link,
                comment = comment.ifBlank { documentType },
                expiryDate = expiryDate,
                notifyOnExpiry = notifyOnExpiry,
                notificationDaysBefore = notificationDaysBefore
            )
            repository.addCompanyAttachment(att)
            _uiState.update { it.copy(isAddCompanyAttachmentDialogOpen = false, errorMessage = null) }
            loadCompanyDetails(currentCompany.id)
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun deleteCompanyAttachment(attachmentId: Long) {
        val currentCompany = _uiState.value.selectedCompany ?: return
        repository.deleteCompanyAttachment(attachmentId)
        loadCompanyDetails(currentCompany.id)
    }

    fun addContact(name: String, phone: String, email: String, role: String, isPrimary: Boolean = false) {
        val currentCompany = _uiState.value.selectedCompany ?: return
        try {
            val contact = Contact(
                companyId = currentCompany.id,
                name = name,
                phone = phone,
                email = email,
                role = role,
                isPrimary = isPrimary
            )
            repository.addContact(contact)
            _uiState.update { it.copy(isAddContactDialogOpen = false, errorMessage = null) }
            loadCompanyDetails(currentCompany.id)
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun deleteContact(contactId: Long) {
        val currentCompany = _uiState.value.selectedCompany ?: return
        repository.deleteContact(contactId)
        loadCompanyDetails(currentCompany.id)
    }

    fun addAddress(
        title: String,
        isPrimary: Boolean,
        line1: String,
        line2: String,
        postalCode: String,
        city: String,
        province: String,
        country: String,
        shippingNotes: String
    ) {
        val currentCompany = _uiState.value.selectedCompany ?: return
        try {
            val addr = Address(
                companyId = currentCompany.id,
                title = title.ifBlank { "العنوان" },
                isPrimary = isPrimary,
                line1 = line1,
                line2 = line2,
                postalCode = postalCode,
                city = city,
                province = province,
                country = country,
                shippingNotes = shippingNotes
            )
            repository.addAddress(addr)
            _uiState.update { it.copy(isAddAddressDialogOpen = false, errorMessage = null) }
            loadCompanyDetails(currentCompany.id)
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun deleteAddress(addressId: Long) {
        val currentCompany = _uiState.value.selectedCompany ?: return
        repository.deleteAddress(addressId)
        loadCompanyDetails(currentCompany.id)
    }

    fun addCompanyBankAccount(
        bankName: String,
        accountName: String,
        accountNumber: String = "",
        iban: String = "",
        swiftBic: String = "",
        currency: String = "USD",
        branchName: String = "",
        isPrimary: Boolean = false
    ) {
        val currentCompany = _uiState.value.selectedCompany ?: return
        if (bankName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "اسم البنك إلزامي") }
            return
        }
        if (accountName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "اسم صاحب الحساب إلزامي") }
            return
        }

        val account = CompanyBankAccount(
            companyId = currentCompany.id,
            bankName = bankName.trim(),
            accountName = accountName.trim(),
            accountNumber = accountNumber.trim(),
            iban = iban.trim(),
            swiftBic = swiftBic.trim(),
            currency = currency.ifBlank { currentCompany.currency },
            branchName = branchName.trim(),
            isPrimary = isPrimary
        )
        repository.addBankAccount(account)
        _uiState.update { it.copy(isAddBankAccountDialogOpen = false, errorMessage = null) }
        loadCompanyDetails(currentCompany.id)
    }

    fun deleteCompanyBankAccount(accountId: Long) {
        val currentCompany = _uiState.value.selectedCompany ?: return
        repository.deleteBankAccount(accountId)
        loadCompanyDetails(currentCompany.id)
    }

    fun saveOrUpdateCompanyLegalRecord(
        commercialRegisterNumber: String,
        taxId: String,
        nationalIdNumber: String = "",
        importLicenseNumber: String = "",
        manufacturingLicenseNumber: String = "",
        activityCodes: String = "",
        issuingAuthority: String = "",
        issueDate: String = "",
        expiryDate: String = ""
    ) {
        val currentCompany = _uiState.value.selectedCompany ?: return
        val currentRecord = _uiState.value.companyLegalRecord

        val newRecord = CompanyLegalRecord(
            id = currentRecord?.id ?: 0L,
            companyId = currentCompany.id,
            commercialRegisterNumber = commercialRegisterNumber.trim(),
            taxId = taxId.trim(),
            nationalIdNumber = nationalIdNumber.trim(),
            importLicenseNumber = importLicenseNumber.trim(),
            manufacturingLicenseNumber = manufacturingLicenseNumber.trim(),
            activityCodes = activityCodes.trim(),
            issuingAuthority = issuingAuthority.trim(),
            issueDate = issueDate.trim(),
            expiryDate = expiryDate.trim()
        )
        repository.saveOrUpdateLegalRecord(newRecord)
        _uiState.update { it.copy(isEditLegalRecordDialogOpen = false, errorMessage = null) }
        loadCompanyDetails(currentCompany.id)
    }

    fun addManufacturerPart(partId: Long, mpn: String, description: String, link: String) {
        val currentCompany = _uiState.value.selectedCompany ?: return
        try {
            val mfgPart = ManufacturerPart(
                partId = partId,
                manufacturerId = currentCompany.id,
                mpn = mpn,
                description = description,
                link = link
            )
            repository.addManufacturerPart(mfgPart)
            _uiState.update { it.copy(isAddManufacturerPartDialogOpen = false, errorMessage = null) }
            loadCompanyDetails(currentCompany.id)
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun deleteManufacturerPart(partId: Long) {
        val currentCompany = _uiState.value.selectedCompany ?: return
        repository.deleteManufacturerPart(partId)
        loadCompanyDetails(currentCompany.id)
    }

    fun addManufacturerPartParameter(name: String, value: String, units: String) {
        val mfgPart = _uiState.value.selectedManufacturerPart ?: return
        try {
            val param = ManufacturerPartParameter(
                manufacturerPartId = mfgPart.id,
                name = name,
                value = value,
                units = units
            )
            repository.addManufacturerPartParameter(param)
            _uiState.update { it.copy(isAddManufacturerPartParameterDialogOpen = false, errorMessage = null) }
            setSelectedManufacturerPart(mfgPart)
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun deleteManufacturerPartParameter(parameterId: Long) {
        val mfgPart = _uiState.value.selectedManufacturerPart ?: return
        repository.deleteManufacturerPartParameter(parameterId)
        setSelectedManufacturerPart(mfgPart)
    }

    fun addManufacturerPartAttachment(attachmentPath: String, link: String, comment: String) {
        val mfgPart = _uiState.value.selectedManufacturerPart ?: return
        try {
            val att = ManufacturerPartAttachment(
                manufacturerPartId = mfgPart.id,
                attachmentPath = attachmentPath,
                link = link,
                comment = comment
            )
            repository.addManufacturerPartAttachment(att)
            _uiState.update { it.copy(isAddManufacturerPartAttachmentDialogOpen = false, errorMessage = null) }
            setSelectedManufacturerPart(mfgPart)
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun deleteManufacturerPartAttachment(attachmentId: Long) {
        val mfgPart = _uiState.value.selectedManufacturerPart ?: return
        repository.deleteManufacturerPartAttachment(attachmentId)
        setSelectedManufacturerPart(mfgPart)
    }

    fun addSupplierPart(
        partId: Long,
        sku: String,
        manufacturerPartId: Long?,
        description: String,
        link: String,
        note: String,
        packaging: String,
        packQuantity: String
    ) {
        val currentCompany = _uiState.value.selectedCompany ?: return
        try {
            val supPart = SupplierPart(
                partId = partId,
                supplierId = currentCompany.id,
                sku = sku,
                manufacturerPartId = manufacturerPartId,
                description = description,
                link = link,
                note = note,
                packaging = packaging,
                packQuantity = packQuantity.ifBlank { "1" }
            )
            repository.addSupplierPart(supPart)
            _uiState.update { it.copy(isAddSupplierPartDialogOpen = false, errorMessage = null) }
            loadCompanyDetails(currentCompany.id)
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun deleteSupplierPart(partId: Long) {
        val currentCompany = _uiState.value.selectedCompany ?: return
        repository.deleteSupplierPart(partId)
        loadCompanyDetails(currentCompany.id)
    }

    fun addPriceBreak(quantity: Double, price: Double, priceCurrency: String) {
        val currentSupPart = _uiState.value.selectedSupplierPart ?: return
        val currentCompany = _uiState.value.selectedCompany
        val defaultCurrency = currentCompany?.currency ?: "USD"
        try {
            val pb = SupplierPriceBreak(
                supplierPartId = currentSupPart.id,
                quantity = quantity,
                price = price,
                priceCurrency = priceCurrency.ifBlank { defaultCurrency }
            )
            repository.addPriceBreak(pb, defaultCompanyCurrency = defaultCurrency)
            _uiState.update { it.copy(isAddPriceBreakDialogOpen = false, errorMessage = null) }
            setSelectedSupplierPart(currentSupPart)
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun deletePriceBreak(priceBreakId: Long) {
        val currentSupPart = _uiState.value.selectedSupplierPart ?: return
        repository.deletePriceBreak(priceBreakId)
        setSelectedSupplierPart(currentSupPart)
    }
}
