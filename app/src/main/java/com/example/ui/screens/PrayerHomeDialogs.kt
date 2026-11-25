package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.PrayerCompletionStatus
import com.example.data.PrayerItemUiModel
import com.example.domain.CityLocation
import com.example.domain.PrayerTimeCalculator
import com.example.ui.components.AppleHairlineDivider
import com.example.ui.components.SwissInsetGroup
import com.example.ui.components.appleSpringClickable
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.NoorTheme
import com.example.ui.theme.asMonoPrecision
import com.example.ui.theme.withTabularDigits
import com.example.viewmodel.PrayerDashboardUiState
import java.util.Locale

@Composable
internal fun PrayerCompletionChoiceDialog(
    prayer: PrayerItemUiModel,
    isWithinJamaahWindow: Boolean,
    autoJamaahWindowMinutes: Int,
    onChooseMode: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = NoorTheme.colors
    val jamaahFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(prayer.jamaahMinutes)
    val isQaza = prayer.completionStatus == PrayerCompletionStatus.MISSED_QADA

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("prayer_completion_choice_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceCard,
            border = BorderStroke(0.5.dp, colors.hairline),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Log ${prayer.nameEnglish}",
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.inkPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = prayer.nameArabic,
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = AmiriFontFamily),
                                fontSize = 19.sp,
                                color = colors.accentGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (isQaza) {
                                "Record Qaza make-up or congregation"
                            } else {
                                "Select how you performed ${prayer.nameEnglish}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("dismiss_completion_choice_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = colors.inkTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AppleHairlineDivider()

                // Option 1: Prayed in Jama'ah (flat hairline row, zero icon-tile box)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isWithinJamaahWindow) colors.accentEmeraldWash.copy(alpha = 0.55f) else Color.Transparent)
                        .clickable { onChooseMode(true) }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .testTag("popup_choose_in_jamaah"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mosque,
                            contentDescription = "Prayed in Jama'ah",
                            tint = colors.accentEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Prayed in Jama'ah",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.inkPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                if (isWithinJamaahWindow) {
                                    Text(
                                        text = "AUTO",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = IbmPlexMonoFontFamily,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.8.sp
                                        ),
                                        color = colors.accentEmerald,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Text(
                                text = if (isWithinJamaahWindow) {
                                    "Within ±${autoJamaahWindowMinutes}m of Jama'ah ($jamaahFormatted)"
                                } else {
                                    "Congregation · Jama'ah $jamaahFormatted"
                                },
                                style = MaterialTheme.typography.bodySmall.withTabularDigits(),
                                color = colors.inkSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = colors.accentEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AppleHairlineDivider(modifier = Modifier.padding(horizontal = 20.dp))

                // Option 2: Prayed Individually
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChooseMode(false) }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .testTag("popup_choose_individually"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = "Prayed Individually",
                            tint = colors.inkSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Prayed Individually",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.inkPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isQaza) {
                                    "Log as Qaza make-up prayer"
                                } else {
                                    "Munfarid · Prayed on time"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = colors.inkTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimeConfigDialog(
    uiState: PrayerDashboardUiState,
    onSavePrayerTimeSettings: (String, Int, Boolean, Int, Int, Boolean, Int) -> Unit,
    onDismissJamaahDialog: () -> Unit
) {
    val colors = NoorTheme.colors
    val editingPrayer = uiState.editingJamaahPrayer
    if (editingPrayer != null) {
        val isMiddayPrayer = editingPrayer.id == "DHUHR" || editingPrayer.settingsKey == "JUMUAH"
        var selectedSettingsKey by remember(editingPrayer.id, editingPrayer.settingsKey) {
            mutableStateOf(
                if (isMiddayPrayer) {
                    if (editingPrayer.settingsKey == "JUMUAH" || uiState.isFridayActiveSheet) "JUMUAH" else "DHUHR"
                } else {
                    editingPrayer.settingsKey
                }
            )
        }

        var activeTierTab by remember(editingPrayer.id, uiState.editingInitialTab) {
            mutableStateOf(uiState.editingInitialTab.ifBlank { "JAMAAH" })
        }

        val savedSettingForKey = uiState.prayerSettingsMap[selectedSettingsKey]
        val pstMin = editingPrayer.pstMinutes

        var adhanOffsetValue by remember(editingPrayer.id, selectedSettingsKey) {
            mutableIntStateOf(savedSettingForKey?.adhanOffsetMinutes ?: editingPrayer.adhanOffsetMinutes)
        }
        var useFixedAdhanMode by remember(editingPrayer.id, selectedSettingsKey) {
            mutableStateOf(savedSettingForKey?.useFixedAdhanTime ?: editingPrayer.useFixedAdhanTime)
        }
        var fixedAdhanMinutesValue by remember(editingPrayer.id, selectedSettingsKey) {
            mutableIntStateOf(
                savedSettingForKey?.fixedAdhanTimeMinutes
                    ?: (pstMin + (savedSettingForKey?.adhanOffsetMinutes ?: editingPrayer.adhanOffsetMinutes)).coerceIn(0, 1439)
            )
        }

        val resolvedAdhanMinutes = if (useFixedAdhanMode) {
            fixedAdhanMinutesValue.coerceIn(0, 1439).coerceAtLeast(pstMin)
        } else {
            (pstMin + adhanOffsetValue).coerceAtLeast(pstMin)
        }

        var jamaahOffsetValue by remember(editingPrayer.id, selectedSettingsKey) {
            mutableIntStateOf(
                savedSettingForKey?.jamaahOffsetMinutes ?: when (selectedSettingsKey) {
                    "JUMUAH" -> uiState.jumuahJamaahOffsetMinutes
                    "DHUHR" -> uiState.dhuhrJamaahOffsetMinutes
                    else -> editingPrayer.jamaahOffsetMinutes
                }
            )
        }
        var useFixedJamaahMode by remember(editingPrayer.id, selectedSettingsKey) {
            mutableStateOf(
                savedSettingForKey?.useFixedTime ?: if (selectedSettingsKey == "JUMUAH") uiState.jumuahUseFixedTime else editingPrayer.useFixedTime
            )
        }
        var fixedJamaahMinutesValue by remember(editingPrayer.id, selectedSettingsKey) {
            mutableIntStateOf(
                savedSettingForKey?.fixedTimeMinutes
                    ?: (resolvedAdhanMinutes + jamaahOffsetValue).coerceIn(0, 1439)
            )
        }

        val resolvedJamaahMinutes = if (useFixedJamaahMode) {
            fixedJamaahMinutesValue.coerceAtLeast(resolvedAdhanMinutes)
        } else {
            (resolvedAdhanMinutes + jamaahOffsetValue).coerceAtLeast(resolvedAdhanMinutes)
        }

        val activeDisplayName = when (selectedSettingsKey) {
            "JUMUAH" -> "Friday Jumu'ah"
            "DHUHR" -> "Daily Dhuhr"
            else -> editingPrayer.nameEnglish
        }

        val pst12h = PrayerTimeCalculator.formatMinutesTo12Hour(pstMin)
        val adhan12h = PrayerTimeCalculator.formatMinutesTo12Hour(resolvedAdhanMinutes)
        val adhan24h = PrayerTimeCalculator.formatMinutesTo24Hour(resolvedAdhanMinutes)
        val jamaah12h = PrayerTimeCalculator.formatMinutesTo12Hour(resolvedJamaahMinutes)
        val jamaah24h = PrayerTimeCalculator.formatMinutesTo24Hour(resolvedJamaahMinutes)

        AlertDialog(
            onDismissRequest = onDismissJamaahDialog,
            containerColor = colors.surfaceCard,
            titleContentColor = colors.inkPrimary,
            textContentColor = colors.inkSecondary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = activeDisplayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.inkPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "PST $pst12h",
                        style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                        color = colors.inkTertiary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isMiddayPrayer) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.segmentedTrack)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isJumuahTab = selectedSettingsKey == "JUMUAH"
                            Surface(
                                color = if (isJumuahTab) colors.segmentedThumb else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedSettingsKey = "JUMUAH" }
                                    .testTag("tab_config_jumuah")
                            ) {
                                Text(
                                    text = "Jumu'ah (Fri)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 12.sp,
                                    color = if (isJumuahTab) colors.inkPrimary else colors.inkSecondary,
                                    fontWeight = if (isJumuahTab) FontWeight.SemiBold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp),
                                    maxLines = 1
                                )
                            }

                            Surface(
                                color = if (!isJumuahTab) colors.segmentedThumb else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedSettingsKey = "DHUHR" }
                                    .testTag("tab_config_dhuhr")
                            ) {
                                Text(
                                    text = "Dhuhr (Daily)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 12.sp,
                                    color = if (!isJumuahTab) colors.inkPrimary else colors.inkSecondary,
                                    fontWeight = if (!isJumuahTab) FontWeight.SemiBold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Three-Tier Horological Summary Strip (borderless hairline)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppleHairlineDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("PST", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = colors.inkTertiary)
                                Text(pst12h, style = MaterialTheme.typography.labelMedium.asMonoPrecision(), color = colors.inkSecondary)
                            }
                            Text("→", color = colors.inkTertiary, fontSize = 11.sp)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ADHAN", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = colors.inkTertiary)
                                Text(adhan12h, style = MaterialTheme.typography.labelMedium.asMonoPrecision(), color = colors.inkPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Text("→", color = colors.inkTertiary, fontSize = 11.sp)
                            Column(horizontalAlignment = Alignment.End) {
                                Text("JAMA'AH", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = colors.accentGold)
                                Text(jamaah12h, style = MaterialTheme.typography.labelMedium.asMonoPrecision(), color = colors.accentGold, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        AppleHairlineDivider()
                    }

                    // Tier Selector Tab
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.segmentedTrack)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isAdhanTab = activeTierTab == "ADHAN"
                        Surface(
                            color = if (isAdhanTab) colors.segmentedThumb else Color.Transparent,
                            shape = RoundedCornerShape(7.5.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(7.5.dp))
                                .clickable { activeTierTab = "ADHAN" }
                                .testTag("tier_tab_adhan")
                        ) {
                            Text(
                                text = "Adhan Time",
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 12.sp,
                                color = if (isAdhanTab) colors.inkPrimary else colors.inkSecondary,
                                fontWeight = if (isAdhanTab) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 8.dp),
                                maxLines = 1
                            )
                        }

                        Surface(
                            color = if (!isAdhanTab) colors.segmentedThumb else Color.Transparent,
                            shape = RoundedCornerShape(7.5.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(7.5.dp))
                                .clickable { activeTierTab = "JAMAAH" }
                                .testTag("tier_tab_jamaah")
                        ) {
                            Text(
                                text = "Jama'ah Time",
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 12.sp,
                                color = if (!isAdhanTab) colors.inkPrimary else colors.inkSecondary,
                                fontWeight = if (!isAdhanTab) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 8.dp),
                                maxLines = 1
                            )
                        }
                    }

                    // Mode Toggle: Specific Clock Time vs Offset
                    val isCurrentTierFixed = if (activeTierTab == "ADHAN") useFixedAdhanMode else useFixedJamaahMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.segmentedTrack)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = if (isCurrentTierFixed) colors.segmentedThumb else Color.Transparent,
                            shape = RoundedCornerShape(7.5.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(7.5.dp))
                                .clickable {
                                    if (activeTierTab == "ADHAN") {
                                        fixedAdhanMinutesValue = resolvedAdhanMinutes
                                        useFixedAdhanMode = true
                                    } else {
                                        fixedJamaahMinutesValue = resolvedJamaahMinutes
                                        useFixedJamaahMode = true
                                    }
                                }
                                .testTag("mode_specific_clock_time")
                        ) {
                            Text(
                                text = "Fixed Time",
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 11.5.sp,
                                color = if (isCurrentTierFixed) colors.accentGold else colors.inkSecondary,
                                fontWeight = if (isCurrentTierFixed) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                maxLines = 1
                            )
                        }

                        Surface(
                            color = if (!isCurrentTierFixed) colors.segmentedThumb else Color.Transparent,
                            shape = RoundedCornerShape(7.5.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(7.5.dp))
                                .clickable {
                                    if (activeTierTab == "ADHAN") {
                                        adhanOffsetValue = (resolvedAdhanMinutes - pstMin).coerceIn(0, 180)
                                        useFixedAdhanMode = false
                                    } else {
                                        jamaahOffsetValue = (resolvedJamaahMinutes - resolvedAdhanMinutes).coerceIn(0, 180)
                                        useFixedJamaahMode = false
                                    }
                                }
                                .testTag("mode_offset_minutes")
                        ) {
                            Text(
                                text = if (activeTierTab == "ADHAN") "PST Offset" else "Adhan Offset",
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 11.5.sp,
                                color = if (!isCurrentTierFixed) colors.accentGold else colors.inkSecondary,
                                fontWeight = if (!isCurrentTierFixed) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                maxLines = 1
                            )
                        }
                    }

                    // Active Value Readout (borderless typographic readout)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (activeTierTab == "ADHAN") adhan12h else jamaah12h,
                            style = MaterialTheme.typography.headlineMedium.asMonoPrecision(),
                            color = colors.accentGold,
                            fontWeight = FontWeight.SemiBold
                        )
                        val detailText = if (activeTierTab == "ADHAN") {
                            if (useFixedAdhanMode) "$adhan24h · Fixed Adhan"
                            else "$adhan24h · +${adhanOffsetValue}m from PST"
                        } else {
                            if (useFixedJamaahMode) "$jamaah24h · Fixed Jama'ah"
                            else "$jamaah24h · +${jamaahOffsetValue}m from Adhan"
                        }
                        Text(
                            text = detailText,
                            style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                            color = colors.inkTertiary,
                            textAlign = TextAlign.Center
                        )
                    }

                    val minAllowedClock = if (activeTierTab == "ADHAN") (pstMin - 60).coerceAtLeast(0) else resolvedAdhanMinutes
                    val maxAllowedClock = (pstMin + 240).coerceAtMost(1439)

                    if (isCurrentTierFixed) {
                        var useDialMode by remember { mutableStateOf(false) }
                        val activeFixedMinutes = if (activeTierTab == "ADHAN") fixedAdhanMinutesValue else fixedJamaahMinutesValue
                        val initialHour = (activeFixedMinutes / 60).coerceIn(0, 23)
                        val initialMinute = (activeFixedMinutes % 60).coerceIn(0, 59)

                        val timePickerState = rememberTimePickerState(
                            initialHour = initialHour,
                            initialMinute = initialMinute,
                            is24Hour = false
                        )

                        LaunchedEffect(timePickerState.hour, timePickerState.minute) {
                            val pickedTotal = (timePickerState.hour * 60 + timePickerState.minute).coerceIn(0, 1439)
                            if (activeTierTab == "ADHAN") {
                                if (fixedAdhanMinutesValue != pickedTotal) fixedAdhanMinutesValue = pickedTotal
                            } else {
                                if (fixedJamaahMinutesValue != pickedTotal) fixedJamaahMinutesValue = pickedTotal
                            }
                        }

                        LaunchedEffect(activeFixedMinutes, activeTierTab) {
                            val targetH = (activeFixedMinutes / 60).coerceIn(0, 23)
                            val targetM = (activeFixedMinutes % 60).coerceIn(0, 59)
                            if (timePickerState.hour != targetH || timePickerState.minute != targetM) {
                                timePickerState.hour = targetH
                                timePickerState.minute = targetM
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") {
                                        fixedAdhanMinutesValue = (fixedAdhanMinutesValue - 5).coerceAtLeast(minAllowedClock)
                                    } else {
                                        fixedJamaahMinutesValue = (fixedJamaahMinutesValue - 5).coerceAtLeast(minAllowedClock)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, colors.hairlineStrong),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("time_picker_minus_5m_button")
                            ) {
                                Text("-5m", style = MaterialTheme.typography.labelSmall.asMonoPrecision(), color = colors.inkPrimary)
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { useDialMode = !useDialMode }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("toggle_time_picker_mode_button"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (useDialMode) Icons.Rounded.Keyboard else Icons.Rounded.Schedule,
                                    contentDescription = "Switch Numpad or Clock Dial",
                                    tint = colors.accentGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (useDialMode) "Keyboard" else "Clock Dial",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 12.sp,
                                    color = colors.inkSecondary
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") {
                                        fixedAdhanMinutesValue = (fixedAdhanMinutesValue + 5).coerceAtMost(maxAllowedClock)
                                    } else {
                                        fixedJamaahMinutesValue = (fixedJamaahMinutesValue + 5).coerceAtMost(maxAllowedClock)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, colors.hairlineStrong),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("time_picker_plus_5m_button")
                            ) {
                                Text("+5m", style = MaterialTheme.typography.labelSmall.asMonoPrecision(), color = colors.inkPrimary)
                            }
                        }

                        val pickerColors = TimePickerDefaults.colors(
                            clockDialColor = colors.surfaceNested,
                            clockDialSelectedContentColor = colors.onAccentGold,
                            clockDialUnselectedContentColor = colors.inkPrimary,
                            selectorColor = colors.accentGold,
                            containerColor = colors.surfaceNested,
                            periodSelectorBorderColor = colors.hairlineStrong,
                            periodSelectorSelectedContainerColor = colors.accentGoldWash,
                            periodSelectorUnselectedContainerColor = colors.surfaceNested,
                            periodSelectorSelectedContentColor = colors.accentGold,
                            periodSelectorUnselectedContentColor = colors.inkSecondary,
                            timeSelectorSelectedContainerColor = colors.accentGoldWash,
                            timeSelectorUnselectedContainerColor = colors.surfaceNested,
                            timeSelectorSelectedContentColor = colors.accentGold,
                            timeSelectorUnselectedContentColor = colors.inkPrimary
                        )

                        if (useDialMode) {
                            TimePicker(
                                state = timePickerState,
                                colors = pickerColors,
                                modifier = Modifier.testTag("native_time_dial_picker")
                            )
                        } else {
                            TimeInput(
                                state = timePickerState,
                                colors = pickerColors,
                                modifier = Modifier.testTag("native_time_numpad_input")
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") {
                                        fixedAdhanMinutesValue = (fixedAdhanMinutesValue - 15).coerceAtLeast(minAllowedClock)
                                    } else {
                                        fixedJamaahMinutesValue = (fixedJamaahMinutesValue - 15).coerceAtLeast(minAllowedClock)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, colors.hairlineStrong),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
                                modifier = Modifier.testTag("jamaah_minus_button")
                            ) {
                                Text("-15m", style = MaterialTheme.typography.labelSmall.asMonoPrecision(), color = colors.inkPrimary)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") {
                                        fixedAdhanMinutesValue = (fixedAdhanMinutesValue - 1).coerceAtLeast(minAllowedClock)
                                    } else {
                                        fixedJamaahMinutesValue = (fixedJamaahMinutesValue - 1).coerceAtLeast(minAllowedClock)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, colors.hairline),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text("-1m", style = MaterialTheme.typography.labelSmall.asMonoPrecision(), color = colors.inkSecondary)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") {
                                        fixedAdhanMinutesValue = (fixedAdhanMinutesValue + 1).coerceAtMost(maxAllowedClock)
                                    } else {
                                        fixedJamaahMinutesValue = (fixedJamaahMinutesValue + 1).coerceAtMost(maxAllowedClock)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, colors.hairline),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text("+1m", style = MaterialTheme.typography.labelSmall.asMonoPrecision(), color = colors.inkSecondary)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") {
                                        fixedAdhanMinutesValue = (fixedAdhanMinutesValue + 15).coerceAtMost(maxAllowedClock)
                                    } else {
                                        fixedJamaahMinutesValue = (fixedJamaahMinutesValue + 15).coerceAtMost(maxAllowedClock)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, colors.hairlineStrong),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
                                modifier = Modifier.testTag("jamaah_plus_button")
                            ) {
                                Text("+15m", style = MaterialTheme.typography.labelSmall.asMonoPrecision(), color = colors.inkPrimary)
                            }
                        }

                        val baseQuarter = ((minAllowedClock + 14) / 15) * 15
                        val clockPresets = (0..6).map { idx ->
                            val m = (baseQuarter + idx * 15).coerceAtMost(1439)
                            PrayerTimeCalculator.formatMinutesTo12Hour(m) to m
                        }.distinctBy { it.second }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            clockPresets.forEach { (label, targetMin) ->
                                val currentVal = if (activeTierTab == "ADHAN") resolvedAdhanMinutes else resolvedJamaahMinutes
                                val isPresetSelected = currentVal == targetMin
                                Surface(
                                    color = if (isPresetSelected) colors.accentGoldWash else colors.surfaceNested,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        0.5.dp,
                                        if (isPresetSelected) colors.accentGold else colors.hairline
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (activeTierTab == "ADHAN") {
                                                fixedAdhanMinutesValue = targetMin
                                                if (useFixedJamaahMode && fixedJamaahMinutesValue < targetMin) {
                                                    fixedJamaahMinutesValue = (targetMin + 15).coerceAtMost(1439)
                                                }
                                            } else {
                                                fixedJamaahMinutesValue = targetMin
                                            }
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                                        color = if (isPresetSelected) colors.accentGold else colors.inkPrimary,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") adhanOffsetValue = (adhanOffsetValue - 5).coerceAtLeast(0)
                                    else jamaahOffsetValue = (jamaahOffsetValue - 5).coerceAtLeast(0)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, colors.hairlineStrong),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("jamaah_minus_button")
                            ) {
                                Text("-5m", style = MaterialTheme.typography.labelSmall.asMonoPrecision(), color = colors.inkPrimary)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") adhanOffsetValue = (adhanOffsetValue - 1).coerceAtLeast(0)
                                    else jamaahOffsetValue = (jamaahOffsetValue - 1).coerceAtLeast(0)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, colors.hairline),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("-1m", style = MaterialTheme.typography.labelSmall.asMonoPrecision(), color = colors.inkSecondary)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") adhanOffsetValue = (adhanOffsetValue + 1).coerceAtMost(180)
                                    else jamaahOffsetValue = (jamaahOffsetValue + 1).coerceAtMost(180)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, colors.hairline),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("+1m", style = MaterialTheme.typography.labelSmall.asMonoPrecision(), color = colors.inkSecondary)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") adhanOffsetValue = (adhanOffsetValue + 5).coerceAtMost(180)
                                    else jamaahOffsetValue = (jamaahOffsetValue + 5).coerceAtMost(180)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, colors.hairlineStrong),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("jamaah_plus_button")
                            ) {
                                Text("+5m", style = MaterialTheme.typography.labelSmall.asMonoPrecision(), color = colors.inkPrimary)
                            }
                        }

                        val offsetPresets = if (activeTierTab == "ADHAN") {
                            listOf(0, 5, 10, 15, 20, 30)
                        } else {
                            listOf(10, 15, 20, 25, 30, 45)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            offsetPresets.forEach { presetOff ->
                                val currentOff = if (activeTierTab == "ADHAN") adhanOffsetValue else jamaahOffsetValue
                                val isPresetSelected = currentOff == presetOff
                                Surface(
                                    color = if (isPresetSelected) colors.accentGoldWash else colors.surfaceNested,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        0.5.dp,
                                        if (isPresetSelected) colors.accentGold else colors.hairline
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (activeTierTab == "ADHAN") adhanOffsetValue = presetOff
                                            else jamaahOffsetValue = presetOff
                                        }
                                ) {
                                    Text(
                                        text = if (presetOff == 0) "0m" else "+${presetOff}m",
                                        style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                                        color = if (isPresetSelected) colors.accentGold else colors.inkPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSavePrayerTimeSettings(
                            selectedSettingsKey,
                            adhanOffsetValue,
                            useFixedAdhanMode,
                            resolvedAdhanMinutes,
                            jamaahOffsetValue,
                            useFixedJamaahMode,
                            resolvedJamaahMinutes
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.inkPrimary,
                        contentColor = colors.surfaceCard
                    ),
                    modifier = Modifier.testTag("save_jamaah_offset_button")
                ) {
                    Text(stringResource(R.string.save_action), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissJamaahDialog) {
                    Text(stringResource(R.string.cancel_action), color = colors.inkSecondary)
                }
            }
        )
    }
}

