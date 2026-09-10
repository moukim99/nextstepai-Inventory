package com.nextstepai.inventory

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "nextstepai-Inventory",
    ) {
        App()
    }
}