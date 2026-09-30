package com.nextstepai.inventory.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import com.nextstepai.inventory.ui.components.LocationBarcodeScannerBottomSheet
import com.nextstepai.inventory.ui.components.QuickTransferBottomSheet
import com.nextstepai.inventory.util.BarcodePayloadHelper
import com.nextstepai.inventory.util.BarcodeEntityType
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.calculateOccupancyPercentage
import com.nextstepai.inventory.data.capacityUnit
import com.nextstepai.inventory.data.contactPerson
import com.nextstepai.inventory.data.contactPhone
import com.nextstepai.inventory.data.currentLabelSnapshot
import com.nextstepai.inventory.data.effectiveCapacity
import com.nextstepai.inventory.data.formatQuantity
import com.nextstepai.inventory.data.isLabelStale
import com.nextstepai.inventory.data.labelGeneratedAt
import com.nextstepai.inventory.data.withCapacityUnit
import com.nextstepai.inventory.data.withContactInfo
import com.nextstepai.inventory.data.isPrimary
import com.nextstepai.inventory.data.withPrimary
import com.nextstepai.inventory.data.CompletenessTone
import com.nextstepai.inventory.data.calculateCompleteness
import com.nextstepai.inventory.ui.components.PrintableLabelBottomSheet

enum class OccupancyFilter {
    ALL, HIGH, OCCUPIED, EMPTY
}

enum class StructureFilter {
    ALL, STRUCTURAL_ONLY, EXTERNAL_ONLY
}

enum class LabelStatusFilter {
    ALL, NEEDS_UPDATE, UPDATED
}

/**
 * الأنواع المعيارية الستة المعتمدة رسمياً في هيكلية قواعد بيانات تطبيق إدارة المخزون والمستودعات.
 */
private val CANONICAL_LOCATION_TYPES = listOf(
    "SITE",
    "WAREHOUSE",
    "ZONE",
    "AISLE",
    "SHELF",
    "BIN"
)

/**
 * توحيد ومعايرة رمز نوع الموقع التخزيني لدمج المسميات القديمة والمكافئة (مثل AREA و LINE تحت ZONE، و RACK تحت SHELF).
 */
private fun normalizeLocationType(typeCode: String): String {
    return when (typeCode.uppercase()) {
        "AREA", "LINE" -> "ZONE"
        "RACK" -> "SHELF"
        else -> typeCode.uppercase()
    }
}

/**
 * دالة مساعدة لتعريب رموز أنواع المواقع التخزينية وتفادي انكسار النص في الواجهة.
 */
private fun getArabicLocationType(typeCode: String): String {
    return when (normalizeLocationType(typeCode)) {
        "SITE" -> "منشأة / موقع"
        "WAREHOUSE" -> "مستودع"
        "ZONE" -> "منطقة"
        "AISLE" -> "ممر"
        "SHELF" -> "رف"
        "BIN" -> "صندوق / حاوية"
        else -> typeCode.uppercase()
    }
}

private fun getLocationTypeIcon(typeCode: String): String {
    return when (normalizeLocationType(typeCode)) {
        "SITE" -> "📍"
        "WAREHOUSE" -> "🏢"
        "ZONE" -> "🧩"
        "AISLE" -> "🚪"
        "SHELF" -> "📐"
        "BIN" -> "📥"
        else -> "📍"
    }
}

private fun getLocationTypeIconVector(typeCode: String, customIconKey: String = ""): ImageVector {
    val normalizedType = normalizeLocationType(typeCode)
    val normalizedIcon = customIconKey.lowercase().trim()

    return when {
        normalizedType == "SITE" -> Icons.Default.Place
        normalizedType == "WAREHOUSE" -> Icons.Default.Warehouse
        normalizedType == "ZONE" -> Icons.Default.GridView
        normalizedType == "AISLE" -> Icons.Default.ViewWeek
        normalizedType == "SHELF" -> Icons.Default.TableRows
        normalizedType == "BIN" -> Icons.Default.Inbox

        normalizedIcon.contains("warehouse") || normalizedIcon.contains("building") || normalizedIcon.contains("store") -> Icons.Default.Warehouse
        normalizedIcon.contains("shelf") || normalizedIcon.contains("table") || normalizedIcon.contains("rows") || normalizedIcon.contains("shelves") -> Icons.Default.TableRows
        normalizedIcon.contains("bin") || normalizedIcon.contains("box") || normalizedIcon.contains("inbox") || normalizedIcon.contains("archive") -> Icons.Default.Inbox
        normalizedIcon.contains("aisle") || normalizedIcon.contains("door") || normalizedIcon.contains("week") -> Icons.Default.ViewWeek
        normalizedIcon.contains("zone") || normalizedIcon.contains("grid") || normalizedIcon.contains("area") -> Icons.Default.GridView
        normalizedIcon.contains("site") || normalizedIcon.contains("place") || normalizedIcon.contains("location") -> Icons.Default.Place

        else -> Icons.Default.Warehouse
    }
}

