package com.amond.kmpbook

import androidx.compose.runtime.Composable

class ImageImportActions(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit,
    val imageVersion: Int,
    private val consumeImage: () -> LoadedImage?,
) {
    fun consumeLoadedImage(): LoadedImage? = consumeImage()
}

@Composable
expect fun rememberImageImportActions(): ImageImportActions
