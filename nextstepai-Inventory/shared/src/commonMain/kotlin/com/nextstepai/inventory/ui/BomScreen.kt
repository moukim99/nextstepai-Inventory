package com.nextstepai.inventory.ui

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.BomItemSubstituteView
import com.nextstepai.inventory.data.ManufacturingPhase
import com.nextstepai.inventory.data.Part
import kotlinx.coroutines.launch
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_bom_item
import nextstepai_inventory.shared.generated.resources.bom_items_count
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.card_bom_title
import nextstepai_inventory.shared.generated.resources.save

/**
 * شاشة إدارة بنود قائمة مواد التصنيع (BOM Items Screen).
 * مطابقة للهيكل القياسي الموحد للتطبيق مع الترويسة العلوية التكيفية واختيار المنتجات المجمعة والبدائل.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BomScreen(
    viewModel: BomViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            BomTopBar(
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
                Spacer(modifier = Modifier.height(10.dp))

                // شريط البحث والفلترة الموحد
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
                                text = "البحث باسم القطعة، الرقم التسلسلي، أو موقع التخزين...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = Color(0xFF64748B),
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
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )

                    Surface(
                        onClick = { viewModel.setFilterSheetOpen(true) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 14.dp)
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "فلتر",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "فلتر",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // التصفية والفلترة الحسابية للبنود
                val filteredBomItems = remember(
                    uiState.bomItems,
                    uiState.allParts,
                    uiState.searchQuery,
                    uiState.filterCategory,
                    uiState.filterMandatoryOnly,
                    uiState.filterOptionalOnly,
                    uiState.filterConsumableOnly,
                    uiState.filterAllowVariantsOnly
                ) {
                    val query = uiState.searchQuery.trim().lowercase()
                    uiState.bomItems.filter { bomItem ->
                        val comp = uiState.allParts.find { it.id == bomItem.subPartId }

                        val matchesSearch = query.isBlank() ||
                                comp?.name?.lowercase()?.contains(query) == true ||
                                comp?.ipn?.lowercase()?.contains(query) == true ||
                                comp?.description?.lowercase()?.contains(query) == true ||
                                comp?.keywords?.lowercase()?.contains(query) == true ||
                                bomItem.reference.lowercase().contains(query) ||
                                bomItem.note.lowercase().contains(query)

                        val matchesCategory = when (uiState.filterCategory) {
                            "MANDATORY" -> !bomItem.optional
                            "OPTIONAL" -> bomItem.optional
                            "VARIANTS" -> bomItem.allowVariants
                            else -> true
                        }

                        val matchesMandatory = !uiState.filterMandatoryOnly || !bomItem.optional
                        val matchesOptional = !uiState.filterOptionalOnly || bomItem.optional
                        val matchesConsumable = !uiState.filterConsumableOnly || bomItem.consumable
                        val matchesVariants = !uiState.filterAllowVariantsOnly || bomItem.allowVariants

                        matchesSearch && matchesCategory && matchesMandatory && matchesOptional && matchesConsumable && matchesVariants
                    }
                }

                // سطر معلومات المنتج الأب والعدد الإجمالي للبنود
                val selectedParentPart = remember(uiState.parentParts, uiState.selectedPartId) {
                    uiState.parentParts.find { it.id == uiState.selectedPartId }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { viewModel.setFilterSheetOpen(true) },
                        color = Color(0xFFEEF2FF),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "🛠️ ${selectedParentPart?.name ?: "اختر منتجاً"}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                color = Color(0xFF4F46E5),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "تغيير",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "إجمالي البنود: ${filteredBomItems.size}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "⚠️ ${uiState.errorMessage}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp
                        )
                    }
                }

                if (filteredBomItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isNotBlank() || uiState.filterCategory != "ALL" || uiState.filterMandatoryOnly || uiState.filterOptionalOnly || uiState.filterConsumableOnly || uiState.filterAllowVariantsOnly) "لا توجد بنود قائمة مواد مطابقة لمعايير البحث أو التصفية الحالية." else "لا توجد مكونات فرعية معرفة لهذا المنتج الأب بعد. اضغط على 'إضافة مكون' لبناء قائمة المواد.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(filteredBomItems, key = { index, it -> "bom-${it.uuid.ifBlank { it.id.toString() }}-$index" }) { _, bomItem ->
                            val component = uiState.allParts.find { it.id == bomItem.subPartId }
                            val substitutes = uiState.substitutesMap[bomItem.id] ?: emptyList()
                            BomItemCard(
                                bomItem = bomItem,
                                component = component,
                                substitutes = substitutes,
                                phases = uiState.phases,
                                onEditClick = { viewModel.openEditBomItemDialog(bomItem) },
                                onDeleteClick = { viewModel.deleteBomItem(uuid = bomItem.uuid, id = bomItem.id) },
                                onAddSubstituteClick = { viewModel.openAddSubstituteDialog(bomItem) }
                            )
                        }
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = { viewModel.setAddDialogOpen(true) },
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
                    contentDescription = stringResource(Res.string.add_bom_item),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "إضافة مكون",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                )
            }
        }
    }

    if (uiState.isSelectParentSheetOpen) {
        SelectParentAssemblyBottomSheet(
            parentParts = uiState.parentParts,
            selectedPartId = uiState.selectedPartId,
            onDismiss = { viewModel.setSelectParentSheetOpen(false) },
            onSelect = { parent ->
                viewModel.selectParentPart(parent?.id)
                viewModel.setSelectParentSheetOpen(false)
            }
        )
    }

    if (uiState.isFilterSheetOpen) {
        BomFilterBottomSheet(
            parentParts = uiState.parentParts,
            selectedPartId = uiState.selectedPartId,
            onSelectParentPart = { viewModel.selectParentPart(it) },
            bomItems = uiState.bomItems,
            availableComponents = uiState.allParts.filter { it.component },
            initialMandatory = uiState.filterMandatoryOnly,
            initialOptional = uiState.filterOptionalOnly,
            initialConsumable = uiState.filterConsumableOnly,
            initialAllowVariants = uiState.filterAllowVariantsOnly,
            onDismiss = { viewModel.setFilterSheetOpen(false) },
            onReset = { viewModel.resetFilters() },
            onApply = { mandatory, optional, consumable, allowVariants ->
                viewModel.applyFilters(mandatory, optional, consumable, allowVariants)
            }
        )
    }

    if (uiState.isAddBomDialogOpen) {
        AddBomItemBottomSheet(
            components = uiState.allParts.filter { it.component },
            parentParts = uiState.parentParts,
            parentPartId = uiState.selectedPartId,
            phases = uiState.phases,
            bomItems = uiState.bomItems,
            editingBomItem = uiState.editingBomItem,
            onDismiss = { viewModel.setAddDialogOpen(false) },
            onConfirm = { pId, subPartId, qty, ref, opt, cons, alwVar, inh, note, substitutePartIds, unit, phaseUuid, closeDialog ->
                viewModel.addBomItem(
                    subPartId = subPartId,
                    quantity = qty,
                    reference = ref,
                    optional = opt,
                    consumable = cons,
                    allowVariants = alwVar,
                    inherited = inh,
                    note = note,
                    substitutePartIds = substitutePartIds,
                    unit = unit,
                    parentPartIdOverride = pId,
                    phaseUuid = phaseUuid,
                    closeDialog = closeDialog
                )
            },
            onAddCustomUnit = { unitCode ->
                viewModel.addCustomUnit(unitCode)
            },
            onDeleteUnit = { unitCode ->
                viewModel.deleteUnit(unitCode)
            },
            onAddPhase = { name, desc ->
                viewModel.addManufacturingPhase(name, desc)
            },
            onDeletePhase = { uuid ->
                viewModel.deleteManufacturingPhase(uuid)
            }
        )
    }

    if (uiState.isAddSubstituteDialogOpen && uiState.selectedBomItemForSubstitute != null) {
        val bomItem = uiState.selectedBomItemForSubstitute!!
        val currentSubPartId = bomItem.subPartId
        val eligibleParts = uiState.allParts.filter { it.component && it.id != currentSubPartId }

        AddSubstituteBottomSheet(
            bomItem = bomItem,
            eligibleParts = eligibleParts,
            onDismiss = { viewModel.closeAddSubstituteDialog() },
            onConfirm = { partId -> viewModel.addSubstitute(partId) }
        )
    }
}

/**
 * الترويسة العلوية لشاشة إدارة بنود BOM
 */
