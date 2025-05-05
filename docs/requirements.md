# Product Requirements Document: Real-Time Flight Tracking Demo

## Table of Contents
- [1. Introduction](#1-introduction)
  - [1.1 Purpose](#11-purpose)
  - [1.2 Scope](#12-scope)
  - [1.3 Audience](#13-audience)
- [2. Use Cases](#2-use-cases)
  - [2.1 Use Case 1: Ktor Consuming Kafka with SSE](#21-use-case-1-ktor-consuming-kafka-with-sse)
  - [2.2 Use Case 2: Ktor Consuming Flink-Processed Kafka Data with SSE](#22-use-case-2-ktor-consuming-flink-processed-kafka-data-with-sse)
  - [2.3 Use Case 3: Flink to Iceberg with Ktor REST and Trino](#23-use-case-3-flink-to-iceberg-with-ktor-rest-and-trino)
- [3. Technical Requirements](#3-technical-requirements)
  - [3.1 Technologies](#31-technologies)
  - [3.2 Data Schema](#32-data-schema)
  - [3.3 Non-Functional Requirements](#33-non-functional-requirements)
- [4. User Experience](#4-user-experience)
  - [4.1 Frontend Interface](#41-frontend-interface)
  - [4.2 User Flow](#42-user-flow)
- [5. Constraints and Assumptions](#5-constraints-and-assumptions)
  - [5.1 Constraints](#51-constraints)
  - [5.2 Assumptions](#52-assumptions)
- [6. Success Metrics](#6-success-metrics)
- [7. Risks and Mitigation](#7-risks-and-mitigation)
- [8. Future Enhancements](#8-future-enhancements)
- [9. References](#9-references)

## 1. Introduction

### 1.1 Purpose
This Product Requirements Document (PRD) outlines the requirements for a demo application showcasing real-time flight tracking using Apache Flink, Kotlin, Ktor, and open-source visualization libraries. The demo targets Kotlin developers and streaming data engineers, demonstrating three distinct use cases leveraging the FlightAware AeroAPI, Kafka with Avro format, and interactive visualizations. This updated version specifies that Flink jobs use only the Table API and SQL, and Ktor uses JetBrains Exposed for database interactions where applicable.

### 1.2 Scope
The demo includes three use cases:

- **Ktor Consuming Kafka with SSE**: Ktor consumes flight data from Kafka and streams it to a frontend via Server-Sent Events (SSE) for visualization.
- **Ktor Consuming Flink-Processed Kafka Data with SSE**: Ktor consumes processed flight data from Flink via Kafka and streams it to a frontend via SSE for visualization.
- **Flink to Iceberg with Ktor REST and Trino**: Flink processes flight data from Kafka using stateless and stateful Table API/SQL queries, saves results to an Iceberg table, and Ktor provides a REST endpoint using Exposed to query the Iceberg table via Trino for frontend visualization.

The demo uses synthetic flight data simulating FlightAware AeroAPI outputs, processed in Avro format via Kafka, and visualized using Lets-Plot.

### 1.3 Audience

- **Kotlin Developers**: Interested in full-stack Kotlin applications using Ktor, Exposed, and Kotlin/JS.
- **Streaming Data Engineers**: Focused on real-time data processing with Flink's Table API/SQL, Kafka, and data lakes like Iceberg.

## 2. Use Cases

### 2.1 Use Case 1: Ktor Consuming Kafka with SSE
**Description**: Ktor consumes raw flight data from a Kafka topic in Avro format and streams it to a frontend via Server-Sent Events (SSE) for real-time visualization.

**Requirements**:

- **Data Source**:
  - Kafka topic (flights) with Avro-serialized flight data (e.g., flight ID, latitude, longitude, altitude, status, timestamp).
  - Synthetic data simulating FlightAware AeroAPI output.

- **Backend**:
  - Ktor server consumes Kafka topic using a Kafka consumer.
  - Ktor deserializes Avro data with Confluent Schema Registry.
  - Ktor streams data to frontend via SSE endpoint (/flights/sse).

- **Frontend**:
  - Web frontend built with Kotlin/JS and Lets-Plot.
  - Subscribes to SSE endpoint to receive real-time flight updates.
  - Visualizes flight positions on a map with color-coded markers (e.g., green for on-time, red for delayed).
  - Displays a bar chart for flight status distribution (e.g., on-time vs. delayed).

- **Non-Functional**:
  - SSE updates every 1-2 seconds for smooth visualization.
  - Handle up to 100 concurrent frontend connections.
  - Kafka consumer lag < 5 seconds.

**Success Criteria**:

- Frontend displays real-time flight positions and statuses with minimal latency.
- Visualizations are interactive and update seamlessly via SSE.

### 2.2 Use Case 2: Ktor Consuming Flink-Processed Kafka Data with SSE
**Description**: Flink processes flight data from Kafka using Table API/SQL, produces aggregated or filtered data to another Kafka topic, and Ktor streams this processed data to a frontend via SSE for visualization.

**Requirements**:

- **Data Source**:
  - Input Kafka topic (flights) with Avro-serialized flight data.
  - Output Kafka topic (processed_flights) for Flink-processed data.

- **Stream Processing**:
  - Flink consumes flights topic using Table API/SQL and performs:
    - Windowed aggregation (e.g., flight count per 5x5-degree region every minute) using SQL TUMBLE window.
    - Filtering for delayed flights using SQL WHERE clause.
  - Flink outputs results to processed_flights topic in Avro format.

- **Backend**:
  - Ktor consumes processed_flights topic using a Kafka consumer.
  - Ktor deserializes Avro data with Confluent Schema Registry.
  - Ktor streams aggregated and delayed flight data to frontend via SSE endpoint (/processed_flights/sse).

- **Frontend**:
  - Web frontend built with Kotlin/JS and Lets-Plot.
  - Subscribes to SSE endpoint for processed data updates.
  - Visualizes flight density on a map (e.g., heatmap or marker size based on count).
  - Displays a line chart for delayed flight trends over time.

- **Non-Functional**:
  - Flink processes data with < 10-second latency.
  - SSE updates every 1-2 seconds.
  - Handle up to 100 concurrent frontend connections.
  - Kafka consumer lag < 5 seconds.

**Success Criteria**:

- Frontend accurately visualizes aggregated and filtered flight data.
- Visualizations reflect real-time processing with low latency.

### 2.3 Use Case 3: Flink to Iceberg with Ktor REST and Trino
**Description**: Flink processes flight data from Kafka using stateless and stateful Table API/SQL queries, saves results to an Apache Iceberg table, and Ktor provides a REST endpoint using JetBrains Exposed to query the Iceberg table via Trino for frontend visualization.

**Requirements**:

- **Data Source**:
  - Kafka topic (flights) with Avro-serialized flight data.

- **Stream Processing**:
  - Flink consumes flights topic using Table API/SQL and performs two types of queries:
    - **Stateless Query**: SQL query to filter flights with altitude above 35,000 feet and transform data to include a region identifier (e.g., `CONCAT(CAST(FLOOR(latitude / 5) * 5 AS STRING), '_', CAST(FLOOR(longitude / 5) * 5 AS STRING))`).
    - **Stateful Query**: SQL query to aggregate flight count and average altitude per 5x5-degree region over a 1-minute tumbling window using TUMBLE function.
  - Flink writes results to an Iceberg table (flight_data) with schema supporting both query outputs.
  - Flink supports incremental updates to Iceberg with checkpointing.

- **Backend**:
  - Trino server with Iceberg catalog for querying flight_data table.
  - Ktor server uses JetBrains Exposed framework to connect to Trino via JDBC driver.
  - Ktor provides REST endpoint (/flights) for querying recent flight data (e.g., last 10 minutes) using Exposed's DSL for SQL queries.
  - Optional query parameters for filtering (e.g., status, region, altitude).

- **Frontend**:
  - Web frontend built with Kotlin/JS and Lets-Plot.
  - Periodically fetches data from REST endpoint (e.g., every 5 seconds).
  - Visualizes flight positions on a map with color-coded markers (e.g., blue for high-altitude flights from stateless query).
  - Displays a table for recent flight statuses and a bar chart for aggregated flight counts by region (from stateful query).

- **Non-Functional**:
  - Flink writes to Iceberg with < 15-second latency.
  - REST endpoint responds in < 1 second for queries.
  - Handle up to 50 concurrent REST requests.
  - Iceberg table supports queries for up to 1 hour of data.

**Success Criteria**:

- Frontend displays accurate, near-real-time flight data from Iceberg, reflecting both stateless and stateful query results.
- REST endpoint provides flexible querying using Exposed with fast response times.
- Demo clearly showcases Flink Table API/SQL for stateless and stateful use cases.

## 3. Technical Requirements

### 3.1 Technologies

- **Data Source**: Synthetic flight data simulating FlightAware AeroAPI.
- **Messaging**: Apache Kafka with Avro format and Confluent Schema Registry.
- **Stream Processing**: Apache Flink with Kotlin and Table API/SQL for processing Kafka data.
- **Data Lake**: Apache Iceberg for persistent storage (Use Case 3).
- **Query Engine**: Trino with JDBC driver for Iceberg queries (Use Case 3).
- **Backend**: Ktor with JetBrains Exposed for database interactions via Trino JDBC.
- **Frontend**: Kotlin/JS with Lets-Plot for map and chart visualizations.
- **Dependencies**:
  - flink-kotlin for idiomatic Flink APIs.
  - kafka-avro-serializer for Avro serialization.
  - ktor-server-netty for Ktor server.
  - org.jetbrains.exposed:exposed-core and org.jetbrains.exposed:exposed-jdbc for database interactions.
  - lets-plot-kotlin for visualizations.
  - flink-sql-connector-kafka and flink-sql-connector-iceberg for Flink SQL.

### 3.2 Data Schema

- **Avro Schema (FlightData)**:
```json
{
  "type": "record",
  "name": "FlightData",
  "fields": [
    {"name": "flight_id", "type": "string"},
    {"name": "latitude", "type": "double"},
    {"name": "longitude", "type": "double"},
    {"name": "altitude", "type": "int"},
    {"name": "status", "type": "string"},
    {"name": "timestamp", "type": "long"}
  ]
}
```

- **Iceberg Table Schema (flight_data, Use Case 3)**:
  - Columns: flight_id (string), latitude (double), longitude (double), altitude (int), status (string), timestamp (timestamp), region (string, from stateless query), flight_count (int, from stateful query), avg_altitude (double, from stateful query), processed_time (timestamp).

### 3.3 Non-Functional Requirements

- **Performance**:
  - End-to-end latency (Kafka to frontend) < 15 seconds for all use cases.
  - Visualization refresh rate: 1-5 seconds.

- **Scalability**:
  - Handle up to 1,000 flights per minute.
  - Support 100 concurrent frontend clients (Use Cases 1, 2) or 50 REST requests (Use Case 3).

- **Reliability**:
  - Flink checkpointing for fault tolerance in stateful queries.
  - Kafka consumer retries for transient failures.
  - Exposed handles database connection pooling for reliable Trino queries.

- **Deployment: Ascertainability**:
  - Local deployment for demo (Flink, Kafka, Trino, Ktor, browser).
  - Docker Compose for easy setup.

## 4. User Experience

### 4.1 Frontend Interface

- **Map Visualization**:
  - World map showing flight positions with markers.
  - Markers color-coded by status (Use Cases 1, 2) or altitude/region (Use Case 3, stateless query).
  - Zoom and pan support.

- **Charts and Tables**:
  - Bar chart for flight status distribution (Use Case 1).
  - Line chart for delayed flight trends (Use Case 2).
  - Table for recent flights and bar chart for flight counts by region (Use Case 3, stateful query).

- **Interactivity**:
  - Hover on markers to show flight details (ID, altitude, status, region).
  - Filter options for status, region, or altitude (Use Case 3).

### 4.2 User Flow

- User opens the web frontend in a browser.
- Frontend connects to SSE (Use Cases 1, 2) or polls REST endpoint (Use Case 3).
- Visualizations update in real-time or near-real-time as new data arrives.
- User interacts with map, charts, and tables to explore flight data.

## 5. Constraints and Assumptions

### 5.1 Constraints

- **FlightAware AeroAPI**: Simulated data due to API key and rate limits.
- **Visualization**: Lets-Plot's map support is basic; advanced features may require Leaflet.js integration.
- **Local Deployment**: Demo runs locally, not in a production cloud environment.
- **Exposed with Trino**: Exposed's JDBC support must be compatible with Trino's query capabilities for Iceberg.

### 5.2 Assumptions

- Kafka, Flink, Trino, and Iceberg are pre-installed and configured.
- Users have basic familiarity with Kotlin and streaming concepts.
- Frontend runs in a modern browser (Chrome, Firefox).
- Exposed can effectively query Iceberg tables via Trino JDBC.

## 6. Success Metrics

- **Technical**:
  - All use cases process and visualize data with < 15-second end-to-end latency.
  - Use Case 3 demonstrates both stateless and stateful Flink Table API/SQL queries effectively.
  - Ktor with Exposed provides reliable and performant database queries.
  - Visualizations update smoothly without noticeable lag.

- **Audience Engagement**:
  - Kotlin developers can replicate the demo using provided code.
  - Streaming data engineers recognize Flink's Table API/SQL and Exposed's capabilities for diverse use cases.

- **Demo Quality**:
  - Frontend is visually appealing and interactive.
  - Code is well-documented and modular.

## 7. Risks and Mitigation

- **Risk**: Complex setup (Kafka, Flink, Trino, Iceberg) may deter users.
  - **Mitigation**: Provide Docker Compose file and clear setup instructions.

- **Risk**: Lets-Plot's map limitations reduce visualization quality.
  - **Mitigation**: Document fallback to Leaflet.js if needed.

- **Risk**: Simulated data lacks realism compared to FlightAware API.
  - **Mitigation**: Ensure synthetic data mimics real flight patterns.

- **Risk**: Exposed's compatibility with Trino JDBC may have limitations.
  - **Mitigation**: Test Exposed thoroughly with Trino queries; fallback to raw JDBC if needed.

- **Risk**: Stateful query performance may degrade with large windows.
  - **Mitigation**: Optimize window size and use Flink's state management features.

## 8. Future Enhancements

- Integrate real FlightAware AeroAPI data with proper authentication.
- Add advanced analytics (e.g., flight path prediction) in Flink Table API/SQL.
- Enhance visualizations with animated flight paths or 3D views.
- Deploy demo to a cloud environment for scalability testing.
- Explore Exposed's advanced features for more complex Trino queries.

## 9. References

- FlightAware AeroAPI: https://www.flightaware.com/aeroapi/portal
- Apache Flink Documentation: https://flink.apache.org/
- Apache Kafka Documentation: https://kafka.apache.org/
- Apache Iceberg Documentation: https://iceberg.apache.org/
- Trino Documentation: https://trino.io/
- Ktor Documentation: https://ktor.io/
- JetBrains Exposed GitHub: https://github.com/JetBrains/Exposed
- Lets-Plot Kotlin GitHub: https://github.com/JetBrains/lets-plot-kotlin
- Confluent Schema Registry: https://docs.confluent.io/platform/current/schema-registry/index.html
