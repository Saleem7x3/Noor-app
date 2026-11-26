package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AsrMadhab
import com.example.domain.CalculationMethod
import com.example.domain.CityLocation
import com.example.domain.PrayerCalculationConfig
import com.example.domain.PrayerTimeCalculator
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.NoorTheme
import com.example.ui.theme.asMonoPrecision
import com.example.ui.theme.withTabularDigits
import kotlin.math.roundToInt

/**
 * Live 3-column preview of Fajr, Asr, and Isha for the selected city & calculation config.
 */
@Composable
fun CalculationLivePreviewStrip(
    city: CityLocation,
    config: PrayerCalculationConfig,
    modifier: Modifier = Modifier
) {
    val colors = NoorTheme.colors
    val previewTimes = remember(city, config) {
        val cal = PrayerTimeCalculator.getCalendarForCity(city)
        PrayerTimeCalculator.calculateDailyPrayers(city, cal, config)
    }

    SwissInsetGroup(
        modifier = modifier.testTag("calculation_live_preview_strip"),
        containerColor = colors.surfaceCard,
        borderColor = colors.hairline
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PreviewTimeColumn(
                label = "FAJR",
                timeText = PrayerTimeCalculator.formatMinutesTo12Hour(previewTimes.fajrMinutes),
                accentColor = colors.accentGold,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(28.dp)
                    .background(colors.hairline)
            )
            PreviewTimeColumn(
                label = if (config.asrMadhab == AsrMadhab.HANAFI) "ASR · HANAFI" else "ASR · STD",
                timeText = PrayerTimeCalculator.formatMinutesTo12Hour(previewTimes.asrMinutes),
                accentColor = colors.accentEmerald,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(28.dp)
                    .background(colors.hairline)
            )
            PreviewTimeColumn(
                label = "ISHA",
                timeText = PrayerTimeCalculator.formatMinutesTo12Hour(previewTimes.ishaMinutes),
                accentColor = colors.inkPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PreviewTimeColumn(
    label: String,
    timeText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = NoorTheme.colors
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        AutoSizeSingleLineText(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = IbmPlexMonoFontFamily,
                letterSpacing = 0.7.sp
            ),
            maxFontSize = 9.sp,
            minFontSize = 7.5.sp,
            color = colors.inkTertiary
        )
        AutoSizeSingleLineText(
            text = timeText,
            style = MaterialTheme.typography.titleSmall.withTabularDigits(),
            maxFontSize = 13.5.sp,
            minFontSize = 10.sp,
            color = accentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Asr Madhab selector (Standard: Shafi'i / Maliki / Hanbali vs Hanafi later Asr).
 */
@Composable
fun AsrMadhabSelectorBlock(
    selectedMadhab: AsrMadhab,
    onSelectMadhab: (AsrMadhab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NoorTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppleSegmentedControl(
            options = listOf(
                AsrMadhab.STANDARD to "Standard (Shafi'i)",
                AsrMadhab.HANAFI to "Hanafi (Later Asr)"
            ),
            selected = selectedMadhab,
            onSelect = onSelectMadhab,
            optionTagPrefix = "asr_madhab_option",
            modifier = Modifier.testTag("asr_madhab_segmented_control")
        )
        Text(
            text = when (selectedMadhab) {
                AsrMadhab.STANDARD -> "Shafi'i, Maliki & Hanbali: Asr begins when an object's shadow equals 1× its length."
                AsrMadhab.HANAFI -> "Hanafi: Asr begins later, when an object's shadow equals 2× its length."
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkSecondary
        )
    }
}

/**
 * Full global Calculation Method list + Manual Custom Fajr/Isha angle/minute controls.
 */
@Composable
fun CalculationMethodSelectorList(
    config: PrayerCalculationConfig,
    onUpdateConfig: (PrayerCalculationConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NoorTheme.colors
    SwissInsetGroup(modifier = modifier.testTag("calculation_methods_list_group")) {
        CalculationMethod.entries.forEachIndexed { index, method ->
            val isSelected = config.method == method
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val updated = if (method == CalculationMethod.MANUAL) {
                            config.copy(method = CalculationMethod.MANUAL)
                        } else {
                            config.copy(
                                method = method,
                                manualFajrAngle = method.fajrAngle,
                                manualIshaUseFixedMinutes = method.ishaMinutesAfterMaghrib != null,
                                manualIshaAngle = if (method.ishaAngle > 0.0) method.ishaAngle else config.manualIshaAngle,
                                manualIshaMinutesAfterMaghrib = method.ishaMinutesAfterMaghrib ?: config.manualIshaMinutesAfterMaghrib
                            )
                        }
                        onUpdateConfig(updated)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("calc_method_row_${method.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = method.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isSelected) colors.accentGold else colors.inkPrimary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                    Text(
                        text = method.regionHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (method == CalculationMethod.MANUAL && isSelected) {
                            config.angleSpecLabel
                        } else {
                            method.angleSpecLabel
                        },
                        style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                        color = if (isSelected) colors.accentGold else colors.inkTertiary
                    )

                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) colors.accentGold else Color.Transparent)
                            .then(
                                if (!isSelected) {
                                    Modifier.border(1.25.dp, colors.hairlineStrong, CircleShape)
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected ${method.displayName}",
                                tint = colors.onAccentGold,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            if (method == CalculationMethod.MANUAL) {
                AnimatedVisibility(visible = config.method == CalculationMethod.MANUAL) {
                    ManualAngleEditorPanel(
                        config = config,
                        onUpdateConfig = onUpdateConfig
                    )
                }
            }

            if (index < CalculationMethod.entries.lastIndex) {
                AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun ManualAngleEditorPanel(
    config: PrayerCalculationConfig,
    onUpdateConfig: (PrayerCalculationConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NoorTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceNested)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("manual_angle_editor_panel"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Fajr Angle Stepper + Slider (10.0° - 24.0°)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fajr Twilight Angle",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.inkPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Sun degrees below eastern horizon",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary
                    )
                }
                AngleStepperControls(
                    valueLabel = CalculationMethod.formatAngle(config.manualFajrAngle),
                    onDecrement = {
                        val next = ((config.manualFajrAngle - 0.5) * 10.0).roundToInt() / 10.0
                        onUpdateConfig(config.copy(manualFajrAngle = next.coerceIn(10.0, 24.0)))
                    },
                    onIncrement = {
                        val next = ((config.manualFajrAngle + 0.5) * 10.0).roundToInt() / 10.0
                        onUpdateConfig(config.copy(manualFajrAngle = next.coerceIn(10.0, 24.0)))
                    },
                    decrementTag = "manual_fajr_angle_minus",
                    incrementTag = "manual_fajr_angle_plus"
                )
            }
            Slider(
                value = config.manualFajrAngle.toFloat(),
                onValueChange = { raw ->
                    val stepped = (raw * 2f).roundToInt() / 2.0
                    onUpdateConfig(config.copy(manualFajrAngle = stepped.coerceIn(10.0, 24.0)))
                },
                valueRange = 10f..24f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accentGold,
                    activeTrackColor = colors.accentGold,
                    inactiveTrackColor = colors.hairlineStrong
                ),
                modifier = Modifier.testTag("manual_fajr_angle_slider")
            )
        }

        AppleHairlineDivider()

        // Isha Mode Toggle (Twilight Angle ° vs Fixed Minutes after Maghrib)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Isha Calculation Mode",
                style = MaterialTheme.typography.titleSmall,
                color = colors.inkPrimary,
                fontWeight = FontWeight.Medium
            )
            AppleSegmentedControl(
                options = listOf(
                    false to "Twilight Angle (°)",
                    true to "Fixed Min after Maghrib"
                ),
                selected = config.manualIshaUseFixedMinutes,
                onSelect = { useFixed ->
                    onUpdateConfig(config.copy(manualIshaUseFixedMinutes = useFixed))
                },
                optionTagPrefix = "manual_isha_mode",
                modifier = Modifier.testTag("manual_isha_mode_toggle")
            )

            if (config.manualIshaUseFixedMinutes) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Minutes after Maghrib",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    AngleStepperControls(
                        valueLabel = "${config.manualIshaMinutesAfterMaghrib}m",
                        onDecrement = {
                            onUpdateConfig(
                                config.copy(
                                    manualIshaMinutesAfterMaghrib = (config.manualIshaMinutesAfterMaghrib - 5).coerceIn(45, 150)
                                )
                            )
                        },
                        onIncrement = {
                            onUpdateConfig(
                                config.copy(
                                    manualIshaMinutesAfterMaghrib = (config.manualIshaMinutesAfterMaghrib + 5).coerceIn(45, 150)
                                )
                            )
                        },
                        decrementTag = "manual_isha_min_minus",
                        incrementTag = "manual_isha_min_plus"
                    )
                }
                Slider(
                    value = config.manualIshaMinutesAfterMaghrib.toFloat(),
                    onValueChange = { raw ->
                        val stepped = ((raw / 5f).roundToInt() * 5).coerceIn(45, 150)
                        onUpdateConfig(config.copy(manualIshaMinutesAfterMaghrib = stepped))
                    },
                    valueRange = 45f..150f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accentGold,
                        activeTrackColor = colors.accentGold,
                        inactiveTrackColor = colors.hairlineStrong
                    ),
                    modifier = Modifier.testTag("manual_isha_minutes_slider")
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sun degrees below western horizon",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    AngleStepperControls(
                        valueLabel = CalculationMethod.formatAngle(config.manualIshaAngle),
                        onDecrement = {
                            val next = ((config.manualIshaAngle - 0.5) * 10.0).roundToInt() / 10.0
                            onUpdateConfig(config.copy(manualIshaAngle = next.coerceIn(10.0, 24.0)))
                        },
                        onIncrement = {
                            val next = ((config.manualIshaAngle + 0.5) * 10.0).roundToInt() / 10.0
                            onUpdateConfig(config.copy(manualIshaAngle = next.coerceIn(10.0, 24.0)))
                        },
                        decrementTag = "manual_isha_angle_minus",
                        incrementTag = "manual_isha_angle_plus"
                    )
                }
                Slider(
                    value = config.manualIshaAngle.toFloat(),
                    onValueChange = { raw ->
                        val stepped = (raw * 2f).roundToInt() / 2.0
                        onUpdateConfig(config.copy(manualIshaAngle = stepped.coerceIn(10.0, 24.0)))
                    },
                    valueRange = 10f..24f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accentGold,
                        activeTrackColor = colors.accentGold,
                        inactiveTrackColor = colors.hairlineStrong
                    ),
                    modifier = Modifier.testTag("manual_isha_angle_slider")
                )
            }
        }
    }
}

@Composable
private fun AngleStepperControls(
    valueLabel: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementTag: String,
    incrementTag: String
) {
    val colors = NoorTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceCard)
                .border(0.5.dp, colors.hairlineStrong, RoundedCornerShape(8.dp))
                .clickable(onClick = onDecrement)
                .testTag(decrementTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Remove,
                contentDescription = "Decrease value",
                tint = colors.inkPrimary,
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = valueLabel,
            style = MaterialTheme.typography.titleSmall.asMonoPrecision(),
            color = colors.accentGold,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceCard)
                .border(0.5.dp, colors.hairlineStrong, RoundedCornerShape(8.dp))
                .clickable(onClick = onIncrement)
                .testTag(incrementTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Increase value",
                tint = colors.inkPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
