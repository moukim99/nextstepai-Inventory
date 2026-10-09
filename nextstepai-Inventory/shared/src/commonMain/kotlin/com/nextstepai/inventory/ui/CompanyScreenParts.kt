package com.nextstepai.inventory.ui

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import kotlin.time.Clock
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.*
import com.nextstepai.inventory.ui.components.*
import com.nextstepai.inventory.util.DateTimeUtils
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_company
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.companies_count
import nextstepai_inventory.shared.generated.resources.save

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddManufacturerPartBottomSheet(
    allParts: List<Part>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (partId: Long, mpn: String, description: String, link: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedPart by remember { mutableStateOf<Part?>(allParts.firstOrNull()) }
    var isPartPickerOpen by remember { mutableStateOf(false) }
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("إضافة قطعة مصنّع (MPN)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = Color(0xFF0F172A))
                        Text("ربط كود التصنيع بالمخزون الداخلي", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // 2. Scrollable Form Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (errorMessage != null) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // Smart Tip Banner
                Surface(
                    color = Color(0xFFEEF2FF).copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, Color(0xFFE0E7FF)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF4F46E5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💡", fontSize = 12.sp)
                        }
                        Text(
                            text = "يضمن تعيين رقم قطعة المصنّع (MPN) دقة شراء المكونات من المورّدين ومطابقتها التامة للوحات والدوائر الإلكترونية.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                            color = Color(0xFF312E81)
                        )
                    }
                }

                // 1. Internal Part Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("القطعة الداخلية المربوطة *", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        Surface(color = Color(0xFFEEF2FF), shape = RoundedCornerShape(6.dp)) {
                            Text("من القائمة M3", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Color(0xFF4338CA), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    OutlinedButton(
                        onClick = { isPartPickerOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedPart?.let { "${it.name} (${it.ipn.ifBlank { "بدون IPN" }})" } ?: "اختر القطعة الداخلية من القائمة...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedPart != null) Color(0xFF0F172A) else Color(0xFF94A3B8)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B))
                        }
                    }
                }

                // 2. MPN Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("رقم القطعة المصنعية (MPN) *", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        Text("رمز المصنع الرسمي", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = Color(0xFF94A3B8))
                    }

                    OutlinedTextField(
                        value = mpn,
                        onValueChange = { mpn = it },
                        placeholder = { Text("مثال: ESP32-D0WD-V3") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 3. Description Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("وصف مواصفات المصنّع", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("أدخل ملخص مواصفات الشريحة، سعة الذاكرة، التردد...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 4. Datasheet Link Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("رابط مواصفات المنتج (Datasheet)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        Text("اختياري", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = Color(0xFF94A3B8))
                    }

                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        placeholder = { Text("https://espressif.com/docs/datasheet.pdf") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 3. Sticky Actions Footer
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (selectedPart != null && mpn.isNotBlank()) {
                                onConfirm(selectedPart!!.id, mpn, description, link)
                            }
                        },
                        enabled = selectedPart != null && mpn.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("حفظ قطعة المصنّع", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp))
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp), color = Color(0xFF64748B))
                    }
                }
            }
        }
    }

    if (isPartPickerOpen) {
        SearchablePartPickerDialog(
            parts = allParts,
            selectedPartId = selectedPart?.id,
            onPartSelected = { selectedPart = it },
            onDismiss = { isPartPickerOpen = false }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddManufacturerPartParameterDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, value: String, units: String) -> Unit
) {
    var name by remember { mutableStateOf("Voltage") }
    var value by remember { mutableStateOf("5V") }
    var units by remember { mutableStateOf("V") }

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
            Text("إضافة معامل تقني لقطعة المصنّع", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الخاصية / المعامل (Voltage / Resistance) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("القيمة التقنية (5V / 10k) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = units, onValueChange = { units = it }, label = { Text("وحدة القياس (V / Ohm / uF)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("إلغاء") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank() && value.isNotBlank()) onConfirm(name, value, units)
                    },
                    enabled = name.isNotBlank() && value.isNotBlank()
                ) { Text("حفظ المعامل") }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddManufacturerPartAttachmentDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (attachmentPath: String, link: String, comment: String) -> Unit
) {
    var path by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("ورقة مواصفات فنية (Datasheet)") }

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
            Text("إضافة مرفق لقطعة المصنّع", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("الوصف / نوع المرفق") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = path, onValueChange = { path = it }, label = { Text("مسار الملف (Datasheet.pdf)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = link, onValueChange = { link = it }, label = { Text("أو رابط المستند الإلكتروني") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("إلغاء") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (path.isNotBlank() || link.isNotBlank()) onConfirm(path, link, comment)
                    },
                    enabled = path.isNotBlank() || link.isNotBlank()
                ) { Text("رفع الحفظ") }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddSupplierPartBottomSheet(
    allParts: List<Part>,
    manufacturerParts: List<ManufacturerPart>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (partId: Long, sku: String, mfgPartId: Long?, description: String, link: String, note: String, packaging: String, packQuantity: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedPart by remember { mutableStateOf<Part?>(allParts.firstOrNull()) }
    var isPartPickerOpen by remember { mutableStateOf(false) }

    val filteredMfgParts = remember(selectedPart, manufacturerParts) {
        if (selectedPart == null) manufacturerParts
        else manufacturerParts.filter { it.partId == selectedPart!!.id }
    }

    var selectedMfgPartId by remember(filteredMfgParts) { mutableStateOf(filteredMfgParts.firstOrNull()?.id) }
    var sku by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var packaging by remember { mutableStateOf("Box") }
    var packQuantity by remember { mutableStateOf("1") }
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
                .imePadding()
        ) {
            // Header
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("إضافة قطعة مورد (SKU)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = Color(0xFF0F172A))
                        Text("ربط أرقام القطع والتغليف والأسعار الخاصة بالمورد", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Form Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // 1. Internal Part Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("القطعة الداخلية المربوطة *", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                    OutlinedButton(
                        onClick = { isPartPickerOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedPart?.let { "${it.name} (${it.ipn.ifBlank { "بدون IPN" }})" } ?: "اختر القطعة الداخلية...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedPart != null) Color(0xFF0F172A) else Color(0xFF94A3B8)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B))
                        }
                    }
                }

                // 2. Filtered MPNs for the selected Part
                if (filteredMfgParts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("أرقام MPN المصنّعة المتاحة لهذه القطعة:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filteredMfgParts.forEach { mfg ->
                                FilterChip(
                                    selected = selectedMfgPartId == mfg.id,
                                    onClick = { selectedMfgPartId = mfg.id },
                                    label = { Text("MPN: ${mfg.mpn}", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("كود المورد (Supplier SKU) *") },
                    placeholder = { Text("مثال: SKU-ESP32-990") },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف التجاري / ملاحظات الشراء") },
                    placeholder = { Text("وصف التغليف والمواصفات الخاصة بالمورد") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = packaging,
                        onValueChange = { packaging = it },
                        label = { Text("نوع التغليف") },
                        placeholder = { Text("Reel / Box") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = packQuantity,
                        onValueChange = { packQuantity = it },
                        label = { Text("كمية التعبئة بالحزمة") },
                        placeholder = { Text("1000") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Footer Actions Bar
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (selectedPart != null && sku.isNotBlank()) {
                                onConfirm(selectedPart!!.id, sku, selectedMfgPartId, description, link, note, packaging, packQuantity)
                            }
                        },
                        enabled = selectedPart != null && sku.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("حفظ قطعة المورّد", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp))
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp), color = Color(0xFF64748B))
                    }
                }
            }
        }
    }

    if (isPartPickerOpen) {
        SearchablePartPickerDialog(
            parts = allParts,
            selectedPartId = selectedPart?.id,
            onPartSelected = { selectedPart = it },
            onDismiss = { isPartPickerOpen = false }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddPriceBreakDialog(
    defaultCurrency: String,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, price: Double, currency: String) -> Unit
) {
    var qtyStr by remember { mutableStateOf("10") }
    var priceStr by remember { mutableStateOf("1.5") }
    var currency by remember { mutableStateOf(defaultCurrency) }

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
            Text("إضافة شريحة سعر متدرجة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            OutlinedTextField(value = qtyStr, onValueChange = { qtyStr = it }, label = { Text("الحد الأدنى للكمية (Min Quantity) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("سعر الوحدة (Unit Price) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            var isPickerOpen by remember { mutableStateOf(false) }
            CurrencySelectorField(
                selectedCurrencyCode = currency,
                onOpenPicker = { isPickerOpen = true },
                label = "العملة (تستخدم عملة الشركة افتراضياً)"
            )
            if (isPickerOpen) {
                CurrencySelectionBottomSheet(
                    selectedCurrencyCode = currency,
                    onDismiss = { isPickerOpen = false },
                    onCurrencySelected = { selectedCurr ->
                        currency = selectedCurr.code
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("إلغاء") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val qty = qtyStr.toDoubleOrNull() ?: 0.0
                        val pr = priceStr.toDoubleOrNull() ?: 0.0
                        onConfirm(qty, pr, currency.ifBlank { defaultCurrency })
                    },
                    enabled = (qtyStr.toDoubleOrNull() ?: 0.0) > 0
                ) { Text("حفظ") }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


