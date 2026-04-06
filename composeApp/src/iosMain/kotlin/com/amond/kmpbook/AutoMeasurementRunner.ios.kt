package com.amond.kmpbook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAutoMeasurementRunner(): AutoMeasurementRunner {
    return remember {
        AutoMeasurementRunner { _, _, _, onComplete ->
            onComplete(
                AutoMeasurementResponse(
                    result = null,
                    errorMessage = "iOS 자동 인식은 아직 구현되지 않았습니다.",
                ),
            )
        }
    }
}
