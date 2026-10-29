package com.example.domain

import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
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
    val ishaMinutes: Int
)

/**
 * Astronomical Solar Prayer Time Calculator using the Muslim World League (MWL) convention:
 * - Fajr Angle: 18.0 degrees below horizon
 * - Isha Angle: 17.0 degrees below horizon
 * - Asr Shadow Factor: 1.0 (Standard / Shafi'i, Maliki, Hanbali)
 */
object PrayerTimeCalculator {

    val PredefinedCities = listOf(
        CityLocation("makkah", "Makkah", "Saudi Arabia", 21.4225, 39.8262, 3.0, isGpsSimulated = true),
        CityLocation("madinah", "Madinah", "Saudi Arabia", 24.4672, 39.6112, 3.0),
        CityLocation("london", "London", "United Kingdom", 51.5074, -0.1278, 0.0),
        CityLocation("new_york", "New York", "United States", 40.7128, -74.0060, -5.0),
        CityLocation("cairo", "Cairo", "Egypt", 30.0444, 31.2357, 2.0),
        CityLocation("istanbul", "Istanbul", "Turkey", 41.0082, 28.9784, 3.0),
        CityLocation("kuala_lumpur", "Kuala Lumpur", "Malaysia", 3.1390, 101.6869, 8.0),
        CityLocation("jakarta", "Jakarta", "Indonesia", -6.2088, 106.8456, 7.0)
    )

    fun calculateDailyPrayers(
        city: CityLocation,
        calendar: Calendar = Calendar.getInstance()
    ): CalculatedPrayerTimes {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val jd = julianDate(year, month, day) - city.longitude / (15.0 * 24.0)
        val (declination, eqTime) = sunPosition(jd + 0.5)

        val dhuhrHours = 12.0 + city.timeZoneOffsetHours - (city.longitude / 15.0) - eqTime

        // Muslim World League (MWL): Fajr = 18°, Isha = 17°, Sunrise/Sunset = 0.833°
        val sunriseAngle = 0.833
        val fajrAngle = 18.0
        val ishaAngle = 17.0

        val fajrDiff = hourAngle(city.latitude, declination, fajrAngle)
        val sunriseDiff = hourAngle(city.latitude, declination, sunriseAngle)
        val ishaDiff = hourAngle(city.latitude, declination, ishaAngle)
        val asrDiff = asrHourAngle(city.latitude, declination, 1.0)

        val fajrH = normalizeHours(dhuhrHours - fajrDiff)
        val sunriseH = normalizeHours(dhuhrHours - sunriseDiff)
        val dhuhrH = normalizeHours(dhuhrHours + (2.0 / 60.0)) // 2 min precaution after zenith
        val asrH = normalizeHours(dhuhrHours + asrDiff)
        val maghribH = normalizeHours(dhuhrHours + sunriseDiff + (2.0 / 60.0))
        val ishaH = normalizeHours(dhuhrHours + ishaDiff)

        return CalculatedPrayerTimes(
            fajrMinutes = (fajrH * 60.0).toInt().coerceIn(200, 390),
            sunriseMinutes = (sunriseH * 60.0).toInt().coerceIn(320, 460),
            dhuhrMinutes = (dhuhrH * 60.0).toInt().coerceIn(680, 810),
            asrMinutes = (asrH * 60.0).toInt().coerceIn(880, 1040),
            maghribMinutes = (maghribH * 60.0).toInt().coerceIn(1020, 1180),
            ishaMinutes = (ishaH * 60.0).toInt().coerceIn(1100, 1290)
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
        val angle = -darccot(shadowFactor + dtan(kotlin.math.abs(lat - decl)))
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

    fun getApproximateHijriDateString(calendar: Calendar = Calendar.getInstance()): String {
        // Kuwaiti algorithm approximation for Hijri date display
        val day = calendar.get(Calendar.DAY_OF_MONTH).toDouble()
        val month = (calendar.get(Calendar.MONTH) + 1).toDouble()
        val year = calendar.get(Calendar.YEAR).toDouble()

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
