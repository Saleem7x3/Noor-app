package com.example.data

import com.example.domain.CityLocation
import com.example.domain.PrayerTimeCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PrayerRepository(private val prayerDao: PrayerDao) {

    val allPrayerSettings: Flow<List<PrayerSettingEntity>> = prayerDao.getAllPrayerSettings()
    val allPrayerRecords: Flow<List<PrayerRecordEntity>> = prayerDao.getAllPrayerRecords()
    val allAthkaarLogs: Flow<List<AthkaarLogEntity>> = prayerDao.getAllAthkaarLogs()
    val userPreferences: Flow<UserPreferencesEntity?> = prayerDao.getUserPreferences()
    val localCities: Flow<List<LocalCityEntity>> = prayerDao.getLocalCities()

    fun getPrayersForDate(dateIso: String): Flow<List<PrayerRecordEntity>> =
        prayerDao.getPrayersForDate(dateIso)

    fun getAthkaarForDate(dateIso: String): Flow<List<AthkaarLogEntity>> =
        prayerDao.getAthkaarForDate(dateIso)

    suspend fun ensureInitialized(todayIso: String) {
        // 1. Prune any unrecognized pre-seeded entries and upsert the alphabetical world city catalog
        val validPredefinedIds = PrayerTimeCalculator.PredefinedCities.map { it.id }
        prayerDao.pruneUnrecognizedPreseededCities(validPredefinedIds)

        val sqlCities = PrayerTimeCalculator.PredefinedCities.map { city ->
            LocalCityEntity(
                cityId = city.id,
                cityName = city.name,
                countryName = city.country,
                latitude = city.latitude,
                longitude = city.longitude,
                timeZoneOffsetHours = city.timeZoneOffsetHours
            )
        }
        prayerDao.insertLocalCities(sqlCities)

        // 2. Seed UserPreferencesEntity with timezone-detected default city, X=15m, Y=20m buffers, and firstLaunchEpochMs
        val nowEpochMs = System.currentTimeMillis()
        val currentPrefs = prayerDao.getUserPreferencesSync()
        val existingSavedCities = prayerDao.getLocalCitiesSync().map { it.cityId }.toSet()
        val isRecognizedSelection = currentPrefs != null && currentPrefs.selectedCityId.isNotBlank() && (
            currentPrefs.useGpsLocation ||
            currentPrefs.selectedCityId in validPredefinedIds ||
            currentPrefs.selectedCityId in existingSavedCities ||
            currentPrefs.selectedCityId.startsWith("gps_") ||
            currentPrefs.selectedCityId.startsWith("nom_") ||
            currentPrefs.selectedCityId.startsWith("custom_") ||
            currentPrefs.selectedCityId.startsWith("geo_") ||
            currentPrefs.selectedCityId.startsWith("osm_")
        )
        if (currentPrefs == null || !isRecognizedSelection) {
            val defaultCity = PrayerTimeCalculator.detectBestDefaultCityForDeviceTimeZone()
            prayerDao.upsertUserPreferences(
                UserPreferencesEntity(
                    id = 1,
                    useGpsLocation = false,
                    selectedCityId = defaultCity.id,
                    lastLatitude = defaultCity.latitude,
                    lastLongitude = defaultCity.longitude,
                    timeZoneOffsetHours = defaultCity.timeZoneOffsetHours,
                    calculationMethod = "MWL",
                    preAdhanAlertMinutes = 5,
                    preJamaahAlertMinutes = 15,
                    nagIntervalMinutes = 10,
                    urgentEndWindowMinutes = 15,
                    autoQadaOnNextPrayer = true,
                    firstLaunchEpochMs = nowEpochMs
                )
            )
        } else if (currentPrefs.firstLaunchEpochMs <= 0L) {
            prayerDao.upsertUserPreferences(
                currentPrefs.copy(firstLaunchEpochMs = nowEpochMs)
            )
        }

        // 3. Default Mosque Adhan & Jama'ah configurations (Fajr 5:00 AM, Dhuhr/Jumu'ah 1:00 PM, Asr 4:30 PM, Maghrib = PST, Isha 8:15 PM)
        val defaultOffsets = listOf(
            PrayerSettingEntity(
                prayerId = "FAJR",
                adhanOffsetMinutes = 0,
                useFixedAdhanTime = true,
                fixedAdhanTimeMinutes = 5 * 60, // 05:00 AM
                jamaahOffsetMinutes = 20,
                useFixedTime = false,
                fixedTimeMinutes = 5 * 60 + 20 // 05:20 AM
            ),
            PrayerSettingEntity(
                prayerId = "DHUHR",
                adhanOffsetMinutes = 0,
                useFixedAdhanTime = true,
                fixedAdhanTimeMinutes = 13 * 60, // 01:00 PM
                jamaahOffsetMinutes = 30,
                useFixedTime = false,
                fixedTimeMinutes = 13 * 60 + 30 // 01:30 PM
            ),
            PrayerSettingEntity(
                prayerId = "JUMUAH",
                adhanOffsetMinutes = 0,
                useFixedAdhanTime = true,
                fixedAdhanTimeMinutes = 13 * 60, // 01:00 PM
                jamaahOffsetMinutes = 30,
                useFixedTime = false,
                fixedTimeMinutes = 13 * 60 + 30 // 01:30 PM
            ),
            PrayerSettingEntity(
                prayerId = "ASR",
                adhanOffsetMinutes = 0,
                useFixedAdhanTime = true,
                fixedAdhanTimeMinutes = 16 * 60 + 30, // 04:30 PM
                jamaahOffsetMinutes = 15,
                useFixedTime = false,
                fixedTimeMinutes = 16 * 60 + 45 // 04:45 PM
            ),
            PrayerSettingEntity(
                prayerId = "MAGHRIB",
                adhanOffsetMinutes = 0,
                useFixedAdhanTime = false, // Same as Prayer Start Time (PST) by default, still adjustable
                fixedAdhanTimeMinutes = 18 * 60 + 15,
                jamaahOffsetMinutes = 10,
                useFixedTime = false,
                fixedTimeMinutes = 18 * 60 + 25
            ),
            PrayerSettingEntity(
                prayerId = "ISHA",
                adhanOffsetMinutes = 0,
                useFixedAdhanTime = true,
                fixedAdhanTimeMinutes = 20 * 60 + 15, // 08:15 PM
                jamaahOffsetMinutes = 15,
                useFixedTime = false,
                fixedTimeMinutes = 20 * 60 + 30 // 08:30 PM
            )
        )
        prayerDao.insertDefaultPrayerSettings(defaultOffsets)

        // 4. Seed realistic historical data for the 7-week contribution graph & today's unmarked initial sheet
        val existing = prayerDao.getAllPrayerRecords().first()
        if (existing.isEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val defaultCity = PrayerTimeCalculator.detectBestDefaultCityForDeviceTimeZone()
            val cal = PrayerTimeCalculator.getCalendarForCity(defaultCity)
            val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val todayCalc = PrayerTimeCalculator.calculateDailyPrayers(defaultCity, cal)

            // Switch to the new day's Fajr-to-Isha sheet right at Islamic Midnight (end of Isha)
            val activeSheetCal = cal.clone() as Calendar
            if (todayCalc.islamicMidnightMinutes < 1440 && nowMinutes >= todayCalc.islamicMidnightMinutes) {
                activeSheetCal.add(Calendar.DAY_OF_YEAR, 1)
            } else if (todayCalc.islamicMidnightMinutes > 1440 && nowMinutes < (todayCalc.islamicMidnightMinutes - 1440)) {
                activeSheetCal.add(Calendar.DAY_OF_YEAR, -1)
            }
            val activeSheetIso = sdf.format(activeSheetCal.time)

            val seedPrayerRecords = mutableListOf<PrayerRecordEntity>()
            val seedAthkaarLogs = mutableListOf<AthkaarLogEntity>()

            val prayerIds = listOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA")

            // First-time experience: all 5 prayers on the active sheet start unmarked (PENDING)
            // so the user is the one to tick them; Qada is only marked for prayer windows that pass AFTER first launch.
            prayerIds.forEach { pId ->
                seedPrayerRecords.add(
                    PrayerRecordEntity(
                        dateIso = activeSheetIso,
                        prayerId = pId,
                        isPrayed = false,
                        isInJamaah = true,
                        completedAtMillis = 0L,
                        completionStatus = PrayerCompletionStatus.PENDING.name
                    )
                )
            }

            seedAthkaarLogs.add(
                AthkaarLogEntity(
                    dateIso = activeSheetIso,
                    athkaarId = "MORNING",
                    currentCount = 0,
                    targetCount = 33,
                    isCompleted = false,
                    completedTimeFormatted = null
                )
            )
            seedAthkaarLogs.add(
                AthkaarLogEntity(
                    dateIso = activeSheetIso,
                    athkaarId = "EVENING",
                    currentCount = 0,
                    targetCount = 33,
                    isCompleted = false,
                    completedTimeFormatted = null
                )
            )
            seedAthkaarLogs.add(
                AthkaarLogEntity(
                    dateIso = activeSheetIso,
                    athkaarId = "BEDTIME",
                    currentCount = 0,
                    targetCount = 33,
                    isCompleted = false,
                    completedTimeFormatted = null
                )
            )

            prayerDao.upsertPrayerRecords(seedPrayerRecords)
            prayerDao.insertAthkaarLogsIfEmpty(seedAthkaarLogs)
        }
    }

    suspend fun updateCalculationMethod(methodName: String) {
        val current = prayerDao.getUserPreferencesSync() ?: UserPreferencesEntity()
        prayerDao.upsertUserPreferences(
            current.copy(calculationMethod = methodName)
        )
    }

    suspend fun upsertCatchUpPrayerRecords(records: List<PrayerRecordEntity>) {
        if (records.isNotEmpty()) {
            prayerDao.upsertPrayerRecords(records)
        }
    }

    suspend fun getSavedSelectedCity(): Pair<CityLocation, Boolean>? {
        val prefs = prayerDao.getUserPreferencesSync() ?: return null
        val savedCities = prayerDao.getLocalCitiesSync()
        val matchedEntity = savedCities.firstOrNull { it.cityId == prefs.selectedCityId }
        val city = if (matchedEntity != null) {
            CityLocation(
                id = matchedEntity.cityId,
                name = matchedEntity.cityName,
                country = matchedEntity.countryName,
                latitude = matchedEntity.latitude,
                longitude = matchedEntity.longitude,
                timeZoneOffsetHours = matchedEntity.timeZoneOffsetHours,
                isGpsSimulated = prefs.useGpsLocation
            )
        } else {
            PrayerTimeCalculator.PredefinedCities.firstOrNull { it.id == prefs.selectedCityId }
                ?: CityLocation(
                    id = prefs.selectedCityId,
                    name = prefs.selectedCityId.replaceFirstChar { it.uppercase() },
                    country = "Saved Location",
                    latitude = prefs.lastLatitude,
                    longitude = prefs.lastLongitude,
                    timeZoneOffsetHours = prefs.timeZoneOffsetHours,
                    isGpsSimulated = prefs.useGpsLocation
                )
        }
        return Pair(city, prefs.useGpsLocation)
    }

    suspend fun updateSelectedLocation(city: CityLocation, isGps: Boolean) {
        // Persist the selected town/city into local_cities so it remains available offline
        prayerDao.insertLocalCities(
            listOf(
                LocalCityEntity(
                    cityId = city.id,
                    cityName = city.name,
                    countryName = city.country,
                    latitude = city.latitude,
                    longitude = city.longitude,
                    timeZoneOffsetHours = city.timeZoneOffsetHours
                )
            )
        )
        val current = prayerDao.getUserPreferencesSync() ?: UserPreferencesEntity()
        prayerDao.upsertUserPreferences(
            current.copy(
                useGpsLocation = isGps,
                selectedCityId = city.id,
                lastLatitude = city.latitude,
                lastLongitude = city.longitude,
                timeZoneOffsetHours = city.timeZoneOffsetHours
            )
        )
    }

    suspend fun togglePrayerCompletion(
        dateIso: String,
        prayerId: String,
        currentlyPrayed: Boolean,
        isInJamaah: Boolean = true
    ) {
        val existing = prayerDao.getPrayerRecordSync(dateIso, prayerId)
        val nextPrayed = !currentlyPrayed
        val wasQada = existing?.completionStatus == PrayerCompletionStatus.MISSED_QADA.name
        prayerDao.upsertPrayerRecord(
            PrayerRecordEntity(
                dateIso = dateIso,
                prayerId = prayerId,
                isPrayed = nextPrayed,
                isInJamaah = if (wasQada) false else isInJamaah,
                completedAtMillis = if (nextPrayed) System.currentTimeMillis() else 0L,
                completionStatus = when {
                    nextPrayed && wasQada -> PrayerCompletionStatus.PRAYED_LATE.name
                    nextPrayed -> PrayerCompletionStatus.PRAYED_ON_TIME.name
                    else -> PrayerCompletionStatus.PENDING.name
                },
                nagCountTriggered = existing?.nagCountTriggered ?: 0
            )
        )
    }

    suspend fun markPrayerCompleted(
        dateIso: String,
        prayerId: String,
        isInJamaah: Boolean = true
    ) {
        val existing = prayerDao.getPrayerRecordSync(dateIso, prayerId)
        val wasQada = existing?.completionStatus == PrayerCompletionStatus.MISSED_QADA.name
        prayerDao.upsertPrayerRecord(
            PrayerRecordEntity(
                dateIso = dateIso,
                prayerId = prayerId,
                isPrayed = true,
                isInJamaah = if (wasQada) false else isInJamaah,
                completedAtMillis = System.currentTimeMillis(),
                completionStatus = if (wasQada) {
                    PrayerCompletionStatus.PRAYED_LATE.name
                } else {
                    PrayerCompletionStatus.PRAYED_ON_TIME.name
                },
                nagCountTriggered = existing?.nagCountTriggered ?: 0
            )
        )
    }

    suspend fun updatePrayerJamaahStatus(
        dateIso: String,
        prayerId: String,
        isInJamaah: Boolean
    ) {
        val existing = prayerDao.getPrayerRecordSync(dateIso, prayerId) ?: return
        prayerDao.upsertPrayerRecord(
            existing.copy(
                isPrayed = true,
                isInJamaah = isInJamaah
            )
        )
    }

    suspend fun markPrayerAsMissedQada(dateIso: String, prayerId: String) {
        val existing = prayerDao.getPrayerRecordSync(dateIso, prayerId)
        prayerDao.upsertPrayerRecord(
            PrayerRecordEntity(
                dateIso = dateIso,
                prayerId = prayerId,
                isPrayed = false,
                isInJamaah = false,
                completedAtMillis = 0L,
                completionStatus = PrayerCompletionStatus.MISSED_QADA.name,
                nagCountTriggered = (existing?.nagCountTriggered ?: 0) + 1
            )
        )
    }

    suspend fun updateHistoricalPrayerStatus(
        dateIso: String,
        prayerId: String,
        isPrayed: Boolean,
        isInJamaah: Boolean,
        completionStatus: PrayerCompletionStatus
    ) {
        val existing = prayerDao.getPrayerRecordSync(dateIso, prayerId)
        prayerDao.upsertPrayerRecord(
            PrayerRecordEntity(
                dateIso = dateIso,
                prayerId = prayerId,
                isPrayed = isPrayed,
                isInJamaah = if (isPrayed) isInJamaah else false,
                completedAtMillis = if (isPrayed) System.currentTimeMillis() else 0L,
                completionStatus = completionStatus.name,
                nagCountTriggered = existing?.nagCountTriggered ?: 0
            )
        )
    }

    suspend fun updatePrayerTimeSettings(
        prayerId: String,
        adhanOffsetMinutes: Int,
        useFixedAdhanTime: Boolean,
        fixedAdhanTimeMinutes: Int,
        jamaahOffsetMinutes: Int,
        useFixedJamaahTime: Boolean,
        fixedJamaahTimeMinutes: Int
    ) {
        prayerDao.upsertPrayerSetting(
            PrayerSettingEntity(
                prayerId = prayerId,
                adhanOffsetMinutes = adhanOffsetMinutes.coerceIn(-30, 180),
                useFixedAdhanTime = useFixedAdhanTime,
                fixedAdhanTimeMinutes = fixedAdhanTimeMinutes.coerceIn(0, 1439),
                jamaahOffsetMinutes = jamaahOffsetMinutes.coerceIn(-30, 180),
                useFixedTime = useFixedJamaahTime,
                fixedTimeMinutes = fixedJamaahTimeMinutes.coerceIn(0, 1439)
            )
        )
    }

    suspend fun updateJamaahOffset(
        prayerId: String,
        offsetMinutes: Int,
        useFixedTime: Boolean = false,
        fixedTimeMinutes: Int = 795
    ) {
        val existing = prayerDao.getAllPrayerSettingsSync().firstOrNull { it.prayerId == prayerId }
        prayerDao.upsertPrayerSetting(
            PrayerSettingEntity(
                prayerId = prayerId,
                adhanOffsetMinutes = existing?.adhanOffsetMinutes ?: 0,
                useFixedAdhanTime = existing?.useFixedAdhanTime ?: false,
                fixedAdhanTimeMinutes = existing?.fixedAdhanTimeMinutes ?: 780,
                jamaahOffsetMinutes = offsetMinutes.coerceIn(-30, 180),
                useFixedTime = useFixedTime,
                fixedTimeMinutes = fixedTimeMinutes.coerceIn(0, 1439)
            )
        )
    }

    suspend fun updateNotificationIntervals(
        nagIntervalMinutes: Int,
        urgentEndWindowMinutes: Int = 15,
        preAdhanAlertMinutes: Int? = null,
        preJamaahAlertMinutes: Int? = null
    ) {
        val current = prayerDao.getUserPreferencesSync() ?: UserPreferencesEntity()
        prayerDao.upsertUserPreferences(
            current.copy(
                preAdhanAlertMinutes = (preAdhanAlertMinutes ?: current.preAdhanAlertMinutes).coerceIn(0, 30),
                preJamaahAlertMinutes = (preJamaahAlertMinutes ?: current.preJamaahAlertMinutes).coerceIn(5, 45),
                nagIntervalMinutes = nagIntervalMinutes.coerceIn(5, 60),
                urgentEndWindowMinutes = urgentEndWindowMinutes.coerceIn(5, 60)
            )
        )
    }

    suspend fun incrementAthkaar(
        dateIso: String,
        athkaarId: String,
        currentCount: Int,
        targetCount: Int,
        step: Int = 11,
        timeFormatted: String
    ) {
        val nextCount = if (currentCount >= targetCount) 0 else (currentCount + step).coerceAtMost(targetCount)
        val completed = nextCount >= targetCount
        prayerDao.upsertAthkaarLog(
            AthkaarLogEntity(
                dateIso = dateIso,
                athkaarId = athkaarId,
                currentCount = nextCount,
                targetCount = targetCount,
                isCompleted = completed,
                completedTimeFormatted = if (completed) timeFormatted else null
            )
        )
    }

    suspend fun ensureDailyAthkaarInitialized(dateIso: String) {
        val freshLogs = listOf("MORNING", "EVENING", "BEDTIME").map { athkaarId ->
            AthkaarLogEntity(
                dateIso = dateIso,
                athkaarId = athkaarId,
                currentCount = 0,
                targetCount = 33,
                isCompleted = false,
                completedTimeFormatted = null
            )
        }
        prayerDao.insertAthkaarLogsIfEmpty(freshLogs)
    }

    suspend fun toggleAthkaarComplete(
        dateIso: String,
        athkaarId: String,
        currentlyCompleted: Boolean,
        targetCount: Int,
        timeFormatted: String
    ) {
        val nextCompleted = !currentlyCompleted
        prayerDao.upsertAthkaarLog(
            AthkaarLogEntity(
                dateIso = dateIso,
                athkaarId = athkaarId,
                currentCount = if (nextCompleted) targetCount else 0,
                targetCount = targetCount,
                isCompleted = nextCompleted,
                completedTimeFormatted = if (nextCompleted) timeFormatted else null
            )
        )
    }
}
