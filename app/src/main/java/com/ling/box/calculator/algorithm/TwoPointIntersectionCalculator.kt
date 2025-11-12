package com.ling.box.calculator.algorithm

import kotlin.math.abs
import kotlin.comparisons.minOf
import kotlin.comparisons.maxOf

/**
 * 两点直线交点法计算器
 * 通过相邻数据点构造直线，计算交点
 */
class TwoPointIntersectionCalculator : BalanceCoefficientCalculator {
    
    // 数据点类，用于匹配相同载荷百分比的数据点
    private data class MatchedPoint(
        val loadPercentage: Double, 
        val upwardCurrent: Float, 
        val downwardCurrent: Float
    )
    
    override fun calculate(
        upwardPoints: List<Pair<Double, Float>>,
        downwardPoints: List<Pair<Double, Float>>
    ): Triple<Double?, Boolean, Double?> {
        if (upwardPoints.size < 2 || downwardPoints.size < 2) {
            return Triple(null, false, null)
        }

        // 匹配相同载荷百分比的数据点
        val tolerance = 0.01
        val matchedPoints = mutableListOf<MatchedPoint>()
        
        upwardPoints.forEach { (upX, upY) ->
            val matchedDown = downwardPoints.minByOrNull { abs(it.first - upX) }
            if (matchedDown != null && abs(matchedDown.first - upX) < tolerance) {
                matchedPoints.add(MatchedPoint(upX, upY, matchedDown.second))
            }
        }
        
        matchedPoints.sortBy { it.loadPercentage }
        
        if (matchedPoints.size < 2) {
            return Triple(null, false, null)
        }

        val allIntersections = mutableListOf<Triple<Double, Boolean, Double>>() // (X, isActual, weight)

        // 方法1：查找实际交点（在相邻线段之间）
        for (i in 0 until matchedPoints.size - 1) {
            val p1 = matchedPoints[i]
            val p2 = matchedPoints[i + 1]
            
            val upStart = Pair(p1.loadPercentage, p1.upwardCurrent)
            val upEnd = Pair(p2.loadPercentage, p2.upwardCurrent)
            val downStart = Pair(p1.loadPercentage, p1.downwardCurrent)
            val downEnd = Pair(p2.loadPercentage, p2.downwardCurrent)
            
            val intersectX = lineIntersect(upStart, upEnd, downStart, downEnd)
            if (intersectX != null && intersectX >= -50.0 && intersectX <= 200.0) {
                val minX = minOf(p1.loadPercentage, p2.loadPercentage)
                val maxX = maxOf(p1.loadPercentage, p2.loadPercentage)
                val isActual = intersectX >= minX && intersectX <= maxX
                
                val segmentCenter = (minX + maxX) / 2.0
                val distanceFromCenter = abs(intersectX - segmentCenter)
                val segmentLength = maxX - minX
                val normalizedDistance = if (segmentLength > 1e-10) distanceFromCenter / segmentLength else 1.0
                val weight = if (isActual) {
                    1.0 - normalizedDistance * 0.5
                } else {
                    0.3 * (1.0 - minOf(normalizedDistance / 2.0, 1.0))
                }
                
                allIntersections.add(Triple(intersectX, isActual, weight))
            }
        }

        // 方法2：如果所有点都在同一侧，使用线性插值和外推
        val allUpwardLess = matchedPoints.all { it.upwardCurrent < it.downwardCurrent }
        val allUpwardGreater = matchedPoints.all { it.upwardCurrent > it.downwardCurrent }
        val hasActualIntersection = allIntersections.any { it.second }
        
        if ((allUpwardLess || allUpwardGreater) && !hasActualIntersection) {
            // 检查相邻点之间的电流差是否改变符号
            for (i in 0 until matchedPoints.size - 1) {
                val p1 = matchedPoints[i]
                val p2 = matchedPoints[i + 1]
                
                val diff1 = p1.upwardCurrent - p1.downwardCurrent
                val diff2 = p2.upwardCurrent - p2.downwardCurrent
                
                if (diff1 * diff2 < 0) {
                    val balanceX = calculateInterpolationPoint(p1, p2)
                    if (balanceX != null && balanceX >= -50.0 && balanceX <= 200.0) {
                        allIntersections.add(Triple(balanceX, true, 1.0))
                    }
                }
            }
            
            // 外推逻辑
            val minX = matchedPoints.first().loadPercentage
            val maxX = matchedPoints.last().loadPercentage
            
            val firstDiff = matchedPoints.first().upwardCurrent - matchedPoints.first().downwardCurrent
            val lastDiff = matchedPoints.last().upwardCurrent - matchedPoints.last().downwardCurrent
            val diffChange = lastDiff - firstDiff
            
            val needExtrapolation = when {
                allUpwardLess && diffChange < 0 -> allIntersections.none { it.first <= minX }
                allUpwardLess && diffChange > 0 -> allIntersections.none { it.first >= maxX }
                allUpwardGreater && diffChange > 0 -> allIntersections.none { it.first <= minX }
                allUpwardGreater && diffChange < 0 -> allIntersections.none { it.first >= maxX }
                else -> allIntersections.isEmpty()
            }
            
            if (needExtrapolation) {
                val extrapolatedPoint = performExtrapolation(matchedPoints, allUpwardLess, allUpwardGreater, diffChange)
                if (extrapolatedPoint != null) {
                    allIntersections.add(extrapolatedPoint)
                }
            }
        }

        // 选择最佳交点
        if (allIntersections.isEmpty()) {
            return Triple(null, false, null)
        }

        val actualIntersections = allIntersections.filter { it.second }
        val intersectionsToConsider = if (actualIntersections.isNotEmpty()) {
            actualIntersections
        } else {
            allIntersections
        }
        
        val bestIntersection = selectBestIntersection(
            intersectionsToConsider, matchedPoints, allUpwardLess, allUpwardGreater
        )
        
        return if (bestIntersection != null) {
            Triple(bestIntersection.first, bestIntersection.second, null)
        } else {
            Triple(null, false, null)
        }
    }
    
