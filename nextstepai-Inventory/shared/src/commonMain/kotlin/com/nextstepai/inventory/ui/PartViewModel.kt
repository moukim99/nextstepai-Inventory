package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.CategoryParameterTemplateView
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartAttachment
import com.nextstepai.inventory.data.PartCategory
import com.nextstepai.inventory.data.PartNotes
import com.nextstepai.inventory.data.PartParameter
import com.nextstepai.inventory.data.PartParameterTemplate
import com.nextstepai.inventory.data.PartRelatedView
import com.nextstepai.inventory.data.PartTestTemplate
import com.nextstepai.inventory.data.db.PartInternalPriceEntity
import com.nextstepai.inventory.data.db.PartPricingEntity
import com.nextstepai.inventory.data.db.PartSalePriceEntity
import com.nextstepai.inventory.repository.BomRepository
import com.nextstepai.inventory.repository.PartRepository
import com.nextstepai.inventory.repository.PartsSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * حالة واجهة المستخدم لشاشة إدارة القطع والمكونات والقوالب الفنية والقطع ذات الصلة (Part Management UI State).
 */
data class PartUiState(
    val parts: List<Part> = emptyList(),
    val categories: List<PartCategory> = emptyList(),
    val templateParts: List<Part> = emptyList(),
    val summary: PartsSummary = PartsSummary(0, 0, 0, 0, 0, 0),
    val searchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val categoryParameterTemplates: List<CategoryParameterTemplateView> = emptyList(),
    val allParameterTemplates: List<PartParameterTemplate> = emptyList(),
    val selectedPartParameters: List<PartParameter> = emptyList(),
    val selectedPartRelated: List<PartRelatedView> = emptyList(),
    val selectedPartTestTemplates: List<PartTestTemplate> = emptyList(),
    val selectedPartAttachments: List<PartAttachment> = emptyList(),
    val selectedPartNotes: PartNotes? = null,
    val selectedPartBomItems: List<BomItem> = emptyList(),
    val selectedPartPricing: PartPricingEntity? = null,
    val selectedPartInternalPrices: List<PartInternalPriceEntity> = emptyList(),
    val selectedPartSalePrices: List<PartSalePriceEntity> = emptyList(),
    val starredPartIds: Set<Long> = emptySet(),
    val lowStockOnlyFilter: Boolean = false,
    val assemblyOnlyFilter: Boolean = false,
    val starredOnlyFilter: Boolean = false,
    val selectedPart: Part? = null,
    val isAddPartDialogOpen: Boolean = false,
    val isAddCategoryParamDialogOpen: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null
)

/**
 * نموذج العرض (ViewModel) المسؤول عن إدارة حالة شاشة القطع والمكونات وفق تحليل جدول Part.
 */
