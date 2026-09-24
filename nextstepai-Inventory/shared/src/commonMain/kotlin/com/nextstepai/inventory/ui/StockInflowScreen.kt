package com.nextstepai.inventory.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextstepai.inventory.data.db.PartEntity
import com.nextstepai.inventory.ui.components.AppCameraKView

/**
 * شاشة الاستلام الميداني السريع (StockInflowScreen) للمسح المباشر المتواصل والتأكيد الفوري.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockInflowScreen(
    viewModel: StockInflowViewModel,
    modifier: Modifier = Modifier,
    inflowTitle: String = "استلام توريد خارجي",
    poReference: String? = "PO-2025-001",
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val detectedPart by viewModel.detectedPart.collectAsState()
    val unrecognizedBarcode by viewModel.unrecognizedBarcode.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var isTorchOn by remember { mutableStateOf(false) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var manualBarcodeText by remember { mutableStateOf("") }

    var customQtyText by remember { mutableStateOf("1") }
    var showCustomQtyDialog by remember { mutableStateOf(false) }
    var selectedPartForCustomQty by remember { mutableStateOf<PartEntity?>(null) }

    var newPartNameInput by remember { mutableStateOf("") }
    var newPartQtyInput by remember { mutableStateOf("1") }

    var isSummaryExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(25.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = inflowTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            poReference?.let { po ->
                                Text(
                                    text = "مرتبط بأمر الشراء: $po",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع"
                            )
                        }
                    },
                    actions = {
                        // 1. زر تشغيل وإيقاف الفلاش (Torch Toggle)
                        IconButton(onClick = { isTorchOn = !isTorchOn }) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "الفلاش",
                                tint = if (isTorchOn) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // 2. زر الإدخال اليدوي للباركود
                        IconButton(onClick = { showManualInputDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "إدخال يدوي للباركود"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. عرض بث الكاميرا المتواصل
            AppCameraKView(
                onBarcodeDetected = { barcode ->
                    viewModel.onBarcodeScanned(barcode)
                },
                onImageCaptured = { bytes ->
                    viewModel.onBarcodeScanned("SKU-STM32-001", bytes)
                },
                onClose = onClose,
                modifier = Modifier.fillMaxSize()
            )

            // 2. التعتيم الجانبي شبه الشفاف وتفريغ نافذة المسح (Darkened Viewfinder Overlay)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val reticleWidth = 280.dp.toPx()
                val reticleHeight = 200.dp.toPx()
                val left = (size.width - reticleWidth) / 2f
                val top = (size.height - reticleHeight) / 2f

                val overlayColor = Color.Black.copy(alpha = 0.55f)

                // Top mask
                drawRect(color = overlayColor, topLeft = Offset(0f, 0f), size = Size(size.width, top))
                // Bottom mask
                drawRect(color = overlayColor, topLeft = Offset(0f, top + reticleHeight), size = Size(size.width, size.height - (top + reticleHeight)))
                // Left mask
                drawRect(color = overlayColor, topLeft = Offset(0f, top), size = Size(left, reticleHeight))
                // Right mask
                drawRect(color = overlayColor, topLeft = Offset(left + reticleWidth, top), size = Size(size.width - (left + reticleWidth), reticleHeight))
            }

            // 3. زوايا التركيز الاحترافية (Laser Corner Brackets)
            Box(
                modifier = Modifier
                    .size(width = 280.dp, height = 200.dp)
                    .align(Alignment.Center)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 4.dp.toPx()
                    val cornerLength = 26.dp.toPx()
                    val cornerColor = if (detectedPart != null) Color(0xFF10B981) else Color.White

                    // Top-Left
                    drawLine(cornerColor, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
                    drawLine(cornerColor, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)

                    // Top-Right
                    drawLine(cornerColor, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth)
                    drawLine(cornerColor, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)

                    // Bottom-Left
                    drawLine(cornerColor, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)
                    drawLine(cornerColor, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth)

                    // Bottom-Right
                    drawLine(cornerColor, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth)
                    drawLine(cornerColor, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth)
                }
            }

            // 4. شريحة التوجيه العائمة بنمط كبسولة (Floating Pill Badge)
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.70f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-130).dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (detectedPart != null) Color(0xFF10B981) else Color(0xFF4F46E5))
                    )
                    Text(
                        text = if (detectedPart != null) "تم التعرف على الصنف بنجاح!" else "وجه الكاميرا نحو باركود الصنف",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        ),
                        color = Color.White
                    )
                }
            }

            // 5. شريحة التراجع الخاطف (Undo Banner) عند إضافة كمية
            AnimatedVisibility(
                visible = uiState.undoWindowActive && uiState.lastAddedItemForUndo != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                uiState.lastAddedItemForUndo?.let { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.Green,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تمت إضافة ${item.quantity.toInt()} × ${item.name}",
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            TextButton(
                                onClick = { viewModel.undoLastAddedItem() },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = "تراجع")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تراجع", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 6. بطاقة التفاعل مع الأصناف المكتشفة (Registered Known Part)
            detectedPart?.let { part ->
                Card(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = part.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "IPN: ${part.ipn} | الرف: ${uiState.defaultLocationName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.dismissDetectedCard() }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "اختر الكمية المستلمة بنقرة واحدة:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(1.0, 5.0, 10.0, 20.0, 50.0).forEach { qty ->
                                FilterChip(
                                    selected = false,
                                    onClick = { viewModel.addQuantityForKnownPart(part, qty) },
                                    label = { Text("${qty.toInt()}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                            IconButton(
                                onClick = {
                                    selectedPartForCustomQty = part
                                    showCustomQtyDialog = true
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "تخصيص")
                            }
                        }
                    }
                }
            }

            // 7. بطاقة الصنف الجديد غير المسجل (Unregistered New Part Inline Card)
            unrecognizedBarcode?.let { barcode ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "صنف جديد غير مسجل ($barcode)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newPartNameInput,
                            onValueChange = { newPartNameInput = it },
                            label = { Text("اسم القطعة الجديدة") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPartQtyInput,
                            onValueChange = { newPartQtyInput = it },
                            label = { Text("الكمية المستلمة") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { viewModel.dismissDetectedCard() }) {
                                Text("إلغاء")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val qty = newPartQtyInput.toDoubleOrNull() ?: 1.0
                                    viewModel.addNewUnregisteredPart(newPartNameInput, qty, barcode)
                                    newPartNameInput = ""
                                    newPartQtyInput = "1"
                                }
                            ) {
                                Text("تأكيد وإضافة كقطعة جديدة")
                            }
                        }
                    }
                }
            }

            // 8. شريط الإنجاز والسلة المصغرة في أسفل الشاشة (Bottom Inflow Bar)
            Card(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ملخص الشحنة: ${uiState.totalItemsCount} أصناف (${uiState.totalQuantityCount.toInt()} قطعة)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "الرف المستهدف: ${uiState.defaultLocationName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (uiState.scannedItems.isNotEmpty()) {
                            TextButton(onClick = { isSummaryExpanded = !isSummaryExpanded }) {
                                Text(if (isSummaryExpanded) "إخفاء التفاصيل" else "عرض العناصر")
                            }
                        }
                    }

                    // قائمة المصغرات والعناصر الممسوحة
                    if (isSummaryExpanded && uiState.scannedItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(uiState.scannedItems, key = { index, item -> "scanned-${item.tempId}-$index" }) { _, item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${item.quantity.toInt()} × ${item.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { viewModel.removeItem(item.tempId) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.commitSession(
                                onSuccess = onClose,
                                onError = { errorMessage = it }
                            )
                        },
                        enabled = uiState.scannedItems.isNotEmpty() && !uiState.isProcessingCommit,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (uiState.isProcessingCommit) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "اعتماد وترحيل الشحنة (${uiState.totalQuantityCount.toInt()} قطعة)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // حوار الإدخال اليدوي للباركود
    if (showManualInputDialog) {
        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            title = { Text("إدخال الباركود يدوياً") },
            text = {
                OutlinedTextField(
                    value = manualBarcodeText,
                    onValueChange = { manualBarcodeText = it },
                    label = { Text("أدخل رقم IPN / الباركود") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualBarcodeText.isNotBlank()) {
                            viewModel.onBarcodeScanned(manualBarcodeText)
                        }
                        showManualInputDialog = false
                        manualBarcodeText = ""
                    }
                ) {
                    Text("بحث ومسح")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInputDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // حوار إدخال الكمية المخصصة
    if (showCustomQtyDialog && selectedPartForCustomQty != null) {
        AlertDialog(
            onDismissRequest = { showCustomQtyDialog = false },
            title = { Text("تحديد الكمية المخصصة") },
            text = {
                OutlinedTextField(
                    value = customQtyText,
                    onValueChange = { customQtyText = it },
                    label = { Text("أدخل عدد القطع المستلمة") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = customQtyText.toDoubleOrNull() ?: 1.0
                        selectedPartForCustomQty?.let { part ->
                            viewModel.addQuantityForKnownPart(part, qty)
                        }
                        showCustomQtyDialog = false
                    }
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomQtyDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // حوار عرض الخطأ إن وجد
    errorMessage?.let { err ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("تنبيه") },
            text = { Text(err) },
            confirmButton = {
                Button(onClick = { errorMessage = null }) {
                    Text("حسناً")
                }
            }
        )
    }
}
