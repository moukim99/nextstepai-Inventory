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
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.StockStatus

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
                title = { Text("إدارة المخزون الفعلي (StockItem)") },
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
                        Text("+ استلام مخزون")
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

            // قائمة السجلات المخزنية مع المفاتيح المستقرة key()
            if (uiState.stockItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد وحدات مخزنية متوفرة في هذا الموقع.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.stockItems, key = { "stock-${it.id}" }) { item ->
                        val part = uiState.parts.find { it.id == item.partId }
                        val loc = uiState.locations.find { it.id == item.locationId }
                        StockItemCard(
                            item = item,
                            partName = part?.name ?: "قطعة #${item.partId}",
                            partUnits = part?.units ?: "pcs",
                            locationName = loc?.name ?: "غير محدد",
                            onSplitClick = { viewModel.setSelectedItemForSplit(item) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.isAddStockDialogOpen) {
        AddStockDialog(
            parts = uiState.parts,
            locations = uiState.locations,
            onDismiss = { viewModel.setAddDialogOpen(false) },
            onConfirm = { partId, locId, qty, serial, batch, pkg, status, notes ->
                viewModel.addStockItem(partId, locId, qty, serial, batch, pkg, status, notes)
            }
        )
    }

    if (uiState.selectedItemForSplit != null) {
        SplitStockDialog(
            parentItem = uiState.selectedItemForSplit!!,
            onDismiss = { viewModel.setSelectedItemForSplit(null) },
            onConfirm = { splitQty ->
                viewModel.splitStockItem(uiState.selectedItemForSplit!!.id, splitQty)
            }
        )
    }
}

@Composable
private fun StockItemCard(
    item: StockItem,
    partName: String,
    partUnits: String,
    locationName: String,
    onSplitClick: () -> Unit
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
                    text = partName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(status = item.status)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الموقع: $locationName",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "الكمية المتاحة: ${item.availableQuantity} $partUnits (من ${item.quantity})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (item.serial.isNotBlank() || item.batch.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (item.serial.isNotBlank()) {
                        TagBadge("الرقم التسلسلي: ${item.serial}", MaterialTheme.colorScheme.primaryContainer)
                    }
                    if (item.batch.isNotBlank()) {
                        TagBadge("الشحنة: ${item.batch}", MaterialTheme.colorScheme.secondaryContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "التغليف: ${item.packaging}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.quantity > 1.0) {
                    OutlinedButton(
                        onClick = onSplitClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("تجزئة الكمية (Split)", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: StockStatus) {
    val (color, label) = when (status) {
        StockStatus.OK -> MaterialTheme.colorScheme.primaryContainer to "سليم (OK)"
        StockStatus.DAMAGED -> MaterialTheme.colorScheme.errorContainer to "تالف"
        StockStatus.DESTROYED -> MaterialTheme.colorScheme.errorContainer to "مدمر"
        StockStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer to "مرفوض"
        StockStatus.QUARANTINE -> MaterialTheme.colorScheme.tertiaryContainer to "قيد الفحص"
        StockStatus.EXPIRED -> MaterialTheme.colorScheme.errorContainer to "منتهي الصلاحية"
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
private fun TagBadge(text: String, color: Color) {
    Surface(color = color, shape = RoundedCornerShape(4.dp)) {
        Text(text = text, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun AddStockDialog(
    parts: List<Part>,
    locations: List<StockLocation>,
    onDismiss: () -> Unit,
    onConfirm: (
        partId: Long,
        locationId: Long?,
        qty: Double,
        serial: String,
        batch: String,
        pkg: String,
        status: StockStatus,
        notes: String
    ) -> Unit
) {
    var selectedPartId by remember { mutableStateOf<Long?>(parts.firstOrNull()?.id) }
    var selectedLocId by remember { mutableStateOf<Long?>(locations.firstOrNull()?.id) }
    var quantityText by remember { mutableStateOf("1.0") }
    var serial by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("") }
    var packaging by remember { mutableStateOf("Box") }
    var status by remember { mutableStateOf(StockStatus.OK) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("استلام مخزون جديد (StockItem)", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedPartId != null) {
                        onConfirm(
                            selectedPartId!!,
                            selectedLocId,
                            quantityText.toDoubleOrNull() ?: 1.0,
                            serial,
                            batch,
                            packaging,
                            status,
                            notes
                        )
                    }
                },
                enabled = selectedPartId != null
            ) {
                Text("حفظ الاستلام")
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
                Text("اختر القطعة المرجعية (part):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(parts, key = { "add-part-${it.id}" }) { p ->
                        FilterChip(
                            selected = selectedPartId == p.id,
                            onClick = { selectedPartPartId(p.id, p.trackable) { selectedPartId = it } },
                            label = { Text(p.name) }
                        )
                    }
                }

                Text("اختر موقع التخزين (location):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(locations, key = { "add-loc-${it.id}" }) { loc ->
                        FilterChip(
                            selected = selectedLocId == loc.id,
                            onClick = { selectedLocId = loc.id },
                            label = { Text(loc.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("الكمية (quantity) - تُجبر لـ 1 إذا أُدخل الرقم التسلسلي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = serial,
                    onValueChange = { serial = it },
                    label = { Text("الرقم التسلسلي (serial - إلزامي إذا كانت القطعة trackable)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = batch,
                    onValueChange = { batch = it },
                    label = { Text("رقم الشحنة/التشغيلة (batch)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = packaging,
                    onValueChange = { packaging = it },
                    label = { Text("نوع التغليف (packaging e.g. Reel, Box)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

private fun selectedPartPartId(id: Long, isTrackable: Boolean, onSelect: (Long) -> Unit) {
    onSelect(id)
}

@Composable
private fun SplitStockDialog(
    parentItem: StockItem,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var splitQtyText by remember { mutableStateOf("1.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تجزئة الكمية المخزنية (Split Stock)", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    val qty = splitQtyText.toDoubleOrNull() ?: 0.0
                    if (qty > 0 && qty < parentItem.availableQuantity) {
                        onConfirm(qty)
                    }
                }
            ) {
                Text("تأكيد التجزئة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الكمية المتاحة حالياً في السجل الأصلي: ${parentItem.availableQuantity}")
                OutlinedTextField(
                    value = splitQtyText,
                    onValueChange = { splitQtyText = it },
                    label = { Text("الكمية المراد اقتطاعها وتخصيصها لسجل جديد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
