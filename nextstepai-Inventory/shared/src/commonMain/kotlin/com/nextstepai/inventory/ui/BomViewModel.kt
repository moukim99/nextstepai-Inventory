package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.BomItemSubstituteView
import com.nextstepai.inventory.data.ManufacturingPhase
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.repository.BomRepository
import com.nextstepai.inventory.repository.PartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * حالة واجهة شاشة إدارة قائمة مواد التصنيع والقطع البديلة (BOM UI State).
 */
data class BomUiState(
    val selectedPartId: Long? = null, // لا يُفرض منتج أب افتراضياً
    val parentParts: List<Part> = emptyList(),
    val availableComponents: List<Part> = emptyList(),
    val allParts: List<Part> = emptyList(),
    val bomItems: List<BomItem> = emptyList(),
    val phases: List<ManufacturingPhase> = emptyList(),
    val substitutesMap: Map<Long, List<BomItemSubstituteView>> = emptyMap(),
    val selectedBomItemForSubstitute: BomItem? = null,
    val searchQuery: String = "",
    val filterCategory: String = "ALL", // ALL, MANDATORY, OPTIONAL, VARIANTS
    val isFilterSheetOpen: Boolean = false,
    val isSelectParentSheetOpen: Boolean = false,
    val filterMandatoryOnly: Boolean = false,
    val filterOptionalOnly: Boolean = false,
    val filterConsumableOnly: Boolean = false,
    val filterAllowVariantsOnly: Boolean = false,
    val isAddBomDialogOpen: Boolean = false,
    val editingBomItem: BomItem? = null,
    val isAddSubstituteDialogOpen: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * نموذج العرض (ViewModel) لشاشة بنود قائمة المواد (BOM Items).
 */
class BomViewModel(
    private val bomRepository: BomRepository = BomRepository(),
    private val partRepository: PartRepository = PartRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BomUiState())
    val uiState: StateFlow<BomUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allParts = partRepository.getParts()
                val assemblies = partRepository.getParentAssemblies()

                val currentPartId = _uiState.value.selectedPartId
                val bomList = if (currentPartId != null) {
                    bomRepository.getBomItemsForPart(currentPartId)
                } else {
                    assemblies.flatMap { bomRepository.getBomItemsForPart(it.id) }
                }
                val allPhases = bomRepository.getAllPhases()
                val existingSubPartIds = bomList.map { it.subPartId }
                val eligibleComponents = if (currentPartId != null) {
                    partRepository.getEligibleSubParts(currentPartId, existingSubPartIds)
                } else {
                    allParts.filter { it.component }
                }

                val substitutes = bomList.associate { item ->
                    item.id to bomRepository.getSubstitutesForBomItem(item.id, allParts)
                }

                _uiState.update {
                    it.copy(
                        selectedPartId = currentPartId,
                        parentParts = assemblies,
                        availableComponents = eligibleComponents,
                        allParts = allParts,
                        bomItems = bomList,
                        phases = allPhases,
                        substitutesMap = substitutes
                    )
                }
            } catch (_: Throwable) {}
        }
    }

    fun setSelectParentSheetOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isSelectParentSheetOpen = isOpen) }
    }

    fun openAddSubstituteDialog(bomItem: BomItem) {
        _uiState.update {
            it.copy(
                selectedBomItemForSubstitute = bomItem,
                isAddSubstituteDialogOpen = true,
                errorMessage = null
            )
        }
    }

    fun closeAddSubstituteDialog() {
        _uiState.update {
            it.copy(
                selectedBomItemForSubstitute = null,
                isAddSubstituteDialogOpen = false
            )
        }
    }

    fun addSubstitute(partId: Long) {
        val selectedBomItem = _uiState.value.selectedBomItemForSubstitute ?: return
        val allParts = partRepository.getParts()
        try {
            bomRepository.addSubstitute(
                bomItemId = selectedBomItem.id,
                partId = partId,
                partsList = allParts
            )
            _uiState.update {
                it.copy(
                    isAddSubstituteDialogOpen = false,
                    selectedBomItemForSubstitute = null,
                    successMessage = "تم ربط القطعة البديلة المعتمدة ببند قائمة المواد بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilterSheetOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isFilterSheetOpen = isOpen) }
    }

    fun selectFilterCategory(category: String) {
        _uiState.update { it.copy(filterCategory = category) }
    }

    fun applyFilters(
        mandatory: Boolean,
        optional: Boolean,
        consumable: Boolean,
        allowVariants: Boolean
    ) {
        _uiState.update {
            it.copy(
                filterMandatoryOnly = mandatory,
                filterOptionalOnly = optional,
                filterConsumableOnly = consumable,
                filterAllowVariantsOnly = allowVariants,
                isFilterSheetOpen = false
            )
        }
    }

    fun resetFilters() {
        _uiState.update {
            it.copy(
                selectedPartId = null,
                filterCategory = "ALL",
                filterMandatoryOnly = false,
                filterOptionalOnly = false,
                filterConsumableOnly = false,
                filterAllowVariantsOnly = false
            )
        }
        loadData()
    }

    fun selectParentPart(partId: Long?) {
        _uiState.update { it.copy(selectedPartId = partId) }
        loadData()
    }

    fun setAddDialogOpen(isOpen: Boolean) {
        _uiState.update {
            it.copy(
                isAddBomDialogOpen = isOpen,
                editingBomItem = if (!isOpen) null else it.editingBomItem,
                errorMessage = null
            )
        }
    }

    fun openEditBomItemDialog(bomItem: BomItem) {
        _uiState.update {
            it.copy(
                editingBomItem = bomItem,
                isAddBomDialogOpen = true,
                errorMessage = null
            )
        }
    }

    fun addBomItem(
        subPartId: Long,
        quantity: Double,
        reference: String,
        optional: Boolean,
        consumable: Boolean,
        allowVariants: Boolean,
        inherited: Boolean,
        note: String,
        substitutePartIds: List<Long> = emptyList(),
        unit: String = "",
        parentPartIdOverride: Long? = null,
        phaseUuid: String? = null,
        closeDialog: Boolean = true
    ) {
        val parentId = parentPartIdOverride ?: _uiState.value.selectedPartId
        if (parentId == null) {
            _uiState.update { it.copy(errorMessage = "تعذر الحفظ: يرجى تحديد المنتج الأب المجمّع أولاً قبل إضافة المكونات.") }
            return
        }

        try {
            if (unit.isNotBlank()) {
                val subPart = partRepository.getPartById(subPartId)
                if (subPart != null && subPart.units != unit) {
                    partRepository.updatePart(subPart.copy(units = unit))
                }
            }

            val editingItem = _uiState.value.editingBomItem
            val item = BomItem(
                id = editingItem?.id ?: 0L,
                partId = parentId,
                subPartId = subPartId,
                quantity = quantity,
                reference = reference,
                optional = optional,
                consumable = consumable,
                allowVariants = allowVariants,
                inherited = inherited,
                note = note,
                phaseUuid = phaseUuid
            )

            val createdItem = if (editingItem != null) {
                bomRepository.updateBomItem(item)
            } else {
                bomRepository.addBomItem(item)
            }

            val allParts = partRepository.getParts()
            if (allowVariants && substitutePartIds.isNotEmpty()) {
                substitutePartIds.forEach { subId ->
                    bomRepository.addSubstitute(
                        bomItemId = createdItem.id,
                        partId = subId,
                        partsList = allParts
                    )
                }
            }

            val currentItemsCount = _uiState.value.bomItems.count { it.partId == parentId }
            val updatedCount = if (editingItem != null) currentItemsCount else currentItemsCount + 1

            _uiState.update {
                it.copy(
                    editingBomItem = null,
                    selectedPartId = parentId,
                    isAddBomDialogOpen = !closeDialog,
                    errorMessage = null,
                    successMessage = if (editingItem != null) "تم تحديث بيانات المكون الفرعي بقائمة المواد بنجاح" else if (closeDialog) "تمت إضافة المكون الفرعي إلى قائمة مواد المنتج بنجاح" else "تم حفظ المكون بنجاح! (إجمالي المكونات المرتبطة بالمنتج: $updatedCount)"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    /**
     * حذف بند من قائمة المواد وتنظيف البدائل المعتمدة المربوطة به بالـ UUID والـ ID.
     */
    fun deleteBomItem(uuid: String, id: Long = 0L) {
        try {
            bomRepository.deleteBomItem(uuid = uuid, id = id)
            _uiState.update {
                it.copy(
                    successMessage = "تم حذف المكون الفرعي من قائمة المواد بنجاح"
                )
            }
            loadData()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(errorMessage = "فشل حذف المكون من قائمة المواد: ${e.message}")
            }
        }
    }

    fun deleteBomItem(id: Long) {
        deleteBomItem(uuid = "bom-$id", id = id)
    }

    /**
     * إدراج مرحلة تصنيعية مخصصة جديدة وحفظها بقاعدة البيانات.
     */
    fun addManufacturingPhase(name: String, description: String = "") {
        if (name.isBlank()) return
        try {
            bomRepository.addPhase(name = name.trim(), description = description.trim())
            _uiState.update {
                it.copy(
                    successMessage = "تم اعتماد وإدراج مرحلة التصنيع الجديدة '${name.trim()}' بنجاح"
                )
            }
            loadData()
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "فشل إضافة مرحلة التصنيع: ${e.message}") }
        }
    }

    /**
     * حذف مرحلة تصنيعية مخصصة وتفريغ ارتباطاتها بقاعدة البيانات.
     */
    fun deleteManufacturingPhase(uuid: String) {
        if (uuid.isBlank()) return
        try {
            val success = bomRepository.deletePhase(uuid)
            if (success) {
                _uiState.update {
                    it.copy(
                        successMessage = "تم حذف مرحلة التصنيع المخصصة وتعديل الارتباطات بنجاح"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = "تنبيه النظام: لا يمكن حذف المراحل التصنيعية القياسية الافتراضية للنظام."
                    )
                }
            }
            loadData()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(errorMessage = "فشل حذف مرحلة التصنيع: ${e.message}")
            }
        }
    }

    /**
     * إدراج وحدة قياس مخصصة جديدة وإصدار حفظ في قاعدة البيانات.
     */
    fun addCustomUnit(unitCode: String) {
        if (unitCode.isBlank()) return
        try {
            partRepository.addParameterTemplate(
                name = unitCode.trim(),
                units = unitCode.trim(),
                description = "وحدة قياس مخصصة"
            )
            _uiState.update {
                it.copy(
                    successMessage = "تم اعتماد وحفظ وحدة القياس الجديدة '${unitCode.trim()}' بنجاح"
                )
            }
            loadData()
        } catch (e: Exception) {
            // القالب موجود مسبقاً أو تعارض
        }
    }

    /**
     * حذف وحدة قياس مخصصة بحذف متتابع من قاعدة البيانات.
     */
    fun deleteUnit(unitCode: String) {
        if (unitCode.isBlank()) return
        try {
            partRepository.deleteParameterTemplateByNameOrUnit(unitCode.trim())
            _uiState.update {
                it.copy(
                    successMessage = "تم حذف وحدة القياس '${unitCode.trim()}' وإلغاء مفرداتها بنجاح"
                )
            }
            loadData()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(errorMessage = "فشل حذف وحدة القياس: ${e.message}")
            }
        }
    }
}
