# Real-Time Flight Tracking Demo

[![Validate Stack](https://github.com/gamussa/event-driven-analytics-ktor-flink/actions/workflows/validate.yml/badge.svg)](https://github.com/gamussa/event-driven-analytics-ktor-flink/actions/workflows/validate.yml)
[![Build Generator](https://github.com/gamussa/event-driven-analytics-ktor-flink/actions/workflows/generator-build.yml/badge.svg)](https://github.com/gamussa/event-driven-analytics-ktor-flink/actions/workflows/generator-build.yml)
[![Renovate](https://img.shields.io/badge/renovate-enabled-brightgreen.svg)](https://renovatebot.com)
[![Kafka](https://img.shields.io/badge/Kafka-7.8.0-231F20?logo=apache-kafka)](https://kafka.apache.org/)
[![Flink](https://img.shields.io/badge/Flink-1.20.1-E6526F?logo=apache-flink)](https://flink.apache.org/)
[![Iceberg](https://img.shields.io/badge/Iceberg-latest-0B5394?logo=apache)](https://iceberg.apache.org/)
[![Trino](https://img.shields.io/badge/Trino-414-DD00A1?logo=trino)](https://trino.io/)
[![Ktor](https://img.shields.io/badge/Ktor-2.3+-E84B3C?logo=kotlin)](https://ktor.io/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8+-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Docker](https://img.shields.io/badge/Docker-compose-2496ED?logo=docker)](https://www.docker.com/)
[![MinIO](https://img.shields.io/badge/MinIO-S3_Storage-C72E49?logo=minio)](https://min.io/)

This project demonstrates real-time flight tracking using Apache Flink, Kotlin, Ktor, and visualization libraries. It showcases three distinct use cases:

1. **Ktor Consuming Kafka with SSE**: Direct streaming of flight data from Kafka to frontend
2. **Ktor Consuming Flink-Processed Kafka Data with SSE**: Streaming of Flink-processed data to frontend
3. **Flink to Iceberg with Ktor REST and Trino**: Persistent storage of processed data with REST API access

## 🚀 Technologies

- **Kafka**: Confluent Kafka 7.8.0 (KRaft mode without Zookeeper)
- **Flink**: Apache Flink 1.20.1
- **Iceberg**: Apache Iceberg for data storage
- **Trino**: For SQL querying of Iceberg tables
- **Ktor**: Backend server with SSE and REST endpoints
- **Kotlin/JS**: Frontend with Lets-Plot visualizations

## 📋 Prerequisites

- Docker and Docker Compose
- JDK 11+
- Gradle 7.5+

## 🔧 Setup and Running

### Initial Setup

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/real-time-flight-tracking-demo.git
   cd real-time-flight-tracking-demo
   ```

2. Initialize Kafka (required only once before first start):
   ```
   make init
   ```

3. Start all services:
   ```
   make start
   ```

### Available Commands

Run `make help` to see all available commands. Here are the most important ones:

- `make start` - Start all services using Docker Compose
- `make stop` - Stop all services
- `make restart` - Restart all services
- `make status` - Check status of all services
- `make logs` - Show logs from all services
- `make build` - Build the application
- `make test` - Run tests
- `make clean` - Clean up resources
- `make kafka-topics` - List Kafka topics
- `make kafka-ui` - Open Kafka UI in browser
- `make flink-ui` - Open Flink UI in browser
- `make trino-ui` - Open Trino UI in browser
- `make minio-ui` - Open MinIO UI in browser

## 🏗️ Architecture

The application uses a modern event-driven architecture:

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ Data Source │───>│    Kafka    │───>│    Flink    │───>│   Iceberg   │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
                         │                  │                   │
                         │                  │                   │
                         ▼                  ▼                   ▼
                   ┌─────────────────────────────────────────────┐
                   │                 Ktor Backend                 │
                   └─────────────────────────────────────────────┘
                                        │
                                        │
                                        ▼
                   ┌─────────────────────────────────────────────┐
                   │              Kotlin/JS Frontend              │
                   └─────────────────────────────────────────────┘
```

### Kafka Configuration

This project uses Confluent Kafka 7.8.0 in KRaft mode (without Zookeeper). The Kafka configuration is defined in `scripts/kafka.properties` and mounted into the Kafka container.

### Data Flow

1. **Use Case 1**:
   - Data Source → Kafka → Ktor → Frontend (SSE)

2. **Use Case 2**:
   - Data Source → Kafka → Flink → Kafka → Ktor → Frontend (SSE)

3. **Use Case 3**:
   - Data Source → Kafka → Flink → Iceberg → Trino → Ktor (with JDBC Extensions) → Frontend (REST)

## 🔍 Monitoring and Validation

### Monitoring UIs
- **Kafka UI**: http://localhost:8080
- **Flink UI**: http://localhost:8082
- **Trino UI**: http://localhost:8083
- **MinIO UI**: http://localhost:9001

### Validating Trino
To validate that Trino is working correctly and properly connected to the Iceberg catalog, run:

```
make validate-trino
```

This command will:
1. Check if the Trino container is running
2. Verify that Trino can connect to the Iceberg catalog
3. Create a test schema and table
4. Insert and query data to confirm functionality

If all steps succeed, you'll see a success message confirming that Trino is properly configured and working.

## 📊 Demo Data

The application uses synthetic flight data simulating FlightAware AeroAPI outputs. The data is processed in Avro format via Kafka and visualized using Lets-Plot.

## 🧪 Testing

Run tests using:

```
make test
```

## 🧹 Cleanup

To stop all services and clean up resources:

```
make clean
```

## 🔄 CI/CD

This project uses GitHub Actions and Renovate for continuous integration and dependency management.

### GitHub Actions

The project includes a GitHub Actions workflow that validates the stack on every push to the main branch and on pull requests:

- Initializes Kafka in KRaft mode
- Starts all services using Docker Compose
- Validates Trino connectivity and functionality
- Checks Kafka topics

You can see the workflow status in the GitHub repository.

### Renovate

Dependency management is automated using Renovate, which creates pull requests to update:

- Docker images (updated every weekend)
- Grouped dependencies by type (Flink, Kafka, Ktor, Kotlin)
- Automatically merges non-major updates

## 🔌 Running and Testing the Ktor Backend

The Ktor backend provides both Server-Sent Events (SSE) and REST endpoints for accessing flight data.

### Running the Ktor Backend

You can run the Ktor backend in several ways:

1. **Using Gradle**:
   ```bash
   ./gradlew :ktor-backend:run
   ```

2. **Using Docker Compose** (recommended for full stack):
   ```bash
   make start
   ```

3. **Using the JAR file**:
   ```bash
   ./gradlew :ktor-backend:build
   java -jar ktor-backend/build/libs/ktor-backend-0.1.0-SNAPSHOT.jar
   ```

### Configuration

The Ktor backend can be configured using environment variables:

- `PORT`: HTTP port (default: 8080)
- `HOST`: Host address (default: 0.0.0.0)
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers (default: localhost:9092)
- `SCHEMA_REGISTRY_URL`: Schema Registry URL (default: http://localhost:8081)
- `TRINO_JDBC_URL`: Trino JDBC URL (default: jdbc:trino://localhost:8083)
- `TRINO_USERNAME`: Trino username (default: trino)
- `TRINO_PASSWORD`: Trino password (default: empty)
- `TRINO_CATALOG`: Trino catalog (default: iceberg)
- `TRINO_SCHEMA`: Trino schema (default: flight_data)

### Available Endpoints

#### SSE Endpoints

- `GET /flights/sse`: Streams raw flight data from Kafka
  - Returns a stream of flight data events in JSON format
  - Example: `curl -N http://localhost:8080/flights/sse`

- `GET /processed_flights/sse`: Streams processed flight data from Kafka
  - Returns a stream of processed flight data events in JSON format
  - Example: `curl -N http://localhost:8080/processed_flights/sse`

#### REST Endpoints

- `GET /flights`: Returns a list of recent flights
  - Query parameters:
    - `limit`: Maximum number of flights to return (default: 10)
    - `minAltitude`: Minimum altitude in feet
    - `status`: Flight status (ON_TIME, DELAYED, CANCELLED, DIVERTED)
    - `timeRange`: Time range in minutes (default: 10)
  - Example: `curl http://localhost:8080/flights?limit=5&minAltitude=35000&status=ON_TIME`

- `GET /flights/regions`: Returns statistics about flight counts by region
  - Query parameters:
    - `timeRange`: Time range in minutes (default: 10)
  - Example: `curl http://localhost:8080/flights/regions?timeRange=30`

### Testing the Ktor Backend

You can test the Ktor backend using tools like curl, Postman, or a web browser:

1. **Testing SSE endpoints with curl**:
   ```bash
   curl -N http://localhost:8080/flights/sse
   ```

2. **Testing REST endpoints with curl**:
   ```bash
   curl http://localhost:8080/flights
   curl http://localhost:8080/flights/regions
   ```

3. **Testing with Postman**:
   - Import the following collection: [Flight Tracking API.postman_collection.json](https://github.com/gamussa/event-driven-analytics-ktor-flink/blob/main/postman/Flight%20Tracking%20API.postman_collection.json)
   - Or create your own requests to the endpoints listed above

4. **Testing with a web browser**:
   - For SSE: Use the EventSource API in JavaScript
   - For REST: Simply navigate to http://localhost:8080/flights or http://localhost:8080/flights/regions

## 📚 Documentation

For more detailed information, see:

- [Implementation Plan](docs/plan.md)
- [Task List](docs/tasks.md)
- [Requirements](docs/requirements.md)
