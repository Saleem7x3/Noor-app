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
 * Stores manual city configs, GPS coordinates, calculation convention, X=15m pre-Jama'ah buffer,
 * Y=20m nagging interval, and auto-Qada rollover preference.
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
    @ColumnInfo(name = "pre_jamaah_alert_minutes") val preJamaahAlertMinutes: Int = 15, // X = 15m before Jama'ah
    @ColumnInfo(name = "nag_interval_minutes") val nagIntervalMinutes: Int = 20,         // Y = 20m nagging loop
    @ColumnInfo(name = "auto_qada_on_next_prayer") val autoQadaOnNextPrayer: Boolean = true
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

@Entity(tableName = "prayer_settings")
data class PrayerSettingEntity(
    @PrimaryKey val prayerId: String, // FAJR, DHUHR, ASR, MAGHRIB, ISHA
    val jamaahOffsetMinutes: Int // Minutes after Adhan for Jama'ah
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

data class PrayerItemUiModel(
    val id: String,
    val nameEnglish: String,
    val nameArabic: String,
    val adhanMinutes: Int,
    val jamaahOffsetMinutes: Int,
    val isPrayed: Boolean,
    val isCurrent: Boolean,
    val isNext: Boolean,
    val completionStatus: PrayerCompletionStatus = if (isPrayed) PrayerCompletionStatus.PRAYED_ON_TIME else PrayerCompletionStatus.PENDING,
    val nagCountTriggered: Int = 0
) {
    val jamaahMinutes: Int get() = adhanMinutes + jamaahOffsetMinutes
    val preJamaahTriggerMinutes: Int get() = (jamaahMinutes - 15).coerceAtLeast(adhanMinutes)
}

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

data class DayContributionCell(
    val dateIso: String,
    val dayOfWeekShort: String,
    val dayOfMonth: Int,
    val prayersCompleted: Int, // 0..5
    val athkaarCompleted: Int, // 0..3
    val qadaCount: Int = 0,
    val isToday: Boolean = false
) {
    val totalScore: Float
        get() = ((prayersCompleted + athkaarCompleted) / 8f).coerceIn(0f, 1f)
}
