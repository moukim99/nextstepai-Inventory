package com.nextstepai.inventory.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * مكون معاينة الكاميرا المباشرة المتوافق عبر المنصات (PlatformCameraPreview).
 */
@Composable
expect fun PlatformCameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptured: (ByteArray) -> Unit = {},
    onBarcodeDetected: ((String) -> Unit)? = null
)
