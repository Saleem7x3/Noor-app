package com.example.domain

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.math.pow
import kotlin.math.sqrt

sealed class LocationResolutionResult {
    data class GpsResolved(
        val cityLocation: CityLocation,
        val rawLatitude: Double,
        val rawLongitude: Double,
        val providerLabel: String
    ) : LocationResolutionResult()

    data class FallbackSqliteCity(
        val cityLocation: CityLocation,
        val reason: String
    ) : LocationResolutionResult()
}

/**
 * Multi-stage Location Resolution & Worldwide Town/Neighborhood Search Engine:
 * - Supports searching ANY town, neighborhood, suburb, pincode, or city worldwide with zero API keys using:
 *   1. Instant local SQLite & alphabetical world city catalog matching
 *   2. Android OS Geocoder.getFromLocationName()
 *   3. OpenStreetMap Nominatim Geocoding API for street/neighborhood-level precision
 */
class LocationResolutionManager(private val context: Context) {

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val systemLocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
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

    fun isDeviceLocationEnabled(): Boolean {
        return runCatching {
            LocationManagerCompat.isLocationEnabled(systemLocationManager)
        }.getOrDefault(false)
    }

    /**
     * Searches for any city, town, neighborhood, village, or postal code worldwide.
     * Combines alphabetical SQLite catalog matches + Android Geocoder + OpenStreetMap Nominatim.
     */
    suspend fun searchLocationsWorldwide(
        query: String,
        localCities: List<CityLocation>
    ): List<CityLocation> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext localCities

        val normalizedQuery = trimmed.lowercase(Locale.US)
        val combinedResults = mutableListOf<CityLocation>()

        // 1. Instant local SQLite & predefined alphabetical world city matches
        val localMatches = localCities.filter { city ->
            val haystack = "${city.name} ${city.country} ${city.id}".lowercase(Locale.US)
            haystack.contains(normalizedQuery)
        }
        combinedResults.addAll(localMatches)

        // 2. Android OS Geocoder (if available on device)
        val geocoderMatches = searchViaAndroidGeocoder(trimmed)
        geocoderMatches.forEach { candidate ->
            if (combinedResults.none { isSamePlace(it, candidate) }) {
                combinedResults.add(candidate)
            }
        }

        // 3. OpenStreetMap Nominatim Global Town/Suburb/Neighborhood API
        val osmMatches = searchViaOpenStreetMapNominatim(trimmed)
        osmMatches.forEach { candidate ->
            if (combinedResults.none { isSamePlace(it, candidate) }) {
                combinedResults.add(candidate)
            }
        }

