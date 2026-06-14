package com.ling.box.calculator.service

import com.ling.box.calculator.model.UnitState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ElevatorCalculationServiceTest {

    @Test
    fun updateCustomBlockPercentages_disabledMode_returnsOriginal() {
        val data = UnitState(
            name = "Test",
            creationDate = "2024-01-01",
            useCustomBlockInput = false,
            customBlockCounts = listOf("10", "20"),
            customBlockPercentages = listOf(null, null)
        )

        val result = ElevatorCalculationService.updateCustomBlockPercentages(data)
        assertEquals("Should return original data when custom block input is disabled", data, result)
    }

    @Test
    fun updateCustomBlockPercentages_validInputs_calculatesPercentages() {
        val data = UnitState(
            name = "Test",
            creationDate = "2024-01-01",
            useCustomBlockInput = true,
            ratedLoad = "1000",
            counterweightBlockWeight = "50",
            customBlockCounts = listOf("10", "20"),
            customBlockPercentages = listOf(null, null)
        )

        val result = ElevatorCalculationService.updateCustomBlockPercentages(data)

        assertNotNull(result.customBlockPercentages)
        assertEquals(2, result.customBlockPercentages.size)

        // 10 blocks * 50 / 1000 = 50%
        assertEquals(50.0, result.customBlockPercentages[0]!!, 0.01)
        // 20 blocks * 50 / 1000 = 100%
        assertEquals(100.0, result.customBlockPercentages[1]!!, 0.01)
    }

    @Test
    fun updateCustomBlockPercentages_invalidRatedLoad_returnsNulls() {
        val data = UnitState(
            name = "Test",
            creationDate = "2024-01-01",
            useCustomBlockInput = true,
            ratedLoad = "invalid",
            counterweightBlockWeight = "50",
            customBlockCounts = listOf("10"),
            customBlockPercentages = listOf(null)
        )

        val result = ElevatorCalculationService.updateCustomBlockPercentages(data)
        
        assertEquals(1, result.customBlockPercentages.size)
        assertNull(result.customBlockPercentages[0])
    }

    @Test
    fun updateCustomBlockPercentages_invalidBlockWeight_returnsNulls() {
        val data = UnitState(
            name = "Test",
            creationDate = "2024-01-01",
            useCustomBlockInput = true,
            ratedLoad = "1000",
            counterweightBlockWeight = "invalid",
            customBlockCounts = listOf("10"),
            customBlockPercentages = listOf(null)
        )

        val result = ElevatorCalculationService.updateCustomBlockPercentages(data)
        
        assertEquals(1, result.customBlockPercentages.size)
        assertNull(result.customBlockPercentages[0])
    }

    @Test
    fun updateCustomBlockPercentages_zeroRatedLoad_returnsNulls() {
        val data = UnitState(
            name = "Test",
            creationDate = "2024-01-01",
            useCustomBlockInput = true,
            ratedLoad = "0",
            counterweightBlockWeight = "50",
            customBlockCounts = listOf("10"),
            customBlockPercentages = listOf(null)
        )

        val result = ElevatorCalculationService.updateCustomBlockPercentages(data)
        
        assertEquals(1, result.customBlockPercentages.size)
        assertNull(result.customBlockPercentages[0])
    }

    @Test
    fun updateCustomBlockPercentages_invalidBlockCount_returnsNull() {
        val data = UnitState(
            name = "Test",
            creationDate = "2024-01-01",
            useCustomBlockInput = true,
            ratedLoad = "1000",
            counterweightBlockWeight = "50",
            customBlockCounts = listOf("invalid"),
            customBlockPercentages = listOf(null)
        )

        val result = ElevatorCalculationService.updateCustomBlockPercentages(data)
        
        assertEquals(1, result.customBlockPercentages.size)
        assertNull(result.customBlockPercentages[0])
    }

    @Test
    fun updateCurrentPoints_validInputs_generatesPoints() {
        val data = UnitState(
            name = "Test",
            creationDate = "2024-01-01",
            ratedLoad = "1000",
            counterweightBlockWeight = "50",
            customBlockCounts = listOf("10", "20"),
            currentReadings = listOf(
                listOf("15", "25"), // upward currents
                listOf("20", "18")  // downward currents
            ),
            upwardCurrentPoints = emptyList(),
            downwardCurrentPoints = emptyList()
        )

        val result = ElevatorCalculationService.updateCurrentPoints(data)
        
        assertEquals(2, result.upwardCurrentPoints.size)
        assertEquals(2, result.downwardCurrentPoints.size)
        
        // 10 blocks * 50 / 1000 = 50%
        assertEquals(50.0, result.upwardCurrentPoints[0].first, 0.01)
        assertEquals(15f, result.upwardCurrentPoints[0].second, 0.01f)
        assertEquals(50.0, result.downwardCurrentPoints[0].first, 0.01)
        assertEquals(20f, result.downwardCurrentPoints[0].second, 0.01f)
        
        // 20 blocks * 50 / 1000 = 100%
        assertEquals(100.0, result.upwardCurrentPoints[1].first, 0.01)
        assertEquals(25f, result.upwardCurrentPoints[1].second, 0.01f)
        assertEquals(100.0, result.downwardCurrentPoints[1].first, 0.01)
        assertEquals(18f, result.downwardCurrentPoints[1].second, 0.01f)
    }

    @Test
    fun updateCurrentPoints_invalidRatedLoad_returnsEmptyLists() {
        val data = UnitState(
            name = "Test",
            creationDate = "2024-01-01",
            ratedLoad = "invalid",
            counterweightBlockWeight = "50",
            customBlockCounts = listOf("10"),
            currentReadings = listOf(listOf("15"), listOf("20")),
            upwardCurrentPoints = emptyList(),
            downwardCurrentPoints = emptyList()
        )

        val result = ElevatorCalculationService.updateCurrentPoints(data)
        
        assertTrue(result.upwardCurrentPoints.isEmpty())
        assertTrue(result.downwardCurrentPoints.isEmpty())
    }

    @Test
    fun updateCurrentPoints_invalidCurrentReadings_skipsInvalidPoints() {
        val data = UnitState(
            name = "Test",
            creationDate = "2024-01-01",
            ratedLoad = "1000",
            counterweightBlockWeight = "50",
            customBlockCounts = listOf("10", "20"),
            currentReadings = listOf(
                listOf("15", "invalid"), // upward currents
                listOf("20", "18")  // downward currents
            ),
            upwardCurrentPoints = emptyList(),
            downwardCurrentPoints = emptyList()
        )

        val result = ElevatorCalculationService.updateCurrentPoints(data)
        
        // Only first point should be valid
        assertEquals(1, result.upwardCurrentPoints.size)
        assertEquals(1, result.downwardCurrentPoints.size)
    }

    @Test
    fun updateCurrentPoints_sortsPointsByPercentage() {
        val data = UnitState(
            name = "Test",
            creationDate = "2024-01-01",
            ratedLoad = "1000",
            counterweightBlockWeight = "50",
            customBlockCounts = listOf("20", "10"), // reverse order
            currentReadings = listOf(
                listOf("25", "15"),
                listOf("18", "20")
            ),
            upwardCurrentPoints = emptyList(),
            downwardCurrentPoints = emptyList()
        )

        val result = ElevatorCalculationService.updateCurrentPoints(data)
        
        // Should be sorted by percentage
        assertEquals(50.0, result.upwardCurrentPoints[0].first, 0.01)
        assertEquals(100.0, result.upwardCurrentPoints[1].first, 0.01)
    }
}
