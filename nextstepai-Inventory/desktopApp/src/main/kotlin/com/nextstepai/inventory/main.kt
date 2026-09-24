package com.nextstepai.inventory

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val appIcon = painterResource("icon.png")

    Window(
        onCloseRequest = ::exitApplication,
        title = "دارة المخازن - NextStep AI Inventory Enterprise",
        icon = appIcon,
        state = rememberWindowState(width = 1280.dp, height = 800.dp)
    ) {
        App()
    }
}
