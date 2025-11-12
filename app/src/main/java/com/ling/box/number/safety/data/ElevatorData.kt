package com.ling.box.number.safety.data

import kotlinx.serialization.Serializable

@Serializable
data class ElevatorData(
    val speed: Float = 1.0f,
    val carBufferCompression: Float = 0f,   // 轿厢压缩行程 k (m)
    val bufferCompression: Float = 0f,      // 对重压缩行程 h (m)
    val bufferDistance: Float = 0f,         // 上端站对重缓冲距 h₁ (mm)
    val inputCarOvertravel: Float = 0f,     // 下端站轿厢越程 g (mm)
    val upperLimitDistance: Float = 0f,     // 上极限距离 m (mm)
    val lowerLimitDistance: Float = 0f,     // 下极限距离 n (mm)
    val mainGuideDistance: Float = 0f,      // 主导轨距 (m)
    val auxGuideDistance: Float = 0f,       // 副导轨距 (m)
    val carGuideTravel: Float = 0f,         // 轿厢导轨终止 S₁ (m)
    val standingHeightTravel: Float = 0f,   // 站人高度 S₂ (m)
    val highestComponentTravelA: Float = 0f,// 最高部件a S₃ (m)
    val highestComponentTravelB: Float = 0f,// 最高部件b S₄ (m)
    val counterweightGuideTravel: Float = 0f,// 对重导轨终止 S₅ (m)
    val lowestHorizontalDistance: Float = 0f, // 最低部件水平距离 (m)
    val lowestVerticalDistance: Float = 0f,   // 最低部件垂直距离 (m)
    val pitHighestDistance: Float = 0f,       // 底坑最高距离 (m)
    val ironCounterweight: Int = 0,           // 铁块对重 e₁ (块)
    val concreteCounterweight: Int = 0,       // 水泥块对重 e₂ (块)
    val counterweightHeight: Float = 0f       // 对重块高度 e₃ (m)
)

data class CalculationResult(
    val a: Float,
    val b: Float,
    val c: Float,
    val d: Float,
    val e: Float,
    val maxOvertravel: Float,
    val limitingCondition: String,
    val isComplete: Boolean = false
)

fun ElevatorData.hasValidInput(): Boolean = speed > 0f

