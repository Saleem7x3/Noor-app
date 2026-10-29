package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PrayerCompletionStatus
import com.example.data.PrayerItemUiModel
import com.example.domain.CityLocation
import com.example.domain.PrayerTimeCalculator
import com.example.ui.components.GlowingWaveCanvas
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CrispOffWhite
import com.example.ui.theme.DeepSlateBlue
import com.example.ui.theme.DeepSlateDarker
import com.example.ui.theme.ElegantGold
import com.example.ui.theme.ElevatedSlateCard
import com.example.ui.theme.ElevatedSlateSurface
import com.example.ui.theme.EmeraldCompleted
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.GoldContainerDark
import com.example.ui.theme.MutedPeriwinkle
import com.example.ui.theme.OnGoldDark
import com.example.ui.theme.RadiantAmberGlow
import com.example.ui.theme.SlateStrokeBorder
import com.example.viewmodel.PrayerDashboardUiState

/**
 * Screen 2: Home & Prayer Dashboard
 * Features:
 * - Top bar showing FusedLocationProviderClient GPS status and a manual SQLite city dropdown selector
 * - Current prayer time displayed prominently with a subtle glowing animated wave pattern
 * - Vertical list of the 5 daily prayers showing Start Time (Adhan), adjustable Jama'ah Time
 *   (with an edit icon), Qada/Nag indicators, and checkmarks for completion
 */
@Composable
fun PrayerHomeScreenContent(
    uiState: PrayerDashboardUiState,
    onSelectCity: (CityLocation) -> Unit,
    onRequestGpsLocation: () -> Unit,
    onTogglePrayerCompleted: (PrayerItemUiModel) -> Unit,
    onEditJamaahClicked: (PrayerItemUiModel) -> Unit,
    onSaveJamaahOffset: (String, Int) -> Unit,
    onDismissJamaahDialog: () -> Unit,
    modifier: Modifier = Modifier,
    isCompactShowcase: Boolean = false
) {
    var cityDropdownExpanded by remember { mutableStateOf(false) }
    val featuredPrayer = uiState.featuredPrayer
    val featuredAdhan = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.adhanMinutes) } ?: "03:45 PM"
    val featuredJamaah = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.jamaahMinutes) } ?: "04:00 PM"

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onRequestGpsLocation()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("screen_2_prayer_dashboard"),
        verticalArrangement = Arrangement.spacedBy(if (isCompactShowcase) 12.dp else 16.dp)
    ) {
        // Top Bar: GPS Location Status (clickable to trigger FusedLocationProvider) + SQLite Manual City Dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // GPS Location Status Pill
            Surface(
                color = ElevatedSlateSurface,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, EmeraldCompleted.copy(alpha = 0.45f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                    .testTag("gps_status_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.GpsFixed,
                        contentDescription = "GPS status active",
                        tint = EmeraldCompleted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.gps_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = CrispOffWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Manual City Dropdown Selector (Backed by local_cities SQLite table)
            Box {
                Surface(
                    color = ElevatedSlateSurface,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { cityDropdownExpanded = true }
                        .testTag("city_dropdown_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${uiState.selectedCity.name}, ${uiState.selectedCity.country.take(2).uppercase()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElegantGold,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = stringResource(R.string.select_city),
                            tint = ElegantGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = cityDropdownExpanded,
                    onDismissRequest = { cityDropdownExpanded = false },
                    modifier = Modifier.background(ElevatedSlateCard)
                ) {
                    uiState.availableCities.forEach { city ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "${city.name}, ${city.country}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (city.id == uiState.selectedCity.id) ElegantGold else CrispOffWhite,
                                        fontWeight = if (city.id == uiState.selectedCity.id) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = "SQLite Fallback • Lat ${city.latitude}°, Lon ${city.longitude}°",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedPeriwinkle
                                    )
                                }
                            },
                            onClick = {
                                onSelectCity(city)
                                cityDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("city_option_${city.id}")
                        )
                    }
                }
            }
        }

        // Hero Card: Current Prayer Time with Subtle Glowing Animated Wave Pattern
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompactShowcase) 168.dp else 188.dp)
                .testTag("hero_current_prayer_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSlateSurface),
            border = BorderStroke(1.2.dp, ElegantGold.copy(alpha = 0.55f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                ElevatedSlateCard,
                                DeepSlateBlue
                            )
                        )
                    )
            ) {
                GlowingWaveCanvas(
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isCompactShowcase) 16.dp else 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.next_prayer_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = ElegantGold,
                            fontWeight = FontWeight.Bold
                        )

                        Surface(
                            color = DeepSlateDarker.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, SlateStrokeBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WbSunny,
                                    contentDescription = "Sunrise time",
                                    tint = RadiantAmberGlow,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Shuruq ${uiState.sunriseTimeFormatted}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedPeriwinkle
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = featuredPrayer?.nameEnglish ?: "Asr",
                                    style = if (isCompactShowcase) {
                                        MaterialTheme.typography.headlineLarge
                                    } else {
                                        MaterialTheme.typography.displayMedium
                                    },
                                    color = CrispOffWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = featuredPrayer?.nameArabic ?: "العصر",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = ElegantGold
                                )
                            }
                            Text(
                                text = featuredAdhan,
                                style = MaterialTheme.typography.headlineMedium,
                                color = RadiantAmberGlow,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                color = GoldContainerDark.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "Jama'ah $featuredJamaah",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ElegantGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "In ${uiState.minutesUntilJamaah}m ${uiState.currentClockSeconds}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrispOffWhite,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Section Header for 5 Daily Prayers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.daily_prayers_title),
                style = MaterialTheme.typography.titleMedium,
                color = CrispOffWhite,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    text = stringResource(R.string.start_time_column),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedPeriwinkle
                )
                Text(
                    text = stringResource(R.string.jamaah_time_column),
                    style = MaterialTheme.typography.labelSmall,
                    color = ElegantGold
                )
                Text(
                    text = stringResource(R.string.status_column),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedPeriwinkle
                )
            }
        }

        // Vertical List of the 5 Daily Prayers
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.prayers.forEach { prayer ->
                PrayerRowCard(
                    prayer = prayer,
                    onToggleCompleted = { onTogglePrayerCompleted(prayer) },
                    onEditJamaah = { onEditJamaahClicked(prayer) },
                    isCompact = isCompactShowcase
                )
            }
        }
    }

    // Adjustable Jama'ah Time Modal Dialog
    val editingPrayer = uiState.editingJamaahPrayer
    if (editingPrayer != null) {
        var offsetValue by remember(editingPrayer.id) {
            mutableIntStateOf(editingPrayer.jamaahOffsetMinutes)
        }

        AlertDialog(
            onDismissRequest = onDismissJamaahDialog,
            containerColor = ElevatedSlateCard,
            titleContentColor = CrispOffWhite,
            textContentColor = MutedPeriwinkle,
            title = {
                Text(
                    text = stringResource(R.string.edit_jamaah_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.edit_jamaah_desc, editingPrayer.nameEnglish),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = { offsetValue = (offsetValue - 5).coerceAtLeast(0) },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DeepSlateBlue)
                                .testTag("jamaah_minus_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = "Decrease 5 minutes",
                                tint = ElegantGold
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "+$offsetValue min",
                                style = MaterialTheme.typography.headlineMedium,
                                color = ElegantGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Jama'ah: ${PrayerTimeCalculator.formatMinutesTo12Hour(editingPrayer.adhanMinutes + offsetValue)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrispOffWhite
                            )
                        }

                        IconButton(
                            onClick = { offsetValue = (offsetValue + 5).coerceAtMost(90) },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DeepSlateBlue)
                                .testTag("jamaah_plus_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Increase 5 minutes",
                                tint = ElegantGold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onSaveJamaahOffset(editingPrayer.id, offsetValue) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantGold,
                        contentColor = OnGoldDark
                    ),
                    modifier = Modifier.testTag("save_jamaah_offset_button")
                ) {
                    Text(stringResource(R.string.save_action), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissJamaahDialog) {
                    Text(stringResource(R.string.cancel_action), color = MutedPeriwinkle)
                }
            }
        )
    }
}

