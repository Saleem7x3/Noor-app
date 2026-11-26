package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.CityLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.prayerAlarmDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "noor_prayer_alarm_state"
)

data class AlarmLoopRuntimeState(
    val activeNagPrayerId: String? = null,
    val activeNagDateIso: String? = null,
    val nagIterationCount: Int = 0,
    val lastTriggerStage: String = "IDLE", // TRIGGER_1_ADHAN, TRIGGER_2_PRE_JAMAAH_15M, TRIGGER_3_NAG_20M, PRAYED_DISMISSED, AUTO_QADA
    val nextScheduledAlarmEpochMs: Long = 0L,
    val snoozedExtraMinutes: Int = 0,
    val isForegroundLoopActive: Boolean = false,
    val lastEventDescription: String = "Exact alarms armed (Trigger 1 @ Adhan, Trigger 2 @ -15m Jama'ah, Nag @ +20m)",
    val hijriDayOffset: Int = 0,
    val autoJamaahWindowMinutes: Int = 20, // 0 = Always Ask / Manual Choice, 15 = Auto within 15m of Jama'ah, 20 = Auto within 20m of Jama'ah
    val manualQazaBacklogCount: Int = 0,
    val autoSilenceDuringJamaah: Boolean = true,
    val savedCityId: String? = null,
    val savedCityName: String? = null,
    val savedCityCountry: String? = null,
    val savedCityLatitude: Double? = null,
    val savedCityLongitude: Double? = null,
    val savedCityTzOffset: Double? = null,
    val savedCityIsGps: Boolean = false,
    val lastAthkaarDateIso: String? = null,
    val themeMode: String = "SYSTEM_AUTO",
    val heroCardStyle: String = "WAVE_CARD",
    val displayScaleMode: String = "ADAPTIVE_CAPPED",
    val dismissedPrayerKey: String? = null
)

class PrayerAlarmDataStore(private val context: Context) {

    private object Keys {
        val ACTIVE_NAG_PRAYER_ID = stringPreferencesKey("active_nag_prayer_id")
        val ACTIVE_NAG_DATE_ISO = stringPreferencesKey("active_nag_date_iso")
        val NAG_ITERATION_COUNT = intPreferencesKey("nag_iteration_count")
        val LAST_TRIGGER_STAGE = stringPreferencesKey("last_trigger_stage")
        val NEXT_ALARM_EPOCH_MS = longPreferencesKey("next_alarm_epoch_ms")
        val SNOOZED_EXTRA_MINUTES = intPreferencesKey("snoozed_extra_minutes")
        val IS_FOREGROUND_LOOP_ACTIVE = booleanPreferencesKey("is_foreground_loop_active")
        val LAST_EVENT_DESCRIPTION = stringPreferencesKey("last_event_description")
        val HIJRI_DAY_OFFSET = intPreferencesKey("hijri_day_offset")
        val AUTO_JAMAAH_WINDOW_MINUTES = intPreferencesKey("auto_jamaah_window_minutes")
        val MANUAL_QAZA_BACKLOG_COUNT = intPreferencesKey("manual_qaza_backlog_count")
        val AUTO_SILENCE_DURING_JAMAAH = booleanPreferencesKey("auto_silence_during_jamaah")
        val SAVED_CITY_ID = stringPreferencesKey("saved_city_id")
        val SAVED_CITY_NAME = stringPreferencesKey("saved_city_name")
        val SAVED_CITY_COUNTRY = stringPreferencesKey("saved_city_country")
        val SAVED_CITY_LATITUDE = doublePreferencesKey("saved_city_latitude")
        val SAVED_CITY_LONGITUDE = doublePreferencesKey("saved_city_longitude")
        val SAVED_CITY_TZ_OFFSET = doublePreferencesKey("saved_city_tz_offset")
        val SAVED_CITY_IS_GPS = booleanPreferencesKey("saved_city_is_gps")
        val LAST_ATHKAAR_DATE_ISO = stringPreferencesKey("last_athkaar_date_iso")
        val THEME_MODE = stringPreferencesKey("app_theme_mode")
        val HERO_CARD_STYLE = stringPreferencesKey("hero_card_style")
        val DISPLAY_SCALE_MODE = stringPreferencesKey("display_scale_mode")
        val DISMISSED_PRAYER_KEY = stringPreferencesKey("dismissed_prayer_key")
    }

