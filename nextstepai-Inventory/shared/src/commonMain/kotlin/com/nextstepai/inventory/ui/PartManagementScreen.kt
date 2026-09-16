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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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
import com.nextstepai.inventory.data.SupplierPart
import com.nextstepai.inventory.data.db.PartInternalPriceEntity
import com.nextstepai.inventory.data.db.PartPricingEntity
import com.nextstepai.inventory.data.db.PartSalePriceEntity
import com.nextstepai.inventory.repository.PartsSummary
import com.nextstepai.inventory.ui.components.CurrencySelectionBottomSheet
import com.nextstepai.inventory.ui.components.CurrencySelectorField
import com.nextstepai.inventory.ui.components.SearchableCompanyPickerDialog
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_part
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.card_parts_title
import nextstepai_inventory.shared.generated.resources.filter_assembly
import nextstepai_inventory.shared.generated.resources.filter_low_stock
import nextstepai_inventory.shared.generated.resources.filter_starred
import nextstepai_inventory.shared.generated.resources.parts_count
import nextstepai_inventory.shared.generated.resources.save

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
                        .height(IntrinsicSize.Min),
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
                            IconButton(
                                onClick = { },
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEEF2FF))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "مسح الباركود",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
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
                            if (isFilterActive) {
                                viewModel.onCategorySelected(null)
                                if (uiState.lowStockOnlyFilter) viewModel.toggleLowStockFilter()
                                if (uiState.assemblyOnlyFilter) viewModel.toggleAssemblyFilter()
                                if (uiState.starredOnlyFilter) viewModel.toggleStarredFilter()
                            } else {
                                viewModel.toggleStarredFilter()
                            }
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

                Spacer(modifier = Modifier.height(12.dp))

                // شريط التصفية والفلترة بالرقاقات (Category & Filter Chips)
                FilterChipsRow(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    lowStockOnly = uiState.lowStockOnlyFilter,
                    assemblyOnly = uiState.assemblyOnlyFilter,
                    starredOnly = uiState.starredOnlyFilter,
                    onCategorySelect = { viewModel.onCategorySelected(it) },
                    onToggleLowStock = { viewModel.toggleLowStockFilter() },
                    onToggleAssembly = { viewModel.toggleAssemblyFilter() },
                    onToggleStarred = { viewModel.toggleStarredFilter() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // قائمة القطع
                if (uiState.parts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pluralStringResource(Res.plurals.parts_count, 0, 0),
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
                            text = pluralStringResource(Res.plurals.parts_count, uiState.parts.size, uiState.parts.size),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(uiState.parts, key = { "part-${it.id}" }) { part ->
                            PartItemCard(
                                part = part,
                                isStarred = uiState.starredPartIds.contains(part.id),
                                onToggleStar = { viewModel.togglePartStar(part.id) },
                                onClick = { viewModel.selectPart(part) }
                            )
                        }
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
            templateParts = uiState.templateParts,
            onDismiss = { viewModel.setAddPartDialogOpen(false) },
            onConfirm = { name, ipn, desc, catId, units, assembly, component, isTemplate, variantOf, minStock, maxStock, initStock ->
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
                    initialStock = initStock
                )
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
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
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
 * صف تصفية الفئات والفلاتر المباشرة
 */
@Composable
private fun FilterChipsRow(
    categories: List<PartCategory>,
    selectedCategoryId: Long?,
    lowStockOnly: Boolean,
    assemblyOnly: Boolean,
    starredOnly: Boolean = false,
    onCategorySelect: (Long?) -> Unit,
    onToggleLowStock: () -> Unit,
    onToggleAssembly: () -> Unit,
    onToggleStarred: () -> Unit = {}
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            val isAllSelected = selectedCategoryId == null && !lowStockOnly && !assemblyOnly && !starredOnly
            Surface(
                onClick = { onCategorySelect(null) },
                shape = RoundedCornerShape(10.dp),
                color = if (isAllSelected) Color(0xFF4F46E5) else Color.White,
                border = BorderStroke(1.dp, if (isAllSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = if (isAllSelected) 2.dp else 0.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = "الكل",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Surface(
                onClick = onToggleStarred,
                shape = RoundedCornerShape(10.dp),
                color = if (starredOnly) Color(0xFF4F46E5) else Color.White,
                border = BorderStroke(1.dp, if (starredOnly) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = if (starredOnly) 2.dp else 0.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = if (starredOnly) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = stringResource(Res.string.filter_starred),
                        tint = if (starredOnly) Color.White else Color(0xFFD97706),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(Res.string.filter_starred),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = if (starredOnly) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        item {
            Surface(
                onClick = onToggleLowStock,
                shape = RoundedCornerShape(10.dp),
                color = if (lowStockOnly) Color(0xFFDC2626) else Color.White,
                border = BorderStroke(1.dp, if (lowStockOnly) Color(0xFFDC2626) else MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = if (lowStockOnly) 2.dp else 0.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 10.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.filter_low_stock),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = if (lowStockOnly) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        item {
            Surface(
                onClick = onToggleAssembly,
                shape = RoundedCornerShape(10.dp),
                color = if (assemblyOnly) Color(0xFF7C3AED) else Color.White,
                border = BorderStroke(1.dp, if (assemblyOnly) Color(0xFF7C3AED) else MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = if (assemblyOnly) 2.dp else 0.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 10.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.filter_assembly),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = if (assemblyOnly) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        items(categories, key = { "category-${it.id}" }) { category ->
            val isSelected = selectedCategoryId == category.id
            Surface(
                onClick = { onCategorySelect(if (isSelected) null else category.id) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) Color(0xFF4F46E5) else Color.White,
                border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = if (isSelected) 2.dp else 0.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * بطاقة عرض القطعة PartItemCard
 */
@Composable
private fun PartItemCard(
    part: Part,
    isStarred: Boolean = false,
    onToggleStar: () -> Unit = {},
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Badges (IPN & Star) on Left (in RTL) + Icon Box on Right (in RTL)
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
                    IconButton(
                        onClick = onToggleStar,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isStarred) Color(0xFFFEF3C7) else Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = stringResource(Res.string.filter_starred),
                            tint = if (isStarred) Color(0xFFD97706) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (part.ipn.isNotBlank()) {
                        Surface(
                            color = Color(0xFFF0FDFA),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFCCFBF1))
                        ) {
                            Text(
                                text = "IPN: ${part.ipn}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFF0D9488),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Category/Type Icon Box
                val (iconBg, iconColor, iconVector) = when {
                    part.assembly -> Triple(Color(0xFFF3E8FF), Color(0xFF9333EA), Icons.Default.Build)
                    part.isTemplate -> Triple(Color(0xFFCCFBF1), Color(0xFF0D9488), Icons.Default.Description)
                    else -> Triple(Color(0xFFEEF2FF), Color(0xFF4F46E5), Icons.Default.Memory)
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg)
                        .border(1.dp, iconColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
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

            Spacer(modifier = Modifier.height(8.dp))

            // Part Name & Description
            Text(
                text = part.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = Color(0xFF0F172A)
            )

            if (part.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = part.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFFF1F5F9)
            )

            // Stock Status Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (part.isLowStock) Color(0xFFFEF2F2) else Color(0xFFECFDF5),
                border = BorderStroke(1.dp, if (part.isLowStock) Color(0xFFFCA5A5) else Color(0xFFA7F3D0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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

            Spacer(modifier = Modifier.height(14.dp))

            // Tags & Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Role/Property Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (part.assembly) {
                        Surface(
                            color = Color(0xFFE11D48),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "تجميعة (Assembly)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (part.trackable) {
                        Surface(
                            color = Color(0xFF4F46E5),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "تتبع أرقام تسلسلية",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (part.isTemplate) {
                        Surface(
                            color = Color(0xFF0D9488),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "قالب (Template)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (part.variantOfId != null) {
                        Surface(
                            color = Color(0xFF0284C7),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "مشتق (Variant)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Action detail button
                Surface(
                    onClick = onClick,
                    color = Color(0xFFEEF2FF),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "التفاصيل",
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(15.dp)
                        )
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
        AddInternalPriceDialog(
            onDismiss = { isAddInternalPriceDialogOpen = false },
            onConfirm = { qty, prc, curr ->
                onAddInternalPrice(qty, prc, curr)
                isAddInternalPriceDialogOpen = false
            }
        )
    }

    if (isAddSalePriceDialogOpen) {
        AddSalePriceDialog(
            onDismiss = { isAddSalePriceDialogOpen = false },
            onConfirm = { qty, prc, curr ->
                onAddSalePrice(qty, prc, curr)
                isAddSalePriceDialogOpen = false
            }
        )
    }

    if (isAddMfgPartDialogOpen) {
        AddManufacturerPartForPartDialog(
            companies = allCompanies,
            onDismiss = { isAddMfgPartDialogOpen = false },
            onConfirm = { mfgId, mpn, desc, link ->
                onAddManufacturerPart(mfgId, mpn, desc, link)
                isAddMfgPartDialogOpen = false
            }
        )
    }

    if (isAddSupPartDialogOpen) {
        AddSupplierPartForPartDialog(
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
internal fun AddPartBottomSheet(
    categories: List<PartCategory>,
    templateParts: List<Part>,
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
        initialStock: Double
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var ipn by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("pcs") }
    var selectedCatId by remember { mutableStateOf<Long?>(categories.firstOrNull()?.id) }
    var assembly by remember { mutableStateOf(false) }
    var component by remember { mutableStateOf(true) }
    var isTemplate by remember { mutableStateOf(false) }
    var selectedVariantOfId by remember { mutableStateOf<Long?>(null) }
    var minimumStockText by remember { mutableStateOf("0") }
    var maximumStockText by remember { mutableStateOf("") }
    var initialStockText by remember { mutableStateOf("0") }

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
                        text = stringResource(Res.string.add_new_part),
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم القطعة (name) *") },
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
                    value = ipn,
                    onValueChange = { ipn = it },
                    label = { Text("رقم القطعة الداخلي (IPN)") },
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
                    label = { Text("وصف القطعة (description)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = units,
                        onValueChange = { units = it },
                        label = { Text("وحدة القياس") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    OutlinedTextField(
                        value = minimumStockText,
                        onValueChange = { minimumStockText = it },
                        label = { Text("الحد الأدنى") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    OutlinedTextField(
                        value = maximumStockText,
                        onValueChange = { maximumStockText = it },
                        label = { Text("الحد الأقصى") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                OutlinedTextField(
                    value = initialStockText,
                    onValueChange = { initialStockText = it },
                    label = { Text("المخزون الأولي (totalInStock)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { assembly = !assembly }
                        ) {
                            Checkbox(
                                checked = assembly,
                                onCheckedChange = { assembly = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("منتج مجمع (assembly / BOM)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { component = !component }
                        ) {
                            Checkbox(
                                checked = component,
                                onCheckedChange = { component = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مكون فرعي في تجميعات (component)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isTemplate = !isTemplate }
                        ) {
                            Checkbox(
                                checked = isTemplate,
                                onCheckedChange = { isTemplate = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("قالب تجريدي (is_template)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
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
                                onConfirm(
                                    name,
                                    ipn,
                                    description,
                                    selectedCatId,
                                    units,
                                    assembly,
                                    component,
                                    isTemplate,
                                    selectedVariantOfId,
                                    minimumStockText.toDoubleOrNull() ?: 0.0,
                                    maximumStockText.toDoubleOrNull(),
                                    initialStockText.toDoubleOrNull() ?: 0.0
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
}

@Composable
private fun AddInternalPriceDialog(
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, price: Double, currency: String) -> Unit
) {
    var quantityText by remember { mutableStateOf("1.0") }
    var priceText by remember { mutableStateOf("0.0") }
    var currency by remember { mutableStateOf("USD") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("إضافة شريحة سعرية (PartInternalPrice)", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    val q = quantityText.toDoubleOrNull() ?: 1.0
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    if (q >= 1.0 && p > 0.0) {
                        onConfirm(q, p, currency)
                    }
                },
                enabled = (quantityText.toDoubleOrNull() ?: 0.0) >= 1.0 && (priceText.toDoubleOrNull() ?: 0.0) > 0.0,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(Res.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("الحد الأدنى للكمية (quantity >= 1) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("سعر الوحدة الواحدة (price) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
            }
        }
    )
}

@Composable
private fun AddSalePriceDialog(
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, price: Double, currency: String) -> Unit
) {
    var quantityText by remember { mutableStateOf("1.0") }
    var priceText by remember { mutableStateOf("0.0") }
    var currency by remember { mutableStateOf("USD") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("إضافة شريحة سعر بيع للعملاء (PartSalePrice)", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    val q = quantityText.toDoubleOrNull() ?: 1.0
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    if (q >= 1.0 && p > 0.0) {
                        onConfirm(q, p, currency)
                    }
                },
                enabled = (quantityText.toDoubleOrNull() ?: 0.0) >= 1.0 && (priceText.toDoubleOrNull() ?: 0.0) > 0.0,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(Res.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("الحد الأدنى للكمية (quantity >= 1) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("سعر بيع الوحدة الواحدة للعميل (price) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
            }
        }
    )
}

@Composable
private fun AddManufacturerPartForPartDialog(
    companies: List<Company>,
    onDismiss: () -> Unit,
    onConfirm: (manufacturerId: Long, mpn: String, description: String, link: String) -> Unit
) {
    val mfgCompanies = remember(companies) { companies.filter { it.isManufacturer } }
    var selectedCompany by remember { mutableStateOf(mfgCompanies.firstOrNull()) }
    var isCompanyPickerOpen by remember { mutableStateOf(false) }
    var mpn by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة قطعة مصنّع (MPN)", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("المصنّع المعتمد:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = { isCompanyPickerOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedCompany?.name ?: "اختر الشركة المصنعة...")
                }
                OutlinedTextField(
                    value = mpn,
                    onValueChange = { mpn = it },
                    label = { Text("رقم قطعة المصنع (MPN)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف الفني") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("رابط كراسة المواصفات (Datasheet)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedCompany != null && mpn.isNotBlank()) {
                        onConfirm(selectedCompany!!.id, mpn, description, link)
                    }
                },
                enabled = selectedCompany != null && mpn.isNotBlank()
            ) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )

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

@Composable
private fun AddSupplierPartForPartDialog(
    companies: List<Company>,
    mfgParts: List<ManufacturerPart>,
    onDismiss: () -> Unit,
    onConfirm: (supplierId: Long, sku: String, mfgPartId: Long?, description: String, link: String, note: String, packaging: String, packQuantity: String) -> Unit
) {
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة قطعة مورّد (SKU)", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("المورّد المعتمد:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = { isCompanyPickerOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedCompany?.name ?: "اختر شركة التوريد...")
                }
                if (mfgParts.isNotEmpty()) {
                    Text("ربط برقم قطع التصنيع (MPN):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    mfgParts.forEach { mp ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMfgPartId = mp.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedMfgPartId == mp.id,
                                onClick = { selectedMfgPartId = mp.id }
                            )
                            Text("MPN: ${mp.mpn} (#${mp.id})", fontSize = 12.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("رمز التوريد (SKU)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = packaging,
                        onValueChange = { packaging = it },
                        label = { Text("نوع التغليف") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = packQuantity,
                        onValueChange = { packQuantity = it },
                        label = { Text("كمية الحزمة") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("رابط صفحة الشراء") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظة التوريد") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedCompany != null && sku.isNotBlank()) {
                        onConfirm(selectedCompany!!.id, sku, selectedMfgPartId, description, link, note, packaging, packQuantity)
                    }
                },
                enabled = selectedCompany != null && sku.isNotBlank()
            ) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )

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

@Composable
private fun ConfirmDeletePartLinkDialog(
    itemTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تأكيد الحذف ⚠️", fontWeight = FontWeight.Bold, color = Color(0xFFE11D48)) },
        text = {
            Text("هل أنت تأكد من إزالة '$itemTitle'؟\n\nتنبيه: في حال كان هذا السجل مرتبطاً بأسعار سابقة أو أوامر شراء، فقد تتأثر تقارير التكلفة المرتبطة به.")
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
            ) {
                Text("حذف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

