package com.nextstepai.inventory.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.AppUser
import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.ui.components.AppCameraKView
import com.nextstepai.inventory.ui.components.LocationBarcodeScannerBottomSheet
import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.data.CompletenessTone
import com.nextstepai.inventory.data.ManufacturerPart
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartAttachment
import com.nextstepai.inventory.data.PendingAttachment
import com.nextstepai.inventory.data.AttachmentType
import com.nextstepai.inventory.data.currentLabelSnapshot
import com.nextstepai.inventory.data.PartCategory
import com.nextstepai.inventory.data.PartNotes
import com.nextstepai.inventory.data.PartParameter
import com.nextstepai.inventory.data.PartRelatedView
import com.nextstepai.inventory.data.PartTestTemplate
import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.SupplierPart
import com.nextstepai.inventory.data.db.PartInternalPriceEntity
import com.nextstepai.inventory.data.db.PartPricingEntity
import com.nextstepai.inventory.data.db.PartSalePriceEntity
import com.nextstepai.inventory.repository.PartsSummary
import com.nextstepai.inventory.ui.components.Barcode128Canvas
import com.nextstepai.inventory.ui.components.CurrencySelectionBottomSheet
import com.nextstepai.inventory.ui.components.CurrencySelectorField
import com.nextstepai.inventory.ui.components.PartAllocationsBottomSheet
import com.nextstepai.inventory.ui.components.PartIdentityBottomSheet
import com.nextstepai.inventory.ui.components.PrintablePartLabelBottomSheet
import com.nextstepai.inventory.ui.components.QrCodeCanvas
import com.nextstepai.inventory.ui.components.QuickTransferBottomSheet
import com.nextstepai.inventory.ui.components.SearchableCompanyPickerDialog
import com.nextstepai.inventory.ui.components.ShakeController
import com.nextstepai.inventory.ui.components.SmartIpnField
import com.nextstepai.inventory.ui.components.SmartIpnGenerator
import com.nextstepai.inventory.util.BarcodePayloadHelper
import com.nextstepai.inventory.util.DateTimeUtils
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_part
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.card_parts_title
import nextstepai_inventory.shared.generated.resources.filter_assembly
import nextstepai_inventory.shared.generated.resources.filter_low_stock
import nextstepai_inventory.shared.generated.resources.filter_starred
import nextstepai_inventory.shared.generated.resources.parts_count
import nextstepai_inventory.shared.generated.resources.save
import kotlin.time.Clock

/**
 * شاشة إدارة القطع والمكونات الأساسية (Part Management Screen).
 * مطابقة للهيكل القياسي الموحد للتطبيق مع الترويسة العلوية التكيفية وحقول البحث والفلترة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartManagementScreen(
    viewModel: PartViewModel,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var isBarcodeScannerOpen by remember { mutableStateOf(false) }
    var receivingStockPart by remember { mutableStateOf<Part?>(null) }
    var editingPart by remember { mutableStateOf<Part?>(null) }
    var deletingPart by remember { mutableStateOf<Part?>(null) }
    var labelPrintingPart by remember { mutableStateOf<Part?>(null) }
    var locationViewingPart by remember { mutableStateOf<Part?>(null) }
    var transferringStockItem by remember { mutableStateOf<StockItem?>(null) }

    Scaffold(
        topBar = {
            PartsTopBar(
                onBackClick = onBackClick
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
                Spacer(modifier = Modifier.height(8.dp))

                // شريط البحث المطور مع دمج الفلتر الخارجي الجانبي المتناسق
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = {
                            Text(
                                text = "البحث باسم القطعة، الـ IPN، أو الوصف...",
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
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
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

                    // 2. زر مسح الباركود المنفصل الموازي لزر الفلتر
                    var isBarcodePressed by remember { mutableStateOf(false) }
                    val barcodeScale by animateFloatAsState(
                        targetValue = if (isBarcodePressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    )

                    LaunchedEffect(isBarcodePressed) {
                        if (isBarcodePressed) {
                            delay(150)
                            isBarcodePressed = false
                        }
                    }

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

                    val isFilterActive = uiState.lowStockOnlyFilter || uiState.assemblyOnlyFilter || uiState.starredOnlyFilter || uiState.selectedCategoryId != null
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
                                text = "فلتر",
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

                // قائمة القطع
                if (uiState.parts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد قطع مسجلة تفي بالشروط",
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
                        val countText = when (uiState.parts.size) {
                            0 -> "لا توجد قطع مسجلة"
                            1 -> "قطعة واحدة مسجلة"
                            2 -> "قطعتان مسجلتان"
                            in 3..10 -> "${uiState.parts.size} قطع مسجلة"
                            else -> "${uiState.parts.size} قطعة مسجلة"
                        }
                        Text(
                            text = countText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(uiState.parts, key = { index, part -> "part-${part.id}-$index" }) { _, part ->
                            val category = uiState.categories.find { c -> c.id == part.categoryId }
                            PartItemCard(
                                part = part,
                                categoryName = category?.name,
                                stockItems = uiState.stockItems,
                                stockLocations = uiState.stockLocations,
                                isStarred = uiState.starredPartIds.contains(part.id),
                                onToggleStar = { viewModel.togglePartStar(part.id) },
                                onAddStock = { receivingStockPart = it },
                                onEditPart = { editingPart = it },
                                onDeletePart = { deletingPart = it },
                                onPrintLabel = { labelPrintingPart = it },
                                onShowLocations = { locationViewingPart = it },
                                onShowAllocations = { viewModel.openAllocationsForPart(it) },
                                onClick = { viewModel.selectPart(part) }
                            )
                        }
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = { viewModel.setAddPartDialogOpen(true) },
                containerColor = Color(0xFF4F46E5),
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.add_new_part),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "إضافة قطعة",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                )
            }

            if (receivingStockPart != null) {
                        AddStockBottomSheet(
                            part = receivingStockPart!!,
                            locations = uiState.stockLocations,
                            users = uiState.users,
                            onDismiss = { receivingStockPart = null },
                            onConfirm = { partId, locId, qty, pkg, refDoc, notes, receiptDate, userId, capturedDocPath ->
                                viewModel.receiveStockItem(
                                    partId = partId,
                                    locationId = locId,
                                    quantity = qty,
                                    packaging = pkg,
                                    referenceDoc = refDoc,
                                    notes = notes,
                                    receiptDate = receiptDate,
                                    receivedByUserId = userId,
                                    capturedDocBytes = null,
                                    capturedDocPath = capturedDocPath
                                )
                            }
                        )
                    }

                    if (editingPart != null) {
                        AddPartBottomSheet(
                            partToEdit = editingPart,
                            categories = uiState.categories,
                            locations = uiState.stockLocations,
                            templateParts = uiState.templateParts,
                            allParts = uiState.parts,
                            existingAttachments = uiState.selectedPartAttachments,
                            onCreateCategory = { catName, catDesc -> viewModel.addNewCategory(catName, catDesc) },
                            onDeleteCategory = { catId -> viewModel.deleteCategory(catId) },
                            onDismiss = { editingPart = null },
                            onUpdate = { updatedPart, pendingAttachments ->
                                viewModel.updatePart(updatedPart, pendingAttachments)
                                editingPart = null
                            },
                            onConfirm = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
                        )
                    }

                    if (deletingPart != null) {
                        ConfirmDeletePartDialog(
                            partName = deletingPart!!.name,
                            onConfirm = {
                                viewModel.deletePart(deletingPart!!.id)
                                deletingPart = null
                            },
                            onDismiss = { deletingPart = null }
                        )
                    }

                    if (labelPrintingPart != null) {
                        val category = uiState.categories.find { c -> c.id == labelPrintingPart!!.categoryId }
                        PrintablePartLabelBottomSheet(
                            part = labelPrintingPart!!,
                            categoryName = category?.name,
                            onDismiss = { labelPrintingPart = null },
                            onRegenerateLabel = {
                                val currentSnapshot = labelPrintingPart!!.currentLabelSnapshot
                                val updated = viewModel.savePartLabelSnapshot(labelPrintingPart!!.id, currentSnapshot)
                                if (updated != null) {
                                    labelPrintingPart = updated
                                }
                            }
                        )
                    }

                    if (locationViewingPart != null) {
                        PartStorageLocationsBottomSheet(
                            part = locationViewingPart!!,
                            stockItems = uiState.stockItems,
                            locations = uiState.stockLocations,
                            onDismiss = { locationViewingPart = null },
                            onInitiateTransfer = { stockItem ->
                                transferringStockItem = stockItem
                            }
                        )
                    }

                    if (uiState.isAllocationsBottomSheetOpen && uiState.selectedAllocationPart != null) {
                        PartAllocationsBottomSheet(
                            part = uiState.selectedAllocationPart!!,
                            allocations = uiState.selectedPartAllocations,
                            onDismiss = { viewModel.closeAllocationsBottomSheet() },
                            onReleaseAllocation = { allocationId ->
                                viewModel.releaseAllocation(uiState.selectedAllocationPart!!.id, allocationId)
                            }
                        )
                    }

                    if (transferringStockItem != null) {
                        val stockItem = transferringStockItem!!
                        val currentLoc = uiState.stockLocations.find { it.id == stockItem.locationId }
                            ?: StockLocation(name = "موقع غير محدد")
                        val partName = uiState.parts.find { it.id == stockItem.partId }?.name ?: "قطعة #${stockItem.partId}"

                        QuickTransferBottomSheet(
                            stockItem = stockItem,
                            partName = partName,
                            currentLocation = currentLoc,
                            allLocations = uiState.stockLocations,
                            allStockItems = uiState.stockItems,
                            onDismiss = { transferringStockItem = null },
                            onConfirmTransfer = { targetLocId, qty, reason, notes ->
                                viewModel.transferStockItem(
                                    itemId = stockItem.id,
                                    sourceLocationId = stockItem.locationId,
                                    targetLocationId = targetLocId,
                                    quantity = qty,
                                    reason = reason,
                                    notes = notes
                                )
                                transferringStockItem = null
                            }
                        )
                    }

    // بطاقة المعاينة والتعريف السريع للقطعة
    if (uiState.selectedPart != null) {
        val uriHandler = LocalUriHandler.current
        PartIdentityBottomSheet(
            part = uiState.selectedPart!!,
            notes = uiState.selectedPartNotes,
            attachments = uiState.selectedPartAttachments,
            manufacturerParts = uiState.selectedPartManufacturerParts,
            supplierParts = uiState.selectedPartSupplierParts,
            relatedParts = uiState.selectedPartRelated,
            allCompanies = uiState.allCompanies,
            onDismiss = { viewModel.selectPart(null) },
            onOpenUrl = { rawUrl ->
                runCatching {
                    val trimmed = rawUrl.trim()
                    if (trimmed.isNotBlank()) {
                        val formattedUrl = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                            trimmed
                        } else {
                            "https://$trimmed"
                        }
                        uriHandler.openUri(formattedUrl)
                    }
                }
            },
            onSelectSubstitute = { substitutePart ->
                viewModel.selectPart(substitutePart)
            }
        )
    }

    // حوار إضافة قطعة جديدة
    if (uiState.isAddPartDialogOpen) {
        AddPartBottomSheet(
            categories = uiState.categories,
            locations = uiState.stockLocations,
            templateParts = uiState.templateParts,
            allParts = uiState.parts,
            onCreateCategory = { catName, catDesc -> viewModel.addNewCategory(catName, catDesc) },
            onDeleteCategory = { catId -> viewModel.deleteCategory(catId) },
            onDismiss = { viewModel.setAddPartDialogOpen(false) },
            onConfirm = { name, ipn, desc, catId, units, assembly, component, isTemplate, variantOf, minStock, maxStock, revision, keywords, trackable, purchaseable, salable, virtual, defaultLocId, defaultExpiryDays, pendingAttachments, active, locked ->
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
            }
        )
    }

    // نافذة الفلترة التفاعلية بـ 3 أقسام (مع دعم الاختيار المتعدد للتصنيفات)
    if (uiState.isFilterBottomSheetOpen) {
        PartFilterBottomSheet(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            initialCategoryIds = uiState.selectedCategoryIds,
            parts = uiState.parts,
            starredPartIds = uiState.starredPartIds,
            initialLowStock = uiState.lowStockOnlyFilter,
            initialAssembly = uiState.assemblyOnlyFilter,
            initialComponent = uiState.componentOnlyFilter,
            initialPurchaseable = uiState.purchaseableOnlyFilter,
            initialSalable = uiState.salableOnlyFilter,
            initialStarred = uiState.starredOnlyFilter,
            onDismiss = { viewModel.setFilterBottomSheetOpen(false) },
            onReset = { viewModel.resetFilters() },
            onApply = { catIds, lowStock, assembly, component, purchaseable, salable, starred ->
                viewModel.applyFilters(
                    categoryId = if (catIds.size == 1) catIds.first() else null,
                    categoryIds = catIds,
                    lowStock = lowStock,
                    assembly = assembly,
                    component = component,
                    purchaseable = purchaseable,
                    salable = salable,
                    starred = starred
                )
            }
        )
    }

    // نافذة ماسح الباركود والـ QR المباشر للقطع والمكونات
    if (isBarcodeScannerOpen) {
        LocationBarcodeScannerBottomSheet(
            onDismiss = { isBarcodeScannerOpen = false },
            onBarcodeScanned = { scannedResult ->
                isBarcodeScannerOpen = false
                val parsed = BarcodePayloadHelper.parsePayload(scannedResult)
                val matchedPart = uiState.parts.find { part ->
                    part.ipn.equals(scannedResult.trim(), ignoreCase = true) ||
                    part.ipn.equals(parsed.uuid, ignoreCase = true) ||
                    "part-${part.id}".equals(parsed.uuid, ignoreCase = true) ||
                    part.id.toString() == parsed.uuid.removePrefix("part-").removePrefix("p-") ||
                    part.name.equals(scannedResult.trim(), ignoreCase = true)
                }
                if (matchedPart != null) {
                    viewModel.onSearchQueryChanged(matchedPart.ipn.ifBlank { matchedPart.name })
                } else {
                    viewModel.onSearchQueryChanged(scannedResult)
                }
            }
        )
    }
        }
    }
}

/**
 * الترويسة العلوية لشاشة دليل القطع والمكونات
 */
@Composable
private fun PartsTopBar(
    onBackClick: (() -> Unit)?,
    onAddClick: (() -> Unit)? = null
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
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.scale(if (isRtl) -1f else 1f, 1f)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "دليل القطع والمكونات",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "إدارة المكونات الإلكترونية، تجميعات BOM وقوالب القياس",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            if (onAddClick != null) {
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEEF2FF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.add_new_part),
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}



/**
 * اختيار الأيقونة واللون الديناميكي للقطعة بناءً على النوع والتصنيف والوسوم.
 */
