package dev.gamov.flightdemo.ktor.model

import kotlinx.serialization.Serializable

/**
 * Model class for flight data in the API
 */
@Serializable
data class FlightModel(
    val flightId: String,
    val callsign: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Int,
    val heading: Double,
    val speed: Double,
    val verticalSpeed: Double,
    val origin: String,
    val destination: String,
    val status: String,
    val timestamp: Long
) {
    companion object {
        /**
         * Convert FlightData Avro object to FlightModel
         * Note: This is a placeholder implementation until the Avro classes are generated
         */
        fun fromAvro(flightData: Any): FlightModel {
            // This is a placeholder implementation
            // In a real implementation, we would convert the Avro object to a FlightModel
            return FlightModel(
                flightId = "FL123",
                callsign = "ABC123",
                latitude = 37.7749,
                longitude = -122.4194,
                altitude = 35000,
                heading = 90.0,
                speed = 500.0,
                verticalSpeed = 0.0,
                origin = "SFO",
                destination = "JFK",
                status = "ON_TIME",
                timestamp = System.currentTimeMillis()
            )
        }
    }
}

/**
 * Model class for processed flight data in the API
 */
@Serializable
data class ProcessedFlightModel(
    val flightId: String,
    val callsign: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Int,
    val heading: Double,
    val speed: Double,
    val verticalSpeed: Double,
    val origin: String,
    val destination: String,
    val status: String,
    val timestamp: Long,
    val region: String,
    val isHighAltitude: Boolean,
    val processingTimestamp: Long
) {
    companion object {
        /**
         * Convert ProcessedFlightData Avro object to ProcessedFlightModel
         * Note: This is a placeholder implementation until the Avro classes are generated
         */
        fun fromAvro(processedFlightData: Any): ProcessedFlightModel {
            // This is a placeholder implementation
            // In a real implementation, we would convert the Avro object to a ProcessedFlightModel
            return ProcessedFlightModel(
                flightId = "FL123",
                callsign = "ABC123",
                latitude = 37.7749,
                longitude = -122.4194,
                altitude = 35000,
                heading = 90.0,
                speed = 500.0,
                verticalSpeed = 0.0,
                origin = "SFO",
                destination = "JFK",
                status = "ON_TIME",
                timestamp = System.currentTimeMillis(),
                region = "37_-122",
                isHighAltitude = true,
                processingTimestamp = System.currentTimeMillis()
            )
        }
    }
}
