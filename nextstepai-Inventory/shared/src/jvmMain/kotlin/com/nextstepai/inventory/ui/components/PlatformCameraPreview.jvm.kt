package com.nextstepai.inventory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * التنفيذ الفعلي لمعاينة الكاميرا لمنصة أجهزة السطح المكتبية (JVM Desktop).
 */
@Composable
actual fun PlatformCameraPreview(
    modifier: Modifier,
    onImageCaptured: (ByteArray) -> Unit,
    onBarcodeDetected: ((String) -> Unit)?
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "معاينة الكاميرا المباشرة (Desktop Mock Camera)",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
