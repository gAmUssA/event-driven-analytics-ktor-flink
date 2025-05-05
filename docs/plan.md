# Implementation Plan: Real-Time Flight Tracking Demo

## Table of Contents
- [1. Project Overview](#1-project-overview)
- [2. Architecture](#2-architecture)
- [3. Component Breakdown](#3-component-breakdown)
- [4. Implementation Phases](#4-implementation-phases)
- [5. Technical Specifications](#5-technical-specifications)
- [6. Testing Strategy](#6-testing-strategy)
- [7. Deployment Instructions](#7-deployment-instructions)
- [8. Timeline](#8-timeline)

## 1. Project Overview

This implementation plan outlines the approach for developing a real-time flight tracking demo application that showcases the integration of Apache Flink, Kotlin, Ktor, and visualization libraries. The project will demonstrate three distinct use cases:

1. **Ktor Consuming Kafka with SSE**: Direct streaming of flight data from Kafka to frontend
2. **Ktor Consuming Flink-Processed Kafka Data with SSE**: Streaming of Flink-processed data to frontend
3. **Flink to Iceberg with Ktor REST and Trino**: Persistent storage of processed data with REST API access

The application will use synthetic flight data simulating FlightAware AeroAPI outputs, process it using Kafka with Avro format, and visualize it using Lets-Plot.

## 2. Architecture

### High-Level Architecture

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

### Data Flow

1. **Use Case 1**:
   - Data Source → Kafka → Ktor → Frontend (SSE)

2. **Use Case 2**:
   - Data Source → Kafka → Flink → Kafka → Ktor → Frontend (SSE)

3. **Use Case 3**:
   - Data Source → Kafka → Flink → Iceberg → Trino → Ktor (with Exposed) → Frontend (REST)

## 3. Component Breakdown

### 3.1 Data Generation

- **Flight Data Generator**
  - Simulates FlightAware AeroAPI output
  - Produces Avro-serialized flight data to Kafka
  - Configurable rate and flight patterns

### 3.2 Kafka Infrastructure

- **Topics**:
  - `flights`: Raw flight data
  - `processed_flights`: Flink-processed flight data
- **Schema Registry**:
  - Manages Avro schemas for flight data

### 3.3 Flink Processing

- **Table API/SQL Jobs**:
  - Stateless filtering and transformation
  - Stateful windowed aggregation
  - Kafka source and sink connectors
  - Iceberg sink connector

### 3.4 Data Storage

- **Iceberg Tables**:
  - `flight_data`: Stores processed flight information
  - Partitioning by time for efficient queries

### 3.5 Ktor Backend

- **Endpoints**:
  - SSE endpoints for streaming data
  - REST endpoints for querying Iceberg via Trino
- **Kafka Integration**:
  - Consumers for raw and processed flight data
- **Database Integration**:
  - Exposed for Trino JDBC queries

### 3.6 Frontend

- **Kotlin/JS Application**:
  - Map visualization with flight markers
  - Charts for flight statistics
  - SSE and REST API clients

## 4. Implementation Phases

### Phase 1: Infrastructure Setup

1. Set up Kafka and Schema Registry
2. Set up Flink cluster
3. Set up Iceberg and Trino
4. Configure Docker Compose for local development

### Phase 2: Data Pipeline Implementation

1. Implement flight data generator
2. Create Avro schema and register with Schema Registry
3. Implement Flink Table API/SQL jobs
4. Configure Iceberg tables and Trino catalog

### Phase 3: Backend Development

1. Set up Ktor project structure
2. Implement Kafka consumer integration
3. Create SSE endpoints
4. Implement Exposed integration with Trino
5. Create REST endpoints

### Phase 4: Frontend Development

1. Set up Kotlin/JS project
2. Implement map visualization with Lets-Plot
3. Create charts and tables for flight data
4. Implement SSE and REST API clients

### Phase 5: Integration and Testing

1. Integrate all components
2. Perform end-to-end testing
3. Optimize performance
4. Document the application

## 5. Technical Specifications

### 5.1 Data Source

- **Flight Data Generator**:
  - Kotlin application using `kotlinx.coroutines` for concurrent data generation
  - Configurable flight patterns and update frequency
  - Integration with Confluent Kafka client and Avro serialization

### 5.2 Kafka Configuration

- **Version**: Apache Kafka 3.4+
- **Topics**:
  - `flights`: 8 partitions, retention 1 hour
  - `processed_flights`: 4 partitions, retention 1 hour
- **Schema Registry**:
  - Confluent Schema Registry 7.3+
  - Compatibility mode: BACKWARD

### 5.3 Flink Jobs

- **Version**: Apache Flink 1.17+
- **Job 1: Stateless Processing**
  - Table API/SQL query for filtering and transformation
  - Input: Kafka topic `flights`
  - Output: Kafka topic `processed_flights` and/or Iceberg table
  - SQL: Filter flights above 35,000 feet and add region identifier

- **Job 2: Stateful Processing**
  - Table API/SQL query for windowed aggregation
  - Input: Kafka topic `flights`
  - Output: Iceberg table
  - SQL: Aggregate flight count and average altitude per region over 1-minute windows

### 5.4 Iceberg and Trino

- **Iceberg Version**: 1.3+
- **Table Schema**:
  ```
  CREATE TABLE flight_data (
    flight_id STRING,
    latitude DOUBLE,
    longitude DOUBLE,
    altitude INT,
    status STRING,
    timestamp TIMESTAMP,
    region STRING,
    flight_count INT,
    avg_altitude DOUBLE,
    processed_time TIMESTAMP
  )
  PARTITIONED BY (days(timestamp))
  ```
- **Trino Version**: 414+
- **Catalog Configuration**:
  - Iceberg connector for Trino
  - JDBC connection parameters for Exposed

### 5.5 Ktor Backend

- **Version**: Ktor 2.3+
- **Modules**:
  - `io.ktor:ktor-server-netty`: Server engine
  - `io.ktor:ktor-server-content-negotiation`: Content negotiation
  - `io.ktor:ktor-serialization-jackson`: JSON serialization
  - `io.ktor:ktor-server-cors`: CORS support
  - `io.ktor:ktor-server-call-logging`: Request logging

- **Kafka Integration**:
  - `org.apache.kafka:kafka-clients`: Kafka consumer
  - `io.confluent:kafka-avro-serializer`: Avro serialization

- **Database Integration**:
  - `org.jetbrains.exposed:exposed-core`: Core Exposed functionality
  - `org.jetbrains.exposed:exposed-jdbc`: JDBC support
  - `io.trino:trino-jdbc`: Trino JDBC driver

- **Endpoints**:
  - `/flights/sse`: SSE endpoint for raw flight data
  - `/processed_flights/sse`: SSE endpoint for processed flight data
  - `/flights`: REST endpoint for querying Iceberg via Trino

### 5.6 Frontend

- **Kotlin/JS**:
  - Kotlin 1.8+
  - Kotlin/JS Gradle plugin

- **Dependencies**:
  - `org.jetbrains.lets-plot:lets-plot-kotlin`: Visualization library
  - `org.jetbrains.kotlinx:kotlinx-coroutines-core-js`: Coroutines support
  - `org.jetbrains.kotlinx:kotlinx-serialization-json`: JSON serialization

- **Features**:
  - Map visualization with flight markers
  - Bar and line charts for flight statistics
  - Real-time updates via SSE
  - Data fetching via REST API

## 6. Testing Strategy

### 6.1 Unit Testing

- **Backend**:
  - Test Ktor routes with `io.ktor:ktor-server-test-host`
  - Test Exposed queries with in-memory H2 database
  - Test Kafka consumers with embedded Kafka

- **Flink Jobs**:
  - Test Table API/SQL queries with `org.apache.flink:flink-table-test-utils`
  - Test Kafka connectors with embedded Kafka

### 6.2 Integration Testing

- **Data Pipeline**:
  - End-to-end tests with Docker Compose
  - Verify data flow from generator to Kafka to Flink to Iceberg

- **Backend-Frontend Integration**:
  - Test SSE endpoints with WebSocket client
  - Test REST endpoints with HTTP client

### 6.3 Performance Testing

- **Latency Testing**:
  - Measure end-to-end latency from data generation to frontend visualization
  - Target: < 15 seconds

- **Throughput Testing**:
  - Test with 1,000 flights per minute
  - Test with 100 concurrent frontend clients

## 7. Deployment Instructions

### 7.1 Local Development

1. Clone the repository
2. Run `docker-compose up` to start Kafka, Flink, Iceberg, and Trino
3. Build and run the Ktor backend
4. Build and run the Kotlin/JS frontend

### 7.2 Docker Compose Configuration

```yaml
# Docker Compose configuration will include:
# - Kafka and Schema Registry
# - Flink JobManager and TaskManager
# - Iceberg catalog
# - Trino coordinator and worker
# - Data generator
```

### 7.3 Production Deployment Considerations

- Kubernetes deployment for scalability
- Monitoring with Prometheus and Grafana
- Logging with ELK stack
- CI/CD pipeline with GitHub Actions

## 8. Timeline

| Phase | Duration | Deliverables |
|-------|----------|--------------|
| Infrastructure Setup | 1 week | Docker Compose, configured services |
| Data Pipeline Implementation | 2 weeks | Data generator, Flink jobs |
| Backend Development | 2 weeks | Ktor application with endpoints |
| Frontend Development | 2 weeks | Kotlin/JS application with visualizations |
| Integration and Testing | 1 week | Integrated application, test reports |
| Documentation and Finalization | 1 week | User guide, API documentation |

**Total Duration**: 9 weeks