package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {
    @Query("SELECT * FROM prayer_records WHERE dateIso = :dateIso")
    fun getPrayersForDate(dateIso: String): Flow<List<PrayerRecordEntity>>

    @Query("SELECT * FROM prayer_records WHERE dateIso = :dateIso AND prayerId = :prayerId LIMIT 1")
    suspend fun getPrayerRecordSync(dateIso: String, prayerId: String): PrayerRecordEntity?

    @Query("SELECT * FROM prayer_records")
    fun getAllPrayerRecords(): Flow<List<PrayerRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrayerRecord(record: PrayerRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrayerRecords(records: List<PrayerRecordEntity>)

    @Query("SELECT * FROM prayer_settings")
    fun getAllPrayerSettings(): Flow<List<PrayerSettingEntity>>

    @Query("SELECT * FROM prayer_settings")
    suspend fun getAllPrayerSettingsSync(): List<PrayerSettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrayerSetting(setting: PrayerSettingEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultPrayerSettings(settings: List<PrayerSettingEntity>)

    @Query("SELECT * FROM athkaar_logs WHERE dateIso = :dateIso")
    fun getAthkaarForDate(dateIso: String): Flow<List<AthkaarLogEntity>>

    @Query("SELECT * FROM athkaar_logs")
    fun getAllAthkaarLogs(): Flow<List<AthkaarLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAthkaarLog(log: AthkaarLogEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAthkaarLogsIfEmpty(logs: List<AthkaarLogEntity>)

    // UserPreferences queries
    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    fun getUserPreferences(): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    suspend fun getUserPreferencesSync(): UserPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserPreferences(prefs: UserPreferencesEntity)

    // Local SQLite City Fallback queries
    @Query("SELECT * FROM local_cities ORDER BY city_name ASC")
    fun getLocalCities(): Flow<List<LocalCityEntity>>

    @Query("SELECT * FROM local_cities")
    suspend fun getLocalCitiesSync(): List<LocalCityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalCities(cities: List<LocalCityEntity>)
}
