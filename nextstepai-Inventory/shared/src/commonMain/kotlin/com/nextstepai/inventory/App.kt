package com.nextstepai.inventory

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.nextstepai.inventory.data.AppThemeMode
import com.nextstepai.inventory.ui.BomScreen
import com.nextstepai.inventory.ui.BomViewModel
import com.nextstepai.inventory.ui.BuildOrderScreen
import com.nextstepai.inventory.ui.BuildOrderViewModel
import com.nextstepai.inventory.ui.CompanyScreen
import com.nextstepai.inventory.ui.CompanyViewModel
import com.nextstepai.inventory.ui.LoginScreen
import com.nextstepai.inventory.ui.LoginViewModel
import com.nextstepai.inventory.ui.MainDashboardScreen
import com.nextstepai.inventory.ui.PartManagementScreen
import com.nextstepai.inventory.ui.PartViewModel
import com.nextstepai.inventory.ui.PurchaseOrderScreen
import com.nextstepai.inventory.ui.PurchaseOrderViewModel
import com.nextstepai.inventory.ui.StockScreen
import com.nextstepai.inventory.ui.StockViewModel
import com.nextstepai.inventory.ui.theme.AppIcons
import com.nextstepai.inventory.ui.theme.AppTheme
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.nav_bom
import nextstepai_inventory.shared.generated.resources.nav_builds
import nextstepai_inventory.shared.generated.resources.nav_companies
import nextstepai_inventory.shared.generated.resources.nav_dashboard
import nextstepai_inventory.shared.generated.resources.nav_orders
import nextstepai_inventory.shared.generated.resources.nav_parts
import nextstepai_inventory.shared.generated.resources.nav_stock

private enum class Screen {
    DASHBOARD,
    BUILDS,
    PURCHASE_ORDERS,
    COMPANIES,
    STOCK,
    PARTS,
    BOM
}

