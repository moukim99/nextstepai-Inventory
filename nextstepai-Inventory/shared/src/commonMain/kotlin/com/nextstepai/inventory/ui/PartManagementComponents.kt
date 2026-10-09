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
 * اختيار الأيقونة واللون الديناميكي للقطعة بناءً على النوع والتصنيف والوسوم.
 */
internal fun getPartIconAndColors(part: Part, categoryName: String?): Triple<Color, Color, ImageVector> {
    val cat = categoryName.orEmpty().lowercase()
    val name = part.name.lowercase()

    return when {
        part.assembly || name.contains("bom") || name.contains("تجميع") ->
            Triple(Color(0xFFF3E8FF), Color(0xFF9333EA), Icons.Default.AccountTree)

        part.salable || name.contains("جاهز") || name.contains("منتج") ->
            Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), Icons.Default.ShoppingBag)

        part.isTemplate || name.contains("قالب") || name.contains("template") ->
            Triple(Color(0xFFCCFBF1), Color(0xFF0D9488), Icons.Default.Extension)

        cat.contains("شاشة") || name.contains("display") || name.contains("lcd") || name.contains("screen") ->
            Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), Icons.Default.Tv)

        cat.contains("كابل") || cat.contains("أسلاك") || name.contains("cable") || name.contains("wire") ->
            Triple(Color(0xFFFFEDD5), Color(0xFFEA580C), Icons.Default.Cable)

        cat.contains("كيميائي") || cat.contains("سوائل") || name.contains("chemical") || name.contains("fluid") ->
            Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Icons.Default.Science)

        cat.contains("ميكانيك") || cat.contains("صلب") || name.contains("screw") || name.contains("metal") ->
            Triple(Color(0xFFF1F5F9), Color(0xFF475569), Icons.Default.Hardware)

        else ->
            Triple(Color(0xFFEEF2FF), Color(0xFF4F46E5), Icons.Default.Memory)
    }
}


/**
 * حساب ميزان ونسبة اكتمال بيانات الصنف (Gamification / Data Completeness Score)
 */
fun getCompletenessTone(score: Int): CompletenessTone = when {
    score <= 30 -> CompletenessTone.RED
    score in 31..99 -> CompletenessTone.ORANGE
    else -> CompletenessTone.GREEN
}


fun calculatePartCompletenessScore(
    name: String,
    ipn: String,
    categoryId: Long?,
    units: String,
    minimumStock: Double,
    hasNature: Boolean,
    defaultLocationId: Long? = null,
    isTemplateOrVariant: Boolean = false,
    hasAdvancedData: Boolean = false
): Int {
    var score = 0
    if (name.isNotBlank()) score += 15
    if (ipn.isNotBlank()) score += 15
    if (units.isNotBlank()) score += 15
    if (minimumStock > 0.0) score += 15
    if (categoryId != null) score += 10
    if (hasNature) score += 10
    if (defaultLocationId != null) score += 10
    if (isTemplateOrVariant) score += 5
    if (hasAdvancedData) score += 5
    return score
}


fun Part.calculateCompletenessScore(): Int {
    var score = 0
    if (name.isNotBlank()) score += 15
    if (ipn.isNotBlank()) score += 15
    if (units.isNotBlank()) score += 15
    if (minimumStock > 0.0) score += 15
    if (categoryId != null) score += 10
    if (component || assembly || salable) score += 10
    if (defaultLocationId != null) score += 10
    if (isTemplate || variantOfId != null) score += 5
    if (description.isNotBlank() || keywords.isNotBlank() || link.isNotBlank() || imageUrl?.isNotBlank() == true) score += 5
    return score
}


