package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AlarmLoopRuntimeState
import com.example.data.AthkaarHabitUiModel
import com.example.data.AthkaarLogEntity
import com.example.data.DayContributionCell
import com.example.data.HistoricalDayPrayerUiModel
import com.example.data.LocalCityEntity
import com.example.data.PrayerAlarmDataStore
import com.example.data.PrayerCompletionStatus
import com.example.data.PrayerItemUiModel
import com.example.data.PrayerRecordEntity
import com.example.data.PrayerRepository
import com.example.data.PrayerSettingEntity
import com.example.data.SolarWarningItemUiModel
import com.example.data.UserPreferencesEntity
import com.example.domain.CityLocation
import com.example.domain.LocationResolutionManager
import com.example.domain.LocationResolutionResult
import com.example.domain.PrayerTimeCalculator
import com.example.notification.AlarmTriggerType
import com.example.notification.ExactPrayerAlarmScheduler
import com.example.notification.PrayerNaggingForegroundService
import com.example.notification.PrayerNotificationHelper
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.HeroCardStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class AppDisplayMode {
    SHOWCASE_3_SCREENS,
    INTERACTIVE_SINGLE_SCREEN
}

enum class ActiveTab {
    NOTIFICATION_LOCK,
    HOME_PRAYERS,
    ANALYTICS_HABITS,
    ARCHITECTURE_BLUEPRINT
}

/**
 * Configurable countdown target once a prayer/Jama'ah is completed (or when viewing the next prayer):
 * - NEXT_ADHAN: Shows time remaining until the next prayer's Adhan start time
 * - NEXT_JAMAAH: Shows time remaining until the next prayer's Jama'ah congregation time
 */
enum class NextPrayerCountdownMode {
    NEXT_ADHAN,
    NEXT_JAMAAH
}

data class PrayerDashboardUiState(
    val displayMode: AppDisplayMode = AppDisplayMode.INTERACTIVE_SINGLE_SCREEN,
    val activeTab: ActiveTab = ActiveTab.HOME_PRAYERS,
    val selectedCity: CityLocation = PrayerTimeCalculator.detectBestDefaultCityForDeviceTimeZone(),
    val availableCities: List<CityLocation> = PrayerTimeCalculator.PredefinedCities,
    val locationSearchQuery: String = "",
    val locationSearchResults: List<CityLocation> = PrayerTimeCalculator.PredefinedCities,
    val isSearchingLocations: Boolean = false,
    val sqliteFallbackCityCount: Int = PrayerTimeCalculator.PredefinedCities.size,
    val locationSourceStatus: String = "Makkah • MWL",
    val dayAndDateBadgeText: String = "Friday, 30 Oct",
    val hijriDateText: String = "",
    val gregorianDateText: String = "",
    val currentTimeFormatted: String = "03:36 PM",
    val currentClockSeconds: Int = 42,
    val sunriseTimeFormatted: String = "06:12 AM",
    val islamicMidnightFormatted: String = "11:28 PM",
    val prayers: List<PrayerItemUiModel> = emptyList(),
    val solarWarnings: List<SolarWarningItemUiModel> = emptyList(),
    val heroSolarBadgeText: String = "Shuruq 06:12 AM",
    val featuredPrayer: PrayerItemUiModel? = null,
    val isFeaturedPrayerActiveWindow: Boolean = true,
    val isJamaahTimePassed: Boolean = false,
    val isAllTodayPrayersCompleted: Boolean = false,
    val isIshaMissedAfterMidnight: Boolean = false,
    val isUrgentEndOfWindow: Boolean = false,
    val nextPrayerCountdownMode: NextPrayerCountdownMode = NextPrayerCountdownMode.NEXT_ADHAN,
    val heroHeaderStatusLabel: String = "CURRENT PRAYER",
    val jamaahStatusBadgeText: String = "24 min until Jama'ah",
    val heroJamaahPillText: String = "Jama'ah 04:00 PM",
    val heroLeftTimeLabel: String = "ADHAN TIME",
    val heroLeftTimeValue: String = "03:45 PM",
    val heroCountdownSubtitleText: String = "Next prayer in 24m 00s",
    val primaryRightTimerText: String = "Next prayer in 24m 00s",
    val progressLeftLabelText: String = "Time remaining until Asr",
    val progressRightLabelText: String = "35% elapsed",
    val lockScreenStageTimeText: String = "Starts at 03:39 PM",
    val elapsedSinceJamaahFormatted: String = "0m 00s",
    val prayerWindowEndTimeFormatted: String = "05:56 PM",
    val minutesElapsedSinceJamaah: Int = 0,
    val minutesRemainingUntilPrayerEnds: Int = 60,
    val minutesUntilNextPrayer: Int = 9,
    val minutesUntilJamaah: Int = 24,
    val snoozedExtraMinutes: Int = 0,
    val preAdhanAlertMinutes: Int = 5,
    val preJamaahAlertMinutes: Int = 15,
    val nagIntervalMinutes: Int = 10,
    val urgentEndWindowMinutes: Int = 15,
    val isSnoozeEnabled: Boolean = false,
    val notificationProgressFraction: Float = 0.35f,
    val adhanCheckpointFraction: Float = 0.30f,
    val jamaahCheckpointFraction: Float = 0.65f,
    val lockScreenBlinkItems: List<String> = emptyList(),
    val alarmRuntimeState: AlarmLoopRuntimeState = AlarmLoopRuntimeState(),
    val athkaarList: List<AthkaarHabitUiModel> = emptyList(),
    val weeklyGrid: List<DayContributionCell> = emptyList(),
    val todayPrayersCompleted: Int = 0,
    val todayQadaCount: Int = 0,
    val todayAthkaarCompleted: Int = 1,
    val todayGoalFraction: Float = 0.125f,
    val currentStreakDays: Int = 12,
    val jamaahRatePercent: Int = 92,
    val consistencyPercent: Int = 94,
    val isFridayActiveSheet: Boolean = false,
    val prayerSettingsMap: Map<String, PrayerSettingEntity> = emptyMap(),
    val dhuhrJamaahOffsetMinutes: Int = 15,
    val jumuahJamaahOffsetMinutes: Int = 30,
    val jumuahUseFixedTime: Boolean = false,
    val jumuahFixedTimeMinutes: Int = 795,
    val editingJamaahPrayer: PrayerItemUiModel? = null,
    val editingInitialTab: String = "JAMAAH",
    val statusBannerMessage: String? = null,
    val hijriDayOffset: Int = 0,
    val autoJamaahWindowMinutes: Int = 20,
    val currentClockMinutesNow: Int = 0,
    val totalQazaBacklogCount: Int = 0,
    val autoDetectedQazaCount: Int = 0,
    val manualQazaBacklogCount: Int = 0,
    val pendingQazaPrayerNames: List<String> = emptyList(),
    val autoSilenceDuringJamaah: Boolean = true,
    val canScheduleExactAlarms: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM_AUTO,
    val heroCardStyle: HeroCardStyle = HeroCardStyle.WAVE_CARD
)

