package dev.gamov.flightdemo.generator.config

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.yaml.YamlPropertySource
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Duration

/**
 * Configuration for the Flight Data Generator.
 */
data class GeneratorConfig(
    val kafka: KafkaConfig,
    val simulation: SimulationConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GeneratorConfig::class.java)

        /**
         * Loads the configuration from the specified file or classpath.
         *
         * @param configFile The path to the configuration file.
         * @return The loaded configuration.
         */
        fun load(configFile: String): GeneratorConfig {
            // Try to load from the provided file path
            val file = File(configFile)
            if (file.exists()) {
                logger.info("Loading configuration from file: {}", configFile)
                try {
                    return loadFromFile(file)
                } catch (e: Exception) {
                    logger.warn("Failed to load configuration from file: {}", e.message)
                }
            }

            // Try to load from classpath
            try {
                logger.info("Trying to load configuration from classpath")
                return loadFromClasspath()
            } catch (e: Exception) {
                logger.warn("Failed to load configuration from classpath: {}", e.message)
            }

            // Fall back to default configuration
            logger.info("Using default configuration")
            return createDefaultConfig()
        }

        private fun loadFromFile(file: File): GeneratorConfig {
            return ConfigLoader.builder()
                .addSource(YamlPropertySource(file.absolutePath))
                .addSource(PropertySource.environment())
                .build()
                .loadConfigOrThrow()
        }

        private fun loadFromClasspath(): GeneratorConfig {
            val configLoader = ConfigLoader.builder()
                .addSource(PropertySource.resource("/generator-config.yaml"))
                .addSource(PropertySource.environment())
                .build()

            return configLoader.loadConfigOrThrow()
        }

        private fun createDefaultConfig(): GeneratorConfig {
            return GeneratorConfig(
                kafka = KafkaConfig(
                    bootstrapServers = "localhost:29092",
                    schemaRegistryUrl = "http://localhost:8081",
                    topic = "flights"
                ),
                simulation = SimulationConfig()
            )
        }
    }
}

/**
 * Kafka configuration.
 */
data class KafkaConfig(
    val bootstrapServers: String,
    val schemaRegistryUrl: String,
    val topic: String,
    val clientId: String = "flight-data-generator",
    val acks: String = "all",
    val retries: Int = 3,
    val batchSize: Int = 16384,
    val lingerMs: Long = 1,
    val bufferMemory: Long = 33554432
)

/**
 * Flight simulation configuration.
 */
data class SimulationConfig(
    val numFlights: Int = 100,
    val updateInterval: Duration = Duration.ofSeconds(1),
    val speedFactor: Double = 1.0,
    val initialAltitudeMin: Int = 30000,
    val initialAltitudeMax: Int = 40000,
    val initialLatitudeMin: Double = 25.0,
    val initialLatitudeMax: Double = 50.0,
    val initialLongitudeMin: Double = -125.0,
    val initialLongitudeMax: Double = -70.0,
    val flightStatusProbabilities: Map<String, Double> = mapOf(
        "ON_TIME" to 0.7,
        "DELAYED" to 0.2,
        "CANCELLED" to 0.05,
        "DIVERTED" to 0.05
    )
)
