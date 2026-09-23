package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextstepai.inventory.data.AppUser
import com.nextstepai.inventory.data.BuildItem
import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildOrderLineItem
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.ManufacturingPhase
import com.nextstepai.inventory.data.ManufacturingPhaseTable
import com.nextstepai.inventory.repository.BuildOrderRepository
import com.nextstepai.inventory.repository.PartRepository
import com.nextstepai.inventory.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * نموذج بيانات أمر البيع والطلب المخصص للزبائن (SalesOrder).
 */
data class SalesOrder(
    val id: Long,
    val reference: String,
    val customerName: String,
    val description: String = "",
    val quantity: Double = 1.0,
    val targetDate: String = ""
)

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
    val salesOrders: List<SalesOrder> = listOf(
        SalesOrder(id = 101L, reference = "SO-2025-101", customerName = "شركة التقنيات المتقدمة", description = "طلب توريد 50 وحدة جهاز تحكم ذكي", quantity = 50.0, targetDate = "2025-03-30"),
        SalesOrder(id = 102L, reference = "SO-2025-102", customerName = "مؤسسة الحلول الصناعية", description = "طلب تصنيع 20 لوحة تحكم رئيسية v2", quantity = 20.0, targetDate = "2025-04-10"),
        SalesOrder(id = 103L, reference = "SO-2025-103", customerName = "شركة الأوتوماتيك والتحكم", description = "طلب تجميع مستشعرات حرارة ورطوبة", quantity = 100.0, targetDate = "2025-04-15")
    ),
    val users: List<AppUser> = emptyList(),
    val searchQuery: String = "",
    val selectedPartId: Long? = null,
    val statusFilter: BuildStatus? = null,
    val isFilterBottomSheetOpen: Boolean = false,
    val selectedBuild: BuildOrder? = null,
    val selectedLineItems: List<BuildOrderLineItem> = emptyList(),
    val allocatedBuildItems: List<BuildItem> = emptyList(),
    val isAddBuildDialogOpen: Boolean = false,
    val isSelectPhaseBottomSheetOpen: Boolean = false,
    val selectedPhaseUuid: String? = null,
    val phases: List<ManufacturingPhase> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * نموذج العرض (ViewModel) لشاشة أوامر الإنتاج والتصنيع.
 */
class BuildOrderViewModel(
    private val repository: BuildOrderRepository = BuildOrderRepository(),
    private val partRepository: PartRepository = PartRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val phaseTable: ManufacturingPhaseTable = ManufacturingPhaseTable()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuildOrderUiState())
    val uiState: StateFlow<BuildOrderUiState> = _uiState.asStateFlow()

    init {
        loadData()
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            val activeUsers = userRepository.getActiveUsers()
            _uiState.update { it.copy(users = activeUsers) }
        }
    }

    fun loadData() {
        val assemblies = partRepository.getParts().filter { it.assembly }
        var builds = repository.searchBuilds(
            query = _uiState.value.searchQuery,
            status = _uiState.value.statusFilter
        )
        if (_uiState.value.selectedPartId != null) {
            builds = builds.filter { it.partId == _uiState.value.selectedPartId }
        }

        val updatedSelected = _uiState.value.selectedBuild?.let { sel ->
            builds.find { it.id == sel.id }
        }
        val lineItems = updatedSelected?.let { repository.getLineItemsForBuild(it.id) } ?: emptyList()
        val buildItems = updatedSelected?.let { repository.getBuildItemsForBuild(it.id) } ?: emptyList()
        val allPhases = phaseTable.getAllPhases().sortedBy { it.sequenceOrder }
        val activePhaseUuid = _uiState.value.selectedPhaseUuid ?: allPhases.firstOrNull()?.uuid

        _uiState.update {
            it.copy(
                builds = builds,
                assemblyParts = assemblies,
                selectedBuild = updatedSelected,
                selectedLineItems = lineItems,
                allocatedBuildItems = buildItems,
                phases = allPhases,
                selectedPhaseUuid = activePhaseUuid
            )
        }
    }

    fun setFilterBottomSheetOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isFilterBottomSheetOpen = isOpen) }
    }

    fun applyFilters(partId: Long?, status: BuildStatus?) {
        _uiState.update {
            it.copy(
                selectedPartId = partId,
                statusFilter = status,
                isFilterBottomSheetOpen = false
            )
        }
        loadData()
    }

    fun resetFilters() {
        _uiState.update {
            it.copy(
                selectedPartId = null,
                statusFilter = null
            )
        }
        loadData()
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

    fun setSelectPhaseBottomSheetOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isSelectPhaseBottomSheetOpen = isOpen) }
    }

    fun selectPhase(phaseUuid: String) {
        _uiState.update { it.copy(selectedPhaseUuid = phaseUuid, isSelectPhaseBottomSheetOpen = false) }
    }

    fun transferUnitsToNextPhase(buildId: Long, currentPhaseUuid: String, quantity: Double) {
        val build = _uiState.value.builds.find { it.id == buildId } ?: return
        val phasesList = _uiState.value.phases
        val currentPhaseIdx = phasesList.indexOfFirst { it.uuid == currentPhaseUuid }
        if (currentPhaseIdx == -1) return

        val currentQty = build.phaseQuantities[currentPhaseUuid] ?: 0.0
        val finalQty = quantity.coerceAtMost(currentQty)
        
        if (finalQty <= 0) return

        val newQuantities = build.phaseQuantities.toMutableMap()
        newQuantities[currentPhaseUuid] = currentQty - finalQty

        val isLastPhase = currentPhaseIdx == phasesList.lastIndex
        if (isLastPhase) {
            val newCompleted = build.completedQuantity + finalQty
            val updatedBuild = build.copy(
                completedQuantity = newCompleted,
                phaseQuantities = newQuantities,
                status = if (newCompleted >= build.quantity) BuildStatus.COMPLETE else build.status
            )
            repository.updateBuild(updatedBuild)
        } else {
            val nextPhaseUuid = phasesList[currentPhaseIdx + 1].uuid
            newQuantities[nextPhaseUuid] = (newQuantities[nextPhaseUuid] ?: 0.0) + finalQty
            val updatedBuild = build.copy(phaseQuantities = newQuantities)
            repository.updateBuild(updatedBuild)
        }
        loadData()
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
        startDate: String = "",
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
                startDate = startDate,
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

    fun updateBuildStatus(buildId: Long, newStatus: BuildStatus) {
        try {
            repository.updateBuildStatus(buildId, newStatus)
            val msg = when (newStatus) {
                BuildStatus.IN_PRODUCTION -> "تم بدء واستئناف عملية التصنيع والإنتاج الفعلي"
                BuildStatus.CANCELLED -> "تم إيقاف/إلغاء أمر التصنيع بنجاح"
                BuildStatus.COMPLETE -> "تم إكتمال وتوريد أمر التصنيع بنجاح"
                else -> "تم تحديث حالة أمر التصنيع بنجاح"
            }
            _uiState.update { it.copy(successMessage = msg) }
            loadData()
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
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

