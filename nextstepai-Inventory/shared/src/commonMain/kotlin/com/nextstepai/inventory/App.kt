package com.nextstepai.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nextstepai.inventory.data.AppThemeMode
import com.nextstepai.inventory.ui.*
import com.nextstepai.inventory.ui.theme.AppIcons
import com.nextstepai.inventory.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.nav_builds
import nextstepai_inventory.shared.generated.resources.nav_management
import nextstepai_inventory.shared.generated.resources.nav_settings
import nextstepai_inventory.shared.generated.resources.nav_warehouse

private enum class Screen {
    WAREHOUSE,
    PRODUCTION,
    MANAGEMENT,
    SETTINGS
}

@Composable
@Preview
fun App() {
    val settingsViewModel = remember { SettingsViewModel() }
    val settingsUiState by settingsViewModel.uiState.collectAsState()

    val themeMode = remember(settingsUiState.settings.themeMode) {
        runCatching { AppThemeMode.valueOf(settingsUiState.settings.themeMode) }
            .getOrDefault(AppThemeMode.SYSTEM)
    }

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
                var showSplash by remember { mutableStateOf(true) }
                var currentScreen by remember { mutableStateOf(Screen.WAREHOUSE) }
                var warehouseResetKey by remember { mutableIntStateOf(0) }
                var productionResetKey by remember { mutableIntStateOf(0) }
                var managementResetKey by remember { mutableIntStateOf(0) }

                val reloadAllData = remember {
                    {
                        partViewModel.loadData()
                        bomViewModel.loadData()
                        stockViewModel.loadData()
                        companyViewModel.loadData()
                        poViewModel.loadData()
                        buildViewModel.loadData()
                    }
                }

                LaunchedEffect(currentScreen) {
                    reloadAllData()
                }

                if (showSplash) {
                    SplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else if (loginUiState.isLoggedIn) {
                    val navSuiteType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
                        currentWindowAdaptiveInfo()
                    )
                    NavigationSuiteScaffoldLayout(
                        navigationSuite = {
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(25.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 4.dp,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                NavigationSuite(
                                    layoutType = navSuiteType,
                                    colors = NavigationSuiteDefaults.colors(
                                        navigationBarContainerColor = Color.Transparent,
                                        shortNavigationBarContainerColor = Color.Transparent
                                    )
                                ) {
                                    item(
                                        selected = currentScreen == Screen.WAREHOUSE,
                                        onClick = {
                                            currentScreen = Screen.WAREHOUSE
                                            warehouseResetKey++
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = AppIcons.Warehouse,
                                                contentDescription = stringResource(Res.string.nav_warehouse),
                                                tint = if (currentScreen == Screen.WAREHOUSE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        label = { Text(stringResource(Res.string.nav_warehouse)) }
                                    )
                                    item(
                                        selected = currentScreen == Screen.PRODUCTION,
                                        onClick = {
                                            currentScreen = Screen.PRODUCTION
                                            productionResetKey++
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = AppIcons.Builds,
                                                contentDescription = stringResource(Res.string.nav_builds),
                                                tint = if (currentScreen == Screen.PRODUCTION) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        label = { Text(stringResource(Res.string.nav_builds)) }
                                    )
                                    item(
                                        selected = currentScreen == Screen.MANAGEMENT,
                                        onClick = {
                                            currentScreen = Screen.MANAGEMENT
                                            managementResetKey++
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = AppIcons.Management,
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
                                                imageVector = AppIcons.Settings,
                                                contentDescription = stringResource(Res.string.nav_settings),
                                                tint = if (currentScreen == Screen.SETTINGS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        label = { Text(stringResource(Res.string.nav_settings)) }
                                    )
                                }
                            }
                        },
                        navigationSuiteType = navSuiteType
                    ) {
                        when (currentScreen) {
                            Screen.WAREHOUSE -> {
                                key(warehouseResetKey) {
                                    WarehouseScreen(
                                        partViewModel = partViewModel,
                                        stockViewModel = stockViewModel,
                                        onBackClick = {
                                            currentScreen = Screen.WAREHOUSE
                                            warehouseResetKey++
                                        }
                                    )
                                }
                            }
                            Screen.PRODUCTION -> {
                                key(productionResetKey) {
                                    ProductionScreen(
                                        buildOrderViewModel = buildViewModel,
                                        bomViewModel = bomViewModel,
                                        onBackClick = {
                                            currentScreen = Screen.PRODUCTION
                                            productionResetKey++
                                        }
                                    )
                                }
                            }
                            Screen.MANAGEMENT -> {
                                key(managementResetKey) {
                                    ManagementScreen(
                                        companyViewModel = companyViewModel,
                                        purchaseOrderViewModel = poViewModel,
                                        onBackClick = {
                                            currentScreen = Screen.MANAGEMENT
                                            managementResetKey++
                                        }
                                    )
                                }
                            }
                            Screen.SETTINGS -> {
                                SettingsScreen(
                                    loginUiState = loginUiState,
                                    themeMode = themeMode,
                                    onThemeModeChange = { mode ->
                                        settingsViewModel.updateThemeMode(mode)
                                    },
                                    settingsViewModel = settingsViewModel,
                                    onLogoutClick = {
                                        currentScreen = Screen.WAREHOUSE
                                        loginViewModel.performLogout()
                                    },
                                    onDataChanged = reloadAllData
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
