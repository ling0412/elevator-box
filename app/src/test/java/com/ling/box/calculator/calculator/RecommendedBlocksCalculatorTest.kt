package com.ling.box.calculator.calculator

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendedBlocksCalculatorTest {

    @Test
    fun nullInputs_returnsErrorMessage() {
        val result = RecommendedBlocksCalculator.calculate(null, 1000.0, 10.0)
        assertNotNull(result)
        assertTrue("Should mention check inputs", result!!.contains("请检查"))
    }

    @Test
    fun zeroRatedLoad_returnsErrorMessage() {
        val result = RecommendedBlocksCalculator.calculate(50.0, 0.0, 10.0)
        assertNotNull(result)
        assertTrue(result!!.contains("请检查"))
    }

    @Test
    fun alreadyInIdealRange_noBlockChange() {
        val result = RecommendedBlocksCalculator.calculate(
            currentBalanceCoefficient = 47.5,
            ratedLoadVal = 1000.0,
            singleBlockWeightVal = 10.0
        )
        assertNotNull(result)
        assertTrue("Should indicate ideal range", result!!.contains("理想范围") || result.contains("保持现状"))
    }

    @Test
    fun belowIdealRange_suggestsAdding() {
        val result = RecommendedBlocksCalculator.calculate(
            currentBalanceCoefficient = 30.0,
            ratedLoadVal = 1000.0,
            singleBlockWeightVal = 10.0
        )
        assertNotNull(result)
        assertTrue("Should suggest adding blocks", result!!.contains("加"))
    }

    @Test
    fun aboveIdealRange_suggestsRemoving() {
        val result = RecommendedBlocksCalculator.calculate(
            currentBalanceCoefficient = 60.0,
            ratedLoadVal = 1000.0,
            singleBlockWeightVal = 10.0
        )
        assertNotNull(result)
        assertTrue("Should suggest removing blocks", result!!.contains("减"))
    }

    @Test
    fun feasibleOptions_listedInOutput() {
        val result = RecommendedBlocksCalculator.calculate(
            currentBalanceCoefficient = 44.0,
            ratedLoadVal = 1000.0,
            singleBlockWeightVal = 10.0
        )
        assertNotNull(result)
        assertTrue("Should list feasible options", result!!.contains("可行方案"))
    }

    @Test
    fun verySmallBlockWeight_handlesGracefully() {
        val result = RecommendedBlocksCalculator.calculate(
            currentBalanceCoefficient = 30.0,
            ratedLoadVal = 1000.0,
            singleBlockWeightVal = 0.001
        )
        assertNotNull(result)
    }
}
