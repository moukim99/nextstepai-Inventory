package com.nextstepai.inventory.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import com.nextstepai.inventory.ui.components.LocationBarcodeScannerBottomSheet
import com.nextstepai.inventory.ui.components.DynamicBreadcrumbAddressField
import com.nextstepai.inventory.ui.components.ShakeController
import com.nextstepai.inventory.data.PendingAttachment
import com.nextstepai.inventory.util.BarcodePayloadHelper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nextstepai.inventory.ui.components.CurrencySelectionBottomSheet
import com.nextstepai.inventory.ui.components.CurrencySelectorField
import com.nextstepai.inventory.ui.components.PrintableLabelBottomSheet
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue
import kotlin.time.Clock
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.vector.ImageVector
import com.nextstepai.inventory.data.AppUser
import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartCategory
import com.nextstepai.inventory.data.PurchaseOrder
import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.SupplierPart
import com.nextstepai.inventory.data.StockItemAttachment
import com.nextstepai.inventory.data.StockItemTestResult
import com.nextstepai.inventory.data.StockItemTracking
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.StockLocationType
import com.nextstepai.inventory.data.capacityUnit
import com.nextstepai.inventory.data.contactPerson
import com.nextstepai.inventory.data.contactPhone
import com.nextstepai.inventory.data.withContactInfo
import com.nextstepai.inventory.data.isPrimary
import com.nextstepai.inventory.data.withPrimary
import com.nextstepai.inventory.data.IntermediateNodeSpec
import com.nextstepai.inventory.data.effectiveCapacity
import com.nextstepai.inventory.data.calculateOccupancyPercentage
import com.nextstepai.inventory.data.calculatePhysicalOccupancy
import com.nextstepai.inventory.data.getOccupancySummary
import com.nextstepai.inventory.data.getFullHierarchyPath
import com.nextstepai.inventory.data.StockStatus
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_stock
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.card_stock_title
import nextstepai_inventory.shared.generated.resources.save
import nextstepai_inventory.shared.generated.resources.stock_items_count

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddStockLocationBottomSheet(
    locations: List<StockLocation>,
    users: List<AppUser> = emptyList(),
    locationTypes: List<StockLocationType> = emptyList(),
    initialLocation: StockLocation? = null,
    onDismiss: () -> Unit,
    onQuickAddParent: ((name: String, type: String, parentId: Long?) -> StockLocation)? = null,
    onAddCapacityUnit: (String) -> Unit = {},
    onDeleteCapacityUnit: (String) -> Unit = {},
    onConfirm: (
        name: String,
        description: String,
        parentId: Long?,
        structural: Boolean,
        external: Boolean,
        locationType: String,
        icon: String,
        ownerId: Long?,
        customIcon: String,
        address: String,
        customCapacity: Double?,
        capacityUnit: String,
        contactPerson: String?,
        contactPhone: String?,
        isPrimary: Boolean,
        intermediates: List<IntermediateNodeSpec>,
        generatedNames: List<String>
    ) -> Unit,
    onConfirmBulk: ((
        parentId: Long?,
        locationType: String,
        prefix: String,
        startNumber: Int,
        endNumber: Int,
        padZeros: Boolean,
        description: String,
        customCapacity: Double?
    ) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isEditMode = initialLocation != null
    var showEditConfirmationDialog by remember { mutableStateOf(false) }

    // Single / Unified Mode States
    var name by remember { mutableStateOf(initialLocation?.name ?: "") }
    var description by remember { mutableStateOf(initialLocation?.description ?: "") }
    var address by remember { mutableStateOf(initialLocation?.address ?: "") }
    var contactPerson by remember { mutableStateOf(initialLocation?.contactPerson ?: "") }
    var contactPhone by remember { mutableStateOf(initialLocation?.contactPhone ?: "") }
    var customCapacityText by remember { mutableStateOf(initialLocation?.customCapacity?.toString() ?: "") }
    var capacityUnit by remember { mutableStateOf(initialLocation?.capacityUnit ?: "") }
    var quantityText by remember { mutableStateOf("1") }
    val count = (quantityText.toIntOrNull() ?: 1).coerceIn(1, 50)
    var selectedParentId by remember { mutableStateOf<Long?>(initialLocation?.parentId) }
    var locationType by remember { mutableStateOf(initialLocation?.locationType ?: "SHELF") }
    var structural by remember { mutableStateOf(initialLocation?.structural ?: false) }
    var external by remember { mutableStateOf(initialLocation?.external ?: false) }
    var icon by remember { mutableStateOf(initialLocation?.icon ?: "shelves") }
    var selectedOwner by remember {
        mutableStateOf<AppUser?>(users.find { user ->
            val uId = user.uuid.filter { it.isDigit() }.toLongOrNull() ?: user.uuid.hashCode().toLong().absoluteValue
            uId == initialLocation?.ownerId
        })
    }

    // Primary Location Logic with 3-way Isolation (Internal Sites vs Internal Warehouses vs External Locations)
    val isExternalLocation = external
    val isSite = locationType.equals("SITE", ignoreCase = true) && !external
    val isWarehouse = locationType.equals("WAREHOUSE", ignoreCase = true) && !external
    val isPrimaryApplicable = isSite || isWarehouse || isExternalLocation

    val existingInternalSitesCount = remember(locations, initialLocation) {
        locations.count { loc ->
            loc.id != (initialLocation?.id ?: -1L) && !loc.external && loc.locationType.equals("SITE", ignoreCase = true)
        }
    }
    val existingInternalWarehousesCount = remember(locations, initialLocation) {
        locations.count { loc ->
            loc.id != (initialLocation?.id ?: -1L) && !loc.external && loc.locationType.equals("WAREHOUSE", ignoreCase = true)
        }
    }
    val existingExternalLocationsCount = remember(locations, initialLocation) {
        locations.count { loc ->
            loc.id != (initialLocation?.id ?: -1L) && loc.external
        }
    }

    val relevantExistingCount = when {
        isExternalLocation -> existingExternalLocationsCount
        isSite -> existingInternalSitesCount
        isWarehouse -> existingInternalWarehousesCount
        else -> 0
    }

    val hasPrimaryInSystem = remember(locations, initialLocation, isSite, isWarehouse, isExternalLocation) {
        locations.any { loc ->
            loc.id != (initialLocation?.id ?: -1L) && loc.isPrimary &&
                    when {
                        isExternalLocation -> loc.external
                        isSite -> loc.locationType.equals("SITE", ignoreCase = true) && !loc.external
                        isWarehouse -> loc.locationType.equals("WAREHOUSE", ignoreCase = true) && !loc.external
                        else -> false
                    }
        }
    }

    val isPrimaryFrozen = isPrimaryApplicable && (relevantExistingCount == 0)

    var isPrimary by remember(initialLocation, relevantExistingCount, isPrimaryApplicable) {
        mutableStateOf(
            if (!isPrimaryApplicable) false
            else if (initialLocation != null) initialLocation.isPrimary
            else if (relevantExistingCount == 0) true
            else false
        )
    }

    var primaryValidationErrorMessage by remember { mutableStateOf<String?>(null) }

    // Hierarchy Distance Engine
    fun getLocationTypeRank(type: String): Int {
        return when (type.uppercase().trim()) {
            "SITE", "ROOT" -> 0
            "WAREHOUSE" -> 1
            "ZONE", "AREA" -> 2
            "AISLE" -> 3
            "RACK", "SHELF" -> 4
            "BIN" -> 5
            else -> 4
        }
    }

    fun getRankLabel(rank: Int): String {
        return when (rank) {
            0 -> "الموقع العام (Site)"
            1 -> "المستودع (Warehouse)"
            2 -> "المنطقة / القسم (Zone)"
            3 -> "الممر (Aisle)"
            4 -> "الرف (Shelf)"
            5 -> "الصندوق / الدرج (Bin)"
            else -> "الطبقة الوسيطة"
        }
    }

    fun getRankShortLabel(rank: Int): String {
        return when (rank) {
            0 -> "موقع"
            1 -> "مستودع"
            2 -> "منطقة"
            3 -> "ممر"
            4 -> "رف"
            5 -> "صندوق"
            else -> "موقع"
        }
    }

    fun getRankDefaultType(rank: Int): String {
        return when (rank) {
            0 -> "SITE"
            1 -> "WAREHOUSE"
            2 -> "ZONE"
            3 -> "AISLE"
            4 -> "SHELF"
            5 -> "BIN"
            else -> "SHELF"
        }
    }

    val currentTypeLabel = when (locationType.uppercase().trim()) {
        "SITE" -> "📍 موقع / منشأة جغرافية"
        "WAREHOUSE" -> "🏢 مستودع"
        "ZONE", "AREA" -> "🧩 منطقة / قسم"
        "AISLE" -> "🚪 ممر"
        "SHELF", "RACK" -> "📐 رف"
        "BIN" -> "📥 صندوق / درج"
        else -> "📐 رف"
    }

    val parentLocationObj = locations.find { it.id == selectedParentId }
    val parentRank = if (selectedParentId == null || parentLocationObj == null) 0 else getLocationTypeRank(parentLocationObj.locationType)
    val targetRank = getLocationTypeRank(locationType)

    val gapCount = if (!external && targetRank > parentRank + 1) (targetRank - parentRank - 1) else 0
    val missingRanks = if (gapCount > 0) (parentRank + 1 until targetRank).toList() else emptyList()

    var isIntermediateLayersSheetOpen by remember { mutableStateOf(false) }

    var customAddressText by remember { mutableStateOf(initialLocation?.address ?: "") }
    var isManuallyEditedAddress by remember { mutableStateOf(initialLocation?.address?.isNotBlank() == true) }
    var isAddressConfirmed by remember { mutableStateOf(false) }
    var hasDuplicateAddressError by remember { mutableStateOf(false) }
    val addressShakeController = remember { ShakeController() }

    var isGapExpanded by remember(selectedParentId, locationType) { mutableStateOf(false) }
    var intermediateSelections by remember(selectedParentId, locationType) {
        mutableStateOf<Map<Int, Pair<Long?, String>>>(emptyMap())
    }

    val effectiveName = remember(name, locationType, gapCount, missingRanks, intermediateSelections) {
        if (name.isNotBlank() && name != "[اسم الموقع]") {
            name.trim()
        } else if (gapCount == 0 || missingRanks.all { r -> intermediateSelections[r]?.second?.isNotBlank() == true }) {
            "${getRankShortLabel(targetRank)} 01"
        } else {
            ""
        }
    }

    val computedBreadcrumbAddress = remember(selectedParentId, locationType, effectiveName, intermediateSelections, gapCount, locations) {
        val parts = mutableListOf<String>()
        if (selectedParentId != null && parentLocationObj != null) {
            parts.add(parentLocationObj.getFullHierarchyPath(locations))
        } else {
            parts.add("المستودع الرئيسي")
        }
        if (gapCount > 0) {
            missingRanks.forEach { rank ->
                val interName = intermediateSelections[rank]?.second
                if (!interName.isNullOrBlank()) {
                    parts.add(interName.trim())
                }
            }
        }
        if (effectiveName.isNotBlank()) {
            parts.add(effectiveName.trim())
        } else {
            parts.add("[اسم الموقع]")
        }
        parts.joinToString(" > ")
    }

    val isAddressLocked = gapCount > 0 && !missingRanks.all { r -> intermediateSelections[r]?.second?.isNotBlank() == true }

    val hasChanged = remember(
        name, description, address, contactPerson, contactPhone, customCapacityText, capacityUnit,
        selectedParentId, locationType, structural, external, icon, selectedOwner, quantityText,
        initialLocation
    ) {
        if (initialLocation != null) {
            val calculatedOwnerId = selectedOwner?.let { user ->
                user.uuid.filter { it.isDigit() }.toLongOrNull()
                    ?: user.uuid.hashCode().toLong().absoluteValue
            }
            name.trim() != initialLocation.name.trim() ||
            description.trim() != initialLocation.description.trim() ||
            address.trim() != initialLocation.address.trim() ||
            contactPerson.trim() != (initialLocation.contactPerson ?: "").trim() ||
            contactPhone.trim() != (initialLocation.contactPhone ?: "").trim() ||
            customCapacityText.trim() != (initialLocation.customCapacity?.toString() ?: "").trim() ||
            capacityUnit != initialLocation.capacityUnit ||
            selectedParentId != initialLocation.parentId ||
            locationType != initialLocation.locationType ||
            structural != initialLocation.structural ||
            external != initialLocation.external ||
            icon != initialLocation.icon ||
            calculatedOwnerId != initialLocation.ownerId
        } else {
            true
        }
    }

    // Sequence Generator Parsing & Duplicate Check Engine
    val parsedSeq = remember(name, effectiveName, count) {
        val targetName = if (name.isNotBlank() && name != "[اسم الموقع]") name.trim() else effectiveName.trim()
        val match = Regex("""^(.*?)(?:[\s\-_]*)(\d+)$""").find(targetName)
        val basePrefix = if (match != null) match.groupValues[1].trim() else targetName
        val rawStartNum = match?.groupValues?.get(2)?.toIntOrNull() ?: 1
        val rawDigitsLength = match?.groupValues?.get(2)?.length ?: 2
        val finalPadding = if (rawDigitsLength < 2 && (rawStartNum + count - 1) >= 10) 2 else rawDigitsLength
        val separator = if (basePrefix.endsWith("-") || basePrefix.endsWith("_")) "" else " "
        Triple(basePrefix, rawStartNum, Pair(finalPadding, separator))
    }

    val bulkCandidateData = remember(parsedSeq, count, selectedParentId, locations, external, intermediateSelections, missingRanks) {
        if (count <= 1) Pair(emptyList<String>(), emptyList<Int>())
        else {
            val (basePrefix, rawStartNum, padAndSep) = parsedSeq
            val (finalPadding, separator) = padAndSep
            val generatedNames = mutableListOf<String>()
            val skippedNums = mutableListOf<Int>()
            var currentNum = rawStartNum

            val evalTargetParentId = if (external) null else if (intermediateSelections.isNotEmpty()) {
                missingRanks.mapNotNull { intermediateSelections[it]?.first }.lastOrNull() ?: selectedParentId
            } else selectedParentId

            while (generatedNames.size < count && currentNum < rawStartNum + count + 500) {
                val formattedNum = currentNum.toString().padStart(finalPadding, '0')
                val candidateName = if (basePrefix.isBlank()) formattedNum else "$basePrefix$separator$formattedNum"

                val isDuplicate = locations.any { loc ->
                    loc.parentId == evalTargetParentId && loc.name.trim().equals(candidateName, ignoreCase = true)
                }

                if (!isDuplicate) {
                    generatedNames.add(candidateName)
                } else {
                    skippedNums.add(currentNum)
                }
                currentNum++
            }
            Pair(generatedNames, skippedNums)
        }
    }
    val generatedNames = bulkCandidateData.first
    val skippedNums = bulkCandidateData.second

    // BottomSheet Pickers State
    var isOwnerPickerOpen by remember { mutableStateOf(false) }
    var isLocationTypePickerOpen by remember { mutableStateOf(false) }
    var isParentLocationPickerOpen by remember { mutableStateOf(false) }
    var isCapacityUnitPickerOpen by remember { mutableStateOf(false) }
    var isQuickAddParentDialogOpen by remember { mutableStateOf(false) }

    if (isQuickAddParentDialogOpen) {
        QuickAddParentBottomSheet(
            locations = locations,
            currentParentId = selectedParentId,
            onDismiss = { isQuickAddParentDialogOpen = false },
            onConfirm = { parentName: String, parentType: String, grandparentId: Long? ->
                val created = onQuickAddParent?.invoke(parentName, parentType, grandparentId)
                if (created != null) {
                    selectedParentId = created.id
                }
                isQuickAddParentDialogOpen = false
            }
        )
    }

    if (isCapacityUnitPickerOpen) {
        SelectCapacityUnitBottomSheet(
            selectedUnit = capacityUnit,
            onDismiss = { isCapacityUnitPickerOpen = false },
            onUnitSelected = { unit ->
                capacityUnit = unit
                isCapacityUnitPickerOpen = false
            }
        )
    }

    if (isOwnerPickerOpen) {
        SelectOwnerBottomSheet(
            users = users,
            selectedOwner = selectedOwner,
            onDismiss = { isOwnerPickerOpen = false },
            onSelectOwner = { selectedOwner = it }
        )
    }

    if (isLocationTypePickerOpen) {
        LocationTypeSelectionBottomSheet(
            selectedLocationType = locationType,
            onDismiss = { isLocationTypePickerOpen = false },
            onLocationTypeSelected = { code ->
                locationType = code
                icon = getDefaultIconForLocationType(code)
                isLocationTypePickerOpen = false
            }
        )
    }

    if (isParentLocationPickerOpen) {
        ParentLocationSelectionBottomSheet(
            locations = locations,
            selectedParentId = selectedParentId,
            onDismiss = { isParentLocationPickerOpen = false },
            onQuickAddParent = onQuickAddParent,
            onParentSelected = { parentId ->
                selectedParentId = parentId
                isParentLocationPickerOpen = false
            }
        )
    }

    if (isIntermediateLayersSheetOpen) {
        SelectIntermediateLayersBottomSheet(
            missingRanks = missingRanks,
            locations = locations,
            selectedParentId = selectedParentId,
            parentLocationName = parentLocationObj?.name ?: "المستودع الرئيسي (Root)",
            targetTypeLabel = currentTypeLabel,
            initialSelections = intermediateSelections,
            onDismiss = { isIntermediateLayersSheetOpen = false },
            onConfirm = { updatedSelections ->
                intermediateSelections = updatedSelections
                isIntermediateLayersSheetOpen = false
                primaryValidationErrorMessage = null
                if (name.isBlank() || name == "[اسم الموقع]") {
                    name = "${getRankShortLabel(targetRank)} 01"
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (count > 1) Icons.Default.FlashOn else Icons.Default.AddLocation,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isEditMode) "التعديل على موقع تخزين" else if (count > 1) "توليد مواقع تخزينية متسلسلة" else "إضافة موقع تخزيني جديد",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isEditMode) "تحديث وتعديل كافة حقول بيانات وسعة هذا الموقع" else if (count > 1) "توليد $count مواقع متطابقة بنفس السعة والتصنيف" else "إدخال موقع تخزين فريد في شجرة المستودع",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Form Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Scope Selector Switch (موقع داخلي vs موقع خارجي)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(4.dp)
                ) {
                    Surface(
                        onClick = {
                            external = false
                            if (locationType == "SITE") locationType = "SHELF"
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (!external) Color.White else Color.Transparent,
                        shadowElevation = if (!external) 2.dp else 0.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.HomeWork,
                                contentDescription = null,
                                tint = if (!external) Color(0xFF4F46E5) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🏢 موقع داخلي (Internal)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = if (!external) Color(0xFF0F172A) else Color(0xFF64748B)
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            external = true
                            locationType = "SITE"
                            structural = false
                            selectedParentId = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (external) Color(0xFF2563EB) else Color.Transparent,
                        shadowElevation = if (external) 2.dp else 0.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = if (external) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🚚 موقع خارجي (External)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = if (external) Color.White else Color(0xFF64748B)
                            )
                        }
                    }
                }

                if (external) {
                    val uriHandler = LocalUriHandler.current
                    val clipboardManager = LocalClipboardManager.current

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الموقع الخارجي") },
                        placeholder = { Text("مثال: مستودع المقاول س، مصنع التجميع") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("عنوان الموقع الخارجي") },
                        placeholder = { Text("مثال: المنطقة الصناعية - وهران، الجزائر") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF2563EB))
                        },
                        trailingIcon = {
                            if (address.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(address.trim()))
                                        try {
                                            val encoded = address.trim().replace(" ", "+")
                                            uriHandler.openUri("https://maps.google.com/?q=$encoded")
                                        } catch (_: Exception) {}
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Directions,
                                        contentDescription = "الانتقال السريع للجيبياس",
                                        tint = Color(0xFF2563EB)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = contactPerson,
                        onValueChange = { contactPerson = it },
                        label = { Text("اسم الشخص المسؤول") },
                        placeholder = { Text("مثال: محمد العربي") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2563EB))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("رقم الهاتف") },
                        placeholder = { Text("مثال: 0550123456") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF2563EB))
                        },
                        trailingIcon = {
                            if (contactPhone.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val cleanPhone = contactPhone.trim().filter { it.isDigit() || it == '+' }
                                        clipboardManager.setText(AnnotatedString(cleanPhone))
                                        try {
                                            uriHandler.openUri("tel:$cleanPhone")
                                        } catch (_: Exception) {}
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "الانتقال السريع واجراء الاتصال",
                                        tint = Color(0xFF16A34A)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                } else {
                    // Internal Mode Fields
                    if (locationType.equals("SITE", ignoreCase = true)) {
                        LaunchedEffect(Unit) {
                            selectedParentId = null
                        }
                    } else {
                        val parentLocationObj = locations.find { it.id == selectedParentId }
                        val fullBreadcrumbPath = buildString {
                            if (selectedParentId != null && parentLocationObj != null) {
                                append(parentLocationObj.getFullHierarchyPath(locations))
                                append(" > ")
                            } else {
                                append("المستودع الرئيسي (Root) > ")
                            }
                            append(if (name.isBlank()) "[الموقع الجديد]" else name)
                        }

                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { isParentLocationPickerOpen = true }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFEEF2FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (selectedParentId == null) Icons.Default.AccountTree else if (parentLocationObj?.structural == true) Icons.Default.HomeWork else Icons.Default.Place,
                                            contentDescription = null,
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "الموقع الحاوي (الأب) في الهرمية الشجرية",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                color = Color(0xFF0F172A)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "اختر الموقع الأب",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Text(
                                            text = "📍 $fullBreadcrumbPath",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color(0xFF059669),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Location Type Picker Card
                    val currentTypeLabel = when (locationType.uppercase()) {
                        "SITE" -> "📍 موقع / منشأة جغرافية"
                        "WAREHOUSE" -> "🏢 مستودع"
                        "ZONE" -> "🧩 منطقة / قسم"
                        "AISLE" -> "🚪 ممر"
                        "SHELF" -> "📐 رف"
                        "BIN" -> "📥 صندوق / درج"
                        else -> "📐 رف"
                    }

                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isLocationTypePickerOpen = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEEF2FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getStockLocationIconVector(icon),
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "التصنيف المباشر لمحتوى الموقع",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = currentTypeLabel,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF4F46E5)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "اختر التصنيف",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Smart Hierarchy Gap Alert Banner
                    if (gapCount > 0) {
                        val missingRanksSummary = missingRanks.joinToString(" ➔ ") { getRankLabel(it) }
                        val isFilled = missingRanks.all { r -> intermediateSelections[r]?.second?.isNotBlank() == true }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isFilled) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, if (isFilled) Color(0xFFA7F3D0) else Color(0xFFFCD34D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isFilled) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isFilled) Icons.Default.CheckCircle else Icons.Default.Layers,
                                            contentDescription = null,
                                            tint = if (isFilled) Color(0xFF059669) else Color(0xFFD97706),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (isFilled) "تم تحديد $gapCount طبقات وسيطة ✓" else "فراغ في تسلسل التخزين ($gapCount مستويات مفقودة)",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            ),
                                            color = if (isFilled) Color(0xFF065F46) else Color(0xFF92400E)
                                        )
                                        Text(
                                            text = if (isFilled)
                                                missingRanks.joinToString(" ➔ ") { r -> intermediateSelections[r]?.second ?: "" }
                                            else
                                                "الطبقات الناقصة: $missingRanksSummary",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                            color = if (isFilled) Color(0xFF047857) else Color(0xFFB45309),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Button(
                                    onClick = { isIntermediateLayersSheetOpen = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFilled) Color(0xFF059669) else Color(0xFF4F46E5),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(40.dp)
                                        .padding(start = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFilled) Icons.Default.Edit else Icons.Default.Layers,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isFilled) "تعديل ✏️" else "+ تحديد الطبقات",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Template Physical Specs Chip/Banner
                    val matchingType = locationTypes.find { it.name.equals(locationType, ignoreCase = true) }
                    if (matchingType != null && matchingType.hasPhysicalSpecs()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "💡 مواصفات القالب: ${matchingType.formatSpecsBadge()}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    color = Color(0xFF1E40AF)
                                )
                            }
                        }
                    }

                    // Geographic Address Input (SITE mode)
                    if (locationType.equals("SITE", ignoreCase = true)) {
                        val uriHandler = LocalUriHandler.current
                        val clipboardManager = LocalClipboardManager.current

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("العنوان الجغرافي / موقع المنشأة") },
                            placeholder = { Text("مثال: المنطقة الصناعية - رغاية، الجزائر العاصمة") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF4F46E5))
                            },
                            trailingIcon = {
                                if (address.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(address.trim()))
                                            try {
                                                val encoded = address.trim().replace(" ", "+")
                                                uriHandler.openUri("https://maps.google.com/?q=$encoded")
                                            } catch (_: Exception) {}
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Directions,
                                            contentDescription = "الانتقال السريع للجيبياس",
                                            tint = Color(0xFF4F46E5)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }

                    // Dynamic Breadcrumb Address Field
                    DynamicBreadcrumbAddressField(
                        computedPath = computedBreadcrumbAddress,
                        customAddress = customAddressText,
                        onAddressChange = { newAddr ->
                            customAddressText = newAddr
                            isManuallyEditedAddress = true
                            hasDuplicateAddressError = false
                            primaryValidationErrorMessage = null

                            val lastSegment = if (newAddr.contains(" > ")) newAddr.substringAfterLast(" > ").trim() else newAddr.trim()
                            name = if (lastSegment != "[اسم الموقع]") lastSegment else ""
                        },
                        isConfirmed = isAddressConfirmed,
                        onConfirmToggle = {
                            isAddressConfirmed = !isAddressConfirmed
                        },
                        isManuallyEdited = isManuallyEditedAddress,
                        onReset = {
                            customAddressText = ""
                            isManuallyEditedAddress = false
                            isAddressConfirmed = false
                            hasDuplicateAddressError = false
                            name = initialLocation?.name ?: ""
                        },
                        isLocked = isAddressLocked,
                        hasDuplicateError = hasDuplicateAddressError,
                        shakeController = addressShakeController,
                        label = "اسم الموقع التخزيني والمسار الميداني",
                        placeholder = "ادخل اسم أو مسار الموقع التخزيني (مثل: الرف B3)..."
                    )

                    // Live Preview Banner for Sequential Bulk Generation
                    if (count > 1 && generatedNames.isNotEmpty()) {
                        val firstName = generatedNames.first()
                        val lastName = generatedNames.last()
                        val skippedText = if (skippedNums.isNotEmpty()) " (تم تخطي ${skippedNums.joinToString(", ")} لوجوده مسبقاً)" else ""
                        val previewMsg = "سيتم توليد $count مواقع: من $firstName إلى $lastName$skippedText بنفس السعة والتصنيف"

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEEF2FF),
                            border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "شريط المعاينة الحية للتوليد المتسلسل:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF3730A3)
                                    )
                                }
                                Text(
                                    text = previewMsg,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF4338CA)
                                )
                            }
                        }
                    }

                    // 5. Side-by-side Capacity & Capacity Unit Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isCapacityUnitPickerOpen = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "وحدة القياس",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        ),
                                        color = Color(0xFF0F172A),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (capacityUnit.isBlank()) "اضغط للاختيار..." else capacityUnit,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (capacityUnit.isBlank()) Color(0xFF64748B) else Color(0xFF4F46E5),
                                        maxLines = 1
                                    )
                                }
                                if (capacityUnit.isNotBlank()) {
                                    IconButton(
                                        onClick = { capacityUnit = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "إلغاء وحدة القياس",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "اختر وحدة القياس",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = customCapacityText,
                            onValueChange = { customCapacityText = it },
                            label = { Text("السعة التخزينية القصوى") },
                            placeholder = { Text("مثال: 500 أو 1000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1.6f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }

                    // 5b. Unified Quantity Counter Row (حقل العدد الموحد تحت وحدة القياس)
                    if (!isEditMode) {
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if ((quantityText.toIntOrNull() ?: 1) > 50) Color(0xFFF59E0B) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (count > 1) Color(0xFFEEF2FF) else Color(0xFFF1F5F9)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (count > 1) Icons.Default.FlashOn else Icons.Default.Filter1,
                                                contentDescription = null,
                                                tint = if (count > 1) Color(0xFF4F46E5) else Color(0xFF64748B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "العدد (توليد مواقع متطابقة)",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = if (count == 1) "إدخال موقع واحد فريد" else "توليد $count مواقع متسلسلة بدفعة واحدة ⚡",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                                color = if (count > 1) Color(0xFF4F46E5) else Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val current = quantityText.toIntOrNull() ?: 1
                                                if (current > 1) {
                                                    quantityText = (current - 1).toString()
                                                }
                                            },
                                            enabled = count > 1,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(if (count > 1) Color(0xFFE2E8F0) else Color(0xFFF1F5F9), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "إنقاص العدد",
                                                tint = if (count > 1) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        OutlinedTextField(
                                            value = quantityText,
                                            onValueChange = { newValue ->
                                                val clean = newValue.filter { it.isDigit() }
                                                val numVal = clean.toIntOrNull()
                                                if (numVal != null && numVal > 50) {
                                                    quantityText = "50"
                                                } else {
                                                    quantityText = clean
                                                }
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            modifier = Modifier.width(60.dp).height(48.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF4F46E5),
                                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                                focusedContainerColor = Color.White,
                                                unfocusedContainerColor = Color.White
                                            )
                                        )

                                        IconButton(
                                            onClick = {
                                                val current = quantityText.toIntOrNull() ?: 1
                                                if (current < 50) {
                                                    quantityText = (current + 1).toString()
                                                } else {
                                                    quantityText = "50"
                                                }
                                            },
                                            enabled = count < 50,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(if (count < 50) Color(0xFFEEF2FF) else Color(0xFFF1F5F9), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "زيادة العدد",
                                                tint = if (count < 50) Color(0xFF4F46E5) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                if ((quantityText.toIntOrNull() ?: 1) >= 50) {
                                    Text(
                                        text = "⚠️ تنبيه: تم الوصول للحد الأقصى للتوليد الدفعي (50 موقعاً للدفعة الواحدة)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                        }
                    }

                    // 6. Owner / Supervisor Selection Card
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isOwnerPickerOpen = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selectedOwner != null) Color(0xFFE0E7FF) else Color(0xFFF1F5F9)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (selectedOwner != null) Icons.Default.Person else Icons.Default.PersonOff,
                                        contentDescription = null,
                                        tint = if (selectedOwner != null) Color(0xFF4338CA) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "المسؤول / المشرف المباشر",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = selectedOwner?.let { "${it.name}${if (it.role.isNotBlank()) " (${it.role})" else ""}" } ?: "بدون مشرف محدد",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = if (selectedOwner != null) Color(0xFF4F46E5) else Color(0xFF64748B)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "اختيار المشرف",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 7. Primary Location / Warehouse Switch
                if (isPrimaryApplicable) {
                    val entityTypeName = if (isSite) "موقع" else "مستودع"
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isPrimary || isPrimaryFrozen) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPrimary || isPrimaryFrozen) Icons.Default.Star else Icons.Default.StarOutline,
                                        contentDescription = null,
                                        tint = if (isPrimary || isPrimaryFrozen) Color(0xFFD97706) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "تعيين كـ $entityTypeName أساسي",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (isPrimaryFrozen) "أول $entityTypeName محدد كـ أساسي تلقائياً"
                                        else if (isPrimary) "هذا هو الـ $entityTypeName الأساسي المعتمد في النظام"
                                        else "انقر لتحديد هذا الـ $entityTypeName كـ رئيسي أساسي",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = if (isPrimary || isPrimaryFrozen) Color(0xFFD97706) else Color(0xFF64748B)
                                    )
                                }
                            }
                            Switch(
                                checked = if (isPrimaryFrozen) true else isPrimary,
                                enabled = !isPrimaryFrozen,
                                onCheckedChange = {
                                    isPrimary = it
                                    primaryValidationErrorMessage = null
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFD97706)
                                )
                            )
                        }
                    }
                }

                if (primaryValidationErrorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = primaryValidationErrorMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }

                if (showEditConfirmationDialog) {
                    AlertDialog(
                        onDismissRequest = { showEditConfirmationDialog = false },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5)
                            )
                        },
                        title = {
                            Text("تأكيد التعديل على الموقع")
                        },
                        text = {
                            Text("هل أنت تأكد من رغبتك في حفظ وتأكيد التحديثات الجديدة على بيانات وسعة موقع التخزين؟")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val finalIsPrimary = if (isPrimaryFrozen) true else isPrimary
                                    if (isPrimaryApplicable && relevantExistingCount > 0 && !hasPrimaryInSystem && !finalIsPrimary) {
                                        val entityLabel = if (isSite) "موقع" else "مستودع"
                                        primaryValidationErrorMessage = "لديك $relevantExistingCount $entityLabel مسجلة مسبقاً، رجاءً عيّن أحدها كـ $entityLabel رئيسي"
                                        showEditConfirmationDialog = false
                                        return@Button
                                    }
                                    showEditConfirmationDialog = false
                                    val calculatedOwnerId = selectedOwner?.let { user ->
                                        user.uuid.filter { it.isDigit() }.toLongOrNull()
                                            ?: user.uuid.hashCode().toLong().absoluteValue
                                    }
                                    val autoStructural = if (external) false else (locationType.equals("SITE", ignoreCase = true) || locationType.equals("WAREHOUSE", ignoreCase = true))
                                    val intermediatesList = missingRanks.map { r ->
                                        val pair = intermediateSelections[r]
                                        IntermediateNodeSpec(
                                            locationType = getRankDefaultType(r),
                                            existingId = pair?.first,
                                            name = pair?.second ?: ""
                                        )
                                    }

                                    val rawAddr = if (external) address.trim() else if (isManuallyEditedAddress) customAddressText.trim() else computedBreadcrumbAddress.trim()
                                    val finalAddress = if (rawAddr.contains("[اسم الموقع]")) "" else rawAddr
                                    val finalName = if (external) name.trim() else {
                                        if (finalAddress.isBlank()) ""
                                        else if (finalAddress.contains(" > ")) finalAddress.substringAfterLast(" > ").trim()
                                        else finalAddress.trim()
                                    }

                                    onConfirm(
                                        finalName,
                                        "",
                                        selectedParentId,
                                        autoStructural,
                                        external,
                                        if (external) "SITE" else locationType,
                                        if (external) "warehouse" else icon,
                                        calculatedOwnerId,
                                        "",
                                        finalAddress,
                                        if (external) null else customCapacityText.toDoubleOrNull(),
                                        capacityUnit,
                                        contactPerson.trim().ifBlank { null },
                                        contactPhone.trim().ifBlank { null },
                                        finalIsPrimary,
                                        intermediatesList,
                                        emptyList()
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                            ) {
                                Text("تأكيد التحديث")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditConfirmationDialog = false }) {
                                Text("إلغاء")
                            }
                        }
                    )
                }
            }

            // Fixed Footer Section
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.cancel), fontWeight = FontWeight.Bold)
                    }

                    val rawAddrForCheck = if (external) address.trim() else if (isManuallyEditedAddress) customAddressText.trim() else computedBreadcrumbAddress.trim()
                    val finalAddrForCheck = if (rawAddrForCheck.contains("[اسم الموقع]")) "" else rawAddrForCheck
                    val finalNameForCheck = if (external) name.trim() else {
                        if (finalAddrForCheck.isBlank()) ""
                        else if (finalAddrForCheck.contains(" > ")) finalAddrForCheck.substringAfterLast(" > ").trim()
                        else finalAddrForCheck.trim()
                    }
                    val isValidSingleInput = if (external) name.isNotBlank() else (finalNameForCheck.isNotBlank() && finalNameForCheck != "[اسم الموقع]")

                    val saveButtonText = when {
                        isEditMode -> "حفظ التحديثات"
                        count > 1 -> "حفظ وتوليد $count مواقع متطابقة ⚡"
                        else -> "حفظ الموقع"
                    }

                    Button(
                        onClick = {
                            if (isValidSingleInput) {
                                if (isEditMode) {
                                    showEditConfirmationDialog = true
                                } else {
                                    val finalIsPrimary = if (isPrimaryFrozen) true else isPrimary
                                    if (isPrimaryApplicable && relevantExistingCount > 0 && !hasPrimaryInSystem && !finalIsPrimary) {
                                        val entityLabel = if (isSite) "موقع" else "مستودع"
                                        primaryValidationErrorMessage = "لديك $relevantExistingCount $entityLabel مسجلة مسبقاً، رجاءً عيّن أحدها كـ $entityLabel رئيسي"
                                        return@Button
                                    }

                                    if (gapCount > 0) {
                                        val unfilledRank = missingRanks.firstOrNull { r ->
                                            intermediateSelections[r]?.second?.isBlank() != false
                                        }
                                        if (unfilledRank != null) {
                                            isIntermediateLayersSheetOpen = true
                                            val label = getRankLabel(unfilledRank)
                                            primaryValidationErrorMessage = "يوجد $gapCount مستويات مفقودة في التسلسل الهرمي، رجاءً اضغط على '+ تحديد الطبقات' لتعريف: $label"
                                            return@Button
                                        }
                                    }

                                    val intermediatesList = missingRanks.map { r ->
                                        val pair = intermediateSelections[r]
                                        IntermediateNodeSpec(
                                            locationType = getRankDefaultType(r),
                                            existingId = pair?.first,
                                            name = pair?.second ?: ""
                                        )
                                    }

                                    val evalTargetParentId = if (external) null else if (intermediatesList.isNotEmpty()) {
                                        intermediatesList.lastOrNull { it.existingId != null }?.existingId ?: selectedParentId
                                    } else selectedParentId

                                    val isDuplicate = if (!isEditMode && !external && count <= 1) {
                                        locations.any { loc ->
                                            loc.id != (initialLocation?.id ?: -1L) &&
                                            loc.parentId == evalTargetParentId &&
                                            (loc.name.trim().equals(finalNameForCheck, ignoreCase = true) ||
                                             (finalAddrForCheck.isNotBlank() && loc.address.trim().isNotBlank() && loc.address.trim().equals(finalAddrForCheck, ignoreCase = true)))
                                        }
                                    } else false

                                    if (isDuplicate) {
                                        hasDuplicateAddressError = true
                                        primaryValidationErrorMessage = "هذا العنوان / الاسم مستخدم بالفعل ضمن هذا المسار"
                                        addressShakeController.trigger()
                                        return@Button
                                    }

                                    val calculatedOwnerId = selectedOwner?.let { user ->
                                        user.uuid.filter { it.isDigit() }.toLongOrNull()
                                            ?: user.uuid.hashCode().toLong().absoluteValue
                                    }
                                    val autoStructural = if (external) false else (locationType.equals("SITE", ignoreCase = true) || locationType.equals("WAREHOUSE", ignoreCase = true))
                                    onConfirm(
                                        finalNameForCheck,
                                        "",
                                        if (external) null else selectedParentId,
                                        autoStructural,
                                        external,
                                        if (external) "SITE" else locationType,
                                        if (external) "warehouse" else icon,
                                        if (external) null else calculatedOwnerId,
                                        "",
                                        finalAddrForCheck,
                                        if (external) null else customCapacityText.toDoubleOrNull(),
                                        capacityUnit,
                                        contactPerson.trim().ifBlank { null },
                                        contactPhone.trim().ifBlank { null },
                                        finalIsPrimary,
                                        intermediatesList,
                                        if (count > 1) generatedNames else emptyList()
                                    )
                                }
                            }
                        },
                        enabled = isValidSingleInput && (!isEditMode || hasChanged),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (external) Color(0xFF2563EB) else Color(0xFF4F46E5),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (count > 1) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(saveButtonText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}



private data class PackagingOption(
    val code: String,
    val labelAr: String,
    val descAr: String
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockItemPackagingSelectionBottomSheet(
    selectedPackagingCode: String,
    packagingOptionsList: List<PackagingOption>,
    onDismiss: () -> Unit,
    onPackagingSelected: (PackagingOption) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "نوع التغليف والتعبئة",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "حدد نوع أسلوب تغليف القطعة أو المادة المخزنية",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                item {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📦 1. تغليف المكونات الإلكترونية والدقيقة (SMD)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = Color(0xFF4338CA),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                items(packagingOptionsList.take(5)) { opt ->
                    val isSelected = opt.code.equals(selectedPackagingCode, ignoreCase = true)
                    Surface(
                        onClick = {
                            onPackagingSelected(opt)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = opt.labelAr,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF0F172A)
                                )
                                Text(
                                    text = opt.descAr,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🚚 2. تغليف الشحن والاستيراد السائب (Bulk)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = Color(0xFF4338CA),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                items(packagingOptionsList.drop(5)) { opt ->
                    val isSelected = opt.code.equals(selectedPackagingCode, ignoreCase = true)
                    Surface(
                        onClick = {
                            onPackagingSelected(opt)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = opt.labelAr,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF0F172A)
                                )
                                Text(
                                    text = opt.descAr,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إلغاء")
            }
        }
    }
}


/**
 * ورقة منبثقة تفاعلية لاختيار أمر الشراء المعتمد (PurchaseOrderSelectionBottomSheet).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PurchaseOrderSelectionBottomSheet(
    purchaseOrders: List<PurchaseOrder>,
    selectedPurchaseOrderId: Long?,
    onOrderSelected: (PurchaseOrder?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredOrders = remember(purchaseOrders, searchQuery) {
        val activeOrders = purchaseOrders.filter { it.status != POStatus.CANCELLED }
        if (searchQuery.isBlank()) {
            activeOrders
        } else {
            val q = searchQuery.trim().lowercase()
            activeOrders.filter {
                it.reference.lowercase().contains(q) ||
                it.supplierName.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "اختيار أمر الشراء المرجعي",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "ربط الشحنة المستلمة بأمر شراء معتمد لتوثيق الحسابات والكميات",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث بالرمز المرجعي أو اسم المورد...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                // الخيار الافتراضي: استلام مباشر حر (بدون أمر شراء)
                item {
                    val isNoneSelected = selectedPurchaseOrderId == null
                    Surface(
                        onClick = {
                            onOrderSelected(null)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isNoneSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.5.dp, if (isNoneSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isNoneSelected) Color(0xFFE0E7FF) else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inbox,
                                        contentDescription = null,
                                        tint = if (isNoneSelected) Color(0xFF4F46E5) else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "استلام مباشر حر (بدون أمر شراء)",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (isNoneSelected) Color(0xFF312E81) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "إيداع فوري بالرصيد المخزني دون تقييد بسجلات التوريد",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            if (isNoneSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // قائمة أوامر الشراء
                items(filteredOrders) { order ->
                    val isSelected = order.id == selectedPurchaseOrderId
                    Surface(
                        onClick = {
                            onOrderSelected(order)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFFE0E7FF),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = order.reference,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.5.sp
                                            ),
                                            color = Color(0xFF4338CA),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Surface(
                                        color = when (order.status) {
                                            POStatus.PLACED -> Color(0xFFDCFCE7)
                                            POStatus.COMPLETE -> Color(0xFFE0F2FE)
                                            POStatus.PENDING -> Color(0xFFFEF3C7)
                                            else -> Color(0xFFF1F5F9)
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = order.status.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp
                                            ),
                                            color = when (order.status) {
                                                POStatus.PLACED -> Color(0xFF15803D)
                                                POStatus.COMPLETE -> Color(0xFF0369A1)
                                                POStatus.PENDING -> Color(0xFFB45309)
                                                else -> Color(0xFF475569)
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "المورّد: ${order.supplierName.ifBlank { "غير محدد" }}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    if (order.description.isNotBlank()) {
                                        Text(
                                            text = order.description,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color(0xFF64748B),
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (order.targetDate.isNotBlank()) {
                                    Text(
                                        text = "التسليم: ${order.targetDate}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إلغاء")
            }
        }
    }
}


/**
 * ورقة منبثقة تفاعلية لاختيار الشركة الموردة (SupplierSelectionBottomSheet).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SupplierSelectionBottomSheet(
    suppliers: List<Company>,
    selectedSupplierId: Long?,
    onSupplierSelected: (Company?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredSuppliers = remember(suppliers, searchQuery) {
        val validSuppliers = suppliers.filter { it.isSupplier }
        if (searchQuery.isBlank()) {
            validSuppliers
        } else {
            val q = searchQuery.trim().lowercase()
            validSuppliers.filter {
                it.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "اختيار الشركة الموردة (Supplier)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "تحديد المورد المعتمد للمادة أو رصيد افتتاحي بدون مورد",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم الشركة الموردة...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                // الخيار الافتراضي: مورد غير محدد / رصيد افتتاحي
                item {
                    val isNoneSelected = selectedSupplierId == null
                    Surface(
                        onClick = {
                            onSupplierSelected(null)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isNoneSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.5.dp, if (isNoneSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isNoneSelected) Color(0xFFE0E7FF) else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = if (isNoneSelected) Color(0xFF4F46E5) else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "مورد غير محدد / رصيد افتتاحي",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (isNoneSelected) Color(0xFF312E81) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "استلام رصيد أولي دون ربطه بشركة توريد محددة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            if (isNoneSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // قائمة الموردين
                items(filteredSuppliers) { company ->
                    val isSelected = company.id == selectedSupplierId
                    Surface(
                        onClick = {
                            onSupplierSelected(company)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFECFDF5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = company.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    if (company.description.isNotBlank()) {
                                        Text(
                                            text = company.description,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color(0xFF64748B),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إلغاء")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddStockItemBottomSheet(
    parts: List<Part>,
    locations: List<StockLocation>,
    categories: List<PartCategory> = emptyList(),
    purchaseOrders: List<PurchaseOrder> = emptyList(),
    suppliers: List<Company> = emptyList(),
    supplierParts: List<SupplierPart> = emptyList(),
    users: List<AppUser> = emptyList(),
    locationTypes: List<StockLocationType> = emptyList(),
    onDismiss: () -> Unit,
    onAddNewPart: (
        name: String,
        ipn: String,
        desc: String,
        catId: Long?,
        units: String,
        assembly: Boolean,
        component: Boolean,
        isTemplate: Boolean,
        variantOf: Long?,
        minimumStock: Double,
        maximumStock: Double?,
        revision: String,
        keywords: String,
        trackable: Boolean,
        purchaseable: Boolean,
        salable: Boolean,
        virtual: Boolean,
        defaultLocationId: Long?,
        defaultExpiryDays: Int?,
        pendingAttachments: List<PendingAttachment>,
        active: Boolean,
        locked: Boolean
    ) -> Part,
    onAddNewLocation: (
        name: String,
        description: String,
        parentId: Long?,
        structural: Boolean,
        external: Boolean,
        locationType: String,
        icon: String,
        ownerId: Long?,
        customIcon: String,
        address: String
    ) -> StockLocation,
    onAddNewLocationBulk: ((Long?, String, String, Int, Int, Boolean, String) -> Unit)? = null,
    onConfirm: (
        partId: Long,
        locationId: Long?,
        quantity: Double,
        serial: String,
        batch: String,
        packaging: String,
        status: StockStatus,
        purchasePrice: Double,
        purchasePriceCurrency: String,
        supplierPartId: Long?,
        purchaseOrderId: Long?,
        expiryDate: String,
        reviewNeeded: Boolean,
        deleteOnDeplete: Boolean,
        link: String,
        notes: String,
        unitWeight: Double?,
        totalWeight: Double?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedPartId by remember { mutableStateOf<Long?>(parts.firstOrNull()?.id) }
    var isPartSelectionSheetOpen by remember { mutableStateOf(false) }
    var isAddPartSheetOpen by remember { mutableStateOf(false) }
    var selectedLocationId by remember { mutableStateOf<Long?>(locations.firstOrNull { !it.structural }?.id ?: locations.firstOrNull()?.id) }
    var isLocationSelectionSheetOpen by remember { mutableStateOf(false) }
    var isAddNewLocationSheetOpen by remember { mutableStateOf(false) }
    var quantityText by remember { mutableStateOf("1.0") }
    var isCalculatorMode by remember { mutableStateOf(true) }
    var packageCountText by remember { mutableStateOf("1") }
    var capacityPerPackageText by remember { mutableStateOf("1.0") }
    var serial by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("") }
    var packaging by remember { mutableStateOf("Box") }
    var isPackagingSheetOpen by remember { mutableStateOf(false) }

    var isWeightModeUnit by remember { mutableStateOf(true) }
    var unitWeightText by remember { mutableStateOf("") }
    var totalWeightText by remember { mutableStateOf("") }

    val packagingOptionsList = remember {
        listOf(
            PackagingOption("Reel", "Reel (بكرة)", "شريط دائري تُلف عليه العناصر السطحية (SMD) للالتقاط الآلي"),
            PackagingOption("Cut Tape", "Cut Tape (شريط مقصوص)", "أجزاء مقصوصة من البكرة للكميات الصغيرة أو العينات"),
            PackagingOption("Tube / Stick", "Tube / Stick (أنبوب بلاستيكي)", "أنابيب صلبة لحماية الدوائر المتكاملة (ICs) ومنع انثناء الأرجل"),
            PackagingOption("Tray", "Tray (صينية واقية)", "صوانٍ مقسمة لحمل المعالجات والشرائح الكبيرة الحساسة والتفريغ الكهروستاتيكي"),
            PackagingOption("Anti-Static Bag", "Anti-Static Bag (كيس مضاد للكهرباء)", "لحفظ الوحدات والمكونات المنفصلة وحمايتها"),
            PackagingOption("Box", "Box / Carton (صندوق / كرتونة)", "التغليف القياسي لتخزين البضائع والمستشعرات والمجموعات الجاهزة"),
            PackagingOption("Bulk / Loose", "Bulk / Loose (سائب)", "قطع غير مغلفة فردياً، مثل البراغي، الصواميل، أو الأسلاك"),
            PackagingOption("Pallet", "Pallet (منصة نقالة)", "للشحنات الكبيرة والحاويات عند تخزين عدد كبير من الكراتين معاً")
        )
    }

    var selectedStatus by remember { mutableStateOf(StockStatus.OK) }
    var purchasePriceText by remember { mutableStateOf("0.0") }
    var currency by remember { mutableStateOf("USD") }
    var selectedPurchaseOrderId by remember { mutableStateOf<Long?>(null) }
    var isPOPickerOpen by remember { mutableStateOf(false) }
    var selectedSupplierId by remember { mutableStateOf<Long?>(null) }
    var selectedSupplierPartId by remember { mutableStateOf<Long?>(null) }
    var isSupplierPickerOpen by remember { mutableStateOf(false) }
    var expiryDate by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(selectedPartId) {
        val targetPart = parts.find { it.id == selectedPartId }
        if (targetPart?.defaultLocationId != null && locations.any { it.id == targetPart.defaultLocationId }) {
            selectedLocationId = targetPart.defaultLocationId
        }
    }

    LaunchedEffect(selectedPurchaseOrderId) {
        if (selectedPurchaseOrderId != null) {
            val po = purchaseOrders.find { it.id == selectedPurchaseOrderId }
            if (po != null) {
                selectedSupplierId = po.supplierId
                if (po.orderCurrency.isNotBlank()) {
                    currency = po.orderCurrency
                }
                val matchingLine = po.lineItems.find { line ->
                    line.supplierPartId == selectedPartId ||
                    supplierParts.any { sp -> sp.id == line.supplierPartId && sp.partId == selectedPartId }
                } ?: po.lineItems.firstOrNull()

                if (matchingLine != null) {
                    val supPart = supplierParts.find { it.id == matchingLine.supplierPartId }
                    if (supPart != null) {
                        selectedSupplierPartId = supPart.id
                    } else if (matchingLine.supplierPartId > 0) {
                        selectedSupplierPartId = matchingLine.supplierPartId
                    }
                    if (matchingLine.purchasePrice > 0.0) {
                        purchasePriceText = matchingLine.purchasePrice.toString()
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddBox,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = stringResource(Res.string.add_new_stock),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = Color(0xFF0F172A)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Form Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "اختر القطعة المراد استلامها *:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )

                val currentSelectedPart = parts.find { it.id == selectedPartId }

                Surface(
                    onClick = { isPartSelectionSheetOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = if (currentSelectedPart != null) Color(0xFFF8FAFC) else Color.White,
                    border = BorderStroke(1.5.dp, if (currentSelectedPart != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (currentSelectedPart != null) Color(0xFFEEF2FF) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Widgets,
                                    contentDescription = null,
                                    tint = if (currentSelectedPart != null) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                if (currentSelectedPart != null) {
                                    Text(
                                        text = currentSelectedPart.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (currentSelectedPart.ipn.isNotBlank()) "الرقم الداخلي IPN: ${currentSelectedPart.ipn}" else "انقر لتغيير القطعة المختارة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                } else {
                                    Text(
                                        text = "انقر هنا لاختيار القطعة من القائمة...",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEEF2FF)
                        ) {
                            Text(
                                text = if (currentSelectedPart != null) "تغيير ▾" else "اختيار ▾",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "اختر موقع التخزين المباشر *:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )

                val currentSelectedLocation = locations.find { it.id == selectedLocationId }

                Surface(
                    onClick = { isLocationSelectionSheetOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = if (currentSelectedLocation != null) Color(0xFFF8FAFC) else Color.White,
                    border = BorderStroke(1.5.dp, if (currentSelectedLocation != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (currentSelectedLocation != null) Color(0xFFEEF2FF) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = if (currentSelectedLocation != null) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                if (currentSelectedLocation != null) {
                                    Text(
                                        text = "${if (currentSelectedLocation.structural) "🏗️" else "📍"} ${currentSelectedLocation.name}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (currentSelectedLocation.description.isNotBlank()) currentSelectedLocation.description else "موقع تخزين جاهز للاستلام",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                } else {
                                    Text(
                                        text = "انقر هنا لاختيار موقع التخزين من القائمة...",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEEF2FF)
                        ) {
                            Text(
                                text = if (currentSelectedLocation != null) "تغيير ▾" else "اختيار ▾",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // بطاقة الكمية والتغليف بالحاسبة الذكية (Packaging & Quantity Calculator Card)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. الترويسة مع زر تبديل نمط الحساب (حاسبة العبوات vs إدخال مباشر)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEEF2FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "حاسبة الكمية والتغليف الذكية",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (isCalculatorMode) "حساب تلقائي (عدد العبوات × سعة العبوة)" else "إدخال مباشر للكمية الإجمالية",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // زر التبديل
                            Surface(
                                onClick = { isCalculatorMode = !isCalculatorMode },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCalculatorMode) Color(0xFFEEF2FF) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isCalculatorMode) Color(0xFFC7D2FE) else Color(0xFFCBD5E1))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCalculatorMode) Icons.Default.EditNote else Icons.Default.Calculate,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isCalculatorMode) "إدخال مباشر" else "حاسبة العبوات",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        ),
                                        color = Color(0xFF4F46E5)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        // 2. حقل اختيار نوع التغليف
                        val selectedOpt = packagingOptionsList.find { it.code.equals(packaging, ignoreCase = true) }
                        val displayPackagingText = selectedOpt?.labelAr ?: packaging.ifBlank { "Box / Carton (صندوق / كرتونة)" }

                        Surface(
                            onClick = { isPackagingSheetOpen = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "نوع التغليف والتعبئة",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = displayPackagingText,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEEF2FF)
                                ) {
                                    Text(
                                        text = "تغيير ▾",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = Color(0xFF4F46E5),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // 3. حقول الإدخال حسب نمط الحساب
                        if (isCalculatorMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Field 1: عدد العبوات
                                OutlinedTextField(
                                    value = packageCountText,
                                    onValueChange = {
                                        packageCountText = it
                                        val count = it.toDoubleOrNull() ?: 0.0
                                        val cap = capacityPerPackageText.toDoubleOrNull() ?: 0.0
                                        quantityText = (count * cap).toString()
                                    },
                                    label = { Text("عدد العبوات/الأغلفة") },
                                    placeholder = { Text("1") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    enabled = serial.isBlank(),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4F46E5),
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )

                                Text("×", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF64748B))

                                // Field 2: سعة العبوة الواحدة
                                OutlinedTextField(
                                    value = capacityPerPackageText,
                                    onValueChange = {
                                        capacityPerPackageText = it
                                        val count = packageCountText.toDoubleOrNull() ?: 0.0
                                        val cap = it.toDoubleOrNull() ?: 0.0
                                        quantityText = (count * cap).toString()
                                    },
                                    label = { Text("سعة العبوة الواحدة") },
                                    placeholder = { Text("100") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    enabled = serial.isBlank(),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4F46E5),
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                            }
                        } else {
                            // نمط الإدخال المباشر للكمية
                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { quantityText = it },
                                label = { Text("إجمالي الكمية المادية المستلمة") },
                                placeholder = { Text("مثال: 50.0") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                enabled = serial.isBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4F46E5),
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }

                        // 4. شريط النتيجة التفاعلي (Live Result Badge)
                        val unitLabel = currentSelectedPart?.units?.ifBlank { "قطعة / pcs" } ?: "قطعة / pcs"
                        val totalQtyVal = if (isCalculatorMode) {
                            (packageCountText.toDoubleOrNull() ?: 0.0) * (capacityPerPackageText.toDoubleOrNull() ?: 0.0)
                        } else {
                            quantityText.toDoubleOrNull() ?: 0.0
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEEF2FF),
                            border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "إجمالي الكمية المستلمة الفعلية:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = Color(0xFF4338CA)
                                        )
                                        if (isCalculatorMode) {
                                            Text(
                                                text = "(${packageCountText.ifBlank { "0" }} عبوات × ${capacityPerPackageText.ifBlank { "0" }} لكل عبوة)",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                color = Color(0xFF6366F1)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "$totalQtyVal $unitLabel",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color(0xFF312E81)
                                )
                            }
                        }
                    }
                }

                // ⚖️ قسم معامل احتساب وزن الشحنة على الرف (يظهر عند اختلاف وحدة الرف عن وحدة الصنف)
                val selectedLocObj = locations.find { it.id == selectedLocationId }
                val selectedPartObj = parts.find { it.id == selectedPartId }
                val locCapUnit = selectedLocObj?.capacityUnit ?: "قطعة"
                val partMeasureUnit = selectedPartObj?.units ?: "pcs"
                val isWeightCapacity = locCapUnit.equals("kg", ignoreCase = true) ||
                        locCapUnit.equals("كغ", ignoreCase = true) ||
                        locCapUnit.equals("كيلوغرام", ignoreCase = true) ||
                        locCapUnit.equals("ton", ignoreCase = true) ||
                        locCapUnit.equals("طن", ignoreCase = true)
                val isUnitMismatch = isWeightCapacity && !partMeasureUnit.equals(locCapUnit, ignoreCase = true)

                if (isUnitMismatch) {
                    val totalQtyVal = if (isCalculatorMode) {
                        (packageCountText.toDoubleOrNull() ?: 0.0) * (capacityPerPackageText.toDoubleOrNull() ?: 0.0)
                    } else {
                        quantityText.toDoubleOrNull() ?: 0.0
                    }

                    val calcUnitWeight = if (isWeightModeUnit) {
                        unitWeightText.toDoubleOrNull()
                    } else {
                        val tot = totalWeightText.toDoubleOrNull()
                        if (tot != null && totalQtyVal > 0.0) tot / totalQtyVal else null
                    }

                    val calcTotalWeight = if (!isWeightModeUnit) {
                        totalWeightText.toDoubleOrNull()
                    } else {
                        val uw = unitWeightText.toDoubleOrNull()
                        if (uw != null && totalQtyVal > 0.0) uw * totalQtyVal else null
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Scale,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "معامل احتساب وزن الشحنة على الرف ⚖️",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "سعة الرف تقاس بـ ($locCapUnit) والصنف بـ ($partMeasureUnit). أدخل الوزن للتحويل التلقائي:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }

                            // Mode Selector Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .padding(3.dp)
                            ) {
                                Surface(
                                    onClick = { isWeightModeUnit = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isWeightModeUnit) Color.White else Color.Transparent,
                                    shadowElevation = if (isWeightModeUnit) 1.dp else 0.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "⚖️ وزن الوحدة الواحدة",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                            color = if (isWeightModeUnit) Color(0xFF92400E) else Color(0xFFB45309)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { isWeightModeUnit = false },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (!isWeightModeUnit) Color.White else Color.Transparent,
                                    shadowElevation = if (!isWeightModeUnit) 1.dp else 0.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "📦 الوزن الكلي للشحنة",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                            color = if (!isWeightModeUnit) Color(0xFF92400E) else Color(0xFFB45309)
                                        )
                                    }
                                }
                            }

                            if (isWeightModeUnit) {
                                OutlinedTextField(
                                    value = unitWeightText,
                                    onValueChange = { unitWeightText = it },
                                    label = { Text("وزن $partMeasureUnit الواحد (كغ / kg)") },
                                    placeholder = { Text("مثال: 2.5") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFD97706),
                                        unfocusedBorderColor = Color(0xFFFDE68A),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                            } else {
                                OutlinedTextField(
                                    value = totalWeightText,
                                    onValueChange = { totalWeightText = it },
                                    label = { Text("الوزن الإجمالي المحمّل على الرف (كغ / kg)") },
                                    placeholder = { Text("مثال: 200") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFD97706),
                                        unfocusedBorderColor = Color(0xFFFDE68A),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                            }

                            // Live Calculation Summary
                            val displayTotalWeight = calcTotalWeight?.let { "%.2f".format(it) } ?: "—"
                            val displayUnitWeight = calcUnitWeight?.let { "%.2f".format(it) } ?: "—"

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "الوزن الكلي المحسوب للرف: $displayTotalWeight كغ",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "وزن الوحدة التقديري للأرشيف: $displayUnitWeight كغ / $partMeasureUnit",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }
                    }
                }

                // قسم المعلومات الإضافية القابل للطي (Accordion / Expandable Section)
                var isAdditionalInfoExpanded by remember { mutableStateOf(false) }

                Surface(
                    onClick = { isAdditionalInfoExpanded = !isAdditionalInfoExpanded },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isAdditionalInfoExpanded) Color(0xFFF8FAFC) else Color(0xFFFAFAFA),
                    border = BorderStroke(1.dp, if (isAdditionalInfoExpanded) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isAdditionalInfoExpanded) Color(0xFFEEF2FF) else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = if (isAdditionalInfoExpanded) Color(0xFF4F46E5) else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "معلومات إضافية (اختياري)",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "رقم الدفعة، الرقم التسلسلي، الصلاحية، سعر الشراء، والملاحظات",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isAdditionalInfoExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isAdditionalInfoExpanded) "طَي" else "توسيع",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (isAdditionalInfoExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Spacer(modifier = Modifier.height(12.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. رقم التشغيلة/الدفعة (Batch Code)
                                OutlinedTextField(
                                    value = batch,
                                    onValueChange = { batch = it },
                                    label = { Text("رقم التشغيلة/الدفعة (Batch Code)") },
                                    placeholder = { Text("مثال: BATCH-2025-01") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4F46E5),
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )

                                // 2. الرقم التسلسلي (Serial Number)
                                OutlinedTextField(
                                    value = serial,
                                    onValueChange = {
                                        serial = it
                                        if (it.isNotBlank()) quantityText = "1.0"
                                    },
                                    label = { Text("الرقم التسلسلي الفريد (Serial Number)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4F46E5),
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )

                                var showExpiryDatePicker by remember { mutableStateOf(false) }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = expiryDate,
                                        onValueChange = { expiryDate = it },
                                        readOnly = true,
                                        label = { Text("تاريخ انتهاء الصلاحية") },
                                        placeholder = { Text("انقر لاختيار التاريخ...") },
                                        singleLine = true,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Event,
                                                contentDescription = null,
                                                tint = Color(0xFF4F46E5)
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { showExpiryDatePicker = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.DateRange,
                                                    contentDescription = "اختيار التاريخ",
                                                    tint = Color(0xFF4F46E5)
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showExpiryDatePicker = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF4F46E5),
                                            unfocusedBorderColor = Color(0xFFCBD5E1),
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White
                                        )
                                    )

                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { showExpiryDatePicker = true }
                                    )
                                }

                                if (showExpiryDatePicker) {
                                    val datePickerState = rememberDatePickerState(
                                        initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds()
                                    )
                                    DatePickerDialog(
                                        onDismissRequest = { showExpiryDatePicker = false },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                datePickerState.selectedDateMillis?.let { millis ->
                                                    val instant = Instant.fromEpochMilliseconds(millis)
                                                    val dateTime = instant.toLocalDateTime(TimeZone.UTC)
                                                    val year = dateTime.year
                                                    val month = dateTime.monthNumber.toString().padStart(2, '0')
                                                    val day = dateTime.dayOfMonth.toString().padStart(2, '0')
                                                    expiryDate = "$year-$month-$day"
                                                }
                                                showExpiryDatePicker = false
                                            }) {
                                                Text("تأكيد الاختيار", fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showExpiryDatePicker = false }) {
                                                Text("إلغاء")
                                            }
                                        }
                                    ) {
                                        DatePicker(state = datePickerState)
                                    }
                                }

                                var isCurrencyPickerOpen by remember { mutableStateOf(false) }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = purchasePriceText,
                                        onValueChange = { purchasePriceText = it },
                                        label = { Text("سعر الشراء (اختياري)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF4F46E5),
                                            unfocusedBorderColor = Color(0xFFCBD5E1),
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White
                                        )
                                    )

                                    CurrencySelectorField(
                                        selectedCurrencyCode = currency,
                                        onOpenPicker = { isCurrencyPickerOpen = true },
                                        label = "العملة",
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (isCurrencyPickerOpen) {
                                    CurrencySelectionBottomSheet(
                                        selectedCurrencyCode = currency,
                                        onDismiss = { isCurrencyPickerOpen = false },
                                        onCurrencySelected = { selectedCurr ->
                                            currency = selectedCurr.code
                                        }
                                    )
                                }

                                // بطاقة اختيار الشركة الموردة (Supplier Selector Card)
                                val selectedPO = remember(purchaseOrders, selectedPurchaseOrderId) {
                                    purchaseOrders.find { it.id == selectedPurchaseOrderId }
                                }
                                val isSupplierLocked = selectedPO != null
                                val selectedSupplier = remember(suppliers, selectedSupplierId) {
                                    suppliers.find { it.id == selectedSupplierId }
                                }

                                Surface(
                                    onClick = { if (!isSupplierLocked) isSupplierPickerOpen = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSupplierLocked) Color(0xFFF1F5F9) else Color.White,
                                    border = BorderStroke(1.dp, if (selectedSupplier != null) Color(0xFFC7D2FE) else Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (selectedSupplier != null) Color(0xFFECFDF5) else Color(0xFFF1F5F9)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocalShipping,
                                                    contentDescription = null,
                                                    tint = if (selectedSupplier != null) Color(0xFF059669) else Color(0xFF64748B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "المورّد / الشركة الموردة (اختياري)",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                        color = Color(0xFF64748B)
                                                    )
                                                    if (isSupplierLocked) {
                                                        Surface(
                                                            color = Color(0xFFFEF3C7),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "🔒 مرتبط بأمر الشراء",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 9.5.sp
                                                                ),
                                                                color = Color(0xFFB45309),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = selectedSupplier?.name ?: "مورد غير محدد / رصيد افتتاحي",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    ),
                                                    color = if (selectedSupplier != null) Color(0xFF0F172A) else Color(0xFF64748B)
                                                )
                                            }
                                        }

                                        if (!isSupplierLocked) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFEEF2FF)
                                            ) {
                                                Text(
                                                    text = if (selectedSupplier != null) "تغيير ▾" else "اختيار ▾",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    ),
                                                    color = Color(0xFF4F46E5),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // صف اختيار SKU المورّد الخاطف (إن وجد)
                                val matchingSKUs = remember(supplierParts, selectedSupplierId, selectedPartId) {
                                    if (selectedSupplierId == null || selectedPartId == null) emptyList()
                                    else supplierParts.filter { it.supplierId == selectedSupplierId && it.partId == selectedPartId }
                                }

                                if (matchingSKUs.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "رمز قطعة المورّد المتاحة (Supplier SKU):",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            color = Color(0xFF4338CA)
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(matchingSKUs) { sp ->
                                                val isSkuSelected = selectedSupplierPartId == sp.id
                                                FilterChip(
                                                    selected = isSkuSelected,
                                                    onClick = {
                                                        selectedSupplierPartId = if (isSkuSelected) null else sp.id
                                                    },
                                                    label = {
                                                        Text("SKU: ${sp.sku}${if (sp.description.isNotBlank()) " (${sp.description})" else ""}")
                                                    },
                                                    leadingIcon = if (isSkuSelected) {
                                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                                    } else null,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // بطاقة اختيار أمر الشراء المرجعي (استخدام selectedPO المعرفة أعلاه)


                                Surface(
                                    onClick = { isPOPickerOpen = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, if (selectedPO != null) Color(0xFFC7D2FE) else Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (selectedPO != null) Color(0xFFEEF2FF) else Color(0xFFF1F5F9)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ShoppingCart,
                                                    contentDescription = null,
                                                    tint = if (selectedPO != null) Color(0xFF4F46E5) else Color(0xFF64748B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = "أمر الشراء المرجعي (اختياري)",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                    color = Color(0xFF64748B)
                                                )
                                                if (selectedPO != null) {
                                                    Text(
                                                        text = "${selectedPO.reference} - ${selectedPO.supplierName.ifBlank { "مورّد" }}",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp
                                                        ),
                                                        color = Color(0xFF0F172A)
                                                    )
                                                } else {
                                                    Text(
                                                        text = "استلام مباشر حر (بدون أمر شراء)",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp
                                                        ),
                                                        color = Color(0xFF475569)
                                                    )
                                                }
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFEEF2FF)
                                        ) {
                                            Text(
                                                text = if (selectedPO != null) "تغيير ▾" else "اختيار ▾",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                ),
                                                color = Color(0xFF4F46E5),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = link,
                                    onValueChange = { link = it },
                                    label = { Text("رابط الوثيقة / الفحص الخارجي (link)") },
                                    placeholder = { Text("https://example.com/doc.pdf") },
                                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF4F46E5)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4F46E5),
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )

                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text("ملاحظات إضافية على الكمية") },
                                    minLines = 2,
                                    maxLines = 4,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4F46E5),
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // شريط الأزرار الثابت في أسفل الواجهة (Fixed Bottom Action Bar)
            HorizontalDivider(color = Color(0xFFF1F5F9))

            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.cancel), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val pId = selectedPartId
                            val qty = if (isCalculatorMode) {
                                (packageCountText.toDoubleOrNull() ?: 0.0) * (capacityPerPackageText.toDoubleOrNull() ?: 0.0)
                            } else {
                                quantityText.toDoubleOrNull() ?: 1.0
                            }
                            val price = purchasePriceText.toDoubleOrNull() ?: 0.0

                            val selectedLocObj = locations.find { it.id == selectedLocationId }
                            val selectedPartObj = parts.find { it.id == selectedPartId }
                            val locCapUnit = selectedLocObj?.capacityUnit ?: "قطعة"
                            val partMeasureUnit = selectedPartObj?.units ?: "pcs"
                            val isWeightCap = locCapUnit.equals("kg", ignoreCase = true) ||
                                    locCapUnit.equals("كغ", ignoreCase = true) ||
                                    locCapUnit.equals("كيلوغرام", ignoreCase = true) ||
                                    locCapUnit.equals("ton", ignoreCase = true) ||
                                    locCapUnit.equals("طن", ignoreCase = true)
                            val isMismatch = isWeightCap && !partMeasureUnit.equals(locCapUnit, ignoreCase = true)

                            val calcUnitWeight = if (isMismatch) {
                                if (isWeightModeUnit) unitWeightText.toDoubleOrNull()
                                else {
                                    val tot = totalWeightText.toDoubleOrNull()
                                    if (tot != null && qty > 0.0) tot / qty else null
                                }
                            } else null

                            val calcTotalWeight = if (isMismatch) {
                                if (!isWeightModeUnit) totalWeightText.toDoubleOrNull()
                                else {
                                    val uw = unitWeightText.toDoubleOrNull()
                                    if (uw != null && qty > 0.0) uw * qty else null
                                }
                            } else null

                            if (pId != null && qty > 0.0) {
                                onConfirm(
                                    pId, selectedLocationId, qty, serial, batch, packaging,
                                    selectedStatus, price, currency, selectedSupplierPartId,
                                    selectedPurchaseOrderId, expiryDate,
                                    false, false, link, notes,
                                    calcUnitWeight, calcTotalWeight
                                )
                            }
                        },
                        enabled = selectedPartId != null && (quantityText.toDoubleOrNull() ?: 0.0) > 0.0,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text(stringResource(Res.string.save), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (isSupplierPickerOpen) {
        SupplierSelectionBottomSheet(
            suppliers = suppliers,
            selectedSupplierId = selectedSupplierId,
            onSupplierSelected = { comp ->
                selectedSupplierId = comp?.id
                // Reset selected supplierPartId if supplier changes
                val matchingSupParts = if (comp == null || selectedPartId == null) emptyList() else supplierParts.filter { it.supplierId == comp.id && it.partId == selectedPartId }
                selectedSupplierPartId = matchingSupParts.firstOrNull()?.id
            },
            onDismiss = { isSupplierPickerOpen = false }
        )
    }

    if (isPOPickerOpen) {
        PurchaseOrderSelectionBottomSheet(
            purchaseOrders = purchaseOrders,
            selectedPurchaseOrderId = selectedPurchaseOrderId,
            onOrderSelected = { po ->
                selectedPurchaseOrderId = po?.id
                if (po != null && po.orderCurrency.isNotBlank()) {
                    currency = po.orderCurrency
                }
            },
            onDismiss = { isPOPickerOpen = false }
        )
    }

    if (isPackagingSheetOpen) {
        StockItemPackagingSelectionBottomSheet(
            selectedPackagingCode = packaging,
            packagingOptionsList = packagingOptionsList,
            onDismiss = { isPackagingSheetOpen = false },
            onPackagingSelected = { opt ->
                packaging = opt.code
                isPackagingSheetOpen = false
            }
        )
    }

    if (isPartSelectionSheetOpen) {
        PartSelectionBottomSheet(
            parts = parts,
            selectedPartId = selectedPartId,
            onPartSelected = { selectedPartId = it },
            onAddNewPartClick = { isAddPartSheetOpen = true },
            onDismiss = { isPartSelectionSheetOpen = false }
        )
    }

    if (isLocationSelectionSheetOpen) {
        LocationSelectionBottomSheet(
            locations = locations,
            selectedLocationId = selectedLocationId,
            onLocationSelected = { selectedLocationId = it },
            onAddNewLocationClick = { isAddNewLocationSheetOpen = true },
            onDismiss = { isLocationSelectionSheetOpen = false }
        )
    }

    if (isAddPartSheetOpen) {
        AddPartBottomSheet(
            categories = categories,
            locations = locations,
            templateParts = remember(parts) { parts.filter { it.isTemplate } },
            allParts = parts,
            onDismiss = { isAddPartSheetOpen = false },
            onConfirm = { name, ipn, desc, catId, units, assembly, component, isTemplate, variantOf, minStock, maxStock, revision, keywords, trackable, purchaseable, salable, virtual, defaultLocId, defaultExpiryDays, pendingAttachments, active, locked ->
                val insertedPart = onAddNewPart(
                    name, ipn, desc, catId, units, assembly, component,
                    isTemplate, variantOf, minStock, maxStock, revision,
                    keywords, trackable, purchaseable, salable, virtual,
                    defaultLocId, defaultExpiryDays, pendingAttachments, active, locked
                )
                selectedPartId = insertedPart.id
                isAddPartSheetOpen = false
            }
        )
    }

    if (isAddNewLocationSheetOpen) {
        AddStockLocationBottomSheet(
            locations = locations,
            users = users,
            locationTypes = locationTypes,
            onDismiss = { isAddNewLocationSheetOpen = false },
            onQuickAddParent = { parentName, parentType, grandparentId ->
                onAddNewLocation(
                    parentName, "", grandparentId, true, false, parentType, "warehouse", null, "", ""
                )
            },
            onConfirm = { name, desc, parentId, structural, external, locationType, icon, ownerId, customIcon, address, customCapacity, capacityUnit, contactPerson, contactPhone, isPrimary, _, generatedNames ->
                if (generatedNames.size > 1) {
                    generatedNames.forEach { locName ->
                        val computedAddress = if (address.contains(" > ")) {
                            "${address.substringBeforeLast(" > ")} > $locName"
                        } else locName
                        onAddNewLocation(
                            locName, desc, parentId, structural, external, locationType, icon, ownerId, customIcon, computedAddress
                        )
                    }
                } else {
                    val insertedLocation = onAddNewLocation(
                        name, desc, parentId, structural, external, locationType, icon, ownerId, customIcon, address
                    )
                    selectedLocationId = insertedLocation.id
                }
                isAddNewLocationSheetOpen = false
            }
        )
    }
}


/**
 * صفحة اختيار القطعة المنبثقة من الأسفل برسم توضيحي ومربع بحث ونمط بطاقات الفلترة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartSelectionBottomSheet(
    parts: List<Part>,
    selectedPartId: Long?,
    onPartSelected: (Long?) -> Unit,
    onAddNewPartClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var tempSelectedPartId by remember { mutableStateOf(selectedPartId) }

    val filteredParts = remember(parts, searchQuery) {
        if (searchQuery.isBlank()) {
            parts
        } else {
            val q = searchQuery.trim().lowercase()
            parts.filter {
                it.name.lowercase().contains(q) ||
                it.ipn.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
                // Header Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEEF2FF))
                                .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "اختيار القطعة المخزنية",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.5.sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "حدد قطعة واحدة فقط من القائمة لاستلامها في المخزون",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = {
                                onDismiss()
                                onAddNewPartClick()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEEF2FF),
                            border = BorderStroke(1.dp, Color(0xFFE0E7FF))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "قطعة جديدة",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color(0xFF4F46E5)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Body Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search Input Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "ابحث عن اسم القطعة، الرقم IPN، أو الوصف...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                                color = Color(0xFF94A3B8)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "مسح",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    // List Header with Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "القطع المسجلة المتاحة",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                            Surface(
                                color = Color(0xFFEEF2FF),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${filteredParts.size} قطعة",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp
                                    ),
                                    color = Color(0xFF4338CA),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "اختيار قطعة واحدة فقط",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Parts List
                    if (filteredParts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "لم يتم العثور على قطع مطابقة للبحث",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(filteredParts, key = { index, part -> "select-part-${part.id}-$index" }) { _, part ->
                                val isSelected = tempSelectedPartId == part.id
                                val toggleSelection = {
                                    tempSelectedPartId = if (tempSelectedPartId == part.id) null else part.id
                                }
                                Surface(
                                    onClick = toggleSelection,
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) Color(0xFFF4F5FF) else Color(0xFFF8FAFC),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Single Choice Selection Indicator
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = null,
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = Color(0xFF4F46E5),
                                                    unselectedColor = Color(0xFF94A3B8)
                                                )
                                            )

                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = part.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.5.sp
                                                    ),
                                                    color = if (isSelected) Color(0xFF312E81) else Color(0xFF0F172A)
                                                )

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (part.ipn.isNotBlank()) {
                                                        Surface(
                                                            color = Color.White,
                                                            shape = RoundedCornerShape(6.dp),
                                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                                        ) {
                                                            Text(
                                                                text = "IPN: ${part.ipn}",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                ),
                                                                color = Color(0xFF475569),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }

                                                    if (part.units.isNotBlank()) {
                                                        Text(
                                                            text = "الوحدة: ${part.units}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                }

                                                if (part.description.isNotBlank()) {
                                                    Text(
                                                        text = part.description,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                        color = Color(0xFF64748B),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = "إلغاء",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF475569)
                        )
                    }

                    Button(
                        onClick = {
                            onPartSelected(tempSelectedPartId)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "تأكيد الاختيار",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }


/**
 * صفحة اختيار موقع التخزين المنبثقة من الأسفل برسم توضيحي ومربع بحث وزر إضافة موقع جديد.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionBottomSheet(
    locations: List<StockLocation>,
    selectedLocationId: Long?,
    onLocationSelected: (Long?) -> Unit,
    onAddNewLocationClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var tempSelectedLocationId by remember { mutableStateOf(selectedLocationId) }

    val filteredLocations = remember(locations, searchQuery) {
        val sorted = sortLocationsHierarchically(locations)
        if (searchQuery.isBlank()) {
            sorted
        } else {
            val q = searchQuery.trim().lowercase()
            sorted.filter {
                it.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
                // Header Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEEF2FF))
                                .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "اختيار موقع التخزين",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.5.sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "حدد موقع التخزين المباشر لحفظ الوحدة المخزنية",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = {
                                onDismiss()
                                onAddNewLocationClick()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEEF2FF),
                            border = BorderStroke(1.dp, Color(0xFFE0E7FF))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "موقع جديد",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color(0xFF4F46E5)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Body Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search Input Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "ابحث عن اسم الموقع، الوصف، أو الرمز...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                                color = Color(0xFF94A3B8)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "مسح",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    // List Header with Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "المواقف والمستودعات المتاحة",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                            Surface(
                                color = Color(0xFFEEF2FF),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${filteredLocations.size} موقع",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp
                                    ),
                                    color = Color(0xFF4338CA),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "اختر موقعاً واحداً للتخزين",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Locations List
                    if (filteredLocations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "لم يتم العثور على مواقع مطابقة للبحث",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(filteredLocations, key = { index, loc -> "select-loc-${loc.id}-$index" }) { _, loc ->
                                val isSelected = tempSelectedLocationId == loc.id
                                val isEnabled = !loc.structural
                                val indentPadding = (loc.level.coerceIn(0, 5) * 24).dp
                                val fullPath = loc.getFullHierarchyPath(locations)
                                val toggleSelection = {
                                    if (isEnabled) {
                                        tempSelectedLocationId = if (tempSelectedLocationId == loc.id) null else loc.id
                                    }
                                }

                                Surface(
                                    onClick = toggleSelection,
                                    shape = RoundedCornerShape(14.dp),
                                    color = when {
                                        !isEnabled -> Color(0xFFF1F5F9)
                                        isSelected -> Color(0xFFF4F5FF)
                                        else -> Color(0xFFF8FAFC)
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = when {
                                            !isEnabled -> Color(0xFFE2E8F0)
                                            isSelected -> Color(0xFF4F46E5)
                                            else -> Color(0xFFE2E8F0)
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = indentPadding)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = null,
                                                enabled = isEnabled,
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = Color(0xFF4F46E5),
                                                    unselectedColor = Color(0xFF94A3B8)
                                                )
                                            )

                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (loc.level > 0) {
                                                        Text(
                                                            text = "↳",
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 14.sp
                                                            ),
                                                            color = Color(0xFF4F46E5)
                                                        )
                                                    }
                                                    Text(
                                                        text = loc.name,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.5.sp
                                                        ),
                                                        color = when {
                                                            !isEnabled -> Color(0xFF94A3B8)
                                                            isSelected -> Color(0xFF312E81)
                                                            else -> Color(0xFF0F172A)
                                                        },
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    if (loc.structural) {
                                                        Surface(
                                                            color = Color(0xFFFEF3C7),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "🏗️ هيكلي",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                ),
                                                                color = Color(0xFF92400E),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    } else if (loc.external) {
                                                        Surface(
                                                            color = Color(0xFFE0F2FE),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "🌐 خارجي",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                ),
                                                                color = Color(0xFF0369A1),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                if (fullPath.isNotBlank() && fullPath != loc.name) {
                                                    Text(
                                                        text = "📍 $fullPath",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                                        color = Color(0xFF4F46E5),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                if (loc.description.isNotBlank()) {
                                                    Text(
                                                        text = loc.description,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                        color = Color(0xFF64748B),
                                                        maxLines = 1
                                                    )
                                                }

                                                if (loc.structural) {
                                                    Text(
                                                        text = "موقع هيكلي تقسيم غير مخصص للتخزين المباشر",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = Color(0xFFB45309)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = "إلغاء",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF475569)
                        )
                    }

                    Button(
                        onClick = {
                            onLocationSelected(tempSelectedLocationId)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "تأكيد الاختيار",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }


