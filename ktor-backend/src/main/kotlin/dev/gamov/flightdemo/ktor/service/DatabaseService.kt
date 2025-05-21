package dev.gamov.flightdemo.ktor.service

import dev.gamov.flightdemo.ktor.config.DatabaseConfig
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

/**
 * Service for database operations
 */
class DatabaseService(private val config: DatabaseConfig) {
    private val logger = LoggerFactory.getLogger(DatabaseService::class.java)
    private val dataSource: DataSource = config.createDataSource()

    /**
     * Execute a query with a connection and map the results
     */
    fun <T> query(sql: String, parameters: List<Any> = emptyList(), mapper: (ResultSet) -> T): List<T> {
        logger.debug("Executing query: $sql with parameters: $parameters")
        
        return withConnection { connection ->
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
            
            results
        }
    }

    /**
     * Execute a query that returns a single result
     */
    fun <T> queryOne(sql: String, parameters: List<Any> = emptyList(), mapper: (ResultSet) -> T): T? {
        return query(sql, parameters, mapper).firstOrNull()
    }

    /**
     * Execute an update statement (INSERT, UPDATE, DELETE)
     */
    fun update(sql: String, parameters: List<Any> = emptyList()): Int {
        logger.debug("Executing update: $sql with parameters: $parameters")
        
        return withConnection { connection ->
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
            
            statement.executeUpdate()
        }
    }

    /**
     * Execute a block with a database connection
     */
    fun <T> withConnection(block: (Connection) -> T): T {
        val connection = dataSource.connection
        
        try {
            return block(connection)
        } finally {
            try {
                connection.close()
            } catch (e: Exception) {
                logger.error("Error closing connection", e)
            }
        }
    }

    /**
     * Execute a block within a transaction
     */
    fun <T> withTransaction(block: (Connection) -> T): T {
        return withConnection { connection ->
            val autoCommit = connection.autoCommit
            connection.autoCommit = false
            
            try {
                val result = block(connection)
                connection.commit()
                return@withConnection result
            } catch (e: Exception) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = autoCommit
            }
        }
    }
}

/**
 * Extension functions for ResultSet
 */
inline fun <reified T> ResultSet.get(columnName: String): T? {
    val value = this.getObject(columnName)
    return when {
        value == null -> null
        T::class.java.isAssignableFrom(value::class.java) -> value as T
        T::class.java == Int::class.java && value is Number -> value.toInt() as T
        T::class.java == Long::class.java && value is Number -> value.toLong() as T
        T::class.java == Double::class.java && value is Number -> value.toDouble() as T
        T::class.java == Boolean::class.java && value is Number -> (value.toInt() != 0) as T
        else -> throw IllegalArgumentException(
            "Cannot convert value of type ${value::class.java.name} to ${T::class.java.name}"
        )
    }
}