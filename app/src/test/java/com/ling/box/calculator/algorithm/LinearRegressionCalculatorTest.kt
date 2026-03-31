package com.ling.box.calculator.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LinearRegressionCalculatorTest {

    private val calculator = LinearRegressionCalculator()

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
    fun perfectLinearData_findsExactIntersection() {
        // 上行: y = 0.1x + 20   下行: y = -0.1x + 30   交点 x = 50
        val up = listOf(0.0 to 20f, 50.0 to 25f, 100.0 to 30f)
        val down = listOf(0.0 to 30f, 50.0 to 25f, 100.0 to 20f)

        val (k, isActual, r2) = calculator.calculate(up, down)
        assertNotNull(k)
        assertTrue("K should be near 50%", abs(k!! - 50.0) < 2.0)
    }

    @Test
    fun allUpwardLess_extrapolates() {
        val up = listOf(0.0 to 5f, 50.0 to 10f)
        val down = listOf(0.0 to 20f, 50.0 to 15f)

        val (k, _, _) = calculator.calculate(up, down)
        assertNotNull("Should produce an extrapolated K", k)
    }

    @Test
    fun symmetricData_returnsK50() {
        val up = listOf(
            0.0 to 15f, 25.0 to 18f, 50.0 to 21f, 75.0 to 24f, 100.0 to 27f
        )
        val down = listOf(
            0.0 to 27f, 25.0 to 24f, 50.0 to 21f, 75.0 to 18f, 100.0 to 15f
        )

        val (k, _, _) = calculator.calculate(up, down)
        assertNotNull(k)
        assertTrue("Symmetric data should yield K near 50%", abs(k!! - 50.0) < 2.0)
    }

    @Test
    fun r2_providedForLinearFit() {
        // 避免在某个数据点上行=下行（会触发提前返回），使用不完全对称的数据
        val up = listOf(0.0 to 20f, 33.0 to 23.3f, 66.0 to 26.6f, 100.0 to 30f)
        val down = listOf(0.0 to 30f, 33.0 to 26.7f, 66.0 to 23.4f, 100.0 to 20f)

        val (k, _, r2) = calculator.calculate(up, down)
        assertNotNull("K should be calculated", k)
        assertNotNull("R² should be provided", r2)
        assertTrue("R² for near-perfect linear data should be high", r2!! > 0.9)
    }

    @Test
    fun unmatchedLoadPercentages_returnsNull() {
        val up = listOf(0.0 to 10f, 50.0 to 20f)
        val down = listOf(10.0 to 30f, 60.0 to 25f)

        val result = calculator.calculate(up, down)
        assertNull("Unmatched load percentages should return null", result.first)
    }
}
