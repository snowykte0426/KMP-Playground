package com.amond.kmpbook

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun attemptAutoDetection(
    loadedImage: LoadedImage,
    referencePreset: ReferencePreset,
    anchors: AutoDetectionAnchors? = null,
): AutoDetectionResult? {
    if (referencePreset == ReferencePreset.Custom) return null

    val image = loadedImage.image
    val targetWidth = min(image.width, 220)
    val scale = targetWidth.toFloat() / image.width.toFloat()
    val targetHeight = max(1, (image.height * scale).toInt())
    val pixelMap = image.toPixelMap()

    val sampled = Array(targetHeight) { BooleanArray(targetWidth) }
    val background = averageCornerColor(pixelMap)
    for (y in 0 until targetHeight) {
        for (x in 0 until targetWidth) {
            val originX = (x / scale).toInt().coerceIn(0, image.width - 1)
            val originY = (y / scale).toInt().coerceIn(0, image.height - 1)
            sampled[y][x] = colorDistance(pixelMap[originX, originY], background) > 0.18f
        }
    }

    val components = extractComponents(sampled)
    if (components.isEmpty()) return null

    val body = components.maxByOrNull { it.area } ?: return null
    if (body.area < targetWidth * targetHeight * 0.08f) return null

    val bodyHeight = body.maxY - body.minY + 1
    val anchorCenterX = anchors?.centerXRatio?.times(targetWidth)?.toInt()?.coerceIn(body.minX, body.maxX)
    val centerX = anchorCenterX ?: body.centerX.toInt()
    val chestRow = anchors?.chestRowRatio
        ?.times(targetHeight)
        ?.toInt()
        ?.coerceIn(body.minY, body.maxY)
        ?: (body.minY + bodyHeight * 0.32f).toInt().coerceIn(body.minY, body.maxY)
    val hipRow = anchors?.hipRowRatio
        ?.times(targetHeight)
        ?.toInt()
        ?.coerceIn(body.minY, body.maxY)
        ?: (body.minY + bodyHeight * 0.72f).toInt().coerceIn(body.minY, body.maxY)
    val chestSpan = findSpanAroundCenter(sampled, chestRow, centerX) ?: return null
    val hipSpan = findSpanAroundCenter(sampled, hipRow, centerX) ?: return null

    val referenceComponent = components
        .filter { it !== body }
        .mapNotNull { component ->
            val width = component.maxX - component.minX + 1
            val height = component.maxY - component.minY + 1
            val ratio = width.toFloat() / height.toFloat()
            val candidates = listOf(referencePreset.aspectRatio, 1f / referencePreset.aspectRatio)
            val ratioScore = candidates.minOf { abs(it - ratio) }
            val fill = component.area.toFloat() / (width * height).toFloat()
            if (ratioScore < 0.42f && fill > 0.58f) {
                Triple(component, ratioScore, fill)
            } else {
                null
            }
        }
        .minByOrNull { it.second - it.third * 0.1f }
        ?.first
        ?: return null

    val referenceStart = BodyPoint(
        x = referenceComponent.minX / scale,
        y = referenceComponent.centerY / scale,
    )
    val referenceEnd = BodyPoint(
        x = referenceComponent.maxX / scale,
        y = referenceComponent.centerY / scale,
    )

    val points = mapOf(
        MeasurementPointType.ReferenceStart to referenceStart,
        MeasurementPointType.ReferenceEnd to referenceEnd,
        MeasurementPointType.ChestLeft to BodyPoint(chestSpan.first / scale, chestRow / scale),
        MeasurementPointType.ChestRight to BodyPoint(chestSpan.second / scale, chestRow / scale),
        MeasurementPointType.HipLeft to BodyPoint(hipSpan.first / scale, hipRow / scale),
        MeasurementPointType.HipRight to BodyPoint(hipSpan.second / scale, hipRow / scale),
    )

    val confidence = (
        0.44f +
            (body.area.toFloat() / (targetWidth * targetHeight).toFloat()) * 0.35f +
            ((chestSpan.second - chestSpan.first).toFloat() / targetWidth.toFloat()) * 0.15f +
            ((hipSpan.second - hipSpan.first).toFloat() / targetWidth.toFloat()) * 0.12f
        ).coerceIn(0.25f, 0.9f)

    return AutoDetectionResult(
        referencePreset = referencePreset,
        points = points,
        confidenceScore = confidence,
        debugSummary = if (anchors == null) {
            "윤곽 기반 / 몸체=${body.area}px, 기준물체=${referenceComponent.area}px"
        } else {
            "ML 앵커 기반 / 몸체=${body.area}px, 기준물체=${referenceComponent.area}px"
        },
    )
}

