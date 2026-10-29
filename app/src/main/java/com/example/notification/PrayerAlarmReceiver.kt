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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Core BroadcastReceiver handling the 3-stage Dynamic Notification Lifecycle:
 * - Trigger 1 (Adhan Start): Posts rich notification, auto-marks any unprayed previous prayer
 *   as MISSED_QADA, and schedules Trigger 2 (15m before Jama'ah).
 * - Trigger 2 (15m before Jama'ah): Updates active countdown badge and arms the 20m Nagging Loop.
 * - Trigger 3 (Nagging Loop every 20m): Checks Room database; if still not marked as Prayed,
 *   starts/updates PrayerNaggingForegroundService and schedules the next +20m exact alarm.
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
                val adhanTime = intent.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_ADHAN_TIME) ?: "03:45 PM"
                val jamaahTime = intent.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_TIME) ?: "04:00 PM"

                when (action) {
                    ExactPrayerAlarmScheduler.ACTION_MARK_PRAYED -> {
                        val existing = dao.getPrayerRecordSync(todayIso, prayerId)
                        dao.upsertPrayerRecord(
                            PrayerRecordEntity(
                                dateIso = todayIso,
                                prayerId = prayerId,
                                isPrayed = true,
                                isInJamaah = true,
                                completedAtMillis = System.currentTimeMillis(),
                                completionStatus = PrayerCompletionStatus.PRAYED_ON_TIME.name,
                                nagCountTriggered = existing?.nagCountTriggered ?: 0
                            )
                        )
                        scheduler.cancelAllAlarmsForPrayer(prayerId)
                        PrayerNaggingForegroundService.stopNaggingLoop(context)
                        alarmDataStore.clearNaggingLoop(
                            reason = "$prayerName marked as Prayed via Notification Banner ✓",
                            stageLabel = "PRAYED_ON_TIME"
                        )
                    }

                    ExactPrayerAlarmScheduler.ACTION_SNOOZE_5M -> {
                        val nextEpoch = System.currentTimeMillis() + (5 * 60 * 1000L)
                        scheduler.scheduleExactTrigger(
                            prayerId = prayerId,
                            prayerName = prayerName,
                            dateIso = todayIso,
                            triggerType = AlarmTriggerType.TRIGGER_3_NAGGING_LOOP_20M,
                            triggerAtEpochMillis = nextEpoch,
                            adhanTimeFormatted = adhanTime,
                            jamaahTimeFormatted = jamaahTime,
                            minutesUntilJamaah = 5,
                            nagIteration = 1
                        )
                        alarmDataStore.recordSnoozeFiveMinutes(
                            prayerId = prayerId,
                            nextAlarmEpochMs = nextEpoch
                        )
                    }

                    ExactPrayerAlarmScheduler.ACTION_DISMISS_NAG -> {
                        scheduler.cancelAllAlarmsForPrayer(prayerId)
                        PrayerNaggingForegroundService.stopNaggingLoop(context)
                        alarmDataStore.clearNaggingLoop(
                            reason = "$prayerName nagging loop dismissed by user",
                            stageLabel = "DISMISSED"
                        )
                    }

                    ExactPrayerAlarmScheduler.ACTION_PRAYER_ALARM_TRIGGER -> {
                        val triggerName = intent.getStringExtra(ExactPrayerAlarmScheduler.EXTRA_TRIGGER_TYPE)
                            ?: AlarmTriggerType.TRIGGER_1_ADHAN_START.name
                        val triggerType = runCatching { AlarmTriggerType.valueOf(triggerName) }
                            .getOrDefault(AlarmTriggerType.TRIGGER_1_ADHAN_START)
                        val nagIteration = intent.getIntExtra(ExactPrayerAlarmScheduler.EXTRA_NAG_ITERATION, 0)
                        val minUntilJamaah = intent.getIntExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_UNTIL_JAMAAH, 15)

                        val prefs = dao.getUserPreferencesSync()
                        val nagIntervalMin = prefs?.nagIntervalMinutes ?: 20
                        val preJamaahMin = prefs?.preJamaahAlertMinutes ?: 15

                        // Check if prayer is already marked as Prayed in Room
                        val currentRecord = dao.getPrayerRecordSync(todayIso, prayerId)
                        if (currentRecord?.isPrayed == true) {
                            scheduler.cancelAllAlarmsForPrayer(prayerId)
                            PrayerNaggingForegroundService.stopNaggingLoop(context)
                            alarmDataStore.clearNaggingLoop(
                                reason = "$prayerName already completed; loop terminated",
                                stageLabel = "PRAYED_ON_TIME"
                            )
                            return@launch
                        }

                        when (triggerType) {
                            AlarmTriggerType.TRIGGER_1_ADHAN_START -> {
                                // Confirmed Decision: Auto-mark previous unprayed prayer as MISSED_QADA when next prayer starts
                                val previousPrayerId = getPreviousPrayerId(prayerId)
                                if (previousPrayerId != null && (prefs?.autoQadaOnNextPrayer != false)) {
                                    val prevRec = dao.getPrayerRecordSync(todayIso, previousPrayerId)
                                    if (prevRec == null || !prevRec.isPrayed) {
                                        dao.upsertPrayerRecord(
                                            PrayerRecordEntity(
                                                dateIso = todayIso,
                                                prayerId = previousPrayerId,
                                                isPrayed = false,
                                                isInJamaah = false,
                                                completedAtMillis = 0L,
                                                completionStatus = PrayerCompletionStatus.MISSED_QADA.name,
                                                nagCountTriggered = prevRec?.nagCountTriggered ?: 1
                                            )
                                        )
                                        scheduler.cancelAllAlarmsForPrayer(previousPrayerId)
                                    }
                                }

                                // Schedule Trigger 2 (X = 15m before Jama'ah)
                                val nextTriggerEpoch = System.currentTimeMillis() + (preJamaahMin * 60 * 1000L)
                                scheduler.scheduleExactTrigger(
                                    prayerId = prayerId,
                                    prayerName = prayerName,
                                    dateIso = todayIso,
                                    triggerType = AlarmTriggerType.TRIGGER_2_PRE_JAMAAH_15M,
                                    triggerAtEpochMillis = nextTriggerEpoch,
                                    adhanTimeFormatted = adhanTime,
                                    jamaahTimeFormatted = jamaahTime,
                                    minutesUntilJamaah = preJamaahMin,
                                    nagIteration = 0
                                )

                                val notification = PrayerNotificationHelper.buildRichPrayerNotification(
                                    context = context,
                                    prayerId = prayerId,
                                    prayerName = prayerName,
                                    dateIso = todayIso,
                                    adhanTimeFormatted = adhanTime,
                                    jamaahTimeFormatted = jamaahTime,
                                    minutesUntilJamaah = minUntilJamaah,
                                    progressPercent = 45,
                                    nagIteration = 0,
                                    isOngoingForeground = false
                                )
                                PrayerNotificationHelper.postNotificationIfPermitted(context, notification)

                                alarmDataStore.recordTriggerActivated(
                                    prayerId = prayerId,
                                    dateIso = todayIso,
                                    triggerStage = "TRIGGER_1_ADHAN",
                                    nagIteration = 0,
                                    nextAlarmEpochMs = nextTriggerEpoch,
                                    foregroundActive = false,
                                    eventDescription = "Trigger 1 fired for $prayerName Adhan • Armed Trigger 2 (-${preJamaahMin}m Jama'ah)"
                                )
                            }

                            AlarmTriggerType.TRIGGER_2_PRE_JAMAAH_15M -> {
                                // Schedule first Nagging Loop alarm at Y = 20 minutes
                                val firstNagEpoch = System.currentTimeMillis() + (nagIntervalMin * 60 * 1000L)
                                scheduler.scheduleExactTrigger(
                                    prayerId = prayerId,
                                    prayerName = prayerName,
                                    dateIso = todayIso,
                                    triggerType = AlarmTriggerType.TRIGGER_3_NAGGING_LOOP_20M,
                                    triggerAtEpochMillis = firstNagEpoch,
                                    adhanTimeFormatted = adhanTime,
                                    jamaahTimeFormatted = jamaahTime,
                                    minutesUntilJamaah = 0,
                                    nagIteration = 1
                                )

                                val notification = PrayerNotificationHelper.buildRichPrayerNotification(
                                    context = context,
                                    prayerId = prayerId,
                                    prayerName = prayerName,
                                    dateIso = todayIso,
                                    adhanTimeFormatted = adhanTime,
                                    jamaahTimeFormatted = jamaahTime,
                                    minutesUntilJamaah = preJamaahMin,
                                    progressPercent = 82,
                                    nagIteration = 0,
                                    isOngoingForeground = true
                                )
                                PrayerNotificationHelper.postNotificationIfPermitted(context, notification)

                                alarmDataStore.recordTriggerActivated(
                                    prayerId = prayerId,
                                    dateIso = todayIso,
                                    triggerStage = "TRIGGER_2_PRE_JAMAAH_${preJamaahMin}M",
                                    nagIteration = 0,
                                    nextAlarmEpochMs = firstNagEpoch,
                                    foregroundActive = true,
                                    eventDescription = "Trigger 2 fired: ${preJamaahMin}m until $prayerName Jama'ah • Nagging loop armed (+${nagIntervalMin}m)"
                                )
                            }

                            AlarmTriggerType.TRIGGER_3_NAGGING_LOOP_20M -> {
                                val nextNagIteration = nagIteration + 1
                                val nextNagEpoch = System.currentTimeMillis() + (nagIntervalMin * 60 * 1000L)

                                // Persist incremented nag count in Room
                                dao.upsertPrayerRecord(
                                    PrayerRecordEntity(
                                        dateIso = todayIso,
                                        prayerId = prayerId,
                                        isPrayed = false,
                                        isInJamaah = false,
                                        completedAtMillis = 0L,
                                        completionStatus = PrayerCompletionStatus.PENDING.name,
                                        nagCountTriggered = nagIteration.coerceAtLeast(1)
                                    )
                                )

                                // Re-arm exact alarm for +Y minutes (20m) via setExactAndAllowWhileIdle
                                scheduler.scheduleExactTrigger(
                                    prayerId = prayerId,
                                    prayerName = prayerName,
                                    dateIso = todayIso,
                                    triggerType = AlarmTriggerType.TRIGGER_3_NAGGING_LOOP_20M,
                                    triggerAtEpochMillis = nextNagEpoch,
                                    adhanTimeFormatted = adhanTime,
                                    jamaahTimeFormatted = jamaahTime,
                                    minutesUntilJamaah = 0,
                                    nagIteration = nextNagIteration
                                )

                                // Keep persistent Foreground Service active
                                PrayerNaggingForegroundService.startOrUpdateNaggingLoop(
                                    context = context,
                                    prayerId = prayerId,
                                    prayerName = prayerName,
                                    dateIso = todayIso,
                                    adhanTime = adhanTime,
                                    jamaahTime = jamaahTime,
                                    minutesUntilJamaah = 0,
                                    nagIteration = nagIteration.coerceAtLeast(1)
                                )

                                alarmDataStore.recordTriggerActivated(
                                    prayerId = prayerId,
                                    dateIso = todayIso,
                                    triggerStage = "TRIGGER_3_NAG_LOOP_#${nagIteration.coerceAtLeast(1)}",
                                    nagIteration = nagIteration.coerceAtLeast(1),
                                    nextAlarmEpochMs = nextNagEpoch,
                                    foregroundActive = true,
                                    eventDescription = "Nagging Loop #${nagIteration.coerceAtLeast(1)} active for $prayerName • Next repeat in ${nagIntervalMin}m"
                                )
                            }
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
