package com.nextstepai.inventory.ui

import androidx.compose.runtime.Composable

@Composable
actual fun CommonBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    // No-op for Desktop/JVM platform
}
