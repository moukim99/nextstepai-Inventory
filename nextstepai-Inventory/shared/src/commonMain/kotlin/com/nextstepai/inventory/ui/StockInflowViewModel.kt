package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextstepai.inventory.data.ScannedInflowItem
import com.nextstepai.inventory.data.StockInflowSessionState
import com.nextstepai.inventory.data.db.PartEntity
import com.nextstepai.inventory.repository.StockInflowRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * مدير حالة واجهة الاستلام الميداني السريع (StockInflowViewModel).
 * يتناول معالجة الباركود ذكياً مع كابح التكرار (2000ms)، وشريط التراجع الخاطف (2500ms)، والاعتماد المجمع.
 */
class StockInflowViewModel(
    private val inflowRepository: StockInflowRepository = StockInflowRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockInflowSessionState())
    val uiState: StateFlow<StockInflowSessionState> = _uiState.asStateFlow()

    private val _detectedPart = MutableStateFlow<PartEntity?>(null)
    val detectedPart: StateFlow<PartEntity?> = _detectedPart.asStateFlow()

    private val _unrecognizedBarcode = MutableStateFlow<String?>(null)
    val unrecognizedBarcode: StateFlow<String?> = _unrecognizedBarcode.asStateFlow()

    private val _scannedImageBytes = MutableStateFlow<ByteArray?>(null)
    val scannedImageBytes: StateFlow<ByteArray?> = _scannedImageBytes.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var lastScannedBarcode: String = ""
    private var lastScanTimestamp: Long = 0L
    private val scanDebounceMillis = 2000L

    private var undoTimerJob: Job? = null
    private val undoDurationMillis = 2500L

    init {
        loadDefaultLocation()
    }

    private fun loadDefaultLocation() {
        viewModelScope.launch {
            val (locId, locName) = inflowRepository.getDefaultReceivingLocation()
            _uiState.update { it.copy(defaultLocationId = locId, defaultLocationName = locName) }
        }
    }

    /**
     * إعداد الجلسة بنوع الاستلام وأمر الشراء المرجعي
     */
    fun startSession(inflowTypeId: String, purchaseOrderId: Long? = null, poReference: String? = null) {
        _uiState.update {
            it.copy(
                selectedInflowTypeId = inflowTypeId,
                purchaseOrderId = purchaseOrderId,
                poReference = poReference,
                scannedItems = emptyList(),
                lastAddedItemForUndo = null,
                undoWindowActive = false
            )
        }
        _detectedPart.value = null
        _unrecognizedBarcode.value = null
    }

    /**
     * معالجة الباركود الملتقط من الكاميرا مع كابح التكرار (Debounce)
     */
    fun onBarcodeScanned(barcode: String, croppedImageBytes: ByteArray? = null) {
        val now = Clock.System.now().toEpochMilliseconds()
        val cleanBarcode = barcode.trim()

        if (cleanBarcode.isBlank()) return

        // كابح التكرار لمنع القراءة المتكررة لنفس الباركود خلال ثانيتين
        if (cleanBarcode == lastScannedBarcode && (now - lastScanTimestamp) < scanDebounceMillis) {
            return
        }

        lastScannedBarcode = cleanBarcode
        lastScanTimestamp = now
        _scannedImageBytes.value = croppedImageBytes

        viewModelScope.launch {
            val matchedPart = inflowRepository.findPartByBarcode(cleanBarcode)
            if (matchedPart != null) {
                _detectedPart.value = matchedPart
                _unrecognizedBarcode.value = null
            } else {
                _unrecognizedBarcode.value = cleanBarcode
                _detectedPart.value = null
            }
        }
    }

    /**
     * إضافة كمية لصنف مسجل معروف في قاعدة البيانات
     */
    fun addQuantityForKnownPart(part: PartEntity, quantity: Double) {
        val currentDefaultLocId = _uiState.value.defaultLocationId
        val currentDefaultLocName = _uiState.value.defaultLocationName

        val newItem = ScannedInflowItem(
            barcode = part.ipn.ifBlank { part.uuid },
            partId = part.uuid.removePrefix("part-").toLongOrNull() ?: 1L,
            partUuid = part.uuid,
            name = part.name,
            quantity = quantity,
            locationId = currentDefaultLocId,
            locationName = currentDefaultLocName,
            isNewPart = false,
            croppedImageBytes = _scannedImageBytes.value
        )

        addItemToSessionWithUndo(newItem)
        _detectedPart.value = null
        _unrecognizedBarcode.value = null
    }

    /**
     * إضافة صنف جديد غير مسجل في قاعدة البيانات
     */
    fun addNewUnregisteredPart(partName: String, quantity: Double, barcode: String) {
        val currentLocId = _uiState.value.defaultLocationId
        val currentLocName = _uiState.value.defaultLocationName

        val newItem = ScannedInflowItem(
            barcode = barcode.ifBlank { "NEW-${Clock.System.now().toEpochMilliseconds()}" },
            name = partName.ifBlank { "صنف جديد ($barcode)" },
            quantity = quantity,
            locationId = currentLocId,
            locationName = currentLocName,
            isNewPart = true,
            croppedImageBytes = _scannedImageBytes.value
        )

        addItemToSessionWithUndo(newItem)
        _unrecognizedBarcode.value = null
        _detectedPart.value = null
    }

    private fun addItemToSessionWithUndo(item: ScannedInflowItem) {
        _uiState.update { it.addItem(item) }
        _statusMessage.value = "تمت إضافة ${item.quantity.toInt()} × ${item.name}"

        // بدء مؤقت التراجع الخاطف لمدة 2.5 ثوانٍ
        undoTimerJob?.cancel()
        undoTimerJob = viewModelScope.launch {
            delay(undoDurationMillis)
            _uiState.update { it.clearUndoBuffer() }
        }
    }

    /**
     * التراجع عن إضافة العنصر الأخير
     */
    fun undoLastAddedItem() {
        val lastItem = _uiState.value.lastAddedItemForUndo
        if (lastItem != null) {
            _uiState.update { it.undoLastAddedItem() }
            _statusMessage.value = "تم التراجع عن إضافة ${lastItem.name}"
            undoTimerJob?.cancel()
        }
    }

    fun removeItem(tempId: String) {
        _uiState.update { it.removeItem(tempId) }
    }

    fun updateItemQuantity(tempId: String, newQty: Double) {
        _uiState.update { it.updateItemQuantity(tempId, newQty) }
    }

    fun dismissDetectedCard() {
        _detectedPart.value = null
        _unrecognizedBarcode.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * اعتماد وترحيل جلسة الاستلام بالكامل ضمن المعاملة الذرية
     */
    fun commitSession(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentState = _uiState.value
        if (currentState.scannedItems.isEmpty()) {
            onError("السلة فارغة، يرجى مسح أصناف أولاً")
            return
        }

        _uiState.update { it.copy(isProcessingCommit = true) }

        viewModelScope.launch {
            val result = inflowRepository.commitInflowSession(currentState)
            _uiState.update { it.copy(isProcessingCommit = false) }

            result.fold(
                onSuccess = {
                    _uiState.update { StockInflowSessionState() }
                    _detectedPart.value = null
                    _unrecognizedBarcode.value = null
                    _statusMessage.value = "تم اعتماد وترحيل الشحنة بنجاح!"
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "حدث خطأ أثناء الترحيل")
                }
            )
        }
    }
}
