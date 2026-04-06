package com.amond.kmpbook

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformCameraCapture(
    modifier: Modifier,
    postureMode: CapturePostureMode,
    onImageCaptured: (LoadedImage) -> Unit,
    onClose: () -> Unit,
) {
    Text("iOS 카메라 미구현")
}
