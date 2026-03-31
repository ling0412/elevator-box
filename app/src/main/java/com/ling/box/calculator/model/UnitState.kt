package com.ling.box.calculator.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

// 定义单台电梯的所有内部状态数据
data class UnitState(
    var name: String = "", // 电梯名称
    val creationDate: String,

    // 基础输入
    var ratedLoad: String = "1050",
    var counterweightWeight: String = "",  // 对重
    var counterweightBlockWeight: String = "25",
    var manualBalanceCoefficientK: String? = null,
    var useManualBalance: Boolean = false,
    var useCustomBlockInput: Boolean = true, // 电梯默认自定义模式

    // 列表输入
    val customBlockCounts: SnapshotStateList<String> = mutableStateListOf(), // 初始化为空列表
    val customBlockPercentages: SnapshotStateList<Double?> = mutableStateListOf(), // 初始化为空列表
    val currentReadings: SnapshotStateList<SnapshotStateList<String>> = mutableStateListOf(
        mutableStateListOf(), // 上行 [0]
        mutableStateListOf()  // 下行 [1]
    ),

    // 计算结果
    var balanceCoefficientK: Double? = null, // 电流法计算结果 K
    var balanceCoefficient: Double? = null,
    var recommendedBlocksMessage: String? = null,
    var hasActualIntersection: Boolean = false,
    var linearRegressionR2: Double? = null, // 线性拟合算法的R²值（决定系数）

    // 图表数据点
    val upwardCurrentPoints: SnapshotStateList<Pair<Double, Float>> = mutableStateListOf(),
    val downwardCurrentPoints: SnapshotStateList<Pair<Double, Float>> = mutableStateListOf()
) {
    // 重置方法，现在只重置输入数据，保持基本信息不变
    fun resetCurrentInputData() {
        manualBalanceCoefficientK = null
        // 模式保持不变，只清空与模式相关的数据
        if (useCustomBlockInput) {
            customBlockCounts.clear()
            customBlockPercentages.clear()
            currentReadings.forEach { list -> list.clear() }
            addInitialBlockSlot() // 清空后添加一个初始行
        } else { // 如果是手动模式，清空 K 值
            manualBalanceCoefficientK = null
        }

        balanceCoefficientK = null
        balanceCoefficient = null
        recommendedBlocksMessage = null
        hasActualIntersection = false
        linearRegressionR2 = null
        upwardCurrentPoints.clear()
        downwardCurrentPoints.clear()
    }

    fun addInitialBlockSlot() {
        customBlockCounts.add("")
        customBlockPercentages.add(null)
        // 确保 currentReadings 也有对应的空槽位
        if (currentReadings.size < 2) {
            // 如果还不足2个，先添加方向列表
            while (currentReadings.size < 2) {
                currentReadings.add(mutableStateListOf())
            }
        }
        currentReadings.forEach { it.add("") }
    }

    /**
     * 创建真正独立的深拷贝，所有 SnapshotStateList 都会创建新实例。
     * data class 的 copy() 只做浅拷贝，列表字段会共享引用，
     * 导致修改副本时原对象也被同步修改，破坏原子更新语义。
     */
    fun deepCopy(): UnitState = UnitState(
        name = name,
        creationDate = creationDate,
        ratedLoad = ratedLoad,
        counterweightWeight = counterweightWeight,
        counterweightBlockWeight = counterweightBlockWeight,
        manualBalanceCoefficientK = manualBalanceCoefficientK,
        useManualBalance = useManualBalance,
        useCustomBlockInput = useCustomBlockInput,
        customBlockCounts = mutableStateListOf<String>().also { it.addAll(customBlockCounts) },
        customBlockPercentages = mutableStateListOf<Double?>().also { it.addAll(customBlockPercentages) },
        currentReadings = mutableStateListOf<SnapshotStateList<String>>().also { outer ->
            currentReadings.forEach { inner ->
                outer.add(mutableStateListOf<String>().also { it.addAll(inner) })
            }
        },
        balanceCoefficientK = balanceCoefficientK,
        balanceCoefficient = balanceCoefficient,
        recommendedBlocksMessage = recommendedBlocksMessage,
        hasActualIntersection = hasActualIntersection,
        linearRegressionR2 = linearRegressionR2,
        upwardCurrentPoints = mutableStateListOf<Pair<Double, Float>>().also { it.addAll(upwardCurrentPoints) },
        downwardCurrentPoints = mutableStateListOf<Pair<Double, Float>>().also { it.addAll(downwardCurrentPoints) },
    )
}