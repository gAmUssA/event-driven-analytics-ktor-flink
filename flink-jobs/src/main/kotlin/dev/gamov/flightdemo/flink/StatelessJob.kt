package dev.gamov.flightdemo.flink

import dev.gamov.flightdemo.avro.FlightData
import dev.gamov.flightdemo.flink.config.FlinkConfig
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroDeserializationSchema
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.datastream.DataStream
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.table.annotation.DataTypeHint
import org.apache.flink.table.annotation.InputGroup
import org.apache.flink.table.api.DataTypes
import org.apache.flink.table.api.Schema
import org.apache.flink.table.api.TableDescriptor
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment
import org.apache.flink.table.functions.ScalarFunction
import org.slf4j.LoggerFactory
import kotlin.math.floor

/**
 * Stateless Flink job that filters flights above 35,000 feet and adds a region identifier.
 *
 * This job reads flight data from a Kafka topic using DataStream API, converts it to Table API,
 * processes it using SQL, and writes the processed data to another Kafka topic.
 */
fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("StatelessJob")
    logger.info("Starting stateless flight processing job")

    // Load configuration
    val configFile = args.getOrNull(0) ?: "application.yaml"
    logger.info("Loading configuration from {}", configFile)
    val config = FlinkConfig.load(configFile)
    logger.info("Loaded configuration from {}", configFile)
    logger.info("Kafka bootstrap servers: {}", config.kafka.bootstrapServers)
    logger.info("Kafka schema registry URL: {}", config.kafka.schemaRegistryUrl)
    logger.info("Kafka topics - flights: {}, processedFlights: {}", config.kafka.topics.flights, config.kafka.topics.processedFlights)

    // Set up the execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment()

    // Configure checkpointing
    env.enableCheckpointing(config.flink.checkpointing.interval)
    env.checkpointConfig.checkpointingMode = CheckpointingMode.EXACTLY_ONCE
    env.checkpointConfig.checkpointTimeout = config.flink.checkpointing.timeout
    env.checkpointConfig.minPauseBetweenCheckpoints = config.flink.checkpointing.minPause
    env.checkpointConfig.maxConcurrentCheckpoints = config.flink.checkpointing.maxConcurrent

    // Create StreamTableEnvironment
    val tableEnv = StreamTableEnvironment.create(env)

    // Create Kafka source using DataStream API
    logger.info("Creating Kafka source using DataStream API")
    val kafkaSource = KafkaSource.builder<FlightData>()
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

    // Create a DataStream from the Kafka source
    val flightsStream: DataStream<FlightData> = env
        .fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka Source")

    // Convert DataStream to Table
    val flightsTable = tableEnv.fromDataStream(flightsStream)

    // Register the table
    tableEnv.createTemporaryView("flights", flightsTable)

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

    // Define Kafka sink table using TableDescriptor
    val processedFlightsSinkDescriptor = TableDescriptor.forConnector("kafka")
        .schema(processedFlightsSchema)
        .option("topic", config.kafka.topics.processedFlights)
        .option("properties.bootstrap.servers", config.kafka.bootstrapServers)
        .option("format", "json")
        .build()

    // Create the processed_flights table
    tableEnv.createTemporaryTable("processed_flights", processedFlightsSinkDescriptor)

    // Define a custom function for calculating the region identifier
    class RegionCalculator : ScalarFunction() {
        // Add a method with non-nullable parameters
        fun eval(latitude: Double?, longitude: Double?): String {
            if (latitude == null || longitude == null) {
                return "unknown_region"
            }
            val latRegion = floor(latitude / 5) * 5
            val lonRegion = floor(longitude / 5) * 5
            return "${latRegion.toInt()}_${lonRegion.toInt()}"
        }
    }

    class EverythingToString : ScalarFunction() {
        fun eval(@DataTypeHint(inputGroup = InputGroup.ANY) x: Any?): String {
            return x.toString()
        }
    }

    class TimestampToEpochMillis : ScalarFunction() {
        fun eval(@DataTypeHint(inputGroup = InputGroup.ANY) timestamp: Any?): Long {
            if (timestamp == null) {
                return 0L
            }
            // Extract the timestamp value directly from the FlightData object
            return when (timestamp) {
                is java.time.LocalDateTime -> java.sql.Timestamp.valueOf(timestamp).time
                is java.time.Instant -> timestamp.toEpochMilli()
                is java.sql.Timestamp -> timestamp.time
                is java.util.Date -> timestamp.time
                is Long -> timestamp
                else -> {
                    // Log the actual type for debugging
                    println("Unexpected timestamp type: ${timestamp.javaClass.name}, value: $timestamp")
                    0L
                }
            }
        }
    }

    // Register the custom function
    tableEnv.createTemporarySystemFunction("calculateRegion", RegionCalculator())
    tableEnv.createTemporarySystemFunction("toString", EverythingToString())
    tableEnv.createTemporarySystemFunction("toEpochMillis", TimestampToEpochMillis())

    // Execute SQL query to filter flights above 35,000 feet and add region identifier
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

    logger.info("Stateless job submitted")
}