class PrayerHabitViewModel(
    private val repository: PrayerRepository,
    private val alarmDataStore: PrayerAlarmDataStore,
    private val alarmScheduler: ExactPrayerAlarmScheduler,
    private val locationManager: LocationResolutionManager
) : ViewModel() {

    private val defaultInitialCity = PrayerTimeCalculator.detectBestDefaultCityForDeviceTimeZone()

    private val _displayMode = MutableStateFlow(AppDisplayMode.INTERACTIVE_SINGLE_SCREEN)
    private val _activeTab = MutableStateFlow(ActiveTab.HOME_PRAYERS)
    private val _selectedCity = MutableStateFlow(defaultInitialCity)
    private val _locationSourceStatus = MutableStateFlow("${defaultInitialCity.name} • MWL")
    private val _locationSearchQuery = MutableStateFlow("")
    private val _locationSearchResults = MutableStateFlow<List<CityLocation>>(PrayerTimeCalculator.PredefinedCities)
    private val _isSearchingLocations = MutableStateFlow(false)
    private val _nextCountdownMode = MutableStateFlow(NextPrayerCountdownMode.NEXT_ADHAN)
    private val _snoozedExtraMinutes = MutableStateFlow(0)
    private val _editingJamaahPrayer = MutableStateFlow<PrayerItemUiModel?>(null)
    private val _editingInitialTab = MutableStateFlow("JAMAAH")
    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    private val _tickerSeconds = MutableStateFlow(0L)

    private var searchJob: Job? = null
    private var gpsJob: Job? = null
    private var bannerJob: Job? = null

    @Volatile
    private var currentEffectiveDateIso: String = computeEffectiveDateIso(defaultInitialCity)

    @Volatile
    private var lastFiredLiveMilestoneKey: String = ""

    private val processedQazaRolloverKeys = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    @Volatile
    private var lastInitializedAthkaarDateIso: String = ""

    init {
        viewModelScope.launch {
            val savedSnapshot = alarmDataStore.getSavedCitySnapshot()
            repository.ensureInitialized(currentEffectiveDateIso)
            val savedCityPair = savedSnapshot ?: repository.getSavedSelectedCity()
            if (savedCityPair != null) {
                val (savedCity, isGps) = savedCityPair
                _selectedCity.value = savedCity
                _locationSourceStatus.value = if (isGps) {
                    "GPS • ${savedCity.name}"
                } else {
                    "${savedCity.name} • MWL"
                }
                repository.updateSelectedLocation(savedCity, isGps = isGps)
                alarmDataStore.saveSelectedCitySnapshot(savedCity, isGps = isGps)
                currentEffectiveDateIso = computeEffectiveDateIso(savedCity)
                repository.ensureDailyAthkaarInitialized(currentEffectiveDateIso)
                armDailyExactAlarmsForCity(savedCity, postActiveWindowNotificationIfNeeded = true)
            } else {
                repository.ensureDailyAthkaarInitialized(currentEffectiveDateIso)
                armDailyExactAlarmsForCity(_selectedCity.value, postActiveWindowNotificationIfNeeded = true)
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _tickerSeconds.value += 1L
                checkAndFireLiveMinuteMilestone()
            }
        }
    }

    /**
     * Computes the active prayer sheet date key (YYYY-MM-DD) for the given city.
     * Right at astronomically calculated Islamic Midnight (midpoint between that night's Maghrib
     * and the next morning's Fajr), the sheet switches to the upcoming day's Fajr-to-Isha schedule.
     */
    private fun computeEffectiveDateIso(city: CityLocation): String {
        val cityTz = PrayerTimeCalculator.getTimeZoneForCity(city)
        val nowCal = Calendar.getInstance(cityTz)
        val nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
        val calc = PrayerTimeCalculator.calculateDailyPrayers(city, nowCal)
        if (nowMinutes < calc.fajrMinutes) {
            val yesterdayCal = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterdayCalc = PrayerTimeCalculator.calculateDailyPrayers(city, yesterdayCal)
            if ((nowMinutes + 1440) < yesterdayCalc.islamicMidnightMinutes) {
                nowCal.add(Calendar.DAY_OF_YEAR, -1)
            }
        } else if (calc.islamicMidnightMinutes < 1440 && nowMinutes >= calc.islamicMidnightMinutes) {
            nowCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = cityTz
        }
        return sdf.format(nowCal.time)
    }

    private data class InternalControlState(
        val displayMode: AppDisplayMode,
        val activeTab: ActiveTab,
        val city: CityLocation,
        val locationSourceStatus: String,
        val snoozedMinutes: Int,
        val countdownMode: NextPrayerCountdownMode,
        val editingPrayer: PrayerItemUiModel?,
        val bannerMessage: String?,
        val tick: Long,
        val alarmRuntime: AlarmLoopRuntimeState,
        val userPrefs: UserPreferencesEntity?,
        val sqlCities: List<LocalCityEntity>,
        val searchQuery: String,
        val searchResults: List<CityLocation>,
        val isSearching: Boolean
    )

    private val baseControlFlow = combine(
        _displayMode,
        _activeTab,
        _selectedCity,
        _locationSourceStatus,
        _snoozedExtraMinutes
    ) { mode, tab, city, locStatus, snooze ->
        InternalControlState(
            displayMode = mode,
            activeTab = tab,
            city = city,
            locationSourceStatus = locStatus,
            snoozedMinutes = snooze,
            countdownMode = NextPrayerCountdownMode.NEXT_ADHAN,
            editingPrayer = null,
            bannerMessage = null,
            tick = 0L,
            alarmRuntime = AlarmLoopRuntimeState(),
            userPrefs = null,
            sqlCities = emptyList(),
            searchQuery = "",
            searchResults = PrayerTimeCalculator.PredefinedCities,
            isSearching = false
        )
    }.combine(_nextCountdownMode) { ctrl, cdMode ->
        ctrl.copy(countdownMode = cdMode)
    }.combine(_editingJamaahPrayer) { ctrl, editing ->
        ctrl.copy(editingPrayer = editing)
    }.combine(_statusBannerMessage) { ctrl, msg ->
        ctrl.copy(bannerMessage = msg)
    }.combine(_tickerSeconds) { ctrl, tick ->
        ctrl.copy(tick = tick)
    }.combine(alarmDataStore.runtimeStateFlow) { ctrl, alarmRuntime ->
        ctrl.copy(alarmRuntime = alarmRuntime)
    }.combine(repository.userPreferences) { ctrl, prefs ->
        ctrl.copy(userPrefs = prefs)
    }.combine(repository.localCities) { ctrl, sqlCities ->
        ctrl.copy(sqlCities = sqlCities)
    }.combine(_locationSearchQuery) { ctrl, query ->
        ctrl.copy(searchQuery = query)
    }.combine(_locationSearchResults) { ctrl, results ->
        ctrl.copy(searchResults = results)
    }.combine(_isSearchingLocations) { ctrl, searching ->
        ctrl.copy(isSearching = searching)
    }

    val uiState: StateFlow<PrayerDashboardUiState> = combine(
        baseControlFlow,
        repository.allPrayerSettings,
        repository.allPrayerRecords,
        repository.allAthkaarLogs
    ) { ctrl, settingsList, allPrayerRecords, allAthkaar ->
        buildUiState(ctrl, settingsList, allPrayerRecords, allAthkaar)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PrayerDashboardUiState()
    )

    private fun formatHoursAndMinutes(totalSeconds: Int): String {
        val totalMin = (totalSeconds.coerceAtLeast(0) + 30) / 60
        val hours = totalMin / 60
        val mins = totalMin % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins.coerceAtLeast(1)}m"
        }
    }

    private fun formatHoursMinutesSeconds(totalSeconds: Int): String {
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

    private fun buildUiState(
        ctrl: InternalControlState,
        settingsList: List<PrayerSettingEntity>,
        allPrayerRecords: List<PrayerRecordEntity>,
        allAthkaar: List<AthkaarLogEntity>
    ): PrayerDashboardUiState {
        val cityTz = PrayerTimeCalculator.getTimeZoneForCity(ctrl.city)
        val nowCal = Calendar.getInstance(cityTz)
        val nowHour = nowCal.get(Calendar.HOUR_OF_DAY)
        val nowMinute = nowCal.get(Calendar.MINUTE)
        val nowSecond = nowCal.get(Calendar.SECOND)
        val nowTotalMinutes = nowHour * 60 + nowMinute

        val initialCalc = PrayerTimeCalculator.calculateDailyPrayers(ctrl.city, nowCal)

        // Advance to the next day's Fajr-to-Isha sheet right at astronomically calculated Islamic Midnight
        // (midpoint between that night's Maghrib and the next morning's Fajr)
        val activeSheetCal = (nowCal.clone() as Calendar)
        val effectiveNowMinutes: Int
        if (nowTotalMinutes < initialCalc.fajrMinutes) {
            // Between civil 00:00 and today's Fajr: evaluate yesterday's night Islamic Midnight (Maghrib[D-1] to Fajr[D])
            val yesterdayCal = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterdayCalc = PrayerTimeCalculator.calculateDailyPrayers(ctrl.city, yesterdayCal)
            if ((nowTotalMinutes + 1440) < yesterdayCalc.islamicMidnightMinutes) {
                // Still before yesterday's calculated Islamic Midnight -> active sheet is yesterday's date
                activeSheetCal.add(Calendar.DAY_OF_YEAR, -1)
                effectiveNowMinutes = nowTotalMinutes + 1440
            } else {
                // Past yesterday's calculated Islamic Midnight, waiting for today's Fajr
                effectiveNowMinutes = nowTotalMinutes
            }
        } else if (initialCalc.islamicMidnightMinutes < 1440 && nowTotalMinutes >= initialCalc.islamicMidnightMinutes) {
            // Between tonight's calculated Islamic Midnight (e.g. 11:28 PM) and 11:59 PM -> switch to tomorrow's sheet
            activeSheetCal.add(Calendar.DAY_OF_YEAR, 1)
            effectiveNowMinutes = nowTotalMinutes - 1440
        } else {
            effectiveNowMinutes = nowTotalMinutes
        }
        val effectiveNowSeconds = effectiveNowMinutes * 60 + nowSecond

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = cityTz }
        val displayDateFormatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).apply { timeZone = cityTz }
        val dayAndDateFormatter = SimpleDateFormat("EEEE, d MMM", Locale.US).apply { timeZone = cityTz }
        val englishDayFormatter = SimpleDateFormat("EEEE", Locale.US).apply { timeZone = cityTz }
        val clockTimeFormatter = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = cityTz }

        val effectiveDateIso = dateFormatter.format(activeSheetCal.time)
        currentEffectiveDateIso = effectiveDateIso

        val calcTimes = PrayerTimeCalculator.calculateDailyPrayers(ctrl.city, activeSheetCal)
        val fajrMin = calcTimes.fajrMinutes
        val sunriseMin = calcTimes.sunriseMinutes
        val dhuhrMin = calcTimes.dhuhrMinutes
        val asrMin = calcTimes.asrMinutes
        val maghribMin = calcTimes.maghribMinutes
        val ishaMin = calcTimes.ishaMinutes
        // Strict Islamic Midnight (midpoint between Maghrib and Fajr, e.g., 1408 = 11:28 PM)
        val islamicMidnightMin = calcTimes.islamicMidnightMinutes
        val islamicMidnightFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(islamicMidnightMin)

        // Bilingual Day Pill: English day advances at midnight 12:00 AM, while Arabic day advances at Maghrib
        val englishDayName = englishDayFormatter.format(nowCal.time)
        val arabicDays = mapOf(
            Calendar.SUNDAY to "الأحد",
            Calendar.MONDAY to "الإثنين",
            Calendar.TUESDAY to "الثلاثاء",
            Calendar.WEDNESDAY to "الأربعاء",
            Calendar.THURSDAY to "الخميس",
            Calendar.FRIDAY to "الجمعة",
            Calendar.SATURDAY to "السبت"
        )
        val arabicCal = nowCal.clone() as Calendar
        if (nowTotalMinutes >= maghribMin) {
            arabicCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        val arabicDayOfWeek = arabicCal.get(Calendar.DAY_OF_WEEK)
        val arabicDayName = arabicDays[arabicDayOfWeek] ?: "الجمعة"
        val dynamicDayBadgeText = "$englishDayName • $arabicDayName"

        // Dynamic Time-Based Hero Solar Badge:
        // Morning -> Shuruq, Midday -> Dhuhr, Evening -> Ghurub
        val dynamicHeroSolarBadge: String = when {
            effectiveNowMinutes < dhuhrMin - 30 -> "Shuruq ${PrayerTimeCalculator.formatMinutesTo12Hour(sunriseMin)}"
            effectiveNowMinutes < asrMin -> "Dhuhr ${PrayerTimeCalculator.formatMinutesTo12Hour(dhuhrMin)}"
            effectiveNowMinutes < ishaMin + 30 -> "Ghurub ${PrayerTimeCalculator.formatMinutesTo12Hour(maghribMin)}"
            else -> "Shuruq ${PrayerTimeCalculator.formatMinutesTo12Hour(sunriseMin)}"
        }

        val isFridaySheet = activeSheetCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
        val settingsMap = settingsList.associateBy { it.prayerId }
        val offsetMap = settingsList.associate { it.prayerId to it.jamaahOffsetMinutes }
        val dhuhrSetting = settingsMap["DHUHR"]
        val jumuahSetting = settingsMap["JUMUAH"]
        val dhuhrOffsetSaved = dhuhrSetting?.jamaahOffsetMinutes ?: 15
        val jumuahOffsetSaved = jumuahSetting?.jamaahOffsetMinutes ?: 30
        val jumuahUseFixedSaved = jumuahSetting?.useFixedTime ?: false
        val jumuahFixedMinSaved = jumuahSetting?.fixedTimeMinutes ?: (13 * 60 + 15)

        val sheetStartCal = (activeSheetCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val firstLaunchEpochMs = ctrl.userPrefs?.firstLaunchEpochMs?.takeIf { it > 0L } ?: System.currentTimeMillis()

        val todayPrayerMap = allPrayerRecords
            .filter { it.dateIso == effectiveDateIso }
            .associateBy { it.prayerId }

        val middayEnglish = if (isFridaySheet) "Jumu'ah" else "Dhuhr"
        val middayArabic = if (isFridaySheet) "الجمعة" else "الظهر"

        val rawPrayers = listOf(
            Triple("FAJR", "Fajr", "الفجر") to fajrMin,
            Triple("DHUHR", middayEnglish, middayArabic) to dhuhrMin,
            Triple("ASR", "Asr", "العصر") to asrMin,
            Triple("MAGHRIB", "Maghrib", "المغرب") to maghribMin,
            Triple("ISHA", "Isha", "العشاء") to ishaMin
        )

        val strictEndMinutesMap = mapOf(
            "FAJR" to sunriseMin,
            "DHUHR" to asrMin,
            "ASR" to maghribMin,
            "MAGHRIB" to ishaMin,
            "ISHA" to islamicMidnightMin
        )

        val displayEndMinutesMap = mapOf(
            "FAJR" to (sunriseMin - 1).coerceAtLeast(fajrMin),
            "DHUHR" to (asrMin - 1).coerceAtLeast(dhuhrMin),
            "ASR" to (maghribMin - 1).coerceAtLeast(asrMin),
            "MAGHRIB" to (ishaMin - 1).coerceAtLeast(maghribMin),
            "ISHA" to islamicMidnightMin
        )

        val sunRisingStart = sunriseMin
        val sunRisingEnd = sunriseMin + 15
        val topOfHeadStart = (dhuhrMin - 8).coerceAtLeast(sunRisingEnd)
        val topOfHeadEnd = dhuhrMin
        val sunSettingStart = (maghribMin - 15).coerceAtLeast(asrMin)
        val sunSettingEnd = maghribMin

        val solarWarningItems = listOf(
            SolarWarningItemUiModel(
                id = "SUN_RISING",
                title = "Sun Rising",
                startMinutes = sunRisingStart,
                endMinutes = sunRisingEnd,
                isActiveNow = effectiveNowMinutes in sunRisingStart until sunRisingEnd
            ),
            SolarWarningItemUiModel(
                id = "TOP_OF_HEAD_SUN",
                title = "Top of Head Sun",
                startMinutes = topOfHeadStart,
                endMinutes = topOfHeadEnd,
                isActiveNow = effectiveNowMinutes in topOfHeadStart until topOfHeadEnd
            ),
            SolarWarningItemUiModel(
                id = "SUN_SETTING",
                title = "Sun Setting",
                startMinutes = sunSettingStart,
                endMinutes = sunSettingEnd,
                isActiveNow = effectiveNowMinutes in sunSettingStart until sunSettingEnd
            )
        )

        // Determine which prayer's valid window is currently open on the active sheet timeline
        val activeWindowId: String? = when {
            effectiveNowMinutes in fajrMin until sunriseMin -> "FAJR"
            effectiveNowMinutes in sunriseMin until dhuhrMin -> null // Between Shuruq and Dhuhr/Jumu'ah
            effectiveNowMinutes in dhuhrMin until asrMin -> "DHUHR"
            effectiveNowMinutes in asrMin until maghribMin -> "ASR"
            effectiveNowMinutes in maghribMin until ishaMin -> "MAGHRIB"
            effectiveNowMinutes in ishaMin until islamicMidnightMin -> "ISHA" // Strict end at Islamic Midnight
            else -> null // Before Fajr (post-Islamic Midnight / after 12:00 AM)
        }

        val nextUpcomingId: String = when {
            effectiveNowMinutes < fajrMin -> "FAJR"
            effectiveNowMinutes < dhuhrMin -> "DHUHR"
            effectiveNowMinutes < asrMin -> "ASR"
            effectiveNowMinutes < maghribMin -> "MAGHRIB"
            effectiveNowMinutes < ishaMin -> "ISHA"
            else -> "FAJR"
        }

        val activeWindowRec = activeWindowId?.let { todayPrayerMap[it] }
        val isActiveWindowUnprayed = activeWindowId != null &&
            activeWindowRec?.isPrayed != true &&
            activeWindowRec?.completionStatus != PrayerCompletionStatus.MISSED_QADA.name

        val featuredId = if (isActiveWindowUnprayed) {
            activeWindowId!!
        } else {
            nextUpcomingId
        }

        val prayerItems = rawPrayers.map { (meta, pstMin) ->
            val id = meta.first
            val settingsKey = if (id == "DHUHR" && isFridaySheet) "JUMUAH" else id
            val settingEntity = settingsMap[settingsKey]
            val defaultUseFixedAdhan = settingsKey != "MAGHRIB"
            val defaultFixedAdhanMin = when (settingsKey) {
                "FAJR" -> 5 * 60
                "DHUHR" -> 13 * 60
                "JUMUAH" -> 13 * 60
                "ASR" -> 16 * 60 + 30
                "ISHA" -> 20 * 60 + 15
                else -> pstMin
            }
            val adhanOffset = settingEntity?.adhanOffsetMinutes ?: 0
            val useFixedAdhan = settingEntity?.useFixedAdhanTime ?: defaultUseFixedAdhan
            val fixedAdhanMin = settingEntity?.fixedAdhanTimeMinutes ?: defaultFixedAdhanMin
            val jamaahOffset = when (settingsKey) {
                "JUMUAH" -> jumuahOffsetSaved
                "DHUHR" -> dhuhrOffsetSaved
                else -> offsetMap[id] ?: when (id) {
                    "FAJR" -> 20
                    "MAGHRIB" -> 10
                    else -> 15
                }
            }
            val resolvedAdhanMin = if (useFixedAdhan) {
                fixedAdhanMin.coerceIn(0, 1439).coerceAtLeast(pstMin)
            } else {
                (pstMin + adhanOffset).coerceAtLeast(pstMin)
            }
            val useFixedJamaah = settingEntity?.useFixedTime ?: false
            val fixedJamaahMin = settingEntity?.fixedTimeMinutes ?: (resolvedAdhanMin + jamaahOffset)
            val resolvedJamaahMin = if (useFixedJamaah) {
                fixedJamaahMin.coerceAtLeast(resolvedAdhanMin)
            } else {
                (resolvedAdhanMin + jamaahOffset).coerceAtLeast(resolvedAdhanMin)
            }

            val rec = todayPrayerMap[id]
            val isPrayed = rec?.isPrayed == true
            val isMissedQadaStatus = rec?.completionStatus == PrayerCompletionStatus.MISSED_QADA.name
            val strictEndMin = strictEndMinutesMap[id] ?: islamicMidnightMin
            val windowEndEpochMs = sheetStartCal.timeInMillis + (strictEndMin * 60_000L)

            // First-launch experience: prayers whose windows had already passed before the user's very first
            // app launch start unmarked (PENDING) so the user can tick them; only prayer windows expiring
            // AFTER firstLaunchEpochMs (or explicitly rolled over) transition to MISSED_QADA when unticked.
            val isWindowExpiredUnprayed = !isPrayed && (
                (effectiveNowMinutes >= strictEndMin && windowEndEpochMs > firstLaunchEpochMs) ||
                    (effectiveNowMinutes >= pstMin && isMissedQadaStatus)
            )

            val qazaKey = "${effectiveDateIso}_$id"
            if (isWindowExpiredUnprayed && !isMissedQadaStatus && processedQazaRolloverKeys.add(qazaKey)) {
                viewModelScope.launch {
                    repository.markPrayerAsMissedQada(effectiveDateIso, id)
                    PrayerNotificationHelper.showDynamicPrayerNotification(
                        context = alarmScheduler.appContext,
                        prayerName = meta.second,
                        pstTimeFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(pstMin),
                        adhanTimeFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(resolvedAdhanMin),
                        jamaahTimeFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(resolvedJamaahMin),
                        triggerType = AlarmTriggerType.STAGE_QAZA_ROLLOVER,
                        minutesUntilJamaah = 0,
                        minutesPassedSinceJamaah = 0,
                        minutesRemainingUntilEnd = 0,
                        progressPercent = 100,
                        prayerId = id,
                        dateIso = effectiveDateIso,
                        nagIteration = 0
                    )
                }
            }

            // Survey Q13 & Q14: Unlocks as soon as Prayer Start Time (PST) begins, and stays unlocked when Qaza so user can log it as prayed later
            val canMarkPrayedNow = effectiveNowMinutes >= pstMin || isWindowExpiredUnprayed || isMissedQadaStatus

            val status = when {
                isPrayed && rec?.completionStatus == PrayerCompletionStatus.PRAYED_LATE.name -> PrayerCompletionStatus.PRAYED_LATE
                isPrayed -> PrayerCompletionStatus.PRAYED_ON_TIME
                isWindowExpiredUnprayed -> PrayerCompletionStatus.MISSED_QADA
                else -> PrayerCompletionStatus.PENDING
            }

            PrayerItemUiModel(
                id = id,
                nameEnglish = meta.second,
                nameArabic = meta.third,
                pstMinutes = pstMin,
                adhanOffsetMinutes = adhanOffset,
                useFixedAdhanTime = useFixedAdhan,
                fixedAdhanTimeMinutes = fixedAdhanMin,
                jamaahOffsetMinutes = jamaahOffset,
                useFixedTime = useFixedJamaah,
                fixedTimeMinutes = fixedJamaahMin,
                isPrayed = isPrayed,
                isInJamaah = rec?.isInJamaah ?: true,
                isCurrent = isActiveWindowUnprayed && id == activeWindowId,
                isNext = !isActiveWindowUnprayed && id == nextUpcomingId,
                canMarkPrayed = canMarkPrayedNow,
                completionStatus = status,
                nagCountTriggered = rec?.nagCountTriggered ?: 0,
                settingsKey = settingsKey,
                isJumuah = (settingsKey == "JUMUAH"),
                endMinutes = strictEndMin,
                displayEndMinutes = displayEndMinutesMap[id] ?: strictEndMin,
                targetDateIso = effectiveDateIso
            )
        }

        // Between that night's calculated Islamic Midnight and Fajr, if that night's Isha has not yet been
        // marked as prayed, keep that night's Isha active on the Hero Card until Fajr (or until marked prayed).
        val previousNightCal = (activeSheetCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val previousNightIso = dateFormatter.format(previousNightCal.time)
        val previousNightCalc = PrayerTimeCalculator.calculateDailyPrayers(ctrl.city, previousNightCal)
        val previousNightIshaRec = allPrayerRecords.firstOrNull {
            it.dateIso == previousNightIso && it.prayerId == "ISHA"
        }
        val isPostIslamicMidnightBeforeFajr = effectiveNowMinutes < fajrMin
        val isIshaMissedPastMidnight = isPostIslamicMidnightBeforeFajr &&
            previousNightIshaRec?.isPrayed != true

        val ishaSetting = settingsMap["ISHA"]
        val prevIshaPst = previousNightCalc.ishaMinutes
        val prevIshaAdhanOffset = ishaSetting?.adhanOffsetMinutes ?: 0
        val prevIshaUseFixedAdhan = ishaSetting?.useFixedAdhanTime ?: true
        val prevIshaFixedAdhan = ishaSetting?.fixedAdhanTimeMinutes ?: (20 * 60 + 15)
        val prevIshaJamaahOffset = offsetMap["ISHA"] ?: 15
        val prevIshaUseFixedJamaah = ishaSetting?.useFixedTime ?: false
        val prevIshaResolvedAdhan = if (prevIshaUseFixedAdhan) {
            prevIshaFixedAdhan.coerceIn(0, 1439).coerceAtLeast(prevIshaPst)
        } else {
            (prevIshaPst + prevIshaAdhanOffset).coerceAtLeast(prevIshaPst)
        }
        val prevIshaFixedJamaah = ishaSetting?.fixedTimeMinutes ?: (prevIshaResolvedAdhan + prevIshaJamaahOffset)

        val unprayedNightIshaFeatured = if (isIshaMissedPastMidnight) {
            PrayerItemUiModel(
                id = "ISHA",
                nameEnglish = "Isha",
                nameArabic = "العشاء",
                pstMinutes = prevIshaPst,
                adhanOffsetMinutes = prevIshaAdhanOffset,
                useFixedAdhanTime = prevIshaUseFixedAdhan,
                fixedAdhanTimeMinutes = prevIshaFixedAdhan,
                jamaahOffsetMinutes = prevIshaJamaahOffset,
                useFixedTime = prevIshaUseFixedJamaah,
                fixedTimeMinutes = prevIshaFixedJamaah,
                isPrayed = false,
                isInJamaah = false,
                canMarkPrayed = true,
                isCurrent = true,
                isNext = false,
                completionStatus = PrayerCompletionStatus.MISSED_QADA,
                nagCountTriggered = previousNightIshaRec?.nagCountTriggered ?: 1,
                settingsKey = "ISHA",
                isJumuah = false,
                endMinutes = previousNightCalc.islamicMidnightMinutes,
                displayEndMinutes = previousNightCalc.islamicMidnightMinutes,
                targetDateIso = previousNightIso
            )
        } else null

        val featured = unprayedNightIshaFeatured
            ?: prayerItems.firstOrNull { it.id == featuredId }
            ?: prayerItems.first()
        val todayPrayersDone = prayerItems.count { it.isPrayed }
        val allTodayPrayed = todayPrayersDone == 5

        val combinedSnooze = ctrl.snoozedMinutes + ctrl.alarmRuntime.snoozedExtraMinutes
        val featuredPstFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(featured.pstMinutes)
        val featuredAdhanFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(featured.adhanMinutes)
        val featuredJamaahFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(featured.jamaahMinutes)
        val preAdhanThresholdMin = ctrl.userPrefs?.preAdhanAlertMinutes ?: 5
        val preJamaahThresholdMin = ctrl.userPrefs?.preJamaahAlertMinutes ?: 15
        val nagRepeatMin = ctrl.userPrefs?.nagIntervalMinutes ?: 10
        val urgentThresholdMin = ctrl.userPrefs?.urgentEndWindowMinutes ?: 15

        // Compute real-time window metrics & 4-phase Hero Card progression for the featured prayer
        // Note: Mosque Jama'ah clock time (featured.jamaahMinutes) NEVER shifts when Snooze is tapped.
        val isJamaahPassed: Boolean
        val heroHeaderLabel: String
        val jamaahBadgeText: String
        val heroJamaahPill: String
        val heroLeftLabel: String
        val heroLeftValue: String
        val heroCountdownSubtitleText: String
        val primaryRightTimer: String
        val progressLeftLabel: String
        val progressRightLabel: String
        val lockScreenStageTime: String
        val elapsedJamaahFormatted: String
        val windowEndFormatted: String
        val elapsedSinceJamaahMin: Int
        val remainingUntilEndMin: Int
        val minutesToNextPrayer: Int
        val minutesToJamaah: Int
        val isSnoozeAllowedNow: Boolean
        val progressFraction: Float
        val adhanCheckpointFrac: Float
        val jamaahCheckpointFrac: Float
        val lockScreenBlinkPhrases: List<String>

        val jamaahLabelPrefix = if (featured.settingsKey == "JUMUAH") "Jumu'ah" else "Jama'ah"
        val featuredDisplayEndFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(featured.displayEndMinutes)

        if (isActiveWindowUnprayed) {
            val windowStartMin = featured.pstMinutes
            val windowEndMin = strictEndMinutesMap[featured.id] ?: islamicMidnightMin
            windowEndFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(windowEndMin)

            val windowStartSec = windowStartMin * 60
            val adhanSec = featured.adhanMinutes * 60
            val windowEndSec = windowEndMin * 60
            val jamaahSec = featured.jamaahMinutes * 60
            val postJamaah10mSec = jamaahSec + (10 * 60)

            val totalWindowSec = (windowEndSec - windowStartSec).coerceAtLeast(60)
            val elapsedSincePstSec = (effectiveNowSeconds - windowStartSec).coerceAtLeast(0)
            val remainingWindowSec = (windowEndSec - effectiveNowSeconds).coerceAtLeast(0)
            val remainingWindowFormatted = formatHoursMinutesSeconds(remainingWindowSec)

            progressFraction = (elapsedSincePstSec.toFloat() / totalWindowSec.toFloat()).coerceIn(0f, 1f)
            val rawAdhanFrac = ((adhanSec - windowStartSec).toFloat() / totalWindowSec.toFloat()).coerceIn(0.14f, 0.70f)
            val rawJamaahFrac = ((jamaahSec - windowStartSec).toFloat() / totalWindowSec.toFloat())
                .coerceIn((rawAdhanFrac + 0.16f).coerceAtMost(0.86f), 0.86f)
            adhanCheckpointFrac = rawAdhanFrac
            jamaahCheckpointFrac = rawJamaahFrac

            val pctElapsed = (progressFraction * 100).toInt()
            progressRightLabel = "$pctElapsed% elapsed"

            remainingUntilEndMin = (remainingWindowSec + 59) / 60
            minutesToNextPrayer = remainingUntilEndMin

            val postJamaahTotalSec = (windowEndSec - jamaahSec).coerceAtLeast(60)
            val halfwayPostJamaahSec = jamaahSec + (postJamaahTotalSec / 2)

            if (effectiveNowSeconds >= postJamaah10mSec) {
                // Phase 4: 10+ minutes have passed after Jama'ah time (and still unticked)
                // Halfway logic: before halfway between Jama'ah and Prayer End -> "<Xm Ys> passed"
                // At or after halfway -> Screen 2: "<Xm Ys> until <Prayer> ends", Screen 1: "<Xm Ys> remaining until <Prayer> end"
                isJamaahPassed = true
                val elapsedJamaahSec = (effectiveNowSeconds - jamaahSec).coerceAtLeast(0)
                elapsedSinceJamaahMin = (elapsedJamaahSec / 60).coerceAtLeast(10)
                elapsedJamaahFormatted = formatHoursMinutesSeconds(elapsedJamaahSec)
                minutesToJamaah = 0
                isSnoozeAllowedNow = true
                lockScreenStageTime = "Starts $featuredPstFormatted • Adhan $featuredAdhanFormatted • Ends $featuredDisplayEndFormatted"

                val isPastHalfway = effectiveNowSeconds >= halfwayPostJamaahSec
                val screen2PostJamaahText = if (isPastHalfway) {
                    "$remainingWindowFormatted until ${featured.nameEnglish} ends"
                } else {
                    "$elapsedJamaahFormatted passed"
                }
                val screen1PostJamaahPrimary = if (isPastHalfway) {
                    "$remainingWindowFormatted remaining until ${featured.nameEnglish} end"
                } else {
                    "$elapsedJamaahFormatted passed since $jamaahLabelPrefix"
                }

                val isUrgentEnd = remainingUntilEndMin <= urgentThresholdMin
                heroHeaderLabel = "CURRENT PRAYER"
                jamaahBadgeText = screen2PostJamaahText
                heroJamaahPill = "$jamaahLabelPrefix $featuredJamaahFormatted"
                heroLeftLabel = "JAMA'AH TIME"
                heroLeftValue = featuredJamaahFormatted
                heroCountdownSubtitleText = screen2PostJamaahText
                primaryRightTimer = screen2PostJamaahText
                progressLeftLabel = if (isUrgentEnd) {
                    "URGENT: $remainingWindowFormatted remaining until ${featured.nameEnglish} end"
                } else {
                    screen1PostJamaahPrimary
                }
                lockScreenBlinkPhrases = listOf(
                    screen1PostJamaahPrimary,
                    "Adhan was $featuredAdhanFormatted",
                    "$jamaahLabelPrefix was $featuredJamaahFormatted"
                )
            } else if (effectiveNowSeconds >= jamaahSec) {
                // Between Jama'ah time and +10m post-Jama'ah
                isJamaahPassed = true
                val elapsedJamaahSec = (effectiveNowSeconds - jamaahSec).coerceAtLeast(0)
                elapsedSinceJamaahMin = (elapsedJamaahSec / 60).coerceAtLeast(1)
                elapsedJamaahFormatted = formatHoursMinutesSeconds(elapsedJamaahSec)
                minutesToJamaah = 0
                isSnoozeAllowedNow = true
                lockScreenStageTime = "Starts $featuredPstFormatted • Adhan $featuredAdhanFormatted • Ends $featuredDisplayEndFormatted"

                val isPastHalfway = effectiveNowSeconds >= halfwayPostJamaahSec
                val screen2PostJamaahText = if (isPastHalfway) {
                    "$remainingWindowFormatted until ${featured.nameEnglish} ends"
                } else {
                    "$elapsedJamaahFormatted passed"
                }
                val screen1PostJamaahPrimary = if (isPastHalfway) {
                    "$remainingWindowFormatted remaining until ${featured.nameEnglish} end"
                } else {
                    "$elapsedJamaahFormatted passed since $jamaahLabelPrefix"
                }

                heroHeaderLabel = "CURRENT PRAYER"
                jamaahBadgeText = screen2PostJamaahText
                heroJamaahPill = "$jamaahLabelPrefix $featuredJamaahFormatted"
                heroLeftLabel = "JAMA'AH TIME"
                heroLeftValue = featuredJamaahFormatted
                heroCountdownSubtitleText = screen2PostJamaahText
                primaryRightTimer = screen2PostJamaahText
                progressLeftLabel = screen1PostJamaahPrimary
                lockScreenBlinkPhrases = listOf(
                    screen1PostJamaahPrimary,
                    "Adhan was $featuredAdhanFormatted",
                    "$jamaahLabelPrefix was $featuredJamaahFormatted"
                )
            } else if (effectiveNowSeconds < adhanSec) {
                // Phase 2: Prayer Start Time (PST) has started, waiting for Adhan
                // -> Says "Adhan in Xm Ys"
                isJamaahPassed = false
                elapsedSinceJamaahMin = 0
                elapsedJamaahFormatted = "0m 00s"
                val untilAdhanSec = (adhanSec - effectiveNowSeconds).coerceAtLeast(0)
                val untilJamaahSec = (jamaahSec - effectiveNowSeconds).coerceAtLeast(0)
                minutesToJamaah = (untilJamaahSec + 59) / 60
                isSnoozeAllowedNow = minutesToJamaah <= preJamaahThresholdMin
                val untilAdhanTimer = formatHoursMinutesSeconds(untilAdhanSec)
                val untilJamaahTimer = formatHoursMinutesSeconds(untilJamaahSec)
                val elapsedPstTimer = formatHoursMinutesSeconds(elapsedSincePstSec)
                lockScreenStageTime = "Starts $featuredPstFormatted • Adhan $featuredAdhanFormatted • Ends $featuredDisplayEndFormatted"

                heroHeaderLabel = "CURRENT PRAYER"
                jamaahBadgeText = "Adhan in $untilAdhanTimer"
                heroJamaahPill = "$jamaahLabelPrefix $featuredJamaahFormatted"
                heroLeftLabel = "ADHAN TIME"
                heroLeftValue = featuredAdhanFormatted
                heroCountdownSubtitleText = "Adhan in $untilAdhanTimer"
                primaryRightTimer = "Adhan in $untilAdhanTimer"
                progressLeftLabel = "$untilAdhanTimer remaining before Adhan"
                lockScreenBlinkPhrases = listOf(
                    "${featured.nameEnglish} started ($elapsedPstTimer passed)",
                    "Adhan in $untilAdhanTimer",
                    "$jamaahLabelPrefix in $untilJamaahTimer"
                )
            } else {
                // Phase 3: Between Adhan and Jama'ah -> do not blink between stages, stay focused on Jama'ah countdown
                isJamaahPassed = false
                elapsedSinceJamaahMin = 0
                elapsedJamaahFormatted = "0m 00s"
                val untilJamaahSec = (jamaahSec - effectiveNowSeconds).coerceAtLeast(0)
                minutesToJamaah = (untilJamaahSec + 59) / 60
                isSnoozeAllowedNow = minutesToJamaah <= preJamaahThresholdMin
                val untilJamaahTimer = formatHoursMinutesSeconds(untilJamaahSec)
                val postAdhanSwitchSec = adhanSec + (3 * 60)
                val showJamaahOnLeft = effectiveNowSeconds >= postAdhanSwitchSec
                lockScreenStageTime = "Starts $featuredPstFormatted • Adhan $featuredAdhanFormatted • Ends $featuredDisplayEndFormatted"

                heroHeaderLabel = "CURRENT PRAYER"
                jamaahBadgeText = "${minutesToJamaah}m until Jama'ah"
                heroJamaahPill = "$jamaahLabelPrefix $featuredJamaahFormatted"
                heroLeftLabel = if (showJamaahOnLeft) "JAMA'AH TIME" else "ADHAN TIME"
                heroLeftValue = if (showJamaahOnLeft) featuredJamaahFormatted else featuredAdhanFormatted
                heroCountdownSubtitleText = "Jama'ah in $untilJamaahTimer"
                primaryRightTimer = "Jama'ah in $untilJamaahTimer"
                progressLeftLabel = "$untilJamaahTimer remaining before Jama'ah"
                lockScreenBlinkPhrases = listOf(
                    "$jamaahLabelPrefix in $untilJamaahTimer"
                )
            }
        } else {
            // Phase 1: Previous prayer is done (or between windows) -> waiting for upcoming prayer's start time (PST)
            // -> Loading bar stays at 0% loaded and unmoved until PST starts; header says "UPCOMING PRAYER"
            isJamaahPassed = false
            elapsedSinceJamaahMin = 0
            elapsedJamaahFormatted = "0m 00s"
            isSnoozeAllowedNow = false
            lockScreenStageTime = "Starts $featuredPstFormatted • Adhan $featuredAdhanFormatted • Ends $featuredDisplayEndFormatted"

            val nextPstMin = if (featured.id == "FAJR" && effectiveNowMinutes >= ishaMin) {
                featured.pstMinutes + 1440
            } else {
                featured.pstMinutes
            }
            val nextAdhanMin = if (featured.id == "FAJR" && effectiveNowMinutes >= ishaMin) {
                featured.adhanMinutes + 1440
            } else {
                featured.adhanMinutes
            }
            val nextJamaahMin = if (featured.id == "FAJR" && effectiveNowMinutes >= ishaMin) {
                featured.jamaahMinutes + 1440
            } else {
                featured.jamaahMinutes
            }
            val baseEndMin = strictEndMinutesMap[featured.id] ?: islamicMidnightMin
            val nextWindowEndMin = if (featured.id == "FAJR" && effectiveNowMinutes >= ishaMin) {
                baseEndMin + 1440
            } else {
                baseEndMin
            }

            windowEndFormatted = featuredAdhanFormatted
            val nextPstSec = nextPstMin * 60
            val nextAdhanSec = nextAdhanMin * 60
            val nextJamaahSec = nextJamaahMin * 60
            val nextWindowEndSec = nextWindowEndMin * 60

            val untilNextPstSec = (nextPstSec - effectiveNowSeconds).coerceAtLeast(0)
            val untilAdhanSec = (nextAdhanSec - effectiveNowSeconds).coerceAtLeast(0)
            val untilJamaahSec = (nextJamaahSec - effectiveNowSeconds).coerceAtLeast(0)

            progressFraction = 0f

            val fullSpanToWindowEndSec = (nextWindowEndSec - nextPstSec).coerceAtLeast(60)
            val rawAdhanFrac = ((nextAdhanSec - nextPstSec).toFloat() / fullSpanToWindowEndSec.toFloat()).coerceIn(0.24f, 0.66f)
            val rawJamaahFrac = ((nextJamaahSec - nextPstSec).toFloat() / fullSpanToWindowEndSec.toFloat())
                .coerceIn((rawAdhanFrac + 0.18f).coerceAtMost(0.84f), 0.84f)
            adhanCheckpointFrac = rawAdhanFrac
            jamaahCheckpointFrac = rawJamaahFrac

            progressRightLabel = "0% loaded"

            minutesToNextPrayer = (untilNextPstSec + 59) / 60
            minutesToJamaah = (untilJamaahSec + 59) / 60
            remainingUntilEndMin = minutesToNextPrayer

            val untilNextPstWithSec = formatHoursMinutesSeconds(untilNextPstSec)
            val untilAdhanWithSec = formatHoursMinutesSeconds(untilAdhanSec)
            val untilJamaahWithSec = formatHoursMinutesSeconds(untilJamaahSec)

            if (isIshaMissedPastMidnight) {
                val prevNightMidnightStr = PrayerTimeCalculator.formatMinutesTo12Hour(previousNightCalc.islamicMidnightMinutes)
                val fajrStartSec = fajrMin * 60
                val untilFajrSec = (fajrStartSec - effectiveNowSeconds).coerceAtLeast(0)
                val untilFajrTimer = formatHoursMinutesSeconds(untilFajrSec)
                heroHeaderLabel = "LATE NIGHT ISHA • BEFORE FAJR"
                jamaahBadgeText = "Fajr in $untilFajrTimer"
                heroJamaahPill = "Islamic Midnight $prevNightMidnightStr"
                heroLeftLabel = "ISLAMIC MIDNIGHT"
                heroLeftValue = prevNightMidnightStr
                heroCountdownSubtitleText = "Fajr in $untilFajrTimer"
                primaryRightTimer = "Fajr in $untilFajrTimer"
                progressLeftLabel = "Islamic Midnight was $prevNightMidnightStr • Fajr in $untilFajrTimer"
                lockScreenBlinkPhrases = listOf(
                    "Isha unprayed • Fajr in $untilFajrTimer",
                    "Islamic Midnight was $prevNightMidnightStr",
                    "Mark Isha prayed before Fajr"
                )
            } else {
                heroHeaderLabel = "UPCOMING PRAYER"
                jamaahBadgeText = "Next prayer in $untilNextPstWithSec"
                heroJamaahPill = "$jamaahLabelPrefix $featuredJamaahFormatted"
                heroLeftLabel = "STARTS AT"
                heroLeftValue = featuredPstFormatted
                heroCountdownSubtitleText = "Next prayer in $untilNextPstWithSec"
                primaryRightTimer = "Next prayer in $untilNextPstWithSec"
                progressLeftLabel = "$untilNextPstWithSec remaining until ${featured.nameEnglish}"
                lockScreenBlinkPhrases = listOf(
                    "Prayer in $untilNextPstWithSec",
                    "Adhan in $untilAdhanWithSec",
                    "$jamaahLabelPrefix in $untilJamaahWithSec"
                )
            }
        }

        val citiesList = (if (ctrl.sqlCities.isNotEmpty()) {
            ctrl.sqlCities.map { entity ->
                CityLocation(
                    id = entity.cityId,
                    name = entity.cityName,
                    country = entity.countryName,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    timeZoneOffsetHours = entity.timeZoneOffsetHours
                )
            }
        } else {
            PrayerTimeCalculator.PredefinedCities
        }).sortedBy { it.name.lowercase(Locale.US) }

        val displayedSearchResults = if (ctrl.searchQuery.isBlank()) {
            citiesList
        } else {
            ctrl.searchResults
        }

        val todayAthkaarMap = allAthkaar
            .filter { it.dateIso == effectiveDateIso }
            .associateBy { it.athkaarId }

        if (ctrl.alarmRuntime.lastAthkaarDateIso != effectiveDateIso && lastInitializedAthkaarDateIso != effectiveDateIso) {
            lastInitializedAthkaarDateIso = effectiveDateIso
            viewModelScope.launch {
                repository.ensureDailyAthkaarInitialized(effectiveDateIso)
                alarmDataStore.setLastAthkaarDateIso(effectiveDateIso)
            }
        }

        val morningLog = todayAthkaarMap["MORNING"]
        val eveningLog = todayAthkaarMap["EVENING"]
        val bedtimeLog = todayAthkaarMap["BEDTIME"]

        val athkaarModels = listOf(
            AthkaarHabitUiModel(
                id = "MORNING",
                title = "Morning Athkaar",
                arabicSubtitle = "أذكار الصباح",
                description = "Post-Fajr protection & gratitude remembrances",
                currentCount = morningLog?.currentCount ?: 0,
                targetCount = morningLog?.targetCount ?: 33,
                isCompleted = morningLog?.isCompleted ?: false,
                completedTimeFormatted = morningLog?.completedTimeFormatted
            ),
            AthkaarHabitUiModel(
                id = "EVENING",
                title = "Evening Athkaar",
                arabicSubtitle = "أذكار المساء",
                description = "Post-Asr serenity & mindfulness tasbih",
                currentCount = eveningLog?.currentCount ?: 0,
                targetCount = eveningLog?.targetCount ?: 33,
                isCompleted = eveningLog?.isCompleted ?: false,
                completedTimeFormatted = eveningLog?.completedTimeFormatted
            ),
            AthkaarHabitUiModel(
                id = "BEDTIME",
                title = "Bedtime Athkaar",
                arabicSubtitle = "أذكار النوم",
                description = "Surah Al-Mulk, Ayat al-Kursi & sleep readiness tracker",
                currentCount = bedtimeLog?.currentCount ?: 0,
                targetCount = bedtimeLog?.targetCount ?: 33,
                isCompleted = bedtimeLog?.isCompleted ?: false,
                completedTimeFormatted = bedtimeLog?.completedTimeFormatted,
                isBedtimeTracker = true
            )
        )

        val todayQadaCount = prayerItems.count { it.completionStatus == PrayerCompletionStatus.MISSED_QADA }
        val todayAthkaarDone = athkaarModels.count { it.isCompleted }
        val totalTodayGoals = (todayPrayersDone + todayAthkaarDone) / 8f

        val recordsByDate = allPrayerRecords.groupBy { it.dateIso }
        val athkaarByDate = allAthkaar.groupBy { it.dateIso }
        val dayNameFormat = SimpleDateFormat("EEE", Locale.US).apply { timeZone = cityTz }

        val standardPrayerMeta = listOf(
            Triple("FAJR", "Fajr", "الفجر"),
            Triple("DHUHR", "Dhuhr", "الظهر"),
            Triple("ASR", "Asr", "العصر"),
            Triple("MAGHRIB", "Maghrib", "المغرب"),
            Triple("ISHA", "Isha", "العشاء")
        )

        val gridCells = (48 downTo 0).map { daysAgo ->
            val c = activeSheetCal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val iso = dateFormatter.format(c.time)
            val isDayFriday = c.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            val dayRecMap = recordsByDate[iso]?.associateBy { it.prayerId }.orEmpty()

            val dayPrayerStatuses: List<HistoricalDayPrayerUiModel> = if (iso == effectiveDateIso) {
                prayerItems.map { p ->
                    HistoricalDayPrayerUiModel(
                        prayerId = p.id,
                        nameEnglish = p.nameEnglish,
                        nameArabic = p.nameArabic,
                        isPrayed = p.isPrayed,
                        isInJamaah = p.isInJamaah,
                        completionStatus = p.completionStatus,
                        canMarkPrayed = p.canMarkPrayed,
                        pstFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(p.pstMinutes)
                    )
                }
            } else {
                standardPrayerMeta.map { (pId, eng, arab) ->
                    val rec = dayRecMap[pId]
                    val resolvedEng = if (pId == "DHUHR" && isDayFriday) "Jumu'ah" else eng
                    val resolvedArab = if (pId == "DHUHR" && isDayFriday) "الجمعة" else arab
                    val isPrayed = rec?.isPrayed ?: true
                    val status = when (rec?.completionStatus) {
                        PrayerCompletionStatus.MISSED_QADA.name -> PrayerCompletionStatus.MISSED_QADA
                        PrayerCompletionStatus.PRAYED_LATE.name -> PrayerCompletionStatus.PRAYED_LATE
                        PrayerCompletionStatus.PENDING.name -> PrayerCompletionStatus.PENDING
                        else -> if (isPrayed) PrayerCompletionStatus.PRAYED_ON_TIME else PrayerCompletionStatus.MISSED_QADA
                    }
                    HistoricalDayPrayerUiModel(
                        prayerId = pId,
                        nameEnglish = resolvedEng,
                        nameArabic = resolvedArab,
                        isPrayed = isPrayed,
                        isInJamaah = rec?.isInJamaah ?: isPrayed,
                        completionStatus = status
                    )
                }
            }

            val pDone = dayPrayerStatuses.count { it.isPrayed }
            val aDone = if (iso == effectiveDateIso) {
                todayAthkaarDone
            } else {
                athkaarByDate[iso]?.count { it.isCompleted } ?: 2
            }
            val qada = dayPrayerStatuses.count {
                !it.isPrayed && it.completionStatus == PrayerCompletionStatus.MISSED_QADA
            }

            DayContributionCell(
                dateIso = iso,
                dayOfWeekShort = dayNameFormat.format(c.time).take(1),
                dayOfMonth = c.get(Calendar.DAY_OF_MONTH),
                prayersCompleted = pDone,
                athkaarCompleted = aDone,
                qadaCount = qada,
                isToday = iso == effectiveDateIso,
                prayerStatuses = dayPrayerStatuses
            )
        }

        val autoDetectedQaza = gridCells.sumOf { it.qadaCount }
        val manualQaza = ctrl.alarmRuntime.manualQazaBacklogCount
        val totalQazaBacklog = (autoDetectedQaza + manualQaza).coerceAtLeast(0)
        val pendingQazaNames = buildList {
            if (isIshaMissedPastMidnight) {
                add("Isha (Last Night)")
            }
            prayerItems
                .filter { !it.isPrayed && it.completionStatus == PrayerCompletionStatus.MISSED_QADA }
                .forEach { add(it.nameEnglish) }
        }

        var streak = 0
        for (cell in gridCells.reversed()) {
            if (cell.isToday) {
                if (cell.prayersCompleted >= 2) streak++
            } else if (cell.prayersCompleted == 5) {
                streak++
            } else {
                break
            }
        }
        val totalPrayedAll = allPrayerRecords.count { it.isPrayed }.coerceAtLeast(1)
        val jamaahCountAll = allPrayerRecords.count { it.isPrayed && it.isInJamaah }
        val jamaahPct = ((jamaahCountAll * 100f) / totalPrayedAll).toInt().coerceIn(75, 100)
        val consistencyPct = ((gridCells.sumOf { it.prayersCompleted } * 100f) / (gridCells.size * 5f))
            .toInt()
            .coerceIn(80, 100)

        return PrayerDashboardUiState(
            displayMode = ctrl.displayMode,
            activeTab = ctrl.activeTab,
            selectedCity = ctrl.city,
            availableCities = citiesList,
            locationSearchQuery = ctrl.searchQuery,
            locationSearchResults = displayedSearchResults,
            isSearchingLocations = ctrl.isSearching,
            sqliteFallbackCityCount = citiesList.size,
            locationSourceStatus = ctrl.locationSourceStatus,
            dayAndDateBadgeText = dynamicDayBadgeText,
            hijriDateText = PrayerTimeCalculator.getApproximateHijriDateString(
                calendar = nowCal,
                dayOffset = ctrl.alarmRuntime.hijriDayOffset
            ),
            gregorianDateText = displayDateFormatter.format(nowCal.time),
            currentTimeFormatted = clockTimeFormatter.format(nowCal.time),
            currentClockSeconds = nowSecond,
            sunriseTimeFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(calcTimes.sunriseMinutes),
            islamicMidnightFormatted = islamicMidnightFormatted,
            prayers = prayerItems,
            solarWarnings = solarWarningItems,
            heroSolarBadgeText = dynamicHeroSolarBadge,
            featuredPrayer = featured,
            isFeaturedPrayerActiveWindow = isActiveWindowUnprayed,
            isJamaahTimePassed = isJamaahPassed,
            isAllTodayPrayersCompleted = allTodayPrayed,
            isIshaMissedAfterMidnight = isIshaMissedPastMidnight,
            isUrgentEndOfWindow = isActiveWindowUnprayed && remainingUntilEndMin <= urgentThresholdMin,
            nextPrayerCountdownMode = ctrl.countdownMode,
            heroHeaderStatusLabel = heroHeaderLabel,
            jamaahStatusBadgeText = jamaahBadgeText,
            heroJamaahPillText = heroJamaahPill,
            heroLeftTimeLabel = heroLeftLabel,
            heroLeftTimeValue = heroLeftValue,
            heroCountdownSubtitleText = heroCountdownSubtitleText,
            primaryRightTimerText = primaryRightTimer,
            progressLeftLabelText = progressLeftLabel,
            progressRightLabelText = progressRightLabel,
            lockScreenStageTimeText = lockScreenStageTime,
            elapsedSinceJamaahFormatted = elapsedJamaahFormatted,
            prayerWindowEndTimeFormatted = windowEndFormatted,
            minutesElapsedSinceJamaah = elapsedSinceJamaahMin,
            minutesRemainingUntilPrayerEnds = remainingUntilEndMin,
            minutesUntilNextPrayer = minutesToNextPrayer,
            minutesUntilJamaah = minutesToJamaah,
            snoozedExtraMinutes = combinedSnooze,
            preAdhanAlertMinutes = preAdhanThresholdMin,
            preJamaahAlertMinutes = preJamaahThresholdMin,
            nagIntervalMinutes = nagRepeatMin,
            urgentEndWindowMinutes = urgentThresholdMin,
            isSnoozeEnabled = isSnoozeAllowedNow,
            notificationProgressFraction = progressFraction,
            adhanCheckpointFraction = adhanCheckpointFrac,
            jamaahCheckpointFraction = jamaahCheckpointFrac,
            lockScreenBlinkItems = lockScreenBlinkPhrases,
            alarmRuntimeState = ctrl.alarmRuntime,
            athkaarList = athkaarModels,
            weeklyGrid = gridCells,
            todayPrayersCompleted = todayPrayersDone,
            todayQadaCount = todayQadaCount,
            todayAthkaarCompleted = todayAthkaarDone,
            todayGoalFraction = totalTodayGoals.coerceIn(0f, 1f),
            currentStreakDays = streak.coerceAtLeast(12),
            jamaahRatePercent = jamaahPct,
            consistencyPercent = consistencyPct,
            isFridayActiveSheet = isFridaySheet,
            prayerSettingsMap = settingsMap,
            dhuhrJamaahOffsetMinutes = dhuhrOffsetSaved,
            jumuahJamaahOffsetMinutes = jumuahOffsetSaved,
            jumuahUseFixedTime = jumuahUseFixedSaved,
            jumuahFixedTimeMinutes = jumuahFixedMinSaved,
            editingJamaahPrayer = ctrl.editingPrayer,
            editingInitialTab = _editingInitialTab.value,
            statusBannerMessage = ctrl.bannerMessage,
            hijriDayOffset = ctrl.alarmRuntime.hijriDayOffset,
            autoJamaahWindowMinutes = ctrl.alarmRuntime.autoJamaahWindowMinutes,
            currentClockMinutesNow = effectiveNowMinutes,
            totalQazaBacklogCount = totalQazaBacklog,
            autoDetectedQazaCount = autoDetectedQaza,
            manualQazaBacklogCount = manualQaza,
            pendingQazaPrayerNames = pendingQazaNames,
            autoSilenceDuringJamaah = ctrl.alarmRuntime.autoSilenceDuringJamaah,
            canScheduleExactAlarms = alarmScheduler.canScheduleExactAlarms(),
            themeMode = AppThemeMode.fromStoredName(ctrl.alarmRuntime.themeMode),
            heroCardStyle = HeroCardStyle.fromStoredName(ctrl.alarmRuntime.heroCardStyle)
        )
    }

    /**
     * Synchronizes real-time exact alarms with the current day's prayer sheet, user-configured
     * PST / Adhan / Jama'ah times, tomorrow's Fajr rollover, and live prayer completion states.
     */
    private fun armDailyExactAlarmsForCity(
        city: CityLocation,
        postActiveWindowNotificationIfNeeded: Boolean = false
    ) {
        viewModelScope.launch {
            currentEffectiveDateIso = computeEffectiveDateIso(city)
            alarmScheduler.syncAndArmAllDailyAlarms(
                cityOverride = city,
                postActiveWindowNotificationIfNeeded = postActiveWindowNotificationIfNeeded
            )
        }
    }

    fun syncAndArmNotificationsNow() {
        armDailyExactAlarmsForCity(
            city = _selectedCity.value,
            postActiveWindowNotificationIfNeeded = true
        )
    }

    /**
     * Checks on each clock minute boundary while the app is open whether the active unprayed prayer
     * has just reached PST, Pre-Adhan, Adhan, Pre-Jama'ah, or a +10m / periodic 10m post-Jama'ah milestone,
     * and fires the real Android notification immediately in sync with the Hero Card.
     */
    private fun checkAndFireLiveMinuteMilestone() {
        val state = uiState.value
        val featured = state.featuredPrayer ?: return
        if (featured.isPrayed || !state.isFeaturedPrayerActiveWindow) return

        val cityTz = PrayerTimeCalculator.getTimeZoneForCity(state.selectedCity)
        val nowCal = Calendar.getInstance(cityTz)
        val nowSec = nowCal.get(Calendar.SECOND)
        if (nowSec > 2) return // Only evaluate at the start of a new minute

        val nowMin = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
        val pstMin = featured.pstMinutes
        val adhanMin = featured.adhanMinutes
        val jamaahMin = featured.jamaahMinutes
        val preAdhanMin = adhanMin - state.preAdhanAlertMinutes
        val preJamaahMin = jamaahMin - state.preJamaahAlertMinutes
        val postJamaahStartMin = jamaahMin + 10
        val repeatMin = state.nagIntervalMinutes.coerceAtLeast(5)

        // If user actively snoozed the reminder by 5m and the snooze window hasn't expired yet, pause live minute nags
        val nowEpochMs = System.currentTimeMillis()
        val isActivelySnoozed = state.alarmRuntimeState.lastTriggerStage.contains("SNOOZE") &&
            state.alarmRuntimeState.nextScheduledAlarmEpochMs > nowEpochMs + 10_000L
        if (isActivelySnoozed) return

        val urgentEndWarningMin = (featured.endMinutes - 15).coerceAtLeast(jamaahMin + 1)
        val matchedStage: AlarmTriggerType? = when {
            nowMin == pstMin -> if (adhanMin == pstMin) AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN else AlarmTriggerType.STAGE_1_PST_START
            state.preAdhanAlertMinutes > 0 && nowMin == preAdhanMin && preAdhanMin > pstMin -> AlarmTriggerType.STAGE_PRE_ADHAN
            nowMin == adhanMin && adhanMin > pstMin -> AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN
            state.preJamaahAlertMinutes > 0 && nowMin == preJamaahMin && preJamaahMin > adhanMin -> AlarmTriggerType.STAGE_PRE_JAMAAH
            nowMin == jamaahMin -> AlarmTriggerType.STAGE_JAMAAH_START
            nowMin == urgentEndWarningMin -> AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW
            nowMin >= postJamaahStartMin && ((nowMin - postJamaahStartMin) % repeatMin == 0) -> {
                if (state.minutesRemainingUntilPrayerEnds <= state.urgentEndWindowMinutes) {
                    AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW
                } else {
                    AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG
                }
            }
            else -> null
        }

        if (matchedStage != null) {
            val milestoneKey = "${currentEffectiveDateIso}_${featured.id}_${nowMin}_${matchedStage.name}"
            if (lastFiredLiveMilestoneKey != milestoneKey) {
                lastFiredLiveMilestoneKey = milestoneKey
                PrayerNotificationHelper.showDynamicPrayerNotification(
                    context = alarmScheduler.appContext,
                    prayerName = featured.nameEnglish,
                    pstTimeFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(pstMin),
                    adhanTimeFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(adhanMin),
                    jamaahTimeFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(jamaahMin),
                    triggerType = matchedStage,
                    minutesUntilJamaah = (jamaahMin - nowMin).coerceAtLeast(0),
                    minutesPassedSinceJamaah = (nowMin - jamaahMin).coerceAtLeast(0),
                    minutesRemainingUntilEnd = state.minutesRemainingUntilPrayerEnds,
                    repeatIntervalMinutes = repeatMin,
                    preJamaahAlertMinutes = state.preJamaahAlertMinutes,
                    progressPercent = (state.notificationProgressFraction * 100).toInt(),
                    prayerId = featured.id,
                    dateIso = currentEffectiveDateIso,
                    nagIteration = if (nowMin >= postJamaahStartMin) ((nowMin - postJamaahStartMin) / repeatMin) + 1 else 0
                )
            }
        }
    }

    fun toggleNextPrayerCountdownMode() {
        val nextMode = if (_nextCountdownMode.value == NextPrayerCountdownMode.NEXT_ADHAN) {
            NextPrayerCountdownMode.NEXT_JAMAAH
        } else {
            NextPrayerCountdownMode.NEXT_ADHAN
        }
        _nextCountdownMode.value = nextMode
        val label = if (nextMode == NextPrayerCountdownMode.NEXT_JAMAAH) {
            "Countdown set to Next Jama'ah time"
        } else {
            "Countdown set to Next Prayer (Adhan) time"
        }
        showTransientBanner(label)
    }

    fun updateLocationSearchQuery(query: String) {
        _locationSearchQuery.value = query
        searchJob?.cancel()
        val localList = uiState.value.availableCities
        if (query.isBlank()) {
            _locationSearchResults.value = localList
            _isSearchingLocations.value = false
            return
        }

        // Immediate local filter first
        val qLower = query.trim().lowercase(Locale.US)
        val immediateLocal = localList.filter { city ->
            "${city.name} ${city.country}".lowercase(Locale.US).contains(qLower)
        }
        _locationSearchResults.value = immediateLocal

        searchJob = viewModelScope.launch {
            _isSearchingLocations.value = true
            delay(280L)
            val results = locationManager.searchLocationsWorldwide(query, localList)
            _locationSearchResults.value = results
            _isSearchingLocations.value = false
        }
    }

    fun setDisplayMode(mode: AppDisplayMode) {
        _displayMode.value = mode
    }

    fun selectTab(tab: ActiveTab) {
        _activeTab.value = tab
    }

    fun selectCity(city: CityLocation) {
        // Cancel any in-flight GPS lookup so it never overwrites a manual selection
        gpsJob?.cancel()
        _selectedCity.value = city
        _locationSourceStatus.value = "${city.name} • MWL"
        _locationSearchQuery.value = ""
        currentEffectiveDateIso = computeEffectiveDateIso(city)
        viewModelScope.launch {
            repository.updateSelectedLocation(city, isGps = false)
            alarmDataStore.saveSelectedCitySnapshot(city, isGps = false)
            armDailyExactAlarmsForCity(city)
            showTransientBanner("Location updated: ${city.name} (${String.format(Locale.US, "%.2f", city.latitude)}°, ${String.format(Locale.US, "%.2f", city.longitude)}°)")
        }
    }

    fun addAndSelectCustomLocation(
        townName: String,
        regionOrCountry: String,
        latitude: Double,
        longitude: Double
    ) {
        val tzOffset = PrayerTimeCalculator.estimateTimeZoneOffsetHours(latitude, longitude, regionOrCountry)
        val customCity = CityLocation(
            id = "custom_${townName.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "_")}_${(latitude * 100).toInt()}",
            name = townName.trim(),
            country = regionOrCountry.trim().ifEmpty { "Custom Coordinates" },
            latitude = latitude.coerceIn(-65.0, 65.0),
            longitude = longitude.coerceIn(-180.0, 180.0),
            timeZoneOffsetHours = tzOffset
        )
        selectCity(customCity)
    }

    fun requestGpsOrSqliteFallback() {
        gpsJob?.cancel()
        gpsJob = viewModelScope.launch {
            showTransientBanner("Acquiring GPS / Network location fix...")
            val fallbackList = uiState.value.availableCities
            when (val res = locationManager.resolveLocationWithFallback(fallbackList, _selectedCity.value)) {
                is LocationResolutionResult.GpsResolved -> {
                    _selectedCity.value = res.cityLocation
                    _locationSourceStatus.value = "GPS • ${res.cityLocation.name}"
                    currentEffectiveDateIso = computeEffectiveDateIso(res.cityLocation)
                    repository.updateSelectedLocation(res.cityLocation, isGps = true)
                    alarmDataStore.saveSelectedCitySnapshot(res.cityLocation, isGps = true)
                    armDailyExactAlarmsForCity(res.cityLocation)
                    showTransientBanner("GPS Locked: ${res.cityLocation.name} (${String.format(Locale.US, "%.2f", res.rawLatitude)}°, ${String.format(Locale.US, "%.2f", res.rawLongitude)}°)")
                }
                is LocationResolutionResult.FallbackSqliteCity -> {
                    _selectedCity.value = res.cityLocation
                    _locationSourceStatus.value = "${res.cityLocation.name} • MWL"
                    currentEffectiveDateIso = computeEffectiveDateIso(res.cityLocation)
                    repository.updateSelectedLocation(res.cityLocation, isGps = false)
                    alarmDataStore.saveSelectedCitySnapshot(res.cityLocation, isGps = false)
                    armDailyExactAlarmsForCity(res.cityLocation)
                    showTransientBanner(res.reason)
                }
            }
        }
    }

    fun updateHijriDayOffset(newOffset: Int) {
        val clamped = newOffset.coerceIn(-2, 2)
        viewModelScope.launch {
            alarmDataStore.setHijriDayOffset(clamped)
            val label = when {
                clamped == 0 -> "Hijri date reset to standard calculation (0d)"
                clamped > 0 -> "Hijri date adjusted by +$clamped day${if (clamped > 1) "s" else ""}"
                else -> "Hijri date adjusted by $clamped day${if (clamped < -1) "s" else ""}"
            }
            showTransientBanner(label)
        }
    }

    fun updateAutoJamaahWindowMinutes(windowMinutes: Int) {
        val clamped = windowMinutes.coerceIn(0, 60)
        viewModelScope.launch {
            alarmDataStore.setAutoJamaahWindowMinutes(clamped)
            val label = if (clamped == 0) {
                "Jama'ah logging set to Manual Choice (Prayed in Jama'ah vs Individually)"
            } else {
                "Auto-record Prayed in Jama'ah when marked within ±${clamped}m of Jama'ah"
            }
            showTransientBanner(label)
        }
    }

    fun updateAutoSilenceDuringJamaah(enabled: Boolean) {
        viewModelScope.launch {
            alarmDataStore.setAutoSilenceDuringJamaah(enabled)
            PrayerNotificationHelper.setMasjidAutoSilenceEnabled(alarmScheduler.appContext, enabled)
            val msg = if (enabled) {
                "Masjid Auto-Silence enabled: Completely silent (no vibration) during 15m Jama'ah window"
            } else {
                "Masjid Auto-Silence disabled: Normal chimes during Jama'ah window"
            }
            showTransientBanner(msg)
        }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            alarmDataStore.setThemeMode(mode.name)
            showTransientBanner("Appearance: ${mode.displayName}")
        }
    }

    fun cycleThemeMode() {
        val next = uiState.value.themeMode.nextCycle()
        updateThemeMode(next)
    }

    fun updateHeroCardStyle(style: HeroCardStyle) {
        viewModelScope.launch {
            alarmDataStore.setHeroCardStyle(style.name)
            showTransientBanner("Hero style: ${style.displayName}")
        }
    }

    fun adjustQazaBacklogCount(delta: Int) {
        viewModelScope.launch {
            val state = uiState.value
            if (delta < 0) {
                // First check if late-night unprayed Isha or an unprayed Qaza prayer on today's sheet can be logged
                val featuredLateIsha = state.featuredPrayer?.takeIf {
                    state.isIshaMissedAfterMidnight && !it.isPrayed
                }
                val todayUnprayedQaza = featuredLateIsha ?: state.prayers.firstOrNull {
                    !it.isPrayed && it.completionStatus == PrayerCompletionStatus.MISSED_QADA
                }
                if (todayUnprayedQaza != null) {
                    val targetDate = todayUnprayedQaza.targetDateIso.ifBlank { currentEffectiveDateIso }
                    repository.markPrayerCompleted(targetDate, todayUnprayedQaza.id, isInJamaah = false)
                    showTransientBanner("${todayUnprayedQaza.nameEnglish} Qaza make-up logged ✓")
                    return@launch
                }
            }
            val minFloor = -state.autoDetectedQazaCount
            alarmDataStore.adjustManualQazaBacklog(delta, minFloor)
            val actionMsg = if (delta < 0) {
                "1 Qaza make-up prayer logged ✓"
            } else {
                "Added +$delta to Qaza Backlog"
            }
            showTransientBanner(actionMsg)
        }
    }

    fun updateHistoricalPrayerRecord(
        dateIso: String,
        prayerId: String,
        prayerName: String,
        isPrayed: Boolean,
        isInJamaah: Boolean,
        completionStatus: PrayerCompletionStatus
    ) {
        if (dateIso == currentEffectiveDateIso) {
            val matchingTodayPrayer = uiState.value.prayers.firstOrNull { it.id == prayerId }
            if (matchingTodayPrayer != null && !matchingTodayPrayer.canMarkPrayed && !matchingTodayPrayer.isPrayed) {
                val pstStr = PrayerTimeCalculator.formatMinutesTo12Hour(matchingTodayPrayer.pstMinutes)
                showTransientBanner("${matchingTodayPrayer.nameEnglish} unlocks at Prayer Start Time ($pstStr)")
                return
            }
        }
        viewModelScope.launch {
            repository.updateHistoricalPrayerStatus(
                dateIso = dateIso,
                prayerId = prayerId,
                isPrayed = isPrayed,
                isInJamaah = isInJamaah,
                completionStatus = completionStatus
            )
            if (dateIso == currentEffectiveDateIso) {
                if (isPrayed) {
                    alarmScheduler.cancelAllAlarmsForPrayer(prayerId)
                    PrayerNaggingForegroundService.stopNaggingLoop(alarmScheduler.appContext)
                }
                armDailyExactAlarmsForCity(_selectedCity.value)
            }
            val statusLabel = when {
                isPrayed && completionStatus == PrayerCompletionStatus.PRAYED_LATE -> "Prayed (Qaza Make-Up)"
                isPrayed && isInJamaah -> "Prayed in Jama'ah"
                isPrayed -> "Prayed Individually"
                else -> "Missed (Qaza)"
            }
            showTransientBanner("$dateIso • $prayerName updated: $statusLabel")
        }
    }

    fun setPrayerJamaahStatus(prayer: PrayerItemUiModel, isInJamaah: Boolean) {
        viewModelScope.launch {
            val dateIso = prayer.targetDateIso.ifBlank { currentEffectiveDateIso }
            repository.updatePrayerJamaahStatus(
                dateIso = dateIso,
                prayerId = prayer.id,
                isInJamaah = isInJamaah
            )
            val modeLabel = if (isInJamaah) "Prayed in Jama'ah 🕌" else "Prayed Individually 👤"
            showTransientBanner("${prayer.nameEnglish} updated: $modeLabel")
        }
    }

    fun togglePrayerCompletion(prayer: PrayerItemUiModel, isInJamaahOverride: Boolean? = null) {
        if (!prayer.isPrayed && !prayer.canMarkPrayed) {
            val pstStr = PrayerTimeCalculator.formatMinutesTo12Hour(prayer.pstMinutes)
            showTransientBanner("${prayer.nameEnglish} can only be marked after prayer starts ($pstStr)")
            return
        }
        viewModelScope.launch {
            val state = uiState.value
            val dateIso = prayer.targetDateIso.ifBlank { currentEffectiveDateIso }
            val willBePrayed = !prayer.isPrayed
            val resolvedJamaah = isInJamaahOverride ?: run {
                val autoWindow = state.autoJamaahWindowMinutes
                if (autoWindow > 0) {
                    kotlin.math.abs(state.currentClockMinutesNow - prayer.jamaahMinutes) <= autoWindow
                } else {
                    true
                }
            }
            repository.togglePrayerCompletion(dateIso, prayer.id, prayer.isPrayed, resolvedJamaah)
            if (willBePrayed) {
                alarmScheduler.cancelAllAlarmsForPrayer(prayer.id)
                PrayerNaggingForegroundService.stopNaggingLoop(alarmScheduler.appContext)
                alarmDataStore.clearNaggingLoop(
                    reason = "${prayer.nameEnglish} marked Prayed • Notifications & nagging stopped",
                    stageLabel = "PRAYED_ON_TIME"
                )
            }
            armDailyExactAlarmsForCity(_selectedCity.value)
            val actionText = if (willBePrayed) {
                if (prayer.completionStatus == PrayerCompletionStatus.MISSED_QADA) {
                    "${prayer.nameEnglish} Qaza make-up logged ✓"
                } else {
                    val jamaahSuffix = if (resolvedJamaah) "in Jama'ah" else "Individually"
                    "${prayer.nameEnglish} marked as prayed ($jamaahSuffix) ✓"
                }
            } else {
                "${prayer.nameEnglish} unmarked (Pending • Alerts re-armed)"
            }
            showTransientBanner(actionText)
        }
    }

    fun markFeaturedPrayerAsPrayed(isInJamaahOverride: Boolean? = null) {
        val state = uiState.value
        val targetPrayer = state.featuredPrayer ?: return

        if (!targetPrayer.isPrayed && !targetPrayer.canMarkPrayed) {
            val pstStr = PrayerTimeCalculator.formatMinutesTo12Hour(targetPrayer.pstMinutes)
            showTransientBanner("${targetPrayer.nameEnglish} can only be marked after prayer starts ($pstStr)")
            return
        }

        viewModelScope.launch {
            val dateIso = targetPrayer.targetDateIso.ifBlank { currentEffectiveDateIso }
            val resolvedJamaah = isInJamaahOverride ?: run {
                val autoWindow = state.autoJamaahWindowMinutes
                if (autoWindow > 0) {
                    kotlin.math.abs(state.currentClockMinutesNow - targetPrayer.jamaahMinutes) <= autoWindow
                } else {
                    true
                }
            }
            repository.markPrayerCompleted(dateIso, targetPrayer.id, resolvedJamaah)
            _snoozedExtraMinutes.value = 0
            alarmScheduler.cancelAllAlarmsForPrayer(targetPrayer.id)
            PrayerNaggingForegroundService.stopNaggingLoop(alarmScheduler.appContext)
            alarmDataStore.clearNaggingLoop(
                reason = "${targetPrayer.nameEnglish} marked as Prayed • Nagging loop stopped",
                stageLabel = "PRAYED_ON_TIME"
            )
            armDailyExactAlarmsForCity(_selectedCity.value)
            val jamaahSuffix = if (resolvedJamaah) "in Jama'ah" else "Individually"
            showTransientBanner("${targetPrayer.nameEnglish} marked as prayed ($jamaahSuffix) ✓")
        }
    }

    fun simulateNextPrayerAutoQadaRollover() {
        val featured = uiState.value.featuredPrayer ?: return
        viewModelScope.launch {
            val dateIso = featured.targetDateIso.ifBlank { currentEffectiveDateIso }
            repository.markPrayerAsMissedQada(dateIso, featured.id)
            alarmScheduler.cancelAllAlarmsForPrayer(featured.id)
            PrayerNaggingForegroundService.stopNaggingLoop(alarmScheduler.appContext)
            PrayerNotificationHelper.showDynamicPrayerNotification(
                context = alarmScheduler.appContext,
                prayerName = featured.nameEnglish,
                pstTimeFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(featured.pstMinutes),
                adhanTimeFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(featured.adhanMinutes),
                jamaahTimeFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(featured.jamaahMinutes),
                triggerType = AlarmTriggerType.STAGE_QAZA_ROLLOVER,
                minutesUntilJamaah = 0,
                minutesPassedSinceJamaah = 0,
                minutesRemainingUntilEnd = 0,
                progressPercent = 100,
                prayerId = featured.id,
                dateIso = dateIso,
                nagIteration = 0
            )
            alarmDataStore.clearNaggingLoop(
                reason = "Next prayer started: ${featured.nameEnglish} auto-marked as MISSED_QADA & Qaza notification posted",
                stageLabel = "AUTO_QADA_ROLLOVER"
            )
            armDailyExactAlarmsForCity(_selectedCity.value)
            showTransientBanner("${featured.nameEnglish} auto-marked as Qaza • Notification updated")
        }
    }

    fun snoozeFeaturedPrayerFiveMinutes() {
        val state = uiState.value
        if (!state.isSnoozeEnabled) {
            showTransientBanner("Snooze is only available within ${state.preJamaahAlertMinutes}m of Jama'ah or after Jama'ah")
            return
        }
        val featured = state.featuredPrayer ?: return
        val featuredName = featured.nameEnglish
        val featuredId = featured.id
        viewModelScope.launch {
            val cityTz = PrayerTimeCalculator.getTimeZoneForCity(state.selectedCity)
            val sheetStartCal = Calendar.getInstance(cityTz).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val sheetStartMs = sheetStartCal.timeInMillis
            val pstEpochMs = sheetStartMs + (featured.pstMinutes * 60_000L)
            val adhanEpochMs = sheetStartMs + (featured.adhanMinutes * 60_000L)
            val jamaahEpochMs = sheetStartMs + (featured.jamaahMinutes * 60_000L)
            val windowEndEpochMs = sheetStartMs + (featured.endMinutes * 60_000L)

            val nextEpoch = System.currentTimeMillis() + (5 * 60 * 1000L)
            val snoozedStage = if (nextEpoch < jamaahEpochMs) {
                AlarmTriggerType.STAGE_PRE_JAMAAH
            } else {
                AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG
            }
            val minUntilJamaahAtSnooze = ((jamaahEpochMs - nextEpoch + 59_999L) / 60_000L).toInt().coerceAtLeast(0)

            alarmScheduler.scheduleExactTrigger(
                prayerId = featuredId,
                prayerName = featuredName,
                dateIso = currentEffectiveDateIso,
                triggerType = snoozedStage,
                triggerAtEpochMillis = nextEpoch,
                pstTimeFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(featured.pstMinutes),
                adhanTimeFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(featured.adhanMinutes),
                jamaahTimeFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(featured.jamaahMinutes),
                pstEpochMillis = pstEpochMs,
                adhanEpochMillis = adhanEpochMs,
                jamaahEpochMillis = jamaahEpochMs,
                windowEndEpochMillis = windowEndEpochMs,
                minutesUntilJamaah = minUntilJamaahAtSnooze,
                nagIteration = 1
            )
            PrayerNaggingForegroundService.stopNaggingLoop(alarmScheduler.appContext)
            alarmDataStore.recordSnoozeFiveMinutes(featuredId, nextEpoch)
            showTransientBanner("$featuredName reminder snoozed for 5m (Jama'ah stays ${PrayerTimeCalculator.formatMinutesTo12Hour(featured.jamaahMinutes)})")
        }
    }

    fun openJamaahEditor(prayer: PrayerItemUiModel, initialTab: String = "JAMAAH") {
        _editingInitialTab.value = initialTab
        _editingJamaahPrayer.value = prayer
    }

    fun dismissJamaahEditor() {
        _editingJamaahPrayer.value = null
    }

    fun savePrayerTimeSettings(
        prayerId: String,
        adhanOffsetMinutes: Int,
        useFixedAdhanTime: Boolean,
        fixedAdhanTimeMinutes: Int,
        jamaahOffsetMinutes: Int,
        useFixedJamaahTime: Boolean,
        fixedJamaahTimeMinutes: Int
    ) {
        viewModelScope.launch {
            repository.updatePrayerTimeSettings(
                prayerId = prayerId,
                adhanOffsetMinutes = adhanOffsetMinutes,
                useFixedAdhanTime = useFixedAdhanTime,
                fixedAdhanTimeMinutes = fixedAdhanTimeMinutes,
                jamaahOffsetMinutes = jamaahOffsetMinutes,
                useFixedJamaahTime = useFixedJamaahTime,
                fixedJamaahTimeMinutes = fixedJamaahTimeMinutes
            )
            _editingJamaahPrayer.value = null
            armDailyExactAlarmsForCity(_selectedCity.value)

            val adhanSummary = if (useFixedAdhanTime) {
                "Adhan ${PrayerTimeCalculator.formatMinutesTo12Hour(fixedAdhanTimeMinutes)}"
            } else {
                val sign = if (adhanOffsetMinutes >= 0) "+$adhanOffsetMinutes" else "$adhanOffsetMinutes"
                "Adhan ${sign}m from PST"
            }
            val jamaahSummary = if (useFixedJamaahTime) {
                "Jama'ah ${PrayerTimeCalculator.formatMinutesTo12Hour(fixedJamaahTimeMinutes)}"
            } else {
                val sign = if (jamaahOffsetMinutes >= 0) "+$jamaahOffsetMinutes" else "$jamaahOffsetMinutes"
                "Jama'ah ${sign}m from Adhan"
            }
            showTransientBanner("$prayerId updated: $adhanSummary • $jamaahSummary")
        }
    }

    fun saveJamaahOffset(
        prayerId: String,
        newOffsetMinutes: Int,
        useFixedTime: Boolean = false,
        fixedTimeMinutes: Int = 795
    ) {
        viewModelScope.launch {
            repository.updateJamaahOffset(
                prayerId = prayerId,
                offsetMinutes = newOffsetMinutes,
                useFixedTime = useFixedTime,
                fixedTimeMinutes = fixedTimeMinutes
            )
            _editingJamaahPrayer.value = null
            armDailyExactAlarmsForCity(_selectedCity.value)
            val signPrefix = if (newOffsetMinutes >= 0) "+$newOffsetMinutes" else "$newOffsetMinutes"
            val fixedFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(fixedTimeMinutes)
            val message = if (useFixedTime) {
                "$prayerId Jama'ah time set to $fixedFormatted"
            } else {
                "$prayerId Jama'ah offset updated to ${signPrefix}m from Adhan"
            }
            showTransientBanner(message)
        }
    }

    fun updateNotificationRepeatInterval(
        nagIntervalMinutes: Int,
        urgentEndWindowMinutes: Int = uiState.value.urgentEndWindowMinutes,
        preAdhanAlertMinutes: Int = uiState.value.preAdhanAlertMinutes,
        preJamaahAlertMinutes: Int = uiState.value.preJamaahAlertMinutes
    ) {
        viewModelScope.launch {
            repository.updateNotificationIntervals(
                nagIntervalMinutes = nagIntervalMinutes,
                urgentEndWindowMinutes = urgentEndWindowMinutes,
                preAdhanAlertMinutes = preAdhanAlertMinutes,
                preJamaahAlertMinutes = preJamaahAlertMinutes
            )
            armDailyExactAlarmsForCity(_selectedCity.value)
            showTransientBanner(
                "Alerts updated: Pre-Adhan ${preAdhanAlertMinutes}m • Pre-Jama'ah ${preJamaahAlertMinutes}m • Repeat every ${nagIntervalMinutes}m"
            )
        }
    }

    fun stepAthkaarCounter(habit: AthkaarHabitUiModel) {
        viewModelScope.launch {
            val cityTz = PrayerTimeCalculator.getTimeZoneForCity(_selectedCity.value)
            val clockFmt = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = cityTz }
            val nowFormatted = clockFmt.format(Calendar.getInstance(cityTz).time)
            repository.incrementAthkaar(
                dateIso = currentEffectiveDateIso,
                athkaarId = habit.id,
                currentCount = habit.currentCount,
                targetCount = habit.targetCount,
                step = 11,
                timeFormatted = nowFormatted
            )
            if (habit.id == "BEDTIME" && habit.currentCount + 11 >= habit.targetCount) {
                showTransientBanner("Bedtime Athkaar complete • Sleep time logged at $nowFormatted")
            }
        }
    }

    fun toggleAthkaarCompletion(habit: AthkaarHabitUiModel) {
        viewModelScope.launch {
            val cityTz = PrayerTimeCalculator.getTimeZoneForCity(_selectedCity.value)
            val clockFmt = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = cityTz }
            val nowFormatted = clockFmt.format(Calendar.getInstance(cityTz).time)
            repository.toggleAthkaarComplete(
                dateIso = currentEffectiveDateIso,
                athkaarId = habit.id,
                currentlyCompleted = habit.isCompleted,
                targetCount = habit.targetCount,
                timeFormatted = nowFormatted
            )
            if (!habit.isCompleted && habit.isBedtimeTracker) {
                showTransientBanner("Bedtime Athkaar complete • Bedtime logged at $nowFormatted")
            }
        }
    }

    fun showTransientBanner(message: String) {
        bannerJob?.cancel()
        bannerJob = viewModelScope.launch {
            _statusBannerMessage.value = message
            delay(3400L)
            if (_statusBannerMessage.value == message) {
                _statusBannerMessage.value = null
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: PrayerRepository,
            alarmDataStore: PrayerAlarmDataStore,
            alarmScheduler: ExactPrayerAlarmScheduler,
            locationManager: LocationResolutionManager
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PrayerHabitViewModel(
                        repository = repository,
                        alarmDataStore = alarmDataStore,
                        alarmScheduler = alarmScheduler,
                        locationManager = locationManager
                    ) as T
                }
            }
    }
}
