package com.nextstepai.inventory.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import com.nextstepai.inventory.ui.components.LocationBarcodeScannerBottomSheet
import com.nextstepai.inventory.ui.components.DynamicBreadcrumbAddressField
import com.nextstepai.inventory.ui.components.ShakeController
import com.nextstepai.inventory.data.PendingAttachment
import com.nextstepai.inventory.util.BarcodePayloadHelper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nextstepai.inventory.ui.components.CurrencySelectionBottomSheet
import com.nextstepai.inventory.ui.components.CurrencySelectorField
import com.nextstepai.inventory.ui.components.PrintableLabelBottomSheet
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue
import kotlin.time.Clock
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.vector.ImageVector
import com.nextstepai.inventory.data.AppUser
import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartCategory
import com.nextstepai.inventory.data.PurchaseOrder
import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.SupplierPart
import com.nextstepai.inventory.data.StockItemAttachment
import com.nextstepai.inventory.data.StockItemTestResult
import com.nextstepai.inventory.data.StockItemTracking
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.StockLocationType
import com.nextstepai.inventory.data.capacityUnit
import com.nextstepai.inventory.data.contactPerson
import com.nextstepai.inventory.data.contactPhone
import com.nextstepai.inventory.data.withContactInfo
import com.nextstepai.inventory.data.isPrimary
import com.nextstepai.inventory.data.withPrimary
import com.nextstepai.inventory.data.IntermediateNodeSpec
import com.nextstepai.inventory.data.effectiveCapacity
import com.nextstepai.inventory.data.calculateOccupancyPercentage
import com.nextstepai.inventory.data.calculatePhysicalOccupancy
import com.nextstepai.inventory.data.getOccupancySummary
import com.nextstepai.inventory.data.getFullHierarchyPath
import com.nextstepai.inventory.data.StockStatus
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_stock
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.card_stock_title
import nextstepai_inventory.shared.generated.resources.save
import nextstepai_inventory.shared.generated.resources.stock_items_count

/**
 * بطاقة عرض الوحدة المخزنية المادية مع كامل أزرار الفحوص والجودة والمرفقات
 */
