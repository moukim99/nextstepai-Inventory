package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PurchaseOrder
import com.nextstepai.inventory.data.PurchaseOrderLineItem
import com.nextstepai.inventory.repository.CompanyRepository
import com.nextstepai.inventory.repository.PartRepository
import com.nextstepai.inventory.repository.PurchaseOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * حالة واجهة أمر الشراء (PurchaseOrder UI State).
 */
data class PurchaseOrderUiState(
    val orders: List<PurchaseOrder> = emptyList(),
    val suppliers: List<Company> = emptyList(),
    val parts: List<Part> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: POStatus? = null,
    val selectedOrder: PurchaseOrder? = null,
    val isAddOrderDialogOpen: Boolean = false,
    val isAddLineDialogOpen: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * نموذج العرض (ViewModel) لشاشة أوامر الشراء وبنودها.
 */
class PurchaseOrderViewModel(
    private val poRepository: PurchaseOrderRepository = PurchaseOrderRepository(),
    private val companyRepository: CompanyRepository = CompanyRepository(),
    private val partRepository: PartRepository = PartRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseOrderUiState())
    val uiState: StateFlow<PurchaseOrderUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val suppliers = companyRepository.searchCompanies(supplierOnly = true)
        val parts = partRepository.getParts()
        val orders = poRepository.searchOrders(
            query = _uiState.value.searchQuery,
            status = _uiState.value.statusFilter
        )

        val updatedSelected = _uiState.value.selectedOrder?.let { sel ->
            orders.find { it.id == sel.id }
        }

        _uiState.update {
            it.copy(
                orders = orders,
                suppliers = suppliers,
                parts = parts,
                selectedOrder = updatedSelected
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadData()
    }

    fun setStatusFilter(status: POStatus?) {
        _uiState.update { it.copy(statusFilter = status) }
        loadData()
    }

    fun selectOrder(order: PurchaseOrder?) {
        _uiState.update { it.copy(selectedOrder = order) }
    }

    fun setAddOrderDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddOrderDialogOpen = isOpen, errorMessage = null) }
    }

    fun setAddLineDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddLineDialogOpen = isOpen, errorMessage = null) }
    }

    fun addPurchaseOrder(
        reference: String,
        supplierId: Long,
        description: String,
        targetDate: String,
        currency: String
    ) {
        try {
            val supplier = _uiState.value.suppliers.find { it.id == supplierId }
            val order = PurchaseOrder(
                reference = reference,
                supplierId = supplierId,
                supplierName = supplier?.name ?: "مورد #${supplierId}",
                description = description,
                targetDate = targetDate,
                orderCurrency = currency.ifBlank { supplier?.currency ?: "USD" }
            )
            poRepository.addOrder(order)
            _uiState.update {
                it.copy(
                    isAddOrderDialogOpen = false,
                    errorMessage = null,
                    successMessage = "تم إنشاء أمر الشراء '${reference}' بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun addLineItem(
        partId: Long,
        quantity: Double,
        price: Double,
        notes: String
    ) {
        val order = _uiState.value.selectedOrder
        if (order == null) {
            _uiState.update { it.copy(errorMessage = "يرجى اختيار أمر الشراء أولاً") }
            return
        }

        try {
            val part = _uiState.value.parts.find { it.id == partId }
            val item = PurchaseOrderLineItem(
                orderId = order.id,
                supplierPartId = partId,
                partName = part?.name ?: "قطعة #${partId}",
                quantity = quantity,
                purchasePrice = price,
                notes = notes
            )
            poRepository.addLineItem(item)
            _uiState.update {
                it.copy(
                    isAddLineDialogOpen = false,
                    errorMessage = null,
                    successMessage = "تمت إضافة البند لأمر الشراء بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun receiveLineItem(lineItemId: Long, receiveQty: Double) {
        try {
            poRepository.receiveLineItem(lineItemId, receiveQty)
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    successMessage = "تم استلام البند بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun issueOrder(orderId: Long) {
        poRepository.updateOrderStatus(orderId, POStatus.PLACED)
        _uiState.update { it.copy(successMessage = "تم اعتماد وإصدار أمر الشراء للمورد بنجاح") }
        loadData()
    }
}
