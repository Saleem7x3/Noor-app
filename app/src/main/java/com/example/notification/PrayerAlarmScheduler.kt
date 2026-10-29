package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

enum class AlarmTriggerType {
    TRIGGER_1_ADHAN_START,
    TRIGGER_2_PRE_JAMAAH_15M,
    TRIGGER_3_NAGGING_LOOP_20M
}

/**
 * Schedules exact wake-up alarms bypassing Doze mode via AlarmManager.setExactAndAllowWhileIdle():
 * - Trigger 1: Fired at exact Adhan start time
 * - Trigger 2: Fired X=15 minutes before user-adjusted Jama'ah time
 * - Trigger 3: Repeating nagging loop every Y=20 minutes if prayer is not marked as Prayed
 */
class ExactPrayerAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleExactTrigger(
        prayerId: String,
        prayerName: String,
        dateIso: String,
        triggerType: AlarmTriggerType,
        triggerAtEpochMillis: Long,
        adhanTimeFormatted: String,
        jamaahTimeFormatted: String,
        minutesUntilJamaah: Int,
        nagIteration: Int = 0
    ) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER_ALARM_TRIGGER
            putExtra(EXTRA_PRAYER_ID, prayerId)
            putExtra(EXTRA_PRAYER_NAME, prayerName)
            putExtra(EXTRA_DATE_ISO, dateIso)
            putExtra(EXTRA_TRIGGER_TYPE, triggerType.name)
            putExtra(EXTRA_ADHAN_TIME, adhanTimeFormatted)
            putExtra(EXTRA_JAMAAH_TIME, jamaahTimeFormatted)
            putExtra(EXTRA_MIN_UNTIL_JAMAAH, minutesUntilJamaah)
            putExtra(EXTRA_NAG_ITERATION, nagIteration)
        }

        val requestCode = (prayerId.hashCode() * 31) + triggerType.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            context,
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
            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = ACTION_PRAYER_ALARM_TRIGGER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
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

    companion object {
        const val ACTION_PRAYER_ALARM_TRIGGER = "com.example.action.PRAYER_ALARM_TRIGGER"
        const val ACTION_MARK_PRAYED = "com.example.action.MARK_PRAYED"
        const val ACTION_SNOOZE_5M = "com.example.action.SNOOZE_5M"
        const val ACTION_DISMISS_NAG = "com.example.action.DISMISS_NAG"

        const val EXTRA_PRAYER_ID = "extra_prayer_id"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_DATE_ISO = "extra_date_iso"
        const val EXTRA_TRIGGER_TYPE = "extra_trigger_type"
        const val EXTRA_ADHAN_TIME = "extra_adhan_time"
        const val EXTRA_JAMAAH_TIME = "extra_jamaah_time"
        const val EXTRA_MIN_UNTIL_JAMAAH = "extra_min_until_jamaah"
        const val EXTRA_NAG_ITERATION = "extra_nag_iteration"
    }
}
