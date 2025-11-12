package com.ling.box.calculator.algorithm

import kotlin.math.abs
import kotlin.comparisons.minOf
import kotlin.comparisons.maxOf

/**
 * 线性拟合算法计算器
 * 使用最小二乘法拟合直线，然后求交点
 */
class LinearRegressionCalculator : BalanceCoefficientCalculator {
    
    // 数据点类，用于匹配相同载荷百分比的数据点
    private data class DataPoint(
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
        val matchedPoints = mutableListOf<DataPoint>()
        
        upwardPoints.forEach { (upX, upY) ->
            val matchedDown = downwardPoints.minByOrNull { abs(it.first - upX) }
            if (matchedDown != null && abs(matchedDown.first - upX) < tolerance) {
                matchedPoints.add(DataPoint(upX, upY, matchedDown.second))
            }
        }
        
        matchedPoints.sortBy { it.loadPercentage }
        
        if (matchedPoints.isEmpty()) {
            return Triple(null, false, null)
        }

        // 分析约束关系
        var lowerBound = 0.0
        var upperBound = 200.0
        
        matchedPoints.forEach { point ->
            val diff = point.upwardCurrent - point.downwardCurrent
            if (abs(diff) < 0.01f) {
                return Triple(point.loadPercentage, true, null)
            } else if (diff < 0) {
                lowerBound = maxOf(lowerBound, point.loadPercentage)
            } else {
                upperBound = minOf(upperBound, point.loadPercentage)
            }
        }

        val hasConstraintConflict = lowerBound >= upperBound
        val allUpwardLess = matchedPoints.all { it.upwardCurrent < it.downwardCurrent }
        val allUpwardGreater = matchedPoints.all { it.upwardCurrent > it.downwardCurrent }
        
        // 趋势分析
        var trendIndicatesLeftSide = false
        var trendBasedEstimate: Double? = null
        
        if ((allUpwardLess || allUpwardGreater) && !hasConstraintConflict) {
            val analysis = analyzeTrend(matchedPoints, allUpwardLess, allUpwardGreater)
            trendIndicatesLeftSide = analysis.first
            trendBasedEstimate = analysis.second
            if (trendIndicatesLeftSide && trendBasedEstimate != null) {
                lowerBound = trendBasedEstimate.coerceAtLeast(-50.0)
                upperBound = (matchedPoints.maxOfOrNull { it.loadPercentage } ?: 200.0) + 
                    (matchedPoints.maxOfOrNull { it.loadPercentage } ?: 0.0) - 
                    (matchedPoints.minOfOrNull { it.loadPercentage } ?: 0.0) * 0.3
            }
        }
        
        if (hasConstraintConflict) {
            val minLoad = matchedPoints.minOfOrNull { it.loadPercentage } ?: 0.0
            val maxLoad = matchedPoints.maxOfOrNull { it.loadPercentage } ?: 200.0
            val dataRange = maxLoad - minLoad
            val expansionFactor = 0.3
            lowerBound = (minLoad - dataRange * expansionFactor).coerceAtLeast(0.0)
            upperBound = (maxLoad + dataRange * expansionFactor).coerceAtMost(200.0)
            if (lowerBound >= upperBound) {
                lowerBound = 0.0
                upperBound = 200.0
            }
        }

        // 方法1：线性插值找到平衡点
        val balancePoints = mutableListOf<Pair<Double, Double>>()
        
        for (i in 0 until matchedPoints.size - 1) {
            val p1 = matchedPoints[i]
            val p2 = matchedPoints[i + 1]
            
            val diff1 = p1.upwardCurrent - p1.downwardCurrent
            val diff2 = p2.upwardCurrent - p2.downwardCurrent
            
            if (diff1 * diff2 < 0) {
                val balanceX = calculateInterpolation(p1, p2)
                if (balanceX != null) {
                    val weight = calculateInterpolationWeight(balanceX, p1.loadPercentage, p2.loadPercentage, hasConstraintConflict)
                    val isValidRange = if (hasConstraintConflict) {
                        balanceX >= 0.0 && balanceX <= 200.0
                    } else {
                        balanceX >= lowerBound && balanceX <= upperBound
                    }
                    if (isValidRange) {
                        balancePoints.add(Pair(balanceX, weight))
                    }
                }
            }
        }
        
        // 如果所有点都在同一侧且没有插值点，进行外推
        if ((allUpwardLess || allUpwardGreater) && balancePoints.isEmpty()) {
            val extrapolatedPoints = performExtrapolation(matchedPoints, allUpwardLess, allUpwardGreater, hasConstraintConflict)
            balancePoints.addAll(extrapolatedPoints)
        }

        // 方法2：使用线性拟合找到交点
        val upwardFit = linearRegression(upwardPoints) ?: return handleNoFitResult(balancePoints, matchedPoints, hasConstraintConflict, lowerBound, upperBound, allUpwardLess, allUpwardGreater)
        val downwardFit = linearRegression(downwardPoints) ?: return handleNoFitResult(balancePoints, matchedPoints, hasConstraintConflict, lowerBound, upperBound, allUpwardLess, allUpwardGreater)
        
        val (a1, b1, r2Upward) = upwardFit
        val (a2, b2, r2Downward) = downwardFit
        val averageR2 = (r2Upward + r2Downward) / 2.0
        
        if (abs(a1 - a2) < 1e-10) {
            return handleParallelLines(balancePoints, matchedPoints, hasConstraintConflict, lowerBound, upperBound, averageR2)
        }

        val fittedIntersectX = (b2 - b1) / (a1 - a2)
        val maxX = matchedPoints.last().loadPercentage
        val minX = matchedPoints.first().loadPercentage
        val dataRange = maxX - minX
        
        val isFittedInDataRange = fittedIntersectX >= minX && fittedIntersectX <= maxX
        val fittedExtrapolationRatio = calculateExtrapolationRatio(fittedIntersectX, minX, maxX, dataRange)
        val maxFittedExtrapolationRatio = if (matchedPoints.size <= 3) 1.5 else 2.5
        val isFittedExtrapolationValid = fittedExtrapolationRatio <= maxFittedExtrapolationRatio
        
        val isFittedInValidRange = if (hasConstraintConflict) {
            fittedIntersectX >= 0.0 && fittedIntersectX <= 200.0 && isFittedExtrapolationValid
        } else {
            fittedIntersectX >= lowerBound && fittedIntersectX <= upperBound && isFittedExtrapolationValid
        }
        
        val minR2Threshold = 0.3
        val goodR2Threshold = 0.7
        val shouldRejectFit = averageR2 < minR2Threshold && matchedPoints.size <= 2
        
        if (isFittedInValidRange && !shouldRejectFit) {
            return handleValidFit(balancePoints, fittedIntersectX, isFittedInDataRange, fittedExtrapolationRatio, 
                maxFittedExtrapolationRatio, averageR2, matchedPoints, hasConstraintConflict, lowerBound, upperBound,
                minX, maxX, trendIndicatesLeftSide, trendBasedEstimate, goodR2Threshold)
        } else {
            return handleInvalidFit(balancePoints, fittedIntersectX, fittedExtrapolationRatio, maxFittedExtrapolationRatio,
                averageR2, matchedPoints, hasConstraintConflict, lowerBound, upperBound, allUpwardLess, allUpwardGreater)
        }
    }
    
