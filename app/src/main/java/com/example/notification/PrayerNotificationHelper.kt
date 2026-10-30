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
    const val URGENT_CHANNEL_ID = "noor_prayer_urgent_end_channel"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chimeUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: chimeUri
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val standardChannel = NotificationChannel(
                CHANNEL_ID,
                "Prayer PST, Adhan & Jama'ah Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "PST start alerts, Adhan countdowns, and post-Jama'ah repeat reminders"
                enableVibration(true)
                setSound(chimeUri, audioAttributes)
            }

            val urgentChannel = NotificationChannel(
                URGENT_CHANNEL_ID,
                "Urgent End-of-Prayer Window Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-urgency warnings when prayer time is close to ending"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                setSound(alarmUri, audioAttributes)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(standardChannel)
            manager.createNotificationChannel(urgentChannel)
        }
    }

    fun buildRichPrayerNotification(
        context: Context,
        prayerId: String,
        prayerName: String,
        dateIso: String,
        pstTimeFormatted: String = "",
        adhanTimeFormatted: String,
        jamaahTimeFormatted: String,
        triggerType: AlarmTriggerType = AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN,
        minutesUntilJamaah: Int = 15,
        minutesPassedSinceJamaah: Int = 0,
        minutesRemainingUntilEnd: Int = 45,
        repeatIntervalMinutes: Int = 10,
        preJamaahAlertMinutes: Int = 15,
        progressPercent: Int = 50,
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

        // Action 2: Snooze 5m (ONLY allowed starting from Pre-Jama'ah reminder or after Jama'ah)
        val canShowSnooze = when (triggerType) {
            AlarmTriggerType.STAGE_1_PST_START,
            AlarmTriggerType.STAGE_PRE_ADHAN -> false
            AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN -> minutesUntilJamaah <= preJamaahAlertMinutes
            AlarmTriggerType.STAGE_PRE_JAMAAH,
            AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG,
            AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW -> true
        }

        val snoozeIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ExactPrayerAlarmScheduler.ACTION_SNOOZE_5M
            putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_ID, prayerId)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_DATE_ISO, dateIso)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_PST_TIME, pstTimeFormatted)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_ADHAN_TIME, adhanTimeFormatted)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_TIME, jamaahTimeFormatted)
        }
        val snoozePi = PendingIntent.getBroadcast(
            context,
            prayerId.hashCode() + 102,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Dismiss
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

        val pstDisplay = pstTimeFormatted.ifBlank { adhanTimeFormatted }

        val titleText: String
        val bodyText: String
        val subText: String
        val channelToUse: String

        when (triggerType) {
            AlarmTriggerType.STAGE_1_PST_START -> {
                titleText = "$prayerName Prayer Time Started ($pstDisplay)"
                bodyText = "Adhan at $adhanTimeFormatted  •  Jama'ah at $jamaahTimeFormatted"
                subText = "Prayer Start Time"
                channelToUse = CHANNEL_ID
            }

            AlarmTriggerType.STAGE_PRE_ADHAN -> {
                titleText = "$prayerName Adhan Soon ($adhanTimeFormatted)"
                bodyText = "Mosque Adhan at $adhanTimeFormatted  •  Jama'ah at $jamaahTimeFormatted"
                subText = "Pre-Adhan Reminder"
                channelToUse = CHANNEL_ID
            }

            AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN -> {
                val minsToJamaah = minutesUntilJamaah.coerceAtLeast(1)
                titleText = "$prayerName Adhan • Jama'ah in $minsToJamaah min"
                bodyText = "Adhan $adhanTimeFormatted  •  Jama'ah $jamaahTimeFormatted"
                subText = "Adhan Call"
                channelToUse = CHANNEL_ID
            }

            AlarmTriggerType.STAGE_PRE_JAMAAH -> {
                val minsToJamaah = minutesUntilJamaah.coerceAtLeast(1)
                titleText = "$prayerName Jama'ah in $minsToJamaah min ($jamaahTimeFormatted)"
                bodyText = "Prepare for $prayerName Jama'ah  •  Jama'ah at $jamaahTimeFormatted"
                subText = "Pre-Jama'ah Reminder"
                channelToUse = CHANNEL_ID
            }

            AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG -> {
                val passedMin = minutesPassedSinceJamaah.coerceAtLeast(1)
                titleText = "$prayerName not prayed • $passedMin mins elapsed since Jama'ah"
                bodyText = "Jama'ah was $jamaahTimeFormatted  •  Please pray $prayerName (${minutesRemainingUntilEnd.coerceAtLeast(1)}m left)"
                subText = "Every ${repeatIntervalMinutes}m Reminder"
                channelToUse = CHANNEL_ID
            }

            AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW -> {
                val remMin = minutesRemainingUntilEnd.coerceAtLeast(1)
                val passedMin = minutesPassedSinceJamaah.coerceAtLeast(1)
                titleText = "URGENT: $remMin min left until $prayerName ends!"
                bodyText = "$prayerName not prayed ($passedMin mins elapsed since Jama'ah) • Pray now"
                subText = "Urgent End-of-Prayer Warning"
                channelToUse = URGENT_CHANNEL_ID
            }
        }

        // Only allow marking as prayed once Jama'ah time has been reached/passed
        val canShowMarkPrayed = when (triggerType) {
            AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG,
            AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW -> true
            else -> minutesUntilJamaah <= 0
        }

        val chimeUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(context, channelToUse)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setSubText(subText)
            .setProgress(100, progressPercent.coerceIn(0, 100), false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(
                if (triggerType == AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW) {
                    NotificationCompat.CATEGORY_ALARM
                } else {
                    NotificationCompat.CATEGORY_REMINDER
                }
            )
            .setSound(chimeUri)
            .setContentIntent(contentPendingIntent)
            .setOngoing(isOngoingForeground)
            .setAutoCancel(!isOngoingForeground)

        if (canShowMarkPrayed) {
            builder.addAction(0, context.getString(R.string.mark_as_prayed), markPrayedPi)
        }

        if (canShowSnooze) {
            builder.addAction(0, context.getString(R.string.snooze_5m), snoozePi)
        }

        builder.addAction(0, "Dismiss", dismissPi)
        return builder.build()
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
        pstTimeFormatted: String = "",
        adhanTimeFormatted: String,
        jamaahTimeFormatted: String,
        triggerType: AlarmTriggerType = AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN,
        minutesUntilJamaah: Int = 15,
        minutesPassedSinceJamaah: Int = 0,
        minutesRemainingUntilEnd: Int = 45,
        repeatIntervalMinutes: Int = 10,
        preJamaahAlertMinutes: Int = 15,
        progressPercent: Int = 50,
        prayerId: String = "ASR",
        dateIso: String = "",
        nagIteration: Int = 0
    ): Boolean {
        val notification = buildRichPrayerNotification(
            context = context,
            prayerId = prayerId,
            prayerName = prayerName,
            dateIso = dateIso,
            pstTimeFormatted = pstTimeFormatted,
            adhanTimeFormatted = adhanTimeFormatted,
            jamaahTimeFormatted = jamaahTimeFormatted,
            triggerType = triggerType,
            minutesUntilJamaah = minutesUntilJamaah,
            minutesPassedSinceJamaah = minutesPassedSinceJamaah,
            minutesRemainingUntilEnd = minutesRemainingUntilEnd,
            repeatIntervalMinutes = repeatIntervalMinutes,
            preJamaahAlertMinutes = preJamaahAlertMinutes,
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
