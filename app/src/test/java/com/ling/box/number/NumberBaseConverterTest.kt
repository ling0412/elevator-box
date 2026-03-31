package com.ling.box.number

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberBaseConverterTest {

    // --- detectInputBase ---

    @Test
    fun detectInputBase_empty_returnsUnknown() {
        val (label, base) = detectInputBase("", InputMode.ALPHANUMERIC)
        assertEquals("未知", label)
        assertNull(base)
    }

    @Test
    fun detectInputBase_binaryDigits() {
        val (label, base) = detectInputBase("1010", InputMode.ALPHANUMERIC)
        assertEquals("二进制", label)
        assertEquals(2, base)
    }

    @Test
    fun detectInputBase_octalDigits() {
        val (label, base) = detectInputBase("347", InputMode.ALPHANUMERIC)
        assertEquals("八进制", label)
        assertEquals(8, base)
    }

    @Test
    fun detectInputBase_decimalDigits() {
        val (label, base) = detectInputBase("981", InputMode.ALPHANUMERIC)
        assertEquals("十进制", label)
        assertEquals(10, base)
    }

    @Test
    fun detectInputBase_hexDigits() {
        val (label, base) = detectInputBase("1A3F", InputMode.ALPHANUMERIC)
        assertEquals("十六进制", label)
        assertEquals(16, base)
    }

    @Test
    fun detectInputBase_numericMode_forcesDecimal() {
        val (label, base) = detectInputBase("347", InputMode.NUMERIC)
        assertEquals("十进制", label)
        assertEquals(10, base)
    }

    // --- calculateAllBases ---

    @Test
    fun calculateAllBases_decimal10() {
        val result = calculateAllBases("10", 10)
        assertEquals("1010", result["二进制"])
        assertEquals("12", result["八进制"])
        assertEquals("10", result["十进制"])
        assertEquals("A", result["十六进制"])
    }

    @Test
    fun calculateAllBases_binary1111() {
        val result = calculateAllBases("1111", 2)
        assertEquals("1111", result["二进制"])
        assertEquals("17", result["八进制"])
        assertEquals("15", result["十进制"])
        assertEquals("F", result["十六进制"])
    }

    @Test
    fun calculateAllBases_hexFF() {
        val result = calculateAllBases("FF", 16)
        assertEquals("11111111", result["二进制"])
        assertEquals("377", result["八进制"])
        assertEquals("255", result["十进制"])
        assertEquals("FF", result["十六进制"])
    }

    @Test
    fun calculateAllBases_zero() {
        val result = calculateAllBases("0", 10)
        assertEquals("0", result["二进制"])
        assertEquals("0", result["八进制"])
        assertEquals("0", result["十进制"])
        assertEquals("0", result["十六进制"])
    }

    @Test
    fun calculateAllBases_invalidInput_returnsEmpty() {
        val result = calculateAllBases("GGG", 16)
        assertEquals(emptyMap<String, String>(), result)
    }

    @Test
    fun calculateAllBases_largeNumber() {
        val result = calculateAllBases("1000000", 10)
        assertEquals("1000000", result["十进制"])
        assertEquals("F4240", result["十六进制"])
    }
}
