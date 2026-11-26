package com.example.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat

/**
 * Foreground Service maintaining the persistent notification state loop so aggressive OEM battery
 * optimizations do not terminate active prayer reminders until "Mark as Prayed" or "Dismiss" is tapped,
 * or the prayer window expires.
 */
class PrayerNaggingForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_NAGGING_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val prayerId = intent?.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_ID) ?: "ASR"
        val prayerName = intent?.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_NAME) ?: "Asr"
        val dateIso = intent?.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_DATE_ISO) ?: ""
        val pstTime = intent?.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_PST_TIME) ?: ""
        val adhanTime = intent?.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_ADHAN_TIME) ?: "03:45 PM"
        val jamaahTime = intent?.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_TIME) ?: "04:00 PM"
        val triggerName = intent?.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_TRIGGER_TYPE)
            ?: AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG.name
        val triggerType = runCatching { AlarmTriggerType.valueOf(triggerName) }
            .getOrDefault(AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG)
        val minutesUntilJamaah = intent?.getIntExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_UNTIL_JAMAAH, 0) ?: 0
        val minutesPassed = intent?.getIntExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_PASSED_SINCE_JAMAAH, 10) ?: 10
        val minutesRemaining = intent?.getIntExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_REMAINING_UNTIL_END, 30) ?: 30
        val repeatIntervalMin = intent?.getIntExtra(ExactPrayerAlarmScheduler.EXTRA_NAG_INTERVAL_MIN, 15) ?: 15
        val nagIteration = intent?.getIntExtra(ExactPrayerAlarmScheduler.EXTRA_NAG_ITERATION, 1) ?: 1
        val pstEpochMillis = intent?.getLongExtra(ExactPrayerAlarmScheduler.EXTRA_PST_EPOCH_MS, 0L) ?: 0L
        val adhanEpochMillis = intent?.getLongExtra(ExactPrayerAlarmScheduler.EXTRA_ADHAN_EPOCH_MS, 0L) ?: 0L
        val jamaahEpochMillis = intent?.getLongExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_EPOCH_MS, 0L) ?: 0L
        val windowEndEpochMillis = intent?.getLongExtra(ExactPrayerAlarmScheduler.EXTRA_WINDOW_END_EPOCH_MS, 0L) ?: 0L

        PrayerNotificationHelper.ensureChannel(this)
        val notification = PrayerNotificationHelper.buildRichPrayerNotification(
            context = this,
            prayerId = prayerId,
            prayerName = prayerName,
            dateIso = dateIso,
            pstTimeFormatted = pstTime,
            adhanTimeFormatted = adhanTime,
            jamaahTimeFormatted = jamaahTime,
            triggerType = triggerType,
            minutesUntilJamaah = minutesUntilJamaah,
            minutesPassedSinceJamaah = minutesPassed,
            minutesRemainingUntilEnd = minutesRemaining,
            repeatIntervalMinutes = repeatIntervalMin,
            progressPercent = if (triggerType == AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW) 92 else 75,
            nagIteration = nagIteration,
            isOngoingForeground = true,
            pstEpochMillis = pstEpochMillis,
            adhanEpochMillis = adhanEpochMillis,
            jamaahEpochMillis = jamaahEpochMillis,
            windowEndEpochMillis = windowEndEpochMillis
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    PrayerNotificationHelper.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(PrayerNotificationHelper.NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            PrayerNotificationHelper.postNotificationIfPermitted(this, notification)
        }

        val targetPct = if (triggerType == AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW) 92 else 75
        PrayerNotificationHelper.startLiveSecondTicker(
            context = this,
            prayerId = prayerId,
            prayerName = prayerName,
            dateIso = dateIso,
            pstTimeFormatted = pstTime,
            adhanTimeFormatted = adhanTime,
            jamaahTimeFormatted = jamaahTime,
            triggerType = triggerType,
            minutesUntilJamaah = minutesUntilJamaah,
            minutesPassedSinceJamaah = minutesPassed,
            minutesRemainingUntilEnd = minutesRemaining,
            repeatIntervalMinutes = repeatIntervalMin,
            targetElapsedPct = targetPct,
            nagIteration = nagIteration,
            runInitialFastSweep = false,
            isOngoingForeground = true,
            pstEpochMillis = pstEpochMillis,
            adhanEpochMillis = adhanEpochMillis,
            jamaahEpochMillis = jamaahEpochMillis,
            windowEndEpochMillis = windowEndEpochMillis
        )

        return START_REDELIVER_INTENT
    }

    companion object {
        const val ACTION_STOP_NAGGING_SERVICE = "com.example.action.STOP_NAGGING_SERVICE"

        fun startOrUpdateNaggingLoop(
            context: Context,
            prayerId: String,
            prayerName: String,
            dateIso: String,
            pstTime: String,
            adhanTime: String,
            jamaahTime: String,
            triggerType: AlarmTriggerType,
            minutesUntilJamaah: Int,
            minutesPassedSinceJamaah: Int,
            minutesRemainingUntilEnd: Int,
            repeatIntervalMinutes: Int,
            nagIteration: Int,
            pstEpochMillis: Long = 0L,
            adhanEpochMillis: Long = 0L,
            jamaahEpochMillis: Long = 0L,
            windowEndEpochMillis: Long = 0L
        ) {
            val serviceIntent = Intent(context, PrayerNaggingForegroundService::class.java).apply {
                putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_ID, prayerId)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_DATE_ISO, dateIso)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_PST_TIME, pstTime)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_ADHAN_TIME, adhanTime)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_TIME, jamaahTime)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_TRIGGER_TYPE, triggerType.name)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_UNTIL_JAMAAH, minutesUntilJamaah)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_PASSED_SINCE_JAMAAH, minutesPassedSinceJamaah)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_REMAINING_UNTIL_END, minutesRemainingUntilEnd)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_NAG_INTERVAL_MIN, repeatIntervalMin(repeatIntervalMinutes))
                putExtra(ExactPrayerAlarmScheduler.EXTRA_NAG_ITERATION, nagIteration)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_PST_EPOCH_MS, pstEpochMillis)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_ADHAN_EPOCH_MS, adhanEpochMillis)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_EPOCH_MS, jamaahEpochMillis)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_WINDOW_END_EPOCH_MS, windowEndEpochMillis)
            }
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                val notification = PrayerNotificationHelper.buildRichPrayerNotification(
                    context = context,
                    prayerId = prayerId,
                    prayerName = prayerName,
                    dateIso = dateIso,
                    pstTimeFormatted = pstTime,
                    adhanTimeFormatted = adhanTime,
                    jamaahTimeFormatted = jamaahTime,
                    triggerType = triggerType,
                    minutesUntilJamaah = minutesUntilJamaah,
                    minutesPassedSinceJamaah = minutesPassedSinceJamaah,
                    minutesRemainingUntilEnd = minutesRemainingUntilEnd,
                    repeatIntervalMinutes = repeatIntervalMin(repeatIntervalMinutes),
                    progressPercent = if (triggerType == AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW) 92 else 75,
                    nagIteration = nagIteration,
                    isOngoingForeground = false,
                    pstEpochMillis = pstEpochMillis,
                    adhanEpochMillis = adhanEpochMillis,
                    jamaahEpochMillis = jamaahEpochMillis,
                    windowEndEpochMillis = windowEndEpochMillis
                )
                PrayerNotificationHelper.postNotificationIfPermitted(context, notification)
            }
        }

        private fun repeatIntervalMin(v: Int): Int = v.coerceIn(5, 60)

        fun stopNaggingLoop(context: Context) {
            try {
                val stopIntent = Intent(context, PrayerNaggingForegroundService::class.java).apply {
                    action = ACTION_STOP_NAGGING_SERVICE
                }
                context.stopService(stopIntent)
            } catch (_: Exception) {
            }
            PrayerNotificationHelper.cancelNotification(context)
        }
    }
}
