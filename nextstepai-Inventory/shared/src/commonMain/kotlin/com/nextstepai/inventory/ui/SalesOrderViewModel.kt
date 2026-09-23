package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.SOStatus
import com.nextstepai.inventory.data.SalesOrder
import com.nextstepai.inventory.data.SalesOrderLineItem
import com.nextstepai.inventory.repository.CompanyRepository
import com.nextstepai.inventory.repository.PartRepository
import com.nextstepai.inventory.repository.SalesOrderFulfillmentResult
import com.nextstepai.inventory.repository.SalesOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * حالة واجهة إدارة المبيعات ومحرك التلبية (SalesOrder UI State).
 */
data class SalesOrderUiState(
    val orders: List<SalesOrder> = emptyList(),
    val customers: List<Company> = emptyList(),
    val parts: List<Part> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: SOStatus? = null,
    val selectedOrder: SalesOrder? = null,
    val isAddOrderDialogOpen: Boolean = false,
    val lastFulfillmentResult: SalesOrderFulfillmentResult? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * نموذج العرض (ViewModel) لشاشة إدارة المبيعات ومحرك التلبية الآلي.
 */
class SalesOrderViewModel(
    private val salesRepository: SalesOrderRepository = SalesOrderRepository(),
    private val companyRepository: CompanyRepository = CompanyRepository(),
    private val partRepository: PartRepository = PartRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesOrderUiState())
    val uiState: StateFlow<SalesOrderUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val customers = companyRepository.searchCompanies(customerOnly = true)
        val parts = partRepository.getParts().filter { it.salable || it.component || it.assembly }
        val orders = salesRepository.searchOrders(
            query = _uiState.value.searchQuery,
            status = _uiState.value.statusFilter
        )

        val updatedSelected = _uiState.value.selectedOrder?.let { sel ->
            orders.find { it.id == sel.id }
        }

        _uiState.update {
            it.copy(
                orders = orders,
                customers = customers,
                parts = parts,
                selectedOrder = updatedSelected
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadData()
    }

    fun setStatusFilter(status: SOStatus?) {
        _uiState.update { it.copy(statusFilter = status) }
        loadData()
    }

    fun selectOrder(order: SalesOrder?) {
        _uiState.update { it.copy(selectedOrder = order) }
    }

    fun setAddOrderDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddOrderDialogOpen = isOpen, errorMessage = null) }
    }

    fun addSalesOrderWithFulfillment(
        reference: String,
        customerId: Long,
        description: String,
        targetDate: String,
        currency: String,
        lines: List<SalesOrderLineItem>
    ) {
        try {
            val customer = _uiState.value.customers.find { it.id == customerId }
            val order = SalesOrder(
                reference = reference,
                customerId = customerId,
                customerName = customer?.name ?: "عميل #${customerId}",
                description = description,
                targetDate = targetDate,
                orderCurrency = currency.ifBlank { customer?.currency ?: "USD" },
                status = SOStatus.APPROVED,
                lineItems = lines
            )

            val result = salesRepository.addSalesOrderWithFulfillment(order, autoFulfill = true)
            _uiState.update {
                it.copy(
                    isAddOrderDialogOpen = false,
                    lastFulfillmentResult = result,
                    errorMessage = null,
                    successMessage = result.summaryMessage
                )
            }
            loadData()
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }
}
