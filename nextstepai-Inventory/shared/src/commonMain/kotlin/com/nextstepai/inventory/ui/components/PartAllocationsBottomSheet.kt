package com.nextstepai.inventory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.db.PartAllocationEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ورقة تفاصيل الحجوزات والمخصصات (PartAllocationsBottomSheet).
 * تعرض ملخص أرصدة المخزون الحي، قائمة الحجوزات النشطة حسب نوع المرجع،
 * وخيار فك الحجز السريع مع حوار التأكيد التحذيري.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartAllocationsBottomSheet(
    part: Part,
    allocations: List<PartAllocationEntity>,
    onDismiss: () -> Unit,
    onReleaseAllocation: (allocationId: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pendingReleaseAllocation by remember { mutableStateOf<PartAllocationEntity?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // 1. Header with Title & Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "تفاصيل حجوزات ومخصصات القطعة",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${part.name} • (${part.ipn.ifBlank { "بدون IPN" }})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "✕",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Inventory Summary Cards (شريط الملخص الرأسي/الأفقي)
            InventorySummaryCards(part = part)

            Spacer(modifier = Modifier.height(20.dp))

            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الحجوزات والمخصصات النشطة (${allocations.count { it.status == "ACTIVE" }})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (part.isLowStock) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Text(
                            text = "⚠️ أقل من الحد الأدنى (${part.minimumStock} ${part.units})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            ),
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Active Allocations List (`LazyColumn`)
            val activeAllocations = remember(allocations) { allocations.filter { it.status == "ACTIVE" } }

            if (activeAllocations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📦", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد حجوزات أو مخصصات نشطة حالياً",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "كافة كميات المخزون متاح بالكامل للاستخدام الحر",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(activeAllocations, key = { it.id }) { allocation ->
                        AllocationCardItem(
                            allocation = allocation,
                            units = part.units,
                            onReleaseClick = { pendingReleaseAllocation = allocation }
                        )
                    }
                }
            }
        }
    }

    // 4. Quick Release Confirmation Dialog (حوار تأكيد فك الحجز)
    if (pendingReleaseAllocation != null) {
        val targetAlloc = pendingReleaseAllocation!!
        AlertDialog(
            onDismissRequest = { pendingReleaseAllocation = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⚠️ ", fontSize = 18.sp)
                    Text(
                        text = "تأكيد فك حجز المخزون",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Text(
                    text = "هل أنت متأكد من فك حجز ${targetAlloc.allocatedQuantity} ${part.units} الخاصة بـ [${targetAlloc.referenceId}]؟\nسيتم إعادة الكمية فوراً للمخزون الحر وتنبيه الوثيقة التابعة.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idToRelease = targetAlloc.id
                        pendingReleaseAllocation = null
                        onReleaseAllocation(idToRelease)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("نعم، فك الحجز", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingReleaseAllocation = null }) {
                    Text("إلغاء")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * شريط البطاقات الإحصائية للرصيد والمحجوز والمتاح.
 */
@Composable
private fun InventorySummaryCards(part: Part) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. On-Hand Physical Stock
        SummaryStatCard(
            modifier = Modifier.weight(1f),
            label = "المخزون المادي",
            value = "${part.totalInStock}",
            unit = part.units,
            badgeColor = Color(0xFF0F172A),
            backgroundColor = Color(0xFFF1F5F9),
            borderColor = Color(0xFFCBD5E1)
        )

        // 2. Allocated / Committed
        SummaryStatCard(
            modifier = Modifier.weight(1f),
            label = "المحجوز",
            value = "${part.committedAllocated}",
            unit = part.units,
            badgeColor = Color(0xFF0369A1),
            backgroundColor = Color(0xFFE0F2FE),
            borderColor = Color(0xFFBAE6FD)
        )

        // 3. Net Available Free Stock
        SummaryStatCard(
            modifier = Modifier.weight(1f),
            label = "المتاح الحر",
            value = "${part.availableStock}",
            unit = part.units,
            badgeColor = if (part.isLowStock) Color(0xFFB91C1C) else Color(0xFF047857),
            backgroundColor = if (part.isLowStock) Color(0xFFFEF2F2) else Color(0xFFECFDF5),
            borderColor = if (part.isLowStock) Color(0xFFFCA5A5) else Color(0xFFA7F3D0)
        )
    }
}

@Composable
private fun SummaryStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    badgeColor: Color,
    backgroundColor: Color,
    borderColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = badgeColor.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    ),
                    color = badgeColor
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = badgeColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

/**
 * بطاقة عرض حجز منفرد داخل قائمة الحجوزات النشطة.
 */
@Composable
private fun AllocationCardItem(
    allocation: PartAllocationEntity,
    units: String,
    onReleaseClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Right Side Info (in Arabic RTL)
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reference Type Icon
                val refIcon = when (allocation.referenceType) {
                    "BUILD_ORDER" -> "🏭"
                    "SALES_ORDER" -> "🚚"
                    "QC_HOLD" -> "🛡️"
                    "RETURN_ORDER" -> "🔄"
                    else -> "📋"
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = refIcon, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    // السطر الأول: الكود + نوع الحجز + شارة الكمية البارزة
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = allocation.referenceId,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Type Badge (HARD / SOFT)
                        val isHard = allocation.allocationType.equals("HARD", ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isHard) Color(0xFFE0F2FE) else Color(0xFFF1F5F9),
                            border = BorderStroke(0.5.dp, if (isHard) Color(0xFFBAE6FD) else Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = if (isHard) "مؤكد Hard" else "مبدئي Soft",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp
                                ),
                                color = if (isHard) Color(0xFF0369A1) else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // شارة الكمية البارزة
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "${allocation.allocatedQuantity} $units",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // السطر الثاني: بيان أو اسم الوثيقة
                    if (allocation.referenceTitle.isNotBlank()) {
                        Text(
                            text = allocation.referenceTitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // السطر الثالث: التاريخ والمستخدم المسؤول فقط
                    val formattedDate = remember(allocation.createdAt) {
                        runCatching {
                            val instant = Instant.fromEpochMilliseconds(allocation.createdAt)
                            val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                            val year = dt.year
                            val month = dt.month.name.take(3)
                            val day = dt.dayOfMonth
                            val hour = dt.hour.toString().padStart(2, '0')
                            val min = dt.minute.toString().padStart(2, '0')
                            "$year-$month-$day $hour:$min"
                        }.getOrDefault("")
                    }

                    Text(
                        text = "$formattedDate • ${allocation.createdByUserId}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick Release Button (زر فك الحجز السريع)
            Button(
                onClick = onReleaseClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFEF2F2),
                    contentColor = Color(0xFFDC2626)
                ),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "فك الحجز",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
