package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AlarmLoopRuntimeState
import com.example.data.AthkaarHabitUiModel
import com.example.data.AthkaarLogEntity
import com.example.data.DayContributionCell
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
    val statusBannerMessage: String? = null
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

    @Volatile
    private var currentEffectiveDateIso: String = computeEffectiveDateIso(defaultInitialCity)

    @Volatile
    private var lastFiredLiveMilestoneKey: String = ""

    init {
        viewModelScope.launch {
            repository.ensureInitialized(currentEffectiveDateIso)
            val savedCityPair = repository.getSavedSelectedCity()
            if (savedCityPair != null) {
                val (savedCity, isGps) = savedCityPair
                _selectedCity.value = savedCity
                _locationSourceStatus.value = if (isGps) {
                    "GPS • ${savedCity.name}"
                } else {
                    "${savedCity.name} • MWL"
                }
                currentEffectiveDateIso = computeEffectiveDateIso(savedCity)
                armDailyExactAlarmsForCity(savedCity)
            } else {
                armDailyExactAlarmsForCity(_selectedCity.value)
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
     * Right at Islamic Midnight (end of Isha), the sheet switches to the upcoming day's
     * Fajr-to-Isha schedule so that Fajr is tracked next and all 5 prayers are upcoming.
     */
    private fun computeEffectiveDateIso(city: CityLocation): String {
        val cityTz = PrayerTimeCalculator.getTimeZoneForCity(city)
        val nowCal = Calendar.getInstance(cityTz)
        val nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
        val calc = PrayerTimeCalculator.calculateDailyPrayers(city, nowCal)
        if (calc.islamicMidnightMinutes < 1440 && nowMinutes >= calc.islamicMidnightMinutes) {
            nowCal.add(Calendar.DAY_OF_YEAR, 1)
        } else if (calc.islamicMidnightMinutes > 1440 && nowMinutes < (calc.islamicMidnightMinutes - 1440)) {
            nowCal.add(Calendar.DAY_OF_YEAR, -1)
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
            String.format(Locale.US, "%dh %02dm", hours, mins)
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

        // Advance to the next day's Fajr-to-Isha sheet right at Islamic Midnight (end of Isha)
        val activeSheetCal = (nowCal.clone() as Calendar)
        val effectiveNowMinutes: Int
        if (initialCalc.islamicMidnightMinutes < 1440 && nowTotalMinutes >= initialCalc.islamicMidnightMinutes) {
            // Between Islamic Midnight (e.g. 11:28 PM) and 11:59 PM -> switch to tomorrow's sheet
            activeSheetCal.add(Calendar.DAY_OF_YEAR, 1)
            effectiveNowMinutes = nowTotalMinutes - 1440
        } else if (initialCalc.islamicMidnightMinutes > 1440 && nowTotalMinutes < (initialCalc.islamicMidnightMinutes - 1440)) {
            // High-latitude edge case where Islamic Midnight is slightly after 12:00 AM and Isha is still active
            activeSheetCal.add(Calendar.DAY_OF_YEAR, -1)
            effectiveNowMinutes = nowTotalMinutes + 1440
        } else {
            // Normal daytime OR after 12:00 AM before Fajr -> active sheet is today's date
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
                fixedAdhanMin.coerceIn(0, 1439)
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

            if (isWindowExpiredUnprayed && !isMissedQadaStatus) {
                viewModelScope.launch {
                    repository.markPrayerAsMissedQada(effectiveDateIso, id)
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
                isCurrent = isActiveWindowUnprayed && id == activeWindowId,
                isNext = !isActiveWindowUnprayed && id == nextUpcomingId,
                canMarkPrayed = canMarkPrayedNow,
                completionStatus = status,
                nagCountTriggered = rec?.nagCountTriggered ?: 0,
                settingsKey = settingsKey,
                isJumuah = (settingsKey == "JUMUAH"),
                endMinutes = strictEndMin,
                displayEndMinutes = displayEndMinutesMap[id] ?: strictEndMin
            )
        }

        val isIshaMissedPastMidnight = false

        val featured = prayerItems.firstOrNull { it.id == featuredId } ?: prayerItems.first()
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

            progressFraction = (elapsedSincePstSec.toFloat() / totalWindowSec.toFloat()).coerceIn(0f, 1f)
            val pctElapsed = (progressFraction * 100).toInt()
            progressRightLabel = "$pctElapsed% elapsed"

            remainingUntilEndMin = (remainingWindowSec + 59) / 60
            minutesToNextPrayer = remainingUntilEndMin

            if (effectiveNowSeconds >= postJamaah10mSec) {
                // Phase 4: 10+ minutes have passed after Jama'ah time (and still unticked)
                // -> Left shows JAMA'AH TIME, Right shows "<Xm Ys / Xh Ym> elapsed since Jama'ah"
                isJamaahPassed = true
                val elapsedJamaahSec = (effectiveNowSeconds - jamaahSec).coerceAtLeast(0)
                elapsedSinceJamaahMin = (elapsedJamaahSec / 60).coerceAtLeast(10)
                elapsedJamaahFormatted = formatHoursMinutesSeconds(elapsedJamaahSec)
                minutesToJamaah = 0
                isSnoozeAllowedNow = true
                lockScreenStageTime = "Ends at $featuredDisplayEndFormatted"

                val isUrgentEnd = remainingUntilEndMin <= urgentThresholdMin
                heroHeaderLabel = "CURRENT PRAYER"
                jamaahBadgeText = "$elapsedJamaahFormatted elapsed since Jama'ah"
                heroJamaahPill = "$jamaahLabelPrefix $featuredJamaahFormatted"
                heroLeftLabel = "JAMA'AH TIME"
                heroLeftValue = featuredJamaahFormatted
                heroCountdownSubtitleText = "$elapsedJamaahFormatted elapsed since Jama'ah"
                primaryRightTimer = "$elapsedJamaahFormatted elapsed since Jama'ah"
                progressLeftLabel = if (isUrgentEnd) {
                    "URGENT: ${remainingUntilEndMin}m remaining until ${featured.nameEnglish} ends"
                } else {
                    "$elapsedJamaahFormatted elapsed since Jama'ah • ${remainingUntilEndMin}m left"
                }
            } else if (effectiveNowSeconds >= jamaahSec) {
                // Between Jama'ah time and +10m post-Jama'ah
                isJamaahPassed = true
                val elapsedJamaahSec = (effectiveNowSeconds - jamaahSec).coerceAtLeast(0)
                elapsedSinceJamaahMin = (elapsedJamaahSec / 60).coerceAtLeast(1)
                elapsedJamaahFormatted = formatHoursMinutesSeconds(elapsedJamaahSec)
                minutesToJamaah = 0
                isSnoozeAllowedNow = true
                lockScreenStageTime = "Ends at $featuredDisplayEndFormatted"

                heroHeaderLabel = "CURRENT PRAYER"
                jamaahBadgeText = "$elapsedJamaahFormatted elapsed since Jama'ah"
                heroJamaahPill = "$jamaahLabelPrefix $featuredJamaahFormatted"
                heroLeftLabel = "JAMA'AH TIME"
                heroLeftValue = featuredJamaahFormatted
                heroCountdownSubtitleText = "$elapsedJamaahFormatted elapsed since Jama'ah"
                primaryRightTimer = "$elapsedJamaahFormatted elapsed since Jama'ah"
                progressLeftLabel = "Jama'ah started at $featuredJamaahFormatted ($elapsedJamaahFormatted elapsed)"
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
                lockScreenStageTime = "Adhan $featuredAdhanFormatted"

                heroHeaderLabel = "CURRENT PRAYER"
                jamaahBadgeText = "Adhan in $untilAdhanTimer"
                heroJamaahPill = "$jamaahLabelPrefix $featuredJamaahFormatted"
                heroLeftLabel = "ADHAN TIME"
                heroLeftValue = featuredAdhanFormatted
                heroCountdownSubtitleText = "Adhan in $untilAdhanTimer"
                primaryRightTimer = "Adhan in $untilAdhanTimer"
                progressLeftLabel = "$untilAdhanTimer remaining before Adhan"
            } else {
                // Phase 3: Adhan time is done, waiting for Jama'ah
                // -> Left shows ADHAN TIME for the first 3 minutes after Adhan, then switches to JAMA'AH TIME
                // -> Right says "Jama'ah in Xm Ys"
                isJamaahPassed = false
                elapsedSinceJamaahMin = 0
                elapsedJamaahFormatted = "0m 00s"
                val untilJamaahSec = (jamaahSec - effectiveNowSeconds).coerceAtLeast(0)
                minutesToJamaah = (untilJamaahSec + 59) / 60
                isSnoozeAllowedNow = minutesToJamaah <= preJamaahThresholdMin
                val untilJamaahTimer = formatHoursMinutesSeconds(untilJamaahSec)
                val postAdhanSwitchSec = adhanSec + (3 * 60)
                val showJamaahOnLeft = effectiveNowSeconds >= postAdhanSwitchSec
                lockScreenStageTime = "$jamaahLabelPrefix $featuredJamaahFormatted"

                heroHeaderLabel = "CURRENT PRAYER"
                jamaahBadgeText = "${minutesToJamaah}m until Jama'ah"
                heroJamaahPill = "$jamaahLabelPrefix $featuredJamaahFormatted"
                heroLeftLabel = if (showJamaahOnLeft) "JAMA'AH TIME" else "ADHAN TIME"
                heroLeftValue = if (showJamaahOnLeft) featuredJamaahFormatted else featuredAdhanFormatted
                heroCountdownSubtitleText = "Jama'ah in $untilJamaahTimer"
                primaryRightTimer = "Jama'ah in $untilJamaahTimer"
                progressLeftLabel = "$untilJamaahTimer remaining before Jama'ah"
            }
        } else {
            // Phase 1: Previous prayer is done (or between windows) -> waiting for next prayer's start time (PST)
            // -> Left shows "STARTS AT" with PST time, Right says ONLY "Next prayer in Xm Ys"
            isJamaahPassed = false
            elapsedSinceJamaahMin = 0
            elapsedJamaahFormatted = "0m 00s"
            isSnoozeAllowedNow = false
            lockScreenStageTime = "Starts at $featuredPstFormatted"

            val nextPstMin = if (featured.id == "FAJR" && effectiveNowMinutes >= ishaMin) {
                featured.pstMinutes + 1440
            } else {
                featured.pstMinutes
            }
            val nextJamaahMin = if (featured.id == "FAJR" && effectiveNowMinutes >= ishaMin) {
                featured.jamaahMinutes + 1440
            } else {
                featured.jamaahMinutes
            }
            val prevTransitionMin = when {
                featured.id == "FAJR" && effectiveNowMinutes < fajrMin -> islamicMidnightMin - 1440
                featured.id == "FAJR" -> ishaMin
                featured.id == "DHUHR" -> fajrMin
                featured.id == "ASR" -> dhuhrMin
                featured.id == "MAGHRIB" -> asrMin
                featured.id == "ISHA" -> maghribMin
                else -> fajrMin
            }

            windowEndFormatted = featuredAdhanFormatted
            val nextPstSec = nextPstMin * 60
            val nextJamaahSec = nextJamaahMin * 60
            val prevTransitionSec = prevTransitionMin * 60

            val untilNextPstSec = (nextPstSec - effectiveNowSeconds).coerceAtLeast(0)
            val untilJamaahSec = (nextJamaahSec - effectiveNowSeconds).coerceAtLeast(0)

            val intervalTotalSec = (nextPstSec - prevTransitionSec).coerceAtLeast(60)
            val intervalElapsedSec = (effectiveNowSeconds - prevTransitionSec).coerceAtLeast(0)

            progressFraction = (intervalElapsedSec.toFloat() / intervalTotalSec.toFloat()).coerceIn(0f, 1f)
            val pctElapsed = (progressFraction * 100).toInt()
            progressRightLabel = "$pctElapsed% elapsed"

            minutesToNextPrayer = (untilNextPstSec + 59) / 60
            minutesToJamaah = (untilJamaahSec + 59) / 60
            remainingUntilEndMin = minutesToNextPrayer

            val untilNextPstWithSec = formatHoursMinutesSeconds(untilNextPstSec)

            heroHeaderLabel = if (allTodayPrayed) "ALL 5 PRAYED ✓" else "NEXT PRAYER"

            jamaahBadgeText = "Next prayer in $untilNextPstWithSec"
            heroJamaahPill = "$jamaahLabelPrefix $featuredJamaahFormatted"
            heroLeftLabel = "STARTS AT"
            heroLeftValue = featuredPstFormatted
            heroCountdownSubtitleText = "Next prayer in $untilNextPstWithSec"
            primaryRightTimer = "Next prayer in $untilNextPstWithSec"
            progressLeftLabel = "$untilNextPstWithSec remaining until ${featured.nameEnglish}"
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

        val morningLog = todayAthkaarMap["MORNING"]
        val eveningLog = todayAthkaarMap["EVENING"]
        val bedtimeLog = todayAthkaarMap["BEDTIME"]

        val athkaarModels = listOf(
            AthkaarHabitUiModel(
                id = "MORNING",
                title = "Morning Athkaar",
                arabicSubtitle = "أذكار الصباح",
                description = "Post-Fajr protection & gratitude remembrances",
                currentCount = morningLog?.currentCount ?: 33,
                targetCount = morningLog?.targetCount ?: 33,
                isCompleted = morningLog?.isCompleted ?: true,
                completedTimeFormatted = morningLog?.completedTimeFormatted ?: "06:15 AM"
            ),
            AthkaarHabitUiModel(
                id = "EVENING",
                title = "Evening Athkaar",
                arabicSubtitle = "أذكار المساء",
                description = "Post-Asr serenity & mindfulness tasbih",
                currentCount = eveningLog?.currentCount ?: 18,
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
                completedTimeFormatted = bedtimeLog?.completedTimeFormatted ?: "10:45 PM (Prev)",
                isBedtimeTracker = true
            )
        )

        val todayQadaCount = prayerItems.count { it.completionStatus == PrayerCompletionStatus.MISSED_QADA }
        val todayAthkaarDone = athkaarModels.count { it.isCompleted }
        val totalTodayGoals = (todayPrayersDone + todayAthkaarDone) / 8f

        val recordsByDate = allPrayerRecords.groupBy { it.dateIso }
        val athkaarByDate = allAthkaar.groupBy { it.dateIso }
        val dayNameFormat = SimpleDateFormat("EEE", Locale.US).apply { timeZone = cityTz }

        val gridCells = (48 downTo 0).map { daysAgo ->
            val c = activeSheetCal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val iso = dateFormatter.format(c.time)
            val pDone = if (iso == effectiveDateIso) {
                todayPrayersDone
            } else {
                recordsByDate[iso]?.count { it.isPrayed } ?: 4
            }
            val aDone = if (iso == effectiveDateIso) {
                todayAthkaarDone
            } else {
                athkaarByDate[iso]?.count { it.isCompleted } ?: 2
            }
            val qada = if (iso == effectiveDateIso) {
                todayQadaCount
            } else {
                recordsByDate[iso]?.count {
                    it.completionStatus == PrayerCompletionStatus.MISSED_QADA.name
                } ?: 0
            }
            DayContributionCell(
                dateIso = iso,
                dayOfWeekShort = dayNameFormat.format(c.time).take(1),
                dayOfMonth = c.get(Calendar.DAY_OF_MONTH),
                prayersCompleted = pDone,
                athkaarCompleted = aDone,
                qadaCount = qada,
                isToday = iso == effectiveDateIso
            )
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
            hijriDateText = PrayerTimeCalculator.getApproximateHijriDateString(nowCal),
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
            statusBannerMessage = ctrl.bannerMessage
        )
    }

    /**
     * Synchronizes real-time exact alarms with the current day's prayer sheet, user-configured
     * PST / Adhan / Jama'ah times, and live prayer completion states (ticked vs unticked).
     * Never fires simulated post-launch test notifications.
     */
    private fun armDailyExactAlarmsForCity(city: CityLocation) {
        viewModelScope.launch {
            val cityTz = PrayerTimeCalculator.getTimeZoneForCity(city)
            val nowCal = Calendar.getInstance(cityTz)
            val nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
            val initialCalc = PrayerTimeCalculator.calculateDailyPrayers(city, nowCal)

            val activeSheetCal = (nowCal.clone() as Calendar)
            if (initialCalc.islamicMidnightMinutes < 1440 && nowMinutes >= initialCalc.islamicMidnightMinutes) {
                activeSheetCal.add(Calendar.DAY_OF_YEAR, 1)
            } else if (initialCalc.islamicMidnightMinutes > 1440 && nowMinutes < (initialCalc.islamicMidnightMinutes - 1440)) {
                activeSheetCal.add(Calendar.DAY_OF_YEAR, -1)
            }

            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = cityTz }
            val sheetIso = dateFormatter.format(activeSheetCal.time)
            currentEffectiveDateIso = sheetIso

            val sheetStartCal = (activeSheetCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val sheetStartEpochMs = sheetStartCal.timeInMillis
            val isFriday = activeSheetCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

            val calc = PrayerTimeCalculator.calculateDailyPrayers(city, activeSheetCal)
            val settingsMap = repository.allPrayerSettings.first().associateBy { it.prayerId }
            val todayRecordsMap = repository.allPrayerRecords.first()
                .filter { it.dateIso == sheetIso }
                .associateBy { it.prayerId }
            val prefs = repository.userPreferences.first()
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
            var nextArmedSummary: String? = null
            var nextArmedEpochMs = 0L

            alarmScheduler.cancelAllDailyPrayerAlarms()

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
                    (setting?.fixedAdhanTimeMinutes ?: defaultFixedAdhanMin).coerceIn(0, 1439)
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

                val pstStr = PrayerTimeCalculator.formatMinutesTo12Hour(pstMin)
                val adhanStr = PrayerTimeCalculator.formatMinutesTo12Hour(adhanMin)
                val jamaahStr = PrayerTimeCalculator.formatMinutesTo12Hour(jamaahMin)
                val urgentBeforeEndMin = 15
                val urgentBeforeEndEpochMs = windowEndEpochMs - (urgentBeforeEndMin * 60_000L)

                val milestones = buildList {
                    // Trigger 1: Natural Prayer Start Time (PST)
                    add(AlarmTriggerType.STAGE_1_PST_START to pstEpochMs)
                    if (preAdhanMin > 0 && preAdhanEpochMs > pstEpochMs) {
                        add(AlarmTriggerType.STAGE_PRE_ADHAN to preAdhanEpochMs)
                    }
                    // Trigger 2: Mosque Adhan Time
                    if (adhanEpochMs > pstEpochMs) {
                        add(AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN to adhanEpochMs)
                    }
                    if (preJamaahMin > 0 && preJamaahEpochMs > adhanEpochMs) {
                        add(AlarmTriggerType.STAGE_PRE_JAMAAH to preJamaahEpochMs)
                    }
                    // Trigger 3: Mosque Jama'ah Time
                    if (jamaahEpochMs > adhanEpochMs) {
                        add(AlarmTriggerType.STAGE_JAMAAH_START to jamaahEpochMs)
                    }
                    // Trigger 4: 10m Post-Jama'ah (and periodic repeats)
                    add(AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG to firstPostJamaahNagEpochMs)
                    // Trigger 5: 15 minutes before prayer window ends if still unprayed
                    if (urgentBeforeEndEpochMs > jamaahEpochMs) {
                        add(AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW to urgentBeforeEndEpochMs)
                    }
                }.sortedBy { it.second }

                val upcomingMilestone = milestones.firstOrNull { it.second > nowMs && it.second < windowEndEpochMs }

                val stageToSchedule: AlarmTriggerType
                val triggerEpochMs: Long

                if (upcomingMilestone != null) {
                    stageToSchedule = upcomingMilestone.first
                    triggerEpochMs = upcomingMilestone.second
                } else {
                    val remMin = ((windowEndEpochMs - nowMs + 59_999L) / 60_000L).toInt()
                    stageToSchedule = if (remMin <= urgentThresholdMin) {
                        AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW
                    } else {
                        AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG
                    }
                    val elapsedSinceFirstNagMs = (nowMs - firstPostJamaahNagEpochMs).coerceAtLeast(0L)
                    val intervalMs = nagIntervalMin * 60_000L
                    val nextGridTickMs = firstPostJamaahNagEpochMs + (((elapsedSinceFirstNagMs / intervalMs) + 1) * intervalMs)
                    triggerEpochMs = nextGridTickMs.coerceAtMost(windowEndEpochMs - 30_000L)
                }

                val minUntilJamaahAtTrigger = ((jamaahEpochMs - triggerEpochMs + 59_999L) / 60_000L).toInt().coerceAtLeast(0)

                if (triggerEpochMs > nowMs && triggerEpochMs < windowEndEpochMs) {
                    alarmScheduler.scheduleExactTrigger(
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
                    armDailyExactAlarmsForCity(res.cityLocation)
                    showTransientBanner("GPS Locked: ${res.cityLocation.name} (${String.format(Locale.US, "%.2f", res.rawLatitude)}°, ${String.format(Locale.US, "%.2f", res.rawLongitude)}°)")
                }
                is LocationResolutionResult.FallbackSqliteCity -> {
                    _selectedCity.value = res.cityLocation
                    _locationSourceStatus.value = "${res.cityLocation.name} • MWL"
                    currentEffectiveDateIso = computeEffectiveDateIso(res.cityLocation)
                    repository.updateSelectedLocation(res.cityLocation, isGps = false)
                    armDailyExactAlarmsForCity(res.cityLocation)
                    showTransientBanner(res.reason)
                }
            }
        }
    }

    fun togglePrayerCompletion(prayer: PrayerItemUiModel) {
        if (!prayer.isPrayed && !prayer.canMarkPrayed) {
            val pstStr = PrayerTimeCalculator.formatMinutesTo12Hour(prayer.pstMinutes)
            showTransientBanner("${prayer.nameEnglish} can only be marked after prayer starts ($pstStr)")
            return
        }
        viewModelScope.launch {
            val dateIso = currentEffectiveDateIso
            val willBePrayed = !prayer.isPrayed
            repository.togglePrayerCompletion(dateIso, prayer.id, prayer.isPrayed)
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
                    "${prayer.nameEnglish} marked as prayed ✓ (Alerts stopped)"
                }
            } else {
                "${prayer.nameEnglish} unmarked (Pending • Alerts re-armed)"
            }
            showTransientBanner(actionText)
        }
    }

    fun markFeaturedPrayerAsPrayed() {
        val state = uiState.value
        val targetPrayer = if (state.isIshaMissedAfterMidnight) {
            state.prayers.firstOrNull { it.id == "ISHA" } ?: state.featuredPrayer
        } else {
            state.featuredPrayer
        } ?: return

        if (!targetPrayer.isPrayed && !targetPrayer.canMarkPrayed) {
            val pstStr = PrayerTimeCalculator.formatMinutesTo12Hour(targetPrayer.pstMinutes)
            showTransientBanner("${targetPrayer.nameEnglish} can only be marked after prayer starts ($pstStr)")
            return
        }

        viewModelScope.launch {
            val dateIso = currentEffectiveDateIso
            repository.markPrayerCompleted(dateIso, targetPrayer.id)
            _snoozedExtraMinutes.value = 0
            alarmScheduler.cancelAllAlarmsForPrayer(targetPrayer.id)
            PrayerNaggingForegroundService.stopNaggingLoop(alarmScheduler.appContext)
            alarmDataStore.clearNaggingLoop(
                reason = "${targetPrayer.nameEnglish} marked as Prayed • Nagging loop stopped",
                stageLabel = "PRAYED_ON_TIME"
            )
            armDailyExactAlarmsForCity(_selectedCity.value)
            showTransientBanner("${targetPrayer.nameEnglish} marked as prayed ✓")
        }
    }

    fun simulateNextPrayerAutoQadaRollover() {
        val featured = uiState.value.featuredPrayer ?: return
        viewModelScope.launch {
            val dateIso = currentEffectiveDateIso
            repository.markPrayerAsMissedQada(dateIso, featured.id)
            alarmScheduler.cancelAllAlarmsForPrayer(featured.id)
            PrayerNaggingForegroundService.stopNaggingLoop(alarmScheduler.appContext)
            alarmDataStore.clearNaggingLoop(
                reason = "Next prayer started: ${featured.nameEnglish} auto-marked as MISSED_QADA & nag stopped",
                stageLabel = "AUTO_QADA_ROLLOVER"
            )
            armDailyExactAlarmsForCity(_selectedCity.value)
            showTransientBanner("${featured.nameEnglish} auto-marked as Missed (Qada) on next prayer rollover")
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
        viewModelScope.launch {
            _statusBannerMessage.value = message
            delay(3200L)
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
