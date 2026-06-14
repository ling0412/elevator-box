package com.ling.box.calculator.model

// 定义单台电梯的所有内部状态数据（不可变版本）
data class UnitState(
    val name: String = "", // 电梯名称
    val creationDate: String,

    // 基础输入
    val ratedLoad: String = "1050",
    val counterweightWeight: String = "",  // 对重
    val counterweightBlockWeight: String = "25",
    val manualBalanceCoefficientK: String? = null,
    val useManualBalance: Boolean = false,
    val useCustomBlockInput: Boolean = true, // 电梯默认自定义模式

    // 列表输入（使用不可变 List）
    val customBlockCounts: List<String> = emptyList(),
    val customBlockPercentages: List<Double?> = emptyList(),
    val currentReadings: List<List<String>> = listOf(emptyList(), emptyList()), // [上行, 下行]

    // 计算结果
    val balanceCoefficientK: Double? = null, // 电流法计算结果 K
    val balanceCoefficient: Double? = null,
    val recommendedBlocksMessage: String? = null,
    val hasActualIntersection: Boolean = false,
    val linearRegressionR2: Double? = null, // 线性拟合算法的R²值（决定系数）

    // 图表数据点
    val upwardCurrentPoints: List<Pair<Double, Float>> = emptyList(),
    val downwardCurrentPoints: List<Pair<Double, Float>> = emptyList()
) {
    // 便捷更新方法
    
    fun withUpdatedName(newName: String): UnitState = copy(name = newName.trim())
    
    fun withUpdatedRatedLoad(value: String): UnitState = copy(ratedLoad = value)
    
    fun withUpdatedCounterweightWeight(value: String): UnitState = copy(counterweightWeight = value)
    
    fun withUpdatedCounterweightBlockWeight(value: String): UnitState = copy(counterweightBlockWeight = value)
    
    fun withUpdatedManualBalanceCoefficientK(value: String?): UnitState = copy(manualBalanceCoefficientK = value)
    
    fun withUpdatedUseManualBalance(isChecked: Boolean): UnitState = copy(useManualBalance = isChecked)
    
    fun withUpdatedUseCustomBlockInput(isChecked: Boolean): UnitState = copy(useCustomBlockInput = isChecked)
    
    fun withUpdatedCustomBlockCount(index: Int, value: String): UnitState {
        if (index !in customBlockCounts.indices) return this
        return copy(
            customBlockCounts = customBlockCounts.toMutableList().apply { this[index] = value }
        )
    }
    
    fun withAddedCustomBlockCountSlot(): UnitState {
        return copy(
            customBlockCounts = customBlockCounts + "",
            customBlockPercentages = customBlockPercentages + null,
            currentReadings = currentReadings.map { it + "" }
        )
    }
    
    fun withRemovedCustomBlockCountSlot(index: Int): UnitState {
        if (customBlockCounts.size <= 1 || index !in customBlockCounts.indices) return this
        return copy(
            customBlockCounts = customBlockCounts.toMutableList().apply { removeAt(index) },
            customBlockPercentages = customBlockPercentages.toMutableList().apply { 
                if (index < size) removeAt(index) 
            },
            currentReadings = currentReadings.map { readings ->
                readings.toMutableList().apply { 
                    if (index < size) removeAt(index) 
                }
            }
        )
    }
    
    fun withUpdatedCurrentReading(directionIndex: Int, pointIndex: Int, value: String): UnitState {
        if (directionIndex !in currentReadings.indices) return this
        if (pointIndex !in currentReadings[directionIndex].indices) return this
        
        return copy(
            currentReadings = currentReadings.mapIndexed { dir, readings ->
                if (dir == directionIndex) {
                    readings.toMutableList().apply { this[pointIndex] = value }
                } else {
                    readings
                }
            }
        )
    }
    
    fun withUpdatedBalanceCoefficientK(k: Double?): UnitState = copy(balanceCoefficientK = k)
    
    fun withUpdatedBalanceCoefficient(value: Double?): UnitState = copy(balanceCoefficient = value)
    
    fun withUpdatedRecommendedBlocksMessage(message: String?): UnitState = copy(recommendedBlocksMessage = message)
    
    fun withUpdatedHasActualIntersection(value: Boolean): UnitState = copy(hasActualIntersection = value)
    
    fun withUpdatedLinearRegressionR2(value: Double?): UnitState = copy(linearRegressionR2 = value)
    
    fun withUpdatedUpwardCurrentPoints(points: List<Pair<Double, Float>>): UnitState = 
        copy(upwardCurrentPoints = points)
    
    fun withUpdatedDownwardCurrentPoints(points: List<Pair<Double, Float>>): UnitState = 
        copy(downwardCurrentPoints = points)
    
    // 重置方法，返回新的不可变实例
    fun resetCurrentInputData(): UnitState {
        return if (useCustomBlockInput) {
            // 清空自定义块数据，添加一个初始行
            copy(
                manualBalanceCoefficientK = null,
                customBlockCounts = listOf(""),
                customBlockPercentages = listOf(null),
                currentReadings = listOf(listOf(""), listOf("")),
                balanceCoefficientK = null,
                balanceCoefficient = null,
                recommendedBlocksMessage = null,
                hasActualIntersection = false,
                linearRegressionR2 = null,
                upwardCurrentPoints = emptyList(),
                downwardCurrentPoints = emptyList()
            )
        } else {
            // 手动模式，只清空 K 值
            copy(
                manualBalanceCoefficientK = null,
                balanceCoefficientK = null,
                balanceCoefficient = null,
                recommendedBlocksMessage = null,
                hasActualIntersection = false,
                linearRegressionR2 = null,
                upwardCurrentPoints = emptyList(),
                downwardCurrentPoints = emptyList()
            )
        }
    }
    
    fun addInitialBlockSlot(): UnitState {
        return copy(
            customBlockCounts = customBlockCounts + "",
            customBlockPercentages = customBlockPercentages + null,
            currentReadings = currentReadings.map { it + "" }
        )
    }
}