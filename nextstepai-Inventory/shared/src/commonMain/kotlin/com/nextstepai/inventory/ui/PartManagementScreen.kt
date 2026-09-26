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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.data.ManufacturerPart
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartAttachment
import com.nextstepai.inventory.data.PartCategory
import com.nextstepai.inventory.data.PartNotes
import com.nextstepai.inventory.data.PartParameter
import com.nextstepai.inventory.data.PartRelatedView
import com.nextstepai.inventory.data.PartTestTemplate
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.SupplierPart
import com.nextstepai.inventory.data.db.PartInternalPriceEntity
import com.nextstepai.inventory.data.db.PartPricingEntity
import com.nextstepai.inventory.data.db.PartSalePriceEntity
import com.nextstepai.inventory.repository.PartsSummary
import com.nextstepai.inventory.ui.components.CurrencySelectionBottomSheet
import com.nextstepai.inventory.ui.components.CurrencySelectorField
import com.nextstepai.inventory.ui.components.SearchableCompanyPickerDialog
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

    Scaffold(
        topBar = {
            PartsTopBar(
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier.padding(bottom = 20.dp, start = 12.dp, end = 12.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.setAddPartDialogOpen(true) },
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
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

                // شريط إحصائيات القطع المكون من حقول التقرير المحسوبة
                PartSummaryCards(summary = uiState.summary)

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
                    var receivingStockPart by remember { mutableStateOf<Part?>(null) }

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
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(uiState.parts, key = { index, part -> "part-${part.id}-$index" }) { _, part ->
                            val category = uiState.categories.find { c -> c.id == part.categoryId }
                            PartItemCard(
                                part = part,
                                categoryName = category?.name,
                                isStarred = uiState.starredPartIds.contains(part.id),
                                onToggleStar = { viewModel.togglePartStar(part.id) },
                                onAddStock = { receivingStockPart = it },
                                onClick = { viewModel.selectPart(part) }
                            )
                        }
                    }

                    if (receivingStockPart != null) {
                        AddStockBottomSheet(
                            part = receivingStockPart!!,
                            locations = uiState.stockLocations,
                            onDismiss = { receivingStockPart = null },
                            onConfirm = { partId, locId, qty, pkg, batch, serial, price, curr, expiry, notes ->
                                viewModel.receiveStockItem(
                                    partId = partId,
                                    locationId = locId,
                                    quantity = qty,
                                    packaging = pkg,
                                    batch = batch,
                                    serial = serial,
                                    purchasePrice = price,
                                    purchasePriceCurrency = curr,
                                    expiryDate = expiry,
                                    notes = notes
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // حوار عرض التفاصيل الكاملة المتبوبة لجميع جداول القطعة
    if (uiState.selectedPart != null) {
        PartDetailsBottomSheet(
            part = uiState.selectedPart!!,
            parameters = uiState.selectedPartParameters,
            relatedParts = uiState.selectedPartRelated,
            testTemplates = uiState.selectedPartTestTemplates,
            attachments = uiState.selectedPartAttachments,
            partNotes = uiState.selectedPartNotes,
            bomItems = uiState.selectedPartBomItems,
            partPricing = uiState.selectedPartPricing,
            internalPrices = uiState.selectedPartInternalPrices,
            salePrices = uiState.selectedPartSalePrices,
            mfgParts = uiState.selectedPartManufacturerParts,
            supParts = uiState.selectedPartSupplierParts,
            allCompanies = uiState.allCompanies,
            onRecalculatePricing = { viewModel.recalculatePartPricing() },
            onAddInternalPrice = { qty, prc, curr -> viewModel.addPartInternalPrice(qty, prc, curr) },
            onAddSalePrice = { qty, prc, curr -> viewModel.addPartSalePrice(qty, prc, curr) },
            onAddManufacturerPart = { mfgId, mpn, desc, link ->
                viewModel.addManufacturerPartForCurrentPart(mfgId, mpn, desc, link)
            },
            onDeleteManufacturerPart = { id -> viewModel.deleteManufacturerPart(id) },
            onAddSupplierPart = { supId, sku, mfgPartId, desc, link, note, pkg, packQty ->
                viewModel.addSupplierPartForCurrentPart(supId, sku, mfgPartId, desc, link, note, pkg, packQty)
            },
            onDeleteSupplierPart = { id -> viewModel.deleteSupplierPart(id) },
            onDismiss = { viewModel.selectPart(null) }
        )
    }

    // حوار إضافة قطعة جديدة
    if (uiState.isAddPartDialogOpen) {
        AddPartBottomSheet(
            categories = uiState.categories,
            locations = uiState.stockLocations,
            templateParts = uiState.templateParts,
            onCreateCategory = { catName, catDesc -> viewModel.addNewCategory(catName, catDesc) },
            onDeleteCategory = { catId -> viewModel.deleteCategory(catId) },
            onDismiss = { viewModel.setAddPartDialogOpen(false) },
            onConfirm = { name, ipn, desc, catId, units, assembly, component, isTemplate, variantOf, minStock, maxStock, revision, keywords, trackable, purchaseable, salable, virtual, defaultLocId, defaultExpiryDays, link, imageUrl, active, locked ->
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
            }
        )
    }

    // نافذة الفلترة التفاعلية بتبويبين
    if (uiState.isFilterBottomSheetOpen) {
        PartFilterBottomSheet(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            parts = uiState.parts,
            initialLowStock = uiState.lowStockOnlyFilter,
            initialAssembly = uiState.assemblyOnlyFilter,
            initialComponent = uiState.componentOnlyFilter,
            initialPurchaseable = uiState.purchaseableOnlyFilter,
            initialSalable = uiState.salableOnlyFilter,
            initialStarred = uiState.starredOnlyFilter,
            onDismiss = { viewModel.setFilterBottomSheetOpen(false) },
            onReset = { viewModel.resetFilters() },
            onApply = { catId, lowStock, assembly, component, purchaseable, salable, starred ->
                viewModel.applyFilters(catId, lowStock, assembly, component, purchaseable, salable, starred)
            }
        )
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
 * شريط الإحصائيات لمكونات جدول Part
 */
@Composable
private fun PartSummaryCards(summary: PartsSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(
            title = "إجمالي القطع",
            value = summary.totalParts.toString(),
            containerColor = Color(0xFFEEF2FF),
            contentColor = Color(0xFF4338CA),
            borderColor = Color(0xFFC7D2FE),
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "نقص بالمخزون",
            value = summary.lowStockParts.toString(),
            containerColor = if (summary.lowStockParts > 0) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
            contentColor = if (summary.lowStockParts > 0) Color(0xFF991B1B) else Color(0xFF64748B),
            borderColor = if (summary.lowStockParts > 0) Color(0xFFFCA5A5) else Color(0xFFE2E8F0),
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "تجميعات BOM",
            value = summary.assemblyParts.toString(),
            containerColor = Color(0xFFF3E8FF),
            contentColor = Color(0xFF6B21A8),
            borderColor = Color(0xFFE9D5FF),
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "قوالب القياس",
            value = summary.templateParts.toString(),
            containerColor = Color(0xFFCCFBF1),
            contentColor = Color(0xFF115E59),
            borderColor = Color(0xFF99F6E4),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
    onDismiss: () -> Unit,
    onConfirm: (
        partId: Long,
        locationId: Long?,
        quantity: Double,
        packaging: String,
        batch: String,
        serial: String,
        purchasePrice: Double,
        purchasePriceCurrency: String,
        expiryDate: String,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 1. Initial State Setup
    val defaultQty = if (part.trackable) "1.0" else "10.0"
    var quantityText by remember { mutableStateOf(defaultQty) }
    var selectedLocationId by remember { mutableStateOf<Long?>(part.defaultLocationId ?: locations.firstOrNull()?.id) }
    var packagingText by remember { mutableStateOf("صندوق") }

    // Auto-generated Batch Code
    val initialBatch = remember {
        val stamp = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
        "BATCH-$stamp"
    }
    var batchText by remember { mutableStateOf(initialBatch) }

    // Serial Number (Mandatory if trackable)
    val initialSerial = remember(part.trackable) {
        if (part.trackable) {
            val stamp = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
            "SN-$stamp"
        } else ""
    }
    var serialText by remember { mutableStateOf(initialSerial) }

    // Auto-calculated Expiry Date (if part.defaultExpiryDays > 0)
    val autoExpiryDate = remember(part.defaultExpiryDays) {
        if (part.defaultExpiryDays != null && part.defaultExpiryDays!! > 0) {
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val expiryMs = nowMs + (part.defaultExpiryDays!! * 86400000L)
            DateTimeUtils.formatEpochMillisToDate(expiryMs)
        } else ""
    }
    var expiryDateText by remember { mutableStateOf(autoExpiryDate) }

    var purchasePriceText by remember { mutableStateOf("") }
    var currencyText by remember { mutableStateOf("USD") }
    var notesText by remember { mutableStateOf("") }

    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var isSelectLocationSheetOpen by remember { mutableStateOf(false) }

    val selectedLocation = remember(selectedLocationId, locations) {
        locations.find { it.id == selectedLocationId }
    }

    // Validation
    val isFormValid = remember(quantityText, serialText, part.trackable) {
        val qty = quantityText.toDoubleOrNull() ?: 0.0
        val serialValid = !part.trackable || serialText.isNotBlank()
        qty > 0.0 && serialValid
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
                .verticalScroll(rememberScrollState())
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

            // Part Details Summary Card
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

            // Notice Card if Trackable
            if (part.trackable) {
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "هذه القطعة خاضعة للتتبع بالرقم التسلسلي (trackable). يرجى التأكد من الرقم التسلسلي الفريد لكل وحدة.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF1E40AF)
                            )
                        )
                    }
                }
            }

            // 1. Quantity & Packaging Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("الكمية المستلمة (${part.units}) *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                OutlinedTextField(
                    value = packagingText,
                    onValueChange = { packagingText = it },
                    label = { Text("نوع التغليف") },
                    placeholder = { Text("صندوق / بكرة / كرتون") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // 2. Serial Number (If Trackable or Optional)
            OutlinedTextField(
                value = serialText,
                onValueChange = { serialText = it },
                label = { Text(if (part.trackable) "الرقم التسلسلي (Serial Number) *" else "الرقم التسلسلي (اختياري)") },
                placeholder = { Text("مثال: SN-884012") },
                singleLine = true,
                trailingIcon = {
                    Surface(
                        onClick = {
                            val stamp = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
                            serialText = "SN-$stamp"
                        },
                        color = Color(0xFFEEF2FF),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = "توليد",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF4F46E5),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
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

            // 3. Batch Code
            OutlinedTextField(
                value = batchText,
                onValueChange = { batchText = it },
                label = { Text("رقم التشغيلة / الدفعة المصنعية (Batch Code)") },
                singleLine = true,
                trailingIcon = {
                    Surface(
                        onClick = {
                            val stamp = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
                            batchText = "BATCH-$stamp"
                        },
                        color = Color(0xFFEEF2FF),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = "توليد",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF4F46E5),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
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

            // 4. Physical Storage Location Selection
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "موقع التخزين المادي (المستودع / الرف) * :",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )

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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
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
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = selectedLocation?.description?.ifBlank { "اضغط لاختيار رف المستودع" } ?: "مكان إيداع الشحنة في المستودع",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFEEF2FF),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                        ) {
                            Text(
                                text = if (selectedLocation != null) "تغيير" else "اختر ▾",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 5. Collapsible Section for Cost & Expiry
            Surface(
                onClick = { isAdvancedExpanded = !isAdvancedExpanded },
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "معلومات التكلفة والصلاحية والملاحظات الإضافية",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        ),
                        color = Color(0xFF1E293B)
                    )
                    Icon(
                        imageVector = if (isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5)
                    )
                }
            }

            if (isAdvancedExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Expiry Date
                    var showExpiryDatePicker by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = expiryDateText,
                            onValueChange = { expiryDateText = it },
                            readOnly = true,
                            label = { Text("تاريخ انتهاء الصلاحية") },
                            placeholder = { Text("انقر لاختيار التاريخ...") },
                            singleLine = true,
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
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
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
                                        expiryDateText = "$year-$month-$day"
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

                    // Purchase Price & Currency
                    var isCurrencyPickerOpen by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = purchasePriceText,
                            onValueChange = { purchasePriceText = it },
                            label = { Text("سعر الشراء الفعلي") },
                            placeholder = { Text("مثال: 12.50") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        CurrencySelectorField(
                            selectedCurrencyCode = currencyText,
                            onOpenPicker = { isCurrencyPickerOpen = true },
                            label = "العملة",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isCurrencyPickerOpen) {
                        CurrencySelectionBottomSheet(
                            selectedCurrencyCode = currencyText,
                            onDismiss = { isCurrencyPickerOpen = false },
                            onCurrencySelected = { selectedCurr ->
                                currencyText = selectedCurr.code
                            }
                        )
                    }

                    // Notes
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("ملاحظات الشحنة / المورد") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
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
                        val qty = quantityText.toDoubleOrNull() ?: 0.0
                        val price = purchasePriceText.toDoubleOrNull() ?: 0.0
                        if (isFormValid) {
                            onConfirm(
                                part.id,
                                selectedLocationId,
                                qty,
                                packagingText,
                                batchText,
                                serialText,
                                price,
                                currencyText,
                                expiryDateText,
                                notesText
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

    if (isSelectLocationSheetOpen) {
        SelectLocationBottomSheet(
            locations = locations,
            selectedLocationId = selectedLocationId,
            onDismiss = { isSelectLocationSheetOpen = false },
            onSelect = { selectedLocationId = it?.id }
        )
    }
}

/**
 * حساب ميزان ونسبة اكتمال بيانات الصنف (Gamification / Data Completeness Score)
 */
fun calculatePartCompletenessScore(
    name: String,
    ipn: String,
    categoryId: Long?,
    units: String,
    minimumStock: Double,
    nature: String?
): Int {
    var score = 0
    if (name.isNotBlank()) score += 25
    if (ipn.isNotBlank()) score += 20
    if (categoryId != null) score += 15
    if (units.isNotBlank()) score += 15
    if (minimumStock > 0.0) score += 15
    if (!nature.isNullOrBlank()) score += 10
    return score
}

fun Part.calculateCompletenessScore(): Int {
    var score = 0
    if (name.isNotBlank()) score += 25
    if (ipn.isNotBlank()) score += 20
    if (categoryId != null) score += 15
    if (units.isNotBlank()) score += 15
    if (minimumStock > 0.0) score += 15
    score += 10 // nature is always set on a Part object
    return score
}

@Composable
private fun CompletenessRing(
    score: Int,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.5.dp,
    showText: Boolean = false
) {
    val ringColor = when {
        score < 40 -> Color(0xFFF59E0B) // Amber
        score < 100 -> Color(0xFF4F46E5) // Indigo
        else -> Color(0xFF10B981) // Emerald Green
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier.matchParentSize(),
            color = ringColor,
            strokeWidth = strokeWidth,
            trackColor = ringColor.copy(alpha = 0.15f)
        )
        if (showText) {
            if (score == 100) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "مكتمل",
                    tint = ringColor,
                    modifier = Modifier.size(10.dp)
                )
            } else {
                Text(
                    text = "$score%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        color = ringColor
                    )
                )
            }
        }
    }
}

/**
 * بطاقة عرض القطعة PartItemCard مع حلقة وسام اكتمال البيانات (Completeness Ring)
 */
@Composable
private fun PartItemCard(
    part: Part,
    categoryName: String? = null,
    isStarred: Boolean = false,
    onToggleStar: () -> Unit = {},
    onAddStock: ((Part) -> Unit)? = null,
    onClick: () -> Unit
) {
    val completenessScore = remember(part) { part.calculateCompletenessScore() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ==========================================
            // 1. Header Row (الهيدر العلوي)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Right side (in RTL): Part Image/Icon + Name & IPN
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Dynamic Icon / Image Box with Completeness Ring surrounding
                    val (iconBg, iconColor, iconVector) = getPartIconAndColors(part, categoryName)

                    Box(
                        modifier = Modifier.size(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CompletenessRing(
                            score = completenessScore,
                            modifier = Modifier.size(50.dp),
                            strokeWidth = 2.5.dp,
                            showText = false
                        )

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(iconBg)
                                .border(1.dp, iconColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = part.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            ),
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (part.ipn.isNotBlank()) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Text(
                                    text = "IPN: ${part.ipn}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF0F766E),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Left side (in RTL): Completeness Badge Tag + Favorite Star + Nature Badge Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Completeness Score Badge Tag
                    val ringColor = when {
                        completenessScore < 40 -> Color(0xFFD97706)
                        completenessScore < 100 -> Color(0xFF4F46E5)
                        else -> Color(0xFF059669)
                    }
                    Surface(
                        color = ringColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ringColor.copy(alpha = 0.25f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            CompletenessRing(
                                score = completenessScore,
                                modifier = Modifier.size(11.dp),
                                strokeWidth = 1.8.dp,
                                showText = false
                            )
                            Text(
                                text = "$completenessScore%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = ringColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Favorite Star
                    IconButton(
                        onClick = onToggleStar,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isStarred) Color(0xFFFEF3C7) else Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = stringResource(Res.string.filter_starred),
                            tint = if (isStarred) Color(0xFFD97706) else Color(0xFF94A3B8),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Nature Badge Tag (مكوّن / تجميع BOM / جاهز للبيع / قالب)
                    val (natureText, natureBg, natureFg) = when {
                        part.assembly -> Triple("تجميع BOM", Color(0xFFF3E8FF), Color(0xFF7E22CE))
                        part.salable -> Triple("جاهز للبيع", Color(0xFFE0F2FE), Color(0xFF0369A1))
                        part.isTemplate -> Triple("قالب أصل", Color(0xFFCCFBF1), Color(0xFF0F766E))
                        else -> Triple("مكوّن", Color(0xFFECFDF5), Color(0xFF047857))
                    }

                    Surface(
                        color = natureBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, natureFg.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = natureText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = natureFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            // ==========================================
            // 2. Middle Row (الوصف والتصنيف)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (part.description.isNotBlank()) {
                    Text(
                        text = part.description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (!categoryName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 3. Stock Status Banner (حالة المخزون)
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (part.isLowStock) Color(0xFFFEF2F2) else Color(0xFFECFDF5),
                border = BorderStroke(1.dp, if (part.isLowStock) Color(0xFFFCA5A5) else Color(0xFFA7F3D0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المخزون المتاح: ${part.availableStock} ${part.units} (من أصل ${part.totalInStock})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = if (part.isLowStock) Color(0xFF991B1B) else Color(0xFF065F46)
                    )

                    if (part.isLowStock) {
                        Text(
                            text = "⚠️ نقص في المخزون (< ${part.minimumStock})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            // ==========================================
            // 4. Action Row (شريط الإجراءات والروابط)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Stock Receive Button (+ استلام مخزون)
                Button(
                    onClick = { onAddStock?.invoke(part) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "استلام مخزون",
                            modifier = Modifier.size(16.dp)
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

                // External Link Button & Details Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (part.link.isNotBlank()) {
                        Surface(
                            onClick = { /* Open link action */ },
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "رابط التوثيق",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onClick,
                        color = Color(0xFFEEF2FF),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "التفاصيل",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
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

/**
 * حوار عرض تفاصيل السجل الكاملة المتبوبة المباشرة لجدول Part
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartDetailsBottomSheet(
    part: Part,
    parameters: List<PartParameter> = emptyList(),
    relatedParts: List<PartRelatedView> = emptyList(),
    testTemplates: List<PartTestTemplate> = emptyList(),
    attachments: List<PartAttachment> = emptyList(),
    partNotes: PartNotes? = null,
    bomItems: List<BomItem> = emptyList(),
    partPricing: PartPricingEntity? = null,
    internalPrices: List<PartInternalPriceEntity> = emptyList(),
    salePrices: List<PartSalePriceEntity> = emptyList(),
    mfgParts: List<ManufacturerPart> = emptyList(),
    supParts: List<SupplierPart> = emptyList(),
    allCompanies: List<Company> = emptyList(),
    onRecalculatePricing: () -> Unit = {},
    onAddInternalPrice: (quantity: Double, price: Double, currency: String) -> Unit = { _, _, _ -> },
    onAddSalePrice: (quantity: Double, price: Double, currency: String) -> Unit = { _, _, _ -> },
    onAddManufacturerPart: (manufacturerId: Long, mpn: String, description: String, link: String) -> Unit = { _, _, _, _ -> },
    onDeleteManufacturerPart: (id: Long) -> Unit = {},
    onAddSupplierPart: (supplierId: Long, sku: String, mfgPartId: Long?, description: String, link: String, note: String, packaging: String, packQuantity: String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeleteSupplierPart: (id: Long) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTabIndex by remember { mutableStateOf(0) }
    var isAddInternalPriceDialogOpen by remember { mutableStateOf(false) }
    var isAddSalePriceDialogOpen by remember { mutableStateOf(false) }
    var isAddMfgPartDialogOpen by remember { mutableStateOf(false) }
    var isAddSupPartDialogOpen by remember { mutableStateOf(false) }
    var deleteItemPending by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    val tabTitles = listOf(
        "📊 العام",
        "⚙️ الـ BOM (${bomItems.size})",
        "📐 المعاملات (${parameters.size})",
        "🧪 الفحوصات (${testTemplates.size})",
        "🔗 الصلة (${relatedParts.size})",
        "📁 المرفقات (${attachments.size})",
        "📝 الملاحظات",
        "💰 التسعير",
        "🏭 المصنّعون (${mfgParts.size})",
        "🛒 المورّدون (${supParts.size})"
    )

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
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
                        val (iconBg, iconColor, iconVector) = when {
                            part.assembly -> Triple(Color(0xFFF3E8FF), Color(0xFF9333EA), Icons.Default.Build)
                            part.isTemplate -> Triple(Color(0xFFCCFBF1), Color(0xFF0D9488), Icons.Default.Description)
                            else -> Triple(Color(0xFFEEF2FF), Color(0xFF4F46E5), Icons.Default.Memory)
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "تفاصيل القطعة: ${part.name}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                            if (part.ipn.isNotBlank()) {
                                Text(
                                    text = "IPN: ${part.ipn}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    ),
                                    color = Color(0xFF0D9488)
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

                Spacer(modifier = Modifier.height(10.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White,
                    contentColor = Color(0xFF4F46E5),
                    edgePadding = 0.dp,
                    divider = { HorizontalDivider(color = Color(0xFFF1F5F9)) }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            selectedContentColor = Color(0xFF4F46E5),
                            unselectedContentColor = Color(0xFF64748B),
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                when (selectedTabIndex) {
                    0 -> {
                        DetailRow("رقم السجل (id):", "#${part.id}")
                        DetailRow("الاسم (name):", part.name)
                        DetailRow("رقم القطعة الداخلي (IPN):", part.ipn.ifBlank { "-" })
                        DetailRow("الوصف (description):", part.description.ifBlank { "-" })
                        DetailRow("الإصدار الهندسي (revision):", part.revision.ifBlank { "الأولي" })
                        DetailRow("وحدة القياس (units):", part.units)
                        DetailRow("الكلمات المفتاحية (keywords):", part.keywords.ifBlank { "-" })
                        HorizontalDivider()
                        DetailRow("منتج مجمع (assembly):", if (part.assembly) "نعم (BOM)" else "لا")
                        DetailRow("مكون فرعي (component):", if (part.component) "نعم" else "لا")
                        DetailRow("قالب تجريدي (is_template):", if (part.isTemplate) "نعم" else "لا")
                        DetailRow("تتبع متسلسل (trackable):", if (part.trackable) "نعم" else "لا")
                        DetailRow("قابل للشراء (purchaseable):", if (part.purchaseable) "نعم" else "لا")
                        DetailRow("قابل للبيع (salable):", if (part.salable) "نعم" else "لا")
                        DetailRow("افتراضي (virtual):", if (part.virtual) "نعم" else "لا")
                        DetailRow("حالة النشاط (active):", if (part.active) "نشط" else "مؤرشف")
                        HorizontalDivider()
                        DetailRow("إجمالي المخزون (totalInStock):", "${part.totalInStock} ${part.units}")
                        DetailRow("محجوز للتصنيع (allocatedToBuild):", "${part.allocatedToBuildOrders} ${part.units}")
                        DetailRow("محجوز للبيع (allocatedToSales):", "${part.allocatedToSalesOrders} ${part.units}")
                        DetailRow("الصافي المتاح (availableStock):", "${part.availableStock} ${part.units}")
                        DetailRow("الحد الأدنى للتنبيه (minimumStock):", "${part.minimumStock} ${part.units}")
                        DetailRow("الحد الأقصى للمخزون (maximumStock):", part.maximumStock?.let { "$it ${part.units}" } ?: "-")
                        DetailRow("تاريخ الإنشاء (creation_date):", part.creationDate.ifBlank { "2025-02-15" })

                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏭 قطع المصنّع الأصلي (MPN):", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            Button(
                                onClick = { isAddMfgPartDialogOpen = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("+ إضافة MPN", fontSize = 10.sp)
                            }
                        }
                        if (mfgParts.isEmpty()) {
                            Text("لا توجد أكواد مصنع مربوطة بهذه القطعة.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            mfgParts.forEach { mp ->
                                val compName = allCompanies.find { it.id == mp.manufacturerId }?.name ?: "مصنع #${mp.manufacturerId}"
                                DetailRow("• $compName:", "MPN: ${mp.mpn}")
                            }
                        }

                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🛒 أكواد الموردين الشركاء (SKU):", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            Button(
                                onClick = { isAddSupPartDialogOpen = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("+ إضافة SKU", fontSize = 10.sp)
                            }
                        }
                        if (supParts.isEmpty()) {
                            Text("لا توجد أكواد مورّدين مربوطة بهذه القطعة.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            supParts.forEach { sp ->
                                val supName = allCompanies.find { it.id == sp.supplierId }?.name ?: "مورد #${sp.supplierId}"
                                DetailRow("• $supName:", "SKU: ${sp.sku}")
                            }
                        }
                    }
                    1 -> {
                        Text("بنود قائمة المواد (Bill of Materials):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (bomItems.isEmpty()) {
                            Text("لا توجد مكونات فرعية مسجلة لهذه القطعة.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            for (item in bomItems) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        DetailRow("مكون #${item.subPartId}:", "الكمية: ${item.quantity}")
                                        if (item.reference.isNotBlank()) DetailRow("المرجع الهندسي:", item.reference)
                                        if (item.note.isNotBlank()) DetailRow("ملاحظة:", item.note)
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        Text("المعاملات الفنية للقطعة (Part Parameters):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (parameters.isEmpty()) {
                            Text("لا توجد مواصفات فنية مسجلة لهذه القطعة.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            for (param in parameters) {
                                DetailRow("المعامل #${param.templateId}:", param.value.ifBlank { "-" })
                            }
                        }
                    }
                    3 -> {
                        Text("قوالب الفحوصات وااختبارات الجودة (PartTestTemplate):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (testTemplates.isEmpty()) {
                            Text("لا توجد قوالب فحوصات مسجلة لهذه القطعة.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            for (test in testTemplates) {
                                val reqBadge = if (test.required) "[إلزامي]" else "[اختياري]"
                                val valBadge = if (test.requiresValue) "[يتطلب قيمة]" else ""
                                val attBadge = if (test.requiresAttachment) "[يتطلب مرفق]" else ""
                                DetailRow("• ${test.testName}:", "$reqBadge $valBadge $attBadge".trim())
                            }
                        }
                    }
                    4 -> {
                        Text("القطع ذات الصلة والبدائل الشبيهة (PartRelated):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (relatedParts.isEmpty()) {
                            Text("لا توجد قطع ذات صلة مقترنة بهذه القطعة.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            for (rel in relatedParts) {
                                DetailRow("• ${rel.relatedPart.name}", rel.relatedPart.ipn.ifBlank { "قطعة #${rel.relatedPart.id}" })
                            }
                        }
                    }
                    5 -> {
                        Text("المرفقات والوثائق الفنية (PartAttachment):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (attachments.isEmpty()) {
                            Text("لا توجد ملفات أو وثائق مرفقة بهذه القطعة.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            for (att in attachments) {
                                val sourceInfo = att.attachment ?: att.link ?: "-"
                                DetailRow("• ${att.comment}:", sourceInfo)
                            }
                        }
                    }
                    6 -> {
                        Text("الملاحظات والتوجيهات التفصيلية (PartNotes):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (partNotes != null && partNotes.notes.isNotBlank()) {
                            Text(
                                text = partNotes.notes,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "آخر تحديث: ${partNotes.updatedAt} بواسطة مستخدم #${partNotes.userId ?: 1}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            Text("لا توجد ملاحظات تفصيلية مسجلة لهذه القطعة.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    7 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("حساب وتتبع التكاليف والأسعار (PartPricing):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { isAddInternalPriceDialogOpen = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("شريحة جديدة +", fontSize = 10.sp)
                                }
                                Button(
                                    onClick = onRecalculatePricing,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("إعادة الحساب 🔄", fontSize = 10.sp)
                                }
                            }
                        }

                        if (partPricing != null) {
                            val curr = partPricing.currency
                            DetailRow("العملة المعتمدة:", curr)
                            HorizontalDivider()
                            DetailRow("التكلفة الكلية الإجمالية (Overall):", "${partPricing.overallMin ?: 0.0} - ${partPricing.overallMax ?: 0.0} $curr")
                            DetailRow("نطاق أسعار الشراء (Purchase):", "${partPricing.purchaseCostMin ?: "-"} - ${partPricing.purchaseCostMax ?: "-"} $curr")
                            DetailRow("نطاق تكلفة التجميع (BOM Cost):", "${partPricing.bomCostMin ?: "-"} - ${partPricing.bomCostMax ?: "-"} $curr")
                            DetailRow("نطاق تكلفة المتغيرات (Variant):", "${partPricing.variantCostMin ?: "-"} - ${partPricing.variantCostMax ?: "-"} $curr")
                            DetailRow("نطاق البيع الداخلي (Internal):", "${partPricing.internalCostMin ?: "-"} - ${partPricing.internalCostMax ?: "-"} $curr")
                            HorizontalDivider()
                            Text(
                                text = "آخر تحديث للتكاليف: ${partPricing.updatedAt.ifBlank { "الآن" }}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            Text("لم يتم تسجيل أو حساب التكاليف لهذه القطعة بعد.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (internalPrices.isNotEmpty()) {
                            HorizontalDivider()
                            Text("شرائح أسعار البيع والتكلفة الداخلية (PartInternalPrice):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            for (p in internalPrices) {
                                DetailRow("• للكمية ${p.quantity}+ قطعة:", "${p.price} ${p.currency} للوحدة")
                            }
                        }

                        if (part.salable) {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("شرائح أسعار البيع للعملاء (PartSalePrice):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Button(
                                    onClick = { isAddSalePriceDialogOpen = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("شريحة بيع +", fontSize = 10.sp)
                                }
                            }

                            if (salePrices.isEmpty()) {
                                Text("لا توجد شرائح أسعار بيع مسجلة لهذه القطعة بعد.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                for (sp in salePrices) {
                                    DetailRow("• خصم عميل للكمية ${sp.quantity}+ قطعة:", "${sp.price} ${sp.currency} للوحدة")
                                }
                            }
                        }
                    }
                    8 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الشركات المصنّعة وأكواد MPN:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Button(
                                onClick = { isAddMfgPartDialogOpen = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("+ إضافة MPN", fontSize = 10.sp)
                            }
                        }

                        if (mfgParts.isEmpty()) {
                            Text("لا توجد شركات مصنّعة مسجلة لهذه القطعة بعد.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            for (mp in mfgParts) {
                                val companyName = allCompanies.find { it.id == mp.manufacturerId }?.name ?: "مصنّع #${mp.manufacturerId}"
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(companyName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("MPN: ${mp.mpn}", fontSize = 11.sp, color = Color(0xFF4F46E5), fontWeight = FontWeight.SemiBold)
                                            if (mp.description.isNotBlank()) Text(mp.description, fontSize = 10.sp, color = Color(0xFF64748B))
                                        }
                                        IconButton(
                                            onClick = {
                                                deleteItemPending = Pair("قطع المصنع MPN: ${mp.mpn}") { onDeleteManufacturerPart(mp.id) }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    9 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("شركات التوريد وأكواد SKU والأسعار:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Button(
                                onClick = { isAddSupPartDialogOpen = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("+ إضافة SKU", fontSize = 10.sp)
                            }
                        }

                        if (supParts.isEmpty()) {
                            Text("لا توجد شركات توريد مسجلة لهذه القطعة بعد.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            for (sp in supParts) {
                                val supplierName = allCompanies.find { it.id == sp.supplierId }?.name ?: "مورّد #${sp.supplierId}"
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(supplierName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("SKU: ${sp.sku}", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.SemiBold)
                                            if (sp.packaging.isNotBlank()) Text("التغليف: ${sp.packaging} (حزمة ${sp.packQuantity})", fontSize = 10.sp, color = Color(0xFF64748B))
                                        }
                                        IconButton(
                                            onClick = {
                                                deleteItemPending = Pair("قطع المورد SKU: ${sp.sku}") { onDeleteSupplierPart(sp.id) }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
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

    if (isAddInternalPriceDialogOpen) {
        AddInternalPriceBottomSheet(
            onDismiss = { isAddInternalPriceDialogOpen = false },
            onConfirm = { qty, prc, curr ->
                onAddInternalPrice(qty, prc, curr)
                isAddInternalPriceDialogOpen = false
            }
        )
    }

    if (isAddSalePriceDialogOpen) {
        AddSalePriceBottomSheet(
            onDismiss = { isAddSalePriceDialogOpen = false },
            onConfirm = { qty, prc, curr ->
                onAddSalePrice(qty, prc, curr)
                isAddSalePriceDialogOpen = false
            }
        )
    }

    if (isAddMfgPartDialogOpen) {
        AddManufacturerPartBottomSheet(
            companies = allCompanies,
            onDismiss = { isAddMfgPartDialogOpen = false },
            onConfirm = { mfgId, mpn, desc, link ->
                onAddManufacturerPart(mfgId, mpn, desc, link)
                isAddMfgPartDialogOpen = false
            }
        )
    }

    if (isAddSupPartDialogOpen) {
        AddSupplierPartBottomSheet(
            companies = allCompanies,
            mfgParts = mfgParts,
            onDismiss = { isAddSupPartDialogOpen = false },
            onConfirm = { supId, sku, mfgPartId, desc, link, note, pkg, packQty ->
                onAddSupplierPart(supId, sku, mfgPartId, desc, link, note, pkg, packQty)
                isAddSupPartDialogOpen = false
            }
        )
    }

    if (deleteItemPending != null) {
        ConfirmDeletePartLinkDialog(
            itemTitle = deleteItemPending!!.first,
            onConfirm = deleteItemPending!!.second,
            onDismiss = { deleteItemPending = null }
        )
    }
}
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
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
                .fillMaxHeight(0.85f)
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
                    val isGeneralSelected = selectedCatId == null
                    Surface(
                        onClick = {
                            onSelect(null)
                            onDismiss()
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFE0E7FF)),
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
                                        text = "بدون تصنيف (عام)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "صنف عام غير مرتبط بفيئة محددة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            if (isGeneralSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5)
                                )
                            }
                        }
                    }
                }

                itemsIndexed(filteredCategories, key = { index, cat -> "cat-${cat.id}-$index" }) { _, cat ->
                    val isSelected = cat.id == selectedCatId
                    Surface(
                        onClick = {
                            onSelect(cat)
                            onDismiss()
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEEF2FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📁", fontSize = 18.sp)
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
                                    if (cat.description.isNotBlank()) {
                                        Text(
                                            text = cat.description,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5)
                                    )
                                }

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
                            }
                        }
                    }
                }
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
    selectedVariantOfId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Part?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

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
                .fillMaxHeight(0.82f)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "اختر الصنف القالب الأصل (variantOfId)",
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
                item {
                    val isStandalone = selectedVariantOfId == null
                    Surface(
                        onClick = {
                            onSelect(null)
                            onDismiss()
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFE0E7FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Extension,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(20.dp)
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
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            if (isStandalone) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5)
                                )
                            }
                        }
                    }
                }

                itemsIndexed(filteredTemplates, key = { index, t -> "tpl-${t.id}-$index" }) { _, t ->
                    val isSelected = t.id == selectedVariantOfId
                    Surface(
                        onClick = {
                            onSelect(t)
                            onDismiss()
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEEF2FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🧩", fontSize = 18.sp)
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
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5)
                                )
                            }
                        }
                    }
                }
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
    trackable: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        purchaseable: Boolean,
        salable: Boolean,
        component: Boolean,
        assembly: Boolean,
        isTemplate: Boolean,
        virtual: Boolean,
        trackable: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var p by remember { mutableStateOf(purchaseable) }
    var s by remember { mutableStateOf(salable) }
    var c by remember { mutableStateOf(component) }
    var a by remember { mutableStateOf(assembly) }
    var t by remember { mutableStateOf(isTemplate) }
    var v by remember { mutableStateOf(virtual) }
    var tr by remember { mutableStateOf(trackable) }

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
    var showAdvancedOptions by remember { mutableStateOf(t || v || tr) }

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
                        // 1. تتبع بالرقم التسلسلي (trackable)
                        Surface(
                            onClick = { tr = !tr },
                            shape = RoundedCornerShape(12.dp),
                            color = if (tr) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (tr) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("تتبع بالرقم التسلسلي واللوط (trackable)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp))
                                    Text("تتبع كل قطعة برقم تسلسلي خاص عند الاستلام والصرف", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                                }
                                Checkbox(checked = tr, onCheckedChange = { tr = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5)))
                            }
                        }

                        // 2. صنف وهمي / خدمة (virtual)
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

                        // 3. اعتماد كقالب تجريدي أصل (isTemplate)
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
                    onConfirm(p, s, c, a, t, v, tr)
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

    val filteredLocations = remember(locations, searchQuery) {
        if (searchQuery.isBlank()) locations
        else locations.filter {
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
                .fillMaxHeight(0.82f)
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
                item {
                    val isNoneSelected = selectedLocationId == null
                    Surface(
                        onClick = {
                            onSelect(null)
                            onDismiss()
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFE0E7FF)),
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
                                        text = "بدون موقع افتراضي",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "يتم تحديد الموقع عند استلام الشحنات وتخزينها",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            if (isNoneSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5)
                                )
                            }
                        }
                    }
                }

                itemsIndexed(filteredLocations, key = { index, loc -> "loc-picker-${loc.id}-$index" }) { _, loc ->
                    val isSelected = loc.id == selectedLocationId
                    Surface(
                        onClick = {
                            onSelect(loc)
                            onDismiss()
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEEF2FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📍", fontSize = 18.sp)
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
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5)
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
    categories: List<PartCategory> = emptyList(),
    locations: List<StockLocation> = emptyList(),
    templateParts: List<Part> = emptyList(),
    onCreateCategory: ((name: String, description: String) -> PartCategory)? = null,
    onDeleteCategory: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit,
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
        link: String,
        imageUrl: String?,
        active: Boolean,
        locked: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // المستوى الأول: الحقول الأساسية الظاهرة والمباشرة الستة
    var name by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf<Long?>(categories.firstOrNull()?.id) }
    var ipn by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("pcs") }
    var minimumStockText by remember { mutableStateOf("0") }
    var lowStockAlertsEnabled by remember { mutableStateOf(true) }

    // طبيعة التعامل والتصنيع: 3 خيارات واضحة
    // "component" (مكوّن أولي خام) | "assembly" (تجميع داخلي BOM) | "salable" (منتج تجاري جاهز للبيع)
    var selectedNature by remember { mutableStateOf("component") }

    // المستوى الثاني: قسم فرعي مطوي بعنوان "خيارات متقدمة وهندسية"
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var maximumStockText by remember { mutableStateOf("") }
    var revision by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }
    var selectedDefaultLocId by remember { mutableStateOf<Long?>(locations.firstOrNull()?.id) }
    var defaultExpiryDaysText by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var trackable by remember { mutableStateOf(false) }
    var isTemplate by remember { mutableStateOf(false) }
    var virtual by remember { mutableStateOf(false) }
    var selectedVariantOfId by remember { mutableStateOf<Long?>(null) }

    val platformPickerLaunchers = rememberPlatformPickerLaunchers(
        onImageCaptured = { path -> imageUrl = path },
        onImagePicked = { path -> imageUrl = path },
        onFilePicked = { path -> link = path }
    )

    var isSelectCategorySheetOpen by remember { mutableStateOf(false) }
    var isQuickAddCategoryOpen by remember { mutableStateOf(false) }
    var isSelectTemplateSheetOpen by remember { mutableStateOf(false) }
    var isSelectUnitSheetOpen by remember { mutableStateOf(false) }
    var isSelectLocationSheetOpen by remember { mutableStateOf(false) }
    var isLinkInputDialogOpen by remember { mutableStateOf(false) }

    val selectedCategory = remember(selectedCatId, categories) {
        categories.find { it.id == selectedCatId }
    }
    val selectedLocation = remember(selectedDefaultLocId, locations) {
        locations.find { it.id == selectedDefaultLocId }
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

                    val completenessScore = remember(
                        name, ipn, selectedCatId, units, minimumStockText, selectedNature
                    ) {
                        calculatePartCompletenessScore(
                            name = name,
                            ipn = ipn,
                            categoryId = selectedCatId,
                            units = units,
                            minimumStock = minimumStockText.toDoubleOrNull() ?: 0.0,
                            nature = selectedNature
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(Res.string.add_new_part),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )

                        // Completeness Ring Badge
                        val ringColor = when {
                            completenessScore < 40 -> Color(0xFFD97706)
                            completenessScore < 100 -> Color(0xFF4F46E5)
                            else -> Color(0xFF059669)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CompletenessRing(
                                score = completenessScore,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                showText = false
                            )
                            Text(
                                text = "اكتمال البيانات: $completenessScore%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = ringColor
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
                // 1. اسم القطعة - إلزامي
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم القطعة *") },
                    placeholder = { Text("مثال: مقاومة 10K 0805 SMD") },
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

                // 2. التصنيف (categoryId) مع دعم الإنشاء السريع
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "التصنيف (Category) :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )

                    Surface(
                        onClick = { isSelectCategorySheetOpen = true },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selectedCategory != null) Color(0xFFF8FAFC) else Color.White,
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = if (selectedCategory != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
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
                                        .background(Color(0xFFEEF2FF))
                                        .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = selectedCategory?.name ?: "بدون تصنيف (عام)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = selectedCategory?.description?.ifBlank { "اضغط لاختيار التصنيف الهندسي المناسب" } ?: "صنف عام غير مرتبط بفيئة محددة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (onCreateCategory != null) {
                                    Surface(
                                        onClick = { isQuickAddCategoryOpen = true },
                                        color = Color(0xFFECFDF5),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "إنشاء سريع",
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "جديد",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF059669)
                                                )
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    color = Color(0xFFEEF2FF),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                                ) {
                                    Text(
                                        text = if (selectedCategory != null) "تغيير" else "اختر ▾",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF4F46E5),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. رقم القطعة الداخلي (IPN) مع زر توليد كود
                OutlinedTextField(
                    value = ipn,
                    onValueChange = { ipn = it },
                    label = { Text("رقم القطعة الداخلي (IPN)") },
                    placeholder = { Text("مثال: ELEC-RES-10K") },
                    singleLine = true,
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            if (ipn.isNotEmpty()) {
                                IconButton(onClick = { ipn = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "مسح",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            Surface(
                                onClick = {
                                    val randomNum = (100000..999999).random()
                                    ipn = "IPN-$randomNum"
                                },
                                color = Color(0xFFEEF2FF),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "توليد الباركود",
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "توليد",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF4F46E5)
                                        )
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                // 4. وحدة القياس (units) + 5. الحد الأدنى للمخزون (minimumStock) متناسقان بالارتفاع
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = units,
                        onValueChange = { units = it },
                        label = { Text("وحدة القياس") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { isSelectUnitSheetOpen = true }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "اختر الوحدة",
                                    tint = Color(0xFF4F46E5)
                                )
                            }
                        },
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

                // 6. طبيعة التعامل والتصنيع (مبسطة في 3 خيارات مباشرة)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "طبيعة التعامل والتصنيع (نوع الصنف) * :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )

                    // Option 1: مكوّن أولي خام (component)
                    val isComponentSelected = selectedNature == "component"
                    Surface(
                        onClick = { selectedNature = "component" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isComponentSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isComponentSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isComponentSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        tint = if (isComponentSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "مكوّن أولي خام (Component)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
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

                            RadioButton(
                                selected = isComponentSelected,
                                onClick = { selectedNature = "component" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                            )
                        }
                    }

                    // Option 2: تجميع داخلي (assembly)
                    val isAssemblySelected = selectedNature == "assembly"
                    Surface(
                        onClick = { selectedNature = "assembly" },
                        shape = RoundedCornerShape(12.dp),
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isAssemblySelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountTree,
                                        contentDescription = null,
                                        tint = if (isAssemblySelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "تجميع داخلي (Assembly / BOM)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
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

                            RadioButton(
                                selected = isAssemblySelected,
                                onClick = { selectedNature = "assembly" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                            )
                        }
                    }

                    // Option 3: منتج تجاري جاهز للبيع (salable)
                    val isSalableSelected = selectedNature == "salable"
                    Surface(
                        onClick = { selectedNature = "salable" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSalableSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSalableSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSalableSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = if (isSalableSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "منتج تجاري جاهز للبيع (Salable Product)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
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

                            RadioButton(
                                selected = isSalableSelected,
                                onClick = { selectedNature = "salable" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                            )
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
                            val uriHandler = LocalUriHandler.current
                            if (imageUrl.isNotBlank() || link.isNotBlank()) {
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
                                        if (imageUrl.isNotBlank()) {
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
                                                    Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                                                    Text(
                                                        text = "صورة الصنف: $imageUrl",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                        color = Color(0xFF0F172A),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                IconButton(onClick = { imageUrl = "" }, modifier = Modifier.size(22.dp)) {
                                                    Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }

                                        if (link.isNotBlank()) {
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
                                                    Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
                                                    Text(
                                                        text = "المستند/الرابط: $link",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                        color = Color(0xFF0F172A),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                IconButton(onClick = { link = "" }, modifier = Modifier.size(22.dp)) {
                                                    Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. اختيار الصنف القالب/الأصل (variantOfId)
                        if (templateParts.isNotEmpty()) {
                            val selectedTemplate = remember(selectedVariantOfId, templateParts) {
                                templateParts.find { it.id == selectedVariantOfId }
                            }

                            Text(
                                text = "مشتق من / متغير عن (variantOfId) :",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF0F172A)
                            )

                            Surface(
                                onClick = { isSelectTemplateSheetOpen = true },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selectedTemplate != null) Color(0xFFF8FAFC) else Color.White,
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    color = if (selectedTemplate != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
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
                                                .background(Color(0xFFEEF2FF))
                                                .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Extension,
                                                contentDescription = null,
                                                tint = Color(0xFF4F46E5),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = selectedTemplate?.name ?: "صنف أصلي مستقل (بدون قالب أصل)",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                ),
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = if (selectedTemplate != null) "IPN: ${selectedTemplate.ipn.ifBlank { "-" }}" else "قطعة قائمة بذاتها وغير مشتقة من قالب آخر",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Surface(
                                        color = Color(0xFFEEF2FF),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                                    ) {
                                        Text(
                                            text = if (selectedTemplate != null) "تغيير" else "اختر ▾",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF4F46E5),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. الكلمات المفتاحية والوسوم (keywords)
                        OutlinedTextField(
                            value = keywords,
                            onValueChange = { keywords = it },
                            label = { Text("الكلمات المفتاحية والوسوم (keywords)") },
                            placeholder = { Text("وسوم مثل: #SMD #0805 #Original") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, tint = Color(0xFF4F46E5)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        // 4. وصف القطعة التفصيلي (description)
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

                        // 5. الحد الأقصى للمخزون + أيام الصلاحية الافتراضية
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

                        // 6. الإصدار الهندسي (revision)
                        OutlinedTextField(
                            value = revision,
                            onValueChange = { revision = it },
                            label = { Text("الإصدار الهندسي (revision)") },
                            placeholder = { Text("مثال: Rev A أو v1.2") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        // 7. موقع التخزين الافتراضي (defaultLocationId)
                        if (locations.isNotEmpty()) {
                            Text(
                                text = "موقع التخزين الافتراضي (defaultLocationId) :",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF0F172A)
                            )

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
                                                .background(Color(0xFFEEF2FF))
                                                .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = Color(0xFF4F46E5),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = selectedLocation?.name ?: "بدون موقع افتراضي",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                ),
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = selectedLocation?.description?.ifBlank { "اضغط لاختيار موقع التخزين" } ?: "موقع التخزين في المستودع",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Surface(
                                        color = Color(0xFFEEF2FF),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                                    ) {
                                        Text(
                                            text = if (selectedLocation != null) "تغيير" else "اختر ▾",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF4F46E5),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

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

                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                // 2. isTemplate (صنف قالب أصل)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isTemplate = !isTemplate },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Extension,
                                            contentDescription = null,
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text("صنف قالب أصل (isTemplate)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("تُشتق منه قطع ومتغيرات فرعية أخرى", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp), color = Color(0xFF64748B))
                                        }
                                    }
                                    Switch(
                                        checked = isTemplate,
                                        onCheckedChange = { isTemplate = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF4F46E5))
                                    )
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                // 3. trackable (تتبع الأرقام التسلسلية)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { trackable = !trackable },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = null,
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text("تتبع الأرقام التسلسلية (trackable)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("تتبع الشحنات بالرقم التسلسلي والأرقام الفردية", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp), color = Color(0xFF64748B))
                                        }
                                    }
                                    Switch(
                                        checked = trackable,
                                        onCheckedChange = { trackable = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0284C7))
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons
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
                                val isComp = selectedNature == "component"
                                val isAssy = selectedNature == "assembly"
                                val isSale = selectedNature == "salable"
                                val isPurch = isComp || isSale

                                onConfirm(
                                    name,
                                    ipn,
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
                                    trackable,
                                    isPurch,
                                    isSale,
                                    virtual,
                                    selectedDefaultLocId,
                                    defaultExpiryDaysText.toIntOrNull(),
                                    link,
                                    imageUrl.ifBlank { null },
                                    true,
                                    false
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
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
            selectedVariantOfId = selectedVariantOfId,
            onDismiss = { isSelectTemplateSheetOpen = false },
            onSelect = { selectedVariantOfId = it?.id }
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
        var tempLink by remember { mutableStateOf(link) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                            link = tempLink
                            isLinkInputDialogOpen = false
                        },
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
    parts: List<Part>,
    initialLowStock: Boolean,
    initialAssembly: Boolean,
    initialComponent: Boolean,
    initialPurchaseable: Boolean,
    initialSalable: Boolean,
    initialStarred: Boolean,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: (
        categoryId: Long?,
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
    var selectedCatIdState by remember { mutableStateOf(selectedCategoryId) }
    var lowStockOnly by remember { mutableStateOf(initialLowStock) }
    var assemblyOnly by remember { mutableStateOf(initialAssembly) }
    var componentOnly by remember { mutableStateOf(initialComponent) }
    var purchaseableOnly by remember { mutableStateOf(initialPurchaseable) }
    var salableOnly by remember { mutableStateOf(initialSalable) }
    var starredOnly by remember { mutableStateOf(initialStarred) }

    val calculatedCount = remember(
        parts, searchQuery, selectedCatIdState,
        lowStockOnly, assemblyOnly, componentOnly, purchaseableOnly, salableOnly, starredOnly
    ) {
        val query = searchQuery.trim()
        parts.count { part ->
            val matchesCategory = selectedCatIdState == null || part.categoryId == selectedCatIdState
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

            matchesCategory && matchesQuery && matchesLowStock && matchesAssembly && matchesComponent && matchesPurchaseable && matchesSalable
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
                        selectedCatIdState = null
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
                                    text = "🏷️ التصنيف الهرمي (${categories.size})",
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
                                    text = "⚙️ طبيعة التعامل والنوع",
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

                // 3. محتوى التبويب المختار
                val filteredCategories = remember(categories, searchQuery) {
                    if (searchQuery.isBlank()) categories
                    else categories.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
                    }
                }

                if (selectedTab == 0) {
                    // تبويب الفئات
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // خيار عرض الكل
                        val isAllSelected = selectedCatIdState == null
                        Surface(
                            onClick = { selectedCatIdState = null },
                            shape = RoundedCornerShape(12.dp),
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
                                Text(
                                    text = "جميع التصنيفات والفئات",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                if (isAllSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        filteredCategories.forEach { category ->
                            val isSelected = selectedCatIdState == category.id
                            Surface(
                                onClick = { selectedCatIdState = category.id },
                                shape = RoundedCornerShape(12.dp),
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = Color(0xFF0F172A)
                                        )
                                        if (category.description.isNotBlank()) {
                                            Text(
                                                text = category.description,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    if (isSelected) {
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
                } else {
                    // تبويب طبيعة التعامل والخصائص
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
                                    .clickable { purchaseableOnly = !purchaseableOnly }
                            ) {
                                Checkbox(
                                    checked = purchaseableOnly,
                                    onCheckedChange = { purchaseableOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Text(
                                    "أصناف قابلة للشراء (Purchaseable)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { salableOnly = !salableOnly }
                            ) {
                                Checkbox(
                                    checked = salableOnly,
                                    onCheckedChange = { salableOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Text(
                                    "أصناف قابلة للبيع (Salable)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { componentOnly = !componentOnly }
                            ) {
                                Checkbox(
                                    checked = componentOnly,
                                    onCheckedChange = { componentOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Text(
                                    "مكونات فرعية (Component)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { assemblyOnly = !assemblyOnly }
                            ) {
                                Checkbox(
                                    checked = assemblyOnly,
                                    onCheckedChange = { assemblyOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Text(
                                    "منتجات مجمعة (Assembly)",
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
                                    "منخفضة المخزون فقط (Low Stock Only)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { starredOnly = !starredOnly }
                            ) {
                                Checkbox(
                                    checked = starredOnly,
                                    onCheckedChange = { starredOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Text(
                                    "الأصناف المفضلة فقط (Starred Items)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                )
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
                                selectedCatIdState,
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

