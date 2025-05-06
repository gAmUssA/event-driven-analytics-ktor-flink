package dev.gamov.flightdemo.generator.simulation

import dev.gamov.flightdemo.generator.config.SimulationConfig
import dev.gamov.flightdemo.generator.model.Flight
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Duration

class FlightSimulatorTest {

    private lateinit var config: SimulationConfig
    private lateinit var simulator: FlightSimulator
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @BeforeEach
    fun setUp() {
        config = SimulationConfig(
            numFlights = 5, // Use a small number for testing
            updateInterval = Duration.ofMillis(100),
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
        simulator = FlightSimulator(config)
    }

    @Test
    fun `initialize creates the correct number of flights`() {
        // Act
        simulator.initialize()

        // Assert
        val flights = simulator.getAllFlights()
        assertEquals(config.numFlights, flights.size)

        // Verify each flight has expected properties
        flights.forEachIndexed { index, flight ->
            val expectedId = index + 1
            assertEquals("FL$expectedId", flight.flightId)
            assertTrue(flight.latitude >= config.initialLatitudeMin && flight.latitude <= config.initialLatitudeMax)
            assertTrue(flight.longitude >= config.initialLongitudeMin && flight.longitude <= config.initialLongitudeMax)
            assertTrue(flight.altitude >= config.initialAltitudeMin && flight.altitude <= config.initialAltitudeMax)
        }
    }

    @Test
    fun `getAllFlights returns a copy of the flights list`() {
        // Arrange
        simulator.initialize()

        // Act
        val flights1 = simulator.getAllFlights()
        val flights2 = simulator.getAllFlights()

        // Assert
        assertNotSame(flights1, flights2) // Should be different instances
        assertEquals(flights1.size, flights2.size) // But with same content

        // Verify modifying the returned list doesn't affect the internal state
        val originalSize = flights1.size
        val mutableList = flights1.toMutableList()
        mutableList.add(mockk())
        assertEquals(originalSize, simulator.getAllFlights().size) // Size should remain the same
    }

    @Test
    fun `getFlightFlow returns a non-null flow`() {
        // Act
        val flow = simulator.getFlightFlow()

        // Assert
        assertNotNull(flow)
    }

    @Test
    fun `start and stop methods do not throw exceptions`() {
        // Arrange
        simulator.initialize()

        // Act & Assert - no exceptions should be thrown
        assertDoesNotThrow {
            simulator.start(testScope)
            simulator.stop()
        }
    }
}
