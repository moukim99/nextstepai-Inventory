package com.nextstepai.inventory.ui

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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.StockStatus
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_stock
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.card_stock_title
import nextstepai_inventory.shared.generated.resources.save
import nextstepai_inventory.shared.generated.resources.stock_items_count

/**
 * شاشة إدارة المخزون الفعلي (StockItem Management Screen).
 * مطابقة تماماً للتصميم الهيكلي الموحد مع الترويسة العلوية، البحث والباركود المدمج، والفلترة بحسب مواقع التخزين،
 * بالإضافة لمتابعة كافة حقول StockItem الـ 24 المعتمدة (جرد، سعر الشراء، الصلاحية، المراجعة الفنية...).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    viewModel: StockViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            StockTopBar(
                onBackClick = onBackClick,
                onAddClick = { viewModel.setAddDialogOpen(true) }
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
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "البحث باسم القطعة، الرقم التسلسلي، أو موقع التخزين...",
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

                // شريط اختيار موقع التخزين عبر LazyRow
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item(key = "location-all") {
                        FilterChip(
                            selected = uiState.selectedLocationId == null,
                            onClick = { viewModel.filterByLocation(null) },
                            label = { Text("كافة المواقع", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(50)
                        )
                    }
                    items(uiState.locations, key = { "location-${it.id}" }) { loc ->
                        FilterChip(
                            selected = uiState.selectedLocationId == loc.id,
                            onClick = { viewModel.filterByLocation(if (uiState.selectedLocationId == loc.id) null else loc.id) },
                            label = { Text("📍 ${loc.name}", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(50)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = "⚠️ ${uiState.errorMessage}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp
                        )
                    }
                }

                val filteredStock = uiState.stockItems.filter { item ->
                    val part = uiState.parts.find { it.id == item.partId }
                    searchQuery.isBlank() || (part?.name?.contains(searchQuery, ignoreCase = true) == true) || item.serial.contains(searchQuery, ignoreCase = true) || item.batch.contains(searchQuery, ignoreCase = true)
                }

                if (filteredStock.isEmpty()) {
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pluralStringResource(Res.plurals.stock_items_count, filteredStock.size, filteredStock.size),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredStock, key = { "stock-${it.id}" }) { item ->
                            val part = uiState.parts.find { it.id == item.partId }
                            val loc = uiState.locations.find { it.id == item.locationId }
                            StockItemCard(
                                stockItem = item,
                                part = part,
                                location = loc,
                                onSplitClick = { viewModel.setSelectedItemForSplit(item) },
                                onStocktakeClick = { viewModel.performStocktake(item.id) }
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
            onConfirm = { partId, locId, qty, serial, batch, pack, status, price, currency, expiry, review, deleteOnDeplete, link, notes ->
                viewModel.addStockItem(
                    partId = partId,
                    locationId = locId,
                    quantity = qty,
                    serial = serial,
                    batch = batch,
                    packaging = pack,
                    status = status,
                    purchasePrice = price,
                    purchasePriceCurrency = currency,
                    expiryDate = expiry,
                    reviewNeeded = review,
                    deleteOnDeplete = deleteOnDeplete,
                    link = link,
                    notes = notes
                )
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
 * الترويسة العلوية لشاشة إدارة المخزون
 */
@Composable
private fun StockTopBar(
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
                    text = "إدارة المخزون الفعلي (StockItem)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFF0F172A)
                )
            }

            // اليسار: زر "+ إضافة وحدة مخزنية"
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.add_new_stock),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "إضافة وحدة",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

/**
 * بطاقة عرض الوحدة المخزنية المادية مع كامل تفاصيل الـ 24 حقل المعتمدة
 */