@Composable
@Preview
fun App() {
    var themeMode by remember { mutableStateOf(AppThemeMode.SYSTEM) }

    AppTheme(themeMode = themeMode) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val loginViewModel = remember { LoginViewModel() }
                val partViewModel = remember { PartViewModel() }
                val bomViewModel = remember { BomViewModel() }
                val stockViewModel = remember { StockViewModel() }
                val companyViewModel = remember { CompanyViewModel() }
                val poViewModel = remember { PurchaseOrderViewModel() }
                val buildViewModel = remember { BuildOrderViewModel() }
                val loginUiState by loginViewModel.uiState.collectAsState()
                var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

                if (loginUiState.isLoggedIn) {
                    NavigationSuiteScaffold(
                        navigationSuiteItems = {
                            item(
                                selected = currentScreen == Screen.DASHBOARD,
                                onClick = { currentScreen = Screen.DASHBOARD },
                                icon = {
                                    Icon(
                                        painter = painterResource(AppIcons.Dashboard),
                                        contentDescription = stringResource(Res.string.nav_dashboard),
                                        tint = if (currentScreen == Screen.DASHBOARD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = { Text(stringResource(Res.string.nav_dashboard)) }
                            )
                            item(
                                selected = currentScreen == Screen.PARTS,
                                onClick = { currentScreen = Screen.PARTS },
                                icon = {
                                    Icon(
                                        painter = painterResource(AppIcons.Parts),
                                        contentDescription = stringResource(Res.string.nav_parts),
                                        tint = if (currentScreen == Screen.PARTS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = { Text(stringResource(Res.string.nav_parts)) }
                            )
                            item(
                                selected = currentScreen == Screen.STOCK,
                                onClick = { currentScreen = Screen.STOCK },
                                icon = {
                                    Icon(
                                        painter = painterResource(AppIcons.Stock),
                                        contentDescription = stringResource(Res.string.nav_stock),
                                        tint = if (currentScreen == Screen.STOCK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = { Text(stringResource(Res.string.nav_stock)) }
                            )
                            item(
                                selected = currentScreen == Screen.COMPANIES,
                                onClick = { currentScreen = Screen.COMPANIES },
                                icon = {
                                    Icon(
                                        painter = painterResource(AppIcons.Companies),
                                        contentDescription = stringResource(Res.string.nav_companies),
                                        tint = if (currentScreen == Screen.COMPANIES) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = { Text(stringResource(Res.string.nav_companies)) }
                            )
                            item(
                                selected = currentScreen == Screen.PURCHASE_ORDERS,
                                onClick = { currentScreen = Screen.PURCHASE_ORDERS },
                                icon = {
                                    Icon(
                                        painter = painterResource(AppIcons.Orders),
                                        contentDescription = stringResource(Res.string.nav_orders),
                                        tint = if (currentScreen == Screen.PURCHASE_ORDERS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = { Text(stringResource(Res.string.nav_orders)) }
                            )
                            item(
                                selected = currentScreen == Screen.BUILDS,
                                onClick = { currentScreen = Screen.BUILDS },
                                icon = {
                                    Icon(
                                        painter = painterResource(AppIcons.Builds),
                                        contentDescription = stringResource(Res.string.nav_builds),
                                        tint = if (currentScreen == Screen.BUILDS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = { Text(stringResource(Res.string.nav_builds)) }
                            )
                            item(
                                selected = currentScreen == Screen.BOM,
                                onClick = { currentScreen = Screen.BOM },
                                icon = {
                                    Icon(
                                        painter = painterResource(AppIcons.Bom),
                                        contentDescription = stringResource(Res.string.nav_bom),
                                        tint = if (currentScreen == Screen.BOM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = { Text(stringResource(Res.string.nav_bom)) }
                            )
                        }
                    ) {
                        when (currentScreen) {
                            Screen.BUILDS -> {
                                BuildOrderScreen(
                                    viewModel = buildViewModel,
                                    onBackClick = { currentScreen = Screen.DASHBOARD }
                                )
                            }
                            Screen.PURCHASE_ORDERS -> {
                                PurchaseOrderScreen(
                                    viewModel = poViewModel,
                                    onBackClick = { currentScreen = Screen.DASHBOARD }
                                )
                            }
                            Screen.COMPANIES -> {
                                CompanyScreen(
                                    viewModel = companyViewModel,
                                    onBackClick = { currentScreen = Screen.DASHBOARD }
                                )
                            }
                            Screen.STOCK -> {
                                StockScreen(
                                    viewModel = stockViewModel,
                                    onBackClick = { currentScreen = Screen.DASHBOARD }
                                )
                            }
                            Screen.PARTS -> {
                                PartManagementScreen(
                                    viewModel = partViewModel,
                                    onBackClick = { currentScreen = Screen.DASHBOARD }
                                )
                            }
                            Screen.BOM -> {
                                BomScreen(
                                    viewModel = bomViewModel,
                                    onBackClick = { currentScreen = Screen.DASHBOARD }
                                )
                            }
                            Screen.DASHBOARD -> {
                                MainDashboardScreen(
                                    loginUiState = loginUiState,
                                    onLogoutClick = {
                                        currentScreen = Screen.DASHBOARD
                                        loginViewModel.performLogout()
                                    },
                                    onOpenPartsClick = { currentScreen = Screen.PARTS },
                                    onOpenBomClick = { currentScreen = Screen.BOM },
                                    onOpenStockClick = { currentScreen = Screen.STOCK },
                                    onOpenCompaniesClick = { currentScreen = Screen.COMPANIES },
                                    onOpenOrdersClick = { currentScreen = Screen.PURCHASE_ORDERS },
                                    onOpenBuildsClick = { currentScreen = Screen.BUILDS }
                                )
                            }
                        }
                    }
                } else {
                    LoginScreen(
                        onLoginClick = { loginViewModel.performSimpleLogin() },
                        isLoading = loginUiState.isLoading
                    )
                }
            }
        }
    }
}
