package dev.gamov.flightdemo.flink.functions

import org.apache.flink.table.functions.ScalarFunction
import kotlin.math.floor

/**
 * A scalar function that calculates a region identifier based on latitude and longitude.
 * The region is defined as a 5x5 degree grid cell.
 */
class RegionCalculator : ScalarFunction() {
    /**
     * Calculates a region identifier based on latitude and longitude.
     * 
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @return A string representation of the region in the format "lat_lon"
     */
    fun eval(latitude: Double?, longitude: Double?): String {
        if (latitude == null || longitude == null) {
            return "unknown_region"
        }
        val latRegion = floor(latitude / 5) * 5
        val lonRegion = floor(longitude / 5) * 5
        return "${latRegion.toInt()}_${lonRegion.toInt()}"
    }
}