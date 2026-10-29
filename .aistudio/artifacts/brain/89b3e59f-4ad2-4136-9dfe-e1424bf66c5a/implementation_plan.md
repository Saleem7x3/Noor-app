# Technical Architecture Blueprint: Noor Islamic Prayer & Habit Tracker

A production-grade Modern Android Development (MAD) architectural blueprint and full-stack Kotlin implementation for **Noor Prayer**, engineered around offline astronomical calculation, `FusedLocationProviderClient` with a local SQLite city fallback, resilient `AlarmManager.setExactAndAllowWhileIdle()` scheduling, `DataStore` state persistence, a Foreground Service persistent "Nagging" loop, and reactive Jetpack Compose UI state flows.

---

### User Review & Confirmed Architecture Decisions

> [!IMPORTANT]
> The following architectural specifications confirmed in Phase 1 govern both this blueprint and the live implementation:
> - **Confirmed Decision 1 — Dynamic Alarm & Nagging Intervals**:
>   - **Trigger 1 (Adhan Start)**: Exact alarm fired at calculated Adhan start time (`T_adhan`).
>   - **Trigger 2 (Pre-Jama'ah Alert)**: Exact alarm fired **X = 15 minutes** before the user-adjusted Jama'ah time (`T_jamaah - 15m`).
>   - **The "Nagging" Loop**: If the prayer is not marked as `PRAYED` in Room, a repeating exact alarm + Foreground Service loop fires every **Y = 20 minutes** (`T_jamaah + 20m * k`).
> - **Confirmed Decision 2 — Loop Termination & Qada Rollover**:
>   - If a user never taps **"Mark as Prayed"** or **"Dismiss"** before the next Fardh prayer window begins, the active nagging loop automatically terminates, cancels pending nag alarms for the expired prayer, and records the previous prayer status as **`MISSED_QADA`** in Room so the user can track and make up missed prayers.
> - **Confirmed Decision 3 — Deliverable Scope**:
>   - Implement the complete production architecture in the Android app (FusedLocationProviderClient + Room SQLite fallback, DataStore state machine, Exact Alarm scheduler, BroadcastReceiver, Foreground Notification Service, and interactive Architecture Blueprint & Alarm Inspector view).

---

### 1. Architecture Overview: Clean Architecture & Unidirectional MVI/MVVM

The system is partitioned into three strict layers (**Presentation**, **Domain**, and **Data/Infrastructure**) communicating exclusively through Kotlin Coroutines and cold/hot `Flow` streams.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              PRESENTATION LAYER (Jetpack Compose)                      │
│  ┌──────────────────────┬───────────────────────┬───────────────────┬───────────────┐  │
│  │ 3-Screen Showcase &  │ Prayer Dashboard &    │ Analytics, Qada & │ Architecture  │  │
│  │ Lock Banner Preview  │ Jama'ah Offset Editor │ 7-Week Habit Grid │ & Alarm Debug │  │
│  └──────────┬───────────┴───────────┬───────────┴─────────┬─────────┴───────┬───────┘  │
│             └───────────────────────┴──────────┬──────────┴─────────────────┘          │
│                                                ▼ StateFlow<PrayerDashboardUiState>     │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │                     PrayerHabitViewModel (MVI / Unidirectional Flow)             │  │
│  │  • Processes UI Intents (MarkPrayed, Snooze, SelectCity, RequestGps, TriggerSim) │  │
│  │  • Merges Room Flows + DataStore Alarm State + 1Hz Countdown Ticker              │  │
│  └─────────────────────────────────────────────┬────────────────────────────────────┘  │
└────────────────────────────────────────────────┼───────────────────────────────────────┘
                                                 │
┌────────────────────────────────────────────────▼───────────────────────────────────────┐
│                                     DOMAIN LAYER                                       │
│  ┌────────────────────────────┬─────────────────────────────┬───────────────────────┐  │
│  │ Offline Solar Adhan Engine │ ResolveLocationUseCase      │ SchedulePrayerAlarms  │  │
│  │ (MWL / ISNA / Umm al-Qura) │ (GPS Fused -> SQLite City)  │ & NaggingStateMachine │  │
│  └────────────────────────────┴─────────────────────────────┴───────────────────────┘  │
└────────────────────────────────────────────────┬───────────────────────────────────────┘
                                                 │
┌────────────────────────────────────────────────▼───────────────────────────────────────┐
│                            DATA & SYSTEM INFRASTRUCTURE LAYER                          │
│  ┌──────────────────────────┬───────────────────────────┬───────────────────────────┐  │
│  │ Room SQLite Database     │ Jetpack DataStore         │ Android OS Services       │  │
│  │ • PrayerLogEntity        │ • ActiveNagPrayerId       │ • FusedLocationClient     │  │
│  │ • UserPreferencesEntity  │ • NagIterationCount       │ • AlarmManager (Exact)    │  │
│  │ • LocalCityEntity (SQL)  │ • LastScheduledEpoch      │ • PrayerAlarmReceiver     │  │
│  │ • AthkaarLogEntity       │ • SnoozeUntilEpochMillis  │ • NaggingForegroundService│  │
│  └──────────────────────────┴───────────────────────────┴───────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Layer Responsibilities
1. **Presentation Layer**:
   - Stateless Jetpack Compose screens observing a single immutable `StateFlow<PrayerDashboardUiState>` via `collectAsStateWithLifecycle()`.
   - Dispatches sealed `PrayerUiEvent` / intent actions to `PrayerHabitViewModel`.
2. **Domain Layer**:
   - **Astronomical Adhan Engine (`PrayerTimeCalculator`)**: Pure Kotlin solar zenith/declination calculator supporting Muslim World League (18°/17°) and custom high-latitude adjustments with zero network dependency.
   - **Location Resolution Policy**: Attempts hardware GPS fix via `FusedLocationProviderClient`; if permission is denied, location is disabled, or timeout occurs, falls back seamlessly to the local SQLite `local_cities` table and `UserPreferencesEntity`.
3. **Data & System Infrastructure Layer**:
   - **Room Database**: Single source of truth for daily `PrayerLog` states (`PENDING`, `PRAYED`, `MISSED_QADA`, `SNOOZED`), `UserPreferences`, local fallback cities, and `AthkaarLog`.
   - **Jetpack DataStore (`Preferences`)**: High-frequency, lightweight atomic store tracking the real-time notification state machine (`active_nag_prayer_id`, `nag_count`, `next_alarm_epoch_ms`, `trigger_stage`) across process death and BroadcastReceiver invocations.
   - **Exact Alarm & Foreground Service Pipeline**: Uses `AlarmManager.setExactAndAllowWhileIdle()` to wake the device in Doze mode, paired with `PrayerNaggingForegroundService` (`foregroundServiceType="specialUse"` / system notification loop) and `PrayerActionReceiver` for instant "Mark as Prayed", "Snooze 5m", and "Dismiss" handling.

---

### 2. Database Schema (Room Entities & DAO Design)

Below are the complete Room Entity definitions for `PrayerLogEntity`, `UserPreferencesEntity`, and `LocalCityEntity` (SQLite city fallback), designed to power both the notification state machine and the 7-week GitHub-style analytical grid.

```kotlin
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
 * Stores daily completion states, timestamps, Jama'ah adherence, and nagging history
 * for each of the 5 Fardh prayers. Indexed by dateIso for fast 7-week grid queries.
 */
@Entity(
    tableName = "prayer_logs",
    primaryKeys = ["date_iso", "prayer_id"],
    indices = [Index(value = ["date_iso"]), Index(value = ["completion_status"])]
)
data class PrayerLogEntity(
    @ColumnInfo(name = "date_iso") val dateIso: String, // YYYY-MM-DD
    @ColumnInfo(name = "prayer_id") val prayerId: String, // FAJR, DHUHR, ASR, MAGHRIB, ISHA
    @ColumnInfo(name = "scheduled_adhan_epoch_ms") val scheduledAdhanEpochMs: Long,
    @ColumnInfo(name = "scheduled_jamaah_epoch_ms") val scheduledJamaahEpochMs: Long,
    @ColumnInfo(name = "completion_status") val completionStatus: PrayerCompletionStatus = PrayerCompletionStatus.PENDING,
    @ColumnInfo(name = "is_prayed") val isPrayed: Boolean = false,
    @ColumnInfo(name = "is_in_jamaah") val isInJamaah: Boolean = false,
    @ColumnInfo(name = "completed_at_epoch_ms") val completedAtEpochMs: Long? = null,
    @ColumnInfo(name = "nag_count_triggered") val nagCountTriggered: Int = 0
)

/**
 * Stores user location preferences, manual city fallback configuration, calculation convention,
 * per-prayer Jama'ah offsets, and dynamic notification timing buffers (X = 15m, Y = 20m).
 */
@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "use_gps_location") val useGpsLocation: Boolean = true,
    @ColumnInfo(name = "selected_city_id") val selectedCityId: String = "makkah",
    @ColumnInfo(name = "last_latitude") val lastLatitude: Double = 21.4225,
    @ColumnInfo(name = "last_longitude") val lastLongitude: Double = 39.8262,
    @ColumnInfo(name = "timezone_offset_hours") val timeZoneOffsetHours: Double = 3.0,
    @ColumnInfo(name = "calculation_method") val calculationMethod: String = "MWL",
    @ColumnInfo(name = "pre_jamaah_alert_minutes") val preJamaahAlertMinutes: Int = 15, // X = 15m before Jama'ah
    @ColumnInfo(name = "nag_interval_minutes") val nagIntervalMinutes: Int = 20,         // Y = 20m nagging loop
    @ColumnInfo(name = "auto_qada_on_next_prayer") val autoQadaOnNextPrayer: Boolean = true,
    @ColumnInfo(name = "fajr_jamaah_offset_min") val fajrJamaahOffsetMin: Int = 20,
    @ColumnInfo(name = "dhuhr_jamaah_offset_min") val dhuhrJamaahOffsetMin: Int = 15,
    @ColumnInfo(name = "asr_jamaah_offset_min") val asrJamaahOffsetMin: Int = 15,
    @ColumnInfo(name = "maghrib_jamaah_offset_min") val maghribJamaahOffsetMin: Int = 10,
    @ColumnInfo(name = "isha_jamaah_offset_min") val ishaJamaahOffsetMin: Int = 15
)

/**
 * Local SQLite fallback table of world cities for offline manual selection when GPS is unavailable.
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
```

---

### 3. Core Notification & Exact Alarm Lifecycle Strategy

To survive aggressive OEM battery management (Doze mode, App Standby Buckets) while respecting Android 14+ background execution limits, the notification lifecycle uses a **3-Stage Exact Alarm + Foreground Service State Machine**:

```
   [Stage 1: T_adhan]                   [Stage 2: T_jamaah - 15m]              [Stage 3: T_jamaah + 20m * k]
┌───────────────────────┐            ┌─────────────────────────────┐         ┌─────────────────────────────────┐
│ Exact Alarm (Trigger1)│            │ Exact Alarm (Trigger 2)     │         │ Persistent Nagging Loop         │
│ • Fires at Adhan time │───────────▶│ • Fires 15m before Jama'ah  │────────▶│ • Checks Room: isPrayed?        │
│ • Posts Rich Banner   │            │ • Updates Countdown Badge   │         │ • Starts Foreground Service     │
│ • Schedules Trigger 2 │            │ • Schedules First Nag (+20m)│         │ • Re-arms Exact Alarm (+20m)    │
└───────────────────────┘            └─────────────────────────────┘         └────────┬───────────────┬────────┘
                                                                                      │               │
                                                       User Taps "Mark as Prayed"     │               │ Next Prayer Starts
                                                       ┌──────────────────────────────┘               └────────────────────────┐
                                                       ▼                                                                       ▼
                                       ┌───────────────────────────────┐                                       ┌───────────────────────────────┐
                                       │ PrayerActionReceiver          │                                       │ Rollover Handler              │
                                       │ • Updates Room: PRAYED_ON_TIME│                                       │ • Updates Room: MISSED_QADA   │
                                       │ • Clears DataStore Nag State  │                                       │ • Cancels Old Nag Alarm       │
                                       │ • Stops Foreground Service    │                                       │ • Starts Next Prayer Trigger 1│
                                       └───────────────────────────────┘                                       └───────────────────────────────┘
```

#### Reference Implementation of the Alarm Receiver, Foreground Service & Action Handling

```kotlin
package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.data.AppDatabase
import com.example.data.PrayerCompletionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

enum class AlarmTriggerType {
    TRIGGER_1_ADHAN_START,
    TRIGGER_2_PRE_JAMAAH_15M,
    TRIGGER_3_NAGGING_LOOP_20M
}

class ExactPrayerAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleExactTrigger(
        prayerId: String,
        dateIso: String,
        triggerType: AlarmTriggerType,
        triggerAtEpochMillis: Long,
        nagIteration: Int = 0
    ) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER_ALARM_TRIGGER
            putExtra(EXTRA_PRAYER_ID, prayerId)
            putExtra(EXTRA_DATE_ISO, dateIso)
            putExtra(EXTRA_TRIGGER_TYPE, triggerType.name)
            putExtra(EXTRA_NAG_ITERATION, nagIteration)
        }
        val requestCode = (prayerId.hashCode() * 31) + triggerType.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, pendingIntent)
        }
    }

    fun cancelAllForPrayer(prayerId: String) {
        AlarmTriggerType.entries.forEach { type ->
            val requestCode = (prayerId.hashCode() * 31) + type.ordinal
            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = ACTION_PRAYER_ALARM_TRIGGER
            }
            val pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) alarmManager.cancel(pi)
        }
    }

    companion object {
        const val ACTION_PRAYER_ALARM_TRIGGER = "com.example.action.PRAYER_ALARM_TRIGGER"
        const val ACTION_MARK_PRAYED = "com.example.action.MARK_PRAYED"
        const val ACTION_SNOOZE_5M = "com.example.action.SNOOZE_5M"
        const val ACTION_DISMISS_NAG = "com.example.action.DISMISS_NAG"
        const val EXTRA_PRAYER_ID = "extra_prayer_id"
        const val EXTRA_DATE_ISO = "extra_date_iso"
        const val EXTRA_TRIGGER_TYPE = "extra_trigger_type"
        const val EXTRA_NAG_ITERATION = "extra_nag_iteration"
    }
}
```

When `PrayerAlarmReceiver` receives `ACTION_PRAYER_ALARM_TRIGGER`:
1. Calls `goAsync()` to acquire a temporary broadcast execution window on `Dispatchers.IO`.
2. Queries Room `PrayerDao` for `(dateIso, prayerId)`:
   - If `isPrayed == true`, it immediately aborts and stops `PrayerNaggingForegroundService`.
   - If `triggerType == TRIGGER_1_ADHAN_START`, it also inspects the **previous prayer**; if the previous prayer is still `PENDING`, it transitions the previous prayer to `MISSED_QADA` and cancels its nagging loop.
   - If `triggerType == TRIGGER_3_NAGGING_LOOP_20M` and `isPrayed == false`, it increments `nagCountTriggered` in Room and `DataStore`, starts/updates `PrayerNaggingForegroundService`, and schedules the next exact alarm for `now + 20 minutes` (`setExactAndAllowWhileIdle`).

---

### 4. Jetpack Compose Reactive State Structure

The presentation layer subscribes to a single cold-to-hot `StateFlow<PrayerDashboardUiState>` constructed via `combine()` over:
1. `PrayerDao.getPrayersForDate(todayIso)` (Room reactive SQLite trigger)
2. `PrayerDao.getUserPreferences()` (Jama'ah buffers, X=15m, Y=20m, GPS vs City mode)
3. `PrayerAlarmDataStore.alarmStateFlow` (Active nagging prayer, nag iteration count, next exact alarm timestamp)
4. `tickerFlow` (1-second coroutine ticker updating countdown badges and lock screen progress bars)

Whenever the user taps **"Mark as Prayed"** on either the Android System Notification (`BroadcastReceiver`), the Lock Screen Preview (Screen 1), or the Prayer Dashboard row (Screen 2), the Room table updates atomically, DataStore clears the nagging state, and Compose recomposes the progress ring, streak counter, and 7-week contribution grid in a single frame.