@Composable
private fun StockItemCard(
    stockItem: StockItem,
    part: Part?,
    location: StockLocation?,
    onSplitClick: () -> Unit,
    onStocktakeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = part?.name ?: "قطعة #${stockItem.partId}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (stockItem.reviewNeeded) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Text(
                                text = "⚠️ إعادة مراجعة",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        color = when (stockItem.status) {
                            StockStatus.OK -> Color(0xFFECFDF5)
                            StockStatus.QUARANTINE -> Color(0xFFFFFBEB)
                            else -> MaterialTheme.colorScheme.errorContainer
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, when (stockItem.status) {
                            StockStatus.OK -> Color(0xFFA7F3D0)
                            else -> Color.Transparent
                        })
                    ) {
                        Text(
                            text = stockItem.status.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (stockItem.status) {
                                StockStatus.OK -> Color(0xFF059669)
                                StockStatus.QUARANTINE -> Color(0xFFB45309)
                                else -> MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                            text = "الكمية المادية المتوفرة:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${stockItem.quantity} ${part?.units ?: "pcs"}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "موقع التخزين:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "📍 ${location?.name ?: "غير محدد"}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (stockItem.serial.isNotBlank() || stockItem.batch.isNotBlank() || stockItem.packaging.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (stockItem.serial.isNotBlank()) {
                        Text(
                            text = "SN: ${stockItem.serial}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (stockItem.batch.isNotBlank()) {
                        Text(
                            text = "Batch: ${stockItem.batch}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "التغليف: ${stockItem.packaging}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (stockItem.purchasePrice > 0.0 || stockItem.expiryDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (stockItem.purchasePrice > 0.0) {
                        Text(
                            text = "سعر الشراء: ${stockItem.purchasePrice} ${stockItem.purchasePriceCurrency}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF047857)
                        )
                    }
                    if (stockItem.expiryDate.isNotBlank()) {
                        Text(
                            text = "الصلاحية: ${stockItem.expiryDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            if (stockItem.stocktakeDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📋 آخر جرد فعلي: ${stockItem.stocktakeDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onStocktakeClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("جرد (Count)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                if (stockItem.serial.isBlank() && stockItem.quantity > 1.0) {
                    OutlinedButton(
                        onClick = onSplitClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text("تجزئة (Split)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
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
        purchasePrice: Double,
        purchasePriceCurrency: String,
        expiryDate: String,
        reviewNeeded: Boolean,
        deleteOnDeplete: Boolean,
        link: String,
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
    var purchasePriceText by remember { mutableStateOf("0.0") }
    var currency by remember { mutableStateOf("USD") }
    var expiryDate by remember { mutableStateOf("") }
    var reviewNeeded by remember { mutableStateOf(false) }
    var deleteOnDeplete by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }
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
                Text(stringResource(Res.string.add_new_stock), fontWeight = FontWeight.Bold)
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
                    val price = purchasePriceText.toDoubleOrNull() ?: 0.0
                    if (pId != null && qty > 0.0) {
                        onConfirm(
                            pId, selectedLocationId, qty, serial, batch, packaging,
                            selectedStatus, price, currency, expiryDate,
                            reviewNeeded, deleteOnDeplete, link, notes
                        )
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
                Text("اختر القطعة المراد استلامها:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(parts, key = { "stock-part-${it.id}" }) { p ->
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
                    label = { Text("الكمية المادية") },
                    singleLine = true,
                    enabled = serial.isBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = serial,
                    onValueChange = {
                        serial = it
                        if (it.isNotBlank()) quantityText = "1.0"
                    },
                    label = { Text("الرقم التسلسلي الفريد (Serial Number)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = batch,
                    onValueChange = { batch = it },
                    label = { Text("رقم التشغيلة/الدفعة (Batch Code)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = packaging,
                    onValueChange = { packaging = it },
                    label = { Text("نوع التغليف (Box / Reel / Tray)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = purchasePriceText,
                        onValueChange = { purchasePriceText = it },
                        label = { Text("سعر الشراء") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("العملة") },
                        singleLine = true,
                        modifier = Modifier.width(90.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("تاريخ انتهاء الصلاحية (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = reviewNeeded, onCheckedChange = { reviewNeeded = it })
                    Text("بحاجة لإعادة مراجعة وتقييم (Review Needed)", fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = deleteOnDeplete, onCheckedChange = { deleteOnDeplete = it })
                    Text("حذف السجل آلياً عند نفاد الكمية (= 0)", fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("رابط خارجي للتوثيق (Link URL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات الفحص والاستلام") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تجزئة الكمية المخزنية (Split Stock)", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = splitQtyText.toDoubleOrNull() ?: 0.0
                    if (qty > 0.0 && qty < item.quantity) {
                        onConfirm(qty)
                    }
                },
                enabled = (splitQtyText.toDoubleOrNull() ?: 0.0) in 0.1..<item.quantity,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("تأكيد التجزئة", fontWeight = FontWeight.Bold)
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    )
}
