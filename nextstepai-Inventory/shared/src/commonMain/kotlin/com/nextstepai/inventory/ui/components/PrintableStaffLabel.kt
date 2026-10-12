package com.nextstepai.inventory.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
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
import com.nextstepai.inventory.sharePdfPayload
import com.nextstepai.inventory.util.DateTimeUtils
import com.nextstepai.inventory.util.StaffLabelPdfGenerator

/**
 * مكون ملصق طباعة كود الموظف والتفعيل القياسي بالباركود الخطي (Physical Staff Label Card 100x60mm).
 */
@Composable
fun PrintableStaffLabel(
    codeStr: String,
    labelTitle: String,
    assignedRole: String = "OPERATOR",
    modifier: Modifier = Modifier,
    issueDate: String = DateTimeUtils.getCurrentDate()
) {
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
            // 1. Header Bar: كبسولة كود التفعيل + كبسولة نوع الملصق
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Text(
                        text = "كود التفعيل: $codeStr",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                ) {
                    Text(
                        text = "ملصق موظف",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        ),
                        color = Color(0xFF3730A3),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // 2. Middle Body: الاسم الوظيفي بارز وبسيط
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = labelTitle.ifBlank { "الاسم الوظيفي" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp
                    ),
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. Visual Core: باركود خطي 1D (Code 128) ممتد أفقياً + التذييل بتاريخ التصدير
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    Barcode128Canvas(
                        content = codeStr,
                        modifier = Modifier.fillMaxSize(),
                        showText = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "تاريخ التصدير: $issueDate",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            color = Color(0xFF64748B)
                        )
                    )
                }
            }
        }
    }
}

/**
 * شاشة منبثقة من الأسفل لمعاينة بطاقة ملصق الموظف والباركود الخطي (Code 128) مع خيار التصدير والمشاركة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintableStaffLabelBottomSheet(
    codeStr: String,
    labelTitle: String,
    assignedRole: String = "OPERATOR",
    onDismiss: () -> Unit,
    onPrintClick: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val issueDate = remember { DateTimeUtils.getCurrentDate() }

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
                .imePadding()
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
                            .background(Color(0xFFEDE9FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "معاينة بطاقة ملصق الموظف والباركود الخطي (1D Barcode)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
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
                            tint = Color(0xFF7C3AED)
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

            // Printable Staff Card Preview with Interactive Zoom Scale
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(zoomScale)
            ) {
                PrintableStaffLabel(
                    codeStr = codeStr,
                    labelTitle = labelTitle,
                    assignedRole = assignedRole,
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
                        text = "هذا الملصق الفيزيائي يحتوي على كود التفعيل والباركود الخطي 1D الخاص بالموظف. مسح الباركود يسهل ربط وتسجيل الموظف مباشرة في التطبيق.",
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
                        val pdfBytes = StaffLabelPdfGenerator.generatePdf(codeStr, labelTitle, assignedRole, issueDate)
                        val previewPng = StaffLabelPdfGenerator.generatePngPreview(codeStr, labelTitle, assignedRole, issueDate)
                        val cleanFileName = "ملصق_موظف_${codeStr.replace(" ", "_")}.pdf"
                        sharePdfPayload(
                            pdfBytes = pdfBytes,
                            fileName = cleanFileName,
                            title = "مشاركة ملصق موظف - $codeStr",
                            previewImageBytes = previewPng
                        )
                        onPrintClick?.invoke()
                    },
                    enabled = StaffLabelPdfGenerator.isPdfExportSupported,
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
                    Text(
                        text = if (StaffLabelPdfGenerator.isPdfExportSupported) "مشاركة" else "غير مدعوم على النظام",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