private fun getPartIconAndColors(part: Part, categoryName: String?): Triple<Color, Color, ImageVector> {
    val cat = categoryName.orEmpty().lowercase()
    val name = part.name.lowercase()

    return when {
        part.assembly || name.contains("bom") || name.contains("تجميع") ->
            Triple(Color(0xFFF3E8FF), Color(0xFF9333EA), Icons.Default.AccountTree)

        part.salable || name.contains("جاهز") || name.contains("منتج") ->
            Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), Icons.Default.ShoppingBag)

        part.isTemplate || name.contains("قالب") || name.contains("template") ->
            Triple(Color(0xFFCCFBF1), Color(0xFF0D9488), Icons.Default.Extension)

        cat.contains("شاشة") || name.contains("display") || name.contains("lcd") || name.contains("screen") ->
            Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), Icons.Default.Tv)

        cat.contains("كابل") || cat.contains("أسلاك") || name.contains("cable") || name.contains("wire") ->
            Triple(Color(0xFFFFEDD5), Color(0xFFEA580C), Icons.Default.Cable)

        cat.contains("كيميائي") || cat.contains("سوائل") || name.contains("chemical") || name.contains("fluid") ->
            Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Icons.Default.Science)

        cat.contains("ميكانيك") || cat.contains("صلب") || name.contains("screw") || name.contains("metal") ->
            Triple(Color(0xFFF1F5F9), Color(0xFF475569), Icons.Default.Hardware)

        else ->
            Triple(Color(0xFFEEF2FF), Color(0xFF4F46E5), Icons.Default.Memory)
    }
}

/**
 * صفحة منبثقة (Bottom Sheet) ناعمة لاستلام وإضافة كمية مخزنية سريعة لقطعة محددة
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStockBottomSheet(
    part: Part,
    locations: List<StockLocation> = emptyList(),
    users: List<AppUser> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        partId: Long,
        locationId: Long?,
        quantity: Double,
        packaging: String,
        referenceDoc: String,
        notes: String,
        receiptDate: String,
        receivedByUserId: Long?,
        capturedDocPath: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 1. Initial State Setup
    var quantityText by remember { mutableStateOf("10.0") }
    var selectedLocationId by remember { mutableStateOf<Long?>(part.defaultLocationId ?: locations.firstOrNull()?.id) }
    var packagingCode by remember { mutableStateOf("Box") }
    var isPackagingSheetOpen by remember { mutableStateOf(false) }
    var referenceDocText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val packagingOptionsList = remember {
        mutableStateListOf(
            StockPackagingOption("Reel", "Reel (بكرة)", "شريط دائري تُلف عليه العناصر السطحية (SMD) للالتقاط الآلي"),
            StockPackagingOption("Cut Tape", "Cut Tape (شريط مقصوص)", "أجزاء مقصوصة من البكرة للكميات الصغيرة أو العينات"),
            StockPackagingOption("Tube / Stick", "Tube / Stick (أنبوب بلاستيكي)", "أنابيب صلبة لحماية الدوائر المتكاملة (ICs) ومنع انثناء الأرجل"),
            StockPackagingOption("Tray", "Tray (صينية واقية)", "صوانٍ مقسمة لحمل المعالجات والشرائح الكبيرة الحساسة والتفريغ الكهروستاتيكي"),
            StockPackagingOption("Anti-Static Bag", "Anti-Static Bag (كيس مضاد للكهرباء)", "لحفظ الوحدات والمكونات المنفصلة وحمايتها"),
            StockPackagingOption("Box", "Box / Carton (صندوق / كرتونة)", "التغليف القياسي لتخزين البضائع والمستشعرات والمجموعات الجاهزة"),
            StockPackagingOption("Bulk / Loose", "Bulk / Loose (سائب)", "قطع غير مغلفة فردياً، مثل البراغي، الصواميل، أو الأسلاك"),
            StockPackagingOption("Pallet", "Pallet (منصة نقالة)", "للشحنات الكبيرة والحاويات عند تخزين عدد كبير من الكراتين معاً")
        )
    }

    val selectedPackagingOpt = remember(packagingCode) {
        packagingOptionsList.find { it.code.equals(packagingCode, ignoreCase = true) }
            ?: packagingOptionsList.find { it.labelAr.contains(packagingCode, ignoreCase = true) }
    }
    val displayPackagingText = selectedPackagingOpt?.labelAr ?: packagingCode.ifBlank { "Box / Carton (صندوق / كرتونة)" }

    // Date & Time Picker Setup (Default: current local date & time)
    val nowDateTime = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    val initialDateStr = "${nowDateTime.year}-${nowDateTime.monthNumber.toString().padStart(2, '0')}-${nowDateTime.dayOfMonth.toString().padStart(2, '0')}"
    val initialTimeStr = "${nowDateTime.hour.toString().padStart(2, '0')}:${nowDateTime.minute.toString().padStart(2, '0')}"
    var receiptDateTimeText by remember { mutableStateOf("$initialDateStr $initialTimeStr") }

    var selectedUser by remember(users) {
        mutableStateOf(users.firstOrNull() ?: AppUser("usr-004", "مدير المستودع والخدمات اللوجستية", "مدير المستودع"))
    }

    // Modal & Picker States
    var isSelectLocationSheetOpen by remember { mutableStateOf(false) }
    var isSelectUserSheetOpen by remember { mutableStateOf(false) }
    var showLocationScanner by remember { mutableStateOf(false) }

    // Two-Stage Date + Time Picker States
    var showReceiptDatePicker by remember { mutableStateOf(false) }
    var showReceiptTimePicker by remember { mutableStateOf(false) }
    var pendingSelectedDatePart by remember { mutableStateOf(initialDateStr) }

    // Ref Doc Scanners & Camera Capture State
    var showRefDocBarcodeScanner by remember { mutableStateOf(false) }
    var capturedDocPath by remember { mutableStateOf<String?>(null) }

    // Platform Camera & File Picker Launchers (Same engine as Add Part)
    val platformPickerLaunchers = rememberPlatformPickerLaunchers(
        onImageCaptured = { path -> capturedDocPath = path },
        onImagePicked = { path -> capturedDocPath = path },
        onFilePicked = { _ -> }
    )

    val selectedLocation = remember(selectedLocationId, locations) {
        locations.find { it.id == selectedLocationId }
    }

    // Validation
    val isFormValid = remember(quantityText) {
        val qty = quantityText.toDoubleOrNull() ?: 0.0
        qty > 0.0
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
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "استلام مخزون وبضاعة جديدة",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "تأكيد وتوثيق الشحنة بجدول Stock Items والتتبع آلياً",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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

            // Scrollable Content Column
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Part Details Summary Card
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
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
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = part.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "IPN: ${part.ipn.ifBlank { "-" }} • الرصيد الحالي: ${part.availableStock} ${part.units}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }

                // 2. Packaging Type & Quantity Row (Swapped & Height Matched to 56.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Packaging Type Selector Field (Height Matched 56.dp)
                    Surface(
                        onClick = { isPackagingSheetOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "نوع التغليف والتعبئة",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = displayPackagingText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5)
                            )
                        }
                    }

                    // 2. Quantity Input Field (Height 56.dp aligned)
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("الكمية المستلمة (${part.units}) *") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // 3. Physical Storage Location Selection with Separate Standalone QR Scanner Button
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "موقع التخزين المادي (المستودع / الرف) * :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Location Card Selector
                        Surface(
                            onClick = { isSelectLocationSheetOpen = true },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedLocation != null) Color(0xFFF8FAFC) else Color.White,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selectedLocation != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFEEF2FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = selectedLocation?.name ?: "بدون موقع تخزين محدد",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = selectedLocation?.description?.ifBlank { "اضغط لاختيار رف المستودع" } ?: "مكان إيداع الشحنة في المستودع",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Text(
                                    text = if (selectedLocation != null) "تغيير" else "اختر ▾",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF4F46E5)
                                )
                            }
                        }

                        // Separate Standalone Location QR Scanner Button Box
                        Surface(
                            onClick = { showLocationScanner = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEEF2FF),
                            border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "مسح موقع QR",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Receipt Date/Time & Responsible Person Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Two-Stage Receipt Date & Time Field
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = receiptDateTimeText,
                            onValueChange = { receiptDateTimeText = it },
                            readOnly = true,
                            label = { Text("تاريخ ووقت الاستلام *") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showReceiptDatePicker = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = "اختيار التاريخ والوقت",
                                        tint = Color(0xFF4F46E5)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showReceiptDatePicker = true }
                        )
                    }

                    // Responsible Person Selector Field
                    Surface(
                        onClick = { isSelectUserSheetOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "المسؤول / المستلم *",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = selectedUser?.name ?: "اختر المستلم",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Stage 1: DatePicker Dialog
                if (showReceiptDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds()
                    )
                    DatePickerDialog(
                        onDismissRequest = { showReceiptDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val instant = Instant.fromEpochMilliseconds(millis)
                                    val dateTime = instant.toLocalDateTime(TimeZone.UTC)
                                    val year = dateTime.year
                                    val month = dateTime.monthNumber.toString().padStart(2, '0')
                                    val day = dateTime.dayOfMonth.toString().padStart(2, '0')
                                    pendingSelectedDatePart = "$year-$month-$day"
                                }
                                showReceiptDatePicker = false
                                showReceiptTimePicker = true
                            }) {
                                Text("التالي: اختيار الوقت ➔", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showReceiptDatePicker = false }) {
                                Text("إلغاء")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                // Stage 2: TimePicker Dialog
                if (showReceiptTimePicker) {
                    val timePickerState = rememberTimePickerState(
                        initialHour = nowDateTime.hour,
                        initialMinute = nowDateTime.minute,
                        is24Hour = true
                    )
                    AlertDialog(
                        onDismissRequest = { showReceiptTimePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val hh = timePickerState.hour.toString().padStart(2, '0')
                                val mm = timePickerState.minute.toString().padStart(2, '0')
                                receiptDateTimeText = "$pendingSelectedDatePart $hh:$mm"
                                showReceiptTimePicker = false
                            }) {
                                Text("تأكيد التاريخ والوقت", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showReceiptTimePicker = false }) {
                                Text("إلغاء")
                            }
                        },
                        text = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                TimePicker(state = timePickerState)
                            }
                        }
                    )
                }

                // 5. Invoice / PO / Tracking Ref Field with Separate Standalone Barcode Scanner Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = referenceDocText,
                        onValueChange = { referenceDocText = it },
                        label = {
                            Text(
                                text = "مرجع الشحنة / الفاتورة (اختياري)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = { Text("مثال: INV-2025-001 أو PO-8840") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF64748B)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    // Separate Standalone Barcode Scanner Button Box
                    Surface(
                        onClick = { showRefDocBarcodeScanner = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEEF2FF),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "مسح باركود الشحنة",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 6. Dedicated Document Photography & Attachment Section (Camera + Gallery - Same as Add Part)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "تصوير وتوثيق مستند الاستلام المادي :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Camera Launcher Button (Same engine & design as Add Part)
                        Surface(
                            onClick = { platformPickerLaunchers.launchCamera() },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFE4E6),
                            border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "الكاميرا",
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "التقاط صورة (كاميرا)",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF9F1239)
                                        )
                                    )
                                    Text(
                                        text = "تصوير الفاتورة / البوليصة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                                        color = Color(0xFFBE123C)
                                    )
                                }
                            }
                        }

                        // 2. Gallery Launcher Button
                        Surface(
                            onClick = { platformPickerLaunchers.launchGalleryPicker() },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "معرض الصور",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "اختيار من المعرض",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF92400E)
                                        )
                                    )
                                    Text(
                                        text = "رفع صورة محفوظة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }
                    }

                    if (platformPickerLaunchers.isLoading) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF2563EB)
                                )
                                Text(
                                    text = "جاري معالجة وحفظ صورة مستند الشحنة...",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1E40AF))
                                )
                            }
                        }
                    }

                    // Display attached photo card if captured or picked
                    if (!capturedDocPath.isNullOrBlank()) {
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFD1FAE5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "تم إرفاق مستند الشحنة بنجاح",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            ),
                                            color = Color(0xFF065F46)
                                        )
                                        Text(
                                            text = capturedDocPath!!.substringAfterLast('/'),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                            color = Color(0xFF047857),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { capturedDocPath = null },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "حذف الصورة",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 7. Shipment Notes (Optional)
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("ملاحظات الشحنة") },
                    placeholder = { Text("أي ملاحظات ميدانية عن الشحنة...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // Action Buttons (Fixed at bottom)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(Res.string.cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = {
                        val qty = quantityText.toDoubleOrNull() ?: 0.0
                        val userIdVal = selectedUser?.uuid?.removePrefix("usr-")?.toLongOrNull() ?: 1L
                        if (isFormValid) {
                            onConfirm(
                                part.id,
                                selectedLocationId,
                                qty,
                                displayPackagingText,
                                referenceDocText,
                                notesText,
                                receiptDateTimeText,
                                userIdVal,
                                capturedDocPath
                            )
                            onDismiss()
                        }
                    },
                    enabled = isFormValid,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(
                        "تأكيد استلام الشحنة",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    // Packaging Selection Bottom Sheet
    if (isPackagingSheetOpen) {
        PackagingSelectionBottomSheet(
            selectedPackagingCode = packagingCode,
            packagingOptionsList = packagingOptionsList,
            onDismiss = { isPackagingSheetOpen = false },
            onPackagingSelected = { opt ->
                packagingCode = opt.code
                isPackagingSheetOpen = false
            },
            onAddPackagingOption = { opt ->
                packagingOptionsList.add(opt)
            },
            onDeletePackagingOption = { opt ->
                packagingOptionsList.remove(opt)
            }
        )
    }

    // Location Scanner Bottom Sheet
    if (showLocationScanner) {
        LocationBarcodeScannerBottomSheet(
            locations = locations,
            onDismiss = { showLocationScanner = false },
            onBarcodeScanned = { payload ->
                val matchedLoc = locations.find {
                    it.id.toString() == payload ||
                    it.name.contains(payload, ignoreCase = true) ||
                    it.description.contains(payload, ignoreCase = true) ||
                    it.uuid == payload
                }
                if (matchedLoc != null) {
                    selectedLocationId = matchedLoc.id
                }
                showLocationScanner = false
            }
        )
    }

    // Ref Doc Barcode Scanner
    if (showRefDocBarcodeScanner) {
        LocationBarcodeScannerBottomSheet(
            onDismiss = { showRefDocBarcodeScanner = false },
            onBarcodeScanned = { barcode ->
                if (barcode.isNotBlank()) {
                    referenceDocText = barcode
                }
                showRefDocBarcodeScanner = false
            }
        )
    }

    if (isSelectLocationSheetOpen) {
        SelectLocationBottomSheet(
            locations = locations,
            selectedLocationId = selectedLocationId,
            onDismiss = { isSelectLocationSheetOpen = false },
            onSelect = { selectedLocationId = it?.id }
        )
    }

    if (isSelectUserSheetOpen) {
        SelectUserBottomSheet(
            users = if (users.isNotEmpty()) users else listOf(selectedUser),
            selectedUserUuid = selectedUser?.uuid,
            onDismiss = { isSelectUserSheetOpen = false },
            onSelect = { selectedUser = it }
        )
    }
}

private data class StockPackagingOption(
    val code: String,
    val labelAr: String,
    val descAr: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackagingSelectionBottomSheet(
    selectedPackagingCode: String,
    packagingOptionsList: List<StockPackagingOption>,
    onDismiss: () -> Unit,
    onPackagingSelected: (StockPackagingOption) -> Unit,
    onAddPackagingOption: (StockPackagingOption) -> Unit,
    onDeletePackagingOption: (StockPackagingOption) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showAddDialog by remember { mutableStateOf(false) }
    var newOptionName by remember { mutableStateOf("") }
    var newOptionDesc by remember { mutableStateOf("") }

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
                            text = "حدد نوع أسلوب تغليف القطعة أو قم بإضافة/حذف الأنواع",
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

            // Add New Packaging Option Button Box
            Surface(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEEF2FF),
                border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إضافة نوع تغليف وتعبئة جديد +",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4F46E5)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                items(packagingOptionsList) { opt ->
                    val isSelected = opt.code.equals(selectedPackagingCode, ignoreCase = true) ||
                        opt.labelAr.equals(selectedPackagingCode, ignoreCase = true)
                    Surface(
                        onClick = {
                            if (isSelected) {
                                // Deselect on re-click (toggle off)
                                onPackagingSelected(StockPackagingOption("", "بدون تغليف محدد", ""))
                            } else {
                                onPackagingSelected(opt)
                            }
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
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF4F46E5) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )

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
                                    if (opt.descAr.isNotBlank()) {
                                        Text(
                                            text = opt.descAr,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            // Delete Option Button
                            IconButton(
                                onClick = { onDeletePackagingOption(opt) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "حذف النوع",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add New Packaging Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text("إضافة نوع تغليف جديد", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newOptionName,
                        onValueChange = { newOptionName = it },
                        label = { Text("اسم نوع التغليف *") },
                        placeholder = { Text("مثال: Wooden Crate (صندوق خشبي)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newOptionDesc,
                        onValueChange = { newOptionDesc = it },
                        label = { Text("الوصف (اختياري)") },
                        placeholder = { Text("وصف مختصر لأسلوب التغليف...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newOptionName.isNotBlank()) {
                            val newOpt = StockPackagingOption(
                                code = newOptionName,
                                labelAr = newOptionName,
                                descAr = newOptionDesc
                            )
                            onAddPackagingOption(newOpt)
                            onPackagingSelected(newOpt)
                            newOptionName = ""
                            newOptionDesc = ""
                            showAddDialog = false
                            onDismiss()
                        }
                    },
                    enabled = newOptionName.isNotBlank()
                ) {
                    Text("حفظ وتحديد", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectUserBottomSheet(
    users: List<AppUser>,
    selectedUserUuid: String?,
    onDismiss: () -> Unit,
    onSelect: (AppUser) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.role.contains(searchQuery, ignoreCase = true)
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Row
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
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "اختيار المشرف المسؤول",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
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

            Spacer(modifier = Modifier.height(12.dp))

            // Search input field for responsible user / role
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن مشرف أو دور وظيفي...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF64748B)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "مسح البحث",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // User items list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredUsers) { user ->
                    val isSelected = user.uuid == selectedUserUuid
                    Surface(
                        onClick = {
                            onSelect(user)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color.White,
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFF4F46E5) else Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color(0xFF4F46E5),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = user.role.ifBlank { "المستخدم المستلم" },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4F46E5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
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

/**
 * حساب ميزان ونسبة اكتمال بيانات الصنف (Gamification / Data Completeness Score)
 */
