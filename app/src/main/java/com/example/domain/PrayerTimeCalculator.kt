package com.example.domain

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

data class CityLocation(
    val id: String,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneOffsetHours: Double,
    val isGpsSimulated: Boolean = false
)

data class CalculatedPrayerTimes(
    val fajrMinutes: Int,
    val sunriseMinutes: Int,
    val dhuhrMinutes: Int,
    val asrMinutes: Int,
    val maghribMinutes: Int,
    val ishaMinutes: Int,
    val islamicMidnightMinutes: Int = 1408 // Continuous minutes from 00:00 (midpoint between Maghrib and Fajr)
)

enum class AsrMadhab(
    val displayName: String,
    val shortLabel: String,
    val subtitle: String,
    val shadowFactor: Double
) {
    STANDARD(
        displayName = "Standard (Shafi'i, Maliki, Hanbali)",
        shortLabel = "Standard",
        subtitle = "Earlier Asr · 1× object shadow",
        shadowFactor = 1.0
    ),
    HANAFI(
        displayName = "Hanafi",
        shortLabel = "Hanafi",
        subtitle = "Later Asr · 2× object shadow",
        shadowFactor = 2.0
    );

    companion object {
        fun fromKey(raw: String?): AsrMadhab {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: STANDARD
        }
    }
}

enum class CalculationMethod(
    val shortCode: String,
    val displayName: String,
    val regionHint: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaMinutesAfterMaghrib: Int? = null
) {
    MWL(
        shortCode = "MWL",
        displayName = "Muslim World League",
        regionHint = "Europe, Far East, Global default",
        fajrAngle = 18.0,
        ishaAngle = 17.0
    ),
    KARACHI(
        shortCode = "Karachi",
        displayName = "Univ. of Islamic Sciences, Karachi",
        regionHint = "India, Pakistan, Bangladesh, South Asia",
        fajrAngle = 18.0,
        ishaAngle = 18.0
    ),
    ISNA(
        shortCode = "ISNA",
        displayName = "Islamic Society of North America",
        regionHint = "United States, Canada",
        fajrAngle = 15.0,
        ishaAngle = 15.0
    ),
    UMM_AL_QURA(
        shortCode = "Makkah",
        displayName = "Umm al-Qura University, Makkah",
        regionHint = "Saudi Arabia, Arabian Peninsula",
        fajrAngle = 18.5,
        ishaAngle = 0.0,
        ishaMinutesAfterMaghrib = 90
    ),
    EGYPT(
        shortCode = "Egypt",
        displayName = "Egyptian General Authority of Survey",
        regionHint = "Egypt, Africa, Syria, Lebanon",
        fajrAngle = 19.5,
        ishaAngle = 17.5
    ),
    GULF_DUBAI(
        shortCode = "Dubai",
        displayName = "Dubai / Gulf Region",
        regionHint = "UAE, Oman, Bahrain",
        fajrAngle = 18.2,
        ishaAngle = 18.2
    ),
    QATAR(
        shortCode = "Qatar",
        displayName = "Qatar",
        regionHint = "Qatar (90m after Maghrib)",
        fajrAngle = 18.0,
        ishaAngle = 0.0,
        ishaMinutesAfterMaghrib = 90
    ),
    KUWAIT(
        shortCode = "Kuwait",
        displayName = "Kuwait",
        regionHint = "Kuwait",
        fajrAngle = 18.0,
        ishaAngle = 17.5
    ),
    TURKEY_DIYANET(
        shortCode = "Diyanet",
        displayName = "Turkey (Diyanet)",
        regionHint = "Turkey, Balkans",
        fajrAngle = 18.0,
        ishaAngle = 17.0
    ),
    SINGAPORE_MUIS(
        shortCode = "MUIS",
        displayName = "Singapore (MUIS) / SE Asia",
        regionHint = "Singapore, Malaysia, Indonesia, Brunei",
        fajrAngle = 20.0,
        ishaAngle = 18.0
    ),
    TEHRAN(
        shortCode = "Tehran",
        displayName = "Institute of Geophysics, Tehran",
        regionHint = "Iran, Central Asia",
        fajrAngle = 17.7,
        ishaAngle = 14.0
    ),
    MOONSIGHTING(
        shortCode = "Moonsighting",
        displayName = "Moonsighting Committee Worldwide",
        regionHint = "Global high-latitude friendly",
        fajrAngle = 18.0,
        ishaAngle = 18.0
    ),
    MANUAL(
        shortCode = "Manual",
        displayName = "Manual / Custom Angles",
        regionHint = "Set your own Fajr & Isha degrees or minutes",
        fajrAngle = 18.0,
        ishaAngle = 17.0
    );

    val angleSpecLabel: String
        get() {
            val f = formatAngle(fajrAngle)
            val i = ishaMinutesAfterMaghrib?.let { "${it}m" } ?: formatAngle(ishaAngle)
            return "$f / $i"
        }

    companion object {
        fun fromKey(raw: String?): CalculationMethod {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MWL
        }

        fun formatAngle(angle: Double): String {
            val rounded = (angle * 10.0).roundToInt() / 10.0
            return if (abs(rounded - rounded.toInt()) < 0.01) {
                "${rounded.toInt()}°"
            } else {
                String.format(Locale.US, "%.1f°", rounded)
            }
        }
    }
}

