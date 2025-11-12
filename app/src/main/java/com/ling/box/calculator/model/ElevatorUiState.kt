package com.ling.box.calculator.model

import kotlin.math.roundToInt

// 定义暴露给 UI 的、不可变的状态数据 (这个类名保持不变)
data class ElevatorUiState(
    val ratedLoad: String = "1050",
    val counterweightWeight: String = "35",
    val counterweightBlockWeight: String = "25",
    val manualBalanceCoefficientK: String? = null,
    val useManualBalance: Boolean = false,
    val useCustomBlockInput: Boolean = false,

    val customBlockCounts: List<String> = listOf("","","","",""),
    val customBlockPercentages: List<Double?> = listOf(null, null, null, null, null),
    val currentReadings: List<List<String>> = listOf(
        listOf("","","","",""),
        listOf("","","","","")
    ),

    val balanceCoefficientK: Double? = null,
    val balanceCoefficient: Double? = null, // 确认命名和含义
    val recommendedBlocksMessage: String? = null,
    val hasActualIntersection: Boolean = false,
    val linearRegressionR2: Double? = null, // 线性拟合算法的R²值（决定系数）

    val upwardCurrentPoints: List<Pair<Double, Float>> = emptyList(),
    val downwardCurrentPoints: List<Pair<Double, Float>> = emptyList(),

    // 可以在这里添加一些 UI 计算的辅助状态，例如
    val isCalculationPossible: Boolean = false,
    val estimatedBlocksTableData: List<EstimatedBlocksRowData> = emptyList() // 表格数据示例
) {
    // 静态默认值
    companion object {
        val DEFAULT = ElevatorUiState()
    }
}

// 表格数据的示例结构 (可以根据需要调整)
data class EstimatedBlocksRowData(
    val header: String, // "40%", "块1" 等
    val actualLoadKg: Int,
    val blockCount: String, // 显示 "x块"
    val actualPercentage: String // 显示 "x.y%"
)

// UnitState 到 ElevatorUiState 的转换函数 (使用新的 UnitState 类名)
fun UnitState.toUiState(
    defaultLoadPercentages: List<Int> = listOf(30, 40, 45, 50, 60) // 从ViewModel传入或定义常量
): ElevatorUiState {
    val ratedLoadValue = this.ratedLoad.toDoubleOrNull()
    val blockWeightValue = this.counterweightBlockWeight.toDoubleOrNull()
    val isBaseInputValid = ratedLoadValue != null && blockWeightValue != null && ratedLoadValue > 0 && blockWeightValue > 0

    // 计算预计块数表格数据
    val tableData = if (isBaseInputValid) {
        if (this.useCustomBlockInput) {
            this.customBlockCounts.mapIndexedNotNull { index, blockCountStr ->
                val blocks = blockCountStr.toIntOrNull()
                if (blocks != null) {
                    val actualLoad = calculateActualLoad(blocks, blockWeightValue)
                    val actualPercentage = calculateActualPercentage(actualLoad, ratedLoadValue)
                    EstimatedBlocksRowData(
                        header = "块${index + 1}",
                        actualLoadKg = actualLoad.roundToInt(),
                        blockCount = "$blocks", // 只显示数字，单位在表头
                        actualPercentage = "%.1f%%".format(actualPercentage)
                    )
                } else if (blockCountStr.isNotEmpty() || index == 0) { // 显示空的或第一个，保持列对齐
                    EstimatedBlocksRowData(header = "块${index + 1}", 0, "", "")
                }
                else null // 如果为空且不是第一个，则可能不显示列
            }
        } else {
            defaultLoadPercentages.map { percentage ->
                val theoreticalLoad = calculateTheoreticalLoad(ratedLoadValue, percentage)
                val numberOfBlocks = calculateNumberOfBlocks(theoreticalLoad, blockWeightValue)
                val actualLoad = calculateActualLoad(numberOfBlocks, blockWeightValue)
                val actualPercentage = calculateActualPercentage(actualLoad, ratedLoadValue)
                EstimatedBlocksRowData(
                    header = "$percentage%",
                    actualLoadKg = actualLoad.roundToInt(),
                    blockCount = "$numberOfBlocks",
                    actualPercentage = "%.1f%%".format(actualPercentage)
                )
            }
        }
    } else {
        emptyList()
    }


    return ElevatorUiState(
        ratedLoad = this.ratedLoad,
        counterweightWeight = this.counterweightWeight,
        counterweightBlockWeight = this.counterweightBlockWeight,
        manualBalanceCoefficientK = this.manualBalanceCoefficientK,
        useManualBalance = this.useManualBalance,
        useCustomBlockInput = this.useCustomBlockInput,
        customBlockCounts = this.customBlockCounts.toList(), // 转为不可变 List
        customBlockPercentages = this.customBlockPercentages.toList(), // 转为不可变 List
        currentReadings = this.currentReadings.map { it.toList() }, // 转为不可变 List<List>
        balanceCoefficientK = this.balanceCoefficientK,
        balanceCoefficient = this.balanceCoefficient,
        recommendedBlocksMessage = this.recommendedBlocksMessage,
        hasActualIntersection = this.hasActualIntersection,
        linearRegressionR2 = this.linearRegressionR2,
        upwardCurrentPoints = this.upwardCurrentPoints.toList(), // 转为不可变 List
        downwardCurrentPoints = this.downwardCurrentPoints.toList(), // 转为不可变 List
        isCalculationPossible = isBaseInputValid,
        estimatedBlocksTableData = tableData
    )
}

// 这些函数是无状态的，接收所需参数

fun calculateTheoreticalLoad(ratedLoadValue: Double, percentage: Number): Double {
    return ratedLoadValue * percentage.toDouble() / 100
}

fun calculateNumberOfBlocks(theoreticalLoad: Double, blockWeightValue: Double): Int {
    return if (blockWeightValue > 0) (theoreticalLoad / blockWeightValue).roundToInt() else 0
}

fun calculateActualLoad(numberOfBlocks: Int, blockWeightValue: Double): Double {
    return numberOfBlocks * blockWeightValue
}

fun calculateActualPercentage(actualLoad: Double, ratedLoadValue: Double): Double {
    return if (ratedLoadValue > 0) (actualLoad / ratedLoadValue) * 100 else 0.0
}
