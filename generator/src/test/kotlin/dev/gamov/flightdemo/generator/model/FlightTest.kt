package dev.gamov.flightdemo.generator.model

import dev.gamov.flightdemo.generator.config.SimulationConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

class FlightTest {

    @Test
    fun `test updatePosition updates latitude and longitude correctly`() {
        // Arrange
        val flight = Flight(
            flightId = "FL123",
            callsign = "AIR123",
            latitude = 40.0,
            longitude = -74.0,
            altitude = 35000,
            heading = 90.0, // East
            speed = 500.0,
            verticalSpeed = 0.0,
            origin = "JFK",
            destination = "LHR",
            status = FlightStatus.ON_TIME,
            timestamp = Instant.now()
        )
        val initialLatitude = flight.latitude
        val initialLongitude = flight.longitude
        
        // Act
        flight.updatePosition(10.0) // 10 seconds elapsed
        
        // Assert
        // Heading is 90 degrees (east), so latitude should remain roughly the same
        // and longitude should increase
        assertEquals(initialLatitude, flight.latitude, 0.1)
        assertTrue(flight.longitude > initialLongitude)
    }
    
    @Test
    fun `test updatePosition updates altitude based on vertical speed`() {
        // Arrange
        val flight = Flight(
            flightId = "FL123",
            callsign = "AIR123",
            latitude = 40.0,
            longitude = -74.0,
            altitude = 35000,
            heading = 0.0,
            speed = 500.0,
            verticalSpeed = 1200.0, // 1200 feet per minute
            origin = "JFK",
            destination = "LHR",
            status = FlightStatus.ON_TIME,
            timestamp = Instant.now()
        )
        val initialAltitude = flight.altitude
        
        // Act
        flight.updatePosition(60.0) // 60 seconds elapsed
        
        // Assert
        // Vertical speed is 1200 feet per minute, so in 60 seconds altitude should increase by 1200
        assertEquals(initialAltitude + 1200, flight.altitude)
    }
    
    @Test
    fun `test randomizeMovement keeps values within expected ranges`() {
        // Arrange
        val flight = Flight(
            flightId = "FL123",
            callsign = "AIR123",
            latitude = 40.0,
            longitude = -74.0,
            altitude = 35000,
            heading = 90.0,
            speed = 500.0,
            verticalSpeed = 0.0,
            origin = "JFK",
            destination = "LHR",
            status = FlightStatus.ON_TIME,
            timestamp = Instant.now()
        )
        
        // Act
        flight.randomizeMovement()
        
        // Assert
        assertTrue(flight.heading >= 0.0 && flight.heading < 360.0)
        assertTrue(flight.speed >= 300.0 && flight.speed <= 600.0)
        assertTrue(flight.verticalSpeed >= -1000.0 && flight.verticalSpeed <= 1000.0)
    }
    
    @Test
    fun `test toMap returns correct map representation`() {
        // Arrange
        val timestamp = Instant.now()
        val flight = Flight(
            flightId = "FL123",
            callsign = "AIR123",
            latitude = 40.0,
            longitude = -74.0,
            altitude = 35000,
            heading = 90.0,
            speed = 500.0,
            verticalSpeed = 0.0,
            origin = "JFK",
            destination = "LHR",
            status = FlightStatus.ON_TIME,
            timestamp = timestamp
        )
        
        // Act
        val map = flight.toMap()
        
        // Assert
        assertEquals("FL123", map["flightId"])
        assertEquals("AIR123", map["callsign"])
        assertEquals(40.0, map["latitude"])
        assertEquals(-74.0, map["longitude"])
        assertEquals(35000, map["altitude"])
        assertEquals(90.0, map["heading"])
        assertEquals(500.0, map["speed"])
        assertEquals(0.0, map["verticalSpeed"])
        assertEquals("JFK", map["origin"])
        assertEquals("LHR", map["destination"])
        assertEquals("ON_TIME", map["status"])
        assertEquals(timestamp.toEpochMilli(), map["timestamp"])
    }
    
    @Test
    fun `test createRandom creates flight with expected properties`() {
        // Arrange
        val config = SimulationConfig(
            numFlights = 100,
            updateInterval = Duration.ofSeconds(1),
            speedFactor = 1.0,
            initialAltitudeMin = 30000,
            initialAltitudeMax = 40000,
            initialLatitudeMin = 25.0,
            initialLatitudeMax = 50.0,
            initialLongitudeMin = -125.0,
            initialLongitudeMax = -70.0,
            flightStatusProbabilities = mapOf(
                "ON_TIME" to 1.0 // Force ON_TIME for deterministic test
            )
        )
        
        // Act
        val flight = Flight.createRandom(123, config)
        
        // Assert
        assertEquals("FL123", flight.flightId)
        assertEquals("AIR123", flight.callsign)
        assertTrue(flight.latitude >= config.initialLatitudeMin && flight.latitude <= config.initialLatitudeMax)
        assertTrue(flight.longitude >= config.initialLongitudeMin && flight.longitude <= config.initialLongitudeMax)
        assertTrue(flight.altitude >= config.initialAltitudeMin && flight.altitude <= config.initialAltitudeMax)
        assertTrue(flight.heading >= 0.0 && flight.heading < 360.0)
        assertTrue(flight.speed >= 300.0 && flight.speed <= 600.0)
        assertTrue(abs(flight.verticalSpeed) <= 500.0)
        assertNotNull(flight.origin)
        assertNotNull(flight.destination)
        assertEquals(FlightStatus.ON_TIME, flight.status)
    }
}