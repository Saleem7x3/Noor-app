package com.example.ui.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.PrayerTimeCalculator
import com.example.notification.AlarmTriggerType
import com.example.notification.ExactPrayerAlarmScheduler
import com.example.notification.PrayerAlarmReceiver
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CrispOffWhite
import com.example.ui.theme.DeepSlateBlue
import com.example.ui.theme.DeepSlateDarker
import com.example.ui.theme.ElegantGold
import com.example.ui.theme.ElevatedSlateCard
import com.example.ui.theme.ElevatedSlateSurface
import com.example.ui.theme.EmeraldCompleted
import com.example.ui.theme.GoldContainerDark
import com.example.ui.theme.MutedPeriwinkle
import com.example.ui.theme.OnGoldDark
import com.example.ui.theme.RadiantAmberGlow
import com.example.ui.theme.SlateStrokeBorder
import com.example.viewmodel.PrayerDashboardUiState

/**
 * Interactive Senior Android Architecture Blueprint & Live Alarm / DataStore State Inspector.
 * Presents all 4 architectural artifacts alongside live interactive triggers for:
 * - Trigger 1: Exact Alarm @ Adhan Start
 * - Trigger 2: Exact Alarm @ 15m Before Jama'ah
 * - Trigger 3: Persistent 20m Nagging Loop (Foreground Service + AlarmManager.setExactAndAllowWhileIdle)
 * - Auto-Qada Rollover when the next prayer window begins
 * - FusedLocationProviderClient GPS resolution with SQLite local_cities fallback
 */