class PartViewModel(
    private val repository: PartRepository = PartRepository(),
    private val bomRepository: BomRepository = BomRepository()
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
        val starredIds = repository.getStarredPartIdsForUser(1L).toSet()

        val filteredParts = repository.searchParts(
            query = _uiState.value.searchQuery,
            categoryId = _uiState.value.selectedCategoryId,
            lowStockOnly = _uiState.value.lowStockOnlyFilter,
            assemblyOnly = _uiState.value.assemblyOnlyFilter
        ).let { list ->
            if (_uiState.value.starredOnlyFilter) list.filter { starredIds.contains(it.id) } else list
        }

        _uiState.update {
            it.copy(
                parts = filteredParts,
                categories = categories,
                templateParts = templateParts,
                summary = summary,
                starredPartIds = starredIds,
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
     * اختيار تصنيف محدد أو إلغاء الاختيار (null) وتحميل قوالب المعاملات الموروثة والمباشرة.
     */
    fun onCategorySelected(categoryId: Long?) {
        val catTemplates = if (categoryId != null) {
            repository.getCategoryParameterTemplates(categoryId)
        } else {
            emptyList()
        }
        val allParamTemplates = repository.getAllParameterTemplates()

        _uiState.update {
            it.copy(
                selectedCategoryId = categoryId,
                categoryParameterTemplates = catTemplates,
                allParameterTemplates = allParamTemplates
            )
        }
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
     * اختيار قطعة لعرض تفاصيلها المباشرة ومعاملاتها الفنية، القطع ذات الصلة، قوالب الفحوصات، والمرفقات (PartAttachment).
     */
    fun selectPart(part: Part?) {
        val partParams = if (part != null) {
            repository.getPartParameters(part.id)
        } else {
            emptyList()
        }
        val relatedParts = if (part != null) {
            repository.getRelatedPartsForPart(part.id)
        } else {
            emptyList()
        }
        val testTemplates = if (part != null) {
            repository.getPartTestTemplates(part.id)
        } else {
            emptyList()
        }
        val attachments = if (part != null) {
            repository.getPartAttachments(part.id)
        } else {
            emptyList()
        }
        val notes = if (part != null) {
            repository.getPartNotes(part.id)
        } else {
            null
        }
        val bomItems = if (part != null && part.assembly) {
            bomRepository.getBomItemsForPart(part.id)
        } else {
            emptyList()
        }
        val internalPrices = if (part != null) {
            repository.getPartInternalPrices(part.id)
        } else {
            emptyList()
        }
        val salePrices = if (part != null) {
            repository.getPartSalePrices(part.id)
        } else {
            emptyList()
        }
        val pricing = if (part != null) {
            repository.getPartPricing(part.id) ?: repository.recalculatePartPricing(part, bomItems)
        } else {
            null
        }
        _uiState.update {
            it.copy(
                selectedPart = part,
                selectedPartParameters = partParams,
                selectedPartRelated = relatedParts,
                selectedPartTestTemplates = testTemplates,
                selectedPartAttachments = attachments,
                selectedPartNotes = notes,
                selectedPartBomItems = bomItems,
                selectedPartPricing = pricing,
                selectedPartInternalPrices = internalPrices,
                selectedPartSalePrices = salePrices
            )
        }
    }

    /**
     * إضافة شريحة سعر بيع جديدة للعملاء في PartSalePrice مع شرط القابلية للبيع (salable = true).
     */
    fun addPartSalePrice(quantity: Double, price: Double, currency: String = "USD") {
        val currentPart = _uiState.value.selectedPart ?: return
        try {
            repository.addPartSalePrice(currentPart, quantity, price, currency)
            val updatedSalePrices = repository.getPartSalePrices(currentPart.id)
            _uiState.update {
                it.copy(
                    selectedPartSalePrices = updatedSalePrices,
                    message = "تمت إضافة شريحة سعر البيع للعملاء بنجاح"
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(message = "خطأ: ${e.message}") }
        }
    }

    /**
     * إضافة شريحة سعرية بيع/تحويل داخلي جديدة في PartInternalPrice وتحديث PartPricing.
     */
    fun addPartInternalPrice(quantity: Double, price: Double, currency: String = "USD") {
        val currentPart = _uiState.value.selectedPart ?: return
        val bomItems = _uiState.value.selectedPartBomItems
        try {
            repository.addPartInternalPrice(currentPart.id, quantity, price, currency)
            val updatedPricing = repository.recalculatePartPricing(currentPart, bomItems)
            val updatedInternalPrices = repository.getPartInternalPrices(currentPart.id)
            _uiState.update {
                it.copy(
                    selectedPartPricing = updatedPricing,
                    selectedPartInternalPrices = updatedInternalPrices,
                    message = "تمت إضافة الشريحة السعرية وإعادة حساب التكاليف بنجاح"
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(message = "خطأ: ${e.message}") }
        }
    }

    /**
     * إعادة حساب وحفظ أسعار وتكاليف القطعة الحالية (PartPricing).
     */
    fun recalculatePartPricing() {
        val currentPart = _uiState.value.selectedPart ?: return
        val bomItems = _uiState.value.selectedPartBomItems
        try {
            val newPricing = repository.recalculatePartPricing(currentPart, bomItems)
            _uiState.update {
                it.copy(
                    selectedPartPricing = newPricing,
                    message = "تمت إعادة حساب التكاليف والأسعار بنجاح"
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(message = "خطأ: ${e.message}") }
        }
    }

    /**
     * حفظ أو تحديث سجل الملاحظات الموسعة للقطعة الحالية في PartNotes.
     */
    fun savePartNotes(notesText: String) {
        val currentPart = _uiState.value.selectedPart ?: return
        try {
            val updatedRecord = repository.saveOrUpdatePartNotes(
                partId = currentPart.id,
                notes = notesText
            )
            _uiState.update {
                it.copy(
                    selectedPartNotes = updatedRecord,
                    message = "تم حفظ التعديلات على سجل الملاحظات بنجاح"
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(message = "خطأ: ${e.message}") }
        }
    }

    /**
     * إضافة مرفق جديد أو رابط للقطعة الحالية في PartAttachment.
     */
    fun addPartAttachment(
        attachmentPath: String?,
        linkUrl: String?,
        comment: String
    ) {
        val currentPart = _uiState.value.selectedPart ?: return
        try {
            val attachmentItem = PartAttachment(
                partId = currentPart.id,
                attachment = attachmentPath,
                link = linkUrl,
                comment = comment
            )
            repository.addPartAttachment(attachmentItem)
            selectPart(currentPart)
            _uiState.update { it.copy(message = "تمت إضافة الوثيقة/المرفق بنجاح") }
        } catch (e: Exception) {
            _uiState.update { it.copy(message = "خطأ: ${e.message}") }
        }
    }

    /**
     * إضافة قالب فحص جديد للقطعة الحالية في PartTestTemplate.
     */
    fun addPartTestTemplate(
        testName: String,
        description: String,
        required: Boolean,
        requiresValue: Boolean,
        requiresAttachment: Boolean
    ) {
        val currentPart = _uiState.value.selectedPart ?: return
        try {
            val template = PartTestTemplate(
                partId = currentPart.id,
                testName = testName,
                description = description,
                required = required,
                requiresValue = requiresValue,
                requiresAttachment = requiresAttachment
            )
            repository.addPartTestTemplate(template)
            selectPart(currentPart)
            _uiState.update { it.copy(message = "تمت إضافة قالب الفحص بنجاح") }
        } catch (e: Exception) {
            _uiState.update { it.copy(message = "خطأ: ${e.message}") }
        }
    }

    /**
     * إضافة قطعة ذات صلة بالقطعة الحالية في PartRelated.
     */
    fun addPartRelated(part2Id: Long) {
        val currentPart = _uiState.value.selectedPart ?: return
        try {
            repository.addPartRelated(currentPart.id, part2Id)
            selectPart(currentPart)
            _uiState.update { it.copy(message = "تمت إضافة القطعة ذات الصلة بنجاح") }
        } catch (e: Exception) {
            _uiState.update { it.copy(message = "خطأ: ${e.message}") }
        }
    }

    /**
     * إضافة قالب معامل تصنيف جديد لجدول PartCategoryParameterTemplate.
     */
    fun addCategoryParameterTemplate(parameterTemplateId: Long, defaultValue: String?) {
        val catId = _uiState.value.selectedCategoryId ?: return
        try {
            repository.addCategoryParameterTemplate(
                categoryId = catId,
                parameterTemplateId = parameterTemplateId,
                defaultValue = defaultValue
            )
            onCategorySelected(catId)
            _uiState.update { it.copy(message = "تم إدراج قالب المعامل الفني للتصنيف بنجاح") }
        } catch (e: Exception) {
            _uiState.update { it.copy(message = "خطأ: ${e.message}") }
        }
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
        maximumStock: Double? = null,
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
            maximumStock = maximumStock,
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

    /**
     * تبديل فلتر إظهار القطع المفضلة والمتابعة فقط (PartStar).
     */
    fun toggleStarredFilter() {
        _uiState.update { it.copy(starredOnlyFilter = !it.starredOnlyFilter) }
        refreshFilteredParts()
    }

    /**
     * تبديل حالة تفضيل ومتابعة قطعة معينة في PartStar (تفاعل زر النجمة ⭐).
     */
    fun togglePartStar(partId: Long) {
        val isStarred = repository.togglePartStar(partId, 1L)
        val updatedStarredIds = repository.getStarredPartIdsForUser(1L).toSet()
        _uiState.update {
            it.copy(
                starredPartIds = updatedStarredIds,
                message = if (isStarred) "تمت إضافة القطعة للمفضلة ⭐" else "تمت إزالة القطعة من المفضلة"
            )
        }
        refreshFilteredParts()
    }

    private fun refreshFilteredParts() {
        val state = _uiState.value
        var filtered = repository.searchParts(
            query = state.searchQuery,
            categoryId = state.selectedCategoryId,
            lowStockOnly = state.lowStockOnlyFilter,
            assemblyOnly = state.assemblyOnlyFilter
        )
        if (state.starredOnlyFilter) {
            filtered = filtered.filter { state.starredPartIds.contains(it.id) }
        }
        _uiState.update { it.copy(parts = filtered) }
    }
}
