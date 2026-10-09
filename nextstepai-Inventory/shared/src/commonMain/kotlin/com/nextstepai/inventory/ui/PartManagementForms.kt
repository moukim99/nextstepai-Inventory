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

/**
 * صفحة منبثقة (Bottom Sheet) ناعمة لاستلام وإضافة كمية مخزنية سريعة لقطعة محددة
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddStockBottomSheet(
    part: Part,
    locations: List<StockLocation> = emptyList(),
    users: List<AppUser> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        partId: Long,
        locationId: Long?,
        quantity: Double,
        packaging: String,
        referenceDoc: String,
        notes: String,
        receiptDate: String,
        receivedByUserId: Long?,
        capturedDocPath: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 1. Initial State Setup
    var quantityText by remember { mutableStateOf("10.0") }
    var selectedLocationId by remember { mutableStateOf<Long?>(part.defaultLocationId ?: locations.firstOrNull()?.id) }
    var packagingCode by remember { mutableStateOf("Box") }
    var isPackagingSheetOpen by remember { mutableStateOf(false) }
    var referenceDocText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val packagingOptionsList = remember {
        mutableStateListOf(
            StockPackagingOption("Reel", "Reel (بكرة)", "شريط دائري تُلف عليه العناصر السطحية (SMD) للالتقاط الآلي"),
            StockPackagingOption("Cut Tape", "Cut Tape (شريط مقصوص)", "أجزاء مقصوصة من البكرة للكميات الصغيرة أو العينات"),
            StockPackagingOption("Tube / Stick", "Tube / Stick (أنبوب بلاستيكي)", "أنابيب صلبة لحماية الدوائر المتكاملة (ICs) ومنع انثناء الأرجل"),
            StockPackagingOption("Tray", "Tray (صينية واقية)", "صوانٍ مقسمة لحمل المعالجات والشرائح الكبيرة الحساسة والتفريغ الكهروستاتيكي"),
            StockPackagingOption("Anti-Static Bag", "Anti-Static Bag (كيس مضاد للكهرباء)", "لحفظ الوحدات والمكونات المنفصلة وحمايتها"),
            StockPackagingOption("Box", "Box / Carton (صندوق / كرتونة)", "التغليف القياسي لتخزين البضائع والمستشعرات والمجموعات الجاهزة"),
            StockPackagingOption("Bulk / Loose", "Bulk / Loose (سائب)", "قطع غير مغلفة فردياً، مثل البراغي، الصواميل، أو الأسلاك"),
            StockPackagingOption("Pallet", "Pallet (منصة نقالة)", "للشحنات الكبيرة والحاويات عند تخزين عدد كبير من الكراتين معاً")
        )
    }

    val selectedPackagingOpt = remember(packagingCode) {
        packagingOptionsList.find { it.code.equals(packagingCode, ignoreCase = true) }
            ?: packagingOptionsList.find { it.labelAr.contains(packagingCode, ignoreCase = true) }
    }
    val displayPackagingText = selectedPackagingOpt?.labelAr ?: packagingCode.ifBlank { "Box / Carton (صندوق / كرتونة)" }

    // Date & Time Picker Setup (Default: current local date & time)
    val nowDateTime = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    val initialDateStr = "${nowDateTime.year}-${nowDateTime.monthNumber.toString().padStart(2, '0')}-${nowDateTime.dayOfMonth.toString().padStart(2, '0')}"
    val initialTimeStr = "${nowDateTime.hour.toString().padStart(2, '0')}:${nowDateTime.minute.toString().padStart(2, '0')}"
    var receiptDateTimeText by remember { mutableStateOf("$initialDateStr $initialTimeStr") }

    var selectedUser by remember(users) {
        mutableStateOf(users.firstOrNull() ?: AppUser("usr-004", "مدير المستودع والخدمات اللوجستية", "مدير المستودع"))
    }

    // Modal & Picker States
    var isSelectLocationSheetOpen by remember { mutableStateOf(false) }
    var isSelectUserSheetOpen by remember { mutableStateOf(false) }
    var showLocationScanner by remember { mutableStateOf(false) }

    // Two-Stage Date + Time Picker States
    var showReceiptDatePicker by remember { mutableStateOf(false) }
    var showReceiptTimePicker by remember { mutableStateOf(false) }
    var pendingSelectedDatePart by remember { mutableStateOf(initialDateStr) }

    // Ref Doc Scanners & Camera Capture State
    var showRefDocBarcodeScanner by remember { mutableStateOf(false) }
    var capturedDocPath by remember { mutableStateOf<String?>(null) }

    // Platform Camera & File Picker Launchers (Same engine as Add Part)
    val platformPickerLaunchers = rememberPlatformPickerLaunchers(
        onImageCaptured = { path -> capturedDocPath = path },
        onImagePicked = { path -> capturedDocPath = path },
        onFilePicked = { _ -> }
    )

    val selectedLocation = remember(selectedLocationId, locations) {
        locations.find { it.id == selectedLocationId }
    }

    // Validation
    val isFormValid = remember(quantityText) {
        val qty = quantityText.toDoubleOrNull() ?: 0.0
        qty > 0.0
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
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "استلام مخزون وبضاعة جديدة",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "تأكيد وتوثيق الشحنة بجدول Stock Items والتتبع آلياً",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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

            // Scrollable Content Column
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Part Details Summary Card
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = part.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "IPN: ${part.ipn.ifBlank { "-" }} • الرصيد الحالي: ${part.availableStock} ${part.units}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }

                // 2. Packaging Type & Quantity Row (Swapped & Height Matched to 56.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Packaging Type Selector Field (Height Matched 56.dp)
                    Surface(
                        onClick = { isPackagingSheetOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "نوع التغليف والتعبئة",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = displayPackagingText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5)
                            )
                        }
                    }

                    // 2. Quantity Input Field (Height 56.dp aligned)
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("الكمية المستلمة (${part.units}) *") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // 3. Physical Storage Location Selection with Separate Standalone QR Scanner Button
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "موقع التخزين المادي (المستودع / الرف) * :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Location Card Selector
                        Surface(
                            onClick = { isSelectLocationSheetOpen = true },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedLocation != null) Color(0xFFF8FAFC) else Color.White,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selectedLocation != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFEEF2FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = selectedLocation?.name ?: "بدون موقع تخزين محدد",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = selectedLocation?.description?.ifBlank { "اضغط لاختيار رف المستودع" } ?: "مكان إيداع الشحنة في المستودع",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Text(
                                    text = if (selectedLocation != null) "تغيير" else "اختر ▾",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF4F46E5)
                                )
                            }
                        }

                        // Separate Standalone Location QR Scanner Button Box
                        Surface(
                            onClick = { showLocationScanner = true },
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
                                    contentDescription = "مسح موقع QR",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Receipt Date/Time & Responsible Person Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Two-Stage Receipt Date & Time Field
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = receiptDateTimeText,
                            onValueChange = { receiptDateTimeText = it },
                            readOnly = true,
                            label = { Text("تاريخ ووقت الاستلام *") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showReceiptDatePicker = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = "اختيار التاريخ والوقت",
                                        tint = Color(0xFF4F46E5)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showReceiptDatePicker = true }
                        )
                    }

                    // Responsible Person Selector Field
                    Surface(
                        onClick = { isSelectUserSheetOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "المسؤول / المستلم *",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = selectedUser?.name ?: "اختر المستلم",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Stage 1: DatePicker Dialog
                if (showReceiptDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds()
                    )
                    DatePickerDialog(
                        onDismissRequest = { showReceiptDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val instant = Instant.fromEpochMilliseconds(millis)
                                    val dateTime = instant.toLocalDateTime(TimeZone.UTC)
                                    val year = dateTime.year
                                    val month = dateTime.monthNumber.toString().padStart(2, '0')
                                    val day = dateTime.dayOfMonth.toString().padStart(2, '0')
                                    pendingSelectedDatePart = "$year-$month-$day"
                                }
                                showReceiptDatePicker = false
                                showReceiptTimePicker = true
                            }) {
                                Text("التالي: اختيار الوقت ➔", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showReceiptDatePicker = false }) {
                                Text("إلغاء")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                // Stage 2: TimePicker Dialog
                if (showReceiptTimePicker) {
                    val timePickerState = rememberTimePickerState(
                        initialHour = nowDateTime.hour,
                        initialMinute = nowDateTime.minute,
                        is24Hour = true
                    )
                    AlertDialog(
                        onDismissRequest = { showReceiptTimePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val hh = timePickerState.hour.toString().padStart(2, '0')
                                val mm = timePickerState.minute.toString().padStart(2, '0')
                                receiptDateTimeText = "$pendingSelectedDatePart $hh:$mm"
                                showReceiptTimePicker = false
                            }) {
                                Text("تأكيد التاريخ والوقت", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showReceiptTimePicker = false }) {
                                Text("إلغاء")
                            }
                        },
                        text = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                TimePicker(state = timePickerState)
                            }
                        }
                    )
                }

                // 5. Invoice / PO / Tracking Ref Field with Separate Standalone Barcode Scanner Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = referenceDocText,
                        onValueChange = { referenceDocText = it },
                        label = {
                            Text(
                                text = "مرجع الشحنة / الفاتورة (اختياري)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = { Text("مثال: INV-2025-001 أو PO-8840") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF64748B)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    // Separate Standalone Barcode Scanner Button Box
                    Surface(
                        onClick = { showRefDocBarcodeScanner = true },
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
                                contentDescription = "مسح باركود الشحنة",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 6. Dedicated Document Photography & Attachment Section (Camera + Gallery - Same as Add Part)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "تصوير وتوثيق مستند الاستلام المادي :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Camera Launcher Button (Same engine & design as Add Part)
                        Surface(
                            onClick = { platformPickerLaunchers.launchCamera() },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFE4E6),
                            border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "الكاميرا",
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "التقاط صورة (كاميرا)",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF9F1239)
                                        )
                                    )
                                    Text(
                                        text = "تصوير الفاتورة / البوليصة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                                        color = Color(0xFFBE123C)
                                    )
                                }
                            }
                        }

                        // 2. Gallery Launcher Button
                        Surface(
                            onClick = { platformPickerLaunchers.launchGalleryPicker() },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "معرض الصور",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "اختيار من المعرض",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF92400E)
                                        )
                                    )
                                    Text(
                                        text = "رفع صورة محفوظة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }
                    }

                    if (platformPickerLaunchers.isLoading) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF2563EB)
                                )
                                Text(
                                    text = "جاري معالجة وحفظ صورة مستند الشحنة...",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1E40AF))
                                )
                            }
                        }
                    }

                    // Display attached photo card if captured or picked
                    if (!capturedDocPath.isNullOrBlank()) {
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFD1FAE5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "تم إرفاق مستند الشحنة بنجاح",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            ),
                                            color = Color(0xFF065F46)
                                        )
                                        Text(
                                            text = capturedDocPath!!.substringAfterLast('/'),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                            color = Color(0xFF047857),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { capturedDocPath = null },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "حذف الصورة",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 7. Shipment Notes (Optional)
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("ملاحظات الشحنة") },
                    placeholder = { Text("أي ملاحظات ميدانية عن الشحنة...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // Action Buttons (Fixed at bottom)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(Res.string.cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = {
                        val qty = quantityText.toDoubleOrNull() ?: 0.0
                        val userIdVal = selectedUser?.uuid?.removePrefix("usr-")?.toLongOrNull() ?: 1L
                        if (isFormValid) {
                            onConfirm(
                                part.id,
                                selectedLocationId,
                                qty,
                                displayPackagingText,
                                referenceDocText,
                                notesText,
                                receiptDateTimeText,
                                userIdVal,
                                capturedDocPath
                            )
                            onDismiss()
                        }
                    },
                    enabled = isFormValid,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(
                        "تأكيد استلام الشحنة",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    // Packaging Selection Bottom Sheet
    if (isPackagingSheetOpen) {
        PackagingSelectionBottomSheet(
            selectedPackagingCode = packagingCode,
            packagingOptionsList = packagingOptionsList,
            onDismiss = { isPackagingSheetOpen = false },
            onPackagingSelected = { opt ->
                packagingCode = opt.code
                isPackagingSheetOpen = false
            },
            onAddPackagingOption = { opt ->
                packagingOptionsList.add(opt)
            },
            onDeletePackagingOption = { opt ->
                packagingOptionsList.remove(opt)
            }
        )
    }

    // Location Scanner Bottom Sheet
    if (showLocationScanner) {
        LocationBarcodeScannerBottomSheet(
            locations = locations,
            onDismiss = { showLocationScanner = false },
            onBarcodeScanned = { payload ->
                val matchedLoc = locations.find {
                    it.id.toString() == payload ||
                    it.name.contains(payload, ignoreCase = true) ||
                    it.description.contains(payload, ignoreCase = true) ||
                    it.uuid == payload
                }
                if (matchedLoc != null) {
                    selectedLocationId = matchedLoc.id
                }
                showLocationScanner = false
            }
        )
    }

    // Ref Doc Barcode Scanner
    if (showRefDocBarcodeScanner) {
        LocationBarcodeScannerBottomSheet(
            onDismiss = { showRefDocBarcodeScanner = false },
            onBarcodeScanned = { barcode ->
                if (barcode.isNotBlank()) {
                    referenceDocText = barcode
                }
                showRefDocBarcodeScanner = false
            }
        )
    }

    if (isSelectLocationSheetOpen) {
        SelectLocationBottomSheet(
            locations = locations,
            selectedLocationId = selectedLocationId,
            onDismiss = { isSelectLocationSheetOpen = false },
            onSelect = { selectedLocationId = it?.id }
        )
    }

    if (isSelectUserSheetOpen) {
        SelectUserBottomSheet(
            users = if (users.isNotEmpty()) users else listOf(selectedUser),
            selectedUserUuid = selectedUser?.uuid,
            onDismiss = { isSelectUserSheetOpen = false },
            onSelect = { selectedUser = it }
        )
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddPartBottomSheet(
    partToEdit: Part? = null,
    categories: List<PartCategory> = emptyList(),
    locations: List<StockLocation> = emptyList(),
    templateParts: List<Part> = emptyList(),
    allParts: List<Part> = emptyList(),
    existingAttachments: List<PartAttachment> = emptyList(),
    onCreateCategory: ((name: String, description: String) -> PartCategory)? = null,
    onDeleteCategory: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit,
    onUpdate: ((Part, List<PendingAttachment>) -> Unit)? = null,
    onConfirm: (
        name: String,
        ipn: String,
        desc: String,
        catId: Long?,
        units: String,
        assembly: Boolean,
        component: Boolean,
        isTemplate: Boolean,
        variantOf: Long?,
        minimumStock: Double,
        maximumStock: Double?,
        revision: String,
        keywords: String,
        trackable: Boolean,
        purchaseable: Boolean,
        salable: Boolean,
        virtual: Boolean,
        defaultLocationId: Long?,
        defaultExpiryDays: Int?,
        pendingAttachments: List<PendingAttachment>,
        active: Boolean,
        locked: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // المستوى الأول: الحقول الأساسية الظاهرة والمباشرة الستة
    var name by remember(partToEdit) { mutableStateOf(partToEdit?.name ?: "") }
    var selectedCatId by remember(partToEdit) { mutableStateOf(partToEdit?.categoryId) }

    val ipnShakeController = remember { ShakeController() }
    var generatedIpn by remember(partToEdit, selectedCatId) {
        mutableStateOf(
            if (partToEdit != null && partToEdit.ipn.isNotBlank()) partToEdit.ipn
            else SmartIpnGenerator.generateNextIpn(selectedCatId, allParts, categories)
        )
    }
    var manualIpnText by remember(partToEdit) { mutableStateOf("") }
    var isManuallyEditedIpn by remember { mutableStateOf(false) }
    var isIpnConfirmed by remember { mutableStateOf(partToEdit != null && partToEdit.ipn.isNotBlank()) }
    var hasDuplicateIpnError by remember { mutableStateOf(false) }

    val ipn = if (isManuallyEditedIpn) manualIpnText else generatedIpn
    var units by remember(partToEdit) { mutableStateOf(partToEdit?.units ?: "pcs") }
    var minimumStockText by remember(partToEdit) { mutableStateOf(partToEdit?.minimumStock?.let { if (it == 0.0) "0" else it.toString() } ?: "0") }
    var lowStockAlertsEnabled by remember(partToEdit) { mutableStateOf(true) }

    // طبيعة التعامل والتصنيع: دعم اختيار أكثر من خيار مع الوضع الافتراضي لعدم الاختيار عند إضافة قطعة جديدة
    var isComponentSelected by remember(partToEdit) { mutableStateOf(partToEdit?.component ?: false) }
    var isAssemblySelected by remember(partToEdit) { mutableStateOf(partToEdit?.assembly ?: false) }
    var isSalableSelected by remember(partToEdit) { mutableStateOf(partToEdit?.salable ?: false) }
    var isSelectNatureSheetOpen by remember { mutableStateOf(false) }

    // المستوى الثاني: قسم فرعي مطوي بعنوان "خيارات متقدمة وهندسية" (غير مفعل ومطوي افتراضياً)
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var description by remember(partToEdit) { mutableStateOf(partToEdit?.description ?: "") }
    var maximumStockText by remember(partToEdit) { mutableStateOf(partToEdit?.maximumStock?.toString() ?: "") }
    var revision by remember(partToEdit) { mutableStateOf(partToEdit?.revision ?: "") }
    var keywords by remember(partToEdit) { mutableStateOf(partToEdit?.keywords ?: "") }
    var selectedDefaultLocId by remember(partToEdit) { mutableStateOf(partToEdit?.defaultLocationId) }
    var defaultExpiryDaysText by remember(partToEdit) { mutableStateOf(partToEdit?.defaultExpiryDays?.toString() ?: "") }
    var isTemplate by remember(partToEdit) { mutableStateOf(partToEdit?.isTemplate ?: false) }
    var virtual by remember(partToEdit) { mutableStateOf(partToEdit?.virtual ?: false) }
    var selectedVariantOfId by remember(partToEdit) { mutableStateOf(partToEdit?.variantOfId) }
    var isStandaloneSelected by remember(partToEdit) {
        mutableStateOf(partToEdit != null && !partToEdit.isTemplate && partToEdit.variantOfId == null)
    }

    val pendingAttachments = remember(partToEdit, existingAttachments) {
        val list = mutableStateListOf<PendingAttachment>()
        if (partToEdit != null) {
            if (!partToEdit.imageUrl.isNullOrBlank()) {
                list.add(
                    PendingAttachment(
                        type = AttachmentType.IMAGE,
                        pathOrUrl = partToEdit.imageUrl!!,
                        label = "صورة الصنف: ${partToEdit.imageUrl!!.substringAfterLast('/')}"
                    )
                )
            }
            if (partToEdit.link.isNotBlank()) {
                list.add(
                    PendingAttachment(
                        type = AttachmentType.LINK,
                        pathOrUrl = partToEdit.link,
                        label = partToEdit.link
                    )
                )
            }
            existingAttachments.forEach { att ->
                if (!att.attachment.isNullOrBlank()) {
                    val isImage = att.attachment!!.contains(".png", true) ||
                            att.attachment!!.contains(".jpg", true) ||
                            att.attachment!!.contains(".jpeg", true) ||
                            att.attachment!!.contains(".webp", true) ||
                            att.attachment!!.contains(".gif", true)
                    val attType = if (isImage) AttachmentType.IMAGE else AttachmentType.DOCUMENT
                    if (list.none { it.pathOrUrl == att.attachment && it.type == attType }) {
                        list.add(
                            PendingAttachment(
                                id = att.id,
                                type = attType,
                                pathOrUrl = att.attachment!!,
                                label = att.comment.ifBlank { att.attachment!!.substringAfterLast('/') }
                            )
                        )
                    }
                }
                if (!att.link.isNullOrBlank()) {
                    if (list.none { it.pathOrUrl == att.link && it.type == AttachmentType.LINK }) {
                        list.add(
                            PendingAttachment(
                                id = att.id,
                                type = AttachmentType.LINK,
                                pathOrUrl = att.link!!,
                                label = att.comment.ifBlank { att.link!! }
                            )
                        )
                    }
                }
            }
        }
        list
    }

    val platformPickerLaunchers = rememberPlatformPickerLaunchers(
        onImageCaptured = { path ->
            pendingAttachments.add(
                PendingAttachment(
                    type = AttachmentType.IMAGE,
                    pathOrUrl = path,
                    label = "صورة كاميرا: ${path.substringAfterLast('/')}"
                )
            )
        },
        onImagePicked = { path ->
            pendingAttachments.add(
                PendingAttachment(
                    type = AttachmentType.IMAGE,
                    pathOrUrl = path,
                    label = "صورة معرض: ${path.substringAfterLast('/')}"
                )
            )
        },
        onFilePicked = { path ->
            pendingAttachments.add(
                PendingAttachment(
                    type = AttachmentType.DOCUMENT,
                    pathOrUrl = path,
                    label = "مستند: ${path.substringAfterLast('/')}"
                )
            )
        }
    )

    var isSelectCategorySheetOpen by remember { mutableStateOf(false) }
    var isQuickAddCategoryOpen by remember { mutableStateOf(false) }
    var isSelectTemplateSheetOpen by remember { mutableStateOf(false) }
    var isSelectUnitSheetOpen by remember { mutableStateOf(false) }
    var isSelectLocationSheetOpen by remember { mutableStateOf(false) }
    var isLinkInputDialogOpen by remember { mutableStateOf(false) }
    var isConfirmUpdateDialogOpen by remember { mutableStateOf(false) }

    // حساب ذكي لتقصي وتتبع أي تغيير فعلي في البيانات الحالية عند التعديل
    val hasChanges = remember(
        partToEdit, name, ipn, description, selectedCatId, units,
        isComponentSelected, isAssemblySelected, isSalableSelected,
        minimumStockText, maximumStockText, revision, keywords, selectedDefaultLocId,
        defaultExpiryDaysText, pendingAttachments.size, isTemplate, virtual, selectedVariantOfId
    ) {
        if (partToEdit == null) true
        else {
            val minStock = minimumStockText.toDoubleOrNull() ?: 0.0
            val maxStock = maximumStockText.toDoubleOrNull()
            val expiryDays = defaultExpiryDaysText.toIntOrNull()

            name.trim() != partToEdit.name.trim() ||
                    ipn.trim() != partToEdit.ipn.trim() ||
                    description.trim() != partToEdit.description.trim() ||
                    selectedCatId != partToEdit.categoryId ||
                    units.trim().ifBlank { "pcs" } != partToEdit.units.trim() ||
                    isComponentSelected != partToEdit.component ||
                    isAssemblySelected != partToEdit.assembly ||
                    isSalableSelected != partToEdit.salable ||
                    minStock != partToEdit.minimumStock ||
                    maxStock != partToEdit.maximumStock ||
                    revision.trim() != partToEdit.revision.trim() ||
                    keywords.trim() != partToEdit.keywords.trim() ||
                    selectedDefaultLocId != partToEdit.defaultLocationId ||
                    expiryDays != partToEdit.defaultExpiryDays ||
                    isTemplate != partToEdit.isTemplate ||
                    virtual != partToEdit.virtual ||
                    selectedVariantOfId != partToEdit.variantOfId
        }
    }

    val isFormValid = remember(name, ipn, hasChanges) {
        name.trim().isNotBlank() && ipn.trim().isNotBlank() && hasChanges
    }

    val selectedCategory = remember(selectedCatId, categories) {
        categories.find { it.id == selectedCatId }
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (partToEdit != null) Icons.Default.Edit else Icons.Default.AddBox,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    val completenessScore = remember(
                        name, ipn, selectedCatId, units, minimumStockText,
                        isComponentSelected, isAssemblySelected, isSalableSelected,
                        selectedDefaultLocId, isTemplate, selectedVariantOfId,
                        description, keywords, pendingAttachments.size
                    ) {
                        calculatePartCompletenessScore(
                            name = name,
                            ipn = ipn,
                            categoryId = selectedCatId,
                            units = units,
                            minimumStock = minimumStockText.toDoubleOrNull() ?: 0.0,
                            hasNature = isComponentSelected || isAssemblySelected || isSalableSelected,
                            defaultLocationId = selectedDefaultLocId,
                            isTemplateOrVariant = isTemplate || selectedVariantOfId != null,
                            hasAdvancedData = description.isNotBlank() || keywords.isNotBlank() || pendingAttachments.isNotEmpty()
                        )
                    }

                    Column {
                        Text(
                            text = if (partToEdit != null) "تعديل بيانات القطعة - ${partToEdit.name}" else stringResource(Res.string.add_new_part),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Completeness Ring Badge
                        val colorTone = getCompletenessTone(completenessScore)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PartCircularCompletionBadge(
                                percentage = completenessScore,
                                colorTone = colorTone,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "اكتمال البيانات: $completenessScore%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = when (colorTone) {
                                    CompletenessTone.RED -> Color(0xFF991B1B)
                                    CompletenessTone.ORANGE -> Color(0xFF92400E)
                                    CompletenessTone.GREEN -> Color(0xFF065F46)
                                }
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

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Form Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. اسم القطعة + 2. التصنيف (في نفس السطر)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // حقل اسم القطعة - إلزامي
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم القطعة *") },
                        placeholder = { Text("مثال: مقاومة 10K") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    // حقل التصنيف (Category)
                    Surface(
                        onClick = { isSelectCategorySheetOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedCategory != null) Color(0xFFF8FAFC) else Color.White,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selectedCategory != null) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEEF2FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.Center) {
                                    Text(
                                        text = "التصنيف",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    )
                                    Text(
                                        text = selectedCategory?.name ?: "عام (اختر التصنيف)",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "اختر التصنيف",
                                tint = Color(0xFF4F46E5)
                            )
                        }
                    }
                }

                // 3. رقم القطعة الداخلي الذكي (Smart IPN Field)
                SmartIpnField(
                    generatedIpn = generatedIpn,
                    manualIpn = manualIpnText,
                    onIpnChange = { newIpn ->
                        manualIpnText = newIpn
                        isManuallyEditedIpn = true
                        hasDuplicateIpnError = false
                    },
                    isConfirmed = isIpnConfirmed,
                    onConfirmToggle = {
                        isIpnConfirmed = !isIpnConfirmed
                    },
                    isManuallyEdited = isManuallyEditedIpn,
                    onReset = {
                        manualIpnText = ""
                        isManuallyEditedIpn = false
                        isIpnConfirmed = false
                        hasDuplicateIpnError = false
                    },
                    onGenerate = {
                        generatedIpn = SmartIpnGenerator.generateNextIpn(selectedCatId, allParts, categories)
                        manualIpnText = ""
                        isManuallyEditedIpn = false
                        isIpnConfirmed = true
                        hasDuplicateIpnError = false
                    },
                    hasDuplicateError = hasDuplicateIpnError,
                    shakeController = ipnShakeController,
                    label = "رقم القطعة الداخلي (IPN) *",
                    placeholder = "مثال: ELEC-RES-0001"
                )

                // 4. وحدة القياس (زر اختيار الهدف بدون خصائص حقل النص) + 5. الحد الأدنى للمخزون
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { isSelectUnitSheetOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "وحدة القياس",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF64748B)
                                    )
                                )
                                Text(
                                    text = units.ifBlank { "pcs" },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "اختر الوحدة",
                                tint = Color(0xFF4F46E5)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = minimumStockText,
                        onValueChange = { minimumStockText = it },
                        label = { Text("الحد الأدنى للمخزون") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }

                // 6. طبيعة التعامل والتصنيع (نوع الصنف) - حقل يفتح نافذة منبثقة للخيارات الثلاثة
                val selectedNaturesList = remember(isComponentSelected, isAssemblySelected, isSalableSelected) {
                    buildList {
                        if (isComponentSelected) add("مكوّن أولي خام (Component)")
                        if (isAssemblySelected) add("تجميع داخلي (Assembly)")
                        if (isSalableSelected) add("منتج تجاري جاهز للبيع (Salable)")
                    }
                }

                Surface(
                    onClick = { isSelectNatureSheetOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedNaturesList.isNotEmpty()) Color(0xFFF8FAFC) else Color.White,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (selectedNaturesList.isNotEmpty()) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
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
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selectedNaturesList.isNotEmpty()) Color(0xFFEEF2FF) else Color(0xFFF1F5F9))
                                    .border(1.dp, if (selectedNaturesList.isNotEmpty()) Color(0xFFC7D2FE) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = if (selectedNaturesList.isNotEmpty()) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = if (selectedNaturesList.isNotEmpty()) selectedNaturesList.joinToString("، ") else "طبيعة التعامل والتصنيع (نوع الصنف)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (selectedNaturesList.isNotEmpty()) Color(0xFF0F172A) else Color(0xFF475569),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (selectedNaturesList.isNotEmpty()) "طبيعة التعامل والتصنيع: اضغط للتعديل" else "اضغط لاختيار طبيعة تصنيع وتعامل الصنف",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            color = if (selectedNaturesList.isNotEmpty()) Color(0xFFEEF2FF) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (selectedNaturesList.isNotEmpty()) Color(0xFFC7D2FE) else Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = if (selectedNaturesList.isNotEmpty()) "تغيير ▾" else "اختر ▾",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedNaturesList.isNotEmpty()) Color(0xFF4F46E5) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // 7. اختيار الصنف القالب/الأصل (variantOfId / isTemplate)
                val selectedTemplate = remember(selectedVariantOfId, templateParts) {
                    templateParts.find { it.id == selectedVariantOfId }
                }
                val isTemplateCardActive = isTemplate || selectedTemplate != null || isStandaloneSelected

                Surface(
                    onClick = { isSelectTemplateSheetOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isTemplateCardActive) Color(0xFFF8FAFC) else Color.White,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (isTemplateCardActive) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
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
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isTemplateCardActive) Color(0xFFEEF2FF) else Color(0xFFF1F5F9))
                                    .border(1.dp, if (isTemplateCardActive) Color(0xFFC7D2FE) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = if (isTemplateCardActive) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = when {
                                        isTemplate -> "قالب أصل"
                                        selectedTemplate != null -> "مشتق عن: ${selectedTemplate.name}"
                                        isStandaloneSelected -> "صنف أصلي مستقل"
                                        else -> "القالب / صنف مشتق"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isTemplateCardActive) Color(0xFF0F172A) else Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        isTemplate -> "مُعتمد كقالب تجريدي تُشتق منه متغيرات فرعية"
                                        selectedTemplate != null -> "صنف فرعي مشتق عن قالب أصل (IPN: ${selectedTemplate.ipn.ifBlank { "-" }})"
                                        isStandaloneSelected -> "قطعة قائمة بذاتها وغير مشتقة من قالب آخر"
                                        else -> "اضغط لاختيار صنف أصلي مستقل، قالب أصل، أو صنف مشتق"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            color = if (isTemplateCardActive) Color(0xFFEEF2FF) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isTemplateCardActive) Color(0xFFC7D2FE) else Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = if (isTemplateCardActive) "تغيير ▾" else "اختر ▾",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isTemplateCardActive) Color(0xFF4F46E5) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // 8. موقع التخزين الافتراضي (defaultLocationId) - الخيارات الأساسية
                if (locations.isNotEmpty()) {
                    val selectedLocation = remember(selectedDefaultLocId, locations) {
                        locations.find { it.id == selectedDefaultLocId }
                    }

                    Surface(
                        onClick = { isSelectLocationSheetOpen = true },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selectedLocation != null) Color(0xFFF8FAFC) else Color.White,
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = if (selectedLocation != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
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
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selectedLocation != null) Color(0xFFEEF2FF) else Color(0xFFF1F5F9))
                                        .border(1.dp, if (selectedLocation != null) Color(0xFFC7D2FE) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (selectedLocation != null) Color(0xFF4F46E5) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = selectedLocation?.name ?: "موقع التخزين الافتراضي",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (selectedLocation != null) Color(0xFF0F172A) else Color(0xFF475569),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = selectedLocation?.description?.ifBlank { "موقع التخزين الافتراضي" } ?: "اضغط لاختيار موقع التخزين الافتراضي للقطعة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Surface(
                                color = if (selectedLocation != null) Color(0xFFEEF2FF) else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selectedLocation != null) Color(0xFFC7D2FE) else Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = if (selectedLocation != null) "تغيير ▾" else "اختر ▾",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedLocation != null) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // =========================================================
                // المستوى الثاني: قسم فرعي مطوي بعنوان "خيارات متقدمة وهندسية"
                // =========================================================
                Surface(
                    onClick = { isAdvancedExpanded = !isAdvancedExpanded },
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "خيارات متقدمة وهندسية",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "القوالب المشتقة، الصور، الوسوم، الموقع، والإصدار",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5)
                        )
                    }
                }

                if (isAdvancedExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. المرفقات والمستندات السريعة (كاميرا، معرض الصور، ملفات، رابط) - في أعلى القسم المتقدم
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "إرفاق الصور والمستندات والروابط :",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF0F172A)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. الكاميرا
                                Surface(
                                    onClick = { platformPickerLaunchers.launchCamera() },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFFE4E6),
                                    border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(70.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = "الكاميرا",
                                            tint = Color(0xFFE11D48),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "الكاميرا",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF9F1239)
                                            )
                                        )
                                    }
                                }

                                // 2. معرض الصور
                                Surface(
                                    onClick = { platformPickerLaunchers.launchGalleryPicker() },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFEF3C7),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(70.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoLibrary,
                                            contentDescription = "معرض الصور",
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "معرض الصور",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF92400E)
                                            )
                                        )
                                    }
                                }

                                // 3. الملفات
                                Surface(
                                    onClick = { platformPickerLaunchers.launchFilePicker() },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFE0F2FE),
                                    border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(70.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.InsertDriveFile,
                                            contentDescription = "الملفات",
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "الملفات",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF075985)
                                            )
                                        )
                                    }
                                }

                                // 4. الرابط الخارجي
                                Surface(
                                    onClick = { isLinkInputDialogOpen = true },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFEEF2FF),
                                    border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(70.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = "الرابط",
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "الرابط",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF3730A3)
                                            )
                                        )
                                    }
                                }
                            }

                            // مؤشر تحميل بصري أثناء نسخ معالجة الملفات الكبيرة
                            if (platformPickerLaunchers.isLoading) {
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF2563EB)
                                        )
                                        Text(
                                            text = "جاري معالجة ونسخ الملف للمستندات المحلية...",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF1E40AF)
                                            )
                                        )
                                    }
                                }
                            }

                            // إظهار بطاقة ملخص المرفقات المنفذة
                            if (pendingAttachments.isNotEmpty()) {
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "المرفقات المضافة (${pendingAttachments.size}):",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                            color = Color(0xFF0F172A)
                                        )
                                        pendingAttachments.forEachIndexed { index, att ->
                                            val icon = when (att.type) {
                                                AttachmentType.IMAGE -> Icons.Default.Image
                                                AttachmentType.DOCUMENT -> Icons.Default.InsertDriveFile
                                                AttachmentType.LINK -> Icons.Default.Link
                                            }
                                            val tint = when (att.type) {
                                                AttachmentType.IMAGE -> Color(0xFFE11D48)
                                                AttachmentType.DOCUMENT -> Color(0xFF0284C7)
                                                AttachmentType.LINK -> Color(0xFF4F46E5)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                                                    Text(
                                                        text = att.label,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                        color = Color(0xFF0F172A),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { pendingAttachments.removeAt(index) },
                                                    modifier = Modifier.size(22.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "حذف", tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. الكلمات المفتاحية والوسوم + الاصدار الهندسي في نفس السطر
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = keywords,
                                onValueChange = { keywords = it },
                                label = { Text("الكلمات المفتاحية") },
                                placeholder = { Text("#SMD #0805") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4F46E5),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = revision,
                                onValueChange = { revision = it },
                                label = { Text("الإصدار الهندسي") },
                                placeholder = { Text("مثال: Rev A") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4F46E5),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }

                        // 3. الحد الأقصى للمخزون + أيام الصلاحية الافتراضية
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = maximumStockText,
                                onValueChange = { maximumStockText = it },
                                label = { Text("الحد الأقصى للمخزون") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4F46E5),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )

                            OutlinedTextField(
                                value = defaultExpiryDaysText,
                                onValueChange = { defaultExpiryDaysText = it },
                                label = { Text("أيام الصلاحية الافتراضية") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4F46E5),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }

                        // 4. وصف القطعة التفصيلي (description) - فوق حقل التنبيهات ونقص المخزون مباشرة
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("وصف التفاصيل والمواصفات (description)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        // 9. الخصائص الفنية والتنبيهات المتقدمة (Switches / Toggles)
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "الخصائص الهندسية والتنبيهات:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                    color = Color(0xFF475569)
                                )

                                // 1. lowStockAlertsEnabled (تفعيل تنبيهات نقص المخزون - مفعل افتراضياً)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { lowStockAlertsEnabled = !lowStockAlertsEnabled },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text("تنبيهات نقص المخزون", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("تفعيل التنبيهات التلقائية لهذا الصنف عند الوصول للحد الأدنى", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp), color = Color(0xFF64748B))
                                        }
                                    }
                                    Switch(
                                        checked = lowStockAlertsEnabled,
                                        onCheckedChange = { lowStockAlertsEnabled = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Fixed Bottom Action Buttons Container at bottom of sheet page
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(stringResource(Res.string.cancel), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Button(
                        onClick = {
                            if (isFormValid) {
                                val trimmedIpn = ipn.trim()
                                val isDuplicate = allParts.any { other ->
                                    other.id != partToEdit?.id && other.ipn.trim().equals(trimmedIpn, ignoreCase = true)
                                }

                                if (isDuplicate) {
                                    hasDuplicateIpnError = true
                                    ipnShakeController.trigger()
                                    return@Button
                                }

                                if (partToEdit != null) {
                                    isConfirmUpdateDialogOpen = true
                                } else {
                                    val isComp = isComponentSelected
                                    val isAssy = isAssemblySelected
                                    val isSale = isSalableSelected
                                    val isPurch = isComp || isSale

                                    onConfirm(
                                        name.trim(),
                                        trimmedIpn,
                                        description,
                                        selectedCatId,
                                        units,
                                        isAssy,
                                        isComp,
                                        isTemplate,
                                        selectedVariantOfId,
                                        minimumStockText.toDoubleOrNull() ?: 0.0,
                                        maximumStockText.toDoubleOrNull(),
                                        revision,
                                        keywords,
                                        false,
                                        isPurch,
                                        isSale,
                                        virtual,
                                        selectedDefaultLocId,
                                        defaultExpiryDaysText.toIntOrNull(),
                                        pendingAttachments,
                                        true,
                                        false
                                    )
                                    onDismiss()
                                }
                            }
                        },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (partToEdit != null) "حفظ التعديلات" else stringResource(Res.string.save),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    if (isConfirmUpdateDialogOpen && partToEdit != null) {
        AlertDialog(
            onDismissRequest = { isConfirmUpdateDialogOpen = false },
            title = {
                Text(
                    text = "تأكيد حفظ التعديلات",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "هل أنت تأكد من رغبتك في حفظ وتطبيق التعديلات الجديدة على القطعة \"$name\"؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedIpn = ipn.trim()
                        val isDuplicate = allParts.any { other ->
                            other.id != partToEdit.id && other.ipn.trim().equals(trimmedIpn, ignoreCase = true)
                        }

                        if (isDuplicate) {
                            hasDuplicateIpnError = true
                            ipnShakeController.trigger()
                            isConfirmUpdateDialogOpen = false
                            return@Button
                        }

                        val isComp = isComponentSelected
                        val isAssy = isAssemblySelected
                        val isSale = isSalableSelected
                        val isPurch = isComp || isSale

                        val resolvedImageUrl = pendingAttachments.firstOrNull { it.type == AttachmentType.IMAGE }?.pathOrUrl
                        val resolvedLink = pendingAttachments.firstOrNull { it.type == AttachmentType.LINK }?.pathOrUrl 
                            ?: pendingAttachments.firstOrNull { it.type == AttachmentType.DOCUMENT }?.pathOrUrl 
                            ?: ""

                        val updatedPart = partToEdit.copy(
                            name = name,
                            ipn = trimmedIpn,
                            description = description,
                            categoryId = selectedCatId,
                            units = units.ifBlank { "pcs" },
                            assembly = isAssy,
                            component = isComp,
                            isTemplate = isTemplate,
                            variantOfId = selectedVariantOfId,
                            minimumStock = minimumStockText.toDoubleOrNull() ?: 0.0,
                            maximumStock = maximumStockText.toDoubleOrNull(),
                            revision = revision,
                            keywords = keywords,
                            trackable = false,
                            purchaseable = isPurch,
                            salable = isSale,
                            virtual = virtual,
                            defaultLocationId = selectedDefaultLocId,
                            defaultExpiryDays = defaultExpiryDaysText.toIntOrNull(),
                            link = resolvedLink,
                            imageUrl = resolvedImageUrl
                        )
                        isConfirmUpdateDialogOpen = false
                        onUpdate?.invoke(updatedPart, pendingAttachments)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تأكيد الحفظ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { isConfirmUpdateDialogOpen = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (isSelectNatureSheetOpen) {
        SelectNatureBottomSheet(
            isComponentSelected = isComponentSelected,
            isAssemblySelected = isAssemblySelected,
            isSalableSelected = isSalableSelected,
            onDismiss = { isSelectNatureSheetOpen = false },
            onToggleComponent = { isComponentSelected = !isComponentSelected },
            onToggleAssembly = { isAssemblySelected = !isAssemblySelected },
            onToggleSalable = { isSalableSelected = !isSalableSelected }
        )
    }

    if (isSelectCategorySheetOpen) {
        SelectCategoryBottomSheet(
            categories = categories,
            selectedCatId = selectedCatId,
            onCreateCategory = onCreateCategory,
            onDeleteCategory = onDeleteCategory,
            onDismiss = { isSelectCategorySheetOpen = false },
            onSelect = { selectedCatId = it?.id }
        )
    }

    if (isQuickAddCategoryOpen && onCreateCategory != null) {
        QuickAddCategoryBottomSheet(
            onDismiss = { isQuickAddCategoryOpen = false },
            onConfirm = { name, desc ->
                val created = onCreateCategory.invoke(name, desc)
                selectedCatId = created.id
                isQuickAddCategoryOpen = false
            }
        )
    }

    if (isSelectTemplateSheetOpen) {
        SelectTemplatePartBottomSheet(
            templateParts = templateParts,
            isTemplate = isTemplate,
            isStandaloneSelected = isStandaloneSelected,
            selectedVariantOfId = selectedVariantOfId,
            onDismiss = { isSelectTemplateSheetOpen = false },
            onSelect = { newIsTemplate, newIsStandalone, selectedTpl ->
                isTemplate = newIsTemplate
                isStandaloneSelected = newIsStandalone
                selectedVariantOfId = selectedTpl?.id
                isSelectTemplateSheetOpen = false
            }
        )
    }

    if (isSelectUnitSheetOpen) {
        SelectUnitBottomSheet(
            selectedUnit = units,
            onDismiss = { isSelectUnitSheetOpen = false },
            onSelect = { units = it }
        )
    }

    if (isSelectLocationSheetOpen) {
        SelectLocationBottomSheet(
            locations = locations,
            selectedLocationId = selectedDefaultLocId,
            onDismiss = { isSelectLocationSheetOpen = false },
            onSelect = { selectedDefaultLocId = it?.id }
        )
    }

    if (isLinkInputDialogOpen) {
        var tempLink by remember { mutableStateOf("") }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val sanitizedUrl = when {
            tempLink.trim().startsWith("http://", ignoreCase = true) || 
            tempLink.trim().startsWith("https://", ignoreCase = true) -> tempLink.trim()
            tempLink.trim().isBlank() -> ""
            else -> "https://${tempLink.trim()}"
        }
        val isUrlValid = tempLink.trim().isBlank() || sanitizedUrl.matches(Regex("^(https?://)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$"))

        ModalBottomSheet(
            onDismissRequest = { isLinkInputDialogOpen = false },
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF4F46E5))
                    Text("إدخال رابط التوثيق الخارجي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                OutlinedTextField(
                    value = tempLink,
                    onValueChange = { tempLink = it },
                    label = { Text("رابط كراسة المواصفات أو الموقع (URL)") },
                    placeholder = { Text("https://example.com/datasheet.pdf") },
                    singleLine = true,
                    isError = !isUrlValid && tempLink.isNotBlank(),
                    supportingText = {
                        if (!isUrlValid && tempLink.isNotBlank()) {
                            Text(
                                text = "صيغة الرابط غير صحيحة، يجب أن يحتوي على اسم نطاق صالح (مثل: https://example.com/datasheet.pdf)",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { isLinkInputDialogOpen = false }) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (tempLink.trim().isNotBlank() && isUrlValid) {
                                pendingAttachments.add(
                                    PendingAttachment(
                                        type = AttachmentType.LINK,
                                        pathOrUrl = sanitizedUrl,
                                        label = sanitizedUrl
                                    )
                                )
                            }
                            isLinkInputDialogOpen = false
                        },
                        enabled = isUrlValid,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text("حفظ الرابط", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickAddCategoryBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newCatName by remember { mutableStateOf("") }
    var newCatDesc by remember { mutableStateOf("") }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "إنشاء تصنيف جديد سريع",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "إضافة تصنيف فرعي إلى شجرة الفئات",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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

            OutlinedTextField(
                value = newCatName,
                onValueChange = { newCatName = it },
                label = { Text("اسم التصنيف (Category Name) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF059669),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            OutlinedTextField(
                value = newCatDesc,
                onValueChange = { newCatDesc = it },
                label = { Text("وصف التصنيف (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF059669),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(Res.string.cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            onConfirm(newCatName, newCatDesc)
                            onDismiss()
                        }
                    },
                    enabled = newCatName.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(
                        "حفظ واختيار",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddInternalPriceBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, price: Double, currency: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantityText by remember { mutableStateOf("1.0") }
    var priceText by remember { mutableStateOf("0.0") }
    var currency by remember { mutableStateOf("USD") }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "إضافة شريحة سعرية (PartInternalPrice)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "سعر التكلفة الداخلي حسب كميات التوريد",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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

            OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = { Text("الحد الأدنى للكمية (quantity >= 1) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("سعر الوحدة الواحدة (price) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            var isCurrencyPickerOpen by remember { mutableStateOf(false) }
            CurrencySelectorField(
                selectedCurrencyCode = currency,
                onOpenPicker = { isCurrencyPickerOpen = true },
                label = "العملة (price_currency)"
            )
            if (isCurrencyPickerOpen) {
                CurrencySelectionBottomSheet(
                    selectedCurrencyCode = currency,
                    onDismiss = { isCurrencyPickerOpen = false },
                    onCurrencySelected = { selectedCurr ->
                        currency = selectedCurr.code
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(Res.string.cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = {
                        val q = quantityText.toDoubleOrNull() ?: 1.0
                        val p = priceText.toDoubleOrNull() ?: 0.0
                        if (q >= 1.0 && p > 0.0) {
                            onConfirm(q, p, currency)
                            onDismiss()
                        }
                    },
                    enabled = (quantityText.toDoubleOrNull() ?: 0.0) >= 1.0 && (priceText.toDoubleOrNull() ?: 0.0) > 0.0,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(
                        stringResource(Res.string.save),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddSalePriceBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, price: Double, currency: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantityText by remember { mutableStateOf("1.0") }
    var priceText by remember { mutableStateOf("0.0") }
    var currency by remember { mutableStateOf("USD") }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sell,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "إضافة شريحة سعر بيع للعملاء (PartSalePrice)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "سعر البيع النهائي حسب الكمية المطلوبة",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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

            OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = { Text("الحد الأدنى للكمية (quantity >= 1) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("سعر بيع الوحدة الواحدة للعميل (price) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            var isCurrencyPickerOpen by remember { mutableStateOf(false) }
            CurrencySelectorField(
                selectedCurrencyCode = currency,
                onOpenPicker = { isCurrencyPickerOpen = true },
                label = "العملة (price_currency)"
            )
            if (isCurrencyPickerOpen) {
                CurrencySelectionBottomSheet(
                    selectedCurrencyCode = currency,
                    onDismiss = { isCurrencyPickerOpen = false },
                    onCurrencySelected = { selectedCurr ->
                        currency = selectedCurr.code
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(Res.string.cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = {
                        val q = quantityText.toDoubleOrNull() ?: 1.0
                        val p = priceText.toDoubleOrNull() ?: 0.0
                        if (q >= 1.0 && p > 0.0) {
                            onConfirm(q, p, currency)
                            onDismiss()
                        }
                    },
                    enabled = (quantityText.toDoubleOrNull() ?: 0.0) >= 1.0 && (priceText.toDoubleOrNull() ?: 0.0) > 0.0,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(
                        stringResource(Res.string.save),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddManufacturerPartBottomSheet(
    companies: List<Company>,
    onDismiss: () -> Unit,
    onConfirm: (manufacturerId: Long, mpn: String, description: String, link: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mfgCompanies = remember(companies) { companies.filter { it.isManufacturer } }
    var selectedCompany by remember { mutableStateOf(mfgCompanies.firstOrNull()) }
    var isCompanyPickerOpen by remember { mutableStateOf(false) }
    var mpn by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrecisionManufacturing,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "إضافة قطعة مصنّع (MPN)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "ربط الصنف برقم قطع الشركة المصنعة",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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

            Text("المصنّع المعتمد:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            OutlinedButton(
                onClick = { isCompanyPickerOpen = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedCompany?.name ?: "اختر الشركة المصنعة...", fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = mpn,
                onValueChange = { mpn = it },
                label = { Text("رقم قطعة المصنع (MPN) *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("الوصف الفني") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("رابط كراسة المواصفات (Datasheet)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.cancel), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }

                Button(
                    onClick = {
                        if (selectedCompany != null && mpn.isNotBlank()) {
                            onConfirm(selectedCompany!!.id, mpn, description, link)
                            onDismiss()
                        }
                    },
                    enabled = selectedCompany != null && mpn.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(stringResource(Res.string.save), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    if (isCompanyPickerOpen) {
        SearchableCompanyPickerDialog(
            companies = mfgCompanies,
            selectedCompanyId = selectedCompany?.id,
            title = "اختر الشركة المصنعة",
            onCompanySelected = { selectedCompany = it },
            onDismiss = { isCompanyPickerOpen = false }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddSupplierPartBottomSheet(
    companies: List<Company>,
    mfgParts: List<ManufacturerPart>,
    onDismiss: () -> Unit,
    onConfirm: (supplierId: Long, sku: String, mfgPartId: Long?, description: String, link: String, note: String, packaging: String, packQuantity: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val supCompanies = remember(companies) { companies.filter { it.isSupplier } }
    var selectedCompany by remember { mutableStateOf(supCompanies.firstOrNull()) }
    var isCompanyPickerOpen by remember { mutableStateOf(false) }
    var selectedMfgPartId by remember { mutableStateOf<Long?>(mfgParts.firstOrNull()?.id) }
    var sku by remember { mutableStateOf("") }
    var packaging by remember { mutableStateOf("Box") }
    var packQuantity by remember { mutableStateOf("1") }
    var description by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

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
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "إضافة قطعة مورّد (SKU)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "ربط الصنف ببيانات شركة التوريد والـ SKU",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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

            Text("المورّد المعتمد:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            OutlinedButton(
                onClick = { isCompanyPickerOpen = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedCompany?.name ?: "اختر شركة التوريد...", fontWeight = FontWeight.Bold)
            }

            if (mfgParts.isNotEmpty()) {
                Text("ربط برقم قطع التصنيع (MPN):", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                mfgParts.forEach { mp ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMfgPartId = mp.id }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = selectedMfgPartId == mp.id,
                            onClick = { selectedMfgPartId = mp.id }
                        )
                        Text("MPN: ${mp.mpn} (#${mp.id})", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            OutlinedTextField(
                value = sku,
                onValueChange = { sku = it },
                label = { Text("رمز التوريد (SKU) *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = packaging,
                    onValueChange = { packaging = it },
                    label = { Text("نوع التغليف") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = packQuantity,
                    onValueChange = { packQuantity = it },
                    label = { Text("كمية الحزمة") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("الوصف") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("رابط صفحة الشراء") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("ملاحظة التوريد") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.cancel), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }

                Button(
                    onClick = {
                        if (selectedCompany != null && sku.isNotBlank()) {
                            onConfirm(selectedCompany!!.id, sku, selectedMfgPartId, description, link, note, packaging, packQuantity)
                            onDismiss()
                        }
                    },
                    enabled = selectedCompany != null && sku.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD97706),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(stringResource(Res.string.save), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    if (isCompanyPickerOpen) {
        SearchableCompanyPickerDialog(
            companies = supCompanies,
            selectedCompanyId = selectedCompany?.id,
            title = "اختر شركة التوريد",
            onCompanySelected = { selectedCompany = it },
            onDismiss = { isCompanyPickerOpen = false }
        )
    }
}


