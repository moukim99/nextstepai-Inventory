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
import com.nextstepai.inventory.data.StockItemAttachment
import com.nextstepai.inventory.data.StockItemTestResult
import com.nextstepai.inventory.data.StockItemTracking
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.StockLocationType
import com.nextstepai.inventory.data.StockStatus
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_stock
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.card_stock_title
import nextstepai_inventory.shared.generated.resources.save
import nextstepai_inventory.shared.generated.resources.stock_items_count

/**
 * شاشة إدارة المخزون الفعلي ومواقع التخزين وأنواعها وسجلات التتبع وفحوص الجودة والمرفقات (StockScreen).
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
                onAddStockClick = { viewModel.setAddDialogOpen(true) },
                onAddLocationClick = { viewModel.setAddLocationDialogOpen(true) }
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

                // شريط اختيار موقع التخزين عبر LazyRow مع بادرات للمواقع الهيكلية
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
                        val badge = when {
                            loc.structural -> "🏗️"
                            loc.external -> "🌐"
                            else -> "📍"
                        }
                        FilterChip(
                            selected = uiState.selectedLocationId == loc.id,
                            onClick = { viewModel.filterByLocation(if (uiState.selectedLocationId == loc.id) null else loc.id) },
                            label = { Text("$badge ${loc.name}", fontWeight = FontWeight.Bold) },
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
                                onStocktakeClick = { viewModel.performStocktake(item.id) },
                                onHistoryClick = { viewModel.openTrackingHistory(item) },
                                onTestsClick = { viewModel.openTestResults(item) },
                                onAttachmentsClick = { viewModel.openAttachments(item) }
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

    if (uiState.isAddLocationDialogOpen) {
        AddStockLocationDialog(
            locations = uiState.locations,
            locationTypes = uiState.locationTypes,
            onDismiss = { viewModel.setAddLocationDialogOpen(false) },
            onAddTypeClick = { viewModel.setAddLocationTypeDialogOpen(true) },
            onConfirm = { name, desc, parentId, structural, external, locationTypeId, icon ->
                viewModel.addLocation(
                    name = name,
                    description = desc,
                    parentId = parentId,
                    structural = structural,
                    external = external,
                    locationTypeId = locationTypeId,
                    icon = icon
                )
            }
        )
    }

    if (uiState.isAddLocationTypeDialogOpen) {
        AddStockLocationTypeDialog(
            onDismiss = { viewModel.setAddLocationTypeDialogOpen(false) },
            onConfirm = { name, desc, icon, customIcon ->
                viewModel.addLocationType(
                    name = name,
                    description = desc,
                    icon = icon,
                    customIcon = customIcon
                )
            }
        )
    }

    if (uiState.selectedItemForHistory != null) {
        StockTrackingHistoryDialog(
            item = uiState.selectedItemForHistory!!,
            logs = uiState.trackingLogsForSelected,
            onDismiss = { viewModel.closeTrackingHistory() }
        )
    }

    if (uiState.selectedItemForTests != null) {
        StockTestResultsDialog(
            item = uiState.selectedItemForTests!!,
            results = uiState.testResultsForSelected,
            onDismiss = { viewModel.closeTestResults() },
            onAddTestClick = { viewModel.setAddTestResultDialogOpen(true) }
        )
    }

    if (uiState.isAddTestResultDialogOpen && uiState.selectedItemForTests != null) {
        AddStockTestResultDialog(
            item = uiState.selectedItemForTests!!,
            onDismiss = { viewModel.setAddTestResultDialogOpen(false) },
            onConfirm = { test, result, value, attach, notes ->
                viewModel.addTestResult(
                    stockItemId = uiState.selectedItemForTests!!.id,
                    test = test,
                    result = result,
                    value = value,
                    attachment = attach,
                    notes = notes
                )
            }
        )
    }

    if (uiState.selectedItemForAttachments != null) {
        StockAttachmentsDialog(
            item = uiState.selectedItemForAttachments!!,
            attachments = uiState.attachmentsForSelected,
            onDismiss = { viewModel.closeAttachments() },
            onAddAttachmentClick = { viewModel.setAddAttachmentDialogOpen(true) },
            onDeleteAttachment = { attId -> viewModel.deleteStockItemAttachment(attId, uiState.selectedItemForAttachments!!.id) }
        )
    }

    if (uiState.isAddAttachmentDialogOpen && uiState.selectedItemForAttachments != null) {
        AddStockItemAttachmentDialog(
            item = uiState.selectedItemForAttachments!!,
            onDismiss = { viewModel.setAddAttachmentDialogOpen(false) },
            onConfirm = { attachPath, link, comment ->
                viewModel.addStockItemAttachment(
                    stockItemId = uiState.selectedItemForAttachments!!.id,
                    attachmentPath = attachPath,
                    link = link,
                    comment = comment
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
 * الترويسة العلوية لشاشة إدارة المخزون والمواقع
 */
