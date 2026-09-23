package com.nextstepai.inventory.ui

import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberPlatformPickerLaunchers(
    onImageCaptured: (String) -> Unit,
    onImagePicked: (String) -> Unit,
    onFilePicked: (String) -> Unit
): PlatformPickerLaunchers {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    return PlatformPickerLaunchers(
        launchCamera = {
            coroutineScope.launch {
                val file = openJFileChooser("اختر صورة من الكاميرا/الحاسوب", arrayOf("png", "jpg", "jpeg"))
                file?.let { onImageCaptured(it.absolutePath) }
            }
        },
        launchGalleryPicker = {
            coroutineScope.launch {
                val file = openJFileChooser("اختر صورة من الصور", arrayOf("png", "jpg", "jpeg", "webp"))
                file?.let { onImagePicked(it.absolutePath) }
            }
        },
        launchFilePicker = {
            coroutineScope.launch {
                val file = openJFileChooser("اختر ملف كراسة المواصفات أو المستند", null)
                file?.let { onFilePicked(it.absolutePath) }
            }
        },
        isLoading = isLoading
    )
}

private suspend fun openJFileChooser(title: String, extensions: Array<String>?): File? = withContext(Dispatchers.IO) {
    try {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            if (!extensions.isNullOrEmpty()) {
                fileFilter = FileNameExtensionFilter(extensions.joinToString(", "), *extensions)
            }
        }
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
