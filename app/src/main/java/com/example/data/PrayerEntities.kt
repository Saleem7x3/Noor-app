package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PrayerCompletionStatus {
    PENDING,
    PRAYED_ON_TIME,
    PRAYED_LATE,
    MISSED_QADA
}

/**
 * Room Entity for PrayerLog (`prayer_records`):
 * Logs daily completion states, timestamps, Jama'ah adherence, Qada status, and nagging loop counts.
 */
@Entity(
    tableName = "prayer_records",
    primaryKeys = ["dateIso", "prayerId"],
    indices = [Index(value = ["dateIso"]), Index(value = ["completion_status"])]
)
data class PrayerRecordEntity(
    val dateIso: String, // YYYY-MM-DD
    val prayerId: String, // FAJR, DHUHR, ASR, MAGHRIB, ISHA
    val isPrayed: Boolean = false,
    val isInJamaah: Boolean = true,
    val completedAtMillis: Long = 0L,
    @ColumnInfo(name = "completion_status")
    val completionStatus: String = if (isPrayed) PrayerCompletionStatus.PRAYED_ON_TIME.name else PrayerCompletionStatus.PENDING.name,
    @ColumnInfo(name = "nag_count_triggered")
    val nagCountTriggered: Int = 0,
    @ColumnInfo(name = "scheduled_adhan_epoch_ms")
    val scheduledAdhanEpochMs: Long = 0L,
    @ColumnInfo(name = "scheduled_jamaah_epoch_ms")
    val scheduledJamaahEpochMs: Long = 0L
)

/**
 * Room Entity for UserPreferences (`user_preferences`):
 * Stores manual city configs, GPS coordinates, calculation convention,
 * post-Jama'ah repeat notification interval (default 15m, configurable 10-15m+),
 * urgent end-of-window warning threshold (default 30m), and first-launch timestamp.
 */
@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "use_gps_location") val useGpsLocation: Boolean = false,
    @ColumnInfo(name = "selected_city_id") val selectedCityId: String = "makkah",
    @ColumnInfo(name = "last_latitude") val lastLatitude: Double = 21.4225,
    @ColumnInfo(name = "last_longitude") val lastLongitude: Double = 39.8262,
    @ColumnInfo(name = "timezone_offset_hours") val timeZoneOffsetHours: Double = 3.0,
    @ColumnInfo(name = "calculation_method") val calculationMethod: String = "MWL",
    @ColumnInfo(name = "pre_adhan_alert_minutes") val preAdhanAlertMinutes: Int = 5,
    @ColumnInfo(name = "pre_jamaah_alert_minutes") val preJamaahAlertMinutes: Int = 15,
    @ColumnInfo(name = "nag_interval_minutes") val nagIntervalMinutes: Int = 10, // Configurable periodic repeat interval (default 10m) after +10m post-Jama'ah
    @ColumnInfo(name = "urgent_end_window_minutes") val urgentEndWindowMinutes: Int = 15, // 15 mins before prayer window ends urgent warning
    @ColumnInfo(name = "auto_qada_on_next_prayer") val autoQadaOnNextPrayer: Boolean = true,
    @ColumnInfo(name = "first_launch_epoch_ms") val firstLaunchEpochMs: Long = 0L
)

/**
 * Room Entity for Local SQLite Fallback Cities (`local_cities`):
 * Used when FusedLocationProviderClient GPS is disabled, denied, or manual city selection is active.
 */
@Entity(tableName = "local_cities")
data class LocalCityEntity(
    @PrimaryKey @ColumnInfo(name = "city_id") val cityId: String,
    @ColumnInfo(name = "city_name") val cityName: String,
    @ColumnInfo(name = "country_name") val countryName: String,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "timezone_offset_hours") val timeZoneOffsetHours: Double
)

/**
 * Stores independent Adhan and Jama'ah time configurations per prayer (FAJR, DHUHR, JUMUAH, ASR, MAGHRIB, ISHA).
 * Both Adhan and Jama'ah support either an offset (from PST for Adhan; from Adhan for Jama'ah)
 * or a specific clock time (minutes from 00:00).
 */
