package dev.gamov.flightdemo.generator

import dev.gamov.flightdemo.generator.config.GeneratorConfig
import dev.gamov.flightdemo.generator.kafka.FlightProducer
import dev.gamov.flightdemo.generator.simulation.FlightSimulator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

/**
 * Main entry point for the Flight Data Generator application.
 * This application simulates flight data and publishes it to Kafka.
 */
object Main {
    private val logger = LoggerFactory.getLogger(Main::class.java)
    private lateinit var simulator: FlightSimulator
    private lateinit var producer: FlightProducer

    @JvmStatic
    fun main(args: Array<String>) = runBlocking(Dispatchers.Default) {
        logger.info("Starting Flight Data Generator")

        try {
            // Load configuration
            val configFile = args.getOrNull(0)
            logger.info("Configuration file parameter: {}", configFile ?: "not provided")
            val config = GeneratorConfig.load(configFile ?: "config/generator-config.yaml")

            // Initialize flight simulator
            simulator = FlightSimulator(config.simulation)
            simulator.initialize()

            // Initialize Kafka producer
            producer = FlightProducer(config.kafka)

            // Start flight simulation and Kafka production
            simulator.start(this)
            producer.start(simulator.getFlightFlow(), this)

            logger.info("Flight Data Generator started successfully")

            // Keep the application running until interrupted
            Runtime.getRuntime().addShutdownHook(Thread {
                logger.info("Shutting down Flight Data Generator")
                runBlocking {
                    // Clean up resources
                    simulator.stop()
                    producer.stop()
                    coroutineContext.cancelChildren()
                }
            })

            // Wait indefinitely
            Thread.currentThread().join()
        } catch (e: Exception) {
            logger.error("Error in Flight Data Generator", e)
            exitProcess(1)
        }
    }
}
