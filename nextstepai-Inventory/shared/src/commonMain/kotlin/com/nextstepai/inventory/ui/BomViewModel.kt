package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.BomItemSubstituteView
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
    val selectedPartId: Long? = 3L, // افتراضياً قطعة الماكينة المصنعة
    val parentParts: List<Part> = emptyList(),
    val availableComponents: List<Part> = emptyList(),
    val bomItems: List<BomItem> = emptyList(),
    val substitutesMap: Map<Long, List<BomItemSubstituteView>> = emptyMap(),
    val selectedBomItemForSubstitute: BomItem? = null,
    val isAddBomDialogOpen: Boolean = false,
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
        val allParts = partRepository.getParts()
        val assemblies = allParts.filter { it.assembly }
        val components = allParts.filter { it.component && it.active }

        val currentPartId = _uiState.value.selectedPartId ?: assemblies.firstOrNull()?.id ?: 3L
        val bomList = bomRepository.getBomItemsForPart(currentPartId)

        val substitutes = bomList.associate { item ->
            item.id to bomRepository.getSubstitutesForBomItem(item.id, allParts)
        }

        _uiState.update {
            it.copy(
                selectedPartId = currentPartId,
                parentParts = assemblies,
                availableComponents = components,
                bomItems = bomList,
                substitutesMap = substitutes
            )
        }
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
                    successMessage = "تمت إضافة القطعة البديلة المعتمدة بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun selectParentPart(partId: Long) {
        _uiState.update { it.copy(selectedPartId = partId) }
        loadData()
    }

    fun setAddDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddBomDialogOpen = isOpen, errorMessage = null) }
    }

    fun addBomItem(
        subPartId: Long,
        quantity: Double,
        reference: String,
        optional: Boolean,
        consumable: Boolean,
        allowVariants: Boolean,
        inherited: Boolean,
        note: String
    ) {
        val parentId = _uiState.value.selectedPartId
        if (parentId == null) {
            _uiState.update { it.copy(errorMessage = "يرجى اختيار المنتج الأب أولاً") }
            return
        }

        try {
            val item = BomItem(
                partId = parentId,
                subPartId = subPartId,
                quantity = quantity,
                reference = reference,
                optional = optional,
                consumable = consumable,
                allowVariants = allowVariants,
                inherited = inherited,
                note = note
            )
            bomRepository.addBomItem(item)
            _uiState.update {
                it.copy(
                    isAddBomDialogOpen = false,
                    errorMessage = null,
                    successMessage = "تمت إضافة المكون لقائمة المواد بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }
}
