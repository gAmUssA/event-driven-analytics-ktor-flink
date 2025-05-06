package dev.gamov.flightdemo.generator.simulation

import dev.gamov.flightdemo.generator.config.SimulationConfig
import dev.gamov.flightdemo.generator.model.Flight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

/**
 * Simulates a collection of flights, updating their positions and states over time.
 */
class FlightSimulator(
    private val config: SimulationConfig
) {
    private val logger = LoggerFactory.getLogger(FlightSimulator::class.java)
    private val flights = mutableListOf<Flight>()
    private val flightFlow = MutableSharedFlow<Flight>(replay = 0, extraBufferCapacity = 100)
    private var lastUpdateTime = Instant.now()
    private var running = false
    
    /**
     * Initializes the simulator with random flights.
     */
    fun initialize() {
        logger.info("Initializing flight simulator with ${config.numFlights} flights")
        flights.clear()
        
        for (i in 1..config.numFlights) {
            flights.add(Flight.createRandom(i, config))
        }
        
        lastUpdateTime = Instant.now()
        logger.info("Flight simulator initialized")
    }
    
    /**
     * Starts the simulation, updating flight positions at regular intervals.
     *
     * @param scope The coroutine scope to launch the simulation in.
     */
    fun start(scope: CoroutineScope) {
        if (running) {
            logger.warn("Flight simulator is already running")
            return
        }
        
        running = true
        logger.info("Starting flight simulator with update interval: ${config.updateInterval}")
        
        scope.launch(Dispatchers.Default) {
            while (isActive && running) {
                updateFlights()
                delay(config.updateInterval.toMillis())
            }
        }
    }
    
    /**
     * Stops the simulation.
     */
    fun stop() {
        running = false
        logger.info("Flight simulator stopped")
    }
    
    /**
     * Gets a flow of flight updates.
     */
    fun getFlightFlow(): Flow<Flight> = flightFlow.asSharedFlow()
    
    /**
     * Gets a snapshot of all current flights.
     */
    fun getAllFlights(): List<Flight> = flights.toList()
    
    /**
     * Updates all flights based on their current state and the elapsed time.
     */
    private suspend fun updateFlights() {
        val now = Instant.now()
        val elapsedSeconds = Duration.between(lastUpdateTime, now).seconds.toDouble()
        lastUpdateTime = now
        
        logger.debug("Updating ${flights.size} flights after $elapsedSeconds seconds")
        
        flights.forEach { flight ->
            // Occasionally change flight parameters to make the simulation more realistic
            if (Random.nextDouble() < 0.1) {
                flight.randomizeMovement()
            }
            
            // Update position based on current heading, speed, and elapsed time
            flight.updatePosition(elapsedSeconds, config.speedFactor)
            
            // Occasionally change flight status
            if (Random.nextDouble() < 0.01) {
                val statusValues = dev.gamov.flightdemo.generator.model.FlightStatus.values()
                flight.status = statusValues[Random.nextInt(statusValues.size)]
            }
            
            // Emit the updated flight
            flightFlow.emit(flight)
        }
    }
}