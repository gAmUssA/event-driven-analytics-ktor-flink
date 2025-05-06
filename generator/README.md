# Flight Data Generator

This application simulates flight data and publishes it to Kafka using Avro serialization and Schema Registry integration.

## Configuration

The application uses a YAML configuration file to configure the Kafka connection and simulation parameters. The configuration is loaded in the following order:

1. From the file path provided as a command-line argument
2. From the classpath resource `/generator-config.yaml` if no file path is provided or the file doesn't exist
3. Default built-in configuration if neither of the above sources are available

The configuration file has the following structure:

```yaml
kafka:
  bootstrapServers: "localhost:9092"
  schemaRegistryUrl: "http://localhost:8081"
  topic: "flights"
  clientId: "flight-data-generator"
  acks: "all"
  retries: 3
  batchSize: 16384
  lingerMs: 1
  bufferMemory: 33554432

simulation:
  numFlights: 100
  updateInterval: "PT1S"  # ISO-8601 duration format: 1 second
  speedFactor: 1.0
  initialAltitudeMin: 30000
  initialAltitudeMax: 40000
  initialLatitudeMin: 25.0
  initialLatitudeMax: 50.0
  initialLongitudeMin: -125.0
  initialLongitudeMax: -70.0
  flightStatusProbabilities:
    ON_TIME: 0.7
    DELAYED: 0.2
    CANCELLED: 0.05
    DIVERTED: 0.05
```

## Building and Running

### Using Gradle

To build the application:

```bash
./gradlew generator:shadowJar
```

To run the application:

```bash
java -jar generator/build/libs/generator-0.1.0-SNAPSHOT.jar [config-file-path]
```

If no config file path is provided, the application will try to load the configuration from the classpath. If you're running the application from a JAR file, you can include a `generator-config.yaml` file in the same directory as the JAR or in a `config` subdirectory.

### Using the Makefile

To build and run the application:

```bash
make run-generator
```

## Continuous Integration

[![Build Generator](https://github.com/gamussa/event-driven-analytics-ktor-flink/actions/workflows/generator-build.yml/badge.svg)](https://github.com/gamussa/event-driven-analytics-ktor-flink/actions/workflows/generator-build.yml)

This project uses GitHub Actions for continuous integration. The workflow automatically builds and tests the generator project on every push to the main branch and on pull requests that affect the generator code.

The CI workflow:
1. Builds the generator module
2. Runs all tests
3. Creates a Shadow JAR (fat JAR with all dependencies)
4. Uploads the JAR as a build artifact

You can view the workflow configuration in `.github/workflows/generator-build.yml`.

## Prerequisites

- Java 17 or later
- Kafka running on localhost:9092
- Schema Registry running on localhost:8081

## Data Format

The application uses Avro for data serialization. The Avro schema is defined in `src/main/avro/FlightData.avsc` and includes the following fields:

- `flightId`: Unique identifier for the flight
- `callsign`: Flight callsign
- `latitude`: Current latitude in degrees
- `longitude`: Current longitude in degrees
- `altitude`: Current altitude in feet
- `heading`: Current heading in degrees (0-360)
- `speed`: Current ground speed in knots
- `verticalSpeed`: Current vertical speed in feet per minute
- `origin`: Origin airport code
- `destination`: Destination airport code
- `status`: Current flight status (ON_TIME, DELAYED, CANCELLED, DIVERTED)
- `timestamp`: Timestamp of the data point in milliseconds since epoch

The application registers the schema with the Schema Registry, allowing consumers to retrieve the schema and deserialize the data.

## Troubleshooting

If you encounter connection errors when trying to connect to Kafka, make sure Kafka is running and accessible at the configured bootstrap servers address.

If you encounter configuration loading issues:

1. Check that the configuration file exists at the path you provided (if any)
2. Ensure the configuration file has the correct YAML format
3. If you're relying on classpath loading, make sure the `generator-config.yaml` file is in the correct location in your JAR or on the classpath
4. Check the application logs for more detailed error messages about configuration loading

If all else fails, the application will use a default configuration with:
- Kafka bootstrap servers: localhost:29092
- Schema Registry URL: http://localhost:8081
- Kafka topic: flights
- Default simulation parameters
