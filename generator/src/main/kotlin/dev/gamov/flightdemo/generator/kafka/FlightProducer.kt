package dev.gamov.flightdemo.generator.kafka

import dev.gamov.flightdemo.avro.FlightData
import dev.gamov.flightdemo.generator.config.KafkaConfig
import dev.gamov.flightdemo.generator.model.Flight
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.util.Properties
import java.util.concurrent.atomic.AtomicLong

/**
 * Produces flight data to Kafka using Avro serialization.
 */
class FlightProducer(
    private val config: KafkaConfig,
    private val producer: KafkaProducer<String, FlightData>? = null
) {
    private val logger = LoggerFactory.getLogger(FlightProducer::class.java)
    private val kafkaProducer: KafkaProducer<String, FlightData>
    private val messageCounter = AtomicLong(0)
    private var running = false

    init {
        kafkaProducer = producer ?: createProducer()
        logger.info("Kafka producer initialized with bootstrap servers: ${config.bootstrapServers}")
        logger.info("Schema Registry URL: ${config.schemaRegistryUrl}")
    }

    private fun createProducer(): KafkaProducer<String, FlightData> {
        val props = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers)
            put(ProducerConfig.CLIENT_ID_CONFIG, config.clientId)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java.name)
            put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, config.schemaRegistryUrl)
            put(ProducerConfig.ACKS_CONFIG, config.acks)
            put(ProducerConfig.RETRIES_CONFIG, config.retries)
            put(ProducerConfig.BATCH_SIZE_CONFIG, config.batchSize)
            put(ProducerConfig.LINGER_MS_CONFIG, config.lingerMs)
            put(ProducerConfig.BUFFER_MEMORY_CONFIG, config.bufferMemory)

            // Add Confluent Cloud specific properties if they are set
            config.securityProtocol?.let { put("security.protocol", it) }
            config.saslMechanism?.let { put("sasl.mechanism", it) }
            config.saslJaasConfig?.let { put("sasl.jaas.config", it) }
            config.schemaRegistryBasicAuthCredentialsSource?.let { 
                put("schema.registry.basic.auth.credentials.source", it) 
            }
            config.schemaRegistryBasicAuthUserInfo?.let { 
                put("schema.registry.basic.auth.user.info", it) 
            }
            config.clientDnsLookup?.let { put("client.dns.lookup", it) }
            config.sessionTimeoutMs?.let { put("session.timeout.ms", it) }

            // Optional environment property for Confluent Cloud
            config.environment?.let { put("environment", it) }

            // Optional API key and secret for Flink
            config.apiKey?.let { put("flink.api.key", it) }
            config.apiSecret?.let { put("flink.api.secret", it) }
        }

        return KafkaProducer(props)
    }

    /**
     * Starts consuming flight updates from the provided flow and producing them to Kafka.
     *
     * @param flightFlow The flow of flight updates.
     * @param scope The coroutine scope to launch the producer in.
     */
    fun start(flightFlow: Flow<Flight>, scope: CoroutineScope) {
        if (running) {
            logger.warn("Flight producer is already running")
            return
        }

        running = true
        logger.info("Starting flight producer to topic: ${config.topic}")

        scope.launch(Dispatchers.IO) {
            flightFlow.collect { flight ->
                sendFlight(flight)
            }
        }
    }

    /**
     * Stops the producer and closes the Kafka connection.
     */
    fun stop() {
        running = false
        kafkaProducer.close()
        logger.info("Flight producer stopped after sending ${messageCounter.get()} messages")
    }

    /**
     * Sends a flight to Kafka using Avro serialization.
     *
     * @param flight The flight to send.
     */
    fun sendFlight(flight: Flight) {
        try {
            // Convert flight to Avro object
            val flightData = flight.toAvro()

            val record = ProducerRecord(
                config.topic,
                flight.flightId,
                flightData
            )

            kafkaProducer.send(record) { metadata, exception ->
                if (exception != null) {
                    logger.error("Error sending flight data to Kafka", exception)
                } else {
                    val count = messageCounter.incrementAndGet()
                    if (count % 1000 == 0L) {
                        logger.info("Sent $count messages to Kafka")
                    }
                    logger.debug("Sent flight data to Kafka: topic=${metadata.topic()}, partition=${metadata.partition()}, offset=${metadata.offset()}")
                }
            }
        } catch (e: Exception) {
            logger.error("Error preparing flight data for Kafka", e)
        }
    }
}
