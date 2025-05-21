package dev.gamov.flightdemo.ktor.config

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.util.Properties

/**
 * Configuration for Kafka consumers
 */
data class KafkaConfig(
    val bootstrapServers: String,
    val schemaRegistryUrl: String,
    val groupId: String,
    val autoOffsetReset: String,
    val topics: Topics
) {
    data class Topics(
        val flights: String,
        val processedFlights: String
    )

    companion object {
        private val logger = LoggerFactory.getLogger(KafkaConfig::class.java)

        /**
         * Load configuration from environment variables with fallback to default values
         */
        fun load(): KafkaConfig {
            // Read from environment variables with fallback to default values
            val bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:29092"
            logger.info("Kafka bootstrap servers: $bootstrapServers")

            return KafkaConfig(
                bootstrapServers = bootstrapServers,
                schemaRegistryUrl = System.getenv("SCHEMA_REGISTRY_URL") ?: "http://localhost:8081",
                groupId = System.getenv("KAFKA_GROUP_ID") ?: "flight-tracking-ktor-backend",
                autoOffsetReset = System.getenv("KAFKA_AUTO_OFFSET_RESET") ?: "earliest",
                topics = Topics(
                    flights = System.getenv("KAFKA_TOPIC_FLIGHTS") ?: "flights",
                    processedFlights = System.getenv("KAFKA_TOPIC_PROCESSED_FLIGHTS") ?: "processed_flights"
                )
            )
        }
    }

    /**
     * Create Kafka consumer properties for Avro deserialization
     */
    fun createConsumerProperties(groupIdSuffix: String = ""): Properties {
        val props = Properties()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = if (groupIdSuffix.isNotEmpty()) "$groupId-$groupIdSuffix" else groupId
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = autoOffsetReset
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.java.name
        props[KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = schemaRegistryUrl
        props[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = "true"
        return props
    }
}
