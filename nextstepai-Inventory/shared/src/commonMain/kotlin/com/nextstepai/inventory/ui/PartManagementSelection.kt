package com.nextstepai.inventory.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.AppUser
import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.ui.components.AppCameraKView
import com.nextstepai.inventory.ui.components.LocationBarcodeScannerBottomSheet
import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.data.CompletenessTone
import com.nextstepai.inventory.data.ManufacturerPart
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartAttachment
import com.nextstepai.inventory.data.PendingAttachment
import com.nextstepai.inventory.data.AttachmentType
import com.nextstepai.inventory.data.currentLabelSnapshot
import com.nextstepai.inventory.data.PartCategory
import com.nextstepai.inventory.data.PartNotes
import com.nextstepai.inventory.data.PartParameter
import com.nextstepai.inventory.data.PartRelatedView
import com.nextstepai.inventory.data.PartTestTemplate
import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.SupplierPart
import com.nextstepai.inventory.data.db.PartInternalPriceEntity
import com.nextstepai.inventory.data.db.PartPricingEntity
import com.nextstepai.inventory.data.db.PartSalePriceEntity
import com.nextstepai.inventory.repository.PartsSummary
import com.nextstepai.inventory.ui.components.Barcode128Canvas
import com.nextstepai.inventory.ui.components.CurrencySelectionBottomSheet
import com.nextstepai.inventory.ui.components.CurrencySelectorField
import com.nextstepai.inventory.ui.components.PartAllocationsBottomSheet
import com.nextstepai.inventory.ui.components.PartIdentityBottomSheet
import com.nextstepai.inventory.ui.components.PrintablePartLabelBottomSheet
import com.nextstepai.inventory.ui.components.QrCodeCanvas
import com.nextstepai.inventory.ui.components.QuickTransferBottomSheet
import com.nextstepai.inventory.ui.components.SearchableCompanyPickerDialog
import com.nextstepai.inventory.ui.components.ShakeController
import com.nextstepai.inventory.ui.components.SmartIpnField
import com.nextstepai.inventory.ui.components.SmartIpnGenerator
import com.nextstepai.inventory.util.BarcodePayloadHelper
import com.nextstepai.inventory.util.DateTimeUtils
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_part
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.card_parts_title
import nextstepai_inventory.shared.generated.resources.filter_assembly
import nextstepai_inventory.shared.generated.resources.filter_low_stock
import nextstepai_inventory.shared.generated.resources.filter_starred
import nextstepai_inventory.shared.generated.resources.parts_count
import nextstepai_inventory.shared.generated.resources.save
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PackagingSelectionBottomSheet(
    selectedPackagingCode: String,
    packagingOptionsList: List<StockPackagingOption>,
    onDismiss: () -> Unit,
    onPackagingSelected: (StockPackagingOption) -> Unit,
    onAddPackagingOption: (StockPackagingOption) -> Unit,
    onDeletePackagingOption: (StockPackagingOption) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showAddDialog by remember { mutableStateOf(false) }
    var newOptionName by remember { mutableStateOf("") }
    var newOptionDesc by remember { mutableStateOf("") }

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
                .padding(horizontal = 20.dp, vertical = 12.dp)
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
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "نوع التغليف والتعبئة",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "حدد نوع أسلوب تغليف القطعة أو قم بإضافة/حذف الأنواع",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Add New Packaging Option Button Box
            Surface(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEEF2FF),
                border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إضافة نوع تغليف وتعبئة جديد +",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4F46E5)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                items(packagingOptionsList) { opt ->
                    val isSelected = opt.code.equals(selectedPackagingCode, ignoreCase = true) ||
                        opt.labelAr.equals(selectedPackagingCode, ignoreCase = true)
                    Surface(
                        onClick = {
                            if (isSelected) {
                                // Deselect on re-click (toggle off)
                                onPackagingSelected(StockPackagingOption("", "بدون تغليف محدد", ""))
                            } else {
                                onPackagingSelected(opt)
                            }
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF4F46E5) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = opt.labelAr,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF0F172A)
                                    )
                                    if (opt.descAr.isNotBlank()) {
                                        Text(
                                            text = opt.descAr,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            // Delete Option Button
                            IconButton(
                                onClick = { onDeletePackagingOption(opt) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "حذف النوع",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add New Packaging Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text("إضافة نوع تغليف جديد", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newOptionName,
                        onValueChange = { newOptionName = it },
                        label = { Text("اسم نوع التغليف *") },
                        placeholder = { Text("مثال: Wooden Crate (صندوق خشبي)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newOptionDesc,
                        onValueChange = { newOptionDesc = it },
                        label = { Text("الوصف (اختياري)") },
                        placeholder = { Text("وصف مختصر لأسلوب التغليف...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newOptionName.isNotBlank()) {
                            val newOpt = StockPackagingOption(
                                code = newOptionName,
                                labelAr = newOptionName,
                                descAr = newOptionDesc
                            )
                            onAddPackagingOption(newOpt)
                            onPackagingSelected(newOpt)
                            newOptionName = ""
                            newOptionDesc = ""
                            showAddDialog = false
                            onDismiss()
                        }
                    },
                    enabled = newOptionName.isNotBlank()
                ) {
                    Text("حفظ وتحديد", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectUserBottomSheet(
    users: List<AppUser>,
    selectedUserUuid: String?,
    onDismiss: () -> Unit,
    onSelect: (AppUser) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.role.contains(searchQuery, ignoreCase = true)
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Row
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
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "اختيار المشرف المسؤول",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
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

            Spacer(modifier = Modifier.height(12.dp))

            // Search input field for responsible user / role
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن مشرف أو دور وظيفي...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF64748B)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "مسح البحث",
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

            Spacer(modifier = Modifier.height(12.dp))

            // User items list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredUsers) { user ->
                    val isSelected = user.uuid == selectedUserUuid
                    Surface(
                        onClick = {
                            onSelect(user)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color.White,
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFF4F46E5) else Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color(0xFF4F46E5),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = user.role.ifBlank { "المستخدم المستلم" },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4F46E5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
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


/**
 * النافذة السفلية لاستعراض أماكن تخزين القطعة ومساراتها المخزنية مع خيار النقل السريع
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PartStorageLocationsBottomSheet(
    part: Part,
    stockItems: List<StockItem>,
    locations: List<StockLocation>,
    onDismiss: () -> Unit,
    onInitiateTransfer: (StockItem) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "أماكن تخزين القطعة",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${part.name} ${if (part.ipn.isNotBlank()) "(IPN: ${part.ipn})" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
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

            HorizontalDivider(color = Color(0xFFE2E8F0))

            val partStockItems = remember(stockItems, part.id) { stockItems.filter { it.partId == part.id } }

            if (partStockItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📍 لا توجد وحدات مخزنية مسجلة لهذه القطعة حالياً",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "يمكنك إضافة شحنة مخزون جديدة عبر زر 'استلام مخزون'",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(partStockItems, key = { index, item -> "part-stock-${item.id}-$index" }) { _, stockItem ->
                        val location = locations.find { it.id == stockItem.locationId }
                        val fullPath = remember(stockItem.locationId, locations) {
                            if (stockItem.locationId == null) "موقع غير محدد"
                            else getBreadcrumbPath(stockItem.locationId, locations).ifBlank { location?.name ?: "موقع #${stockItem.locationId}" }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "📍 $fullPath",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = Color(0xFF1E293B)
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "الكمية: ${stockItem.quantity} ${part.units}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            ),
                                            color = Color(0xFF047857)
                                        )

                                        if (stockItem.batch.isNotBlank()) {
                                            Text(
                                                text = "دفعة: ${stockItem.batch}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = Color(0xFF475569)
                                            )
                                        }

                                        if (stockItem.serial.isNotBlank()) {
                                            Text(
                                                text = "تسلسلي: ${stockItem.serial}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = Color(0xFF475569)
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { onInitiateTransfer(stockItem) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFEEF2FF),
                                        contentColor = Color(0xFF4F46E5)
                                    ),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "⇄ نقل",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        )
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


private fun getBreadcrumbPath(parentId: Long?, locations: List<StockLocation>): String {
    if (parentId == null) return ""
    val visited = mutableSetOf<Long>()
    val pathNames = mutableListOf<String>()
    var currId: Long? = parentId

    while (currId != null && !visited.contains(currId)) {
        visited.add(currId)
        val loc = locations.find { it.id == currId } ?: break
        pathNames.add(0, loc.name)
        currId = loc.parentId
    }

    return pathNames.joinToString(" > ")
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectCategoryBottomSheet(
    categories: List<PartCategory>,
    selectedCatId: Long?,
    onCreateCategory: ((name: String, description: String) -> PartCategory)? = null,
    onDeleteCategory: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSelect: (PartCategory?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var isQuickAddDialogOpen by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<PartCategory?>(null) }
    var currentSelectedCatId by remember(selectedCatId) { mutableStateOf(selectedCatId) }

    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories
        else categories.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
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
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5)
                    )
                    Text(
                        text = "اختر التصنيف (Category)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color(0xFF0F172A)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onCreateCategory != null) {
                        TextButton(
                            onClick = { isQuickAddDialogOpen = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصنيف جديد", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم التصنيف أو الوصف...") },
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

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (onCreateCategory != null) {
                    item {
                        Surface(
                            onClick = { isQuickAddDialogOpen = true },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.5.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
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
                                            .background(Color(0xFF059669)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "+ إنشاء تصنيف جديد سريع",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            color = Color(0xFF065F46)
                                        )
                                        Text(
                                            text = "إضافة تصنيف جديد عاجل إلى شجرة النظام",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF059669)
                                )
                            }
                        }
                    }
                }

                item {
                    val isGeneralSelected = currentSelectedCatId == null
                    Surface(
                        onClick = {
                            currentSelectedCatId = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isGeneralSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isGeneralSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isGeneralSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = if (isGeneralSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "بدون تصنيف (عام)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "صنف عام غير مرتبط بفيئة محددة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Checkbox(
                                checked = isGeneralSelected,
                                onCheckedChange = { currentSelectedCatId = null },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4F46E5),
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                itemsIndexed(filteredCategories, key = { index, cat -> "cat-${cat.id}-$index" }) { _, cat ->
                    val isSelected = cat.id == currentSelectedCatId
                    Surface(
                        onClick = {
                            currentSelectedCatId = if (isSelected) null else cat.id
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = cat.description.ifBlank { "تصنيف فرعي في النظام" },
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (onDeleteCategory != null) {
                                    IconButton(
                                        onClick = { categoryToDelete = cat },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "حذف التصنيف",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        currentSelectedCatId = if (isSelected) null else cat.id
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF4F46E5),
                                        uncheckedColor = Color(0xFF94A3B8)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    onSelect(categories.find { it.id == currentSelectedCatId })
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "تم (تأكيد الاختيار)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }

    if (categoryToDelete != null) {
        val catToDelete = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                    Text("تأكيد حذف التصنيف", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "هل أنت تأكد من رغبتك في حذف تصنيف \"${catToDelete.name}\"؟ لن تؤثر العملية على القطع القائمة بل ستصبح تصنيفاتها (عام).",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory?.invoke(catToDelete.id)
                        if (selectedCatId == catToDelete.id) {
                            onSelect(null)
                        }
                        categoryToDelete = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (isQuickAddDialogOpen && onCreateCategory != null) {
        QuickAddCategoryBottomSheet(
            onDismiss = { isQuickAddDialogOpen = false },
            onConfirm = { name, desc ->
                val created = onCreateCategory.invoke(name, desc)
                onSelect(created)
                isQuickAddDialogOpen = false
                onDismiss()
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectTemplatePartBottomSheet(
    templateParts: List<Part>,
    isTemplate: Boolean,
    isStandaloneSelected: Boolean,
    selectedVariantOfId: Long?,
    onDismiss: () -> Unit,
    onSelect: (isTemplate: Boolean, isStandalone: Boolean, selectedTemplate: Part?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var currentIsTemplate by remember(isTemplate, isStandaloneSelected, selectedVariantOfId) { mutableStateOf(isTemplate) }
    var currentIsStandalone by remember(isTemplate, isStandaloneSelected, selectedVariantOfId) { mutableStateOf(isStandaloneSelected) }
    var currentSelectedVariantOfId by remember(isTemplate, isStandaloneSelected, selectedVariantOfId) { mutableStateOf(selectedVariantOfId) }

    val filteredTemplates = remember(templateParts, searchQuery) {
        if (searchQuery.isBlank()) templateParts
        else templateParts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.ipn.contains(searchQuery, ignoreCase = true)
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "اختر الصنف القالب الأصل",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFF0F172A)
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم القالب أو رقم IPN...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF64748B)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                // 1. الخيار الأول: تعيين كقالب جديد (isTemplate)
                item {
                    val isNewTemplateSelected = currentIsTemplate && !currentIsStandalone && currentSelectedVariantOfId == null
                    Surface(
                        onClick = {
                            currentIsTemplate = true
                            currentIsStandalone = false
                            currentSelectedVariantOfId = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isNewTemplateSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isNewTemplateSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isNewTemplateSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Extension,
                                        contentDescription = null,
                                        tint = if (isNewTemplateSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "تعيين كقالب جديد",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "اعتماد هذا الصنف كقالب تجريدي تُشتق منه قطع ومتغيرات فرعية أخرى",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Checkbox(
                                checked = isNewTemplateSelected,
                                onCheckedChange = {
                                    currentIsTemplate = true
                                    currentIsStandalone = false
                                    currentSelectedVariantOfId = null
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4F46E5),
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                // 2. الخيار الثاني: صنف أصلي مستقل (بدون قالب أصل)
                item {
                    val isStandalone = currentIsStandalone && !currentIsTemplate && currentSelectedVariantOfId == null
                    Surface(
                        onClick = {
                            currentIsTemplate = false
                            currentIsStandalone = true
                            currentSelectedVariantOfId = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isStandalone) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isStandalone) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isStandalone) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Extension,
                                        contentDescription = null,
                                        tint = if (isStandalone) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "صنف أصلي مستقل (بدون قالب أصل)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "قطعة قائمة بذاتها وغير مشتقة من قالب آخر",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Checkbox(
                                checked = isStandalone,
                                onCheckedChange = {
                                    currentIsTemplate = false
                                    currentIsStandalone = true
                                    currentSelectedVariantOfId = null
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4F46E5),
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                // 3. الخيارات المشتقة من قوالب أصلية أخرى قائمة
                itemsIndexed(filteredTemplates, key = { index, t -> "tpl-${t.id}-$index" }) { _, t ->
                    val isSelected = !currentIsTemplate && !currentIsStandalone && t.id == currentSelectedVariantOfId
                    Surface(
                        onClick = {
                            currentIsTemplate = false
                            currentIsStandalone = false
                            currentSelectedVariantOfId = t.id
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Extension,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = t.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "IPN: ${t.ipn.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    currentIsTemplate = false
                                    currentIsStandalone = false
                                    currentSelectedVariantOfId = t.id
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4F46E5),
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val selectedTpl = if (currentIsTemplate || currentIsStandalone) null else templateParts.find { it.id == currentSelectedVariantOfId }
                    onSelect(currentIsTemplate, currentIsStandalone, selectedTpl)
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "تم (تأكيد الاختيار)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

private enum class PartRoleOption {
    RAW_COMPONENT,    // مكوّن أولي / مادة خام (شراء + مكون فرعي)
    ASSEMBLY_PRODUCT, // منتج مجمّع / تصنيع داخلي (تجميع + بيع)
    FINISHED_GOOD,   // منتج تجاري جاهز (شراء + بيع)
    CUSTOM            // تخصيص مخصص
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectPartNatureOptionsBottomSheet(
    purchaseable: Boolean,
    salable: Boolean,
    component: Boolean,
    assembly: Boolean,
    isTemplate: Boolean,
    virtual: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        purchaseable: Boolean,
        salable: Boolean,
        component: Boolean,
        assembly: Boolean,
        isTemplate: Boolean,
        virtual: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var p by remember { mutableStateOf(purchaseable) }
    var s by remember { mutableStateOf(salable) }
    var c by remember { mutableStateOf(component) }
    var a by remember { mutableStateOf(assembly) }
    var t by remember { mutableStateOf(isTemplate) }
    var v by remember { mutableStateOf(virtual) }

    // تحديد الدور الأولي تلقائياً بناءً على الأعلام المحددة
    var activeRole by remember {
        mutableStateOf(
            when {
                p && c && !a && !s -> PartRoleOption.RAW_COMPONENT
                a && s && !p && !c -> PartRoleOption.ASSEMBLY_PRODUCT
                p && s && !a && !c -> PartRoleOption.FINISHED_GOOD
                else -> PartRoleOption.CUSTOM
            }
        )
    }

    var showCustomFlags by remember { mutableStateOf(activeRole == PartRoleOption.CUSTOM) }
    var showAdvancedOptions by remember { mutableStateOf(t || v) }

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
                .padding(horizontal = 20.dp, vertical = 8.dp)
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
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
                            text = "طبيعة التعامل والدور الوظيفي للصنف",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "اختر طبيعة استخدام الصنف في النظام لتشغيل الأعلام تلقائياً",
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

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. مكوّن أولي / مادة خام (Raw Component)
                val isRawSelected = activeRole == PartRoleOption.RAW_COMPONENT
                Surface(
                    onClick = {
                        activeRole = PartRoleOption.RAW_COMPONENT
                        p = true
                        c = true
                        a = false
                        s = false
                        showCustomFlags = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isRawSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.5.dp,
                        if (isRawSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isRawSelected) Color(0xFFE0E7FF) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = if (isRawSelected) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "مكوّن أولي / مادة خام (Raw Component)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isRawSelected) Color(0xFF312E81) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "قطعة نشتريها من مورد وتدخل في تجميع المكونات (مثل: مقاوم، برغي، سلك)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        RadioButton(
                            selected = isRawSelected,
                            onClick = {
                                activeRole = PartRoleOption.RAW_COMPONENT
                                p = true
                                c = true
                                a = false
                                s = false
                                showCustomFlags = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                        )
                    }
                }

                // 2. منتج مجمّع / تصنيع داخلي (Assembly Product)
                val isAssemblySelected = activeRole == PartRoleOption.ASSEMBLY_PRODUCT
                Surface(
                    onClick = {
                        activeRole = PartRoleOption.ASSEMBLY_PRODUCT
                        a = true
                        s = true
                        p = false
                        c = false
                        showCustomFlags = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isAssemblySelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.5.dp,
                        if (isAssemblySelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isAssemblySelected) Color(0xFFE0E7FF) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = if (isAssemblySelected) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "منتج مجمّع / تصنيع داخلي (Assembly)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isAssemblySelected) Color(0xFF312E81) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "لوحة أو جهاز نصنعه محلياً من عدة مكونات ويمتلك قائمة مواد (BOM)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        RadioButton(
                            selected = isAssemblySelected,
                            onClick = {
                                activeRole = PartRoleOption.ASSEMBLY_PRODUCT
                                a = true
                                s = true
                                p = false
                                c = false
                                showCustomFlags = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                        )
                    }
                }

                // 3. منتج تجاري جاهز (Finished / Trading Good)
                val isFinishedSelected = activeRole == PartRoleOption.FINISHED_GOOD
                Surface(
                    onClick = {
                        activeRole = PartRoleOption.FINISHED_GOOD
                        p = true
                        s = true
                        a = false
                        c = false
                        showCustomFlags = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isFinishedSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.5.dp,
                        if (isFinishedSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isFinishedSelected) Color(0xFFE0E7FF) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = if (isFinishedSelected) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "منتج تجاري جاهز (Finished Goods)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isFinishedSelected) Color(0xFF312E81) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "منتج نشتريه جاهزاً ونعيد بيعه مباشرة للعملاء دون تصنيع",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        RadioButton(
                            selected = isFinishedSelected,
                            onClick = {
                                activeRole = PartRoleOption.FINISHED_GOOD
                                p = true
                                s = true
                                a = false
                                c = false
                                showCustomFlags = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                        )
                    }
                }

                // 4. تخصيص مخصص (Custom Selection)
                val isCustomSelected = activeRole == PartRoleOption.CUSTOM
                Surface(
                    onClick = {
                        activeRole = PartRoleOption.CUSTOM
                        showCustomFlags = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCustomSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.5.dp,
                        if (isCustomSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCustomSelected) Color(0xFFE0E7FF) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = if (isCustomSelected) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "تخصيص يدوي مخصص (Custom Role)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isCustomSelected) Color(0xFF312E81) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "تحديد وتخصيص أعلام الشراء والبيع والتجميع بشكل يدوي منفصل",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        RadioButton(
                            selected = isCustomSelected,
                            onClick = {
                                activeRole = PartRoleOption.CUSTOM
                                showCustomFlags = true
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                        )
                    }
                }

                // عرض أعلام التخصيص اليدوي عند اختيار "تخصيص مخصص"
                if (showCustomFlags) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "تعديل الأعلام اليدوية المنفصلة:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF475569)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { p = !p }) {
                                Checkbox(checked = p, onCheckedChange = { p = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5)))
                                Text("قابل للشراء والتوريد (purchaseable)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { s = !s }) {
                                Checkbox(checked = s, onCheckedChange = { s = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5)))
                                Text("قابل للبيع والطلب (salable)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { c = !c }) {
                                Checkbox(checked = c, onCheckedChange = { c = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5)))
                                Text("مكون فرعي في تجميعات (component)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { a = !a }) {
                                Checkbox(checked = a, onCheckedChange = { a = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5)))
                                Text("منتج مجمع وقائمة مواد (assembly)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // قسم الخصائص المتقدمة المطوي (Advanced Characteristics)
                Surface(
                    onClick = { showAdvancedOptions = !showAdvancedOptions },
                    color = Color(0xFFEEF2FF).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "خصائص هندسية إضافية (تتبع، قالب، خدمة)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                ),
                                color = Color(0xFF3730A3)
                            )
                        }

                        Icon(
                            imageVector = if (showAdvancedOptions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5)
                        )
                    }
                }

                if (showAdvancedOptions) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    ) {
                        // 1. صنف وهمي / خدمة (virtual)
                        Surface(
                            onClick = { v = !v },
                            shape = RoundedCornerShape(12.dp),
                            color = if (v) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (v) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("صنف وهمي / خدمة (virtual)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp))
                                    Text("صنف غير ملموس (خدمة تركيب، ترخيص برمجي)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                                }
                                Checkbox(checked = v, onCheckedChange = { v = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5)))
                            }
                        }

                        // 2. اعتماد كقالب تجريدي أصل (isTemplate)
                        Surface(
                            onClick = { t = !t },
                            shape = RoundedCornerShape(12.dp),
                            color = if (t) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (t) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("اعتماد كقالب تجريدي أصل (is_template)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp))
                                    Text("اعتماد هذا الصنف كقالب لأصناف أخرى (مثل: مقاسات أو ألوان مختلفة)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                                }
                                Checkbox(checked = t, onCheckedChange = { t = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5)))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    onConfirm(p, s, c, a, t, v)
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "تأكيد الطبيعة والخصائص",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectLocationBottomSheet(
    locations: List<StockLocation>,
    selectedLocationId: Long?,
    onDismiss: () -> Unit,
    onSelect: (StockLocation?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var currentSelectedLocationId by remember(selectedLocationId) { mutableStateOf(selectedLocationId) }
    var showScannerInSheet by remember { mutableStateOf(false) }
    var showTypeFilterBar by remember { mutableStateOf(false) }
    var selectedTypeFilter by remember { mutableStateOf<String?>(null) }

    val availableTypes = remember(locations) {
        locations.mapNotNull { it.locationType.ifBlank { null } }.distinct()
    }

    val filteredLocations = remember(locations, searchQuery, selectedTypeFilter) {
        locations.filter { loc ->
            val matchesSearch = searchQuery.isBlank() ||
                loc.name.contains(searchQuery, ignoreCase = true) ||
                loc.description.contains(searchQuery, ignoreCase = true)
            val matchesType = selectedTypeFilter == null ||
                loc.locationType.equals(selectedTypeFilter, ignoreCase = true)
            matchesSearch && matchesType
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "اختر موقع التخزين الافتراضي",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFF0F172A)
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search input field with separate standalone Filter & QR scanner buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث باسم الموقع...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "مسح البحث",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // Storage Location Filter Button Box
                Surface(
                    onClick = { showTypeFilterBar = !showTypeFilterBar },
                    shape = RoundedCornerShape(12.dp),
                    color = if (showTypeFilterBar || selectedTypeFilter != null) Color(0xFF4F46E5) else Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "فرز وتصفية المواقع",
                            tint = if (showTypeFilterBar || selectedTypeFilter != null) Color.White else Color(0xFF4F46E5),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Separate Standalone QR Scanner Button Box
                Surface(
                    onClick = { showScannerInSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "مسح رمز الموقع QR",
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Active Filter Badge Indicator Chip
            if (selectedTypeFilter != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "التصفية المطبقة:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color(0xFF64748B)
                    )
                    Surface(
                        color = Color(0xFFEEF2FF),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = selectedTypeFilter!!,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF4F46E5)
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إلغاء الفلتر",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { selectedTypeFilter = null }
                            )
                        }
                    }
                }
            }

            if (showScannerInSheet) {
                LocationBarcodeScannerBottomSheet(
                    locations = locations,
                    onDismiss = { showScannerInSheet = false },
                    onBarcodeScanned = { barcode ->
                        val matchedLoc = locations.find {
                            it.id.toString() == barcode ||
                            it.name.contains(barcode, ignoreCase = true) ||
                            it.uuid == barcode
                        }
                        if (matchedLoc != null) {
                            onSelect(matchedLoc)
                            onDismiss()
                        } else {
                            searchQuery = barcode
                        }
                        showScannerInSheet = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    val isNoneSelected = currentSelectedLocationId == null
                    Surface(
                        onClick = {
                            currentSelectedLocationId = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isNoneSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isNoneSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isNoneSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (isNoneSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "بدون موقع افتراضي",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "يتم تحديد الموقع عند استلام الشحنات وتخزينها",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Checkbox(
                                checked = isNoneSelected,
                                onCheckedChange = { currentSelectedLocationId = null },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4F46E5),
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                itemsIndexed(filteredLocations, key = { index, loc -> "loc-picker-${loc.id}-$index" }) { _, loc ->
                    val isSelected = loc.id == currentSelectedLocationId
                    Surface(
                        onClick = {
                            currentSelectedLocationId = if (isSelected) null else loc.id
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = loc.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    if (loc.description.isNotBlank()) {
                                        Text(
                                            text = loc.description,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    currentSelectedLocationId = if (isSelected) null else loc.id
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4F46E5),
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    onSelect(locations.find { it.id == currentSelectedLocationId })
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "تم (تأكيد الاختيار)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }

    // Sub-BottomSheet for Storage Location Type Filtering
    if (showTypeFilterBar) {
        LocationTypeFilterBottomSheet(
            availableTypes = availableTypes,
            selectedTypeFilter = selectedTypeFilter,
            allLocationsCount = locations.size,
            locations = locations,
            onSelectFilter = { type ->
                selectedTypeFilter = type
                showTypeFilterBar = false
            },
            onDismiss = { showTypeFilterBar = false }
        )
    }
}


internal fun normalizeLocationTypeCode(typeCode: String): String {
    return when (typeCode.uppercase().trim()) {
        "AREA", "LINE" -> "ZONE"
        "RACK" -> "SHELF"
        "BOX", "CONTAINER", "DRAWER" -> "BIN"
        else -> typeCode.uppercase().trim()
    }
}


internal fun getArabicLocationTypeLabel(typeCode: String): String {
    return when (normalizeLocationTypeCode(typeCode)) {
        "SITE" -> "📍 المنشأة / الموقع المادي (Site)"
        "WAREHOUSE" -> "🏢 المستودع الرئيسي (Warehouse)"
        "ZONE" -> "🧩 المنطقة / الساحة (Zone / Area)"
        "AISLE" -> "🚪 الممر (Aisle)"
        "SHELF" -> "📐 رف التخزين (Shelf / Rack)"
        "BIN" -> "📥 الصندوق / الحاوية (Box / Bin / Drawer)"
        else -> typeCode.ifBlank { "موقع تخزين" }
    }
}

private data class LocationHierarchyFilterOption(
    val code: String,
    val arabicLabel: String,
    val count: Int
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationTypeFilterBottomSheet(
    availableTypes: List<String>,
    selectedTypeFilter: String?,
    allLocationsCount: Int,
    locations: List<StockLocation>,
    onSelectFilter: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val standardHierarchyCodes = remember { listOf("SITE", "WAREHOUSE", "ZONE", "AISLE", "SHELF", "BIN") }

    val hierarchyOptions = remember(locations, availableTypes) {
        val options = mutableListOf<LocationHierarchyFilterOption>()

        // Add standard location hierarchy levels in order
        standardHierarchyCodes.forEach { code ->
            val count = locations.count { normalizeLocationTypeCode(it.locationType) == code }
            options.add(
                LocationHierarchyFilterOption(
                    code = code,
                    arabicLabel = getArabicLocationTypeLabel(code),
                    count = count
                )
            )
        }

        // Add any custom/other types present in locations that aren't in the standard 6 codes
        availableTypes.forEach { rawType ->
            val norm = normalizeLocationTypeCode(rawType)
            if (norm !in standardHierarchyCodes && rawType.isNotBlank()) {
                val count = locations.count { it.locationType.equals(rawType, ignoreCase = true) }
                options.add(
                    LocationHierarchyFilterOption(
                        code = rawType,
                        arabicLabel = getArabicLocationTypeLabel(rawType),
                        count = count
                    )
                )
            }
        }
        options
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "تصفية حسب نوع ومستوى موقع التخزين",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "حدد مستوى التصفية (منشأة ➔ مستودع ➔ منطقة ➔ ممر ➔ رف ➔ صندوق)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Option 1: All Locations (Reset Filter)
            val isAllSelected = selectedTypeFilter == null
            Surface(
                onClick = {
                    onSelectFilter(null)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                color = if (isAllSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isAllSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isAllSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isAllSelected) Color(0xFF4F46E5) else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "جميع المواقع التخزينية (عرض الكل)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            color = if (isAllSelected) Color(0xFF4F46E5) else Color(0xFF0F172A)
                        )
                    }
                    Surface(
                        color = Color(0xFFE0E7FF),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$allLocationsCount موقع",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF4338CA),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Location Types Hierarchy List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                items(hierarchyOptions) { opt ->
                    val isSelected = selectedTypeFilter?.let {
                        normalizeLocationTypeCode(it) == normalizeLocationTypeCode(opt.code) ||
                        it.equals(opt.code, ignoreCase = true)
                    } ?: false

                    Surface(
                        onClick = {
                            if (isSelected) {
                                // Toggle off on re-click
                                onSelectFilter(null)
                            } else {
                                onSelectFilter(opt.code)
                            }
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF4F46E5) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = opt.arabicLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF0F172A)
                                )
                            }
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${opt.count}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
internal fun SelectNatureBottomSheet(
    isComponentSelected: Boolean,
    isAssemblySelected: Boolean,
    isSalableSelected: Boolean,
    onDismiss: () -> Unit,
    onToggleComponent: () -> Unit,
    onToggleAssembly: () -> Unit,
    onToggleSalable: () -> Unit
) {
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
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "طبيعة التعامل والتصنيع (نوع الصنف)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "يمكنك تحديد أكثر من خيار واحد أو إلغاء جميع الخيارات (ضغطة للاختيار وضغطة للالغة)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF64748B)
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

            // Option 1: Component
            Surface(
                onClick = onToggleComponent,
                shape = RoundedCornerShape(14.dp),
                color = if (isComponentSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                border = BorderStroke(
                    1.5.dp,
                    if (isComponentSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isComponentSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = if (isComponentSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "مكوّن أولي خام (Component)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "قطعة مفردة أو خامة تُستخدم كعنصر رئيسي في الإنتاج والتجميع",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Checkbox(
                        checked = isComponentSelected,
                        onCheckedChange = { onToggleComponent() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF4F46E5),
                            uncheckedColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            // Option 2: Assembly
            Surface(
                onClick = onToggleAssembly,
                shape = RoundedCornerShape(14.dp),
                color = if (isAssemblySelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                border = BorderStroke(
                    1.5.dp,
                    if (isAssemblySelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAssemblySelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = null,
                                tint = if (isAssemblySelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "تجميع داخلي (Assembly / BOM)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "تجميعة أو بوردة تحتوي على شجرة مكونات وقائمة مواد BOM",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Checkbox(
                        checked = isAssemblySelected,
                        onCheckedChange = { onToggleAssembly() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF4F46E5),
                            uncheckedColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            // Option 3: Salable
            Surface(
                onClick = onToggleSalable,
                shape = RoundedCornerShape(14.dp),
                color = if (isSalableSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                border = BorderStroke(
                    1.5.dp,
                    if (isSalableSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSalableSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = if (isSalableSelected) Color(0xFF3730A3) else Color(0xFF64748B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "منتج تجاري جاهز للبيع (Salable Product)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "منتج نهائي مُعد للبيع المباشر للعملاء وأوامر البيع",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Checkbox(
                        checked = isSalableSelected,
                        onCheckedChange = { onToggleSalable() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF4F46E5),
                            uncheckedColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("تم (تأكيد الاختيار)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PartFilterBottomSheet(
    categories: List<PartCategory>,
    selectedCategoryId: Long?,
    initialCategoryIds: Set<Long> = emptySet(),
    parts: List<Part>,
    starredPartIds: Set<Long> = emptySet(),
    initialLowStock: Boolean,
    initialAssembly: Boolean,
    initialComponent: Boolean,
    initialPurchaseable: Boolean,
    initialSalable: Boolean,
    initialStarred: Boolean,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: (
        categoryIds: Set<Long>,
        lowStock: Boolean,
        assembly: Boolean,
        component: Boolean,
        purchaseable: Boolean,
        salable: Boolean,
        starred: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCatIdsState by remember {
        mutableStateOf<Set<Long>>(
            if (initialCategoryIds.isNotEmpty()) initialCategoryIds
            else if (selectedCategoryId != null) setOf(selectedCategoryId)
            else emptySet()
        )
    }
    var lowStockOnly by remember { mutableStateOf(initialLowStock) }
    var assemblyOnly by remember { mutableStateOf(initialAssembly) }
    var componentOnly by remember { mutableStateOf(initialComponent) }
    var purchaseableOnly by remember { mutableStateOf(initialPurchaseable) }
    var salableOnly by remember { mutableStateOf(initialSalable) }
    var starredOnly by remember { mutableStateOf(initialStarred) }

    val calculatedCount = remember(
        parts, searchQuery, selectedCatIdsState,
        lowStockOnly, assemblyOnly, componentOnly, purchaseableOnly, salableOnly, starredOnly
    ) {
        val query = searchQuery.trim()
        parts.count { part ->
            val matchesCategory = selectedCatIdsState.isEmpty() || (part.categoryId != null && selectedCatIdsState.contains(part.categoryId))
            val matchesQuery = query.isBlank() ||
                    part.name.contains(query, ignoreCase = true) ||
                    part.ipn.contains(query, ignoreCase = true) ||
                    part.description.contains(query, ignoreCase = true) ||
                    part.keywords.contains(query, ignoreCase = true)

            val matchesLowStock = !lowStockOnly || part.totalInStock <= part.minimumStock
            val matchesAssembly = !assemblyOnly || part.assembly
            val matchesComponent = !componentOnly || part.component
            val matchesPurchaseable = !purchaseableOnly || part.purchaseable
            val matchesSalable = !salableOnly || part.salable
            val matchesStarred = !starredOnly || starredPartIds.contains(part.id)

            matchesCategory && matchesQuery && matchesLowStock && matchesAssembly && matchesComponent && matchesPurchaseable && matchesSalable && matchesStarred
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
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
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
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "تصفية دليل القطع والمكونات",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "تحديد التصنيف الهرمي وخصائص وطبيعة التعامل",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = {
                        searchQuery = ""
                        selectedCatIdsState = emptySet()
                        lowStockOnly = false
                        assemblyOnly = false
                        componentOnly = false
                        purchaseableOnly = false
                        salableOnly = false
                        starredOnly = false
                        onReset()
                    }) {
                        Text(
                            text = "مسح الكل",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5)
                            )
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
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Body Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. حقل البحث الرئيسي
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن الفئة، اسم القطعة، أو الـ IPN...") },
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
                                    contentDescription = null,
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

                // 2. شريط التبويبات الثلاثية
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
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🏷️ التصنيف الهرمي",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (selectedTab == 0) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚡ الفلاتر والحالات",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (selectedTab == 1) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            onClick = { selectedTab = 2 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 2) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 2) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚙️ طبيعة التعامل والخصائص",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (selectedTab == 2) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 3. محتوى التبويب المختار
                val filteredCategories = remember(categories, searchQuery) {
                    if (searchQuery.isBlank()) categories
                    else categories.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
                    }
                }

                val lowStockCount = remember(parts) { parts.count { it.totalInStock <= it.minimumStock } }
                val assemblyCount = remember(parts) { parts.count { it.assembly } }
                val starredCount = remember(parts, starredPartIds) { parts.count { starredPartIds.contains(it.id) } }
                val purchaseableCount = remember(parts) { parts.count { it.purchaseable } }
                val salableCount = remember(parts) { parts.count { it.salable } }
                val componentCount = remember(parts) { parts.count { it.component } }

                if (selectedTab == 0) {
                    // تبويب الفئات والتصنيفات الهرمية (دعم الاختيار المتعدد)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // خيار عرض الكل (يظهر مفصلاً عندما تكون مجموعة التصنيفات فارغة)
                        val isAllSelected = selectedCatIdsState.isEmpty()
                        Surface(
                            onClick = { selectedCatIdsState = emptySet() },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isAllSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (isAllSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isAllSelected,
                                    onCheckedChange = { if (it) selectedCatIdsState = emptySet() },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "جميع التصنيفات والفئات",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (isAllSelected) Color(0xFF4338CA) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "عرض كافة المكونات والأصناف بدون تحديد تصنيف محدد",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    color = if (isAllSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = parts.size.toString(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isAllSelected) Color(0xFF3730A3) else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        filteredCategories.forEach { category ->
                            val isSelected = selectedCatIdsState.contains(category.id)
                            val catCount = remember(parts, category.id) { parts.count { it.categoryId == category.id } }
                            Surface(
                                onClick = {
                                    selectedCatIdsState = if (isSelected) {
                                        selectedCatIdsState - category.id
                                    } else {
                                        selectedCatIdsState + category.id
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            selectedCatIdsState = if (isSelected) {
                                                selectedCatIdsState - category.id
                                            } else {
                                                selectedCatIdsState + category.id
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = if (isSelected) Color(0xFF4338CA) else Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = if (category.description.isNotBlank()) category.description else "عرض الأصناف والقطع التابعة لقسم ${category.name}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Surface(
                                        color = if (isSelected) Color(0xFFC7D2FE) else Color(0xFFE2E8F0),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = catCount.toString(),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            ),
                                            color = if (isSelected) Color(0xFF3730A3) else Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    // تبويب الفلاتر والحالات السريعة (بطاقات موحدة التصميم)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. خيار النواقص (منخفضة المخزون)
                        Surface(
                            onClick = { lowStockOnly = !lowStockOnly },
                            shape = RoundedCornerShape(14.dp),
                            color = if (lowStockOnly) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (lowStockOnly) Color(0xFFEF4444) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = lowStockOnly,
                                    onCheckedChange = { lowStockOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444))
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "⚠️ النواقص بالمخزون",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (lowStockOnly) Color(0xFF991B1B) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "عرض القطع والمكونات التي تقل كميتها عن حد الأمان",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    color = if (lowStockCount > 0) Color(0xFFFEE2E2) else Color(0xFFE2E8F0),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = lowStockCount.toString(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (lowStockCount > 0) Color(0xFF991B1B) else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        // 2. خيار تجميعات BOM
                        Surface(
                            onClick = { assemblyOnly = !assemblyOnly },
                            shape = RoundedCornerShape(14.dp),
                            color = if (assemblyOnly) Color(0xFFF3E8FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (assemblyOnly) Color(0xFFC084FC) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = assemblyOnly,
                                    onCheckedChange = { assemblyOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF9333EA))
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "🧩 تجميعات BOM",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (assemblyOnly) Color(0xFF6B21A8) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "عرض المنتجات والمكونات المجمعة ذات قائمة المواد",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    color = if (assemblyCount > 0) Color(0xFFE9D5FF) else Color(0xFFE2E8F0),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = assemblyCount.toString(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (assemblyCount > 0) Color(0xFF6B21A8) else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        // 3. خيار الأصناف المفضلة
                        Surface(
                            onClick = { starredOnly = !starredOnly },
                            shape = RoundedCornerShape(14.dp),
                            color = if (starredOnly) Color(0xFFFEF3C7) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (starredOnly) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = starredOnly,
                                    onCheckedChange = { starredOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706))
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "⭐ الأصناف المفضلة",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (starredOnly) Color(0xFF92400E) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "عرض الأصناف والمكونات المحفوظة في قائمة المفضلة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    color = if (starredCount > 0) Color(0xFFFDE68A) else Color(0xFFE2E8F0),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = starredCount.toString(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (starredCount > 0) Color(0xFF92400E) else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // تبويب طبيعة التعامل والخصائص (بطاقات موحدة التصميم)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. خيار الشراء (Purchaseable)
                        Surface(
                            onClick = { purchaseableOnly = !purchaseableOnly },
                            shape = RoundedCornerShape(14.dp),
                            color = if (purchaseableOnly) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (purchaseableOnly) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = purchaseableOnly,
                                    onCheckedChange = { purchaseableOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "🛒 أصناف قابلة للشراء (Purchaseable)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (purchaseableOnly) Color(0xFF4338CA) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "تحديد الأصناف والمكونات المتاحة للشراء واستلامها من الموردين",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    color = if (purchaseableOnly) Color(0xFFC7D2FE) else Color(0xFFE2E8F0),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = purchaseableCount.toString(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (purchaseableOnly) Color(0xFF3730A3) else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        // 2. خيار البيع (Salable)
                        Surface(
                            onClick = { salableOnly = !salableOnly },
                            shape = RoundedCornerShape(14.dp),
                            color = if (salableOnly) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (salableOnly) Color(0xFF10B981) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = salableOnly,
                                    onCheckedChange = { salableOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF059669))
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "🏷️ أصناف قابلة للبيع (Salable)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (salableOnly) Color(0xFF047857) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "تحديد المنتجات والأصناف المتاحة للبيع والتصدير للعملاء",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    color = if (salableOnly) Color(0xFFA7F3D0) else Color(0xFFE2E8F0),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = salableCount.toString(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (salableOnly) Color(0xFF065F46) else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        // 3. خيار المكونات الفرعية (Component)
                        Surface(
                            onClick = { componentOnly = !componentOnly },
                            shape = RoundedCornerShape(14.dp),
                            color = if (componentOnly) Color(0xFFFFF7ED) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (componentOnly) Color(0xFFF97316) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = componentOnly,
                                    onCheckedChange = { componentOnly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEA580C))
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "⚙️ مكونات فرعية (Component)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (componentOnly) Color(0xFFC2410C) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "تحديد القطع والأجزاء التي تدخل كعناصر داخلية في التجميعات",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    color = if (componentOnly) Color(0xFFFFEDD5) else Color(0xFFE2E8F0),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = componentCount.toString(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (componentOnly) Color(0xFF9A3412) else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer Section
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onApply(
                                selectedCatIdsState,
                                lowStockOnly,
                                assemblyOnly,
                                componentOnly,
                                purchaseableOnly,
                                salableOnly,
                                starredOnly
                            )
                        },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text(
                            text = "تطبيق التصفية (عرض $calculatedCount قطعة)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}


