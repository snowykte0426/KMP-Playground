package com.amond.kmpbook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asAndroidBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

@Composable
actual fun rememberAutoMeasurementRunner(): AutoMeasurementRunner {
    val detector = remember {
        PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
                .build(),
        )
    }

    DisposableEffect(detector) {
        onDispose {
            detector.close()
        }
    }

    return remember(detector) {
        AutoMeasurementRunner { image, referencePreset, postureMode, onComplete ->
            if (referencePreset == ReferencePreset.Custom) {
                onComplete(
                    AutoMeasurementResponse(
                        result = null,
                        errorMessage = "자동 인식은 카드 또는 A4 기준 물체 선택이 필요합니다.",
                    ),
                )
                return@AutoMeasurementRunner
            }

            val bitmap = image.image.asAndroidBitmap()
            val input = InputImage.fromBitmap(bitmap, 0)
            detector.process(input)
                .addOnSuccessListener { pose ->
                    val anchors = pose.toAnchors(bitmap.width.toFloat(), bitmap.height.toFloat())
                    val mlAnchored = anchors?.let { attemptAutoDetection(image, referencePreset, it) }
                    val fallback = mlAnchored ?: attemptAutoDetection(image, referencePreset)
                    val finalResult = fallback?.let {
                        if (anchors == null) {
                            it.copy(debugSummary = "포즈 미검출 fallback / ${it.debugSummary}")
                        } else {
                            it
                        }
                    }

                    onComplete(
                        if (finalResult == null) {
                            AutoMeasurementResponse(
                                result = null,
                                errorMessage = "포즈와 기준 물체를 함께 찾지 못했습니다. ${postureMode.label} 자세와 카드/A4가 모두 보이게 다시 시도하세요.",
                            )
                        } else {
                            AutoMeasurementResponse(result = finalResult, errorMessage = null)
                        },
                    )
                }
                .addOnFailureListener {
                    onComplete(
                        AutoMeasurementResponse(
                            result = null,
                            errorMessage = "ML 포즈 분석에 실패했습니다. 다른 사진으로 다시 시도하세요.",
                        ),
                    )
                }
        }
    }
}

private fun Pose.toAnchors(
    imageWidth: Float,
    imageHeight: Float,
): AutoDetectionAnchors? {
    val leftShoulder = getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: return null
    val rightShoulder = getPoseLandmark(PoseLandmark.RIGHT_SHOULDER) ?: return null
    val leftHip = getPoseLandmark(PoseLandmark.LEFT_HIP) ?: return null
    val rightHip = getPoseLandmark(PoseLandmark.RIGHT_HIP) ?: return null

    val centerX = listOf(
        leftShoulder.position.x,
        rightShoulder.position.x,
        leftHip.position.x,
        rightHip.position.x,
    ).average().toFloat() / imageWidth

    val chestRow = ((leftShoulder.position.y + rightShoulder.position.y) / 2f) / imageHeight
    val hipRow = ((leftHip.position.y + rightHip.position.y) / 2f) / imageHeight

    return AutoDetectionAnchors(
        centerXRatio = centerX.coerceIn(0f, 1f),
        chestRowRatio = chestRow.coerceIn(0f, 1f),
        hipRowRatio = hipRow.coerceIn(0f, 1f),
    )
}
