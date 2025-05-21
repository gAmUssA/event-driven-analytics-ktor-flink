package dev.gamov.flightdemo.ktor.service

import dev.gamov.flightdemo.avro.FlightData
import dev.gamov.flightdemo.avro.ProcessedFlightData
import dev.gamov.flightdemo.ktor.config.KafkaConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service for consuming flight data from Kafka
 */
class KafkaService(private val config: KafkaConfig) {
    private val logger = LoggerFactory.getLogger(KafkaService::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _flightFlow = MutableSharedFlow<FlightData>(replay = 10)
    val flightFlow: Flow<FlightData> = _flightFlow.asSharedFlow()

    private val _processedFlightFlow = MutableSharedFlow<ProcessedFlightData>(replay = 10)
    val processedFlightFlow: Flow<ProcessedFlightData> = _processedFlightFlow.asSharedFlow()

    private val running = AtomicBoolean(false)
    private val reconnectChannel = Channel<Unit>(Channel.CONFLATED)

    /**
     * Start consuming flight data from Kafka
     */
    fun start() {
        if (running.compareAndSet(false, true)) {
            logger.info("Starting Kafka consumers with bootstrap servers: ${config.bootstrapServers}")
            startFlightConsumer()
            startProcessedFlightConsumer()
            startReconnectMonitor()
        }
    }

    /**
     * Stop consuming flight data from Kafka
     */
    fun stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("Stopping Kafka consumers")
            reconnectChannel.close()
        }
    }

    private fun startFlightConsumer() {
        scope.launch {
            try {
                logger.info("Starting flight consumer")
                val consumer = createFlightConsumer()
                consumer.subscribe(listOf(config.topics.flights))

                while (running.get()) {
                    try {
                        val records = consumer.poll(Duration.ofMillis(100))
                        records.forEach { record ->
                            val flight = record.value() as FlightData
                            _flightFlow.emit(flight)
                        }
                    } catch (e: Exception) {
                        logger.error("Error polling flight records", e)
                        reconnectChannel.send(Unit)
                        break
                    }
                }

                consumer.close()
            } catch (e: Exception) {
                logger.error("Error in flight consumer", e)
                reconnectChannel.send(Unit)
            }
        }
    }

    private fun startProcessedFlightConsumer() {
        scope.launch {
            try {
                logger.info("Starting processed flight consumer")
                val consumer = createProcessedFlightConsumer()
                consumer.subscribe(listOf(config.topics.processedFlights))

                while (running.get()) {
                    try {
                        val records = consumer.poll(Duration.ofMillis(100))
                        records.forEach { record ->
                            val processedFlight = record.value() as ProcessedFlightData
                            _processedFlightFlow.emit(processedFlight)
                        }
                    } catch (e: Exception) {
                        logger.error("Error polling processed flight records", e)
                        reconnectChannel.send(Unit)
                        break
                    }
                }

                consumer.close()
            } catch (e: Exception) {
                logger.error("Error in processed flight consumer", e)
                reconnectChannel.send(Unit)
            }
        }
    }

    private fun startReconnectMonitor() {
        scope.launch {
            for (unit in reconnectChannel) {
                if (running.get()) {
                    logger.info("Reconnecting Kafka consumers")
                    startFlightConsumer()
                    startProcessedFlightConsumer()
                }
            }
        }
    }

    private fun createFlightConsumer(): KafkaConsumer<String, Any> {
        val props = config.createConsumerProperties("flight")
        return KafkaConsumer(props)
    }

    private fun createProcessedFlightConsumer(): KafkaConsumer<String, Any> {
        val props = config.createConsumerProperties("processed-flight")
        return KafkaConsumer(props)
    }
}