@Composable
fun ArchitectureBlueprintScreenContent(
    uiState: PrayerDashboardUiState,
    onRequestGpsLocation: () -> Unit,
    onSimulateQadaRollover: () -> Unit,
    onMarkFeaturedAsPrayed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val featured = uiState.featuredPrayer
    val prayerId = featured?.id ?: "ASR"
    val prayerName = featured?.nameEnglish ?: "Asr"
    val adhanTime = featured?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.adhanMinutes) } ?: "03:45 PM"
    val jamaahTime = featured?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.jamaahMinutes) } ?: "04:00 PM"

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onRequestGpsLocation()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("screen_4_architecture_blueprint"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live System & DataStore Alarm State Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("live_alarm_state_inspector_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSlateSurface),
            border = BorderStroke(1.2.dp, ElegantGold.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessAlarm,
                            contentDescription = "Live Alarm State",
                            tint = ElegantGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Live AlarmManager & DataStore Loop",
                            style = MaterialTheme.typography.titleMedium,
                            color = CrispOffWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = if (uiState.alarmRuntimeState.isForegroundLoopActive) GoldContainerDark else DeepSlateDarker,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            1.dp,
                            if (uiState.alarmRuntimeState.isForegroundLoopActive) ElegantGold else EmeraldCompleted
                        )
                    ) {
                        Text(
                            text = uiState.alarmRuntimeState.lastTriggerStage,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.alarmRuntimeState.isForegroundLoopActive) RadiantAmberGlow else EmeraldCompleted,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = uiState.alarmRuntimeState.lastEventDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedPeriwinkle
                )

                // Key Metrics Row: Pre-Jama'ah X=15m, Nag Y=20m, Active Nag Count, Qada Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArchMetricChip(
                        label = "Trigger 2 Buffer",
                        value = "-${uiState.preJamaahAlertMinutes}m Jama'ah",
                        accent = ElegantGold,
                        modifier = Modifier.weight(1f)
                    )
                    ArchMetricChip(
                        label = "Nagging Interval",
                        value = "Every ${uiState.nagIntervalMinutes}m",
                        accent = RadiantAmberGlow,
                        modifier = Modifier.weight(1f)
                    )
                    ArchMetricChip(
                        label = "Active Nag #",
                        value = "${uiState.alarmRuntimeState.nagIterationCount}",
                        accent = if (uiState.alarmRuntimeState.nagIterationCount > 0) CoralAlert else EmeraldCompleted,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Live BroadcastReceiver Trigger Simulator Buttons
                Text(
                    text = "Dispatch Real BroadcastReceiver Intents ($prayerName):",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElegantGold,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                                action = ExactPrayerAlarmScheduler.ACTION_PRAYER_ALARM_TRIGGER
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_ID, prayerId)
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_TRIGGER_TYPE, AlarmTriggerType.TRIGGER_1_ADHAN_START.name)
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_ADHAN_TIME, adhanTime)
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_TIME, jamaahTime)
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_UNTIL_JAMAAH, uiState.minutesUntilJamaah)
                            }
                            context.sendBroadcast(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("sim_trigger_1_adhan_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "1. Adhan Alarm",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrispOffWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                                action = ExactPrayerAlarmScheduler.ACTION_PRAYER_ALARM_TRIGGER
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_ID, prayerId)
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_TRIGGER_TYPE, AlarmTriggerType.TRIGGER_2_PRE_JAMAAH_15M.name)
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_ADHAN_TIME, adhanTime)
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_TIME, jamaahTime)
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_MIN_UNTIL_JAMAAH, 15)
                            }
                            context.sendBroadcast(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("sim_trigger_2_jamaah_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "2. -15m Jama'ah",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElegantGold,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val nextNag = uiState.alarmRuntimeState.nagIterationCount + 1
                            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                                action = ExactPrayerAlarmScheduler.ACTION_PRAYER_ALARM_TRIGGER
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_ID, prayerId)
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_TRIGGER_TYPE, AlarmTriggerType.TRIGGER_3_NAGGING_LOOP_20M.name)
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_ADHAN_TIME, adhanTime)
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_JAMAAH_TIME, jamaahTime)
                                putExtra(ExactPrayerAlarmScheduler.EXTRA_NAG_ITERATION, nextNag)
                            }
                            context.sendBroadcast(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("sim_trigger_3_nag_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CoralAlert.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "3. +20m Nag Loop",
                            style = MaterialTheme.typography.labelSmall,
                            color = CoralAlert,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onMarkFeaturedAsPrayed,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("arch_resolve_prayed_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldCompleted,
                            contentColor = DeepSlateBlue
                        )
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mark Prayed (Stop Loop)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onSimulateQadaRollover,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("arch_qada_rollover_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateStrokeBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = null,
                            tint = RadiantAmberGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Next Prayer -> Auto Qada",
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantAmberGlow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Location Engine Card: FusedLocationProviderClient + SQLite local_cities Fallback
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSlateSurface),
            border = BorderStroke(1.dp, SlateStrokeBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = null,
                            tint = EmeraldCompleted,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Location Engine & SQLite Fallback",
                            style = MaterialTheme.typography.titleMedium,
                            color = CrispOffWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${uiState.sqliteFallbackCityCount} SQLite Cities",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElegantGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Active Source: ${uiState.locationSourceStatus} • ${uiState.selectedCity.name} (${uiState.selectedCity.latitude}°, ${uiState.selectedCity.longitude}°)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedPeriwinkle
                )

                Button(
                    onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("resolve_fused_gps_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantGold,
                        contentColor = OnGoldDark
                    )
                ) {
                    Icon(Icons.Filled.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Query FusedLocationProviderClient (Fallback to SQLite)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Architectural Artifact Breakdown Cards (1..4)
        ArchitectureBlueprintSectionCard(
            badge = "Artifact 01",
            title = "Clean Architecture & Unidirectional MVI/MVVM",
            icon = Icons.Filled.Architecture,
            bullets = listOf(
                "Presentation Layer: Stateless Jetpack Compose screens observing StateFlow<PrayerDashboardUiState> via collectAsStateWithLifecycle().",
                "Domain Layer: Offline Astronomical Adhan Engine (Muslim World League 18°/17°) + LocationResolutionManager (FusedLocationClient -> SQLite fallback).",
                "Data & Infrastructure Layer: Room Database + Jetpack DataStore<Preferences> + ExactPrayerAlarmScheduler + PrayerNaggingForegroundService."
            )
        )

        ArchitectureBlueprintSectionCard(
            badge = "Artifact 02",
            title = "Room Schema: PrayerLog, UserPreferences & LocalCities",
            icon = Icons.Filled.Storage,
            bullets = listOf(
                "PrayerRecordEntity (prayer_records): Composite PK (dateIso, prayerId), indexed on dateIso & completion_status (PENDING, PRAYED_ON_TIME, PRAYED_LATE, MISSED_QADA), plus nag_count_triggered.",
                "UserPreferencesEntity (user_preferences): Stores use_gps_location, selected_city_id, pre_jamaah_alert_minutes (X=15m), nag_interval_minutes (Y=20m), and auto_qada_on_next_prayer.",
                "LocalCityEntity (local_cities): Offline SQLite city coordinate catalog guaranteeing zero-network fallback."
            )
        )

        ArchitectureBlueprintSectionCard(
            badge = "Artifact 03",
            title = "Exact Alarm & Persistent Foreground Nagging Loop",
            icon = Icons.Filled.NotificationsActive,
            bullets = listOf(
                "Bypassing OEM Doze Mode: Uses AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, epochMs, pendingIntent) for deterministic wake-ups.",
                "Trigger 1 (Adhan) -> Trigger 2 (15m before Jama'ah) -> Trigger 3 (20m Nagging Loop via PrayerNaggingForegroundService).",
                "Termination Policy: Loop runs every 20m until PrayerAlarmReceiver captures ACTION_MARK_PRAYED / ACTION_DISMISS_NAG, or auto-marks MISSED_QADA when the next prayer window starts."
            )
        )

        ArchitectureBlueprintSectionCard(
            badge = "Artifact 04",
            title = "Reactive Jetpack Compose StateFlow Pipeline",
            icon = Icons.Filled.SyncAlt,
            bullets = listOf(
                "Combines 5 cold/hot streams: Room PrayerDao.getAllPrayerRecords(), UserPreferencesFlow, LocalCitiesFlow, PrayerAlarmDataStore.runtimeStateFlow, and a 1Hz Coroutine Ticker.",
                "Zero UI lag: Marking a prayer as completed in the system notification banner mutates Room & DataStore in Dispatchers.IO, immediately recomposing the circular goal ring and 7-week contribution grid."
            )
        )
    }
}

@Composable
private fun ArchMetricChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DeepSlateDarker,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SlateStrokeBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MutedPeriwinkle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArchitectureBlueprintSectionCard(
    badge: String,
    title: String,
    icon: ImageVector,
    bullets: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSlateSurface),
        border = BorderStroke(1.dp, SlateStrokeBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = GoldContainerDark,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElegantGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ElegantGold,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = CrispOffWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            bullets.forEach { line ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(ElegantGold)
                    )
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedPeriwinkle
                    )
                }
            }
        }
    }
}
