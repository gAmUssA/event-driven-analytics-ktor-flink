package dev.gamov.flightdemo.ktor.db

import dev.gamov.flightdemo.ktor.model.FlightModel
import dev.gamov.flightdemo.ktor.model.ProcessedFlightModel
import dev.gamov.flightdemo.ktor.routes.RegionStat
import dev.gamov.flightdemo.ktor.service.DatabaseService
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Repository for flight data
 */
class FlightRepository(private val databaseService: DatabaseService) {
    private val logger = LoggerFactory.getLogger(FlightRepository::class.java)

    /**
     * Get recent flights with optional filtering
     */
    fun getRecentFlights(
        limit: Int = 100,
        minAltitude: Int? = null,
        status: String? = null,
        timeRangeMinutes: Int = 10
    ): List<FlightModel> {
        logger.debug("Getting recent flights with limit=$limit, minAltitude=$minAltitude, status=$status, timeRangeMinutes=$timeRangeMinutes")

        val conditions = mutableListOf<String>()
        val parameters = mutableListOf<Any>()

        // Add time range condition
        val cutoffTime = Instant.now().minus(timeRangeMinutes.toLong(), ChronoUnit.MINUTES)
        conditions.add("${FlightTable.timestamp.name} >= ?")
        parameters.add(cutoffTime)

        // Add altitude condition if specified
        if (minAltitude != null) {
            conditions.add("${FlightTable.altitude.name} >= ?")
            parameters.add(minAltitude)
        }

        // Add status condition if specified
        if (status != null) {
            conditions.add("${FlightTable.status.name} = ?")
            parameters.add(status)
        }

        val whereClause = conditions.joinToString(" AND ")

        val query = FlightTable.select { 
            where(whereClause, *parameters.toTypedArray())
            orderBy(FlightTable.timestamp.name, SortOrder.DESC)
            limit(limit)
        }

        return databaseService.withConnection { connection ->
            query.executeQuery(connection) { rs ->
                rs.toFlightModel()
            }
        }
    }

    /**
     * Get flight counts by region
     */
    fun getFlightCountsByRegion(timeRangeMinutes: Int = 10): List<RegionStat> {
        logger.debug("Getting flight counts by region with timeRangeMinutes=$timeRangeMinutes")

        val cutoffTime = Instant.now().minus(timeRangeMinutes.toLong(), ChronoUnit.MINUTES)

        val sql = """
            SELECT 
                ${RegionStatsTable.region.name}, 
                ${RegionStatsTable.flightCount.name}, 
                ${RegionStatsTable.avgAltitude.name}
            FROM ${RegionStatsTable.name}
            WHERE ${RegionStatsTable.timestamp.name} >= ?
            ORDER BY ${RegionStatsTable.flightCount.name} DESC
        """.trimIndent()

        return databaseService.query(sql, listOf(cutoffTime)) { rs ->
            RegionStat(
                region = rs.getString(RegionStatsTable.region.name),
                flightCount = rs.getInt(RegionStatsTable.flightCount.name),
                avgAltitude = rs.getInt(RegionStatsTable.avgAltitude.name)
            )
        }
    }

    /**
     * Convert ResultSet to FlightModel
     */
    private fun ResultSet.toFlightModel(): FlightModel {
        return FlightModel(
            flightId = getString(FlightTable.flightId.name),
            callsign = getString(FlightTable.callsign.name),
            latitude = getDouble(FlightTable.latitude.name),
            longitude = getDouble(FlightTable.longitude.name),
            altitude = getInt(FlightTable.altitude.name),
            heading = getDouble(FlightTable.heading.name),
            speed = getDouble(FlightTable.speed.name),
            verticalSpeed = getDouble(FlightTable.verticalSpeed.name),
            origin = getString(FlightTable.origin.name),
            destination = getString(FlightTable.destination.name),
            status = getString(FlightTable.status.name),
            timestamp = getTimestamp(FlightTable.timestamp.name).toInstant().toEpochMilli()
        )
    }

    /**
     * Convert ResultSet to ProcessedFlightModel
     */
    private fun ResultSet.toProcessedFlightModel(): ProcessedFlightModel {
        return ProcessedFlightModel(
            flightId = getString(FlightTable.flightId.name),
            callsign = getString(FlightTable.callsign.name),
            latitude = getDouble(FlightTable.latitude.name),
            longitude = getDouble(FlightTable.longitude.name),
            altitude = getInt(FlightTable.altitude.name),
            heading = getDouble(FlightTable.heading.name),
            speed = getDouble(FlightTable.speed.name),
            verticalSpeed = getDouble(FlightTable.verticalSpeed.name),
            origin = getString(FlightTable.origin.name),
            destination = getString(FlightTable.destination.name),
            status = getString(FlightTable.status.name),
            timestamp = getTimestamp(FlightTable.timestamp.name).toInstant().toEpochMilli(),
            region = getString(FlightTable.region.name),
            isHighAltitude = getBoolean(FlightTable.isHighAltitude.name),
            processingTimestamp = getTimestamp(FlightTable.processingTimestamp.name).toInstant().toEpochMilli()
        )
    }
}
