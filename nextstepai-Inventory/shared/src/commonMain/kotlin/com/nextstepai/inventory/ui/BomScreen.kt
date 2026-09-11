package com.nextstepai.inventory.ui

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
import com.nextstepai.inventory.data.BomItemSubstituteView
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.ui.theme.AppIcons
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_bom_item
import nextstepai_inventory.shared.generated.resources.back
import nextstepai_inventory.shared.generated.resources.bom_items_count
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.card_bom_title
import nextstepai_inventory.shared.generated.resources.save

/**
 * شاشة إدارة بنود قائمة مواد التصنيع (BOM Items Screen).
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
            TopAppBar(
                title = { Text(stringResource(Res.string.card_bom_title)) },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(AppIcons.Back),
                            contentDescription = stringResource(Res.string.back),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.back))
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.setAddDialogOpen(true) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(AppIcons.Add),
                            contentDescription = stringResource(Res.string.add_bom_item),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.add_bom_item))
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
            val isWide = this@BoxWithConstraints.maxWidth > 600.dp
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "اختر المنتج الأب (Assembly) لعرض قائمة مواده:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // اختيار المنتج الأب عبر LazyRow مع المفاتيح الثابتة المستقرة key()
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(uiState.parentParts, key = { "parent-assembly-${it.id}" }) { parent ->
                        FilterChip(
                            selected = uiState.selectedPartId == parent.id,
                            onClick = { viewModel.selectParentPart(parent.id) },
                            label = { Text(parent.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // عرض رسائل الخطأ إن وجدت
                if (uiState.errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
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

                // عرض قائمة بنود BOM للقطعة المختارة
                if (uiState.bomItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pluralStringResource(Res.plurals.bom_items_count, 0, 0),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = pluralStringResource(Res.plurals.bom_items_count, uiState.bomItems.size, uiState.bomItems.size),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.bomItems, key = { "bom-${it.id}" }) { bomItem ->
                            val component = uiState.availableComponents.find { it.id == bomItem.subPartId }
                            val substitutes = uiState.substitutesMap[bomItem.id] ?: emptyList()
                            BomItemCard(
                                bomItem = bomItem,
                                component = component,
                                substitutes = substitutes,
                                onAddSubstituteClick = { viewModel.openAddSubstituteDialog(bomItem) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isAddBomDialogOpen) {
        AddBomItemDialog(
            components = uiState.availableComponents,
            onDismiss = { viewModel.setAddDialogOpen(false) },
            onConfirm = { subPartId, qty, ref, opt, cons, alwVar, inh, note ->
                viewModel.addBomItem(subPartId, qty, ref, opt, cons, alwVar, inh, note)
            }
        )
    }

    if (uiState.isAddSubstituteDialogOpen && uiState.selectedBomItemForSubstitute != null) {
        val bomItem = uiState.selectedBomItemForSubstitute!!
        val currentSubPartId = bomItem.subPartId
        val eligibleParts = uiState.availableComponents.filter { it.id != currentSubPartId }

        AddSubstituteDialog(
            bomItem = bomItem,
            eligibleParts = eligibleParts,
            onDismiss = { viewModel.closeAddSubstituteDialog() },
            onConfirm = { partId -> viewModel.addSubstitute(partId) }
        )
    }
}

/**
 * بطاقة عرض بند قائمة المواد المجمعة BOM ومعلوماته الفنية والبدائل المعتمدة.
 */
@Composable
private fun BomItemCard(
    bomItem: BomItem,
    component: Part?,
    substitutes: List<BomItemSubstituteView> = emptyList(),
    onAddSubstituteClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = component?.name ?: "مكون #${bomItem.subPartId}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "الكمية المطلوبة: ${bomItem.quantity} ${component?.units ?: "pcs"}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (bomItem.reference.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "المرجع الهندسي (Ref): ${bomItem.reference}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (bomItem.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ملاحظات: ${bomItem.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // الشروط المنطقية المعتمدة
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (bomItem.optional) Badge("اختياري (Optional)", MaterialTheme.colorScheme.secondaryContainer)
                if (bomItem.consumable) Badge("مستهلك (Consumable)", MaterialTheme.colorScheme.errorContainer)
                if (bomItem.allowVariants) Badge("يسمح ببدائل المتغيرات", MaterialTheme.colorScheme.tertiaryContainer)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // شريط البدائل المعتمدة BomItemSubstitute
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "البدائل المعتمدة (${substitutes.size}):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                TextButton(
                    onClick = onAddSubstituteClick,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("+ إضافة بديل", fontSize = 11.sp)
                }
            }

            if (substitutes.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (sub in substitutes) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${sub.substitutePart.name}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "IPN: ${sub.substitutePart.ipn.ifBlank { "-" }}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
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

@Composable
private fun AddBomItemDialog(
    components: List<Part>,
    onDismiss: () -> Unit,
    onConfirm: (
        subPartId: Long,
        quantity: Double,
        reference: String,
        optional: Boolean,
        consumable: Boolean,
        allowVariants: Boolean,
        inherited: Boolean,
        note: String
    ) -> Unit
) {
    var selectedSubPartId by remember { mutableStateOf<Long?>(components.firstOrNull()?.id) }
    var quantityText by remember { mutableStateOf("1.0") }
    var reference by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var optional by remember { mutableStateOf(false) }
    var consumable by remember { mutableStateOf(false) }
    var allowVariants by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_bom_item), fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    val subId = selectedSubPartId
                    val qty = quantityText.toDoubleOrNull() ?: 1.0
                    if (subId != null && qty > 0.0) {
                        onConfirm(subId, qty, reference, optional, consumable, allowVariants, false, note)
                    }
                },
                enabled = selectedSubPartId != null && (quantityText.toDoubleOrNull() ?: 0.0) > 0.0
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
                Text("اختر القطعة المكونة الفرعية (SubPart):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(components, key = { "comp-select-${it.id}" }) { comp ->
                        FilterChip(
                            selected = selectedSubPartId == comp.id,
                            onClick = { selectedSubPartId = comp.id },
                            label = { Text(comp.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("الكمية المطلوبة (quantity > 0)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("المرجع الهندسي على البوردة (Reference/Designator)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظات تركيبية (Note)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = optional, onCheckedChange = { optional = it })
                    Text("مكون اختياري (Optional)", fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = consumable, onCheckedChange = { consumable = it })
                    Text("مكون مستهلك (Consumable)", fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = allowVariants, onCheckedChange = { allowVariants = it })
                    Text("السماح باستخدام بدائل المكون (Allow Variants)", fontSize = 12.sp)
                }
            }
        }
    )
}

@Composable
private fun AddSubstituteDialog(
    bomItem: BomItem,
    eligibleParts: List<Part>,
    onDismiss: () -> Unit,
    onConfirm: (partId: Long) -> Unit
) {
    var selectedPartId by remember { mutableStateOf<Long?>(eligibleParts.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة قطعة بديلة معتمدة (BomItemSubstitute)", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    val pId = selectedPartId
                    if (pId != null) {
                        onConfirm(pId)
                    }
                },
                enabled = selectedPartId != null
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
                Text("اختر القطعة البديلة المعتمدة:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(eligibleParts, key = { "eligible-sub-${it.id}" }) { p ->
                        FilterChip(
                            selected = selectedPartId == p.id,
                            onClick = { selectedPartId = p.id },
                            label = { Text(p.name) }
                        )
                    }
                }
            }
        }
    )
}
