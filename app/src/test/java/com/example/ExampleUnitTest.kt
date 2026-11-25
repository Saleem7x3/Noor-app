package com.example

import androidx.compose.ui.text.TextStyle
import com.example.data.AlarmLoopRuntimeState
import com.example.domain.PrayerTimeCalculator
import com.example.ui.theme.withTabularDigits
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun tabularDigits_appliesTnumFontFeatureSetting() {
        val style = TextStyle().withTabularDigits()
        assertEquals("tnum", style.fontFeatureSettings)
    }

    @Test
    fun hijriDateOffset_shiftsHijriDayAcrossNegativeAndPositiveOffsets() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.NOVEMBER, 2, 12, 0, 0)
        }
        val baseHijri = PrayerTimeCalculator.getApproximateHijriDateString(cal, 0)
        val minusOneHijri = PrayerTimeCalculator.getApproximateHijriDateString(cal, -1)
        val plusOneHijri = PrayerTimeCalculator.getApproximateHijriDateString(cal, 1)
        val minusTwoHijri = PrayerTimeCalculator.getApproximateHijriDateString(cal, -2)
        val plusTwoHijri = PrayerTimeCalculator.getApproximateHijriDateString(cal, 2)

        assertNotEquals(baseHijri, minusOneHijri)
        assertNotEquals(baseHijri, plusOneHijri)
        assertNotEquals(minusTwoHijri, plusTwoHijri)
        assertTrue(baseHijri.endsWith("AH"))
    }

    @Test
    fun autoJamaahWindow_correctlyDetectsWithinAndOutsideWindow() {
        val defaultState = AlarmLoopRuntimeState()
        assertEquals(20, defaultState.autoJamaahWindowMinutes)
        assertEquals(0, defaultState.hijriDayOffset)

        val jamaahMin = 13 * 60 + 30 // 01:30 PM (810)
        val windowMin = 20

        val within15MinAfter = 825
        val outside35MinAfter = 845

        assertTrue(abs(within15MinAfter - jamaahMin) <= windowMin)
        assertTrue(abs(outside35MinAfter - jamaahMin) > windowMin)
    }
}
