package com.amond.kmpbook

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.CameraController
import androidx.camera.view.PreviewView
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import androidx.core.graphics.scale

@Composable
actual fun PlatformCameraCapture(
    modifier: Modifier,
    postureMode: CapturePostureMode,
    onImageCaptured: (LoadedImage) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var guideState by remember { mutableStateOf(CameraGuideState()) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    val poseDetector = remember {
        PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build(),
        )
    }

    val controller = remember(context) {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }

    DisposableEffect(hasPermission, lifecycleOwner, controller, poseDetector, previewView) {
        if (hasPermission) {
            controller.bindToLifecycle(lifecycleOwner)
            controller.setImageAnalysisAnalyzer(
                mainExecutor,
                MlKitAnalyzer(
                    listOf(poseDetector),
                    ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    mainExecutor,
                ) { result ->
                    val pose = result.getValue(poseDetector)
                    val view = previewView
                    if (view != null) {
                        guideState = buildGuideState(
                            pose = pose,
                            width = view.width.toFloat().coerceAtLeast(1f),
                            height = view.height.toFloat().coerceAtLeast(1f),
                            postureMode = postureMode,
                        )
                    }
                },
            )
        }

        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            poseDetector.close()
        }
    }

    Box(
        modifier = modifier
            .background(BrutalWhite, RoundedCornerShape(26.dp))
            .border(4.dp, BrutalInk, RoundedCornerShape(26.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "실시간 촬영 가이드",
                color = BrutalInk,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )

            if (!hasPermission) {
                Text(
                    text = "카메라 프리뷰와 ${postureMode.label} 자세 가이드를 위해 카메라 권한이 필요합니다.",
                    color = BrutalInk,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrutalButton(
                        text = "권한 요청",
                        background = BrutalYellow,
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    )
                    BrutalButton(
                        text = "닫기",
                        background = BrutalDanger,
                        onClick = onClose,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .background(Color.Black, RoundedCornerShape(22.dp))
                        .border(4.dp, BrutalInk, RoundedCornerShape(22.dp)),
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { viewContext ->
                            PreviewView(viewContext).also { view ->
                                view.scaleType = PreviewView.ScaleType.FILL_CENTER
                                view.controller = controller
                                previewView = view
                            }
                        },
                        update = { view ->
                            previewView = view
                            view.controller = controller
                        },
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .border(
                                width = 4.dp,
                                color = if (guideState.canCapture) BrutalMint else BrutalYellow,
                                shape = RoundedCornerShape(24.dp),
                            ),
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .background(
                                if (guideState.canCapture) BrutalMint else BrutalYellow,
                                RoundedCornerShape(16.dp),
                            )
                            .border(3.dp, BrutalInk, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = guideState.message,
                            color = BrutalInk,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Text(
                    text = "${postureMode.guideSummary}. 기준 물체(${ReferencePreset.Card.label} 또는 ${ReferencePreset.A4ShortEdge.label})가 함께 보이게 두면 자동 측정 성공률이 올라갑니다.",
                    color = BrutalInk,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrutalButton(
                        text = "현재 프레임 사용",
                        background = BrutalPink,
                        onClick = {
                            val bitmap = previewView?.bitmap?.copy(Bitmap.Config.ARGB_8888, false)
                            if (bitmap != null) {
                                val scaled = bitmap.scaleDownIfNeeded()
                                onImageCaptured(
                                    LoadedImage(
                                        image = scaled.asImageBitmap(),
                                        widthPx = scaled.width,
                                        heightPx = scaled.height,
                                        sourceLabel = "CameraX 프리뷰 캡처 ${scaled.width}x${scaled.height}",
                                    ),
                                )
                            }
                        },
                        enabled = guideState.canCapture,
                    )
                    BrutalButton(
                        text = "닫기",
                        background = BrutalDanger,
                        onClick = onClose,
                    )
                }
            }
        }
    }
}

private data class CameraGuideState(
    val canCapture: Boolean = false,
    val message: String = "한 사람을 프레임 중앙에 맞춰주세요.",
)

private fun buildGuideState(
    pose: Pose?,
    width: Float,
    height: Float,
    postureMode: CapturePostureMode,
): CameraGuideState {
    if (pose == null) {
        return CameraGuideState(
            canCapture = false,
            message = "사람을 찾지 못했습니다. 한 명만 프레임 안으로 들어오게 해주세요.",
        )
    }

    val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
    val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
    val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
    val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
    val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
    val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
    val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

    if (nose == null || leftShoulder == null || rightShoulder == null || leftHip == null || rightHip == null) {
        return CameraGuideState(
            canCapture = false,
            message = "상체 핵심 포인트를 찾지 못했습니다. 몸이 잘 보이도록 다시 맞춰주세요.",
        )
    }

    val centerX = listOf(
        nose.position.x,
        leftShoulder.position.x,
        rightShoulder.position.x,
        leftHip.position.x,
        rightHip.position.x,
    ).average().toFloat() / width

    if (centerX !in 0.28f..0.72f) {
        return CameraGuideState(
            canCapture = false,
            message = "몸을 화면 중앙으로 조금 더 이동해주세요.",
        )
    }

    val shoulderSpan = (rightShoulder.position.x - leftShoulder.position.x).let { kotlin.math.abs(it) } / width
    if (shoulderSpan < 0.16f) {
        return CameraGuideState(
            canCapture = false,
            message = "카메라와 조금 더 가깝게 서주세요.",
        )
    }
    if (shoulderSpan > 0.52f) {
        return CameraGuideState(
            canCapture = false,
            message = "카메라와 너무 가깝습니다. 한 걸음 뒤로 가주세요.",
        )
    }

    val leftOffset = kotlin.math.abs(nose.position.x - leftShoulder.position.x)
    val rightOffset = kotlin.math.abs(rightShoulder.position.x - nose.position.x)
    val asymmetryRatio = maxOf(leftOffset, rightOffset) / maxOf(1f, minOf(leftOffset, rightOffset))

    if (postureMode != CapturePostureMode.Seated && asymmetryRatio < 1.12f) {
        return CameraGuideState(
            canCapture = false,
            message = "${postureMode.label}가 되도록 몸통을 조금 더 틀어주세요.",
        )
    }

    if (leftAnkle == null || rightAnkle == null) {
        return CameraGuideState(
            canCapture = postureMode == CapturePostureMode.Seated,
            message = if (postureMode == CapturePostureMode.Seated) {
                "앉은 자세 상체 정렬은 안정적입니다. 둔부 윤곽과 기준 물체가 보이면 촬영할 수 있습니다."
            } else {
                "가능하면 하체까지 보이게 맞추면 더 정확합니다."
            },
        )
    }

    val bodyHeightRatio = (maxOf(leftAnkle.position.y, rightAnkle.position.y) - nose.position.y) / height
    if (postureMode != CapturePostureMode.Seated && bodyHeightRatio < 0.6f) {
        return CameraGuideState(
            canCapture = false,
            message = "몸이 너무 작게 잡혔습니다. 카메라에 더 가까이 와주세요.",
        )
    }

    return CameraGuideState(
        canCapture = true,
        message = when (postureMode) {
            CapturePostureMode.Left45,
            CapturePostureMode.Right45 -> "45도 정렬이 안정적입니다. 현재 프레임으로 촬영할 수 있습니다."
            CapturePostureMode.Seated -> "앉은 자세 정렬이 안정적입니다. 현재 프레임으로 촬영할 수 있습니다."
        },
    )
}

private fun Bitmap.scaleDownIfNeeded(maxEdge: Int = 1600): Bitmap {
    val largest = maxOf(width, height)
    if (largest <= maxEdge) return this
    val scale = maxEdge.toFloat() / largest.toFloat()
    return this.scale((width * scale).toInt(), (height * scale).toInt())
}