    private fun analyzeTrend(
        matchedPoints: List<DataPoint>,
        allUpwardLess: Boolean,
        allUpwardGreater: Boolean
    ): Pair<Boolean, Double?> {
        val currentDiffs = matchedPoints.map { it.upwardCurrent - it.downwardCurrent }
        val n = matchedPoints.size.toDouble()
        val sumX = matchedPoints.sumOf { it.loadPercentage }
        val sumY = currentDiffs.sumOf { it.toDouble() }
        val sumXY = matchedPoints.zip(currentDiffs).sumOf { (point, diff) -> point.loadPercentage * diff.toDouble() }
        val sumXX = matchedPoints.sumOf { it.loadPercentage * it.loadPercentage }
        val denominator = n * sumXX - sumX * sumX
        
        if (abs(denominator) > 1e-10) {
            val slope = (n * sumXY - sumX * sumY) / denominator
            val minLoad = matchedPoints.minOfOrNull { it.loadPercentage } ?: 0.0
            val maxLoad = matchedPoints.maxOfOrNull { it.loadPercentage } ?: 200.0
            val dataRange = maxLoad - minLoad
            
            if ((slope < 0 && allUpwardLess) || (slope > 0 && allUpwardGreater)) {
                val avgDiff = currentDiffs.average()
                val absAvgDiff = abs(avgDiff)
                val offsetRatio = when {
                    absAvgDiff < 2.0 -> 0.2
                    absAvgDiff < 5.0 -> 0.3
                    else -> 0.4
                }
                val estimatedLowerBound = minLoad - dataRange * offsetRatio
                return Pair(true, estimatedLowerBound.coerceAtLeast(-50.0))
            }
        }
        return Pair(false, null)
    }
    
