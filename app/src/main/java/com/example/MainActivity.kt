package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.PrayerAlarmDataStore
import com.example.data.PrayerRepository
import com.example.domain.LocationResolutionManager
import com.example.notification.ExactPrayerAlarmScheduler
import com.example.notification.PrayerNotificationHelper
import com.example.ui.screens.NoorPrayerAppRoot
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PrayerHabitViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        PrayerNotificationHelper.ensureChannel(this)

        val appCtx = applicationContext
        val database = AppDatabase.getInstance(appCtx)
        val repository = PrayerRepository(database.prayerDao())
        val alarmDataStore = PrayerAlarmDataStore(appCtx)
        val alarmScheduler = ExactPrayerAlarmScheduler(appCtx)
        val locationManager = LocationResolutionManager(appCtx)

        setContent {
            MyApplicationTheme {
                val viewModel: PrayerHabitViewModel = viewModel(
                    factory = PrayerHabitViewModel.provideFactory(
                        repository = repository,
                        alarmDataStore = alarmDataStore,
                        alarmScheduler = alarmScheduler,
                        locationManager = locationManager
                    )
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                NoorPrayerAppRoot(
                    uiState = uiState,
                    onSetDisplayMode = viewModel::setDisplayMode,
                    onSelectTab = viewModel::selectTab,
                    onSelectCity = viewModel::selectCity,
                    onSearchLocationQueryChanged = viewModel::updateLocationSearchQuery,
                    onAddCustomLocation = viewModel::addAndSelectCustomLocation,
                    onRequestGpsLocation = viewModel::requestGpsOrSqliteFallback,
                    onMarkFeaturedAsPrayed = viewModel::markFeaturedPrayerAsPrayed,
                    onSnoozeFeaturedFiveMin = viewModel::snoozeFeaturedPrayerFiveMinutes,
                    onSimulateQadaRollover = viewModel::simulateNextPrayerAutoQadaRollover,
                    onTogglePrayerCompleted = viewModel::togglePrayerCompletion,
                    onEditJamaahClicked = { prayer -> viewModel.openJamaahEditor(prayer, "JAMAAH") },
                    onEditAdhanClicked = { prayer -> viewModel.openJamaahEditor(prayer, "ADHAN") },
                    onSaveJamaahOffset = viewModel::saveJamaahOffset,
                    onSavePrayerTimeSettings = viewModel::savePrayerTimeSettings,
                    onDismissJamaahDialog = viewModel::dismissJamaahEditor,
                    onStepAthkaar = viewModel::stepAthkaarCounter,
                    onToggleAthkaarComplete = viewModel::toggleAthkaarCompletion,
                    onBannerMessage = viewModel::showTransientBanner,
                    onToggleCountdownMode = viewModel::toggleNextPrayerCountdownMode,
                    onUpdateRepeatInterval = viewModel::updateNotificationRepeatInterval
                )
            }
        }
    }
}
