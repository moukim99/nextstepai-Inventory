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
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.sharePdfPayload
import com.nextstepai.inventory.util.PartLabelPdfGenerator
import com.nextstepai.inventory.data.getLabelDiffDetails
import com.nextstepai.inventory.data.isLabelStale
import com.nextstepai.inventory.data.labelSnapshotData
import com.nextstepai.inventory.util.DateTimeUtils

/**
 * مكون ملصق طباعة القطعة والمكون الفيزيائي القياسي بالباركود الخطي (Physical Part Label Card 100x60mm).
 * يعرض البيانات التعريفية الثابتة للصنف: كود الـ IPN المقروء بالعين، نوع الطبيعة، اسم القطعة، التصنيف، تاريخ التصدير، والباركود الخطي 1D (Code 128).
 * تم استبعاد رمز الـ QR Code بالكامل لمنع الازدواجية البصرية وحصره في ملصقات المواقع والتخزين.
 */
@Composable
fun PrintablePartLabel(
    part: Part,
    modifier: Modifier = Modifier,
    categoryName: String? = null,
    issueDate: String = DateTimeUtils.getCurrentDate()
) {
    val ipnCode = remember(part.ipn, part.id) {
        part.ipn.ifBlank { "IPN-${part.id}" }
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
            // 1. Header Bar: كبسولة نوع الطبيعة (تجميع/مكوّن/منتج) + كبسولة كود الـ IPN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (natureText, natureBg, natureFg) = when {
                    part.assembly -> Triple("تجميع BOM", Color(0xFFF3E8FF), Color(0xFF7E22CE))
                    part.salable -> Triple("منتج تجاري", Color(0xFFE0F2FE), Color(0xFF0369A1))
                    else -> Triple("مكوّن أولي", Color(0xFFECFDF5), Color(0xFF047857))
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = natureBg,
                    border = BorderStroke(1.dp, natureFg.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = natureText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        ),
                        color = natureFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                ) {
                    Text(
                        text = "IPN: $ipnCode",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF3730A3),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // 2. Middle Body: اسم القطعة بخط عريض، وأسفله التصنيف بخط رمادي ناعم
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = part.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!categoryName.isNullOrBlank()) {
                    Text(
                        text = "التصنيف: $categoryName",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3. Visual Core: باركود خطي 1D (Code 128) ممتد أفقياً + التذييل بتاريخ التصدير (استبعاد حقول المخزون والـ QR)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // الباركود الخطي 1D الممتد أفقياً بارتفاع واضح
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
                        content = ipnCode,
                        modifier = Modifier.fillMaxSize(),
                        showText = true
                    )
                }

                // Footer: تاريخ الاصدار فقط مع استبعاد الرصيد المتغير
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
 * شاشة منبثقة من الأسفل لمعاينة بطاقة ملصق القطعة بالباركود الخطي (Code 128) وكشف الفروقات وإعادة التوليد.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintablePartLabelBottomSheet(
    part: Part,
    categoryName: String? = null,
    onDismiss: () -> Unit,
    onPrintClick: (() -> Unit)? = null,
    onRegenerateLabel: (() -> Unit)? = null
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
                            text = "معاينة بطاقة ملصق القطعة والباركود الخطي (1D Barcode)",
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

            // Stale Label Detection & Warning Diff Bar
            if (part.isLabelStale) {
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
                                text = "⚠️ بيانات الملصق قديمة (تم تعديل بيانات القطعة في النظام)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF92400E)
                            )
                        }

                        val snapshotData = part.labelSnapshotData?.replace("|", " | ") ?: "غير مؤرخة"
                        val diffDetails = part.getLabelDiffDetails(categoryName)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "• أسباب التحذير والفروقات: ${diffDetails.joinToString(" • ")}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "• البيانات المطبوعة سابقاً: $snapshotData",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                color = Color(0xFFB45309)
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

            // Printable Part Card Preview with Interactive Zoom Scale
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(zoomScale)
            ) {
                PrintablePartLabel(
                    part = part,
                    categoryName = categoryName,
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
                        text = "هذا الملصق الفيزيائي يحتوي على كود الـ IPN والباركود الخطي 1D الخاص بالقطعة. مسح الباركود يفتح بطاقة بيانات القطعة والتفاصيل الحية مباشرة في التطبيق.",
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
                        val pdfBytes = PartLabelPdfGenerator.generatePdf(part, categoryName ?: "", issueDate)
                        val previewPng = PartLabelPdfGenerator.generatePngPreview(part, categoryName ?: "", issueDate)
                        val cleanFileName = "ملصق_قطعة_${part.name.replace(" ", "_")}.pdf"
                        sharePdfPayload(
                            pdfBytes = pdfBytes,
                            fileName = cleanFileName,
                            title = "مشاركة ملصق قطعة - ${part.name}",
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
