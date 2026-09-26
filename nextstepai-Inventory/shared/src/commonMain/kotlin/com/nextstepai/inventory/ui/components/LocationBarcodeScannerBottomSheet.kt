package com.nextstepai.inventory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nextstepai.inventory.data.StockLocation

/**
 * نافذة قارئ الباركود والكاميرا المباشرة (CameraK & CameraX) لمسح باركود وتشفير موقع التخزين
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationBarcodeScannerBottomSheet(
    locations: List<StockLocation> = emptyList(),
    onDismiss: () -> Unit,
    onBarcodeScanned: (scannedPayload: String) -> Unit
) {
    var isTorchOn by remember { mutableStateOf(false) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var manualBarcodeText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. عرض بث الكاميرا الحي المباشر (AppCameraKView)
                AppCameraKView(
                    onBarcodeDetected = { barcode ->
                        onBarcodeScanned(barcode)
                    },
                    onImageCaptured = { _ -> },
                    onClose = onDismiss,
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
                        val cornerColor = Color.White

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
                                .background(Color(0xFF6366F1))
                        )
                        Text(
                            text = "وجه الكاميرا نحو باركود الموقع",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                // 5. الترويسة العلوية الأنيقة (Top Bar Overlay)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(25.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "قارئ الباركود والكاميرا المباشرة (CameraK & CameraX)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "مسح باركود وتشفير موقع التخزين",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = Color(0xFFA5B4FC)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { isTorchOn = !isTorchOn }) {
                                Icon(
                                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "الفلاش",
                                    tint = if (isTorchOn) Color(0xFFF59E0B) else Color.White
                                )
                            }
                            IconButton(onClick = { showManualInputDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = "إدخال يدوي للباركود",
                                    tint = Color.White
                                )
                            }
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
                    label = { Text("أدخل رمز الموقع أو الباركود") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualBarcodeText.isNotBlank()) {
                            onBarcodeScanned(manualBarcodeText.trim())
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
}
