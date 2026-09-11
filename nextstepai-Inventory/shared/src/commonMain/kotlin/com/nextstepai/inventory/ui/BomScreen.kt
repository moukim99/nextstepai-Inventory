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
import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.Part

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
                title = { Text("قائمة مواد التصنيع (BOM)") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("➔ العودة", fontSize = 14.sp)
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.setAddDialogOpen(true) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("+ إضافة مكون BOM")
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

            // قائمة بنود BOM مع المفاتيح المستقرة key()
            if (uiState.bomItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مكونات مسجلة في قائمة المواد لهذا المنتج.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.bomItems, key = { "bom-item-${it.id}" }) { item ->
                        val subPart = uiState.availableComponents.find { it.id == item.subPartId }
                        BomItemCard(
                            bomItem = item,
                            subPartName = subPart?.name ?: "مكون #${item.subPartId}",
                            subPartIpn = subPart?.ipn ?: ""
                        )
                    }
                }
            }
        }
    }

    if (uiState.isAddBomDialogOpen) {
        AddBomItemDialog(
            components = uiState.availableComponents.filter { it.id != uiState.selectedPartId },
            onDismiss = { viewModel.setAddDialogOpen(false) },
            onConfirm = { subPartId, qty, ref, optional, consumable, allowVariants, inherited, note ->
                viewModel.addBomItem(subPartId, qty, ref, optional, consumable, allowVariants, inherited, note)
            }
        )
    }
}

@Composable
private fun BomItemCard(
    bomItem: BomItem,
    subPartName: String,
    subPartIpn: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subPartName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "الكمية: ${bomItem.quantity}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (subPartIpn.isNotBlank() || bomItem.reference.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (subPartIpn.isNotBlank()) {
                        Text(text = "IPN: $subPartIpn", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (bomItem.reference.isNotBlank()) {
                        Text(text = "المرجع: ${bomItem.reference}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (bomItem.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ملاحظة: ${bomItem.note}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (bomItem.optional) BomBadge("اختياري (Optional)", MaterialTheme.colorScheme.secondaryContainer)
                if (bomItem.consumable) BomBadge("استهلاكي (Consumable)", MaterialTheme.colorScheme.tertiaryContainer)
                if (bomItem.allowVariants) BomBadge("بدائل مسموحة", MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

@Composable
private fun BomBadge(text: String, color: Color) {
    Surface(color = color, shape = RoundedCornerShape(4.dp)) {
        Text(text = text, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun AddBomItemDialog(
    components: List<Part>,
    onDismiss: () -> Unit,
    onConfirm: (
        subPartId: Long,
        qty: Double,
        ref: String,
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
    var optional by remember { mutableStateOf(false) }
    var consumable by remember { mutableStateOf(false) }
    var allowVariants by remember { mutableStateOf(false) }
    var inherited by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مكون جديد (BomItem)", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedSubPartId != null) {
                        onConfirm(
                            selectedSubPartId!!,
                            quantityText.toDoubleOrNull() ?: 1.0,
                            reference,
                            optional,
                            consumable,
                            allowVariants,
                            inherited,
                            note
                        )
                    }
                },
                enabled = selectedSubPartId != null
            ) {
                Text("إضافة للبند")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("اختر المكون الفرعي (sub_part):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(components, key = { "dialog-component-${it.id}" }) { comp ->
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
                    label = { Text("الكمية المطلوب (quantity) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("المرجع الهندسي/المكاني (reference e.g. R1, C3)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("تعليمات التثبيت والاستهلاك (note)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = optional, onCheckedChange = { optional = it })
                    Text("مكون اختياري (optional)", fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = consumable, onCheckedChange = { consumable = it })
                    Text("مكون استهلاكي (consumable)", fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = allowVariants, onCheckedChange = { allowVariants = it })
                    Text("السماح باستخدام البدائل (allow_variants)", fontSize = 12.sp)
                }
            }
        }
    )
}
