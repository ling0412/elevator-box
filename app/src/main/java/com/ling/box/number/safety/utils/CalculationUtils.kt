package com.ling.box.number.safety.utils

import com.ling.box.number.safety.data.CalculationResult
import com.ling.box.number.safety.data.ElevatorData

fun calculateMaximumOvertravel(data: ElevatorData): CalculationResult {
    val v = data.speed
    val h = data.bufferCompression
    val k = data.carBufferCompression

    val safetyDistanceFactor = 0.1f + 0.035f * v * v
    val conditions = mutableListOf<Pair<String, Float>>()

    data.carGuideTravel.takeIf { it > 0 && h > 0 }?.let { s1 ->
        conditions.add("导轨制导条件 (a)" to (s1 - h - safetyDistanceFactor))
    }
    data.standingHeightTravel.takeIf { it > 0 && h > 0 }?.let { s2 ->
        conditions.add("站人高度条件 (b)" to (s2 - h - (1.0f + 0.035f * v * v)))
    }
    data.highestComponentTravelA.takeIf { it > 0 && h > 0 }?.let { s3 ->
        conditions.add("最高部件a条件 (c)" to (s3 - h - (0.3f + 0.035f * v * v)))
    }
    data.highestComponentTravelB.takeIf { it > 0 && h > 0 }?.let { s4 ->
        conditions.add("最高部件b条件 (d)" to (s4 - h - safetyDistanceFactor))
    }
    val calculatedCarOvertravel = data.counterweightGuideTravel.takeIf { it > 0 && k > 0 }?.let { s5 ->
        s5 - k - safetyDistanceFactor
    } ?: 0f

    val (limitingCondition, minOvertravelValue) = conditions
        .filter { it.second >= 0 }
        .minByOrNull { it.second }
        ?: ("等待输入" to 0f)

    val allSValuesPresent = data.carGuideTravel > 0 && data.standingHeightTravel > 0 &&
            data.highestComponentTravelA > 0 && data.highestComponentTravelB > 0 &&
            data.counterweightGuideTravel > 0
    val buffersPresent = h > 0 && k > 0
    val isComplete = allSValuesPresent && buffersPresent

    val currentLimitingText = when {
        isComplete && minOvertravelValue >= 0 -> limitingCondition
        conditions.any { it.second >= 0 } -> "部分输入"
        v > 0f -> "等待缓冲/行程参数"
        else -> "请选择速度"
    }

    val resultA = conditions.find { it.first == "导轨制导条件 (a)" }?.second ?: 0f
    val resultB = conditions.find { it.first == "站人高度条件 (b)" }?.second ?: 0f
    val resultC = conditions.find { it.first == "最高部件a条件 (c)" }?.second ?: 0f
    val resultD = conditions.find { it.first == "最高部件b条件 (d)" }?.second ?: 0f

    return CalculationResult(
        a = resultA,
        b = resultB,
        c = resultC,
        d = resultD,
        e = calculatedCarOvertravel,
        maxOvertravel = if (isComplete && minOvertravelValue >= 0) minOvertravelValue else 0f,
        limitingCondition = currentLimitingText,
        isComplete = isComplete
    )
}

fun calculateMinPitVerticalDistance(horizontalDistance: Float): Float {
    return when {
        horizontalDistance < 0.15f -> 0.15f
        horizontalDistance <= 0.50f -> 0.10f + (horizontalDistance - 0.15f) * (8.0f / 7.0f)
        else -> 0.50f
    }
}