    val runtimeStateFlow: Flow<AlarmLoopRuntimeState> = context.prayerAlarmDataStore.data.map { prefs ->
        AlarmLoopRuntimeState(
            activeNagPrayerId = prefs[Keys.ACTIVE_NAG_PRAYER_ID]?.takeIf { it.isNotBlank() },
            activeNagDateIso = prefs[Keys.ACTIVE_NAG_DATE_ISO]?.takeIf { it.isNotBlank() },
            nagIterationCount = prefs[Keys.NAG_ITERATION_COUNT] ?: 0,
            lastTriggerStage = prefs[Keys.LAST_TRIGGER_STAGE] ?: "ARMED_EXACT_IDLE",
            nextScheduledAlarmEpochMs = prefs[Keys.NEXT_ALARM_EPOCH_MS] ?: 0L,
            snoozedExtraMinutes = prefs[Keys.SNOOZED_EXTRA_MINUTES] ?: 0,
            isForegroundLoopActive = prefs[Keys.IS_FOREGROUND_LOOP_ACTIVE] ?: false,
            lastEventDescription = prefs[Keys.LAST_EVENT_DESCRIPTION]
                ?: "Exact alarms armed (Trigger 1 @ Adhan, Trigger 2 @ -15m Jama'ah, Nag @ +20m)",
            hijriDayOffset = (prefs[Keys.HIJRI_DAY_OFFSET] ?: 0).coerceIn(-2, 2),
            autoJamaahWindowMinutes = prefs[Keys.AUTO_JAMAAH_WINDOW_MINUTES] ?: 20,
            manualQazaBacklogCount = (prefs[Keys.MANUAL_QAZA_BACKLOG_COUNT] ?: 0).coerceAtLeast(-500),
            autoSilenceDuringJamaah = prefs[Keys.AUTO_SILENCE_DURING_JAMAAH] ?: true,
            savedCityId = prefs[Keys.SAVED_CITY_ID]?.takeIf { it.isNotBlank() },
            savedCityName = prefs[Keys.SAVED_CITY_NAME]?.takeIf { it.isNotBlank() },
            savedCityCountry = prefs[Keys.SAVED_CITY_COUNTRY]?.takeIf { it.isNotBlank() },
            savedCityLatitude = prefs[Keys.SAVED_CITY_LATITUDE],
            savedCityLongitude = prefs[Keys.SAVED_CITY_LONGITUDE],
            savedCityTzOffset = prefs[Keys.SAVED_CITY_TZ_OFFSET],
            savedCityIsGps = prefs[Keys.SAVED_CITY_IS_GPS] ?: false,
            lastAthkaarDateIso = prefs[Keys.LAST_ATHKAAR_DATE_ISO]?.takeIf { it.isNotBlank() },
            themeMode = prefs[Keys.THEME_MODE]?.takeIf { it.isNotBlank() } ?: "SYSTEM_AUTO",
            heroCardStyle = prefs[Keys.HERO_CARD_STYLE]?.takeIf { it.isNotBlank() } ?: "WAVE_CARD",
            displayScaleMode = prefs[Keys.DISPLAY_SCALE_MODE]?.takeIf { it.isNotBlank() } ?: "ADAPTIVE_CAPPED",
            dismissedPrayerKey = prefs[Keys.DISMISSED_PRAYER_KEY]?.takeIf { it.isNotBlank() }
        )
    }

    suspend fun saveSelectedCitySnapshot(city: CityLocation, isGps: Boolean) {
        context.prayerAlarmDataStore.edit { prefs ->
            prefs[Keys.SAVED_CITY_ID] = city.id
            prefs[Keys.SAVED_CITY_NAME] = city.name
            prefs[Keys.SAVED_CITY_COUNTRY] = city.country
            prefs[Keys.SAVED_CITY_LATITUDE] = city.latitude
            prefs[Keys.SAVED_CITY_LONGITUDE] = city.longitude
            prefs[Keys.SAVED_CITY_TZ_OFFSET] = city.timeZoneOffsetHours
            prefs[Keys.SAVED_CITY_IS_GPS] = isGps
        }
    }

    suspend fun getSavedCitySnapshot(): Pair<CityLocation, Boolean>? {
        val state = runtimeStateFlow.first()
        val id = state.savedCityId ?: return null
        val name = state.savedCityName ?: return null
        val lat = state.savedCityLatitude ?: return null
        val lon = state.savedCityLongitude ?: return null
        val tz = state.savedCityTzOffset ?: return null
        return Pair(
            CityLocation(
                id = id,
                name = name,
                country = state.savedCityCountry ?: "Saved Location",
                latitude = lat,
                longitude = lon,
                timeZoneOffsetHours = tz,
                isGpsSimulated = state.savedCityIsGps
            ),
            state.savedCityIsGps
        )
    }

