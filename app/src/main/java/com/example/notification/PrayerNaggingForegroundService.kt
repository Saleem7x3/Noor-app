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
 * or the next prayer auto-marks the expired prayer as MISSED_QADA.
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
        val adhanTime = intent?.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_ADHAN_TIME) ?: "03:45 PM"
        val jamaahTime = intent?.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_TIME) ?: "04:00 PM"
        val minutesUntilJamaah = intent?.getIntExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_UNTIL_JAMAAH, 15) ?: 15
        val nagIteration = intent?.getIntExtra(ExactPrayerAlarmScheduler.EXTRA_NAG_ITERATION, 1) ?: 1

        PrayerNotificationHelper.ensureChannel(this)
        val notification = PrayerNotificationHelper.buildRichPrayerNotification(
            context = this,
            prayerId = prayerId,
            prayerName = prayerName,
            dateIso = dateIso,
            adhanTimeFormatted = adhanTime,
            jamaahTimeFormatted = jamaahTime,
            minutesUntilJamaah = minutesUntilJamaah,
            progressPercent = if (nagIteration > 0) 100 else 65,
            nagIteration = nagIteration,
            isOngoingForeground = true
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
            // Fallback to standard notification manager post if FGS start is restricted
            PrayerNotificationHelper.postNotificationIfPermitted(this, notification)
        }

        return START_REDELIVER_INTENT
    }

    companion object {
        const val ACTION_STOP_NAGGING_SERVICE = "com.example.action.STOP_NAGGING_SERVICE"

        fun startOrUpdateNaggingLoop(
            context: Context,
            prayerId: String,
            prayerName: String,
            dateIso: String,
            adhanTime: String,
            jamaahTime: String,
            minutesUntilJamaah: Int,
            nagIteration: Int
        ) {
            val serviceIntent = Intent(context, PrayerNaggingForegroundService::class.java).apply {
                putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_ID, prayerId)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_DATE_ISO, dateIso)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_ADHAN_TIME, adhanTime)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_TIME, jamaahTime)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_UNTIL_JAMAAH, minutesUntilJamaah)
                putExtra(ExactPrayerAlarmScheduler.EXTRA_NAG_ITERATION, nagIteration)
            }
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                val notification = PrayerNotificationHelper.buildRichPrayerNotification(
                    context = context,
                    prayerId = prayerId,
                    prayerName = prayerName,
                    dateIso = dateIso,
                    adhanTimeFormatted = adhanTime,
                    jamaahTimeFormatted = jamaahTime,
                    minutesUntilJamaah = minutesUntilJamaah,
                    progressPercent = 90,
                    nagIteration = nagIteration,
                    isOngoingForeground = false
                )
                PrayerNotificationHelper.postNotificationIfPermitted(context, notification)
            }
        }

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
