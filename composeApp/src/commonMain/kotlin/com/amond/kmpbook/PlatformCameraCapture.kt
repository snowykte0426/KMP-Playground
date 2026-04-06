package com.amond.kmpbook

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformCameraCapture(
    modifier: Modifier = Modifier,
    postureMode: CapturePostureMode,
    onImageCaptured: (LoadedImage) -> Unit,
    onClose: () -> Unit,
)
