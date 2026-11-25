package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PrayerItemUiModel
import com.example.domain.PrayerTimeCalculator
import com.example.notification.AlarmTriggerType
import com.example.notification.PrayerNotificationHelper
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
import kotlin.math.abs
import kotlinx.coroutines.delay

@Composable
fun NotificationLockScreenContent(
    uiState: PrayerDashboardUiState,
    onMarkAsPrayed: () -> Unit,
    onSnoozeFiveMinutes: () -> Unit,
    onNotificationTriggered: (String) -> Unit,
    onEditJamaahClicked: (PrayerItemUiModel) -> Unit = {},
    onSavePrayerTimeSettings: (String, Int, Boolean, Int, Int, Boolean, Int) -> Unit = { _, _, _, _, _, _, _ -> },
    onDismissJamaahDialog: () -> Unit = {},
    onToggleCountdownMode: () -> Unit = {},
    onUpdateRepeatInterval: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onMarkAsPrayedWithMode: (Boolean?) -> Unit = { _ -> onMarkAsPrayed() },
    onUpdateHijriOffset: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    isCompactShowcase: Boolean = false
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val colors = NoorTheme.colors
    val featuredPrayer = uiState.featuredPrayer
    val prayerName = featuredPrayer?.nameEnglish ?: "Asr"
    val prayerArabic = featuredPrayer?.nameArabic ?: "العصر"
    val pstTime = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.pstMinutes) } ?: "03:39 PM"
    val adhanTime = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.adhanMinutes) } ?: "04:30 PM"
    val jamaahTime = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.jamaahMinutes) } ?: "04:45 PM"
    val canMarkPrayedNow = (uiState.isFeaturedPrayerActiveWindow && featuredPrayer?.canMarkPrayed == true) ||
        uiState.isIshaMissedAfterMidnight

    val isWithinAutoJamaahWindow = featuredPrayer != null &&
        uiState.autoJamaahWindowMinutes > 0 &&
        abs(uiState.currentClockMinutesNow - featuredPrayer.jamaahMinutes) <= uiState.autoJamaahWindowMinutes

    var showHijriStepper by remember { mutableStateOf(false) }
    var showExactCheckpointTimes by remember { mutableStateOf(false) }
    var isAlertPillExpanded by remember { mutableStateOf(false) }

    val activeStage = when {
        uiState.isUrgentEndOfWindow || (uiState.isFeaturedPrayerActiveWindow && uiState.isJamaahTimePassed &&
            uiState.minutesUntilNextPrayer in 1..uiState.urgentEndWindowMinutes) ->
            AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW
        uiState.isFeaturedPrayerActiveWindow && uiState.isJamaahTimePassed ->
            AlarmTriggerType.STAGE_3_POST_JAMAAH_NAG
        uiState.isFeaturedPrayerActiveWindow && uiState.isSnoozeEnabled ->
            AlarmTriggerType.STAGE_PRE_JAMAAH
        uiState.isFeaturedPrayerActiveWindow && uiState.heroCountdownSubtitleText.startsWith("Jama'ah in") ->
            AlarmTriggerType.STAGE_2_ADHAN_COUNTDOWN
        else ->
            AlarmTriggerType.STAGE_1_PST_START
    }

    var sweepTriggerCount by remember { mutableStateOf(0) }
    var isFastSweeping by remember { mutableStateOf(false) }
    var sweepTarget by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(featuredPrayer?.id, activeStage, sweepTriggerCount, uiState.isFeaturedPrayerActiveWindow) {
        if (uiState.isFeaturedPrayerActiveWindow) {
            isFastSweeping = true
            sweepTarget = 0.18f
            delay(120L)
            sweepTarget = 1.0f
            delay(1500L)
            isFastSweeping = false
        } else {
            isFastSweeping = false
            sweepTarget = 0f
        }
    }

    val targetFraction = when {
        !uiState.isFeaturedPrayerActiveWindow -> 0f
        isFastSweeping -> sweepTarget
        else -> uiState.notificationProgressFraction
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = if (isFastSweeping) 1400 else 450),
        label = "notification_progress_bar"
    )

    val shouldBlinkStages = uiState.lockScreenBlinkItems.size > 1
    var blinkIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(featuredPrayer?.id, shouldBlinkStages) {
        if (!shouldBlinkStages) {
            blinkIndex = 0
            return@LaunchedEffect
        }
        while (true) {
            delay(2800L)
            blinkIndex = (blinkIndex + 1) % uiState.lockScreenBlinkItems.size.coerceAtLeast(1)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && uiState.isFeaturedPrayerActiveWindow) {
            PrayerNotificationHelper.showDynamicPrayerNotification(
                context = context,
                prayerName = prayerName,
                pstTimeFormatted = pstTime,
                adhanTimeFormatted = adhanTime,
                jamaahTimeFormatted = jamaahTime,
                minutesUntilJamaah = uiState.minutesUntilJamaah,
                minutesPassedSinceJamaah = uiState.minutesElapsedSinceJamaah,
                minutesRemainingUntilEnd = uiState.minutesUntilNextPrayer,
                repeatIntervalMinutes = uiState.nagIntervalMinutes,
                preJamaahAlertMinutes = uiState.preJamaahAlertMinutes,
                progressPercent = (uiState.notificationProgressFraction * 100).toInt(),
                triggerType = activeStage
            )
            onNotificationTriggered("Live notification posted for $prayerName")
        } else if (granted) {
            onNotificationTriggered("$prayerName starts at $pstTime (${uiState.heroCountdownSubtitleText})")
        } else {
            onNotificationTriggered("Notification permission denied; preview active on screen")
        }
    }

    val triggerNotificationAction = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        sweepTriggerCount += 1
        if (!uiState.isFeaturedPrayerActiveWindow) {
            onNotificationTriggered(
                "$prayerName has not started yet (Starts at $pstTime · ${uiState.heroCountdownSubtitleText})"
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val sent = PrayerNotificationHelper.showDynamicPrayerNotification(
                context = context,
                prayerName = prayerName,
                pstTimeFormatted = pstTime,
                adhanTimeFormatted = adhanTime,
                jamaahTimeFormatted = jamaahTime,
                minutesUntilJamaah = uiState.minutesUntilJamaah,
                minutesPassedSinceJamaah = uiState.minutesElapsedSinceJamaah,
                minutesRemainingUntilEnd = uiState.minutesUntilNextPrayer,
                repeatIntervalMinutes = uiState.nagIntervalMinutes,
                preJamaahAlertMinutes = uiState.preJamaahAlertMinutes,
                progressPercent = (uiState.notificationProgressFraction * 100).toInt(),
                triggerType = activeStage
            )
            if (!sent) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onNotificationTriggered("System notification posted for $prayerName")
            }
        } else {
            PrayerNotificationHelper.showDynamicPrayerNotification(
                context = context,
                prayerName = prayerName,
                pstTimeFormatted = pstTime,
                adhanTimeFormatted = adhanTime,
                jamaahTimeFormatted = jamaahTime,
                minutesUntilJamaah = uiState.minutesUntilJamaah,
                minutesPassedSinceJamaah = uiState.minutesElapsedSinceJamaah,
                minutesRemainingUntilEnd = uiState.minutesUntilNextPrayer,
                repeatIntervalMinutes = uiState.nagIntervalMinutes,
                preJamaahAlertMinutes = uiState.preJamaahAlertMinutes,
                progressPercent = (uiState.notificationProgressFraction * 100).toInt(),
                triggerType = activeStage
            )
            onNotificationTriggered("System notification posted for $prayerName")
        }
    }

    val isUrgentVisual = uiState.isIshaMissedAfterMidnight || activeStage == AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("screen_1_notification_lock")
    ) {
        val isTightDisplay = isCompactShowcase || maxWidth < 375.dp || LocalDensity.current.fontScale > 1.05f

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isTightDisplay) 18.dp else 24.dp)
        ) {
            // 1. Open Swiss Typographic Lock Clock directly on canvas (no pills, no boxed seconds)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = if (isTightDisplay) 8.dp else 18.dp)
            ) {
                // Borderless tappable Hijri date header
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showHijriStepper = !showHijriStepper
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("hijri_date_pill"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Lock screen & Hijri date adjustment",
                        tint = if (uiState.hijriDayOffset != 0) colors.accentGold else colors.inkTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    val offsetBadge = when {
                        uiState.hijriDayOffset > 0 -> " (+${uiState.hijriDayOffset}d)"
                        uiState.hijriDayOffset < 0 -> " (${uiState.hijriDayOffset}d)"
                        else -> ""
                    }
                    Text(
                        text = (uiState.hijriDateText.ifEmpty { "26 Rabi' al-Thani 1448 AH" }) + offsetBadge,
                        style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                        color = if (uiState.hijriDayOffset != 0) colors.accentGold else colors.inkSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Inline moonsighting adjustment strip
                AnimatedVisibility(
                    visible = showHijriStepper,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SwissInsetGroup(
                        cornerRadius = 12.dp,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .testTag("hijri_offset_stepper_bar")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "HIJRI MOONSIGHTING OFFSET",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = IbmPlexMonoFontFamily,
                                    letterSpacing = 0.9.sp,
                                    fontSize = 9.5.sp
                                ),
                                color = colors.inkTertiary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.segmentedTrack)
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                listOf(-2, -1, 0, 1, 2).forEach { dayOff ->
                                    val isSelected = uiState.hijriDayOffset == dayOff
                                    val label = when {
                                        dayOff == 0 -> "0d"
                                        dayOff > 0 -> "+${dayOff}d"
                                        else -> "${dayOff}d"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) colors.segmentedThumb else Color.Transparent)
                                            .clickable { onUpdateHijriOffset(dayOff) }
                                            .padding(vertical = 6.dp)
                                            .testTag("hijri_offset_chip_$dayOff"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                                            color = if (isSelected) colors.accentGold else colors.inkSecondary,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pure Open Horological Clock (no box around seconds)
                val clockMain = uiState.currentTimeFormatted.substringBefore(" ")
                val clockPeriod = uiState.currentTimeFormatted.substringAfter(" ", "PM")
                val secondsFormatted = String.format(Locale.US, ":%02d", uiState.currentClockSeconds)

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = clockMain,
                        style = MaterialTheme.typography.displayLarge.withTabularDigits(),
                        fontSize = if (isTightDisplay) 48.sp else 60.sp,
                        lineHeight = if (isTightDisplay) 52.sp else 64.sp,
                        letterSpacing = (-2.2).sp,
                        color = colors.inkPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .testTag("clock_seconds_pill"),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = secondsFormatted,
                            style = MaterialTheme.typography.labelMedium.asMonoPrecision(),
                            fontSize = if (isTightDisplay) 12.sp else 13.sp,
                            color = colors.accentGold,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = clockPeriod,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                            fontSize = 10.sp,
                            color = colors.inkTertiary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val dayParts = uiState.dayAndDateBadgeText.split(" • ")
                val englishDayPart = dayParts.firstOrNull().orEmpty()
                val arabicDayPart = dayParts.getOrNull(1).orEmpty()

                Text(
                    text = buildAnnotatedString {
                        append(englishDayPart)
                        if (arabicDayPart.isNotEmpty()) {
                            append(" · ")
                            withStyle(
                                SpanStyle(
                                    fontFamily = AmiriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accentGold,
                                    fontSize = 14.5.sp
                                )
                            ) {
                                append(arabicDayPart)
                            }
                        }
                        append(" · ${uiState.selectedCity.name}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 2. Single Calm Flat Notification Surface (zero icon-tile box, 2dp hairline timeline)
            SwissInsetGroup(
                borderColor = if (isUrgentVisual) colors.accentCoral.copy(alpha = 0.5f) else colors.hairline,
                modifier = Modifier.testTag("dynamic_notification_banner")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isTightDisplay) 16.dp else 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header: Quiet Status Dot + Mono Eyebrow (Left) + Borderless Tappable Jama'ah (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isUrgentVisual) colors.accentCoral else colors.accentGold)
                            )
                            Text(
                                text = uiState.heroHeaderStatusLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = IbmPlexMonoFontFamily,
                                    letterSpacing = 0.9.sp
                                ),
                                fontSize = 10.sp,
                                color = if (isUrgentVisual) colors.accentCoral else colors.inkSecondary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${uiState.heroJamaahPillText} ✎",
                            style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                            fontSize = 10.5.sp,
                            color = colors.accentGold,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    featuredPrayer?.let { onEditJamaahClicked(it) }
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .testTag("minutes_until_jamaah_badge")
                        )
                    }

                    // Prayer Title + Schedule Line
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = prayerName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontSize = if (isTightDisplay) 24.sp else 28.sp,
                                color = colors.inkPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = prayerArabic,
                                fontFamily = AmiriFontFamily,
                                fontSize = if (isTightDisplay) 20.sp else 23.sp,
                                color = colors.accentGold,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        Text(
                            text = uiState.lockScreenStageTimeText,
                            style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                            fontSize = 11.sp,
                            color = colors.inkSecondary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Swiss 2dp Checkpoint Timeline
                    val blinkItems = uiState.lockScreenBlinkItems
                    val safeBlinkStage = if (blinkItems.isNotEmpty()) blinkIndex % blinkItems.size else 0
                    val currentBlinkText = if (blinkItems.isNotEmpty()) {
                        blinkItems[safeBlinkStage]
                    } else {
                        uiState.heroCountdownSubtitleText
                    }
                    val jamaahCheckpointLabel = if (featuredPrayer?.settingsKey == "JUMUAH") "Jumu'ah" else "Jama'ah"
                    val adhanFrac = uiState.adhanCheckpointFraction.coerceIn(0.14f, 0.72f)
                    val jamaahFrac = uiState.jamaahCheckpointFraction.coerceIn((adhanFrac + 0.16f).coerceAtMost(0.88f), 0.88f)
                    val isAdhanReached = uiState.isFeaturedPrayerActiveWindow && animatedProgress >= adhanFrac
                    val isJamaahReached = uiState.isFeaturedPrayerActiveWindow && (uiState.isJamaahTimePassed || animatedProgress >= jamaahFrac)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        blinkIndex = (blinkIndex + 1) % blinkItems.size.coerceAtLeast(1)
                                    }
                            ) {
                                AnimatedContent(
                                    targetState = safeBlinkStage,
                                    transitionSpec = {
                                        (slideInVertically(animationSpec = tween(220)) { h -> h / 2 } +
                                            fadeIn(animationSpec = tween(220)))
                                            .togetherWith(
                                                slideOutVertically(animationSpec = tween(160)) { h -> -h / 2 } +
                                                    fadeOut(animationSpec = tween(160))
                                            )
                                    },
                                    label = "blink_stage_vertical_slide"
                                ) { stageIdx ->
                                    val phrase = if (blinkItems.isNotEmpty()) {
                                        blinkItems[stageIdx % blinkItems.size]
                                    } else {
                                        currentBlinkText
                                    }
                                    Text(
                                        text = phrase,
                                        style = MaterialTheme.typography.labelLarge.withTabularDigits(),
                                        fontSize = if (isTightDisplay) 12.sp else 13.sp,
                                        color = if (uiState.isUrgentEndOfWindow) colors.accentCoral else colors.inkPrimary,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("lock_screen_countdown_above_bar")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                                color = colors.inkTertiary
                            )
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val barWidth = maxWidth
                            val adhanOffsetX = (barWidth * adhanFrac - 5.dp).coerceAtLeast(2.dp)
                            val jamaahOffsetX = (barWidth * jamaahFrac - 5.dp).coerceAtLeast(adhanOffsetX + 24.dp)

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(14.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    // Razor-thin 2dp flat track
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .background(colors.hairline)
                                            .testTag("notification_progress_bar")
                                    ) {
                                        val barFillFraction = if (!uiState.isFeaturedPrayerActiveWindow || animatedProgress <= 0.001f) {
                                            0f
                                        } else {
                                            animatedProgress.coerceIn(0.03f, 1f)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(barFillFraction)
                                                .height(2.dp)
                                                .background(
                                                    if (activeStage == AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW) {
                                                        colors.accentCoral
                                                    } else {
                                                        colors.accentGold
                                                    }
                                                )
                                        )
                                    }

                                    // Checkpoint 1: Adhan Node
                                    Box(
                                        modifier = Modifier
                                            .offset(x = adhanOffsetX)
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isAdhanReached) colors.accentGold else colors.surfaceCard)
                                            .border(1.25.dp, colors.accentGold, CircleShape)
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                showExactCheckpointTimes = !showExactCheckpointTimes
                                            }
                                            .testTag("checkpoint_adhan_marker")
                                    )

                                    // Checkpoint 2: Jama'ah Node
                                    Box(
                                        modifier = Modifier
                                            .offset(x = jamaahOffsetX)
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isJamaahReached) colors.accentEmerald else colors.surfaceCard)
                                            .border(1.25.dp, colors.accentEmerald, CircleShape)
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                showExactCheckpointTimes = !showExactCheckpointTimes
                                            }
                                            .testTag("checkpoint_jamaah_marker")
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(16.dp)
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showExactCheckpointTimes = !showExactCheckpointTimes
                                        }
                                ) {
                                    val adhanText = if (showExactCheckpointTimes) "Adhan $adhanTime" else "Adhan"
                                    val jamaahText = if (showExactCheckpointTimes) "$jamaahCheckpointLabel $jamaahTime" else jamaahCheckpointLabel

                                    val minGap = if (showExactCheckpointTimes) 88.dp else 44.dp
                                    val maxRightReserve = if (showExactCheckpointTimes) 102.dp else 50.dp
                                    val adhanLabelOffset = (barWidth * adhanFrac - 16.dp)
                                        .coerceIn(0.dp, (barWidth - (minGap + maxRightReserve)).coerceAtLeast(0.dp))
                                    val jamaahLabelOffset = (barWidth * jamaahFrac - 20.dp).coerceIn(
                                        (adhanLabelOffset + minGap).coerceAtMost((barWidth - maxRightReserve).coerceAtLeast(0.dp)),
                                        (barWidth - maxRightReserve).coerceAtLeast(0.dp)
                                    )

                                    Text(
                                        text = adhanText,
                                        style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                                        fontSize = 9.5.sp,
                                        color = if (isAdhanReached || showExactCheckpointTimes) colors.accentGold else colors.inkTertiary,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.offset(x = adhanLabelOffset)
                                    )

                                    Text(
                                        text = jamaahText,
                                        style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                                        fontSize = 9.5.sp,
                                        color = if (isJamaahReached || showExactCheckpointTimes) colors.accentEmerald else colors.inkTertiary,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.offset(x = jamaahLabelOffset)
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons (clean monochrome primary + hairline secondary split)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (canMarkPrayedNow) {
                                    onMarkAsPrayedWithMode(null)
                                } else {
                                    onNotificationTriggered("$prayerName unlocks at Prayer Start Time ($pstTime)")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("notification_mark_as_prayed_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = if (!canMarkPrayedNow) BorderStroke(0.5.dp, colors.hairline) else null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isUrgentVisual -> colors.accentCoral
                                    canMarkPrayedNow -> colors.inkPrimary
                                    else -> colors.surfaceNested
                                },
                                contentColor = when {
                                    isUrgentVisual -> Color.White
                                    canMarkPrayedNow -> colors.surfaceCard
                                    else -> colors.inkTertiary
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Icon(
                                imageVector = if (canMarkPrayedNow) Icons.Rounded.Check else Icons.Rounded.Lock,
                                contentDescription = "Mark as Prayed",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val buttonLabel = when {
                                uiState.isIshaMissedAfterMidnight -> "Log Isha Make-Up (Qada)"
                                canMarkPrayedNow && isWithinAutoJamaahWindow ->
                                    "Mark $prayerName Prayed · Jama'ah"
                                canMarkPrayedNow -> "Mark $prayerName Prayed"
                                else -> "Unlocks at $pstTime"
                            }
                            Text(
                                text = buttonLabel,
                                style = MaterialTheme.typography.labelLarge.withTabularDigits(),
                                fontSize = if (isTightDisplay) 12.sp else 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (canMarkPrayedNow && !uiState.isIshaMissedAfterMidnight) {
                            AppleHairlineDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onMarkAsPrayedWithMode(true) }
                                        .padding(vertical = 8.dp)
                                        .testTag("lock_mark_in_jamaah_chip"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Mosque,
                                        contentDescription = null,
                                        tint = colors.accentEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Jama'ah",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontSize = 12.sp,
                                        color = colors.accentEmerald,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(0.5.dp)
                                        .height(18.dp)
                                        .background(colors.hairline)
                                )

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onMarkAsPrayedWithMode(false) }
                                        .padding(vertical = 8.dp)
                                        .testTag("lock_mark_individually_chip"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = colors.inkSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Individual",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontSize = 12.sp,
                                        color = colors.inkPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Quiet Borderless System Alert Trigger directly on canvas
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .appleSpringClickable(shape = RoundedCornerShape(8.dp)) {
                            if (isAlertPillExpanded) {
                                isAlertPillExpanded = false
                            } else {
                                triggerNotificationAction()
                                isAlertPillExpanded = true
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("trigger_system_notification_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isFeaturedPrayerActiveWindow) colors.accentGold else colors.inkTertiary)
                    )
                    Text(
                        text = if (uiState.isFeaturedPrayerActiveWindow) {
                            "Alert Active · $prayerName $adhanTime"
                        } else {
                            "Alert Armed · $prayerName $pstTime"
                        },
                        style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                        color = colors.inkSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                    Icon(
                        imageVector = if (isAlertPillExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Expand alert test action",
                        tint = colors.inkTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isAlertPillExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    OutlinedButton(
                        onClick = triggerNotificationAction,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, colors.hairlineStrong),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.inkPrimary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = if (uiState.isFeaturedPrayerActiveWindow) {
                                "Send Live System Notification"
                            } else {
                                "Preview Upcoming Alert"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    PrayerTimeConfigDialog(
        uiState = uiState,
        onSavePrayerTimeSettings = onSavePrayerTimeSettings,
        onDismissJamaahDialog = onDismissJamaahDialog
    )
}
