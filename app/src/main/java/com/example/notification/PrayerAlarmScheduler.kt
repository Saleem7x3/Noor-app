package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

enum class AlarmTriggerType {
    STAGE_1_PST_START,
    STAGE_PRE_ADHAN,
    STAGE_2_ADHAN_COUNTDOWN,
    STAGE_PRE_JAMAAH,
    STAGE_JAMAAH_START,
    STAGE_3_POST_JAMAAH_NAG,
    STAGE_4_URGENT_END_OF_WINDOW,
    STAGE_QAZA_ROLLOVER
}

/**
 * Schedules exact wake-up alarms bypassing Doze mode via AlarmManager.setExactAndAllowWhileIdle():
 * -STAGE_1_PST_START: Fired at natural Prayer Start Time (PST)
 * -STAGE_PRE_ADHAN: Fired X mins before Mosque Adhan (default 5m)
 * -STAGE_2_ADHAN_COUNTDOWN: Fired at Mosque Adhan Time ("Jama'ah in X mins")
 * -STAGE_PRE_JAMAAH: Fired X mins before Jama'ah (default 15m, unlocks Snooze)
 * -STAGE_3_POST_JAMAAH_NAG: Starts 10m after Jama'ah if unticked, then repeats every N mins (default 10m)
 * -STAGE_4_URGENT_END_OF_WINDOW: Urgent warning when close to next prayer ("Z mins remaining until prayer time ends")
 */
class ExactPrayerAlarmScheduler(private val context: Context) {

    val appContext: Context = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleExactTrigger(
        prayerId: String,
        prayerName: String,
        dateIso: String,
        triggerType: AlarmTriggerType,
        triggerAtEpochMillis: Long,
        pstTimeFormatted: String = "",
        adhanTimeFormatted: String,
        jamaahTimeFormatted: String,
        pstEpochMillis: Long = 0L,
        adhanEpochMillis: Long = 0L,
        jamaahEpochMillis: Long = 0L,
        windowEndEpochMillis: Long = 0L,
        minutesUntilJamaah: Int = 0,
        nagIteration: Int = 0
    ) {
        val intent = Intent(appContext, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER_ALARM_TRIGGER
            putExtra(EXTRA_PRAYER_ID, prayerId)
            putExtra(EXTRA_PRAYER_NAME, prayerName)
            putExtra(EXTRA_DATE_ISO, dateIso)
            putExtra(EXTRA_TRIGGER_TYPE, triggerType.name)
            putExtra(EXTRA_PST_TIME, pstTimeFormatted)
            putExtra(EXTRA_ADHAN_TIME, adhanTimeFormatted)
            putExtra(EXTRA_JAMAAH_TIME, jamaahTimeFormatted)
            putExtra(EXTRA_PST_EPOCH_MS, pstEpochMillis)
            putExtra(EXTRA_ADHAN_EPOCH_MS, adhanEpochMillis)
            putExtra(EXTRA_JAMAAH_EPOCH_MS, jamaahEpochMillis)
            putExtra(EXTRA_WINDOW_END_EPOCH_MS, windowEndEpochMillis)
            putExtra(EXTRA_MIN_UNTIL_JAMAAH, minutesUntilJamaah)
            putExtra(EXTRA_NAG_ITERATION, nagIteration)
        }

        val requestCode = (prayerId.hashCode() * 31) + triggerType.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtEpochMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtEpochMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtEpochMillis,
                pendingIntent
            )
        }
    }

    fun cancelAllAlarmsForPrayer(prayerId: String) {
        AlarmTriggerType.entries.forEach { triggerType ->
            val requestCode = (prayerId.hashCode() * 31) + triggerType.ordinal
            val intent = Intent(appContext, PrayerAlarmReceiver::class.java).apply {
                action = ACTION_PRAYER_ALARM_TRIGGER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun cancelAllDailyPrayerAlarms() {
        listOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA").forEach { id ->
            cancelAllAlarmsForPrayer(id)
        }
    }

    companion object {
        const val ACTION_PRAYER_ALARM_TRIGGER = "com.example.action.PRAYER_ALARM_TRIGGER"
        const val ACTION_MARK_PRAYED = "com.example.action.MARK_PRAYED"
        const val ACTION_SNOOZE_5M = "com.example.action.SNOOZE_5M"
        const val ACTION_DISMISS_NAG = "com.example.action.DISMISS_NAG"

        const val EXTRA_PRAYER_ID = "extra_prayer_id"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_DATE_ISO = "extra_date_iso"
        const val EXTRA_TRIGGER_TYPE = "extra_trigger_type"
        const val EXTRA_PST_TIME = "extra_pst_time"
        const val EXTRA_ADHAN_TIME = "extra_adhan_time"
        const val EXTRA_JAMAAH_TIME = "extra_jamaah_time"
        const val EXTRA_PST_EPOCH_MS = "extra_pst_epoch_ms"
        const val EXTRA_ADHAN_EPOCH_MS = "extra_adhan_epoch_ms"
        const val EXTRA_JAMAAH_EPOCH_MS = "extra_jamaah_epoch_ms"
        const val EXTRA_WINDOW_END_EPOCH_MS = "extra_window_end_epoch_ms"
        const val EXTRA_MIN_UNTIL_JAMAAH = "extra_min_until_jamaah"
        const val EXTRA_MIN_PASSED_SINCE_JAMAAH = "extra_min_passed_since_jamaah"
        const val EXTRA_MIN_REMAINING_UNTIL_END = "extra_min_remaining_until_end"
        const val EXTRA_NAG_INTERVAL_MIN = "extra_nag_interval_min"
        const val EXTRA_NAG_ITERATION = "extra_nag_iteration"
    }
}
