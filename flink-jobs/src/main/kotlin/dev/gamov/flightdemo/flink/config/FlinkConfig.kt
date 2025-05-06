package dev.gamov.flightdemo.flink.config

import com.sksamuel.hoplite.ConfigAlias
import dev.gamov.flightdemo.common.config.ConfigUtils
import dev.gamov.flightdemo.common.config.KafkaConfigBase

/**
 * Configuration for the Flink Table API jobs.
 */
data class FlinkConfig(
    val kafka: KafkaConfig,
    val flink: FlinkRuntimeConfig,
    val iceberg: IcebergConfig
) {
    companion object {
        /**
         * Loads the configuration from the specified file or classpath.
         *
         * @param configFile The path to the configuration file.
         * @return The loaded configuration.
         */
        fun load(configFile: String): FlinkConfig {
            return ConfigUtils.loadConfig(
                configFile = configFile,
                classpathResource = "/application.yaml",
                defaultConfig = {
                    FlinkConfig(
                        kafka = KafkaConfig(
                            bootstrapServers = "kafka:9092",
                            schemaRegistryUrl = "http://schema-registry:8081",
                            groupId = "flink-jobs",
                            autoOffsetReset = "earliest",
                            topics = KafkaTopics(
                                flights = "flights",
                                processedFlights = "processed_flights"
                            )
                        ),
                        flink = FlinkRuntimeConfig(
                            checkpointing = CheckpointingConfig(
                                interval = 60000,
                                timeout = 30000,
                                minPause = 10000,
                                maxConcurrent = 1
                            ),
                            state = StateConfig(
                                backend = "rocksdb",
                                dir = "/tmp/flink-checkpoints"
                            )
                        ),
                        iceberg = IcebergConfig(
                            catalog = IcebergCatalogConfig(
                                name = "iceberg_catalog",
                                type = "rest",
                                uri = "http://rest-catalog:8181",
                                warehouse = "s3a://warehouse/wh"
                            ),
                            tables = IcebergTablesConfig(
                                flightData = IcebergTableConfig(
                                    name = "flight_data",
                                    partitioning = "days(timestamp)"
                                ),
                                flightAggregates = IcebergTableConfig(
                                    name = "flight_aggregates",
                                    partitioning = "days(window_start)"
                                )
                            )
                        )
                    )
                }
            )
        }
    }
}

/**
 * Kafka configuration for Flink jobs.
 * Implements the common KafkaConfigBase interface with Flink-specific properties.
 */
data class KafkaConfig(
    override val bootstrapServers: String,
    override val schemaRegistryUrl: String,
    val groupId: String,
    val autoOffsetReset: String,
    val topics: KafkaTopics
) : KafkaConfigBase

/**
 * Kafka topics configuration.
 */
data class KafkaTopics(
    val flights: String,
    val processedFlights: String
)

/**
 * Flink runtime configuration.
 */
data class FlinkRuntimeConfig(
    val checkpointing: CheckpointingConfig,
    val state: StateConfig
)

/**
 * Checkpointing configuration.
 */
data class CheckpointingConfig(
    val interval: Long,
    val timeout: Long,
    val minPause: Long,
    val maxConcurrent: Int
)

/**
 * State backend configuration.
 */
data class StateConfig(
    val backend: String,
    val dir: String
)

/**
 * Iceberg configuration.
 */
data class IcebergConfig(
    val catalog: IcebergCatalogConfig,
    val tables: IcebergTablesConfig
)

/**
 * Iceberg catalog configuration.
 */
data class IcebergCatalogConfig(
    val name: String,
    val type: String,
    val uri: String,
    val warehouse: String
)

/**
 * Iceberg tables configuration.
 */
data class IcebergTablesConfig(
    val flightData: IcebergTableConfig,
    val flightAggregates: IcebergTableConfig
)

/**
 * Iceberg table configuration.
 */
data class IcebergTableConfig(
    val name: String,
    val partitioning: String
)
