package dev.gamov.flightdemo.flink.functions

import org.apache.flink.table.annotation.DataTypeHint
import org.apache.flink.table.annotation.InputGroup
import org.apache.flink.table.functions.ScalarFunction

/**
 * A scalar function that converts any input value to its string representation.
 * This is particularly useful for handling Avro enum types in SQL queries.
 */
class EverythingToString : ScalarFunction() {
    /**
     * Converts any input value to its string representation.
     * 
     * @param x The input value of any type
     * @return The string representation of the input value
     */
    fun eval(@DataTypeHint(inputGroup = InputGroup.ANY) x: Any?): String {
        return x.toString()
    }
}