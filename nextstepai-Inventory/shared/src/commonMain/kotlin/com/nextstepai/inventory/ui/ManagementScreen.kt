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

enum class ManagementTab {
    COMPANIES,
    PURCHASE_ORDERS
}

/**
 * شاشة الإدارة المجمعة: تضم شريطي تبويب علويين (الشركات والعلاقات / أوامر الشراء وبنودها).
 */
@Composable
fun ManagementScreen(
    companyViewModel: CompanyViewModel,
    purchaseOrderViewModel: PurchaseOrderViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ManagementTab.COMPANIES) }

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
                    selected = selectedTab == ManagementTab.COMPANIES,
                    onClick = { selectedTab = ManagementTab.COMPANIES },
                    text = {
                        Text(
                            text = stringResource(Res.string.tab_companies),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedTab == ManagementTab.COMPANIES) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = AppIcons.Companies,
                            contentDescription = stringResource(Res.string.tab_companies),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )

                Tab(
                    selected = selectedTab == ManagementTab.PURCHASE_ORDERS,
                    onClick = { selectedTab = ManagementTab.PURCHASE_ORDERS },
                    text = {
                        Text(
                            text = stringResource(Res.string.tab_orders),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedTab == ManagementTab.PURCHASE_ORDERS) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = AppIcons.Orders,
                            contentDescription = stringResource(Res.string.tab_orders),
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
                    ManagementTab.COMPANIES -> {
                        CompanyScreen(
                            viewModel = companyViewModel,
                            onBackClick = onBackClick
                        )
                    }
                    ManagementTab.PURCHASE_ORDERS -> {
                        PurchaseOrderScreen(
                            viewModel = purchaseOrderViewModel,
                            onBackClick = onBackClick
                        )
                    }
                }
            }
        }
    }
}