    private fun calculateInterpolation(p1: DataPoint, p2: DataPoint): Double? {
        val x1 = p1.loadPercentage
        val x2 = p2.loadPercentage
        val y1 = (p1.upwardCurrent - p1.downwardCurrent).toDouble()
        val y2 = (p2.upwardCurrent - p2.downwardCurrent).toDouble()
        
        if (abs(y2 - y1) < 1e-10) return null
        val balanceX = x1 - y1 * (x2 - x1) / (y2 - y1)
        return if (balanceX >= -50.0 && balanceX <= 200.0) balanceX else null
    }
    
    private fun calculateInterpolationWeight(
        balanceX: Double,
        x1: Double,
        x2: Double,
        hasConstraintConflict: Boolean
    ): Double {
        val t = (balanceX - x1) / (x2 - x1)
        val distanceFromCenter = abs(t - 0.5)
        var weight = (1.0 - distanceFromCenter * 1.5).coerceIn(0.3, 1.0)
        if (hasConstraintConflict) {
            weight *= 0.8
        }
        return weight
    }
    
    private fun performExtrapolation(
        matchedPoints: List<DataPoint>,
        allUpwardLess: Boolean,
        allUpwardGreater: Boolean,
        hasConstraintConflict: Boolean
    ): List<Pair<Double, Double>> {
        val result = mutableListOf<Pair<Double, Double>>()
        val minX = matchedPoints.first().loadPercentage
        val maxX = matchedPoints.last().loadPercentage
        val dataRange = maxX - minX
        
        val currentDiffs = matchedPoints.map { it.upwardCurrent - it.downwardCurrent }
        val n = matchedPoints.size.toDouble()
        val sumX = matchedPoints.sumOf { it.loadPercentage }
        val sumY = currentDiffs.sumOf { it.toDouble() }
        val sumXY = matchedPoints.zip(currentDiffs).sumOf { (point, diff) -> point.loadPercentage * diff.toDouble() }
        val sumXX = matchedPoints.sumOf { it.loadPercentage * it.loadPercentage }
        val denominator = n * sumXX - sumX * sumX
        
        if (abs(denominator) > 1e-10) {
            val slope = (n * sumXY - sumX * sumY) / denominator
            val intercept = (sumY - slope * sumX) / n
            
            val extrapolatedX = if (abs(slope) > 1e-10) {
                -intercept / slope
            } else {
                null
            }
            
            if (extrapolatedX != null) {
                val distanceFromDataRange = calculateExtrapolationRatio(extrapolatedX, minX, maxX, dataRange)
                val maxExtrapolationRatio = if (matchedPoints.size <= 3) 1.0 else 2.0
                
                val isValidExtrapolation = when {
                    extrapolatedX >= minX && extrapolatedX <= maxX -> true
                    distanceFromDataRange > maxExtrapolationRatio -> false
                    extrapolatedX >= -50.0 && extrapolatedX <= 200.0 -> true
                    else -> false
                }
                
                if (isValidExtrapolation) {
                    var baseWeight = 0.6
                    val distancePenalty = minOf(distanceFromDataRange / maxExtrapolationRatio, 1.0)
                    baseWeight *= (1.0 - distancePenalty * 0.6)
                    if (matchedPoints.size <= 3) {
                        baseWeight *= 0.7
                    }
                    if (hasConstraintConflict) {
                        baseWeight *= 0.8
                    }
                    result.add(Pair(extrapolatedX, baseWeight.coerceIn(0.15, 0.6)))
                } else if (distanceFromDataRange > maxExtrapolationRatio) {
                    // 处理外推距离过大的情况
                    val conservativeEstimate = calculateConservativeEstimate(
                        matchedPoints, slope, allUpwardLess, allUpwardGreater, minX, maxX, dataRange, currentDiffs
                    )
                    if (conservativeEstimate != null) {
                        result.add(conservativeEstimate)
                    }
                }
            }
        }
        
        return result
    }
    
