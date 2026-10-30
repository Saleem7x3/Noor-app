package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CrispOffWhite
import com.example.ui.theme.DeepSlateDarker
import com.example.ui.theme.ElegantGold
import com.example.ui.theme.ElevatedSlateSurface
import com.example.ui.theme.EmeraldCompleted
import com.example.ui.theme.GoldContainerDark
import com.example.ui.theme.MutedPeriwinkle
import com.example.ui.theme.OnGoldDark
import com.example.ui.theme.RadiantAmberGlow
import com.example.ui.theme.SlateStrokeBorder
import com.example.viewmodel.PrayerDashboardUiState

/**
 * Clean, user-facing Settings screen replacing the former Architecture tab.
 * Provides stacked-row chip selectors (preventing any horizontal squeezing) for:
 * - Pre-Adhan Reminder (default 5m before Adhan)
 * - Pre-Jama'ah Reminder & Snooze Unlock (default 15m before Jama'ah)
 * - Post-Jama'ah Periodic Reminder (starts +10m after Jama'ah if unticked, default every 10m)
 * - Urgent End-of-Window Warning (default 30m before prayer ends)
 * - Location & Calculation Method
 */
@Composable
fun ArchitectureBlueprintScreenContent(
    uiState: PrayerDashboardUiState,
    onRequestGpsLocation: () -> Unit,
    onSimulateQadaRollover: () -> Unit = {},
    onMarkFeaturedAsPrayed: () -> Unit = {},
    onUpdateNotificationSettings: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
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
        // Card 1: Pre-Prayer Reminders (Pre-Adhan & Pre-Jama'ah)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_pre_prayer_reminders_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSlateSurface),
            border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = ElegantGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Pre-Prayer Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            color = CrispOffWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Alerts before Mosque Adhan and Congregation (Jama'ah)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedPeriwinkle
                        )
                    }
                }

                // 1A. Pre-Adhan Reminder (Default 5m before Adhan)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pre-Adhan Reminder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrispOffWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            color = GoldContainerDark,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = if (uiState.preAdhanAlertMinutes == 0) "Off" else "${uiState.preAdhanAlertMinutes}m before Adhan",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElegantGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 5, 10, 15).forEach { mins ->
                            val isSelected = uiState.preAdhanAlertMinutes == mins
                            Surface(
                                color = if (isSelected) ElegantGold else DeepSlateDarker,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) ElegantGold else SlateStrokeBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onUpdateNotificationSettings(
                                            uiState.nagIntervalMinutes,
                                            uiState.urgentEndWindowMinutes,
                                            mins,
                                            uiState.preJamaahAlertMinutes
                                        )
                                    }
                                    .testTag("pre_adhan_chip_${mins}m")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (mins == 0) "Off" else "${mins}m",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) OnGoldDark else CrispOffWhite,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // 1B. Pre-Jama'ah Reminder (Default 15m before Jama'ah; unlocks Snooze)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pre-Jama'ah Reminder",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CrispOffWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Enables Snooze 5m once this reminder fires",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedPeriwinkle
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = GoldContainerDark,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "${uiState.preJamaahAlertMinutes}m before Jama'ah",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElegantGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 20, 30).forEach { mins ->
                            val isSelected = uiState.preJamaahAlertMinutes == mins
                            Surface(
                                color = if (isSelected) ElegantGold else DeepSlateDarker,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) ElegantGold else SlateStrokeBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onUpdateNotificationSettings(
                                            uiState.nagIntervalMinutes,
                                            uiState.urgentEndWindowMinutes,
                                            uiState.preAdhanAlertMinutes,
                                            mins
                                        )
                                    }
                                    .testTag("pre_jamaah_chip_${mins}m")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) OnGoldDark else CrispOffWhite,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 2: Post-Jama'ah & Urgent End-of-Window Reminders
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_post_jamaah_reminders_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSlateSurface),
            border = BorderStroke(1.dp, SlateStrokeBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = RadiantAmberGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Unticked Prayer Follow-Up",
                            style = MaterialTheme.typography.titleMedium,
                            color = CrispOffWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Starts +10m after Jama'ah if prayer is still not marked",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedPeriwinkle
                        )
                    }
                }

                // 2A. Periodic Post-Jama'ah Repeat Interval (Default 10m)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Repeat Interval After +10m Post-Jama'ah",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrispOffWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            color = GoldContainerDark,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, RadiantAmberGlow.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "Every ${uiState.nagIntervalMinutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = RadiantAmberGlow,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 12, 15, 20, 30).forEach { intervalMin ->
                            val isSelected = uiState.nagIntervalMinutes == intervalMin
                            Surface(
                                color = if (isSelected) ElegantGold else DeepSlateDarker,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) ElegantGold else SlateStrokeBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onUpdateNotificationSettings(
                                            intervalMin,
                                            uiState.urgentEndWindowMinutes,
                                            uiState.preAdhanAlertMinutes,
                                            uiState.preJamaahAlertMinutes
                                        )
                                    }
                                    .testTag("nag_interval_chip_${intervalMin}m")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${intervalMin}m",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) OnGoldDark else CrispOffWhite,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // 2B. Urgent End-of-Window Warning Threshold (Default 30m before end)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Urgent End-of-Window Warning",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrispOffWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            color = CoralAlert.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, CoralAlert.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "Last ${uiState.urgentEndWindowMinutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = CoralAlert,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 20, 30, 45).forEach { urgentMin ->
                            val isSelected = uiState.urgentEndWindowMinutes == urgentMin
                            Surface(
                                color = if (isSelected) CoralAlert.copy(alpha = 0.25f) else DeepSlateDarker,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) CoralAlert else SlateStrokeBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onUpdateNotificationSettings(
                                            uiState.nagIntervalMinutes,
                                            urgentMin,
                                            uiState.preAdhanAlertMinutes,
                                            uiState.preJamaahAlertMinutes
                                        )
                                    }
                                    .testTag("urgent_window_chip_${urgentMin}m")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Last ${urgentMin}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) CoralAlert else CrispOffWhite,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 3: Location & Calculation Method
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSlateSurface),
            border = BorderStroke(1.dp, SlateStrokeBorder)
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
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = null,
                            tint = EmeraldCompleted,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Location & Calculation Method",
                            style = MaterialTheme.typography.titleMedium,
                            color = CrispOffWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = DeepSlateDarker,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, SlateStrokeBorder)
                    ) {
                        Text(
                            text = "MWL (18° / 17°)",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElegantGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "${uiState.selectedCity.name} (${uiState.selectedCity.latitude}°, ${uiState.selectedCity.longitude}°)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedPeriwinkle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                        text = "Refresh Current GPS Location",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
