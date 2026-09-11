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
import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PurchaseOrder
import com.nextstepai.inventory.data.PurchaseOrderLineItem

/**
 * شاشة إدارة أوامر الشراء (PurchaseOrder Management Screen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseOrderScreen(
    viewModel: PurchaseOrderViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أوامر الشراء (PurchaseOrder)") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("➔ العودة", fontSize = 14.sp)
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.setAddOrderDialogOpen(true) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("+ أمر شراء جديد")
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
                placeholder = { Text("البحث بالرمز المرجعي، المورد، أو وصف أمر الشراء...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // شريط تصفية حالات أمر الشراء (Status Filter Chips)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item(key = "po-status-all") {
                    FilterChip(
                        selected = uiState.statusFilter == null,
                        onClick = { viewModel.setStatusFilter(null) },
                        label = { Text("كافة الحالات") }
                    )
                }
                items(POStatus.entries.toTypedArray(), key = { "po-status-${it.code}" }) { status ->
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

            // قائمة أوامر الشراء مع استخدام المفاتيح الثابتة المستقرة key()
            if (uiState.orders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد أوامر شراء مطابقة لشروط البحث.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.orders, key = { "po-${it.id}" }) { order ->
                        PurchaseOrderCard(
                            order = order,
                            onClick = { viewModel.selectOrder(order) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.selectedOrder != null) {
        OrderDetailsDialog(
            order = uiState.selectedOrder!!,
            onDismiss = { viewModel.selectOrder(null) },
            onAddLineClick = { viewModel.setAddLineDialogOpen(true) },
            onIssueClick = { viewModel.issueOrder(uiState.selectedOrder!!.id) },
            onReceiveLine = { lineId, qty -> viewModel.receiveLineItem(lineId, qty) }
        )
    }

    if (uiState.isAddOrderDialogOpen) {
        AddOrderDialog(
            suppliers = uiState.suppliers,
            onDismiss = { viewModel.setAddOrderDialogOpen(false) },
            onConfirm = { ref, supId, desc, targetDate, curr ->
                viewModel.addPurchaseOrder(ref, supId, desc, targetDate, curr)
            }
        )
    }

    if (uiState.isAddLineDialogOpen && uiState.selectedOrder != null) {
        AddLineDialog(
            parts = uiState.parts,
            onDismiss = { viewModel.setAddLineDialogOpen(false) },
            onConfirm = { partId, qty, price, notes ->
                viewModel.addLineItem(partId, qty, price, notes)
            }
        )
    }
}

@Composable
private fun PurchaseOrderCard(
    order: PurchaseOrder,
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
                    text = order.reference,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                POStatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "المورد: ${order.supplierName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            if (order.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = order.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
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
                    text = "البنود: ${order.lineItemsCount} بند",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "الإجمالي: ${order.totalCost} ${order.orderCurrency}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun POStatusBadge(status: POStatus) {
    val (color, label) = when (status) {
        POStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant to "مسودة (Pending)"
        POStatus.PLACED -> MaterialTheme.colorScheme.primaryContainer to "معتمد ومصدر (Placed)"
        POStatus.COMPLETE -> MaterialTheme.colorScheme.secondaryContainer to "مكتمل (Complete)"
        POStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer to "ملغي (Cancelled)"
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
private fun OrderDetailsDialog(
    order: PurchaseOrder,
    onDismiss: () -> Unit,
    onAddLineClick: () -> Unit,
    onIssueClick: () -> Unit,
    onReceiveLine: (lineId: Long, qty: Double) -> Unit
) {
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
                Text("أمر الشراء: ${order.reference}", fontWeight = FontWeight.Bold)
                if (order.status == POStatus.PENDING) {
                    Button(
                        onClick = onIssueClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("اعتماد وإصدار ➔", fontSize = 11.sp)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("المورد: ${order.supplierName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("العملة: ${order.orderCurrency}", fontSize = 12.sp)
                }

                Text("تاريخ الإنشاء: ${order.creationDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (order.targetDate.isNotBlank()) {
                    Text("الموعد المستهدف: ${order.targetDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("بنود أمر الشراء (${order.lineItemsCount}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (!order.isStateLocked) {
                        TextButton(onClick = onAddLineClick) { Text("+ إضافة بند") }
                    }
                }

                if (order.lineItems.isEmpty()) {
                    Text("لا توجد بنود مضافة لهذا الأمر حتى الآن.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    order.lineItems.forEach { line ->
                        LineItemRow(line = line, isLocked = order.status == POStatus.PENDING, onReceive = onReceiveLine)
                    }
                }

                HorizontalDivider()

                Text("التكلفة الإجمالية: ${order.totalCost} ${order.orderCurrency}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun LineItemRow(
    line: PurchaseOrderLineItem,
    isLocked: Boolean,
    onReceive: (Long, Double) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(line.partName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("${line.lineTotal} USD", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المطلوب: ${line.quantity} | المستلم: ${line.receivedQuantity}",
                    fontSize = 11.sp,
                    color = if (line.isFullyReceived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

                if (!isLocked && !line.isFullyReceived) {
                    Button(
                        onClick = { onReceive(line.id, line.quantity - line.receivedQuantity) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("استلام الكامل", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddOrderDialog(
    suppliers: List<Company>,
    onDismiss: () -> Unit,
    onConfirm: (reference: String, supplierId: Long, desc: String, targetDate: String, currency: String) -> Unit
) {
    var reference by remember { mutableStateOf("PO-2025-002") }
    var selectedSupplierId by remember { mutableStateOf<Long?>(suppliers.firstOrNull()?.id) }
    var description by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("2025-03-01") }
    var currency by remember { mutableStateOf("USD") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء أمر شراء جديد", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    if (reference.isNotBlank() && selectedSupplierId != null) {
                        onConfirm(reference, selectedSupplierId!!, description, targetDate, currency)
                    }
                },
                enabled = reference.isNotBlank() && selectedSupplierId != null
            ) {
                Text("حفظ الأمر")
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
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("الرمز المرجعي (reference) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("اختر المورد (supplier):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suppliers, key = { "dialog-supplier-${it.id}" }) { s ->
                        FilterChip(
                            selected = selectedSupplierId == s.id,
                            onClick = { selectedSupplierId = s.id },
                            label = { Text(s.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف أمر الشراء (description)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetDate,
                        onValueChange = { targetDate = it },
                        label = { Text("تاريخ التسليم المستهدف") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("العملة") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }
    )
}

@Composable
private fun AddLineDialog(
    parts: List<Part>,
    onDismiss: () -> Unit,
    onConfirm: (partId: Long, qty: Double, price: Double, notes: String) -> Unit
) {
    var selectedPartId by remember { mutableStateOf<Long?>(parts.firstOrNull()?.id) }
    var qtyText by remember { mutableStateOf("10.0") }
    var priceText by remember { mutableStateOf("1.0") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة بند لأمر الشراء", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedPartId != null) {
                        onConfirm(
                            selectedPartId!!,
                            qtyText.toDoubleOrNull() ?: 1.0,
                            priceText.toDoubleOrNull() ?: 0.0,
                            notes
                        )
                    }
                },
                enabled = selectedPartId != null
            ) {
                Text("إضافة البند")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("اختر القطعة:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(parts, key = { "add-po-part-${it.id}" }) { p ->
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
                        label = { Text("الكمية") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("سعر الوحدة") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات البند") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
