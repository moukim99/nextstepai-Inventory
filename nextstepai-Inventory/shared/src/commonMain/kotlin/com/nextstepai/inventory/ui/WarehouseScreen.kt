package com.nextstepai.inventory.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.nextstepai.inventory.ui.theme.AppIcons
import org.jetbrains.compose.resources.stringResource
import nextstepai_inventory.shared.generated.resources.*

enum class WarehouseTab {
    INVENTORY,
    PRODUCTION
}

enum class InventorySubTab {
    PARTS,
    STOCK
}

enum class ProductionSubTab {
    BUILDS,
    BOM
}

/**
 * شاشة المستودع المجمعة: تضم هيدر لوحة التحكم الملخصة القابلة للطي،
 * والأشرطة العلوية للمخزون (القطع والمواقع) والإنتاج (أوامر التصنيع وقائمة المواد BOM).
 */
@Composable
fun WarehouseScreen(
    partViewModel: PartViewModel,
    stockViewModel: StockViewModel,
    buildOrderViewModel: BuildOrderViewModel,
    bomViewModel: BomViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onScanClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(WarehouseTab.INVENTORY) }
    var selectedInventorySubTab by remember { mutableStateOf(InventorySubTab.PARTS) }
    var selectedProductionSubTab by remember { mutableStateOf(ProductionSubTab.BUILDS) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. الشريط العلوي الرئيسي للتبويب (Top Primary Tab Row: المخزون | الإنتاج)
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color.White,
                contentColor = Color(0xFF4F46E5),
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTab.ordinal),
                        color = Color(0xFF4F46E5),
                        width = 48.dp
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == WarehouseTab.INVENTORY,
                    onClick = { selectedTab = WarehouseTab.INVENTORY },
                    selectedContentColor = Color(0xFF4F46E5),
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            text = stringResource(Res.string.tab_inventory),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedTab == WarehouseTab.INVENTORY) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = stringResource(Res.string.tab_inventory),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )

                Tab(
                    selected = selectedTab == WarehouseTab.PRODUCTION,
                    onClick = { selectedTab = WarehouseTab.PRODUCTION },
                    selectedContentColor = Color(0xFF4F46E5),
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            text = stringResource(Res.string.tab_production),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedTab == WarehouseTab.PRODUCTION) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = stringResource(Res.string.tab_production),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            // 3. الشريط الفرعي التكتيكي المقسم (Segmented Controls)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selectedTab) {
                        WarehouseTab.INVENTORY -> {
                            val isPartsSelected = selectedInventorySubTab == InventorySubTab.PARTS
                            Surface(
                                onClick = { selectedInventorySubTab = InventorySubTab.PARTS },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isPartsSelected) Color(0xFF4F46E5) else Color.White,
                                border = BorderStroke(1.dp, if (isPartsSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                                shadowElevation = if (isPartsSelected) 2.dp else 0.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Parts,
                                        contentDescription = null,
                                        tint = if (isPartsSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(Res.string.subtab_parts),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isPartsSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val isStockSelected = selectedInventorySubTab == InventorySubTab.STOCK
                            Surface(
                                onClick = { selectedInventorySubTab = InventorySubTab.STOCK },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isStockSelected) Color(0xFF4F46E5) else Color.White,
                                border = BorderStroke(1.dp, if (isStockSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                                shadowElevation = if (isStockSelected) 2.dp else 0.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Stock,
                                        contentDescription = null,
                                        tint = if (isStockSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(Res.string.subtab_stock),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isStockSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        WarehouseTab.PRODUCTION -> {
                            val isBuildsSelected = selectedProductionSubTab == ProductionSubTab.BUILDS
                            Surface(
                                onClick = { selectedProductionSubTab = ProductionSubTab.BUILDS },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isBuildsSelected) Color(0xFF4F46E5) else Color.White,
                                border = BorderStroke(1.dp, if (isBuildsSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                                shadowElevation = if (isBuildsSelected) 2.dp else 0.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Builds,
                                        contentDescription = null,
                                        tint = if (isBuildsSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(Res.string.subtab_builds),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isBuildsSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val isBomSelected = selectedProductionSubTab == ProductionSubTab.BOM
                            Surface(
                                onClick = { selectedProductionSubTab = ProductionSubTab.BOM },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isBomSelected) Color(0xFF4F46E5) else Color.White,
                                border = BorderStroke(1.dp, if (isBomSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                                shadowElevation = if (isBomSelected) 2.dp else 0.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Bom,
                                        contentDescription = null,
                                        tint = if (isBomSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(Res.string.subtab_bom),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isBomSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. المحتوى الفعلي للشاشة المحددة (Screen Content)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    WarehouseTab.INVENTORY -> {
                        when (selectedInventorySubTab) {
                            InventorySubTab.PARTS -> {
                                PartManagementScreen(
                                    viewModel = partViewModel,
                                    onBackClick = onBackClick
                                )
                            }
                            InventorySubTab.STOCK -> {
                                StockScreen(
                                    viewModel = stockViewModel,
                                    onBackClick = onBackClick
                                )
                            }
                        }
                    }
                    WarehouseTab.PRODUCTION -> {
                        when (selectedProductionSubTab) {
                            ProductionSubTab.BUILDS -> {
                                BuildOrderScreen(
                                    viewModel = buildOrderViewModel,
                                    onBackClick = onBackClick
                                )
                            }
                            ProductionSubTab.BOM -> {
                                BomScreen(
                                    viewModel = bomViewModel,
                                    onBackClick = onBackClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
