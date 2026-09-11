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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.StockStatus
import com.nextstepai.inventory.ui.theme.AppIcons
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_stock
import nextstepai_inventory.shared.generated.resources.back
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.card_stock_title
import nextstepai_inventory.shared.generated.resources.save
import nextstepai_inventory.shared.generated.resources.stock_items_count

/**
 * شاشة إدارة المخزون الفعلي (StockItem Management Screen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    viewModel: StockViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.card_stock_title)) },
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
                            contentDescription = stringResource(Res.string.add_new_stock),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.add_new_stock))
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
                    text = "تصفية حسب موقع التخزين في المستودع:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // اختيار موقع التخزين عبر LazyRow مع المفاتيح المستقرة key()
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item(key = "location-all") {
                        FilterChip(
                            selected = uiState.selectedLocationId == null,
                            onClick = { viewModel.filterByLocation(null) },
                            label = { Text("كافة المواقع") }
                        )
                    }
                    items(uiState.locations, key = { "location-${it.id}" }) { loc ->
                        FilterChip(
                            selected = uiState.selectedLocationId == loc.id,
                            onClick = { viewModel.filterByLocation(if (uiState.selectedLocationId == loc.id) null else loc.id) },
                            label = { Text(loc.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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

                if (uiState.stockItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pluralStringResource(Res.plurals.stock_items_count, 0, 0),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = pluralStringResource(Res.plurals.stock_items_count, uiState.stockItems.size, uiState.stockItems.size),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.stockItems, key = { "stock-${it.id}" }) { item ->
                            val part = uiState.parts.find { it.id == item.partId }
                            val loc = uiState.locations.find { it.id == item.locationId }
                            StockItemCard(
                                stockItem = item,
                                part = part,
                                location = loc,
                                onSplitClick = { viewModel.setSelectedItemForSplit(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isAddStockDialogOpen) {
        AddStockItemDialog(
            parts = uiState.parts,
            locations = uiState.locations,
            onDismiss = { viewModel.setAddDialogOpen(false) },
            onConfirm = { partId, locId, qty, serial, batch, pack, status, notes ->
                viewModel.addStockItem(partId, locId, qty, serial, batch, pack, status, notes)
            }
        )
    }

    if (uiState.selectedItemForSplit != null) {
        SplitStockDialog(
            item = uiState.selectedItemForSplit!!,
            onDismiss = { viewModel.setSelectedItemForSplit(null) },
            onConfirm = { qty -> viewModel.splitStockItem(uiState.selectedItemForSplit!!.id, qty) }
        )
    }
}

/**
 * بطاقة عرض الوحدة المخزنية المادية في المستودع.
 */
@Composable
private fun StockItemCard(
    stockItem: StockItem,
    part: Part?,
    location: StockLocation?,
    onSplitClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = part?.name ?: "قطعة #${stockItem.partId}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Surface(
                    color = when (stockItem.status) {
                        StockStatus.OK -> MaterialTheme.colorScheme.primaryContainer
                        StockStatus.QUARANTINE -> MaterialTheme.colorScheme.tertiaryContainer
                        StockStatus.DAMAGED, StockStatus.DESTROYED, StockStatus.REJECTED, StockStatus.EXPIRED -> MaterialTheme.colorScheme.errorContainer
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = stockItem.status.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "الكمية المادية: ${stockItem.quantity} ${part?.units ?: "pcs"}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "الموقع: ${location?.name ?: "غير محدد"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (stockItem.serial.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الرقم التسلسلي (SN): ${stockItem.serial}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (stockItem.batch.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "تشغيلة الدفعة (Batch): ${stockItem.batch}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (stockItem.serial.isBlank() && stockItem.quantity > 1.0) {
                    OutlinedButton(
                        onClick = onSplitClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("تجزئة الكمية (Split)", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddStockItemDialog(
    parts: List<Part>,
    locations: List<StockLocation>,
    onDismiss: () -> Unit,
    onConfirm: (
        partId: Long,
        locationId: Long?,
        quantity: Double,
        serial: String,
        batch: String,
        packaging: String,
        status: StockStatus,
        notes: String
    ) -> Unit
) {
    var selectedPartId by remember { mutableStateOf<Long?>(parts.firstOrNull()?.id) }
    var selectedLocationId by remember { mutableStateOf<Long?>(locations.firstOrNull()?.id) }
    var quantityText by remember { mutableStateOf("1.0") }
    var serial by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("") }
    var packaging by remember { mutableStateOf("Box") }
    var selectedStatus by remember { mutableStateOf(StockStatus.OK) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_new_stock), fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    val pId = selectedPartId
                    val qty = quantityText.toDoubleOrNull() ?: 1.0
                    if (pId != null && qty > 0.0) {
                        onConfirm(pId, selectedLocationId, qty, serial, batch, packaging, selectedStatus, notes)
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
                Text("اختر القطعة المراد استلامها:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(parts, key = { "stock-part-${it.id}" }) { p ->
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
                    label = { Text("الكمية المادية") },
                    singleLine = true,
                    enabled = serial.isBlank(), // إجبار الكمية إلى 1.0 عند إدخال الرقم التسلسلي
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = serial,
                    onValueChange = {
                        serial = it
                        if (it.isNotBlank()) quantityText = "1.0"
                    },
                    label = { Text("الرقم التسلسلي الفريد (Serial Number)") },
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
                    value = packaging,
                    onValueChange = { packaging = it },
                    label = { Text("نوع التغليف (Box / Reel / Tray)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات الفحص والاستلام") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
private fun SplitStockDialog(
    item: StockItem,
    onDismiss: () -> Unit,
    onConfirm: (splitQty: Double) -> Unit
) {
    var splitQtyText by remember { mutableStateOf("1.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تجزئة الكمية المخزنية (Split Stock)", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    val qty = splitQtyText.toDoubleOrNull() ?: 0.0
                    if (qty > 0.0 && qty < item.quantity) {
                        onConfirm(qty)
                    }
                },
                enabled = (splitQtyText.toDoubleOrNull() ?: 0.0) in 0.1..<item.quantity
            ) {
                Text("تأكيد التجزئة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("إجمالي الكمية الأصلية الحالية: ${item.quantity}", fontSize = 12.sp)

                OutlinedTextField(
                    value = splitQtyText,
                    onValueChange = { splitQtyText = it },
                    label = { Text("الكمية المقتطعة للدفعة الفرعية (أقل من ${item.quantity})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
