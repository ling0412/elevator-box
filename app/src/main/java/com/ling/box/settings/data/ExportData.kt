package com.ling.box.settings.data

import com.ling.box.calculator.model.UnitState
import kotlinx.serialization.Serializable

fun UnitState.toExport(): UnitStateExport = UnitStateExport(
    name = name,
    creationDate = creationDate,
    ratedLoad = ratedLoad,
    counterweightWeight = counterweightWeight,
    counterweightBlockWeight = counterweightBlockWeight,
    manualBalanceCoefficientK = manualBalanceCoefficientK,
    useManualBalance = useManualBalance,
    useCustomBlockInput = useCustomBlockInput,
    customBlockCounts = customBlockCounts,
    currentReadings = currentReadings
)

fun UnitStateExport.toUnitState(): UnitState {
    // 确保至少有一个块数槽位
    val blockCounts = if (customBlockCounts.isEmpty()) {
        listOf("")
    } else {
        customBlockCounts
    }
    
    // 确保 currentReadings 有两个方向，并且每个方向有足够的槽位
    val readings = if (currentReadings.size < 2) {
        listOf(
            currentReadings.getOrNull(0) ?: listOf(""),
            currentReadings.getOrNull(1) ?: listOf("")
        )
    } else {
        currentReadings
    }
    
    // 确保 readings 的每个方向有足够的槽位
    val adjustedReadings = readings.map { direction ->
        if (direction.size < blockCounts.size) {
            direction + List(blockCounts.size - direction.size) { "" }
        } else {
            direction
        }
    }
    
    return UnitState(
        name = name,
        creationDate = creationDate,
        ratedLoad = ratedLoad,
        counterweightWeight = counterweightWeight,
        counterweightBlockWeight = counterweightBlockWeight,
        manualBalanceCoefficientK = manualBalanceCoefficientK,
        useManualBalance = useManualBalance,
        useCustomBlockInput = useCustomBlockInput,
        customBlockCounts = blockCounts,
        customBlockPercentages = List(blockCounts.size) { null },
        currentReadings = adjustedReadings
    )
}

/**
 * 用于导出的完整数据模型
 */
@Serializable
data class ExportData(
    val version: Int = 1, // 数据格式版本号
    val exportTime: Long = System.currentTimeMillis(),
    val calculatorData: CalculatorExportData,
    val safetyData: SafetyExportData
)

/**
 * 磅梯数据（计算器数据）
 */
@Serializable
data class CalculatorExportData(
    val currentElevatorIndex: Int,
    val balanceCoefficientAlgorithm: Int,
    val elevators: List<UnitStateExport>
)

/**
 * 单台电梯的导出数据
 */
@Serializable
data class UnitStateExport(
    val name: String,
    val creationDate: String,
    val ratedLoad: String,
    val counterweightWeight: String,
    val counterweightBlockWeight: String,
    val manualBalanceCoefficientK: String?,
    val useManualBalance: Boolean,
    val useCustomBlockInput: Boolean,
    val customBlockCounts: List<String>,
    val currentReadings: List<List<String>> // [上行, 下行]
)

/**
 * 自检数据（安全计算数据）
 */
@Serializable
data class SafetyExportData(
    val elevators: Map<String, ElevatorDataExport>
)

/**
 * 自检电梯数据的导出格式
 */
@Serializable
data class ElevatorDataExport(
    val speed: Float,
    val carBufferCompression: Float,
    val bufferCompression: Float,
    val bufferDistance: Float,
    val inputCarOvertravel: Float,
    val upperLimitDistance: Float,
    val lowerLimitDistance: Float,
    val mainGuideDistance: Float,
    val auxGuideDistance: Float,
    val carGuideTravel: Float,
    val standingHeightTravel: Float,
    val highestComponentTravelA: Float,
    val highestComponentTravelB: Float,
    val counterweightGuideTravel: Float,
    val lowestHorizontalDistance: Float,
    val lowestVerticalDistance: Float,
    val pitHighestDistance: Float,
    val ironCounterweight: Int,
    val concreteCounterweight: Int,
    val counterweightHeight: Float
)

