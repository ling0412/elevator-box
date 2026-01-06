package com.ling.box.calculator.calculator

import kotlin.math.abs
import kotlin.math.round

/**
 * 推荐块数计算器
 * 根据当前平衡系数计算推荐的块数调整方案
 */
object RecommendedBlocksCalculator {
    
    /**
     * 计算推荐块数
     * @param currentBalanceCoefficient 当前平衡系数 K
     * @param ratedLoadVal 额定载重
     * @param singleBlockWeightVal 单个砝码的重量
     * @param targetKMin 目标 K 值最小值（默认 45.0）
     * @param targetKMax 目标 K 值最大值（默认 50.0）
     * @param idealK 理想 K 值（默认 47.5）
     * @return 推荐方案的字符串描述
     */
    fun calculate(
        currentBalanceCoefficient: Double?,
        ratedLoadVal: Double?,
        singleBlockWeightVal: Double?,
        targetKMin: Double = 45.0,
        targetKMax: Double = 50.0,
        idealK: Double = 47.5
    ): String? {
        // 输入校验
        if (ratedLoadVal == null || singleBlockWeightVal == null || currentBalanceCoefficient == null ||
            ratedLoadVal <= 0 || singleBlockWeightVal <= 0) {
            return "无法计算推荐 (请检查额定载重、单个砝码重量和平衡系数)"
        }

        return buildString {
            val currentK = currentBalanceCoefficient

            // 计算单个砝码能带来的 K 值变化量
            val kChangePerBlock = (singleBlockWeightVal / ratedLoadVal) * 100.0

            // 如果单个砝码的影响非常小或为零，则无法有效调整
            if (kChangePerBlock == 0.0) {
                append("当前状态: ${"%.1f".format(currentK)}%")
                if (currentK > -30.0 && currentK < 50.0) {
                    append(" (✓ 合理范围)")
                }
                append("\n★ 推荐方案: 无法通过增减砝码精确调整 (单个砝码重量相对于额定载重过小或为零)")
                return@buildString
            }

            // 使用传入的目标 K 值范围和最佳 K 值（已在参数中定义）

            val feasibleOptions = mutableListOf<Triple<Int, Double, Double>>() // (delta, theoreticalBlocks, newK)
            var bestDelta: Int? = null
            var minKDiff = Double.MAX_VALUE

            // 计算当前 K 值对应的理论总对重总重量
            val currentTheoreticalTotalWeight = currentK * ratedLoadVal / 100.0

            // 遍历可能的调整块数
            val searchRange = -100..100
            searchRange.forEach { delta ->
                val newTheoreticalTotalWeight = currentTheoreticalTotalWeight + (delta * singleBlockWeightVal)

                // 物理检查：对重总重量必须大于等于 0
                if (newTheoreticalTotalWeight < 0) return@forEach

                // 计算新的理论 K 值
                val newK = (newTheoreticalTotalWeight / ratedLoadVal) * 100.0

                // 检查是否在有效 K 值范围内
                if (newK >= -50.0 && newK <= 200.0) {
                    // 如果在新 K 值在可行范围内 (45%-50%)，则加入可行方案
                    if (newK >= targetKMin && newK <= targetKMax) {
                        val theoreticalBlocks = newTheoreticalTotalWeight / singleBlockWeightVal
                        feasibleOptions.add(Triple(delta, theoreticalBlocks, newK))
                    }

                    // 寻找最接近理想 K 值 (47.5%) 的方案
                    val currentKDiff = abs(newK - idealK)
                    if (currentKDiff < minKDiff) {
                        minKDiff = currentKDiff
                        bestDelta = delta
                    }
                }
            }

            // 构建"可行方案"部分
            if (feasibleOptions.isNotEmpty()) {
                append("可行方案:\n")
                feasibleOptions.sortedBy { it.first }.forEach { (delta, _, newK) ->
                    val action = when {
                        delta > 0 -> "加${delta}块"
                        delta < 0 -> "减${-delta}块"
                        else -> "保持现状"
                    }
                    append("$action → ${"%.1f".format(newK)}%\n")
                }
            } else {
                append("无方案可达理想范围 (${"%.1f".format(targetKMin)}-${"%.1f".format(targetKMax)}%)\n")
            }

            // 构建"当前状态"部分
            append("\n当前状态: ${"%.1f".format(currentK)}%")
            if (currentK >= targetKMin && currentK <= targetKMax) {
                append(" (✓ 理想范围)")
            } else if (currentK >= -50.0 && currentK <= 200.0) {
                val diff = idealK - currentK
                val blocksNeededRoughly = if (kChangePerBlock != 0.0) diff / kChangePerBlock else 0.0
                when {
                    blocksNeededRoughly > 0 -> append(" (建议加约 ${round(blocksNeededRoughly).toInt()} 块)")
                    blocksNeededRoughly < 0 -> append(" (建议减约 ${round(abs(blocksNeededRoughly)).toInt()} 块)")
                    else -> append(" (请检查数据)")
                }
            } else {
                append(" (❗ 系数超出范围 (-50% ~ 200%)")
            }

            // 构建"推荐方案"部分
            append("\n★ 推荐方案: ")
            bestDelta?.let { delta ->
                val newTheoreticalTotalWeight = currentTheoreticalTotalWeight + (delta * singleBlockWeightVal)
                val newK = (newTheoreticalTotalWeight / ratedLoadVal) * 100.0

                val action = when {
                    delta > 0 -> "加${delta} 块"
                    delta < 0 -> "减${-delta} 块"
                    else -> "保持现状"
                }
                append("$action → ${"%.1f".format(newK)}%")

                when {
                    newK in 46.5..48.5 -> append(" (推荐)")
                    newK >= targetKMin && newK <= targetKMax -> append(" (可接受)")
                    delta == 0 -> append(" (已在最佳范围附近)")
                    else -> append(" (最接近 ${"%.1f".format(idealK)}% 的方案)")
                }
            } ?: append("无法找到最佳方案")
        }
    }
}