@Composable
internal fun LocationSearchModalDialog(
    uiState: PrayerDashboardUiState,
    onDismiss: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSelectLocation: (CityLocation) -> Unit,
    onAddCustomLocation: (String, String, Double, Double) -> Unit,
    onUseLiveGps: () -> Unit
) {
    val colors = NoorTheme.colors
    var showCustomCoordEntry by remember { mutableStateOf(false) }
    var customTownName by remember { mutableStateOf("") }
    var customRegion by remember { mutableStateOf("") }
    var customLat by remember { mutableStateOf("21.4225") }
    var customLon by remember { mutableStateOf("39.8262") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 640.dp)
                .testTag("location_search_modal"),
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceCard,
            border = BorderStroke(0.5.dp, colors.hairline),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Location",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.inkPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${uiState.availableCities.size}+ cities & worldwide search",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close location search",
                            tint = colors.inkTertiary
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.locationSearchQuery,
                    onValueChange = onQueryChanged,
                    placeholder = {
                        Text(
                            text = "Search city or neighborhood...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.inkTertiary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = colors.inkTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (uiState.isSearchingLocations) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 1.5.dp,
                                color = colors.accentGold
                            )
                        } else if (uiState.locationSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    tint = colors.inkSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.inkPrimary,
                        unfocusedBorderColor = colors.hairline,
                        focusedTextColor = colors.inkPrimary,
                        unfocusedTextColor = colors.inkPrimary,
                        focusedContainerColor = colors.canvas,
                        unfocusedContainerColor = colors.canvas
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val quickPlaces = listOf("Abu Dhabi", "Cairo", "Doha", "Dubai", "Istanbul", "Kuala Lumpur", "London", "Madinah", "Makkah")
                    quickPlaces.forEach { place ->
                        Text(
                            text = place,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.inkSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onQueryChanged(place) }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onUseLiveGps,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.inkPrimary,
                            contentColor = colors.surfaceCard
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Rounded.GpsFixed, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Use Live GPS",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = { showCustomCoordEntry = !showCustomCoordEntry },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, colors.hairlineStrong),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = colors.inkPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showCustomCoordEntry) "Hide Custom" else "Coordinates",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.inkPrimary,
                            maxLines = 1
                        )
                    }
                }

                if (showCustomCoordEntry) {
                    SwissInsetGroup(containerColor = colors.canvas, cornerRadius = 12.dp) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customTownName,
                                    onValueChange = { customTownName = it },
                                    label = { Text("Town / Area") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customRegion,
                                    onValueChange = { customRegion = it },
                                    label = { Text("Country") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customLat,
                                    onValueChange = { customLat = it },
                                    label = { Text("Latitude") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customLon,
                                    onValueChange = { customLon = it },
                                    label = { Text("Longitude") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Button(
                                onClick = {
                                    val lat = customLat.toDoubleOrNull() ?: 21.4225
                                    val lon = customLon.toDoubleOrNull() ?: 39.8262
                                    val name = customTownName.ifBlank { "Custom Town" }
                                    onAddCustomLocation(name, customRegion, lat, lon)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accentGold,
                                    contentColor = colors.onAccentGold
                                )
                            ) {
                                Text("Save & Select", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                AppleHairlineDivider()

                // Hairline-separated city list (no per-city cards)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    itemsIndexed(uiState.locationSearchResults, key = { _, it -> it.id }) { idx, city ->
                        val isSelected = city.id == uiState.selectedCity.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectLocation(city) }
                                .padding(vertical = 11.dp, horizontal = 4.dp)
                                .testTag("city_option_${city.id}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = city.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) colors.accentGold else colors.inkPrimary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                )
                                Text(
                                    text = "${city.country} · ${String.format(Locale.US, "%.2f", city.latitude)}°, ${String.format(Locale.US, "%.2f", city.longitude)}°",
                                    style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                                    color = colors.inkTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = colors.accentGold,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        if (idx < uiState.locationSearchResults.lastIndex) {
                            AppleHairlineDivider()
                        }
                    }
                }
            }
        }
    }
}