data class PrayerCalculationConfig(
    val method: CalculationMethod = CalculationMethod.MWL,
    val asrMadhab: AsrMadhab = AsrMadhab.STANDARD,
    val manualFajrAngle: Double = 18.0,
    val manualIshaUseFixedMinutes: Boolean = false,
    val manualIshaAngle: Double = 17.0,
    val manualIshaMinutesAfterMaghrib: Int = 90
) {
    val effectiveFajrAngle: Double
        get() = if (method == CalculationMethod.MANUAL) {
            manualFajrAngle.coerceIn(10.0, 24.0)
        } else {
            method.fajrAngle
        }

    val effectiveIshaFixedMinutes: Int?
        get() = if (method == CalculationMethod.MANUAL) {
            if (manualIshaUseFixedMinutes) manualIshaMinutesAfterMaghrib.coerceIn(45, 150) else null
        } else {
            method.ishaMinutesAfterMaghrib
        }

    val effectiveIshaAngle: Double
        get() = if (method == CalculationMethod.MANUAL) {
            manualIshaAngle.coerceIn(10.0, 24.0)
        } else {
            method.ishaAngle.coerceIn(10.0, 24.0)
        }

    val angleSpecLabel: String
        get() {
            val f = CalculationMethod.formatAngle(effectiveFajrAngle)
            val i = effectiveIshaFixedMinutes?.let { "${it}m" }
                ?: CalculationMethod.formatAngle(effectiveIshaAngle)
            return "$f/$i"
        }

    val shortSummaryBadge: String
        get() = "${method.shortCode} $angleSpecLabel · ${asrMadhab.shortLabel}"
}

/**
 * Astronomical Solar Prayer Time Calculator supporting global calculation conventions,
 * custom manual Fajr/Isha twilight angles (or fixed minutes after Maghrib),
 * and Standard (Shafi'i, Maliki, Hanbali — 1×) or Hanafi (2×) Asr juristic shadow factors.
 */
object PrayerTimeCalculator {