    private fun calculateConservativeEstimate(
        matchedPoints: List<DataPoint>,
        slope: Double,
        allUpwardLess: Boolean,
        allUpwardGreater: Boolean,
        minX: Double,
        maxX: Double,
        dataRange: Double,
        currentDiffs: List<Float>
    ): Pair<Double, Double>? {
        return when {
            slope < 0 && allUpwardLess -> {
                val avgDiff = currentDiffs.average()
                val absAvgDiff = abs(avgDiff)
                val offsetRatio = when {
                    absAvgDiff < 2.0 -> 0.2
                    absAvgDiff < 5.0 -> 0.3
                    else -> 0.4
                }
                val estimatedK = minX - dataRange * offsetRatio
                if (estimatedK >= -50.0) Pair(estimatedK.coerceAtLeast(-50.0), 0.25) else null
            }
            slope > 0 && allUpwardLess -> {
                val estimatedK = maxX + dataRange * 0.3
                if (estimatedK <= 200.0) Pair(estimatedK.coerceAtMost(200.0), 0.2) else null
            }
            slope > 0 && allUpwardGreater -> {
                val estimatedK = minX - dataRange * 0.3
                if (estimatedK >= -50.0) Pair(estimatedK.coerceAtLeast(-50.0), 0.2) else null
            }
            slope < 0 && allUpwardGreater -> {
                val estimatedK = maxX + dataRange * 0.3
                if (estimatedK <= 200.0) Pair(estimatedK.coerceAtMost(200.0), 0.2) else null
            }
            else -> null
        }
    }
    
    private fun linearRegression(points: List<Pair<Double, Float>>): Triple<Double, Double, Double>? {
        if (points.isEmpty() || points.size < 2) return null
        val n = points.size.toDouble()
        val sumX = points.sumOf { it.first }
        val sumY = points.sumOf { it.second.toDouble() }
        val sumXY = points.sumOf { it.first * it.second.toDouble() }
        val sumXX = points.sumOf { it.first * it.first }
        val denominator = n * sumXX - sumX * sumX
        if (abs(denominator) < 1e-10) return null
        val slope = (n * sumXY - sumX * sumY) / denominator
        val intercept = (sumY - slope * sumX) / n
        
        val meanY = sumY / n
        var totalSumSquares = 0.0
        var residualSumSquares = 0.0
        
        points.forEach { (x, y) ->
            val yValue = y.toDouble()
            val predictedY = slope * x + intercept
            totalSumSquares += (yValue - meanY) * (yValue - meanY)
            residualSumSquares += (yValue - predictedY) * (yValue - predictedY)
        }
        
        val rSquared = if (totalSumSquares < 1e-10) {
            1.0
        } else {
            (1.0 - residualSumSquares / totalSumSquares).coerceIn(0.0, 1.0)
        }
        
        return Triple(slope, intercept, rSquared)
    }
    
