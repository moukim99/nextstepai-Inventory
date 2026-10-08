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
internal fun PartsTopBar(
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