    val PredefinedCities = listOf(
        CityLocation("abidjan", "Abidjan", "Ivory Coast", 5.3600, -4.0083, 0.0),
        CityLocation("abu_dhabi", "Abu Dhabi", "United Arab Emirates", 24.4539, 54.3773, 4.0),
        CityLocation("abuja", "Abuja", "Nigeria", 9.0765, 7.3986, 1.0),
        CityLocation("accra", "Accra", "Ghana", 5.6037, -0.1870, 0.0),
        CityLocation("addis_ababa", "Addis Ababa", "Ethiopia", 9.0320, 38.7469, 3.0),
        CityLocation("adelaide", "Adelaide", "Australia", -34.9285, 138.6007, 9.5),
        CityLocation("ahmedabad", "Ahmedabad", "India", 23.0225, 72.5714, 5.5),
        CityLocation("alexandria", "Alexandria", "Egypt", 31.2001, 29.9187, 2.0),
        CityLocation("algiers", "Algiers", "Algeria", 36.7538, 3.0588, 1.0),
        CityLocation("almaty", "Almaty", "Kazakhstan", 43.2220, 76.8512, 5.0),
        CityLocation("amman", "Amman", "Jordan", 31.9454, 35.9284, 3.0),
        CityLocation("amsterdam", "Amsterdam", "Netherlands", 52.3676, 4.9041, 1.0),
        CityLocation("ankara", "Ankara", "Turkey", 39.9334, 32.8597, 3.0),
        CityLocation("astana", "Astana", "Kazakhstan", 51.1694, 71.4491, 5.0),
        CityLocation("athens", "Athens", "Greece", 37.9838, 23.7275, 2.0),
        CityLocation("atlanta", "Atlanta", "United States", 33.7490, -84.3880, -5.0),
        CityLocation("auckland", "Auckland", "New Zealand", -36.8485, 174.7633, 12.0),
        CityLocation("baghdad", "Baghdad", "Iraq", 33.3152, 44.3661, 3.0),
        CityLocation("baku", "Baku", "Azerbaijan", 40.4093, 49.8671, 4.0),
        CityLocation("bandar_seri_begawan", "Bandar Seri Begawan", "Brunei", 4.9031, 114.9398, 8.0),
        CityLocation("bandung", "Bandung", "Indonesia", -6.9175, 107.6191, 7.0),
        CityLocation("bangkok", "Bangkok", "Thailand", 13.7563, 100.5018, 7.0),
        CityLocation("barcelona", "Barcelona", "Spain", 41.3874, 2.1686, 1.0),
        CityLocation("beirut", "Beirut", "Lebanon", 33.8938, 35.5018, 2.0),
        CityLocation("bengaluru", "Bengaluru", "India", 12.9716, 77.5946, 5.5),
        CityLocation("berlin", "Berlin", "Germany", 52.5200, 13.4050, 1.0),
        CityLocation("birmingham", "Birmingham", "United Kingdom", 52.4862, -1.8904, 0.0),
        CityLocation("bishkek", "Bishkek", "Kyrgyzstan", 42.8746, 74.5698, 6.0),
        CityLocation("boston", "Boston", "United States", 42.3601, -71.0589, -5.0),
        CityLocation("brussels", "Brussels", "Belgium", 50.8503, 4.3517, 1.0),
        CityLocation("buenos_aires", "Buenos Aires", "Argentina", -34.6037, -58.3816, -3.0),
        CityLocation("cairo", "Cairo", "Egypt", 30.0444, 31.2357, 2.0),
        CityLocation("cape_town", "Cape Town", "South Africa", -33.9249, 18.4241, 2.0),
        CityLocation("casablanca", "Casablanca", "Morocco", 33.5731, -7.5898, 1.0),
        CityLocation("chennai", "Chennai", "India", 13.0827, 80.2707, 5.5),
        CityLocation("chicago", "Chicago", "United States", 41.8781, -87.6298, -6.0),
        CityLocation("colombo", "Colombo", "Sri Lanka", 6.9271, 79.8612, 5.5),
        CityLocation("copenhagen", "Copenhagen", "Denmark", 55.6761, 12.5683, 1.0),
        CityLocation("dakar", "Dakar", "Senegal", 14.7167, -17.4677, 0.0),
        CityLocation("dallas", "Dallas", "United States", 32.7767, -96.7970, -6.0),
        CityLocation("damascus", "Damascus", "Syria", 33.5138, 36.2765, 3.0),
        CityLocation("dar_es_salaam", "Dar es Salaam", "Tanzania", -6.7924, 39.2083, 3.0),
        CityLocation("delhi", "Delhi (New Delhi)", "India", 28.6139, 77.2090, 5.5),
        CityLocation("detroit", "Detroit", "United States", 42.3314, -83.0458, -5.0),
        CityLocation("dhaka", "Dhaka", "Bangladesh", 23.8103, 90.4125, 6.0),
        CityLocation("doha", "Doha", "Qatar", 25.2854, 51.5310, 3.0),
        CityLocation("dubai", "Dubai", "United Arab Emirates", 25.2048, 55.2708, 4.0),
        CityLocation("dublin", "Dublin", "Ireland", 53.3498, -6.2603, 0.0),
        CityLocation("durban", "Durban", "South Africa", -29.8587, 31.0218, 2.0),
        CityLocation("dushanbe", "Dushanbe", "Tajikistan", 38.5598, 68.7870, 5.0),
        CityLocation("fez", "Fez", "Morocco", 34.0181, -5.0078, 1.0),
        CityLocation("frankfurt", "Frankfurt", "Germany", 50.1109, 8.6821, 1.0),
        CityLocation("geneva", "Geneva", "Switzerland", 46.2044, 6.1432, 1.0),
        CityLocation("guangzhou", "Guangzhou", "China", 23.1291, 113.2644, 8.0),
        CityLocation("hong_kong", "Hong Kong", "China", 22.3193, 114.1694, 8.0),
        CityLocation("houston", "Houston", "United States", 29.7604, -95.3698, -6.0),
        CityLocation("hyderabad", "Hyderabad", "India", 17.3850, 78.4867, 5.5),
        CityLocation("islamabad", "Islamabad", "Pakistan", 33.6844, 73.0479, 5.0),
        CityLocation("istanbul", "Istanbul", "Turkey", 41.0082, 28.9784, 3.0),
        CityLocation("jakarta", "Jakarta", "Indonesia", -6.2088, 106.8456, 7.0),
        CityLocation("jeddah", "Jeddah", "Saudi Arabia", 21.5433, 39.1728, 3.0),
        CityLocation("jerusalem", "Jerusalem", "Palestine", 31.7683, 35.2137, 2.0),
        CityLocation("johannesburg", "Johannesburg", "South Africa", -26.2041, 28.0473, 2.0),
        CityLocation("kabul", "Kabul", "Afghanistan", 34.5553, 69.2075, 4.5),
        CityLocation("kampala", "Kampala", "Uganda", 0.3476, 32.5825, 3.0),
        CityLocation("kano", "Kano", "Nigeria", 12.0022, 8.5920, 1.0),
        CityLocation("karachi", "Karachi", "Pakistan", 24.8607, 67.0011, 5.0),
        CityLocation("kathmandu", "Kathmandu", "Nepal", 27.7172, 85.3240, 5.75),
        CityLocation("khartoum", "Khartoum", "Sudan", 15.5007, 32.5599, 2.0),
        CityLocation("kochi", "Kochi", "India", 9.9312, 76.2673, 5.5),
        CityLocation("kolkata", "Kolkata", "India", 22.5726, 88.3639, 5.5),
        CityLocation("kozhikode", "Kozhikode", "India", 11.2588, 75.7804, 5.5),
        CityLocation("kuala_lumpur", "Kuala Lumpur", "Malaysia", 3.1390, 101.6869, 8.0),
        CityLocation("kuwait_city", "Kuwait City", "Kuwait", 29.3759, 47.9774, 3.0),
        CityLocation("lagos", "Lagos", "Nigeria", 6.5244, 3.3792, 1.0),
        CityLocation("lahore", "Lahore", "Pakistan", 31.5204, 74.3587, 5.0),
        CityLocation("london", "London", "United Kingdom", 51.5074, -0.1278, 0.0),
        CityLocation("los_angeles", "Los Angeles", "United States", 34.0522, -118.2437, -8.0),
        CityLocation("lucknow", "Lucknow", "India", 26.8467, 80.9462, 5.5),
        CityLocation("madinah", "Madinah", "Saudi Arabia", 24.4672, 39.6112, 3.0),
        CityLocation("madrid", "Madrid", "Spain", 40.4168, -3.7038, 1.0),
        CityLocation("makkah", "Makkah", "Saudi Arabia", 21.4225, 39.8262, 3.0),
        CityLocation("manama", "Manama", "Bahrain", 26.2285, 50.5860, 3.0),
        CityLocation("manchester", "Manchester", "United Kingdom", 53.4808, -2.2426, 0.0),
        CityLocation("manila", "Manila", "Philippines", 14.5995, 120.9842, 8.0),
        CityLocation("marrakesh", "Marrakesh", "Morocco", 31.6295, -7.9811, 1.0),
        CityLocation("melbourne", "Melbourne", "Australia", -37.8136, 144.9631, 10.0),
        CityLocation("milan", "Milan", "Italy", 45.4642, 9.1900, 1.0),
        CityLocation("mogadishu", "Mogadishu", "Somalia", 2.0469, 45.3182, 3.0),
        CityLocation("mombasa", "Mombasa", "Kenya", -4.0435, 39.6682, 3.0),
        CityLocation("montreal", "Montreal", "Canada", 45.5017, -73.5673, -5.0),
        CityLocation("moscow", "Moscow", "Russia", 55.7558, 37.6173, 3.0),
        CityLocation("multan", "Multan", "Pakistan", 30.1575, 71.5249, 5.0),
        CityLocation("mumbai", "Mumbai", "India", 19.0760, 72.8777, 5.5),
        CityLocation("munich", "Munich", "Germany", 48.1351, 11.5820, 1.0),
        CityLocation("muscat", "Muscat", "Oman", 23.5880, 58.3829, 4.0),
        CityLocation("nairobi", "Nairobi", "Kenya", -1.2921, 36.8219, 3.0),
        CityLocation("new_york", "New York", "United States", 40.7128, -74.0060, -5.0),
        CityLocation("oslo", "Oslo", "Norway", 59.9139, 10.7522, 1.0),
        CityLocation("ottawa", "Ottawa", "Canada", 45.4215, -75.6972, -5.0),
        CityLocation("paris", "Paris", "France", 48.8566, 2.3522, 1.0),
        CityLocation("perth", "Perth", "Australia", -31.9505, 115.8605, 8.0),
        CityLocation("peshawar", "Peshawar", "Pakistan", 34.0151, 71.5249, 5.0),
        CityLocation("philadelphia", "Philadelphia", "United States", 39.9526, -75.1652, -5.0),
        CityLocation("rabat", "Rabat", "Morocco", 34.0209, -6.8416, 1.0),
        CityLocation("riyadh", "Riyadh", "Saudi Arabia", 24.7136, 46.6753, 3.0),
        CityLocation("rome", "Rome", "Italy", 41.9028, 12.4964, 1.0),
        CityLocation("san_francisco", "San Francisco", "United States", 37.7749, -122.4194, -8.0),
        CityLocation("sanaa", "Sanaa", "Yemen", 15.3694, 44.1910, 3.0),
        CityLocation("sarajevo", "Sarajevo", "Bosnia & Herzegovina", 43.8563, 18.4131, 1.0),
        CityLocation("seattle", "Seattle", "United States", 47.6062, -122.3321, -8.0),
        CityLocation("seoul", "Seoul", "South Korea", 37.5665, 126.9780, 9.0),
        CityLocation("sharjah", "Sharjah", "United Arab Emirates", 25.3463, 55.4209, 4.0),
        CityLocation("singapore", "Singapore", "Singapore", 1.3521, 103.8198, 8.0),
        CityLocation("srinagar", "Srinagar", "India", 34.0837, 74.7973, 5.5),
        CityLocation("stockholm", "Stockholm", "Sweden", 59.3293, 18.0686, 1.0),
        CityLocation("surabaya", "Surabaya", "Indonesia", -7.2575, 112.7521, 7.0),
        CityLocation("sydney", "Sydney", "Australia", -33.8688, 151.2093, 10.0),
        CityLocation("tashkent", "Tashkent", "Uzbekistan", 41.2995, 69.2401, 5.0),
        CityLocation("tehran", "Tehran", "Iran", 35.6892, 51.3890, 3.5),
        CityLocation("tokyo", "Tokyo", "Japan", 35.6762, 139.6503, 9.0),
        CityLocation("toronto", "Toronto", "Canada", 43.6532, -79.3832, -5.0),
        CityLocation("tripoli", "Tripoli", "Libya", 32.8872, 13.1913, 2.0),
        CityLocation("tunis", "Tunis", "Tunisia", 36.8065, 10.1815, 1.0),
        CityLocation("vancouver", "Vancouver", "Canada", 49.2827, -123.1207, -8.0),
        CityLocation("vienna", "Vienna", "Austria", 48.2082, 16.3738, 1.0),
        CityLocation("washington_dc", "Washington, D.C.", "United States", 38.9072, -77.0369, -5.0),
        CityLocation("zurich", "Zurich", "Switzerland", 47.3769, 8.5417, 1.0)
    )

