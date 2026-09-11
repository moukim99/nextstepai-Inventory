package com.nextstepai.inventory

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.nextstepai.inventory.ui.LoginScreen
import com.nextstepai.inventory.ui.LoginViewModel
import com.nextstepai.inventory.ui.MainDashboardScreen
import com.nextstepai.inventory.ui.PartManagementScreen
import com.nextstepai.inventory.ui.PartViewModel

@Composable
@Preview
fun App() {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val loginViewModel = remember { LoginViewModel() }
                val partViewModel = remember { PartViewModel() }
                val loginUiState by loginViewModel.uiState.collectAsState()
                var isPartsScreenOpen by remember { mutableStateOf(false) }

                if (loginUiState.isLoggedIn) {
                    if (isPartsScreenOpen) {
                        PartManagementScreen(
                            viewModel = partViewModel,
                            onBackClick = { isPartsScreenOpen = false }
                        )
                    } else {
                        MainDashboardScreen(
                            loginUiState = loginUiState,
                            onLogoutClick = {
                                isPartsScreenOpen = false
                                loginViewModel.performLogout()
                            },
                            onOpenPartsClick = { isPartsScreenOpen = true }
                        )
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
