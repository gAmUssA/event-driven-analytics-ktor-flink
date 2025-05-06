# Flink Table API Jobs

This module contains Flink Table API and SQL jobs for processing flight data.

## Overview

The module includes two Flink jobs:

1. **Stateless Job**: Filters flights above 35,000 feet and adds a region identifier.
2. **Stateful Job**: Aggregates flight data by region over 1-minute windows.

## Configuration

The jobs are configured using a YAML file. The default configuration file is `application.yaml` in the resources directory. You can provide a custom configuration file as a command-line argument.

### Configuration Properties

- **Kafka**:
  - `bootstrap.servers`: Kafka bootstrap servers
  - `schema.registry.url`: Schema Registry URL
  - `group.id`: Consumer group ID
  - `auto.offset.reset`: Auto offset reset policy
  - `topics.flights`: Topic for raw flight data
  - `topics.processed_flights`: Topic for processed flight data

- **Flink**:
  - `checkpointing.interval`: Checkpointing interval in milliseconds
  - `checkpointing.timeout`: Checkpointing timeout in milliseconds
  - `checkpointing.min-pause`: Minimum pause between checkpoints in milliseconds
  - `checkpointing.max-concurrent`: Maximum number of concurrent checkpoints
  - `state.backend`: State backend type (e.g., "rocksdb")
  - `state.dir`: Directory for state backend

- **Iceberg**:
  - `catalog.name`: Iceberg catalog name
  - `catalog.type`: Catalog type (e.g., "rest")
  - `catalog.uri`: Catalog URI
  - `catalog.warehouse`: Warehouse location
  - `tables.flight_data.name`: Table name for flight data
  - `tables.flight_data.partitioning`: Partitioning expression for flight data
  - `tables.flight_aggregates.name`: Table name for flight aggregates
  - `tables.flight_aggregates.partitioning`: Partitioning expression for flight aggregates

## Running the Jobs Locally

### Stateless Job

The stateless job reads flight data from a Kafka topic, filters flights above 35,000 feet, adds a region identifier, and writes the processed data to another Kafka topic.

```bash
./gradlew :flink-jobs:runStatelessJob
```

### Stateful Job

The stateful job reads flight data from a Kafka topic, aggregates it by region over 1-minute windows, and writes the aggregated data to an Iceberg table.

```bash
./gradlew :flink-jobs:runStatefulJob
```

### Custom Configuration

You can provide a custom configuration file as a command-line argument:

```bash
./gradlew :flink-jobs:runStatelessJob --args="/path/to/custom-config.yaml"
./gradlew :flink-jobs:runStatefulJob --args="/path/to/custom-config.yaml"
```

## Building the JAR

To build a fat JAR containing all dependencies:

```bash
./gradlew :flink-jobs:shadowJar
```

The JAR will be created in `flink-jobs/build/libs/`.

## Running the JAR

To run the JAR:

```bash
java -jar flink-jobs/build/libs/flink-jobs-0.1.0-SNAPSHOT.jar [config-file]
```

By default, the stateless job will be executed. To run the stateful job:

```bash
java -cp flink-jobs/build/libs/flink-jobs-0.1.0-SNAPSHOT.jar dev.gamov.flightdemo.flink.StatefulJobKt [config-file]
```

## Connecting to the Flink Docker Cluster

The project includes a Docker Compose setup with a Flink cluster. To connect to the Flink cluster and test your jobs:

1. Start the Docker Compose environment:

```bash
docker-compose up -d
```

2. Access the Flink Dashboard:
   - Open your browser and navigate to http://localhost:8082
   - This will show you the Flink JobManager UI where you can monitor jobs

3. Submit a job to the Flink cluster:

```bash
# Build the job JAR
./gradlew :flink-jobs:shadowJar

# Submit the job to the Flink cluster
docker exec -it jobmanager flink run -d \
  -c dev.gamov.flightdemo.flink.StatelessJobKt \
  /opt/flink/usrlib/flink-jobs-0.1.0-SNAPSHOT.jar
```

4. To submit the stateful job:

```bash
docker exec -it jobmanager flink run -d \
  -c dev.gamov.flightdemo.flink.StatefulJobKt \
  /opt/flink/usrlib/flink-jobs-0.1.0-SNAPSHOT.jar
```

5. Copy the JAR to the Flink cluster:

