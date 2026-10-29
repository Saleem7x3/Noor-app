package com.example.domain

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.math.pow
import kotlin.math.sqrt

sealed class LocationResolutionResult {
    data class GpsResolved(
        val cityLocation: CityLocation,
        val rawLatitude: Double,
        val rawLongitude: Double
    ) : LocationResolutionResult()

    data class FallbackSqliteCity(
        val cityLocation: CityLocation,
        val reason: String
    ) : LocationResolutionResult()
}

/**
 * Resolves coordinates via FusedLocationProviderClient when location permission is granted,
 * and falls back gracefully to the local SQLite database city catalog when unavailable.
 */
class LocationResolutionManager(private val context: Context) {

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun resolveLocationWithFallback(
        fallbackCities: List<CityLocation>,
        selectedFallbackCity: CityLocation
    ): LocationResolutionResult {
        if (!hasLocationPermission()) {
            return LocationResolutionResult.FallbackSqliteCity(
                cityLocation = selectedFallbackCity,
                reason = "Location permission not granted; using SQLite fallback (${selectedFallbackCity.name})"
            )
        }

        val gpsCoords = fetchFusedCoordinatesOrNull()
        return if (gpsCoords != null) {
            val (lat, lon) = gpsCoords
            val nearest = fallbackCities.minByOrNull { city ->
                sqrt((city.latitude - lat).pow(2) + (city.longitude - lon).pow(2))
            }
            val tzOffsetHours = TimeZone.getDefault().rawOffset / (1000.0 * 60.0 * 60.0)
            val resolvedCity = if (nearest != null &&
                sqrt((nearest.latitude - lat).pow(2) + (nearest.longitude - lon).pow(2)) < 2.5
            ) {
                nearest.copy(latitude = lat, longitude = lon, isGpsSimulated = true)
            } else {
                CityLocation(
                    id = "gps_live",
                    name = "GPS (${String.format("%.2f", lat)}, ${String.format("%.2f", lon)})",
                    country = "Live Fused",
                    latitude = lat,
                    longitude = lon,
                    timeZoneOffsetHours = tzOffsetHours,
                    isGpsSimulated = true
                )
            }
            LocationResolutionResult.GpsResolved(
                cityLocation = resolvedCity,
                rawLatitude = lat,
                rawLongitude = lon
            )
        } else {
            LocationResolutionResult.FallbackSqliteCity(
                cityLocation = selectedFallbackCity,
                reason = "FusedLocationProvider returned null; using SQLite fallback (${selectedFallbackCity.name})"
            )
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchFusedCoordinatesOrNull(): Pair<Double, Double>? =
        suspendCancellableCoroutine { cont ->
            try {
                val cts = CancellationTokenSource()
                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { location ->
                        if (cont.isActive) {
                            if (location != null) {
                                cont.resume(Pair(location.latitude, location.longitude))
                            } else {
                                cont.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
}
