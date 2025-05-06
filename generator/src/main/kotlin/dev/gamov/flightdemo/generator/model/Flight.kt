package dev.gamov.flightdemo.generator.model

import dev.gamov.flightdemo.avro.FlightData
import dev.gamov.flightdemo.avro.FlightStatus as AvroFlightStatus
import java.time.Instant
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Flight status enum.
 * Maps to the Avro-generated enum.
 */
enum class FlightStatus {
    ON_TIME, DELAYED, CANCELLED, DIVERTED
}

/**
 * Represents a flight with its current state.
 * This class wraps the Avro-generated FlightData class and provides additional functionality.
 */
data class Flight(
    val flightId: String,
    val callsign: String,
    var latitude: Double,
    var longitude: Double,
    var altitude: Int,
    var heading: Double,
    var speed: Double,
    var verticalSpeed: Double,
    val origin: String,
    val destination: String,
    var status: FlightStatus,
    var timestamp: Instant = Instant.now()
) {
    /**
     * Updates the flight's position based on its current heading, speed, and the elapsed time.
     *
     * @param elapsedSeconds The time elapsed since the last update in seconds.
     * @param speedFactor A factor to multiply the speed by for simulation purposes.
     */
    fun updatePosition(elapsedSeconds: Double, speedFactor: Double = 1.0) {
        // Convert knots to degrees per second (very approximate)
        // 1 knot ≈ 0.0003 degrees of longitude per second at the equator
        val speedInDegreesPerSecond = speed * 0.0003 * speedFactor

        // Calculate new position
        val headingRadians = Math.toRadians(heading)
        val latitudeChange = speedInDegreesPerSecond * elapsedSeconds * cos(headingRadians)
        val longitudeChange = speedInDegreesPerSecond * elapsedSeconds * sin(headingRadians)

        latitude += latitudeChange
        longitude += longitudeChange

        // Update altitude based on vertical speed (feet per minute to feet)
        altitude += (verticalSpeed / 60.0 * elapsedSeconds).toInt()

        // Update timestamp
        timestamp = Instant.now()
    }

    /**
     * Randomly changes the flight's heading, speed, and vertical speed to simulate realistic flight behavior.
     */
    fun randomizeMovement() {
        // Small random changes to heading (-5 to +5 degrees)
        heading = (heading + Random.nextDouble(-5.0, 5.0)) % 360.0
        if (heading < 0) heading += 360.0

        // Small random changes to speed (-10 to +10 knots)
        speed = (speed + Random.nextDouble(-10.0, 10.0)).coerceIn(300.0, 600.0)

        // Small random changes to vertical speed (-100 to +100 feet per minute)
        verticalSpeed = (verticalSpeed + Random.nextDouble(-100.0, 100.0)).coerceIn(-1000.0, 1000.0)

        // If altitude is too high or too low, adjust vertical speed to bring it back to a reasonable range
        when {
            altitude > 40000 -> verticalSpeed = -500.0
            altitude < 30000 -> verticalSpeed = 500.0
        }
    }

    /**
     * Converts this Flight to a Map for serialization.
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "flightId" to flightId,
            "callsign" to callsign,
            "latitude" to latitude,
            "longitude" to longitude,
            "altitude" to altitude,
            "heading" to heading,
            "speed" to speed,
            "verticalSpeed" to verticalSpeed,
            "origin" to origin,
            "destination" to destination,
            "status" to status.name,
            "timestamp" to timestamp.toEpochMilli()
        )
    }

    /**
     * Converts this Flight to an Avro FlightData object for serialization.
     */
    fun toAvro(): FlightData {
        return FlightData.newBuilder()
            .setFlightId(flightId)
            .setCallsign(callsign)
            .setLatitude(latitude)
            .setLongitude(longitude)
            .setAltitude(altitude)
            .setHeading(heading)
            .setSpeed(speed)
            .setVerticalSpeed(verticalSpeed)
            .setOrigin(origin)
            .setDestination(destination)
            .setStatus(convertStatus(status))
            .setTimestamp(timestamp)
            .build()
    }

    /**
     * Converts a Flight.FlightStatus to an Avro FlightStatus.
     */
    private fun convertStatus(status: FlightStatus): AvroFlightStatus {
        return when (status) {
            FlightStatus.ON_TIME -> AvroFlightStatus.ON_TIME
            FlightStatus.DELAYED -> AvroFlightStatus.DELAYED
            FlightStatus.CANCELLED -> AvroFlightStatus.CANCELLED
            FlightStatus.DIVERTED -> AvroFlightStatus.DIVERTED
        }
    }

    companion object {
        /**
         * Creates a random flight.
         *
         * @param id The flight ID.
         * @param config The simulation configuration.
         * @return A new random flight.
         */
        fun createRandom(id: Int, config: dev.gamov.flightdemo.generator.config.SimulationConfig): Flight {
            val flightId = "FL$id"
            val callsign = "AIR$id"

            // Random position within the configured bounds
            val latitude = Random.nextDouble(
                config.initialLatitudeMin,
                config.initialLatitudeMax
            )
            val longitude = Random.nextDouble(
                config.initialLongitudeMin,
                config.initialLongitudeMax
            )

            // Random altitude within the configured bounds
            val altitude = Random.nextInt(
                config.initialAltitudeMin,
                config.initialAltitudeMax
            )

            // Random heading (0-360 degrees)
            val heading = Random.nextDouble(0.0, 360.0)

            // Random speed (300-600 knots)
            val speed = Random.nextDouble(300.0, 600.0)

            // Random vertical speed (-500 to 500 feet per minute)
            val verticalSpeed = Random.nextDouble(-500.0, 500.0)

            // Random origin and destination (simple 3-letter codes)
            val origins = listOf("JFK", "LAX", "ORD", "DFW", "DEN", "SEA", "SFO", "ATL", "MIA", "BOS")
            val destinations = listOf("LHR", "CDG", "FRA", "AMS", "MAD", "FCO", "ZRH", "IST", "DXB", "HKG")
            val origin = origins.random()
            val destination = destinations.random()

            // Random status based on configured probabilities
            val status = randomStatus(config.flightStatusProbabilities)

            return Flight(
                flightId = flightId,
                callsign = callsign,
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                heading = heading,
                speed = speed,
                verticalSpeed = verticalSpeed,
                origin = origin,
                destination = destination,
                status = status
            )
        }

        /**
         * Selects a random flight status based on the provided probabilities.
         *
         * @param probabilities A map of status to probability.
         * @return A random flight status.
         */
        private fun randomStatus(probabilities: Map<String, Double>): FlightStatus {
            val random = Random.nextDouble()
            var cumulativeProbability = 0.0

            for ((status, probability) in probabilities) {
                cumulativeProbability += probability
                if (random < cumulativeProbability) {
                    return FlightStatus.valueOf(status)
                }
            }

            // Default to ON_TIME if probabilities don't add up to 1.0
            return FlightStatus.ON_TIME
        }
    }
}
