package dev.gamov.flightdemo.flink.functions

import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import kotlin.test.assertEquals

/**
 * Tests for the TimestampToEpochMillis scalar function.
 */
class TimestampToEpochMillisTest {

    private val converter = TimestampToEpochMillis()

    // Reference timestamp: 2023-01-01T12:00:00Z (1672574400000 milliseconds since epoch)
    private val referenceEpochMillis = 1672574400000L
    private val referenceInstant = Instant.ofEpochMilli(referenceEpochMillis)
    private val referenceLocalDateTime = LocalDateTime.ofInstant(referenceInstant, ZoneOffset.UTC)
    private val referenceSqlTimestamp = Timestamp.from(referenceInstant)
    private val referenceDate = Date.from(referenceInstant)

    @Test
    fun `test with LocalDateTime`() {
        // When converting LocalDateTime to timestamp, we need to specify the time zone
        // The TimestampToEpochMillis function uses the system default time zone
        // So we need to create a LocalDateTime that, when interpreted in the system default time zone,
        // will produce the expected epoch millis
        val systemZone = ZoneOffset.systemDefault()
        val localDateTimeInSystemZone = LocalDateTime.ofInstant(referenceInstant, systemZone)

        val result = converter.eval(localDateTimeInSystemZone)
        assertEquals(referenceEpochMillis, result)
    }

    @Test
    fun `test with Instant`() {
        val result = converter.eval(referenceInstant)
        assertEquals(referenceEpochMillis, result)
    }

    @Test
    fun `test with SQL Timestamp`() {
        val result = converter.eval(referenceSqlTimestamp)
        assertEquals(referenceEpochMillis, result)
    }

    @Test
    fun `test with Date`() {
        val result = converter.eval(referenceDate)
        assertEquals(referenceEpochMillis, result)
    }

    @Test
    fun `test with Long`() {
        val result = converter.eval(referenceEpochMillis)
        assertEquals(referenceEpochMillis, result)
    }

    @Test
    fun `test with null`() {
        val result = converter.eval(null)
        assertEquals(0L, result)
    }

    @Test
    fun `test with unsupported type`() {
        val result = converter.eval("not a timestamp")
        assertEquals(0L, result)
    }

    @Test
    fun `test with different time zones`() {
        // Create a specific instant: 2023-01-01T12:00:00Z
        val specificInstant = Instant.parse("2023-01-01T12:00:00Z")
        val expectedMillis = specificInstant.toEpochMilli()

        // Convert to a LocalDateTime in the system default time zone
        val systemZone = ZoneOffset.systemDefault()
        val localDateTimeInSystemZone = LocalDateTime.ofInstant(specificInstant, systemZone)

        // Test the conversion
        val result = converter.eval(localDateTimeInSystemZone)
        assertEquals(expectedMillis, result)
    }

    @Test
    fun `test with epoch start`() {
        // Test with the Unix epoch start: 1970-01-01T00:00:00Z
        val epochStart = Instant.EPOCH
        val result = converter.eval(epochStart)
        assertEquals(0L, result)
    }

    @Test
    fun `test with future date`() {
        // Test with a future date: 2050-01-01T00:00:00Z
        val futureDate = LocalDateTime.of(2050, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC)
        val expectedMillis = futureDate.toEpochMilli()

        val result = converter.eval(futureDate)
        assertEquals(expectedMillis, result)
    }
}
