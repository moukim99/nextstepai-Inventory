package com.nextstepai.inventory.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nextstepai.inventory.ui.theme.AppIcons
import org.jetbrains.compose.resources.painterResource
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
    var isDashboardExpanded by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. شريط ترويسة لوحة التحكم القابل للطي (Collapsible Dashboard KPI Header)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isDashboardExpanded = !isDashboardExpanded }
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "مؤشرات لوحة التحكم والمسح السريع",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = if (isDashboardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isDashboardExpanded) "طي" else "توسيع",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = isDashboardExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OperationalKpiSection()
                            BarcodeScannerActionCard(onScanClick = onScanClick)
                        }
                    }
                }
            }

            // 2. الشريط العلوي الرئيسي للتبويب (Top Primary Tab Row: المخزون | الإنتاج)
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == WarehouseTab.INVENTORY,
                    onClick = { selectedTab = WarehouseTab.INVENTORY },
                    text = {
                        Text(
                            text = stringResource(Res.string.tab_inventory),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedTab == WarehouseTab.INVENTORY) FontWeight.Bold else FontWeight.Normal
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
                    text = {
                        Text(
                            text = stringResource(Res.string.tab_production),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedTab == WarehouseTab.PRODUCTION) FontWeight.Bold else FontWeight.Normal
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

            // 3. الشريط الفرعي التكتيكي (Secondary Sub-Tab / Segmented Controls)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selectedTab) {
                        WarehouseTab.INVENTORY -> {
                            FilterChip(
                                selected = selectedInventorySubTab == InventorySubTab.PARTS,
                                onClick = { selectedInventorySubTab = InventorySubTab.PARTS },
                                label = { Text(stringResource(Res.string.subtab_parts)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(AppIcons.Parts),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedInventorySubTab == InventorySubTab.STOCK,
                                onClick = { selectedInventorySubTab = InventorySubTab.STOCK },
                                label = { Text(stringResource(Res.string.subtab_stock)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(AppIcons.Stock),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        WarehouseTab.PRODUCTION -> {
                            FilterChip(
                                selected = selectedProductionSubTab == ProductionSubTab.BUILDS,
                                onClick = { selectedProductionSubTab = ProductionSubTab.BUILDS },
                                label = { Text(stringResource(Res.string.subtab_builds)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(AppIcons.Builds),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedProductionSubTab == ProductionSubTab.BOM,
                                onClick = { selectedProductionSubTab = ProductionSubTab.BOM },
                                label = { Text(stringResource(Res.string.subtab_bom)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(AppIcons.Bom),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
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
