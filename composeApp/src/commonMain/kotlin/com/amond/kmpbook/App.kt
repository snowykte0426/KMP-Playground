package com.amond.kmpbook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

internal val BrutalBackground = Color(0xFFF7F3E8)
internal val BrutalInk = Color(0xFF111111)
internal val BrutalYellow = Color(0xFFFFD24A)
internal val BrutalMint = Color(0xFFB9F3D0)
internal val BrutalPink = Color(0xFFFFB3C7)
internal val BrutalBlue = Color(0xFFAED8FF)
internal val BrutalDanger = Color(0xFFFF8E7A)
internal val BrutalWhite = Color(0xFFFFFCF4)

@Composable
fun App() {
    MaterialTheme {
        val imageImportActions = rememberImageImportActions()
        val historyStore = rememberMeasurementHistoryStore()
        val autoMeasurementRunner = rememberAutoMeasurementRunner()
        var loadedImage by remember { mutableStateOf<LoadedImage?>(null) }
        var referencePreset by rememberSaveable { mutableStateOf(ReferencePreset.Card) }
        var manualReferenceCmText by rememberSaveable { mutableStateOf("8.56") }
        var measurementMode by rememberSaveable { mutableStateOf(MeasurementMode.Auto) }
        var postureMode by rememberSaveable { mutableStateOf(CapturePostureMode.Left45) }
        var showCameraCapture by remember { mutableStateOf(false) }
        var showSplash by remember { mutableStateOf(true) }
        var isAutoProcessing by remember { mutableStateOf(false) }
        var autoState by remember { mutableStateOf<AutoDetectionResult?>(null) }
        var autoFailureMessage by remember { mutableStateOf<String?>(null) }
        var measurementResult by remember { mutableStateOf<MeasurementResult?>(null) }
        var workflowMessage by remember { mutableStateOf<String?>(null) }
        var historyEntries by remember { mutableStateOf(historyStore.load()) }
        val manualPoints = remember { mutableStateMapOf<MeasurementPointType, BodyPoint>() }
        var activePoint by rememberSaveable { mutableStateOf(MeasurementPointType.ReferenceStart) }

        LaunchedEffect(imageImportActions.imageVersion) {
            imageImportActions.consumeLoadedImage()?.let { newImage ->
                loadedImage = newImage
                autoState = null
                autoFailureMessage = null
                measurementResult = null
                workflowMessage = null
                manualPoints.clear()
                activePoint = MeasurementPointType.ReferenceStart
                measurementMode = MeasurementMode.Auto
            }
        }

        LaunchedEffect(Unit) {
            delay(1200)
            showSplash = false
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BrutalBackground,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .safeDrawingPadding()
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TopBrandBar()
                    HeroCard()
                    SourceCard(
                        sourceSummary = loadedImage?.sourceLabel ?: "아직 이미지 없음",
                        onCamera = { showCameraCapture = true },
                        onGallery = imageImportActions.launchGallery,
                        hasImage = loadedImage != null,
                    )
                    if (showCameraCapture) {
                        PlatformCameraCapture(
                            modifier = Modifier.fillMaxWidth(),
                            postureMode = postureMode,
                            onImageCaptured = { newImage ->
                                loadedImage = newImage
                                showCameraCapture = false
                                autoState = null
                                autoFailureMessage = null
                                measurementResult = null
                                workflowMessage = "${postureMode.label} 가이드로 촬영한 이미지를 불러왔습니다."
                                manualPoints.clear()
                                activePoint = MeasurementPointType.ReferenceStart
                                measurementMode = MeasurementMode.Auto
                            },
                            onClose = { showCameraCapture = false },
                        )
                    }
                    MeasurementModeCard(
                        measurementMode = measurementMode,
                        onModeSelected = {
                            measurementMode = it
                            measurementResult = null
                        },
                        postureMode = postureMode,
                        onPostureModeChange = { postureMode = it },
                        referencePreset = referencePreset,
                        onReferencePresetChange = { referencePreset = it },
                        manualReferenceCmText = manualReferenceCmText,
                        onManualReferenceCmChange = { manualReferenceCmText = it },
                    )
                    workflowMessage?.let {
                        StatusStrip(text = it, background = BrutalYellow)
                    }

                    val image = loadedImage
                    if (image == null) {
                        EmptyStateCard(postureMode = postureMode)
                    } else {
                        MeasurementCanvasCard(
                            loadedImage = image,
                            mode = measurementMode,
                            postureMode = postureMode,
                            autoDetectionResult = autoState,
                            manualPoints = manualPoints,
                            activePoint = activePoint,
                            onPointSelected = { activePoint = it },
                            onPointPlaced = { type, point ->
                                manualPoints[type] = point
                                activePoint = type.next()
                                measurementResult = null
                                workflowMessage = "${type.label}을(를) 배치했습니다."
                            },
                        )

                        when (measurementMode) {
                            MeasurementMode.Auto -> {
                                AutoControlCard(
                                    referencePreset = referencePreset,
                                    postureMode = postureMode,
                                    autoFailureMessage = autoFailureMessage,
                                    autoDetectionResult = autoState,
                                    isProcessing = isAutoProcessing,
                                    onAutoAttempt = {
                                        measurementResult = null
                                        workflowMessage = null
                                        isAutoProcessing = true
                                        autoMeasurementRunner.run(image, referencePreset, postureMode) { response ->
                                            isAutoProcessing = false
                                            val result = response.result
                                            if (result == null) {
                                                autoState = null
                                                autoFailureMessage =
                                                    response.errorMessage
                                                        ?: "자동 인식이 실패했습니다. 자세와 기준 물체가 잘 보이도록 다시 시도하거나 수동 기준점 입력으로 전환하세요."
                                            } else {
                                                autoState = result
                                                autoFailureMessage = null
                                                workflowMessage = "ML 기반 자동 인식 추천점을 만들었습니다. 확인 후 결과 계산을 누르세요."
                                            }
                                        }
                                    },
                                    onSwitchToManual = {
                                        measurementMode = MeasurementMode.Manual
                                        measurementResult = null
                                        workflowMessage = "수동 기준점 입력으로 전환했습니다."
                                    },
                                    onEstimate = {
                                        val auto = autoState ?: return@AutoControlCard
                                        val result = estimateMeasurementResult(
                                            referenceCm = auto.referencePreset.lengthCm,
                                            points = auto.points,
                                            imageWidthPx = image.widthPx.toFloat(),
                                            imageHeightPx = image.heightPx.toFloat(),
                                            postureMode = postureMode,
                                        )
                                        measurementResult = result
                                        workflowMessage = if (result == null) {
                                            "자동 측정 계산에 실패했습니다. 수동 기준점 입력으로 다시 시도하세요."
                                        } else {
                                            "자동 측정 결과를 계산했습니다."
                                        }
                                    },
                                )
                            }

                            MeasurementMode.Manual -> {
                                ManualControlCard(
                                    postureMode = postureMode,
                                    activePoint = activePoint,
                                    manualPoints = manualPoints,
                                    onPointSelected = { activePoint = it },
                                    onReset = {
                                        manualPoints.clear()
                                        measurementResult = null
                                        workflowMessage = "수동 기준점을 초기화했습니다."
                                        activePoint = MeasurementPointType.ReferenceStart
                                    },
                                    onEstimate = {
                                        val referenceCm = resolveReferenceLengthCm(
                                            preset = referencePreset,
                                            manualValue = manualReferenceCmText,
                                        )
                                        if (referenceCm == null) {
                                            workflowMessage = "기준 길이를 확인하세요. 직접 입력 값은 0보다 큰 숫자여야 합니다."
                                            return@ManualControlCard
                                        }

                                        val result = estimateMeasurementResult(
                                            referenceCm = referenceCm,
                                            points = manualPoints.toMap(),
                                            imageWidthPx = image.widthPx.toFloat(),
                                            imageHeightPx = image.heightPx.toFloat(),
                                            postureMode = postureMode,
                                        )
                                        measurementResult = result
                                        workflowMessage = if (result == null) {
                                            "모든 기준점이 필요합니다. 빠진 점이 없는지 확인하세요."
                                        } else {
                                            "수동 측정 결과를 계산했습니다."
                                        }
                                    },
                                )
                            }
                        }
                    }

                    measurementResult?.let {
                        ResultCard(
                            result = it,
                            onSave = {
                                historyStore.save(
                                    MeasurementHistoryEntry(
                                        timestampMillis = currentEpochMillis(),
                                        chestCircumferenceCm = it.chestCircumferenceCm,
                                        hipCircumferenceCm = it.hipCircumferenceCm,
                                        koreanCupSize = it.koreanCupSize,
                                        topSize = it.topSize,
                                        bottomSize = it.bottomSize,
                                        confidenceLabel = it.confidenceLabel,
                                    ),
                                )
                                historyEntries = historyStore.load()
                                workflowMessage = "현재 결과를 로컬 히스토리에 저장했습니다."
                            },
                        )
                    }

                    HistoryCard(
                        entries = historyEntries,
                        onClear = {
                            historyStore.clear()
                            historyEntries = emptyList()
                            workflowMessage = "로컬 히스토리를 비웠습니다."
                        },
                    )
                }

                if (showSplash) {
                    SplashOverlay()
                }
            }
        }
    }
}

