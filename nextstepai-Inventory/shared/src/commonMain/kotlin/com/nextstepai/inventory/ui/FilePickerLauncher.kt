package com.nextstepai.inventory.ui

import androidx.compose.runtime.Composable

data class PlatformPickerLaunchers(
    val launchCamera: () -> Unit,
    val launchGalleryPicker: () -> Unit,
    val launchFilePicker: () -> Unit,
    val isLoading: Boolean
)

@Composable
expect fun rememberPlatformPickerLaunchers(
    onImageCaptured: (String) -> Unit,
    onImagePicked: (String) -> Unit,
    onFilePicked: (String) -> Unit
): PlatformPickerLaunchers
