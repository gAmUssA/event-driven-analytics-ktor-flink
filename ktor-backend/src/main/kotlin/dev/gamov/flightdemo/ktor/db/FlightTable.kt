package dev.gamov.flightdemo.ktor.db

import java.sql.Connection
import java.sql.ResultSet
import java.time.Instant

/**
 * Base class for table definitions
 */
abstract class Table(val name: String) {
    /**
     * Column definition
     */
    class Column<T>(val name: String, val type: String)

    /**
     * Define a string column
     */
    fun varchar(name: String, length: Int): Column<String> = Column(name, "VARCHAR($length)")

    /**
     * Define an integer column
     */
    fun integer(name: String): Column<Int> = Column(name, "INTEGER")

    /**
     * Define a double column
     */
    fun double(name: String): Column<Double> = Column(name, "DOUBLE")

    /**
     * Define a boolean column
     */
    fun boolean(name: String): Column<Boolean> = Column(name, "BOOLEAN")

    /**
     * Define a timestamp column
     */
    fun timestamp(name: String): Column<Instant> = Column(name, "TIMESTAMP")

    /**
     * Select query builder
     */
    fun select(block: (SelectBuilder.() -> Unit)? = null): SelectBuilder {
        val builder = SelectBuilder(this)
        block?.let { builder.it() }
        return builder
    }
}

/**
 * Builder for SELECT queries
 */
class SelectBuilder(private val table: Table) {
    private var whereClause: String? = null
    private var orderByClause: String? = null
    private var limitClause: Int? = null
    private var parameters: MutableList<Any> = mutableListOf()

    /**
     * Add a WHERE clause
     */
    fun where(condition: String, vararg params: Any): SelectBuilder {
        whereClause = condition
        parameters.addAll(params)
        return this
    }

    /**
     * Add an ORDER BY clause
     */
    fun orderBy(column: String, order: SortOrder = SortOrder.ASC): SelectBuilder {
        orderByClause = "$column ${order.name}"
        return this
    }

    /**
     * Add a LIMIT clause
     */
    fun limit(limit: Int): SelectBuilder {
        limitClause = limit
        return this
    }

    /**
     * Build the SQL query
     */
    fun build(): String {
        val sql = StringBuilder("SELECT * FROM ${table.name}")
        
        whereClause?.let { sql.append(" WHERE $it") }
        orderByClause?.let { sql.append(" ORDER BY $it") }
        limitClause?.let { sql.append(" LIMIT $it") }
        
        return sql.toString()
    }

    /**
     * Execute the query
     */
    fun <T> executeQuery(connection: Connection, mapper: (ResultSet) -> T): List<T> {
        val sql = build()
        val statement = connection.prepareStatement(sql)
        
        // Set parameters
        parameters.forEachIndexed { index, param ->
            when (param) {
                is String -> statement.setString(index + 1, param)
                is Int -> statement.setInt(index + 1, param)
                is Long -> statement.setLong(index + 1, param)
                is Double -> statement.setDouble(index + 1, param)
                is Boolean -> statement.setBoolean(index + 1, param)
                else -> throw IllegalArgumentException("Unsupported parameter type: ${param::class.java.name}")
            }
        }
        
        val resultSet = statement.executeQuery()
        val results = mutableListOf<T>()
        
        while (resultSet.next()) {
            results.add(mapper(resultSet))
        }
        
        return results
    }

    /**
     * Get the parameters for the query
     */
    fun getParameters(): List<Any> = parameters
}

/**
 * Sort order for ORDER BY clause
 */
enum class SortOrder {
    ASC, DESC
}

/**
 * Flight data table definition
 */
object FlightTable : Table("flight_data") {
    val flightId = varchar("flight_id", 50)
    val callsign = varchar("callsign", 50)
    val latitude = double("latitude")
    val longitude = double("longitude")
    val altitude = integer("altitude")
    val heading = double("heading")
    val speed = double("speed")
    val verticalSpeed = double("vertical_speed")
    val origin = varchar("origin", 10)
    val destination = varchar("destination", 10)
    val status = varchar("status", 20)
    val timestamp = timestamp("timestamp")
    val region = varchar("region", 20)
    val isHighAltitude = boolean("is_high_altitude")
    val processingTimestamp = timestamp("processing_timestamp")
}

/**
 * Region statistics table definition
 */
object RegionStatsTable : Table("region_stats") {
    val region = varchar("region", 20)
    val flightCount = integer("flight_count")
    val avgAltitude = double("avg_altitude")
    val timestamp = timestamp("timestamp")
}