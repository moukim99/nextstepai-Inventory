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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.ui.theme.AppIcons
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_build
import nextstepai_inventory.shared.generated.resources.back
import nextstepai_inventory.shared.generated.resources.builds_count
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.card_builds_title
import nextstepai_inventory.shared.generated.resources.save
import nextstepai_inventory.shared.generated.resources.search_placeholder

/**
 * شاشة إدارة أوامر التصنيع والإنتاج (BuildOrder Screen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildOrderScreen(
    viewModel: BuildOrderViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.card_builds_title)) },
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
                            contentDescription = stringResource(Res.string.add_new_build),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.add_new_build))
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

                // شريط تصفية حالات أوامر الإنتاج
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item(key = "build-status-all") {
                        FilterChip(
                            selected = uiState.statusFilter == null,
                            onClick = { viewModel.setStatusFilter(null) },
                            label = { Text("الكل") }
                        )
                    }
                    items(BuildStatus.entries, key = { "build-status-${it.code}" }) { status ->
                        FilterChip(
                            selected = uiState.statusFilter == status,
                            onClick = { viewModel.setStatusFilter(if (uiState.statusFilter == status) null else status) },
                            label = { Text(status.label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.builds.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pluralStringResource(Res.plurals.builds_count, 0, 0),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = pluralStringResource(Res.plurals.builds_count, uiState.builds.size, uiState.builds.size),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.builds, key = { "build-${it.id}" }) { build ->
                            BuildOrderCard(
                                build = build,
                                onClick = { viewModel.selectBuild(build) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.selectedBuild != null) {
        BuildDetailsDialog(
            build = uiState.selectedBuild!!,
            onStartProduction = { viewModel.startProduction(it) },
            onCompleteOutput = { buildId, qty -> viewModel.completeBuildOutput(buildId, qty) },
            onDismiss = { viewModel.selectBuild(null) }
        )
    }

    if (uiState.isAddBuildDialogOpen) {
        AddBuildDialog(
            assemblyParts = uiState.assemblyParts,
            onDismiss = { viewModel.setAddDialogOpen(false) },
            onConfirm = { ref, title, partId, qty, batch, date, notes ->
                viewModel.addBuildOrder(ref, title, partId, qty, batch, date, notes)
            }
        )
    }
}

@Composable
private fun BuildOrderCard(
    build: BuildOrder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    text = "المرجع: ${build.reference}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Surface(
                    color = when (build.status) {
                        BuildStatus.COMPLETE -> MaterialTheme.colorScheme.primaryContainer
                        BuildStatus.IN_PRODUCTION -> MaterialTheme.colorScheme.tertiaryContainer
                        BuildStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = build.status.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "المنتج الأب: ${build.partName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (build.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = build.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الكمية: ${build.completedQuantity} / ${build.quantity}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "الإنجاز: ${build.completionPercentage}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { build.completionPercentage / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun BuildDetailsDialog(
    build: BuildOrder,
    onStartProduction: (buildId: Long) -> Unit,
    onCompleteOutput: (buildId: Long, qty: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var outputQtyText by remember { mutableStateOf("1.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("أمر تصنيع: ${build.reference}", fontWeight = FontWeight.Bold)

                if (build.status == BuildStatus.PENDING) {
                    Button(onClick = { onStartProduction(build.id) }) {
                        Text("بدء التصنيع 🏭", fontSize = 11.sp)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("رقم الأمر:", "#${build.id}")
                DetailRow("العنوان:", build.title.ifBlank { "-" })
                DetailRow("المنتج الأب Target Part:", build.partName)
                DetailRow("الحالة:", build.status.label)
                DetailRow("تشغيلة الدفعة Batch:", build.batch.ifBlank { "-" })
                DetailRow("تاريخ الإنجاز المستهدف Target Date:", build.targetDate.ifBlank { "-" })
                DetailRow("الكمية المطلوبة Total Qty:", "${build.quantity}")
                DetailRow("الكمية المكتملة Completed Qty:", "${build.completedQuantity}")
                DetailRow("نسبة الإنجاز:", "${build.completionPercentage}%")

                if (build.status == BuildStatus.IN_PRODUCTION && build.completedQuantity < build.quantity) {
                    HorizontalDivider()
                    Text("توريد مخرجات تصنيع جديدة (Build Output):", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = outputQtyText,
                            onValueChange = { outputQtyText = it },
                            label = { Text("الكمية المخرجة") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val qty = outputQtyText.toDoubleOrNull() ?: 0.0
                                if (qty > 0.0) {
                                    onCompleteOutput(build.id, qty)
                                }
                            },
                            enabled = (outputQtyText.toDoubleOrNull() ?: 0.0) > 0.0
                        ) {
                            Text("توريد 📦", fontSize = 11.sp)
                        }
                    }
                }
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

@Composable
private fun AddBuildDialog(
    assemblyParts: List<Part>,
    onDismiss: () -> Unit,
    onConfirm: (
        reference: String,
        title: String,
        partId: Long,
        quantity: Double,
        batch: String,
        targetDate: String,
        notes: String
    ) -> Unit
) {
    var reference by remember { mutableStateOf("BO-2025-001") }
    var title by remember { mutableStateOf("") }
    var selectedPartId by remember { mutableStateOf<Long?>(assemblyParts.firstOrNull()?.id) }
    var quantityText by remember { mutableStateOf("10.0") }
    var batch by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("2025-04-15") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_new_build), fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    val pId = selectedPartId
                    val qty = quantityText.toDoubleOrNull() ?: 1.0
                    if (reference.isNotBlank() && pId != null && qty > 0.0) {
                        onConfirm(reference, title, pId, qty, batch, targetDate, notes)
                    }
                },
                enabled = reference.isNotBlank() && selectedPartId != null && (quantityText.toDoubleOrNull() ?: 0.0) > 0.0
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
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("الرمز المرجعي لأمر الإنتاج (Reference) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان أمر التصنيع") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("اختر المنتج المجمع الأب (Assembly Target):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(assemblyParts, key = { "build-assembly-${it.id}" }) { p ->
                        FilterChip(
                            selected = selectedPartId == p.id,
                            onClick = { selectedPartId = p.id },
                            label = { Text(p.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("الكمية المطلوب تصنيعها (Quantity)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = batch,
                    onValueChange = { batch = it },
                    label = { Text("رقم التشغيلة/الدفعة (Batch Code)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("تاريخ الإنجاز المستهدف (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات التصنيع") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
