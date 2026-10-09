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

/**
 * شاشة إدارة المخزون الفعلي ومواقع التخزين وأنواعها وسجلات التتبع وفحوص الجودة والمرفقات (StockScreen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    viewModel: StockViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isBarcodeScannerOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            StockTopBar(
                onBackClick = onBackClick,
                onAddStockClick = { viewModel.setAddDialogOpen(true) },
                onAddLocationClick = { viewModel.setAddLocationDialogOpen(true) }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // شريط البحث المطور مع دمج الفلتر الخارجي الجانبي المتناسق
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "البحث باسم القطعة، الرقم التسلسلي، أو الموقع...",
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
                        modifier = Modifier.weight(1f).fillMaxHeight(),
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

                    // زر مسح الباركود المنفصل الموازي لزر الفلتر
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

                    val isFilterActive = uiState.selectedLocationId != null || uiState.selectedLocationIds.isNotEmpty() || searchQuery.isNotBlank()
                    var isFilterPressed by remember { mutableStateOf(false) }
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isFilterPressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    )
                    val buttonBgColor by animateColorAsState(
                        targetValue = if (isFilterActive) Color(0xFFEEF2FF) else Color.White,
                        animationSpec = tween(durationMillis = 250)
                    )
                    val buttonBorderColor by animateColorAsState(
                        targetValue = if (isFilterActive) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant,
                        animationSpec = tween(durationMillis = 250)
                    )

                    LaunchedEffect(isFilterPressed) {
                        if (isFilterPressed) {
                            delay(150)
                            isFilterPressed = false
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            isFilterPressed = true
                            viewModel.setFilterBottomSheetOpen(true)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, buttonBorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = buttonBgColor,
                            contentColor = if (isFilterActive) Color(0xFF4F46E5) else MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .fillMaxHeight()
                            .scale(buttonScale)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "فلتر",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (uiState.selectedLocationIds.isNotEmpty()) "فلتر (${uiState.selectedLocationIds.size})" else "فلتر",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = if (isFilterActive) Color(0xFF4338CA) else MaterialTheme.colorScheme.onSurface
                            )
                            if (isFilterActive) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = "⚠️ ${uiState.errorMessage}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp
                        )
                    }
                }

                val filteredStock = uiState.stockItems.filter { item ->
                    val part = uiState.parts.find { it.id == item.partId }
                    searchQuery.isBlank() || (part?.name?.contains(searchQuery, ignoreCase = true) == true) || item.serial.contains(searchQuery, ignoreCase = true) || item.batch.contains(searchQuery, ignoreCase = true)
                }

                if (filteredStock.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pluralStringResource(Res.plurals.stock_items_count, 0, 0),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pluralStringResource(Res.plurals.stock_items_count, filteredStock.size, filteredStock.size),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(filteredStock, key = { index, item -> "stock-${item.id}-$index" }) { _, item ->
                            val part = uiState.parts.find { it.id == item.partId }
                            val loc = uiState.locations.find { it.id == item.locationId }
                            StockItemCard(
                                stockItem = item,
                                part = part,
                                location = loc,
                                onSplitClick = { viewModel.setSelectedItemForSplit(item) },
                                onStocktakeClick = { viewModel.performStocktake(item.id) },
                                onHistoryClick = { viewModel.openTrackingHistory(item) },
                                onTestsClick = { viewModel.openTestResults(item) },
                                onAttachmentsClick = { viewModel.openAttachments(item) }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 12.dp, start = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(
                    onClick = { viewModel.setAddLocationDialogOpen(true) },
                    containerColor = Color(0xFF059669),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLocation,
                            contentDescription = "إضافة موقع تخزين جديد",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "موقع جديد",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                ExtendedFloatingActionButton(
                    onClick = { viewModel.setAddDialogOpen(true) },
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.add_new_stock),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إضافة وحدة",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    )
                }
            }
        }
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
                    viewModel.filterByLocation(matchedLoc.id)
                    searchQuery = matchedLoc.name
                } else {
                    searchQuery = scannedResult
                }
            }
        )
    }

    if (uiState.isFilterBottomSheetOpen) {
        StockFilterBottomSheet(
            uiState = uiState,
            onDismiss = { viewModel.setFilterBottomSheetOpen(false) },
            onApply = { selectedLocationIds ->
                viewModel.applyLocationFilters(selectedLocationIds)
            },
            onClear = { viewModel.clearLocationFilters() }
        )
    }

    if (uiState.isAddStockDialogOpen) {
        AddStockItemBottomSheet(
            parts = uiState.parts,
            locations = uiState.locations,
            categories = uiState.categories,
            purchaseOrders = uiState.purchaseOrders,
            suppliers = uiState.suppliers,
            supplierParts = uiState.supplierParts,
            users = uiState.users,
            locationTypes = uiState.locationTypes,
            onDismiss = { viewModel.setAddDialogOpen(false) },
            onAddNewPart = { name, ipn, desc, catId, units, assembly, component, isTemplate, variantOf, minStock, maxStock, revision, keywords, trackable, purchaseable, salable, virtual, defaultLocId, defaultExpiryDays, pendingAttachments, active, locked ->
                viewModel.addNewPart(
                    name = name,
                    ipn = ipn,
                    description = desc,
                    categoryId = catId,
                    units = units,
                    assembly = assembly,
                    component = component,
                    isTemplate = isTemplate,
                    variantOfId = variantOf,
                    minimumStock = minStock,
                    maximumStock = maxStock,
                    revision = revision,
                    keywords = keywords,
                    trackable = trackable,
                    purchaseable = purchaseable,
                    salable = salable,
                    virtual = virtual,
                    defaultLocationId = defaultLocId,
                    defaultExpiryDays = defaultExpiryDays,
                    pendingAttachments = pendingAttachments,
                    active = active,
                    locked = locked
                )
            },
            onAddNewLocation = { name, desc, parentId, structural, external, locationType, icon, ownerId, customIcon, address ->
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
                    address = address
                )
            },
            onAddNewLocationBulk = { parentId, locationType, prefix, startNum, endNum, padZeros, desc ->
                viewModel.generateBulkLocations(
                    parentId = parentId,
                    locationType = locationType,
                    prefix = prefix,
                    startNumber = startNum,
                    endNumber = endNum,
                    padZeros = padZeros,
                    description = desc
                )
            },
            onConfirm = { partId, locId, qty, serial, batch, pack, status, price, currency, supplierPartId, purchaseOrderId, expiry, review, deleteOnDeplete, link, notes, unitWeight, totalWeight ->
                viewModel.addStockItem(
                    partId = partId,
                    locationId = locId,
                    quantity = qty,
                    serial = serial,
                    batch = batch,
                    packaging = pack,
                    status = status,
                    purchasePrice = price,
                    purchasePriceCurrency = currency,
                    supplierPartId = supplierPartId,
                    purchaseOrderId = purchaseOrderId,
                    expiryDate = expiry,
                    reviewNeeded = review,
                    deleteOnDeplete = deleteOnDeplete,
                    link = link,
                    notes = notes,
                    unitWeight = unitWeight,
                    totalWeight = totalWeight
                )
            }
        )
    }

    if (uiState.isAddLocationDialogOpen) {
        AddStockLocationBottomSheet(
            locations = uiState.locations,
            users = uiState.users,
            locationTypes = uiState.locationTypes,
            onDismiss = { viewModel.setAddLocationDialogOpen(false) },
            onQuickAddParent = { parentName, parentType, grandparentId ->
                viewModel.quickCreateParentLocation(
                    name = parentName,
                    locationType = parentType,
                    parentId = grandparentId
                )
            },
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

    if (uiState.selectedItemForHistory != null) {
        StockTrackingHistoryBottomSheet(
            item = uiState.selectedItemForHistory!!,
            logs = uiState.trackingLogsForSelected,
            onDismiss = { viewModel.closeTrackingHistory() }
        )
    }

    if (uiState.selectedItemForTests != null) {
        StockTestResultsBottomSheet(
            item = uiState.selectedItemForTests!!,
            results = uiState.testResultsForSelected,
            onDismiss = { viewModel.closeTestResults() },
            onAddTestClick = { viewModel.setAddTestResultDialogOpen(true) }
        )
    }

    if (uiState.isAddTestResultDialogOpen && uiState.selectedItemForTests != null) {
        AddStockTestResultDialog(
            item = uiState.selectedItemForTests!!,
            onDismiss = { viewModel.setAddTestResultDialogOpen(false) },
            onConfirm = { test, result, value, attach, notes ->
                viewModel.addTestResult(
                    stockItemId = uiState.selectedItemForTests!!.id,
                    test = test,
                    result = result,
                    value = value,
                    attachment = attach,
                    notes = notes
                )
            }
        )
    }

    if (uiState.selectedItemForAttachments != null) {
        StockAttachmentsBottomSheet(
            item = uiState.selectedItemForAttachments!!,
            attachments = uiState.attachmentsForSelected,
            onDismiss = { viewModel.closeAttachments() },
            onAddAttachmentClick = { viewModel.setAddAttachmentDialogOpen(true) },
            onDeleteAttachment = { attId -> viewModel.deleteStockItemAttachment(attId, uiState.selectedItemForAttachments!!.id) }
        )
    }

    if (uiState.isAddAttachmentDialogOpen && uiState.selectedItemForAttachments != null) {
        AddStockItemAttachmentDialog(
            item = uiState.selectedItemForAttachments!!,
            onDismiss = { viewModel.setAddAttachmentDialogOpen(false) },
            onConfirm = { attachPath, link, comment ->
                viewModel.addStockItemAttachment(
                    stockItemId = uiState.selectedItemForAttachments!!.id,
                    attachmentPath = attachPath,
                    link = link,
                    comment = comment
                )
            }
        )
    }

    if (uiState.selectedItemForSplit != null) {
        SplitStockDialog(
            item = uiState.selectedItemForSplit!!,
            onDismiss = { viewModel.setSelectedItemForSplit(null) },
            onConfirm = { qty -> viewModel.splitStockItem(uiState.selectedItemForSplit!!.id, qty) }
        )
    }
}


/**
 * الترويسة العلوية لشاشة إدارة المخزون والمواقع
 */
@Composable
internal fun StockTopBar(
    onBackClick: () -> Unit,
    onAddStockClick: () -> Unit,
    onAddLocationClick: () -> Unit
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
                    text = "المخزون والمواقع (Stock)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "إدارة الوحدات المادية، مواقع التخزين، وفحوصات الجودة",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onAddLocationClick) {
                    Icon(
                        imageVector = Icons.Default.AddLocation,
                        contentDescription = "إضافة موقع تخزين جديد",
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = onAddStockClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة وحدة مخزنية جديدة",
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}


