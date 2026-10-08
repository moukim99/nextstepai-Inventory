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

data class VisualIconOption(
    val key: String,
    val nameAr: String,
    val iconVector: ImageVector
)

private val availableLocationIcons = listOf(
    VisualIconOption("warehouse", "مستودع / مخزن", Icons.Default.Warehouse),
    VisualIconOption("shelves", "رفوف / أرفف", Icons.Default.TableRows),
    VisualIconOption("inventory_2", "صندوق / حاوية", Icons.Default.Inventory2),
    VisualIconOption("grid_view", "منطقة / قسم", Icons.Default.GridView),
    VisualIconOption("door", "ممر / مدخل", Icons.Default.ViewWeek),
    VisualIconOption("archive", "أرشيف / تخزين", Icons.Default.Archive),
    VisualIconOption("store", "متجر / معرض", Icons.Default.Store),
    VisualIconOption("local_shipping", "شحنة / ترانزيت", Icons.Default.LocalShipping),
    VisualIconOption("place", "موقع / نقطة", Icons.Default.Place),
    VisualIconOption("home_work", "مقَر / فرع", Icons.Default.HomeWork),
    VisualIconOption("widgets", "وحدة / عُقدة", Icons.Default.Widgets),
    VisualIconOption("corporate_fare", "منشأة / مجمع", Icons.Default.CorporateFare)
)


internal fun getDefaultIconForLocationType(type: String): String {
    return when (type.uppercase()) {
        "WAREHOUSE" -> "warehouse"
        "ZONE" -> "grid_view"
        "AISLE" -> "door"
        "SHELF" -> "shelves"
        "BIN" -> "inventory_2"
        else -> "warehouse"
    }
}


internal fun getStockLocationIconVector(iconKey: String, locationType: String = ""): ImageVector {
    val normType = locationType.uppercase().trim()
    val normIcon = iconKey.lowercase().trim()

    return when {
        normType == "SITE" -> Icons.Default.Place
        normType == "WAREHOUSE" -> Icons.Default.Warehouse
        normType == "ZONE" || normType == "AREA" || normType == "LINE" -> Icons.Default.GridView
        normType == "AISLE" -> Icons.Default.ViewWeek
        normType == "SHELF" || normType == "RACK" -> Icons.Default.TableRows
        normType == "BIN" || normType == "DRAWER" -> Icons.Default.Inbox

        normIcon.contains("warehouse") || normIcon.contains("building") || normIcon.contains("store") -> Icons.Default.Warehouse
        normIcon.contains("grid_view") || normIcon.contains("zone") || normIcon.contains("category") -> Icons.Default.GridView
        normIcon.contains("door") || normIcon.contains("aisle") || normIcon.contains("view_week") -> Icons.Default.ViewWeek
        normIcon.contains("shelves") || normIcon.contains("straighten") || normIcon.contains("shelf") || normIcon.contains("table_rows") -> Icons.Default.TableRows
        normIcon.contains("inventory_2") || normIcon.contains("bin") || normIcon.contains("box") || normIcon.contains("archive") || normIcon.contains("inbox") -> Icons.Default.Inbox
        normIcon.contains("place") || normIcon.contains("location_on") || normIcon.contains("site") -> Icons.Default.Place
        normIcon.contains("local_shipping") || normIcon.contains("transit") -> Icons.Default.LocalShipping
        normIcon.contains("home_work") -> Icons.Default.HomeWork
        normIcon.contains("widgets") -> Icons.Default.Widgets
        normIcon.contains("corporate_fare") -> Icons.Default.CorporateFare
        else -> Icons.Default.Warehouse
    }
}


internal fun getArabicIconName(iconKey: String): String {
    return availableLocationIcons.find { it.key.equals(iconKey, ignoreCase = true) }?.nameAr ?: "أيقونة قياسية ($iconKey)"
}


internal fun formatStockLocationFullPath(
    locations: List<StockLocation>,
    locationId: Long?,
    separator: String = " > "
): String {
    if (locationId == null) return "موقع رئيسي (Root)"
    val visited = mutableSetOf<Long>()
    val pathNames = mutableListOf<String>()
    var currId: Long? = locationId

    while (currId != null && !visited.contains(currId)) {
        visited.add(currId)
        val loc = locations.find { it.id == currId } ?: break
        pathNames.add(0, loc.name)
        currId = loc.parentId
    }

    return if (pathNames.isNotEmpty()) pathNames.joinToString(separator) else "موقع #$locationId"
}


