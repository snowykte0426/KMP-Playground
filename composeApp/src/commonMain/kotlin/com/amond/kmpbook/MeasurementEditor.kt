package com.amond.kmpbook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun MeasurementEditor(
    modifier: Modifier = Modifier,
    image: ImageBitmap,
    manualPoints: Map<MeasurementPointType, BodyPoint>,
    autoPoints: Map<MeasurementPointType, BodyPoint>,
    activePoint: MeasurementPointType,
    mode: MeasurementMode,
    onTapPoint: (BodyPoint) -> Unit,
) {
    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(22.dp))
            .border(4.dp, Color(0xFF111111), RoundedCornerShape(22.dp))
            .aspectRatio(image.width.toFloat() / image.height.toFloat())
            .heightIn(min = 220.dp),
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(image, manualPoints, activePoint, mode) {
                    detectTapGestures { tapOffset ->
                        val transform = calculateContentTransform(
                            containerWidth = size.width.toFloat(),
                            containerHeight = size.height.toFloat(),
                            imageWidth = image.width.toFloat(),
                            imageHeight = image.height.toFloat(),
                        )
                        val imagePoint = transform.screenToImage(tapOffset)
                        if (imagePoint != null) {
                            onTapPoint(BodyPoint(imagePoint.x, imagePoint.y))
                        }
                    }
                },
        ) {
            val transform = calculateContentTransform(
                containerWidth = size.width,
                containerHeight = size.height,
                imageWidth = image.width.toFloat(),
                imageHeight = image.height.toFloat(),
            )

            drawRect(
                color = Color.Transparent,
                topLeft = Offset(transform.offsetX, transform.offsetY),
                size = Size(transform.drawWidth, transform.drawHeight),
                style = Stroke(width = 2.dp.toPx()),
            )

            val pointsToDraw = if (mode == MeasurementMode.Manual) manualPoints else autoPoints
            pointsToDraw.forEach { (type, point) ->
                val screen = transform.imageToScreen(point.toOffset())
                val pointColor = when (type) {
                    MeasurementPointType.ReferenceStart,
                    MeasurementPointType.ReferenceEnd -> Color(0xFF00B894)

                    MeasurementPointType.ChestLeft,
                    MeasurementPointType.ChestRight -> Color(0xFFFF7675)

                    MeasurementPointType.HipLeft,
                    MeasurementPointType.HipRight -> Color(0xFF0984E3)
                }
                drawCircle(
                    color = pointColor,
                    center = screen,
                    radius = 7.dp.toPx(),
                )
                drawCircle(
                    color = Color(0xFF111111),
                    center = screen,
                    radius = 9.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }

            val activeScreenPoint = manualPoints[activePoint]?.let { transform.imageToScreen(it.toOffset()) }
            if (mode == MeasurementMode.Manual && activeScreenPoint != null) {
                drawCircle(
                    color = Color.Transparent,
                    center = activeScreenPoint,
                    radius = 14.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }

            drawGuideLine(
                transform = transform,
                points = pointsToDraw,
                first = MeasurementPointType.ReferenceStart,
                second = MeasurementPointType.ReferenceEnd,
                color = Color(0xFF00B894),
            )
            drawGuideLine(
                transform = transform,
                points = pointsToDraw,
                first = MeasurementPointType.ChestLeft,
                second = MeasurementPointType.ChestRight,
                color = Color(0xFFFF7675),
            )
            drawGuideLine(
                transform = transform,
                points = pointsToDraw,
                first = MeasurementPointType.HipLeft,
                second = MeasurementPointType.HipRight,
                color = Color(0xFF0984E3),
            )

            if (mode == MeasurementMode.Manual) {
                drawIntoCanvas {
                    val label = activePoint.label
                    val labelOffset = Offset(transform.offsetX + 12.dp.toPx(), transform.offsetY + 22.dp.toPx())
                    drawRect(
                        color = Color(0xFFFFD24A),
                        topLeft = labelOffset.copy(y = labelOffset.y - 18.dp.toPx()),
                        size = Size((label.length * 12).dp.toPx(), 28.dp.toPx()),
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGuideLine(
    transform: ContentTransform,
    points: Map<MeasurementPointType, BodyPoint>,
    first: MeasurementPointType,
    second: MeasurementPointType,
    color: Color,
) {
    val firstPoint = points[first] ?: return
    val secondPoint = points[second] ?: return
    drawLine(
        color = color,
        start = transform.imageToScreen(firstPoint.toOffset()),
        end = transform.imageToScreen(secondPoint.toOffset()),
        strokeWidth = 3.dp.toPx(),
    )
}

private data class ContentTransform(
    val offsetX: Float,
    val offsetY: Float,
    val drawWidth: Float,
    val drawHeight: Float,
    val imageWidth: Float,
    val imageHeight: Float,
) {
    fun screenToImage(screen: Offset): Offset? {
        if (screen.x !in offsetX..(offsetX + drawWidth) || screen.y !in offsetY..(offsetY + drawHeight)) {
            return null
        }
        return Offset(
            x = ((screen.x - offsetX) / drawWidth) * imageWidth,
            y = ((screen.y - offsetY) / drawHeight) * imageHeight,
        )
    }

    fun imageToScreen(image: Offset): Offset {
        return Offset(
            x = offsetX + (image.x / imageWidth) * drawWidth,
            y = offsetY + (image.y / imageHeight) * drawHeight,
        )
    }
}

private fun calculateContentTransform(
    containerWidth: Float,
    containerHeight: Float,
    imageWidth: Float,
    imageHeight: Float,
): ContentTransform {
    val imageRatio = imageWidth / imageHeight
    val containerRatio = containerWidth / containerHeight
    return if (imageRatio > containerRatio) {
        val drawHeight = containerWidth / imageRatio
        ContentTransform(
            offsetX = 0f,
            offsetY = (containerHeight - drawHeight) / 2f,
            drawWidth = containerWidth,
            drawHeight = drawHeight,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
        )
    } else {
        val drawHeight = containerHeight
        val drawWidth = drawHeight * imageRatio
        ContentTransform(
            offsetX = (containerWidth - drawWidth) / 2f,
            offsetY = 0f,
            drawWidth = drawWidth,
            drawHeight = drawHeight,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
        )
    }
}