```bash
# First, create the usrlib directory if it doesn't exist
docker exec -it jobmanager mkdir -p /opt/flink/usrlib

# Copy the JAR to the Flink cluster
docker cp flink-jobs/build/libs/flink-jobs-0.1.0-SNAPSHOT.jar jobmanager:/opt/flink/usrlib/
```

## Testing the Jobs

To test the jobs, you need to:

1. Ensure the Docker Compose environment is running:

```bash
docker-compose up -d
```

2. Generate some test data using the generator module:

```bash
./gradlew :generator:run
```

3. Monitor the job execution in the Flink Dashboard (http://localhost:8082)

4. Check the output:
   - For the stateless job: Use Kafka UI (http://localhost:8080) to view messages in the `processed_flights` topic
   - For the stateful job: Use Trino to query the Iceberg table:

```bash
# Connect to Trino
docker exec -it trino-coordinator trino

# In the Trino CLI
USE iceberg.default;
SELECT * FROM flight_aggregates ORDER BY window_start DESC LIMIT 10;
```

5. To cancel a running job:

```bash
# List running jobs
docker exec -it jobmanager flink list

# Cancel a job (replace JOB_ID with the actual job ID)
docker exec -it jobmanager flink cancel JOB_ID
```

## Using Table API DSL Instead of SQL Strings

The current implementation uses SQL strings for defining tables and queries. To use the Table API DSL instead, follow these guidelines:

### 1. Import Required Classes

Import the necessary classes for Table API DSL:
- `org.apache.flink.table.api.*` - Core Table API classes
- `org.apache.flink.table.api.bridge.java.StreamTableEnvironment` - For streaming Table API
- `org.apache.flink.api.common.typeinfo.Types` - For type information
- `org.apache.flink.streaming.api.datastream.DataStream` - For DataStream API integration
- `org.apache.flink.types.Row` - For row-based operations
- `org.apache.flink.table.api.Expressions.*` - For DSL expressions like col(), lit(), etc.

### 2. Create StreamTableEnvironment

Use StreamTableEnvironment instead of TableEnvironment for better integration with DataStream API:

```
val tableEnv = StreamTableEnvironment.create(env)
```

### 3. Define Schema Using Schema Builder

Define your schema using the Schema Builder API instead of SQL DDL:

```
val schema = Schema.newBuilder()
    .column("flightId", DataTypes.STRING())
    .column("callsign", DataTypes.STRING())
    // Add all other columns
    .watermark("timestamp", "timestamp - INTERVAL '5' SECOND")
    .build()
```

### 4. Create Tables Using TableDescriptor

Create tables using TableDescriptor instead of SQL CREATE TABLE statements:

```
// For source table
val sourceDescriptor = TableDescriptor.forConnector("kafka")
    .schema(schema)
    .option("topic", "your_topic")
    // Add all other options
    .build()

val sourceTable = tableEnv.from(sourceDescriptor)

// For sink table
val sinkDescriptor = TableDescriptor.forConnector("kafka")
    .schema(schema)
    .option("topic", "your_sink_topic")
    // Add all other options
    .build()

tableEnv.createTemporaryTable("sink_table_name", sinkDescriptor)
```

### 5. Use Table API for Queries

Use Table API methods instead of SQL queries:

- For filtering: `table.filter(condition)`
- For projection: `table.select(columns)`
- For aggregation: `table.groupBy(keys).select(aggregates)`
- For joining: `table1.join(table2).where(condition)`

Example for filtering flights above 35,000 feet:

```
val filteredTable = sourceTable.filter("altitude > 35000")
```

### 6. Define Custom Functions

Define custom functions as classes extending ScalarFunction, TableFunction, or AggregateFunction:

```
class RegionCalculator extends ScalarFunction {
    def eval(latitude: Double, longitude: Double): String = {
        // Calculate region
    }
}

// Register the function
tableEnv.createTemporarySystemFunction("calculateRegion", new RegionCalculator())
```

### 7. For Windowed Aggregations

Use the window API for windowed aggregations:

```
val windowedTable = sourceTable
    .window([WindowSpec])
    .groupBy("key", "w")
    .select("key, window_start, window_end, aggregations")
```

By using the Table API DSL instead of SQL strings, you get better type safety, IDE support, and easier refactoring.