    fun getTimeZoneForCity(city: CityLocation): TimeZone {
        val totalMinutes = (city.timeZoneOffsetHours * 60.0).roundToInt()
        val sign = if (totalMinutes >= 0) "+" else "-"
        val absMin = abs(totalMinutes)
        val hours = absMin / 60
        val mins = absMin % 60
        val tzId = String.format(Locale.US, "GMT%s%02d:%02d", sign, hours, mins)
        return TimeZone.getTimeZone(tzId)
    }

    fun getCalendarForCity(city: CityLocation): Calendar {
        return Calendar.getInstance(getTimeZoneForCity(city))
    }

    fun detectBestDefaultCityForDeviceTimeZone(): CityLocation {
        val tz = TimeZone.getDefault()
        val tzId = tz.id.lowercase(Locale.US)
        val offsetHours = tz.getOffset(System.currentTimeMillis()) / (1000.0 * 60.0 * 60.0)

        return when {
            tzId.contains("dubai") || tzId.contains("muscat") -> PredefinedCities.first { it.id == "abu_dhabi" }
            tzId.contains("riyadh") || tzId.contains("aden") || tzId.contains("kuwait") -> PredefinedCities.first { it.id == "makkah" }
            tzId.contains("qatar") -> PredefinedCities.first { it.id == "doha" }
            tzId.contains("cairo") -> PredefinedCities.first { it.id == "cairo" }
            tzId.contains("istanbul") -> PredefinedCities.first { it.id == "istanbul" }
            tzId.contains("karachi") -> PredefinedCities.first { it.id == "karachi" }
            tzId.contains("dhaka") -> PredefinedCities.first { it.id == "dhaka" }
            tzId.contains("kolkata") || tzId.contains("calcutta") || abs(offsetHours - 5.5) < 0.1 ->
                PredefinedCities.first { it.id == "mumbai" }
            tzId.contains("kuala_lumpur") -> PredefinedCities.first { it.id == "kuala_lumpur" }
            tzId.contains("jakarta") -> PredefinedCities.first { it.id == "jakarta" }
            tzId.contains("singapore") -> PredefinedCities.first { it.id == "singapore" }
            tzId.contains("london") -> PredefinedCities.first { it.id == "london" }
            tzId.contains("paris") -> PredefinedCities.first { it.id == "paris" }
            tzId.contains("berlin") -> PredefinedCities.first { it.id == "berlin" }
            tzId.contains("toronto") -> PredefinedCities.first { it.id == "toronto" }
            tzId.contains("new_york") -> PredefinedCities.first { it.id == "new_york" }
            tzId.contains("chicago") -> PredefinedCities.first { it.id == "chicago" }
            tzId.contains("los_angeles") -> PredefinedCities.first { it.id == "los_angeles" }
            tzId.contains("sydney") -> PredefinedCities.first { it.id == "sydney" }
            else -> PredefinedCities.minByOrNull { abs(it.timeZoneOffsetHours - offsetHours) }
                ?: PredefinedCities.first { it.id == "makkah" }
        }
    }