/**
 * شاشة إدارة مواقع التخزين والأرفف الموحدة (LocationManagementScreen).
 * تتميز بترويسة ممركزة مطابقة لباقي التطبيق، وشريط بحث يضع مربع البحث على اليمين وزر الفلتر على اليسار،
 * مع دعم دقة التجميع الهجينة للـ Hybrid Keys (ID + UUID) وتعدد وحدات قياس السعة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationManagementScreen(
    viewModel: StockViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypesFilter by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedParentIdsFilter by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedStructuresFilter by remember { mutableStateOf<Set<StructureFilter>>(emptySet()) }
    var selectedOccupanciesFilter by remember { mutableStateOf<Set<OccupancyFilter>>(emptySet()) }
    var selectedLabelStatusesFilter by remember { mutableStateOf<Set<LabelStatusFilter>>(emptySet()) }

    var isFilterBottomSheetOpen by remember { mutableStateOf(false) }
    var isBarcodeScannerOpen by remember { mutableStateOf(false) }

    var selectedLocationForItems by remember { mutableStateOf<StockLocation?>(null) }
    var selectedLocationForPrint by remember { mutableStateOf<StockLocation?>(null) }
    var selectedLocationForEdit by remember { mutableStateOf<StockLocation?>(null) }
    var selectedLocationForDelete by remember { mutableStateOf<StockLocation?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var pendingEnableLocationId by remember { mutableStateOf<Long?>(null) }
    var highlightedLocationId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(highlightedLocationId) {
        if (highlightedLocationId != null) {
            delay(2500)
            highlightedLocationId = null
        }
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    // تصفية المواقع المتقدمة مع التجميع الهجين للـ ID و UUID
    val filteredLocations = remember(
        uiState.locations,
        uiState.allStockItems,
        searchQuery,
        selectedTypesFilter,
        selectedParentIdsFilter,
        selectedStructuresFilter,
        selectedOccupanciesFilter,
        selectedLabelStatusesFilter
    ) {
        val parsedPayload = BarcodePayloadHelper.parsePayload(searchQuery)
        uiState.locations.filter { loc ->
            val matchesSearch = searchQuery.isBlank() ||
                    loc.name.contains(searchQuery, ignoreCase = true) ||
                    loc.description.contains(searchQuery, ignoreCase = true) ||
                    loc.locationType.contains(searchQuery, ignoreCase = true) ||
                    loc.uuid.contains(searchQuery, ignoreCase = true) ||
                    loc.effectiveUuid.contains(searchQuery, ignoreCase = true) ||
                    "location-${loc.id}".contains(searchQuery, ignoreCase = true) ||
                    (parsedPayload.entityType == BarcodeEntityType.LOCATION && (
                        loc.uuid.equals(parsedPayload.uuid, ignoreCase = true) ||
                        loc.effectiveUuid.equals(parsedPayload.uuid, ignoreCase = true) ||
                        loc.id.toString() == parsedPayload.uuid.removePrefix("location-").removePrefix("loc-")
                    ))

            val matchesType = selectedTypesFilter.isEmpty() ||
                    selectedTypesFilter.any { filterType ->
                        filterType.equals(normalizeLocationType(loc.locationType), ignoreCase = true)
                    }

            val matchesParent = selectedParentIdsFilter.isEmpty() ||
                    (loc.parentId != null && selectedParentIdsFilter.contains(loc.parentId))

            val matchesStructure = selectedStructuresFilter.isEmpty() ||
                    selectedStructuresFilter.any { filter ->
                        when (filter) {
                            StructureFilter.ALL -> true
                            StructureFilter.STRUCTURAL_ONLY -> loc.structural
                            StructureFilter.EXTERNAL_ONLY -> loc.external
                        }
                    }

            val locItems = uiState.allStockItems.filter {
                it.locationId != null && (it.locationId == loc.id || (loc.uuid.startsWith("location-") && it.locationId == loc.uuid.removePrefix("location-").toLongOrNull()))
            }
            val currentQty = locItems.sumOf { it.quantity }
            val occPct = loc.calculateOccupancyPercentage(currentQty)

            val matchesOccupancy = selectedOccupanciesFilter.isEmpty() ||
                    selectedOccupanciesFilter.any { filter ->
                        when (filter) {
                            OccupancyFilter.ALL -> true
                            OccupancyFilter.HIGH -> occPct >= 90.0
                            OccupancyFilter.OCCUPIED -> currentQty > 0.0
                            OccupancyFilter.EMPTY -> currentQty == 0.0
                        }
                    }

            val matchesLabelStatus = selectedLabelStatusesFilter.isEmpty() ||
                    selectedLabelStatusesFilter.any { filter ->
                        when (filter) {
                            LabelStatusFilter.ALL -> true
                            LabelStatusFilter.NEEDS_UPDATE -> loc.labelGeneratedAt == null || loc.isLabelStale
                            LabelStatusFilter.UPDATED -> loc.labelGeneratedAt != null && !loc.isLabelStale
                        }
                    }

            matchesSearch && matchesType && matchesParent && matchesStructure && matchesOccupancy && matchesLabelStatus
        }
    }

    val onDisabledSwitchClick: (StockLocation) -> Unit = { loc ->
        val entityTypeName = if (loc.locationType.equals("SITE", ignoreCase = true) || loc.external) "الموقع" else "المستودع"
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = "هذا هو $entityTypeName الوحيد المسجل بالنظام وهو رئيسي أساسي تلقائياً",
                duration = SnackbarDuration.Short
            )
        }
    }

    val onTogglePrimary: (StockLocation, Boolean) -> Unit = { loc, newChecked ->
        val isExternal = loc.external
        val isSite = loc.locationType.equals("SITE", ignoreCase = true) && !loc.external
        val isWarehouse = loc.locationType.equals("WAREHOUSE", ignoreCase = true) && !loc.external

        if (newChecked) {
            val existingPrimary = uiState.locations.find { existing ->
                existing.id != loc.id && existing.isPrimary &&
                        when {
                            isExternal -> existing.external
                            isSite -> existing.locationType.equals("SITE", ignoreCase = true) && !existing.external
                            isWarehouse -> existing.locationType.equals("WAREHOUSE", ignoreCase = true) && !existing.external
                            else -> false
                        }
            }

            if (existingPrimary != null) {
                val indexA = filteredLocations.indexOfFirst { it.id == existingPrimary.id }
                if (indexA != -1) {
                    coroutineScope.launch {
                        listState.animateScrollToItem(indexA)
                    }
                }
                highlightedLocationId = existingPrimary.id
                pendingEnableLocationId = loc.id
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "يجب إلغاء تعيين الموقع الأساسي الحالي (${existingPrimary.name}) أولاً",
                        duration = SnackbarDuration.Short
                    )
                }
            } else {
                viewModel.updateLocation(loc.withPrimary(true))
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "تم تعيين '${loc.name}' كـ موقع أساسي بنجاح",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        } else {
            viewModel.updateLocation(loc.withPrimary(false))

            if (pendingEnableLocationId != null) {
                val pendingTarget = uiState.locations.find { it.id == pendingEnableLocationId }
                if (pendingTarget != null) {
                    val indexB = filteredLocations.indexOfFirst { it.id == pendingTarget.id }
                    if (indexB != -1) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(indexB)
                        }
                    }
                    highlightedLocationId = pendingTarget.id
                    viewModel.updateLocation(pendingTarget.withPrimary(true))
                    pendingEnableLocationId = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "تم تعيين '${pendingTarget.name}' كـ موقع أساسي تلقائياً",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "تم إلغاء تعيين الموقع الأساسي بنجاح",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    val availableTypes = remember(uiState.locations) {
        val presentNormalizedTypes = uiState.locations.map { loc ->
            normalizeLocationType(loc.locationType)
        }.distinct()

        val customTypes = presentNormalizedTypes.filter { !CANONICAL_LOCATION_TYPES.contains(it) }.sorted()
        (CANONICAL_LOCATION_TYPES + customTypes).distinct()
    }

    val isFilterActive = selectedTypesFilter.isNotEmpty() ||
            selectedParentIdsFilter.isNotEmpty() ||
            selectedStructuresFilter.isNotEmpty() ||
            selectedOccupanciesFilter.isNotEmpty() ||
            selectedLabelStatusesFilter.isNotEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LocationTopBar(onBackClick = onBackClick)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            Spacer(modifier = Modifier.height(12.dp))

            // شريط البحث والفلتر: البحث على اليمين والفلتر على اليسار (في RTL)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. مربع البحث (يظهر أولاً على اليمين في نمط RTL)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "البحث باسم الموقع، الوصف، أو الرمز...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "مسح البحث",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                // 2. زر مسح الباركود المنفصل الموازي لزر الفلتر
                var isBarcodePressed by remember { mutableStateOf(false) }
                val barcodeScale by animateFloatAsState(
                    targetValue = if (isBarcodePressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )

                OutlinedButton(
                    onClick = {
                        isBarcodePressed = true
                        isBarcodeScannerOpen = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF4F46E5)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFEEF2FF),
                        contentColor = Color(0xFF4F46E5)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .fillMaxHeight()
                        .scale(barcodeScale)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "مسح الباركود",
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "باركود",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = Color(0xFF4338CA)
                        )
                    }
                }

                // 2. زر الفلتر (يظهر ثانياً على اليسار في نمط RTL)
                var isFilterPressed by remember { mutableStateOf(false) }
                val buttonScale by animateFloatAsState(
                    targetValue = if (isFilterPressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )

                OutlinedButton(
                    onClick = { isFilterBottomSheetOpen = true },
                    modifier = Modifier
                        .height(48.dp)
                        .scale(buttonScale),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isFilterActive) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isFilterActive) Color(0xFFEEF2FF) else Color.White,
                        contentColor = if (isFilterActive) Color(0xFF4F46E5) else Color(0xFF334155)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "فلتر",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "فلتر",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredLocations.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "لا توجد مواقع تخزينية مطابقة للبحث أو الفلتر",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "يمكنك تغيير خيارات البحث أو إضافة موقع جديد من الزر العائم لأسفل الشاشة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyLocationsList(
                    locations = filteredLocations,
                    allLocations = uiState.locations,
                    allStockItems = uiState.allStockItems,
                    listState = listState,
                    highlightedLocationId = highlightedLocationId,
                    onTogglePrimary = onTogglePrimary,
                    onDisabledSwitchClick = onDisabledSwitchClick,
                    onShowItems = { selectedLocationForItems = it },
                    onPrintLabel = { selectedLocationForPrint = it },
                    onEditLocation = { selectedLocationForEdit = it },
                    onDeleteLocation = { selectedLocationForDelete = it }
                )
            }
        }

            ExtendedFloatingActionButton(
                onClick = { viewModel.setAddLocationDialogOpen(true) },
                containerColor = Color(0xFF4F46E5),
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddLocation,
                    contentDescription = "إضافة موقع",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "إضافة موقع",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                )
            }
        }
    }

    // Modal BottomSheet الفلترة المتقدمة للمواقع
    if (isFilterBottomSheetOpen) {
        LocationFilterBottomSheet(
            allLocations = uiState.locations,
            allStockItems = uiState.allStockItems,
            searchQuery = searchQuery,
            availableTypes = availableTypes,
            selectedTypes = selectedTypesFilter,
            selectedParentIds = selectedParentIdsFilter,
            selectedStructures = selectedStructuresFilter,
            selectedOccupancies = selectedOccupanciesFilter,
            selectedLabelStatuses = selectedLabelStatusesFilter,
            onApplyFilter = { types, parentIds, structs, occs, labelStatuses ->
                selectedTypesFilter = types
                selectedParentIdsFilter = parentIds
                selectedStructuresFilter = structs
                selectedOccupanciesFilter = occs
                selectedLabelStatusesFilter = labelStatuses
                isFilterBottomSheetOpen = false
            },
            onResetFilter = {
                selectedTypesFilter = emptySet()
                selectedParentIdsFilter = emptySet()
                selectedStructuresFilter = emptySet()
                selectedOccupanciesFilter = emptySet()
                selectedLabelStatusesFilter = emptySet()
                isFilterBottomSheetOpen = false
            },
            onDismiss = { isFilterBottomSheetOpen = false }
        )
    }

    // Modal BottomSheet إضافة موقع جديد
    if (uiState.isAddLocationDialogOpen) {
        AddStockLocationBottomSheet(
            locations = uiState.locations,
            users = uiState.users,
            locationTypes = uiState.locationTypes,
            onDismiss = { viewModel.setAddLocationDialogOpen(false) },
            onAddCapacityUnit = { unitCode -> viewModel.addCustomUnit(unitCode) },
            onDeleteCapacityUnit = { unitCode -> viewModel.deleteUnit(unitCode) },
            onConfirm = { name, desc, parentId, structural, external, locationType, icon, ownerId, customIcon, address, customCapacity, capacityUnit, contactPerson, contactPhone, isPrimary, intermediates, generatedNames ->
                viewModel.addLocation(
                    name = name,
                    description = desc,
                    parentId = parentId,
                    structural = structural,
                    external = external,
                    locationType = locationType,
                    icon = icon,
                    ownerId = ownerId,
                    customIcon = customIcon,
                    address = address,
                    customCapacity = customCapacity,
                    capacityUnit = capacityUnit,
                    contactPerson = contactPerson,
                    contactPhone = contactPhone,
                    isPrimary = isPrimary,
                    intermediates = intermediates,
                    generatedNames = generatedNames
                )
            }
        )
    }

    // Modal BottomSheet تعديل موقع قائم عند الضغط على زر التعديل (يعيد استخدام نفس الحوار الموحد)
    selectedLocationForEdit?.let { loc ->
        AddStockLocationBottomSheet(
            locations = uiState.locations,
            users = uiState.users,
            locationTypes = uiState.locationTypes,
            initialLocation = loc,
            onDismiss = { selectedLocationForEdit = null },
            onAddCapacityUnit = { unitCode -> viewModel.addCustomUnit(unitCode) },
            onDeleteCapacityUnit = { unitCode -> viewModel.deleteUnit(unitCode) },
            onConfirm = { name, desc, parentId, structural, external, locationType, icon, ownerId, customIcon, address, customCapacity, capacityUnit, contactPerson, contactPhone, isPrimary, _, _ ->
                val updatedLoc = loc.copy(
                    name = name,
                    description = desc,
                    parentId = parentId,
                    structural = structural,
                    external = external,
                    locationType = locationType,
                    icon = icon,
                    ownerId = ownerId,
                    customIcon = customIcon,
                    address = address,
                    customCapacity = customCapacity
                )
                    .withCapacityUnit(capacityUnit)
                    .withContactInfo(contactPerson, contactPhone)
                    .withPrimary(isPrimary)
                viewModel.updateLocation(updatedLoc)
                selectedLocationForEdit = null
            },
            onConfirmBulk = { _, _, _, _, _, _, _, _ -> }
        )
    }

    if (isBarcodeScannerOpen) {
        LocationBarcodeScannerBottomSheet(
            locations = uiState.locations,
            onDismiss = { isBarcodeScannerOpen = false },
            onBarcodeScanned = { scannedResult ->
                isBarcodeScannerOpen = false
                val parsed = BarcodePayloadHelper.parsePayload(scannedResult)
                val matchedLoc = uiState.locations.find { loc ->
                    loc.uuid.equals(parsed.uuid, ignoreCase = true) ||
                    loc.effectiveUuid.equals(parsed.uuid, ignoreCase = true) ||
                    "location-${loc.id}".equals(parsed.uuid, ignoreCase = true) ||
                    loc.id.toString() == parsed.uuid.removePrefix("location-").removePrefix("loc-") ||
                    loc.name.equals(scannedResult.trim(), ignoreCase = true)
                }
                if (matchedLoc != null) {
                    searchQuery = matchedLoc.name
                } else {
                    searchQuery = scannedResult
                }
            }
        )
    }

    // Modal BottomSheet طباعة ملصق الـ QR
    selectedLocationForPrint?.let { loc ->
        val parentPath = getBreadcrumbPath(loc.parentId, uiState.locations)
        PrintableLabelBottomSheet(
            location = loc,
            parentPath = parentPath,
            onDismiss = { selectedLocationForPrint = null },
            onRegenerateLabel = {
                val currentSnapshot = loc.currentLabelSnapshot
                val updated = viewModel.saveLocationLabelSnapshot(loc.id, currentSnapshot)
                if (updated != null) {
                    selectedLocationForPrint = updated
                }
            }
        )
    }

    // BottomSheet عرض العناصر والقطع المخزنة داخل هذا الموقع
    selectedLocationForItems?.let { loc ->
        LocationItemsBottomSheet(
            location = loc,
            allStockItems = uiState.allStockItems,
            parts = uiState.parts,
            allLocations = uiState.locations,
            onConfirmTransfer = { itemId, sourceLocationId, targetLocationId, quantity, reason, notes ->
                viewModel.transferStockItem(itemId, sourceLocationId, targetLocationId, quantity, reason, notes)
            },
            onDismiss = { selectedLocationForItems = null }
        )
    }

    // حوار تأكيد الحذف
    selectedLocationForDelete?.let { loc ->
        AlertDialog(
            onDismissRequest = { selectedLocationForDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("حذف الموقع التخزيني")
            },
            text = {
                Text("هل أنت تأكد من رغبتك في حذف الموقع '${loc.name}'؟ سيتم التأكد من خلوه تماماً قبل الحذف.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = loc.id
                        selectedLocationForDelete = null
                        viewModel.deleteLocation(targetId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedLocationForDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * الترويسة العلوية لشاشة إدارة أماكن وهيكل التخزين بنفس تصميم وتنسيق باقي التطبيق (الممركز والمحاذى).
 */
