package com.nextstepai.inventory.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nextstepai.inventory.ui.theme.AppIcons
import org.jetbrains.compose.resources.stringResource
import nextstepai_inventory.shared.generated.resources.*

enum class ProductionTab {
    BUILDS,
    BOM
}

/**
 * شاشة الإنتاج المجمعة: تضم شريطي تبويب علويين (أوامر التصنيع / قائمة المواد BOM).
 */
@Composable
fun ProductionScreen(
    buildOrderViewModel: BuildOrderViewModel,
    bomViewModel: BomViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ProductionTab.BUILDS) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // الشريط العلوي الأساسي للتبويب (Top Tab Row)
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == ProductionTab.BUILDS,
                    onClick = { selectedTab = ProductionTab.BUILDS },
                    text = {
                        Text(
                            text = stringResource(Res.string.subtab_builds),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedTab == ProductionTab.BUILDS) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = AppIcons.Builds,
                            contentDescription = stringResource(Res.string.subtab_builds),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )

                Tab(
                    selected = selectedTab == ProductionTab.BOM,
                    onClick = { selectedTab = ProductionTab.BOM },
                    text = {
                        Text(
                            text = stringResource(Res.string.subtab_bom),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedTab == ProductionTab.BOM) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = AppIcons.Bom,
                            contentDescription = stringResource(Res.string.subtab_bom),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    ProductionTab.BUILDS -> {
                        BuildOrderScreen(
                            viewModel = buildOrderViewModel,
                            onBackClick = onBackClick
                        )
                    }
                    ProductionTab.BOM -> {
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
