package com.nextstepai.inventory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.capacityUnit
import com.nextstepai.inventory.data.unitWeight
import com.nextstepai.inventory.data.totalWeight
import com.nextstepai.inventory.data.calculateOccupancyPercentage
import com.nextstepai.inventory.data.effectiveCapacity
import com.nextstepai.inventory.data.formatQuantity
import com.nextstepai.inventory.util.BarcodePayloadHelper

/**
 * تفكيك وتحليل نص العبوة وسعتها (Parsed Package Info).
 */
data class ParsedPackageInfo(
    val packageName: String,
    val packSize: Double
)

/**
 * دالة مساعدة لتحليل نص packaging واستخراج اسم العبوة وسعتها (Pack Size).
 * مثال: "Reel 250" -> اسم العبوة: Reel، سعة العبوة: 250 قطعة.
 */
private fun parsePackageInfo(packagingStr: String, totalQuantity: Double): ParsedPackageInfo {
    val trimmed = packagingStr.trim()
    if (trimmed.isBlank()) {
        return ParsedPackageInfo(packageName = "عبوة", packSize = 1.0)
    }

    val numberRegex = Regex("""\d+(\.\d+)?""")
    val match = numberRegex.find(trimmed)
    val extractedSize = match?.value?.toDoubleOrNull()

    val packageName = trimmed.replace(numberRegex, "").trim().ifBlank { "عبوة" }

    val packSize = when {
        extractedSize != null && extractedSize > 0.0 -> extractedSize
        totalQuantity > 1.0 && (packageName.lowercase() in listOf("reel", "box", "tray", "carton", "bobbins", "كرتونة", "بكرة", "صندوق", "حاوية", "رزمة")) -> totalQuantity
        else -> 1.0
    }

    return ParsedPackageInfo(
        packageName = packageName,
        packSize = packSize
    )
}

/**
 * خيارات وسيناريوهات أسباب الحركة والمصدر المخزني المعتمدة بالنظام.
 */
data class MovementReasonOption(
    val title: String,
    val description: String,
    val emoji: String
)

private val SYSTEM_MOVEMENT_REASONS = listOf(
    MovementReasonOption(
        title = "تحويل بين الفروع والمستودعات",
        description = "إعادة ترتيب الأرفف، التخزين المؤقت، أو التحويل بين المستودعات والمباني",
        emoji = "🔄"
    ),
    MovementReasonOption(
        title = "تغذية خط إنتاج وتصنيع",
        description = "صرف وتغذية المواد لأمر بناء وتصنيع محلي (Build Order)",
        emoji = "⚙️"
    ),
    MovementReasonOption(
        title = "مرتجع مبيعات / إعادة تخزين",
        description = "إعادة إيداع كمية مسترجعة من المبيعات أو المرتجعات",
        emoji = "🛒"
    ),
    MovementReasonOption(
        title = "تسوية رصيد / جرد مخزني",
        description = "تعديل تسويات جردية وإعادة ضبط فروقات الجرد الفعلي",
        emoji = "📋"
    ),
    MovementReasonOption(
        title = "عزل وفحص جودة (Quarantine)",
        description = "تحويل المواد التالفة أو المشكوك بحالتها لمنطقة الفحص والعزل",
        emoji = "⚠️"
    )
)