@Composable
private fun PrayerRowCard(
    prayer: PrayerItemUiModel,
    onToggleCompleted: () -> Unit,
    onEditJamaah: () -> Unit,
    isCompact: Boolean
) {
    val isMissedQada = prayer.completionStatus == PrayerCompletionStatus.MISSED_QADA
    val borderColor = when {
        prayer.isCurrent -> ElegantGold
        prayer.isPrayed -> EmeraldCompleted.copy(alpha = 0.45f)
        isMissedQada -> CoralAlert.copy(alpha = 0.65f)
        else -> SlateStrokeBorder
    }
    val containerColor = when {
        prayer.isCurrent -> ElevatedSlateCard
        else -> ElevatedSlateSurface
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (prayer.isCurrent) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_row_${prayer.id.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isCompact) 12.dp else 14.dp, vertical = if (isCompact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Active indicator + Prayer Name (English & Arabic)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1.1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                prayer.isCurrent -> ElegantGold
                                prayer.isPrayed -> EmeraldCompleted
                                isMissedQada -> CoralAlert
                                else -> SlateStrokeBorder
                            }
                        )
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = prayer.nameEnglish,
                            style = MaterialTheme.typography.titleMedium,
                            color = when {
                                prayer.isCurrent -> ElegantGold
                                isMissedQada -> CoralAlert
                                else -> CrispOffWhite
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = prayer.nameArabic,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedPeriwinkle
                        )
                    }
                    val subtitleText = when {
                        isMissedQada -> "Missed (Qada) • Tap ✓ to make up"
                        prayer.nagCountTriggered > 0 && !prayer.isPrayed -> "Nagging loop #${prayer.nagCountTriggered} (20m)"
                        prayer.isCurrent -> "Active • Alert -15m Jama'ah"
                        else -> "+${prayer.jamaahOffsetMinutes}m Jama'ah"
                    }
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isMissedQada -> CoralAlert
                            prayer.isCurrent -> RadiantAmberGlow
                            else -> MutedPeriwinkle
                        }
                    )
                }
            }

            // Middle: Start Time (Adhan) + Adjustable Jama'ah Time with Edit Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = PrayerTimeCalculator.formatMinutesTo24Hour(prayer.adhanMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedPeriwinkle,
                    fontWeight = FontWeight.Medium
                )

                Surface(
                    color = if (prayer.isCurrent) GoldContainerDark else DeepSlateDarker,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onEditJamaah)
                        .testTag("edit_jamaah_${prayer.id.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = PrayerTimeCalculator.formatMinutesTo24Hour(prayer.jamaahMinutes),
                            style = MaterialTheme.typography.labelLarge,
                            color = ElegantGold,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit ${prayer.nameEnglish} Jama'ah time",
                            tint = ElegantGold,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                IconButton(
                    onClick = onToggleCompleted,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("toggle_prayer_${prayer.id.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    prayer.isPrayed -> EmeraldCompleted
                                    isMissedQada -> CoralAlert.copy(alpha = 0.25f)
                                    else -> EmeraldContainer.copy(alpha = 0.35f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = if (prayer.isPrayed) {
                                "${prayer.nameEnglish} completed"
                            } else {
                                "Mark ${prayer.nameEnglish} as completed"
                            },
                            tint = if (prayer.isPrayed) DeepSlateBlue else MutedPeriwinkle.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
