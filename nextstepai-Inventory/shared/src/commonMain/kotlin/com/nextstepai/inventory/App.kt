package com.nextstepai.inventory

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.nextstepai.inventory.data.AppThemeMode
import com.nextstepai.inventory.ui.*
import com.nextstepai.inventory.ui.theme.AppIcons
import com.nextstepai.inventory.ui.theme.AppTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.nav_management
import nextstepai_inventory.shared.generated.resources.nav_settings
import nextstepai_inventory.shared.generated.resources.nav_warehouse

private enum class Screen {
    WAREHOUSE,
    MANAGEMENT,
    SETTINGS
}

@Composable
@Preview
fun App() {
    var themeMode by remember { mutableStateOf(AppThemeMode.SYSTEM) }

    AppTheme(themeMode = themeMode) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
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
                var currentScreen by remember { mutableStateOf(Screen.WAREHOUSE) }

                if (loginUiState.isLoggedIn) {
                    NavigationSuiteScaffold(
                        navigationSuiteItems = {
                            item(
                                selected = currentScreen == Screen.WAREHOUSE,
                                onClick = { currentScreen = Screen.WAREHOUSE },
                                icon = {
                                    Icon(
                                        painter = painterResource(AppIcons.Warehouse),
                                        contentDescription = stringResource(Res.string.nav_warehouse),
                                        tint = if (currentScreen == Screen.WAREHOUSE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = { Text(stringResource(Res.string.nav_warehouse)) }
                            )
                            item(
                                selected = currentScreen == Screen.MANAGEMENT,
                                onClick = { currentScreen = Screen.MANAGEMENT },
                                icon = {
                                    Icon(
                                        painter = painterResource(AppIcons.Management),
                                        contentDescription = stringResource(Res.string.nav_management),
                                        tint = if (currentScreen == Screen.MANAGEMENT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = { Text(stringResource(Res.string.nav_management)) }
                            )
                            item(
                                selected = currentScreen == Screen.SETTINGS,
                                onClick = { currentScreen = Screen.SETTINGS },
                                icon = {
                                    Icon(
                                        painter = painterResource(AppIcons.Settings),
                                        contentDescription = stringResource(Res.string.nav_settings),
                                        tint = if (currentScreen == Screen.SETTINGS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = { Text(stringResource(Res.string.nav_settings)) }
                            )
                        }
                    ) {
                        when (currentScreen) {
                            Screen.WAREHOUSE -> {
                                WarehouseScreen(
                                    partViewModel = partViewModel,
                                    stockViewModel = stockViewModel,
                                    buildOrderViewModel = buildViewModel,
                                    bomViewModel = bomViewModel,
                                    onBackClick = { currentScreen = Screen.WAREHOUSE }
                                )
                            }
                            Screen.MANAGEMENT -> {
                                ManagementScreen(
                                    companyViewModel = companyViewModel,
                                    purchaseOrderViewModel = poViewModel,
                                    onBackClick = { currentScreen = Screen.MANAGEMENT }
                                )
                            }
                            Screen.SETTINGS -> {
                                SettingsScreen(
                                    loginUiState = loginUiState,
                                    themeMode = themeMode,
                                    onThemeModeChange = { themeMode = it },
                                    onLogoutClick = {
                                        currentScreen = Screen.WAREHOUSE
                                        loginViewModel.performLogout()
                                    }
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
