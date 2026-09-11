package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartCategory
import com.nextstepai.inventory.repository.PartRepository
import com.nextstepai.inventory.repository.PartsSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * حالة واجهة المستخدم لشاشة إدارة القطع والمكونات (Part Management UI State).
 */
data class PartUiState(
    val parts: List<Part> = emptyList(),
    val categories: List<PartCategory> = emptyList(),
    val templateParts: List<Part> = emptyList(),
    val summary: PartsSummary = PartsSummary(0, 0, 0, 0, 0, 0),
    val searchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val lowStockOnlyFilter: Boolean = false,
    val assemblyOnlyFilter: Boolean = false,
    val selectedPart: Part? = null,
    val isAddPartDialogOpen: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null
)

/**
 * نموذج العرض (ViewModel) المسؤول عن إدارة حالة شاشة القطع والمكونات وفق تحليل جدول Part.
 */
class PartViewModel(
    private val repository: PartRepository = PartRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PartUiState())
    val uiState: StateFlow<PartUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    /**
     * تحميل البيانات الأولية وتحديث القوائم المفلترة.
     */
    fun loadData() {
        _uiState.update { it.copy(isLoading = true) }

        val categories = repository.getCategories()
        val templateParts = repository.getTemplateParts()
        val summary = repository.getPartsSummary()
        val filteredParts = repository.searchParts(
            query = _uiState.value.searchQuery,
            categoryId = _uiState.value.selectedCategoryId,
            lowStockOnly = _uiState.value.lowStockOnlyFilter,
            assemblyOnly = _uiState.value.assemblyOnlyFilter
        )

        _uiState.update {
            it.copy(
                parts = filteredParts,
                categories = categories,
                templateParts = templateParts,
                summary = summary,
                isLoading = false
            )
        }
    }

    /**
     * تحديث نص البحث وإعادة التصفية.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        refreshFilteredParts()
    }

    /**
     * اختيار تصنيف محدد أو إلغاء الاختيار (null).
     */
    fun onCategorySelected(categoryId: Long?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        refreshFilteredParts()
    }

    /**
     * تبديل فلتر إظهار القطع منخفضة المخزون فقط.
     */
    fun toggleLowStockFilter() {
        _uiState.update { it.copy(lowStockOnlyFilter = !it.lowStockOnlyFilter) }
        refreshFilteredParts()
    }

    /**
     * تبديل فلتر إظهار القطع المجمعة (Assembly) فقط.
     */
    fun toggleAssemblyFilter() {
        _uiState.update { it.copy(assemblyOnlyFilter = !it.assemblyOnlyFilter) }
        refreshFilteredParts()
    }

    /**
     * اختيار قطعة لعرض تفاصيلها الكاملة وفق تحليل أعمدة جدول Part.
     */
    fun selectPart(part: Part?) {
        _uiState.update { it.copy(selectedPart = part) }
    }

    /**
     * فتح أو إغلاق حوار إضافة قطعة جديدة.
     */
    fun setAddPartDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddPartDialogOpen = isOpen) }
    }

    /**
     * إضافة قطعة جديدة وفق الحقول الأساسية لجدول Part.
     */
    fun addNewPart(
        name: String,
        ipn: String,
        description: String,
        categoryId: Long?,
        units: String,
        assembly: Boolean,
        component: Boolean,
        isTemplate: Boolean,
        variantOfId: Long?,
        minimumStock: Double,
        initialStock: Double
    ) {
        val newPart = Part(
            name = name,
            ipn = ipn,
            description = description,
            categoryId = categoryId,
            units = units.ifBlank { "pcs" },
            assembly = assembly,
            component = component,
            isTemplate = isTemplate,
            variantOfId = variantOfId,
            minimumStock = minimumStock,
            totalInStock = initialStock,
            creationDate = "2025-02-15"
        )

        repository.addPart(newPart)

        _uiState.update {
            it.copy(
                isAddPartDialogOpen = false,
                message = "تمت إضافة القطعة '${name}' بنجاح"
            )
        }
        loadData()
    }

    private fun refreshFilteredParts() {
        val state = _uiState.value
        val filtered = repository.searchParts(
            query = state.searchQuery,
            categoryId = state.selectedCategoryId,
            lowStockOnly = state.lowStockOnlyFilter,
            assemblyOnly = state.assemblyOnlyFilter
        )
        _uiState.update { it.copy(parts = filtered) }
    }
}
