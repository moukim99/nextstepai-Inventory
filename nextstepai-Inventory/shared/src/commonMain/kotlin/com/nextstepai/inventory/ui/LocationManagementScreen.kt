package com.nextstepai.inventory.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.nextstepai.inventory.data.effectiveCapacity
import com.nextstepai.inventory.data.formatQuantity
import com.nextstepai.inventory.data.withCapacityUnit
import com.nextstepai.inventory.ui.components.PrintableLabelBottomSheet

enum class OccupancyFilter {
    ALL, HIGH, OCCUPIED, EMPTY
}

enum class StructureFilter {
    ALL, STRUCTURAL_ONLY, EXTERNAL_ONLY
}

/**
 * دالة مساعدة لتعريب رموز أنواع المواقع التخزينية وتفادي انكسار النص في الواجهة.
 */
private fun getArabicLocationType(typeCode: String): String {
    return when (typeCode.uppercase()) {
        "WAREHOUSE" -> "مستودع"
        "SHELF" -> "رف"
        "LINE" -> "خط إنتاج"
        "BIN" -> "صندوق / حاوية"
        "AISLE" -> "ممر"
        "ZONE" -> "منطقة"
        "SITE" -> "منشأة"
        "PALLET" -> "منصة (Pallet)"
        "RACK" -> "رف رئيسي"
        else -> typeCode.uppercase()
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
    var selectedTypeFilter by remember { mutableStateOf<String?>(null) }
    var selectedStructureFilter by remember { mutableStateOf(StructureFilter.ALL) }
    var selectedOccupancyFilter by remember { mutableStateOf(OccupancyFilter.ALL) }

    var isFilterBottomSheetOpen by remember { mutableStateOf(false) }

    var selectedLocationForItems by remember { mutableStateOf<StockLocation?>(null) }
    var selectedLocationForPrint by remember { mutableStateOf<StockLocation?>(null) }
    var selectedLocationForEdit by remember { mutableStateOf<StockLocation?>(null) }
    var selectedLocationForDelete by remember { mutableStateOf<StockLocation?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

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
        selectedTypeFilter,
        selectedStructureFilter,
        selectedOccupancyFilter
    ) {
        uiState.locations.filter { loc ->
            val matchesSearch = searchQuery.isBlank() ||
                    loc.name.contains(searchQuery, ignoreCase = true) ||
                    loc.description.contains(searchQuery, ignoreCase = true) ||
                    loc.locationType.contains(searchQuery, ignoreCase = true)

            val matchesType = selectedTypeFilter == null ||
                    loc.locationType.equals(selectedTypeFilter, ignoreCase = true)

            val matchesStructure = when (selectedStructureFilter) {
                StructureFilter.ALL -> true
                StructureFilter.STRUCTURAL_ONLY -> loc.structural
                StructureFilter.EXTERNAL_ONLY -> loc.external
            }

            val locItems = uiState.allStockItems.filter {
                it.locationId != null && (it.locationId == loc.id || (loc.uuid.startsWith("location-") && it.locationId == loc.uuid.removePrefix("location-").toLongOrNull()))
            }
            val currentQty = locItems.sumOf { it.quantity }
            val occPct = loc.calculateOccupancyPercentage(currentQty)

            val matchesOccupancy = when (selectedOccupancyFilter) {
                OccupancyFilter.ALL -> true
                OccupancyFilter.HIGH -> occPct >= 90.0
                OccupancyFilter.OCCUPIED -> currentQty > 0.0
                OccupancyFilter.EMPTY -> currentQty == 0.0
            }

            matchesSearch && matchesType && matchesStructure && matchesOccupancy
        }
    }

    val availableTypes = remember(uiState.locations) {
        uiState.locations.map { it.locationType.uppercase() }.distinct().sorted()
    }

    val isFilterActive = selectedTypeFilter != null ||
            selectedStructureFilter != StructureFilter.ALL ||
            selectedOccupancyFilter != OccupancyFilter.ALL

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LocationTopBar(onBackClick = onBackClick)
        },
        floatingActionButton = {
            Box(
                modifier = Modifier.padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.setAddLocationDialogOpen(true) },
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
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
        },
        floatingActionButtonPosition = FabPosition.Start
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
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
                            text = "البحث باسم الموقع، الوصف، أو النوع...",
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
                    onShowItems = { selectedLocationForItems = it },
                    onPrintLabel = { selectedLocationForPrint = it },
                    onEditLocation = { selectedLocationForEdit = it },
                    onDeleteLocation = { selectedLocationForDelete = it }
                )
            }
        }
    }

    // Modal BottomSheet الفلترة المتقدمة للمواقع
    if (isFilterBottomSheetOpen) {
        LocationFilterBottomSheet(
            availableTypes = availableTypes,
            selectedType = selectedTypeFilter,
            selectedStructure = selectedStructureFilter,
            selectedOccupancy = selectedOccupancyFilter,
            onApplyFilter = { type, struct, occ ->
                selectedTypeFilter = type
                selectedStructureFilter = struct
                selectedOccupancyFilter = occ
                isFilterBottomSheetOpen = false
            },
            onResetFilter = {
                selectedTypeFilter = null
                selectedStructureFilter = StructureFilter.ALL
                selectedOccupancyFilter = OccupancyFilter.ALL
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
            onConfirm = { name, desc, parentId, structural, external, locationType, icon, ownerId, customIcon, address, customCapacity, capacityUnit ->
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
                    capacityUnit = capacityUnit
                )
            },
            onConfirmBulk = { parentId, locationType, prefix, startNumber, endNumber, padZeros, desc, customCapacity ->
                viewModel.generateBulkLocations(
                    parentId = parentId,
                    locationType = locationType,
                    prefix = prefix,
                    startNumber = startNumber,
                    endNumber = endNumber,
                    padZeros = padZeros,
                    customCapacity = customCapacity,
                    description = desc
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
            onConfirm = { name, desc, parentId, structural, external, locationType, icon, ownerId, customIcon, address, customCapacity, capacityUnit ->
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
                ).withCapacityUnit(capacityUnit)
                viewModel.updateLocation(updatedLoc)
                selectedLocationForEdit = null
            },
            onConfirmBulk = { _, _, _, _, _, _, _, _ -> }
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
                val currentSnapshot = "${loc.name}|${loc.parentId}|${loc.effectiveCapacity.formatQuantity()}|${loc.locationType}|${loc.capacityUnit}"
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
    onShowItems: (StockLocation) -> Unit,
    onPrintLabel: (StockLocation) -> Unit,
    onEditLocation: (StockLocation) -> Unit,
    onDeleteLocation: (StockLocation) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(locations, key = { it.id }) { loc ->
            LocationCardItem(
                location = loc,
                allLocations = allLocations,
                allStockItems = allStockItems,
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
    onShowItems: () -> Unit,
    onPrintLabel: () -> Unit,
    onEditLocation: () -> Unit,
    onDeleteLocation: () -> Unit
) {
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Icon, Name, Code Badge, Translated Type Tag
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
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(24.dp)
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
            }

            if (location.description.isNotBlank()) {
                Text(
                    text = location.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            // Occupancy Indicator Progress Bar مع وحدة القياس وتنسيق الكميات
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
                    trackColor = Color(0xFFE2E8F0)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Footer Actions مع إبراز الأصناف الفرعية المختلفة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onShowItems,
                    label = { Text("📦 $distinctPartsCount أصناف مخزنة") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFFEEF2FF),
                        labelColor = Color(0xFF3730A3)
                    ),
                    border = null
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrintLabel,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "طباعة ملصق",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onEditLocation,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeleteLocation,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        )
                    ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationFilterBottomSheet(
    availableTypes: List<String>,
    selectedType: String?,
    selectedStructure: StructureFilter,
    selectedOccupancy: OccupancyFilter,
    onApplyFilter: (String?, StructureFilter, OccupancyFilter) -> Unit,
    onResetFilter: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var tempType by remember { mutableStateOf(selectedType) }
    var tempStructure by remember { mutableStateOf(selectedStructure) }
    var tempOccupancy by remember { mutableStateOf(selectedOccupancy) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
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

            // 1. نوع الموقع المعرب
            if (availableTypes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "نوع الموقع:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = tempType == null,
                                onClick = { tempType = null },
                                label = { Text("الكل") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF4F46E5),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        items(availableTypes) { type ->
                            FilterChip(
                                selected = tempType.equals(type, ignoreCase = true),
                                onClick = {
                                    tempType = if (tempType.equals(type, ignoreCase = true)) null else type
                                },
                                label = { Text(getArabicLocationType(type)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF4F46E5),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 2. نوع الهيكل (هيكلي / خارجي)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "نوع الهيكلية والملكية:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tempStructure == StructureFilter.ALL,
                        onClick = { tempStructure = StructureFilter.ALL },
                        label = { Text("الكل") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = tempStructure == StructureFilter.STRUCTURAL_ONLY,
                        onClick = { tempStructure = StructureFilter.STRUCTURAL_ONLY },
                        label = { Text("مواقع هيكلية فقط") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = tempStructure == StructureFilter.EXTERNAL_ONLY,
                        onClick = { tempStructure = StructureFilter.EXTERNAL_ONLY },
                        label = { Text("مواقع خارجية فقط") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // 3. حالة الإشغال والسعة
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "حالة الإشغال والسعة:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = tempOccupancy == OccupancyFilter.ALL,
                        onClick = { tempOccupancy = OccupancyFilter.ALL },
                        label = { Text("الكل") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = tempOccupancy == OccupancyFilter.HIGH,
                        onClick = { tempOccupancy = OccupancyFilter.HIGH },
                        label = { Text("ممتلئ (>90%)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = tempOccupancy == OccupancyFilter.OCCUPIED,
                        onClick = { tempOccupancy = OccupancyFilter.OCCUPIED },
                        label = { Text("فيه مواد مخزنة") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = tempOccupancy == OccupancyFilter.EMPTY,
                        onClick = { tempOccupancy = OccupancyFilter.EMPTY },
                        label = { Text("فارغ") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onResetFilter,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إعادة ضبط")
                }

                Button(
                    onClick = {
                        onApplyFilter(tempType, tempStructure, tempOccupancy)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("تطبيق الفلتر")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationItemsBottomSheet(
    location: StockLocation,
    allStockItems: List<StockItem>,
    parts: List<Part>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // الفلترة الهجينة للـ ID و UUID بالنافذة السفلية
    val locItems = remember(allStockItems, location.id, location.uuid) {
        allStockItems.filter {
            it.locationId != null && (it.locationId == location.id || (location.uuid.startsWith("location-") && it.locationId == location.uuid.removePrefix("location-").toLongOrNull()))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        text = "الموقع: ${location.name} (${locItems.size} سجلات مخزنية)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null)
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
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(locItems) { item ->
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
                            }
                        }
                    }
                }
            }
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
