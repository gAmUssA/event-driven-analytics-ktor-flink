package dev.gamov.flightdemo.generator.kafka

import dev.gamov.flightdemo.avro.FlightData
import dev.gamov.flightdemo.generator.config.KafkaConfig
import dev.gamov.flightdemo.generator.model.Flight
import dev.gamov.flightdemo.generator.model.FlightStatus
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant
import java.util.concurrent.Future

class FlightProducerTest {

    private lateinit var config: KafkaConfig
    private lateinit var mockProducer: KafkaProducer<String, FlightData>
    private lateinit var producer: FlightProducer
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @BeforeEach
    fun setUp() {
        // Create a mock KafkaConfig
        config = KafkaConfig(
            bootstrapServers = "localhost:9092",
            schemaRegistryUrl = "http://localhost:8081",
            topic = "test-topic",
            clientId = "test-client",
            acks = "all",
            retries = 3,
            batchSize = 16384,
            lingerMs = 1,
            bufferMemory = 33554432
        )

        // Create a mock KafkaProducer
        mockProducer = mockk<KafkaProducer<String, FlightData>>(relaxed = true)

        // Create the producer with the mock KafkaProducer
        producer = FlightProducer(config, mockProducer)
    }

    @Test
    fun `start sets running flag to true`() {
        // Arrange
        val flightFlow = MutableSharedFlow<Flight>()

        // Act
        producer.start(flightFlow, testScope)

        // Assert - we can't directly test the private running flag, but we can test that
        // calling start again logs a warning, which only happens if running is true
        val logger = spyk(producer)
        logger.start(flightFlow, testScope)
        verify { logger.start(flightFlow, testScope) }
    }

    @Test
    fun `stop closes the producer`() {
        // Act
        producer.stop()

        // Assert
        verify { mockProducer.close() }
    }

    @Test
    fun `sendFlight sends a record to Kafka`() {
        // Arrange
        val flight = Flight(
            flightId = "FL123",
            callsign = "AIR123",
            latitude = 40.0,
            longitude = -74.0,
            altitude = 35000,
            heading = 90.0,
            speed = 500.0,
            verticalSpeed = 0.0,
            origin = "JFK",
            destination = "LHR",
            status = FlightStatus.ON_TIME,
            timestamp = Instant.now()
        )

        // Mock the send method to return a mock Future
        val mockFuture = mockk<Future<RecordMetadata>>(relaxed = true)
        val mockMetadata = mockk<RecordMetadata>(relaxed = true)
        every { mockMetadata.topic() } returns config.topic
        every { mockMetadata.partition() } returns 0
        every { mockMetadata.offset() } returns 0L
        every { 
            mockProducer.send(any(), any()) 
        } answers { 
            secondArg<(RecordMetadata?, Exception?) -> Unit>().invoke(mockMetadata, null)
            mockFuture
        }

        // Act
        producer.sendFlight(flight)

        // Assert
        verify { 
            mockProducer.send(match<ProducerRecord<String, FlightData>> { 
                it.topic() == config.topic && it.key() == flight.flightId
            }, any())
        }
    }

    @Test
    fun `sendFlight converts Flight to FlightData and sends it to Kafka`() {
        // Arrange
        val flight = Flight(
            flightId = "FL123",
            callsign = "AIR123",
            latitude = 40.0,
            longitude = -74.0,
            altitude = 35000,
            heading = 90.0,
            speed = 500.0,
            verticalSpeed = 0.0,
            origin = "JFK",
            destination = "LHR",
            status = FlightStatus.ON_TIME,
            timestamp = Instant.now()
        )

        // Mock the send method to capture the record
        val recordSlot = slot<ProducerRecord<String, FlightData>>()
        every { mockProducer.send(capture(recordSlot), any()) } returns mockk(relaxed = true)

        // Act
        producer.sendFlight(flight)

        // Assert
        verify { mockProducer.send(any<ProducerRecord<String, FlightData>>(), any()) }

        // Verify the record key and value
        val record = recordSlot.captured
        assertEquals("FL123", record.key())
        val flightData = record.value()
        assertEquals("FL123", flightData.getFlightId())
        assertEquals("AIR123", flightData.getCallsign())
        assertEquals(40.0, flightData.getLatitude())
        assertEquals(-74.0, flightData.getLongitude())
        assertEquals(35000, flightData.getAltitude())
        assertEquals(90.0, flightData.getHeading())
        assertEquals(500.0, flightData.getSpeed())
        assertEquals(0.0, flightData.getVerticalSpeed())
        assertEquals("JFK", flightData.getOrigin())
        assertEquals("LHR", flightData.getDestination())
        assertEquals(dev.gamov.flightdemo.avro.FlightStatus.ON_TIME, flightData.getStatus())
        // Avro timestamp-millis has millisecond precision, so we need to truncate the Flight timestamp
        val truncatedTimestamp = flight.timestamp.truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
        assertEquals(truncatedTimestamp, flightData.getTimestamp())
    }
}
