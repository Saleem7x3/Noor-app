package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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
    val lastEventDescription: String = "Exact alarms armed (Trigger 1 @ Adhan, Trigger 2 @ -15m Jama'ah, Nag @ +20m)"
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
                ?: "Exact alarms armed (Trigger 1 @ Adhan, Trigger 2 @ -15m Jama'ah, Nag @ +20m)"
        )
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