fun getCompletenessTone(score: Int): CompletenessTone = when {
    score <= 30 -> CompletenessTone.RED
    score in 31..99 -> CompletenessTone.ORANGE
    else -> CompletenessTone.GREEN
}

fun calculatePartCompletenessScore(
    name: String,
    ipn: String,
    categoryId: Long?,
    units: String,
    minimumStock: Double,
    hasNature: Boolean,
    defaultLocationId: Long? = null,
    isTemplateOrVariant: Boolean = false,
    hasAdvancedData: Boolean = false
): Int {
    var score = 0
    if (name.isNotBlank()) score += 15
    if (ipn.isNotBlank()) score += 15
    if (units.isNotBlank()) score += 15
    if (minimumStock > 0.0) score += 15
    if (categoryId != null) score += 10
    if (hasNature) score += 10
    if (defaultLocationId != null) score += 10
    if (isTemplateOrVariant) score += 5
    if (hasAdvancedData) score += 5
    return score
}

fun Part.calculateCompletenessScore(): Int {
    var score = 0
    if (name.isNotBlank()) score += 15
    if (ipn.isNotBlank()) score += 15
    if (units.isNotBlank()) score += 15
    if (minimumStock > 0.0) score += 15
    if (categoryId != null) score += 10
    if (component || assembly || salable) score += 10
    if (defaultLocationId != null) score += 10
    if (isTemplate || variantOfId != null) score += 5
    if (description.isNotBlank() || keywords.isNotBlank() || link.isNotBlank() || imageUrl?.isNotBlank() == true) score += 5
    return score
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

private fun formatStockNumber(value: Double): String {
    return if (value % 1.0 == 0.0) "${value.toLong()}.0" else "$value"
}

/**
 * بطاقة عرض القطعة PartItemCard المعاد هيكلتها وفق لغة تصميم بطاقة التخزين المعتمدة بنظام الطبقتين السفليتين
 */
@Composable
private fun PartItemCard(
    part: Part,
    categoryName: String? = null,
    stockItems: List<StockItem> = emptyList(),
    stockLocations: List<StockLocation> = emptyList(),
    isStarred: Boolean = false,
    onToggleStar: () -> Unit = {},
    onAddStock: ((Part) -> Unit)? = null,
    onEditPart: ((Part) -> Unit)? = null,
    onDeletePart: ((Part) -> Unit)? = null,
    onPrintLabel: ((Part) -> Unit)? = null,
    onShowLocations: ((Part) -> Unit)? = null,
    onShowAllocations: ((Part) -> Unit)? = null,
    onClick: () -> Unit
) {
    val completenessScore = remember(part) { part.calculateCompletenessScore() }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            // ==========================================
            // 1. Header Row (الرأس العلوي للمعلومات الأساسية)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val (natureIcon, natureBg, natureFg) = when {
                    part.assembly -> Triple(Icons.Default.AccountTree, Color(0xFFF3E8FF), Color(0xFF7E22CE))
                    part.salable -> Triple(Icons.Default.ShoppingBag, Color(0xFFE0F2FE), Color(0xFF0369A1))
                    else -> Triple(Icons.Default.Memory, Color(0xFFECFDF5), Color(0xFF047857))
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(natureBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = natureIcon,
                        contentDescription = null,
                        tint = natureFg,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = part.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (part.ipn.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEEF2FF),
                                border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                            ) {
                                Text(
                                    text = "IPN: ${part.ipn}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF3730A3),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }

                        if (!categoryName.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 10.5.sp
                                    ),
                                    color = Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                val colorTone = getCompletenessTone(completenessScore)
                CircularCompletionBadge(
                    percentage = completenessScore,
                    colorTone = colorTone
                )
            }

            if (part.description.isNotBlank()) {
                Text(
                    text = part.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ==========================================
            // Inventory Progress Bar (شريط نسبة المخزون)
            // ==========================================
            val showStockProgressBar = part.units.isNotBlank() && part.minimumStock > 0.0

            if (showStockProgressBar) {
                val maxStock = part.maximumStock
                val isOverstock = maxStock != null && maxStock > 0.0 && part.totalInStock > maxStock
                val isShortage = part.totalInStock < part.minimumStock

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
                            text = "نسبة المخزون:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isOverstock) {
                            val maxStockVal = maxStock
                            val overstockPct = (((part.totalInStock - maxStockVal) / maxStockVal) * 100).toInt()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF3E8FF),
                                    border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                                ) {
                                    Text(
                                        text = "تكدس +$overstockPct%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = Color(0xFF7E22CE),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = "(${part.units} ${formatStockNumber(part.totalInStock)} / ${formatStockNumber(maxStockVal)} max)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF7E22CE)
                                )
                            }
                        } else if (isShortage) {
                            val stockRatio = (part.totalInStock / part.minimumStock).toFloat().coerceIn(0f, 1f)
                            val stockPct = (stockRatio * 100).toInt()
                            Text(
                                text = "$stockPct% (${part.units} ${formatStockNumber(part.totalInStock)} / ${formatStockNumber(part.minimumStock)} min)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFD97706)
                            )
                        } else {
                            val surplusPct = (((part.totalInStock - part.minimumStock) / part.minimumStock) * 100).toInt()
                            Text(
                                text = "100% (+$surplusPct%) (${part.units} ${formatStockNumber(part.totalInStock)} / ${formatStockNumber(part.minimumStock)} min)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF059669)
                            )
                        }
                    }

                    val (barRatio, barColor, trackColor) = when {
                        isOverstock -> Triple(
                            1.0f,
                            Color(0xFF7C3AED),
                            Color(0xFFF3E8FF)
                        )
                        isShortage -> {
                            val ratio = (part.totalInStock / part.minimumStock).toFloat().coerceIn(0f, 1f)
                            Triple(
                                ratio,
                                Color(0xFFD97706),
                                Color(0xFFFEF3C7)
                            )
                        }
                        else -> Triple(
                            1.0f,
                            Color(0xFF2563EB),
                            Color(0xFFDBEAFE)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { barRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = barColor,
                        trackColor = trackColor,
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                }
            }

            // ==========================================
            // Tier 1: Storage Locations & Stock Status Row (الطبقة التشغيلية الأولى)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Storage Locations Button (زر / كبسولة مواقع التخزين - الجهة اليمنى فوق زر استلام مخزون)
                val partStockItems = remember(stockItems, part.id) { stockItems.filter { it.partId == part.id } }
                val uniqueLocIds = remember(partStockItems) { partStockItems.mapNotNull { it.locationId }.distinct() }
                val locationsCount = uniqueLocIds.size
                val locationLabel = remember(uniqueLocIds, stockLocations) {
                    when {
                        locationsCount == 1 -> {
                            val locName = stockLocations.find { it.id == uniqueLocIds.first() }?.name
                            if (locName != null) "📍 $locName ▾" else "📍 1 موقع ▾"
                        }
                        locationsCount > 1 -> "📍 $locationsCount مواقع ▾"
                        else -> "📍 0 مواقع ▾"
                    }
                }

                Surface(
                    onClick = { onShowLocations?.invoke(part) },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                    modifier = Modifier
                        .height(40.dp)
                        .defaultMinSize(minWidth = 132.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = locationLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = Color(0xFF3730A3)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 2. Stock Breakdown Badge (ترقية شارة المتاح والمحجوز إلى مركز تحكم تفاعلي)
                Surface(
                    onClick = { onShowAllocations?.invoke(part) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (part.isLowStock) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, if (part.isLowStock) Color(0xFFFCA5A5) else Color(0xFFBBF7D0)),
                    modifier = Modifier
                        .height(40.dp)
                        .weight(1f, fill = false)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val reserved = part.committedAllocated
                        val badgeText = if (reserved > 0) {
                            "المتاح: ${part.availableStock} | محجوز: $reserved"
                        } else {
                            "المتاح: ${part.availableStock}"
                        }

                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (part.isLowStock) Color(0xFFDC2626) else Color(0xFF047857)
                        )

                        if (part.isLowStock) {
                            Text(
                                text = "⚠️ < الحد",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = Color(0xFFDC2626)
                            )
                        }

                        Text(
                            text = "▾",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (part.isLowStock) Color(0xFFDC2626) else Color(0xFF047857)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ==========================================
            // Tier 2: Actions & Operations Row (الطبقة السفلية الثانية)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Primary Operational Action (الجهة اليمنى في الواجهة العربية): [+ استلام مخزون]
                Button(
                    onClick = { onAddStock?.invoke(part) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(40.dp)
                        .defaultMinSize(minWidth = 132.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "استلام مخزون",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "استلام مخزون",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // 2. Controls & Management (الجهة اليسرى في الواجهة العربية)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite Star Capsule
                    Surface(
                        onClick = onToggleStar,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isStarred) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isStarred) Color(0xFFFDE68A) else Color(0xFFE2E8F0)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "تفضيل",
                                tint = if (isStarred) Color(0xFFD97706) else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            if (isStarred) {
                                Text(
                                    text = "مفضلة",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp
                                    ),
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                    }

                    // Barcode / QR Label Action
                    Surface(
                        onClick = { onPrintLabel?.invoke(part) },
                        shape = CircleShape,
                        color = Color(0xFFF3E8FF),
                        border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "ملصق وباركود القطعة",
                                tint = Color(0xFF7E22CE),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Edit Action
                    Surface(
                        onClick = { onEditPart?.invoke(part) },
                        shape = CircleShape,
                        color = Color(0xFFEEF2FF),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل القطعة",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Delete Action
                    Surface(
                        onClick = { onDeletePart?.invoke(part) },
                        shape = CircleShape,
                        color = Color(0xFFFDE8E8),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "حذف القطعة",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * النافذة السفلية لاستعراض أماكن تخزين القطعة ومساراتها المخزنية مع خيار النقل السريع
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartStorageLocationsBottomSheet(
    part: Part,
    stockItems: List<StockItem>,
    locations: List<StockLocation>,
    onDismiss: () -> Unit,
    onInitiateTransfer: (StockItem) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
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
                        text = "أماكن تخزين القطعة",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${part.name} ${if (part.ipn.isNotBlank()) "(IPN: ${part.ipn})" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
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

            HorizontalDivider(color = Color(0xFFE2E8F0))

            val partStockItems = remember(stockItems, part.id) { stockItems.filter { it.partId == part.id } }

            if (partStockItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📍 لا توجد وحدات مخزنية مسجلة لهذه القطعة حالياً",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "يمكنك إضافة شحنة مخزون جديدة عبر زر 'استلام مخزون'",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(partStockItems, key = { index, item -> "part-stock-${item.id}-$index" }) { _, stockItem ->
                        val location = locations.find { it.id == stockItem.locationId }
                        val fullPath = remember(stockItem.locationId, locations) {
                            if (stockItem.locationId == null) "موقع غير محدد"
                            else getBreadcrumbPath(stockItem.locationId, locations).ifBlank { location?.name ?: "موقع #${stockItem.locationId}" }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "📍 $fullPath",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = Color(0xFF1E293B)
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "الكمية: ${stockItem.quantity} ${part.units}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            ),
                                            color = Color(0xFF047857)
                                        )

                                        if (stockItem.batch.isNotBlank()) {
                                            Text(
                                                text = "دفعة: ${stockItem.batch}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = Color(0xFF475569)
                                            )
                                        }

                                        if (stockItem.serial.isNotBlank()) {
                                            Text(
                                                text = "تسلسلي: ${stockItem.serial}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = Color(0xFF475569)
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { onInitiateTransfer(stockItem) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFEEF2FF),
                                        contentColor = Color(0xFF4F46E5)
                                    ),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "⇄ نقل",
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

    return pathNames.joinToString(" > ")
}

@Composable
private fun ConfirmDeletePartDialog(
    partName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "حذف القطعة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Text(
                text = "هل أنت تأكد من رغبتك في حذف القطعة \"$partName\"؟ ستتم أرشفة القطعة وقد تتأثر السجلات المرتبطة بها.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("تأكيد الحذف", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun BadgeTag(text: String, bgColor: Color, textColor: Color) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectCategoryBottomSheet(
    categories: List<PartCategory>,
    selectedCatId: Long?,
    onCreateCategory: ((name: String, description: String) -> PartCategory)? = null,
    onDeleteCategory: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSelect: (PartCategory?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var isQuickAddDialogOpen by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<PartCategory?>(null) }
    var currentSelectedCatId by remember(selectedCatId) { mutableStateOf(selectedCatId) }

    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories
        else categories.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
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
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5)
                    )
                    Text(
                        text = "اختر التصنيف (Category)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color(0xFF0F172A)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onCreateCategory != null) {
                        TextButton(
                            onClick = { isQuickAddDialogOpen = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصنيف جديد", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم التصنيف أو الوصف...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF64748B)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "مسح",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (onCreateCategory != null) {
                    item {
                        Surface(
                            onClick = { isQuickAddDialogOpen = true },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.5.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
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
                                            .background(Color(0xFF059669)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "+ إنشاء تصنيف جديد سريع",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            color = Color(0xFF065F46)
                                        )
                                        Text(
                                            text = "إضافة تصنيف جديد عاجل إلى شجرة النظام",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF059669)
                                )
                            }
                        }
                    }
                }

                item {
                    val isGeneralSelected = currentSelectedCatId == null
                    Surface(
                        onClick = {
                            currentSelectedCatId = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isGeneralSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isGeneralSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isGeneralSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = if (isGeneralSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "بدون تصنيف (عام)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "صنف عام غير مرتبط بفيئة محددة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Checkbox(
                                checked = isGeneralSelected,
                                onCheckedChange = { currentSelectedCatId = null },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4F46E5),
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                itemsIndexed(filteredCategories, key = { index, cat -> "cat-${cat.id}-$index" }) { _, cat ->
                    val isSelected = cat.id == currentSelectedCatId
                    Surface(
                        onClick = {
                            currentSelectedCatId = if (isSelected) null else cat.id
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = cat.description.ifBlank { "تصنيف فرعي في النظام" },
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (onDeleteCategory != null) {
                                    IconButton(
                                        onClick = { categoryToDelete = cat },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "حذف التصنيف",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        currentSelectedCatId = if (isSelected) null else cat.id
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF4F46E5),
                                        uncheckedColor = Color(0xFF94A3B8)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    onSelect(categories.find { it.id == currentSelectedCatId })
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "تم (تأكيد الاختيار)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }

    if (categoryToDelete != null) {
        val catToDelete = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                    Text("تأكيد حذف التصنيف", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "هل أنت تأكد من رغبتك في حذف تصنيف \"${catToDelete.name}\"؟ لن تؤثر العملية على القطع القائمة بل ستصبح تصنيفاتها (عام).",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory?.invoke(catToDelete.id)
                        if (selectedCatId == catToDelete.id) {
                            onSelect(null)
                        }
                        categoryToDelete = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (isQuickAddDialogOpen && onCreateCategory != null) {
        QuickAddCategoryBottomSheet(
            onDismiss = { isQuickAddDialogOpen = false },
            onConfirm = { name, desc ->
                val created = onCreateCategory.invoke(name, desc)
                onSelect(created)
                isQuickAddDialogOpen = false
                onDismiss()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectTemplatePartBottomSheet(
    templateParts: List<Part>,
    isTemplate: Boolean,
    isStandaloneSelected: Boolean,
    selectedVariantOfId: Long?,
    onDismiss: () -> Unit,
    onSelect: (isTemplate: Boolean, isStandalone: Boolean, selectedTemplate: Part?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var currentIsTemplate by remember(isTemplate, isStandaloneSelected, selectedVariantOfId) { mutableStateOf(isTemplate) }
    var currentIsStandalone by remember(isTemplate, isStandaloneSelected, selectedVariantOfId) { mutableStateOf(isStandaloneSelected) }
    var currentSelectedVariantOfId by remember(isTemplate, isStandaloneSelected, selectedVariantOfId) { mutableStateOf(selectedVariantOfId) }

    val filteredTemplates = remember(templateParts, searchQuery) {
        if (searchQuery.isBlank()) templateParts
        else templateParts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.ipn.contains(searchQuery, ignoreCase = true)
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "اختر الصنف القالب الأصل",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم القالب أو رقم IPN...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF64748B)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                // 1. الخيار الأول: تعيين كقالب جديد (isTemplate)
                item {
                    val isNewTemplateSelected = currentIsTemplate && !currentIsStandalone && currentSelectedVariantOfId == null
                    Surface(
                        onClick = {
                            currentIsTemplate = true
                            currentIsStandalone = false
                            currentSelectedVariantOfId = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isNewTemplateSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isNewTemplateSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isNewTemplateSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Extension,
                                        contentDescription = null,
                                        tint = if (isNewTemplateSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "تعيين كقالب جديد",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "اعتماد هذا الصنف كقالب تجريدي تُشتق منه قطع ومتغيرات فرعية أخرى",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Checkbox(
                                checked = isNewTemplateSelected,
                                onCheckedChange = {
                                    currentIsTemplate = true
                                    currentIsStandalone = false
                                    currentSelectedVariantOfId = null
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4F46E5),
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                // 2. الخيار الثاني: صنف أصلي مستقل (بدون قالب أصل)
                item {
                    val isStandalone = currentIsStandalone && !currentIsTemplate && currentSelectedVariantOfId == null
                    Surface(
                        onClick = {
                            currentIsTemplate = false
                            currentIsStandalone = true
                            currentSelectedVariantOfId = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isStandalone) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isStandalone) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isStandalone) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Extension,
                                        contentDescription = null,
                                        tint = if (isStandalone) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "صنف أصلي مستقل (بدون قالب أصل)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "قطعة قائمة بذاتها وغير مشتقة من قالب آخر",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Checkbox(
                                checked = isStandalone,
                                onCheckedChange = {
                                    currentIsTemplate = false
                                    currentIsStandalone = true
                                    currentSelectedVariantOfId = null
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4F46E5),
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                // 3. الخيارات المشتقة من قوالب أصلية أخرى قائمة
                itemsIndexed(filteredTemplates, key = { index, t -> "tpl-${t.id}-$index" }) { _, t ->
                    val isSelected = !currentIsTemplate && !currentIsStandalone && t.id == currentSelectedVariantOfId
                    Surface(
                        onClick = {
                            currentIsTemplate = false
                            currentIsStandalone = false
                            currentSelectedVariantOfId = t.id
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Extension,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = t.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "IPN: ${t.ipn.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    currentIsTemplate = false
                                    currentIsStandalone = false
                                    currentSelectedVariantOfId = t.id
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4F46E5),
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val selectedTpl = if (currentIsTemplate || currentIsStandalone) null else templateParts.find { it.id == currentSelectedVariantOfId }
                    onSelect(currentIsTemplate, currentIsStandalone, selectedTpl)
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "تم (تأكيد الاختيار)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

private enum class PartRoleOption {
    RAW_COMPONENT,    // مكوّن أولي / مادة خام (شراء + مكون فرعي)
    ASSEMBLY_PRODUCT, // منتج مجمّع / تصنيع داخلي (تجميع + بيع)
    FINISHED_GOOD,   // منتج تجاري جاهز (شراء + بيع)
    CUSTOM            // تخصيص مخصص
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectPartNatureOptionsBottomSheet(
    purchaseable: Boolean,
    salable: Boolean,
    component: Boolean,
    assembly: Boolean,
    isTemplate: Boolean,
    virtual: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        purchaseable: Boolean,
        salable: Boolean,
        component: Boolean,
        assembly: Boolean,
        isTemplate: Boolean,
        virtual: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var p by remember { mutableStateOf(purchaseable) }
    var s by remember { mutableStateOf(salable) }
    var c by remember { mutableStateOf(component) }
    var a by remember { mutableStateOf(assembly) }
    var t by remember { mutableStateOf(isTemplate) }
    var v by remember { mutableStateOf(virtual) }

    // تحديد الدور الأولي تلقائياً بناءً على الأعلام المحددة
    var activeRole by remember {
        mutableStateOf(
            when {
                p && c && !a && !s -> PartRoleOption.RAW_COMPONENT
                a && s && !p && !c -> PartRoleOption.ASSEMBLY_PRODUCT
                p && s && !a && !c -> PartRoleOption.FINISHED_GOOD
                else -> PartRoleOption.CUSTOM
            }
        )
    }

    var showCustomFlags by remember { mutableStateOf(activeRole == PartRoleOption.CUSTOM) }
    var showAdvancedOptions by remember { mutableStateOf(t || v) }

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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Section
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
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "طبيعة التعامل والدور الوظيفي للصنف",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "اختر طبيعة استخدام الصنف في النظام لتشغيل الأعلام تلقائياً",
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

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. مكوّن أولي / مادة خام (Raw Component)
                val isRawSelected = activeRole == PartRoleOption.RAW_COMPONENT
                Surface(
                    onClick = {
                        activeRole = PartRoleOption.RAW_COMPONENT
                        p = true
                        c = true
                        a = false
                        s = false
                        showCustomFlags = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isRawSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.5.dp,
                        if (isRawSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isRawSelected) Color(0xFFE0E7FF) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = if (isRawSelected) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "مكوّن أولي / مادة خام (Raw Component)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isRawSelected) Color(0xFF312E81) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "قطعة نشتريها من مورد وتدخل في تجميع المكونات (مثل: مقاوم، برغي، سلك)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        RadioButton(
                            selected = isRawSelected,
                            onClick = {
                                activeRole = PartRoleOption.RAW_COMPONENT
                                p = true
                                c = true
                                a = false
                                s = false
                                showCustomFlags = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                        )
                    }
                }

                // 2. منتج مجمّع / تصنيع داخلي (Assembly Product)
                val isAssemblySelected = activeRole == PartRoleOption.ASSEMBLY_PRODUCT
                Surface(
                    onClick = {
                        activeRole = PartRoleOption.ASSEMBLY_PRODUCT
                        a = true
                        s = true
                        p = false
                        c = false
                        showCustomFlags = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isAssemblySelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.5.dp,
                        if (isAssemblySelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isAssemblySelected) Color(0xFFE0E7FF) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = if (isAssemblySelected) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "منتج مجمّع / تصنيع داخلي (Assembly)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isAssemblySelected) Color(0xFF312E81) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "لوحة أو جهاز نصنعه محلياً من عدة مكونات ويمتلك قائمة مواد (BOM)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        RadioButton(
                            selected = isAssemblySelected,
                            onClick = {
                                activeRole = PartRoleOption.ASSEMBLY_PRODUCT
                                a = true
                                s = true
                                p = false
                                c = false
                                showCustomFlags = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                        )
                    }
                }

                // 3. منتج تجاري جاهز (Finished / Trading Good)
                val isFinishedSelected = activeRole == PartRoleOption.FINISHED_GOOD
                Surface(
                    onClick = {
                        activeRole = PartRoleOption.FINISHED_GOOD
                        p = true
                        s = true
                        a = false
                        c = false
                        showCustomFlags = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isFinishedSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.5.dp,
                        if (isFinishedSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isFinishedSelected) Color(0xFFE0E7FF) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = if (isFinishedSelected) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "منتج تجاري جاهز (Finished Goods)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isFinishedSelected) Color(0xFF312E81) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "منتج نشتريه جاهزاً ونعيد بيعه مباشرة للعملاء دون تصنيع",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        RadioButton(
                            selected = isFinishedSelected,
                            onClick = {
                                activeRole = PartRoleOption.FINISHED_GOOD
                                p = true
                                s = true
                                a = false
                                c = false
                                showCustomFlags = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                        )
                    }
                }

                // 4. تخصيص مخصص (Custom Selection)
                val isCustomSelected = activeRole == PartRoleOption.CUSTOM
                Surface(
                    onClick = {
                        activeRole = PartRoleOption.CUSTOM
                        showCustomFlags = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCustomSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.5.dp,
                        if (isCustomSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCustomSelected) Color(0xFFE0E7FF) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = if (isCustomSelected) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "تخصيص يدوي مخصص (Custom Role)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isCustomSelected) Color(0xFF312E81) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "تحديد وتخصيص أعلام الشراء والبيع والتجميع بشكل يدوي منفصل",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        RadioButton(
                            selected = isCustomSelected,
                            onClick = {
                                activeRole = PartRoleOption.CUSTOM
                                showCustomFlags = true
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                        )
                    }
                }

                // عرض أعلام التخصيص اليدوي عند اختيار "تخصيص مخصص"
                if (showCustomFlags) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "تعديل الأعلام اليدوية المنفصلة:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF475569)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { p = !p }) {
                                Checkbox(checked = p, onCheckedChange = { p = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5)))
                                Text("قابل للشراء والتوريد (purchaseable)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { s = !s }) {
                                Checkbox(checked = s, onCheckedChange = { s = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5)))
                                Text("قابل للبيع والطلب (salable)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { c = !c }) {
                                Checkbox(checked = c, onCheckedChange = { c = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5)))
                                Text("مكون فرعي في تجميعات (component)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { a = !a }) {
                                Checkbox(checked = a, onCheckedChange = { a = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5)))
                                Text("منتج مجمع وقائمة مواد (assembly)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // قسم الخصائص المتقدمة المطوي (Advanced Characteristics)
                Surface(
                    onClick = { showAdvancedOptions = !showAdvancedOptions },
                    color = Color(0xFFEEF2FF).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
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
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "خصائص هندسية إضافية (تتبع، قالب، خدمة)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                ),
                                color = Color(0xFF3730A3)
                            )
                        }

                        Icon(
                            imageVector = if (showAdvancedOptions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5)
                        )
                    }
                }

                if (showAdvancedOptions) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    ) {
                        // 1. صنف وهمي / خدمة (virtual)
                        Surface(
                            onClick = { v = !v },
                            shape = RoundedCornerShape(12.dp),
                            color = if (v) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (v) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("صنف وهمي / خدمة (virtual)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp))
                                    Text("صنف غير ملموس (خدمة تركيب، ترخيص برمجي)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                                }
                                Checkbox(checked = v, onCheckedChange = { v = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5)))
                            }
                        }

                        // 2. اعتماد كقالب تجريدي أصل (isTemplate)
                        Surface(
                            onClick = { t = !t },
                            shape = RoundedCornerShape(12.dp),
                            color = if (t) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (t) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("اعتماد كقالب تجريدي أصل (is_template)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp))
                                    Text("اعتماد هذا الصنف كقالب لأصناف أخرى (مثل: مقاسات أو ألوان مختلفة)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                                }
                                Checkbox(checked = t, onCheckedChange = { t = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5)))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    onConfirm(p, s, c, a, t, v)
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "تأكيد الطبيعة والخصائص",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectLocationBottomSheet(
    locations: List<StockLocation>,
    selectedLocationId: Long?,
    onDismiss: () -> Unit,
    onSelect: (StockLocation?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var currentSelectedLocationId by remember(selectedLocationId) { mutableStateOf(selectedLocationId) }
    var showScannerInSheet by remember { mutableStateOf(false) }
    var showTypeFilterBar by remember { mutableStateOf(false) }
    var selectedTypeFilter by remember { mutableStateOf<String?>(null) }

    val availableTypes = remember(locations) {
        locations.mapNotNull { it.locationType.ifBlank { null } }.distinct()
    }

    val filteredLocations = remember(locations, searchQuery, selectedTypeFilter) {
        locations.filter { loc ->
            val matchesSearch = searchQuery.isBlank() ||
                loc.name.contains(searchQuery, ignoreCase = true) ||
                loc.description.contains(searchQuery, ignoreCase = true)
            val matchesType = selectedTypeFilter == null ||
                loc.locationType.equals(selectedTypeFilter, ignoreCase = true)
            matchesSearch && matchesType
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "اختر موقع التخزين الافتراضي",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
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

            Spacer(modifier = Modifier.height(8.dp))

            // Search input field with separate standalone Filter & QR scanner buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث باسم الموقع...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "مسح البحث",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // Storage Location Filter Button Box
                Surface(
                    onClick = { showTypeFilterBar = !showTypeFilterBar },
                    shape = RoundedCornerShape(12.dp),
                    color = if (showTypeFilterBar || selectedTypeFilter != null) Color(0xFF4F46E5) else Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "فرز وتصفية المواقع",
                            tint = if (showTypeFilterBar || selectedTypeFilter != null) Color.White else Color(0xFF4F46E5),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Separate Standalone QR Scanner Button Box
                Surface(
                    onClick = { showScannerInSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "مسح رمز الموقع QR",
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Active Filter Badge Indicator Chip
            if (selectedTypeFilter != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "التصفية المطبقة:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color(0xFF64748B)
                    )
                    Surface(
                        color = Color(0xFFEEF2FF),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = selectedTypeFilter!!,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF4F46E5)
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إلغاء الفلتر",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { selectedTypeFilter = null }
                            )
                        }
                    }
                }
            }

            if (showScannerInSheet) {
                LocationBarcodeScannerBottomSheet(
                    locations = locations,
                    onDismiss = { showScannerInSheet = false },
                    onBarcodeScanned = { barcode ->
                        val matchedLoc = locations.find {
                            it.id.toString() == barcode ||
                            it.name.contains(barcode, ignoreCase = true) ||
                            it.uuid == barcode
                        }
                        if (matchedLoc != null) {
                            onSelect(matchedLoc)
                            onDismiss()
                        } else {
                            searchQuery = barcode
                        }
                        showScannerInSheet = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    val isNoneSelected = currentSelectedLocationId == null
                    Surface(
                        onClick = {
                            currentSelectedLocationId = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isNoneSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isNoneSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isNoneSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (isNoneSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "بدون موقع افتراضي",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "يتم تحديد الموقع عند استلام الشحنات وتخزينها",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Checkbox(
                                checked = isNoneSelected,
                                onCheckedChange = { currentSelectedLocationId = null },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4F46E5),
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                itemsIndexed(filteredLocations, key = { index, loc -> "loc-picker-${loc.id}-$index" }) { _, loc ->
                    val isSelected = loc.id == currentSelectedLocationId
                    Surface(
                        onClick = {
                            currentSelectedLocationId = if (isSelected) null else loc.id
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = loc.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    if (loc.description.isNotBlank()) {
                                        Text(
                                            text = loc.description,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    currentSelectedLocationId = if (isSelected) null else loc.id
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4F46E5),
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    onSelect(locations.find { it.id == currentSelectedLocationId })
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "تم (تأكيد الاختيار)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }

    // Sub-BottomSheet for Storage Location Type Filtering
    if (showTypeFilterBar) {
        LocationTypeFilterBottomSheet(
            availableTypes = availableTypes,
            selectedTypeFilter = selectedTypeFilter,
            allLocationsCount = locations.size,
            locations = locations,
            onSelectFilter = { type ->
                selectedTypeFilter = type
                showTypeFilterBar = false
            },
            onDismiss = { showTypeFilterBar = false }
        )
    }
}

private fun normalizeLocationTypeCode(typeCode: String): String {
    return when (typeCode.uppercase().trim()) {
        "AREA", "LINE" -> "ZONE"
        "RACK" -> "SHELF"
        "BOX", "CONTAINER", "DRAWER" -> "BIN"
        else -> typeCode.uppercase().trim()
    }
}

private fun getArabicLocationTypeLabel(typeCode: String): String {
    return when (normalizeLocationTypeCode(typeCode)) {
        "SITE" -> "📍 المنشأة / الموقع المادي (Site)"
        "WAREHOUSE" -> "🏢 المستودع الرئيسي (Warehouse)"
        "ZONE" -> "🧩 المنطقة / الساحة (Zone / Area)"
        "AISLE" -> "🚪 الممر (Aisle)"
        "SHELF" -> "📐 رف التخزين (Shelf / Rack)"
        "BIN" -> "📥 الصندوق / الحاوية (Box / Bin / Drawer)"
        else -> typeCode.ifBlank { "موقع تخزين" }
    }
}

private data class LocationHierarchyFilterOption(
    val code: String,
    val arabicLabel: String,
    val count: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationTypeFilterBottomSheet(
    availableTypes: List<String>,
    selectedTypeFilter: String?,
    allLocationsCount: Int,
    locations: List<StockLocation>,
    onSelectFilter: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val standardHierarchyCodes = remember { listOf("SITE", "WAREHOUSE", "ZONE", "AISLE", "SHELF", "BIN") }

    val hierarchyOptions = remember(locations, availableTypes) {
        val options = mutableListOf<LocationHierarchyFilterOption>()

        // Add standard location hierarchy levels in order
        standardHierarchyCodes.forEach { code ->
            val count = locations.count { normalizeLocationTypeCode(it.locationType) == code }
            options.add(
                LocationHierarchyFilterOption(
                    code = code,
                    arabicLabel = getArabicLocationTypeLabel(code),
                    count = count
                )
            )
        }

        // Add any custom/other types present in locations that aren't in the standard 6 codes
        availableTypes.forEach { rawType ->
            val norm = normalizeLocationTypeCode(rawType)
            if (norm !in standardHierarchyCodes && rawType.isNotBlank()) {
                val count = locations.count { it.locationType.equals(rawType, ignoreCase = true) }
                options.add(
                    LocationHierarchyFilterOption(
                        code = rawType,
                        arabicLabel = getArabicLocationTypeLabel(rawType),
                        count = count
                    )
                )
            }
        }
        options
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "تصفية حسب نوع ومستوى موقع التخزين",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "حدد مستوى التصفية (منشأة ➔ مستودع ➔ منطقة ➔ ممر ➔ رف ➔ صندوق)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Option 1: All Locations (Reset Filter)
            val isAllSelected = selectedTypeFilter == null
            Surface(
                onClick = {
                    onSelectFilter(null)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                color = if (isAllSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isAllSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isAllSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isAllSelected) Color(0xFF4F46E5) else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "جميع المواقع التخزينية (عرض الكل)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            color = if (isAllSelected) Color(0xFF4F46E5) else Color(0xFF0F172A)
                        )
                    }
                    Surface(
                        color = Color(0xFFE0E7FF),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$allLocationsCount موقع",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF4338CA),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Location Types Hierarchy List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                items(hierarchyOptions) { opt ->
                    val isSelected = selectedTypeFilter?.let {
                        normalizeLocationTypeCode(it) == normalizeLocationTypeCode(opt.code) ||
                        it.equals(opt.code, ignoreCase = true)
                    } ?: false

                    Surface(
                        onClick = {
                            if (isSelected) {
                                // Toggle off on re-click
                                onSelectFilter(null)
                            } else {
                                onSelectFilter(opt.code)
                            }
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF4F46E5) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = opt.arabicLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF0F172A)
                                )
                            }
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${opt.count}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF64748B),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddPartBottomSheet(
    partToEdit: Part? = null,
    categories: List<PartCategory> = emptyList(),
    locations: List<StockLocation> = emptyList(),
    templateParts: List<Part> = emptyList(),
    allParts: List<Part> = emptyList(),
    existingAttachments: List<PartAttachment> = emptyList(),
    onCreateCategory: ((name: String, description: String) -> PartCategory)? = null,
    onDeleteCategory: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit,
    onUpdate: ((Part, List<PendingAttachment>) -> Unit)? = null,
    onConfirm: (
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
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // المستوى الأول: الحقول الأساسية الظاهرة والمباشرة الستة
    var name by remember(partToEdit) { mutableStateOf(partToEdit?.name ?: "") }
    var selectedCatId by remember(partToEdit) { mutableStateOf(partToEdit?.categoryId) }

    val ipnShakeController = remember { ShakeController() }
    var generatedIpn by remember(partToEdit, selectedCatId) {
        mutableStateOf(
            if (partToEdit != null && partToEdit.ipn.isNotBlank()) partToEdit.ipn
            else SmartIpnGenerator.generateNextIpn(selectedCatId, allParts, categories)
        )
    }
    var manualIpnText by remember(partToEdit) { mutableStateOf("") }
    var isManuallyEditedIpn by remember { mutableStateOf(false) }
    var isIpnConfirmed by remember { mutableStateOf(partToEdit != null && partToEdit.ipn.isNotBlank()) }
    var hasDuplicateIpnError by remember { mutableStateOf(false) }

    val ipn = if (isManuallyEditedIpn) manualIpnText else generatedIpn
    var units by remember(partToEdit) { mutableStateOf(partToEdit?.units ?: "pcs") }
    var minimumStockText by remember(partToEdit) { mutableStateOf(partToEdit?.minimumStock?.let { if (it == 0.0) "0" else it.toString() } ?: "0") }
    var lowStockAlertsEnabled by remember(partToEdit) { mutableStateOf(true) }

    // طبيعة التعامل والتصنيع: دعم اختيار أكثر من خيار مع الوضع الافتراضي لعدم الاختيار عند إضافة قطعة جديدة
    var isComponentSelected by remember(partToEdit) { mutableStateOf(partToEdit?.component ?: false) }
    var isAssemblySelected by remember(partToEdit) { mutableStateOf(partToEdit?.assembly ?: false) }
    var isSalableSelected by remember(partToEdit) { mutableStateOf(partToEdit?.salable ?: false) }
    var isSelectNatureSheetOpen by remember { mutableStateOf(false) }

    // المستوى الثاني: قسم فرعي مطوي بعنوان "خيارات متقدمة وهندسية" (غير مفعل ومطوي افتراضياً)
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var description by remember(partToEdit) { mutableStateOf(partToEdit?.description ?: "") }
    var maximumStockText by remember(partToEdit) { mutableStateOf(partToEdit?.maximumStock?.toString() ?: "") }
    var revision by remember(partToEdit) { mutableStateOf(partToEdit?.revision ?: "") }
    var keywords by remember(partToEdit) { mutableStateOf(partToEdit?.keywords ?: "") }
    var selectedDefaultLocId by remember(partToEdit) { mutableStateOf(partToEdit?.defaultLocationId) }
    var defaultExpiryDaysText by remember(partToEdit) { mutableStateOf(partToEdit?.defaultExpiryDays?.toString() ?: "") }
    var isTemplate by remember(partToEdit) { mutableStateOf(partToEdit?.isTemplate ?: false) }
    var virtual by remember(partToEdit) { mutableStateOf(partToEdit?.virtual ?: false) }
    var selectedVariantOfId by remember(partToEdit) { mutableStateOf(partToEdit?.variantOfId) }
    var isStandaloneSelected by remember(partToEdit) {
        mutableStateOf(partToEdit != null && !partToEdit.isTemplate && partToEdit.variantOfId == null)
    }

    val pendingAttachments = remember(partToEdit, existingAttachments) {
        val list = mutableStateListOf<PendingAttachment>()
        if (partToEdit != null) {
            if (!partToEdit.imageUrl.isNullOrBlank()) {
                list.add(
                    PendingAttachment(
                        type = AttachmentType.IMAGE,
                        pathOrUrl = partToEdit.imageUrl!!,
                        label = "صورة الصنف: ${partToEdit.imageUrl!!.substringAfterLast('/')}"
                    )
                )
            }
            if (partToEdit.link.isNotBlank()) {
                list.add(
                    PendingAttachment(
                        type = AttachmentType.LINK,
                        pathOrUrl = partToEdit.link,
                        label = partToEdit.link
                    )
                )
            }
            existingAttachments.forEach { att ->
                if (!att.attachment.isNullOrBlank()) {
                    val isImage = att.attachment!!.contains(".png", true) ||
                            att.attachment!!.contains(".jpg", true) ||
                            att.attachment!!.contains(".jpeg", true) ||
                            att.attachment!!.contains(".webp", true) ||
                            att.attachment!!.contains(".gif", true)
                    val attType = if (isImage) AttachmentType.IMAGE else AttachmentType.DOCUMENT
                    if (list.none { it.pathOrUrl == att.attachment && it.type == attType }) {
                        list.add(
                            PendingAttachment(
                                id = att.id,
                                type = attType,
                                pathOrUrl = att.attachment!!,
                                label = att.comment.ifBlank { att.attachment!!.substringAfterLast('/') }
                            )
                        )
                    }
                }
                if (!att.link.isNullOrBlank()) {
                    if (list.none { it.pathOrUrl == att.link && it.type == AttachmentType.LINK }) {
                        list.add(
                            PendingAttachment(
                                id = att.id,
                                type = AttachmentType.LINK,
                                pathOrUrl = att.link!!,
                                label = att.comment.ifBlank { att.link!! }
                            )
                        )
                    }
                }
            }
        }
        list
    }

    val platformPickerLaunchers = rememberPlatformPickerLaunchers(
        onImageCaptured = { path ->
            pendingAttachments.add(
                PendingAttachment(
                    type = AttachmentType.IMAGE,
                    pathOrUrl = path,
                    label = "صورة كاميرا: ${path.substringAfterLast('/')}"
                )
            )
        },
        onImagePicked = { path ->
            pendingAttachments.add(
                PendingAttachment(
                    type = AttachmentType.IMAGE,
                    pathOrUrl = path,
                    label = "صورة معرض: ${path.substringAfterLast('/')}"
                )
            )
        },
        onFilePicked = { path ->
            pendingAttachments.add(
                PendingAttachment(
                    type = AttachmentType.DOCUMENT,
                    pathOrUrl = path,
                    label = "مستند: ${path.substringAfterLast('/')}"
                )
            )
        }
    )

    var isSelectCategorySheetOpen by remember { mutableStateOf(false) }
    var isQuickAddCategoryOpen by remember { mutableStateOf(false) }
    var isSelectTemplateSheetOpen by remember { mutableStateOf(false) }
    var isSelectUnitSheetOpen by remember { mutableStateOf(false) }
    var isSelectLocationSheetOpen by remember { mutableStateOf(false) }
    var isLinkInputDialogOpen by remember { mutableStateOf(false) }
    var isConfirmUpdateDialogOpen by remember { mutableStateOf(false) }

    // حساب ذكي لتقصي وتتبع أي تغيير فعلي في البيانات الحالية عند التعديل
    val hasChanges = remember(
        partToEdit, name, ipn, description, selectedCatId, units,
        isComponentSelected, isAssemblySelected, isSalableSelected,
        minimumStockText, maximumStockText, revision, keywords, selectedDefaultLocId,
        defaultExpiryDaysText, pendingAttachments.size, isTemplate, virtual, selectedVariantOfId
    ) {
        if (partToEdit == null) true
        else {
            val minStock = minimumStockText.toDoubleOrNull() ?: 0.0
            val maxStock = maximumStockText.toDoubleOrNull()
            val expiryDays = defaultExpiryDaysText.toIntOrNull()

            name.trim() != partToEdit.name.trim() ||
                    ipn.trim() != partToEdit.ipn.trim() ||
                    description.trim() != partToEdit.description.trim() ||
                    selectedCatId != partToEdit.categoryId ||
                    units.trim().ifBlank { "pcs" } != partToEdit.units.trim() ||
                    isComponentSelected != partToEdit.component ||
                    isAssemblySelected != partToEdit.assembly ||
                    isSalableSelected != partToEdit.salable ||
                    minStock != partToEdit.minimumStock ||
                    maxStock != partToEdit.maximumStock ||
                    revision.trim() != partToEdit.revision.trim() ||
                    keywords.trim() != partToEdit.keywords.trim() ||
                    selectedDefaultLocId != partToEdit.defaultLocationId ||
                    expiryDays != partToEdit.defaultExpiryDays ||
                    isTemplate != partToEdit.isTemplate ||
                    virtual != partToEdit.virtual ||
                    selectedVariantOfId != partToEdit.variantOfId
        }
    }

    val isFormValid = remember(name, ipn, hasChanges) {
        name.trim().isNotBlank() && ipn.trim().isNotBlank() && hasChanges
    }

    val selectedCategory = remember(selectedCatId, categories) {
        categories.find { it.id == selectedCatId }
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
                            imageVector = if (partToEdit != null) Icons.Default.Edit else Icons.Default.AddBox,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    val completenessScore = remember(
                        name, ipn, selectedCatId, units, minimumStockText,
                        isComponentSelected, isAssemblySelected, isSalableSelected,
                        selectedDefaultLocId, isTemplate, selectedVariantOfId,
                        description, keywords, pendingAttachments.size
                    ) {
                        calculatePartCompletenessScore(
                            name = name,
                            ipn = ipn,
                            categoryId = selectedCatId,
                            units = units,
                            minimumStock = minimumStockText.toDoubleOrNull() ?: 0.0,
                            hasNature = isComponentSelected || isAssemblySelected || isSalableSelected,
                            defaultLocationId = selectedDefaultLocId,
                            isTemplateOrVariant = isTemplate || selectedVariantOfId != null,
                            hasAdvancedData = description.isNotBlank() || keywords.isNotBlank() || pendingAttachments.isNotEmpty()
                        )
                    }

                    Column {
                        Text(
                            text = if (partToEdit != null) "تعديل بيانات القطعة - ${partToEdit.name}" else stringResource(Res.string.add_new_part),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Completeness Ring Badge
                        val colorTone = getCompletenessTone(completenessScore)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CircularCompletionBadge(
                                percentage = completenessScore,
                                colorTone = colorTone,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "اكتمال البيانات: $completenessScore%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = when (colorTone) {
                                    CompletenessTone.RED -> Color(0xFF991B1B)
                                    CompletenessTone.ORANGE -> Color(0xFF92400E)
                                    CompletenessTone.GREEN -> Color(0xFF065F46)
                                }
                            )
                        }
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. اسم القطعة + 2. التصنيف (في نفس السطر)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // حقل اسم القطعة - إلزامي
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم القطعة *") },
                        placeholder = { Text("مثال: مقاومة 10K") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    // حقل التصنيف (Category)
                    Surface(
                        onClick = { isSelectCategorySheetOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedCategory != null) Color(0xFFF8FAFC) else Color.White,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selectedCategory != null) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEEF2FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.Center) {
                                    Text(
                                        text = "التصنيف",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    )
                                    Text(
                                        text = selectedCategory?.name ?: "عام (اختر التصنيف)",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "اختر التصنيف",
                                tint = Color(0xFF4F46E5)
                            )
                        }
                    }
                }

                // 3. رقم القطعة الداخلي الذكي (Smart IPN Field)
                SmartIpnField(
                    generatedIpn = generatedIpn,
                    manualIpn = manualIpnText,
                    onIpnChange = { newIpn ->
                        manualIpnText = newIpn
                        isManuallyEditedIpn = true
                        hasDuplicateIpnError = false
                    },
                    isConfirmed = isIpnConfirmed,
                    onConfirmToggle = {
                        isIpnConfirmed = !isIpnConfirmed
                    },
                    isManuallyEdited = isManuallyEditedIpn,
                    onReset = {
                        manualIpnText = ""
                        isManuallyEditedIpn = false
                        isIpnConfirmed = false
                        hasDuplicateIpnError = false
                    },
                    onGenerate = {
                        generatedIpn = SmartIpnGenerator.generateNextIpn(selectedCatId, allParts, categories)
                        manualIpnText = ""
                        isManuallyEditedIpn = false
                        isIpnConfirmed = true
                        hasDuplicateIpnError = false
                    },
                    hasDuplicateError = hasDuplicateIpnError,
                    shakeController = ipnShakeController,
                    label = "رقم القطعة الداخلي (IPN) *",
                    placeholder = "مثال: ELEC-RES-0001"
                )

                // 4. وحدة القياس (زر اختيار الهدف بدون خصائص حقل النص) + 5. الحد الأدنى للمخزون
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { isSelectUnitSheetOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "وحدة القياس",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF64748B)
                                    )
                                )
                                Text(
                                    text = units.ifBlank { "pcs" },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "اختر الوحدة",
                                tint = Color(0xFF4F46E5)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = minimumStockText,
                        onValueChange = { minimumStockText = it },
                        label = { Text("الحد الأدنى للمخزون") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }

                // 6. طبيعة التعامل والتصنيع (نوع الصنف) - حقل يفتح نافذة منبثقة للخيارات الثلاثة
                val selectedNaturesList = remember(isComponentSelected, isAssemblySelected, isSalableSelected) {
                    buildList {
                        if (isComponentSelected) add("مكوّن أولي خام (Component)")
                        if (isAssemblySelected) add("تجميع داخلي (Assembly)")
                        if (isSalableSelected) add("منتج تجاري جاهز للبيع (Salable)")
                    }
                }

                Surface(
                    onClick = { isSelectNatureSheetOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedNaturesList.isNotEmpty()) Color(0xFFF8FAFC) else Color.White,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (selectedNaturesList.isNotEmpty()) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selectedNaturesList.isNotEmpty()) Color(0xFFEEF2FF) else Color(0xFFF1F5F9))
                                    .border(1.dp, if (selectedNaturesList.isNotEmpty()) Color(0xFFC7D2FE) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = if (selectedNaturesList.isNotEmpty()) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = if (selectedNaturesList.isNotEmpty()) selectedNaturesList.joinToString("، ") else "طبيعة التعامل والتصنيع (نوع الصنف)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (selectedNaturesList.isNotEmpty()) Color(0xFF0F172A) else Color(0xFF475569),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (selectedNaturesList.isNotEmpty()) "طبيعة التعامل والتصنيع: اضغط للتعديل" else "اضغط لاختيار طبيعة تصنيع وتعامل الصنف",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            color = if (selectedNaturesList.isNotEmpty()) Color(0xFFEEF2FF) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (selectedNaturesList.isNotEmpty()) Color(0xFFC7D2FE) else Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = if (selectedNaturesList.isNotEmpty()) "تغيير ▾" else "اختر ▾",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedNaturesList.isNotEmpty()) Color(0xFF4F46E5) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // 7. اختيار الصنف القالب/الأصل (variantOfId / isTemplate)
                val selectedTemplate = remember(selectedVariantOfId, templateParts) {
                    templateParts.find { it.id == selectedVariantOfId }
                }
                val isTemplateCardActive = isTemplate || selectedTemplate != null || isStandaloneSelected

                Surface(
                    onClick = { isSelectTemplateSheetOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isTemplateCardActive) Color(0xFFF8FAFC) else Color.White,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (isTemplateCardActive) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isTemplateCardActive) Color(0xFFEEF2FF) else Color(0xFFF1F5F9))
                                    .border(1.dp, if (isTemplateCardActive) Color(0xFFC7D2FE) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = if (isTemplateCardActive) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = when {
                                        isTemplate -> "قالب أصل"
                                        selectedTemplate != null -> "مشتق عن: ${selectedTemplate.name}"
                                        isStandaloneSelected -> "صنف أصلي مستقل"
                                        else -> "القالب / صنف مشتق"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isTemplateCardActive) Color(0xFF0F172A) else Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        isTemplate -> "مُعتمد كقالب تجريدي تُشتق منه متغيرات فرعية"
                                        selectedTemplate != null -> "صنف فرعي مشتق عن قالب أصل (IPN: ${selectedTemplate.ipn.ifBlank { "-" }})"
                                        isStandaloneSelected -> "قطعة قائمة بذاتها وغير مشتقة من قالب آخر"
                                        else -> "اضغط لاختيار صنف أصلي مستقل، قالب أصل، أو صنف مشتق"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            color = if (isTemplateCardActive) Color(0xFFEEF2FF) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isTemplateCardActive) Color(0xFFC7D2FE) else Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = if (isTemplateCardActive) "تغيير ▾" else "اختر ▾",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isTemplateCardActive) Color(0xFF4F46E5) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // 8. موقع التخزين الافتراضي (defaultLocationId) - الخيارات الأساسية
                if (locations.isNotEmpty()) {
                    val selectedLocation = remember(selectedDefaultLocId, locations) {
                        locations.find { it.id == selectedDefaultLocId }
                    }

                    Surface(
                        onClick = { isSelectLocationSheetOpen = true },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selectedLocation != null) Color(0xFFF8FAFC) else Color.White,
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = if (selectedLocation != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selectedLocation != null) Color(0xFFEEF2FF) else Color(0xFFF1F5F9))
                                        .border(1.dp, if (selectedLocation != null) Color(0xFFC7D2FE) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (selectedLocation != null) Color(0xFF4F46E5) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = selectedLocation?.name ?: "موقع التخزين الافتراضي",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (selectedLocation != null) Color(0xFF0F172A) else Color(0xFF475569),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = selectedLocation?.description?.ifBlank { "موقع التخزين الافتراضي" } ?: "اضغط لاختيار موقع التخزين الافتراضي للقطعة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Surface(
                                color = if (selectedLocation != null) Color(0xFFEEF2FF) else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selectedLocation != null) Color(0xFFC7D2FE) else Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = if (selectedLocation != null) "تغيير ▾" else "اختر ▾",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedLocation != null) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // =========================================================
                // المستوى الثاني: قسم فرعي مطوي بعنوان "خيارات متقدمة وهندسية"
                // =========================================================
                Surface(
                    onClick = { isAdvancedExpanded = !isAdvancedExpanded },
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "خيارات متقدمة وهندسية",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "القوالب المشتقة، الصور، الوسوم، الموقع، والإصدار",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5)
                        )
                    }
                }

                if (isAdvancedExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. المرفقات والمستندات السريعة (كاميرا، معرض الصور، ملفات، رابط) - في أعلى القسم المتقدم
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "إرفاق الصور والمستندات والروابط :",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF0F172A)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. الكاميرا
                                Surface(
                                    onClick = { platformPickerLaunchers.launchCamera() },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFFE4E6),
                                    border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(70.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = "الكاميرا",
                                            tint = Color(0xFFE11D48),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "الكاميرا",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF9F1239)
                                            )
                                        )
                                    }
                                }

                                // 2. معرض الصور
                                Surface(
                                    onClick = { platformPickerLaunchers.launchGalleryPicker() },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFEF3C7),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(70.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoLibrary,
                                            contentDescription = "معرض الصور",
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "معرض الصور",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF92400E)
                                            )
                                        )
                                    }
                                }

                                // 3. الملفات
                                Surface(
                                    onClick = { platformPickerLaunchers.launchFilePicker() },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFE0F2FE),
                                    border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(70.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.InsertDriveFile,
                                            contentDescription = "الملفات",
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "الملفات",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF075985)
                                            )
                                        )
                                    }
                                }

                                // 4. الرابط الخارجي
                                Surface(
                                    onClick = { isLinkInputDialogOpen = true },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFEEF2FF),
                                    border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(70.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = "الرابط",
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "الرابط",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF3730A3)
                                            )
                                        )
                                    }
                                }
                            }

                            // مؤشر تحميل بصري أثناء نسخ معالجة الملفات الكبيرة
                            if (platformPickerLaunchers.isLoading) {
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF2563EB)
                                        )
                                        Text(
                                            text = "جاري معالجة ونسخ الملف للمستندات المحلية...",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF1E40AF)
                                            )
                                        )
                                    }
                                }
                            }

                            // إظهار بطاقة ملخص المرفقات المنفذة
                            if (pendingAttachments.isNotEmpty()) {
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "المرفقات المضافة (${pendingAttachments.size}):",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                            color = Color(0xFF0F172A)
                                        )
                                        pendingAttachments.forEachIndexed { index, att ->
                                            val icon = when (att.type) {
                                                AttachmentType.IMAGE -> Icons.Default.Image
                                                AttachmentType.DOCUMENT -> Icons.Default.InsertDriveFile
                                                AttachmentType.LINK -> Icons.Default.Link
                                            }
                                            val tint = when (att.type) {
                                                AttachmentType.IMAGE -> Color(0xFFE11D48)
                                                AttachmentType.DOCUMENT -> Color(0xFF0284C7)
                                                AttachmentType.LINK -> Color(0xFF4F46E5)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                                                    Text(
                                                        text = att.label,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                        color = Color(0xFF0F172A),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { pendingAttachments.removeAt(index) },
                                                    modifier = Modifier.size(22.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "حذف", tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. الكلمات المفتاحية والوسوم + الاصدار الهندسي في نفس السطر
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = keywords,
                                onValueChange = { keywords = it },
                                label = { Text("الكلمات المفتاحية") },
                                placeholder = { Text("#SMD #0805") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4F46E5),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = revision,
                                onValueChange = { revision = it },
                                label = { Text("الإصدار الهندسي") },
                                placeholder = { Text("مثال: Rev A") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4F46E5),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }

                        // 3. الحد الأقصى للمخزون + أيام الصلاحية الافتراضية
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = maximumStockText,
                                onValueChange = { maximumStockText = it },
                                label = { Text("الحد الأقصى للمخزون") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4F46E5),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )

                            OutlinedTextField(
                                value = defaultExpiryDaysText,
                                onValueChange = { defaultExpiryDaysText = it },
                                label = { Text("أيام الصلاحية الافتراضية") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4F46E5),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }

                        // 4. وصف القطعة التفصيلي (description) - فوق حقل التنبيهات ونقص المخزون مباشرة
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("وصف التفاصيل والمواصفات (description)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        // 9. الخصائص الفنية والتنبيهات المتقدمة (Switches / Toggles)
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "الخصائص الهندسية والتنبيهات:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                    color = Color(0xFF475569)
                                )

                                // 1. lowStockAlertsEnabled (تفعيل تنبيهات نقص المخزون - مفعل افتراضياً)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { lowStockAlertsEnabled = !lowStockAlertsEnabled },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text("تنبيهات نقص المخزون", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("تفعيل التنبيهات التلقائية لهذا الصنف عند الوصول للحد الأدنى", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp), color = Color(0xFF64748B))
                                        }
                                    }
                                    Switch(
                                        checked = lowStockAlertsEnabled,
                                        onCheckedChange = { lowStockAlertsEnabled = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Fixed Bottom Action Buttons Container at bottom of sheet page
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(stringResource(Res.string.cancel), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Button(
                        onClick = {
                            if (isFormValid) {
                                val trimmedIpn = ipn.trim()
                                val isDuplicate = allParts.any { other ->
                                    other.id != partToEdit?.id && other.ipn.trim().equals(trimmedIpn, ignoreCase = true)
                                }

                                if (isDuplicate) {
                                    hasDuplicateIpnError = true
                                    ipnShakeController.trigger()
                                    return@Button
                                }

                                if (partToEdit != null) {
                                    isConfirmUpdateDialogOpen = true
                                } else {
                                    val isComp = isComponentSelected
                                    val isAssy = isAssemblySelected
                                    val isSale = isSalableSelected
                                    val isPurch = isComp || isSale

                                    onConfirm(
                                        name.trim(),
                                        trimmedIpn,
                                        description,
                                        selectedCatId,
                                        units,
                                        isAssy,
                                        isComp,
                                        isTemplate,
                                        selectedVariantOfId,
                                        minimumStockText.toDoubleOrNull() ?: 0.0,
                                        maximumStockText.toDoubleOrNull(),
                                        revision,
                                        keywords,
                                        false,
                                        isPurch,
                                        isSale,
                                        virtual,
                                        selectedDefaultLocId,
                                        defaultExpiryDaysText.toIntOrNull(),
                                        pendingAttachments,
                                        true,
                                        false
                                    )
                                    onDismiss()
                                }
                            }
                        },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (partToEdit != null) "حفظ التعديلات" else stringResource(Res.string.save),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    if (isConfirmUpdateDialogOpen && partToEdit != null) {
        AlertDialog(
            onDismissRequest = { isConfirmUpdateDialogOpen = false },
            title = {
                Text(
                    text = "تأكيد حفظ التعديلات",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "هل أنت تأكد من رغبتك في حفظ وتطبيق التعديلات الجديدة على القطعة \"$name\"؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedIpn = ipn.trim()
                        val isDuplicate = allParts.any { other ->
                            other.id != partToEdit.id && other.ipn.trim().equals(trimmedIpn, ignoreCase = true)
                        }

                        if (isDuplicate) {
                            hasDuplicateIpnError = true
                            ipnShakeController.trigger()
                            isConfirmUpdateDialogOpen = false
                            return@Button
                        }

                        val isComp = isComponentSelected
                        val isAssy = isAssemblySelected
                        val isSale = isSalableSelected
                        val isPurch = isComp || isSale

                        val resolvedImageUrl = pendingAttachments.firstOrNull { it.type == AttachmentType.IMAGE }?.pathOrUrl
                        val resolvedLink = pendingAttachments.firstOrNull { it.type == AttachmentType.LINK }?.pathOrUrl 
                            ?: pendingAttachments.firstOrNull { it.type == AttachmentType.DOCUMENT }?.pathOrUrl 
                            ?: ""

                        val updatedPart = partToEdit.copy(
                            name = name,
                            ipn = trimmedIpn,
                            description = description,
                            categoryId = selectedCatId,
                            units = units.ifBlank { "pcs" },
                            assembly = isAssy,
                            component = isComp,
                            isTemplate = isTemplate,
                            variantOfId = selectedVariantOfId,
                            minimumStock = minimumStockText.toDoubleOrNull() ?: 0.0,
                            maximumStock = maximumStockText.toDoubleOrNull(),
                            revision = revision,
                            keywords = keywords,
                            trackable = false,
                            purchaseable = isPurch,
                            salable = isSale,
                            virtual = virtual,
                            defaultLocationId = selectedDefaultLocId,
                            defaultExpiryDays = defaultExpiryDaysText.toIntOrNull(),
                            link = resolvedLink,
                            imageUrl = resolvedImageUrl
                        )
                        isConfirmUpdateDialogOpen = false
                        onUpdate?.invoke(updatedPart, pendingAttachments)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تأكيد الحفظ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { isConfirmUpdateDialogOpen = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (isSelectNatureSheetOpen) {
        SelectNatureBottomSheet(
            isComponentSelected = isComponentSelected,
            isAssemblySelected = isAssemblySelected,
            isSalableSelected = isSalableSelected,
            onDismiss = { isSelectNatureSheetOpen = false },
            onToggleComponent = { isComponentSelected = !isComponentSelected },
            onToggleAssembly = { isAssemblySelected = !isAssemblySelected },
            onToggleSalable = { isSalableSelected = !isSalableSelected }
        )
    }

    if (isSelectCategorySheetOpen) {
        SelectCategoryBottomSheet(
            categories = categories,
            selectedCatId = selectedCatId,
            onCreateCategory = onCreateCategory,
            onDeleteCategory = onDeleteCategory,
            onDismiss = { isSelectCategorySheetOpen = false },
            onSelect = { selectedCatId = it?.id }
        )
    }

    if (isQuickAddCategoryOpen && onCreateCategory != null) {
        QuickAddCategoryBottomSheet(
            onDismiss = { isQuickAddCategoryOpen = false },
            onConfirm = { name, desc ->
                val created = onCreateCategory.invoke(name, desc)
                selectedCatId = created.id
                isQuickAddCategoryOpen = false
            }
        )
    }

    if (isSelectTemplateSheetOpen) {
        SelectTemplatePartBottomSheet(
            templateParts = templateParts,
            isTemplate = isTemplate,
            isStandaloneSelected = isStandaloneSelected,
            selectedVariantOfId = selectedVariantOfId,
            onDismiss = { isSelectTemplateSheetOpen = false },
            onSelect = { newIsTemplate, newIsStandalone, selectedTpl ->
                isTemplate = newIsTemplate
                isStandaloneSelected = newIsStandalone
                selectedVariantOfId = selectedTpl?.id
                isSelectTemplateSheetOpen = false
            }
        )
    }

    if (isSelectUnitSheetOpen) {
        SelectUnitBottomSheet(
            selectedUnit = units,
            onDismiss = { isSelectUnitSheetOpen = false },
            onSelect = { units = it }
        )
    }

    if (isSelectLocationSheetOpen) {
        SelectLocationBottomSheet(
            locations = locations,
            selectedLocationId = selectedDefaultLocId,
            onDismiss = { isSelectLocationSheetOpen = false },
            onSelect = { selectedDefaultLocId = it?.id }
        )
    }

    if (isLinkInputDialogOpen) {
        var tempLink by remember { mutableStateOf("") }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val sanitizedUrl = when {
            tempLink.trim().startsWith("http://", ignoreCase = true) || 
            tempLink.trim().startsWith("https://", ignoreCase = true) -> tempLink.trim()
            tempLink.trim().isBlank() -> ""
            else -> "https://${tempLink.trim()}"
        }
        val isUrlValid = tempLink.trim().isBlank() || sanitizedUrl.matches(Regex("^(https?://)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$"))

        ModalBottomSheet(
            onDismissRequest = { isLinkInputDialogOpen = false },
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF4F46E5))
                    Text("إدخال رابط التوثيق الخارجي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                OutlinedTextField(
                    value = tempLink,
                    onValueChange = { tempLink = it },
                    label = { Text("رابط كراسة المواصفات أو الموقع (URL)") },
                    placeholder = { Text("https://example.com/datasheet.pdf") },
                    singleLine = true,
                    isError = !isUrlValid && tempLink.isNotBlank(),
                    supportingText = {
                        if (!isUrlValid && tempLink.isNotBlank()) {
                            Text(
                                text = "صيغة الرابط غير صحيحة، يجب أن يحتوي على اسم نطاق صالح (مثل: https://example.com/datasheet.pdf)",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { isLinkInputDialogOpen = false }) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (tempLink.trim().isNotBlank() && isUrlValid) {
                                pendingAttachments.add(
                                    PendingAttachment(
                                        type = AttachmentType.LINK,
                                        pathOrUrl = sanitizedUrl,
                                        label = sanitizedUrl
                                    )
                                )
                            }
                            isLinkInputDialogOpen = false
                        },
                        enabled = isUrlValid,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text("حفظ الرابط", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectNatureBottomSheet(
    isComponentSelected: Boolean,
    isAssemblySelected: Boolean,
    isSalableSelected: Boolean,
    onDismiss: () -> Unit,
    onToggleComponent: () -> Unit,
    onToggleAssembly: () -> Unit,
    onToggleSalable: () -> Unit
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
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "طبيعة التعامل والتصنيع (نوع الصنف)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "يمكنك تحديد أكثر من خيار واحد أو إلغاء جميع الخيارات (ضغطة للاختيار وضغطة للالغة)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF64748B)
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

            // Option 1: Component
            Surface(
                onClick = onToggleComponent,
                shape = RoundedCornerShape(14.dp),
                color = if (isComponentSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                border = BorderStroke(
                    1.5.dp,
                    if (isComponentSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isComponentSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = if (isComponentSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "مكوّن أولي خام (Component)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "قطعة مفردة أو خامة تُستخدم كعنصر رئيسي في الإنتاج والتجميع",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Checkbox(
                        checked = isComponentSelected,
                        onCheckedChange = { onToggleComponent() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF4F46E5),
                            uncheckedColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            // Option 2: Assembly
            Surface(
                onClick = onToggleAssembly,
                shape = RoundedCornerShape(14.dp),
                color = if (isAssemblySelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                border = BorderStroke(
                    1.5.dp,
                    if (isAssemblySelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAssemblySelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = null,
                                tint = if (isAssemblySelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "تجميع داخلي (Assembly / BOM)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "تجميعة أو بوردة تحتوي على شجرة مكونات وقائمة مواد BOM",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Checkbox(
                        checked = isAssemblySelected,
                        onCheckedChange = { onToggleAssembly() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF4F46E5),
                            uncheckedColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            // Option 3: Salable
            Surface(
                onClick = onToggleSalable,
                shape = RoundedCornerShape(14.dp),
                color = if (isSalableSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                border = BorderStroke(
                    1.5.dp,
                    if (isSalableSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSalableSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = if (isSalableSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "منتج تجاري جاهز للبيع (Salable Product)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "منتج نهائي مُعد للبيع المباشر للعملاء وأوامر البيع",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Checkbox(
                        checked = isSalableSelected,
                        onCheckedChange = { onToggleSalable() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF4F46E5),
                            uncheckedColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("تم (تأكيد الاختيار)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddCategoryBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newCatName by remember { mutableStateOf("") }
    var newCatDesc by remember { mutableStateOf("") }

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
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "إنشاء تصنيف جديد سريع",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "إضافة تصنيف فرعي إلى شجرة الفئات",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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

            OutlinedTextField(
                value = newCatName,
                onValueChange = { newCatName = it },
                label = { Text("اسم التصنيف (Category Name) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF059669),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            OutlinedTextField(
                value = newCatDesc,
                onValueChange = { newCatDesc = it },
                label = { Text("وصف التصنيف (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF059669),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(Res.string.cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            onConfirm(newCatName, newCatDesc)
                            onDismiss()
                        }
                    },
                    enabled = newCatName.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(
                        "حفظ واختيار",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddInternalPriceBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, price: Double, currency: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantityText by remember { mutableStateOf("1.0") }
    var priceText by remember { mutableStateOf("0.0") }
    var currency by remember { mutableStateOf("USD") }

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
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "إضافة شريحة سعرية (PartInternalPrice)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "سعر التكلفة الداخلي حسب كميات التوريد",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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

            OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = { Text("الحد الأدنى للكمية (quantity >= 1) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("سعر الوحدة الواحدة (price) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            var isCurrencyPickerOpen by remember { mutableStateOf(false) }
            CurrencySelectorField(
                selectedCurrencyCode = currency,
                onOpenPicker = { isCurrencyPickerOpen = true },
                label = "العملة (price_currency)"
            )
            if (isCurrencyPickerOpen) {
                CurrencySelectionBottomSheet(
                    selectedCurrencyCode = currency,
                    onDismiss = { isCurrencyPickerOpen = false },
                    onCurrencySelected = { selectedCurr ->
                        currency = selectedCurr.code
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(Res.string.cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = {
                        val q = quantityText.toDoubleOrNull() ?: 1.0
                        val p = priceText.toDoubleOrNull() ?: 0.0
                        if (q >= 1.0 && p > 0.0) {
                            onConfirm(q, p, currency)
                            onDismiss()
                        }
                    },
                    enabled = (quantityText.toDoubleOrNull() ?: 0.0) >= 1.0 && (priceText.toDoubleOrNull() ?: 0.0) > 0.0,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(
                        stringResource(Res.string.save),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSalePriceBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, price: Double, currency: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantityText by remember { mutableStateOf("1.0") }
    var priceText by remember { mutableStateOf("0.0") }
    var currency by remember { mutableStateOf("USD") }

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
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sell,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "إضافة شريحة سعر بيع للعملاء (PartSalePrice)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "سعر البيع النهائي حسب الكمية المطلوبة",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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

            OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = { Text("الحد الأدنى للكمية (quantity >= 1) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("سعر بيع الوحدة الواحدة للعميل (price) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            var isCurrencyPickerOpen by remember { mutableStateOf(false) }
            CurrencySelectorField(
                selectedCurrencyCode = currency,
                onOpenPicker = { isCurrencyPickerOpen = true },
                label = "العملة (price_currency)"
            )
            if (isCurrencyPickerOpen) {
                CurrencySelectionBottomSheet(
                    selectedCurrencyCode = currency,
                    onDismiss = { isCurrencyPickerOpen = false },
                    onCurrencySelected = { selectedCurr ->
                        currency = selectedCurr.code
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(Res.string.cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = {
                        val q = quantityText.toDoubleOrNull() ?: 1.0
                        val p = priceText.toDoubleOrNull() ?: 0.0
                        if (q >= 1.0 && p > 0.0) {
                            onConfirm(q, p, currency)
                            onDismiss()
                        }
                    },
                    enabled = (quantityText.toDoubleOrNull() ?: 0.0) >= 1.0 && (priceText.toDoubleOrNull() ?: 0.0) > 0.0,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(
                        stringResource(Res.string.save),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddManufacturerPartBottomSheet(
    companies: List<Company>,
    onDismiss: () -> Unit,
    onConfirm: (manufacturerId: Long, mpn: String, description: String, link: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mfgCompanies = remember(companies) { companies.filter { it.isManufacturer } }
    var selectedCompany by remember { mutableStateOf(mfgCompanies.firstOrNull()) }
    var isCompanyPickerOpen by remember { mutableStateOf(false) }
    var mpn by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

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
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrecisionManufacturing,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "إضافة قطعة مصنّع (MPN)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "ربط الصنف برقم قطع الشركة المصنعة",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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

            Text("المصنّع المعتمد:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            OutlinedButton(
                onClick = { isCompanyPickerOpen = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedCompany?.name ?: "اختر الشركة المصنعة...", fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = mpn,
                onValueChange = { mpn = it },
                label = { Text("رقم قطعة المصنع (MPN) *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("الوصف الفني") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("رابط كراسة المواصفات (Datasheet)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.cancel), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }

                Button(
                    onClick = {
                        if (selectedCompany != null && mpn.isNotBlank()) {
                            onConfirm(selectedCompany!!.id, mpn, description, link)
                            onDismiss()
                        }
                    },
                    enabled = selectedCompany != null && mpn.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(stringResource(Res.string.save), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    if (isCompanyPickerOpen) {
        SearchableCompanyPickerDialog(
            companies = mfgCompanies,
            selectedCompanyId = selectedCompany?.id,
            title = "اختر الشركة المصنعة",
            onCompanySelected = { selectedCompany = it },
            onDismiss = { isCompanyPickerOpen = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSupplierPartBottomSheet(
    companies: List<Company>,
    mfgParts: List<ManufacturerPart>,
    onDismiss: () -> Unit,
    onConfirm: (supplierId: Long, sku: String, mfgPartId: Long?, description: String, link: String, note: String, packaging: String, packQuantity: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val supCompanies = remember(companies) { companies.filter { it.isSupplier } }
    var selectedCompany by remember { mutableStateOf(supCompanies.firstOrNull()) }
    var isCompanyPickerOpen by remember { mutableStateOf(false) }
    var selectedMfgPartId by remember { mutableStateOf<Long?>(mfgParts.firstOrNull()?.id) }
    var sku by remember { mutableStateOf("") }
    var packaging by remember { mutableStateOf("Box") }
    var packQuantity by remember { mutableStateOf("1") }
    var description by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

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
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "إضافة قطعة مورّد (SKU)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "ربط الصنف ببيانات شركة التوريد والـ SKU",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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

            Text("المورّد المعتمد:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            OutlinedButton(
                onClick = { isCompanyPickerOpen = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedCompany?.name ?: "اختر شركة التوريد...", fontWeight = FontWeight.Bold)
            }

            if (mfgParts.isNotEmpty()) {
                Text("ربط برقم قطع التصنيع (MPN):", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                mfgParts.forEach { mp ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMfgPartId = mp.id }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = selectedMfgPartId == mp.id,
                            onClick = { selectedMfgPartId = mp.id }
                        )
                        Text("MPN: ${mp.mpn} (#${mp.id})", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            OutlinedTextField(
                value = sku,
                onValueChange = { sku = it },
                label = { Text("رمز التوريد (SKU) *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = packaging,
                    onValueChange = { packaging = it },
                    label = { Text("نوع التغليف") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = packQuantity,
                    onValueChange = { packQuantity = it },
                    label = { Text("كمية الحزمة") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("الوصف") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("رابط صفحة الشراء") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("ملاحظة التوريد") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.cancel), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }

                Button(
                    onClick = {
                        if (selectedCompany != null && sku.isNotBlank()) {
                            onConfirm(selectedCompany!!.id, sku, selectedMfgPartId, description, link, note, packaging, packQuantity)
                            onDismiss()
                        }
                    },
                    enabled = selectedCompany != null && sku.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD97706),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(stringResource(Res.string.save), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    if (isCompanyPickerOpen) {
        SearchableCompanyPickerDialog(
            companies = supCompanies,
            selectedCompanyId = selectedCompany?.id,
            title = "اختر شركة التوريد",
            onCompanySelected = { selectedCompany = it },
            onDismiss = { isCompanyPickerOpen = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmDeletePartLinkDialog(
    itemTitle: String,
    onConfirm: () -> Unit,
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("تأكيد الحذف ⚠️", fontWeight = FontWeight.Bold, color = Color(0xFFE11D48), style = MaterialTheme.typography.titleMedium)
            Text("هل أنت تأكد من إزالة '$itemTitle'؟\n\nتنبيه: في حال كان هذا السجل مرتبطاً بأسعار سابقة أو أوامر شراء، فقد تتأثر تقارير التكلفة المرتبطة به.", style = MaterialTheme.typography.bodyMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("إلغاء") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("حذف")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartFilterBottomSheet(
    categories: List<PartCategory>,
    selectedCategoryId: Long?,
    initialCategoryIds: Set<Long> = emptySet(),
    parts: List<Part>,
    starredPartIds: Set<Long> = emptySet(),
    initialLowStock: Boolean,
    initialAssembly: Boolean,
    initialComponent: Boolean,
    initialPurchaseable: Boolean,
    initialSalable: Boolean,
    initialStarred: Boolean,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: (
        categoryIds: Set<Long>,
        lowStock: Boolean,
        assembly: Boolean,
        component: Boolean,
        purchaseable: Boolean,
        salable: Boolean,
        starred: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCatIdsState by remember {
        mutableStateOf<Set<Long>>(
            if (initialCategoryIds.isNotEmpty()) initialCategoryIds
            else if (selectedCategoryId != null) setOf(selectedCategoryId)
            else emptySet()
        )
    }
    var lowStockOnly by remember { mutableStateOf(initialLowStock) }
    var assemblyOnly by remember { mutableStateOf(initialAssembly) }
    var componentOnly by remember { mutableStateOf(initialComponent) }
    var purchaseableOnly by remember { mutableStateOf(initialPurchaseable) }
    var salableOnly by remember { mutableStateOf(initialSalable) }
    var starredOnly by remember { mutableStateOf(initialStarred) }

    val calculatedCount = remember(
        parts, searchQuery, selectedCatIdsState,
        lowStockOnly, assemblyOnly, componentOnly, purchaseableOnly, salableOnly, starredOnly
    ) {
        val query = searchQuery.trim()
        parts.count { part ->
            val matchesCategory = selectedCatIdsState.isEmpty() || (part.categoryId != null && selectedCatIdsState.contains(part.categoryId))
            val matchesQuery = query.isBlank() ||
                    part.name.contains(query, ignoreCase = true) ||
                    part.ipn.contains(query, ignoreCase = true) ||
                    part.description.contains(query, ignoreCase = true) ||
                    part.keywords.contains(query, ignoreCase = true)

            val matchesLowStock = !lowStockOnly || part.totalInStock <= part.minimumStock
            val matchesAssembly = !assemblyOnly || part.assembly
            val matchesComponent = !componentOnly || part.component
            val matchesPurchaseable = !purchaseableOnly || part.purchaseable
            val matchesSalable = !salableOnly || part.salable
            val matchesStarred = !starredOnly || starredPartIds.contains(part.id)

            matchesCategory && matchesQuery && matchesLowStock && matchesAssembly && matchesComponent && matchesPurchaseable && matchesSalable && matchesStarred
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
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "تصفية دليل القطع والمكونات",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "تحديد التصنيف الهرمي وخصائص وطبيعة التعامل",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = {
                        searchQuery = ""
                        selectedCatIdsState = emptySet()
                        lowStockOnly = false
                        assemblyOnly = false
                        componentOnly = false
                        purchaseableOnly = false
                        salableOnly = false
                        starredOnly = false
                        onReset()
                    }) {
                        Text(
                            text = "مسح الكل",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5)
                            )
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
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Body Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. حقل البحث الرئيسي
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن الفئة، اسم القطعة، أو الـ IPN...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // 2. شريط التبويبات الثلاثية
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
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🏷️ التصنيف الهرمي",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (selectedTab == 0) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚡ الفلاتر والحالات",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (selectedTab == 1) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            onClick = { selectedTab = 2 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 2) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 2) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚙️ طبيعة التعامل والخصائص",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (selectedTab == 2) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 3. محتوى التبويب المختار
                val filteredCategories = remember(categories, searchQuery) {
                    if (searchQuery.isBlank()) categories
                    else categories.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
                    }
                }

                val lowStockCount = remember(parts) { parts.count { it.totalInStock <= it.minimumStock } }
                val assemblyCount = remember(parts) { parts.count { it.assembly } }
                val starredCount = remember(parts, starredPartIds) { parts.count { starredPartIds.contains(it.id) } }
                val purchaseableCount = remember(parts) { parts.count { it.purchaseable } }
                val salableCount = remember(parts) { parts.count { it.salable } }
                val componentCount = remember(parts) { parts.count { it.component } }

                if (selectedTab == 0) {
                    // تبويب الفئات والتصنيفات الهرمية (دعم الاختيار المتعدد)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // خيار عرض الكل (يظهر مفصلاً عندما تكون مجموعة التصنيفات فارغة)
                        val isAllSelected = selectedCatIdsState.isEmpty()
                        Surface(
                            onClick = { selectedCatIdsState = emptySet() },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isAllSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (isAllSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isAllSelected,
                                    onCheckedChange = { if (it) selectedCatIdsState = emptySet() },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "جميع التصنيفات والفئات",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (isAllSelected) Color(0xFF4338CA) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "عرض كافة المكونات والأصناف بدون تحديد تصنيف محدد",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    color = if (isAllSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = parts.size.toString(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isAllSelected) Color(0xFF3730A3) else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        filteredCategories.forEach { category ->
                            val isSelected = selectedCatIdsState.contains(category.id)
                            val catCount = remember(parts, category.id) { parts.count { it.categoryId == category.id } }
                            Surface(
                                onClick = {
                                    selectedCatIdsState = if (isSelected) {
                                        selectedCatIdsState - category.id
                                    } else {
                                        selectedCatIdsState + category.id
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            selectedCatIdsState = if (isSelected) {
                                                selectedCatIdsState - category.id
                                            } else {
                                                selectedCatIdsState + category.id
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = if (isSelected) Color(0xFF4338CA) else Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = if (category.description.isNotBlank()) category.description else "عرض الأصناف والقطع التابعة لقسم ${category.name}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Surface(
                                        color = if (isSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = catCount.toString(),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            ),
                                            color = if (isSelected) Color(0xFF3730A3) else Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    // تبويب الفلاتر والحالات السريعة (بطاقات موحدة التصميم)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. خيار النواقص (منخفضة المخزون)
                        Surface(
                            onClick = { lowStockOnly = !lowStockOnly },
                            shape = RoundedCornerShape(14.dp),
                            color = if (lowStockOnly) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (lowStockOnly) Color(0xFFEF4444) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = lowStockOnly,
                                    onCheckedChange = { lowStockOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444))
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "⚠️ النواقص بالمخزون",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (lowStockOnly) Color(0xFF991B1B) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "عرض القطع والمكونات التي تقل كميتها عن حد الأمان",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    color = if (lowStockCount > 0) Color(0xFFFEE2E2) else Color(0xFFE2E8F0),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = lowStockCount.toString(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (lowStockCount > 0) Color(0xFF991B1B) else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        // 2. خيار تجميعات BOM
                        Surface(
                            onClick = { assemblyOnly = !assemblyOnly },
                            shape = RoundedCornerShape(14.dp),
                            color = if (assemblyOnly) Color(0xFFF3E8FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (assemblyOnly) Color(0xFFC084FC) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = assemblyOnly,
                                    onCheckedChange = { assemblyOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF9333EA))
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "🧩 تجميعات BOM",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (assemblyOnly) Color(0xFF6B21A8) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "عرض المنتجات والمكونات المجمعة ذات قائمة المواد",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    color = if (assemblyCount > 0) Color(0xFFE9D5FF) else Color(0xFFE2E8F0),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = assemblyCount.toString(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (assemblyCount > 0) Color(0xFF6B21A8) else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        // 3. خيار الأصناف المفضلة
                        Surface(
                            onClick = { starredOnly = !starredOnly },
                            shape = RoundedCornerShape(14.dp),
                            color = if (starredOnly) Color(0xFFFEF3C7) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (starredOnly) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = starredOnly,
                                    onCheckedChange = { starredOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706))
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "⭐ الأصناف المفضلة",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (starredOnly) Color(0xFF92400E) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "عرض الأصناف والمكونات المحفوظة في قائمة المفضلة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    color = if (starredCount > 0) Color(0xFFFDE68A) else Color(0xFFE2E8F0),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = starredCount.toString(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (starredCount > 0) Color(0xFF92400E) else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // تبويب طبيعة التعامل والخصائص (بطاقات موحدة التصميم)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. خيار الشراء (Purchaseable)
                        Surface(
                            onClick = { purchaseableOnly = !purchaseableOnly },
                            shape = RoundedCornerShape(14.dp),
                            color = if (purchaseableOnly) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (purchaseableOnly) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = purchaseableOnly,
                                    onCheckedChange = { purchaseableOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "🛒 أصناف قابلة للشراء (Purchaseable)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (purchaseableOnly) Color(0xFF4338CA) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "تحديد الأصناف والمكونات المتاحة للشراء واستلامها من الموردين",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    color = if (purchaseableOnly) Color(0xFFC7D2FE) else Color(0xFFE2E8F0),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = purchaseableCount.toString(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (purchaseableOnly) Color(0xFF3730A3) else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        // 2. خيار البيع (Salable)
                        Surface(
                            onClick = { salableOnly = !salableOnly },
                            shape = RoundedCornerShape(14.dp),
                            color = if (salableOnly) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (salableOnly) Color(0xFF10B981) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = salableOnly,
                                    onCheckedChange = { salableOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF059669))
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "🏷️ أصناف قابلة للبيع (Salable)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (salableOnly) Color(0xFF047857) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "تحديد المنتجات والأصناف المتاحة للبيع والتصدير للعملاء",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    color = if (salableOnly) Color(0xFFA7F3D0) else Color(0xFFE2E8F0),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = salableCount.toString(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (salableOnly) Color(0xFF065F46) else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        // 3. خيار المكونات الفرعية (Component)
                        Surface(
                            onClick = { componentOnly = !componentOnly },
                            shape = RoundedCornerShape(14.dp),
                            color = if (componentOnly) Color(0xFFFFF7ED) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (componentOnly) Color(0xFFF97316) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = componentOnly,
                                    onCheckedChange = { componentOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEA580C))
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "⚙️ مكونات فرعية (Component)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (componentOnly) Color(0xFFC2410C) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "تحديد القطع والأجزاء التي تدخل كعناصر داخلية في التجميعات",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    color = if (componentOnly) Color(0xFFFFEDD5) else Color(0xFFE2E8F0),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = componentCount.toString(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (componentOnly) Color(0xFF9A3412) else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer Section
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onApply(
                                selectedCatIdsState,
                                lowStockOnly,
                                assemblyOnly,
                                componentOnly,
                                purchaseableOnly,
                                salableOnly,
                                starredOnly
                            )
                        },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text(
                            text = "تطبيق التصفية (عرض $calculatedCount قطعة)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

