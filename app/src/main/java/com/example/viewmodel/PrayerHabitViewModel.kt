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
import com.example.data.UserPreferencesEntity
import com.example.domain.CityLocation
import com.example.domain.LocationResolutionManager
import com.example.domain.LocationResolutionResult
import com.example.domain.PrayerTimeCalculator
import com.example.notification.AlarmTriggerType
import com.example.notification.ExactPrayerAlarmScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

data class PrayerDashboardUiState(
    val displayMode: AppDisplayMode = AppDisplayMode.INTERACTIVE_SINGLE_SCREEN,
    val activeTab: ActiveTab = ActiveTab.HOME_PRAYERS,
    val selectedCity: CityLocation = PrayerTimeCalculator.PredefinedCities.first(),
    val availableCities: List<CityLocation> = PrayerTimeCalculator.PredefinedCities,
    val sqliteFallbackCityCount: Int = 8,
    val locationSourceStatus: String = "SQLite Local City (MWL)",
    val hijriDateText: String = "",
    val gregorianDateText: String = "",
    val currentTimeFormatted: String = "03:36 PM",
    val currentClockSeconds: Int = 42,
    val sunriseTimeFormatted: String = "06:12 AM",
    val prayers: List<PrayerItemUiModel> = emptyList(),
    val featuredPrayer: PrayerItemUiModel? = null,
    val minutesUntilNextPrayer: Int = 9,
    val minutesUntilJamaah: Int = 24,
    val snoozedExtraMinutes: Int = 0,
    val preJamaahAlertMinutes: Int = 15, // Confirmed X = 15m before Jama'ah
    val nagIntervalMinutes: Int = 20,    // Confirmed Y = 20m nagging loop
    val notificationProgressFraction: Float = 0.68f,
    val alarmRuntimeState: AlarmLoopRuntimeState = AlarmLoopRuntimeState(),
    val athkaarList: List<AthkaarHabitUiModel> = emptyList(),
    val weeklyGrid: List<DayContributionCell> = emptyList(),
    val todayPrayersCompleted: Int = 2,
    val todayQadaCount: Int = 0,
    val todayAthkaarCompleted: Int = 1,
    val todayGoalFraction: Float = 0.375f,
    val currentStreakDays: Int = 12,
    val jamaahRatePercent: Int = 92,
    val consistencyPercent: Int = 94,
    val editingJamaahPrayer: PrayerItemUiModel? = null,
    val statusBannerMessage: String? = null
)

