package dev.gamov.flightdemo.flink.functions

import org.apache.flink.table.annotation.DataTypeHint
import org.apache.flink.table.annotation.InputGroup
import org.apache.flink.table.functions.ScalarFunction
import org.slf4j.LoggerFactory

/**
 * A scalar function that converts various timestamp types to epoch milliseconds.
 * This is useful for converting timestamp types to a format compatible with Avro's timestamp-millis logical type.
 */
class TimestampToEpochMillis : ScalarFunction() {
    private val logger = LoggerFactory.getLogger(TimestampToEpochMillis::class.java)

    /**
     * Converts a timestamp value to epoch milliseconds.
     * 
     * @param timestamp The timestamp value of any supported type
     * @return The epoch milliseconds representation of the timestamp
     */
    fun eval(@DataTypeHint(inputGroup = InputGroup.ANY) timestamp: Any?): Long {
        if (timestamp == null) {
            return 0L
        }
        
        // Extract the timestamp value based on its type
        return when (timestamp) {
            is java.time.LocalDateTime -> java.sql.Timestamp.valueOf(timestamp).time
            is java.time.Instant -> timestamp.toEpochMilli()
            is java.sql.Timestamp -> timestamp.time
            is java.util.Date -> timestamp.time
            is Long -> timestamp
            else -> {
                // Log the actual type for debugging
                logger.warn("Unexpected timestamp type: ${timestamp.javaClass.name}, value: $timestamp")
                0L
            }
        }
    }
}