/**
 * نافذة النقل المخزني السريع المتقدمة (Quick Stock Transfer BottomSheet)
 * تدعم قياس الوحدات المزدوج (Dual-UoM: Packaging vs Units) مع التحليل والتحويل الدقيق لحظياً.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTransferBottomSheet(
    stockItem: StockItem,
    partName: String,
    currentLocation: StockLocation,
    allLocations: List<StockLocation>,
    allStockItems: List<StockItem> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmTransfer: (targetLocationId: Long, quantity: Double, reason: String, notes: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // تحليل بيانات العبوة والسعة
    val packageInfo = remember(stockItem.packaging, stockItem.quantity) {
        parsePackageInfo(stockItem.packaging, stockItem.quantity)
    }
    val totalPackages = if (packageInfo.packSize > 0.0) (stockItem.quantity / packageInfo.packSize) else stockItem.quantity

    // تصفية المواقع المؤهلة للنقل (استبعاد الموقع الحالي والمواقع الهيكلية)
    val candidateLocations = remember(allLocations, currentLocation.id) {
        allLocations.filter { (it.id != currentLocation.id) && !it.structural }
    }

    var selectedTargetLocation by remember { mutableStateOf<StockLocation?>(null) }
    var isLocationPickerOpen by remember { mutableStateOf(false) }
    var isReasonPickerOpen by remember { mutableStateOf(false) }
    var isTargetBarcodeScannerOpen by remember { mutableStateOf(false) }

    var isPackagingUnitMode by remember { mutableStateOf(packageInfo.packSize > 1.0) }
    var quantityInput by remember(isPackagingUnitMode) {
        mutableStateOf(if (isPackagingUnitMode) totalPackages.formatQuantity() else stockItem.quantity.formatQuantity())
    }

    var selectedReason by remember { mutableStateOf(SYSTEM_MOVEMENT_REASONS.first().title) }
    var notesInput by remember { mutableStateOf("") }

    val prefilledUnitWeight = remember(stockItem) { stockItem.unitWeight }
    val prefilledTotalWeight = remember(stockItem) { stockItem.totalWeight }

    var isWeightModeUnit by remember { mutableStateOf(true) }
    var unitWeightInput by remember(prefilledUnitWeight) {
        mutableStateOf(prefilledUnitWeight?.toString() ?: "")
    }
    var totalWeightInput by remember(prefilledTotalWeight) {
        mutableStateOf(prefilledTotalWeight?.toString() ?: "")
    }

    val parsedInput = quantityInput.toDoubleOrNull() ?: 0.0
    val maxAvailable = if (isPackagingUnitMode) totalPackages else stockItem.quantity
    val isValidQuantity = parsedInput > 0.0 && parsedInput <= maxAvailable
    val isTargetSelected = selectedTargetLocation != null
    val canConfirm = isTargetSelected && isValidQuantity && selectedReason.isNotBlank()

    // الخصم الفعلي بالقطع المعيارية المعتمدة لحسابات قاعدة البيانات
    val transferBaseUnits = if (isPackagingUnitMode) (parsedInput * packageInfo.packSize) else parsedInput
    val remainingBaseUnits = (stockItem.quantity - transferBaseUnits).coerceAtLeast(0.0)
    val remainingPackages = if (packageInfo.packSize > 0.0) (remainingBaseUnits / packageInfo.packSize) else remainingBaseUnits

    val unitLabel = if (isPackagingUnitMode) packageInfo.packageName else "قطعة"

    // 1. قارئ الـ QR للرف الهدف المباشر
    if (isTargetBarcodeScannerOpen) {
        LocationBarcodeScannerBottomSheet(
            locations = candidateLocations,
            onDismiss = { isTargetBarcodeScannerOpen = false },
            onBarcodeScanned = { scannedCode ->
                val parsed = BarcodePayloadHelper.parsePayload(scannedCode)
                val matchedLoc = candidateLocations.find { loc ->
                    loc.uuid == parsed.uuid ||
                    loc.uuid == "location-${parsed.uuid.removePrefix("loc-")}" ||
                    loc.id.toString() == parsed.uuid ||
                    loc.id.toString() == parsed.uuid.removePrefix("loc-") ||
                    loc.id.toString() == parsed.uuid.removePrefix("location-") ||
                    loc.name.equals(parsed.rawContent, ignoreCase = true)
                }
                if (matchedLoc != null) {
                    selectedTargetLocation = matchedLoc
                }
                isTargetBarcodeScannerOpen = false
            }
        )
    }

    // 2. النافذة المنبثقة لاختيار الموقع الهدف (Location Picker Sheet)
    if (isLocationPickerOpen) {
        LocationPickerSheet(
            candidateLocations = candidateLocations,
            allStockItems = allStockItems,
            selectedLocation = selectedTargetLocation,
            unitLabel = unitLabel,
            onSelectLocation = { loc ->
                selectedTargetLocation = loc
                isLocationPickerOpen = false
            },
            onOpenBarcodeScanner = {
                isLocationPickerOpen = false
                isTargetBarcodeScannerOpen = true
            },
            onDismiss = { isLocationPickerOpen = false }
        )
    }

    // 3. النافذة المنبثقة لاختيار سبب الحركة والمصدر المخزني (Reason Picker Sheet)
    if (isReasonPickerOpen) {
        ReasonPickerSheet(
            selectedReason = selectedReason,
            reasonOptions = SYSTEM_MOVEMENT_REASONS,
            onSelectReason = { reason ->
                selectedReason = reason
                isReasonPickerOpen = false
            },
            onDismiss = { isReasonPickerOpen = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // الترويسة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEEF2FF)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "النقل المخزني السريع",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "تحويل موقع تخزين القطعة وتحديث سعات الأرفف آلياً",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            // بطاقة تفاصيل القطعة المراد نقلها مع سعة العبوة المحددة
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = partName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (stockItem.serial.isNotBlank()) {
                            Text(
                                text = "تسلسلي: ${stockItem.serial}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (stockItem.batch.isNotBlank()) {
                            Text(
                                text = "الدفعة: ${stockItem.batch}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = if (packageInfo.packSize > 1.0) {
                                "المتاح: ${totalPackages.formatQuantity()} ${packageInfo.packageName} (${stockItem.quantity.formatQuantity()} قطعة)"
                            } else {
                                "المتاح: ${stockItem.quantity.formatQuantity()} ${packageInfo.packageName}"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // مسار النقل والموقع الهدف + سبب الحركة المدمجين
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "مسار النقل والموقع الهدف",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                // الموقع الحالي (المصدر)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "📍 من (الموقع الحالي): ${currentLocation.name}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        )
                    }
                }

                // اختيار الموقع الهدف عبر كرت مقتضب تفاعلي
                Surface(
                    onClick = { isLocationPickerOpen = true },
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, if (selectedTargetLocation != null) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "إلى (الموقع الهدف)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedTargetLocation?.let { loc ->
                                    val currentTargetQty = allStockItems.filter { it.locationId == loc.id }.sumOf { it.quantity }
                                    val occPct = loc.calculateOccupancyPercentage(currentTargetQty)
                                    "${loc.name} (إشغال ${occPct.toInt()}%)"
                                } ?: "اضغط لاختيار الموقع الهدف من القائمة...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selectedTargetLocation != null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTargetLocation != null) Color(0xFF1E293B) else MaterialTheme.colorScheme.outline
                                )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = { isTargetBarcodeScannerOpen = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "مسح باركود الرف الهدف",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // سعة الرف الهدف المتبقية (Target Location Space Available Check)
                selectedTargetLocation?.let { loc ->
                    val currentTargetQty = allStockItems.filter { it.locationId == loc.id }.sumOf { it.quantity }
                    val availableSpace = (loc.effectiveCapacity - currentTargetQty).coerceAtLeast(0.0)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "📏 المساحة المتاحة في هذا الرف: ${availableSpace.formatQuantity()} $unitLabel (السعة: ${loc.effectiveCapacity.formatQuantity()})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // اختيار سبب الحركة والمصدر المخزني مدمج مباشرة تحت الموقع الهدف
                Surface(
                    onClick = { isReasonPickerOpen = true },
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFF4F46E5).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val selectedOption = SYSTEM_MOVEMENT_REASONS.find { it.title == selectedReason } ?: SYSTEM_MOVEMENT_REASONS.first()
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedOption.emoji,
                                fontSize = 18.sp
                            )
                            Column {
                                Text(
                                    text = "سبب الحركة والمصدر المخزني",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = selectedOption.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEEF2FF)
                        ) {
                            Text(
                                text = "تغيير",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4F46E5)
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // وضع وحدة النقل بالتعبئة والتغليف (Packaging Mode vs Units)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "وحدة النقل والتعبئة",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isPackagingUnitMode,
                        onClick = {
                            isPackagingUnitMode = true
                            quantityInput = totalPackages.formatQuantity()
                        },
                        label = { Text("📦 عبوة كاملة (${packageInfo.packageName})") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEEF2FF),
                            selectedLabelColor = Color(0xFF3730A3)
                        )
                    )

                    FilterChip(
                        selected = !isPackagingUnitMode,
                        onClick = {
                            isPackagingUnitMode = false
                            quantityInput = stockItem.quantity.formatQuantity()
                        },
                        label = { Text("🧩 قطع منفردة (Units)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEEF2FF),
                            selectedLabelColor = Color(0xFF3730A3)
                        )
                    )
                }
            }

            // حقل الكمية المنقولة + زر السحب الكامل
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isPackagingUnitMode) "الكمية بـ (${packageInfo.packageName})" else "الكمية بالقطع المنفردة",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (canConfirm) {
                                    val target = selectedTargetLocation ?: return@KeyboardActions
                                    val unitPrefix = if (isPackagingUnitMode) {
                                        "[${parsedInput.formatQuantity()} ${packageInfo.packageName} = ${transferBaseUnits.formatQuantity()} قطعة]"
                                    } else {
                                        "[${transferBaseUnits.formatQuantity()} قطعة]"
                                    }
                                    val formattedNotes = "$unitPrefix ${notesInput.trim()}".trim()
                                    onConfirmTransfer(target.id, transferBaseUnits, selectedReason, formattedNotes)
                                }
                            }
                        ),
                        isError = !isValidQuantity && quantityInput.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        label = { Text("الكمية") }
                    )

                    Button(
                        onClick = {
                            quantityInput = if (isPackagingUnitMode) totalPackages.formatQuantity() else stockItem.quantity.formatQuantity()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEEF2FF),
                            contentColor = Color(0xFF4F46E5)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = if (isPackagingUnitMode) {
                                "الكل (${totalPackages.formatQuantity()} ${packageInfo.packageName})"
                            } else {
                                "الكل (${stockItem.quantity.formatQuantity()} قطعة)"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // المعاينة اللحظية التفاعلية للأرصدة (Live Remaining Quantity Preview)
            if (quantityInput.isNotBlank()) {
                if (!isValidQuantity) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "⚠️ عجز في الرصيد: المطلوب (${parsedInput.formatQuantity()}) أكبر من المتاح (${maxAvailable.formatQuantity()} $unitLabel)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "💡 معاينة الأرصدة اللحظية بعد النقل:",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // بطاقة المتبقي في المصدر
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFECFDF5),
                                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "المتبقي في المصدر:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF047857)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        val mainSourceText = if (isPackagingUnitMode) {
                                            "${remainingPackages.formatQuantity()} ${packageInfo.packageName}"
                                        } else {
                                            "${remainingBaseUnits.formatQuantity()} قطعة"
                                        }
                                        val subSourceText = if (packageInfo.packSize > 1.0) {
                                            if (isPackagingUnitMode) "(${remainingBaseUnits.formatQuantity()} قطعة)" else "(${remainingPackages.formatQuantity()} ${packageInfo.packageName})"
                                        } else null

                                        Text(
                                            text = mainSourceText,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF065F46)
                                            )
                                        )
                                        if (subSourceText != null) {
                                            Text(
                                                text = subSourceText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF047857).copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }

                                // بطاقة المضاف للهدف
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEEF2FF),
                                    border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "المضاف للهدف:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF3730A3)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        val targetPacks = if (packageInfo.packSize > 0.0) (transferBaseUnits / packageInfo.packSize) else transferBaseUnits
                                        val mainTargetText = if (isPackagingUnitMode) {
                                            "+${parsedInput.formatQuantity()} ${packageInfo.packageName}"
                                        } else {
                                            "+${transferBaseUnits.formatQuantity()} قطعة"
                                        }
                                        val subTargetText = if (packageInfo.packSize > 1.0) {
                                            if (isPackagingUnitMode) "(+${transferBaseUnits.formatQuantity()} قطعة)" else "(+${targetPacks.formatQuantity()} ${packageInfo.packageName})"
                                        } else null

                                        Text(
                                            text = mainTargetText,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF312E81)
                                            )
                                        )
                                        if (subTargetText != null) {
                                            Text(
                                                text = subTargetText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF3730A3).copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ⚖️ معامل احتساب وزن الشحنة المنقولة (يظهر عند اختيار رف بجدولة وزنية)
            val targetCapUnit = selectedTargetLocation?.capacityUnit ?: "قطعة"
            val isTargetWeightCap = targetCapUnit.equals("kg", ignoreCase = true) ||
                    targetCapUnit.equals("كغ", ignoreCase = true) ||
                    targetCapUnit.equals("كيلوغرام", ignoreCase = true) ||
                    targetCapUnit.equals("ton", ignoreCase = true) ||
                    targetCapUnit.equals("طن", ignoreCase = true)
            val isWeightMismatch = isTargetWeightCap && !unitLabel.equals(targetCapUnit, ignoreCase = true)

            if (isWeightMismatch) {
                val calcUnitWeight = if (isWeightModeUnit) {
                    unitWeightInput.toDoubleOrNull()
                } else {
                    val tot = totalWeightInput.toDoubleOrNull()
                    if (tot != null && transferBaseUnits > 0.0) tot / transferBaseUnits else null
                }

                val calcTotalWeight = if (!isWeightModeUnit) {
                    totalWeightInput.toDoubleOrNull()
                } else {
                    val uw = unitWeightInput.toDoubleOrNull()
                    if (uw != null && transferBaseUnits > 0.0) uw * transferBaseUnits else null
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Scale,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "معامل احتساب وزن الشحنة المنقولة ⚖️",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            )
                        }

                        if (prefilledUnitWeight != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = "💡 تم سحب وزن الوحدة تلقائياً من بيانات الصنف (${prefilledUnitWeight} كغ)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Mode Selector Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(2.dp)
                        ) {
                            FilterChip(
                                selected = isWeightModeUnit,
                                onClick = { isWeightModeUnit = true },
                                label = { Text("⚖️ وزن الوحدة", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            FilterChip(
                                selected = !isWeightModeUnit,
                                onClick = { isWeightModeUnit = false },
                                label = { Text("📦 الوزن الكلي", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp)
                            )
                        }

                        if (isWeightModeUnit) {
                            OutlinedTextField(
                                value = unitWeightInput,
                                onValueChange = { unitWeightInput = it },
                                label = { Text("وزن $unitLabel الواحد (كغ / kg)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        } else {
                            OutlinedTextField(
                                value = totalWeightInput,
                                onValueChange = { totalWeightInput = it },
                                label = { Text("الوزن الكلي المحمّل للشحنة المنقولة (كغ / kg)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        val displayTotalWeight = calcTotalWeight?.let { "%.2f".format(it) } ?: "—"
                        val displayUnitWeight = calcUnitWeight?.let { "%.2f".format(it) } ?: "—"

                        Text(
                            text = "الحمل المحسوب للرف الهدف: $displayTotalWeight كغ (وزن الوحدة: $displayUnitWeight كغ)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            // حقل ملاحظات إضافية متجاوب مع حجم النص
            OutlinedTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = { Text("ملاحظات إضافية (اختياري)") },
                placeholder = { Text("مثال: تم إبلاغ أمين المستودع وتأكيد النقل...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 1,
                maxLines = 4,
                shape = RoundedCornerShape(10.dp)
            )

            // الأزرار السفلية (إلغاء / تأكيد)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("إلغاء")
                }

                Button(
                    onClick = {
                        val target = selectedTargetLocation ?: return@Button
                        val unitPrefix = if (isPackagingUnitMode) {
                            "[${parsedInput.formatQuantity()} ${packageInfo.packageName} = ${transferBaseUnits.formatQuantity()} قطعة]"
                        } else {
                            "[${transferBaseUnits.formatQuantity()} قطعة]"
                        }

                        val weightNote = if (isWeightMismatch) {
                            val calcUW = if (isWeightModeUnit) unitWeightInput.toDoubleOrNull() else {
                                val tot = totalWeightInput.toDoubleOrNull()
                                if (tot != null && transferBaseUnits > 0.0) tot / transferBaseUnits else null
                            }
                            val calcTW = if (!isWeightModeUnit) totalWeightInput.toDoubleOrNull() else {
                                val uw = unitWeightInput.toDoubleOrNull()
                                if (uw != null && transferBaseUnits > 0.0) uw * transferBaseUnits else null
                            }
                            if (calcTW != null || calcUW != null) " [وزن الشحنة: ${calcTW?.let { "%.2f".format(it) } ?: "—"} كغ | وزن الوحدة: ${calcUW?.let { "%.2f".format(it) } ?: "—"} كغ]" else ""
                        } else ""

                        val formattedNotes = "$unitPrefix$weightNote ${notesInput.trim()}".trim()
                        onConfirmTransfer(target.id, transferBaseUnits, selectedReason, formattedNotes)
                    },
                    enabled = canConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFCBD5E1),
                        disabledContentColor = Color.White
                    )
                ) {
                    Text(
                        text = "تأكيد النقل ⇄",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * النافذة المنبثقة المخصصة لاختيار الموقع الهدف مع البحث ومسح الباركود والمشغولات (Location Picker Sheet)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationPickerSheet(
    candidateLocations: List<StockLocation>,
    allStockItems: List<StockItem>,
    selectedLocation: StockLocation?,
    unitLabel: String,
    onSelectLocation: (StockLocation) -> Unit,
    onOpenBarcodeScanner: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredLocations = remember(candidateLocations, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) candidateLocations
        else candidateLocations.filter {
            it.name.contains(q, ignoreCase = true) ||
            it.description.contains(q, ignoreCase = true) ||
            it.locationType.contains(q, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "اختر الموقع الهدف الجديد",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "مسح الملصق الميداني أو اختيار الرف من القائمة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("البحث باسم الرف أو الوصف...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "مسح")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedIconButton(
                    onClick = onOpenBarcodeScanner,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF4F46E5)),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = Color(0xFFEEF2FF),
                        contentColor = Color(0xFF4F46E5)
                    ),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "مسح الباركود",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (filteredLocations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مواقع تخزينية مطابقة لبحثك.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLocations, key = { "target-loc-${it.id}" }) { loc ->
                        val currentTargetQty = allStockItems.filter { item -> item.locationId == loc.id }.sumOf { item -> item.quantity }
                        val occPct = loc.calculateOccupancyPercentage(currentTargetQty)
                        val availableSpace = (loc.effectiveCapacity - currentTargetQty).coerceAtLeast(0.0)
                        val isHighOccupancy = occPct >= 90.0
                        val isSelected = selectedLocation?.id == loc.id

                        Surface(
                            onClick = { onSelectLocation(loc) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFEEF2FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF4F46E5)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = loc.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF3730A3) else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    if (loc.description.isNotBlank()) {
                                        Text(
                                            text = loc.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "المساحة المتاحة: ${availableSpace.formatQuantity()} $unitLabel (من أصل ${loc.effectiveCapacity.formatQuantity()})",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFF475569)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isHighOccupancy) Color(0xFFFEE2E2) else Color(0xFFF1F5F9)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isHighOccupancy) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = "ممتلئ تقريباً",
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                            Text(
                                                text = "إشغال ${occPct.toInt()}%",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isHighOccupancy) Color(0xFF991B1B) else Color(0xFF475569)
                                                )
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "محدد",
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(20.dp)
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

/**
 * النافذة المنبثقة المخصصة لاختيار سبب الحركة والمصدر المخزني (Reason Picker Sheet)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReasonPickerSheet(
    selectedReason: String,
    reasonOptions: List<MovementReasonOption>,
    onSelectReason: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "سبب الحركة والمصدر المخزني",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "تحديد تصنيف المعاملة لربطها ببرنامج إدارة المخزون",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reasonOptions, key = { it.title }) { option ->
                    val isSelected = selectedReason == option.title
                    Surface(
                        onClick = { onSelectReason(option.title) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF4F46E5)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option.emoji,
                                    fontSize = 22.sp
                                )
                                Column {
                                    Text(
                                        text = option.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF3730A3) else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = option.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "محدد",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
