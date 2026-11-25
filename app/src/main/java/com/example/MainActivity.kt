package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.PrayerAlarmDataStore
import com.example.data.PrayerRepository
import com.example.domain.LocationResolutionManager
import com.example.notification.ExactPrayerAlarmScheduler
import com.example.notification.PrayerNotificationHelper
import com.example.ui.screens.NoorPrayerAppRoot
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PrayerHabitViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        PrayerNotificationHelper.ensureChannel(this)

        val appCtx = applicationContext
        val database = AppDatabase.getInstance(appCtx)
        val repository = PrayerRepository(database.prayerDao())
        val alarmDataStore = PrayerAlarmDataStore(appCtx)
        val alarmScheduler = ExactPrayerAlarmScheduler(appCtx)
        val locationManager = LocationResolutionManager(appCtx)

        setContent {
            val viewModel: PrayerHabitViewModel = viewModel(
                factory = PrayerHabitViewModel.provideFactory(
                    repository = repository,
                    alarmDataStore = alarmDataStore,
                    alarmScheduler = alarmScheduler,
                    locationManager = locationManager
                )
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val systemDark = isSystemInDarkTheme()
            val resolvedDarkTheme = when (uiState.themeMode) {
                AppThemeMode.SYSTEM_AUTO -> systemDark
                AppThemeMode.APPLE_WHITE_LIGHT -> false
                AppThemeMode.APPLE_OLED_DARK -> true
            }

            // Dynamically switch system status/navigation bar icon polarity for Apple White vs OLED Dark
            LaunchedEffect(resolvedDarkTheme) {
                if (resolvedDarkTheme) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        ),
                        navigationBarStyle = SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    )
                }
            }

            MyApplicationTheme(darkTheme = resolvedDarkTheme) {
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        viewModel.syncAndArmNotificationsNow()
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasPerm) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.syncAndArmNotificationsNow()
                        }
                    } else {
                        viewModel.syncAndArmNotificationsNow()
                    }
                }

                NoorPrayerAppRoot(
                    uiState = uiState,
                    onSetDisplayMode = viewModel::setDisplayMode,
                    onSelectTab = viewModel::selectTab,
                    onSelectCity = viewModel::selectCity,
                    onSearchLocationQueryChanged = viewModel::updateLocationSearchQuery,
                    onAddCustomLocation = viewModel::addAndSelectCustomLocation,
                    onRequestGpsLocation = viewModel::requestGpsOrSqliteFallback,
                    onMarkFeaturedAsPrayed = { viewModel.markFeaturedPrayerAsPrayed(null) },
                    onSnoozeFeaturedFiveMin = viewModel::snoozeFeaturedPrayerFiveMinutes,
                    onSimulateQadaRollover = viewModel::simulateNextPrayerAutoQadaRollover,
                    onTogglePrayerCompleted = { prayer -> viewModel.togglePrayerCompletion(prayer, null) },
                    onEditJamaahClicked = { prayer -> viewModel.openJamaahEditor(prayer, "JAMAAH") },
                    onEditAdhanClicked = { prayer -> viewModel.openJamaahEditor(prayer, "ADHAN") },
                    onSaveJamaahOffset = viewModel::saveJamaahOffset,
                    onSavePrayerTimeSettings = viewModel::savePrayerTimeSettings,
                    onDismissJamaahDialog = viewModel::dismissJamaahEditor,
                    onStepAthkaar = viewModel::stepAthkaarCounter,
                    onToggleAthkaarComplete = viewModel::toggleAthkaarCompletion,
                    onBannerMessage = viewModel::showTransientBanner,
                    onToggleCountdownMode = viewModel::toggleNextPrayerCountdownMode,
                    onUpdateRepeatInterval = viewModel::updateNotificationRepeatInterval,
                    onMarkFeaturedAsPrayedWithMode = viewModel::markFeaturedPrayerAsPrayed,
                    onTogglePrayerCompletedWithMode = viewModel::togglePrayerCompletion,
                    onSetPrayerJamaahStatus = viewModel::setPrayerJamaahStatus,
                    onUpdateHijriOffset = viewModel::updateHijriDayOffset,
                    onUpdateAutoJamaahWindow = viewModel::updateAutoJamaahWindowMinutes,
                    onAdjustQazaBacklog = viewModel::adjustQazaBacklogCount,
                    onUpdateHistoricalPrayer = viewModel::updateHistoricalPrayerRecord,
                    onUpdateAutoSilenceDuringJamaah = viewModel::updateAutoSilenceDuringJamaah,
                    onUpdateThemeMode = viewModel::updateThemeMode,
                    onCycleThemeMode = viewModel::cycleThemeMode
                )
            }
        }
    }
}
