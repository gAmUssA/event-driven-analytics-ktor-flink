package dev.gamov.flightdemo.flink.functions

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for the RegionCalculator scalar function.
 */
class RegionCalculatorTest {

    private val calculator = RegionCalculator()

    @Test
    fun `test with positive coordinates`() {
        val latitude = 37.7749
        val longitude = -122.4194
        val result = calculator.eval(latitude, longitude)
        assertEquals("35_-125", result)
    }

    @Test
    fun `test with negative coordinates`() {
        val latitude = -33.8688
        val longitude = 151.2093
        val result = calculator.eval(latitude, longitude)
        assertEquals("-35_150", result)
    }

    @Test
    fun `test with zero coordinates`() {
        val latitude = 0.0
        val longitude = 0.0
        val result = calculator.eval(latitude, longitude)
        assertEquals("0_0", result)
    }

    @Test
    fun `test with coordinates at grid boundaries`() {
        // Test with coordinates exactly at 5-degree boundaries
        val latitude = 5.0
        val longitude = 10.0
        val result = calculator.eval(latitude, longitude)
        assertEquals("5_10", result)
    }

    @Test
    fun `test with coordinates just below grid boundaries`() {
        // Test with coordinates just below 5-degree boundaries
        val latitude = 4.999
        val longitude = 9.999
        val result = calculator.eval(latitude, longitude)
        assertEquals("0_5", result)
    }

    @Test
    fun `test with null latitude`() {
        val result = calculator.eval(null, 10.0)
        assertEquals("unknown_region", result)
    }

    @Test
    fun `test with null longitude`() {
        val result = calculator.eval(45.0, null)
        assertEquals("unknown_region", result)
    }

    @Test
    fun `test with both null coordinates`() {
        val result = calculator.eval(null, null)
        assertEquals("unknown_region", result)
    }

    @Test
    fun `test with extreme values`() {
        // Test with extreme latitude and longitude values
        val latitude = 89.9
        val longitude = 179.9
        val result = calculator.eval(latitude, longitude)
        assertEquals("85_175", result)

        val latitude2 = -89.9
        val longitude2 = -179.9
        val result2 = calculator.eval(latitude2, longitude2)
        assertEquals("-90_-180", result2)
    }
}