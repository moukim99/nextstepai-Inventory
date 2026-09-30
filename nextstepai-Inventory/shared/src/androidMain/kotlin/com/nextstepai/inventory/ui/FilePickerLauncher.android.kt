package com.nextstepai.inventory.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
                    coroutineScope.launch {
                        isLoading = true
                        val path = withContext(Dispatchers.IO) {
                            compressImageFileInPlace(photoFile)
                        }
                        isLoading = false
                        if (path != null) {
                            onImageCaptured(path)
                        } else {
                            onImageCaptured(photoFile.absolutePath)
                        }
                    }
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
                    copyUriToInternalStorage(context, uri, "images", "gallery_img", ".jpg")
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
        val ext = if (subDir == "images") ".jpg" else defaultExt
        val destFile = File(targetDir, "${prefix}_${System.currentTimeMillis()}$ext")

        if (subDir == "images") {
            // 1. Decode bounds efficiently without loading full bitmap into heap memory
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            // 2. Calculate optimal inSampleSize using largest dimension
            val maxDimension = 1280
            val largestDimension = maxOf(options.outHeight, options.outWidth)
            var inSampleSize = 1
            while ((largestDimension / inSampleSize) > maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            // 3. Decode scaled bitmap and compress
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                if (bitmap != null) {
                    FileOutputStream(destFile).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    }
                    bitmap.recycle()
                } else {
                    // Fallback to direct stream copy if decode fails
                    context.contentResolver.openInputStream(uri)?.use { src ->
                        FileOutputStream(destFile).use { out -> src.copyTo(out) }
                    }
                }
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun compressImageFileInPlace(file: File): String? {
    return try {
        // 1. Decode bounds efficiently from file
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)

        // 2. Calculate optimal inSampleSize using largest dimension
        val maxDimension = 1280
        val largestDimension = maxOf(options.outHeight, options.outWidth)
        var inSampleSize = 1
        while ((largestDimension / inSampleSize) > maxDimension) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }

        // 3. Decode scaled bitmap and compress over the file
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        }
        bitmap.recycle()
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
