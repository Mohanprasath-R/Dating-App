package com.example.dating_app.util

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import kotlin.math.*

object LocationHelper {

    /**
     * Detects if the location is spoofed (Mock Location).
     */
    fun isLocationSpoofed(location: Location): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
    }

    /**
     * Calculates the approximate distance between two points.
     * To enhance security, we never return exact meters, only rounded kilometers.
     */
    fun calculateApproximateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        val r = 6371 // Radius of the earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = r * c // Distance in km
        
        return when {
            distance < 1 -> "Less than 1 km away"
            distance < 5 -> "Nearby (~${distance.roundToInt()} km)"
            else -> "${(distance / 5).roundToInt() * 5} km away" // Round to nearest 5km for better privacy
        }
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}