    /**
     * Accurately estimates the timezone offset (in hours) for any searched town, village, or city
     * based on its country and coordinates.
     */
    fun estimateTimeZoneOffsetHours(
        latitude: Double,
        longitude: Double,
        countryOrRegion: String
    ): Double {
        val lower = countryOrRegion.lowercase(Locale.US)
        return when {
            lower.contains("india") || lower == "in" || (latitude in 6.0..37.2 && longitude in 68.1..97.4) -> 5.5
            lower.contains("sri lanka") || lower == "lk" -> 5.5
            lower.contains("nepal") || lower == "np" -> 5.75
            lower.contains("pakistan") || lower == "pk" -> 5.0
            lower.contains("bangladesh") || lower == "bd" -> 6.0
            lower.contains("afghanistan") || lower == "af" -> 4.5
            lower.contains("iran") || lower == "ir" -> 3.5
            lower.contains("united arab emirates") || lower.contains("uae") || lower.contains("oman") -> 4.0
            lower.contains("saudi") || lower.contains("qatar") || lower.contains("kuwait") ||
                lower.contains("bahrain") || lower.contains("iraq") || lower.contains("jordan") ||
                lower.contains("turkey") || lower.contains("yemen") -> 3.0
            lower.contains("egypt") || lower.contains("palestine") || lower.contains("lebanon") -> 2.0
            lower.contains("malaysia") || lower.contains("singapore") || lower.contains("philippines") -> 8.0
            lower.contains("united kingdom") || lower == "uk" || lower == "gb" || lower.contains("ireland") -> 0.0
            else -> {
                val deviceOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (1000.0 * 60.0 * 60.0)
                val solarOffset = (longitude / 15.0)
                if (abs(deviceOffset - solarOffset) <= 1.5) {
                    deviceOffset
                } else {
                    (solarOffset * 2.0).roundToInt() / 2.0
                }
            }
        }
    }