@Composable
internal fun PartCircularCompletionBadge(
    percentage: Int,
    colorTone: CompletenessTone,
    modifier: Modifier = Modifier
) {
    val progress = (percentage / 100f).coerceIn(0f, 1f)
    val strokeWidth = 3.dp

    val strokeColor = when (colorTone) {
        CompletenessTone.RED -> Color(0xFFDC2626)
        CompletenessTone.ORANGE -> Color(0xFFD97706)
        CompletenessTone.GREEN -> Color(0xFF059669)
    }
    val trackColor = when (colorTone) {
        CompletenessTone.RED -> Color(0xFFFEE2E2)
        CompletenessTone.ORANGE -> Color(0xFFFEF3C7)
        CompletenessTone.GREEN -> Color(0xFFD1FAE5)
    }
    val textColor = when (colorTone) {
        CompletenessTone.RED -> Color(0xFF991B1B)
        CompletenessTone.ORANGE -> Color(0xFF92400E)
        CompletenessTone.GREEN -> Color(0xFF065F46)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(38.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // 1. Background Track Arc
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // 2. Clockwise Progress Arc (Starts at -90 degrees / top)
            drawArc(
                color = strokeColor,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "%$percentage",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = textColor
        )
    }
}


internal fun formatStockNumber(value: Double): String {
    return if (value % 1.0 == 0.0) "${value.toLong()}.0" else "$value"
}


/**
 * بطاقة عرض القطعة PartItemCard المعاد هيكلتها وفق لغة تصميم بطاقة التخزين المعتمدة بنظام الطبقتين السفليتين
 */
@Composable
internal fun PartItemCard(
    part: Part,
    categoryName: String? = null,
    stockItems: List<StockItem> = emptyList(),
    stockLocations: List<StockLocation> = emptyList(),
    isStarred: Boolean = false,
    onToggleStar: () -> Unit = {},
    onAddStock: ((Part) -> Unit)? = null,
    onEditPart: ((Part) -> Unit)? = null,
    onDeletePart: ((Part) -> Unit)? = null,
    onPrintLabel: ((Part) -> Unit)? = null,
    onShowLocations: ((Part) -> Unit)? = null,
    onShowAllocations: ((Part) -> Unit)? = null,
    onClick: () -> Unit
) {
    val completenessScore = remember(part) { part.calculateCompletenessScore() }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ==========================================
            // 1. Header Row (الرأس العلوي للمعلومات الأساسية)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val (natureIcon, natureBg, natureFg) = when {
                    part.assembly -> Triple(Icons.Default.AccountTree, Color(0xFFF3E8FF), Color(0xFF7E22CE))
                    part.salable -> Triple(Icons.Default.ShoppingBag, Color(0xFFE0F2FE), Color(0xFF0369A1))
                    else -> Triple(Icons.Default.Memory, Color(0xFFECFDF5), Color(0xFF047857))
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(natureBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = natureIcon,
                        contentDescription = null,
                        tint = natureFg,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = part.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (part.ipn.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEEF2FF),
                                border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                            ) {
                                Text(
                                    text = "IPN: ${part.ipn}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF3730A3),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }

                        if (!categoryName.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 10.5.sp
                                    ),
                                    color = Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                val colorTone = getCompletenessTone(completenessScore)
                PartCircularCompletionBadge(
                    percentage = completenessScore,
                    colorTone = colorTone
                )
            }

            if (part.description.isNotBlank()) {
                Text(
                    text = part.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ==========================================
            // Inventory Progress Bar (شريط نسبة المخزون)
            // ==========================================
            val showStockProgressBar = part.units.isNotBlank() && part.minimumStock > 0.0

            if (showStockProgressBar) {
                val maxStock = part.maximumStock
                val isOverstock = maxStock != null && maxStock > 0.0 && part.totalInStock > maxStock
                val isShortage = part.totalInStock < part.minimumStock

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نسبة المخزون:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isOverstock) {
                            val maxStockVal = maxStock
                            val overstockPct = (((part.totalInStock - maxStockVal) / maxStockVal) * 100).toInt()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF3E8FF),
                                    border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                                ) {
                                    Text(
                                        text = "تكدس +$overstockPct%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = Color(0xFF7E22CE),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = "(${part.units} ${formatStockNumber(part.totalInStock)} / ${formatStockNumber(maxStockVal)} max)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF7E22CE)
                                )
                            }
                        } else if (isShortage) {
                            val stockRatio = (part.totalInStock / part.minimumStock).toFloat().coerceIn(0f, 1f)
                            val stockPct = (stockRatio * 100).toInt()
                            Text(
                                text = "$stockPct% (${part.units} ${formatStockNumber(part.totalInStock)} / ${formatStockNumber(part.minimumStock)} min)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFD97706)
                            )
                        } else {
                            val surplusPct = (((part.totalInStock - part.minimumStock) / part.minimumStock) * 100).toInt()
                            Text(
                                text = "100% (+$surplusPct%) (${part.units} ${formatStockNumber(part.totalInStock)} / ${formatStockNumber(part.minimumStock)} min)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF059669)
                            )
                        }
                    }

                    val (barRatio, barColor, trackColor) = when {
                        isOverstock -> Triple(
                            1.0f,
                            Color(0xFF7C3AED),
                            Color(0xFFF3E8FF)
                        )
                        isShortage -> {
                            val ratio = (part.totalInStock / part.minimumStock).toFloat().coerceIn(0f, 1f)
                            Triple(
                                ratio,
                                Color(0xFFD97706),
                                Color(0xFFFEF3C7)
                            )
                        }
                        else -> Triple(
                            1.0f,
                            Color(0xFF2563EB),
                            Color(0xFFDBEAFE)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { barRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = barColor,
                        trackColor = trackColor,
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                }
            }

            // ==========================================
            // Tier 1: Storage Locations & Stock Status Row (الطبقة التشغيلية الأولى)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Storage Locations Button (زر / كبسولة مواقع التخزين - الجهة اليمنى فوق زر استلام مخزون)
                val partStockItems = remember(stockItems, part.id) { stockItems.filter { it.partId == part.id } }
                val uniqueLocIds = remember(partStockItems) { partStockItems.mapNotNull { it.locationId }.distinct() }
                val locationsCount = uniqueLocIds.size
                val locationLabel = remember(uniqueLocIds, stockLocations) {
                    when {
                        locationsCount == 1 -> {
                            val locName = stockLocations.find { it.id == uniqueLocIds.first() }?.name
                            if (locName != null) "📍 $locName ▾" else "📍 1 موقع ▾"
                        }
                        locationsCount > 1 -> "📍 $locationsCount مواقع ▾"
                        else -> "📍 0 مواقع ▾"
                    }
                }

                Surface(
                    onClick = { onShowLocations?.invoke(part) },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                    modifier = Modifier
                        .height(40.dp)
                        .defaultMinSize(minWidth = 132.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = locationLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = Color(0xFF3730A3)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 2. Stock Breakdown Badge (ترقية شارة المتاح والمحجوز إلى مركز تحكم تفاعلي)
                Surface(
                    onClick = { onShowAllocations?.invoke(part) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (part.isLowStock) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, if (part.isLowStock) Color(0xFFFCA5A5) else Color(0xFFBBF7D0)),
                    modifier = Modifier
                        .height(40.dp)
                        .weight(1f, fill = false)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val reserved = part.committedAllocated
                        val badgeText = if (reserved > 0) {
                            "المتاح: ${part.availableStock} | محجوز: $reserved"
                        } else {
                            "المتاح: ${part.availableStock}"
                        }

                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (part.isLowStock) Color(0xFFDC2626) else Color(0xFF047857)
                        )

                        if (part.isLowStock) {
                            Text(
                                text = "⚠️ < الحد",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = Color(0xFFDC2626)
                            )
                        }

                        Text(
                            text = "▾",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (part.isLowStock) Color(0xFFDC2626) else Color(0xFF047857)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ==========================================
            // Tier 2: Actions & Operations Row (الطبقة السفلية الثانية)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Primary Operational Action (الجهة اليمنى في الواجهة العربية): [+ استلام مخزون]
                Button(
                    onClick = { onAddStock?.invoke(part) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(40.dp)
                        .defaultMinSize(minWidth = 132.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "استلام مخزون",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "استلام مخزون",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // 2. Controls & Management (الجهة اليسرى في الواجهة العربية)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite Star Capsule
                    Surface(
                        onClick = onToggleStar,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isStarred) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isStarred) Color(0xFFFDE68A) else Color(0xFFE2E8F0)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "تفضيل",
                                tint = if (isStarred) Color(0xFFD97706) else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            if (isStarred) {
                                Text(
                                    text = "مفضلة",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp
                                    ),
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                    }

                    // Barcode / QR Label Action
                    Surface(
                        onClick = { onPrintLabel?.invoke(part) },
                        shape = CircleShape,
                        color = Color(0xFFF3E8FF),
                        border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "ملصق وباركود القطعة",
                                tint = Color(0xFF7E22CE),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Edit Action
                    Surface(
                        onClick = { onEditPart?.invoke(part) },
                        shape = CircleShape,
                        color = Color(0xFFEEF2FF),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل القطعة",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Delete Action
                    Surface(
                        onClick = { onDeletePart?.invoke(part) },
                        shape = CircleShape,
                        color = Color(0xFFFDE8E8),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "حذف القطعة",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
internal fun ConfirmDeletePartDialog(
    partName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "حذف القطعة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Text(
                text = "هل أنت تأكد من رغبتك في حذف القطعة \"$partName\"؟ ستتم أرشفة القطعة وقد تتأثر السجلات المرتبطة بها.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("تأكيد الحذف", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfirmDeletePartLinkDialog(
    itemTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
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
            Text("تأكيد الحذف ⚠️", fontWeight = FontWeight.Bold, color = Color(0xFFE11D48), style = MaterialTheme.typography.titleMedium)
            Text("هل أنت تأكد من إزالة '$itemTitle'؟\n\nتنبيه: في حال كان هذا السجل مرتبطاً بأسعار سابقة أو أوامر شراء، فقد تتأثر تقارير التكلفة المرتبطة به.", style = MaterialTheme.typography.bodyMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("إلغاء") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("حذف")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}




internal data class StockPackagingOption(
    val code: String,
    val labelAr: String,
    val descAr: String
)