internal fun buildHierarchyTreeLines(
    locations: List<StockLocation>,
    parentId: Long?,
    newName: String
): List<Pair<Int, String>> {
    val result = mutableListOf<Pair<Int, String>>()
    if (parentId == null) {
        val displayName = if (newName.isNotBlank()) newName.trim() else "[✨ اسم الموقع قيد الإنشاء]"
        result.add(0 to "🏢 $displayName (مركز رئيسي Root)")
        return result
    }

    val ancestors = mutableListOf<StockLocation>()
    val visited = mutableSetOf<Long>()
    var currId: Long? = parentId

    while (currId != null && !visited.contains(currId)) {
        visited.add(currId)
        val loc = locations.find { it.id == currId } ?: break
        ancestors.add(0, loc)
        currId = loc.parentId
    }

    var indent = 0
    ancestors.forEach { loc ->
        val iconEmoji = when (loc.locationType.uppercase()) {
            "WAREHOUSE" -> "🏢"
            "ZONE" -> "🧩"
            "AISLE" -> "🚪"
            "SHELF" -> "📐"
            "BIN" -> "📥"
            else -> if (loc.structural) "🏗️" else "📍"
        }
        result.add(indent to "$iconEmoji ${loc.name}")
        indent++
    }

    val childName = if (newName.isNotBlank()) newName.trim() else "[✨ اسم الموقع قيد الإنشاء]"
    result.add(indent to "✨ $childName")

    return result
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VisualIconPickerDialog(
    selectedIconKey: String,
    onDismiss: () -> Unit,
    onSelectIcon: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "اختيار الأيقونة البصرية للموقع",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                ) {
                    items(availableLocationIcons, key = { "icon-opt-${it.key}" }) { opt ->
                        val isSelected = opt.key.equals(selectedIconKey, ignoreCase = true)
                        Surface(
                            onClick = {
                                onSelectIcon(opt.key)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = opt.iconVector,
                                    contentDescription = opt.nameAr,
                                    tint = if (isSelected) Color.White else Color(0xFF4F46E5),
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = opt.nameAr,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 10.5.sp
                                    ),
                                    color = if (isSelected) Color.White else Color(0xFF334155),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectOwnerBottomSheet(
    users: List<AppUser>,
    selectedOwner: AppUser?,
    onDismiss: () -> Unit,
    onSelectOwner: (AppUser?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, searchQuery) {
        val activeUsers = users.filter { it.active && !it.isDeleted }
        if (searchQuery.isBlank()) {
            activeUsers
        } else {
            activeUsers.filter { user ->
                user.name.contains(searchQuery, ignoreCase = true) ||
                        user.role.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupervisorAccount,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "اختيار المشرف المسؤول",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "حدد المستخدم المسؤول عن إدارة هذا الموقع",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن مشرف أو دور وظيفي...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color(0xFF64748B))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "owner-none") {
                    val isSelected = selectedOwner == null
                    Surface(
                        onClick = {
                            onSelectOwner(null)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonOff,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "بدون مشرف محدد",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "تمرير قيمة فارغة (null) لمالك الموقع التخزيني",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "محدد",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                itemsIndexed(filteredUsers, key = { index, user -> "user-${user.uuid}-$index" }) { _, user ->
                    val isSelected = selectedOwner != null && (selectedOwner.uuid == user.uuid || selectedOwner.name.equals(user.name, ignoreCase = true))
                    Surface(
                        onClick = {
                            if (isSelected) {
                                onSelectOwner(null)
                            } else {
                                onSelectOwner(user)
                            }
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0E7FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF4338CA),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    if (user.role.isNotBlank()) {
                                        Text(
                                            text = user.role,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "محدد",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectCapacityUnitBottomSheet(
    selectedUnit: String,
    onDismiss: () -> Unit,
    onUnitSelected: (String) -> Unit,
    onAddUnit: (String) -> Unit = {},
    onDeleteUnit: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempSelectedUnit by remember { mutableStateOf(selectedUnit) }
    var searchQuery by remember { mutableStateOf("") }
    var customUnitInput by remember { mutableStateOf("") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var unitToDelete by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    val unitOptions = remember {
        mutableStateListOf(
            Triple("قطعة", "📦 قطعة / وحدة مادية (PCS)", "المعيار الافتراضي لعد القطع والمنتجات الفردية"),
            Triple("كغ", "⚖️ كيلوغرام (Kg)", "معيار الوزن التراكمي للمواد الخام والصب والفلزات"),
            Triple("طن", "🏗️ طن (Ton)", "معيار الوزن الثقيل للشحنات والحمولات الضخمة"),
            Triple("م³", "📐 متر مكعب (m³)", "معيار الحجم والاطراد المكاني للأرضيات والحاويات"),
            Triple("م²", "🏁 متر مربع (m²)", "معيار المساحة المسطحة للحاويات والأرضيات المفتوحة"),
            Triple("صندوق", "📥 صندوق / حاوية (Bin)", "معيار العد بحجم الصناديق والحاويات التخزينية"),
            Triple("طبلية", "🪵 طبلية (Pallet)", "معيار الحمولات المرصوفة على المنصات الخشبية"),
            Triple("بكرة", "🧵 بكرة (Reel)", "معيار بكرات الكوابل والأسلاك والأشرطة")
        )
    }

    LaunchedEffect(selectedUnit) {
        if (selectedUnit.isNotBlank() && unitOptions.none { 
            it.first.equals(selectedUnit, ignoreCase = true) || 
            it.second.contains(selectedUnit, ignoreCase = true) 
        }) {
            unitOptions.add(0, Triple(selectedUnit, "📦 $selectedUnit", "وحدة قياس مخصصة ($selectedUnit)"))
        }
    }

    val filteredUnits = remember(searchQuery, unitOptions.size) {
        if (searchQuery.isBlank()) {
            unitOptions.toList()
        } else {
            unitOptions.filter { (code, title, desc) ->
                code.contains(searchQuery, ignoreCase = true) ||
                title.contains(searchQuery, ignoreCase = true) ||
                desc.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "اختيار وحدة قياس السعة التخزينية",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "حدد المعيار الفيزيائي لقياس السعة القصوى للرف أو الوعاء",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // حقل البحث
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن وحدة قياس (مثل: كغ, متر, قطعة)...", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF64748B)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "مسح",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )

            // إنشاء وحدة قياس جديدة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customUnitInput,
                    onValueChange = { customUnitInput = it },
                    placeholder = { Text("أو أدخل وحدة قياس جديدة (مثال: لتر, كرتونة)...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                Button(
                    onClick = {
                        val newUnit = customUnitInput.trim()
                        if (newUnit.isNotBlank()) {
                            val existing = unitOptions.find { 
                                it.first.equals(newUnit, ignoreCase = true) || 
                                it.second.contains(newUnit, ignoreCase = true) 
                            }
                            if (existing == null) {
                                val newItem = Triple(newUnit, "📦 $newUnit", "وحدة قياس مخصصة ($newUnit)")
                                unitOptions.add(0, newItem)
                            }
                            tempSelectedUnit = newUnit
                            onUnitSelected(newUnit)
                            onAddUnit(newUnit)
                            customUnitInput = ""
                        }
                    },
                    enabled = customUnitInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        disabledContainerColor = Color(0xFFE2E8F0),
                        contentColor = Color.White,
                        disabledContentColor = Color(0xFF94A3B8)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة", fontWeight = FontWeight.Bold)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // الخيار الأول: بدون تحديد وحدة قياس (إمكانية الإلغاء والتراجع)
                val isNoneSelected = tempSelectedUnit.isBlank()
                Surface(
                    onClick = {
                        tempSelectedUnit = ""
                        onUnitSelected("")
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isNoneSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isNoneSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🚫 بدون وحدة قياس (غير محدد)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                color = if (isNoneSelected) Color(0xFF3730A3) else Color(0xFF0F172A)
                            )
                            Text(
                                text = "تجاهل تخصيص وحدة القياس وإبقاؤها فارغة",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF64748B)
                            )
                        }
                        if (isNoneSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                filteredUnits.forEach { item ->
                    val (code, title, desc) = item
                    val isSelected = tempSelectedUnit.equals(code, ignoreCase = true) ||
                                     (tempSelectedUnit.isNotBlank() && title.contains(tempSelectedUnit, ignoreCase = true))
                    Surface(
                        onClick = {
                            if (isSelected) {
                                tempSelectedUnit = ""
                                onUnitSelected("")
                            } else {
                                tempSelectedUnit = code
                                onUnitSelected(code)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (isSelected) Color(0xFF3730A3) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    unitToDelete = item
                                    showDeleteConfirmDialog = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "حذف وحدة القياس",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showDeleteConfirmDialog && unitToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                unitToDelete = null
            },
            title = {
                Text(
                    text = "تأكيد حذف وحدة القياس",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                val nameToDisplay = unitToDelete!!.second
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف وحدة القياس '$nameToDisplay'؟ سيتم إزالتها نهائياً من قائمة الخيارات المتاحة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = unitToDelete!!
                        onDeleteUnit(target.first)
                        unitOptions.removeIf { 
                            it.first.equals(target.first, ignoreCase = true) && 
                            it.second.equals(target.second, ignoreCase = true) 
                        }
                        if (tempSelectedUnit.equals(target.first, ignoreCase = true)) {
                            tempSelectedUnit = ""
                            onUnitSelected("")
                        }
                        showDeleteConfirmDialog = false
                        unitToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("حذف", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        unitToDelete = null
                    }
                ) {
                    Text("إلغاء", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationTypeSelectionBottomSheet(
    selectedLocationType: String,
    onDismiss: () -> Unit,
    onLocationTypeSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempSelectedType by remember { mutableStateOf(selectedLocationType) }

    val typeOptions = remember {
        listOf(
            Triple("SITE", "📍 موقع / منشأة جغرافية", "موقع جغرافي أو مجمع لوجستي رئيسي يضم عدة مستودعات"),
            Triple("WAREHOUSE", "🏢 مستودع", "مواقف ومستودعات تخزينية كاملة أو مبانٍ فرعية مستقلة"),
            Triple("ZONE", "🧩 منطقة / قسم", "تقسيم أو زاوية تخزينية داخل المستودع الرئيسي"),
            Triple("AISLE", "🚪 ممر", "ممر تنظيم حركي يربط المصفوفات والأرفف المتوازية"),
            Triple("SHELF", "📐 رف", "رف تخزين فيزيائي مباشر للمواد والقطع"),
            Triple("BIN", "📥 صندوق / درج", "حاوية أو صندوق لحفظ القطع الصغيرة والمعزولة")
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "اختيار تصنيف الموقع التخزيني",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "حدد النمط الهيكلي للموقع لتحديد طبيعة التخزين",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(typeOptions, key = { index, opt -> "type-opt-${opt.first}-$index" }) { _, (code, title, desc) ->
                    val isSelected = tempSelectedType.equals(code, ignoreCase = true)
                    Surface(
                        onClick = { tempSelectedType = code },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { tempSelectedType = code },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF3B82F6))
                                )
                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onLocationTypeSelected(tempSelectedType)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تأكيد الاختيار", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


internal fun sortLocationsHierarchically(locations: List<StockLocation>): List<StockLocation> {
    val result = mutableListOf<StockLocation>()
    val childrenMap = locations.groupBy { it.parentId }

    fun addWithChildren(parentId: Long?) {
        val directChildren = childrenMap[parentId]?.sortedBy { it.name } ?: return
        for (child in directChildren) {
            result.add(child)
            addWithChildren(child.id)
        }
    }

    addWithChildren(null)
    val addedIds = result.map { it.id }.toSet()
    val orphans = locations.filter { it.id !in addedIds }
    result.addAll(orphans)
    return result
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickAddParentBottomSheet(
    locations: List<StockLocation>,
    currentParentId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, parentId: Long?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var parentName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("ZONE") }
    var selectedGrandparentId by remember { mutableStateOf<Long?>(currentParentId) }
    var isGrandparentPickerOpen by remember { mutableStateOf(false) }

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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFECFDF5))
                            .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "إضافة موقع أب وسيط (+ Quick Add Parent)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "أنشئ موقعاً حاوياً (منطقة/ممر/مستودع) فوراً ليصبح الأب الحالي",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Body Fields
            OutlinedTextField(
                value = parentName,
                onValueChange = { parentName = it },
                label = { Text("اسم الموقع الحاوي (الأب)") },
                placeholder = { Text("مثال: المنطقة الشرقية، ممر 04...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF059669),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "التصنيف الهيكلي للموقع الأب",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF334155)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("WAREHOUSE" to "🏬 مستودع", "ZONE" to "📍 منطقة", "RACK" to "🧱 ممر", "AREA" to "📐 قطاع").forEach { (typeCode, typeLabel) ->
                        val isSelected = selectedType == typeCode
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedType = typeCode },
                            label = { Text(typeLabel, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF059669),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            val grandparentObj = locations.find { it.id == selectedGrandparentId }
            val grandparentTitle = if (selectedGrandparentId == null) "موقع رئيسي (Root)" else grandparentObj?.name ?: "موقع #${selectedGrandparentId}"

            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isGrandparentPickerOpen = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "الموقع الأب له (تحديد المستوى الأعلى):",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "📍 $grandparentTitle",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                            color = Color(0xFF059669)
                        )
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B))
                }
            }

            if (isGrandparentPickerOpen) {
                ParentLocationSelectionBottomSheet(
                    locations = locations,
                    selectedParentId = selectedGrandparentId,
                    onDismiss = { isGrandparentPickerOpen = false },
                    onParentSelected = { newGrandparentId ->
                        selectedGrandparentId = newGrandparentId
                        isGrandparentPickerOpen = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (parentName.isNotBlank()) {
                            onConfirm(parentName, selectedType, selectedGrandparentId)
                        }
                    },
                    enabled = parentName.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ واختيار كأب", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ParentLocationSelectionBottomSheet(
    locations: List<StockLocation>,
    selectedParentId: Long?,
    onDismiss: () -> Unit,
    onQuickAddParent: ((name: String, type: String, parentId: Long?) -> StockLocation)? = null,
    onParentSelected: (Long?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var tempSelectedParentId by remember { mutableStateOf(selectedParentId) }
    var isQuickAddParentDialogOpen by remember { mutableStateOf(false) }

    if (isQuickAddParentDialogOpen) {
        QuickAddParentBottomSheet(
            locations = locations,
            currentParentId = tempSelectedParentId,
            onDismiss = { isQuickAddParentDialogOpen = false },
            onConfirm = { parentName, parentType, grandparentId ->
                val created = onQuickAddParent?.invoke(parentName, parentType, grandparentId)
                if (created != null) {
                    tempSelectedParentId = created.id
                }
                isQuickAddParentDialogOpen = false
            }
        )
    }

    // Filter tree to Sites (المواقع) ONLY as parent container assets
    val availableParentLocations = remember(locations) {
        locations.filter { loc ->
            loc.locationType.equals("SITE", ignoreCase = true) ||
            loc.locationType.equals("ROOT", ignoreCase = true)
        }
    }

    val rootSites = remember(availableParentLocations, searchQuery) {
        val topSites = availableParentLocations.filter { site ->
            site.parentId == null || locations.none { parent ->
                parent.id == site.parentId && (parent.locationType.equals("SITE", ignoreCase = true) || parent.locationType.equals("ROOT", ignoreCase = true))
            }
        }
        if (searchQuery.isBlank()) topSites
        else topSites.filter { site ->
            site.name.contains(searchQuery, ignoreCase = true) ||
            site.description.contains(searchQuery, ignoreCase = true) ||
            availableParentLocations.any { child ->
                child.parentId == site.id && (child.name.contains(searchQuery, ignoreCase = true) || child.description.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    var expandedSiteIds by remember(selectedParentId) {
        val initialExpanded = mutableSetOf<Long>()
        if (selectedParentId != null) {
            val selectedLoc = locations.find { it.id == selectedParentId }
            if (selectedLoc != null && selectedLoc.parentId != null) {
                initialExpanded.add(selectedLoc.parentId!!)
            } else if (selectedLoc != null && (selectedLoc.locationType.equals("SITE", ignoreCase = true) || selectedLoc.locationType.equals("ROOT", ignoreCase = true))) {
                initialExpanded.add(selectedLoc.id)
            }
        }
        mutableStateOf<Set<Long>>(initialExpanded)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "اختيار الموقع الحاوي (الأب)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "تصفح المواقع المتاحة كـ أصول حاوية",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onQuickAddParent != null) {
                        Surface(
                            onClick = { isQuickAddParentDialogOpen = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "أب جديد",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color(0xFF64748B)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث عن اسم الموقع...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color(0xFF64748B))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "المواقع المتاحة",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEEF2FF)
                    ) {
                        Text(
                            text = "${availableParentLocations.size + 1} عنصر",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = Color(0xFF4F46E5),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "اختر موقعاً واحداً",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color(0xFF94A3B8)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "parent-root-opt") {
                    val isSelected = tempSelectedParentId == null
                    Surface(
                        onClick = { tempSelectedParentId = null },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { tempSelectedParentId = null },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF3B82F6))
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "موقع رئيسي (Root)",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            color = Color(0xFF0F172A)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                text = "جذر الشجرة 🏗️",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                                color = Color(0xFFD97706),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "موقع رئيسي مستقل غير تابع لأي موقع أب حاوي",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }

                items(rootSites, key = { "site-${it.id}" }) { site ->
                    val childSites = availableParentLocations.filter {
                        it.parentId == site.id
                    }
                    val isExpanded = expandedSiteIds.contains(site.id)
                    val isSiteSelected = tempSelectedParentId == site.id

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { tempSelectedParentId = site.id },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSiteSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSiteSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = isSiteSelected,
                                        onClick = { tempSelectedParentId = site.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                                    )
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = site.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                ),
                                                color = Color(0xFF0F172A)
                                            )
                                            if (site.isPrimary) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFFEF3C7)
                                                ) {
                                                    Text(
                                                        text = "رئيسي ⭐️",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                                        color = Color(0xFFD97706),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (childSites.isNotEmpty()) {
                                            Text(
                                                text = "${childSites.size} مواقع تابعة",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = Color(0xFF64748B)
                                            )
                                        } else if (site.description.isNotBlank()) {
                                            Text(
                                                text = site.description,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else {
                                            Text(
                                                text = "موقع رئيسي / منشأة تخزينية",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }

                                if (childSites.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            expandedSiteIds = if (isExpanded) {
                                                expandedSiteIds - site.id
                                            } else {
                                                expandedSiteIds + site.id
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isExpanded) "طي" else "توسيع",
                                            tint = Color(0xFF4F46E5)
                                        )
                                    }
                                }
                            }
                        }

                        if (isExpanded && childSites.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                childSites.forEach { childSite ->
                                    val isChildSelected = tempSelectedParentId == childSite.id
                                    Surface(
                                        onClick = { tempSelectedParentId = childSite.id },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isChildSelected) Color(0xFFEEF2FF) else Color(0xFFFAFAFA),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isChildSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                RadioButton(
                                                    selected = isChildSelected,
                                                    onClick = { tempSelectedParentId = childSite.id },
                                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                                                )
                                                Column {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "📍 ${childSite.name}",
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp
                                                            ),
                                                            color = Color(0xFF0F172A)
                                                        )
                                                        if (childSite.isPrimary) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFFFEF3C7)
                                                            ) {
                                                                Text(
                                                                    text = "رئيسي ⭐️",
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                                                    color = Color(0xFFD97706),
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (childSite.description.isNotBlank()) {
                                                        Text(
                                                            text = childSite.description,
                                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                                            color = Color(0xFF64748B),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onParentSelected(tempSelectedParentId)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تأكيد الاختيار", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LayerSpecificLocationPickerSheet(
    layerLabel: String,
    layerType: String,
    locations: List<StockLocation>,
    computedPath: String = "",
    onDismiss: () -> Unit,
    onLocationSelected: (StockLocation) -> Unit,
    onAddNewLocationName: ((String) -> Unit)? = null,
    onScanBarcodeClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredLocations = remember(locations, layerType, searchQuery) {
        val targetType = layerType.uppercase().trim()
        val typeFiltered = locations.filter { loc ->
            when (targetType) {
                "WAREHOUSE" -> loc.locationType.equals("WAREHOUSE", ignoreCase = true)
                "ZONE", "AREA" -> loc.locationType.equals("ZONE", ignoreCase = true) || loc.locationType.equals("AREA", ignoreCase = true)
                "AISLE" -> loc.locationType.equals("AISLE", ignoreCase = true)
                "RACK", "SHELF" -> loc.locationType.equals("SHELF", ignoreCase = true) || loc.locationType.equals("RACK", ignoreCase = true)
                "BIN" -> loc.locationType.equals("BIN", ignoreCase = true)
                else -> loc.locationType.equals(targetType, ignoreCase = true)
            }
        }
        if (searchQuery.isBlank()) {
            typeFiltered
        } else {
            typeFiltered.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    var isCreateModeOpen by remember(filteredLocations) { mutableStateOf(filteredLocations.isEmpty()) }
    var customAddressText by remember { mutableStateOf("") }
    var isManuallyEdited by remember { mutableStateOf(false) }
    var isConfirmed by remember { mutableStateOf(false) }
    val shakeController = remember { ShakeController() }
    var hasError by remember { mutableStateOf(false) }

    fun submitNewName() {
        val rawText = if (isManuallyEdited) customAddressText.trim() else computedPath.trim()
        var extractedName = if (rawText.contains(" > ")) rawText.substringAfterLast(" > ").trim() else rawText.trim()
        val isPlaceholder = extractedName.startsWith("[") && extractedName.endsWith("]")
        if (isPlaceholder || extractedName.isBlank()) {
            extractedName = "$layerLabel 01"
        }
        if (extractedName.isNotBlank()) {
            onAddNewLocationName?.invoke(extractedName)
            onDismiss()
        } else {
            hasError = true
            shakeController.trigger()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isCreateModeOpen) "إضافة $layerLabel جديد بالمسار الميداني" else "اختر موقعاً قائماً لـ $layerLabel",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isCreateModeOpen) "إدخال اسم جديد ذكي مدمج بمسار التخزين" else "عرض الكيانات المعرفة كـ ($layerType) حصراً",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            if (filteredLocations.isNotEmpty()) {
                Surface(
                    onClick = { isCreateModeOpen = !isCreateModeOpen },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCreateModeOpen) Color(0xFFEEF2FF) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isCreateModeOpen) Color(0xFF818CF8) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isCreateModeOpen) Icons.Default.Search else Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCreateModeOpen) "العودة لقائمة الـ $layerLabel القائمة 🔍" else "+ إضافة $layerLabel جديد بالمسار الميداني ✨",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF4F46E5)
                        )
                    }
                }
            }

            if (isCreateModeOpen || filteredLocations.isEmpty()) {
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (filteredLocations.isEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "لا توجد مواقع مسجلة مسبقاً كـ $layerLabel، أدخل اسماً جديداً مباشرةً:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                    color = Color(0xFF334155)
                                )
                            }
                        }

                        DynamicBreadcrumbAddressField(
                            computedPath = computedPath,
                            customAddress = customAddressText,
                            onAddressChange = { newText ->
                                customAddressText = newText
                                isManuallyEdited = true
                                hasError = false
                            },
                            isConfirmed = isConfirmed,
                            onConfirmToggle = {
                                isConfirmed = !isConfirmed
                                submitNewName()
                            },
                            isManuallyEdited = isManuallyEdited,
                            onReset = {
                                customAddressText = ""
                                isManuallyEdited = false
                                isConfirmed = false
                            },
                            isLocked = false,
                            hasDuplicateError = hasError,
                            shakeController = shakeController,
                            label = "اسم $layerLabel الجديد والمسار الميداني",
                            placeholder = "ادخل اسم $layerLabel الجديد..."
                        )

                        Button(
                            onClick = { submitNewName() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4F46E5),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تأكيد وإضافة $layerLabel",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث عن اسم $layerLabel...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color(0xFF64748B))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Surface(
                        onClick = onScanBarcodeClick,
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEEF2FF),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "مسح باركود $layerLabel",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLocations, key = { "picker-loc-${it.id}" }) { loc ->
                        val path = loc.getFullHierarchyPath(locations)
                        Surface(
                            onClick = { onLocationSelected(loc) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = loc.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "📍 $path",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF4F46E5)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "اختيار",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectIntermediateLayersBottomSheet(
    missingRanks: List<Int>,
    locations: List<StockLocation>,
    selectedParentId: Long?,
    parentLocationName: String,
    targetTypeLabel: String,
    initialSelections: Map<Int, Pair<Long?, String>>,
    onDismiss: () -> Unit,
    onConfirm: (Map<Int, Pair<Long?, String>>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempSelections by remember { mutableStateOf(initialSelections) }
    var inlineInputsOpen by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }
    var inlineTypedTexts by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var activePickerRank by remember { mutableStateOf<Int?>(null) }
    var activeScannerRank by remember { mutableStateOf<Int?>(null) }
    var isScannerOpen by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun getRankLabel(rank: Int): String {
        return when (rank) {
            0 -> "موقع عام"
            1 -> "مستودع"
            2 -> "منطقة / قسم"
            3 -> "ممر"
            4 -> "رف"
            5 -> "صندوق / درج"
            else -> "طبقة وسيطة"
        }
    }

    fun getRankDefaultType(rank: Int): String {
        return when (rank) {
            0 -> "SITE"
            1 -> "WAREHOUSE"
            2 -> "ZONE"
            3 -> "AISLE"
            4 -> "SHELF"
            5 -> "BIN"
            else -> "SHELF"
        }
    }

    var customAddresses by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var confirmedStates by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }
    var manuallyEditedStates by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }
    val shakeControllers = remember(missingRanks) { missingRanks.associateWith { ShakeController() } }
    var duplicateErrorRank by remember { mutableStateOf<Int?>(null) }

    fun computeBreadcrumbPathForRank(rankIndex: Int): String {
        val segments = mutableListOf<String>()
        val baseParent = if (selectedParentId != null) {
            locations.find { it.id == selectedParentId }?.getFullHierarchyPath(locations) ?: parentLocationName.trim()
        } else {
            parentLocationName.trim()
        }
        segments.add(baseParent)

        for (i in 0 until rankIndex) {
            val prevRank = missingRanks[i]
            val prevSelection = tempSelections[prevRank]?.second?.trim()
            if (!prevSelection.isNullOrBlank()) {
                segments.add(prevSelection)
            }
        }
        val currentRank = missingRanks[rankIndex]
        val currentSelection = tempSelections[currentRank]?.second?.trim()
        if (!currentSelection.isNullOrBlank()) {
            if (segments.lastOrNull() != currentSelection) {
                segments.add(currentSelection)
            }
        } else {
            segments.add("[${getRankLabel(currentRank)}]")
        }
        return segments.joinToString(" > ")
    }

    val isAllIntermediatesCompleted = missingRanks.isNotEmpty() && missingRanks.all { r ->
        tempSelections[r]?.second?.isNotBlank() == true
    }

    if (activePickerRank != null) {
        val rank = activePickerRank!!
        val rankIdx = missingRanks.indexOf(rank)
        val rankComputedPath = if (rankIdx != -1) computeBreadcrumbPathForRank(rankIdx) else ""

        LayerSpecificLocationPickerSheet(
            layerLabel = getRankLabel(rank),
            layerType = getRankDefaultType(rank),
            locations = locations,
            computedPath = rankComputedPath,
            onDismiss = { activePickerRank = null },
            onLocationSelected = { loc ->
                tempSelections = tempSelections + (rank to Pair(loc.id, loc.name))

                var currParentId = loc.parentId
                if (rankIdx > 0) {
                    for (pIdx in rankIdx - 1 downTo 0) {
                        if (currParentId == null) break
                        val parentLoc = locations.find { it.id == currParentId } ?: break
                        val pRank = missingRanks[pIdx]
                        tempSelections = tempSelections + (pRank to Pair(parentLoc.id, parentLoc.name))
                        currParentId = parentLoc.parentId
                    }
                }

                if (rankIdx != -1) {
                    missingRanks.drop(rankIdx + 1).forEach { subRank ->
                        customAddresses = customAddresses - subRank
                        manuallyEditedStates = manuallyEditedStates - subRank
                        inlineTypedTexts = inlineTypedTexts - subRank
                        tempSelections = tempSelections - subRank
                    }
                }
                activePickerRank = null

                if (rankIdx != -1 && rankIdx + 1 < missingRanks.size) {
                    val nextRank = missingRanks[rankIdx + 1]
                    if (tempSelections[nextRank]?.second.isNullOrBlank()) {
                        activePickerRank = nextRank
                    }
                }
            },
            onAddNewLocationName = { newName ->
                val rankIdx = missingRanks.indexOf(rank)
                if (rankIdx > 0) {
                    for (pIdx in 0 until rankIdx) {
                        val pRank = missingRanks[pIdx]
                        if (tempSelections[pRank]?.second.isNullOrBlank()) {
                            tempSelections = tempSelections + (pRank to Pair(null, "${getRankLabel(pRank)} 01"))
                        }
                    }
                }
                tempSelections = tempSelections + (rank to Pair(null, newName))
                inlineInputsOpen = inlineInputsOpen + (rank to true)
                activePickerRank = null

                if (rankIdx != -1 && rankIdx + 1 < missingRanks.size) {
                    val nextRank = missingRanks[rankIdx + 1]
                    if (tempSelections[nextRank]?.second.isNullOrBlank()) {
                        activePickerRank = nextRank
                    }
                }
            },
            onScanBarcodeClick = {
                activeScannerRank = rank
                isScannerOpen = true
            }
        )
    }

    if (isScannerOpen) {
        LocationBarcodeScannerBottomSheet(
            locations = locations,
            onDismiss = {
                isScannerOpen = false
                activeScannerRank = null
            },
            onBarcodeScanned = { scannedResult ->
                isScannerOpen = false
                val parsed = BarcodePayloadHelper.parsePayload(scannedResult)
                val matchedLoc = locations.find { loc ->
                    loc.uuid.equals(parsed.uuid, ignoreCase = true) ||
                    loc.effectiveUuid.equals(parsed.uuid, ignoreCase = true) ||
                    loc.id.toString() == parsed.uuid.removePrefix("location-").removePrefix("loc-") ||
                    loc.name.equals(scannedResult.trim(), ignoreCase = true)
                }
                val rank = activeScannerRank
                if (rank != null) {
                    val rankIndex = missingRanks.indexOf(rank)
                    if (rankIndex > 0) {
                        for (pIdx in 0 until rankIndex) {
                            val pRank = missingRanks[pIdx]
                            if (tempSelections[pRank]?.second.isNullOrBlank()) {
                                tempSelections = tempSelections + (pRank to Pair(null, "${getRankLabel(pRank)} 01"))
                            }
                        }
                    }

                    if (matchedLoc != null) {
                        tempSelections = tempSelections + (rank to Pair(matchedLoc.id, matchedLoc.name))
                        errorMessage = null

                        var currParentId = matchedLoc.parentId
                        if (rankIndex > 0) {
                            for (pIdx in rankIndex - 1 downTo 0) {
                                if (currParentId == null) break
                                val parentLoc = locations.find { it.id == currParentId } ?: break
                                val pRank = missingRanks[pIdx]
                                tempSelections = tempSelections + (pRank to Pair(parentLoc.id, parentLoc.name))
                                currParentId = parentLoc.parentId
                            }
                        }

                        val currentIndex = missingRanks.indexOf(rank)
                        if (currentIndex != -1 && currentIndex + 1 < missingRanks.size) {
                            val nextRank = missingRanks[currentIndex + 1]
                            if (tempSelections[nextRank]?.second.isNullOrBlank()) {
                                activePickerRank = nextRank
                            }
                        }
                    } else {
                        val fallbackName = scannedResult.trim()
                        if (fallbackName.isNotBlank()) {
                            tempSelections = tempSelections + (rank to Pair(null, fallbackName))
                            inlineInputsOpen = inlineInputsOpen + (rank to true)
                            errorMessage = null
                        }
                    }
                }
                activeScannerRank = null
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "رصف وتحديد الطبقات الوسيطة المفقودة",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "حشو فجوة الهرمية بين ($parentLocationName) ➔ ($targetTypeLabel)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                missingRanks.forEachIndexed { index, rank ->
                    val layerLabel = getRankLabel(rank)

                    val currentPair = tempSelections[rank]
                    val selectedId = currentPair?.first
                    val selectedName = currentPair?.second ?: ""
                    val isCompleted = selectedName.isNotBlank()
                    val isInlineOpen = inlineInputsOpen[rank] == true
                    var typedName by remember(rank, isInlineOpen) { mutableStateOf(inlineTypedTexts[rank] ?: "") }
                    val isRankLocked = index > 0 && tempSelections[missingRanks[index - 1]]?.second.isNullOrBlank()

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCompleted) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isCompleted) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCompleted) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
                                    ) {
                                        Text(
                                            text = "مستوى ${index + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            color = if (isCompleted) Color(0xFF047857) else Color(0xFFD97706),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = layerLabel,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                if (isCompleted) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFD1FAE5)
                                    ) {
                                        Text(
                                            text = if (selectedId != null) "موقع قائم 🏢" else "اسم جديد ✨",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.5.sp
                                            ),
                                            color = Color(0xFF047857),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        activePickerRank = rank
                                        errorMessage = null
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, if (isCompleted) Color(0xFF86EFAC) else Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (isCompleted) Icons.Default.Place else Icons.Default.Search,
                                                contentDescription = null,
                                                tint = if (isCompleted) Color(0xFF059669) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = if (isCompleted) selectedName else "اختر $layerLabel قائماً...",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 12.5.sp
                                                ),
                                                color = if (isCompleted) Color(0xFF0F172A) else Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isCompleted) {
                                            IconButton(
                                                onClick = {
                                                    val rankIdx = missingRanks.indexOf(rank)
                                                    if (rankIdx != -1) {
                                                        missingRanks.drop(rankIdx).forEach { subRank ->
                                                            tempSelections = tempSelections - subRank
                                                            inlineTypedTexts = inlineTypedTexts - subRank
                                                            customAddresses = customAddresses - subRank
                                                            manuallyEditedStates = manuallyEditedStates - subRank
                                                            confirmedStates = confirmedStates - subRank
                                                        }
                                                    } else {
                                                        tempSelections = tempSelections - rank
                                                    }
                                                    inlineInputsOpen = inlineInputsOpen + (rank to false)
                                                    errorMessage = null
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "مسح الاختيار والتراجع",
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "فتح البحث",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    onClick = {
                                        if (index > 0) {
                                            for (pIdx in 0 until index) {
                                                val pRank = missingRanks[pIdx]
                                                if (tempSelections[pRank]?.second.isNullOrBlank()) {
                                                    tempSelections = tempSelections + (pRank to Pair(null, "${getRankLabel(pRank)} 01"))
                                                }
                                            }
                                        }
                                        inlineInputsOpen = inlineInputsOpen + (rank to !isInlineOpen)
                                        errorMessage = null
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isInlineOpen) Color(0xFFEEF2FF) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isInlineOpen) Color(0xFF818CF8) else Color(0xFFE2E8F0)),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "إضافة اسم جديد",
                                            tint = if (isInlineOpen) Color(0xFF4F46E5) else Color(0xFF475569),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = {
                                        if (index > 0) {
                                            for (pIdx in 0 until index) {
                                                val pRank = missingRanks[pIdx]
                                                if (tempSelections[pRank]?.second.isNullOrBlank()) {
                                                    tempSelections = tempSelections + (pRank to Pair(null, "${getRankLabel(pRank)} 01"))
                                                }
                                            }
                                        }
                                        activeScannerRank = rank
                                        isScannerOpen = true
                                        errorMessage = null
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFEEF2FF),
                                    border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "مسح باركود",
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            if (isInlineOpen || (selectedId == null && selectedName.isNotBlank())) {
                                val rankComputedPath = computeBreadcrumbPathForRank(index)
                                val currentCustomAddress = if (manuallyEditedStates[rank] == true) (customAddresses[rank] ?: "") else ""

                                DynamicBreadcrumbAddressField(
                                    computedPath = rankComputedPath,
                                    customAddress = currentCustomAddress,
                                    onAddressChange = { newText ->
                                        customAddresses = customAddresses + (rank to newText)
                                        inlineTypedTexts = inlineTypedTexts + (rank to newText)
                                        manuallyEditedStates = manuallyEditedStates + (rank to true)
                                        duplicateErrorRank = null
                                        errorMessage = null

                                        val lastSegment = if (newText.contains(" > ")) newText.substringAfterLast(" > ").trim() else newText.trim()
                                        val isPlaceholder = lastSegment.startsWith("[") && lastSegment.endsWith("]")
                                        if (lastSegment.isNotBlank() && !isPlaceholder) {
                                            tempSelections = tempSelections + (rank to Pair(null, lastSegment))
                                        } else {
                                            tempSelections = tempSelections - rank
                                        }
                                    },
                                    isConfirmed = confirmedStates[rank] == true,
                                    onConfirmToggle = {
                                        val isConf = !(confirmedStates[rank] ?: false)
                                        confirmedStates = confirmedStates + (rank to isConf)
                                        val rawText = if (manuallyEditedStates[rank] == true) (customAddresses[rank]?.ifBlank { null } ?: rankComputedPath) else rankComputedPath
                                        var lastSegment = if (rawText.contains(" > ")) rawText.substringAfterLast(" > ").trim() else rawText.trim()
                                        val isPlaceholder = lastSegment.startsWith("[") && lastSegment.endsWith("]")
                                        if (isPlaceholder || lastSegment.isBlank()) {
                                            lastSegment = "${getRankLabel(rank)} 01"
                                        }
                                        if (lastSegment.isNotBlank()) {
                                            tempSelections = tempSelections + (rank to Pair(null, lastSegment))
                                            errorMessage = null

                                            val currentIndex = missingRanks.indexOf(rank)
                                            if (currentIndex != -1 && currentIndex + 1 < missingRanks.size) {
                                                val nextRank = missingRanks[currentIndex + 1]
                                                if (tempSelections[nextRank]?.second.isNullOrBlank()) {
                                                    activePickerRank = nextRank
                                                }
                                            }
                                        }
                                    },
                                    isManuallyEdited = manuallyEditedStates[rank] == true,
                                    onReset = {
                                        customAddresses = customAddresses - rank
                                        inlineTypedTexts = inlineTypedTexts - rank
                                        manuallyEditedStates = manuallyEditedStates - rank
                                        tempSelections = tempSelections - rank
                                        duplicateErrorRank = null
                                    },
                                    isLocked = false,
                                    hasDuplicateError = duplicateErrorRank == rank,
                                    shakeController = shakeControllers[rank],
                                    label = "اسم $layerLabel الجديد والمسار الميداني",
                                    placeholder = "ادخل اسم $layerLabel الجديد...",
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (isAllIntermediatesCompleted) {
                        var foundDuplicateRank: Int? = null
                        for (i in missingRanks.indices) {
                            val r = missingRanks[i]
                            val layerName = tempSelections[r]?.second ?: ""
                            val layerAddress = if (manuallyEditedStates[r] == true) customAddresses[r] else computeBreadcrumbPathForRank(i)

                            val evalParentId = if (i == 0) selectedParentId else tempSelections[missingRanks[i - 1]]?.first
                            if (evalParentId != null) {
                                val isDup = locations.any { loc ->
                                    loc.parentId == evalParentId && (
                                        loc.name.trim().equals(layerName.trim(), ignoreCase = true) ||
                                        (!layerAddress.isNullOrBlank() && loc.address.trim().isNotBlank() && loc.address.trim().equals(layerAddress.trim(), ignoreCase = true))
                                    )
                                }
                                if (isDup) {
                                    foundDuplicateRank = r
                                    break
                                }
                            }
                        }

                        if (foundDuplicateRank != null) {
                            duplicateErrorRank = foundDuplicateRank
                            errorMessage = "هذا العنوان / الاسم مستخدم بالفعل ضمن هذا المسار"
                            shakeControllers[foundDuplicateRank]?.trigger()
                        } else {
                            onConfirm(tempSelections)
                        }
                    }
                },
                enabled = isAllIntermediatesCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                    disabledContainerColor = Color(0xFFE2E8F0),
                    contentColor = Color.White,
                    disabledContentColor = Color(0xFF94A3B8)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAllIntermediatesCompleted) "تأكيد ورصف كافة الطبقات الوسيطة" else "يرجى تعبئة كافة الطبقات المفقودة لتفعيل التأكيد",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}