@Composable
private fun BomTopBar(
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
                    text = "جدول بنود قائمة المواد (BOM)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "قائمة المكونات الفرعية والقطع البديلة للمُنتج المجمّع",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

/**
 * بطاقة عرض بند قائمة المواد المجمعة BOM
 */
@Composable
private fun BomItemCard(
    bomItem: BomItem,
    component: Part?,
    substitutes: List<BomItemSubstituteView> = emptyList(),
    phases: List<ManufacturingPhase> = emptyList(),
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onAddSubstituteClick: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val phase = remember(bomItem.phaseUuid, phases) {
        phases.find { it.uuid == bomItem.phaseUuid }
    }

    val totalStock = component?.totalInStock ?: 0.0
    val requiredQty = bomItem.quantity
    val isStockSufficient = totalStock >= requiredQty

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
            // 1. الترويسة العلوية: الأيقونة + اسم القطعة الصريح + رقم الـ IPN + زري التعديل والحذف
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                            text = component?.name ?: "مكون #${bomItem.subPartId}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        if (component != null && component.ipn.isNotBlank()) {
                            Text(
                                text = "IPN: ${component.ipn}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                // الجانب الأيسر: زران صريحان للتحكم بالبطاقة (أيقونة القلم للتعديل + أيقونة السلة للحذف)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل معلومات ارتباط المكون",
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف الارتباط من القائمة",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. السطر الوسطي: شارة مرحلة الإنتاج + شارات الكمية المطلوبة والمخزون المتوفر
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // شارة مرحلة التصنيع
                if (phase != null) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = phase.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFF334155)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // شارات الكميات المطلوبة والموجودة للمقارنة الفورية
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge الكمية المطلوبة
                    Surface(
                        color = Color(0xFFEEF2FF),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                    ) {
                        Text(
                            text = "المطلوب: ${bomItem.quantity} ${component?.units ?: "pcs"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF4F46E5),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Badge الرصيد المتوفر بمستودعات التصنيع
                    Surface(
                        color = if (isStockSufficient) Color(0xFFECFDF5) else Color(0xFFFFE4E6),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, if (isStockSufficient) Color(0xFFA7F3D0) else Color(0xFFFECDD3))
                    ) {
                        Text(
                            text = "المتوفر: ${component?.totalInStock ?: 0.0} ${component?.units ?: "pcs"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (isStockSufficient) Color(0xFF047857) else Color(0xFFBE123C),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 3. سطر المرجع الهندسي والملاحظات مع حماية اتجاه الأقواس (BiDi / RTL Fixed)
            if (bomItem.reference.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📍 المرجع الهندسي: ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "\u200E${bomItem.reference}\u200E",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = Color(0xFF0F172A)
                    )
                }
            }

            if (bomItem.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📝 ملاحظة التركيب: ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = bomItem.note,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = Color(0xFF334155)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. الشروط المنطقية المعتمدة
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (bomItem.optional) BadgeTag("اختياري (Optional)", Color(0xFFF0F9FF), Color(0xFF0284C7))
                if (bomItem.consumable) BadgeTag("مستهلك (Consumable)", Color(0xFFFFE4E6), Color(0xFFE11D48))
                if (bomItem.allowVariants) BadgeTag("يسمح ببدائل المتغيرات", Color(0xFFECFDF5), Color(0xFF059669))
            }

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Spacer(modifier = Modifier.height(8.dp))

            // 5. شريط البدائل المعتمدة BomItemSubstitute
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "البدائل المعتمدة (${substitutes.size}):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = Color(0xFF0F172A)
                )
            }

            if (substitutes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (sub in substitutes) {
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${sub.substitutePart.name}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "IPN: ${sub.substitutePart.ipn.ifBlank { "-" }}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // النافذة المنبثقة لـ تأكيد حذف المكون
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "تأكيد حذف المكون من القائمة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف المكون '${component?.name ?: "مكون #${bomItem.subPartId}"}' من قائمة المواد لهذه القطعة المجمعة؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("حذف", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBomItemBottomSheet(
    components: List<Part>,
    parentParts: List<Part> = emptyList(),
    parentPartId: Long? = null,
    phases: List<ManufacturingPhase> = emptyList(),
    bomItems: List<BomItem> = emptyList(),
    editingBomItem: BomItem? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        parentPartId: Long,
        subPartId: Long,
        quantity: Double,
        reference: String,
        optional: Boolean,
        consumable: Boolean,
        allowVariants: Boolean,
        inherited: Boolean,
        note: String,
        substitutePartIds: List<Long>,
        unit: String,
        phaseUuid: String?,
        closeDialog: Boolean
    ) -> Unit,
    onAddCustomUnit: (String) -> Unit = {},
    onDeleteUnit: (String) -> Unit = {},
    onAddPhase: (String, String) -> Unit = { _, _ -> },
    onDeletePhase: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var isSaving by remember { mutableStateOf(false) }

    var selectedParentPartId by remember(editingBomItem) {
        mutableStateOf<Long?>(editingBomItem?.partId ?: parentPartId ?: parentParts.firstOrNull()?.id)
    }
    var isSelectParentSheetOpen by remember { mutableStateOf(false) }
    val selectedParentComponent = parentParts.find { it.id == selectedParentPartId } ?: parentParts.find { it.id == parentPartId }

    val currentParentItemsCount = remember(selectedParentPartId, parentPartId, bomItems) {
        val targetId = selectedParentPartId ?: parentPartId
        if (targetId != null) {
            bomItems.count { it.partId == targetId }
        } else 0
    }

    var selectedPhaseUuid by remember(editingBomItem) { mutableStateOf<String?>(editingBomItem?.phaseUuid ?: phases.firstOrNull()?.uuid) }
    var isSelectPhaseSheetOpen by remember { mutableStateOf(false) }
    val selectedPhase = phases.find { it.uuid == selectedPhaseUuid }

    var selectedSubPartId by remember(editingBomItem) { mutableStateOf<Long?>(editingBomItem?.subPartId) }
    var isSelectSubPartSheetOpen by remember { mutableStateOf(false) }
    var quantityText by remember(editingBomItem) { mutableStateOf(editingBomItem?.quantity?.toString() ?: "1.0") }

    val selectedComponent = components.find { it.id == selectedSubPartId }
    var selectedUnit by remember(editingBomItem, selectedComponent) { mutableStateOf(selectedComponent?.units?.ifBlank { "pcs" } ?: "pcs") }
    var isSelectUnitSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSubPartId) {
        selectedComponent?.units?.takeIf { it.isNotBlank() }?.let {
            selectedUnit = it
        }
    }

    var reference by remember(editingBomItem) { mutableStateOf(editingBomItem?.reference ?: "") }
    var note by remember(editingBomItem) { mutableStateOf(editingBomItem?.note ?: "") }
    var optional by remember(editingBomItem) { mutableStateOf(editingBomItem?.optional ?: false) }
    var consumable by remember(editingBomItem) { mutableStateOf(editingBomItem?.consumable ?: false) }
    var allowVariants by remember(editingBomItem) { mutableStateOf(editingBomItem?.allowVariants ?: false) }
    var inherited by remember(editingBomItem) { mutableStateOf(editingBomItem?.inherited ?: false) }
    var isSelectOptionsSheetOpen by remember { mutableStateOf(false) }

    val selectedSubstituteIds = remember(editingBomItem) { mutableStateListOf<Long>() }
    var isSelectSubstituteSheetOpen by remember { mutableStateOf(false) }

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
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (editingBomItem != null) "تحديث معلومات ارتباط المكون" else stringResource(Res.string.add_bom_item),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        BadgeTag("المكونات المضافة: $currentParentItemsCount", Color(0xFFE0E7FF), Color(0xFF3730A3))
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
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. قسم المنتج المجمّع / الأساسي (Parent Part Selector Card) [إلزامي]
                Text(
                    text = "المنتج المجمّع / الأساسي (Parent Part) *:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )

                Surface(
                    onClick = { isSelectParentSheetOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedParentComponent != null) Color(0xFFF8FAFC) else Color.White,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (selectedParentComponent != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedParentComponent == null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5)
                                )
                                Text(
                                    text = "اختر المنتج المجمّع (assembly = 1) *",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )
                                )
                            }
                        } else {
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
                                        imageVector = Icons.Default.Build,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = selectedParentComponent.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "IPN: ${selectedParentComponent.ipn.ifBlank { "-" }} | المخزون المتوفر: ${selectedParentComponent.totalInStock} ${selectedParentComponent.units}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
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
                                text = "تغيير",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // 2. قسم القطعة المكونة الفرعية (SubPart Selector Card) [إلزامي]
                Text(
                    text = "المادة الأولية / القطعة المكونة (SubPart) *:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )

                Surface(
                    onClick = { isSelectSubPartSheetOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (selectedComponent != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
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
                                    text = selectedComponent?.name ?: "اختر القطعة المكونة الفرعية (SubPart) *",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (selectedComponent != null) "IPN: ${selectedComponent.ipn.ifBlank { "-" }} | المخزون المتوفر: ${selectedComponent.totalInStock} ${selectedComponent.units}" else "اضغط للبحث واختيار المكون الفرعي المتاح",
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
                                text = if (selectedComponent != null) "تغيير" else "اختر ▾",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // 3. مرحلة الإنتاج والتصنيع (Manufacturing Phase Selector Card)
                Text(
                    text = "مرحلة الإنتاج / التصنيع (Manufacturing Phase) :",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )

                Surface(
                    onClick = { isSelectPhaseSheetOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedPhase != null) Color(0xFFF8FAFC) else Color.White,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (selectedPhase != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedPhase == null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5)
                                )
                                Text(
                                    text = "اختر مرحلة الإنتاج (تجهيز، تجميع، تغليف...)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )
                                )
                            }
                        } else {
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
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = selectedPhase.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    if (selectedPhase.description.isNotBlank()) {
                                        Text(
                                            text = selectedPhase.description,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            color = Color(0xFFEEF2FF),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                        ) {
                            Text(
                                text = "تغيير",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // 2. الكمية المطلوبة ووحدة القياس (حقل واحد مع شارة خيار منبثقة للنوع)
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("الكمية المطلوبة (quantity > 0)") },
                    placeholder = { Text("1.0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = {
                        Surface(
                            onClick = { isSelectUnitSheetOpen = true },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEEF2FF),
                            border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = selectedUnit.ifBlank { "pcs" },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4F46E5)
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "اختيار نوع الوحدة",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // 3. المرجع الهندسي والملاحظات الفنية
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("المرجع الهندسي على البوردة (Reference/Designator)") },
                    placeholder = { Text("مثال: R1, R2 أو U1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظات تركيبية (Note)") },
                    placeholder = { Text("تعليمات التلحيم أو التجميع") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // 4. خيارات وخصائص المكون (حقل واحد مع صفحة منبثقة)
                Text(
                    text = "خيارات وخصائص المكون :",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )

                Surface(
                    onClick = { isSelectOptionsSheetOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFEEF2FF))
                                    .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(10.dp)),
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
                                    text = "تحديد خيارات المكون",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                val activeOptionsList = mutableListOf<String>()
                                if (optional) activeOptionsList.add("اختياري")
                                if (consumable) activeOptionsList.add("مستهلك")
                                if (allowVariants) activeOptionsList.add("بدائل")
                                if (inherited) activeOptionsList.add("وراثة")

                                val summaryText = if (activeOptionsList.isEmpty()) {
                                    "افتراضي (بدون خصائص خاصة)"
                                } else {
                                    activeOptionsList.joinToString(" • ")
                                }

                                Text(
                                    text = summaryText,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF4F46E5)
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFEEF2FF),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                        ) {
                            Text(
                                text = "تخصيص",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // 5. قسم القطعة البديلة المعتمدة (حقل بطاقة ثابت داخل نموذج إدراج المكون)
                Text(
                    text = "القطع البديلة المعتمدة (Approved Substitutes) :",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEEF2FF))
                                        .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "البدائل المعتمدة المضافة (${selectedSubstituteIds.size})",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (selectedSubstituteIds.isEmpty()) "اضغط للبدء بإضافة قطع بديلة معتمدة" else "تم ربط ${selectedSubstituteIds.size} قطعة بديلة معتمدة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Surface(
                                onClick = { isSelectSubstituteSheetOpen = true },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEEF2FF),
                                border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "إضافة بديل",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF4F46E5)
                                    )
                                }
                            }
                        }

                        if (selectedSubstituteIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                selectedSubstituteIds.forEach { subId ->
                                    val subPart = components.find { it.id == subId }
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Extension,
                                                    contentDescription = null,
                                                    tint = Color(0xFF4F46E5),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = subPart?.name ?: "قطعة #${subId}",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.5.sp
                                                    ),
                                                    color = Color(0xFF0F172A)
                                                )
                                                if (subPart != null && subPart.ipn.isNotBlank()) {
                                                    Text(
                                                        text = "(${subPart.ipn})",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = { selectedSubstituteIds.remove(subId) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "حذف البديل",
                                                    tint = Color(0xFFEF4444),
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

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. زر إلغاء
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Text(stringResource(Res.string.cancel), fontWeight = FontWeight.Bold)
                    }

                    // 2. زر فرعي: حفظ وإنهاء
                    FilledTonalButton(
                        onClick = {
                            if (!isSaving) {
                                val pId = selectedParentPartId ?: parentPartId
                                val subId = selectedSubPartId
                                val qty = quantityText.toDoubleOrNull() ?: 1.0
                                if (pId != null && subId != null && qty > 0.0) {
                                    isSaving = true
                                    onConfirm(
                                        pId,
                                        subId,
                                        qty,
                                        reference,
                                        optional,
                                        consumable,
                                        allowVariants,
                                        inherited,
                                        note,
                                        if (allowVariants) selectedSubstituteIds.toList() else emptyList(),
                                        selectedUnit,
                                        selectedPhaseUuid,
                                        true
                                    )
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving && (selectedParentPartId ?: parentPartId) != null && selectedSubPartId != null && (quantityText.toDoubleOrNull() ?: 0.0) > 0.0,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Text("حفظ وإنهاء", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    // 3. زر رئيسي: حفظ ومتابعة
                    Button(
                        onClick = {
                            if (!isSaving) {
                                val pId = selectedParentPartId ?: parentPartId
                                val subId = selectedSubPartId
                                val qty = quantityText.toDoubleOrNull() ?: 1.0
                                if (pId != null && subId != null && qty > 0.0) {
                                    isSaving = true
                                    onConfirm(
                                        pId,
                                        subId,
                                        qty,
                                        reference,
                                        optional,
                                        consumable,
                                        allowVariants,
                                        inherited,
                                        note,
                                        if (allowVariants) selectedSubstituteIds.toList() else emptyList(),
                                        selectedUnit,
                                        selectedPhaseUuid,
                                        false
                                    )
                                    // تصفير حقول المكون بعد الحفظ لتكون جاهزة للمكون التالي
                                    selectedSubPartId = null
                                    quantityText = "1.0"
                                    reference = ""
                                    note = ""
                                    optional = false
                                    consumable = false
                                    allowVariants = false
                                    inherited = false
                                    selectedSubstituteIds.clear()

                                    // التمرير التلقائي لأعلى النموذج
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(0)
                                    }
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving && (selectedParentPartId ?: parentPartId) != null && selectedSubPartId != null && (quantityText.toDoubleOrNull() ?: 0.0) > 0.0,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (editingBomItem != null) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(if (editingBomItem != null) "تحديث المكون" else "حفظ ومتابعة", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }
                }
            }
        }
    }

    if (isSelectParentSheetOpen) {
        SelectParentAssemblyBottomSheet(
            parentParts = parentParts,
            selectedPartId = selectedParentPartId,
            onDismiss = { isSelectParentSheetOpen = false },
            onSelect = { parent ->
                if (parent != null) {
                    selectedParentPartId = parent.id
                }
                isSelectParentSheetOpen = false
            }
        )
    }

    if (isSelectPhaseSheetOpen) {
        SelectPhaseBottomSheet(
            phases = phases,
            selectedPhaseUuid = selectedPhaseUuid,
            onDismiss = { isSelectPhaseSheetOpen = false },
            onSelect = { phase ->
                selectedPhaseUuid = phase.uuid
                isSelectPhaseSheetOpen = false
            },
            onAddPhase = onAddPhase,
            onDeletePhase = onDeletePhase
        )
    }

    if (isSelectSubPartSheetOpen) {
        SelectSubPartBottomSheet(
            components = components,
            selectedSubPartId = selectedSubPartId,
            currentParentPartId = selectedParentPartId ?: parentPartId,
            onDismiss = { isSelectSubPartSheetOpen = false },
            onSelect = { comp ->
                selectedSubPartId = comp.id
                selectedSubstituteIds.remove(comp.id)
                isSelectSubPartSheetOpen = false
            }
        )
    }

    if (isSelectUnitSheetOpen) {
        SelectUnitBottomSheet(
            selectedUnit = selectedUnit,
            onDismiss = { isSelectUnitSheetOpen = false },
            onSelect = { unitCode ->
                selectedUnit = unitCode
                isSelectUnitSheetOpen = false
            },
            onAddUnit = onAddCustomUnit,
            onDeleteUnit = onDeleteUnit
        )
    }

    if (isSelectOptionsSheetOpen) {
        SelectComponentOptionsBottomSheet(
            optional = optional,
            consumable = consumable,
            allowVariants = allowVariants,
            inherited = inherited,
            onDismiss = { isSelectOptionsSheetOpen = false },
            onConfirm = { opt, cons, alwVar, inh ->
                optional = opt
                consumable = cons
                allowVariants = alwVar
                inherited = inh
                isSelectOptionsSheetOpen = false
            }
        )
    }

    if (isSelectSubstituteSheetOpen) {
        val eligibleParts = components.filter {
            it.id != parentPartId &&
            it.id != selectedSubPartId &&
            !selectedSubstituteIds.contains(it.id)
        }

        SelectSubstituteForAddBottomSheet(
            eligibleParts = eligibleParts,
            onDismiss = { isSelectSubstituteSheetOpen = false },
            onConfirm = { substitutePartId ->
                if (!selectedSubstituteIds.contains(substitutePartId)) {
                    selectedSubstituteIds.add(substitutePartId)
                    allowVariants = true
                }
                isSelectSubstituteSheetOpen = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectSubPartBottomSheet(
    components: List<Part>,
    selectedSubPartId: Long?,
    currentParentPartId: Long? = null,
    onDismiss: () -> Unit,
    onSelect: (Part) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    val rawMaterialsCount = remember(components) {
        components.count { it.component && !it.assembly }
    }
    val subAssembliesCount = remember(components, currentParentPartId) {
        components.count { it.component && it.assembly && (currentParentPartId == null || it.id != currentParentPartId) }
    }

    val filteredComponents = remember(components, currentParentPartId, selectedTab, searchQuery) {
        val baseList = if (selectedTab == 0) {
            components.filter { it.component && !it.assembly }
        } else {
            components.filter { it.component && it.assembly && (currentParentPartId == null || it.id != currentParentPartId) }
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.ipn.contains(searchQuery, ignoreCase = true)
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
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اختر القطعة المكونة الفرعية (SubPart)",
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

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث بالاسم أو كود IPN...") },
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

                Spacer(modifier = Modifier.height(10.dp))

                // Segmented Tab Row (2 Tabs: مواد أولية خام | تجميعات ثانوية)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = { selectedTab = 0 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 0) Color(0xFF4F46E5) else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "مواد أولية خام ($rawMaterialsCount)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = if (selectedTab == 0) Color.White else Color(0xFF475569)
                            )
                        }
                    }

                    Surface(
                        onClick = { selectedTab = 1 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 1) Color(0xFF4F46E5) else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "تجميعات ثانوية ($subAssembliesCount)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = if (selectedTab == 1) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredComponents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedTab == 0) "لا توجد مواد أولية خام مطابقة للبحث" else "لا توجد تجميعات ثانوية مطابقة للبحث",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(filteredComponents, key = { index, comp -> "select-subpart-${comp.id}-$index" }) { _, comp ->
                            val isSelected = selectedSubPartId == comp.id
                            Surface(
                                onClick = { onSelect(comp) },
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
                                            text = comp.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "IPN: ${comp.ipn.ifBlank { "-" }} | المخزون المتوفر: ${comp.totalInStock} ${comp.units}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(22.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectSubstituteForAddBottomSheet(
    eligibleParts: List<Part>,
    onDismiss: () -> Unit,
    onConfirm: (partId: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedPartId by remember { mutableStateOf<Long?>(eligibleParts.firstOrNull()?.id) }

    val filteredParts = remember(eligibleParts, searchQuery) {
        if (searchQuery.isBlank()) {
            eligibleParts
        } else {
            eligibleParts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.ipn.contains(searchQuery, ignoreCase = true)
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
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "اختيار قطعة بديلة معتمدة",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث بالاسم أو كود IPN...") },
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

            if (filteredParts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد قطع بديلة مؤهلة متاحة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(filteredParts, key = { index, part -> "sub-picker-${part.id}-$index" }) { _, part ->
                        val isSelected = selectedPartId == part.id
                        Surface(
                            onClick = { selectedPartId = part.id },
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
                                        text = part.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "IPN: ${part.ipn.ifBlank { "-" }} | المخزون المتوفر: ${part.totalInStock} ${part.units}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                        val pId = selectedPartId
                        if (pId != null) {
                            onConfirm(pId)
                        }
                    },
                    enabled = selectedPartId != null,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text("إضافة البديل", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSubstituteBottomSheet(
    bomItem: BomItem,
    eligibleParts: List<Part>,
    onDismiss: () -> Unit,
    onConfirm: (partId: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedPartId by remember { mutableStateOf<Long?>(eligibleParts.firstOrNull()?.id) }

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
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "إضافة قطعة بديلة معتمدة",
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("اختر القطعة البديلة المناسبة من المستودع:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0F172A))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(eligibleParts, key = { index, part -> "sub-part-${part.id}-$index" }) { _, part ->
                        val isSelected = selectedPartId == part.id
                        Surface(
                            onClick = { selectedPartId = part.id },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(part.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Text("IPN: ${part.ipn.ifBlank { "-" }}", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

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
                            val pId = selectedPartId
                            if (pId != null) {
                                onConfirm(pId)
                            }
                        },
                        enabled = selectedPartId != null,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BomFilterBottomSheet(
    parentParts: List<Part> = emptyList(),
    selectedPartId: Long? = null,
    onSelectParentPart: (Long?) -> Unit = {},
    bomItems: List<BomItem>,
    availableComponents: List<Part>,
    initialMandatory: Boolean,
    initialOptional: Boolean,
    initialConsumable: Boolean,
    initialAllowVariants: Boolean,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: (mandatory: Boolean, optional: Boolean, consumable: Boolean, allowVariants: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedParentIdState by remember { mutableStateOf(selectedPartId) }
    var mandatoryOnly by remember { mutableStateOf(initialMandatory) }
    var optionalOnly by remember { mutableStateOf(initialOptional) }
    var consumableOnly by remember { mutableStateOf(initialConsumable) }
    var allowVariantsOnly by remember { mutableStateOf(initialAllowVariants) }

    val calculatedCount = remember(
        bomItems, availableComponents, searchQuery,
        mandatoryOnly, optionalOnly, consumableOnly, allowVariantsOnly
    ) {
        val query = searchQuery.trim()
        bomItems.count { bomItem ->
            val comp = availableComponents.find { it.id == bomItem.subPartId }
            val matchesQuery = query.isBlank() ||
                    comp?.name?.contains(query, ignoreCase = true) == true ||
                    comp?.ipn?.contains(query, ignoreCase = true) == true ||
                    comp?.description?.contains(query, ignoreCase = true) == true ||
                    comp?.keywords?.contains(query, ignoreCase = true) == true ||
                    bomItem.reference.contains(query, ignoreCase = true) ||
                    bomItem.note.contains(query, ignoreCase = true)

            val matchesMandatory = !mandatoryOnly || !bomItem.optional
            val matchesOptional = !optionalOnly || bomItem.optional
            val matchesConsumable = !consumableOnly || bomItem.consumable
            val matchesVariants = !allowVariantsOnly || bomItem.allowVariants

            matchesQuery && matchesMandatory && matchesOptional && matchesConsumable && matchesVariants
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
                            text = "تصفية بنود قائمة المواد (BOM)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "تحديد المكونات المعروضة حسب الخصائص والمشتقات",
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
                        mandatoryOnly = false
                        optionalOnly = false
                        consumableOnly = false
                        allowVariantsOnly = false
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
                // 1. حقل البحث الرئيسي بقمة النافذة
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن اسم المنتج الأب، المكون، أو الـ IPN...") },
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

                // 2. العنوانان بجانب بعضهما كـ Tabs تفاعلية سريعة
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
                                    text = "🛠️ المنتجات الأب (${parentParts.size})",
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
                                    text = "⚙️ معايير تصفية المكونات",
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
                val filteredParentParts = remember(parentParts, searchQuery) {
                    if (searchQuery.isBlank()) {
                        parentParts
                    } else {
                        val query = searchQuery.trim()
                        parentParts.filter {
                            it.name.contains(query, ignoreCase = true) ||
                            it.ipn.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true) ||
                            it.keywords.contains(query, ignoreCase = true)
                        }
                    }
                }

                if (selectedTab == 0) {
                    // تبويب قائمة المنتجات الأب المجمعة العمودية
                    if (filteredParentParts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد منتجات أجهزة مطابقة للبحث",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B)
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filteredParentParts.forEach { parent ->
                                val isSelected = selectedParentIdState == parent.id
                                Surface(
                                    onClick = {
                                        if (selectedParentIdState == parent.id) {
                                            selectedParentIdState = null
                                            onSelectParentPart(null)
                                        } else {
                                            selectedParentIdState = parent.id
                                            onSelectParentPart(parent.id)
                                        }
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
                                                    .background(if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Build,
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color.White else Color(0xFF475569),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Column {
                                                Text(
                                                    text = parent.name,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.5.sp
                                                    ),
                                                    color = Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = "IPN: ${parent.ipn.ifBlank { "-" }} | المخزون: ${parent.totalInStock} ${parent.units}",
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
                    }
                } else {
                    // تبويب معايير تصفية المكونات المتاحة
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
                                    .clickable { mandatoryOnly = !mandatoryOnly }
                            ) {
                                Checkbox(
                                    checked = mandatoryOnly,
                                    onCheckedChange = { mandatoryOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Text(
                                    "المكونات الإلزامية فقط (Mandatory Only)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { optionalOnly = !optionalOnly }
                            ) {
                                Checkbox(
                                    checked = optionalOnly,
                                    onCheckedChange = { optionalOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Text(
                                    "المكونات الاختيارية (Optional Items)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { consumableOnly = !consumableOnly }
                            ) {
                                Checkbox(
                                    checked = consumableOnly,
                                    onCheckedChange = { consumableOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Text(
                                    "المكونات الاستهلاكية (Consumable)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { allowVariantsOnly = !allowVariantsOnly }
                            ) {
                                Checkbox(
                                    checked = allowVariantsOnly,
                                    onCheckedChange = { allowVariantsOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Text(
                                    "المكونات التي تسمح ببدائل (Allow Variants)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                )
                            }
                        }
                    }
                }
            }

            // Footer Action Bar
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
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
                            onApply(mandatoryOnly, optionalOnly, consumableOnly, allowVariantsOnly)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1.8f)
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
                                text = "تطبيق التصفية (عرض $calculatedCount بند)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectParentAssemblyBottomSheet(
    parentParts: List<Part>,
    selectedPartId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Part?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    val finalProductsCount = remember(parentParts) {
        parentParts.count { it.assembly && it.salable }
    }
    val subAssembliesCount = remember(parentParts) {
        parentParts.count { it.assembly && !it.salable }
    }

    val filteredAssemblies = remember(parentParts, selectedTab, searchQuery) {
        val baseList = if (selectedTab == 0) {
            parentParts.filter { it.assembly && it.salable }
        } else {
            parentParts.filter { it.assembly && !it.salable }
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.ipn.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "اختر المنتج الأب المجمع (Assembly)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "عرض الأصناف المسجلة بـ (assembly = 1) لإنشاء قائمة موادها",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
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

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن اسم المنتج الأب، الـ IPN...") },
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

            // Segmented Tab Row (2 Tabs: منتجات نهائية | تجميعات ثانوية)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = { selectedTab = 0 },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTab == 0) Color(0xFF4F46E5) else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "منتجات نهائية ($finalProductsCount)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            ),
                            color = if (selectedTab == 0) Color.White else Color(0xFF475569)
                        )
                    }
                }

                Surface(
                    onClick = { selectedTab = 1 },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTab == 1) Color(0xFF4F46E5) else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "تجميعات ثانوية ($subAssembliesCount)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            ),
                            color = if (selectedTab == 1) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredAssemblies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handyman,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (selectedTab == 0) "لا توجد منتجات نهائية مطابقة للبحث" else "لا توجد تجميعات ثانوية مطابقة للبحث",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                        )
                        Text(
                            text = "تأكد من اختيار التبويب الصحيح أو تفعيل خيار التجميع الهندسي (assembly) في قسم إدارة القطع.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredAssemblies, key = { index, parent -> "parent-picker-${parent.id}-$index" }) { _, parent ->
                        val isSelected = selectedPartId == parent.id
                        Surface(
                            onClick = {
                                if (selectedPartId == parent.id) {
                                    onSelect(null)
                                } else {
                                    onSelect(parent)
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
                                            .background(if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else Color(0xFF475569),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = parent.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp
                                            ),
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "IPN: ${parent.ipn.ifBlank { "-" }} | المخزون المتوفر: ${parent.totalInStock} ${parent.units}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(22.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectUnitBottomSheet(
    selectedUnit: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onAddUnit: (String) -> Unit = {},
    onDeleteUnit: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var customUnitInput by remember { mutableStateOf("") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var unitToDelete by remember { mutableStateOf<String?>(null) }

    val initialUnits = remember {
        mutableStateListOf(
            "pcs" to "قطعة (pcs) - قياس بالعدد والقطع المفردة",
            "unit" to "وحدة (unit) - وحدة متكاملة أو جهازا",
            "kg" to "كيلوجرام (kg) - قياس الوزن الثقيل",
            "g" to "جرام (g) - قياس الوزن الدقيق",
            "m" to "متر (m) - أطوال وأسلاك",
            "cm" to "سنتيمتر (cm) - أطوال متوسطة",
            "mm" to "مليمتر (mm) - أبعاد قياسية دقيقة",
            "L" to "لتر (L) - مواد سائلة وشحمية",
            "mL" to "مليلتر (mL) - أحجام دقيقة",
            "set" to "طقم (set) - حزمة طقم متكامل",
            "pack" to "عبوة (pack) - طرد أو حزمة",
            "roll" to "لفة (roll) - لفة سلك أو بكرة",
            "box" to "صندوق (box) - صندوق أو كرتونة",
            "pair" to "زوج (pair) - زوج متطابق"
        )
    }

    LaunchedEffect(selectedUnit) {
        if (selectedUnit.isNotBlank() && initialUnits.none { it.first.equals(selectedUnit, ignoreCase = true) }) {
            initialUnits.add(0, selectedUnit to "وحدة قياس مخصصة ($selectedUnit)")
        }
    }

    val filteredUnits = remember(searchQuery, initialUnits.size) {
        if (searchQuery.isBlank()) {
            initialUnits.toList()
        } else {
            initialUnits.filter { (code, label) ->
                code.contains(searchQuery, ignoreCase = true) ||
                label.contains(searchQuery, ignoreCase = true)
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
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "اختر نوع الوحدة / وحدة القياس",
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

            // حقل البحث
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن نوع الوحدة (مثل: pcs, جرام, متر)...") },
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

            Spacer(modifier = Modifier.height(12.dp))

            // إدخال وحدة مخصصة مع ارتفاع ثابت كلياً عند التفعيل والتركيز
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customUnitInput,
                    onValueChange = { customUnitInput = it },
                    placeholder = { Text("أو ادخل وحدة مخصصة (مثال: Ω, V)...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                Button(
                    onClick = {
                        val newUnit = customUnitInput.trim()
                        if (newUnit.isNotBlank()) {
                            if (initialUnits.none { it.first.equals(newUnit, ignoreCase = true) }) {
                                initialUnits.add(0, newUnit to "وحدة قياس مخصصة ($newUnit)")
                            }
                            onAddUnit(newUnit)
                            onSelect(newUnit)
                            customUnitInput = ""
                        }
                    },
                    enabled = customUnitInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        disabledContainerColor = Color(0xFFE2E8F0),
                        contentColor = Color.White,
                        disabledContentColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("إضافة", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filteredUnits, key = { index, unit -> "unit-${unit.first}-$index" }) { _, (code, label) ->
                    val isSelected = selectedUnit.equals(code, ignoreCase = true)
                    Surface(
                        onClick = { onSelect(code) },
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
                            // الجانب الأيمن: شارة التحديد وتفاصيل الوحدة
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // الجانب الأيسر: زر الحذف لكل نوع وحدة
                            IconButton(
                                onClick = {
                                    unitToDelete = code
                                    showDeleteConfirmDialog = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "حذف وحدة القياس",
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

    // النافذة المنبثقة لتأكيد الحذف أو إلغائه
    if (showDeleteConfirmDialog && unitToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                unitToDelete = null
            },
            title = {
                Text(
                    text = "تأكيد حذف وحدة القياس",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف وحدة القياس '${unitToDelete}'؟ سيتم إزالتها نهائياً من قائمة الخيارات المتاحة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = unitToDelete!!
                        onDeleteUnit(target)
                        initialUnits.removeIf { it.first.equals(target, ignoreCase = true) }
                        if (selectedUnit.equals(target, ignoreCase = true)) {
                            onSelect("pcs")
                        }
                        showDeleteConfirmDialog = false
                        unitToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("حذف", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        unitToDelete = null
                    }
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectComponentOptionsBottomSheet(
    optional: Boolean,
    consumable: Boolean,
    allowVariants: Boolean,
    inherited: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (optional: Boolean, consumable: Boolean, allowVariants: Boolean, inherited: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var opt by remember { mutableStateOf(optional) }
    var cons by remember { mutableStateOf(consumable) }
    var alwVar by remember { mutableStateOf(allowVariants) }
    var inh by remember { mutableStateOf(inherited) }

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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "خيارات وخصائص بند المكون",
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

            Spacer(modifier = Modifier.height(14.dp))

            val optionsList = listOf(
                OptionItemSpec(
                    title = "مكون اختياري في التجميع (Optional)",
                    subtitle = "يمكن استثناء المكون أثناء عملية التجميع والإنتاج عند الحاجة",
                    isSelected = opt,
                    onToggle = { opt = !opt }
                ),
                OptionItemSpec(
                    title = "مكون مستهلك / لا يُرد (Consumable)",
                    subtitle = "المكون يستهلك بالكامل أثناء التجميع ولا يمكن استرداده للمخزون",
                    isSelected = cons,
                    onToggle = { cons = !cons }
                ),
                OptionItemSpec(
                    title = "السماح باستخدام بدائل المكون (Allow Variants)",
                    subtitle = "تفعيل خيارات القطع البديلة المعتمدة لهذا البند عند عدم توفر الأصلي",
                    isSelected = alwVar,
                    onToggle = { alwVar = !alwVar }
                ),
                OptionItemSpec(
                    title = "وراثة المكون للقطع المشتقة تلقائياً (Inherited)",
                    subtitle = "تطبييق المكون تلقائياً على كافة التجميعات والمنتجات الفرعية المشتقة",
                    isSelected = inh,
                    onToggle = { inh = !inh }
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                optionsList.forEach { spec ->
                    Surface(
                        onClick = spec.onToggle,
                        shape = RoundedCornerShape(14.dp),
                        color = if (spec.isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (spec.isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = spec.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (spec.isSelected) Color(0xFF312E81) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = spec.subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF64748B)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Checkbox(
                                checked = spec.isSelected,
                                onCheckedChange = { spec.onToggle() },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4F46E5)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    onConfirm(opt, cons, alwVar, inh)
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
                    text = "تأكيد الخيارات",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

private data class OptionItemSpec(
    val title: String,
    val subtitle: String,
    val isSelected: Boolean,
    val onToggle: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectPhaseBottomSheet(
    phases: List<ManufacturingPhase>,
    selectedPhaseUuid: String?,
    onDismiss: () -> Unit,
    onSelect: (ManufacturingPhase) -> Unit,
    onAddPhase: (name: String, desc: String) -> Unit = { _, _ -> },
    onDeletePhase: (uuid: String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var customPhaseNameInput by remember { mutableStateOf("") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var phaseToDelete by remember { mutableStateOf<ManufacturingPhase?>(null) }

    val filteredPhases = remember(phases, searchQuery) {
        if (searchQuery.isBlank()) {
            phases
        } else {
            phases.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
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
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "اختر مرحلة الإنتاج والتصنيع",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن مرحلة (تجهيز، خلط، تجميع، فحص)...") },
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

            Spacer(modifier = Modifier.height(12.dp))

            // إدخال مرحلة جديدة بارتفاع ثابت كلياً عند التفعيل والتركيز
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customPhaseNameInput,
                    onValueChange = { customPhaseNameInput = it },
                    placeholder = { Text("أو ادخل مرحلة مخصصة جديدة...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                Button(
                    onClick = {
                        val newName = customPhaseNameInput.trim()
                        if (newName.isNotBlank()) {
                            onAddPhase(newName, "مرحلة تصنيعية مخصصة")
                            customPhaseNameInput = ""
                        }
                    },
                    enabled = customPhaseNameInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        disabledContainerColor = Color(0xFFE2E8F0),
                        contentColor = Color.White,
                        disabledContentColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("إضافة", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filteredPhases, key = { index, phase -> "phase-${phase.uuid}-$index" }) { _, phase ->
                    val isSelected = selectedPhaseUuid == phase.uuid
                    Surface(
                        onClick = { onSelect(phase) },
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
                            // الجانب الأيمن: شارة التحديد وتفاصيل المرحلة
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = phase.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp
                                            ),
                                            color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF0F172A)
                                        )
                                        if (phase.isSystemDefault) {
                                            BadgeTag("قياسي", Color(0xFFE0E7FF), Color(0xFF3730A3))
                                        }
                                    }
                                    if (phase.description.isNotBlank()) {
                                        Text(
                                            text = phase.description,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            // الجانب الأيسر: زر الحذف لجميع عناصر المراحل
                            IconButton(
                                onClick = {
                                    phaseToDelete = phase
                                    showDeleteConfirmDialog = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "حذف المرحلة",
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

    // النافذة المنبثقة لـ تأكيد حذف المرحلة
    if (showDeleteConfirmDialog && phaseToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                phaseToDelete = null
            },
            title = {
                Text(
                    text = "تأكيد حذف مرحلة الإنتاج",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف مرحلة الإنتاج '${phaseToDelete?.name}'؟ سيتم إزالتها وتفريغ ارتباطاتها بباقي المكونات.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = phaseToDelete!!
                        onDeletePhase(target.uuid)
                        showDeleteConfirmDialog = false
                        phaseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("حذف", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        phaseToDelete = null
                    }
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

