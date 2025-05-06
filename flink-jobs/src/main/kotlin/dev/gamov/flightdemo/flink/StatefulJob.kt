package dev.gamov.flightdemo.flink

import dev.gamov.flightdemo.flink.config.FlinkConfig
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.table.api.*
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment
import org.apache.flink.api.common.typeinfo.Types
import org.apache.flink.types.Row
import org.apache.flink.table.functions.ScalarFunction
import org.slf4j.LoggerFactory

/**
 * Stateful Flink job that aggregates flight data by region over 1-minute windows.
 * 
 * This job reads flight data from a Kafka topic, processes it using Flink Table API and SQL,
 * and writes the aggregated data to an Iceberg table.
 */
fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("StatefulJob")
    logger.info("Starting stateful flight processing job")

    // Load configuration
    val configFile = args.getOrNull(0) ?: "application.yaml"
    val config = FlinkConfig.load(configFile)
    logger.info("Loaded configuration from {}", configFile)

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

    // Configure Iceberg catalog
    tableEnv.executeSql("""
        CREATE CATALOG ${config.iceberg.catalog.name} WITH (
            'type' = '${config.iceberg.catalog.type}',
            'catalog-impl' = 'org.apache.iceberg.rest.RESTCatalog',
            'uri' = '${config.iceberg.catalog.uri}',
            'warehouse' = '${config.iceberg.catalog.warehouse}'
        )
    """)

    // Use the Iceberg catalog
    tableEnv.useCatalog(config.iceberg.catalog.name)

    // Create Iceberg table if it doesn't exist
    tableEnv.executeSql("""
        CREATE TABLE IF NOT EXISTS ${config.iceberg.tables.flightAggregates.name} (
            region STRING,
            window_start TIMESTAMP(3),
            window_end TIMESTAMP(3),
            flight_count BIGINT,
            avg_altitude DOUBLE,
            PRIMARY KEY (region, window_start) NOT ENFORCED
        ) PARTITIONED BY (days(window_start))
    """)

    // Define schema for flights table
    val flightsSchema = Schema.newBuilder()
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
        .column("status", DataTypes.STRING())
        .column("timestamp", DataTypes.TIMESTAMP(3))
        .watermark("timestamp", "timestamp - INTERVAL '5' SECOND")
        .build()

    // Define Kafka source table using TableDescriptor
    val flightsSourceDescriptor = TableDescriptor.forConnector("kafka")
        .schema(flightsSchema)
        .option("topic", config.kafka.topics.flights)
        .option("properties.bootstrap.servers", config.kafka.bootstrapServers)
        .option("properties.group.id", config.kafka.groupId)
        .option("scan.startup.mode", "earliest-offset")
        .option("format", "avro-confluent")
        .option("avro-confluent.schema-registry.url", config.kafka.schemaRegistryUrl)
        .build()

    // Create the flights table
    tableEnv.createTemporaryTable("flights", flightsSourceDescriptor)

    // Define a custom function for calculating the region identifier
    class RegionCalculator : ScalarFunction() {
        fun eval(latitude: Double, longitude: Double): String {
            val latRegion = Math.floor(latitude / 5) * 5
            val lonRegion = Math.floor(longitude / 5) * 5
            return "${latRegion.toInt()}_${lonRegion.toInt()}"
        }
    }

    // Register the custom function
    tableEnv.createTemporarySystemFunction("calculateRegion", RegionCalculator())

    // Execute SQL query for windowed aggregation
    tableEnv.executeSql("""
        INSERT INTO ${config.iceberg.tables.flightAggregates.name}
        SELECT 
            calculateRegion(latitude, longitude) AS region,
            window_start,
            window_end,
            COUNT(*) AS flight_count,
            AVG(altitude) AS avg_altitude
        FROM TABLE(
            TUMBLE(TABLE flights, DESCRIPTOR(timestamp), INTERVAL '1' MINUTE)
        )
        GROUP BY 
            calculateRegion(latitude, longitude),
            window_start,
            window_end
    """)

    logger.info("Stateful job submitted")
}
