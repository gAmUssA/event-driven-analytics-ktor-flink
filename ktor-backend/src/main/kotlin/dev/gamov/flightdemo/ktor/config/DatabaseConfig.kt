package dev.gamov.flightdemo.ktor.config

import com.sksamuel.hoplite.ConfigLoader
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.Properties
import javax.sql.DataSource

/**
 * Configuration for Trino database connection
 */
data class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val catalog: String,
    val schema: String,
    val maxPoolSize: Int,
    val connectionTimeout: Long,
    val idleTimeout: Long,
    val maxLifetime: Long
) {
    companion object {
        /**
         * Load configuration from environment variables or default values
         */
        fun load(): DatabaseConfig {
            return DatabaseConfig(
                jdbcUrl = System.getenv("TRINO_JDBC_URL") ?: "jdbc:trino://localhost:8083",
                username = System.getenv("TRINO_USERNAME") ?: "trino",
                password = System.getenv("TRINO_PASSWORD") ?: "",
                catalog = System.getenv("TRINO_CATALOG") ?: "iceberg",
                schema = System.getenv("TRINO_SCHEMA") ?: "flight_data",
                maxPoolSize = System.getenv("TRINO_MAX_POOL_SIZE")?.toIntOrNull() ?: 10,
                connectionTimeout = System.getenv("TRINO_CONNECTION_TIMEOUT")?.toLongOrNull() ?: 30000,
                idleTimeout = System.getenv("TRINO_IDLE_TIMEOUT")?.toLongOrNull() ?: 600000,
                maxLifetime = System.getenv("TRINO_MAX_LIFETIME")?.toLongOrNull() ?: 1800000
            )
        }
    }

    /**
     * Create a HikariCP data source for database connections
     */
    fun createDataSource(): DataSource {
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = jdbcUrl
        hikariConfig.username = username
        hikariConfig.password = password
        hikariConfig.maximumPoolSize = maxPoolSize
        hikariConfig.connectionTimeout = connectionTimeout
        hikariConfig.idleTimeout = idleTimeout
        hikariConfig.maxLifetime = maxLifetime

        // Trino-specific properties
        hikariConfig.addDataSourceProperty("trino.catalog", catalog)
        hikariConfig.addDataSourceProperty("trino.schema", schema)

        return HikariDataSource(hikariConfig)
    }
}
