package dev.gamov.flightdemo.ktor.routes

import dev.gamov.flightdemo.ktor.db.FlightRepository
import dev.gamov.flightdemo.ktor.model.FlightModel
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

/**
 * Configure REST routes for flight data
 */
fun Application.configureRestRoutes() {
    val logger = LoggerFactory.getLogger("RestRoutes")
    val flightRepository by inject<FlightRepository>()

    routing {
        /**
         * REST endpoint for recent flights
         */
        get("/flights") {
            logger.info("GET /flights")

            // Get query parameters for filtering
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val minAltitude = call.request.queryParameters["minAltitude"]?.toIntOrNull()
            val status = call.request.queryParameters["status"]
            val timeRangeMinutes = call.request.queryParameters["timeRange"]?.toIntOrNull() ?: 10

            try {
                // Get flights from the repository
                val flights = flightRepository.getRecentFlights(
                    limit = limit,
                    minAltitude = minAltitude,
                    status = status,
                    timeRangeMinutes = timeRangeMinutes
                )

                call.respond(flights)
            } catch (e: Exception) {
                logger.error("Error in /flights endpoint", e)
                call.respond(HttpStatusCode.InternalServerError, "Error retrieving flight data")
            }
        }

        /**
         * REST endpoint for region statistics
         */
        get("/flights/regions") {
            logger.info("GET /flights/regions")

            try {
                // Get region statistics from the repository
                val timeRangeMinutes = call.request.queryParameters["timeRange"]?.toIntOrNull() ?: 10
                val regionStats = flightRepository.getFlightCountsByRegion(timeRangeMinutes)

                call.respond(regionStats)
            } catch (e: Exception) {
                logger.error("Error in /flights/regions endpoint", e)
                call.respond(HttpStatusCode.InternalServerError, "Error retrieving region statistics")
            }
        }
    }
}

/**
 * Model class for region statistics
 */
@Serializable
data class RegionStat(
    val region: String,
    val flightCount: Int,
    val avgAltitude: Int
)