    private fun calculateExtrapolationRatio(x: Double, minX: Double, maxX: Double, dataRange: Double): Double {
        return when {
            x > maxX -> (x - maxX) / dataRange.coerceAtLeast(0.01)
            x < minX -> (minX - x) / dataRange.coerceAtLeast(0.01)
            else -> 0.0
        }
    }
    
    private fun handleNoFitResult(
        balancePoints: List<Pair<Double, Double>>,
        matchedPoints: List<DataPoint>,
        hasConstraintConflict: Boolean,
        lowerBound: Double,
        upperBound: Double,
        allUpwardLess: Boolean,
        allUpwardGreater: Boolean
    ): Triple<Double?, Boolean, Double?> {
        if (balancePoints.isEmpty()) {
            if (hasConstraintConflict) {
                val minX = matchedPoints.first().loadPercentage
                val maxX = matchedPoints.last().loadPercentage
                return Triple((minX + maxX) / 2.0, false, null)
            }
            return Triple(null, false, null)
        }
        val weightedAverage = balancePoints.sumOf { it.first * it.second } / balancePoints.sumOf { it.second }
        val finalLowerBound = if (hasConstraintConflict) 0.0 else lowerBound
        val finalUpperBound = if (hasConstraintConflict) 200.0 else upperBound
        val constrainedK = weightedAverage.coerceIn(finalLowerBound, finalUpperBound)
        return Triple(constrainedK, constrainedK >= matchedPoints.first().loadPercentage && 
            constrainedK <= matchedPoints.last().loadPercentage, null)
    }
    
    private fun handleParallelLines(
        balancePoints: List<Pair<Double, Double>>,
        matchedPoints: List<DataPoint>,
        hasConstraintConflict: Boolean,
        lowerBound: Double,
        upperBound: Double,
        averageR2: Double
    ): Triple<Double?, Boolean, Double?> {
        if (balancePoints.isEmpty()) {
            if (hasConstraintConflict) {
                val minX = matchedPoints.first().loadPercentage
                val maxX = matchedPoints.last().loadPercentage
                return Triple((minX + maxX) / 2.0, false, averageR2)
            }
            return Triple(null, false, averageR2)
        }
        val weightedAverage = balancePoints.sumOf { it.first * it.second } / balancePoints.sumOf { it.second }
        val finalLowerBound = if (hasConstraintConflict) 0.0 else lowerBound
        val finalUpperBound = if (hasConstraintConflict) 200.0 else upperBound
        val constrainedK = weightedAverage.coerceIn(finalLowerBound, finalUpperBound)
        return Triple(constrainedK, constrainedK >= matchedPoints.first().loadPercentage && 
            constrainedK <= matchedPoints.last().loadPercentage, averageR2)
    }
    
