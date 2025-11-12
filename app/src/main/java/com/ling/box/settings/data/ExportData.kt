package com.ling.box.settings.data

import kotlinx.serialization.Serializable

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

