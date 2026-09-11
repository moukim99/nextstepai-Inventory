package com.nextstepai.inventory

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.nextstepai.inventory.ui.LoginScreen
import com.nextstepai.inventory.ui.LoginViewModel
import com.nextstepai.inventory.ui.MainDashboardScreen

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
                val viewModel = remember { LoginViewModel() }
                val uiState by viewModel.uiState.collectAsState()

                if (uiState.isLoggedIn) {
                    MainDashboardScreen(
                        loginUiState = uiState,
                        onLogoutClick = { viewModel.performLogout() }
                    )
                } else {
                    LoginScreen(
                        onLoginClick = { viewModel.performSimpleLogin() },
                        isLoading = uiState.isLoading
                    )
                }
            }
        }
    }
}
