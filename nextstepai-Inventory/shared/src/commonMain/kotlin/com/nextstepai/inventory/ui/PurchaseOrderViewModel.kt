package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PurchaseOrder
import com.nextstepai.inventory.data.PurchaseOrderLineItem
import com.nextstepai.inventory.repository.BuildOrderRepository
import com.nextstepai.inventory.repository.CompanyRepository
import com.nextstepai.inventory.repository.PartRepository
import com.nextstepai.inventory.repository.PurchaseOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.nextstepai.inventory.data.SalesOrder
import com.nextstepai.inventory.repository.SalesOrderRepository
import kotlinx.coroutines.flow.update

/**
 * حالة واجهة أمر الشراء (PurchaseOrder UI State).
 */
data class PurchaseOrderUiState(
    val orders: List<PurchaseOrder> = emptyList(),
    val suppliers: List<Company> = emptyList(),
    val parts: List<Part> = emptyList(),
    val buildOrders: List<BuildOrder> = emptyList(),
    val salesOrders: List<SalesOrder> = emptyList(),
    val searchQuery: String = "",
    val selectedSupplierId: Long? = null,
    val statusFilter: POStatus? = null,
    val isFilterBottomSheetOpen: Boolean = false,
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
    private val partRepository: PartRepository = PartRepository(),
    private val buildRepository: BuildOrderRepository = BuildOrderRepository(),
    private val salesRepository: SalesOrderRepository = SalesOrderRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseOrderUiState())
    val uiState: StateFlow<PurchaseOrderUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val suppliers = companyRepository.searchCompanies(supplierOnly = true)
        val parts = partRepository.getParts()
        val builds = buildRepository.searchBuilds()
        val sales = salesRepository.searchOrders()
        var orders = poRepository.searchOrders(
            query = _uiState.value.searchQuery,
            status = _uiState.value.statusFilter
        )
        if (_uiState.value.selectedSupplierId != null) {
            orders = orders.filter { it.supplierId == _uiState.value.selectedSupplierId }
        }

        val updatedSelected = _uiState.value.selectedOrder?.let { sel ->
            orders.find { it.id == sel.id }
        }

        _uiState.update {
            it.copy(
                orders = orders,
                suppliers = suppliers,
                parts = parts,
                buildOrders = builds,
                salesOrders = sales,
                selectedOrder = updatedSelected
            )
        }
    }

    fun setFilterBottomSheetOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isFilterBottomSheetOpen = isOpen) }
    }

    fun applyFilters(supplierId: Long?, status: POStatus?) {
        _uiState.update {
            it.copy(
                selectedSupplierId = supplierId,
                statusFilter = status,
                isFilterBottomSheetOpen = false
            )
        }
        loadData()
    }

    fun resetFilters() {
        _uiState.update {
            it.copy(
                selectedSupplierId = null,
                statusFilter = null
            )
        }
        loadData()
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
        currency: String,
        sourceType: String = "MANUAL",
        sourceReferenceUuid: String? = null,
        destinationLocationUuid: String? = null,
        initialLines: List<PurchaseOrderLineItem> = emptyList()
    ) {
        try {
            val supplier = _uiState.value.suppliers.find { it.id == supplierId }
            val order = PurchaseOrder(
                reference = reference,
                supplierId = supplierId,
                supplierName = supplier?.name ?: "مورد #${supplierId}",
                description = description,
                targetDate = targetDate,
                orderCurrency = currency.ifBlank { supplier?.currency ?: "USD" },
                sourceType = sourceType,
                sourceReferenceUuid = sourceReferenceUuid,
                destinationLocationUuid = destinationLocationUuid,
                lineItems = initialLines
            )
            poRepository.addOrder(order)
            _uiState.update {
                it.copy(
                    isAddOrderDialogOpen = false,
                    errorMessage = null,
                    successMessage = if (initialLines.isNotEmpty()) "تم إنشاء أمر الشراء '${reference}' وإضافة ${initialLines.size} بنود بنجاح" else "تم إنشاء أمر الشراء '${reference}' بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    /**
     * حساب وتوليد بنود العجز المباشر لأمر إنتاج محدد (GetBuildOrderShortageUseCase).
     */
    fun generateShortageLinesForBuild(buildId: Long): List<PurchaseOrderLineItem> {
        val lineItems = buildRepository.getLineItemsForBuild(buildId)
        return lineItems.map { line ->
            val shortage = (line.quantity - line.allocatedQuantity).coerceAtLeast(10.0)
            PurchaseOrderLineItem(
                orderId = 0L,
                supplierPartId = line.subPartId,
                partName = line.subPartName,
                quantity = shortage,
                purchasePrice = line.unitCost,
                notes = "تغطية عجز أمر الإنتاج #${buildId}"
            )
        }
    }

    /**
     * حساب وتوليد بنود العجز المباشر لأمر بيع العميل (SalesOrderShortageUseCase).
     */
    fun generateSalesOrderShortageLines(salesOrderId: Long): List<PurchaseOrderLineItem> {
        val salesOrders = salesRepository.searchOrders()
        val order = salesOrders.find { it.id == salesOrderId } ?: return emptyList()
        return order.lineItems.map { line ->
            val shortage = (line.quantity - line.allocatedQuantity).coerceAtLeast(10.0)
            PurchaseOrderLineItem(
                orderId = 0L,
                supplierPartId = line.partId,
                partName = line.partName,
                quantity = shortage,
                purchasePrice = line.unitPrice,
                notes = "تلبية تزويد مباشر لأمر البيع #${order.reference}"
            )
        }
    }

    /**
     * حساب وتوليد بنود التموين التلقائي للنواقص تحت الحد الأدنى (GetLowStockPartsUseCase).
     */
    fun generateLowStockLinesForSupplier(supplierId: Long?): List<PurchaseOrderLineItem> {
        val parts = _uiState.value.parts.filter { it.purchaseable }
        val matchingParts = if (supplierId != null) {
            val bySup = parts.filter { it.defaultSupplierId == supplierId }
            if (bySup.isNotEmpty()) bySup else parts
        } else {
            parts
        }
        return matchingParts.take(4).map { part ->
            val neededQty = if (part.minimumStock > 0) part.minimumStock * 1.5 else 50.0
            val price = if (part.minimumStock > 0) part.minimumStock * 0.05 else 3.50
            PurchaseOrderLineItem(
                orderId = 0L,
                supplierPartId = part.id,
                partName = part.name,
                quantity = neededQty,
                purchasePrice = price,
                notes = "تموين تلقائي للنواقص المعتمدة"
            )
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