    private fun lineIntersect(
        a: Pair<Double, Float>, b: Pair<Double, Float>,
        c: Pair<Double, Float>, d: Pair<Double, Float>
    ): Double? {
        val (x1, y1) = a
        val (x2, y2) = b
        val (x3, y3) = c
        val (x4, y4) = d
        
        val dx1 = x2 - x1
        if (abs(dx1) < 1e-10) {
            if (abs(x3 - x4) < 1e-10) {
                return if (abs(x1 - x3) < 1e-10) x1 else null
            }
            return x1
        }
        val k1 = (y2 - y1) / dx1
        val b1 = y1 - k1 * x1
        
        val dx2 = x4 - x3
        if (abs(dx2) < 1e-10) {
            return x3
        }
        val k2 = (y4 - y3) / dx2
        val b2 = y3 - k2 * x3
        
        if (abs(k1 - k2) < 1e-10) {
            return null
        }
        
        return (b2 - b1) / (k1 - k2)
    }
    
    private fun calculateInterpolationPoint(p1: MatchedPoint, p2: MatchedPoint): Double? {
        val x1 = p1.loadPercentage
        val x2 = p2.loadPercentage
        
        val kUp = (p2.upwardCurrent - p1.upwardCurrent) / (x2 - x1)
        val bUp = p1.upwardCurrent - kUp * x1
        
        val kDown = (p2.downwardCurrent - p1.downwardCurrent) / (x2 - x1)
        val bDown = p1.downwardCurrent - kDown * x1
        
        if (abs(kUp - kDown) > 1e-10) {
            return (bDown - bUp) / (kUp - kDown)
        }
        return null
    }
    
