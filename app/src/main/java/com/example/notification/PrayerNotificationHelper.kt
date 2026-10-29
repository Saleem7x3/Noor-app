package com.example.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object PrayerNotificationHelper {
    const val CHANNEL_ID = "noor_prayer_soft_chime_channel"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chimeUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Prayer & Jama'ah Alerts (Soft Chime)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Dynamic prayer countdowns, 15m pre-Jama'ah alerts, and 20m nagging reminders"
                enableVibration(true)
                setSound(chimeUri, audioAttributes)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun buildRichPrayerNotification(
        context: Context,
        prayerId: String,
        prayerName: String,
        dateIso: String,
        adhanTimeFormatted: String,
        jamaahTimeFormatted: String,
        minutesUntilJamaah: Int,
        progressPercent: Int,
        nagIteration: Int = 0,
        isOngoingForeground: Boolean = false
    ): Notification {
        ensureChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Mark as Prayed
        val markPrayedIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ExactPrayerAlarmScheduler.ACTION_MARK_PRAYED
            putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_ID, prayerId)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_DATE_ISO, dateIso)
        }
        val markPrayedPi = PendingIntent.getBroadcast(
            context,
            prayerId.hashCode() + 101,
            markPrayedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Snooze 5m
        val snoozeIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ExactPrayerAlarmScheduler.ACTION_SNOOZE_5M
            putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_ID, prayerId)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_DATE_ISO, dateIso)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_ADHAN_TIME, adhanTimeFormatted)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_TIME, jamaahTimeFormatted)
        }
        val snoozePi = PendingIntent.getBroadcast(
            context,
            prayerId.hashCode() + 102,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Dismiss Nagging Loop
        val dismissIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ExactPrayerAlarmScheduler.ACTION_DISMISS_NAG
            putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_ID, prayerId)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_DATE_ISO, dateIso)
        }
        val dismissPi = PendingIntent.getBroadcast(
            context,
            prayerId.hashCode() + 103,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = if (nagIteration > 0) {
            "$prayerName Unprayed (Reminder #$nagIteration • 20m Loop)"
        } else {
            "$prayerName • $minutesUntilJamaah min until Jama'ah"
        }

        val chimeUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titleText)
            .setContentText("Adhan $adhanTimeFormatted  •  Jama'ah $jamaahTimeFormatted")
            .setSubText(if (nagIteration > 0) "Persistent Reminder Loop" else "Muslim World League")
            .setProgress(100, progressPercent.coerceIn(0, 100), false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(chimeUri)
            .setContentIntent(contentPendingIntent)
            .setOngoing(isOngoingForeground)
            .setAutoCancel(!isOngoingForeground)
            .addAction(0, context.getString(R.string.mark_as_prayed), markPrayedPi)
            .addAction(0, context.getString(R.string.snooze_5m), snoozePi)
            .addAction(0, "Dismiss", dismissPi)
            .build()
    }

    fun postNotificationIfPermitted(context: Context, notification: Notification): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }

    fun showDynamicPrayerNotification(
        context: Context,
        prayerName: String,
        adhanTimeFormatted: String,
        jamaahTimeFormatted: String,
        minutesUntilJamaah: Int,
        progressPercent: Int,
        prayerId: String = "ASR",
        dateIso: String = "",
        nagIteration: Int = 0
    ): Boolean {
        val notification = buildRichPrayerNotification(
            context = context,
            prayerId = prayerId,
            prayerName = prayerName,
            dateIso = dateIso,
            adhanTimeFormatted = adhanTimeFormatted,
            jamaahTimeFormatted = jamaahTimeFormatted,
            minutesUntilJamaah = minutesUntilJamaah,
            progressPercent = progressPercent,
            nagIteration = nagIteration,
            isOngoingForeground = nagIteration > 0
        )
        return postNotificationIfPermitted(context, notification)
    }

    fun cancelNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