    fun calculateDailyPrayers(
        city: CityLocation,
        calendar: Calendar = getCalendarForCity(city),
        config: PrayerCalculationConfig = PrayerCalculationConfig()
    ): CalculatedPrayerTimes {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val jd = julianDate(year, month, day) - city.longitude / (15.0 * 24.0)
        val (declination, eqTime) = sunPosition(jd + 0.5)

        val dhuhrHours = 12.0 + city.timeZoneOffsetHours - (city.longitude / 15.0) - eqTime

        val sunriseAngle = 0.833
        val fajrAngle = config.effectiveFajrAngle
        val ishaFixedMinutes = config.effectiveIshaFixedMinutes
        val ishaAngle = config.effectiveIshaAngle
        val asrShadowFactor = config.asrMadhab.shadowFactor

        val fajrDiff = hourAngle(city.latitude, declination, fajrAngle)
        val sunriseDiff = hourAngle(city.latitude, declination, sunriseAngle)
        val asrDiff = asrHourAngle(city.latitude, declination, asrShadowFactor)

        val fajrH = normalizeHours(dhuhrHours - fajrDiff)
        val sunriseH = normalizeHours(dhuhrHours - sunriseDiff)
        val dhuhrH = normalizeHours(dhuhrHours + (2.0 / 60.0))
        val asrH = normalizeHours(dhuhrHours + asrDiff)
        val maghribH = normalizeHours(dhuhrHours + sunriseDiff + (2.0 / 60.0))

        val fajrMin = (fajrH * 60.0).toInt().coerceIn(120, 480)
        val sunriseMin = (sunriseH * 60.0).toInt().coerceIn(210, 600).coerceAtLeast(fajrMin + 20)
        val dhuhrMin = (dhuhrH * 60.0).toInt().coerceIn(630, 870).coerceAtLeast(sunriseMin + 30)
        val asrMin = (asrH * 60.0).toInt().coerceIn(780, 1140).coerceAtLeast(dhuhrMin + 30)
        val maghribMin = (maghribH * 60.0).toInt().coerceIn(870, 1350).coerceAtLeast(asrMin + 20)
        val ishaMin = if (ishaFixedMinutes != null) {
            (maghribMin + ishaFixedMinutes).coerceIn(960, 1425).coerceAtLeast(maghribMin + 30)
        } else {
            val ishaDiff = hourAngle(city.latitude, declination, ishaAngle)
            val ishaH = normalizeHours(dhuhrHours + ishaDiff)
            (ishaH * 60.0).toInt().coerceIn(960, 1425).coerceAtLeast(maghribMin + 30)
        }

        // Islamic Midnight (Muntasaf al-Layl) = exact astronomical midpoint between this day's Maghrib (Sunset)
        // and the next morning's Fajr (never hardcoded to 12:00 AM)
        val nextDayCal = (calendar.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val nextYear = nextDayCal.get(Calendar.YEAR)
        val nextMonth = nextDayCal.get(Calendar.MONTH) + 1
        val nextDay = nextDayCal.get(Calendar.DAY_OF_MONTH)
        val nextJd = julianDate(nextYear, nextMonth, nextDay) - city.longitude / (15.0 * 24.0)
        val (nextDeclination, nextEqTime) = sunPosition(nextJd + 0.5)
        val nextDhuhrHours = 12.0 + city.timeZoneOffsetHours - (city.longitude / 15.0) - nextEqTime
        val nextFajrDiff = hourAngle(city.latitude, nextDeclination, fajrAngle)
        val nextFajrH = normalizeHours(nextDhuhrHours - nextFajrDiff)
        val nextDayFajrMin = (nextFajrH * 60.0).toInt().coerceIn(120, 480)

        val nightDurationMin = ((nextDayFajrMin + 1440) - maghribMin).coerceAtLeast(120)
        val islamicMidnightContinuousMin = (maghribMin + (nightDurationMin / 2)).coerceAtLeast(ishaMin + 15)

        return CalculatedPrayerTimes(
            fajrMinutes = fajrMin,
            sunriseMinutes = sunriseMin,
            dhuhrMinutes = dhuhrMin,
            asrMinutes = asrMin,
            maghribMinutes = maghribMin,
            ishaMinutes = ishaMin,
            islamicMidnightMinutes = islamicMidnightContinuousMin
        )
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0
        val eqTime = q / 15.0 - fixHour(ra)
        val decl = darcsin(dsin(e) * dsin(l))
        return Pair(decl, eqTime)
    }

    private fun hourAngle(lat: Double, decl: Double, angle: Double): Double {
        val term = (-dsin(angle) - dsin(lat) * dsin(decl)) / (dcos(lat) * dcos(decl))
        val clamped = term.coerceIn(-0.9999, 0.9999)
        return darccos(clamped) / 15.0
    }

    private fun asrHourAngle(lat: Double, decl: Double, shadowFactor: Double): Double {
        val angle = -darccot(shadowFactor + dtan(abs(lat - decl)))
        return hourAngle(lat, decl, angle)
    }

    private fun normalizeHours(h: Double): Double {
        var res = h % 24.0
        if (res < 0) res += 24.0
        return res
    }

    private fun fixAngle(a: Double): Double {
        var res = a - 360.0 * floor(a / 360.0)
        if (res < 0) res += 360.0
        return res
    }

    private fun fixHour(a: Double): Double {
        var res = a - 24.0 * floor(a / 24.0)
        if (res < 0) res += 24.0
        return res
    }

    private fun dsin(d: Double): Double = sin(d * PI / 180.0)
    private fun dcos(d: Double): Double = cos(d * PI / 180.0)
    private fun dtan(d: Double): Double = tan(d * PI / 180.0)
    private fun darcsin(x: Double): Double = asin(x) * 180.0 / PI
    private fun darccos(x: Double): Double = acos(x) * 180.0 / PI
    private fun darctan2(y: Double, x: Double): Double = atan2(y, x) * 180.0 / PI
    private fun darccot(x: Double): Double = atan(1.0 / x) * 180.0 / PI

    fun formatMinutesTo12Hour(totalMinutes: Int): String {
        val normalized = ((totalMinutes % 1440) + 1440) % 1440
        val h24 = normalized / 60
        val m = normalized % 60
        val period = if (h24 >= 12) "PM" else "AM"
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        return String.format(Locale.US, "%02d:%02d %s", h12, m, period)
    }

    fun formatMinutesTo24Hour(totalMinutes: Int): String {
        val normalized = ((totalMinutes % 1440) + 1440) % 1440
        val h24 = normalized / 60
        val m = normalized % 60
        return String.format(Locale.US, "%02d:%02d", h24, m)
    }

    fun getApproximateHijriDateString(
        calendar: Calendar = Calendar.getInstance(),
        dayOffset: Int = 0
    ): String {
        val adjustedCal = (calendar.clone() as Calendar).apply {
            if (dayOffset != 0) {
                add(Calendar.DAY_OF_YEAR, dayOffset.coerceIn(-2, 2))
            }
        }
        val day = adjustedCal.get(Calendar.DAY_OF_MONTH).toDouble()
        val month = (adjustedCal.get(Calendar.MONTH) + 1).toDouble()
        val year = adjustedCal.get(Calendar.YEAR).toDouble()

        var m = month
        var y = year
        if (m < 3) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.0

        val iyear = 10631.0 / 30.0
        val epochAstro = 1948084.0
        val shift1 = 8.01 / 60.0

        var z = jd - epochAstro
        val cyc = floor(z / 10631.0)
        z -= 10631.0 * cyc
        val j = floor((z - shift1) / iyear)
        val iy = (30 * cyc + j).toInt()
        z -= floor(j * iyear + shift1)
        var im = floor((z + 28.5001) / 29.5).toInt()
        if (im == 13) im = 12
        val id = (z - floor(29.5001 * im - 29.0)).toInt().coerceIn(1, 30)

        val hijriMonths = listOf(
            "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
            "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
        )
        val monthName = hijriMonths.getOrElse(im - 1) { "Rabi' al-Thani" }
        return "$id $monthName $iy AH"
    }
}
