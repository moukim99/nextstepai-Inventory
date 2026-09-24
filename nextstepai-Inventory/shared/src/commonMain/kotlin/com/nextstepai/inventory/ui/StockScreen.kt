package com.nextstepai.inventory.ui

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
import com.nextstepai.inventory.data.effectiveCapacity
import com.nextstepai.inventory.data.calculateOccupancyPercentage
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

    Scaffold(
        topBar = {
            StockTopBar(
                onBackClick = onBackClick,
                onAddStockClick = { viewModel.setAddDialogOpen(true) },
                onAddLocationClick = { viewModel.setAddLocationDialogOpen(true) }
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier.padding(bottom = 20.dp, start = 12.dp, end = 12.dp)
            ) {
                // زر إضافة وحدة
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
        },
        floatingActionButtonPosition = FabPosition.Start,
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
                                text = "البحث باسم القطعة، الرقم التسلسلي، أو موقع التخزين...",
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
                        contentPadding = PaddingValues(bottom = 80.dp)
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
        }
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
            onAddNewPart = { name, ipn, desc, catId, units, assembly, component, isTemplate, variantOf, minStock, maxStock, revision, keywords, trackable, purchaseable, salable, virtual, defaultLocId, defaultExpiryDays, link, imageUrl, active, locked ->
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
                    link = link,
                    imageUrl = imageUrl,
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
            onConfirm = { partId, locId, qty, serial, batch, pack, status, price, currency, supplierPartId, purchaseOrderId, expiry, review, deleteOnDeplete, link, notes ->
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
                    notes = notes
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
            onConfirmBulk = { parentId, locationType, prefix, startNum, endNum, padZeros, desc, customCapacity ->
                viewModel.generateBulkLocations(
                    parentId = parentId,
                    locationType = locationType,
                    prefix = prefix,
                    startNumber = startNum,
                    endNumber = endNum,
                    padZeros = padZeros,
                    customCapacity = customCapacity,
                    description = desc
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
private fun StockTopBar(
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

            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

/**
 * بطاقة عرض الوحدة المخزنية المادية مع كامل أزرار الفحوص والجودة والمرفقات
 */
@Composable
private fun StockItemCard(
    stockItem: StockItem,
    part: Part?,
    location: StockLocation?,
    onSplitClick: () -> Unit,
    onStocktakeClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onTestsClick: () -> Unit,
    onAttachmentsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Badges (Left in RTL) & Icon Box (Right in RTL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (stockItem.reviewNeeded) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Text(
                                text = "⚠️ إعادة مراجعة",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    val (statusBg, statusBorder, statusColor) = when (stockItem.status) {
                        StockStatus.OK -> Triple(Color(0xFFECFDF5), Color(0xFFA7F3D0), Color(0xFF047857))
                        StockStatus.QUARANTINE -> Triple(Color(0xFFFEF3C7), Color(0xFFFDE68A), Color(0xFFD97706))
                        else -> Triple(Color(0xFFFEF2F2), Color(0xFFFCA5A5), Color(0xFFDC2626))
                    }

                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, statusBorder)
                    ) {
                        Text(
                            text = stockItem.status.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            ),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Location Icon Box
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEF2FF))
                        .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Part Name
            Text(
                text = part?.name ?: "قطعة #${stockItem.partId}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity & Location Surface
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "الكمية المادية المتوفرة:",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "${stockItem.quantity} ${part?.units ?: "pcs"}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF4F46E5)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "موقع التخزين:",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "📍 ${location?.name ?: "غير محدد"}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }

            // Batch, Packaging & Serial Info
            if (stockItem.serial.isNotBlank() || stockItem.batch.isNotBlank() || stockItem.packaging.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (stockItem.packaging.isNotBlank()) {
                        Text(
                            text = "التغليف: ${stockItem.packaging}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                    if (stockItem.batch.isNotBlank()) {
                        Text(
                            text = "Batch: ${stockItem.batch}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF334155)
                        )
                    }
                    if (stockItem.serial.isNotBlank()) {
                        Text(
                            text = "SN: ${stockItem.serial}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFFF1F5F9)
            )

            // Action Buttons Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onSplitClick,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                        Text("تجزئة ✂️", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = Color(0xFF334155))
                    }
                }

                Surface(
                    onClick = onStocktakeClick,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEEF2FF),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                        Text("جرد 📌", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = Color(0xFF4F46E5))
                    }
                }

                Surface(
                    onClick = onHistoryClick,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                        Text("السجل 🕒", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = Color(0xFF334155))
                    }
                }

                Surface(
                    onClick = onTestsClick,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                        Text("الجودة 🔬", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = Color(0xFF334155))
                    }
                }

                Surface(
                    onClick = onAttachmentsClick,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                        Text("مرفقات 📎", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = Color(0xFF334155))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockAttachmentsBottomSheet(
    item: StockItem,
    attachments: List<StockItemAttachment>,
    onDismiss: () -> Unit,
    onAddAttachmentClick: () -> Unit,
    onDeleteAttachment: (id: Long) -> Unit
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
                .fillMaxHeight(0.85f)
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
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "المستندات والمرفقات",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "الوحدة المخزنية #${item.id} (SN: ${item.serial.ifBlank { "غير معرّف" }})",
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onAddAttachmentClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("رفع / إضافة مرفق جديد", fontWeight = FontWeight.Bold)
                    }
                }

                if (attachments.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد مستندات أو شهادات مرفقة لهذه القطعة حتى الآن", color = Color(0xFF64748B))
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        itemsIndexed(attachments, key = { index, att -> "att-${att.id}-$index" }) { _, att ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(att.comment, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        val source = att.attachment ?: att.link ?: "-"
                                        Text("المصدر: $source", fontSize = 11.sp, color = Color(0xFF4F46E5))
                                        Text("التاريخ: ${att.uploadDate}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    }
                                    IconButton(onClick = { onDeleteAttachment(att.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStockItemAttachmentDialog(
    item: StockItem,
    onDismiss: () -> Unit,
    onConfirm: (
        attachmentPath: String?,
        link: String?,
        comment: String
    ) -> Unit
) {
    var attachmentPath by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("إضافة مرفق/مستند جديد", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = attachmentPath,
                    onValueChange = { attachmentPath = it },
                    label = { Text("مسار الملف المرفوع محلياً (File Path)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("أو أدخل رابط ويب خارجي للوثيقة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("الرابط الإلكتروني الخارجي (URL Link)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("وصف/تعليق الوثيقة (مثل: شهادة منشأ، فاتورة)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val path = attachmentPath.trim().ifBlank { null }
                        val url = link.trim().ifBlank { null }
                        if (path != null || url != null) {
                            onConfirm(path, url, comment.trim())
                        }
                    },
                    enabled = attachmentPath.isNotBlank() || link.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(Res.string.save), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockTestResultsBottomSheet(
    item: StockItem,
    results: List<StockItemTestResult>,
    onDismiss: () -> Unit,
    onAddTestClick: () -> Unit
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
                .fillMaxHeight(0.85f)
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
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "نتائج فحوص الجودة والقياسات",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "الوحدة المخزنية #${item.id} (SN: ${item.serial.ifBlank { "غير معرّف" }})",
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onAddTestClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("إضافة فحص فني جديد", fontWeight = FontWeight.Bold)
                    }
                }

                if (results.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد فحوصات فنية مسجلة لهذه القطعة حتى الآن", color = Color(0xFF64748B))
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        itemsIndexed(results, key = { index, res -> "test-${res.id}-$index" }) { _, res ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (res.result) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, if (res.result) Color(0xFFA7F3D0) else Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(res.test, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        Surface(
                                            color = if (res.result) Color(0xFF047857) else Color(0xFFDC2626),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (res.result) "✅ ناجح (Pass)" else "❌ راسب (Fail)",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("القيمة المقاسة: ${res.value}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                    if (res.notes.isNotBlank()) {
                                        Text("ملاحظات: ${res.notes}", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Text("التاريخ: ${res.date}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStockTestResultDialog(
    item: StockItem,
    onDismiss: () -> Unit,
    onConfirm: (
        test: String,
        result: Boolean,
        value: String,
        attachment: String,
        notes: String
    ) -> Unit
) {
    var testName by remember { mutableStateOf("فحص الجهد والأبعاد الفنية") }
    var measuredValue by remember { mutableStateOf("5.00 V - OK") }
    var isPass by remember { mutableStateOf(true) }
    var attachment by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تسجيل نتيجة فحص جودة (QC Test)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = testName,
                    onValueChange = { testName = it },
                    label = { Text("اسم الاختبار الفني (مثل: فحص العزل الكهربائي)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = measuredValue,
                    onValueChange = { measuredValue = it },
                    label = { Text("القيمة المقاسة الفعلية (Value)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("النتيجة الإجمالية للفحص:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    FilterChip(
                        selected = isPass,
                        onClick = { isPass = true },
                        label = { Text("✅ ناجح (Pass)") },
                        shape = RoundedCornerShape(50)
                    )
                    FilterChip(
                        selected = !isPass,
                        onClick = { isPass = false },
                        label = { Text("❌ راسب (Fail)") },
                        shape = RoundedCornerShape(50)
                    )
                }

                OutlinedTextField(
                    value = attachment,
                    onValueChange = { attachment = it },
                    label = { Text("مسار تقرير أو ملف التوثيق المرفق (PDF/Image)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات المفتش الفني") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (testName.isNotBlank()) {
                            onConfirm(testName.trim(), isPass, measuredValue.trim(), attachment.trim(), notes.trim())
                        }
                    },
                    enabled = testName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(Res.string.save), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockTrackingHistoryBottomSheet(
    item: StockItem,
    logs: List<StockItemTracking>,
    onDismiss: () -> Unit
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
                .fillMaxHeight(0.85f)
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
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "سجل التتبع والحركات",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "الوحدة المخزنية #${item.id} (SN: ${item.serial.ifBlank { "غير معرّف" }})",
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا يوجد سجل حركات تاريخية لهذه القطعة", color = Color(0xFF64748B))
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        itemsIndexed(logs, key = { index, log -> "track-${log.id}-$index" }) { _, log ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(log.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF4F46E5))
                                        Text(log.date, fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                                    }
                                    if (log.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("ملاحظات: ${log.notes}", fontSize = 11.sp, color = Color(0xFF334155))
                                    }
                                    if (log.deltas != "{}") {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("التغيرات (Deltas): ${log.deltas}", fontSize = 10.5.sp, color = Color(0xFF0284C7))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class VisualIconOption(
    val key: String,
    val nameAr: String,
    val iconVector: ImageVector
)

private val availableLocationIcons = listOf(
    VisualIconOption("warehouse", "مستودع / مخزن", Icons.Default.Warehouse),
    VisualIconOption("shelves", "رفوف / أرفف", Icons.Default.TableRows),
    VisualIconOption("inventory_2", "صندوق / حاوية", Icons.Default.Inventory2),
    VisualIconOption("grid_view", "منطقة / قسم", Icons.Default.GridView),
    VisualIconOption("door", "ممر / مدخل", Icons.Default.ViewWeek),
    VisualIconOption("archive", "أرشيف / تخزين", Icons.Default.Archive),
    VisualIconOption("store", "متجر / معرض", Icons.Default.Store),
    VisualIconOption("local_shipping", "شحنة / ترانزيت", Icons.Default.LocalShipping),
    VisualIconOption("place", "موقع / نقطة", Icons.Default.Place),
    VisualIconOption("home_work", "مقَر / فرع", Icons.Default.HomeWork),
    VisualIconOption("widgets", "وحدة / عُقدة", Icons.Default.Widgets),
    VisualIconOption("corporate_fare", "منشأة / مجمع", Icons.Default.CorporateFare)
)

private fun getDefaultIconForLocationType(type: String): String {
    return when (type.uppercase()) {
        "WAREHOUSE" -> "warehouse"
        "ZONE" -> "grid_view"
        "AISLE" -> "door"
        "SHELF" -> "shelves"
        "BIN" -> "inventory_2"
        else -> "warehouse"
    }
}

private fun getStockLocationIconVector(iconKey: String): ImageVector {
    return when (iconKey.lowercase()) {
        "warehouse", "building", "store" -> Icons.Default.Warehouse
        "grid_view", "zone", "category" -> Icons.Default.GridView
        "door", "aisle", "door_sliding", "view_week" -> Icons.Default.ViewWeek
        "shelves", "straighten", "shelf", "table_rows" -> Icons.Default.TableRows
        "inventory_2", "bin", "box", "archive" -> Icons.Default.Inventory2
        "local_shipping", "transit" -> Icons.Default.LocalShipping
        "home_work" -> Icons.Default.HomeWork
        "place", "location_on" -> Icons.Default.Place
        "widgets" -> Icons.Default.Widgets
        "corporate_fare" -> Icons.Default.CorporateFare
        else -> Icons.Default.Warehouse
    }
}

private fun getArabicIconName(iconKey: String): String {
    return availableLocationIcons.find { it.key.equals(iconKey, ignoreCase = true) }?.nameAr ?: "أيقونة قياسية ($iconKey)"
}

private fun getFullPathForLocation(
    locations: List<StockLocation>,
    locationId: Long?,
    separator: String = " > "
): String {
    if (locationId == null) return "موقع رئيسي (Root)"
    val visited = mutableSetOf<Long>()
    val pathNames = mutableListOf<String>()
    var currId: Long? = locationId

    while (currId != null && !visited.contains(currId)) {
        visited.add(currId)
        val loc = locations.find { it.id == currId } ?: break
        pathNames.add(0, loc.name)
        currId = loc.parentId
    }

    return if (pathNames.isNotEmpty()) pathNames.joinToString(separator) else "موقع #$locationId"
}

private fun buildHierarchyTreeLines(
    locations: List<StockLocation>,
    parentId: Long?,
    newName: String
): List<Pair<Int, String>> {
    val result = mutableListOf<Pair<Int, String>>()
    if (parentId == null) {
        val displayName = if (newName.isNotBlank()) newName.trim() else "[✨ اسم الموقع قيد الإنشاء]"
        result.add(0 to "🏢 $displayName (مركز رئيسي Root)")
        return result
    }

    val ancestors = mutableListOf<StockLocation>()
    val visited = mutableSetOf<Long>()
    var currId: Long? = parentId

    while (currId != null && !visited.contains(currId)) {
        visited.add(currId)
        val loc = locations.find { it.id == currId } ?: break
        ancestors.add(0, loc)
        currId = loc.parentId
    }

    var indent = 0
    ancestors.forEach { loc ->
        val iconEmoji = when (loc.locationType.uppercase()) {
            "WAREHOUSE" -> "🏢"
            "ZONE" -> "🧩"
            "AISLE" -> "🚪"
            "SHELF" -> "📐"
            "BIN" -> "📥"
            else -> if (loc.structural) "🏗️" else "📍"
        }
        result.add(indent to "$iconEmoji ${loc.name}")
        indent++
    }

    val childName = if (newName.isNotBlank()) newName.trim() else "[✨ اسم الموقع قيد الإنشاء]"
    result.add(indent to "✨ $childName")

    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisualIconPickerDialog(
    selectedIconKey: String,
    onDismiss: () -> Unit,
    onSelectIcon: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "اختيار الأيقونة البصرية للموقع",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
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

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                ) {
                    items(availableLocationIcons, key = { "icon-opt-${it.key}" }) { opt ->
                        val isSelected = opt.key.equals(selectedIconKey, ignoreCase = true)
                        Surface(
                            onClick = {
                                onSelectIcon(opt.key)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = opt.iconVector,
                                    contentDescription = opt.nameAr,
                                    tint = if (isSelected) Color.White else Color(0xFF4F46E5),
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = opt.nameAr,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 10.5.sp
                                    ),
                                    color = if (isSelected) Color.White else Color(0xFF334155),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectOwnerBottomSheet(
    users: List<AppUser>,
    selectedOwner: AppUser?,
    onDismiss: () -> Unit,
    onSelectOwner: (AppUser?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, searchQuery) {
        val activeUsers = users.filter { it.active && !it.isDeleted }
        if (searchQuery.isBlank()) {
            activeUsers
        } else {
            activeUsers.filter { user ->
                user.name.contains(searchQuery, ignoreCase = true) ||
                        user.role.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            imageVector = Icons.Default.SupervisorAccount,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "اختيار المشرف المسؤول",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "حدد المستخدم المسؤول عن إدارة هذا الموقع",
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

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن مشرف أو دور وظيفي...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color(0xFF64748B))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "owner-none") {
                    val isSelected = selectedOwner == null
                    Surface(
                        onClick = {
                            onSelectOwner(null)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonOff,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "بدون مشرف محدد",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "تمرير قيمة فارغة (null) لمالك الموقع التخزيني",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "محدد",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                items(filteredUsers, key = { "user-${it.uuid}" }) { user ->
                    val isSelected = selectedOwner?.uuid == user.uuid
                    Surface(
                        onClick = {
                            onSelectOwner(user)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0E7FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF4338CA),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    if (user.role.isNotBlank()) {
                                        Text(
                                            text = user.role,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "محدد",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectCapacityUnitBottomSheet(
    selectedUnit: String,
    onDismiss: () -> Unit,
    onUnitSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempSelectedUnit by remember { mutableStateOf(selectedUnit) }

    val unitOptions = remember {
        listOf(
            Triple("قطعة", "📦 قطعة / وحدة مادية (PCS)", "المعيار الافتراضي لعد القطع والمنتجات الفردية"),
            Triple("كغ", "⚖️ كيلوغرام (Kg)", "معيار الوزن التراكمي للمواد الخام والصب والفلزات"),
            Triple("طن", "🏗️ طن (Ton)", "معيار الوزن الثقيل للشحنات والحمولات الضخمة"),
            Triple("م³", "📐 متر مكعب (m³)", "معيار الحجم والاطراد المكاني للأرضيات والحاويات"),
            Triple("م²", "🏁 متر مربع (m²)", "معيار المساحة المسطحة للحاويات والأرضيات المفتوحة"),
            Triple("صندوق", "📥 صندوق / حاوية (Bin)", "معيار العد بحجم الصناديق والحاويات التخزينية"),
            Triple("طبلية", "🪵 طبلية (Pallet)", "معيار الحمولات المرصوفة على المنصات الخشبية"),
            Triple("بكرة", "🧵 بكرة (Reel)", "معيار بكرات الكوابل والأسلاك والأشرطة")
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "اختيار وحدة قياس السعة التخزينية",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "حدد المعيار الفيزيائي لقياس السعة القصوى للرف أو الوعاء",
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                unitOptions.forEach { (code, title, desc) ->
                    val isSelected = tempSelectedUnit == code
                    Surface(
                        onClick = {
                            tempSelectedUnit = code
                            onUnitSelected(code)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isSelected) Color(0xFF3730A3) else Color(0xFF0F172A)
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationTypeSelectionBottomSheet(
    selectedLocationType: String,
    onDismiss: () -> Unit,
    onLocationTypeSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempSelectedType by remember { mutableStateOf(selectedLocationType) }

    val typeOptions = remember {
        listOf(
            Triple("SITE", "📍 موقع / منشأة جغرافية", "موقع جغرافي أو مجمع لوجستي رئيسي يضم عدة مستودعات"),
            Triple("WAREHOUSE", "🏢 مستودع", "مواقف ومستودعات تخزينية كاملة أو مبانٍ فرعية مستقلة"),
            Triple("ZONE", "🧩 منطقة / قسم", "تقسيم أو زاوية تخزينية داخل المستودع الرئيسي"),
            Triple("AISLE", "🚪 ممر", "ممر تنظيم حركي يربط المصفوفات والأرفف المتوازية"),
            Triple("SHELF", "📐 رف", "رف تخزين فيزيائي مباشر للمواد والقطع"),
            Triple("BIN", "📥 صندوق / درج", "حاوية أو صندوق لحفظ القطع الصغيرة والمعزولة")
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "اختيار تصنيف الموقع التخزيني",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "حدد النمط الهيكلي للموقع لتحديد طبيعة التخزين",
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

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(typeOptions, key = { "type-opt-${it.first}" }) { (code, title, desc) ->
                    val isSelected = tempSelectedType.equals(code, ignoreCase = true)
                    Surface(
                        onClick = { tempSelectedType = code },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { tempSelectedType = code },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF3B82F6))
                                )
                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onLocationTypeSelected(tempSelectedType)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تأكيد الاختيار", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentLocationSelectionBottomSheet(
    locations: List<StockLocation>,
    selectedParentId: Long?,
    onDismiss: () -> Unit,
    onParentSelected: (Long?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var tempSelectedParentId by remember { mutableStateOf(selectedParentId) }

    val filteredLocations = remember(locations, searchQuery) {
        if (searchQuery.isBlank()) {
            locations
        } else {
            val q = searchQuery.trim().lowercase()
            locations.filter {
                it.name.lowercase().contains(q) ||
                        it.description.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "اختيار الموقع الحاوي (الأب)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "حدد موقع التخزين الأب ضمن الشجرة الهرمية MPTT",
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

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث عن اسم الموقع، الوصف، أو الرمز...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color(0xFF64748B))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

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
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEEF2FF)
                    ) {
                        Text(
                            text = "${filteredLocations.size + 1} موقع",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = Color(0xFF4F46E5),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "اختر موقعاً واحداً للتخزين",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color(0xFF94A3B8)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "parent-root-opt") {
                    val isSelected = tempSelectedParentId == null
                    Surface(
                        onClick = { tempSelectedParentId = null },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { tempSelectedParentId = null },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF3B82F6))
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "موقع رئيسي (Root)",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            color = Color(0xFF0F172A)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                text = "جذر الشجرة 🏗️",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                                color = Color(0xFFD97706),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "موقع رئيسي مستقل غير تابع لأي موقع أب حاوي",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }

                itemsIndexed(filteredLocations, key = { index, loc -> "parent-loc-${loc.id}-$index" }) { _, loc ->
                    val isSelected = tempSelectedParentId == loc.id
                    val fullParentPath = getFullPathForLocation(locations, loc.parentId, separator = " > ")
                    Surface(
                        onClick = { tempSelectedParentId = loc.id },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { tempSelectedParentId = loc.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF3B82F6))
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = loc.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            color = Color(0xFF0F172A)
                                        )
                                        if (loc.structural) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFFEF3C7)
                                            ) {
                                                Text(
                                                    text = "🏗️ هيكلي",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                                    color = Color(0xFFD97706),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (fullParentPath.isNotBlank() && fullParentPath != "موقع رئيسي (Root)") {
                                        Text(
                                            text = "📍 $fullParentPath",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                            color = Color(0xFF4F46E5)
                                        )
                                    }
                                    Text(
                                        text = loc.description.ifBlank { if (loc.structural) "موقع هيكلي تقسيم غير مخصص للتخزين المباشر" else "موقع تخزين حاوي" },
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = if (loc.structural) Color(0xFFD97706) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onParentSelected(tempSelectedParentId)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تأكيد الاختيار", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddStockLocationBottomSheet(
    locations: List<StockLocation>,
    users: List<AppUser> = emptyList(),
    locationTypes: List<StockLocationType> = emptyList(),
    initialLocation: StockLocation? = null,
    onDismiss: () -> Unit,
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
        capacityUnit: String
    ) -> Unit,
    onConfirmBulk: (
        parentId: Long?,
        locationType: String,
        prefix: String,
        startNumber: Int,
        endNumber: Int,
        padZeros: Boolean,
        description: String,
        customCapacity: Double?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isEditMode = initialLocation != null
    var isBulkMode by remember { mutableStateOf(false) }
    var showEditConfirmationDialog by remember { mutableStateOf(false) }

    // Single Mode States
    var name by remember { mutableStateOf(initialLocation?.name ?: "") }
    var description by remember { mutableStateOf(initialLocation?.description ?: "") }
    var address by remember { mutableStateOf(initialLocation?.address ?: "") }
    var customCapacityText by remember { mutableStateOf(initialLocation?.customCapacity?.toString() ?: "") }
    var capacityUnit by remember { mutableStateOf(initialLocation?.capacityUnit ?: "قطعة") }
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

    // Bulk Mode States
    var prefix by remember { mutableStateOf("R-") }
    var startNumberText by remember { mutableStateOf("1") }
    var endNumberText by remember { mutableStateOf("20") }
    var padZeros by remember { mutableStateOf(true) }
    var bulkDescription by remember { mutableStateOf("") }
    var bulkCapacityText by remember { mutableStateOf("") }

    // BottomSheet Pickers State
    var isOwnerPickerOpen by remember { mutableStateOf(false) }
    var isLocationTypePickerOpen by remember { mutableStateOf(false) }
    var isParentLocationPickerOpen by remember { mutableStateOf(false) }
    var isCapacityUnitPickerOpen by remember { mutableStateOf(false) }

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
            onParentSelected = { parentId ->
                selectedParentId = parentId
                isParentLocationPickerOpen = false
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
                .fillMaxHeight(0.92f)
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
                            imageVector = if (isBulkMode) Icons.Default.FlashOn else Icons.Default.AddLocation,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isEditMode) "التعديل على موقع تخزين" else if (isBulkMode) "توليد مواقع تخزينية متسلسلة" else "إضافة موقع تخزيني جديد",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isEditMode) "تحديث وتعديل كافة حقول بيانات وسعة هذا الموقع" else if (isBulkMode) "معالج التوليد الدفعي المتسلسل للأرفف والحاويات" else "إدخال موقع تخزين فريد في شجرة المستودع",
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
                // Creation Mode Toggle (فردي vs توليد متسلسل - يُخفى في حالة التعديل)
                if (!isEditMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(4.dp)
                    ) {
                        Surface(
                            onClick = { isBulkMode = false },
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isBulkMode) Color.White else Color.Transparent,
                            shadowElevation = if (!isBulkMode) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddLocation,
                                    contentDescription = null,
                                    tint = if (!isBulkMode) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إضافة موقع فردي",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = if (!isBulkMode) Color(0xFF0F172A) else Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            onClick = { isBulkMode = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isBulkMode) Color(0xFF4F46E5) else Color.Transparent,
                            shadowElevation = if (isBulkMode) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = if (isBulkMode) Color.White else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "توليد متسلسل / متعدد ⚡",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = if (isBulkMode) Color.White else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                if (!isBulkMode) {
                    // Single Location Form Body
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الموقع التخزيني (مثل: الرف B3، مستودع أ)") },
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

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("الوصف التفصيلي للموقع") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    // Field 1: Location Type Picker Card
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
                                        text = "التصنيف المباشر لمحتوى الموقع (locationType)",
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

                    // Field 2: Parent Location Picker Card (Only for non-SITE locations)
                    if (locationType == "SITE") {
                        LaunchedEffect(Unit) {
                            selectedParentId = null
                        }
                    } else {
                        val parentLocationObj = locations.find { it.id == selectedParentId }
                        val parentTitle = if (selectedParentId == null) "موقع رئيسي (Root)" else parentLocationObj?.name ?: "موقع #${selectedParentId}"
                        val parentBreadcrumb = if (selectedParentId != null) getFullPathForLocation(locations, selectedParentId, separator = " > ") else "عقدة جذرية بدون موقع أب حاوي"

                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isParentLocationPickerOpen = true }
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
                                            imageVector = if (selectedParentId == null) Icons.Default.AccountTree else if (parentLocationObj?.structural == true) Icons.Default.HomeWork else Icons.Default.Place,
                                            contentDescription = null,
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "الموقع الحاوي (الأب) في الهرمية الشجرية",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "$parentTitle — $parentBreadcrumb",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color(0xFF4F46E5)
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "اختر الموقع الأب",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Requirement 3: Hierarchy Placement Preview Card
                    val previewLocName = name.ifBlank { "موقع جديد" }
                    val treeLines = buildHierarchyTreeLines(locations, selectedParentId, previewLocName)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountTree,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "📍 التموضع الشجري للموقع الجديد (Hierarchy Placement):",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF15803D)
                                )
                            }

                            if (selectedParentId == null) {
                                Text(
                                    text = "سيتم إنشاء هذا الموقع كمركز رئيسي (Root) في أعلى الهرمية التخزينية.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF166534)
                                )
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                treeLines.forEach { (depth, text) ->
                                    val indentPrefix = if (depth == 0) "" else "   ".repeat(depth) + "└── "
                                    Text(
                                        text = "$indentPrefix$text",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = if (depth == treeLines.lastIndex) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (depth == treeLines.lastIndex) Color(0xFF15803D) else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }

                    // Structural Location Checkbox / Toggle
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { structural = !structural }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = structural,
                                onCheckedChange = { structural = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                            )
                            Column {
                                Text(
                                    text = "موقع هيكلي لتجميع العقد",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "يُستخدم فقط لتقسيم الهيكلية الشجرية ويمنع التخزين المباشر به",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // External Location Toggle
                    Surface(
                        color = if (external) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (external) Color(0xFF3B82F6) else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { external = !external }
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
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (external) Color(0xFFDBEAFE) else Color(0xFFEEF2FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (external) Icons.Default.Public else Icons.Default.HomeWork,
                                        contentDescription = null,
                                        tint = if (external) Color(0xFF2563EB) else Color(0xFF4F46E5),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "موقع تخزيني خارجي (external)",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "مستودع خارجي، موقع شريك/مقاول تصنيع، أو شحنة ترانزيت",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Switch(
                                checked = external,
                                onCheckedChange = { external = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF3B82F6),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }

                    if (locationType == "SITE") {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("العنوان الجغرافي / موقع المنشأة (Address)") },
                            placeholder = { Text("مثال: المنطقة الصناعية - رغاية، الجزائر العاصمة") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF4F46E5))
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

                    OutlinedTextField(
                        value = customCapacityText,
                        onValueChange = { customCapacityText = it },
                        label = { Text("السعة التخزينية القصوى للموقع (customCapacity)") },
                        placeholder = { Text("مثال: 500 أو 1000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                    // Capacity Unit Selection Card (Matching design with other pickers)
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCapacityUnitPickerOpen = true }
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
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "وحدة قياس السعة التخزينية (Capacity Unit)",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "المعيار المعتمد: $capacityUnit",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFF4F46E5)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "اختر وحدة القياس",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Owner / Supervisor Selection Card
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
                                        text = "المسؤول / المشرف المباشر (ownerId)",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = selectedOwner?.let { "${it.name}${if (it.role.isNotBlank()) " (${it.role})" else ""}" } ?: "بدون مشرف محدد (null)",
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                if (name.isNotBlank()) {
                                    if (isEditMode) {
                                        showEditConfirmationDialog = true
                                    } else {
                                        val calculatedOwnerId = selectedOwner?.let { user ->
                                            user.uuid.filter { it.isDigit() }.toLongOrNull()
                                                ?: user.uuid.hashCode().toLong().absoluteValue
                                        }
                                        onConfirm(
                                            name.trim(),
                                            description.trim(),
                                            selectedParentId,
                                            structural,
                                            external,
                                            locationType,
                                            icon,
                                            calculatedOwnerId,
                                            "",
                                            address.trim(),
                                            customCapacityText.toDoubleOrNull(),
                                            capacityUnit
                                        )
                                    }
                                }
                            },
                            enabled = name.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4F46E5),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isEditMode) "حفظ التحديثات" else stringResource(Res.string.save),
                                fontWeight = FontWeight.Bold
                            )
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
                                        showEditConfirmationDialog = false
                                        val calculatedOwnerId = selectedOwner?.let { user ->
                                            user.uuid.filter { it.isDigit() }.toLongOrNull()
                                                ?: user.uuid.hashCode().toLong().absoluteValue
                                        }
                                        onConfirm(
                                            name.trim(),
                                            description.trim(),
                                            selectedParentId,
                                            structural,
                                            external,
                                            locationType,
                                            icon,
                                            calculatedOwnerId,
                                            "",
                                            address.trim(),
                                            customCapacityText.toDoubleOrNull(),
                                            capacityUnit
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
                } else {
                    // Bulk Location Generator Form Body
                    // Parent Location Selector (Only for non-SITE locations)
                    if (locationType == "SITE") {
                        LaunchedEffect(Unit) {
                            selectedParentId = null
                        }
                    } else {
                        val parentLocationObj = locations.find { it.id == selectedParentId }
                        val parentTitle = if (selectedParentId == null) "موقع رئيسي (Root)" else parentLocationObj?.name ?: "موقع #${selectedParentId}"
                        val parentBreadcrumb = if (selectedParentId != null) getFullPathForLocation(locations, selectedParentId, separator = " > ") else "عقدة جذرية بدون موقع أب حاوي"

                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isParentLocationPickerOpen = true }
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
                                            imageVector = if (selectedParentId == null) Icons.Default.AccountTree else if (parentLocationObj?.structural == true) Icons.Default.HomeWork else Icons.Default.Place,
                                            contentDescription = null,
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "الموقع الأب الحاوي لتوليد العُقد",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "$parentTitle — $parentBreadcrumb",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color(0xFF4F46E5)
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "اختر الموقع الأب",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Hierarchy Placement Preview Card for Bulk Mode
                    val previewBulkLocName = if (prefix.isNotBlank()) "${prefix.trim()}01" else "R-01"
                    val treeLinesBulk = buildHierarchyTreeLines(locations, selectedParentId, previewBulkLocName)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountTree,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "📍 التموضع الشجري للمواقع المولدة (Hierarchy Placement):",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF15803D)
                                )
                            }

                            if (selectedParentId == null) {
                                Text(
                                    text = "سيتم إنشاء هذه المواقع كعقد رئيسية (Root) في أعلى الهرمية التخزينية.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF166534)
                                )
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                treeLinesBulk.forEach { (depth, text) ->
                                    val indentPrefix = if (depth == 0) "" else "   ".repeat(depth) + "└── "
                                    Text(
                                        text = "$indentPrefix$text",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = if (depth == treeLinesBulk.lastIndex) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (depth == treeLinesBulk.lastIndex) Color(0xFF15803D) else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }

                    // Location Type Selector
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
                                        text = "قالب / نوع المواقع المراد توليدها",
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

                    // Prefix Field
                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { prefix = it },
                        label = { Text("البادئة النصية (Prefix) (مثل: R- أو B-)") },
                        placeholder = { Text("مثال: R- للرفوف أو B- للصناديق") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    // Sequence Range Inputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = startNumberText,
                            onValueChange = { startNumberText = it },
                            label = { Text("من رقم (بداية التسلسل)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        OutlinedTextField(
                            value = endNumberText,
                            onValueChange = { endNumberText = it },
                            label = { Text("إلى رقم (نهاية التسلسل)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }

                    // Zero-padding Toggle
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { padZeros = !padZeros }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = padZeros,
                                onCheckedChange = { padZeros = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                            )
                            Column {
                                Text(
                                    text = "ملء الأصفار للتنسيق (Zero-padding)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "تنسيق الأرقام لتصبح موحدة مثل: 01, 02 ... 20 بدلاً من 1, 2 ... 20",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = bulkDescription,
                        onValueChange = { bulkDescription = it },
                        label = { Text("وصف اختياري للمواقع المولدة") },
                        placeholder = { Text("مثال: رفوف مخصصة للمكونات السلبية") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    OutlinedTextField(
                        value = bulkCapacityText,
                        onValueChange = { bulkCapacityText = it },
                        label = { Text("السعة التخزينية القصوى لكل موقع مولد") },
                        placeholder = { Text("مثال: 200 وحدة لكل رف") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    // Live Preview Banner
                    val sNum = startNumberText.toIntOrNull() ?: 1
                    val eNum = endNumberText.toIntOrNull() ?: 20
                    val totalCount = (eNum - sNum + 1).coerceAtLeast(1)
                    val effectivePfx = prefix.ifBlank { "R-" }
                    val digits = if (padZeros) eNum.toString().length.coerceAtLeast(2) else 1
                    val p1 = if (padZeros) sNum.toString().padStart(digits, '0') else sNum.toString()
                    val p2 = if (padZeros) (sNum + 1).toString().padStart(digits, '0') else (sNum + 1).toString()
                    val p3 = if (padZeros) (sNum + 2).toString().padStart(digits, '0') else (sNum + 2).toString()
                    val pEnd = if (padZeros) eNum.toString().padStart(digits, '0') else eNum.toString()

                    val previewText = "سيتم إنشاء $totalCount موقعاً تخزينياً: ($effectivePfx$p1, $effectivePfx$p2, $effectivePfx$p3 ... $effectivePfx$pEnd) تابعة للموقع المختار ومزودة بمواصفات القالب."

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
                                text = previewText,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = Color(0xFF4338CA)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                val start = startNumberText.toIntOrNull() ?: 1
                                val end = endNumberText.toIntOrNull() ?: 20
                                if (start <= end) {
                                    onConfirmBulk(
                                        selectedParentId,
                                        locationType,
                                        prefix.ifBlank { "R-" },
                                        start,
                                        end,
                                        padZeros,
                                        bulkDescription.trim(),
                                        bulkCapacityText.toDoubleOrNull()
                                    )
                                }
                            },
                            enabled = (startNumberText.toIntOrNull() ?: 0) <= (endNumberText.toIntOrNull() ?: 0),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4F46E5),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("توليد $totalCount موقعاً دفعة واحدة", fontWeight = FontWeight.Bold)
                        }
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
private fun PackagingSelectionBottomSheet(
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
private fun PurchaseOrderSelectionBottomSheet(
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
private fun SupplierSelectionBottomSheet(
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
private fun AddStockItemBottomSheet(
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
        link: String,
        imageUrl: String?,
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
        notes: String
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
                .fillMaxHeight(0.92f)
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
                            val qty = quantityText.toDoubleOrNull() ?: 1.0
                            val price = purchasePriceText.toDoubleOrNull() ?: 0.0
                            if (pId != null && qty > 0.0) {
                                onConfirm(
                                    pId, selectedLocationId, qty, serial, batch, packaging,
                                    selectedStatus, price, currency, selectedSupplierPartId,
                                    selectedPurchaseOrderId, expiryDate,
                                    false, false, link, notes
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
        PackagingSelectionBottomSheet(
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
            onDismiss = { isAddPartSheetOpen = false },
            onConfirm = { name, ipn, desc, catId, units, assembly, component, isTemplate, variantOf, minStock, maxStock, revision, keywords, trackable, purchaseable, salable, virtual, defaultLocId, defaultExpiryDays, link, imageUrl, active, locked ->
                val insertedPart = onAddNewPart(
                    name, ipn, desc, catId, units, assembly, component,
                    isTemplate, variantOf, minStock, maxStock, revision,
                    keywords, trackable, purchaseable, salable, virtual,
                    defaultLocId, defaultExpiryDays, link, imageUrl, active, locked
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
            onConfirm = { name, desc, parentId, structural, external, locationType, icon, ownerId, customIcon, address, customCapacity, capacityUnit ->
                val insertedLocation = onAddNewLocation(
                    name, desc, parentId, structural, external, locationType, icon, ownerId, customIcon, address
                )
                selectedLocationId = insertedLocation.id
                isAddNewLocationSheetOpen = false
            },
            onConfirmBulk = { parentId, locationType, prefix, startNum, endNum, padZeros, desc, customCapacity ->
                onAddNewLocationBulk?.invoke(parentId, locationType, prefix, startNum, endNum, padZeros, desc)
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
                .fillMaxHeight(0.88f)
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
        if (searchQuery.isBlank()) {
            locations
        } else {
            val q = searchQuery.trim().lowercase()
            locations.filter {
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
                .fillMaxHeight(0.88f)
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
                                                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitStockDialog(
    item: StockItem,
    onDismiss: () -> Unit,
    onConfirm: (splitQty: Double) -> Unit
) {
    var splitQtyText by remember { mutableStateOf("1.0") }

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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تجزئة الكمية المخزنية (Split Stock)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            Text("إجمالي الكمية الأصلية الحالية: ${item.quantity}", fontSize = 12.sp)

            OutlinedTextField(
                value = splitQtyText,
                onValueChange = { splitQtyText = it },
                label = { Text("الكمية المقتطعة للدفعة الفرعية (أقل من ${item.quantity})") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val qty = splitQtyText.toDoubleOrNull() ?: 0.0
                        if (qty > 0.0 && qty < item.quantity) {
                            onConfirm(qty)
                        }
                    },
                    enabled = (splitQtyText.toDoubleOrNull() ?: 0.0) in 0.1..<item.quantity,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تأكيد التجزئة", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockFilterBottomSheet(
    uiState: StockUiState,
    onDismiss: () -> Unit,
    onApply: (selectedLocationIds: Set<Long>) -> Unit,
    onClear: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedLocationIds by remember { mutableStateOf(uiState.selectedLocationIds) }
    var selectedLocationForLabel by remember { mutableStateOf<StockLocation?>(null) }
    var inStockOnly by remember { mutableStateOf(false) }
    var lowStockOnly by remember { mutableStateOf(false) }
    var outOfStockOnly by remember { mutableStateOf(false) }

    val filteredLocations = remember(uiState.locations, searchQuery) {
        val query = searchQuery.normalizeArabic()
        if (query.isBlank()) {
            uiState.locations
        } else {
            uiState.locations.filter { loc ->
                loc.name.normalizeArabic().contains(query) ||
                loc.description.normalizeArabic().contains(query) ||
                loc.locationType.normalizeArabic().contains(query)
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
                .fillMaxHeight(0.85f)
        ) {
            // 1. Header Section
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            "تصفية المخزون والمواقع",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "تحديد المواد المعروضة حسب المخزن وحالة التخزين",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = {
                        selectedLocationIds = emptySet()
                        inStockOnly = false
                        lowStockOnly = false
                        outOfStockOnly = false
                        onClear()
                    }) {
                        Text(
                            "مسح الكل",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = Color(0xFF4F46E5)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // 2. Scrollable Body Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Search Input by Location / Warehouse / Shelf
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن اسم المستودع، الموقع، الرف...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // 2. شريط التبويبات المزدوجة
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { selectedTab = 0 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 0) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 0) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "📍 الموقع الفيزيائي (${uiState.locations.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (selectedTab == 0) Color(0xFF4F46E5) else Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            onClick = { selectedTab = 1 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 1) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 1) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚙️ حالة البضاعة والقطع",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (selectedTab == 1) Color(0xFF4F46E5) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                if (selectedTab == 0) {
                    // Section: Locations Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredLocations.forEach { loc ->
                            val isChecked = selectedLocationIds.contains(loc.id)
                            val count = remember(loc.id, uiState.allStockItems) {
                                uiState.allStockItems.count { it.locationId == loc.id }
                            }
                            val totalQty = remember(loc.id, uiState.allStockItems) {
                                uiState.allStockItems.filter { it.locationId == loc.id }.sumOf { it.quantity }
                            }
                            val occupancyPct = loc.calculateOccupancyPercentage(totalQty)
                            val effectiveCap = loc.effectiveCapacity
                            val barColor = when {
                                occupancyPct >= 90.0 -> Color(0xFFEF4444)
                                occupancyPct >= 70.0 -> Color(0xFFF97316)
                                else -> Color(0xFF10B981)
                            }
                            val badgeEmoji = when {
                                loc.structural -> "🏗️"
                                loc.external -> "🌐"
                                else -> "📍"
                            }

                            Surface(
                                onClick = {
                                    selectedLocationIds = if (isChecked) selectedLocationIds - loc.id else selectedLocationIds + loc.id
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChecked) Color(0xFFEEF2FF).copy(alpha = 0.6f) else Color.White,
                                border = BorderStroke(1.dp, if (isChecked) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(badgeEmoji, fontSize = 20.sp)
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                loc.name,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                ),
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                loc.description.ifBlank { "موقع تخزين • ${loc.locationType}" },
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                                color = Color(0xFF64748B)
                                            )
                                            if (!loc.structural) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    LinearProgressIndicator(
                                                        progress = { (occupancyPct / 100.0).toFloat() },
                                                        modifier = Modifier
                                                            .width(70.dp)
                                                            .height(5.dp)
                                                            .clip(CircleShape),
                                                        color = barColor,
                                                        trackColor = Color(0xFFE2E8F0)
                                                    )
                                                    Text(
                                                        "الإشغال: ${occupancyPct.toInt()}% (${totalQty.toInt()}/${effectiveCap.toInt()})",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = barColor
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = if (count > 0) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, if (count > 0) Color(0xFFA7F3D0) else Color(0xFFE2E8F0))
                                        ) {
                                            Text(
                                                "$count قطعة",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = if (count > 0) Color(0xFF047857) else Color(0xFF64748B),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { selectedLocationForLabel = loc },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Print,
                                                contentDescription = "طباعة ملصق الرف",
                                                tint = Color(0xFF4F46E5),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        if (isChecked) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF4F46E5),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedLocationForLabel != null) {
                        val parentPath = remember(selectedLocationForLabel, uiState.locations) {
                            getFullPathForLocation(uiState.locations, selectedLocationForLabel!!.parentId, separator = " > ")
                        }
                        PrintableLabelBottomSheet(
                            location = selectedLocationForLabel!!,
                            parentPath = parentPath,
                            onDismiss = { selectedLocationForLabel = null }
                        )
                    }
                } else {
                    // تبويب حالة البضاعة المخزنية
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { inStockOnly = !inStockOnly }
                            ) {
                                Checkbox(
                                    checked = inStockOnly,
                                    onCheckedChange = { inStockOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Text(
                                    "متوفر بالكامل (In Stock)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { lowStockOnly = !lowStockOnly }
                            ) {
                                Checkbox(
                                    checked = lowStockOnly,
                                    onCheckedChange = { lowStockOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Text(
                                    "منخفض المخزون (Low Stock)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { outOfStockOnly = !outOfStockOnly }
                            ) {
                                Checkbox(
                                    checked = outOfStockOnly,
                                    onCheckedChange = { outOfStockOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Text(
                                    "نفد من المخزون (Out of Stock)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Sticky Actions Footer Bar
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                val totalMatchingCount = remember(selectedLocationIds, uiState.allStockItems) {
                    if (selectedLocationIds.isEmpty()) {
                        uiState.allStockItems.size
                    } else {
                        uiState.allStockItems.count { it.locationId != null && selectedLocationIds.contains(it.locationId) }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onApply(selectedLocationIds)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                "تطبيق التصفية (عرض $totalMatchingCount قطعة)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "إلغاء",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

