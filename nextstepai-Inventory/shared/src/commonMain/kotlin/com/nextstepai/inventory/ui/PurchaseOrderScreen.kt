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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PurchaseOrder
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_order
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.orders_count
import nextstepai_inventory.shared.generated.resources.save

/**
 * شاشة إدارة أوامر الشراء (PurchaseOrder Management Screen).
 * تم تحديثها لتتبع الهيكل القياسي للتطبيق، شريط البحث والباركود المدمج، وشريط التصفية التفاعلي.
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
            PurchaseOrdersTopBar(
                onBackClick = onBackClick,
                onAddClick = { viewModel.setAddOrderDialogOpen(true) }
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
                Spacer(modifier = Modifier.height(12.dp))

                // شريط البحث المطور مع زر الباركود المدمج
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            text = "البحث بالرمز المرجعي، المورد، أو الوصف...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "مسح الباركود",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // شريط تصفية حالات أمر الشراء (POStatus Filter Chips)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item(key = "po-status-all") {
                        FilterChip(
                            selected = uiState.statusFilter == null,
                            onClick = { viewModel.setStatusFilter(null) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("كافة الحالات")
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (uiState.statusFilter == null) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "${uiState.orders.size}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(50)
                        )
                    }

                    items(POStatus.entries, key = { "po-status-${it.code}" }) { status ->
                        val count = uiState.orders.count { it.status == status }
                        FilterChip(
                            selected = uiState.statusFilter == status,
                            onClick = { viewModel.setStatusFilter(if (uiState.statusFilter == status) null else status) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val statusDotColor = when (status) {
                                        POStatus.COMPLETE -> Color(0xFF10B981)
                                        POStatus.PLACED -> Color(0xFF0284C7)
                                        POStatus.CANCELLED -> MaterialTheme.colorScheme.error
                                        else -> Color(0xFFF59E0B)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(statusDotColor)
                                    )
                                    Text(status.label)
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (uiState.statusFilter == status) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(50)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pluralStringResource(Res.plurals.orders_count, uiState.orders.size, uiState.orders.size),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
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

/**
 * الترويسة العلوية لشاشة أوامر الشراء Top App Bar
 */
@Composable
private fun PurchaseOrdersTopBar(
    onBackClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // اليمين: زر الرجوع وعنوان الشاشة
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                Text(
                    text = "جدول أوامر الشراء وبنودها",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFF0F172A)
                )
            }

            // اليسار: زر "+ أمر شراء جديد"
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.add_new_order),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "أمر شراء جديد",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

/**
 * بطاقة امر الشراء التفاعلية المطورة
 */
@Composable
private fun PurchaseOrderCard(
    order: PurchaseOrder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // السطر العلوي: الكود المرجعي والشارة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // اليسار: شارة الحالة
                Surface(
                    shape = RoundedCornerShape(50),
                    color = when (order.status) {
                        POStatus.COMPLETE -> Color(0xFFECFDF5)
                        POStatus.PLACED -> Color(0xFFF0F9FF)
                        POStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                        else -> Color(0xFFFFFBEB)
                    },
                    border = BorderStroke(1.dp, when (order.status) {
                        POStatus.COMPLETE -> Color(0xFFA7F3D0)
                        POStatus.PLACED -> Color(0xFFE0F2FE)
                        else -> Color.Transparent
                    })
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statusDotColor = when (order.status) {
                            POStatus.COMPLETE -> Color(0xFF10B981)
                            POStatus.PLACED -> Color(0xFF0284C7)
                            POStatus.CANCELLED -> MaterialTheme.colorScheme.error
                            else -> Color(0xFFF59E0B)
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )
                        Text(
                            text = order.status.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (order.status) {
                                POStatus.COMPLETE -> Color(0xFF059669)
                                POStatus.PLACED -> Color(0xFF0284C7)
                                POStatus.CANCELLED -> MaterialTheme.colorScheme.error
                                else -> Color(0xFFB45309)
                            }
                        )
                    }
                }

                // اليمين: الرمز المرجعي
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF0F9FF)
                    ) {
                        Text(
                            text = "PO",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0284C7),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = order.reference,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // اسم المورد
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = order.supplierName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (order.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = order.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // التكلفة الإجمالية وعدد البنود
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "التكلفة الإجمالية للطلب:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${order.totalCost} ${order.orderCurrency}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0284C7)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "عدد البنود المسجلة:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${order.lineItems.size} بنود",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * حوار تفاصيل ومتابعة امر الشراء
 */
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
        shape = RoundedCornerShape(24.dp),
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("أمر شراء: ${order.reference}", fontWeight = FontWeight.Bold)

                if (order.status == POStatus.PENDING) {
                    Button(
                        onClick = { onIssueOrder(order.id) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("اعتماد وإصدار ➔", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("رقم الطلب:", "#${order.id}")
                DetailRow("المورد:", order.supplierName)
                DetailRow("الحالة الحالية:", order.status.label)
                DetailRow("إجمالي التكلفة:", "${order.totalCost} ${order.orderCurrency}")
                DetailRow("تاريخ التسليم المستهدف:", order.targetDate.ifBlank { "-" })

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("بنود أمر الشراء (${order.lineItems.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    if (order.status == POStatus.PENDING) {
                        OutlinedButton(
                            onClick = onAddLineClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+ إضافة بند", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (order.lineItems.isEmpty()) {
                    Text("لا توجد بنود مضافة لهذا الطلب بعد.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    for (line in order.lineItems) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
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
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp)
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
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("إنشاء أمر شراء جديد (PO)", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val supId = selectedSupplierId
                    if (reference.isNotBlank() && supId != null) {
                        onConfirm(reference, supId, description, targetDate, currency)
                    }
                },
                enabled = reference.isNotBlank() && selectedSupplierId != null,
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
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("الرمز المرجعي لأمر الشراء (Reference) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
                            label = { Text(sup.name, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف الطلب وملاحظاته") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("تاريخ التسليم المتوقع (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text("العملة المعتمدة للطلب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("إضافة بند جديد لأمر الشراء", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
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
                enabled = selectedPartId != null && (quantityText.toDoubleOrNull() ?: 0.0) > 0.0,
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
                Text("اختر القطعة المطلوبة:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(parts, key = { "po-part-select-${it.id}" }) { p ->
                        FilterChip(
                            selected = selectedPartId == p.id,
                            onClick = { selectedPartId = p.id },
                            label = { Text(p.name, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("الكمية المطلوبة للشراء") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("سعر شراء الوحدة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات البند") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    )
}