@Composable
private fun StockTopBar(
    onBackClick: () -> Unit,
    onAddStockClick: () -> Unit,
    onAddLocationClick: () -> Unit
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
                    text = "المخزون والمواقع (Stock)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFF0F172A)
                )
            }

            // اليسار: زر إضافة وحدة وزر إضافة موقع
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAddLocationClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("موقع", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                Button(
                    onClick = onAddStockClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
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
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

/**
 * بطاقة عرض الوحدة المخزنية المادية مع كامل أزرار الفحوص والجودة والمرفقات
 */
@Composable
private fun StockItemCard(
    stockItem: StockItem,
    part: Part?,
    location: StockLocation?,
    onSplitClick: () -> Unit,
    onStocktakeClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onTestsClick: () -> Unit,
    onAttachmentsClick: () -> Unit
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
                    onClick = onAttachmentsClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(13.dp))
                        Text("مرفقات", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                OutlinedButton(
                    onClick = onTestsClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.secondary)
                        Text("الجودة", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                OutlinedButton(
                    onClick = onHistoryClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(13.dp))
                        Text("السجل", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                OutlinedButton(
                    onClick = onStocktakeClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("جرد", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                if (stockItem.serial.isBlank() && stockItem.quantity > 1.0) {
                    OutlinedButton(
                        onClick = onSplitClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text("تجزئة", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun StockAttachmentsDialog(
    item: StockItem,
    attachments: List<StockItemAttachment>,
    onDismiss: () -> Unit,
    onAddAttachmentClick: () -> Unit,
    onDeleteAttachment: (id: Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("المستندات والمرفقات (StockItemAttachment)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAddAttachmentClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("رفع / إضافة مرفق", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("الوحدة المخزنية #${item.id} (الرقم التسلسلي: ${item.serial.ifBlank { "غير معرّف" }})", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

                if (attachments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد مستندات أو شهادات مرفقة لهذه القطعة حتى الآن", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(attachments, key = { "att-${it.id}" }) { att ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(att.comment, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        val source = att.attachment ?: att.link ?: "-"
                                        Text("المصدر: $source", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        Text("التاريخ: ${att.uploadDate}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    IconButton(onClick = { onDeleteAttachment(att.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
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
private fun AddStockItemAttachmentDialog(
    item: StockItem,
    onDismiss: () -> Unit,
    onConfirm: (
        attachmentPath: String?,
        link: String?,
        comment: String
    ) -> Unit
) {
    var attachmentPath by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("إضافة مرفق/مستند جديد", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val path = attachmentPath.trim().ifBlank { null }
                    val url = link.trim().ifBlank { null }
                    if (path != null || url != null) {
                        onConfirm(path, url, comment.trim())
                    }
                },
                enabled = attachmentPath.isNotBlank() || link.isNotBlank(),
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
                    value = attachmentPath,
                    onValueChange = { attachmentPath = it },
                    label = { Text("مسار الملف المرفوع محلياً (File Path)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("أو أدخل رابط ويب خارجي للوثيقة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("الرابط الإلكتروني الخارجي (URL Link)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("وصف/تعليق الوثيقة (مثل: شهادة منشأ، فاتورة)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    )
}

@Composable
private fun StockTestResultsDialog(
    item: StockItem,
    results: List<StockItemTestResult>,
    onDismiss: () -> Unit,
    onAddTestClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("نتائج فحوص الجودة والقياسات (QA/QC Tests)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAddTestClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("إضافة فحص فني", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("الوحدة المخزنية #${item.id} (الرقم التسلسلي: ${item.serial.ifBlank { "غير معرّف" }})", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

                if (results.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد فحوصات فنية مسجلة لهذه القطعة حتى الآن", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(results, key = { "test-${it.id}" }) { res ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = if (res.result) Color(0xFFECFDF5) else Color(0xFFFEF2F2)),
                                border = BorderStroke(1.dp, if (res.result) Color(0xFFA7F3D0) else Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(res.test, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Surface(
                                            color = if (res.result) Color(0xFF059669) else Color(0xFFDC2626),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = if (res.result) "✅ ناجح (Pass)" else "❌ راسب (Fail)",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("القيمة المقاسة: ${res.value}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    if (res.notes.isNotBlank()) {
                                        Text("ملاحظات: ${res.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Text("التاريخ: ${res.date}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
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
private fun AddStockTestResultDialog(
    item: StockItem,
    onDismiss: () -> Unit,
    onConfirm: (
        test: String,
        result: Boolean,
        value: String,
        attachment: String,
        notes: String
    ) -> Unit
) {
    var testName by remember { mutableStateOf("فحص الجهد والأبعاد الفنية") }
    var measuredValue by remember { mutableStateOf("5.00 V - OK") }
    var isPass by remember { mutableStateOf(true) }
    var attachment by remember { mutableStateOf("") }
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
                Text("تسجيل نتيجة فحص جودة (QC Test)", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (testName.isNotBlank()) {
                        onConfirm(testName.trim(), isPass, measuredValue.trim(), attachment.trim(), notes.trim())
                    }
                },
                enabled = testName.isNotBlank(),
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
                    value = testName,
                    onValueChange = { testName = it },
                    label = { Text("اسم الاختبار الفني (مثل: فحص العزل الكهربائي)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = measuredValue,
                    onValueChange = { measuredValue = it },
                    label = { Text("القيمة المقاسة الفعلية (Value)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("النتيجة الإجمالية للفحص:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    FilterChip(
                        selected = isPass,
                        onClick = { isPass = true },
                        label = { Text("✅ ناجح (Pass)") },
                        shape = RoundedCornerShape(50)
                    )
                    FilterChip(
                        selected = !isPass,
                        onClick = { isPass = false },
                        label = { Text("❌ راسب (Fail)") },
                        shape = RoundedCornerShape(50)
                    )
                }

                OutlinedTextField(
                    value = attachment,
                    onValueChange = { attachment = it },
                    label = { Text("مسار تقرير أو ملف التوثيق المرفق (PDF/Image)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات المفتش الفني") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    )
}

@Composable
private fun StockTrackingHistoryDialog(
    item: StockItem,
    logs: List<StockItemTracking>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("سجل التتبع والحركات (StockItemTracking)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق", fontWeight = FontWeight.Bold) }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("الوحدة المخزنية #${item.id} (الرقم التسلسلي: ${item.serial.ifBlank { "غير معرّف" }})", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("لا يوجد سجل حركات تاريخية لهذه القطعة", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(logs, key = { "track-${it.id}" }) { log ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(log.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(log.date, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    if (log.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("ملاحظات: ${log.notes}", fontSize = 11.sp)
                                    }
                                    if (log.deltas != "{}") {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("التغيرات (Deltas): ${log.deltas}", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
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
private fun AddStockLocationTypeDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        description: String,
        icon: String,
        customIcon: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("warehouse") }
    var customIcon by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تعريف نوع موقع جديد (StockLocationType)", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), description.trim(), icon, customIcon)
                    }
                },
                enabled = name.isNotBlank(),
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم نوع/نمط الموقع (مثل: رف، غرفة نظيفة)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف استخدام هذا النوع") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("اسم الأيقونة المعيارية (Icon)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = customIcon,
                    onValueChange = { customIcon = it },
                    label = { Text("مسار الأيقونة المخصصة (Custom Icon)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    )
}

@Composable
private fun AddStockLocationDialog(
    locations: List<StockLocation>,
    locationTypes: List<StockLocationType>,
    onDismiss: () -> Unit,
    onAddTypeClick: () -> Unit,
    onConfirm: (
        name: String,
        description: String,
        parentId: Long?,
        structural: Boolean,
        external: Boolean,
        locationTypeId: Long?,
        icon: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<Long?>(null) }
    var selectedTypeId by remember { mutableStateOf<Long?>(locationTypes.firstOrNull()?.id) }
    var structural by remember { mutableStateOf(false) }
    var external by remember { mutableStateOf(false) }
    var icon by remember { mutableStateOf("warehouse") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("إضافة موقع تخزيني جديد (StockLocation)", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), description.trim(), selectedParentId, structural, external, selectedTypeId, icon)
                    }
                },
                enabled = name.isNotBlank(),
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الموقع التخزيني (مثل: الرف B3، مستودع أ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف التفصيلي للموقع") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("اختر نمط/نوع الموقع (StockLocationType):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onAddTypeClick) { Text("+ نوع جديد", fontSize = 11.sp) }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item(key = "type-none") {
                        FilterChip(
                            selected = selectedTypeId == null,
                            onClick = { selectedTypeId = null },
                            label = { Text("غير محدد", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    items(locationTypes, key = { "type-${it.id}" }) { t ->
                        FilterChip(
                            selected = selectedTypeId == t.id,
                            onClick = { selectedTypeId = t.id },
                            label = { Text("🏷️ ${t.name}", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Text("اختر الموقع الحاوي (الأب) ضمن الهرمية الشجرية:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item(key = "parent-root") {
                        FilterChip(
                            selected = selectedParentId == null,
                            onClick = { selectedParentId = null },
                            label = { Text("موقع رئيسي (Root)", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    items(locations, key = { "loc-parent-${it.id}" }) { loc ->
                        FilterChip(
                            selected = selectedParentId == loc.id,
                            onClick = { selectedParentId = loc.id },
                            label = { Text("${if (loc.structural) "🏗️ " else "📍 "}${loc.name}", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = structural, onCheckedChange = { structural = it })
                    Text("موقع هيكلي لتجميع العقد (يمنع التخزين المباشر)", fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = external, onCheckedChange = { external = it })
                    Text("موقع خارجي (مملوك للعميل/المورد/سيارة نقل)", fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("رمز/أيقونة الموقع (Icon Name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    )
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
    var selectedLocationId by remember { mutableStateOf<Long?>(locations.firstOrNull { !it.structural }?.id ?: locations.firstOrNull()?.id) }
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

                Text("اختر موقع التخزين المباشر:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(locations, key = { "stock-loc-${it.id}" }) { loc ->
                        FilterChip(
                            selected = selectedLocationId == loc.id,
                            onClick = { selectedLocationId = loc.id },
                            enabled = !loc.structural,
                            label = { Text("${if (loc.structural) "🏗️ (هيكلي)" else "📍"} ${loc.name}", fontWeight = FontWeight.Bold) },
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
