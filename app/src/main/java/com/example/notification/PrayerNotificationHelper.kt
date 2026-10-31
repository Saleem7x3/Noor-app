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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

object PrayerNotificationHelper {
    const val CHANNEL_ID = "noor_prayer_soft_chime_channel"
    const val URGENT_CHANNEL_ID = "noor_prayer_urgent_end_channel"
    const val NOTIFICATION_ID = 1001

    private val animationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var activeSweepJob: Job? = null

    fun formatElapsedSecondsOrHours(totalSeconds: Int): String {
        val clamped = totalSeconds.coerceAtLeast(0)
        val hours = clamped / 3600
        val mins = (clamped % 3600) / 60
        val secs = clamped % 60
        return if (hours > 0) {
            String.format(Locale.US, "%dh %02dm", hours, mins)
        } else {
            String.format(Locale.US, "%dm %02ds", mins, secs)
        }
    }

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
                description = "PST start alerts, Adhan countdowns, Jama'ah alerts, and post-Jama'ah repeat reminders"
                enableVibration(true)
                setSound(chimeUri, audioAttributes)
            }

            val urgentChannel = NotificationChannel(
                URGENT_CHANNEL_ID,
                "Urgent End-of-Prayer Window Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-urgency warnings 15 minutes before prayer time ends"
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
        isOngoingForeground: Boolean = false,
        onlyAlertOnce: Boolean = false,
        customSubTextSuffix: String? = null
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

        // Primary Action: Mark as Prayed (Snooze removed per user design)
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

        // Secondary Action: Dismiss
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
        val clampedPct = progressPercent.coerceIn(1, 100)

        val titleText: String
        val bodyText: String
        val baseSubText: String
        val channelToUse: String

        when (triggerType) {
            AlarmTriggerType.STAGE_1_PST_START -> {
                titleText = "$prayerName Prayer Time Started ($pstDisplay)"
                bodyText = "Adhan at $adhanTimeFormatted  •  Jama'ah at $jamaahTimeFormatted"
                baseSubText = "Prayer Start Time"
                channelToUse = CHANNEL_ID
            }

            AlarmTriggerType.STAGE_PRE_ADHAN -> {
                titleText = "$prayerName Adhan Soon ($adhanTimeFormatted)"
                bodyText = "Mosque Adhan at $adhanTimeFormatted  •  Jama'ah at $jamaahTimeFormatted"
                baseSubText = "Pre-Adhan Reminder"
                channelToUse = CHANNEL_ID
            }

            AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN -> {
                val minsToJamaah = minutesUntilJamaah.coerceAtLeast(1)
                titleText = "$prayerName Adhan • Jama'ah in $minsToJamaah min"
                bodyText = "Adhan $adhanTimeFormatted  •  Jama'ah $jamaahTimeFormatted"
                baseSubText = "Adhan Call"
                channelToUse = CHANNEL_ID
            }

            AlarmTriggerType.STAGE_PRE_JAMAAH -> {
                val minsToJamaah = minutesUntilJamaah.coerceAtLeast(1)
                titleText = "$prayerName Jama'ah in $minsToJamaah min ($jamaahTimeFormatted)"
                bodyText = "Prepare for $prayerName Jama'ah  •  Jama'ah at $jamaahTimeFormatted"
                baseSubText = "Pre-Jama'ah Reminder"
                channelToUse = CHANNEL_ID
            }

            AlarmTriggerType.STAGE_JAMAAH_START -> {
                titleText = "$prayerName Jama'ah Time ($jamaahTimeFormatted)"
                bodyText = "Jama'ah congregation for $prayerName has started  •  Mark as Prayed when done"
                baseSubText = "Jama'ah Started"
                channelToUse = CHANNEL_ID
            }

            AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG -> {
                val passedMin = minutesPassedSinceJamaah.coerceAtLeast(1)
                val passedFormatted = formatElapsedSecondsOrHours(passedMin * 60)
                titleText = "$prayerName not prayed • $passedFormatted elapsed since Jama'ah"
                bodyText = "Jama'ah was $jamaahTimeFormatted  •  Please pray $prayerName (${minutesRemainingUntilEnd.coerceAtLeast(1)}m left)"
                baseSubText = "Every ${repeatIntervalMinutes}m Reminder"
                channelToUse = CHANNEL_ID
            }

            AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW -> {
                val remMin = minutesRemainingUntilEnd.coerceAtLeast(1)
                val passedMin = minutesPassedSinceJamaah.coerceAtLeast(1)
                val passedFormatted = formatElapsedSecondsOrHours(passedMin * 60)
                titleText = "URGENT: $remMin min left until $prayerName ends!"
                bodyText = "$prayerName not prayed ($passedFormatted elapsed since Jama'ah) • Pray now"
                baseSubText = "Urgent 15m End Warning"
                channelToUse = URGENT_CHANNEL_ID
            }
        }

        val resolvedSubText = customSubTextSuffix ?: String.format(Locale.US, "%s • %d%% loaded", baseSubText, clampedPct)

        val chimeUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(context, channelToUse)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setSubText(resolvedSubText)
            .setProgress(100, clampedPct, false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(onlyAlertOnce)
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

        // Prayer completion unlocks as soon as Prayer Start Time begins -> always include Mark as Prayed
        builder.addAction(0, context.getString(R.string.mark_as_prayed), markPrayedPi)
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

    /**
     * Posts the notification with a Discord-ping style fast-loading bar animation (~2.4 seconds)
     * that sweeps from 18% -> 52% -> 86% -> 100% and then hangs persistently in the notification
     * shade displaying the actual elapsed percentage (X% loaded).
     */
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
        val appCtx = context.applicationContext
        val targetElapsedPct = progressPercent.coerceIn(4, 100)

        // Initial ping frame (alerts once with sound/vibration, starts fast loading sweep at 18%)
        val initialNotification = buildRichPrayerNotification(
            context = appCtx,
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
            progressPercent = 18,
            nagIteration = nagIteration,
            isOngoingForeground = true,
            onlyAlertOnce = false,
            customSubTextSuffix = "Syncing prayer timer • 18%"
        )

        val posted = postNotificationIfPermitted(appCtx, initialNotification)
        if (!posted) return false

        activeSweepJob?.cancel()
        activeSweepJob = animationScope.launch {
            val fastSweepFrames = listOf(52, 86, 100)
            for (sweepPct in fastSweepFrames) {
                delay(550L)
                val sweepNotif = buildRichPrayerNotification(
                    context = appCtx,
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
                    progressPercent = sweepPct,
                    nagIteration = nagIteration,
                    isOngoingForeground = true,
                    onlyAlertOnce = true,
                    customSubTextSuffix = "Syncing prayer timer • $sweepPct%"
                )
                postNotificationIfPermitted(appCtx, sweepNotif)
            }

            // Settle after ~2.2s onto the persistent notification showing actual elapsed X% loaded
            delay(550L)
            val settledNotif = buildRichPrayerNotification(
                context = appCtx,
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
                progressPercent = targetElapsedPct,
                nagIteration = nagIteration,
                isOngoingForeground = true,
                onlyAlertOnce = true,
                customSubTextSuffix = null
            )
            postNotificationIfPermitted(appCtx, settledNotif)
        }

        return true
    }

    fun cancelNotification(context: Context) {
        activeSweepJob?.cancel()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