        return@withContext combinedResults
    }

    private fun isSamePlace(a: CityLocation, b: CityLocation): Boolean {
        val dist = sqrt((a.latitude - b.latitude).pow(2) + (a.longitude - b.longitude).pow(2))
        return dist < 0.02 && a.name.equals(b.name, ignoreCase = true)
    }

    @Suppress("DEPRECATION")
    private fun searchViaAndroidGeocoder(query: String): List<CityLocation> {
        return try {
            if (!Geocoder.isPresent()) return emptyList()
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(query, 6) ?: return emptyList()
            addresses.mapNotNull { addr ->
                if (!addr.hasLatitude() || !addr.hasLongitude()) return@mapNotNull null
                val lat = addr.latitude
                val lon = addr.longitude
                val primaryName = addr.subLocality
                    ?: addr.locality
                    ?: addr.featureName
                    ?: addr.subAdminArea
                    ?: query
                val cityOrState = listOfNotNull(
                    addr.locality?.takeIf { !it.equals(primaryName, ignoreCase = true) },
                    addr.adminArea,
                    addr.countryName
                ).joinToString(", ").ifEmpty { addr.countryName ?: "Global" }

                val tzOffset = PrayerTimeCalculator.estimateTimeZoneOffsetHours(
                    latitude = lat,
                    longitude = lon,
                    countryOrRegion = "${addr.countryName.orEmpty()} ${addr.countryCode.orEmpty()}"
                )
                val cleanId = "geo_${primaryName.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "_")}_${(lat * 100).toInt()}"
                CityLocation(
                    id = cleanId,
                    name = primaryName,
                    country = cityOrState,
                    latitude = lat,
                    longitude = lon,
                    timeZoneOffsetHours = tzOffset
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun searchViaOpenStreetMapNominatim(query: String): List<CityLocation> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&addressdetails=1&limit=8")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("User-Agent", "NoorPrayerTrackerAndroid/2.0 (IslamicPrayerApp)")
                setRequestProperty("Accept-Language", "en")
            }

            if (conn.responseCode != 200) return emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(body)
            val list = mutableListOf<CityLocation>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val lat = obj.optString("lat").toDoubleOrNull() ?: continue
                val lon = obj.optString("lon").toDoubleOrNull() ?: continue
                val rawName = obj.optString("name").ifBlank {
                    obj.optString("display_name").substringBefore(",")
                }
                val addressObj = obj.optJSONObject("address")
                val suburb = addressObj?.optString("suburb")?.takeIf { it.isNotBlank() }
                val neighbourhood = addressObj?.optString("neighbourhood")?.takeIf { it.isNotBlank() }
                val town = addressObj?.optString("town")?.takeIf { it.isNotBlank() }
                    ?: addressObj?.optString("city")?.takeIf { it.isNotBlank() }
                    ?: addressObj?.optString("county")?.takeIf { it.isNotBlank() }
                val state = addressObj?.optString("state")?.takeIf { it.isNotBlank() }
                val country = addressObj?.optString("country")?.takeIf { it.isNotBlank() } ?: ""
                val countryCode = addressObj?.optString("country_code")?.takeIf { it.isNotBlank() } ?: ""

                val displayTitle = when {
                    suburb != null && town != null && !suburb.equals(town, ignoreCase = true) -> "$suburb, $town"
                    neighbourhood != null && town != null && !neighbourhood.equals(town, ignoreCase = true) -> "$neighbourhood, $town"
                    town != null && !rawName.equals(town, ignoreCase = true) -> "$rawName, $town"
                    else -> rawName
                }

                val regionSubtitle = listOfNotNull(state, country).joinToString(", ").ifBlank {
                    obj.optString("display_name").substringAfter(",", "").trim()
                }

                val tzOffset = PrayerTimeCalculator.estimateTimeZoneOffsetHours(
                    latitude = lat,
                    longitude = lon,
                    countryOrRegion = "$country $countryCode"
                )
                val id = "osm_${displayTitle.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "_")}_${(lat * 100).toInt()}_${(lon * 100).toInt()}"

                list.add(
                    CityLocation(
                        id = id,
                        name = displayTitle,
                        country = regionSubtitle,
                        latitude = lat,
                        longitude = lon,
                        timeZoneOffsetHours = tzOffset
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun resolveLocationWithFallback(
        fallbackCities: List<CityLocation>,
        selectedFallbackCity: CityLocation
    ): LocationResolutionResult = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            val tzCity = PrayerTimeCalculator.detectBestDefaultCityForDeviceTimeZone()
            return@withContext LocationResolutionResult.FallbackSqliteCity(
                cityLocation = tzCity,
                reason = "Location permission needed • Auto-selected ${tzCity.name} from device timezone"
            )
        }

        if (!isDeviceLocationEnabled()) {
            val tzCity = PrayerTimeCalculator.detectBestDefaultCityForDeviceTimeZone()
            return@withContext LocationResolutionResult.FallbackSqliteCity(
                cityLocation = tzCity,
                reason = "Turn on Device Location/GPS in Quick Settings • Using ${tzCity.name}"
            )
        }

        // Stage 1: Try FusedLocationProviderClient.lastLocation
        var resolvedPair: Pair<Location, String>? = fetchFusedLastLocation()?.let { it to "Fused LastFix" }

        // Stage 2: Try FusedLocationProviderClient.getCurrentLocation(HIGH_ACCURACY)
        if (resolvedPair == null) {
            resolvedPair = withTimeoutOrNull(4500L) {
                fetchFusedCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY)
            }?.let { it to "Fused GPS High-Accuracy" }
        }

        // Stage 3: Try FusedLocationProviderClient.getCurrentLocation(BALANCED)
        if (resolvedPair == null) {
            resolvedPair = withTimeoutOrNull(3500L) {
                fetchFusedCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            }?.let { it to "Fused Network/Wi-Fi" }
        }

        // Stage 4: Try FusedLocationProviderClient active single update
        if (resolvedPair == null) {
            resolvedPair = withTimeoutOrNull(4000L) {
                fetchSingleFusedUpdate()
            }?.let { it to "Fused Active Update" }
        }

        // Stage 5: Try Android native LocationManager
        if (resolvedPair == null) {
            resolvedPair = fetchNativeLocationManagerFix()
        }

        if (resolvedPair != null) {
            val (loc, providerLabel) = resolvedPair
            val lat = loc.latitude
            val lon = loc.longitude
            val tzOffsetHours = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (1000.0 * 60.0 * 60.0)

            val geocoded = reverseGeocodeNeighborhoodAndCity(lat, lon)
            val nearestCity = fallbackCities.minByOrNull { city ->
                sqrt((city.latitude - lat).pow(2) + (city.longitude - lon).pow(2))
            }

            val cityName = geocoded?.first
                ?: nearestCity?.takeIf {
                    sqrt((it.latitude - lat).pow(2) + (it.longitude - lon).pow(2)) < 1.5
                }?.name
                ?: "GPS (${String.format(Locale.US, "%.2f", lat)}, ${String.format(Locale.US, "%.2f", lon)})"

            val countryName = geocoded?.second
                ?: nearestCity?.takeIf {
                    sqrt((it.latitude - lat).pow(2) + (it.longitude - lon).pow(2)) < 1.5
                }?.country
                ?: "GPS"

            val resolvedCity = CityLocation(
                id = "gps_live",
                name = cityName,
                country = countryName,
                latitude = lat,
                longitude = lon,
                timeZoneOffsetHours = tzOffsetHours,
                isGpsSimulated = true
            )

            return@withContext LocationResolutionResult.GpsResolved(
                cityLocation = resolvedCity,
                rawLatitude = lat,
                rawLongitude = lon,
                providerLabel = providerLabel
            )
        } else {
            val tzCity = PrayerTimeCalculator.detectBestDefaultCityForDeviceTimeZone()
            return@withContext LocationResolutionResult.FallbackSqliteCity(
                cityLocation = tzCity,
                reason = "GPS signal weak indoors • Auto-matched ${tzCity.name} from device timezone"
            )
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchFusedLastLocation(): Location? =
        suspendCancellableCoroutine { cont ->
            try {
                fusedClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (cont.isActive) cont.resume(location)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun fetchFusedCurrentLocation(priority: Int): Location? =
        suspendCancellableCoroutine { cont ->
            try {
                val cts = CancellationTokenSource()
                cont.invokeOnCancellation { cts.cancel() }
                fusedClient.getCurrentLocation(priority, cts.token)
                    .addOnSuccessListener { location ->
                        if (cont.isActive) cont.resume(location)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun fetchSingleFusedUpdate(): Location? =
        suspendCancellableCoroutine { cont ->
            try {
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                    .setWaitForAccurateLocation(false)
                    .setMaxUpdates(1)
                    .build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        fusedClient.removeLocationUpdates(this)
                        if (cont.isActive) {
                            cont.resume(result.lastLocation)
                        }
                    }
                }
                cont.invokeOnCancellation {
                    fusedClient.removeLocationUpdates(callback)
                }
                fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun fetchNativeLocationManagerFix(): Pair<Location, String>? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER to "Hardware GPS",
            LocationManager.NETWORK_PROVIDER to "Network Location",
            LocationManager.PASSIVE_PROVIDER to "Cached Location"
        )

        var bestLastKnown: Pair<Location, String>? = null
        for ((provider, label) in providers) {
            val loc = runCatching { systemLocationManager.getLastKnownLocation(provider) }.getOrNull()
            if (loc != null) {
                if (bestLastKnown == null || loc.time > bestLastKnown.first.time) {
                    bestLastKnown = loc to label
                }
            }
        }
        if (bestLastKnown != null) return bestLastKnown

        val activeProvider = when {
            runCatching { systemLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) ->
                LocationManager.NETWORK_PROVIDER
            runCatching { systemLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) ->
                LocationManager.GPS_PROVIDER
            else -> null
        } ?: return null

        val activeLoc = withTimeoutOrNull(4000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { systemLocationManager.removeUpdates(this) }
                        if (cont.isActive) cont.resume(location)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                cont.invokeOnCancellation {
                    runCatching { systemLocationManager.removeUpdates(listener) }
                }
                try {
                    systemLocationManager.requestLocationUpdates(
                        activeProvider,
                        0L,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
        return activeLoc?.let { it to "Android LocationManager" }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocodeNeighborhoodAndCity(lat: Double, lon: Double): Pair<String, String>? {
        return try {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull() ?: return null
            val subLoc = addr.subLocality
            val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: return null
            val title = if (!subLoc.isNullOrBlank() && !subLoc.equals(locality, ignoreCase = true)) {
                "$subLoc, $locality"
            } else {
                locality
            }
            val country = listOfNotNull(addr.adminArea, addr.countryName).joinToString(", ")
            Pair(title, country)
        } catch (e: Exception) {
            null
        }
    }
}
