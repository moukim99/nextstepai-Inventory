package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.repository.BomRepository
import com.nextstepai.inventory.repository.PartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * حالة واجهة شاشة إدارة قائمة مواد التصنيع (BOM UI State).
 */
data class BomUiState(
    val selectedPartId: Long? = 3L, // افتراضياً قطعة الماكينة المصنعة
    val parentParts: List<Part> = emptyList(),
    val availableComponents: List<Part> = emptyList(),
    val bomItems: List<BomItem> = emptyList(),
    val isAddBomDialogOpen: Boolean = false,
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

        _uiState.update {
            it.copy(
                selectedPartId = currentPartId,
                parentParts = assemblies,
                availableComponents = components,
                bomItems = bomList
            )
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
