package com.nextstepai.inventory.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartAttachment
import com.nextstepai.inventory.data.PartCategory
import com.nextstepai.inventory.data.PartNotes
import com.nextstepai.inventory.data.PartParameter
import com.nextstepai.inventory.data.PartRelatedView
import com.nextstepai.inventory.data.PartTestTemplate
import com.nextstepai.inventory.data.db.PartInternalPriceEntity
import com.nextstepai.inventory.data.db.PartPricingEntity
import com.nextstepai.inventory.data.db.PartSalePriceEntity
import com.nextstepai.inventory.repository.PartsSummary
import com.nextstepai.inventory.ui.theme.AppIcons
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_part
import nextstepai_inventory.shared.generated.resources.back
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.card_parts_title
import nextstepai_inventory.shared.generated.resources.filter_assembly
import nextstepai_inventory.shared.generated.resources.filter_low_stock
import nextstepai_inventory.shared.generated.resources.filter_starred
import nextstepai_inventory.shared.generated.resources.parts_count
import nextstepai_inventory.shared.generated.resources.save
import nextstepai_inventory.shared.generated.resources.search_placeholder

/**
 * شاشة إدارة القطع والمكونات الأساسية (Part Management Screen).
 * تعرض شريط البحث، الفلترة حسب التصنيف والمخزون، إحصائيات القطع، تفاصيل حقول Part الحقيقية، وحوار إضافة قطعة.
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
            TopAppBar(
                title = { Text(stringResource(Res.string.card_parts_title)) },
                navigationIcon = {
                    if (onBackClick != null) {
                        TextButton(onClick = onBackClick) {
                            Icon(
                                painter = painterResource(AppIcons.Back),
                                contentDescription = stringResource(Res.string.back),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(Res.string.back))
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.setAddPartDialogOpen(true) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(AppIcons.Add),
                            contentDescription = stringResource(Res.string.add_new_part),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.add_new_part))
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            val isWideScreen = this@BoxWithConstraints.maxWidth > 720.dp

            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(if (isWideScreen && uiState.selectedPart != null) 1f else 1f)
                ) {
                    // شريط إحصائيات القطع المكون من حقول التقرير المحسوبة
                    PartSummaryCards(summary = uiState.summary)

                    Spacer(modifier = Modifier.height(12.dp))

                    // شريط البحث المطور
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(AppIcons.Search),
                                contentDescription = stringResource(Res.string.search_placeholder),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))

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
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
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
    }

    // حوار عرض التفاصيل الكاملة المتبوبة لجميع جداول القطعة
    if (uiState.selectedPart != null) {
        PartDetailsDialog(
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
            onRecalculatePricing = { viewModel.recalculatePartPricing() },
            onAddInternalPrice = { qty, prc, curr -> viewModel.addPartInternalPrice(qty, prc, curr) },
            onAddSalePrice = { qty, prc, curr -> viewModel.addPartSalePrice(qty, prc, curr) },
            onDismiss = { viewModel.selectPart(null) }
        )
    }

    // حوار إضافة قطعة جديدة
    if (uiState.isAddPartDialogOpen) {
        AddPartDialog(
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
 * شريط الإحصائيات لمكونات جدول Part باستخدام plurals.
 */