@Composable
private fun LocationTopBar(
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(25.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "رجوع",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.scale(if (isRtl) -1f else 1f, 1f)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "أماكن وهيكل التخزين",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "إدارة المستودعات، الأرفف، ضبط السعات، وطباعة ملصقات الـ QR",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun LazyLocationsList(
    locations: List<StockLocation>,
    allLocations: List<StockLocation>,
    allStockItems: List<StockItem>,
    listState: LazyListState,
    highlightedLocationId: Long?,
    onTogglePrimary: (StockLocation, Boolean) -> Unit,
    onDisabledSwitchClick: (StockLocation) -> Unit,
    onShowItems: (StockLocation) -> Unit,
    onPrintLabel: (StockLocation) -> Unit,
    onEditLocation: (StockLocation) -> Unit,
    onDeleteLocation: (StockLocation) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(locations, key = { index, loc -> "loc-${loc.id}-$index" }) { _, loc ->
            LocationCardItem(
                location = loc,
                allLocations = allLocations,
                allStockItems = allStockItems,
                highlightedLocationId = highlightedLocationId,
                onTogglePrimary = { newChecked -> onTogglePrimary(loc, newChecked) },
                onDisabledSwitchClick = { onDisabledSwitchClick(loc) },
                onShowItems = { onShowItems(loc) },
                onPrintLabel = { onPrintLabel(loc) },
                onEditLocation = { onEditLocation(loc) },
                onDeleteLocation = { onDeleteLocation(loc) }
            )
        }
    }
}

@Composable
private fun LocationCardItem(
    location: StockLocation,
    allLocations: List<StockLocation>,
    allStockItems: List<StockItem>,
    highlightedLocationId: Long?,
    onTogglePrimary: (Boolean) -> Unit,
    onDisabledSwitchClick: () -> Unit,
    onShowItems: () -> Unit,
    onPrintLabel: () -> Unit,
    onEditLocation: () -> Unit,
    onDeleteLocation: () -> Unit
) {
    val isExternalLocation = location.external
    val isSite = location.locationType.equals("SITE", ignoreCase = true) && !location.external
    val isWarehouse = location.locationType.equals("WAREHOUSE", ignoreCase = true) && !location.external
    val isPrimaryApplicable = isSite || isWarehouse || isExternalLocation

    val totalCategoryCount = remember(allLocations, isSite, isWarehouse, isExternalLocation) {
        allLocations.count { loc ->
            when {
                isExternalLocation -> loc.external
                isSite -> loc.locationType.equals("SITE", ignoreCase = true) && !loc.external
                isWarehouse -> loc.locationType.equals("WAREHOUSE", ignoreCase = true) && !loc.external
                else -> false
            }
        }
    }
    val isSwitchEnabled = totalCategoryCount > 1
    val isHighlighted = highlightedLocationId == location.id

    // التجميـع الهجين للـ ID و UUID لمنع فقدان البيانات
    val locStockItems = remember(allStockItems, location.id, location.uuid) {
        allStockItems.filter {
            it.locationId != null && (it.locationId == location.id || (location.uuid.startsWith("location-") && it.locationId == location.uuid.removePrefix("location-").toLongOrNull()))
        }
    }

    // حساب الأصناف الفرعية المختلفة المودعة بالرف
    val distinctPartsCount = remember(locStockItems) {
        locStockItems.map { it.partId }.distinct().size
    }

    val currentQty = remember(locStockItems) { locStockItems.sumOf { it.quantity } }
    val occupancyPct = location.calculateOccupancyPercentage(currentQty)
    val effectiveCap = location.effectiveCapacity
    val completeness = location.calculateCompleteness()
    val parentPath = remember(allLocations, location.parentId) {
        getBreadcrumbPath(location.parentId, allLocations)
    }

    // استخراج وإبراز رمز الرف/الموقع إن وُجد (مثل A-01 أو P-10)
    val (codeBadge, cleanName) = remember(location.name) {
        val parts = location.name.split("-", limit = 2)
        if (parts.size == 2 && parts[0].trim().length <= 6 && parts[0].trim().all { it.isLetterOrDigit() }) {
            Pair(parts[0].trim(), parts[1].trim())
        } else {
            Pair(null, location.name)
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isHighlighted) Modifier.border(2.5.dp, Color(0xFFD97706), RoundedCornerShape(18.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isHighlighted) 8.dp else 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isHighlighted) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val typeIconEmoji = getLocationTypeIcon(location.locationType)

            // Header: Icon, Name, Code Badge, Translated Type Tag, Primary Badge & Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEF2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = typeIconEmoji,
                        fontSize = 22.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (codeBadge != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEEF2FF),
                                border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                            ) {
                                Text(
                                    text = codeBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = Color(0xFF3730A3),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = cleanName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = getArabicLocationType(location.locationType),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = Color(0xFF475569),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (parentPath.isNotEmpty()) {
                        Text(
                            text = "📌 $parentPath",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "📍 موقع رئيسي (Root)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1
                        )
                    }
                }

                // Circular Completion Progress Badge (مؤشر اكتمال البيانات الدائري المزين بشريط التقدم)
                val completeness = location.calculateCompleteness()
                CircularCompletionBadge(
                    percentage = completeness.percentage,
                    colorTone = completeness.colorTone
                )
            }

            if (location.description.isNotBlank()) {
                Text(
                    text = location.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            if (location.external) {
                val uriHandler = LocalUriHandler.current
                val clipboardManager = LocalClipboardManager.current

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🚚", fontSize = 12.sp)
                            Text(
                                text = "موقع تخزيني خارجي",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = Color(0xFF1E40AF)
                            )
                        }

                        if (location.address.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(location.address.trim()))
                                        try {
                                            val encoded = location.address.trim().replace(" ", "+")
                                            uriHandler.openUri("https://maps.google.com/?q=$encoded")
                                        } catch (_: Exception) {}
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📍 العنوان: ${location.address}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF1E3A8A),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.Directions,
                                    contentDescription = "فتح في الخريطة",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        val person = location.contactPerson
                        val phone = location.contactPhone
                        if (!person.isNullOrBlank() || !phone.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val contactStr = listOfNotNull(
                                    person?.let { "👤 المسؤول: $it" },
                                    phone?.let { "📞 $it" }
                                ).joinToString("  |  ")
                                Text(
                                    text = contactStr,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF1E3A8A),
                                    modifier = Modifier.weight(1f)
                                )
                                if (!phone.isNullOrBlank()) {
                                    IconButton(
                                        onClick = {
                                            val cleanPhone = phone.trim().filter { it.isDigit() || it == '+' }
                                            clipboardManager.setText(AnnotatedString(cleanPhone))
                                            try {
                                                uriHandler.openUri("tel:$cleanPhone")
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "اتصال مباشر",
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Occupancy Indicator Progress Bar (يُعرض حصراً عند اكتمال بيانات الموقع 100%)
            if (completeness.percentage == 100) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نسبة الإشغال والسعة:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${occupancyPct.toInt()}% (${currentQty.formatQuantity()} / ${effectiveCap.formatQuantity()} ${location.capacityUnit})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (occupancyPct >= 90.0) Color(0xFFDC2626) else Color(0xFF4F46E5)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (occupancyPct / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (occupancyPct >= 90.0) Color(0xFFDC2626) else Color(0xFF4F46E5),
                        trackColor = Color(0xFFE2E8F0),
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Footer Actions (متناسقة وبارتفاع موحد 40.dp بكافة العناصر)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onShowItems,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEEF2FF),
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Text(
                            text = "📦 $distinctPartsCount أصناف مخزنة",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = Color(0xFF3730A3)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPrimaryApplicable) {
                        Surface(
                            onClick = {
                                if (isSwitchEnabled) {
                                    onTogglePrimary(!location.isPrimary)
                                } else {
                                    onDisabledSwitchClick()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (location.isPrimary) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (location.isPrimary) Color(0xFFFDE68A) else Color(0xFFE2E8F0)),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                if (location.isPrimary) {
                                    Text(
                                        text = "أساسي ⭐️",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = Color(0xFFD97706)
                                    )
                                }
                                Switch(
                                    checked = location.isPrimary,
                                    enabled = true,
                                    onCheckedChange = { newChecked ->
                                        if (isSwitchEnabled) {
                                            onTogglePrimary(newChecked)
                                        } else {
                                            onDisabledSwitchClick()
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFD97706),
                                        uncheckedThumbColor = Color(0xFF94A3B8),
                                        uncheckedTrackColor = Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.scale(0.75f)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onPrintLabel,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "طباعة ملصق",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onEditLocation,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onDeleteLocation,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "حذف",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class FilterSubSheetType {
    TYPES, PARENT_LOCATION, STRUCTURE, OCCUPANCY, LABEL_STATUS
}

@Composable
private fun FilterFieldCard(
    title: String,
    selectedValueText: String,
    icon: ImageVector,
    activeCount: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(
            width = 1.dp,
            color = if (activeCount > 0) Color(0xFF818CF8) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (activeCount > 0) Color(0xFFEEF2FF) else Color(0xFFF1F5F9),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (activeCount > 0) Color(0xFF4F46E5) else Color(0xFF64748B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = selectedValueText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (activeCount > 0) Color(0xFF4338CA) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF4F46E5)
                    ) {
                        Text(
                            text = "$activeCount محدد",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> MultiSelectFilterSheet(
    title: String,
    items: List<T>,
    selectedItems: Set<T>,
    getItemTitle: (T) -> String,
    getItemIcon: (T) -> String = { "" },
    getItemCount: (T) -> Int,
    totalCount: Int,
    onConfirm: (Set<T>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var localSelected by remember { mutableStateOf(selectedItems) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // خيار "الكل"
                val isAllSelected = localSelected.isEmpty()
                Surface(
                    onClick = { localSelected = emptySet() },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAllSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isAllSelected) Color(0xFF818CF8) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isAllSelected,
                                onCheckedChange = { localSelected = emptySet() },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                            )
                            Text(
                                text = "الكل",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isAllSelected) Color(0xFF312E81) else Color(0xFF334155)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (isAllSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)
                        ) {
                            Text(
                                text = "$totalCount",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isAllSelected) Color(0xFF312E81) else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // بقية الخيارات المقترنة بالعناصر
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(items, key = { index, _ -> "filter-item-$index" }) { _, item ->
                        val isSelected = localSelected.contains(item)
                        val count = getItemCount(item)
                        val icon = getItemIcon(item)
                        val itemTitle = getItemTitle(item)

                        Surface(
                            onClick = {
                                localSelected = if (isSelected) {
                                    localSelected - item
                                } else {
                                    localSelected + item
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF818CF8) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            localSelected = if (checked) {
                                                localSelected + item
                                            } else {
                                                localSelected - item
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                    )
                                    val labelText = if (icon.isNotBlank()) "$icon $itemTitle" else itemTitle
                                    Text(
                                        text = labelText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) Color(0xFF312E81) else Color(0xFF334155)
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)
                                ) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color(0xFF312E81) else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onConfirm(localSelected) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
            ) {
                Text(
                    text = "تأكيد الاختيار",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationFilterBottomSheet(
    allLocations: List<StockLocation>,
    allStockItems: List<StockItem>,
    searchQuery: String = "",
    availableTypes: List<String>,
    selectedTypes: Set<String>,
    selectedParentIds: Set<Long>,
    selectedStructures: Set<StructureFilter>,
    selectedOccupancies: Set<OccupancyFilter>,
    selectedLabelStatuses: Set<LabelStatusFilter>,
    onApplyFilter: (Set<String>, Set<Long>, Set<StructureFilter>, Set<OccupancyFilter>, Set<LabelStatusFilter>) -> Unit,
    onResetFilter: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var tempTypes by remember { mutableStateOf(selectedTypes) }
    var tempParentIds by remember { mutableStateOf(selectedParentIds) }
    var tempStructures by remember { mutableStateOf(selectedStructures) }
    var tempOccupancies by remember { mutableStateOf(selectedOccupancies) }
    var tempLabelStatuses by remember { mutableStateOf(selectedLabelStatuses) }

    var activeSubSheet by remember { mutableStateOf<FilterSubSheetType?>(null) }

    val totalCount = allLocations.size

    val typeCounts = remember(allLocations, availableTypes) {
        availableTypes.associateWith { type ->
            allLocations.count { loc ->
                normalizeLocationType(loc.locationType).equals(type, ignoreCase = true)
            }
        }
    }

    val availableParentLocations = remember(allLocations) {
        val parentIdsWithChildren = allLocations.mapNotNull { it.parentId }.toSet()
        allLocations.filter { loc ->
            loc.parentId == null || loc.structural || parentIdsWithChildren.contains(loc.id)
        }.sortedBy { it.name }
    }

    val structuralCount = remember(allLocations) { allLocations.count { it.structural } }
    val externalCount = remember(allLocations) { allLocations.count { it.external } }

    val occupancyData = remember(allLocations, allStockItems) {
        var high = 0
        var occ = 0
        var empty = 0
        allLocations.forEach { loc ->
            val items = allStockItems.filter {
                it.locationId != null && (it.locationId == loc.id || (loc.uuid.startsWith("location-") && it.locationId == loc.uuid.removePrefix("location-").toLongOrNull()))
            }
            val currentQty = items.sumOf { it.quantity }
            val pct = loc.calculateOccupancyPercentage(currentQty)
            if (pct >= 90.0) high++
            if (currentQty > 0.0) occ++
            if (currentQty == 0.0) empty++
        }
        Triple(high, occ, empty)
    }
    val (highOccupancyCount, occupiedCount, emptyCount) = occupancyData

    val labelStatusData = remember(allLocations) {
        var needsUpdate = 0
        var updated = 0
        allLocations.forEach { loc ->
            if (loc.labelGeneratedAt == null || loc.isLabelStale) {
                needsUpdate++
            } else {
                updated++
            }
        }
        Pair(needsUpdate, updated)
    }
    val (needsUpdateLabelCount, updatedLabelCount) = labelStatusData

    val tempFilteredLocationsCount = remember(
        allLocations,
        allStockItems,
        searchQuery,
        tempTypes,
        tempParentIds,
        tempStructures,
        tempOccupancies,
        tempLabelStatuses
    ) {
        allLocations.count { loc ->
            val matchesSearch = searchQuery.isBlank() ||
                    loc.name.contains(searchQuery, ignoreCase = true) ||
                    loc.description.contains(searchQuery, ignoreCase = true) ||
                    loc.locationType.contains(searchQuery, ignoreCase = true)

            val matchesType = tempTypes.isEmpty() ||
                    tempTypes.any { filterType ->
                        filterType.equals(normalizeLocationType(loc.locationType), ignoreCase = true)
                    }

            val matchesParent = tempParentIds.isEmpty() ||
                    (loc.parentId != null && tempParentIds.contains(loc.parentId))

            val matchesStructure = tempStructures.isEmpty() ||
                    tempStructures.any { filter ->
                        when (filter) {
                            StructureFilter.ALL -> true
                            StructureFilter.STRUCTURAL_ONLY -> loc.structural
                            StructureFilter.EXTERNAL_ONLY -> loc.external
                        }
                    }

            val locItems = allStockItems.filter {
                it.locationId != null && (it.locationId == loc.id || (loc.uuid.startsWith("location-") && it.locationId == loc.uuid.removePrefix("location-").toLongOrNull()))
            }
            val currentQty = locItems.sumOf { it.quantity }
            val occPct = loc.calculateOccupancyPercentage(currentQty)

            val matchesOccupancy = tempOccupancies.isEmpty() ||
                    tempOccupancies.any { filter ->
                        when (filter) {
                            OccupancyFilter.ALL -> true
                            OccupancyFilter.HIGH -> occPct >= 90.0
                            OccupancyFilter.OCCUPIED -> currentQty > 0.0
                            OccupancyFilter.EMPTY -> currentQty == 0.0
                        }
                    }

            val matchesLabelStatus = tempLabelStatuses.isEmpty() ||
                    tempLabelStatuses.any { filter ->
                        when (filter) {
                            LabelStatusFilter.ALL -> true
                            LabelStatusFilter.NEEDS_UPDATE -> loc.labelGeneratedAt == null || loc.isLabelStale
                            LabelStatusFilter.UPDATED -> loc.labelGeneratedAt != null && !loc.isLabelStale
                        }
                    }

            matchesSearch && matchesType && matchesParent && matchesStructure && matchesOccupancy && matchesLabelStatus
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "فلترة أماكن التخزين",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null)
                }
            }

            // 1. حقل "الموقع الأب / المستودع التابع له" العمودي (في الأعلى)
            val parentSummary = remember(tempParentIds, availableParentLocations) {
                if (tempParentIds.isEmpty()) "الكل ($totalCount موقعاً)"
                else {
                    val selectedNames = availableParentLocations
                        .filter { tempParentIds.contains(it.id) }
                        .map { it.name }
                    if (selectedNames.isNotEmpty()) selectedNames.joinToString("، ")
                    else "الكل ($totalCount موقعاً)"
                }
            }
            FilterFieldCard(
                title = "الموقع الأب / المستودع التابع له",
                selectedValueText = parentSummary,
                icon = Icons.Default.AccountTree,
                activeCount = tempParentIds.size,
                onClick = { activeSubSheet = FilterSubSheetType.PARENT_LOCATION }
            )

            // 2. حقل "نوع الموقع" العمودي
            val typesSummary = remember(tempTypes) {
                if (tempTypes.isEmpty()) "الكل ($totalCount موقعاً)"
                else tempTypes.joinToString("، ") { getArabicLocationType(it) }
            }
            FilterFieldCard(
                title = "نوع الموقع",
                selectedValueText = typesSummary,
                icon = Icons.Default.Inventory2,
                activeCount = tempTypes.size,
                onClick = { activeSubSheet = FilterSubSheetType.TYPES }
            )

            // 3. حقل "نوع الهيكلية والملكية" العمودي
            val structureSummary = remember(tempStructures) {
                if (tempStructures.isEmpty()) "الكل ($totalCount موقعاً)"
                else tempStructures.joinToString("، ") { struct ->
                    when (struct) {
                        StructureFilter.ALL -> "الكل"
                        StructureFilter.STRUCTURAL_ONLY -> "مواقع هيكلية فقط"
                        StructureFilter.EXTERNAL_ONLY -> "مواقع خارجية فقط"
                    }
                }
            }
            FilterFieldCard(
                title = "نوع الهيكلية والملكية",
                selectedValueText = structureSummary,
                icon = Icons.Default.FilterList,
                activeCount = tempStructures.size,
                onClick = { activeSubSheet = FilterSubSheetType.STRUCTURE }
            )

            // 4. حقل "حالة الإشغال والسعة" العمودي
            val occupancySummary = remember(tempOccupancies) {
                if (tempOccupancies.isEmpty()) "الكل ($totalCount موقعاً)"
                else tempOccupancies.joinToString("، ") { occ ->
                    when (occ) {
                        OccupancyFilter.ALL -> "الكل"
                        OccupancyFilter.HIGH -> "🔴 ممتلئ (>90%)"
                        OccupancyFilter.OCCUPIED -> "🟢 فيه مواد مخزنة"
                        OccupancyFilter.EMPTY -> "⚪ فارغ"
                    }
                }
            }
            FilterFieldCard(
                title = "حالة الإشغال والسعة",
                selectedValueText = occupancySummary,
                icon = Icons.Default.FilterList,
                activeCount = tempOccupancies.size,
                onClick = { activeSubSheet = FilterSubSheetType.OCCUPANCY }
            )

            // 5. حقل "حالة طباعة الملصق" العمودي
            val labelStatusSummary = remember(tempLabelStatuses) {
                if (tempLabelStatuses.isEmpty()) "الكل ($totalCount موقعاً)"
                else tempLabelStatuses.joinToString("، ") { status ->
                    when (status) {
                        LabelStatusFilter.ALL -> "الكل"
                        LabelStatusFilter.NEEDS_UPDATE -> "⚠️ بحاجة لتحديث الملصق"
                        LabelStatusFilter.UPDATED -> "✅ ملصق محدث ومطبوع"
                    }
                }
            }
            FilterFieldCard(
                title = "حالة طباعة الملصق",
                selectedValueText = labelStatusSummary,
                icon = Icons.Default.QrCode2,
                activeCount = tempLabelStatuses.size,
                onClick = { activeSubSheet = FilterSubSheetType.LABEL_STATUS }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 6. أزرار التحكم السفلية المحسوبة اللحظية
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        tempTypes = emptySet()
                        tempParentIds = emptySet()
                        tempStructures = emptySet()
                        tempOccupancies = emptySet()
                        tempLabelStatuses = emptySet()
                        onResetFilter()
                    },
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "إعادة ضبط",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = {
                        onApplyFilter(tempTypes, tempParentIds, tempStructures, tempOccupancies, tempLabelStatuses)
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text(
                        text = "تطبيق الفلتر (إظهار $tempFilteredLocationsCount مواقع)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // المنبثقات العائمة لتحديد خيارات الفلترة المتقدمة (Sub-sheets for Multi-Selection)
    when (activeSubSheet) {
        FilterSubSheetType.TYPES -> {
            MultiSelectFilterSheet(
                title = "اختر أنواع المواقع التخزينية",
                items = availableTypes,
                selectedItems = tempTypes,
                getItemTitle = { getArabicLocationType(it) },
                getItemIcon = { getLocationTypeIcon(it) },
                getItemCount = { typeCounts[it] ?: 0 },
                totalCount = totalCount,
                onConfirm = {
                    tempTypes = it
                    activeSubSheet = null
                },
                onDismiss = { activeSubSheet = null }
            )
        }

        FilterSubSheetType.PARENT_LOCATION -> {
            val selectedParentLocations = remember(tempParentIds, availableParentLocations) {
                availableParentLocations.filter { tempParentIds.contains(it.id) }.toSet()
            }
            MultiSelectFilterSheet(
                title = "اختر الموقع الأب / المستودع التابع له",
                items = availableParentLocations,
                selectedItems = selectedParentLocations,
                getItemTitle = { parentLoc -> parentLoc.name },
                getItemIcon = { parentLoc -> getLocationTypeIcon(parentLoc.locationType) },
                getItemCount = { parentLoc -> allLocations.count { it.parentId == parentLoc.id } },
                totalCount = totalCount,
                onConfirm = { selectedLocs ->
                    tempParentIds = selectedLocs.map { it.id }.toSet()
                    activeSubSheet = null
                },
                onDismiss = { activeSubSheet = null }
            )
        }

        FilterSubSheetType.STRUCTURE -> {
            MultiSelectFilterSheet(
                title = "اختر نوع الهيكلية والملكية",
                items = listOf(StructureFilter.STRUCTURAL_ONLY, StructureFilter.EXTERNAL_ONLY),
                selectedItems = tempStructures,
                getItemTitle = { struct ->
                    when (struct) {
                        StructureFilter.ALL -> "الكل"
                        StructureFilter.STRUCTURAL_ONLY -> "مواقع هيكلية فقط"
                        StructureFilter.EXTERNAL_ONLY -> "مواقع خارجية فقط"
                    }
                },
                getItemIcon = { struct ->
                    when (struct) {
                        StructureFilter.ALL -> "🏢"
                        StructureFilter.STRUCTURAL_ONLY -> "🏛️"
                        StructureFilter.EXTERNAL_ONLY -> "🚛"
                    }
                },
                getItemCount = { struct ->
                    when (struct) {
                        StructureFilter.ALL -> totalCount
                        StructureFilter.STRUCTURAL_ONLY -> structuralCount
                        StructureFilter.EXTERNAL_ONLY -> externalCount
                    }
                },
                totalCount = totalCount,
                onConfirm = {
                    tempStructures = it
                    activeSubSheet = null
                },
                onDismiss = { activeSubSheet = null }
            )
        }

        FilterSubSheetType.OCCUPANCY -> {
            MultiSelectFilterSheet(
                title = "اختر حالة الإشغال والسعة",
                items = listOf(OccupancyFilter.HIGH, OccupancyFilter.OCCUPIED, OccupancyFilter.EMPTY),
                selectedItems = tempOccupancies,
                getItemTitle = { occ ->
                    when (occ) {
                        OccupancyFilter.ALL -> "الكل"
                        OccupancyFilter.HIGH -> "ممتلئ (>90%)"
                        OccupancyFilter.OCCUPIED -> "فيه مواد مخزنة"
                        OccupancyFilter.EMPTY -> "فارغ"
                    }
                },
                getItemIcon = { occ ->
                    when (occ) {
                        OccupancyFilter.ALL -> "📊"
                        OccupancyFilter.HIGH -> "🔴"
                        OccupancyFilter.OCCUPIED -> "🟢"
                        OccupancyFilter.EMPTY -> "⚪"
                    }
                },
                getItemCount = { occ ->
                    when (occ) {
                        OccupancyFilter.ALL -> totalCount
                        OccupancyFilter.HIGH -> highOccupancyCount
                        OccupancyFilter.OCCUPIED -> occupiedCount
                        OccupancyFilter.EMPTY -> emptyCount
                    }
                },
                totalCount = totalCount,
                onConfirm = {
                    tempOccupancies = it
                    activeSubSheet = null
                },
                onDismiss = { activeSubSheet = null }
            )
        }

        FilterSubSheetType.LABEL_STATUS -> {
            MultiSelectFilterSheet(
                title = "اختر حالة طباعة الملصق",
                items = listOf(LabelStatusFilter.NEEDS_UPDATE, LabelStatusFilter.UPDATED),
                selectedItems = tempLabelStatuses,
                getItemTitle = { status ->
                    when (status) {
                        LabelStatusFilter.ALL -> "الكل"
                        LabelStatusFilter.NEEDS_UPDATE -> "بحاجة لتحديث الملصق"
                        LabelStatusFilter.UPDATED -> "ملصق محدث ومطبوع"
                    }
                },
                getItemIcon = { status ->
                    when (status) {
                        LabelStatusFilter.ALL -> "🏷️"
                        LabelStatusFilter.NEEDS_UPDATE -> "⚠️"
                        LabelStatusFilter.UPDATED -> "✅"
                    }
                },
                getItemCount = { status ->
                    when (status) {
                        LabelStatusFilter.ALL -> totalCount
                        LabelStatusFilter.NEEDS_UPDATE -> needsUpdateLabelCount
                        LabelStatusFilter.UPDATED -> updatedLabelCount
                    }
                },
                totalCount = totalCount,
                onConfirm = {
                    tempLabelStatuses = it
                    activeSubSheet = null
                },
                onDismiss = { activeSubSheet = null }
            )
        }

        null -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationItemsBottomSheet(
    location: StockLocation,
    allStockItems: List<StockItem>,
    parts: List<Part>,
    allLocations: List<StockLocation>,
    onConfirmTransfer: (itemId: Long, sourceLocationId: Long?, targetLocationId: Long, quantity: Double, reason: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var itemSearchQuery by remember { mutableStateOf("") }
    var isItemBarcodeScannerOpen by remember { mutableStateOf(false) }
    var selectedItemForTransfer by remember { mutableStateOf<StockItem?>(null) }

    // الفلترة الهجينة للـ ID و UUID بالنافذة السفلية
    val locItems = remember(allStockItems, location.id, location.uuid) {
        allStockItems.filter {
            it.locationId != null && (it.locationId == location.id || (location.uuid.startsWith("location-") && it.locationId == location.uuid.removePrefix("location-").toLongOrNull()))
        }
    }

    val filteredLocItems = remember(locItems, parts, itemSearchQuery) {
        val query = itemSearchQuery.trim()
        if (query.isBlank()) {
            locItems
        } else {
            val parsedPayload = BarcodePayloadHelper.parsePayload(query)
            locItems.filter { item ->
                val part = parts.find { it.id == item.partId }
                val partName = part?.name ?: ""
                val partIpn = part?.ipn ?: ""

                partName.contains(query, ignoreCase = true) ||
                partIpn.contains(query, ignoreCase = true) ||
                item.serial.contains(query, ignoreCase = true) ||
                item.batch.contains(query, ignoreCase = true) ||
                item.notes.contains(query, ignoreCase = true) ||
                (parsedPayload.entityType == BarcodeEntityType.STOCK_ITEM && (
                    item.id.toString() == parsedPayload.uuid.removePrefix("stock-") ||
                    item.serial.equals(parsedPayload.uuid, ignoreCase = true)
                )) ||
                (parsedPayload.entityType == BarcodeEntityType.PART && (
                    item.partId.toString() == parsedPayload.uuid.removePrefix("part-") ||
                    partIpn.equals(parsedPayload.uuid, ignoreCase = true)
                ))
            }
        }
    }

    if (isItemBarcodeScannerOpen) {
        LocationBarcodeScannerBottomSheet(
            locations = emptyList(),
            onDismiss = { isItemBarcodeScannerOpen = false },
            onBarcodeScanned = { scannedResult ->
                itemSearchQuery = scannedResult
                isItemBarcodeScannerOpen = false
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "السلع والمواد المخزنة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "الموقع: ${location.name} (${filteredLocItems.size}/${locItems.size} سجلات مخزنية)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null)
                }
            }

            if (locItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = itemSearchQuery,
                        onValueChange = { itemSearchQuery = it },
                        placeholder = {
                            Text(
                                text = "البحث باسم القطعة، الرقم التسلسلي، الدفعة...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (itemSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { itemSearchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "مسح البحث",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    var isBarcodePressed by remember { mutableStateOf(false) }
                    val barcodeScale by animateFloatAsState(
                        targetValue = if (isBarcodePressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    )

                    OutlinedButton(
                        onClick = {
                            isBarcodePressed = true
                            isItemBarcodeScannerOpen = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF4F46E5)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFEEF2FF),
                            contentColor = Color(0xFF4F46E5)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .fillMaxHeight()
                            .scale(barcodeScale)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "مسح الباركود",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "باركود",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = Color(0xFF4338CA)
                            )
                        }
                    }
                }
            }

            if (locItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هذا الموقع خالي حالياً ولا يحتوي على أية عناصر مخزنة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else if (filteredLocItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مواد مخزنة مطابقة لبحثك ('$itemSearchQuery').",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredLocItems, key = { index, item -> "loc-item-${item.id}-$index" }) { _, item ->
                        val partName = parts.find { it.id == item.partId }?.name ?: "قطعة #${item.partId}"
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = partName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (item.serial.isNotBlank()) {
                                        Text(
                                            text = "الرقم التسلسلي: ${item.serial}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (item.batch.isNotBlank()) {
                                        Text(
                                            text = "دفعة التشغيل: ${item.batch}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFEEF2FF)
                                    ) {
                                        Text(
                                            text = "الكمية: ${item.quantity.formatQuantity()} ${item.packaging}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF3730A3)
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = { selectedItemForTransfer = item },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFF4F46E5).copy(alpha = 0.12f),
                                            contentColor = Color(0xFF4F46E5)
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("نقل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        selectedItemForTransfer?.let { stockItem ->
            val partName = parts.find { it.id == stockItem.partId }?.name ?: "قطعة #${stockItem.partId}"
            QuickTransferBottomSheet(
                stockItem = stockItem,
                partName = partName,
                currentLocation = location,
                allLocations = allLocations,
                allStockItems = allStockItems,
                onDismiss = { selectedItemForTransfer = null },
                onConfirmTransfer = { targetLocId, qty, reason, notes ->
                    onConfirmTransfer(stockItem.id, location.id, targetLocId, qty, reason, notes)
                    selectedItemForTransfer = null
                }
            )
        }
    }
}

private fun getBreadcrumbPath(parentId: Long?, locations: List<StockLocation>): String {
    if (parentId == null) return ""
    val visited = mutableSetOf<Long>()
    val pathNames = mutableListOf<String>()
    var currId: Long? = parentId

    while (currId != null && !visited.contains(currId)) {
        visited.add(currId)
        val loc = locations.find { it.id == currId } ?: break
        pathNames.add(0, loc.name)
        currId = loc.parentId
    }

    return pathNames.joinToString(" / ")
}

@Composable
private fun CircularCompletionBadge(
    percentage: Int,
    colorTone: CompletenessTone,
    modifier: Modifier = Modifier
) {
    val progress = (percentage / 100f).coerceIn(0f, 1f)
    val strokeWidth = 3.dp

    val strokeColor = when (colorTone) {
        CompletenessTone.RED -> Color(0xFFDC2626)
        CompletenessTone.ORANGE -> Color(0xFFD97706)
        CompletenessTone.GREEN -> Color(0xFF059669)
    }
    val trackColor = when (colorTone) {
        CompletenessTone.RED -> Color(0xFFFEE2E2)
        CompletenessTone.ORANGE -> Color(0xFFFEF3C7)
        CompletenessTone.GREEN -> Color(0xFFD1FAE5)
    }
    val textColor = when (colorTone) {
        CompletenessTone.RED -> Color(0xFF991B1B)
        CompletenessTone.ORANGE -> Color(0xFF92400E)
        CompletenessTone.GREEN -> Color(0xFF065F46)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(38.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // 1. Background Track Arc
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // 2. Clockwise Progress Arc (Starts at -90 degrees / top)
            drawArc(
                color = strokeColor,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "%$percentage",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = textColor
        )
    }
}
