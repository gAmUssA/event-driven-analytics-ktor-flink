package dev.gamov.flightdemo.flink

import dev.gamov.flightdemo.avro.FlightData
import dev.gamov.flightdemo.flink.config.FlinkConfig
import dev.gamov.flightdemo.flink.functions.EverythingToString
import dev.gamov.flightdemo.flink.functions.RegionCalculator
import dev.gamov.flightdemo.flink.functions.TimestampToEpochMillis
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroDeserializationSchema
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.datastream.DataStream
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.table.api.DataTypes
import org.apache.flink.table.api.Schema
import org.apache.flink.table.api.TableDescriptor
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment
import org.slf4j.LoggerFactory

/**
 * Stateless Flink job that filters flights above 35,000 feet and adds a region identifier.
 *
 * This job reads flight data from a Kafka topic using DataStream API, converts it to Table API,
 * processes it using SQL, and writes the processed data to another Kafka topic.
 */
class StatelessJob {
    companion object {
        private val logger = LoggerFactory.getLogger(StatelessJob::class.java)

        /**
         * Main entry point for the stateless job.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            logger.info("Starting stateless flight processing job")

            // Load configuration
            val configFile = args.getOrNull(0) ?: "application.yaml"
            logger.info("Loading configuration from {}", configFile)
            val config = FlinkConfig.load(configFile)
            logger.info("Loaded configuration from {}", configFile)
            logger.info("Kafka bootstrap servers: {}", config.kafka.bootstrapServers)
            logger.info("Kafka schema registry URL: {}", config.kafka.schemaRegistryUrl)
            logger.info("Kafka topics - flights: {}, processedFlights: {}", config.kafka.topics.flights, config.kafka.topics.processedFlights)

            // Run the job
            runJob(config)
        }

        /**
         * Runs the stateless job with the given configuration.
         *
         * @param config The Flink configuration
         */
        fun runJob(config: FlinkConfig) {
            // Set up the execution environment
            val env = setupEnvironment(config)

            // Create StreamTableEnvironment
            val tableEnv = StreamTableEnvironment.create(env)

            // Create and register the flights table
            createAndRegisterFlightsTable(tableEnv, env, config)

            // Create the processed flights table
            createProcessedFlightsTable(tableEnv, config)

            // Register custom functions
            registerCustomFunctions(tableEnv)

            // Execute the SQL query
            executeQuery(tableEnv)

            logger.info("Stateless job submitted")
        }

        /**
         * Sets up the Flink execution environment with checkpointing.
         *
         * @param config The Flink configuration
         * @return The configured StreamExecutionEnvironment
         */
        fun setupEnvironment(config: FlinkConfig): StreamExecutionEnvironment {
            val env = StreamExecutionEnvironment.getExecutionEnvironment()

            // Configure checkpointing
            env.enableCheckpointing(config.flink.checkpointing.interval)
            env.checkpointConfig.checkpointingMode = CheckpointingMode.EXACTLY_ONCE
            env.checkpointConfig.checkpointTimeout = config.flink.checkpointing.timeout
            env.checkpointConfig.minPauseBetweenCheckpoints = config.flink.checkpointing.minPause
            env.checkpointConfig.maxConcurrentCheckpoints = config.flink.checkpointing.maxConcurrent

            return env
        }

        /**
         * Creates a Kafka source for flight data.
         *
         * @param config The Flink configuration
         * @return The configured KafkaSource
         */
        fun createKafkaSource(config: FlinkConfig): KafkaSource<FlightData> {
            logger.info("Creating Kafka source using DataStream API")
            return KafkaSource.builder<FlightData>()
                .setBootstrapServers(config.kafka.bootstrapServers)
                .setTopics(config.kafka.topics.flights)
                .setGroupId(config.kafka.groupId)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(
                    ConfluentRegistryAvroDeserializationSchema.forSpecific(
                        FlightData::class.java,
                        config.kafka.schemaRegistryUrl
                    )
                )
                .build()
        }

        /**
         * Creates and registers the flights table in the TableEnvironment.
         *
         * @param tableEnv The StreamTableEnvironment
         * @param env The StreamExecutionEnvironment
         * @param config The Flink configuration
         */
        fun createAndRegisterFlightsTable(
            tableEnv: StreamTableEnvironment,
            env: StreamExecutionEnvironment,
            config: FlinkConfig
        ) {
            // Create Kafka source
            val kafkaSource = createKafkaSource(config)

            // Create a DataStream from the Kafka source
            val flightsStream: DataStream<FlightData> = env
                .fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka Source")

            // Convert DataStream to Table
            val flightsTable = tableEnv.fromDataStream(flightsStream)

            // Register the table
            tableEnv.createTemporaryView("flights", flightsTable)
        }

        /**
         * Creates the processed flights table in the TableEnvironment.
         *
         * @param tableEnv The StreamTableEnvironment
         * @param config The Flink configuration
         */
        fun createProcessedFlightsTable(tableEnv: StreamTableEnvironment, config: FlinkConfig) {
            // Define schema for processed flights table
            val processedFlightsSchema = Schema.newBuilder()
                .column("flightId", DataTypes.STRING())
                .column("callsign", DataTypes.STRING())
                .column("latitude", DataTypes.DOUBLE())
                .column("longitude", DataTypes.DOUBLE())
                .column("altitude", DataTypes.INT())
                .column("heading", DataTypes.DOUBLE())
                .column("speed", DataTypes.DOUBLE())
                .column("verticalSpeed", DataTypes.DOUBLE())
                .column("origin", DataTypes.STRING())
                .column("destination", DataTypes.STRING())
                .column("status", DataTypes.STRING())  // Use STRING for Avro enum
                .column("timestamp", DataTypes.BIGINT())  // Match Avro's long type with timestamp-millis logical type
                .column("region", DataTypes.STRING())
                .build()

            // Check if we're in a test environment (topic name starts with "test-")
            val isTestEnvironment = config.kafka.topics.processedFlights.startsWith("test-")

            // Define Kafka sink table using TableDescriptor
            val descriptorBuilder = TableDescriptor.forConnector("kafka")
                .schema(processedFlightsSchema)
                .option("topic", config.kafka.topics.processedFlights)
                .option("properties.bootstrap.servers", config.kafka.bootstrapServers)

            // Use JSON format for tests, Avro for production
            val processedFlightsSinkDescriptor = if (isTestEnvironment) {
                logger.info("Using JSON format for test environment")
                descriptorBuilder
                    .option("format", "json")
                    .build()
            } else {
                logger.info("Using Avro format for production environment")
                descriptorBuilder
                    .option("format", "avro")
                    .option("avro.schema-registry.url", config.kafka.schemaRegistryUrl)
                    .option("avro.schema-registry.subject", "${config.kafka.topics.processedFlights}-value")
                    .build()
            }

            // Create the processed_flights table
            tableEnv.createTemporaryTable("processed_flights", processedFlightsSinkDescriptor)
        }

        /**
         * Registers custom functions in the TableEnvironment.
         *
         * @param tableEnv The StreamTableEnvironment
         */
        fun registerCustomFunctions(tableEnv: StreamTableEnvironment) {
            tableEnv.createTemporarySystemFunction("calculateRegion", RegionCalculator())
            tableEnv.createTemporarySystemFunction("toString", EverythingToString())
            tableEnv.createTemporarySystemFunction("toEpochMillis", TimestampToEpochMillis())
        }

        /**
         * Executes the SQL query to filter flights and add region identifier.
         *
         * @param tableEnv The StreamTableEnvironment
         */
        fun executeQuery(tableEnv: StreamTableEnvironment) {
            tableEnv.executeSql(
                """
                INSERT INTO processed_flights
                SELECT 
                    flightId, 
                    callsign,
                    latitude, 
                    longitude,
                    altitude,
                    heading,
                    speed,
                    verticalSpeed,
                    origin,
                    destination,
                    toString(status) AS status, 
                    toEpochMillis(`timestamp`) AS `timestamp`, 
                    calculateRegion(latitude, longitude) AS region
                FROM flights
                WHERE altitude > 35000
                """
            )
        }
    }
}