data class AutoDetectionAnchors(
    val centerXRatio: Float,
    val chestRowRatio: Float,
    val hipRowRatio: Float,
)

private val ReferencePreset.aspectRatio: Float
    get() = when (this) {
        ReferencePreset.Card -> 1.586f
        ReferencePreset.A4ShortEdge -> 1f / 1.414f
        ReferencePreset.Custom -> 1f
    }

private fun averageCornerColor(pixelMap: androidx.compose.ui.graphics.PixelMap): Color {
    val samplePoints = listOf(
        0 to 0,
        pixelMap.width - 1 to 0,
        0 to pixelMap.height - 1,
        pixelMap.width - 1 to pixelMap.height - 1,
    )
    var red = 0f
    var green = 0f
    var blue = 0f
    samplePoints.forEach { (x, y) ->
        val color = pixelMap[x, y]
        red += color.red
        green += color.green
        blue += color.blue
    }
    return Color(red / 4f, green / 4f, blue / 4f, 1f)
}

private fun colorDistance(a: Color, b: Color): Float {
    return abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue)
}

private data class Component(
    val area: Int,
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int,
    val centerX: Float,
    val centerY: Float,
)

private fun extractComponents(mask: Array<BooleanArray>): List<Component> {
    val height = mask.size
    val width = mask.firstOrNull()?.size ?: return emptyList()
    val visited = Array(height) { BooleanArray(width) }
    val components = mutableListOf<Component>()
    val stack = ArrayDeque<Pair<Int, Int>>()

    for (y in 0 until height) {
        for (x in 0 until width) {
            if (!mask[y][x] || visited[y][x]) continue

            var area = 0
            var minX = x
            var maxX = x
            var minY = y
            var maxY = y
            var xSum = 0f
            var ySum = 0f

            stack.addLast(x to y)
            visited[y][x] = true

            while (stack.isNotEmpty()) {
                val (cx, cy) = stack.removeLast()
                area += 1
                minX = min(minX, cx)
                maxX = max(maxX, cx)
                minY = min(minY, cy)
                maxY = max(maxY, cy)
                xSum += cx
                ySum += cy

                for (ny in max(0, cy - 1)..min(height - 1, cy + 1)) {
                    for (nx in max(0, cx - 1)..min(width - 1, cx + 1)) {
                        if (!visited[ny][nx] && mask[ny][nx]) {
                            visited[ny][nx] = true
                            stack.addLast(nx to ny)
                        }
                    }
                }
            }

            components += Component(
                area = area,
                minX = minX,
                maxX = maxX,
                minY = minY,
                maxY = maxY,
                centerX = xSum / area.toFloat(),
                centerY = ySum / area.toFloat(),
            )
        }
    }

    return components
}

private fun findSpanAroundCenter(
    mask: Array<BooleanArray>,
    targetRow: Int,
    centerX: Int,
): Pair<Int, Int>? {
    for (offset in 0..6) {
        val row = (targetRow + if (offset % 2 == 0) offset / 2 else -(offset + 1) / 2)
            .coerceIn(mask.indices)
        var best: Pair<Int, Int>? = null
        var start = -1
        for (x in mask[row].indices) {
            if (mask[row][x] && start == -1) start = x
            val endOfSpan = (!mask[row][x] || x == mask[row].lastIndex) && start != -1
            if (endOfSpan) {
                val end = if (mask[row][x] && x == mask[row].lastIndex) x else x - 1
                if (centerX in start..end) {
                    return start to end
                }
                if (best == null || (end - start) > (best.second - best.first)) {
                    best = start to end
                }
                start = -1
            }
        }
        if (best != null) return best
    }
    return null
}