    private fun handleValidFit(
        balancePoints: MutableList<Pair<Double, Double>>,
        fittedIntersectX: Double,
        isFittedInDataRange: Boolean,
        fittedExtrapolationRatio: Double,
        maxFittedExtrapolationRatio: Double,
        averageR2: Double,
        matchedPoints: List<DataPoint>,
        hasConstraintConflict: Boolean,
        lowerBound: Double,
        upperBound: Double,
        minX: Double,
        maxX: Double,
        trendIndicatesLeftSide: Boolean,
        trendBasedEstimate: Double?,
        goodR2Threshold: Double
    ): Triple<Double?, Boolean, Double?> {
        if (balancePoints.isNotEmpty()) {
            val fittedWeight = calculateFittedWeight(
                averageR2, matchedPoints.size, isFittedInDataRange, fittedExtrapolationRatio,
                maxFittedExtrapolationRatio, balancePoints, fittedIntersectX, hasConstraintConflict
            )
            balancePoints.add(Pair(fittedIntersectX, fittedWeight))
            val weightedAverage = balancePoints.sumOf { it.first * it.second } / balancePoints.sumOf { it.second }
            val conservativeK = if (!isFittedInDataRange && averageR2 < goodR2Threshold && weightedAverage > lowerBound) {
                (lowerBound + weightedAverage) / 2.0
            } else {
                weightedAverage
            }
            val finalLowerBound = if (hasConstraintConflict) 0.0 else lowerBound
            val finalUpperBound = if (hasConstraintConflict) 200.0 else upperBound
            val constrainedK = conservativeK.coerceIn(finalLowerBound, finalUpperBound)
            return Triple(constrainedK, constrainedK >= minX && constrainedK <= maxX, averageR2)
        } else {
            val finalLowerBound = if (hasConstraintConflict) 0.0 else lowerBound
            val finalUpperBound = if (hasConstraintConflict) 200.0 else upperBound
            val useTrendEstimate = averageR2 < goodR2Threshold && trendIndicatesLeftSide && trendBasedEstimate != null
            
            if (useTrendEstimate) {
                val trendWeight = if (fittedExtrapolationRatio > 0.5) 0.8 else 0.6
                val fittedWeight = 1.0 - trendWeight
                val weightedK = trendBasedEstimate!! * trendWeight + fittedIntersectX * fittedWeight
                return Triple(weightedK.coerceIn(finalLowerBound, finalUpperBound), false, averageR2)
            }
            
            if (trendIndicatesLeftSide && trendBasedEstimate != null) {
                if (fittedIntersectX > maxX && fittedExtrapolationRatio > 0.5) {
                    return Triple(trendBasedEstimate, false, averageR2)
                }
                if (isFittedInDataRange && matchedPoints.size <= 3 && averageR2 < goodR2Threshold) {
                    val dataRange = maxX - minX
                    if (fittedIntersectX >= maxX - dataRange * 0.1) {
                        return Triple(trendBasedEstimate, false, averageR2)
                    }
                    val weightedK = trendBasedEstimate * 0.7 + fittedIntersectX * 0.3
                    return Triple(weightedK.coerceIn(finalLowerBound, finalUpperBound), false, averageR2)
                }
            }
            
            val finalK = when {
                !isFittedInDataRange && averageR2 < goodR2Threshold && fittedIntersectX > finalLowerBound -> {
                    (finalLowerBound + fittedIntersectX) / 2.0
                }
                else -> fittedIntersectX
            }
            return Triple(finalK.coerceIn(finalLowerBound, finalUpperBound), isFittedInDataRange, averageR2)
        }
    }
    
    private fun calculateFittedWeight(
        averageR2: Double,
        dataPointCount: Int,
        isFittedInDataRange: Boolean,
        fittedExtrapolationRatio: Double,
        maxFittedExtrapolationRatio: Double,
        balancePoints: List<Pair<Double, Double>>,
        fittedIntersectX: Double,
        hasConstraintConflict: Boolean
    ): Double {
        val excellentR2Threshold = 0.9
        val goodR2Threshold = 0.7
        val minR2Threshold = 0.3
        
        val r2WeightMultiplier = when {
            averageR2 >= excellentR2Threshold -> 1.3
            averageR2 >= goodR2Threshold -> 1.0
            averageR2 >= minR2Threshold -> 0.7
            else -> 0.4
        }
        
        val baseWeight = if (dataPointCount >= 3) {
            1.0 + (dataPointCount - 3.0) * 0.2
        } else {
            1.0
        }
        var fittedWeight = (baseWeight * r2WeightMultiplier).coerceIn(0.3, 1.5)
        
        if (!isFittedInDataRange) {
            val extrapolationPenalty = minOf(fittedExtrapolationRatio / maxFittedExtrapolationRatio, 1.0)
            fittedWeight *= (1.0 - extrapolationPenalty * 0.4)
        }
        
        val closestBalancePoint = balancePoints.minByOrNull { abs(it.first - fittedIntersectX) }
        val distanceToClosest = closestBalancePoint?.let { abs(it.first - fittedIntersectX) } ?: Double.MAX_VALUE
        
        when {
            distanceToClosest < 0.1 -> fittedWeight *= 0.6
            distanceToClosest > 1.0 -> fittedWeight *= 0.8
        }
        
        if (hasConstraintConflict) {
            fittedWeight *= 0.7
        }
        
        return fittedWeight.coerceIn(0.2, 1.5)
    }
    
