package dev.gamov.flightdemo.ktor.routes

import dev.gamov.flightdemo.ktor.model.FlightModel
import dev.gamov.flightdemo.ktor.model.ProcessedFlightModel
import dev.gamov.flightdemo.ktor.service.KafkaService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Configure SSE routes for flight data
 */
fun Application.configureSseRoutes(kafkaService: KafkaService) {
    val logger = LoggerFactory.getLogger("SseRoutes")

    routing {
        /**
         * SSE endpoint for raw flight data
         */
        get("/flights/sse") {
            logger.info("Client connected to /flights/sse")

            // Set appropriate headers for SSE
            call.response.cacheControl(CacheControl.NoCache(CacheControl.Visibility.Public))
            call.response.header(HttpHeaders.Connection, "keep-alive")
            call.response.header("X-Accel-Buffering", "no") // For Nginx

            try {
                call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                    try {
                        // Subscribe to the flight flow from KafkaService
                        kafkaService.flightFlow
                            .map { flightData ->
                                // Convert Avro object to FlightModel
                                FlightModel(
                                    flightId = flightData.getFlightId(),
                                    callsign = flightData.getCallsign(),
                                    latitude = flightData.getLatitude(),
                                    longitude = flightData.getLongitude(),
                                    altitude = flightData.getAltitude(),
                                    heading = flightData.getHeading(),
                                    speed = flightData.getSpeed(),
                                    verticalSpeed = flightData.getVerticalSpeed(),
                                    origin = flightData.getOrigin(),
                                    destination = flightData.getDestination(),
                                    status = flightData.getStatus().toString(),
                                    timestamp = flightData.getTimestamp().toEpochMilli()
                                )
                            }
                            .catch { e ->
                                logger.error("Error in flight flow", e)
                                write("event: error\ndata: ${e.message}\n\n")
                                flush()
                            }
                            .onCompletion {
                                logger.info("Flight flow completed")
                            }
                            .collect { flightModel ->
                                // Serialize to JSON and send as SSE event
                                val json = Json.encodeToString(flightModel)
                                write("data: $json\n\n")
                                flush()
                            }
                    } catch (e: Exception) {
                        logger.error("Error in flight SSE stream", e)
                        write("event: error\ndata: ${e.message}\n\n")
                        flush()
                    } finally {
                        logger.info("Client disconnected from /flights/sse")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error in flight SSE endpoint", e)
                call.respond(HttpStatusCode.InternalServerError, "Error streaming flight data")
            }
        }

        /**
         * SSE endpoint for processed flight data
         */
        get("/processed_flights/sse") {
            logger.info("Client connected to /processed_flights/sse")

            // Set appropriate headers for SSE
            call.response.cacheControl(CacheControl.NoCache(CacheControl.Visibility.Public))
            call.response.header(HttpHeaders.Connection, "keep-alive")
            call.response.header("X-Accel-Buffering", "no") // For Nginx

            try {
                call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                    try {
                        // Subscribe to the processed flight flow from KafkaService
                        kafkaService.processedFlightFlow
                            .map { processedFlightData ->
                                // Convert Avro object to ProcessedFlightModel
                                ProcessedFlightModel(
                                    flightId = processedFlightData.getFlightId(),
                                    callsign = processedFlightData.getCallsign(),
                                    latitude = processedFlightData.getLatitude(),
                                    longitude = processedFlightData.getLongitude(),
                                    altitude = processedFlightData.getAltitude(),
                                    heading = processedFlightData.getHeading(),
                                    speed = processedFlightData.getSpeed(),
                                    verticalSpeed = processedFlightData.getVerticalSpeed(),
                                    origin = processedFlightData.getOrigin(),
                                    destination = processedFlightData.getDestination(),
                                    status = processedFlightData.getStatus().toString(),
                                    timestamp = processedFlightData.getTimestamp().toEpochMilli(),
                                    region = processedFlightData.getRegion(),
                                    isHighAltitude = processedFlightData.getIsHighAltitude(),
                                    processingTimestamp = processedFlightData.getProcessingTimestamp().toEpochMilli()
                                )
                            }
                            .catch { e ->
                                logger.error("Error in processed flight flow", e)
                                write("event: error\ndata: ${e.message}\n\n")
                                flush()
                            }
                            .onCompletion {
                                logger.info("Processed flight flow completed")
                            }
                            .collect { processedFlightModel ->
                                // Serialize to JSON and send as SSE event
                                val json = Json.encodeToString(processedFlightModel)
                                write("data: $json\n\n")
                                flush()
                            }
                    } catch (e: Exception) {
                        logger.error("Error in processed flight SSE stream", e)
                        write("event: error\ndata: ${e.message}\n\n")
                        flush()
                    } finally {
                        logger.info("Client disconnected from /processed_flights/sse")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error in processed flight SSE endpoint", e)
                call.respond(HttpStatusCode.InternalServerError, "Error streaming processed flight data")
            }
        }
    }
}