@Composable
private fun PartSummaryCards(summary: PartsSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(
            title = pluralStringResource(Res.plurals.parts_count, summary.totalParts, summary.totalParts),
            value = summary.totalParts.toString(),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = stringResource(Res.string.filter_low_stock),
            value = summary.lowStockParts.toString(),
            containerColor = if (summary.lowStockParts > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (summary.lowStockParts > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = stringResource(Res.string.filter_assembly),
            value = summary.assemblyParts.toString(),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "Templates",
            value = summary.templateParts.toString(),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    containerColor: Color,
    contentColor: Color = contentColorFor(containerColor),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = title, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * صف تصفية الفئات والفلاتر المباشرة.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            FilterChip(
                selected = selectedCategoryId == null && !starredOnly,
                onClick = { onCategorySelect(null) },
                label = { Text("الكل") }
            )
        }
        item {
            FilterChip(
                selected = starredOnly,
                onClick = onToggleStarred,
                leadingIcon = {
                    Icon(
                        painter = painterResource(AppIcons.StarFilled),
                        contentDescription = stringResource(Res.string.filter_starred),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(stringResource(Res.string.filter_starred)) }
            )
        }
        item {
            FilterChip(
                selected = lowStockOnly,
                onClick = onToggleLowStock,
                label = { Text(stringResource(Res.string.filter_low_stock)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                )
            )
        }
        item {
            FilterChip(
                selected = assemblyOnly,
                onClick = onToggleAssembly,
                label = { Text(stringResource(Res.string.filter_assembly)) }
            )
        }
        items(categories, key = { "category-${it.id}" }) { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onCategorySelect(if (selectedCategoryId == category.id) null else category.id) },
                label = { Text(category.name) }
            )
        }
    }
}

/**
 * بطاقة عرض القطعة القابلة للنقر لتفاصيل Part مع زر التفضيل والمتابعة ⭐ (PartStar).
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleStar, modifier = Modifier.size(28.dp)) {
                        Icon(
                            painter = painterResource(if (isStarred) AppIcons.StarFilled else AppIcons.Star),
                            contentDescription = stringResource(Res.string.filter_starred),
                            tint = if (isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = part.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (part.ipn.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "IPN: ${part.ipn}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (part.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = part.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // العلامات المنطقية للقطعة (Flags & Tags)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (part.isTemplate) {
                    BadgeTag("قالب (Template)", MaterialTheme.colorScheme.tertiaryContainer)
                }
                if (part.variantOfId != null) {
                    BadgeTag("مشتق (Variant)", MaterialTheme.colorScheme.secondaryContainer)
                }
                if (part.assembly) {
                    BadgeTag("تجميعة (Assembly)", MaterialTheme.colorScheme.surfaceVariant)
                }
                if (part.trackable) {
                    BadgeTag("تتبع أرقام تسلسلية", MaterialTheme.colorScheme.primaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // مؤشر كمية المخزون المتاح والتنبيهات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المخزون المتاح: ${part.availableStock} ${part.units} (من أصل ${part.totalInStock})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (part.isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                if (part.isLowStock) {
                    Text(
                        text = "⚠️ نقص في المخزون (< ${part.minimumStock})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeTag(text: String, color: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * حوار عرض تفاصيل السجل الكاملة المتبوبة المباشرة لجدول Part وكافة الجداول والكيانات الفرعية الثمانية (بما فيها التسعير PartPricing والشرائح PartInternalPrice).
 */
@Composable
private fun PartDetailsDialog(
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
    onRecalculatePricing: () -> Unit = {},
    onAddInternalPrice: (quantity: Double, price: Double, currency: String) -> Unit = { _, _, _ -> },
    onAddSalePrice: (quantity: Double, price: Double, currency: String) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var isAddInternalPriceDialogOpen by remember { mutableStateOf(false) }
    var isAddSalePriceDialogOpen by remember { mutableStateOf(false) }
    val tabTitles = listOf(
        "📊 العام",
        "⚙️ الـ BOM (${bomItems.size})",
        "📐 المعاملات (${parameters.size})",
        "🧪 الفحوصات (${testTemplates.size})",
        "🔗 الصلة (${relatedParts.size})",
        "📁 المرفقات (${attachments.size})",
        "📝 الملاحظات",
        "💰 التسعير"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        title = {
            Column {
                Text("تفاصيل القطعة: ${part.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(6.dp))
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 0.dp
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontSize = 11.sp, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> { // البيانات العامة والتجارية
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
                        DetailRow("افتراضي/افتراضي (virtual):", if (part.virtual) "نعم" else "لا")
                        DetailRow("حالة النشاط (active):", if (part.active) "نشط" else "مؤرشف")
                        HorizontalDivider()
                        DetailRow("إجمالي المخزون (totalInStock):", "${part.totalInStock} ${part.units}")
                        DetailRow("محجوز للتصنيع (allocatedToBuild):", "${part.allocatedToBuildOrders} ${part.units}")
                        DetailRow("محجوز للبيع (allocatedToSales):", "${part.allocatedToSalesOrders} ${part.units}")
                        DetailRow("الصافي المتاح (availableStock):", "${part.availableStock} ${part.units}")
                        DetailRow("الحد الأدنى للتنبيه (minimumStock):", "${part.minimumStock} ${part.units}")
                        DetailRow("الحد الأقصى للمخزون (maximumStock):", part.maximumStock?.let { "$it ${part.units}" } ?: "-")
                        DetailRow("تاريخ الإنشاء (creation_date):", part.creationDate.ifBlank { "2025-02-15" })
                    }
                    1 -> { // قائمة التركيب والتصنيع BOM & Substitutes
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
                    2 -> { // المعاملات والمواصفات الفنية
                        Text("المعاملات الفنية للقطعة (Part Parameters):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (parameters.isEmpty()) {
                            Text("لا توجد مواصفات فنية مسجلة لهذه القطعة.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            for (param in parameters) {
                                DetailRow("المعامل #${param.templateId}:", param.value.ifBlank { "-" })
                            }
                        }
                    }
                    3 -> { // قوالب اختبارات الجودة
                        Text("قوالب الفحوصات واختبارات الجودة (PartTestTemplate):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                    4 -> { // القطع ذات الصلة
                        Text("القطع ذات الصلة والبدائل الشبيهة (PartRelated):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (relatedParts.isEmpty()) {
                            Text("لا توجد قطع ذات صلة مقترنة بهذه القطعة.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            for (rel in relatedParts) {
                                DetailRow("• ${rel.relatedPart.name}", rel.relatedPart.ipn.ifBlank { "قطعة #${rel.relatedPart.id}" })
                            }
                        }
                    }
                    5 -> { // المرفقات والوثائق
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
                    6 -> { // الملاحظات وسجل التدقيق
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
                    7 -> { // التكاليف والأسعار المحسوبة PartPricing وشرائح الأسعار PartInternalPrice
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
                }
            }
        }
    )

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

/**
 * حوار إضافة قطعة جديدة بالحقوق والخيارات الأساسية.
 */
@Composable
private fun AddPartDialog(
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_new_part), fontWeight = FontWeight.Bold) },
        confirmButton = {
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
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم القطعة (name) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ipn,
                    onValueChange = { ipn = it },
                    label = { Text("رقم القطعة الداخلي (IPN)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف القطعة (description)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = units,
                        onValueChange = { units = it },
                        label = { Text("وحدة القياس") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minimumStockText,
                        onValueChange = { minimumStockText = it },
                        label = { Text("الحد الأدنى") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maximumStockText,
                        onValueChange = { maximumStockText = it },
                        label = { Text("الحد الأقصى") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = initialStockText,
                    onValueChange = { initialStockText = it },
                    label = { Text("المخزون الأولي (totalInStock)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // الخيارات المنطقية (Flags)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = assembly, onCheckedChange = { assembly = it })
                    Text("منتج مجمع (assembly / BOM)", fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = component, onCheckedChange = { component = it })
                    Text("مكون فرعي في تجميعات (component)", fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isTemplate, onCheckedChange = { isTemplate = it })
                    Text("قالب تجريدي (is_template)", fontSize = 12.sp)
                }
            }
        }
    )
}

/**
 * حوار إضافة شريحة سعرية بيع/تحويل داخلي جديدة (PartInternalPrice Modal).
 */
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
                enabled = (quantityText.toDoubleOrNull() ?: 0.0) >= 1.0 && (priceText.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("الحد الأدنى للكمية (quantity >= 1) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("سعر الوحدة الواحدة (price) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text("العملة (price_currency)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

/**
 * حوار إضافة شريحة سعر بيع للعملاء جديدة (PartSalePrice Modal).
 */
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
                enabled = (quantityText.toDoubleOrNull() ?: 0.0) >= 1.0 && (priceText.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("الحد الأدنى للكمية (quantity >= 1) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("سعر بيع الوحدة الواحدة للعميل (price) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text("العملة (price_currency)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