    private fun handleInvalidFit(
        balancePoints: List<Pair<Double, Double>>,
        fittedIntersectX: Double,
        fittedExtrapolationRatio: Double,
        maxFittedExtrapolationRatio: Double,
        averageR2: Double,
        matchedPoints: List<DataPoint>,
        hasConstraintConflict: Boolean,
        lowerBound: Double,
        upperBound: Double,
        allUpwardLess: Boolean,
        allUpwardGreater: Boolean
    ): Triple<Double?, Boolean, Double?> {
        if (balancePoints.isNotEmpty()) {
            val weightedAverage = balancePoints.sumOf { it.first * it.second } / balancePoints.sumOf { it.second }
            val finalLowerBound = if (hasConstraintConflict) 0.0 else lowerBound
            val finalUpperBound = if (hasConstraintConflict) 200.0 else upperBound
            val constrainedK = weightedAverage.coerceIn(finalLowerBound, finalUpperBound)
            val maxX = matchedPoints.last().loadPercentage
            val minX = matchedPoints.first().loadPercentage
            return Triple(constrainedK, constrainedK >= minX && constrainedK <= maxX, averageR2)
        } else {
            // 使用保守估计
            val maxX = matchedPoints.last().loadPercentage
            val minX = matchedPoints.first().loadPercentage
            val dataRange = maxX - minX
            
            if (fittedExtrapolationRatio > maxFittedExtrapolationRatio) {
                val currentDiffs = matchedPoints.map { it.upwardCurrent - it.downwardCurrent }
                val conservativeEstimate = calculateConservativeEstimateForInvalidFit(
                    matchedPoints, currentDiffs, allUpwardLess, allUpwardGreater, minX, maxX, dataRange
                )
                if (conservativeEstimate != null) {
                    return Triple(conservativeEstimate, false, averageR2)
                }
            }
            
            if (hasConstraintConflict && fittedIntersectX >= 0.0 && fittedIntersectX <= 200.0 
                && fittedExtrapolationRatio <= 2.0) {
                return Triple(fittedIntersectX.coerceIn(0.0, 200.0), false, averageR2)
            }
            
            val finalLowerBound = if (hasConstraintConflict) minX else lowerBound
            val finalUpperBound = if (hasConstraintConflict) maxX * 1.1 else upperBound
            val estimatedK = if (hasConstraintConflict) {
                (minX + maxX) / 2.0
            } else {
                calculateFinalEstimate(matchedPoints, allUpwardLess, allUpwardGreater, minX, maxX, dataRange, finalLowerBound, finalUpperBound)
            }
            return Triple(estimatedK.coerceIn(finalLowerBound.coerceAtLeast(-50.0), finalUpperBound.coerceAtMost(200.0)), false, averageR2)
        }
    }
    
