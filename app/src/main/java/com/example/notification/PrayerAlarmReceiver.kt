package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.data.PrayerAlarmDataStore
import com.example.data.PrayerCompletionStatus
import com.example.data.PrayerRecordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Core BroadcastReceiver handling the 4-stage Dynamic Notification Lifecycle:
 * - Stage 1 (STAGE_1_PST_START): Fires at Prayer Start Time (PST), alerts that prayer time has started,
 *   auto-marks any unprayed previous prayer as MISSED_QADA (if expired after first launch), and arms Stage 2 at Adhan.
 * - Stage 2 (STAGE_2_ADHAN_COUNTDOWN): Fires at Adhan Time, alerts that X minutes remain before Jama'ah,
 *   and arms Stage 3 for 10 minutes after Jama'ah.
 * - Stage 3 (STAGE_3_POST_JAMAAH_NAG): Starts 10 minutes after Jama'ah if still unticked, then repeats
 *   every configurable N minutes (default 15m, user-configurable 10-15m+) stating that X prayer is still not prayed
 *   and Y minutes have passed.
 * - Stage 4 (STAGE_4_URGENT_END_OF_WINDOW): When close to the next prayer / end of prayer window, alarmingly nags
 *   that Z minutes are remaining until prayer time ends.
 * - Action Intents: Handles "Mark as Prayed", "Snooze 5m", and "Dismiss" directly from the banner.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val db = AppDatabase.getInstance(context)
                val dao = db.prayerDao()
                val alarmDataStore = PrayerAlarmDataStore(context)
                val scheduler = ExactPrayerAlarmScheduler(context)

                val todayIso = intent.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_DATE_ISO)
                    ?.takeIf { it.isNotBlank() }
                    ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
                val prayerId = intent.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_ID) ?: "ASR"
                val prayerName = intent.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_NAME) ?: "Asr"
                val pstTime = intent.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_PST_TIME) ?: ""
                val adhanTime = intent.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_ADHAN_TIME) ?: "03:45 PM"
                val jamaahTime = intent.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_TIME) ?: "04:00 PM"
                val pstEpochMs = intent.getLongExtra(ExactPrayerAlarmScheduler.EXTRA_PST_EPOCH_MS, 0L)
                val adhanEpochMs = intent.getLongExtra(ExactPrayerAlarmScheduler.EXTRA_ADHAN_EPOCH_MS, 0L)
                val jamaahEpochMs = intent.getLongExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_EPOCH_MS, 0L)
                val windowEndEpochMs = intent.getLongExtra(ExactPrayerAlarmScheduler.EXTRA_WINDOW_END_EPOCH_MS, 0L)

                when (action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    "android.intent.action.LOCKED_BOOT_COMPLETED",
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" -> {
                        scheduler.syncAndArmAllDailyAlarms(
                            postActiveWindowNotificationIfNeeded = true
                        )
                    }

                    ExactPrayerAlarmScheduler.ACTION_MARK_PRAYED -> {
                        val nowMarkMs = System.currentTimeMillis()
                        val existing = dao.getPrayerRecordSync(todayIso, prayerId)
                        val wasQaza = existing?.completionStatus == PrayerCompletionStatus.MISSED_QADA.name

                        val runtimeState = alarmDataStore.runtimeStateFlow.first()
                        val autoJamaahWindow = runtimeState.autoJamaahWindowMinutes
                        val repository = com.example.data.PrayerRepository(dao)
                        val city = alarmDataStore.getSavedCitySnapshot()?.first
                            ?: repository.getSavedSelectedCity()?.first
                            ?: com.example.domain.PrayerTimeCalculator.detectBestDefaultCityForDeviceTimeZone()
                        val cityTz = com.example.domain.PrayerTimeCalculator.getTimeZoneForCity(city)
                        val nowCal = Calendar.getInstance(cityTz)
                        val nowMinutesOfDay = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
                        val calc = com.example.domain.PrayerTimeCalculator.calculateDailyPrayers(city, nowCal)
                        val isFriday = nowCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
                        val settingsKey = if (prayerId == "DHUHR" && isFriday) "JUMUAH" else prayerId
                        val setting = dao.getAllPrayerSettingsSync().firstOrNull { it.prayerId == settingsKey }
                        val pstMin = when (prayerId) {
                            "FAJR" -> calc.fajrMinutes
                            "DHUHR" -> calc.dhuhrMinutes
                            "ASR" -> calc.asrMinutes
                            "MAGHRIB" -> calc.maghribMinutes
                            "ISHA" -> calc.ishaMinutes
                            else -> calc.asrMinutes
                        }
                        val defaultUseFixedAdhan = settingsKey != "MAGHRIB"
                        val defaultFixedAdhanMin = when (settingsKey) {
                            "FAJR" -> 5 * 60
                            "DHUHR" -> 13 * 60
                            "JUMUAH" -> 13 * 60
                            "ASR" -> 16 * 60 + 30
                            "ISHA" -> 20 * 60 + 15
                            else -> pstMin
                        }
                        val adhanMin = if (setting?.useFixedAdhanTime ?: defaultUseFixedAdhan) {
                            (setting?.fixedAdhanTimeMinutes ?: defaultFixedAdhanMin).coerceIn(0, 1439).coerceAtLeast(pstMin)
                        } else {
                            (pstMin + (setting?.adhanOffsetMinutes ?: 0)).coerceAtLeast(pstMin)
                        }
                        val jamaahMin = if (setting?.useFixedTime == true) {
                            setting.fixedTimeMinutes.coerceAtLeast(adhanMin)
                        } else {
                            (adhanMin + (setting?.jamaahOffsetMinutes ?: 15)).coerceAtLeast(adhanMin)
                        }

                        val resolvedInJamaah = when {
                            wasQaza -> false
                            autoJamaahWindow <= 0 -> true
                            else -> kotlin.math.abs(nowMinutesOfDay - jamaahMin) <= autoJamaahWindow
                        }

                        dao.upsertPrayerRecord(
                            PrayerRecordEntity(
                                dateIso = todayIso,
                                prayerId = prayerId,
                                isPrayed = true,
                                isInJamaah = resolvedInJamaah,
                                completedAtMillis = nowMarkMs,
                                completionStatus = if (wasQaza) {
                                    PrayerCompletionStatus.PRAYED_LATE.name
                                } else {
                                    PrayerCompletionStatus.PRAYED_ON_TIME.name
                                },
                                nagCountTriggered = existing?.nagCountTriggered ?: 0
                            )
                        )
                        scheduler.cancelAllAlarmsForPrayer(prayerId)
                        PrayerNaggingForegroundService.stopNaggingLoop(context)
                        PrayerNotificationHelper.cancelNotification(context)
                        val modeSuffix = when {
                            wasQaza -> "(Qaza Make-Up)"
                            resolvedInJamaah -> "(In Jama'ah)"
                            else -> "(Individually)"
                        }
                        alarmDataStore.clearDismissedPrayerKey()
                        alarmDataStore.clearNaggingLoop(
                            reason = "$prayerName marked as Prayed $modeSuffix via Notification Banner ✓",
                            stageLabel = if (wasQaza) "PRAYED_LATE" else "PRAYED_ON_TIME"
                        )
                        scheduler.syncAndArmAllDailyAlarms(postActiveWindowNotificationIfNeeded = false)
                    }

                    ExactPrayerAlarmScheduler.ACTION_SNOOZE_5M -> {
                        val nextSnoozeEpoch = System.currentTimeMillis() + (5 * 60_000L)
                        val minUntilJamaah = if (jamaahEpochMs > 0L) {
                            ((jamaahEpochMs - nextSnoozeEpoch + 59_999L) / 60_000L).toInt().coerceAtLeast(0)
                        } else {
                            (intent.getIntExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_UNTIL_JAMAAH, 0) - 5).coerceAtLeast(0)
                        }
                        val snoozedStage = if (minUntilJamaah > 0) {
                            AlarmTriggerType.STAGE_PRE_JAMAAH
                        } else {
                            AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG
                        }
                        alarmDataStore.clearDismissedPrayerKey()
                        scheduler.cancelAllAlarmsForPrayer(prayerId)
                        scheduler.scheduleExactTrigger(
                            prayerId = prayerId,
                            prayerName = prayerName,
                            dateIso = todayIso,
                            triggerType = snoozedStage,
                            triggerAtEpochMillis = nextSnoozeEpoch,
                            pstTimeFormatted = pstTime,
                            adhanTimeFormatted = adhanTime,
                            jamaahTimeFormatted = jamaahTime,
                            pstEpochMillis = pstEpochMs,
                            adhanEpochMillis = adhanEpochMs,
                            jamaahEpochMillis = jamaahEpochMs,
                            windowEndEpochMillis = windowEndEpochMs,
                            minutesUntilJamaah = minUntilJamaah,
                            nagIteration = 1
                        )
                        PrayerNaggingForegroundService.stopNaggingLoop(context)
                        PrayerNotificationHelper.cancelNotification(context)
                        alarmDataStore.recordSnoozeFiveMinutes(prayerId, nextSnoozeEpoch)
                    }

                    ExactPrayerAlarmScheduler.ACTION_DISMISS_NAG -> {
                        scheduler.cancelAllAlarmsForPrayer(prayerId)
                        PrayerNaggingForegroundService.stopNaggingLoop(context)
                        PrayerNotificationHelper.cancelNotification(context)
                        alarmDataStore.recordDismissedPrayerKey("${todayIso}_$prayerId")
                        alarmDataStore.clearNaggingLoop(
                            reason = "$prayerName notification dismissed by user",
                            stageLabel = "DISMISSED"
                        )
                        scheduler.syncAndArmAllDailyAlarms(postActiveWindowNotificationIfNeeded = false)
                    }

                    ExactPrayerAlarmScheduler.ACTION_PRAYER_ALARM_TRIGGER -> {
                        val triggerName = intent.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_TRIGGER_TYPE)
                            ?: AlarmTriggerType.STAGE_1_PST_START.name
                        val triggerType = runCatching { AlarmTriggerType.valueOf(triggerName) }
                            .getOrDefault(AlarmTriggerType.STAGE_1_PST_START)
                        val nagIteration = intent.getIntExtra(ExactPrayerAlarmScheduler.EXTRA_NAG_ITERATION, 0)

                        val prefs = dao.getUserPreferencesSync()
                        val preAdhanMin = (prefs?.preAdhanAlertMinutes ?: 5).coerceIn(0, 30)
                        val preJamaahMin = (prefs?.preJamaahAlertMinutes ?: 15).coerceIn(5, 45)
                        val nagIntervalMin = (prefs?.nagIntervalMinutes ?: 10).coerceIn(5, 60)
                        val urgentThresholdMin = (prefs?.urgentEndWindowMinutes ?: 15).coerceIn(5, 60)
                        val firstLaunchEpochMs = prefs?.firstLaunchEpochMs ?: 0L

                        // 1. If prayer is already marked as Prayed in Room, cancel all alarms & stop notification
                        val currentRecord = dao.getPrayerRecordSync(todayIso, prayerId)
                        if (currentRecord?.isPrayed == true) {
                            scheduler.cancelAllAlarmsForPrayer(prayerId)
                            PrayerNaggingForegroundService.stopNaggingLoop(context)
                            alarmDataStore.clearNaggingLoop(
                                reason = "$prayerName already marked Prayed • Notifications cancelled",
                                stageLabel = "PRAYED_ON_TIME"
                            )
                            scheduler.syncAndArmAllDailyAlarms(postActiveWindowNotificationIfNeeded = false)
                            return@launch
                        }

                        // 1a. If user explicitly dismissed this prayer window, do not fire intermediate/nag reminders
                        val dismissedKey = alarmDataStore.runtimeStateFlow.first().dismissedPrayerKey
                        if (dismissedKey == "${todayIso}_$prayerId" && triggerType != AlarmTriggerType.STAGE_QAZA_ROLLOVER) {
                            return@launch
                        }

                        val nowMs = System.currentTimeMillis()

                        // 1b. Strict guard: never post any notification before the prayer's natural start time (PST)
                        if (pstEpochMs > 0L && nowMs < pstEpochMs - 15_000L) {
                            scheduler.scheduleExactTrigger(
                                prayerId = prayerId,
                                prayerName = prayerName,
                                dateIso = todayIso,
                                triggerType = AlarmTriggerType.STAGE_1_PST_START,
                                triggerAtEpochMillis = pstEpochMs,
                                pstTimeFormatted = pstTime,
                                adhanTimeFormatted = adhanTime,
                                jamaahTimeFormatted = jamaahTime,
                                pstEpochMillis = pstEpochMs,
                                adhanEpochMillis = adhanEpochMs,
                                jamaahEpochMillis = jamaahEpochMs,
                                windowEndEpochMillis = windowEndEpochMs,
                                minutesUntilJamaah = ((jamaahEpochMs - pstEpochMs + 59_999L) / 60_000L).toInt().coerceAtLeast(0),
                                nagIteration = 0
                            )
                            return@launch
                        }

                        // 2. If the prayer window has already expired, transition to Qaza and update notification to Qaza alert
                        if ((windowEndEpochMs > 0L && nowMs >= windowEndEpochMs) || triggerType == AlarmTriggerType.STAGE_QAZA_ROLLOVER) {
                            scheduler.cancelAllAlarmsForPrayer(prayerId)
                            PrayerNaggingForegroundService.stopNaggingLoop(context)
                            if (windowEndEpochMs > firstLaunchEpochMs) {
                                dao.upsertPrayerRecord(
                                    PrayerRecordEntity(
                                        dateIso = todayIso,
                                        prayerId = prayerId,
                                        isPrayed = false,
                                        isInJamaah = false,
                                        completedAtMillis = 0L,
                                        completionStatus = PrayerCompletionStatus.MISSED_QADA.name,
                                        nagCountTriggered = currentRecord?.nagCountTriggered ?: 1
                                    )
                                )
                                PrayerNotificationHelper.showDynamicPrayerNotification(
                                    context = context,
                                    prayerName = prayerName,
                                    pstTimeFormatted = pstTime,
                                    adhanTimeFormatted = adhanTime,
                                    jamaahTimeFormatted = jamaahTime,
                                    triggerType = AlarmTriggerType.STAGE_QAZA_ROLLOVER,
                                    minutesUntilJamaah = 0,
                                    minutesPassedSinceJamaah = 0,
                                    minutesRemainingUntilEnd = 0,
                                    repeatIntervalMinutes = nagIntervalMin,
                                    preJamaahAlertMinutes = preJamaahMin,
                                    progressPercent = 100,
                                    prayerId = prayerId,
                                    dateIso = todayIso,
                                    nagIteration = 0
                                )
                            }
                            alarmDataStore.clearNaggingLoop(
                                reason = "$prayerName window ended • Auto-marked as Qaza",
                                stageLabel = "WINDOW_ENDED_QADA"
                            )
                            scheduler.syncAndArmAllDailyAlarms(postActiveWindowNotificationIfNeeded = false)
                            return@launch
                        }

                        val effectiveJamaahEpoch = if (jamaahEpochMs > 0L) jamaahEpochMs else (nowMs + 15 * 60_000L)
                        val effectiveAdhanEpoch = if (adhanEpochMs > 0L) adhanEpochMs else nowMs
                        val effectiveEndEpoch = if (windowEndEpochMs > 0L) windowEndEpochMs else (effectiveJamaahEpoch + 90 * 60_000L)

                        val preAdhanEpoch = effectiveAdhanEpoch - (preAdhanMin * 60_000L)
                        val preJamaahEpoch = effectiveJamaahEpoch - (preJamaahMin * 60_000L)
                        val firstPostJamaahEpoch = effectiveJamaahEpoch + (10 * 60_000L)

                        val minUntilJamaah = ((effectiveJamaahEpoch - nowMs + 59_999L) / 60_000L).toInt().coerceAtLeast(0)
                        val minPassedSinceJamaah = ((nowMs - effectiveJamaahEpoch) / 60_000L).toInt().coerceAtLeast(0)
                        val minRemainingUntilEnd = ((effectiveEndEpoch - nowMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)

                        fun findNextMilestoneAfter(referenceMs: Long): Pair<AlarmTriggerType, Long>? {
                            val milestones = buildList {
                                if (preAdhanMin > 0 && preAdhanEpoch > pstEpochMs) {
                                    add(AlarmTriggerType.STAGE_PRE_ADHAN to preAdhanEpoch)
                                }
                                if (effectiveAdhanEpoch > pstEpochMs) {
                                    add(AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN to effectiveAdhanEpoch)
                                }
                                if (preJamaahMin > 0 && preJamaahEpoch > effectiveAdhanEpoch) {
                                    add(AlarmTriggerType.STAGE_PRE_JAMAAH to preJamaahEpoch)
                                }
                                if (effectiveJamaahEpoch > effectiveAdhanEpoch) {
                                    add(AlarmTriggerType.STAGE_JAMAAH_START to effectiveJamaahEpoch)
                                }
                                add(AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG to firstPostJamaahEpoch)
                                val urgentEpoch = effectiveEndEpoch - (urgentThresholdMin * 60_000L)
                                if (urgentEpoch > effectiveJamaahEpoch) {
                                    add(AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW to urgentEpoch)
                                }
                                add(AlarmTriggerType.STAGE_QAZA_ROLLOVER to effectiveEndEpoch)
                            }.sortedBy { it.second }

                            return milestones.firstOrNull { it.second > referenceMs + 20_000L && it.second <= effectiveEndEpoch }
                        }

                        when (triggerType) {
                            AlarmTriggerType.STAGE_1_PST_START,
                            AlarmTriggerType.STAGE_PRE_ADHAN,
                            AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN,
                            AlarmTriggerType.STAGE_PRE_JAMAAH,
                            AlarmTriggerType.STAGE_JAMAAH_START -> {
                                if (triggerType == AlarmTriggerType.STAGE_1_PST_START) {
                                    val previousPrayerId = getPreviousPrayerId(prayerId)
                                    if (previousPrayerId != null && (prefs?.autoQadaOnNextPrayer != false) && nowMs > firstLaunchEpochMs) {
                                        val prevRec = dao.getPrayerRecordSync(todayIso, previousPrayerId)
                                        if (prevRec != null && !prevRec.isPrayed) {
                                            dao.upsertPrayerRecord(
                                                prevRec.copy(
                                                    isInJamaah = false,
                                                    completionStatus = PrayerCompletionStatus.MISSED_QADA.name
                                                )
                                            )
                                            scheduler.cancelAllAlarmsForPrayer(previousPrayerId)
                                        }
                                    }
                                }

                                if (triggerType == AlarmTriggerType.STAGE_1_PST_START && prayerId == "FAJR") {
                                    // When Fajr starts on a new day in the background, ensure all 5 daily prayers + next day's Fajr are armed
                                    scheduler.syncAndArmAllDailyAlarms(postActiveWindowNotificationIfNeeded = false)
                                }

                                val nextMilestone = findNextMilestoneAfter(nowMs)
                                if (nextMilestone != null) {
                                    val (nextStage, nextEpoch) = nextMilestone
                                    val nextMinToJamaah = ((effectiveJamaahEpoch - nextEpoch + 59_999L) / 60_000L).toInt().coerceAtLeast(0)
                                    scheduler.scheduleExactTrigger(
                                        prayerId = prayerId,
                                        prayerName = prayerName,
                                        dateIso = todayIso,
                                        triggerType = nextStage,
                                        triggerAtEpochMillis = nextEpoch,
                                        pstTimeFormatted = pstTime,
                                        adhanTimeFormatted = adhanTime,
                                        jamaahTimeFormatted = jamaahTime,
                                        pstEpochMillis = pstEpochMs,
                                        adhanEpochMillis = effectiveAdhanEpoch,
                                        jamaahEpochMillis = effectiveJamaahEpoch,
                                        windowEndEpochMillis = effectiveEndEpoch,
                                        minutesUntilJamaah = nextMinToJamaah,
                                        nagIteration = if (nextStage == AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG) 1 else 0
                                    )
                                }

                                val displayStage = if (triggerType == AlarmTriggerType.STAGE_1_PST_START && effectiveAdhanEpoch <= nowMs + 30_000L) {
                                    AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN
                                } else {
                                    triggerType
                                }

                                val effectiveStartEpoch = if (pstEpochMs > 0L) pstEpochMs else effectiveAdhanEpoch
                                val totalWinMs = (effectiveEndEpoch - effectiveStartEpoch).coerceAtLeast(60_000L)
                                val elapsedWinMs = (nowMs - effectiveStartEpoch).coerceAtLeast(0L)
                                val actualWindowPct = ((elapsedWinMs * 100L) / totalWinMs).toInt().coerceIn(5, 100)

                                PrayerNotificationHelper.showDynamicPrayerNotification(
                                    context = context,
                                    prayerName = prayerName,
                                    pstTimeFormatted = pstTime,
                                    adhanTimeFormatted = adhanTime,
                                    jamaahTimeFormatted = jamaahTime,
                                    triggerType = displayStage,
                                    minutesUntilJamaah = minUntilJamaah,
                                    minutesPassedSinceJamaah = 0,
                                    minutesRemainingUntilEnd = minRemainingUntilEnd,
                                    repeatIntervalMinutes = nagIntervalMin,
                                    preJamaahAlertMinutes = preJamaahMin,
                                    progressPercent = actualWindowPct,
                                    prayerId = prayerId,
                                    dateIso = todayIso,
                                    nagIteration = 0,
                                    pstEpochMillis = pstEpochMs,
                                    adhanEpochMillis = effectiveAdhanEpoch,
                                    jamaahEpochMillis = effectiveJamaahEpoch,
                                    windowEndEpochMillis = effectiveEndEpoch
                                )

                                alarmDataStore.recordTriggerActivated(
                                    prayerId = prayerId,
                                    dateIso = todayIso,
                                    triggerStage = displayStage.name,
                                    nagIteration = 0,
                                    nextAlarmEpochMs = nextMilestone?.second ?: 0L,
                                    foregroundActive = false,
                                    eventDescription = "$prayerName (${displayStage.name}) • ${minUntilJamaah}m to Jama'ah"
                                )
                            }

                            AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG,
                            AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW -> {
                                val currentIter = nagIteration.coerceAtLeast(1)
                                val isUrgentWindow = minRemainingUntilEnd <= urgentThresholdMin
                                val effectiveStage = if (isUrgentWindow) {
                                    AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW
                                } else {
                                    AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG
                                }

                                dao.upsertPrayerRecord(
                                    PrayerRecordEntity(
                                        dateIso = todayIso,
                                        prayerId = prayerId,
                                        isPrayed = false,
                                        isInJamaah = false,
                                        completedAtMillis = 0L,
                                        completionStatus = PrayerCompletionStatus.PENDING.name,
                                        nagCountTriggered = currentIter
                                    )
                                )

                                val stepMinutes = if (isUrgentWindow) {
                                    nagIntervalMin.coerceAtMost(10)
                                } else {
                                    nagIntervalMin
                                }
                                val urgentBoundaryEpoch = effectiveEndEpoch - (urgentThresholdMin * 60_000L)
                                val candidateNextEpoch = nowMs + (stepMinutes * 60_000L)
                                val nextNagEpoch = if (!isUrgentWindow && urgentBoundaryEpoch in (nowMs + 60_000L) until candidateNextEpoch) {
                                    urgentBoundaryEpoch
                                } else {
                                    candidateNextEpoch
                                }

                                val nextStageToSchedule = if (nextNagEpoch >= urgentBoundaryEpoch) {
                                    AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW
                                } else {
                                    AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG
                                }

                                if (nextNagEpoch < effectiveEndEpoch) {
                                    scheduler.scheduleExactTrigger(
                                        prayerId = prayerId,
                                        prayerName = prayerName,
                                        dateIso = todayIso,
                                        triggerType = nextStageToSchedule,
                                        triggerAtEpochMillis = nextNagEpoch,
                                        pstTimeFormatted = pstTime,
                                        adhanTimeFormatted = adhanTime,
                                        jamaahTimeFormatted = jamaahTime,
                                        pstEpochMillis = pstEpochMs,
                                        adhanEpochMillis = effectiveAdhanEpoch,
                                        jamaahEpochMillis = effectiveJamaahEpoch,
                                        windowEndEpochMillis = effectiveEndEpoch,
                                        minutesUntilJamaah = 0,
                                        nagIteration = currentIter + 1
                                    )
                                } else {
                                    // Schedule exact Qaza rollover at the end of the prayer window
                                    scheduler.scheduleExactTrigger(
                                        prayerId = prayerId,
                                        prayerName = prayerName,
                                        dateIso = todayIso,
                                        triggerType = AlarmTriggerType.STAGE_QAZA_ROLLOVER,
                                        triggerAtEpochMillis = effectiveEndEpoch,
                                        pstTimeFormatted = pstTime,
                                        adhanTimeFormatted = adhanTime,
                                        jamaahTimeFormatted = jamaahTime,
                                        pstEpochMillis = pstEpochMs,
                                        adhanEpochMillis = effectiveAdhanEpoch,
                                        jamaahEpochMillis = effectiveJamaahEpoch,
                                        windowEndEpochMillis = effectiveEndEpoch,
                                        minutesUntilJamaah = 0,
                                        nagIteration = 0
                                    )
                                }

                                PrayerNaggingForegroundService.startOrUpdateNaggingLoop(
                                    context = context,
                                    prayerId = prayerId,
                                    prayerName = prayerName,
                                    dateIso = todayIso,
                                    pstTime = pstTime,
                                    adhanTime = adhanTime,
                                    jamaahTime = jamaahTime,
                                    triggerType = effectiveStage,
                                    minutesUntilJamaah = 0,
                                    minutesPassedSinceJamaah = minPassedSinceJamaah.coerceAtLeast(1),
                                    minutesRemainingUntilEnd = minRemainingUntilEnd,
                                    repeatIntervalMinutes = stepMinutes,
                                    nagIteration = currentIter,
                                    pstEpochMillis = pstEpochMs,
                                    adhanEpochMillis = effectiveAdhanEpoch,
                                    jamaahEpochMillis = effectiveJamaahEpoch,
                                    windowEndEpochMillis = effectiveEndEpoch
                                )

                                val stageDesc = if (isUrgentWindow) {
                                    "URGENT ($prayerName): ${minRemainingUntilEnd}m remaining until prayer time ends!"
                                } else {
                                    val elapsedSec = ((nowMs - effectiveJamaahEpoch) / 1000L).toInt().coerceAtLeast(60)
                                    val elapsedStr = PrayerNotificationHelper.formatElapsedSecondsOrHours(elapsedSec)
                                    "$prayerName unprayed: $elapsedStr elapsed since Jama'ah • Repeating every ${nagIntervalMin}m"
                                }

                                alarmDataStore.recordTriggerActivated(
                                    prayerId = prayerId,
                                    dateIso = todayIso,
                                    triggerStage = effectiveStage.name,
                                    nagIteration = currentIter,
                                    nextAlarmEpochMs = if (nextNagEpoch < effectiveEndEpoch) nextNagEpoch else 0L,
                                    foregroundActive = true,
                                    eventDescription = stageDesc
                                )
                            }

                            AlarmTriggerType.STAGE_QAZA_ROLLOVER -> Unit
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun getPreviousPrayerId(currentPrayerId: String): String? {
        val order = listOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA")
        val idx = order.indexOf(currentPrayerId)
        return if (idx > 0) order[idx - 1] else null
    }
}
