package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.PrayerAlarmDataStore
import com.example.data.PrayerRepository
import com.example.domain.CityLocation
import com.example.domain.PrayerTimeCalculator
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
 * -STAGE_QAZA_ROLLOVER: Fired at strict end of prayer window if still unprayed, and chains next day's schedule
 */
class ExactPrayerAlarmScheduler(private val context: Context) {

    val appContext: Context = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { alarmManager.canScheduleExactAlarms() }.getOrDefault(true)
        } else {
            true
        }
    }

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
        nagIteration: Int = 0,
        requestCodeOverride: Int? = null
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

        val requestCode = requestCodeOverride ?: ((prayerId.hashCode() * 31) + triggerType.ordinal)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPrimaryAlarmClockMilestone = triggerType == AlarmTriggerType.STAGE_1_PST_START ||
            triggerType == AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN ||
            triggerType == AlarmTriggerType.STAGE_JAMAAH_START ||
            triggerType == AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtEpochMillis,
                    pendingIntent
                )
            } else if (isPrimaryAlarmClockMilestone) {
                // setAlarmClock() is exempt from Android Doze & Samsung One UI Deep Sleep throttling
                val showAppIntent = Intent(appContext, com.example.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val showAppPi = PendingIntent.getActivity(
                    appContext,
                    requestCode + 5000,
                    showAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtEpochMillis, showAppPi),
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

    /**
     * Standalone background-safe alarm synchronizer that reads Room + DataStore directly,
     * arms all remaining prayer milestones for the current sheet, arms tomorrow's Fajr rollover
     * so notifications continue indefinitely across days even if the user never opens the app,
     * and optionally posts the active prayer's live notification immediately on launch/permission grant.
     */
    suspend fun syncAndArmAllDailyAlarms(
        cityOverride: CityLocation? = null,
        postActiveWindowNotificationIfNeeded: Boolean = false
    ) {
        val db = AppDatabase.getInstance(appContext)
        val dao = db.prayerDao()
        val repository = PrayerRepository(dao)
        val alarmDataStore = PrayerAlarmDataStore(appContext)

        val city = cityOverride
            ?: alarmDataStore.getSavedCitySnapshot()?.first
            ?: repository.getSavedSelectedCity()?.first
            ?: PrayerTimeCalculator.detectBestDefaultCityForDeviceTimeZone()

        val runtimeState = alarmDataStore.runtimeStateFlow.first()
        val calcConfig = runtimeState.toCalculationConfig()
        val cityTz = PrayerTimeCalculator.getTimeZoneForCity(city)
        val nowCal = Calendar.getInstance(cityTz)
        val nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
        val initialCalc = PrayerTimeCalculator.calculateDailyPrayers(city, nowCal, calcConfig)

        val activeSheetCal = (nowCal.clone() as Calendar)
        if (nowMinutes < initialCalc.fajrMinutes) {
            // Between civil 00:00 and today's Fajr: check yesterday's night Islamic Midnight (Maghrib[D-1] to Fajr[D])
            val yesterdayCal = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterdayCalc = PrayerTimeCalculator.calculateDailyPrayers(city, yesterdayCal, calcConfig)
            if ((nowMinutes + 1440) < yesterdayCalc.islamicMidnightMinutes) {
                activeSheetCal.add(Calendar.DAY_OF_YEAR, -1)
            }
        } else if (initialCalc.islamicMidnightMinutes < 1440 && nowMinutes >= initialCalc.islamicMidnightMinutes) {
            activeSheetCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = cityTz }
        val sheetIso = dateFormatter.format(activeSheetCal.time)

        val sheetStartCal = (activeSheetCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val sheetStartEpochMs = sheetStartCal.timeInMillis
        val isFriday = activeSheetCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

        val calc = PrayerTimeCalculator.calculateDailyPrayers(city, activeSheetCal, calcConfig)
        val settingsMap = dao.getAllPrayerSettingsSync().associateBy { it.prayerId }
        val todayRecordsMap = repository.allPrayerRecords.first()
            .filter { it.dateIso == sheetIso }
            .associateBy { it.prayerId }
        val prefs = dao.getUserPreferencesSync()
        val preAdhanMin = (prefs?.preAdhanAlertMinutes ?: 5).coerceIn(0, 30)
        val preJamaahMin = (prefs?.preJamaahAlertMinutes ?: 15).coerceIn(5, 45)
        val nagIntervalMin = (prefs?.nagIntervalMinutes ?: 10).coerceIn(5, 60)
        val urgentThresholdMin = (prefs?.urgentEndWindowMinutes ?: 15).coerceIn(5, 60)

        val middayName = if (isFriday) "Jumu'ah" else "Dhuhr"
        val prayerDefs = listOf(
            Triple("FAJR", "Fajr", calc.fajrMinutes) to calc.sunriseMinutes,
            Triple("DHUHR", middayName, calc.dhuhrMinutes) to calc.asrMinutes,
            Triple("ASR", "Asr", calc.asrMinutes) to calc.maghribMinutes,
            Triple("MAGHRIB", "Maghrib", calc.maghribMinutes) to calc.ishaMinutes,
            Triple("ISHA", "Isha", calc.ishaMinutes) to calc.islamicMidnightMinutes
        )

        val nowMs = System.currentTimeMillis()
        val dismissedKey = runtimeState.dismissedPrayerKey
        var nextArmedSummary: String? = null
        var nextArmedEpochMs = 0L

        cancelAllDailyPrayerAlarms()

        for ((meta, strictEndMin) in prayerDefs) {
            val id = meta.first
            val name = meta.second
            val pstMin = meta.third
            val settingsKey = if (id == "DHUHR" && isFriday) "JUMUAH" else id
            val setting = settingsMap[settingsKey]
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

            val isPrayed = todayRecordsMap[id]?.isPrayed == true
            val pstEpochMs = sheetStartEpochMs + (pstMin * 60_000L)
            val adhanEpochMs = sheetStartEpochMs + (adhanMin * 60_000L)
            val preAdhanEpochMs = adhanEpochMs - (preAdhanMin * 60_000L)
            val jamaahEpochMs = sheetStartEpochMs + (jamaahMin * 60_000L)
            val preJamaahEpochMs = jamaahEpochMs - (preJamaahMin * 60_000L)
            val firstPostJamaahNagEpochMs = jamaahEpochMs + (10 * 60_000L)
            val windowEndEpochMs = sheetStartEpochMs + (strictEndMin * 60_000L)

            if (isPrayed || nowMs >= windowEndEpochMs) {
                continue
            }

            val isDismissedForWindow = dismissedKey == "${sheetIso}_$id"

            val pstStr = PrayerTimeCalculator.formatMinutesTo12Hour(pstMin)
            val adhanStr = PrayerTimeCalculator.formatMinutesTo12Hour(adhanMin)
            val jamaahStr = PrayerTimeCalculator.formatMinutesTo12Hour(jamaahMin)
            val urgentBeforeEndEpochMs = windowEndEpochMs - (urgentThresholdMin * 60_000L)

            if (isDismissedForWindow) {
                // Respect user's explicit Dismiss: do not re-arm reminders or post active alerts for this prayer,
                // only arm strict Qaza rollover at the end of the prayer window.
                if (windowEndEpochMs > nowMs) {
                    scheduleExactTrigger(
                        prayerId = id,
                        prayerName = name,
                        dateIso = sheetIso,
                        triggerType = AlarmTriggerType.STAGE_QAZA_ROLLOVER,
                        triggerAtEpochMillis = windowEndEpochMs,
                        pstTimeFormatted = pstStr,
                        adhanTimeFormatted = adhanStr,
                        jamaahTimeFormatted = jamaahStr,
                        pstEpochMillis = pstEpochMs,
                        adhanEpochMillis = adhanEpochMs,
                        jamaahEpochMillis = jamaahEpochMs,
                        windowEndEpochMillis = windowEndEpochMs,
                        minutesUntilJamaah = 0,
                        nagIteration = 0
                    )
                }
                continue
            }

            // If this prayer's window is currently active right now and the caller requested an immediate
            // notification sync (e.g., on app launch or right after granting POST_NOTIFICATIONS), post it now
            if (postActiveWindowNotificationIfNeeded && nowMs in pstEpochMs until windowEndEpochMs) {
                val minUntilJamaahNow = ((jamaahEpochMs - nowMs + 59_999L) / 60_000L).toInt().coerceAtLeast(0)
                val minPassedSinceJamaahNow = ((nowMs - jamaahEpochMs) / 60_000L).toInt().coerceAtLeast(0)
                val minRemainingNow = ((windowEndEpochMs - nowMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
                val totalWinMs = (windowEndEpochMs - pstEpochMs).coerceAtLeast(60_000L)
                val elapsedWinMs = (nowMs - pstEpochMs).coerceAtLeast(0L)
                val pctNow = ((elapsedWinMs * 100L) / totalWinMs).toInt().coerceIn(5, 100)

                val currentActiveStage = when {
                    minRemainingNow <= urgentThresholdMin && nowMs >= jamaahEpochMs ->
                        AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW
                    nowMs >= firstPostJamaahNagEpochMs ->
                        AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG
                    nowMs >= jamaahEpochMs ->
                        AlarmTriggerType.STAGE_JAMAAH_START
                    nowMs >= preJamaahEpochMs && preJamaahEpochMs > adhanEpochMs ->
                        AlarmTriggerType.STAGE_PRE_JAMAAH
                    nowMs >= adhanEpochMs ->
                        AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN
                    nowMs >= preAdhanEpochMs && preAdhanEpochMs > pstEpochMs ->
                        AlarmTriggerType.STAGE_PRE_ADHAN
                    else ->
                        AlarmTriggerType.STAGE_1_PST_START
                }

                PrayerNotificationHelper.showDynamicPrayerNotification(
                    context = appContext,
                    prayerName = name,
                    pstTimeFormatted = pstStr,
                    adhanTimeFormatted = adhanStr,
                    jamaahTimeFormatted = jamaahStr,
                    triggerType = currentActiveStage,
                    minutesUntilJamaah = minUntilJamaahNow,
                    minutesPassedSinceJamaah = minPassedSinceJamaahNow,
                    minutesRemainingUntilEnd = minRemainingNow,
                    repeatIntervalMinutes = nagIntervalMin,
                    preJamaahAlertMinutes = preJamaahMin,
                    progressPercent = pctNow,
                    prayerId = id,
                    dateIso = sheetIso,
                    nagIteration = if (nowMs >= firstPostJamaahNagEpochMs) 1 else 0,
                    pstEpochMillis = pstEpochMs,
                    adhanEpochMillis = adhanEpochMs,
                    jamaahEpochMillis = jamaahEpochMs,
                    windowEndEpochMillis = windowEndEpochMs
                )
            }

            val milestones = buildList {
                add(AlarmTriggerType.STAGE_1_PST_START to pstEpochMs)
                if (preAdhanMin > 0 && preAdhanEpochMs > pstEpochMs) {
                    add(AlarmTriggerType.STAGE_PRE_ADHAN to preAdhanEpochMs)
                }
                if (adhanEpochMs > pstEpochMs) {
                    add(AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN to adhanEpochMs)
                }
                if (preJamaahMin > 0 && preJamaahEpochMs > adhanEpochMs) {
                    add(AlarmTriggerType.STAGE_PRE_JAMAAH to preJamaahEpochMs)
                }
                if (jamaahEpochMs > adhanEpochMs) {
                    add(AlarmTriggerType.STAGE_JAMAAH_START to jamaahEpochMs)
                }
                add(AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG to firstPostJamaahNagEpochMs)
                if (urgentBeforeEndEpochMs > jamaahEpochMs) {
                    add(AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW to urgentBeforeEndEpochMs)
                }
                add(AlarmTriggerType.STAGE_QAZA_ROLLOVER to windowEndEpochMs)
            }.sortedBy { it.second }

            val upcomingMilestone = milestones.firstOrNull { it.second > nowMs && it.second <= windowEndEpochMs }

            val stageToSchedule: AlarmTriggerType
            val triggerEpochMs: Long

            if (upcomingMilestone != null && (nowMs < firstPostJamaahNagEpochMs || upcomingMilestone.first == AlarmTriggerType.STAGE_QAZA_ROLLOVER)) {
                if (nowMs >= firstPostJamaahNagEpochMs) {
                    val elapsedSinceFirstNagMs = (nowMs - firstPostJamaahNagEpochMs).coerceAtLeast(0L)
                    val intervalMs = nagIntervalMin * 60_000L
                    val nextGridTickMs = firstPostJamaahNagEpochMs + (((elapsedSinceFirstNagMs / intervalMs) + 1) * intervalMs)
                    if (nextGridTickMs < windowEndEpochMs - 20_000L) {
                        val remAtNextTickMin = ((windowEndEpochMs - nextGridTickMs + 59_999L) / 60_000L).toInt()
                        stageToSchedule = if (remAtNextTickMin <= urgentThresholdMin) {
                            AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW
                        } else {
                            AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG
                        }
                        triggerEpochMs = nextGridTickMs
                    } else {
                        stageToSchedule = AlarmTriggerType.STAGE_QAZA_ROLLOVER
                        triggerEpochMs = windowEndEpochMs
                    }
                } else {
                    stageToSchedule = upcomingMilestone.first
                    triggerEpochMs = upcomingMilestone.second
                }
            } else {
                val elapsedSinceFirstNagMs = (nowMs - firstPostJamaahNagEpochMs).coerceAtLeast(0L)
                val intervalMs = nagIntervalMin * 60_000L
                val nextGridTickMs = firstPostJamaahNagEpochMs + (((elapsedSinceFirstNagMs / intervalMs) + 1) * intervalMs)
                if (nextGridTickMs < windowEndEpochMs - 20_000L) {
                    val remAtNextTickMin = ((windowEndEpochMs - nextGridTickMs + 59_999L) / 60_000L).toInt()
                    stageToSchedule = if (remAtNextTickMin <= urgentThresholdMin) {
                        AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW
                    } else {
                        AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG
                    }
                    triggerEpochMs = nextGridTickMs
                } else {
                    stageToSchedule = AlarmTriggerType.STAGE_QAZA_ROLLOVER
                    triggerEpochMs = windowEndEpochMs
                }
            }

            val minUntilJamaahAtTrigger = ((jamaahEpochMs - triggerEpochMs + 59_999L) / 60_000L).toInt().coerceAtLeast(0)

            if (triggerEpochMs > nowMs && triggerEpochMs <= windowEndEpochMs) {
                scheduleExactTrigger(
                    prayerId = id,
                    prayerName = name,
                    dateIso = sheetIso,
                    triggerType = stageToSchedule,
                    triggerAtEpochMillis = triggerEpochMs,
                    pstTimeFormatted = pstStr,
                    adhanTimeFormatted = adhanStr,
                    jamaahTimeFormatted = jamaahStr,
                    pstEpochMillis = pstEpochMs,
                    adhanEpochMillis = adhanEpochMs,
                    jamaahEpochMillis = jamaahEpochMs,
                    windowEndEpochMillis = windowEndEpochMs,
                    minutesUntilJamaah = minUntilJamaahAtTrigger,
                    nagIteration = if (stageToSchedule == AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG ||
                        stageToSchedule == AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW
                    ) 1 else 0
                )

                if (nextArmedSummary == null) {
                    nextArmedEpochMs = triggerEpochMs
                    nextArmedSummary = "$name • PST $pstStr • Adhan $adhanStr • Jama'ah $jamaahStr"
                }
            }
        }

        // Always arm tomorrow's Fajr STAGE_1_PST_START as a guaranteed next-day chain so notifications
        // continue indefinitely across days even if the user never opens the app
        val tomorrowSheetCal = (activeSheetCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowIso = dateFormatter.format(tomorrowSheetCal.time)
        val tomorrowStartCal = (tomorrowSheetCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tomorrowCalc = PrayerTimeCalculator.calculateDailyPrayers(city, tomorrowSheetCal, calcConfig)
        val fajrSetting = settingsMap["FAJR"]
        val tomorrowFajrPstMin = tomorrowCalc.fajrMinutes
        val tomorrowFajrAdhanMin = if (fajrSetting?.useFixedAdhanTime ?: true) {
            (fajrSetting?.fixedAdhanTimeMinutes ?: (5 * 60)).coerceIn(0, 1439).coerceAtLeast(tomorrowFajrPstMin)
        } else {
            (tomorrowFajrPstMin + (fajrSetting?.adhanOffsetMinutes ?: 0)).coerceAtLeast(tomorrowFajrPstMin)
        }
        val tomorrowFajrJamaahMin = if (fajrSetting?.useFixedTime == true) {
            fajrSetting.fixedTimeMinutes.coerceAtLeast(tomorrowFajrAdhanMin)
        } else {
            (tomorrowFajrAdhanMin + (fajrSetting?.jamaahOffsetMinutes ?: 20)).coerceAtLeast(tomorrowFajrAdhanMin)
        }
        val tomorrowFajrPstEpoch = tomorrowStartCal.timeInMillis + (tomorrowFajrPstMin * 60_000L)
        val tomorrowFajrAdhanEpoch = tomorrowStartCal.timeInMillis + (tomorrowFajrAdhanMin * 60_000L)
        val tomorrowFajrJamaahEpoch = tomorrowStartCal.timeInMillis + (tomorrowFajrJamaahMin * 60_000L)
        val tomorrowFajrEndEpoch = tomorrowStartCal.timeInMillis + (tomorrowCalc.sunriseMinutes * 60_000L)

        if (tomorrowFajrPstEpoch > nowMs) {
            val tomorrowPstStr = PrayerTimeCalculator.formatMinutesTo12Hour(tomorrowFajrPstMin)
            val tomorrowAdhanStr = PrayerTimeCalculator.formatMinutesTo12Hour(tomorrowFajrAdhanMin)
            val tomorrowJamaahStr = PrayerTimeCalculator.formatMinutesTo12Hour(tomorrowFajrJamaahMin)
            scheduleExactTrigger(
                prayerId = "FAJR",
                prayerName = "Fajr",
                dateIso = tomorrowIso,
                triggerType = AlarmTriggerType.STAGE_1_PST_START,
                triggerAtEpochMillis = tomorrowFajrPstEpoch,
                pstTimeFormatted = tomorrowPstStr,
                adhanTimeFormatted = tomorrowAdhanStr,
                jamaahTimeFormatted = tomorrowJamaahStr,
                pstEpochMillis = tomorrowFajrPstEpoch,
                adhanEpochMillis = tomorrowFajrAdhanEpoch,
                jamaahEpochMillis = tomorrowFajrJamaahEpoch,
                windowEndEpochMillis = tomorrowFajrEndEpoch,
                minutesUntilJamaah = (tomorrowFajrJamaahMin - tomorrowFajrPstMin).coerceAtLeast(0),
                nagIteration = 0,
                requestCodeOverride = REQUEST_CODE_TOMORROW_FAJR_ROLLOVER
            )
            if (nextArmedSummary == null) {
                nextArmedEpochMs = tomorrowFajrPstEpoch
                nextArmedSummary = "Tomorrow Fajr • PST $tomorrowPstStr • Adhan $tomorrowAdhanStr • Jama'ah $tomorrowJamaahStr"
            }
        }

        if (nextArmedSummary != null) {
            alarmDataStore.recordTriggerActivated(
                prayerId = "",
                dateIso = sheetIso,
                triggerStage = "ARMED",
                nagIteration = 0,
                nextAlarmEpochMs = nextArmedEpochMs,
                foregroundActive = false,
                eventDescription = nextArmedSummary
            )
        }
    }

    companion object {
        const val ACTION_PRAYER_ALARM_TRIGGER = "com.example.action.PRAYER_ALARM_TRIGGER"
        const val ACTION_MARK_PRAYED = "com.example.action.MARK_PRAYED"
        const val ACTION_SNOOZE_5M = "com.example.action.SNOOZE_5M"
        const val ACTION_DISMISS_NAG = "com.example.action.DISMISS_NAG"

        const val REQUEST_CODE_TOMORROW_FAJR_ROLLOVER = 990_101

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

