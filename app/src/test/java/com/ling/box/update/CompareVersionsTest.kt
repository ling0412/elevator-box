package com.ling.box.update

import com.ling.box.update.utils.compareVersions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompareVersionsTest {

    @Test
    fun sameVersion_returnsZero() {
        assertEquals(0, compareVersions("1.5.0", "1.5.0"))
        assertEquals(0, compareVersions("2.0.0", "2.0.0"))
    }

    @Test
    fun newerVersion_returnsPositive() {
        assertTrue(compareVersions("1.5.1", "1.5.0") > 0)
        assertTrue(compareVersions("2.0.0", "1.9.9") > 0)
        assertTrue(compareVersions("1.10.0", "1.9.0") > 0)
    }

    @Test
    fun olderVersion_returnsNegative() {
        assertTrue(compareVersions("1.4.0", "1.5.0") < 0)
        assertTrue(compareVersions("1.5.0", "1.5.1") < 0)
    }

    @Test
    fun differentLengths_padsWithZero() {
        assertEquals(0, compareVersions("1.5", "1.5.0"))
        assertTrue(compareVersions("1.5.1", "1.5") > 0)
        assertTrue(compareVersions("1", "1.0.1") < 0)
    }

    @Test
    fun majorVersionDifference() {
        assertTrue(compareVersions("3.0.0", "2.9.9") > 0)
        assertTrue(compareVersions("1.0.0", "2.0.0") < 0)
    }
}