@Composable
internal fun StockItemCard(
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Badges (Left in RTL) & Icon Box (Right in RTL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (stockItem.reviewNeeded) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Text(
                                text = "⚠️ إعادة مراجعة",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    val (statusBg, statusBorder, statusColor) = when (stockItem.status) {
                        StockStatus.OK -> Triple(Color(0xFFECFDF5), Color(0xFFA7F3D0), Color(0xFF047857))
                        StockStatus.QUARANTINE -> Triple(Color(0xFFFEF3C7), Color(0xFFFDE68A), Color(0xFFD97706))
                        else -> Triple(Color(0xFFFEF2F2), Color(0xFFFCA5A5), Color(0xFFDC2626))
                    }

                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, statusBorder)
                    ) {
                        Text(
                            text = stockItem.status.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            ),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Location Icon Box
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEF2FF))
                        .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Part Name
            Text(
                text = part?.name ?: "قطعة #${stockItem.partId}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity & Location Surface
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "الكمية المادية المتوفرة:",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "${stockItem.quantity} ${part?.units ?: "pcs"}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF4F46E5)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "موقع التخزين:",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "📍 ${location?.name ?: "غير محدد"}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }

            // Batch, Packaging & Serial Info
            if (stockItem.serial.isNotBlank() || stockItem.batch.isNotBlank() || stockItem.packaging.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (stockItem.packaging.isNotBlank()) {
                        Text(
                            text = "التغليف: ${stockItem.packaging}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                    if (stockItem.batch.isNotBlank()) {
                        Text(
                            text = "Batch: ${stockItem.batch}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF334155)
                        )
                    }
                    if (stockItem.serial.isNotBlank()) {
                        Text(
                            text = "SN: ${stockItem.serial}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFFF1F5F9)
            )

            // Action Buttons Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onSplitClick,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                        Text("تجزئة ✂️", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = Color(0xFF334155))
                    }
                }

                Surface(
                    onClick = onStocktakeClick,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEEF2FF),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                        Text("جرد 📌", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = Color(0xFF4F46E5))
                    }
                }

                Surface(
                    onClick = onHistoryClick,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                        Text("السجل 🕒", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = Color(0xFF334155))
                    }
                }

                Surface(
                    onClick = onTestsClick,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                        Text("الجودة 🔬", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = Color(0xFF334155))
                    }
                }

                Surface(
                    onClick = onAttachmentsClick,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                        Text("مرفقات 📎", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = Color(0xFF334155))
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SplitStockDialog(
    item: StockItem,
    onDismiss: () -> Unit,
    onConfirm: (splitQty: Double) -> Unit
) {
    var splitQtyText by remember { mutableStateOf("1.0") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تجزئة الكمية المخزنية (Split Stock)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            Text("إجمالي الكمية الأصلية الحالية: ${item.quantity}", fontSize = 12.sp)

            OutlinedTextField(
                value = splitQtyText,
                onValueChange = { splitQtyText = it },
                label = { Text("الكمية المقتطعة للدفعة الفرعية (أقل من ${item.quantity})") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
                Spacer(modifier = Modifier.width(8.dp))
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
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StockFilterBottomSheet(
    uiState: StockUiState,
    onDismiss: () -> Unit,
    onApply: (selectedLocationIds: Set<Long>) -> Unit,
    onClear: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedLocationIds by remember { mutableStateOf(uiState.selectedLocationIds) }
    var selectedLocationForLabel by remember { mutableStateOf<StockLocation?>(null) }
    var inStockOnly by remember { mutableStateOf(false) }
    var lowStockOnly by remember { mutableStateOf(false) }
    var outOfStockOnly by remember { mutableStateOf(false) }

    val filteredLocations = remember(uiState.locations, searchQuery) {
        val query = searchQuery.normalizeArabic()
        if (query.isBlank()) {
            uiState.locations
        } else {
            uiState.locations.filter { loc ->
                loc.name.normalizeArabic().contains(query) ||
                loc.description.normalizeArabic().contains(query) ||
                loc.locationType.normalizeArabic().contains(query)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            // 1. Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            "تصفية المخزون والمواقع",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "تحديد المواد المعروضة حسب المخزن وحالة التخزين",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = {
                        selectedLocationIds = emptySet()
                        inStockOnly = false
                        lowStockOnly = false
                        outOfStockOnly = false
                        onClear()
                    }) {
                        Text(
                            "مسح الكل",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = Color(0xFF4F46E5)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // 2. Scrollable Body Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Search Input by Location / Warehouse / Shelf
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن اسم المستودع، الموقع، الرف...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // 2. شريط التبويبات المزدوجة
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { selectedTab = 0 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 0) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 0) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "📍 الموقع الفيزيائي (${uiState.locations.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (selectedTab == 0) Color(0xFF4F46E5) else Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            onClick = { selectedTab = 1 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 1) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 1) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚙️ حالة البضاعة والقطع",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (selectedTab == 1) Color(0xFF4F46E5) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                if (selectedTab == 0) {
                    // Section: Locations Selector with Tree Hierarchy
                    val sortedTreeLocations = remember(filteredLocations) {
                        sortLocationsHierarchically(filteredLocations)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        sortedTreeLocations.forEach { loc ->
                            val isChecked = selectedLocationIds.contains(loc.id)
                            val indentPadding = (loc.level.coerceIn(0, 5) * 20).dp
                            val count = remember(loc.id, uiState.allStockItems) {
                                uiState.allStockItems.count { it.locationId == loc.id }
                            }
                            val itemsInLoc = remember(loc.id, uiState.allStockItems) {
                                uiState.allStockItems.filter { it.locationId == loc.id }
                            }
                            val occupancyPct = loc.calculatePhysicalOccupancy(itemsInLoc, uiState.parts)
                            val (percentageText, detailPreviewText) = loc.getOccupancySummary(itemsInLoc, uiState.parts)
                            val barColor = when {
                                occupancyPct >= 90.0 -> Color(0xFFEF4444)
                                occupancyPct >= 70.0 -> Color(0xFFF97316)
                                else -> Color(0xFF10B981)
                            }
                            val badgeEmoji = when {
                                loc.structural -> "🏗️"
                                loc.external -> "🌐"
                                else -> "📍"
                            }

                            Surface(
                                onClick = {
                                    selectedLocationIds = if (isChecked) selectedLocationIds - loc.id else selectedLocationIds + loc.id
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChecked) Color(0xFFEEF2FF).copy(alpha = 0.6f) else Color.White,
                                border = BorderStroke(1.dp, if (isChecked) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = indentPadding)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (loc.level > 0) {
                                            Text(
                                                text = "↳",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                ),
                                                color = Color(0xFF4F46E5)
                                            )
                                        }
                                        Text(badgeEmoji, fontSize = 18.sp)
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    loc.name,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.5.sp
                                                    ),
                                                    color = Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFEEF2FF)
                                                ) {
                                                    Text(
                                                        text = loc.locationType.uppercase(),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                        color = Color(0xFF4338CA),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            val parentObj = uiState.locations.find { it.id == loc.parentId }
                                            val pathText = if (parentObj != null) "📍 ${parentObj.getFullHierarchyPath(uiState.locations)}" else loc.description.ifBlank { "موقع تخزين رئيسي" }
                                            Text(
                                                pathText,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!loc.structural) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    LinearProgressIndicator(
                                                        progress = { (occupancyPct / 100.0).toFloat() },
                                                        modifier = Modifier
                                                            .width(70.dp)
                                                            .height(5.dp)
                                                            .clip(CircleShape),
                                                        color = barColor,
                                                        trackColor = Color(0xFFE2E8F0),
                                                        gapSize = 0.dp,
                                                        drawStopIndicator = {}
                                                    )
                                                    Text(
                                                        "الإشغال: $percentageText",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = barColor
                                                    )
                                                }
                                                Text(
                                                    detailPreviewText,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = if (count > 0) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, if (count > 0) Color(0xFFA7F3D0) else Color(0xFFE2E8F0))
                                        ) {
                                            Text(
                                                "$count قطعة",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = if (count > 0) Color(0xFF047857) else Color(0xFF64748B),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { selectedLocationForLabel = loc },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Print,
                                                contentDescription = "طباعة ملصق الرف",
                                                tint = Color(0xFF4F46E5),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        if (isChecked) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF4F46E5),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedLocationForLabel != null) {
                        val parentPath = remember(selectedLocationForLabel, uiState.locations) {
                            formatStockLocationFullPath(uiState.locations, selectedLocationForLabel!!.parentId, separator = " > ")
                        }
                        PrintableLabelBottomSheet(
                            location = selectedLocationForLabel!!,
                            parentPath = parentPath,
                            onDismiss = { selectedLocationForLabel = null }
                        )
                    }
                } else {
                    // تبويب حالة البضاعة المخزنية
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { inStockOnly = !inStockOnly }
                            ) {
                                Checkbox(
                                    checked = inStockOnly,
                                    onCheckedChange = { inStockOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Text(
                                    "متوفر بالكامل (In Stock)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { lowStockOnly = !lowStockOnly }
                            ) {
                                Checkbox(
                                    checked = lowStockOnly,
                                    onCheckedChange = { lowStockOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Text(
                                    "منخفض المخزون (Low Stock)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { outOfStockOnly = !outOfStockOnly }
                            ) {
                                Checkbox(
                                    checked = outOfStockOnly,
                                    onCheckedChange = { outOfStockOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Text(
                                    "نفد من المخزون (Out of Stock)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Sticky Actions Footer Bar
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                val totalMatchingCount = remember(selectedLocationIds, uiState.allStockItems) {
                    if (selectedLocationIds.isEmpty()) {
                        uiState.allStockItems.size
                    } else {
                        uiState.allStockItems.count { it.locationId != null && selectedLocationIds.contains(it.locationId) }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onApply(selectedLocationIds)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                "تطبيق التصفية (عرض $totalMatchingCount قطعة)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "إلغاء",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}


