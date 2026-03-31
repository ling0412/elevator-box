package com.ling.box.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionMigrationHelperTest {

    @Test
    fun parseVersionCode_standard() {
        assertEquals(10500, VersionMigrationHelper.parseVersionCode("1.5.0"))
        assertEquals(10400, VersionMigrationHelper.parseVersionCode("1.4.0"))
        assertEquals(10501, VersionMigrationHelper.parseVersionCode("1.5.1"))
    }

    @Test
    fun parseVersionCode_twoDigitMinor() {
        assertEquals(11002, VersionMigrationHelper.parseVersionCode("1.10.2"))
        assertEquals(11200, VersionMigrationHelper.parseVersionCode("1.12.0"))
    }

    @Test
    fun parseVersionCode_noCollision() {
        val v1_1_0 = VersionMigrationHelper.parseVersionCode("1.1.0")
        val v1_10_0 = VersionMigrationHelper.parseVersionCode("1.10.0")
        assertTrue("1.10.0 should be greater than 1.1.0", v1_10_0 > v1_1_0)
    }

    @Test
    fun parseVersionCode_majorVersion() {
        assertEquals(20000, VersionMigrationHelper.parseVersionCode("2.0.0"))
        assertEquals(30102, VersionMigrationHelper.parseVersionCode("3.1.2"))
    }

    @Test
    fun parseVersionCode_noPatch() {
        assertEquals(10500, VersionMigrationHelper.parseVersionCode("1.5"))
    }

    @Test
    fun parseVersionCode_invalidInput_returnsZero() {
        assertEquals(0, VersionMigrationHelper.parseVersionCode("abc"))
        assertEquals(0, VersionMigrationHelper.parseVersionCode(""))
        assertEquals(0, VersionMigrationHelper.parseVersionCode("1"))
    }

    @Test
    fun parseVersionCode_ordering() {
        val versions = listOf("1.4.0", "1.4.9", "1.5.0", "1.5.1", "1.10.0", "2.0.0")
        val codes = versions.map { VersionMigrationHelper.parseVersionCode(it) }
        assertEquals(codes, codes.sorted())
    }
}