@Composable
private fun TopBrandBar() {
    BrutalCard(
        background = BrutalWhite,
        accent = BrutalBlue,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LogoMark()
                Column {
                    Text(
                        text = "BODY FIT",
                        fontWeight = FontWeight.Black,
                        color = BrutalInk,
                    )
                    Text(
                        text = "measurement studio",
                        color = BrutalInk,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .background(BrutalYellow, RoundedCornerShape(12.dp))
                    .border(3.dp, BrutalInk, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "MVP",
                    color = BrutalInk,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun HeroCard() {
    BrutalCard(
        background = BrutalYellow,
        accent = BrutalPink,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LogoMark()
            Text(
                text = "BODY FIT MVP",
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = BrutalInk,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "45도 선 자세와 앉은 자세를 기준으로 흉부·둔부 둘레, 한국 컵, 의류 사이즈를 추정하는 Android 우선 측정 MVP입니다.",
            color = BrutalInk,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "기준 물체와 단면 투영폭을 이용한 계산 모델을 사용합니다. 결과는 로컬 기기 안에서만 처리됩니다.",
            color = BrutalInk,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LogoMark() {
    Box(
        modifier = Modifier
            .background(BrutalWhite, RoundedCornerShape(18.dp))
            .border(4.dp, BrutalInk, RoundedCornerShape(18.dp))
            .padding(8.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .background(BrutalPink, RoundedCornerShape(10.dp))
                    .border(3.dp, BrutalInk, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text("BF", color = BrutalInk, fontWeight = FontWeight.Black)
            }
            Box(
                modifier = Modifier
                    .background(BrutalYellow, RoundedCornerShape(8.dp))
                    .border(3.dp, BrutalInk, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("TAPE", color = BrutalInk, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SplashOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrutalBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        BrutalCard(
            background = BrutalYellow,
            accent = BrutalPink,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LogoMark()
                Column {
                    Text(
                        text = "BODY FIT",
                        color = BrutalInk,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "precise local measurement",
                        color = BrutalInk,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceCard(
    sourceSummary: String,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    hasImage: Boolean,
) {
    BrutalCard(background = BrutalWhite, accent = BrutalBlue) {
        Text(
            text = "1. 이미지 입력",
            fontWeight = FontWeight.ExtraBold,
            color = BrutalInk,
        )
        Spacer(Modifier.height(10.dp))
        Text(text = sourceSummary, color = BrutalInk)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BrutalButton(
                text = if (hasImage) "다시 촬영" else "카메라 촬영",
                background = BrutalMint,
                onClick = onCamera,
            )
            BrutalButton(
                text = if (hasImage) "다시 불러오기" else "이미지 불러오기",
                background = BrutalPink,
                onClick = onGallery,
            )
        }
    }
}

@Composable
private fun MeasurementModeCard(
    measurementMode: MeasurementMode,
    onModeSelected: (MeasurementMode) -> Unit,
    postureMode: CapturePostureMode,
    onPostureModeChange: (CapturePostureMode) -> Unit,
    referencePreset: ReferencePreset,
    onReferencePresetChange: (ReferencePreset) -> Unit,
    manualReferenceCmText: String,
    onManualReferenceCmChange: (String) -> Unit,
) {
    BrutalCard(background = BrutalWhite, accent = BrutalYellow) {
        Text(
            text = "2. 기준 방식",
            fontWeight = FontWeight.ExtraBold,
            color = BrutalInk,
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MeasurementMode.entries.forEach { mode ->
                SelectableTag(
                    text = mode.label,
                    selected = measurementMode == mode,
                    onClick = { onModeSelected(mode) },
                    selectedColor = BrutalMint,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "촬영 자세",
            fontWeight = FontWeight.Bold,
            color = BrutalInk,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CapturePostureMode.entries.forEach { posture ->
                SelectableTag(
                    text = posture.label,
                    selected = postureMode == posture,
                    onClick = { onPostureModeChange(posture) },
                    selectedColor = BrutalPink,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = postureMode.guideSummary,
            color = BrutalInk,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "기준 물체",
            fontWeight = FontWeight.Bold,
            color = BrutalInk,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReferencePreset.entries.forEach { preset ->
                SelectableTag(
                    text = preset.label,
                    selected = referencePreset == preset,
                    onClick = { onReferencePresetChange(preset) },
                    selectedColor = BrutalBlue,
                )
            }
        }
        if (referencePreset == ReferencePreset.Custom) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = manualReferenceCmText,
                onValueChange = onManualReferenceCmChange,
                label = { Text("실제 길이(cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun EmptyStateCard(postureMode: CapturePostureMode) {
    BrutalCard(background = BrutalWhite, accent = BrutalDanger) {
        Text(
            text = "이미지가 필요합니다",
            fontWeight = FontWeight.ExtraBold,
            color = BrutalInk,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${postureMode.label} 자세에서 몸의 윤곽과 기준 물체가 함께 보이는 사진이 필요합니다. 기준 물체는 카드 또는 A4를 권장합니다.",
            color = BrutalInk,
        )
    }
}

@Composable
private fun MeasurementCanvasCard(
    loadedImage: LoadedImage,
    mode: MeasurementMode,
    postureMode: CapturePostureMode,
    autoDetectionResult: AutoDetectionResult?,
    manualPoints: Map<MeasurementPointType, BodyPoint>,
    activePoint: MeasurementPointType,
    onPointSelected: (MeasurementPointType) -> Unit,
    onPointPlaced: (MeasurementPointType, BodyPoint) -> Unit,
) {
    BrutalCard(background = BrutalWhite, accent = BrutalMint, contentPadding = PaddingValues(12.dp)) {
        Text(
            text = "3. 측정 화면",
            fontWeight = FontWeight.ExtraBold,
            color = BrutalInk,
        )
        Spacer(Modifier.height(10.dp))
        MeasurementEditor(
            modifier = Modifier.fillMaxWidth(),
            image = loadedImage.image,
            manualPoints = manualPoints,
            autoPoints = autoDetectionResult?.points.orEmpty(),
            activePoint = activePoint,
            mode = mode,
            onTapPoint = { bodyPoint ->
                if (mode == MeasurementMode.Manual) {
                    onPointPlaced(activePoint, bodyPoint)
                }
            },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "현재 자세 모드: ${postureMode.label}",
            color = BrutalInk,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        if (mode == MeasurementMode.Manual) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MeasurementPointType.entries.forEach { type ->
                    SelectableTag(
                        text = type.label,
                        selected = type == activePoint,
                        onClick = { onPointSelected(type) },
                        selectedColor = BrutalYellow,
                    )
                }
            }
        } else {
            Text(
                text = "자동 인식 모드에서는 ${postureMode.label} 자세를 기준으로 포즈와 기준 물체를 분석해 추천 기준점을 만듭니다.",
                color = BrutalInk,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AutoControlCard(
    referencePreset: ReferencePreset,
    postureMode: CapturePostureMode,
    autoFailureMessage: String?,
    autoDetectionResult: AutoDetectionResult?,
    isProcessing: Boolean,
    onAutoAttempt: () -> Unit,
    onSwitchToManual: () -> Unit,
    onEstimate: () -> Unit,
) {
    BrutalCard(background = BrutalWhite, accent = BrutalBlue) {
        Text(
            text = "4. 자동 인식",
            fontWeight = FontWeight.ExtraBold,
            color = BrutalInk,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (referencePreset == ReferencePreset.Custom) {
                "자동 인식은 사용자 정의 길이를 직접 찾을 수 없으므로 카드 또는 A4 기준을 선택하세요."
            } else {
                "${postureMode.label} 자세에서 ${referencePreset.label} 자동 탐지와 신체 윤곽 추정을 시도합니다."
            },
            color = BrutalInk,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BrutalButton(
                text = if (isProcessing) "분석 중..." else "자동 인식 시도",
                background = BrutalYellow,
                onClick = onAutoAttempt,
                enabled = referencePreset != ReferencePreset.Custom && !isProcessing,
            )
            BrutalButton(
                text = "수동 기준점 입력",
                background = BrutalMint,
                onClick = onSwitchToManual,
                enabled = !isProcessing,
            )
        }
        autoFailureMessage?.let {
            Spacer(Modifier.height(12.dp))
            StatusStrip(text = it, background = BrutalDanger)
        }
        autoDetectionResult?.let {
            Spacer(Modifier.height(12.dp))
            StatusStrip(
                text = "자동 인식 성공: 신뢰도 ${it.confidenceLabel} / 윤곽 ${it.debugSummary}",
                background = BrutalMint,
            )
            Spacer(Modifier.height(10.dp))
            BrutalButton(
                text = "결과 계산",
                background = BrutalPink,
                onClick = onEstimate,
            )
        }
    }
}

@Composable
private fun ManualControlCard(
    postureMode: CapturePostureMode,
    activePoint: MeasurementPointType,
    manualPoints: Map<MeasurementPointType, BodyPoint>,
    onPointSelected: (MeasurementPointType) -> Unit,
    onReset: () -> Unit,
    onEstimate: () -> Unit,
) {
    BrutalCard(background = BrutalWhite, accent = BrutalPink) {
        Text(
            text = "4. 수동 기준점 입력",
            fontWeight = FontWeight.ExtraBold,
            color = BrutalInk,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "현재 활성 점: ${activePoint.label}. 이미지를 눌러 점을 배치하세요. ${postureMode.label} 자세 기준으로 기준 물체 2점, 흉부 좌우 2점, 둔부 좌우 2점이 모두 필요합니다.",
            color = BrutalInk,
        )
        Spacer(Modifier.height(10.dp))
        StatusStrip(
            text = activePoint.description(postureMode),
            background = BrutalYellow,
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MeasurementPointType.entries.forEach { type ->
                val done = manualPoints.containsKey(type)
                SelectableTag(
                    text = if (done) "${type.label} 완료" else type.label,
                    selected = type == activePoint,
                    onClick = { onPointSelected(type) },
                    selectedColor = if (done) BrutalMint else BrutalYellow,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MeasurementPointType.entries.forEach { type ->
                Text(
                    text = "• ${type.label}: ${type.description(postureMode)}",
                    color = BrutalInk,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BrutalButton(
                text = "초기화",
                background = BrutalDanger,
                onClick = onReset,
            )
            BrutalButton(
                text = "결과 계산",
                background = BrutalBlue,
                onClick = onEstimate,
                enabled = manualPoints.size == MeasurementPointType.entries.size,
            )
        }
    }
}

@Composable
private fun ResultCard(
    result: MeasurementResult,
    onSave: () -> Unit,
) {
    BrutalCard(background = BrutalInk, accent = BrutalYellow) {
        Text(
            text = "5. 추정 결과",
            color = BrutalWhite,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(12.dp))
        ResultRow("흉부 둘레", "${result.chestCircumferenceCm} cm")
        ResultRow("둔부 둘레", "${result.hipCircumferenceCm} cm")
        ResultRow("대략 브라 밴드", "${result.bandSize}")
        ResultRow("한국 컵 추정", result.koreanCupSize)
        ResultRow("상의 사이즈", result.topSize)
        ResultRow("하의 사이즈", result.bottomSize)
        ResultRow("신뢰도", result.confidenceLabel)
        Spacer(Modifier.height(10.dp))
        Text(
            text = result.summary,
            color = BrutalWhite,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        BrutalButton(
            text = "결과 로컬 저장",
            background = BrutalYellow,
            onClick = onSave,
        )
    }
}

@Composable
private fun HistoryCard(
    entries: List<MeasurementHistoryEntry>,
    onClear: () -> Unit,
) {
    BrutalCard(background = BrutalWhite, accent = BrutalBlue) {
        Text(
            text = "6. 최근 기록",
            fontWeight = FontWeight.ExtraBold,
            color = BrutalInk,
        )
        Spacer(Modifier.height(10.dp))
        if (entries.isEmpty()) {
            Text(
                text = "저장된 로컬 기록이 없습니다.",
                color = BrutalInk,
            )
        } else {
            entries.forEachIndexed { index, entry ->
                HistoryRow(index = index, entry = entry)
                if (index != entries.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            BrutalButton(
                text = "기록 비우기",
                background = BrutalDanger,
                onClick = onClear,
            )
        }
    }
}

@Composable
private fun HistoryRow(
    index: Int,
    entry: MeasurementHistoryEntry,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrutalBackground, RoundedCornerShape(18.dp))
            .border(3.dp, BrutalInk, RoundedCornerShape(18.dp))
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "기록 ${index + 1}",
                color = BrutalInk,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "흉부 ${entry.chestCircumferenceCm}cm / 둔부 ${entry.hipCircumferenceCm}cm / 컵 ${entry.koreanCupSize}",
                color = BrutalInk,
            )
            Text(
                text = "상의 ${entry.topSize} / 하의 ${entry.bottomSize} / 신뢰도 ${entry.confidenceLabel}",
                color = BrutalInk,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = BrutalWhite, fontWeight = FontWeight.Bold)
        Text(text = value, color = BrutalYellow, fontWeight = FontWeight.Black)
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun StatusStrip(text: String, background: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(14.dp))
            .border(3.dp, BrutalInk, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(text = text, color = BrutalInk, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SelectableTag(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) selectedColor else BrutalWhite,
                shape = RoundedCornerShape(14.dp),
            )
            .border(3.dp, BrutalInk, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            color = BrutalInk,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
        )
    }
}

@Composable
internal fun BrutalButton(
    text: String,
    background: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = BrutalInk,
            disabledContainerColor = background.copy(alpha = 0.45f),
            disabledContentColor = BrutalInk.copy(alpha = 0.65f),
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = text, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun BrutalCard(
    background: Color,
    accent: Color,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent, RoundedCornerShape(26.dp))
            .padding(start = 8.dp, top = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(background, RoundedCornerShape(26.dp))
                .border(4.dp, BrutalInk, RoundedCornerShape(26.dp))
                .padding(contentPadding),
            content = content,
        )
    }
}

private fun resolveReferenceLengthCm(
    preset: ReferencePreset,
    manualValue: String,
): Float? {
    return when (preset) {
        ReferencePreset.Card -> preset.lengthCm
        ReferencePreset.A4ShortEdge -> preset.lengthCm
        ReferencePreset.Custom -> manualValue.toFloatOrNull()?.takeIf { it > 0f }
    }
}
