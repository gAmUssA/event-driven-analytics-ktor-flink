package dev.gamov.flightdemo.flink

import dev.gamov.flightdemo.flink.config.FlinkConfig
import dev.gamov.flightdemo.flink.config.IcebergConfig
import dev.gamov.flightdemo.flink.config.IcebergCatalogConfig
import dev.gamov.flightdemo.flink.config.IcebergTableConfig
import dev.gamov.flightdemo.flink.config.IcebergTablesConfig
import dev.gamov.flightdemo.flink.config.KafkaConfig
import dev.gamov.flightdemo.flink.config.KafkaTopics
import dev.gamov.flightdemo.flink.config.FlinkRuntimeConfig
import dev.gamov.flightdemo.flink.config.CheckpointingConfig
import dev.gamov.flightdemo.flink.config.StateConfig
import dev.gamov.flightdemo.generator.config.KafkaConfig as GeneratorKafkaConfig
import dev.gamov.flightdemo.generator.config.SimulationConfig
import dev.gamov.flightdemo.generator.kafka.FlightProducer
import dev.gamov.flightdemo.generator.simulation.FlightSimulator
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.avro.generic.GenericRecord
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration
import org.apache.flink.test.util.MiniClusterWithClientResource
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.clients.admin.AdminClientConfig
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.Properties
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatelessJobIT {
    private val logger = LoggerFactory.getLogger(StatelessJobIT::class.java)

    companion object {
        private const val FLIGHTS_TOPIC = "test-flights"
        private const val PROCESSED_FLIGHTS_TOPIC = "test-processed-flights"
        private const val SCHEMA_REGISTRY_PORT = 8081
    }

    private val network = Network.newNetwork()

    @Container
    private val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.9.0"))
        .withNetwork(network)
        .withNetworkAliases("kafka")

    @Container
    private val schemaRegistry = SchemaRegistryContainer("confluentinc/cp-schema-registry:7.9.0")
        .withNetwork(network)
        .withNetworkAliases("schema-registry")
        .dependsOn(kafka)

    private val flinkMiniCluster = MiniClusterWithClientResource(
        MiniClusterResourceConfiguration.Builder()
            .setNumberSlotsPerTaskManager(2)
            .setNumberTaskManagers(1)
            .build()
    )

    private lateinit var flightSimulator: FlightSimulator
    private lateinit var flightProducer: FlightProducer
    private lateinit var flinkConfig: FlinkConfig

    @BeforeAll
    fun setup() {
        // Start Flink mini cluster
        flinkMiniCluster.before()

        // Wait for Kafka and Schema Registry containers to start
        logger.info("Starting Kafka container...")
        kafka.start()
        logger.info("Kafka container started, bootstrap servers: {}", kafka.bootstrapServers)

        logger.info("Starting Schema Registry container...")
        schemaRegistry.start()
        logger.info("Schema Registry container started, URL: {}", schemaRegistry.schemaRegistryUrl)

        // Create Kafka topics
        createKafkaTopics()

        // Initialize flight simulator
        val simulationConfig = SimulationConfig(
            numFlights = 10,
            updateInterval = Duration.ofMillis(100),
            initialLatitudeMin = 30.0,
            initialLatitudeMax = 50.0,
            initialLongitudeMin = -120.0,
            initialLongitudeMax = -70.0,
            initialAltitudeMin = 30000,
            initialAltitudeMax = 40000,
            speedFactor = 10.0,
            flightStatusProbabilities = mapOf(
                "ON_TIME" to 0.7,
                "DELAYED" to 0.2,
                "CANCELLED" to 0.05,
                "DIVERTED" to 0.05
            )
        )
        flightSimulator = FlightSimulator(simulationConfig)
        flightSimulator.initialize()

        // Initialize flight producer
        val kafkaConfig = GeneratorKafkaConfig(
            bootstrapServers = kafka.bootstrapServers,
            schemaRegistryUrl = schemaRegistry.schemaRegistryUrl,
            topic = FLIGHTS_TOPIC,
            clientId = "test-producer",
            acks = "all",
            retries = 3,
            batchSize = 16384,
            lingerMs = 1,
            bufferMemory = 33554432
        )
        flightProducer = FlightProducer(kafkaConfig)

        // Create Flink config for the job
        flinkConfig = FlinkConfig(
            kafka = KafkaConfig(
                bootstrapServers = kafka.bootstrapServers,
                schemaRegistryUrl = schemaRegistry.schemaRegistryUrl,
                groupId = "test-consumer-group",
                autoOffsetReset = "earliest",
                topics = KafkaTopics(
                    flights = FLIGHTS_TOPIC,
                    processedFlights = PROCESSED_FLIGHTS_TOPIC
                )
            ),
            flink = FlinkRuntimeConfig(
                checkpointing = CheckpointingConfig(
                    interval = 5000,
                    timeout = 60000,
                    minPause = 1000,
                    maxConcurrent = 1
                ),
                state = StateConfig(
                    backend = "memory",
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

    @AfterAll
    fun tearDown() {
        flinkMiniCluster.after()
    }

    @Test
    fun `test stateless job processes flights above 35000 feet`() = runBlocking {
        // Generate and send test flights
        val scope = CoroutineScope(Dispatchers.Default)

        // Start the flight simulator and producer
        flightSimulator.start(scope)
        flightProducer.start(flightSimulator.getFlightFlow(), scope)

        // Wait for some data to be produced
        delay(2000)

        // Stop the producer and simulator
        flightProducer.stop()
        flightSimulator.stop()

        // Run the stateless job
        runStatelessJob("src/test/resources/test-application.yaml")

        // Wait for job to process data
        delay(5000)

        // Consume and verify processed flights
        val processedFlights = consumeProcessedFlights()

        // Verify that all processed flights are above 35000 feet
        assertTrue(processedFlights.isNotEmpty(), "No processed flights found")
        processedFlights.forEach { record ->
            logger.info("Verifying record: {}", record)

            // Convert numeric types as needed (JSON deserializes numbers as different types)
            val altitude = (record["altitude"] as Number).toInt()
            assertTrue(altitude > 35000, "Flight altitude should be above 35000 feet: $altitude")

            // Verify that the region field exists
            val region = record["region"] as String
            assertTrue(region.isNotEmpty(), "Flight region should not be empty")

            // Verify that the region is correctly calculated
            val latitude = (record["latitude"] as Number).toDouble()
            val longitude = (record["longitude"] as Number).toDouble()
            val expectedRegion = "${(Math.floor(latitude / 5) * 5).toInt()}_${(Math.floor(longitude / 5) * 5).toInt()}"
            assertEquals(expectedRegion, region, "Region should be correctly calculated")
        }

        logger.info("Successfully verified ${processedFlights.size} processed flights")
    }

    private fun createKafkaTopics() {
        logger.info("Creating Kafka topics: {} and {}", FLIGHTS_TOPIC, PROCESSED_FLIGHTS_TOPIC)

        // Create admin client
        val adminProps = Properties().apply {
            put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
        }

        AdminClient.create(adminProps).use { adminClient ->
            // Create topics
            val newTopics = listOf(
                NewTopic(FLIGHTS_TOPIC, 1, 1.toShort()),
                NewTopic(PROCESSED_FLIGHTS_TOPIC, 1, 1.toShort())
            )

            try {
                val result = adminClient.createTopics(newTopics)
                result.all().get(30, TimeUnit.SECONDS) // Wait for topic creation to complete
                logger.info("Kafka topics created successfully")
            } catch (e: Exception) {
                logger.warn("Error creating Kafka topics: {}", e.message)
                // Continue anyway, as topics might already exist
            }
        }
    }

    private fun consumeProcessedFlights(): List<Map<String, Any>> {
        val props = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-${UUID.randomUUID()}")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
        }

        val consumer = KafkaConsumer<String, String>(props)
        consumer.subscribe(listOf(PROCESSED_FLIGHTS_TOPIC))

        logger.info("Polling for records from topic: {}", PROCESSED_FLIGHTS_TOPIC)
        val records = consumer.poll(Duration.ofSeconds(10))
        logger.info("Received {} records", records.count())

        val flights = records.map { record ->
            logger.info("Processing record: {}", record.value())
            // Parse JSON string to Map
            val mapper = com.fasterxml.jackson.databind.ObjectMapper()
            @Suppress("UNCHECKED_CAST")
            mapper.readValue(record.value(), Map::class.java) as Map<String, Any>
        }

        consumer.close()
        return flights
    }

    /**
     * Runs the stateless job with the given configuration file.
     */
    private fun runStatelessJob(configFile: String) {
        // Load configuration
        logger.info("Loading configuration from {}", configFile)

        // Create a custom configuration with the test container addresses
        val config = FlinkConfig(
            kafka = KafkaConfig(
                bootstrapServers = kafka.bootstrapServers,
                schemaRegistryUrl = schemaRegistry.schemaRegistryUrl,
                groupId = "test-consumer-group",
                autoOffsetReset = "earliest",
                topics = KafkaTopics(
                    flights = FLIGHTS_TOPIC,
                    processedFlights = PROCESSED_FLIGHTS_TOPIC
                )
            ),
            flink = FlinkRuntimeConfig(
                checkpointing = CheckpointingConfig(
                    interval = 5000,
                    timeout = 60000,
                    minPause = 1000,
                    maxConcurrent = 1
                ),
                state = StateConfig(
                    backend = "memory",
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

        logger.info("Using custom configuration with Kafka bootstrap servers: {}", config.kafka.bootstrapServers)
        logger.info("Using custom configuration with Schema Registry URL: {}", config.kafka.schemaRegistryUrl)

        // Run the job using the refactored StatelessJob class
        StatelessJob.runJob(config)

        logger.info("Stateless job submitted")
    }

    /**
     * Custom Schema Registry container.
     */
    class SchemaRegistryContainer(dockerImageName: String) : 
        org.testcontainers.containers.GenericContainer<SchemaRegistryContainer>(dockerImageName) {

        init {
            withExposedPorts(SCHEMA_REGISTRY_PORT)
            withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "kafka:9092")
            waitingFor(Wait.forHttp("/subjects").forStatusCode(200))
        }

        val schemaRegistryUrl: String
            get() = "http://${host}:${getMappedPort(SCHEMA_REGISTRY_PORT)}"
    }
}
