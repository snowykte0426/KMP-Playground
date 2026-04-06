package com.amond.kmpbook

import androidx.compose.runtime.Composable

data class AutoMeasurementResponse(
    val result: AutoDetectionResult?,
    val errorMessage: String?,
)

class AutoMeasurementRunner(
    private val runInternal: (
        LoadedImage,
        ReferencePreset,
        CapturePostureMode,
        (AutoMeasurementResponse) -> Unit,
    ) -> Unit,
) {
    fun run(
        image: LoadedImage,
        referencePreset: ReferencePreset,
        postureMode: CapturePostureMode,
        onComplete: (AutoMeasurementResponse) -> Unit,
    ) {
        runInternal(image, referencePreset, postureMode, onComplete)
    }
}

@Composable
expect fun rememberAutoMeasurementRunner(): AutoMeasurementRunner
