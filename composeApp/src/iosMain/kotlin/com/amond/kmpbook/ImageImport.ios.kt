package com.amond.kmpbook

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImageImportActions(): ImageImportActions {
    return ImageImportActions(
        launchCamera = {},
        launchGallery = {},
        imageVersion = 0,
        consumeImage = { null },
    )
}
