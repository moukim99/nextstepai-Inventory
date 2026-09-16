package com.nextstepai.inventory.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextstepai.inventory.ui.theme.AppIcons
import org.jetbrains.compose.resources.stringResource
import nextstepai_inventory.shared.generated.resources.*

private enum class WarehouseSubView {
    MAIN_DASHBOARD,
    STOCK,
    PARTS
}

/**
 * شاشة المستودع الرئيسية: تعرض صفحة هبوط ذات بطاقتين تفاعليتين
 * تتيحان الانتقال المستقل إلى (قسم المخزون والمواقع) أو (قسم دليل القطع والمكونات).
 */
@Composable
fun WarehouseScreen(
    partViewModel: PartViewModel,
    stockViewModel: StockViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onScanClick: () -> Unit = {}
) {
    var currentSubView by remember { mutableStateOf(WarehouseSubView.MAIN_DASHBOARD) }

    val stockUiState by stockViewModel.uiState.collectAsState()
    val partUiState by partViewModel.uiState.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentSubView,
            label = "WarehouseSubViewTransition"
        ) { subView ->
            when (subView) {
                WarehouseSubView.MAIN_DASHBOARD -> {
                    WarehouseMainDashboard(
                        stockItemsCount = stockUiState.stockItems.size,
                        locationsCount = stockUiState.locations.size,
                        partsCount = partUiState.parts.size,
                        categoriesCount = partUiState.categories.size,
                        onOpenStockClick = { currentSubView = WarehouseSubView.STOCK },
                        onOpenPartsClick = { currentSubView = WarehouseSubView.PARTS }
                    )
                }
                WarehouseSubView.STOCK -> {
                    StockScreen(
                        viewModel = stockViewModel,
                        onBackClick = { currentSubView = WarehouseSubView.MAIN_DASHBOARD }
                    )
                }
                WarehouseSubView.PARTS -> {
                    PartManagementScreen(
                        viewModel = partViewModel,
                        onBackClick = { currentSubView = WarehouseSubView.MAIN_DASHBOARD }
                    )
                }
            }
        }
    }
}

@Composable
private fun WarehouseMainDashboard(
    stockItemsCount: Int,
    locationsCount: Int,
    partsCount: Int,
    categoriesCount: Int,
    onOpenStockClick: () -> Unit,
    onOpenPartsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. هيدر عنوان قسم المستودع
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Warehouse,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.nav_warehouse),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "مركز إدارة المكونات والوحدات المادية والمواقع التخزينية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "أقسام المستودع الرئيسية:",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // 2. بطاقة قسم المخزون والمواقع (Stock & Locations Card)
        ElevatedCard(
            onClick = onOpenStockClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Stock,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.card_stock_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(Res.string.card_stock_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("📦 $stockItemsCount وحدة") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFEEF2FF),
                                labelColor = Color(0xFF3730A3)
                            ),
                            border = null
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("📍 $locationsCount موقع") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF334155)
                            ),
                            border = null
                        )
                    }

                    Button(
                        onClick = onOpenStockClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.open_stock),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // 3. بطاقة قسم دليل القطع والمكونات (Parts Catalog Card)
        ElevatedCard(
            onClick = onOpenPartsClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Parts,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.card_parts_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(Res.string.card_parts_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(24.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("🧩 $partsCount قطعة") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFECFDF5),
                                labelColor = Color(0xFF065F46)
                            ),
                            border = null
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("🏷️ $categoriesCount فئات") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF334155)
                            ),
                            border = null
                        )
                    }

                    Button(
                        onClick = onOpenPartsClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.open_parts),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