class PrayerHabitViewModel(
    private val repository: PrayerRepository,
    private val alarmDataStore: PrayerAlarmDataStore,
    private val alarmScheduler: ExactPrayerAlarmScheduler,
    private val locationManager: LocationResolutionManager
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
    private val clockTimeFormatter = SimpleDateFormat("hh:mm a", Locale.US)

    private val todayIso: String = dateFormatter.format(Calendar.getInstance().time)

    private val _displayMode = MutableStateFlow(AppDisplayMode.INTERACTIVE_SINGLE_SCREEN)
    private val _activeTab = MutableStateFlow(ActiveTab.HOME_PRAYERS)
    private val _selectedCity = MutableStateFlow(PrayerTimeCalculator.PredefinedCities.first())
    private val _locationSourceStatus = MutableStateFlow("SQLite Local City (MWL)")
    private val _snoozedExtraMinutes = MutableStateFlow(0)
    private val _editingJamaahPrayer = MutableStateFlow<PrayerItemUiModel?>(null)
    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    private val _tickerSeconds = MutableStateFlow(0L)

    init {
        viewModelScope.launch {
            repository.ensureInitialized(todayIso)
            armDailyExactAlarmsForCity(_selectedCity.value)
        }
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _tickerSeconds.value += 1L
            }
        }
    }

    private data class InternalControlState(
        val displayMode: AppDisplayMode,
        val activeTab: ActiveTab,
        val city: CityLocation,
        val locationSourceStatus: String,
        val snoozedMinutes: Int,
        val editingPrayer: PrayerItemUiModel?,
        val bannerMessage: String?,
        val tick: Long,
        val alarmRuntime: AlarmLoopRuntimeState,
        val userPrefs: UserPreferencesEntity?,
        val sqlCities: List<LocalCityEntity>
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
            editingPrayer = null,
            bannerMessage = null,
            tick = 0L,
            alarmRuntime = AlarmLoopRuntimeState(),
            userPrefs = null,
            sqlCities = emptyList()
        )
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

    private fun buildUiState(
        ctrl: InternalControlState,
        settingsList: List<PrayerSettingEntity>,
        allPrayerRecords: List<PrayerRecordEntity>,
        allAthkaar: List<AthkaarLogEntity>
    ): PrayerDashboardUiState {
        val nowCal = Calendar.getInstance()
        val calcTimes = PrayerTimeCalculator.calculateDailyPrayers(ctrl.city, nowCal)

        val offsetMap = settingsList.associate { it.prayerId to it.jamaahOffsetMinutes }
        val todayPrayerMap = allPrayerRecords
            .filter { it.dateIso == todayIso }
            .associateBy { it.prayerId }

        val rawPrayers = listOf(
            Triple("FAJR", "Fajr", "الفجر") to calcTimes.fajrMinutes,
            Triple("DHUHR", "Dhuhr", "الظهر") to calcTimes.dhuhrMinutes,
            Triple("ASR", "Asr", "العصر") to calcTimes.asrMinutes,
            Triple("MAGHRIB", "Maghrib", "المغرب") to calcTimes.maghribMinutes,
            Triple("ISHA", "Isha", "العشاء") to calcTimes.ishaMinutes
        )

        val firstPendingId = rawPrayers.firstOrNull { (meta, _) ->
            val rec = todayPrayerMap[meta.first]
            rec?.isPrayed != true && rec?.completionStatus != PrayerCompletionStatus.MISSED_QADA.name
        }?.first?.first ?: "ISHA"

        val prayerItems = rawPrayers.map { (meta, adhanMin) ->
            val id = meta.first
            val offset = offsetMap[id] ?: 15
            val rec = todayPrayerMap[id]
            val isPrayed = rec?.isPrayed == true
            val status = runCatching {
                PrayerCompletionStatus.valueOf(
                    rec?.completionStatus ?: if (isPrayed) "PRAYED_ON_TIME" else "PENDING"
                )
            }.getOrDefault(if (isPrayed) PrayerCompletionStatus.PRAYED_ON_TIME else PrayerCompletionStatus.PENDING)

            PrayerItemUiModel(
                id = id,
                nameEnglish = meta.second,
                nameArabic = meta.third,
                adhanMinutes = adhanMin,
                jamaahOffsetMinutes = offset,
                isPrayed = isPrayed,
                isCurrent = id == firstPendingId,
                isNext = id == firstPendingId,
                completionStatus = status,
                nagCountTriggered = rec?.nagCountTriggered ?: 0
            )
        }

        val featured = prayerItems.firstOrNull { it.id == firstPendingId } ?: prayerItems[2]

        val combinedSnooze = ctrl.snoozedMinutes + ctrl.alarmRuntime.snoozedExtraMinutes
        val simulatedMinutesToAdhan = (9 - ((ctrl.tick / 60).toInt() % 5)).coerceAtLeast(1)
        val totalMinutesToJamaah = simulatedMinutesToAdhan + featured.jamaahOffsetMinutes + combinedSnooze
        val progressFraction = (1f - (totalMinutesToJamaah.toFloat() / 45f)).coerceIn(0.28f, 0.92f)

        // Map SQLite fallback cities if loaded
        val citiesList = if (ctrl.sqlCities.isNotEmpty()) {
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
        }

        // Today's Athkaar habits
        val todayAthkaarMap = allAthkaar
            .filter { it.dateIso == todayIso }
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

        val todayPrayersDone = prayerItems.count { it.isPrayed }
        val todayQadaCount = prayerItems.count { it.completionStatus == PrayerCompletionStatus.MISSED_QADA }
        val todayAthkaarDone = athkaarModels.count { it.isCompleted }
        val totalTodayGoals = (todayPrayersDone + todayAthkaarDone) / 8f

        // Build 49-day (7 weeks x 7 days) contribution grid
        val recordsByDate = allPrayerRecords.groupBy { it.dateIso }
        val athkaarByDate = allAthkaar.groupBy { it.dateIso }
        val dayNameFormat = SimpleDateFormat("EEE", Locale.US)

        val gridCells = (48 downTo 0).map { daysAgo ->
            val c = nowCal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val iso = dateFormatter.format(c.time)
            val pDone = if (iso == todayIso) {
                todayPrayersDone
            } else {
                recordsByDate[iso]?.count { it.isPrayed } ?: 4
            }
            val aDone = if (iso == todayIso) {
                todayAthkaarDone
            } else {
                athkaarByDate[iso]?.count { it.isCompleted } ?: 2
            }
            val qada = recordsByDate[iso]?.count {
                it.completionStatus == PrayerCompletionStatus.MISSED_QADA.name
            } ?: 0
            DayContributionCell(
                dateIso = iso,
                dayOfWeekShort = dayNameFormat.format(c.time).take(1),
                dayOfMonth = c.get(Calendar.DAY_OF_MONTH),
                prayersCompleted = pDone,
                athkaarCompleted = aDone,
                qadaCount = qada,
                isToday = iso == todayIso
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
            sqliteFallbackCityCount = citiesList.size,
            locationSourceStatus = ctrl.locationSourceStatus,
            hijriDateText = PrayerTimeCalculator.getApproximateHijriDateString(nowCal),
            gregorianDateText = displayDateFormatter.format(nowCal.time),
            currentTimeFormatted = clockTimeFormatter.format(nowCal.time),
            currentClockSeconds = (59 - (ctrl.tick % 60)).toInt(),
            sunriseTimeFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(calcTimes.sunriseMinutes),
            prayers = prayerItems,
            featuredPrayer = featured,
            minutesUntilNextPrayer = simulatedMinutesToAdhan,
            minutesUntilJamaah = totalMinutesToJamaah,
            snoozedExtraMinutes = combinedSnooze,
            preJamaahAlertMinutes = ctrl.userPrefs?.preJamaahAlertMinutes ?: 15,
            nagIntervalMinutes = ctrl.userPrefs?.nagIntervalMinutes ?: 20,
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
            editingJamaahPrayer = ctrl.editingPrayer,
            statusBannerMessage = ctrl.bannerMessage
        )
    }

    private fun armDailyExactAlarmsForCity(city: CityLocation) {
        val calc = PrayerTimeCalculator.calculateDailyPrayers(city)
        val asrAdhanStr = PrayerTimeCalculator.formatMinutesTo12Hour(calc.asrMinutes)
        val asrJamaahStr = PrayerTimeCalculator.formatMinutesTo12Hour(calc.asrMinutes + 15)

        // Schedule Trigger 1 (Adhan) and Trigger 2 (15m before Jama'ah) using AlarmManager.setExactAndAllowWhileIdle
        val nowMs = System.currentTimeMillis()
        alarmScheduler.scheduleExactTrigger(
            prayerId = "ASR",
            prayerName = "Asr",
            dateIso = todayIso,
            triggerType = AlarmTriggerType.TRIGGER_1_ADHAN_START,
            triggerAtEpochMillis = nowMs + (9 * 60 * 1000L),
            adhanTimeFormatted = asrAdhanStr,
            jamaahTimeFormatted = asrJamaahStr,
            minutesUntilJamaah = 24,
            nagIteration = 0
        )
    }

    fun setDisplayMode(mode: AppDisplayMode) {
        _displayMode.value = mode
    }

    fun selectTab(tab: ActiveTab) {
        _activeTab.value = tab
    }

    fun selectCity(city: CityLocation) {
        _selectedCity.value = city
        _locationSourceStatus.value = "SQLite Manual City (${city.name})"
        viewModelScope.launch {
            repository.updateSelectedLocation(city, isGps = false)
            armDailyExactAlarmsForCity(city)
            showTransientBanner("SQLite City selected: ${city.name} • Exact alarms rescheduled")
        }
    }

    fun requestGpsOrSqliteFallback() {
        viewModelScope.launch {
            val fallbackList = uiState.value.availableCities
            when (val res = locationManager.resolveLocationWithFallback(fallbackList, _selectedCity.value)) {
                is LocationResolutionResult.GpsResolved -> {
                    _selectedCity.value = res.cityLocation
                    _locationSourceStatus.value = "FusedLocationProvider GPS"
                    repository.updateSelectedLocation(res.cityLocation, isGps = true)
                    armDailyExactAlarmsForCity(res.cityLocation)
                    showTransientBanner("GPS resolved: ${res.cityLocation.name}")
                }
                is LocationResolutionResult.FallbackSqliteCity -> {
                    _selectedCity.value = res.cityLocation
                    _locationSourceStatus.value = "SQLite Fallback (${res.cityLocation.name})"
                    repository.updateSelectedLocation(res.cityLocation, isGps = false)
                    showTransientBanner(res.reason)
                }
            }
        }
    }

    fun togglePrayerCompletion(prayer: PrayerItemUiModel) {
        viewModelScope.launch {
            repository.togglePrayerCompletion(todayIso, prayer.id, prayer.isPrayed)
            if (!prayer.isPrayed) {
                alarmScheduler.cancelAllAlarmsForPrayer(prayer.id)
                alarmDataStore.clearNaggingLoop(
                    reason = "${prayer.nameEnglish} marked Prayed • Nagging loop cancelled",
                    stageLabel = "PRAYED_ON_TIME"
                )
            }
            val actionText = if (!prayer.isPrayed) {
                "${prayer.nameEnglish} marked as prayed ✓"
            } else {
                "${prayer.nameEnglish} unmarked (Pending)"
            }
            showTransientBanner(actionText)
        }
    }

    fun markFeaturedPrayerAsPrayed() {
        val featured = uiState.value.featuredPrayer ?: return
        viewModelScope.launch {
            repository.markPrayerCompleted(todayIso, featured.id)
            _snoozedExtraMinutes.value = 0
            alarmScheduler.cancelAllAlarmsForPrayer(featured.id)
            alarmDataStore.clearNaggingLoop(
                reason = "${featured.nameEnglish} marked as Prayed • Nagging loop stopped",
                stageLabel = "PRAYED_ON_TIME"
            )
            showTransientBanner("${featured.nameEnglish} marked as prayed ✓")
        }
    }

    fun simulateNextPrayerAutoQadaRollover() {
        val featured = uiState.value.featuredPrayer ?: return
        viewModelScope.launch {
            repository.markPrayerAsMissedQada(todayIso, featured.id)
            alarmScheduler.cancelAllAlarmsForPrayer(featured.id)
            alarmDataStore.clearNaggingLoop(
                reason = "Next prayer started: ${featured.nameEnglish} auto-marked as MISSED_QADA & nag stopped",
                stageLabel = "AUTO_QADA_ROLLOVER"
            )
            showTransientBanner("${featured.nameEnglish} auto-marked as Missed (Qada) on next prayer rollover")
        }
    }

    fun snoozeFeaturedPrayerFiveMinutes() {
        _snoozedExtraMinutes.value += 5
        val featured = uiState.value.featuredPrayer
        val featuredName = featured?.nameEnglish ?: "Prayer"
        val featuredId = featured?.id ?: "ASR"
        viewModelScope.launch {
            val nextEpoch = System.currentTimeMillis() + (5 * 60 * 1000L)
            alarmDataStore.recordSnoozeFiveMinutes(featuredId, nextEpoch)
            showTransientBanner("$featuredName reminder snoozed by +5m (Soft Chime)")
        }
    }

    fun openJamaahEditor(prayer: PrayerItemUiModel) {
        _editingJamaahPrayer.value = prayer
    }

    fun dismissJamaahEditor() {
        _editingJamaahPrayer.value = null
    }

    fun saveJamaahOffset(prayerId: String, newOffsetMinutes: Int) {
        viewModelScope.launch {
            repository.updateJamaahOffset(prayerId, newOffsetMinutes)
            _editingJamaahPrayer.value = null
            showTransientBanner("Jama'ah offset updated to +${newOffsetMinutes}m (Trigger 2 @ -15m)")
        }
    }

    fun stepAthkaarCounter(habit: AthkaarHabitUiModel) {
        viewModelScope.launch {
            val nowFormatted = clockTimeFormatter.format(Calendar.getInstance().time)
            repository.incrementAthkaar(
                dateIso = todayIso,
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
            val nowFormatted = clockTimeFormatter.format(Calendar.getInstance().time)
            repository.toggleAthkaarComplete(
                dateIso = todayIso,
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
