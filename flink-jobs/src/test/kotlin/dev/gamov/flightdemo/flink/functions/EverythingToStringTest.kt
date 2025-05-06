package dev.gamov.flightdemo.flink.functions

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for the EverythingToString scalar function.
 */
class EverythingToStringTest {

    private val function = EverythingToString()

    @Test
    fun `test with string input`() {
        val input = "test string"
        val result = function.eval(input)
        assertEquals(input, result)
    }

    @Test
    fun `test with integer input`() {
        val input = 42
        val result = function.eval(input)
        assertEquals("42", result)
    }

    @Test
    fun `test with double input`() {
        val input = 3.14
        val result = function.eval(input)
        assertEquals("3.14", result)
    }

    @Test
    fun `test with boolean input`() {
        val input = true
        val result = function.eval(input)
        assertEquals("true", result)
    }

    @Test
    fun `test with null input`() {
        val result = function.eval(null)
        assertEquals("null", result)
    }

    @Test
    fun `test with custom object`() {
        val input = CustomObject("test")
        val result = function.eval(input)
        assertEquals("CustomObject(value=test)", result)
    }

    @Test
    fun `test with enum input`() {
        val input = TestEnum.VALUE_1
        val result = function.eval(input)
        assertEquals("VALUE_1", result)
    }

    // Custom class for testing toString() behavior
    private data class CustomObject(val value: String)

    // Enum for testing enum toString() behavior
    private enum class TestEnum {
        VALUE_1, VALUE_2
    }
}