@Entity(tableName = "prayer_settings")
data class PrayerSettingEntity(
    @PrimaryKey val prayerId: String, // FAJR, DHUHR, JUMUAH, ASR, MAGHRIB, ISHA
    @ColumnInfo(name = "adhan_offset_minutes") val adhanOffsetMinutes: Int = 0, // Minutes after PST for Adhan
    @ColumnInfo(name = "use_fixed_adhan_time") val useFixedAdhanTime: Boolean = false,
    @ColumnInfo(name = "fixed_adhan_time_minutes") val fixedAdhanTimeMinutes: Int = 780, // e.g. 01:00 PM
    val jamaahOffsetMinutes: Int = 15, // Minutes after Adhan for Jama'ah
    @ColumnInfo(name = "use_fixed_time") val useFixedTime: Boolean = false,
    @ColumnInfo(name = "fixed_time_minutes") val fixedTimeMinutes: Int = 795 // e.g. 01:15 PM
)

@Entity(tableName = "athkaar_logs", primaryKeys = ["dateIso", "athkaarId"])
data class AthkaarLogEntity(
    val dateIso: String,
    val athkaarId: String, // MORNING, EVENING, BEDTIME
    val currentCount: Int = 0,
    val targetCount: Int = 33,
    val isCompleted: Boolean = false,
    val completedTimeFormatted: String? = null
)

/**
 * Three-Tier Prayer UI Model:
 * 1. pstMinutes: Natural solar event Prayer Start Time (PST)
 * 2. adhanMinutes: Mosque Adhan time (either fixed clock time or PST + adhanOffsetMinutes)
 * 3. jamaahMinutes: Mosque Jama'ah time (either fixed clock time or Adhan + jamaahOffsetMinutes)
 */
data class PrayerItemUiModel(
    val id: String,
    val nameEnglish: String,
    val nameArabic: String,
    val pstMinutes: Int,
    val adhanOffsetMinutes: Int = 0,
    val useFixedAdhanTime: Boolean = false,
    val fixedAdhanTimeMinutes: Int = pstMinutes,
    val jamaahOffsetMinutes: Int = 15,
    val useFixedTime: Boolean = false,
    val fixedTimeMinutes: Int = 795,
    val isPrayed: Boolean = false,
    val isInJamaah: Boolean = true,
    val canMarkPrayed: Boolean = false,
    val isCurrent: Boolean = false,
    val isNext: Boolean = false,
    val completionStatus: PrayerCompletionStatus = if (isPrayed) PrayerCompletionStatus.PRAYED_ON_TIME else PrayerCompletionStatus.PENDING,
    val nagCountTriggered: Int = 0,
    val settingsKey: String = id,
    val isJumuah: Boolean = false,
    val endMinutes: Int = 0,
    val displayEndMinutes: Int = endMinutes,
    val targetDateIso: String = ""
) {
    val adhanMinutes: Int
        get() = if (useFixedAdhanTime) {
            fixedAdhanTimeMinutes.coerceIn(0, 1439).coerceAtLeast(pstMinutes)
        } else {
            (pstMinutes + adhanOffsetMinutes).coerceAtLeast(pstMinutes)
        }

    val jamaahMinutes: Int
        get() = if (useFixedTime) {
            fixedTimeMinutes.coerceAtLeast(adhanMinutes)
        } else {
            (adhanMinutes + jamaahOffsetMinutes).coerceAtLeast(adhanMinutes)
        }

    val preJamaahTriggerMinutes: Int
        get() = (jamaahMinutes - 15).coerceAtLeast(adhanMinutes)
}

data class SolarWarningItemUiModel(
    val id: String,
    val title: String,
    val startMinutes: Int,
    val endMinutes: Int,
    val isActiveNow: Boolean = false
)

data class AthkaarHabitUiModel(
    val id: String,
    val title: String,
    val arabicSubtitle: String,
    val description: String,
    val currentCount: Int,
    val targetCount: Int,
    val isCompleted: Boolean,
    val completedTimeFormatted: String?,
    val isBedtimeTracker: Boolean = false
)

data class HistoricalDayPrayerUiModel(
    val prayerId: String,
    val nameEnglish: String,
    val nameArabic: String,
    val isPrayed: Boolean,
    val isInJamaah: Boolean,
    val completionStatus: PrayerCompletionStatus,
    val canMarkPrayed: Boolean = true,
    val pstFormatted: String = ""
)

data class DayContributionCell(
    val dateIso: String,
    val dayOfWeekShort: String,
    val dayOfMonth: Int,
    val prayersCompleted: Int, // 0..5
    val athkaarCompleted: Int, // 0..3
    val qadaCount: Int = 0,
    val isToday: Boolean = false,
    val prayerStatuses: List<HistoricalDayPrayerUiModel> = emptyList()
) {
    val totalScore: Float
        get() = ((prayersCompleted + athkaarCompleted) / 8f).coerceIn(0f, 1f)
}

