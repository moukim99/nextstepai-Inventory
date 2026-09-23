package com.nextstepai.inventory.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kashif.cameraK.compose.rememberCameraKState
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.state.CameraConfiguration
import com.kashif.cameraK.state.CameraKState
import kotlinx.coroutines.launch

/**
 * مكون الكاميرا القياسي للتطبيق والمبني باستخدام مكتبة CameraK و CameraX الرسمية لمنصة Jetpack Compose.
 * يتيح عرض بث فيديو الكاميرا الحي والتقاط الصور المباشرة وتحليل إطارات الرمز الشريطي (Barcode / QR Code) لحظياً.
 */
@Composable
fun AppCameraKView(
    onImageCaptured: (ByteArray) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onBarcodeDetected: ((String) -> Unit)? = null
) {
    val cameraState by rememberCameraKState(config = CameraConfiguration())
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        // 1. عرض بث الكاميرا الحي التفاعلي في الخلفية
        PlatformCameraPreview(
            modifier = Modifier.fillMaxSize(),
            onImageCaptured = onImageCaptured,
            onBarcodeDetected = onBarcodeDetected
        )

        // 2. تراكب أزرار التحكم والتعليمات فوق بث الكاميرا
        when (val state = cameraState) {
            is CameraKState.Ready -> {
                val controller = state.controller

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "قارئ الباركود والكاميرا المباشرة (CameraK & CameraX)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("إلغاء")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    when (val result = controller.takePictureToFile()) {
                                        is ImageCaptureResult.Success -> {
                                            onImageCaptured(result.byteArray)
                                        }
                                        is ImageCaptureResult.SuccessWithFile -> {
                                            onImageCaptured(ByteArray(0))
                                        }
                                        is ImageCaptureResult.Error -> {
                                            // Fallback handling
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("التقاط صنف")
                        }
                    }
                }
            }
            is CameraKState.Initializing -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is CameraKState.Error -> {
                Text(
                    text = "خطأ في تشغيل الكاميرا: ${state.message}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
