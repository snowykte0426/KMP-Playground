package com.amond.kmpbook

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.roundToInt

data class LoadedImage(
    val image: ImageBitmap,
    val widthPx: Int,
    val heightPx: Int,
    val sourceLabel: String,
)

data class BodyPoint(
    val x: Float,
    val y: Float,
) {
    fun toOffset() = Offset(x, y)
}

enum class MeasurementMode(val label: String) {
    Auto("자동 인식"),
    Manual("수동 기준점 입력"),
}

enum class CapturePostureMode(
    val label: String,
    val shortLabel: String,
    val guideSummary: String,
) {
    Left45(
        label = "왼쪽 45도",
        shortLabel = "L45",
        guideSummary = "사용자 기준 왼쪽 어깨가 조금 더 카메라 쪽으로 오게 45도 정도 틀어 선 자세",
    ),
    Right45(
        label = "오른쪽 45도",
        shortLabel = "R45",
        guideSummary = "사용자 기준 오른쪽 어깨가 조금 더 카메라 쪽으로 오게 45도 정도 틀어 선 자세",
    ),
    Seated(
        label = "앉은 자세",
        shortLabel = "SEAT",
        guideSummary = "의자 끝에 허리를 세우고 앉아 상체와 둔부 윤곽이 보이게 촬영하는 자세",
    ),
}

enum class MeasurementPointType(val label: String) {
    ReferenceStart("기준 시작점"),
    ReferenceEnd("기준 끝점"),
    ChestLeft("흉부 왼쪽"),
    ChestRight("흉부 오른쪽"),
    HipLeft("둔부 왼쪽"),
    HipRight("둔부 오른쪽");

    fun next(): MeasurementPointType = entries[(ordinal + 1) % entries.size]
}

enum class ReferencePreset(val label: String, val lengthCm: Float) {
    Card("신용카드 8.56cm", 8.56f),
    A4ShortEdge("A4 짧은 변 21.0cm", 21.0f),
    Custom("직접 입력", 0f),
}

data class AutoDetectionResult(
    val referencePreset: ReferencePreset,
    val points: Map<MeasurementPointType, BodyPoint>,
    val confidenceScore: Float,
    val debugSummary: String,
) {
    val confidenceLabel: String
        get() = confidenceScore.toConfidenceLabel()
}

data class MeasurementResult(
    val chestCircumferenceCm: Int,
    val hipCircumferenceCm: Int,
    val bandSize: String,
    val koreanCupSize: String,
    val topSize: String,
    val bottomSize: String,
    val confidenceLabel: String,
    val summary: String,
)

fun Float.toConfidenceLabel(): String {
    return when {
        this >= 0.78f -> "높음"
        this >= 0.56f -> "보통"
        else -> "낮음"
    }
}

fun Float.roundToNearestFive(): Int {
    return (this / 5f).roundToInt() * 5
}

fun MeasurementPointType.description(postureMode: CapturePostureMode): String {
    return when (this) {
        MeasurementPointType.ReferenceStart ->
            "카드나 A4의 한쪽 끝점입니다. 길이를 알고 있는 기준 물체의 가장 바깥 모서리 한 점을 찍으세요."

        MeasurementPointType.ReferenceEnd ->
            "기준 물체의 반대쪽 끝점입니다. 시작점과 동일한 변 위에 놓여 실제 길이를 재는 직선이 되게 찍으세요."

        MeasurementPointType.ChestLeft ->
            when (postureMode) {
                CapturePostureMode.Left45,
                CapturePostureMode.Right45 ->
                    "겨드랑이 바로 아래 흉부 단면이 시작되는 바깥 윤곽점입니다. 옷 주름이 아닌 몸통 외곽을 고르세요."

                CapturePostureMode.Seated ->
                    "앉은 자세에서 겨드랑이 아래 흉부 단면의 왼쪽 외곽점입니다. 팔이 몸에 붙어 가리지 않게 한 뒤 몸통 윤곽을 찍으세요."
            }

        MeasurementPointType.ChestRight ->
            when (postureMode) {
                CapturePostureMode.Left45,
                CapturePostureMode.Right45 ->
                    "흉부 단면의 반대편 바깥 윤곽점입니다. 좌우 점을 잇는 선이 가슴둘레를 대표하는 높이가 되도록 맞추세요."

                CapturePostureMode.Seated ->
                    "앉은 자세 흉부 단면의 오른쪽 외곽점입니다. 좌우 점 높이가 비슷해야 계산 오차가 줄어듭니다."
            }

        MeasurementPointType.HipLeft ->
            when (postureMode) {
                CapturePostureMode.Left45,
                CapturePostureMode.Right45 ->
                    "둔부가 가장 돌출되는 높이에서 왼쪽 외곽점입니다. 허리선이 아니라 엉덩이 최대 둘레 높이를 기준으로 잡으세요."

                CapturePostureMode.Seated ->
                    "앉은 자세에서 엉덩이와 허벅지가 만나는 가장 넓은 윤곽의 왼쪽점입니다. 의자 모서리가 아닌 신체 윤곽을 찍으세요."
            }

        MeasurementPointType.HipRight ->
            when (postureMode) {
                CapturePostureMode.Left45,
                CapturePostureMode.Right45 ->
                    "둔부 최대 둘레 높이의 반대편 외곽점입니다. 좌우 점을 잇는 선이 엉덩이 최대 폭을 지나가게 맞추세요."

                CapturePostureMode.Seated ->
                    "앉은 자세 둔부 단면의 오른쪽 외곽점입니다. 체중으로 눌린 부분보다 자연 윤곽의 바깥선을 기준으로 잡으세요."
            }
    }
}
