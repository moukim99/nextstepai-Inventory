package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextstepai.inventory.data.AppUser
import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartCategory
import com.nextstepai.inventory.data.PurchaseOrder
import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockItemAttachment
import com.nextstepai.inventory.data.StockItemTestResult
import com.nextstepai.inventory.data.StockItemTracking
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.StockLocationType
import com.nextstepai.inventory.data.capacityUnit
import com.nextstepai.inventory.data.withCapacityUnit
import com.nextstepai.inventory.data.contactPerson
import com.nextstepai.inventory.data.contactPhone
import com.nextstepai.inventory.data.withContactInfo
import com.nextstepai.inventory.data.isPrimary
import com.nextstepai.inventory.data.withPrimary
import com.nextstepai.inventory.data.IntermediateNodeSpec
import com.nextstepai.inventory.data.withWeightInfo
import com.nextstepai.inventory.data.unitWeight
import com.nextstepai.inventory.data.totalWeight
import com.nextstepai.inventory.data.calculatePhysicalOccupancy
import com.nextstepai.inventory.data.getOccupancySummary
import com.nextstepai.inventory.data.StockStatus
import com.nextstepai.inventory.data.SupplierPart
import com.nextstepai.inventory.repository.CompanyRepository
import com.nextstepai.inventory.repository.PartRepository
import com.nextstepai.inventory.repository.PurchaseOrderRepository
import com.nextstepai.inventory.repository.StockRepository
import com.nextstepai.inventory.repository.UserRepository
import com.nextstepai.inventory.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * حالة واجهة إدارة المخزون الفعلي ومواقع التخزين وأنواعها وسجلات التتبع وفحوصات الجودة والمرفقات (Stock UI State).
 */
