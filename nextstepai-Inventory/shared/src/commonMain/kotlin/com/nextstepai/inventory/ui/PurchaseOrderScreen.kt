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
import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PurchaseOrder
import com.nextstepai.inventory.data.PurchaseOrderLineItem
import com.nextstepai.inventory.ui.theme.AppIcons
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_order
import nextstepai_inventory.shared.generated.resources.back
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.card_orders_title
import nextstepai_inventory.shared.generated.resources.orders_count
import nextstepai_inventory.shared.generated.resources.save
import nextstepai_inventory.shared.generated.resources.search_placeholder

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
                title = { Text(stringResource(Res.string.card_orders_title)) },
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
                        onClick = { viewModel.setAddOrderDialogOpen(true) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(AppIcons.Add),
                            contentDescription = stringResource(Res.string.add_new_order),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.add_new_order))
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

                // شريط تصفية حالات أمر الشراء (Status Filter Chips)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item(key = "po-status-all") {
                        FilterChip(
                            selected = uiState.statusFilter == null,
                            onClick = { viewModel.setStatusFilter(null) },
                            label = { Text("الكل") }
                        )
                    }
                    items(POStatus.entries, key = { "po-status-${it.code}" }) { status ->
                        FilterChip(
                            selected = uiState.statusFilter == status,
                            onClick = { viewModel.setStatusFilter(if (uiState.statusFilter == status) null else status) },
                            label = { Text(status.label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.orders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pluralStringResource(Res.plurals.orders_count, 0, 0),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = pluralStringResource(Res.plurals.orders_count, uiState.orders.size, uiState.orders.size),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

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
    }

    if (uiState.selectedOrder != null) {
        OrderDetailsDialog(
            order = uiState.selectedOrder!!,
            onIssueOrder = { viewModel.issueOrder(it) },
            onAddLineClick = { viewModel.setAddLineDialogOpen(true) },
            onReceiveLine = { lineId, qty -> viewModel.receiveLineItem(lineId, qty) },
            onDismiss = { viewModel.selectOrder(null) }
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
        AddLineItemDialog(
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
                    text = "الرمز المرجعي: ${order.reference}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Surface(
                    color = when (order.status) {
                        POStatus.COMPLETE -> MaterialTheme.colorScheme.primaryContainer
                        POStatus.PLACED -> MaterialTheme.colorScheme.secondaryContainer
                        POStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = order.status.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "المورد: ${order.supplierName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (order.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = order.description,
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
                    text = "التكلفة الإجمالية: ${order.totalCost} ${order.orderCurrency}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "عدد البنود: ${order.lineItems.size}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OrderDetailsDialog(
    order: PurchaseOrder,
    onIssueOrder: (orderId: Long) -> Unit,
    onAddLineClick: () -> Unit,
    onReceiveLine: (lineId: Long, qty: Double) -> Unit,
    onDismiss: () -> Unit
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
                Text("أمر شراء: ${order.reference}", fontWeight = FontWeight.Bold)

                if (order.status == POStatus.PENDING) {
                    Button(onClick = { onIssueOrder(order.id) }) {
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
                DetailRow("رقم الطلب:", "#${order.id}")
                DetailRow("المورد:", order.supplierName)
                DetailRow("الحالة الحالية:", order.status.label)
                DetailRow("إجمالي التكلفة:", "${order.totalCost} ${order.orderCurrency}")
                DetailRow("تاريخ التسليم المستهدف:", order.targetDate.ifBlank { "-" })

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("بنود أمر الشراء (${order.lineItems.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    if (order.status == POStatus.PENDING) {
                        OutlinedButton(
                            onClick = onAddLineClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+ إضافة بند", fontSize = 11.sp)
                        }
                    }
                }

                if (order.lineItems.isEmpty()) {
                    Text("لا توجد بنود مضافة لهذا الطلب بعد.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    for (line in order.lineItems) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = line.partName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(text = "السعر: ${line.purchasePrice} ${order.orderCurrency}", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "المطلوب: ${line.quantity} | المستلم: ${line.receivedQuantity}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (order.status == POStatus.PLACED && line.receivedQuantity < line.quantity) {
                                        Button(
                                            onClick = { onReceiveLine(line.id, line.quantity - line.receivedQuantity) },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("استلام الكامل", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
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
private fun AddOrderDialog(
    suppliers: List<Company>,
    onDismiss: () -> Unit,
    onConfirm: (reference: String, supplierId: Long, description: String, targetDate: String, currency: String) -> Unit
) {
    var reference by remember { mutableStateOf("PO-2025-001") }
    var selectedSupplierId by remember { mutableStateOf<Long?>(suppliers.firstOrNull()?.id) }
    var description by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("2025-03-30") }
    var currency by remember { mutableStateOf("USD") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_new_order), fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    val supId = selectedSupplierId
                    if (reference.isNotBlank() && supId != null) {
                        onConfirm(reference, supId, description, targetDate, currency)
                    }
                },
                enabled = reference.isNotBlank() && selectedSupplierId != null
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
                    label = { Text("الرمز المرجعي لأمر الشراء (Reference) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("اختر المورد:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suppliers, key = { "po-supplier-${it.id}" }) { sup ->
                        FilterChip(
                            selected = selectedSupplierId == sup.id,
                            onClick = {
                                selectedSupplierId = sup.id
                                currency = sup.currency
                            },
                            label = { Text(sup.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف الطلب وملاحظاته") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("تاريخ التسليم المتوقع (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text("العملة المعتمدة للطلب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
private fun AddLineItemDialog(
    parts: List<Part>,
    onDismiss: () -> Unit,
    onConfirm: (partId: Long, quantity: Double, price: Double, notes: String) -> Unit
) {
    var selectedPartId by remember { mutableStateOf<Long?>(parts.firstOrNull()?.id) }
    var quantityText by remember { mutableStateOf("10.0") }
    var priceText by remember { mutableStateOf("5.0") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة بند جديد لأمر الشراء", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    val pId = selectedPartId
                    val qty = quantityText.toDoubleOrNull() ?: 1.0
                    val prc = priceText.toDoubleOrNull() ?: 0.0
                    if (pId != null && qty > 0.0) {
                        onConfirm(pId, qty, prc, notes)
                    }
                },
                enabled = selectedPartId != null && (quantityText.toDoubleOrNull() ?: 0.0) > 0.0
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
                Text("اختر القطعة المطلوبة:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(parts, key = { "po-part-select-${it.id}" }) { p ->
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
                    label = { Text("الكمية المطلوبة للشراء") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("سعر شراء الوحدة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
