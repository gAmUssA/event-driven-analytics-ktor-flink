# Development Guidelines for Real-Time Flight Tracking Demo

## Table of Contents
- [1. General Coding Standards](#1-general-coding-standards)
- [2. Kotlin Guidelines](#2-kotlin-guidelines)
- [3. Ktor Development Guidelines](#3-ktor-development-guidelines)
- [4. Flink Table API and SQL Guidelines](#4-flink-table-api-and-sql-guidelines)
- [5. Exposed ORM Guidelines](#5-exposed-orm-guidelines)
- [6. Frontend Development Guidelines](#6-frontend-development-guidelines)
- [7. Testing Guidelines](#7-testing-guidelines)
- [8. Documentation Guidelines](#8-documentation-guidelines)

## 1. General Coding Standards

### 1.1 Code Style
- Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Use meaningful names for variables, functions, and classes
- Add KDoc comments for all public APIs

### 1.2 Project Structure
- Organize code by feature rather than by layer
- Use consistent package naming: `dev.gamov.flightdemo.[feature].[component]`
- Keep related functionality together

### 1.3 Version Control
- Use feature branches for development
- Write descriptive commit messages
- Perform code reviews before merging

## 2. Kotlin Guidelines

### 2.1 Language Features
- Prefer val over var when possible
- Use data classes for DTOs and domain models
- Leverage extension functions for utility methods
- Use coroutines for asynchronous operations
- Utilize sealed classes for representing state

### 2.2 Null Safety
- Avoid using `!!` operator
- Use safe calls (`?.`) and the elvis operator (`?:`) for null handling
- Make non-nullable types the default choice

### 2.3 Collections
- Use the Kotlin standard library collection functions (map, filter, etc.)
- Prefer immutable collections when possible
- Use sequences for large collection processing

### 2.4 Error Handling
- Use Result type for operations that can fail
- Prefer exceptions for truly exceptional conditions
- Document all exceptions that can be thrown

## 3. Ktor Development Guidelines

### 3.1 Project Structure
- Organize routes by feature
- Use separate files for route definitions
- Place application configuration in Application.kt

### 3.2 Routing
- Use type-safe routing with route() functions
- Group related endpoints under common paths
- Use meaningful route names

```kotlin
routing {
    route("/flights") {
        get { /* Get all flights */ }
        get("/{id}") { /* Get flight by ID */ }
        // ...
    }
}
```

### 3.3 Server-Sent Events (SSE)
- Use the `sse` DSL for SSE endpoints
- Implement proper error handling and reconnection logic
- Close channels when clients disconnect

```kotlin
get("/flights/sse") {
    call.response.cacheControl(CacheControl.NoCache(null))
    call.respondSSE {
        // Send events
    }
}
```

### 3.4 Kafka Integration
- Use structured concurrency with coroutines
- Implement proper error handling and retry logic
- Close Kafka consumers when no longer needed

### 3.5 Exposed Integration
- Use the DSL API for type-safe queries
- Implement proper connection pooling
- Use transactions for database operations

## 4. Flink Table API and SQL Guidelines

### 4.1 Table API Usage
- Use the Table API for complex transformations
- Define schemas explicitly
- Use meaningful table and column names

```kotlin
val tableEnv = StreamTableEnvironment.create(env)
val flightsTable = tableEnv.fromDataStream(
    flightsStream,
    Schema.newBuilder()
        .column("flight_id", DataTypes.STRING())
        .column("latitude", DataTypes.DOUBLE())
        // ...
        .build()
)
```

### 4.2 SQL Queries
- Format SQL queries for readability
- Use parameterized queries when possible
- Document complex SQL logic

```kotlin
val result = tableEnv.sqlQuery("""
    SELECT 
        flight_id, 
        latitude, 
        longitude,
        CONCAT(
            CAST(FLOOR(latitude / 5) * 5 AS STRING), 
            '_', 
            CAST(FLOOR(longitude / 5) * 5 AS STRING)
        ) AS region
    FROM flights
    WHERE altitude > 35000
""")
```

### 4.3 Windowing Operations
- Use appropriate window types (tumbling, sliding, session)
- Define window size based on data characteristics
- Consider late data handling

```kotlin
val result = tableEnv.sqlQuery("""
    SELECT
        region,
        COUNT(*) AS flight_count,
        AVG(altitude) AS avg_altitude
    FROM flights
    GROUP BY
        region,
        TUMBLE(timestamp, INTERVAL '1' MINUTE)
""")
```

### 4.4 Checkpointing
- Enable checkpointing for stateful operations
- Configure appropriate checkpoint interval
- Use RocksDB state backend for large state

## 5. Exposed ORM Guidelines

### 5.1 Table Definitions
- Define tables as objects extending Table
- Use meaningful column names
- Define appropriate column types

```kotlin
object FlightTable : Table("flight_data") {
    val flightId = varchar("flight_id", 50)
    val latitude = double("latitude")
    val longitude = double("longitude")
    val altitude = integer("altitude")
    val status = varchar("status", 20)
    val timestamp = datetime("timestamp")
    
    override val primaryKey = PrimaryKey(flightId)
}
```

### 5.2 Queries
- Use DSL for type-safe queries
- Use transactions for database operations
- Implement proper error handling

```kotlin
val flights = transaction {
    FlightTable
        .select { FlightTable.altitude greater 35000 }
        .orderBy(FlightTable.timestamp to SortOrder.DESC)
        .limit(100)
        .map { it.toFlight() }
}
```

### 5.3 JDBC Integration
- Configure connection pooling
- Use appropriate JDBC driver settings
- Implement proper error handling for JDBC operations

## 6. Frontend Development Guidelines

### 6.1 Kotlin/JS Structure
- Organize code by feature
- Use component-based architecture
- Separate UI and business logic

### 6.2 Lets-Plot Usage
- Follow the official Lets-Plot documentation
- Use appropriate chart types for different data
- Implement responsive visualizations

```kotlin
val plot = letsPlot(flightData) +
    geomPoint(
        color = "status",
        size = 3.0,
        alpha = 0.7
    ) +
    ggtitle("Flight Positions")
```

### 6.3 API Integration
- Use coroutines for asynchronous operations
- Implement proper error handling
- Use typed models for API responses

### 6.4 SSE Client
- Implement reconnection logic
- Handle connection errors gracefully
- Clean up resources when component is destroyed

## 7. Testing Guidelines

### 7.1 Unit Testing
- Write tests for all business logic
- Use JUnit 5 for testing
- Use mockk for mocking dependencies

### 7.2 Ktor Testing
- Use `testApplication` for testing Ktor routes
- Test both success and error cases
- Verify response status and content

```kotlin
@Test
fun testFlightsEndpoint() = testApplication {
    val response = client.get("/flights")
    assertEquals(HttpStatusCode.OK, response.status)
    // Verify response content
}
```

### 7.3 Flink Testing
- Use `StreamExecutionEnvironment.createLocalEnvironment()` for testing
- Test with small, deterministic datasets
- Verify output using collectors

### 7.4 Integration Testing
- Use Docker Compose for integration tests
- Test the entire data flow
- Verify end-to-end functionality

## 8. Documentation Guidelines

### 8.1 Code Documentation
- Add KDoc comments for all public APIs
- Document complex algorithms and business logic
- Keep documentation up-to-date with code changes

### 8.2 API Documentation
- Document all API endpoints
- Include request/response examples
- Document error responses

### 8.3 Architecture Documentation
- Maintain up-to-date architecture diagrams
- Document component interactions
- Include deployment instructions