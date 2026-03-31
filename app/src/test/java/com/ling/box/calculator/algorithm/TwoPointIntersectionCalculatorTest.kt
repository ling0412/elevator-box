package com.ling.box.calculator.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TwoPointIntersectionCalculatorTest {

    private val calculator = TwoPointIntersectionCalculator()

    @Test
    fun insufficientPoints_returnsNull() {
        val result = calculator.calculate(
            upwardPoints = listOf(0.0 to 10f),
            downwardPoints = listOf(0.0 to 20f)
        )
        assertNull(result.first)
    }

    @Test
    fun emptyInput_returnsNull() {
        val result = calculator.calculate(emptyList(), emptyList())
        assertNull(result.first)
    }

    @Test
    fun clearIntersection_findsK() {
        // 上行：(0%, 20A), (100%, 30A)  => y = 0.1x + 20
        // 下行：(0%, 30A), (100%, 20A)  => y = -0.1x + 30
        // 交点：0.1x + 20 = -0.1x + 30  =>  0.2x = 10  =>  x = 50%
        val up = listOf(0.0 to 20f, 100.0 to 30f)
        val down = listOf(0.0 to 30f, 100.0 to 20f)

        val (k, isActual, _) = calculator.calculate(up, down)
        assertNotNull(k)
        assertTrue("K should be near 50%", abs(k!! - 50.0) < 1.0)
        assertTrue("Should be an actual intersection", isActual)
    }

    @Test
    fun intersectionOutsideDataRange() {
        // 上行比下行始终小，但趋势收敛 → 外推
        val up = listOf(0.0 to 5f, 50.0 to 12f)
        val down = listOf(0.0 to 20f, 50.0 to 15f)

        val (k, isActual, _) = calculator.calculate(up, down)
        assertNotNull("Should produce an extrapolated K", k)
    }

    @Test
    fun multipleDataPoints_findsK() {
        // 多组数据点，上行电流从小到大，下行电流从大到小，应有实际交点
        val up = listOf(0.0 to 10f, 25.0 to 15f, 50.0 to 20f, 75.0 to 25f, 100.0 to 30f)
        val down = listOf(0.0 to 30f, 25.0 to 26f, 50.0 to 22f, 75.0 to 18f, 100.0 to 14f)

        val (k, isActual, _) = calculator.calculate(up, down)
        assertNotNull(k)
        assertTrue("K should be in 0-100 range", k!! in 0.0..100.0)
    }

    @Test
    fun identicalCurrents_atPoint() {
        // 在 50% 处上行=下行
        val up = listOf(0.0 to 10f, 50.0 to 20f, 100.0 to 30f)
        val down = listOf(0.0 to 30f, 50.0 to 20f, 100.0 to 10f)

        val (k, _, _) = calculator.calculate(up, down)
        assertNotNull(k)
        assertTrue("K should be near 50%", abs(k!! - 50.0) < 1.0)
    }
}
