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
        // 1. Seed local SQLite fallback city database if empty
        val existingCities = prayerDao.getLocalCitiesSync()
        if (existingCities.isEmpty()) {
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
        }

        // 2. Seed UserPreferencesEntity with confirmed X=15m pre-Jama'ah and Y=20m nagging interval
        if (prayerDao.getUserPreferencesSync() == null) {
            val defaultCity = PrayerTimeCalculator.PredefinedCities.first()
            prayerDao.upsertUserPreferences(
                UserPreferencesEntity(
                    id = 1,
                    useGpsLocation = false,
                    selectedCityId = defaultCity.id,
                    lastLatitude = defaultCity.latitude,
                    lastLongitude = defaultCity.longitude,
                    timeZoneOffsetHours = defaultCity.timeZoneOffsetHours,
                    calculationMethod = "MWL",
                    preJamaahAlertMinutes = 15,
                    nagIntervalMinutes = 20,
                    autoQadaOnNextPrayer = true
                )
            )
        }

        // 3. Default Jama'ah congregation offsets (minutes after Adhan)
        val defaultOffsets = listOf(
            PrayerSettingEntity("FAJR", 20),
            PrayerSettingEntity("DHUHR", 15),
            PrayerSettingEntity("ASR", 15),
            PrayerSettingEntity("MAGHRIB", 10),
            PrayerSettingEntity("ISHA", 15)
        )
        prayerDao.insertDefaultPrayerSettings(defaultOffsets)

        // 4. Seed realistic historical data for the 7-week contribution graph & today's initial progress
        val existing = prayerDao.getAllPrayerRecords().first()
        if (existing.isEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance()
            val seedPrayerRecords = mutableListOf<PrayerRecordEntity>()
            val seedAthkaarLogs = mutableListOf<AthkaarLogEntity>()

            val prayerIds = listOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA")

            // Today: Fajr and Dhuhr already completed on time; Asr is the active upcoming prayer
            seedPrayerRecords.add(
                PrayerRecordEntity(
                    dateIso = todayIso,
                    prayerId = "FAJR",
                    isPrayed = true,
                    isInJamaah = true,
                    completedAtMillis = System.currentTimeMillis() - 14400000L,
                    completionStatus = PrayerCompletionStatus.PRAYED_ON_TIME.name
                )
            )
            seedPrayerRecords.add(
                PrayerRecordEntity(
                    dateIso = todayIso,
                    prayerId = "DHUHR",
                    isPrayed = true,
                    isInJamaah = true,
                    completedAtMillis = System.currentTimeMillis() - 3600000L,
                    completionStatus = PrayerCompletionStatus.PRAYED_ON_TIME.name
                )
            )
            seedPrayerRecords.add(
                PrayerRecordEntity(
                    dateIso = todayIso,
                    prayerId = "ASR",
                    isPrayed = false,
                    isInJamaah = true,
                    completionStatus = PrayerCompletionStatus.PENDING.name
                )
            )
            seedPrayerRecords.add(
                PrayerRecordEntity(
                    dateIso = todayIso,
                    prayerId = "MAGHRIB",
                    isPrayed = false,
                    isInJamaah = true,
                    completionStatus = PrayerCompletionStatus.PENDING.name
                )
            )
            seedPrayerRecords.add(
                PrayerRecordEntity(
                    dateIso = todayIso,
                    prayerId = "ISHA",
                    isPrayed = false,
                    isInJamaah = true,
                    completionStatus = PrayerCompletionStatus.PENDING.name
                )
            )

            seedAthkaarLogs.add(
                AthkaarLogEntity(
                    dateIso = todayIso,
                    athkaarId = "MORNING",
                    currentCount = 33,
                    targetCount = 33,
                    isCompleted = true,
                    completedTimeFormatted = "06:15 AM"
                )
            )
            seedAthkaarLogs.add(
                AthkaarLogEntity(
                    dateIso = todayIso,
                    athkaarId = "EVENING",
                    currentCount = 18,
                    targetCount = 33,
                    isCompleted = false,
                    completedTimeFormatted = null
                )
            )
            seedAthkaarLogs.add(
                AthkaarLogEntity(
                    dateIso = todayIso,
                    athkaarId = "BEDTIME",
                    currentCount = 0,
                    targetCount = 33,
                    isCompleted = false,
                    completedTimeFormatted = "10:45 PM (Yesterday)"
                )
            )

            // Past 48 days for rich weekly contribution grid & streak stats
            for (daysAgo in 1..48) {
                val dayCal = cal.clone() as Calendar
                dayCal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                val dateStr = sdf.format(dayCal.time)

                val prayersDoneCount = when {
                    daysAgo <= 12 -> 5
                    daysAgo % 7 == 0 -> 3
                    daysAgo % 5 == 0 -> 4
                    else -> 5
                }
                prayerIds.forEachIndexed { index, pId ->
                    val prayed = index < prayersDoneCount
                    seedPrayerRecords.add(
                        PrayerRecordEntity(
                            dateIso = dateStr,
                            prayerId = pId,
                            isPrayed = prayed,
                            isInJamaah = index != 1 || daysAgo % 3 != 0,
                            completionStatus = if (prayed) {
                                PrayerCompletionStatus.PRAYED_ON_TIME.name
                            } else {
                                PrayerCompletionStatus.MISSED_QADA.name
                            }
                        )
                    )
                }

                val athkaarDoneCount = when {
                    daysAgo <= 12 -> 3
                    daysAgo % 4 == 0 -> 2
                    else -> 3
                }
                val athkaarTypes = listOf("MORNING", "EVENING", "BEDTIME")
                athkaarTypes.forEachIndexed { idx, aId ->
                    val done = idx < athkaarDoneCount
                    seedAthkaarLogs.add(
                        AthkaarLogEntity(
                            dateIso = dateStr,
                            athkaarId = aId,
                            currentCount = if (done) 33 else 12,
                            targetCount = 33,
                            isCompleted = done,
                            completedTimeFormatted = if (aId == "BEDTIME" && done) "10:${30 + (daysAgo % 25)} PM" else null
                        )
                    )
                }
            }

            prayerDao.upsertPrayerRecords(seedPrayerRecords)
            prayerDao.insertAthkaarLogsIfEmpty(seedAthkaarLogs)
        }
    }

    suspend fun updateSelectedLocation(city: CityLocation, isGps: Boolean) {
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

    suspend fun togglePrayerCompletion(dateIso: String, prayerId: String, currentlyPrayed: Boolean) {
        val existing = prayerDao.getPrayerRecordSync(dateIso, prayerId)
        val nextPrayed = !currentlyPrayed
        prayerDao.upsertPrayerRecord(
            PrayerRecordEntity(
                dateIso = dateIso,
                prayerId = prayerId,
                isPrayed = nextPrayed,
                isInJamaah = true,
                completedAtMillis = if (nextPrayed) System.currentTimeMillis() else 0L,
                completionStatus = if (nextPrayed) {
                    PrayerCompletionStatus.PRAYED_ON_TIME.name
                } else {
                    PrayerCompletionStatus.PENDING.name
                },
                nagCountTriggered = existing?.nagCountTriggered ?: 0
            )
        )
    }

    suspend fun markPrayerCompleted(dateIso: String, prayerId: String) {
        val existing = prayerDao.getPrayerRecordSync(dateIso, prayerId)
        prayerDao.upsertPrayerRecord(
            PrayerRecordEntity(
                dateIso = dateIso,
                prayerId = prayerId,
                isPrayed = true,
                isInJamaah = true,
                completedAtMillis = System.currentTimeMillis(),
                completionStatus = PrayerCompletionStatus.PRAYED_ON_TIME.name,
                nagCountTriggered = existing?.nagCountTriggered ?: 0
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

    suspend fun updateJamaahOffset(prayerId: String, offsetMinutes: Int) {
        prayerDao.upsertPrayerSetting(
            PrayerSettingEntity(
                prayerId = prayerId,
                jamaahOffsetMinutes = offsetMinutes.coerceIn(0, 90)
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
