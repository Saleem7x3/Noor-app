package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PrayerCompletionStatus
import com.example.data.PrayerItemUiModel
import com.example.data.SolarWarningItemUiModel
import com.example.domain.CityLocation
import com.example.domain.PrayerTimeCalculator
import com.example.ui.components.AppleHairlineDivider
import com.example.ui.components.AutoSizeSingleLineText
import com.example.ui.components.HeroWaveBackdropCanvas
import com.example.ui.components.SwissInsetGroup
import com.example.ui.components.SwissSectionLabel
import com.example.ui.components.appleSpringClickable
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.HeroCardStyle
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.NoorTheme
import com.example.ui.theme.arabicCalligraphyStyle
import com.example.ui.theme.asMonoPrecision
import com.example.ui.theme.withTabularDigits
import com.example.viewmodel.PrayerDashboardUiState

@Composable
fun PrayerHomeScreenContent(
    uiState: PrayerDashboardUiState,
    onSelectCity: (CityLocation) -> Unit,
    onSearchLocationQueryChanged: (String) -> Unit,
    onAddCustomLocation: (String, String, Double, Double) -> Unit,
    onRequestGpsLocation: () -> Unit,
    onTogglePrayerCompleted: (PrayerItemUiModel) -> Unit,
    onTogglePrayerCompletedWithMode: (PrayerItemUiModel, Boolean?) -> Unit = { prayer, _ ->
        onTogglePrayerCompleted(prayer)
    },
    onSetPrayerJamaahStatus: (PrayerItemUiModel, Boolean) -> Unit = { _, _ -> },
    onEditJamaahClicked: (PrayerItemUiModel) -> Unit,
    onSaveJamaahOffset: (String, Int, Boolean, Int) -> Unit,
    onDismissJamaahDialog: () -> Unit,
    onEditAdhanClicked: (PrayerItemUiModel) -> Unit = onEditJamaahClicked,
    onSavePrayerTimeSettings: (String, Int, Boolean, Int, Int, Boolean, Int) -> Unit = { id, _, _, _, jOff, jFixed, jMin ->
        onSaveJamaahOffset(id, jOff, jFixed, jMin)
    },
    onToggleCountdownMode: () -> Unit = {},
    onCycleThemeMode: () -> Unit = {},
    onPrayerUnchecked: (PrayerItemUiModel, Boolean) -> Unit = { _, _ -> },
    onClearUndoForPrayer: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    isCompactShowcase: Boolean = false
) {
    val colors = NoorTheme.colors
    val haptics = LocalHapticFeedback.current

    var showLocationSearchModal by remember { mutableStateOf(false) }
    var isSolarBannerExpanded by remember { mutableStateOf(false) }
    var pendingCompletionPrayer by remember { mutableStateOf<PrayerItemUiModel?>(null) }

    val featuredPrayer = uiState.featuredPrayer
    val featuredAdhan = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.adhanMinutes) } ?: "03:45 PM"
    val featuredJamaah = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.jamaahMinutes) } ?: "04:00 PM"
    val featuredEnd = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.displayEndMinutes) } ?: "05:18 PM"

    val heroProgressFraction = remember(
        featuredPrayer?.id,
        featuredPrayer?.pstMinutes,
        featuredPrayer?.endMinutes,
        uiState.currentClockMinutesNow,
        uiState.isFeaturedPrayerActiveWindow
    ) {
        if (featuredPrayer != null && uiState.isFeaturedPrayerActiveWindow) {
            val totalSpan = (featuredPrayer.endMinutes - featuredPrayer.pstMinutes).coerceAtLeast(1)
            val elapsed = (uiState.currentClockMinutesNow - featuredPrayer.pstMinutes).coerceIn(0, totalSpan)
            (elapsed.toFloat() / totalSpan.toFloat()).coerceIn(0.03f, 1f)
        } else {
            0f
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onRequestGpsLocation()
    }

    val themeQuickIcon = when (uiState.themeMode) {
        AppThemeMode.SYSTEM_AUTO -> Icons.Rounded.BrightnessAuto
        AppThemeMode.APPLE_WHITE_LIGHT -> Icons.Rounded.LightMode
        AppThemeMode.APPLE_OLED_DARK -> Icons.Rounded.DarkMode
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("screen_2_prayer_dashboard")
    ) {
        val density = LocalDensity.current
        val isTightDisplay = isCompactShowcase || maxWidth < 375.dp || density.fontScale > 1.05f

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (isTightDisplay) 16.dp else 22.dp)
        ) {
            // 1. Borderless Editorial Top Bar (compact auto-fit single row, zero ellipsis)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val badgeParts = uiState.dayAndDateBadgeText.split(" • ")
                val engDay = badgeParts.firstOrNull().orEmpty()
                val arabDay = badgeParts.getOrNull(1).orEmpty()
                val compactCityLabel = remember(uiState.selectedCity.name) {
                    val segments = uiState.selectedCity.name
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    if (segments.size > 1 && uiState.selectedCity.name.length > 14) {
                        segments.first()
                    } else {
                        uiState.selectedCity.name
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .weight(0.46f)
                        .testTag("day_date_badge")
                ) {
                    AutoSizeSingleLineText(
                        text = engDay,
                        style = MaterialTheme.typography.bodyMedium,
                        maxFontSize = if (isTightDisplay) 12.5.sp else 13.5.sp,
                        minFontSize = 10.sp,
                        color = colors.inkSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    if (arabDay.isNotEmpty()) {
                        Text(
                            text = arabDay,
                            style = arabicCalligraphyStyle(
                                fontSize = if (isTightDisplay) 13.5.sp else 14.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.accentGold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(0.54f)
                ) {
                    // Bare GPS icon trigger
                    IconButton(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("gps_status_badge")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GpsFixed,
                            contentDescription = "Use Live GPS",
                            tint = colors.inkTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Borderless Compact City Selector (tapping opens full location modal)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(8.dp))
                            .appleSpringClickable(shape = RoundedCornerShape(8.dp)) {
                                showLocationSearchModal = true
                            }
                            .padding(horizontal = 5.dp, vertical = 6.dp)
                            .testTag("city_dropdown_selector")
                    ) {
                        AutoSizeSingleLineText(
                            text = compactCityLabel,
                            style = MaterialTheme.typography.labelLarge,
                            maxFontSize = if (isTightDisplay) 12.5.sp else 13.5.sp,
                            minFontSize = 10.sp,
                            color = colors.inkPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.select_city),
                            tint = colors.inkTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Minimalist Quick-Toggle Theme Icon (Auto -> White -> OLED)
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCycleThemeMode()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("theme_quick_toggle_button")
                    ) {
                        Icon(
                            imageVector = themeQuickIcon,
                            contentDescription = "Switch appearance (${uiState.themeMode.displayName})",
                            tint = colors.inkSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Android 12-14+ Exact Alarm Permission Guard Strip (flat hairline)
            if (!uiState.canScheduleExactAlarms) {
                val context = LocalContext.current
                SwissInsetGroup(
                    borderColor = colors.accentCoral.copy(alpha = 0.45f),
                    cornerRadius = 12.dp,
                    modifier = Modifier.testTag("exact_alarm_permission_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Exact alarm access disabled",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.accentCoral,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Allow Alarms & Reminders for reliable alerts",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Enable",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.accentGold,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .appleSpringClickable(shape = RoundedCornerShape(8.dp)) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                        runCatching {
                                            val intent = android.content.Intent(
                                                android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                android.net.Uri.parse("package:${context.packageName}")
                                            ).apply {
                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        }.onFailure {
                                            runCatching {
                                                val fallback = android.content.Intent(
                                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                    android.net.Uri.parse("package:${context.packageName}")
                                                ).apply {
                                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(fallback)
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("enable_exact_alarms_button")
                        )
                    }
                }
            }

            // 2. Switchable Hero Presentation: Wave Card (Default) <-> Minimal Editorial Open
            AnimatedContent(
                targetState = uiState.heroCardStyle,
                transitionSpec = {
                    fadeIn(tween(220)).togetherWith(fadeOut(tween(160)))
                },
                label = "prayer_hero_style_transition"
            ) { heroStyle ->
                val isUrgentHero = uiState.isUrgentEndOfWindow || uiState.isIshaMissedAfterMidnight
                val wavePrimaryTint = when {
                    isUrgentHero -> colors.accentCoral
                    uiState.isAllTodayPrayersCompleted -> colors.accentEmerald
                    else -> colors.accentGold
                }
                val waveSecondaryTint = when {
                    isUrgentHero -> colors.accentGold
                    uiState.isAllTodayPrayersCompleted -> colors.accentGold
                    else -> colors.accentEmerald
                }

                if (heroStyle == HeroCardStyle.WAVE_CARD) {
                    Surface(
                        color = colors.surfaceCard,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(
                            0.5.dp,
                            if (isUrgentHero) colors.accentCoral.copy(alpha = 0.45f) else colors.hairlineStrong.copy(alpha = 0.7f)
                        ),
                        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hero_current_prayer_card")
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            HeroWaveBackdropCanvas(
                                accentColor = wavePrimaryTint,
                                secondaryColor = waveSecondaryTint,
                                isDark = colors.isDark,
                                modifier = Modifier
                                    .matchParentSize()
                                    .testTag("hero_wave_backdrop_canvas")
                            )
                            PrayerHeroInnerContent(
                                uiState = uiState,
                                featuredPrayer = featuredPrayer,
                                featuredAdhan = featuredAdhan,
                                featuredJamaah = featuredJamaah,
                                featuredEnd = featuredEnd,
                                heroProgressFraction = heroProgressFraction,
                                isTightDisplay = isTightDisplay,
                                onEditJamaahClicked = onEditJamaahClicked,
                                onRequestMarkLateIsha = { pendingCompletionPrayer = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = if (isTightDisplay) 16.dp else 20.dp,
                                        vertical = if (isTightDisplay) 16.dp else 20.dp
                                    )
                            )
                        }
                    }
                } else {
                    PrayerHeroInnerContent(
                        uiState = uiState,
                        featuredPrayer = featuredPrayer,
                        featuredAdhan = featuredAdhan,
                        featuredJamaah = featuredJamaah,
                        featuredEnd = featuredEnd,
                        heroProgressFraction = heroProgressFraction,
                        isTightDisplay = isTightDisplay,
                        onEditJamaahClicked = onEditJamaahClicked,
                        onRequestMarkLateIsha = { pendingCompletionPrayer = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (isTightDisplay) 4.dp else 8.dp, bottom = 2.dp)
                            .testTag("hero_current_prayer_card")
                    )
                }
            }

            // 3. Single Grouped Inset Surface for the 5 Daily Prayers (crisp 0.5dp hairline dividers)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SwissSectionLabel(
                    title = if (isTightDisplay) "Daily Prayers" else stringResource(R.string.daily_prayers_title),
                    trailingText = "ADHAN · JAMA'AH"
                )

                SwissInsetGroup {
                    uiState.prayers.forEachIndexed { index, prayer ->
                        val activeProgressFraction = if (prayer.isCurrent) {
                            val totalSpan = (prayer.endMinutes - prayer.pstMinutes).coerceAtLeast(1)
                            val elapsed = (uiState.currentClockMinutesNow - prayer.pstMinutes).coerceIn(0, totalSpan)
                            (elapsed.toFloat() / totalSpan.toFloat()).coerceIn(0.04f, 1f)
                        } else {
                            null
                        }

                        SwissPrayerRowItem(
                            prayer = prayer,
                            activeProgressFraction = activeProgressFraction,
                            onToggleCompleted = {
                                if (prayer.isPrayed) {
                                    onPrayerUnchecked(prayer, prayer.isInJamaah)
                                    onTogglePrayerCompletedWithMode(prayer, null)
                                } else if (!prayer.canMarkPrayed) {
                                    onTogglePrayerCompletedWithMode(prayer, null)
                                } else {
                                    pendingCompletionPrayer = prayer
                                }
                            },
                            onEditAdhan = { onEditAdhanClicked(prayer) },
                            onEditJamaah = { onEditJamaahClicked(prayer) },
                            isCompact = isTightDisplay
                        )

                        if (index < uiState.prayers.lastIndex) {
                            AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }

            // 4. Collapsible Prohibited Solar Times (Makruh) — Zero-Ellipsis Horological Preview
            if (uiState.solarWarnings.isNotEmpty()) {
                val anyActiveSolarWarning = uiState.solarWarnings.any { it.isActiveNow }
                val activeSolarTitle = uiState.solarWarnings.firstOrNull { it.isActiveNow }?.title

                SwissInsetGroup(
                    borderColor = if (anyActiveSolarWarning) colors.accentCoral.copy(alpha = 0.5f) else colors.hairline,
                    containerColor = if (anyActiveSolarWarning) colors.accentCoralWash else colors.surfaceCard,
                    modifier = Modifier.testTag("collapsible_solar_warning_banner")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSolarBannerExpanded = !isSolarBannerExpanded }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (anyActiveSolarWarning) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(colors.accentCoral)
                                    )
                                }
                                Text(
                                    text = if (anyActiveSolarWarning) {
                                        "Prohibited Window Active · $activeSolarTitle"
                                    } else {
                                        "Prohibited Solar Windows"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (anyActiveSolarWarning) colors.accentCoral else colors.inkPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Icon(
                                imageVector = if (isSolarBannerExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = if (isSolarBannerExpanded) "Collapse prohibited solar times" else "Expand prohibited solar times",
                                tint = colors.inkTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (!isSolarBannerExpanded) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                uiState.solarWarnings.forEachIndexed { idx, warning ->
                                    val shortLabel = when {
                                        warning.id.contains("SUNRISE", ignoreCase = true) ||
                                            warning.title.contains("Rising", ignoreCase = true) -> "SUNRISE"
                                        warning.id.contains("ZENITH", ignoreCase = true) ||
                                            warning.id.contains("ZAWAL", ignoreCase = true) ||
                                            warning.title.contains("Head", ignoreCase = true) -> "ZAWAL"
                                        else -> "SUNSET"
                                    }
                                    val align = when (idx) {
                                        0 -> Alignment.Start
                                        uiState.solarWarnings.lastIndex -> Alignment.End
                                        else -> Alignment.CenterHorizontally
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = align,
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        AutoSizeSingleLineText(
                                            text = shortLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = IbmPlexMonoFontFamily,
                                                letterSpacing = 0.6.sp
                                            ),
                                            maxFontSize = 9.sp,
                                            minFontSize = 7.5.sp,
                                            color = if (warning.isActiveNow) colors.accentCoral else colors.inkTertiary
                                        )
                                        AutoSizeSingleLineText(
                                            text = PrayerTimeCalculator.formatMinutesTo12Hour(warning.startMinutes),
                                            style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                                            maxFontSize = 10.5.sp,
                                            minFontSize = 8.5.sp,
                                            color = if (warning.isActiveNow) colors.accentCoral else colors.inkSecondary,
                                            fontWeight = if (warning.isActiveNow) FontWeight.SemiBold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isSolarBannerExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            uiState.solarWarnings.forEach { warningItem ->
                                AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                SolarWarningRowItem(
                                    item = warningItem,
                                    isCompact = isTightDisplay
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modals & Editors
    val targetCompletionPrayer = pendingCompletionPrayer
    if (targetCompletionPrayer != null) {
        val isWithinJamaahWindow = uiState.autoJamaahWindowMinutes > 0 &&
            kotlin.math.abs(uiState.currentClockMinutesNow - targetCompletionPrayer.jamaahMinutes) <= uiState.autoJamaahWindowMinutes
        PrayerCompletionChoiceDialog(
            prayer = targetCompletionPrayer,
            isWithinJamaahWindow = isWithinJamaahWindow,
            autoJamaahWindowMinutes = uiState.autoJamaahWindowMinutes,
            onChooseMode = { inJamaah ->
                onClearUndoForPrayer(targetCompletionPrayer.id)
                onTogglePrayerCompletedWithMode(targetCompletionPrayer, inJamaah)
                pendingCompletionPrayer = null
            },
            onDismiss = { pendingCompletionPrayer = null }
        )
    }

    if (showLocationSearchModal) {
        LocationSearchModalDialog(
            uiState = uiState,
            onDismiss = {
                onSearchLocationQueryChanged("")
                showLocationSearchModal = false
            },
            onQueryChanged = onSearchLocationQueryChanged,
            onSelectLocation = { city ->
                onSelectCity(city)
                showLocationSearchModal = false
            },
            onAddCustomLocation = { name, region, lat, lon ->
                onAddCustomLocation(name, region, lat, lon)
                showLocationSearchModal = false
            },
            onUseLiveGps = {
                showLocationSearchModal = false
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        )
    }

    PrayerTimeConfigDialog(
        uiState = uiState,
        onSavePrayerTimeSettings = onSavePrayerTimeSettings,
        onDismissJamaahDialog = onDismissJamaahDialog
    )
}

@Composable
private fun PrayerHeroInnerContent(
    uiState: PrayerDashboardUiState,
    featuredPrayer: PrayerItemUiModel?,
    featuredAdhan: String,
    featuredJamaah: String,
    featuredEnd: String,
    heroProgressFraction: Float,
    isTightDisplay: Boolean,
    onEditJamaahClicked: (PrayerItemUiModel) -> Unit,
    onRequestMarkLateIsha: (PrayerItemUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NoorTheme.colors

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (isTightDisplay) 12.dp else 16.dp)
    ) {
        // Eyebrow Status (Left) & Quiet Solar Marker (Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (uiState.isUrgentEndOfWindow || uiState.isIshaMissedAfterMidnight) {
                                colors.accentCoral
                            } else {
                                colors.accentGold
                            }
                        )
                )
                AutoSizeSingleLineText(
                    text = uiState.heroHeaderStatusLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = IbmPlexMonoFontFamily,
                        letterSpacing = 0.9.sp
                    ),
                    maxFontSize = if (isTightDisplay) 10.sp else 10.5.sp,
                    minFontSize = 8.sp,
                    color = if (uiState.isUrgentEndOfWindow || uiState.isIshaMissedAfterMidnight) {
                        colors.accentCoral
                    } else {
                        colors.accentGold
                    },
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            AutoSizeSingleLineText(
                text = uiState.heroSolarBadgeText,
                style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                maxFontSize = if (isTightDisplay) 10.sp else 10.5.sp,
                minFontSize = 8.5.sp,
                color = colors.inkTertiary
            )
        }

        // Oversized Swiss Display Prayer Title + Full Unclipped Arabic + Live Countdown
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = featuredPrayer?.nameEnglish ?: "Asr",
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = if (isTightDisplay) 40.sp else 48.sp,
                    lineHeight = if (isTightDisplay) 44.sp else 52.sp,
                    color = colors.inkPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = featuredPrayer?.nameArabic ?: "العصر",
                    style = arabicCalligraphyStyle(
                        fontSize = if (isTightDisplay) 23.sp else 27.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.accentGold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            AutoSizeSingleLineText(
                text = uiState.heroCountdownSubtitleText,
                style = MaterialTheme.typography.titleLarge.withTabularDigits(),
                maxFontSize = if (isTightDisplay) 17.sp else 20.sp,
                minFontSize = 12.5.sp,
                color = when {
                    uiState.isUrgentEndOfWindow || uiState.isIshaMissedAfterMidnight -> colors.accentCoral
                    uiState.isJamaahTimePassed -> colors.accentGold
                    else -> colors.inkSecondary
                },
                fontWeight = FontWeight.Normal,
                modifier = Modifier.testTag("hero_countdown_status_text")
            )
        }

        // Ultra-thin 2dp edge-to-edge window hairline progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(colors.hairline)
        ) {
            if (heroProgressFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(heroProgressFraction)
                        .background(
                            if (uiState.isUrgentEndOfWindow || uiState.isIshaMissedAfterMidnight) {
                                colors.accentCoral
                            } else {
                                colors.accentGold
                            }
                        )
                )
            }
        }

        // Borderless 3-column Horological Schedule Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                AutoSizeSingleLineText(
                    text = uiState.heroLeftTimeLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = IbmPlexMonoFontFamily,
                        letterSpacing = 0.7.sp
                    ),
                    maxFontSize = 9.5.sp,
                    minFontSize = 8.sp,
                    color = colors.inkTertiary
                )
                AutoSizeSingleLineText(
                    text = uiState.heroLeftTimeValue.ifEmpty { featuredAdhan },
                    style = MaterialTheme.typography.titleMedium.asMonoPrecision(),
                    maxFontSize = if (isTightDisplay) 13.5.sp else 15.sp,
                    minFontSize = 11.sp,
                    color = colors.inkPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Tappable Jama'ah Column with crisp vector Edit icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .appleSpringClickable(shape = RoundedCornerShape(8.dp)) {
                        featuredPrayer?.let { onEditJamaahClicked(it) }
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("hero_jamaah_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    AutoSizeSingleLineText(
                        text = if (featuredPrayer?.settingsKey == "JUMUAH") "JUMU'AH" else "JAMA'AH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = IbmPlexMonoFontFamily,
                            letterSpacing = 0.7.sp
                        ),
                        maxFontSize = 9.5.sp,
                        minFontSize = 8.sp,
                        color = colors.accentGold
                    )
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Jama'ah time",
                        tint = colors.accentGold,
                        modifier = Modifier.size(10.dp)
                    )
                }
                AutoSizeSingleLineText(
                    text = featuredJamaah,
                    style = MaterialTheme.typography.titleMedium.asMonoPrecision(),
                    maxFontSize = if (isTightDisplay) 13.5.sp else 15.sp,
                    minFontSize = 11.sp,
                    color = colors.accentGold,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                AutoSizeSingleLineText(
                    text = "ENDS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = IbmPlexMonoFontFamily,
                        letterSpacing = 0.7.sp
                    ),
                    maxFontSize = 9.5.sp,
                    minFontSize = 8.sp,
                    color = colors.inkTertiary
                )
                AutoSizeSingleLineText(
                    text = featuredEnd,
                    style = MaterialTheme.typography.titleMedium.asMonoPrecision(),
                    maxFontSize = if (isTightDisplay) 13.5.sp else 15.sp,
                    minFontSize = 11.sp,
                    color = colors.inkSecondary,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // Late-night unprayed Isha quick action (only shown when active)
        if (uiState.isIshaMissedAfterMidnight && featuredPrayer != null && !featuredPrayer.isPrayed) {
            Surface(
                color = colors.accentEmerald,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleSpringClickable(shape = RoundedCornerShape(10.dp)) {
                        onRequestMarkLateIsha(featuredPrayer)
                    }
                    .testTag("hero_mark_late_night_isha_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Mark Night Isha Prayed",
                        tint = colors.onAccentEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mark Night Isha Prayed",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onAccentEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SwissPrayerRowItem(
    prayer: PrayerItemUiModel,
    activeProgressFraction: Float?,
    onToggleCompleted: () -> Unit,
    onEditAdhan: () -> Unit,
    onEditJamaah: () -> Unit,
    isCompact: Boolean
) {
    val colors = NoorTheme.colors
    val isMissedQada = prayer.completionStatus == PrayerCompletionStatus.MISSED_QADA
    val isHighlighted = prayer.isCurrent || prayer.isNext
    val isLockedBeforePst = !prayer.isPrayed && !prayer.canMarkPrayed

    val pstFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(prayer.pstMinutes)
    val endFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(prayer.displayEndMinutes)
    val adhanFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(prayer.adhanMinutes)
    val jamaahFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(prayer.jamaahMinutes)

    val rowBg = if (prayer.isCurrent) colors.surfaceCardHighlight else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .testTag("prayer_row_${prayer.id.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 8.dp,
                    top = if (isCompact) 11.dp else 13.dp,
                    bottom = if (isCompact) 11.dp else 13.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Minimal Status Dot + Bilingual Name + Quiet Solar Window Range
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                prayer.isCurrent -> colors.accentGold
                                prayer.isPrayed -> colors.accentEmerald
                                isMissedQada -> colors.accentCoral
                                else -> colors.hairlineStrong
                            }
                        )
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            text = prayer.nameEnglish,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = if (isCompact) 15.sp else 16.sp,
                            color = when {
                                prayer.isCurrent -> colors.inkPrimary
                                isMissedQada -> colors.accentCoral
                                else -> colors.inkPrimary
                            },
                            fontWeight = if (prayer.isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = prayer.nameArabic,
                            style = arabicCalligraphyStyle(
                                fontSize = if (isCompact) 14.5.sp else 15.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isHighlighted) colors.accentGold else colors.inkTertiary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    AutoSizeSingleLineText(
                        text = "$pstFormatted – $endFormatted",
                        style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                        maxFontSize = 10.sp,
                        minFontSize = 8.5.sp,
                        color = colors.inkTertiary
                    )
                }
            }

            // Middle-Right: Clean Tappable Adhan & Jama'ah Columns (NO pills, NO borders)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = adhanFormatted,
                    style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                    fontSize = if (isCompact) 10.5.sp else 11.sp,
                    color = colors.inkSecondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onEditAdhan)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                        .testTag("edit_adhan_${prayer.id.lowercase()}")
                )
                Text(
                    text = jamaahFormatted,
                    style = MaterialTheme.typography.labelMedium.asMonoPrecision(),
                    fontSize = if (isCompact) 12.sp else 12.5.sp,
                    color = if (isHighlighted) colors.accentGold else colors.inkPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onEditJamaah)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                        .testTag("edit_jamaah_${prayer.id.lowercase()}")
                )
            }

            // Far Right: Accessible 48dp Circular Completion Control
            IconButton(
                onClick = onToggleCompleted,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("toggle_prayer_${prayer.id.lowercase()}")
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                prayer.isPrayed -> colors.accentEmerald
                                isMissedQada -> colors.accentCoralWash
                                else -> Color.Transparent
                            }
                        )
                        .then(
                            when {
                                prayer.isPrayed && prayer.isInJamaah ->
                                    Modifier.border(1.5.dp, colors.accentGold, CircleShape)
                                prayer.isPrayed -> Modifier
                                isMissedQada ->
                                    Modifier.border(1.dp, colors.accentCoral, CircleShape)
                                isLockedBeforePst ->
                                    Modifier.border(0.75.dp, colors.hairline, CircleShape)
                                else ->
                                    Modifier.border(1.25.dp, colors.hairlineStrong, CircleShape)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isMissedQada && !prayer.isPrayed -> {
                            Text(
                                text = "Q",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                                fontSize = 10.sp,
                                color = colors.accentCoral,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        prayer.isPrayed && prayer.isInJamaah -> {
                            Icon(
                                imageVector = Icons.Rounded.Mosque,
                                contentDescription = "${prayer.nameEnglish} completed in Jama'ah",
                                tint = colors.onAccentEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        prayer.isPrayed -> {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "${prayer.nameEnglish} completed individually",
                                tint = colors.onAccentEmerald,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        isLockedBeforePst -> {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "${prayer.nameEnglish} unlocks at Start Time ($pstFormatted)",
                                tint = colors.inkTertiary.copy(alpha = 0.55f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        else -> {
                            // Crisp open circle ready to tap
                        }
                    }
                }
            }
        }

        // Ultra-thin 1.5dp flush hairline progress along bottom edge of active prayer row
        if (activeProgressFraction != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(colors.hairline)
                    .testTag("active_prayer_row_mini_progress_${prayer.id.lowercase()}")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(activeProgressFraction.coerceIn(0.04f, 1f))
                        .background(if (prayer.isPrayed) colors.accentEmerald else colors.accentGold)
                )
            }
        }
    }
}

@Composable
private fun SolarWarningRowItem(
    item: SolarWarningItemUiModel,
    isCompact: Boolean
) {
    val colors = NoorTheme.colors
    val startFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(item.startMinutes)
    val endFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(item.endMinutes)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (item.isActiveNow) colors.accentCoralWash else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = if (isCompact) 10.dp else 11.dp)
            .testTag("solar_warning_row_${item.id.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            color = if (item.isActiveNow) colors.accentCoral else colors.inkSecondary,
            fontWeight = if (item.isActiveNow) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        AutoSizeSingleLineText(
            text = "$startFormatted – $endFormatted",
            style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
            maxFontSize = 10.5.sp,
            minFontSize = 8.5.sp,
            color = if (item.isActiveNow) colors.accentCoral else colors.inkPrimary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End
        )
    }
}
