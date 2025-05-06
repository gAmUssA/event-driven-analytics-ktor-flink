package dev.gamov.flightdemo.common.config

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.yaml.YamlPropertySource
import com.sksamuel.hoplite.ConfigAlias
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Utility class for loading configuration.
 * Provides common functionality for loading configuration from file, classpath, or defaults.
 */
object ConfigUtils {
    // Make logger public to be accessible from inline functions
    val logger = LoggerFactory.getLogger(ConfigUtils::class.java)

    /**
     * Loads the configuration from the specified file or classpath.
     *
     * @param configFile The path to the configuration file.
     * @param classpathResource The name of the classpath resource to load if file is not found.
     * @param defaultConfig A function that returns the default configuration if both file and classpath loading fail.
     * @return The loaded configuration.
     */
    inline fun <reified T : Any> loadConfig(
        configFile: String,
        classpathResource: String,
        defaultConfig: () -> T
    ): T {
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
            logger.info("Trying to load configuration from classpath: {}", classpathResource)
            return loadFromClasspath(classpathResource)
        } catch (e: Exception) {
            logger.warn("Failed to load configuration from classpath: {}", e.message)
        }

        // Fall back to default configuration
        logger.info("Using default configuration")
        return defaultConfig()
    }

    /**
     * Loads configuration from a file.
     *
     * @param file The file to load configuration from.
     * @return The loaded configuration.
     */
    inline fun <reified T : Any> loadFromFile(file: File): T {
        return ConfigLoader.builder()
            .addSource(YamlPropertySource(file.absolutePath))
            .addSource(PropertySource.environment())
            .build()
            .loadConfigOrThrow()
    }

    /**
     * Loads configuration from the classpath.
     *
     * @param resource The classpath resource to load.
     * @return The loaded configuration.
     */
    inline fun <reified T : Any> loadFromClasspath(resource: String): T {
        val configLoader = ConfigLoader.builder()
            .addSource(PropertySource.resource(resource))
            .addSource(PropertySource.environment())
            .build()

        return configLoader.loadConfigOrThrow()
    }
}

/**
 * Base interface for Kafka configuration properties.
 */
interface KafkaConfigBase {
    val bootstrapServers: String
    val schemaRegistryUrl: String
}

/**
 * Common Kafka configuration properties.
 */
data class KafkaConfig(
    override val bootstrapServers: String,
    override val schemaRegistryUrl: String
) : KafkaConfigBase
