package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.StockStatus
import com.nextstepai.inventory.repository.PartRepository
import com.nextstepai.inventory.repository.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * حالة واجهة إدارة المخزون الفعلي ومواقع التخزين (Stock UI State).
 */
data class StockUiState(
    val stockItems: List<StockItem> = emptyList(),
    val locations: List<StockLocation> = emptyList(),
    val parts: List<Part> = emptyList(),
    val selectedLocationId: Long? = null,
    val selectedPartId: Long? = null,
    val isAddStockDialogOpen: Boolean = false,
    val isAddLocationDialogOpen: Boolean = false,
    val selectedItemForSplit: StockItem? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * نموذج العرض (ViewModel) لشاشة إدارة المخزون الفعلي والمواقع (Stock Management).
 */
class StockViewModel(
    private val stockRepository: StockRepository = StockRepository(),
    private val partRepository: PartRepository = PartRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockUiState())
    val uiState: StateFlow<StockUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val allParts = partRepository.getParts()
        val locations = stockRepository.getLocations()
        val allStock = stockRepository.getStockItems().filter { item ->
            (_uiState.value.selectedLocationId == null || item.locationId == _uiState.value.selectedLocationId) &&
                    (_uiState.value.selectedPartId == null || item.partId == _uiState.value.selectedPartId)
        }

        _uiState.update {
            it.copy(
                stockItems = allStock,
                locations = locations,
                parts = allParts
            )
        }
    }

    fun filterByLocation(locationId: Long?) {
        _uiState.update { it.copy(selectedLocationId = locationId) }
        loadData()
    }

    fun filterByPart(partId: Long?) {
        _uiState.update { it.copy(selectedPartId = partId) }
        loadData()
    }

    fun setAddDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddStockDialogOpen = isOpen, errorMessage = null) }
    }

    fun setAddLocationDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddLocationDialogOpen = isOpen, errorMessage = null) }
    }

    fun setSelectedItemForSplit(item: StockItem?) {
        _uiState.update { it.copy(selectedItemForSplit = item, errorMessage = null) }
    }

    fun addLocation(
        name: String,
        description: String = "",
        parentId: Long? = null,
        structural: Boolean = false,
        external: Boolean = false,
        icon: String = "warehouse"
    ) {
        try {
            val loc = StockLocation(
                name = name,
                description = description,
                parentId = parentId,
                structural = structural,
                external = external,
                icon = icon
            )
            stockRepository.addLocation(loc)
            _uiState.update {
                it.copy(
                    isAddLocationDialogOpen = false,
                    errorMessage = null,
                    successMessage = "تم إنشاء موقع التخزين بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun addStockItem(
        partId: Long,
        locationId: Long?,
        quantity: Double,
        serial: String = "",
        batch: String = "",
        packaging: String = "Box",
        status: StockStatus = StockStatus.OK,
        purchasePrice: Double = 0.0,
        purchasePriceCurrency: String = "USD",
        expiryDate: String = "",
        reviewNeeded: Boolean = false,
        deleteOnDeplete: Boolean = false,
        link: String = "",
        notes: String = ""
    ) {
        try {
            val item = StockItem(
                partId = partId,
                locationId = locationId,
                quantity = quantity,
                serial = serial,
                batch = batch,
                packaging = packaging.ifBlank { "Box" },
                status = status,
                purchasePrice = purchasePrice,
                purchasePriceCurrency = purchasePriceCurrency,
                expiryDate = expiryDate,
                reviewNeeded = reviewNeeded,
                deleteOnDeplete = deleteOnDeplete,
                link = link,
                notes = notes
            )
            stockRepository.addStockItem(item)
            _uiState.update {
                it.copy(
                    isAddStockDialogOpen = false,
                    errorMessage = null,
                    successMessage = "تم إضافة الرصيد المخزني بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun splitStockItem(parentId: Long, splitQty: Double) {
        try {
            stockRepository.splitStockItem(parentId, splitQty)
            _uiState.update {
                it.copy(
                    selectedItemForSplit = null,
                    errorMessage = null,
                    successMessage = "تمت تجزئة الكمية المخزنية بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun performStocktake(stockId: Long, userId: Long = 1L, stocktakeDate: String = "2025-02-15") {
        try {
            stockRepository.performStocktake(stockId, userId, stocktakeDate)
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    successMessage = "تم تسجيل عملية الجرد للقطعة بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }
}
