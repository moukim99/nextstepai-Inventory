package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import com.nextstepai.inventory.data.BuildItem
import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildOrderLineItem
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.StockLocation
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
    val stockLocations: List<StockLocation> = listOf(
        StockLocation(id = 1L, name = "المستودع الرئيسي (Main Warehouse)"),
        StockLocation(id = 2L, name = "خط التجميع والمكونات (Assembly Line A)"),
        StockLocation(id = 3L, name = "مخزن المنتجات المكتملة (Finished Goods)")
    ),
    val searchQuery: String = "",
    val statusFilter: BuildStatus? = null,
    val selectedBuild: BuildOrder? = null,
    val selectedLineItems: List<BuildOrderLineItem> = emptyList(),
    val allocatedBuildItems: List<BuildItem> = emptyList(),
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
        val lineItems = updatedSelected?.let { repository.getLineItemsForBuild(it.id) } ?: emptyList()
        val buildItems = updatedSelected?.let { repository.getBuildItemsForBuild(it.id) } ?: emptyList()

        _uiState.update {
            it.copy(
                builds = builds,
                assemblyParts = assemblies,
                selectedBuild = updatedSelected,
                selectedLineItems = lineItems,
                allocatedBuildItems = buildItems
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
        val lineItems = build?.let { repository.getLineItemsForBuild(it.id) } ?: emptyList()
        val buildItems = build?.let { repository.getBuildItemsForBuild(it.id) } ?: emptyList()
        _uiState.update {
            it.copy(
                selectedBuild = build,
                selectedLineItems = lineItems,
                allocatedBuildItems = buildItems
            )
        }
    }

    fun autoAllocateBuildOrder(buildId: Long) {
        val count = repository.autoAllocateBuildOrder(buildId)
        _uiState.update { it.copy(successMessage = "تم التخصيص الأوتوماتيكي لـ $count بند من بنود المخزون المتاحة بنجاح (FIFO)") }
        loadData()
    }

    fun allocateLineItemStock(lineItemId: Long, qty: Double) {
        try {
            repository.allocateStock(lineItemId, qty)
            _uiState.update { it.copy(successMessage = "تم حجز وتخصيص $qty وحدة من المخزون بنجاح") }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun consumeLineItemStock(lineItemId: Long, qty: Double) {
        try {
            repository.consumeStock(lineItemId, qty)
            _uiState.update { it.copy(successMessage = "تم تسجيل استهلاك $qty وحدة في عملية التجميع بنجاح") }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
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
        takeFromLocationId: Long? = null,
        destinationLocationId: Long? = null,
        parentId: Long? = null,
        salesOrderId: Long? = null,
        issuedBy: String = "",
        responsible: String = "",
        notes: String = "",
        link: String = ""
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
                takeFromLocationId = takeFromLocationId,
                destinationLocationId = destinationLocationId,
                parentId = parentId,
                salesOrderId = salesOrderId,
                issuedBy = issuedBy,
                responsible = responsible,
                notes = notes,
                link = link
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

    fun cancelBuildOrder(buildId: Long) {
        repository.cancelBuildOrder(buildId)
        _uiState.update { it.copy(successMessage = "تم إلغاء أمر التصنيع بنجاح") }
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

