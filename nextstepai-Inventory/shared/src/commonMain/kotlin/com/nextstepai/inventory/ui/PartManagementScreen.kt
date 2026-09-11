package com.nextstepai.inventory.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartCategory
import com.nextstepai.inventory.repository.PartsSummary

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
                title = { Text("إدارة القطع والمكونات (Part)") },
                navigationIcon = {
                    if (onBackClick != null) {
                        TextButton(onClick = onBackClick) {
                            Text("➔ العودة", fontSize = 14.sp)
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.setAddPartDialogOpen(true) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("+ إضافة قطعة")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // شريط إحصائيات القطع المكون من حقول التقرير المحسوبة
            PartSummaryCards(summary = uiState.summary)

            Spacer(modifier = Modifier.height(12.dp))

            // شريط البحث المطور
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("البحث باسم القطعة، IPN، الوصف، أو الكلمات المفتاحية...") },
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
                onCategorySelect = { viewModel.onCategorySelected(it) },
                onToggleLowStock = { viewModel.toggleLowStockFilter() },
                onToggleAssembly = { viewModel.toggleAssemblyFilter() }
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
                        text = "لا توجد قطع مطابقة لشروط البحث.",
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
                            onClick = { viewModel.selectPart(part) }
                        )
                    }
                }
            }
        }
    }

    // حوار عرض التفاصيل الكاملة لأعمدة سجل القطعة Selected Part
    if (uiState.selectedPart != null) {
        PartDetailsDialog(
            part = uiState.selectedPart!!,
            onDismiss = { viewModel.selectPart(null) }
        )
    }

    // حوار إضافة قطعة جديدة
    if (uiState.isAddPartDialogOpen) {
        AddPartDialog(
            categories = uiState.categories,
            templateParts = uiState.templateParts,
            onDismiss = { viewModel.setAddPartDialogOpen(false) },
            onConfirm = { name, ipn, desc, catId, units, assembly, component, isTemplate, variantOf, minStock, initStock ->
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
                    initialStock = initStock
                )
            }
        )
    }
}

/**
 * شريط الإحصائيات لمكونات جدول Part.
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
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "نقص المخزون",
            value = summary.lowStockParts.toString(),
            containerColor = if (summary.lowStockParts > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (summary.lowStockParts > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "تجميعات BOM",
            value = summary.assemblyParts.toString(),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "قوالب Templates",
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
    onCategorySelect: (Long?) -> Unit,
    onToggleLowStock: () -> Unit,
    onToggleAssembly: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelect(null) },
                label = { Text("الكل") }
            )
        }
        item {
            FilterChip(
                selected = lowStockOnly,
                onClick = onToggleLowStock,
                label = { Text("⚠️ منخفض المخزون") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                )
            )
        }
        item {
            FilterChip(
                selected = assemblyOnly,
                onClick = onToggleAssembly,
                label = { Text("⚙️ تجميعة (Assembly)") }
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
 * بطاقة عرض القطعة القابلة للنقر لتفاصيل Part.
 */
@Composable
private fun PartItemCard(
    part: Part,
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
                Text(
                    text = part.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )

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
 * حوار عرض تفاصيل السجل الكاملة لجدول Part وفق الأعمدة الموضحة في وثيقة التحليل.
 */
@Composable
private fun PartDetailsDialog(
    part: Part,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        },
        title = {
            Text("تفاصيل القطعة: ${part.name}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
                DetailRow("تاريخ الإنشاء (creation_date):", part.creationDate.ifBlank { "2025-02-15" })
            }
        }
    )
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
    var initialStockText by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة قطعة جديدة (Part)", fontWeight = FontWeight.Bold) },
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
                            initialStockText.toDoubleOrNull() ?: 0.0
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("حفظ القطعة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
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
                        label = { Text("وحدة القياس (units)") },
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