data class StockUiState(
    val stockItems: List<StockItem> = emptyList(),
    val allStockItems: List<StockItem> = emptyList(),
    val locations: List<StockLocation> = emptyList(),
    val locationTypes: List<StockLocationType> = emptyList(),
    val parts: List<Part> = emptyList(),
    val categories: List<PartCategory> = emptyList(),
    val purchaseOrders: List<PurchaseOrder> = emptyList(),
    val suppliers: List<Company> = emptyList(),
    val supplierParts: List<SupplierPart> = emptyList(),
    val users: List<AppUser> = emptyList(),
    val selectedLocationId: Long? = null,
    val selectedLocationIds: Set<Long> = emptySet(),
    val selectedPartId: Long? = null,
    val isFilterBottomSheetOpen: Boolean = false,
    val isAddStockDialogOpen: Boolean = false,
    val isAddLocationDialogOpen: Boolean = false,
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
    private val partRepository: PartRepository = PartRepository(),
    private val poRepository: PurchaseOrderRepository = PurchaseOrderRepository(),
    private val companyRepository: CompanyRepository = CompanyRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockUiState())
    val uiState: StateFlow<StockUiState> = _uiState.asStateFlow()

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
        val allParts = partRepository.getParts()
        val categories = partRepository.getCategories()
        val locations = stockRepository.getLocations()
        val locationTypes = stockRepository.getLocationTypes()
        val rawStock = stockRepository.getStockItems()
        val purchaseOrders = poRepository.searchOrders()
        val suppliers = companyRepository.getCompanies().filter { it.isSupplier }
        val supplierParts = suppliers.flatMap { companyRepository.getSupplierPartsForCompany(it.id) }

        val selectedLocs = _uiState.value.selectedLocationIds
        val singleLoc = _uiState.value.selectedLocationId
        val effectiveLocIds = if (selectedLocs.isNotEmpty()) selectedLocs else if (singleLoc != null) setOf(singleLoc) else emptySet()

        val filteredStock = rawStock.filter { item ->
            (effectiveLocIds.isEmpty() || (item.locationId != null && item.locationId in effectiveLocIds)) &&
                    (_uiState.value.selectedPartId == null || item.partId == _uiState.value.selectedPartId)
        }

        _uiState.update {
            it.copy(
                stockItems = filteredStock,
                allStockItems = rawStock,
                locations = locations,
                locationTypes = locationTypes,
                parts = allParts,
                categories = categories,
                purchaseOrders = purchaseOrders,
                suppliers = suppliers,
                supplierParts = supplierParts
            )
        }
    }

    fun filterByLocation(locationId: Long?) {
        val updatedSet = if (locationId == null) emptySet() else setOf(locationId)
        _uiState.update {
            it.copy(
                selectedLocationIds = updatedSet,
                selectedLocationId = locationId
            )
        }
        loadData()
    }

    fun toggleLocationFilter(locationId: Long) {
        val current = _uiState.value.selectedLocationIds
        val updated = if (current.contains(locationId)) current - locationId else current + locationId
        _uiState.update {
            it.copy(
                selectedLocationIds = updated,
                selectedLocationId = updated.firstOrNull()
            )
        }
        loadData()
    }

    fun applyLocationFilters(locationIds: Set<Long>) {
        _uiState.update {
            it.copy(
                selectedLocationIds = locationIds,
                selectedLocationId = locationIds.firstOrNull(),
                isFilterBottomSheetOpen = false
            )
        }
        loadData()
    }

    fun clearLocationFilters() {
        _uiState.update {
            it.copy(
                selectedLocationIds = emptySet(),
                selectedLocationId = null,
                isFilterBottomSheetOpen = false
            )
        }
        loadData()
    }

    fun setFilterBottomSheetOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isFilterBottomSheetOpen = isOpen) }
    }

    fun filterByPart(partId: Long?) {
        _uiState.update { it.copy(selectedPartId = partId) }
        loadData()
    }

    fun addNewPart(
        name: String,
        ipn: String = "",
        description: String = "",
        categoryId: Long? = null,
        units: String = "pcs",
        assembly: Boolean = false,
        component: Boolean = true,
        isTemplate: Boolean = false,
        variantOfId: Long? = null,
        minimumStock: Double = 0.0,
        maximumStock: Double? = null,
        revision: String = "",
        keywords: String = "",
        trackable: Boolean = false,
        purchaseable: Boolean = true,
        salable: Boolean = false,
        virtual: Boolean = false,
        defaultLocationId: Long? = null,
        defaultExpiryDays: Int? = null,
        link: String = "",
        imageUrl: String? = null,
        active: Boolean = true,
        locked: Boolean = false
    ): Part {
        val newPart = Part(
            name = name,
            ipn = ipn,
            description = description,
            revision = revision,
            keywords = keywords,
            categoryId = categoryId,
            units = units.ifBlank { "pcs" },
            assembly = assembly,
            component = component,
            isTemplate = isTemplate,
            variantOfId = variantOfId,
            trackable = trackable,
            purchaseable = purchaseable,
            salable = salable,
            virtual = virtual,
            active = active,
            locked = locked,
            defaultLocationId = defaultLocationId,
            defaultExpiryDays = defaultExpiryDays,
            minimumStock = minimumStock,
            maximumStock = maximumStock,
            imageUrl = imageUrl,
            totalInStock = 0.0,
            link = link
        )
        val inserted = partRepository.addPart(newPart)
        loadData()
        return inserted
    }

    fun setAddDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddStockDialogOpen = isOpen, errorMessage = null) }
    }

    fun setAddLocationDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddLocationDialogOpen = isOpen, errorMessage = null) }
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

    fun addLocation(
        name: String,
        description: String = "",
        parentId: Long? = null,
        structural: Boolean = false,
        external: Boolean = false,
        locationType: String = "SHELF",
        icon: String = "warehouse",
        ownerId: Long? = null,
        customIcon: String = "",
        address: String = "",
        customCapacity: Double? = null,
        capacityUnit: String = "قطعة",
        contactPerson: String? = null,
        contactPhone: String? = null,
        isPrimary: Boolean = false,
        intermediates: List<IntermediateNodeSpec> = emptyList(),
        generatedNames: List<String> = emptyList()
    ): StockLocation {
        if (generatedNames.size > 1) {
            val insertedList = addSequentialLocations(
                generatedNames = generatedNames,
                description = description,
                parentId = parentId,
                structural = structural,
                external = external,
                locationType = locationType,
                icon = icon,
                ownerId = ownerId,
                customIcon = customIcon,
                baseAddress = address,
                customCapacity = customCapacity,
                capacityUnit = capacityUnit,
                contactPerson = contactPerson,
                contactPhone = contactPhone,
                isPrimary = isPrimary,
                intermediates = intermediates
            )
            return insertedList.firstOrNull() ?: StockLocation(name = name)
        }

        val loc = StockLocation(
            name = name,
            description = description,
            parentId = parentId,
            structural = structural,
            external = external,
            locationType = locationType,
            ownerId = ownerId,
            icon = icon,
            customIcon = customIcon,
            address = address,
            customCapacity = customCapacity
        )
            .withCapacityUnit(capacityUnit)
            .withContactInfo(contactPerson, contactPhone)
            .withPrimary(isPrimary)

        val inserted = if (intermediates.isNotEmpty()) {
            stockRepository.addLocationWithIntermediates(loc, intermediates)
        } else {
            stockRepository.addLocation(loc)
        }

        _uiState.update {
            it.copy(
                isAddLocationDialogOpen = false,
                errorMessage = null,
                successMessage = if (intermediates.isNotEmpty()) "تم إنشاء الموقع والتسلسل الهرمي بنجاح" else "تم إنشاء موقع التخزين بنجاح"
            )
        }
        loadData()
        return inserted
    }

    /**
     * توليد وإنشاء دفعة من المواقع التخزينية المتسلسلة من القائمة المحددة للأسماء المولدة.
     */
    fun addSequentialLocations(
        generatedNames: List<String>,
        description: String = "",
        parentId: Long? = null,
        structural: Boolean = false,
        external: Boolean = false,
        locationType: String = "SHELF",
        icon: String = "warehouse",
        ownerId: Long? = null,
        customIcon: String = "",
        baseAddress: String = "",
        customCapacity: Double? = null,
        capacityUnit: String = "قطعة",
        contactPerson: String? = null,
        contactPhone: String? = null,
        isPrimary: Boolean = false,
        intermediates: List<IntermediateNodeSpec> = emptyList()
    ): List<StockLocation> {
        if (generatedNames.isEmpty()) return emptyList()

        // 1. Resolve missing intermediate layers if any
        var targetParentId = parentId
        if (intermediates.isNotEmpty()) {
            for (spec in intermediates) {
                if (spec.existingId != null && spec.existingId > 0L) {
                    targetParentId = spec.existingId
                } else if (spec.name.isNotBlank()) {
                    val newIntermediate = StockLocation(
                        name = spec.name.trim(),
                        description = "طبقة وسيطة مضافة آلياً لحشو فجوة الهرمية",
                        parentId = targetParentId,
                        structural = true,
                        locationType = spec.locationType
                    )
                    val inserted = stockRepository.addLocation(newIntermediate)
                    targetParentId = inserted.id
                }
            }
        }

        // 2. Build StockLocation objects for each generated name
        val locationsToInsert = generatedNames.mapIndexed { index, locName ->
            val computedAddress = if (baseAddress.contains(" > ")) {
                val prefixPath = baseAddress.substringBeforeLast(" > ")
                "$prefixPath > $locName"
            } else {
                locName
            }

            StockLocation(
                name = locName,
                description = description.ifBlank { "موقع مولد آلياً ضمن دفعة متسلسلة" },
                parentId = if (external) null else targetParentId,
                structural = structural,
                external = external,
                locationType = if (external) "SITE" else locationType,
                ownerId = if (external) null else ownerId,
                icon = if (external) "warehouse" else icon,
                customIcon = customIcon,
                address = computedAddress,
                customCapacity = customCapacity,
                isBulkGenerated = true
            )
            .withCapacityUnit(capacityUnit)
            .withContactInfo(contactPerson, contactPhone)
            .withPrimary(if (index == 0) isPrimary else false)
        }

        // 3. Save via native batch save repository pipeline
        val insertedList = stockRepository.addBatchLocations(locationsToInsert)

        _uiState.update {
            it.copy(
                isAddLocationDialogOpen = false,
                errorMessage = null,
                successMessage = "تم توليد وإنشاء ${insertedList.size} موقع تخزيني بنجاح ⚡"
            )
        }
        loadData()
        return insertedList
    }

    /**
     * إنشاء موقع أب/حاوي وسيط بسرعة (Quick Add Parent Location) مع تعيينه كـ structural وتحديث التدفق فوراً.
     */
    fun quickCreateParentLocation(
        name: String,
        locationType: String = "ZONE",
        parentId: Long? = null,
        description: String = ""
    ): StockLocation {
        val defaultIcon = when (locationType.uppercase().trim()) {
            "WAREHOUSE" -> "warehouse"
            "ZONE" -> "zone"
            "RACK" -> "rack"
            "AREA" -> "area"
            else -> "warehouse"
        }
        val loc = StockLocation(
            name = name.trim(),
            description = description.trim(),
            parentId = parentId,
            structural = true,
            locationType = locationType,
            icon = defaultIcon
        )
        val inserted = stockRepository.addLocation(loc)
        _uiState.update {
            it.copy(
                errorMessage = null,
                successMessage = "تم إضافة الموقع الحاوي الوسيط (${inserted.name}) بنجاح"
            )
        }
        loadData()
        return inserted
    }


    /**
     * تحديث بيانات وسعة موقع تخزيني قائم في النظام.
     */
    fun updateLocation(location: StockLocation): StockLocation {
        val updated = stockRepository.updateLocation(location)
        _uiState.update {
            it.copy(
                errorMessage = null,
                successMessage = "تم تحديث بيانات الموقع التخزيني بنجاح"
            )
        }
        loadData()
        return updated
    }

    /**
     * إدراج وحدة قياس مخصصة جديدة وإصدار حفظ في قاعدة البيانات.
     */
    fun addCustomUnit(unitCode: String) {
        if (unitCode.isBlank()) return
        viewModelScope.launch {
            try {
                partRepository.addParameterTemplate(
                    name = unitCode.trim(),
                    units = unitCode.trim(),
                    description = "وحدة قياس مخصصة"
                )
                _uiState.update {
                    it.copy(
                        errorMessage = null,
                        successMessage = "تم إضافة وحدة القياس الجديدة '${unitCode.trim()}' بنجاح"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "فشل إضافة وحدة القياس: ${e.message}") }
            }
        }
    }

    /**
     * حذف وحدة قياس مخصصة بحذف متتابع من قاعدة البيانات.
     */
    fun deleteUnit(unitCode: String) {
        if (unitCode.isBlank()) return
        viewModelScope.launch {
            try {
                partRepository.deleteParameterTemplateByNameOrUnit(unitCode.trim())
                _uiState.update {
                    it.copy(
                        errorMessage = null,
                        successMessage = "تم حذف وحدة القياس '${unitCode.trim()}' بنجاح"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "فشل حذف وحدة القياس: ${e.message}") }
            }
        }
    }

    /**
     * حفظ وأرشفة لقطة بيانات الملصق المطبوع.
     */
    fun saveLocationLabelSnapshot(locationId: Long, snapshotData: String): StockLocation? {
        val updated = stockRepository.saveLocationLabelSnapshot(locationId, snapshotData)
        if (updated != null) {
            loadData()
        }
        return updated
    }

    /**
     * حذف موقع تخزيني بعد التأكد من اجتياز القيود الأمنية الثلاثية.
     */
    fun deleteLocation(locationId: Long) {
        try {
            stockRepository.deleteLocation(locationId)
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    successMessage = "تم حذف الموقع التخزيني بنجاح"
                )
            }
            loadData()
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    /**
     * تنفيذ النقل المخزني السريع (Quick Stock Transfer) للكميات كلياً أو جزئياً وتحديث الواجهة فورياً.
     */
    fun transferStockItem(
        itemId: Long,
        sourceLocationId: Long?,
        targetLocationId: Long,
        quantity: Double,
        reason: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                val success = stockRepository.transferStockItem(
                    itemId = itemId,
                    sourceLocationId = sourceLocationId,
                    targetLocationId = targetLocationId,
                    quantityToTransfer = quantity,
                    reason = reason,
                    notes = notes
                )
                if (success) {
                    _uiState.update {
                        it.copy(successMessage = "تم نقل الكمية بنجاح إلى الموقع الجديد ⇄")
                    }
                    loadData()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "فشل في عملية النقل المخزني") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    /**
     * توليد وإنشاء دفعة من المواقع التخزينية المتسلسلة آلياً (Bulk Location Generator).
     */
    fun generateBulkLocations(
        parentId: Long?,
        locationType: String,
        prefix: String,
        startNumber: Int,
        endNumber: Int,
        padZeros: Boolean,
        customCapacity: Double? = null,
        description: String = ""
    ): List<StockLocation> {
        val effectivePrefix = prefix.ifBlank {
            when (locationType.uppercase()) {
                "SHELF" -> "S-"
                "BIN" -> "B-"
                "AISLE" -> "A-"
                "ZONE" -> "Z-"
                else -> "LOC-"
            }
        }

        val padDigits = if (padZeros) endNumber.toString().length.coerceAtLeast(2) else 1

        val bulkList = mutableListOf<StockLocation>()
        for (i in startNumber..endNumber) {
            val formattedNum = if (padZeros) i.toString().padStart(padDigits, '0') else i.toString()
            val locName = "$effectivePrefix$formattedNum"
            bulkList.add(
                StockLocation(
                    name = locName,
                    description = description.ifBlank { "موقع مولد آلياً بنمط $effectivePrefix" },
                    parentId = parentId,
                    locationType = locationType,
                    customCapacity = customCapacity,
                    isBulkGenerated = true
                )
            )
        }

        val inserted = stockRepository.addBatchLocations(bulkList)
        _uiState.update {
            it.copy(
                isAddLocationDialogOpen = false,
                errorMessage = null,
                successMessage = "تم توليد وإنشاء ${inserted.size} موقع تخزيني بنجاح"
            )
        }
        loadData()
        return inserted
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
        supplierPartId: Long? = null,
        purchaseOrderId: Long? = null,
        expiryDate: String = "",
        reviewNeeded: Boolean = false,
        deleteOnDeplete: Boolean = false,
        link: String = "",
        notes: String = "",
        unitWeight: Double? = null,
        totalWeight: Double? = null
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
                supplierPartId = supplierPartId,
                purchaseOrderId = purchaseOrderId,
                expiryDate = expiryDate,
                reviewNeeded = reviewNeeded,
                deleteOnDeplete = deleteOnDeplete,
                link = link,
                notes = notes
            ).withWeightInfo(unitWeight, totalWeight)
            stockRepository.addStockItem(item)

            // 🎯 الأثر التشغيلي عند الحفظ: إن كان مفترناً بأمر شراء Mapped PurchaseOrder
            if (purchaseOrderId != null) {
                val orders = poRepository.searchOrders()
                val targetPO = orders.find { it.id == purchaseOrderId }
                if (targetPO != null) {
                    val matchingLine = targetPO.lineItems.find { line ->
                        line.supplierPartId == partId ||
                        (supplierPartId != null && line.supplierPartId == supplierPartId)
                    } ?: targetPO.lineItems.firstOrNull { !it.isFullyReceived } ?: targetPO.lineItems.firstOrNull()

                    if (matchingLine != null) {
                        poRepository.receiveLineItem(matchingLine.id, quantity)
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isAddStockDialogOpen = false,
                    errorMessage = null,
                    successMessage = "تم إضافة الرصيد المخزني وتسجيل الاستلام بنجاح"
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
