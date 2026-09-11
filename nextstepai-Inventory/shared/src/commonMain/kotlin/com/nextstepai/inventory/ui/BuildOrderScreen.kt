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
import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.Part

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
                title = { Text("أوامر التصنيع والإنتاج (Build)") },
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
                        Text("+ أمر إنتاج جديد")
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
            // شريط البحث المطور
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("البحث بالرمز المرجعي، العنوان، المنتج، أو رقم التشغيلة...") },
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
                        label = { Text("كافة الحالات") }
                    )
                }
                items(BuildStatus.entries.toTypedArray(), key = { "build-status-${it.code}" }) { status ->
                    FilterChip(
                        selected = uiState.statusFilter == status,
                        onClick = { viewModel.setStatusFilter(if (uiState.statusFilter == status) null else status) },
                        label = { Text(status.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Text(
                        text = "⚠️ ${uiState.errorMessage}",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp),
                        fontSize = 12.sp
                    )
                }
            }

            // قائمة أوامر الإنتاج مع المفاتيح المستقرة key()
            if (uiState.builds.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد أوامر إنتاج مطابقة لشروط البحث.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
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

    if (uiState.selectedBuild != null) {
        BuildDetailsDialog(
            build = uiState.selectedBuild!!,
            onDismiss = { viewModel.selectBuild(null) },
            onStartProduction = { viewModel.startProduction(uiState.selectedBuild!!.id) },
            onCompleteOutput = { qty -> viewModel.completeBuildOutput(uiState.selectedBuild!!.id, qty) }
        )
    }

    if (uiState.isAddBuildDialogOpen) {
        AddBuildDialog(
            assemblyParts = uiState.assemblyParts,
            onDismiss = { viewModel.setAddDialogOpen(false) },
            onConfirm = { ref, title, partId, qty, batch, targetDate, notes ->
                viewModel.addBuildOrder(ref, title, partId, qty, batch, targetDate, notes)
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
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
                    text = build.reference,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                BuildStatusBadge(status = build.status)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "المنتج: ${build.partName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            if (build.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = build.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // مؤشر شريط التقدم للإنتاج المكتمل
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الإنجاز: ${build.completedQuantity} / ${build.quantity} (${build.completionPercentage.toInt()}%)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (build.batch.isNotBlank()) {
                    Text(text = "التشغيلة: ${build.batch}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { build.completionPercentage / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (build.canComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun BuildStatusBadge(status: BuildStatus) {
    val (color, label) = when (status) {
        BuildStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant to "مسودة (Pending)"
        BuildStatus.IN_PRODUCTION -> MaterialTheme.colorScheme.primaryContainer to "قيد التصنيع (Production)"
        BuildStatus.COMPLETE -> MaterialTheme.colorScheme.secondaryContainer to "مكتمل (Complete)"
        BuildStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer to "ملغي (Cancelled)"
    }

    Surface(color = color, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun BuildDetailsDialog(
    build: BuildOrder,
    onDismiss: () -> Unit,
    onStartProduction: () -> Unit,
    onCompleteOutput: (Double) -> Unit
) {
    var outputQtyText by remember { mutableStateOf("10.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
        title = {
            Text("أمر الإنتاج: ${build.reference}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("المنتج الأب: ${build.partName}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (build.title.isNotBlank()) Text("العنوان: ${build.title}", fontSize = 12.sp)
                Text("حجم الإنتاج المطلوب: ${build.quantity}", fontSize = 12.sp)
                Text("الكمية المنجزة فعلياً: ${build.completedQuantity}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Text("تاريخ الإنشاء: ${build.creationDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (build.targetDate.isNotBlank()) Text("تاريخ التسليم المستهدف: ${build.targetDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)

                HorizontalDivider()

                if (build.status == BuildStatus.PENDING) {
                    Button(
                        onClick = onStartProduction,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("بدء عملية التصنيع والإنتاج الفعلي ➔")
                    }
                } else if (build.status == BuildStatus.IN_PRODUCTION) {
                    Text("توريد كمية منتجة جديدة (Complete Build Output):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = outputQtyText,
                            onValueChange = { outputQtyText = it },
                            label = { Text("الكمية") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val q = outputQtyText.toDoubleOrNull() ?: 0.0
                                if (q > 0) onCompleteOutput(q)
                            }
                        ) {
                            Text("تأكيد الإنهاء")
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun AddBuildDialog(
    assemblyParts: List<Part>,
    onDismiss: () -> Unit,
    onConfirm: (reference: String, title: String, partId: Long, qty: Double, batch: String, targetDate: String, notes: String) -> Unit
) {
    var reference by remember { mutableStateOf("BO-2025-002") }
    var title by remember { mutableStateOf("") }
    var selectedPartId by remember { mutableStateOf<Long?>(assemblyParts.firstOrNull()?.id) }
    var qtyText by remember { mutableStateOf("10.0") }
    var batch by remember { mutableStateOf("BATCH-2025-02") }
    var targetDate by remember { mutableStateOf("2025-03-15") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء أمر إنتاج جديد (Build)", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    if (reference.isNotBlank() && selectedPartId != null) {
                        onConfirm(
                            reference,
                            title,
                            selectedPartId!!,
                            qtyText.toDoubleOrNull() ?: 1.0,
                            batch,
                            targetDate,
                            notes
                        )
                    }
                },
                enabled = reference.isNotBlank() && selectedPartId != null
            ) {
                Text("حفظ الأمر")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("الكود المرجعي (reference) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان أود وصف الهدف (title)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("اختر المنتج الأب المجمع (assembly = true):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(assemblyParts, key = { "add-build-part-${it.id}" }) { p ->
                        FilterChip(
                            selected = selectedPartId == p.id,
                            onClick = { selectedPartId = p.id },
                            label = { Text(p.name) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("كمية الإنتاج (quantity)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = batch,
                        onValueChange = { batch = it },
                        label = { Text("رقم التشغيلة (batch)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("التاريخ المستهدف لإنهاء الإنتاج") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