    private fun calculateConservativeEstimateForInvalidFit(
        matchedPoints: List<DataPoint>,
        currentDiffs: List<Float>,
        allUpwardLess: Boolean,
        allUpwardGreater: Boolean,
        minX: Double,
        maxX: Double,
        dataRange: Double
    ): Double? {
        val n = matchedPoints.size.toDouble()
        val sumX = matchedPoints.sumOf { it.loadPercentage }
        val sumY = currentDiffs.sumOf { it.toDouble() }
        val sumXY = matchedPoints.zip(currentDiffs).sumOf { (point, diff) -> point.loadPercentage * diff.toDouble() }
        val sumXX = matchedPoints.sumOf { it.loadPercentage * it.loadPercentage }
        val denominator = n * sumXX - sumX * sumX
        
        if (abs(denominator) > 1e-10) {
            val slope = (n * sumXY - sumX * sumY) / denominator
            
            return when {
                slope < 0 && allUpwardLess -> {
                    val avgDiff = currentDiffs.average()
                    val absAvgDiff = abs(avgDiff)
                    val offsetRatio = when {
                        absAvgDiff < 2.0 -> 0.2
                        absAvgDiff < 5.0 -> 0.3
                        else -> 0.4
                    }
                    val estimatedK = minX - dataRange * offsetRatio
                    if (estimatedK >= -50.0) estimatedK.coerceAtLeast(-50.0) else null
                }
                slope > 0 && allUpwardLess -> {
                    val estimatedK = maxX + dataRange * 0.3
                    if (estimatedK <= 200.0) estimatedK.coerceAtMost(200.0) else null
                }
                slope > 0 && allUpwardGreater -> {
                    val estimatedK = minX - dataRange * 0.3
                    if (estimatedK >= -50.0) estimatedK.coerceAtLeast(-50.0) else null
                }
                slope < 0 && allUpwardGreater -> {
                    val estimatedK = maxX + dataRange * 0.3
                    if (estimatedK <= 200.0) estimatedK.coerceAtMost(200.0) else null
                }
                else -> null
            }
        }
        return null
    }
    
    private fun calculateFinalEstimate(
        matchedPoints: List<DataPoint>,
        allUpwardLess: Boolean,
        allUpwardGreater: Boolean,
        minX: Double,
        maxX: Double,
        dataRange: Double,
        finalLowerBound: Double,
        finalUpperBound: Double
    ): Double {
        val currentDiffs = matchedPoints.map { it.upwardCurrent - it.downwardCurrent }
        val n = matchedPoints.size.toDouble()
        val sumX = matchedPoints.sumOf { it.loadPercentage }
        val sumY = currentDiffs.sumOf { it.toDouble() }
        val sumXY = matchedPoints.zip(currentDiffs).sumOf { (point, diff) -> point.loadPercentage * diff.toDouble() }
        val sumXX = matchedPoints.sumOf { it.loadPercentage * it.loadPercentage }
        val denominator = n * sumXX - sumX * sumX
        
        return if (abs(denominator) > 1e-10) {
            val slope = (n * sumXY - sumX * sumY) / denominator
            
            when {
                slope < 0 && allUpwardLess -> {
                    val avgDiff = currentDiffs.average()
                    val absAvgDiff = abs(avgDiff)
                    val offsetRatio = when {
                        absAvgDiff < 2.0 -> 0.15
                        absAvgDiff < 5.0 -> 0.25
                        else -> 0.35
                    }
                    (minX - dataRange * offsetRatio).coerceAtLeast(-50.0)
                }
                slope > 0 && allUpwardLess -> (maxX + dataRange * 0.2).coerceAtMost(200.0)
                slope > 0 && allUpwardGreater -> (minX - dataRange * 0.2).coerceAtLeast(-50.0)
                slope < 0 && allUpwardGreater -> (maxX + dataRange * 0.2).coerceAtMost(200.0)
                else -> (finalLowerBound + minOf(finalUpperBound, maxX * 1.1)) / 2.0
            }
        } else {
            when {
                allUpwardLess -> (minX - dataRange * 0.2).coerceAtLeast(-50.0)
                allUpwardGreater -> (maxX + dataRange * 0.2).coerceAtMost(200.0)
                else -> (finalLowerBound + minOf(finalUpperBound, maxX * 1.1)) / 2.0
            }
        }
    }
}

