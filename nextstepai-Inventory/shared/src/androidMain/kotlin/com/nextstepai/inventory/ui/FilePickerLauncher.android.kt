package com.nextstepai.inventory.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberPlatformPickerLaunchers(
    onImageCaptured: (String) -> Unit,
    onImagePicked: (String) -> Unit,
    onFilePicked: (String) -> Unit
): PlatformPickerLaunchers {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // 1. Camera Setup
    var currentCameraPhotoFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentCameraPhotoFile?.let { photoFile ->
                if (photoFile.exists() && photoFile.length() > 0) {
                    onImageCaptured(photoFile.absolutePath)
                }
            }
        }
    }

    // 2. Gallery Setup
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isLoading = true
                val path = withContext(Dispatchers.IO) {
                    copyUriToInternalStorage(context, uri, "images", "gallery_img", ".png")
                }
                isLoading = false
                if (path != null) {
                    onImagePicked(path)
                }
            }
        }
    }

    // 3. Document File Setup
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isLoading = true
                val path = withContext(Dispatchers.IO) {
                    copyUriToInternalStorage(context, uri, "documents", "part_doc", ".pdf")
                }
                isLoading = false
                if (path != null) {
                    onFilePicked(path)
                }
            }
        }
    }

    return PlatformPickerLaunchers(
        launchCamera = {
            try {
                val imagesDir = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }
                val photoFile = File(imagesDir, "part_photo_${System.currentTimeMillis()}.jpg")
                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                currentCameraPhotoFile = photoFile
                cameraLauncher.launch(photoUri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        launchGalleryPicker = {
            galleryLauncher.launch("image/*")
        },
        launchFilePicker = {
            fileLauncher.launch("*/*")
        },
        isLoading = isLoading
    )
}

private fun copyUriToInternalStorage(
    context: Context,
    uri: Uri,
    subDir: String,
    prefix: String,
    defaultExt: String
): String? {
    return try {
        val targetDir = File(context.filesDir, subDir).apply { if (!exists()) mkdirs() }
        val destFile = File(targetDir, "${prefix}_${System.currentTimeMillis()}$defaultExt")

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
