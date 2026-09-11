package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.repository.BuildOrderRepository
import com.nextstepai.inventory.repository.PartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * حالة واجهة إدارة أوامر الإنتاج والتصنيع (BuildOrder UI State).
 */
data class BuildOrderUiState(
    val builds: List<BuildOrder> = emptyList(),
    val assemblyParts: List<Part> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: BuildStatus? = null,
    val selectedBuild: BuildOrder? = null,
    val isAddBuildDialogOpen: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * نموذج العرض (ViewModel) لشاشة أوامر الإنتاج والتصنيع.
 */
class BuildOrderViewModel(
    private val repository: BuildOrderRepository = BuildOrderRepository(),
    private val partRepository: PartRepository = PartRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuildOrderUiState())
    val uiState: StateFlow<BuildOrderUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val assemblies = partRepository.getParts().filter { it.assembly }
        val builds = repository.searchBuilds(
            query = _uiState.value.searchQuery,
            status = _uiState.value.statusFilter
        )

        val updatedSelected = _uiState.value.selectedBuild?.let { sel ->
            builds.find { it.id == sel.id }
        }

        _uiState.update {
            it.copy(
                builds = builds,
                assemblyParts = assemblies,
                selectedBuild = updatedSelected
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadData()
    }

    fun setStatusFilter(status: BuildStatus?) {
        _uiState.update { it.copy(statusFilter = status) }
        loadData()
    }

    fun selectBuild(build: BuildOrder?) {
        _uiState.update { it.copy(selectedBuild = build) }
    }

    fun setAddDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddBuildDialogOpen = isOpen, errorMessage = null) }
    }

    fun addBuildOrder(
        reference: String,
        title: String,
        partId: Long,
        quantity: Double,
        batch: String,
        targetDate: String,
        notes: String
    ) {
        try {
            val part = _uiState.value.assemblyParts.find { it.id == partId }
            val build = BuildOrder(
                reference = reference,
                title = title,
                partId = partId,
                partName = part?.name ?: "قطعة مجمعة #${partId}",
                quantity = quantity,
                batch = batch,
                targetDate = targetDate,
                notes = notes
            )
            repository.addBuildOrder(build)
            _uiState.update {
                it.copy(
                    isAddBuildDialogOpen = false,
                    errorMessage = null,
                    successMessage = "تم إنشاء أمر الإنتاج '${reference}' بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun startProduction(buildId: Long) {
        repository.startProduction(buildId)
        _uiState.update { it.copy(successMessage = "تم بدء عملية التصنيع والإنتاج الفعلي") }
        loadData()
    }

    fun completeBuildOutput(buildId: Long, qty: Double) {
        try {
            repository.completeBuildOutput(buildId, qty)
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    successMessage = "تم إنهاء وتوريد $qty وحدة من أصل أمر الإنتاج بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }
}
