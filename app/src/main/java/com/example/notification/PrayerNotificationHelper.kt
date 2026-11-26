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
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
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
    const val SOFT_PST_CHANNEL_ID = "noor_prayer_pst_soft_channel"
    const val CHANNEL_ID = "noor_prayer_soft_chime_channel"
    const val URGENT_CHANNEL_ID = "noor_prayer_urgent_end_channel"
    const val SILENT_JAMAAH_CHANNEL_ID = "noor_prayer_masjid_silent_channel"
    const val TEST_CHANNEL_ID = "noor_prayer_test_channel"
    const val NOTIFICATION_ID = 1001
    const val TEST_NOTIFICATION_ID = 1002

    const val PREFS_NAME = "noor_prayer_sound_prefs"

    // Sound Tier Keys (Item 20: Adhan, Jama'ah, and Urgent Warning sound customization)
    const val TIER_ADHAN = "ADHAN"
    const val TIER_JAMAAH = "JAMAAH"
    const val TIER_URGENT = "URGENT"

    const val KEY_ADHAN_SOUND_MODE = "adhan_sound_mode"
    const val KEY_ADHAN_CUSTOM_URI = "adhan_custom_uri"
    const val KEY_ADHAN_CUSTOM_LABEL = "adhan_custom_label"

    const val KEY_JAMAAH_SOUND_MODE = "jamaah_sound_mode"
    const val KEY_JAMAAH_CUSTOM_URI = "jamaah_custom_uri"
    const val KEY_JAMAAH_CUSTOM_LABEL = "jamaah_custom_label"

    const val KEY_URGENT_SOUND_MODE = "urgent_sound_mode"
    const val KEY_URGENT_CUSTOM_URI = "urgent_custom_uri"
    const val KEY_URGENT_CUSTOM_LABEL = "urgent_custom_label"

    const val KEY_MASJID_AUTO_SILENCE = "masjid_auto_silence_15m"

    const val URGENT_SOUND_ALARM = "ALARM"
    const val URGENT_SOUND_NOTIFICATION = "NOTIFICATION"
    const val URGENT_SOUND_CUSTOM = "CUSTOM"
    const val SOUND_MODE_SILENT = "SILENT"

    private val animationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var activeSweepJob: Job? = null

    // Item 19: Live Sound Preview Controller (Play / Stop)
    @Volatile
    private var activePreviewRingtone: Ringtone? = null
    @Volatile
    private var activePreviewTier: String? = null
    private var previewAutoStopJob: Job? = null

    fun isMasjidAutoSilenceEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MASJID_AUTO_SILENCE, true)
    }

    fun setMasjidAutoSilenceEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MASJID_AUTO_SILENCE, enabled).apply()
        ensureChannel(context)
    }

    fun getTierSoundMode(context: Context, tier: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when (tier) {
            TIER_ADHAN -> prefs.getString(KEY_ADHAN_SOUND_MODE, URGENT_SOUND_NOTIFICATION) ?: URGENT_SOUND_NOTIFICATION
            TIER_JAMAAH -> prefs.getString(KEY_JAMAAH_SOUND_MODE, URGENT_SOUND_NOTIFICATION) ?: URGENT_SOUND_NOTIFICATION
            else -> prefs.getString(KEY_URGENT_SOUND_MODE, URGENT_SOUND_ALARM) ?: URGENT_SOUND_ALARM
        }
    }

    fun getTierCustomSoundUri(context: Context, tier: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when (tier) {
            TIER_ADHAN -> prefs.getString(KEY_ADHAN_CUSTOM_URI, null)
            TIER_JAMAAH -> prefs.getString(KEY_JAMAAH_CUSTOM_URI, null)
            else -> prefs.getString(KEY_URGENT_CUSTOM_URI, null)
        }
    }

    fun getTierCustomSoundLabel(context: Context, tier: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when (tier) {
            TIER_ADHAN -> prefs.getString(KEY_ADHAN_CUSTOM_LABEL, "Custom Adhan Tone") ?: "Custom Adhan Tone"
            TIER_JAMAAH -> prefs.getString(KEY_JAMAAH_CUSTOM_LABEL, "Custom Jama'ah Tone") ?: "Custom Jama'ah Tone"
            else -> prefs.getString(KEY_URGENT_CUSTOM_LABEL, "Custom Sound") ?: "Custom Sound"
        }
    }

    fun setTierSoundPreference(
        context: Context,
        tier: String,
        mode: String,
        customUri: String? = null,
        customLabel: String? = null
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val (modeKey, uriKey, labelKey) = when (tier) {
            TIER_ADHAN -> Triple(KEY_ADHAN_SOUND_MODE, KEY_ADHAN_CUSTOM_URI, KEY_ADHAN_CUSTOM_LABEL)
            TIER_JAMAAH -> Triple(KEY_JAMAAH_SOUND_MODE, KEY_JAMAAH_CUSTOM_URI, KEY_JAMAAH_CUSTOM_LABEL)
            else -> Triple(KEY_URGENT_SOUND_MODE, KEY_URGENT_CUSTOM_URI, KEY_URGENT_CUSTOM_LABEL)
        }
        prefs.edit().apply {
            putString(modeKey, mode)
            if (customUri != null) {
                putString(uriKey, customUri)
            }
            if (!customLabel.isNullOrBlank()) {
                putString(labelKey, customLabel)
            }
            apply()
        }
        ensureChannel(context)
    }

    fun resolveTierSoundDisplayLabel(context: Context, tier: String): String {
        return when (getTierSoundMode(context, tier)) {
            URGENT_SOUND_NOTIFICATION -> "Notification Chime"
            URGENT_SOUND_CUSTOM -> getTierCustomSoundLabel(context, tier)
            SOUND_MODE_SILENT -> "Silent"
            else -> "Device Alarm"
        }
    }

    fun resolveTierSoundUri(context: Context, tier: String): Uri? {
        val chimeUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: chimeUri
        return when (getTierSoundMode(context, tier)) {
            SOUND_MODE_SILENT -> null
            URGENT_SOUND_NOTIFICATION -> chimeUri
            URGENT_SOUND_CUSTOM -> {
                val saved = getTierCustomSoundUri(context, tier)
                if (!saved.isNullOrBlank()) {
                    runCatching { Uri.parse(saved) }.getOrNull() ?: chimeUri
                } else {
                    chimeUri
                }
            }
            else -> alarmUri
        }
    }

    // Legacy wrappers for Urgent Warning Sound
    fun getUrgentSoundMode(context: Context): String = getTierSoundMode(context, TIER_URGENT)

    fun getUrgentCustomSoundUri(context: Context): String? = getTierCustomSoundUri(context, TIER_URGENT)

    fun getUrgentCustomSoundLabel(context: Context): String = getTierCustomSoundLabel(context, TIER_URGENT)

    fun setUrgentSoundPreference(
        context: Context,
        mode: String,
        customUri: String? = null,
        customLabel: String? = null
    ) = setTierSoundPreference(context, TIER_URGENT, mode, customUri, customLabel)

    fun resolveUrgentSoundDisplayLabel(context: Context): String = resolveTierSoundDisplayLabel(context, TIER_URGENT)

    fun resolveUrgentSoundUri(context: Context): Uri {
        val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        return resolveTierSoundUri(context, TIER_URGENT) ?: fallback
    }

    // Item 19: Live Sound Preview (Play / Stop)
    fun toggleSoundPreview(
        context: Context,
        tier: String,
        onStateChanged: (String?) -> Unit = {}
    ): String? {
        if (activePreviewTier == tier && activePreviewRingtone?.isPlaying == true) {
            stopSoundPreview()
            onStateChanged(null)
            return null
        }
        stopSoundPreview()
        val uri = resolveTierSoundUri(context, tier)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = runCatching { RingtoneManager.getRingtone(context.applicationContext, uri) }.getOrNull()
        if (ringtone != null) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val mode = getTierSoundMode(context, tier)
                    ringtone.audioAttributes = AudioAttributes.Builder()
                        .setUsage(
                            if (mode == URGENT_SOUND_ALARM) AudioAttributes.USAGE_ALARM
                            else AudioAttributes.USAGE_NOTIFICATION
                        )
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                ringtone.play()
                activePreviewRingtone = ringtone
                activePreviewTier = tier
                onStateChanged(tier)
                previewAutoStopJob = animationScope.launch {
                    delay(6000L)
                    if (activePreviewTier == tier) {
                        stopSoundPreview()
                        onStateChanged(null)
                    }
                }
            }.onFailure {
                stopSoundPreview()
                onStateChanged(null)
            }
        }
        return activePreviewTier
    }

    fun stopSoundPreview() {
        previewAutoStopJob?.cancel()
        previewAutoStopJob = null
        runCatching {
            activePreviewRingtone?.let {
                if (it.isPlaying) it.stop()
            }
        }
        activePreviewRingtone = null
        activePreviewTier = null
    }

    fun getActivePreviewTier(): String? {
        return if (activePreviewRingtone?.isPlaying == true) activePreviewTier else null
    }

    fun getActiveTierChannelId(context: Context, tier: String): String {
        val mode = getTierSoundMode(context, tier)
        val customHash = (getTierCustomSoundUri(context, tier) ?: "default").hashCode()
        return when (tier) {
            TIER_ADHAN -> "noor_prayer_adhan_${mode.lowercase(Locale.US)}_$customHash"
            TIER_JAMAAH -> "noor_prayer_jamaah_${mode.lowercase(Locale.US)}_$customHash"
            else -> when (mode) {
                URGENT_SOUND_NOTIFICATION -> "noor_prayer_urgent_notif_channel"
                URGENT_SOUND_CUSTOM -> "noor_prayer_urgent_custom_$customHash"
                else -> URGENT_CHANNEL_ID
            }
        }
    }

    fun getActiveUrgentChannelId(context: Context): String = getActiveTierChannelId(context, TIER_URGENT)

    fun formatElapsedSecondsOrHours(totalSeconds: Int): String {
        val clamped = totalSeconds.coerceAtLeast(0)
        val hours = clamped / 3600
        val mins = (clamped % 3600) / 60
        val secs = clamped % 60
        return if (hours > 0) {
            String.format(Locale.US, "%dh %02dm %02ds", hours, mins, secs)
        } else {
            String.format(Locale.US, "%dm %02ds", mins, secs)
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chimeUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val adhanUri = resolveTierSoundUri(context, TIER_ADHAN)
            val jamaahUri = resolveTierSoundUri(context, TIER_JAMAAH)
            val urgentUri = resolveTierSoundUri(context, TIER_URGENT)

            val adhanMode = getTierSoundMode(context, TIER_ADHAN)
            val jamaahMode = getTierSoundMode(context, TIER_JAMAAH)
            val urgentMode = getTierSoundMode(context, TIER_URGENT)

            val adhanChannelId = getActiveTierChannelId(context, TIER_ADHAN)
            val jamaahChannelId = getActiveTierChannelId(context, TIER_JAMAAH)
            val activeUrgentChannelId = getActiveTierChannelId(context, TIER_URGENT)

            fun audioAttrsForMode(mode: String): AudioAttributes {
                return AudioAttributes.Builder()
                    .setUsage(
                        if (mode == URGENT_SOUND_ALARM) AudioAttributes.USAGE_ALARM
                        else AudioAttributes.USAGE_NOTIFICATION
                    )
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }

            val notifAudioAttributes = audioAttrsForMode(URGENT_SOUND_NOTIFICATION)

            val softPstChannel = NotificationChannel(
                SOFT_PST_CHANNEL_ID,
                "Soft Prayer Start & Qaza Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle alerts when Prayer Start Time begins or when an unprayed window rolls over to Qaza"
                enableVibration(false)
                setSound(chimeUri, notifAudioAttributes)
            }

            val standardChannel = NotificationChannel(
                CHANNEL_ID,
                "Active Adhan, Jama'ah & 10m Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active Adhan calls, Jama'ah alerts, and every-10-minute post-Jama'ah reminders"
                enableVibration(true)
                setSound(chimeUri, notifAudioAttributes)
            }

            val adhanChannel = NotificationChannel(
                adhanChannelId,
                "Mosque Adhan Call Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Customizable Adhan call and Pre-Adhan notifications"
                enableVibration(adhanMode != SOUND_MODE_SILENT)
                setSound(adhanUri, if (adhanUri != null) audioAttrsForMode(adhanMode) else null)
            }

            val jamaahChannel = NotificationChannel(
                jamaahChannelId,
                "Mosque Jama'ah & Congregation Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Customizable Jama'ah and Post-Jama'ah notifications"
                enableVibration(jamaahMode != SOUND_MODE_SILENT)
                setSound(jamaahUri, if (jamaahUri != null) audioAttrsForMode(jamaahMode) else null)
            }

            val urgentChannel = NotificationChannel(
                activeUrgentChannelId,
                "Urgent End-of-Prayer Window Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-urgency warnings before prayer time ends"
                enableVibration(urgentMode != SOUND_MODE_SILENT)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                setSound(urgentUri, if (urgentUri != null) audioAttrsForMode(urgentMode) else null)
            }

            // Item 22: Completely silent, zero-vibration channel during the 15-minute mosque Jama'ah window
            val silentMasjidChannel = NotificationChannel(
                SILENT_JAMAAH_CHANNEL_ID,
                "Masjid Jama'ah Silent Window (15m)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Completely silent notifications with zero vibration during the 15-minute mosque congregation window"
                enableVibration(false)
                vibrationPattern = longArrayOf(0L)
                setSound(null, null)
            }

            val testChannel = NotificationChannel(
                TEST_CHANNEL_ID,
                "Test Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Immediate test notifications to verify sound, vibration, and lock-screen delivery"
                enableVibration(true)
                setSound(chimeUri, notifAudioAttributes)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(softPstChannel)
            manager.createNotificationChannel(standardChannel)
            manager.createNotificationChannel(adhanChannel)
            manager.createNotificationChannel(jamaahChannel)
            manager.createNotificationChannel(urgentChannel)
            manager.createNotificationChannel(silentMasjidChannel)
            manager.createNotificationChannel(testChannel)
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
        customSubTextSuffix: String? = null,
        secondsUntilJamaahOverride: Int? = null,
        secondsPassedSinceJamaahOverride: Int? = null,
        secondsRemainingUntilEndOverride: Int? = null,
        fixedChronometerWhenMs: Long? = null,
        pstEpochMillis: Long = 0L,
        adhanEpochMillis: Long = 0L,
        jamaahEpochMillis: Long = 0L,
        windowEndEpochMillis: Long = 0L
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

        // Primary Action: Mark as Prayed
        val markPrayedIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ExactPrayerAlarmScheduler.ACTION_MARK_PRAYED
            putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_ID, prayerId)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_DATE_ISO, dateIso)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_TIME, jamaahTimeFormatted)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_UNTIL_JAMAAH, minutesUntilJamaah)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_PASSED_SINCE_JAMAAH, minutesPassedSinceJamaah)
        }
        val markPrayedPi = PendingIntent.getBroadcast(
            context,
            prayerId.hashCode() + 101,
            markPrayedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Item 21: Single Snooze 5m Action Button preserving full real prayer window epoch metadata
        val snooze5mIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ExactPrayerAlarmScheduler.ACTION_SNOOZE_5M
            putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_ID, prayerId)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_DATE_ISO, dateIso)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_PST_TIME, pstTimeFormatted)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_ADHAN_TIME, adhanTimeFormatted)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_TIME, jamaahTimeFormatted)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_PST_EPOCH_MS, pstEpochMillis)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_ADHAN_EPOCH_MS, adhanEpochMillis)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_EPOCH_MS, jamaahEpochMillis)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_WINDOW_END_EPOCH_MS, windowEndEpochMillis)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_UNTIL_JAMAAH, minutesUntilJamaah)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_PASSED_SINCE_JAMAAH, minutesPassedSinceJamaah)
            putExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_REMAINING_UNTIL_END, minutesRemainingUntilEnd)
        }
        val snooze5mPi = PendingIntent.getBroadcast(
            context,
            prayerId.hashCode() + 102,
            snooze5mIntent,
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
        val nowEpochMs = System.currentTimeMillis()

        val liveSecUntilJamaah = secondsUntilJamaahOverride ?: (minutesUntilJamaah.coerceAtLeast(1) * 60)
        val liveSecPassedSinceJamaah = secondsPassedSinceJamaahOverride ?: (minutesPassedSinceJamaah.coerceAtLeast(0) * 60)
        val liveSecRemainingUntilEnd = secondsRemainingUntilEndOverride ?: (minutesRemainingUntilEnd.coerceAtLeast(1) * 60)

        val untilJamaahFormatted = formatElapsedSecondsOrHours(liveSecUntilJamaah)
        val passedJamaahFormatted = formatElapsedSecondsOrHours(liveSecPassedSinceJamaah.coerceAtLeast(1))
        val remainingEndFormatted = formatElapsedSecondsOrHours(liveSecRemainingUntilEnd)

        // Item 22: Check if currently inside the 15-minute mosque Jama'ah congregation window [Jama'ah, Jama'ah + 15m]
        val isInside15mJamaahWindow = isMasjidAutoSilenceEnabled(context) && (
            triggerType == AlarmTriggerType.STAGE_JAMAAH_START ||
                (triggerType == AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG && liveSecPassedSinceJamaah in 0..(15 * 60))
        )

        val titleText: String
        val bodyText: String
        val baseSubText: String
        val rawChannelToUse: String
        val rawSoundUri: Uri?
        val useLiveChronometer: Boolean
        val chronometerCountDown: Boolean
        val chronometerWhenMs: Long

        when (triggerType) {
            AlarmTriggerType.STAGE_1_PST_START -> {
                titleText = "$prayerName Started ($pstDisplay) • Jama'ah in $untilJamaahFormatted"
                bodyText = "Adhan at $adhanTimeFormatted  •  $untilJamaahFormatted until Jama'ah ($jamaahTimeFormatted)"
                baseSubText = "Prayer Start Time"
                rawChannelToUse = SOFT_PST_CHANNEL_ID
                rawSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                useLiveChronometer = true
                chronometerCountDown = true
                chronometerWhenMs = fixedChronometerWhenMs ?: (nowEpochMs + (liveSecUntilJamaah * 1000L))
            }

            AlarmTriggerType.STAGE_PRE_ADHAN -> {
                titleText = "$prayerName Adhan Soon ($adhanTimeFormatted)"
                bodyText = "$untilJamaahFormatted until Jama'ah ($jamaahTimeFormatted)  •  Adhan at $adhanTimeFormatted"
                baseSubText = "Pre-Adhan Reminder"
                rawChannelToUse = getActiveTierChannelId(context, TIER_ADHAN)
                rawSoundUri = resolveTierSoundUri(context, TIER_ADHAN)
                useLiveChronometer = true
                chronometerCountDown = true
                chronometerWhenMs = fixedChronometerWhenMs ?: (nowEpochMs + (liveSecUntilJamaah * 1000L))
            }

            AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN -> {
                titleText = "$prayerName Adhan • Jama'ah in $untilJamaahFormatted"
                bodyText = "$untilJamaahFormatted until Jama'ah ($jamaahTimeFormatted)  •  Adhan $adhanTimeFormatted"
                baseSubText = "Adhan Call"
                rawChannelToUse = getActiveTierChannelId(context, TIER_ADHAN)
                rawSoundUri = resolveTierSoundUri(context, TIER_ADHAN)
                useLiveChronometer = true
                chronometerCountDown = true
                chronometerWhenMs = fixedChronometerWhenMs ?: (nowEpochMs + (liveSecUntilJamaah * 1000L))
            }

            AlarmTriggerType.STAGE_PRE_JAMAAH -> {
                titleText = "$prayerName Jama'ah in $untilJamaahFormatted ($jamaahTimeFormatted)"
                bodyText = "$untilJamaahFormatted until $prayerName Jama'ah  •  Jama'ah at $jamaahTimeFormatted"
                baseSubText = "Pre-Jama'ah Reminder"
                rawChannelToUse = getActiveTierChannelId(context, TIER_JAMAAH)
                rawSoundUri = resolveTierSoundUri(context, TIER_JAMAAH)
                useLiveChronometer = true
                chronometerCountDown = true
                chronometerWhenMs = fixedChronometerWhenMs ?: (nowEpochMs + (liveSecUntilJamaah * 1000L))
            }

            AlarmTriggerType.STAGE_JAMAAH_START -> {
                titleText = "$prayerName Jama'ah ($jamaahTimeFormatted) • $passedJamaahFormatted passed"
                bodyText = "Jama'ah started  •  $remainingEndFormatted remaining until $prayerName end"
                baseSubText = if (isInside15mJamaahWindow) "Jama'ah Window (Silent Mode)" else "Jama'ah Started"
                rawChannelToUse = getActiveTierChannelId(context, TIER_JAMAAH)
                rawSoundUri = resolveTierSoundUri(context, TIER_JAMAAH)
                useLiveChronometer = true
                chronometerCountDown = false
                chronometerWhenMs = fixedChronometerWhenMs ?: (nowEpochMs - (liveSecPassedSinceJamaah * 1000L))
            }

            AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG -> {
                val isPastHalfway = liveSecPassedSinceJamaah >= liveSecRemainingUntilEnd
                titleText = if (isPastHalfway) {
                    "$prayerName not prayed • $remainingEndFormatted remaining until $prayerName end"
                } else {
                    "$prayerName not prayed • $passedJamaahFormatted passed"
                }
                bodyText = "$passedJamaahFormatted elapsed since Jama'ah ($jamaahTimeFormatted)  •  $remainingEndFormatted remaining until $prayerName end"
                baseSubText = if (isInside15mJamaahWindow) {
                    "Masjid Silent Window (15m)"
                } else {
                    "Every ${repeatIntervalMinutes}m Reminder"
                }
                rawChannelToUse = getActiveTierChannelId(context, TIER_JAMAAH)
                rawSoundUri = resolveTierSoundUri(context, TIER_JAMAAH)
                useLiveChronometer = true
                chronometerCountDown = isPastHalfway
                chronometerWhenMs = fixedChronometerWhenMs ?: if (isPastHalfway) {
                    nowEpochMs + (liveSecRemainingUntilEnd * 1000L)
                } else {
                    nowEpochMs - (liveSecPassedSinceJamaah * 1000L)
                }
            }

            AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW -> {
                titleText = "URGENT: $remainingEndFormatted remaining until $prayerName end!"
                bodyText = "$prayerName not prayed ($passedJamaahFormatted passed) • Pray now"
                baseSubText = "Urgent 15m End Warning"
                rawChannelToUse = getActiveTierChannelId(context, TIER_URGENT)
                rawSoundUri = resolveTierSoundUri(context, TIER_URGENT)
                useLiveChronometer = true
                chronometerCountDown = true
                chronometerWhenMs = fixedChronometerWhenMs ?: (nowEpochMs + (liveSecRemainingUntilEnd * 1000L))
            }

            AlarmTriggerType.STAGE_QAZA_ROLLOVER -> {
                titleText = "$prayerName Marked as Qaza"
                bodyText = "$prayerName window ended unprayed  •  Tap 'Mark as Prayed' once you make it up"
                baseSubText = "Qaza Rollover Alert"
                rawChannelToUse = SOFT_PST_CHANNEL_ID
                rawSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                useLiveChronometer = false
                chronometerCountDown = false
                chronometerWhenMs = nowEpochMs
            }
        }

        // Item 22: If inside the 15-minute Jama'ah window and Masjid Auto-Silence is on, force SILENT_JAMAAH_CHANNEL_ID with no sound and no vibration
        val channelToUse = if (isInside15mJamaahWindow) SILENT_JAMAAH_CHANNEL_ID else rawChannelToUse
        val soundUri = if (isInside15mJamaahWindow) null else rawSoundUri

        val resolvedSubText = customSubTextSuffix ?: if (triggerType == AlarmTriggerType.STAGE_QAZA_ROLLOVER) {
            "$baseSubText • Log make-up prayer"
        } else {
            String.format(Locale.US, "%s • %d%% loaded", baseSubText, clampedPct)
        }

        val builder = NotificationCompat.Builder(context, channelToUse)
            .setSmallIcon(R.drawable.ic_stat_noor_prayer)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setSubText(resolvedSubText)
            .setProgress(100, if (triggerType == AlarmTriggerType.STAGE_QAZA_ROLLOVER) 100 else clampedPct, false)
            .setShowWhen(true)
            .setWhen(chronometerWhenMs)
            .setUsesChronometer(useLiveChronometer && customSubTextSuffix == null)
            .setPriority(if (isInside15mJamaahWindow) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(onlyAlertOnce || isInside15mJamaahWindow)
            .setSilent(isInside15mJamaahWindow)
            .setCategory(
                if (triggerType == AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW) {
                    NotificationCompat.CATEGORY_ALARM
                } else {
                    NotificationCompat.CATEGORY_REMINDER
                }
            )
            .setContentIntent(contentPendingIntent)
            .setOngoing(isOngoingForeground && triggerType != AlarmTriggerType.STAGE_QAZA_ROLLOVER)
            .setAutoCancel(!isOngoingForeground || triggerType == AlarmTriggerType.STAGE_QAZA_ROLLOVER)

        if (isInside15mJamaahWindow) {
            builder.setVibrate(longArrayOf(0L))
            builder.setDefaults(0)
        } else if (soundUri != null) {
            builder.setSound(soundUri)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setChronometerCountDown(chronometerCountDown)
        }

        val actionLabel = if (triggerType == AlarmTriggerType.STAGE_QAZA_ROLLOVER) {
            "Log $prayerName (Qaza)"
        } else {
            context.getString(R.string.mark_as_prayed)
        }
        builder.addAction(0, actionLabel, markPrayedPi)
        if (triggerType != AlarmTriggerType.STAGE_QAZA_ROLLOVER) {
            builder.addAction(0, "Snooze 5m", snooze5mPi)
        }
        builder.addAction(0, "Dismiss", dismissPi)
        return builder.build()
    }

    fun postNotificationIfPermitted(
        context: Context,
        notification: Notification,
        notificationId: Int = NOTIFICATION_ID
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        NotificationManagerCompat.from(context).notify(notificationId, notification)
        return true
    }

    /**
     * Fires an immediate, real, dismissible Android system notification on a dedicated ID (1002)
     * so users can verify status bar icon, heads-up banner, sound, and vibration at any hour
     * without interfering with scheduled prayer alarms.
     */
    fun showTestNotification(
        context: Context,
        nextPrayerName: String,
        nextPrayerTimeFormatted: String,
        canScheduleExactAlarms: Boolean
    ): Boolean {
        val appCtx = context.applicationContext
        ensureChannel(appCtx)

        val openAppIntent = Intent(appCtx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appCtx,
            TEST_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmStatus = if (canScheduleExactAlarms) {
            "Exact wake alarms active"
        } else {
            "Enable Alarms & Reminders in Settings for exact timing"
        }
        val bodyText = "Notifications are working. Next armed prayer: $nextPrayerName at $nextPrayerTimeFormatted · $alarmStatus."
        val chimeUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(appCtx, TEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_noor_prayer)
            .setContentTitle("Noor Prayer · Test Notification")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setSubText("Delivery Check")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(chimeUri)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(contentPendingIntent)
            .build()

        return postNotificationIfPermitted(appCtx, notification, TEST_NOTIFICATION_ID)
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
        nagIteration: Int = 0,
        pstEpochMillis: Long = 0L,
        adhanEpochMillis: Long = 0L,
        jamaahEpochMillis: Long = 0L,
        windowEndEpochMillis: Long = 0L
    ): Boolean {
        val appCtx = context.applicationContext
        val targetElapsedPct = progressPercent.coerceIn(4, 100)
        val startBaselineMs = System.currentTimeMillis()
        val nowSecondInMin = ((startBaselineMs / 1000L) % 60L).toInt()
        val initialSecUntilJamaah = (minutesUntilJamaah.coerceAtLeast(1) * 60 - nowSecondInMin).coerceAtLeast(1)
        val initialSecPassedSinceJamaah = (minutesPassedSinceJamaah.coerceAtLeast(0) * 60 + nowSecondInMin).coerceAtLeast(0)
        val initialSecRemainingUntilEnd = (minutesRemainingUntilEnd.coerceAtLeast(1) * 60 - nowSecondInMin).coerceAtLeast(1)

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
            progressPercent = targetElapsedPct,
            nagIteration = nagIteration,
            isOngoingForeground = false,
            onlyAlertOnce = false,
            customSubTextSuffix = null,
            secondsUntilJamaahOverride = initialSecUntilJamaah,
            secondsPassedSinceJamaahOverride = initialSecPassedSinceJamaah,
            secondsRemainingUntilEndOverride = initialSecRemainingUntilEnd,
            pstEpochMillis = pstEpochMillis,
            adhanEpochMillis = adhanEpochMillis,
            jamaahEpochMillis = jamaahEpochMillis,
            windowEndEpochMillis = windowEndEpochMillis
        )

        val posted = postNotificationIfPermitted(appCtx, initialNotification)
        if (!posted) return false

        if (triggerType != AlarmTriggerType.STAGE_QAZA_ROLLOVER) {
            startLiveSecondTicker(
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
                targetElapsedPct = targetElapsedPct,
                nagIteration = nagIteration,
                runInitialFastSweep = false,
                isOngoingForeground = false,
                pstEpochMillis = pstEpochMillis,
                adhanEpochMillis = adhanEpochMillis,
                jamaahEpochMillis = jamaahEpochMillis,
                windowEndEpochMillis = windowEndEpochMillis
            )
        }

        return true
    }

    fun startLiveSecondTicker(
        context: Context,
        prayerId: String,
        prayerName: String,
        dateIso: String,
        pstTimeFormatted: String,
        adhanTimeFormatted: String,
        jamaahTimeFormatted: String,
        triggerType: AlarmTriggerType,
        minutesUntilJamaah: Int,
        minutesPassedSinceJamaah: Int,
        minutesRemainingUntilEnd: Int,
        repeatIntervalMinutes: Int,
        preJamaahAlertMinutes: Int = 15,
        targetElapsedPct: Int = 50,
        nagIteration: Int = 0,
        runInitialFastSweep: Boolean = false,
        isOngoingForeground: Boolean = false,
        pstEpochMillis: Long = 0L,
        adhanEpochMillis: Long = 0L,
        jamaahEpochMillis: Long = 0L,
        windowEndEpochMillis: Long = 0L
    ) {
        val appCtx = context.applicationContext
        val startBaselineMs = System.currentTimeMillis()
        val nowSecondInMin = ((startBaselineMs / 1000L) % 60L).toInt()
        val initialSecUntilJamaah = (minutesUntilJamaah.coerceAtLeast(1) * 60 - nowSecondInMin).coerceAtLeast(1)
        val initialSecPassedSinceJamaah = (minutesPassedSinceJamaah.coerceAtLeast(0) * 60 + nowSecondInMin).coerceAtLeast(0)
        val initialSecRemainingUntilEnd = (minutesRemainingUntilEnd.coerceAtLeast(1) * 60 - nowSecondInMin).coerceAtLeast(1)

        val fixedChronoWhen = when (triggerType) {
            AlarmTriggerType.STAGE_1_PST_START,
            AlarmTriggerType.STAGE_PRE_ADHAN,
            AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN,
            AlarmTriggerType.STAGE_PRE_JAMAAH -> startBaselineMs + (initialSecUntilJamaah * 1000L)
            AlarmTriggerType.STAGE_JAMAAH_START -> startBaselineMs - (initialSecPassedSinceJamaah * 1000L)
            AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG -> {
                if (initialSecPassedSinceJamaah >= initialSecRemainingUntilEnd) {
                    startBaselineMs + (initialSecRemainingUntilEnd * 1000L)
                } else {
                    startBaselineMs - (initialSecPassedSinceJamaah * 1000L)
                }
            }
            AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW -> startBaselineMs + (initialSecRemainingUntilEnd * 1000L)
            AlarmTriggerType.STAGE_QAZA_ROLLOVER -> startBaselineMs
        }

        activeSweepJob?.cancel()
        activeSweepJob = animationScope.launch {
            if (runInitialFastSweep) {
                val fastSweepFrames = listOf(52, 86, 100)
                for (sweepPct in fastSweepFrames) {
                    delay(550L)
                    val elapsedSec = ((System.currentTimeMillis() - startBaselineMs) / 1000L).toInt().coerceAtLeast(0)
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
                        isOngoingForeground = isOngoingForeground,
                        onlyAlertOnce = true,
                        customSubTextSuffix = "Syncing prayer timer • $sweepPct%",
                        secondsUntilJamaahOverride = (initialSecUntilJamaah - elapsedSec).coerceAtLeast(0),
                        secondsPassedSinceJamaahOverride = (initialSecPassedSinceJamaah + elapsedSec).coerceAtLeast(0),
                        secondsRemainingUntilEndOverride = (initialSecRemainingUntilEnd - elapsedSec).coerceAtLeast(0),
                        fixedChronometerWhenMs = fixedChronoWhen,
                        pstEpochMillis = pstEpochMillis,
                        adhanEpochMillis = adhanEpochMillis,
                        jamaahEpochMillis = jamaahEpochMillis,
                        windowEndEpochMillis = windowEndEpochMillis
                    )
                    postNotificationIfPermitted(appCtx, sweepNotif)
                }
                delay(550L)
            }

            if (triggerType == AlarmTriggerType.STAGE_QAZA_ROLLOVER) {
                val qazaNotif = buildRichPrayerNotification(
                    context = appCtx,
                    prayerId = prayerId,
                    prayerName = prayerName,
                    dateIso = dateIso,
                    pstTimeFormatted = pstTimeFormatted,
                    adhanTimeFormatted = adhanTimeFormatted,
                    jamaahTimeFormatted = jamaahTimeFormatted,
                    triggerType = AlarmTriggerType.STAGE_QAZA_ROLLOVER,
                    progressPercent = 100,
                    isOngoingForeground = false,
                    onlyAlertOnce = true
                )
                postNotificationIfPermitted(appCtx, qazaNotif)
                return@launch
            }

            while (true) {
                delay(15_000L)
                // Abort silently if user already marked this prayer as Prayed
                if (dateIso.isNotBlank()) {
                    val alreadyPrayed = kotlinx.coroutines.withContext(Dispatchers.IO) {
                        runCatching {
                            com.example.data.AppDatabase.getInstance(appCtx)
                                .prayerDao()
                                .getPrayerRecordSync(dateIso, prayerId)
                                ?.isPrayed == true
                        }.getOrDefault(false)
                    }
                    if (alreadyPrayed) {
                        cancelNotification(appCtx)
                        break
                    }
                }

                val nowMs = System.currentTimeMillis()
                val elapsedSec = ((nowMs - startBaselineMs) / 1000L).toInt().coerceAtLeast(0)
                val curSecUntilJamaah = (initialSecUntilJamaah - elapsedSec).coerceAtLeast(0)
                val curSecPassedSinceJamaah = (initialSecPassedSinceJamaah + elapsedSec).coerceAtLeast(0)
                val curSecRemainingUntilEnd = (initialSecRemainingUntilEnd - elapsedSec).coerceAtLeast(0)

                val liveStage = when {
                    curSecRemainingUntilEnd <= 0 -> AlarmTriggerType.STAGE_QAZA_ROLLOVER
                    triggerType in listOf(
                        AlarmTriggerType.STAGE_1_PST_START,
                        AlarmTriggerType.STAGE_PRE_ADHAN,
                        AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN,
                        AlarmTriggerType.STAGE_PRE_JAMAAH
                    ) && curSecUntilJamaah <= 0 -> AlarmTriggerType.STAGE_JAMAAH_START
                    else -> triggerType
                }

                val livePct = if (pstEpochMillis > 0L && windowEndEpochMillis > pstEpochMillis) {
                    val totalWin = (windowEndEpochMillis - pstEpochMillis).coerceAtLeast(60_000L)
                    val elapsedWin = (nowMs - pstEpochMillis).coerceIn(0L, totalWin)
                    ((elapsedWin * 100L) / totalWin).toInt().coerceIn(4, 100)
                } else {
                    val estTotalSec = (initialSecPassedSinceJamaah + initialSecRemainingUntilEnd).coerceAtLeast(60)
                    val estElapsedSec = (curSecPassedSinceJamaah).coerceIn(0, estTotalSec)
                    if (curSecPassedSinceJamaah > 0) {
                        ((estElapsedSec * 100) / estTotalSec).coerceIn(targetElapsedPct, 100)
                    } else {
                        targetElapsedPct
                    }
                }

                val liveNotif = buildRichPrayerNotification(
                    context = appCtx,
                    prayerId = prayerId,
                    prayerName = prayerName,
                    dateIso = dateIso,
                    pstTimeFormatted = pstTimeFormatted,
                    adhanTimeFormatted = adhanTimeFormatted,
                    jamaahTimeFormatted = jamaahTimeFormatted,
                    triggerType = liveStage,
                    minutesUntilJamaah = (curSecUntilJamaah + 59) / 60,
                    minutesPassedSinceJamaah = curSecPassedSinceJamaah / 60,
                    minutesRemainingUntilEnd = (curSecRemainingUntilEnd + 59) / 60,
                    repeatIntervalMinutes = repeatIntervalMinutes,
                    preJamaahAlertMinutes = preJamaahAlertMinutes,
                    progressPercent = livePct,
                    nagIteration = nagIteration,
                    isOngoingForeground = isOngoingForeground && liveStage != AlarmTriggerType.STAGE_QAZA_ROLLOVER,
                    onlyAlertOnce = true,
                    customSubTextSuffix = null,
                    secondsUntilJamaahOverride = curSecUntilJamaah,
                    secondsPassedSinceJamaahOverride = curSecPassedSinceJamaah,
                    secondsRemainingUntilEndOverride = curSecRemainingUntilEnd,
                    fixedChronometerWhenMs = fixedChronoWhen,
                    pstEpochMillis = pstEpochMillis,
                    adhanEpochMillis = adhanEpochMillis,
                    jamaahEpochMillis = jamaahEpochMillis,
                    windowEndEpochMillis = windowEndEpochMillis
                )
                postNotificationIfPermitted(appCtx, liveNotif)

                if (liveStage == AlarmTriggerType.STAGE_QAZA_ROLLOVER) {
                    break
                }
            }
        }
    }

    fun cancelNotification(context: Context) {
        activeSweepJob?.cancel()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
