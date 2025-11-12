package com.ling.box.calculator.algorithm

/**
 * 平衡系数计算器接口
 * 用于计算电梯平衡系数 K 值
 */
interface BalanceCoefficientCalculator {
    /**
     * 计算平衡系数 K
     * @param upwardPoints 上行电流数据点列表 (载荷百分比, 电流值)
     * @param downwardPoints 下行电流数据点列表 (载荷百分比, 电流值)
     * @return Triple(K值, 是否有实际交点, R²值(如果适用))
     */
    fun calculate(
        upwardPoints: List<Pair<Double, Float>>,
        downwardPoints: List<Pair<Double, Float>>
    ): Triple<Double?, Boolean, Double?>
}



