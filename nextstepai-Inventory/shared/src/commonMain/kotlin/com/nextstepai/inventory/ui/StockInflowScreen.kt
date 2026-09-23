package com.nextstepai.inventory.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. عرض الكاميرا المتواصل بدقة إطار المسح
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

            // 2. إطار تركيز المسح المباشر (Scanning Reticle)
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .align(Alignment.Center)
                    .border(
                        width = 3.dp,
                        color = if (detectedPart != null) Color.Green else MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "إطار المسح",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(54.dp)
                )
            }

            // 3. شريحة التراجع الخاطف (Undo Banner) عند إضافة كمية
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

            // 4. بطاقة التفاعل مع الأصناف المكتشفة (Registered Known Part)
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

            // 5. بطاقة الصنف الجديد غير المسجل (Unregistered New Part Inline Card)
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

            // 6. شريط الإنجاز والسلة المصغرة في أسفل الشاشة (Bottom Inflow Bar)
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
                            items(uiState.scannedItems, key = { it.tempId }) { item ->
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