    suspend fun setLastAthkaarDateIso(dateIso: String) {
        context.prayerAlarmDataStore.edit { prefs ->
            prefs[Keys.LAST_ATHKAAR_DATE_ISO] = dateIso
        }
    }

    suspend fun setHijriDayOffset(offset: Int) {
        context.prayerAlarmDataStore.edit { prefs ->
            prefs[Keys.HIJRI_DAY_OFFSET] = offset.coerceIn(-2, 2)
        }
    }

    suspend fun setAutoJamaahWindowMinutes(windowMinutes: Int) {
        context.prayerAlarmDataStore.edit { prefs ->
            prefs[Keys.AUTO_JAMAAH_WINDOW_MINUTES] = windowMinutes.coerceIn(0, 60)
        }
    }

    suspend fun adjustManualQazaBacklog(delta: Int, minFloor: Int = 0) {
        context.prayerAlarmDataStore.edit { prefs ->
            val current = prefs[Keys.MANUAL_QAZA_BACKLOG_COUNT] ?: 0
            prefs[Keys.MANUAL_QAZA_BACKLOG_COUNT] = (current + delta).coerceAtLeast(minFloor)
        }
    }

    suspend fun setAutoSilenceDuringJamaah(enabled: Boolean) {
        context.prayerAlarmDataStore.edit { prefs ->
            prefs[Keys.AUTO_SILENCE_DURING_JAMAAH] = enabled
        }
    }

    suspend fun setThemeMode(modeName: String) {
        context.prayerAlarmDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = modeName
        }
    }

    suspend fun setHeroCardStyle(styleName: String) {
        context.prayerAlarmDataStore.edit { prefs ->
            prefs[Keys.HERO_CARD_STYLE] = styleName
        }
    }

    suspend fun setDisplayScaleMode(modeName: String) {
        context.prayerAlarmDataStore.edit { prefs ->
            prefs[Keys.DISPLAY_SCALE_MODE] = modeName
        }
    }

    suspend fun recordDismissedPrayerKey(prayerWindowKey: String) {
        context.prayerAlarmDataStore.edit { prefs ->
            prefs[Keys.DISMISSED_PRAYER_KEY] = prayerWindowKey
        }
    }

    suspend fun clearDismissedPrayerKey() {
        context.prayerAlarmDataStore.edit { prefs ->
            prefs[Keys.DISMISSED_PRAYER_KEY] = ""
        }
    }

    suspend fun recordTriggerActivated(
        prayerId: String,
        dateIso: String,
        triggerStage: String,
        nagIteration: Int,
        nextAlarmEpochMs: Long,
        foregroundActive: Boolean,
        eventDescription: String
    ) {
        context.prayerAlarmDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_NAG_PRAYER_ID] = prayerId
            prefs[Keys.ACTIVE_NAG_DATE_ISO] = dateIso
            prefs[Keys.LAST_TRIGGER_STAGE] = triggerStage
            prefs[Keys.NAG_ITERATION_COUNT] = nagIteration
            prefs[Keys.NEXT_ALARM_EPOCH_MS] = nextAlarmEpochMs
            prefs[Keys.IS_FOREGROUND_LOOP_ACTIVE] = foregroundActive
            prefs[Keys.LAST_EVENT_DESCRIPTION] = eventDescription
        }
    }

    suspend fun recordSnoozeFiveMinutes(
        prayerId: String,
        nextAlarmEpochMs: Long
    ) {
        context.prayerAlarmDataStore.edit { prefs ->
            val currentSnooze = prefs[Keys.SNOOZED_EXTRA_MINUTES] ?: 0
            val updatedSnooze = currentSnooze + 5
            prefs[Keys.SNOOZED_EXTRA_MINUTES] = updatedSnooze
            prefs[Keys.NEXT_ALARM_EPOCH_MS] = nextAlarmEpochMs
            prefs[Keys.LAST_TRIGGER_STAGE] = "SNOOZED_+${updatedSnooze}M"
            prefs[Keys.LAST_EVENT_DESCRIPTION] = "$prayerId snoozed +5m • Exact re-trigger scheduled"
        }
    }

    suspend fun clearNaggingLoop(reason: String, stageLabel: String = "PRAYED_RESOLVED") {
        context.prayerAlarmDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_NAG_PRAYER_ID] = ""
            prefs[Keys.NAG_ITERATION_COUNT] = 0
            prefs[Keys.SNOOZED_EXTRA_MINUTES] = 0
            prefs[Keys.IS_FOREGROUND_LOOP_ACTIVE] = false
            prefs[Keys.LAST_TRIGGER_STAGE] = stageLabel
            prefs[Keys.LAST_EVENT_DESCRIPTION] = reason
        }
    }
}
