package com.nextstepai.inventory.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.capacityUnit
import com.nextstepai.inventory.data.effectiveCapacity
import com.nextstepai.inventory.data.formatQuantity
import com.nextstepai.inventory.data.isLabelStale
import com.nextstepai.inventory.data.labelSnapshotData
import com.nextstepai.inventory.sharePdfPayload
import com.nextstepai.inventory.util.BarcodePayloadHelper
import com.nextstepai.inventory.util.DateTimeUtils
import com.nextstepai.inventory.util.LocationLabelPdfGenerator

/**
 * دالة مساعدة لتحديد عنوان الملصق التفاعلي حسب نوع التخزين.
 */
private fun getDynamicLabelTitle(typeCode: String): String {
    return when (typeCode.uppercase()) {
        "WAREHOUSE" -> "ملصق مستودع"
        "LINE" -> "ملصق خط إنتاج"
        "SHELF" -> "ملصق رف"
        "BIN" -> "ملصق صندوق / حاوية"
        "ZONE" -> "ملصق منطقة"
        "AISLE" -> "ملصق ممر"
        "PALLET" -> "ملصق منصة"
        "RACK" -> "ملصق حامل رئيسي"
        "SITE" -> "ملصق منشأة"
        else -> "ملصق موقع"
    }
}

/**
 * مكون ملصق طباعة الرفوف والمواقع التخزينية الميداني القياسي (Physical Shelf Label Card 100x60 mm).
 * يعرض البيانات الثابتة الفيزيائية فقط (السعة القصوى التصميمية، الكود المقروء بالعين، تاريخ الطباعة، ورمز الـ QR المودع).
 */
@Composable
fun PrintableLocationLabel(
    location: StockLocation,
    modifier: Modifier = Modifier,
    parentPath: String = "",
    issueDate: String = DateTimeUtils.getCurrentDate()
) {
    val payloadUri = BarcodePayloadHelper.generateLocationPayload(location.effectiveUuid)
    val effectiveCap = location.effectiveCapacity

    val shortCode = remember(location.effectiveUuid) {
        location.effectiveUuid.removePrefix("location-").let {
            if (it.all { ch -> ch.isDigit() }) "LOC-$it" else it.uppercase()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.6f), // نسبة أبعاد قياسية 100x60mm للملصقات الحرارية
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(2.dp, Color(0xFF0F172A)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Header Bar: نوع الموقع + المعرف الفريد البارز المقروء للبشر
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "${getDynamicLabelTitle(location.locationType)} • ${location.locationType.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // كود المعرف البارز المختصر المقروء بالعين بدون انكسار النص
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Text(
                        text = shortCode,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // 2. Middle Body: اسم الموقع التخزيني والمسار الشجري
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (parentPath.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = parentPath,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 3. Main Footer: رمز الـ QR البارز جداً (108dp) والسعة القصوى المصممة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // المعلومات الدائمة للسعة وتاريخ الطباعة
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEEF2FF),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "السعة التصميمية: ${effectiveCap.formatQuantity()} ${location.capacityUnit}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFF3730A3)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "تاريخ التصدير: $issueDate",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                color = Color(0xFF64748B)
                            )
                        )
                    }
                }

                // رمز الـ QR العملاق والبارز جداً ليملاء الجهة السفلية بالكامل (130dp)
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                        .padding(6.dp)
                ) {
                    QrCodeCanvas(
                        content = payloadUri,
                        modifier = Modifier.fillMaxSize(),
                        quietZonePadding = false
                    )
                }
            }
        }
    }
}

/**
 * شاشة منبثقة من الأسفل لمعاينة ملصق الطباعة ورسم الـ QR وتصدير مستندات الـ PDF ومشاركتها بالمعاينة البصرية الحية.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintableLabelBottomSheet(
    location: StockLocation,
    parentPath: String = "",
    onDismiss: () -> Unit,
    onPrintClick: (() -> Unit)? = null,
    onRegenerateLabel: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val issueDate = DateTimeUtils.getCurrentDate()

    var isZoomed by remember { mutableStateOf(false) }
    val zoomScale by animateFloatAsState(targetValue = if (isZoomed) 1.25f else 1f)

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
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
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
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "معاينة بطاقة ملصق الرف والـ QR",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "جاهز للطباعة على الملصقات الحرارية وتصدير الـ PDF",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isZoomed = !isZoomed }) {
                        Icon(
                            imageVector = if (isZoomed) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                            contentDescription = "تكبير/تصغير",
                            tint = Color(0xFF4F46E5)
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

            // Stale Label Detection & Warning Diff Bar
            if (location.isLabelStale) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "⚠️ بيانات الملصق قديمة (تم تعديل بيانات الرف في النظام)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF92400E)
                            )
                        }

                        val snapshotData = location.labelSnapshotData?.replace("|", " | ") ?: "غير مؤرخة"
                        val currentData = "${location.name} | السعة: ${location.effectiveCapacity.toInt()} | النوع: ${location.locationType}"

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "• البيانات في الملصق المطبوع: $snapshotData",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFFB45309)
                            )
                            Text(
                                text = "• البيانات المحدثة في النظام: $currentData",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = Color(0xFF92400E)
                            )
                        }

                        if (onRegenerateLabel != null) {
                            Button(
                                onClick = onRegenerateLabel,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🔄 إعادة توليد وتحديث الملصق", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Printable Card Preview with Interactive Zoom Scale
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(zoomScale)
            ) {
                PrintableLocationLabel(
                    location = location,
                    parentPath = parentPath,
                    issueDate = issueDate
                )
            }

            // Info Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💡", fontSize = 16.sp)
                    Text(
                        text = "هذا الملصق الفيزيائي يحتوي على البيانات الثابتة والتصميمية للرف. مسح رمز الـ QR يفتح حالة الرف الحية ونسبة الإشغال اللحظية مباشرة في التطبيق.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = Color(0xFF334155)
                    )
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        val pdfBytes = LocationLabelPdfGenerator.generatePdf(location, parentPath, issueDate)
                        val previewPng = LocationLabelPdfGenerator.generatePngPreview(location, parentPath, issueDate)
                        val cleanFileName = "ملصق_${location.name.replace(" ", "_")}.pdf"
                        sharePdfPayload(
                            pdfBytes = pdfBytes,
                            fileName = cleanFileName,
                            title = "مشاركة ملصق موقع تخزيني - ${location.name}",
                            previewImageBytes = previewPng
                        )
                        onPrintClick?.invoke()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مشاركة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
