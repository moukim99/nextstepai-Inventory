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
 * مكون الكاميرا القياسي للتطبيق والمبني باستخدام مكتبة CameraK الرسمية لمنصة Jetpack Compose.
 */
@Composable
fun AppCameraKView(
    onImageCaptured: (ByteArray) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraState by rememberCameraKState(config = CameraConfiguration())
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
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
                        text = "الكاميرا جاهزة (CameraK)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
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
                                            // Handle file result if needed
                                            onImageCaptured(ByteArray(0))
                                        }
                                        is ImageCaptureResult.Error -> {
                                            // Fallback error handling
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("التقاط صورة")
                        }
                    }
                }
            }
            is CameraKState.Initializing -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is CameraKState.Error -> {
                Text(
                    text = "خطأ في الكاميرا: ${state.message}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
