package com.ling.box.calculator.service

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.ling.box.calculator.model.UnitState

/**
 * 电梯计算服务
 * 负责状态更新和计算触发逻辑
 */
object ElevatorCalculationService {
    
    /**
     * 更新自定义块数百分比
     */
    fun updateCustomBlockPercentages(data: UnitState) {
        if (!data.useCustomBlockInput) return

        val ratedLoadValue = data.ratedLoad.toDoubleOrNull()
        val blockWeightValue = data.counterweightBlockWeight.toDoubleOrNull()

        if (ratedLoadValue != null && blockWeightValue != null && ratedLoadValue > 0 && blockWeightValue > 0) {
            // 确保 customBlockPercentages 与 customBlockCounts 保持同步大小
            ensureListSize(data.customBlockPercentages, data.customBlockCounts.size, null)
            while(data.customBlockPercentages.size > data.customBlockCounts.size) {
                data.customBlockPercentages.removeAt(data.customBlockPercentages.lastIndex)
            }

            data.customBlockCounts.forEachIndexed { index, blockCount ->
                val blocks = blockCount.toIntOrNull()
                val percentage = if (blocks != null) {
                    val actualLoad = blocks * blockWeightValue
                    calculateActualPercentage(actualLoad, ratedLoadValue)
                } else {
                    null
                }
                if (index < data.customBlockPercentages.size) {
                    data.customBlockPercentages[index] = percentage
                }
            }
        } else {
            data.customBlockPercentages.fill(null)
        }
    }
    
    /**
     * 更新当前数据点
     */
    fun updateCurrentPoints(data: UnitState) {
        val ratedLoadValue = data.ratedLoad.toDoubleOrNull()
        val blockWeightValue = data.counterweightBlockWeight.toDoubleOrNull()

        val tempUpwardPoints = mutableListOf<Pair<Double, Float>>()
        val tempDownwardPoints = mutableListOf<Pair<Double, Float>>()

        if (ratedLoadValue != null && blockWeightValue != null && ratedLoadValue > 0 && blockWeightValue > 0) {
            // 根据实际的 customBlockCounts 来生成点
            val actualPercentages: List<Pair<Double, Int>> = data.customBlockCounts.mapIndexedNotNull { index, blockCount ->
                blockCount.toIntOrNull()?.let { blocks ->
                    Pair((blocks * blockWeightValue / ratedLoadValue) * 100.0, index)
                }
            }

            actualPercentages.forEach { (actualPercentage, index) ->
                val upward = data.currentReadings.getOrNull(0)?.getOrNull(index)?.toFloatOrNull()
                val downward = data.currentReadings.getOrNull(1)?.getOrNull(index)?.toFloatOrNull()
                if (upward != null && downward != null) {
                    tempUpwardPoints.add(actualPercentage to upward)
                    tempDownwardPoints.add(actualPercentage to downward)
                }
            }
            tempUpwardPoints.sortBy { it.first }
            tempDownwardPoints.sortBy { it.first }
        }

        // 直接更新 SnapshotStateList 的内容
        data.upwardCurrentPoints.clear()
        data.upwardCurrentPoints.addAll(tempUpwardPoints)
        data.downwardCurrentPoints.clear()
        data.downwardCurrentPoints.addAll(tempDownwardPoints)
    }
    
    /**
     * 计算实际百分比
     */
    private fun calculateActualPercentage(actualLoad: Double, ratedLoad: Double): Double {
        return (actualLoad / ratedLoad) * 100
    }
    
    /**
     * 辅助函数确保 SnapshotStateList 足够长
     */
    fun <T> ensureListSize(list: SnapshotStateList<T>, requiredSize: Int, defaultValue: T) {
        while (list.size < requiredSize) {
            list.add(defaultValue)
        }
    }
}

