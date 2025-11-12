package com.ling.box.calculator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ling.box.calculator.model.BalanceCoefficientAlgorithm
import kotlin.math.abs
import kotlin.math.max

@Composable
fun CurrentChart(
    upwardCurrentPoints: List<Pair<Double, Float>>,
    downwardCurrentPoints: List<Pair<Double, Float>>,
    useCustomBlockInput: Boolean,
    defaultLoadPercentages: List<Int>,
    algorithm: BalanceCoefficientAlgorithm = BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION,
    r2Value: Double? = null, // R²值，只在拟合算法时显示
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val colors = remember(colorScheme) {
        mapOf(
            "axis" to colorScheme.onSurface.copy(alpha = 0.5f),
            "grid" to colorScheme.onSurface.copy(alpha = 0.1f),
            "primary" to colorScheme.primary,
            "secondary" to colorScheme.secondary,
            "surface" to colorScheme.surfaceVariant.copy(alpha = 0.1f),
            "text" to colorScheme.onSurface,
            "textHighlight" to colorScheme.primary
        )
    }
    val density = LocalDensity.current

    val textPaint = remember(density, colors) {
        android.graphics.Paint().apply {
            color = colors["text"]!!.toArgb()
            textSize = with(density) { 12.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val r2TextPaint = remember(density, colors) {
        android.graphics.Paint().apply {
            color = colors["text"]!!.toArgb()
            textSize = with(density) { 11.sp.toPx() }
            textAlign = android.graphics.Paint.Align.LEFT
            isAntiAlias = true
            isFakeBoldText = true
        }
    }
    val yAxisTextPaint = remember(density, colors) {
        android.graphics.Paint().apply {
            color = colors["text"]!!.toArgb()
            textSize = with(density) { 11.sp.toPx() }
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }
    }
    val axisPaint = remember(density, colors) {
        Paint().apply {
            color = colors["axis"]!!
            strokeWidth = with(density) { 1.5.dp.toPx() }
            isAntiAlias = true
            style = PaintingStyle.Stroke
        }
    }
    val gridPaint = remember(density, colors) {
        Paint().apply {
            color = colors["grid"]!!
            strokeWidth = with(density) { 0.8.dp.toPx() }
            isAntiAlias = true
            style = PaintingStyle.Stroke
        }
    }
    val curvePaintPrimary = remember(density, colors) {
        Paint().apply {
            color = colors["primary"]!!
            strokeWidth = with(density) { 3.dp.toPx() }
            isAntiAlias = true
            style = PaintingStyle.Stroke
            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Round
        }
    }
    val curvePaintSecondary = remember(density, colors) {
        Paint().apply {
            color = colors["secondary"]!!
            strokeWidth = with(density) { 3.dp.toPx() }
            isAntiAlias = true
            style = PaintingStyle.Stroke
            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Round
        }
    }
    val pointPaintPrimary = remember(colors) { Paint().apply { color = colors["primary"]!!; isAntiAlias = true } }
    val pointPaintSecondary = remember(colors) { Paint().apply { color = colors["secondary"]!!; isAntiAlias = true } }
    val pointBorderPaint = remember(density) { Paint().apply { color = Color.White; strokeWidth = with(density) { 1.dp.toPx() }; isAntiAlias = true; style = PaintingStyle.Stroke } }

    Box(
        modifier = modifier
            .background(
                color = colors["surface"]!!,
                shape = MaterialTheme.shapes.medium
            )
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val paddingLeft = with(density) { 48.dp.toPx() }
            val paddingBottom = with(density) { 48.dp.toPx() }
            val paddingTop = with(density) { 32.dp.toPx() }
            val paddingRight = with(density) { 32.dp.toPx() }
            val drawWidth = size.width - paddingLeft - paddingRight
            val drawHeight = size.height - paddingTop - paddingBottom

            if (drawWidth <= 0 || drawHeight <= 0) return@Canvas

            val allYValues = upwardCurrentPoints.map { it.second } + downwardCurrentPoints.map { it.second }
            val actualMax = allYValues.maxOrNull() ?: 10f
            val actualMin = allYValues.minOrNull() ?: 0f
            val range = actualMax - actualMin
            val yBuffer = if (range < 1e-6f) 1f else range * 0.1f
            val maxY = actualMax + yBuffer
            val minY = max(0f, actualMin - yBuffer)
            val effectiveRangeY = maxY - minY
            if (effectiveRangeY < 1e-6f) return@Canvas

            val numYGridLines = 5
            repeat(numYGridLines + 1) { i ->
                val yPos = size.height - paddingBottom - (i * drawHeight / numYGridLines)
                drawLine(
                    color = gridPaint.color,
                    start = Offset(paddingLeft, yPos),
                    end = Offset(size.width - paddingRight, yPos),
                    strokeWidth = gridPaint.strokeWidth
                )
                val value = minY + (effectiveRangeY * i / numYGridLines)
                drawContext.canvas.nativeCanvas.drawText(
                    "%.1f".format(value),
                    paddingLeft - with(density) { 10.dp.toPx() },
                    yPos + (yAxisTextPaint.textSize / 3),
                    yAxisTextPaint
                )
            }

            val xDataPoints = (upwardCurrentPoints.map { it.first } + downwardCurrentPoints.map { it.first }).distinct().sorted().map { it.toFloat() }
            val xReferencePoints = if (useCustomBlockInput) {
                xDataPoints
            } else {
                defaultLoadPercentages.map { it.toFloat() }.distinct().sorted()
            }
            val minX = xReferencePoints.firstOrNull() ?: 0f
            val maxX = xReferencePoints.lastOrNull() ?: 100f
            val rangeX = maxX - minX
            if (rangeX < 1e-6f && xReferencePoints.size < 2) return@Canvas

            val mandatoryPercentages = listOf(40f, 50f)
            val minSpacingPx = with(density) { 55.dp.toPx() }
            val mergeThreshold = 1.5f

            val allPotentialLabels = xReferencePoints.toMutableSet()
            allPotentialLabels.add(minX)
            allPotentialLabels.add(maxX)
            allPotentialLabels.addAll(mandatoryPercentages)

            val sortedPotentialLabels = allPotentialLabels.toList().sorted().filter { it >= minX && it <= maxX }

            val visibleXLabels = mutableListOf<Float>()
            var lastXPosPx = Float.NEGATIVE_INFINITY

            var i = 0
            while (i < sortedPotentialLabels.size) {
                val current = sortedPotentialLabels[i]
                val mergeGroup = mutableListOf<Float>()
                mergeGroup.add(current)
                var j = i

                while (j + 1 < sortedPotentialLabels.size &&
                    abs(sortedPotentialLabels[j + 1] - current) < mergeThreshold) {
                    j++
                    mergeGroup.add(sortedPotentialLabels[j])
                }

                // 优先选择实际输入点（xDataPoints中的点）
                val actualDataPointInGroup = mergeGroup.firstOrNull { it in xDataPoints }
                val labelToAdd = if (actualDataPointInGroup != null) {
                    // 如果有实际输入点，优先使用实际输入点
                    actualDataPointInGroup
                } else if (useCustomBlockInput) {
                    // 自定义模式下，使用当前值
                    current
                } else {
                    // 标准模式下，检查是否是锚点
                    val isAnchor = mandatoryPercentages.any { mandatory ->
                        mergeGroup.any { abs(it - mandatory) < 0.5f }
                    } || current == minX || current == maxX
                    
                    if (isAnchor) {
                        // 如果是锚点，使用最接近的强制点或边界点
                        val anchorValue = mandatoryPercentages.firstOrNull { mandatory ->
                            mergeGroup.any { abs(it - mandatory) < 0.5f }
                        } ?: (if (current == minX) minX else if (current == maxX) maxX else current)
                        anchorValue
                    } else {
                        // 否则使用平均值
                        mergeGroup.average().toFloat()
                    }
                }

                // 判断是否是锚点（用于显示优先级）
                val isAnchor = if (actualDataPointInGroup != null) {
                    // 如果选择了实际输入点，检查它是否接近强制点
                    mandatoryPercentages.any { abs(labelToAdd - it) < 0.5f } || labelToAdd == minX || labelToAdd == maxX
                } else {
                    mandatoryPercentages.any { mandatory ->
                        mergeGroup.any { abs(it - mandatory) < 0.5f }
                    } || current == minX || current == maxX
                }

                val xPosPx = paddingLeft + drawWidth * (labelToAdd - minX) / rangeX

                if (isAnchor || xPosPx - lastXPosPx >= minSpacingPx || visibleXLabels.isEmpty()) {
                    if (isAnchor && visibleXLabels.isNotEmpty() && xPosPx - lastXPosPx < minSpacingPx * 0.6f) {
                        val lastLabel = visibleXLabels.last()
                        // 检查上一个标签是否是锚点（强制点、边界点或接近强制点的实际输入点）
                        val isLastAnchor = mandatoryPercentages.any { abs(lastLabel - it) < 0.5f } || 
                                          lastLabel == minX || lastLabel == maxX

                        if (!isLastAnchor) {
                            visibleXLabels.removeAt(visibleXLabels.lastIndex)
                            visibleXLabels.add(labelToAdd)
                            lastXPosPx = xPosPx
                        }
                    } else {
                        visibleXLabels.add(labelToAdd)
                        lastXPosPx = xPosPx
                    }
                }

                i = j + 1
            }

            visibleXLabels.distinct().sorted().forEach { xPercent ->
                val xPosPx = paddingLeft + drawWidth * (xPercent - minX) / (if (rangeX > 1e-6f) rangeX else 1f)

                drawLine(
                    color = gridPaint.color,
                    start = Offset(xPosPx, paddingTop),
                    end = Offset(xPosPx, size.height - paddingBottom),
                    strokeWidth = gridPaint.strokeWidth
                )

                val isKeyPoint = mandatoryPercentages.any { abs(xPercent - it) < 0.5f }
                textPaint.isFakeBoldText = isKeyPoint
                textPaint.color = if (isKeyPoint) colors["textHighlight"]!!.toArgb() else colors["text"]!!.toArgb()

                drawContext.canvas.nativeCanvas.drawText(
                    if (useCustomBlockInput) "%.1f%%".format(xPercent) else "${xPercent.toInt()}%",
                    xPosPx,
                    size.height - paddingBottom + with(density) { 18.dp.toPx() },
                    textPaint
                )
            }

            textPaint.isFakeBoldText = false
            textPaint.color = colors["text"]!!.toArgb()

            drawLine(
                color = axisPaint.color,
                start = Offset(paddingLeft, size.height - paddingBottom),
                end = Offset(size.width - paddingRight, size.height - paddingBottom),
                strokeWidth = axisPaint.strokeWidth
            )
            drawLine(
                color = axisPaint.color,
                start = Offset(paddingLeft, size.height - paddingBottom),
                end = Offset(paddingLeft, paddingTop),
                strokeWidth = axisPaint.strokeWidth
            )

            // Catmull-Rom样条插值函数（必须在 createSplinePath 之前定义）
            fun catmullRom(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
                val t2 = t * t
                val t3 = t2 * t
                
                val x = 0.5f * (
                    (2f * p1.x) +
                    (-p0.x + p2.x) * t +
                    (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 +
                    (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3
                )
                
                val y = 0.5f * (
                    (2f * p1.y) +
                    (-p0.y + p2.y) * t +
                    (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
                    (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3
                )
                
                return Offset(x, y)
            }
            
            // 三次样条插值函数：创建平滑曲线并经过所有数据点
            fun createSplinePath(points: List<Pair<Double, Float>>): Path {
                val path = Path()
                if (points.isEmpty()) return path
                if (points.size == 1) {
                    val (xPercent, yValue) = points[0]
                    val x = paddingLeft + drawWidth * (xPercent.toFloat() - minX) / (if (rangeX > 1e-6f) rangeX else 1f)
                    val y = size.height - paddingBottom - drawHeight * (yValue - minY) / effectiveRangeY
                    path.moveTo(x, y)
                    return path
                }
                
                // 如果只有2个点，使用直线连接
                if (points.size == 2) {
                    val (x1, y1) = points[0]
                    val (x2, y2) = points[1]
                    val px1 = paddingLeft + drawWidth * (x1.toFloat() - minX) / (if (rangeX > 1e-6f) rangeX else 1f)
                    val py1 = size.height - paddingBottom - drawHeight * (y1 - minY) / effectiveRangeY
                    val px2 = paddingLeft + drawWidth * (x2.toFloat() - minX) / (if (rangeX > 1e-6f) rangeX else 1f)
                    val py2 = size.height - paddingBottom - drawHeight * (y2 - minY) / effectiveRangeY
                    path.moveTo(px1, py1)
                    path.lineTo(px2, py2)
                    return path
                }
                
                // 三个或更多点：使用Catmull-Rom样条插值
                val sortedPoints = points.sortedBy { it.first }
                val screenPoints = sortedPoints.map { (xPercent, yValue) ->
                    val x = paddingLeft + drawWidth * (xPercent.toFloat() - minX) / (if (rangeX > 1e-6f) rangeX else 1f)
                    val y = size.height - paddingBottom - drawHeight * (yValue - minY) / effectiveRangeY
                    Offset(x, y)
                }
                
                // 移动到第一个点
                path.moveTo(screenPoints[0].x, screenPoints[0].y)
                
                // 使用Catmull-Rom样条连接所有点
                for (i in 0 until screenPoints.size - 1) {
                    val p0 = if (i > 0) screenPoints[i - 1] else screenPoints[i]
                    val p1 = screenPoints[i]
                    val p2 = screenPoints[i + 1]
                    val p3 = if (i < screenPoints.size - 2) screenPoints[i + 2] else screenPoints[i + 1]
                    
                    // Catmull-Rom样条：在p1和p2之间插值
                    // t=0时经过p1，t=1时经过p2，确保曲线经过所有数据点
                    val steps = 50 // 插值步数，越多越平滑
                    for (j in 0..steps) {
                        val t = j.toFloat() / steps
                        val point = catmullRom(p0, p1, p2, p3, t)
                        if (j == 0 && i > 0) {
                            // 对于非第一段，第一个点（t=0）就是上一个段的最后一个点，跳过避免重复
                            continue
                        }
                        path.lineTo(point.x, point.y)
                    }
                }
                
                return path
            }
            
            // 创建直线路径（用于非拟合算法）
            fun createLinePath(points: List<Pair<Double, Float>>): Path {
                val path = Path()
                if (points.isEmpty()) return path
                
                val sortedPoints = points.sortedBy { it.first }
                sortedPoints.forEachIndexed { index, (xPercent, yValue) ->
                    val x = paddingLeft + drawWidth * (xPercent.toFloat() - minX) / (if (rangeX > 1e-6f) rangeX else 1f)
                    val y = size.height - paddingBottom - drawHeight * (yValue - minY) / effectiveRangeY
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                return path
            }

            fun drawCurveAndPoints(points: List<Pair<Double, Float>>, curvePaint: Paint, pointPaint: Paint) {
                if (points.isEmpty()) return
                
                // 根据算法类型选择使用曲线还是直线
                val path = if (algorithm == BalanceCoefficientAlgorithm.LINEAR_REGRESSION) {
                    // 拟合算法：使用平滑曲线
                    createSplinePath(points)
                } else {
                    // 其他算法：使用直线连接
                    createLinePath(points)
                }
                
                drawPath(path, brush = SolidColor(curvePaint.color), style = Stroke(width = curvePaint.strokeWidth, cap = curvePaint.strokeCap, join = curvePaint.strokeJoin))

                // 绘制数据点
                val pointRadius = with(density) { 4.dp.toPx() }
                val pointBorderRadius = with(density) { 5.dp.toPx() }
                points.forEach { (xPercent, yValue) ->
                    val x = paddingLeft + drawWidth * (xPercent.toFloat() - minX) / (if (rangeX > 1e-6f) rangeX else 1f)
                    val y = size.height - paddingBottom - drawHeight * (yValue - minY) / effectiveRangeY
                    drawCircle(color = Color.White, radius = pointBorderRadius, center = Offset(x, y), style = Stroke(width = pointBorderPaint.strokeWidth))
                    drawCircle(color = pointPaint.color, radius = pointRadius, center = Offset(x, y))
                }
            }

            drawCurveAndPoints(upwardCurrentPoints, curvePaintPrimary, pointPaintPrimary)
            drawCurveAndPoints(downwardCurrentPoints, curvePaintSecondary, pointPaintSecondary)
            
            // 在拟合算法时显示R²值（在图表右上角）
            if (algorithm == BalanceCoefficientAlgorithm.LINEAR_REGRESSION && r2Value != null) {
                val r2Text = "R² = %.3f".format(r2Value)
                val r2X = size.width - paddingRight - with(density) { 8.dp.toPx() }
                val r2Y = paddingTop + with(density) { 16.dp.toPx() }
                
                // 绘制R²值文本背景（可选，增加可读性）
                val textWidth = r2TextPaint.measureText(r2Text)
                val textHeight = r2TextPaint.textSize
                val bgPadding = with(density) { 4.dp.toPx() }
                
                // 绘制半透明背景
                drawRect(
                    color = colors["surface"]!!.copy(alpha = 0.8f),
                    topLeft = Offset(r2X - textWidth - bgPadding, r2Y - textHeight - bgPadding),
                    size = Size(textWidth + bgPadding * 2, textHeight + bgPadding * 2)
                )
                
                // 绘制R²值文本
                drawContext.canvas.nativeCanvas.drawText(
                    r2Text,
                    r2X - textWidth,
                    r2Y,
                    r2TextPaint
                )
            }
        }
    }
}

