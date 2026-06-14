package com.ling.box.calculator.service

import com.ling.box.calculator.model.UnitState
import com.ling.box.calculator.model.calculateActualPercentage

/**
 * 电梯计算服务
 * 负责状态更新和计算触发逻辑（适配不可变 UnitState）
 */
object ElevatorCalculationService {
    
    /**
     * 更新自定义块数百分比（返回新的 UnitState）
     */
    fun updateCustomBlockPercentages(data: UnitState): UnitState {
        if (!data.useCustomBlockInput) return data

        val ratedLoadValue = data.ratedLoad.toDoubleOrNull()
        val blockWeightValue = data.counterweightBlockWeight.toDoubleOrNull()

        if (ratedLoadValue != null && blockWeightValue != null && ratedLoadValue > 0 && blockWeightValue > 0) {
            val percentages = data.customBlockCounts.map { blockCount ->
                val blocks = blockCount.toIntOrNull()
                if (blocks != null) {
                    val actualLoad = blocks * blockWeightValue
                    calculateActualPercentage(actualLoad, ratedLoadValue)
                } else {
                    null
                }
            }
            return data.copy(customBlockPercentages = percentages)
        } else {
            return data.copy(customBlockPercentages = List(data.customBlockCounts.size) { null })
        }
    }
    
    /**
     * 更新当前数据点（返回新的 UnitState）
     */
    fun updateCurrentPoints(data: UnitState): UnitState {
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

        return data.copy(
            upwardCurrentPoints = tempUpwardPoints.toList(),
            downwardCurrentPoints = tempDownwardPoints.toList()
        )
    }
}