    private fun performExtrapolation(
        matchedPoints: List<MatchedPoint>,
        allUpwardLess: Boolean,
        allUpwardGreater: Boolean,
        diffChange: Float
    ): Triple<Double, Boolean, Double>? {
        val minX = matchedPoints.first().loadPercentage
        val maxX = matchedPoints.last().loadPercentage
        val dataRange = maxX - minX
        
        val (p1, p2) = when {
            allUpwardLess && diffChange > 0 -> {
                val lastIndex = matchedPoints.size - 1
                Pair(matchedPoints[lastIndex - 1], matchedPoints[lastIndex])
            }
            allUpwardLess && diffChange < 0 -> {
                Pair(matchedPoints[0], matchedPoints[1])
            }
            allUpwardGreater && diffChange > 0 -> {
                Pair(matchedPoints[0], matchedPoints[1])
            }
            allUpwardGreater && diffChange < 0 -> {
                val lastIndex = matchedPoints.size - 1
                Pair(matchedPoints[lastIndex - 1], matchedPoints[lastIndex])
            }
            else -> {
                val lastIndex = matchedPoints.size - 1
                Pair(matchedPoints[lastIndex - 1], matchedPoints[lastIndex])
            }
        }
        
        val x1 = p1.loadPercentage
        val x2 = p2.loadPercentage
        
        val kUp = (p2.upwardCurrent - p1.upwardCurrent) / (x2 - x1)
        val bUp = p1.upwardCurrent - kUp * x1
        
        val kDown = (p2.downwardCurrent - p1.downwardCurrent) / (x2 - x1)
        val bDown = p1.downwardCurrent - kDown * x1
        
        if (abs(kUp - kDown) > 1e-10) {
            val extrapolatedX = (bDown - bUp) / (kUp - kDown)
            val maxExtrapolation = dataRange * 2.0
            
            val isValidExtrapolation = when {
                extrapolatedX > maxX -> (extrapolatedX - maxX) <= maxExtrapolation
                extrapolatedX < minX -> (minX - extrapolatedX) <= maxExtrapolation
                else -> true
            }
            
            if (isValidExtrapolation && extrapolatedX >= -50.0 && extrapolatedX <= 200.0) {
                val extrapolationDistance = when {
                    extrapolatedX > maxX -> extrapolatedX - maxX
                    extrapolatedX < minX -> minX - extrapolatedX
                    else -> 0.0
                }
                val normalizedDistance = if (dataRange > 1e-10) extrapolationDistance / dataRange else 1.0
                val weight = 0.5 * (1.0 - minOf(normalizedDistance / 2.0, 1.0))
                return Triple(extrapolatedX, false, weight.coerceAtLeast(0.2))
            }
        }
        return null
    }
    
    private fun selectBestIntersection(
        intersections: List<Triple<Double, Boolean, Double>>,
        matchedPoints: List<MatchedPoint>,
        allUpwardLess: Boolean,
        allUpwardGreater: Boolean
    ): Triple<Double, Boolean, Double>? {
        val minX = matchedPoints.first().loadPercentage
        val maxX = matchedPoints.last().loadPercentage
        
        return if (allUpwardLess || allUpwardGreater) {
            val firstDiff = matchedPoints.first().upwardCurrent - matchedPoints.first().downwardCurrent
            val lastDiff = matchedPoints.last().upwardCurrent - matchedPoints.last().downwardCurrent
            val diffChange = lastDiff - firstDiff
            
            if (allUpwardLess) {
                if (diffChange > 0) {
                    val rightSideIntersections = intersections.filter { it.first >= maxX }
                    rightSideIntersections.minByOrNull { abs(it.first - maxX) }
                        ?: intersections.minByOrNull { abs(it.first - maxX) }
                        ?: intersections.maxByOrNull { it.third }
                } else {
                    selectLeftSideIntersection(intersections, minX)
                        ?: intersections.minByOrNull { abs(it.first - minX) }
                        ?: intersections.maxByOrNull { it.third }
                }
            } else {
                if (diffChange > 0) {
                    selectLeftSideIntersection(intersections, minX)
                        ?: intersections.minByOrNull { abs(it.first - minX) }
                        ?: intersections.maxByOrNull { it.third }
                } else {
                    val rightSideIntersections = intersections.filter { it.first >= maxX }
                    rightSideIntersections.minByOrNull { abs(it.first - maxX) }
                        ?: intersections.minByOrNull { abs(it.first - maxX) }
                        ?: intersections.maxByOrNull { it.third }
                }
            }
        } else {
            intersections.maxByOrNull { it.third }
        }
    }
    
    private fun selectLeftSideIntersection(
        intersections: List<Triple<Double, Boolean, Double>>,
        minX: Double
    ): Triple<Double, Boolean, Double>? {
        val leftSideIntersections = intersections.filter { it.first <= minX }
        if (leftSideIntersections.isEmpty()) return null
        
        val nearZeroIntersections = leftSideIntersections.filter { abs(it.first) <= 1.0 }
        val otherLeftIntersections = leftSideIntersections.filter { abs(it.first) > 1.0 }
        
        val candidatesToConsider = if (nearZeroIntersections.isNotEmpty() && otherLeftIntersections.isNotEmpty()) {
            otherLeftIntersections
        } else {
            leftSideIntersections
        }
        
        val maxWeight = candidatesToConsider.maxOfOrNull { it.third } ?: 0.0
        val highestWeightLeft = candidatesToConsider.filter { it.third >= maxWeight - 0.01 }
        
        return if (highestWeightLeft.size > 1) {
            highestWeightLeft.minByOrNull { it.first } ?: highestWeightLeft.first()
        } else {
            highestWeightLeft.firstOrNull()
        }
    }
}

