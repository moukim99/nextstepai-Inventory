package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockItemAttachment
import com.nextstepai.inventory.data.StockItemTestResult
import com.nextstepai.inventory.data.StockItemTracking
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.StockLocationType
import com.nextstepai.inventory.data.StockStatus
import com.nextstepai.inventory.repository.PartRepository
import com.nextstepai.inventory.repository.StockRepository
import com.nextstepai.inventory.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * حالة واجهة إدارة المخزون الفعلي ومواقع التخزين وأنواعها وسجلات التتبع وفحوصات الجودة والمرفقات (Stock UI State).
 */
data class StockUiState(
    val stockItems: List<StockItem> = emptyList(),
    val locations: List<StockLocation> = emptyList(),
    val locationTypes: List<StockLocationType> = emptyList(),
    val parts: List<Part> = emptyList(),
    val selectedLocationId: Long? = null,
    val selectedPartId: Long? = null,
    val isAddStockDialogOpen: Boolean = false,
    val isAddLocationDialogOpen: Boolean = false,
    val isAddLocationTypeDialogOpen: Boolean = false,
    val isAddTestResultDialogOpen: Boolean = false,
    val isAddAttachmentDialogOpen: Boolean = false,
    val selectedItemForSplit: StockItem? = null,
    val selectedItemForHistory: StockItem? = null,
    val trackingLogsForSelected: List<StockItemTracking> = emptyList(),
    val selectedItemForTests: StockItem? = null,
    val testResultsForSelected: List<StockItemTestResult> = emptyList(),
    val selectedItemForAttachments: StockItem? = null,
    val attachmentsForSelected: List<StockItemAttachment> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * نموذج العرض (ViewModel) لشاشة إدارة المخزون والمرفقات وفحوصات الجودة (Stock Management).
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
        val locationTypes = stockRepository.getLocationTypes()
        val allStock = stockRepository.getStockItems().filter { item ->
            (_uiState.value.selectedLocationId == null || item.locationId == _uiState.value.selectedLocationId) &&
                    (_uiState.value.selectedPartId == null || item.partId == _uiState.value.selectedPartId)
        }

        _uiState.update {
            it.copy(
                stockItems = allStock,
                locations = locations,
                locationTypes = locationTypes,
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

    fun setAddLocationTypeDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddLocationTypeDialogOpen = isOpen, errorMessage = null) }
    }

    fun setAddTestResultDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddTestResultDialogOpen = isOpen, errorMessage = null) }
    }

    fun setAddAttachmentDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddAttachmentDialogOpen = isOpen, errorMessage = null) }
    }

    fun setSelectedItemForSplit(item: StockItem?) {
        _uiState.update { it.copy(selectedItemForSplit = item, errorMessage = null) }
    }

    fun openTrackingHistory(item: StockItem) {
        val logs = stockRepository.getTrackingForStockItem(item.id)
        _uiState.update {
            it.copy(
                selectedItemForHistory = item,
                trackingLogsForSelected = logs,
                errorMessage = null
            )
        }
    }

    fun closeTrackingHistory() {
        _uiState.update {
            it.copy(
                selectedItemForHistory = null,
                trackingLogsForSelected = emptyList()
            )
        }
    }

    fun openTestResults(item: StockItem) {
        val results = stockRepository.getTestResultsForStockItem(item.id)
        _uiState.update {
            it.copy(
                selectedItemForTests = item,
                testResultsForSelected = results,
                errorMessage = null
            )
        }
    }

    fun closeTestResults() {
        _uiState.update {
            it.copy(
                selectedItemForTests = null,
                testResultsForSelected = emptyList()
            )
        }
    }

    fun openAttachments(item: StockItem) {
        val attachments = stockRepository.getAttachmentsForStockItem(item.id)
        _uiState.update {
            it.copy(
                selectedItemForAttachments = item,
                attachmentsForSelected = attachments,
                errorMessage = null
            )
        }
    }

    fun closeAttachments() {
        _uiState.update {
            it.copy(
                selectedItemForAttachments = null,
                attachmentsForSelected = emptyList()
            )
        }
    }

    fun addStockItemAttachment(
        stockItemId: Long,
        attachmentPath: String?,
        link: String?,
        comment: String
    ) {
        try {
            val attachmentItem = StockItemAttachment(
                stockItemId = stockItemId,
                attachment = attachmentPath,
                link = link,
                comment = comment
            )
            stockRepository.addStockItemAttachment(attachmentItem)
            _uiState.update {
                it.copy(
                    isAddAttachmentDialogOpen = false,
                    errorMessage = null,
                    successMessage = "تم إضافة المرفق المخزني بنجاح"
                )
            }
            if (_uiState.value.selectedItemForAttachments?.id == stockItemId) {
                openAttachments(_uiState.value.selectedItemForAttachments!!)
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun deleteStockItemAttachment(attachmentId: Long, stockItemId: Long) {
        try {
            stockRepository.deleteStockItemAttachment(attachmentId)
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    successMessage = "تم حذف المرفق بنجاح"
                )
            }
            if (_uiState.value.selectedItemForAttachments?.id == stockItemId) {
                openAttachments(_uiState.value.selectedItemForAttachments!!)
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun addTestResult(
        stockItemId: Long,
        test: String,
        result: Boolean,
        value: String,
        attachment: String = "",
        notes: String = ""
    ) {
        try {
            val testResult = StockItemTestResult(
                stockItemId = stockItemId,
                test = test,
                result = result,
                value = value,
                attachment = attachment,
                notes = notes
            )
            stockRepository.addTestResult(testResult)
            _uiState.update {
                it.copy(
                    isAddTestResultDialogOpen = false,
                    errorMessage = null,
                    successMessage = "تم إضافة نتيجة الفحص الفني بنجاح"
                )
            }
            if (_uiState.value.selectedItemForTests?.id == stockItemId) {
                openTestResults(_uiState.value.selectedItemForTests!!)
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun addLocationType(
        name: String,
        description: String = "",
        icon: String = "warehouse",
        customIcon: String = ""
    ) {
        try {
            val type = StockLocationType(
                name = name,
                description = description,
                icon = icon,
                customIcon = customIcon
            )
            stockRepository.addLocationType(type)
            _uiState.update {
                it.copy(
                    isAddLocationTypeDialogOpen = false,
                    errorMessage = null,
                    successMessage = "تم إضافة نوع موقع التخزين بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun addLocation(
        name: String,
        description: String = "",
        parentId: Long? = null,
        structural: Boolean = false,
        external: Boolean = false,
        locationTypeId: Long? = null,
        icon: String = "warehouse"
    ) {
        try {
            val loc = StockLocation(
                name = name,
                description = description,
                parentId = parentId,
                structural = structural,
                external = external,
                locationTypeId = locationTypeId,
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

    fun performStocktake(stockId: Long, userId: Long = 1L, stocktakeDate: String = DateTimeUtils.getCurrentDate()) {
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
