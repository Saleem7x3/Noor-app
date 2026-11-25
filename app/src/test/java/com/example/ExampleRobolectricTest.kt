package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PrayerItemUiModel
import com.example.domain.PrayerTimeCalculator
import com.example.notification.PrayerNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context and verify elapsed time formatting`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Noor Prayer", appName)
        assertEquals("12m 34s", PrayerNotificationHelper.formatElapsedSecondsOrHours(12 * 60 + 34))
        assertEquals("1h 15m 20s", PrayerNotificationHelper.formatElapsedSecondsOrHours(75 * 60 + 20))
    }

    @Test
    fun `fixed adhan time is clamped to never precede solar prayer start time`() {
        val fajrLaterThanFixed = PrayerItemUiModel(
            id = "FAJR",
            nameEnglish = "Fajr",
            nameArabic = "الفجر",
            pstMinutes = 5 * 60 + 40, // 05:40 AM solar PST
            useFixedAdhanTime = true,
            fixedAdhanTimeMinutes = 5 * 60, // 05:00 AM fixed Adhan
            jamaahOffsetMinutes = 20
        )
        assertEquals(5 * 60 + 40, fajrLaterThanFixed.adhanMinutes)
        assertEquals(6 * 60, fajrLaterThanFixed.jamaahMinutes)
        assertTrue(fajrLaterThanFixed.adhanMinutes >= fajrLaterThanFixed.pstMinutes)
        assertTrue(fajrLaterThanFixed.jamaahMinutes >= fajrLaterThanFixed.adhanMinutes)
    }

    @Test
    fun `prayer times and astronomical islamic midnight maintain strict chronological order`() {
        val testCities = listOf("makkah", "london", "oslo", "mumbai", "new_york")
        val months = listOf(Calendar.JANUARY, Calendar.JUNE, Calendar.NOVEMBER)
        for (cityId in testCities) {
            val city = PrayerTimeCalculator.PredefinedCities.first { it.id == cityId }
            for (month in months) {
                val cal = PrayerTimeCalculator.getCalendarForCity(city).apply {
                    set(Calendar.YEAR, 2026)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, 15)
                }
                val times = PrayerTimeCalculator.calculateDailyPrayers(city, cal)
                assertTrue("$cityId month=$month fajr < sunrise", times.fajrMinutes < times.sunriseMinutes)
                assertTrue("$cityId month=$month sunrise < dhuhr", times.sunriseMinutes < times.dhuhrMinutes)
                assertTrue("$cityId month=$month dhuhr < asr", times.dhuhrMinutes < times.asrMinutes)
                assertTrue("$cityId month=$month asr < maghrib", times.asrMinutes < times.maghribMinutes)
                assertTrue("$cityId month=$month maghrib < isha", times.maghribMinutes < times.ishaMinutes)
                assertTrue("$cityId month=$month isha < islamicMidnight", times.ishaMinutes < times.islamicMidnightMinutes)
            }
        }
    }
}
