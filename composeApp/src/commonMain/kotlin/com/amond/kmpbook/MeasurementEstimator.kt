package com.amond.kmpbook

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

fun estimateMeasurementResult(
    referenceCm: Float,
    points: Map<MeasurementPointType, BodyPoint>,
    imageWidthPx: Float,
    imageHeightPx: Float,
    postureMode: CapturePostureMode,
): MeasurementResult? {
    val referenceStart = points[MeasurementPointType.ReferenceStart] ?: return null
    val referenceEnd = points[MeasurementPointType.ReferenceEnd] ?: return null
    val chestLeft = points[MeasurementPointType.ChestLeft] ?: return null
    val chestRight = points[MeasurementPointType.ChestRight] ?: return null
    val hipLeft = points[MeasurementPointType.HipLeft] ?: return null
    val hipRight = points[MeasurementPointType.HipRight] ?: return null

    val referencePx = distance(referenceStart, referenceEnd)
    if (referencePx <= 1f || referenceCm <= 0f) return null

    val cmPerPx = referenceCm / referencePx
    val chestProjectedWidthCm = distance(chestLeft, chestRight) * cmPerPx
    val hipProjectedWidthCm = distance(hipLeft, hipRight) * cmPerPx

    val coefficients = postureMode.coefficients()
    val chestFrontalWidthCm = recoverFrontalWidth(
        projectedWidthCm = chestProjectedWidthCm,
        angleDegrees = coefficients.viewAngleDegrees,
        depthToWidthRatio = coefficients.chestDepthRatio,
    )
    val hipFrontalWidthCm = recoverFrontalWidth(
        projectedWidthCm = hipProjectedWidthCm,
        angleDegrees = coefficients.viewAngleDegrees,
        depthToWidthRatio = coefficients.hipDepthRatio,
    )

    val chestCircumferenceCm = ellipseCircumference(
        widthCm = chestFrontalWidthCm,
        depthCm = chestFrontalWidthCm * coefficients.chestDepthRatio,
    ).roundToInt()
    val hipCircumferenceCm = (
        ellipseCircumference(
            widthCm = hipFrontalWidthCm,
            depthCm = hipFrontalWidthCm * coefficients.hipDepthRatio,
        ) * coefficients.hipCompressionFactor
        ).roundToInt()

    val bandEstimate = max(60, (chestCircumferenceCm * 0.81f).roundToNearestFive())
    val cupDiff = chestCircumferenceCm - bandEstimate

    val cupSize = when {
        cupDiff < 7 -> "AA"
        cupDiff < 10 -> "A"
        cupDiff < 12.5f -> "B"
        cupDiff < 15 -> "C"
        cupDiff < 17.5f -> "D"
        cupDiff < 20 -> "E"
        else -> "F+"
    }

    val topSize = when {
        chestCircumferenceCm < 80 -> "44"
        chestCircumferenceCm < 85 -> "55"
        chestCircumferenceCm < 90 -> "66"
        chestCircumferenceCm < 95 -> "77"
        chestCircumferenceCm < 100 -> "88"
        else -> "99"
    }

    val bottomSize = when {
        hipCircumferenceCm < 86 -> "24"
        hipCircumferenceCm < 90 -> "25"
        hipCircumferenceCm < 94 -> "26"
        hipCircumferenceCm < 98 -> "27"
        hipCircumferenceCm < 102 -> "28"
        hipCircumferenceCm < 106 -> "29"
        else -> "30+"
    }

    val chestYSpread = abs(chestLeft.y - chestRight.y) / max(1f, imageHeightPx)
    val hipYSpread = abs(hipLeft.y - hipRight.y) / max(1f, imageHeightPx)
    val horizontalAlignmentPenalty = min(0.28f, chestYSpread + hipYSpread)

    val chestCoverage = abs(chestRight.x - chestLeft.x) / max(1f, imageWidthPx)
    val hipCoverage = abs(hipRight.x - hipLeft.x) / max(1f, imageWidthPx)
    val widthConfidence = ((chestCoverage + hipCoverage) / 2f).coerceIn(0.15f, 0.95f)
    val confidence = (0.92f - horizontalAlignmentPenalty + (widthConfidence - 0.5f) * 0.35f)
        .coerceIn(0.2f, 0.94f)

    return MeasurementResult(
        chestCircumferenceCm = chestCircumferenceCm,
        hipCircumferenceCm = hipCircumferenceCm,
        bandSize = "${bandEstimate}",
        koreanCupSize = "${bandEstimate}${cupSize}",
        topSize = topSize,
        bottomSize = bottomSize,
        confidenceLabel = confidence.toConfidenceLabel(),
        summary = "${postureMode.label} 사진에서 기준 물체 비율과 단면 투영폭을 이용해 흉부 ${chestCircumferenceCm}cm, 둔부 ${hipCircumferenceCm}cm로 계산했습니다. 대략 ${bandEstimate}${cupSize}, 상의 ${topSize}, 하의 ${bottomSize} 기준입니다.",
    )
}

private fun distance(a: BodyPoint, b: BodyPoint): Float {
    return hypot(a.x - b.x, a.y - b.y)
}

private data class PostureCoefficients(
    val viewAngleDegrees: Float,
    val chestDepthRatio: Float,
    val hipDepthRatio: Float,
    val hipCompressionFactor: Float,
)

private fun CapturePostureMode.coefficients(): PostureCoefficients {
    return when (this) {
        CapturePostureMode.Left45,
        CapturePostureMode.Right45 -> PostureCoefficients(
            viewAngleDegrees = 45f,
            chestDepthRatio = 0.72f,
            hipDepthRatio = 0.86f,
            hipCompressionFactor = 1f,
        )

        CapturePostureMode.Seated -> PostureCoefficients(
            viewAngleDegrees = 45f,
            chestDepthRatio = 0.72f,
            hipDepthRatio = 0.92f,
            hipCompressionFactor = 1.04f,
        )
    }
}

private fun recoverFrontalWidth(
    projectedWidthCm: Float,
    angleDegrees: Float,
    depthToWidthRatio: Float,
): Float {
    val theta = angleDegrees / 180f * PI.toFloat()
    val projectionFactor = cos(theta) + depthToWidthRatio * sin(theta)
    return projectedWidthCm / projectionFactor.coerceAtLeast(0.35f)
}

private fun ellipseCircumference(
    widthCm: Float,
    depthCm: Float,
): Float {
    val a = widthCm / 2f
    val b = depthCm / 2f
    val term = (3 * a + b) * (a + 3 * b)
    return (PI.toFloat() * (3 * (a + b) - sqrt(term))).coerceAtLeast(0f)
}
