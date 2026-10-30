package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.PrayerTimeCalculator
import com.example.notification.AlarmTriggerType
import com.example.notification.PrayerNotificationHelper
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
 * Screen 1: Dynamic Notification & Lock Screen
 * Streamlined, uncluttered lock screen and live notification preview:
 * - Clean Android Lock Screen clock & Hijri date header
 * - Focused Android notification card synced with the 4-phase prayer status
 * - "Mark as Prayed" is locked until Jama'ah time is reached
 * - "Snooze 5m" is hidden entirely before the Pre-Jama'ah reminder threshold (uiState.isSnoozeEnabled)
 */
@Composable
fun NotificationLockScreenContent(
    uiState: PrayerDashboardUiState,
    onMarkAsPrayed: () -> Unit,
    onSnoozeFiveMinutes: () -> Unit,
    onNotificationTriggered: (String) -> Unit,
    onToggleCountdownMode: () -> Unit = {},
    onUpdateRepeatInterval: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier,
    isCompactShowcase: Boolean = false
) {
    val context = LocalContext.current
    val featuredPrayer = uiState.featuredPrayer
    val prayerName = featuredPrayer?.nameEnglish ?: "Asr"
    val prayerArabic = featuredPrayer?.nameArabic ?: "العصر"
    val pstTime = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.pstMinutes) } ?: "03:39 PM"
    val adhanTime = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.adhanMinutes) } ?: "04:30 PM"
    val jamaahTime = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.jamaahMinutes) } ?: "04:45 PM"
    val canMarkPrayedNow = featuredPrayer?.canMarkPrayed == true ||
        uiState.isAllTodayPrayersCompleted ||
        uiState.isIshaMissedAfterMidnight

    val activeStage = when {
        uiState.isFeaturedPrayerActiveWindow && uiState.isJamaahTimePassed &&
            uiState.minutesUntilNextPrayer in 1..uiState.urgentEndWindowMinutes ->
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

    val animatedProgress by animateFloatAsState(
        targetValue = uiState.notificationProgressFraction,
        animationSpec = tween(durationMillis = 450),
        label = "notification_progress_bar"
    )

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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("screen_1_notification_lock")
    ) {
        val isTightDisplay = isCompactShowcase || maxWidth < 375.dp || LocalDensity.current.fontScale > 1.05f

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isTightDisplay) 14.dp else 18.dp)
        ) {
            // Lock Screen Status & Time Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = if (isTightDisplay) 6.dp else 12.dp)
            ) {
                Surface(
                    color = ElevatedSlateSurface,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, SlateStrokeBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Lock screen indicator",
                            tint = ElegantGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = uiState.hijriDateText.ifEmpty { "26 Rabi' al-Thani 1448 AH" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedPeriwinkle,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = uiState.currentTimeFormatted.substringBefore(" "),
                    style = if (isTightDisplay) {
                        MaterialTheme.typography.displayMedium
                    } else {
                        MaterialTheme.typography.displayLarge
                    },
                    color = CrispOffWhite,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${uiState.gregorianDateText} • ${uiState.selectedCity.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedPeriwinkle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Focused Android Dynamic Notification Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dynamic_notification_banner"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ElevatedSlateSurface),
                border = BorderStroke(
                    1.2.dp,
                    if (uiState.isIshaMissedAfterMidnight || activeStage == AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW)
                        CoralAlert.copy(alpha = 0.7f)
                    else ElegantGold.copy(alpha = 0.45f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ElevatedSlateCard,
                                    ElevatedSlateSurface
                                )
                            )
                        )
                        .padding(if (isTightDisplay) 14.dp else 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Notification Top Row: Status Header on Left + Compact Jama'ah Pill on Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            uiState.isIshaMissedAfterMidnight || activeStage == AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW ->
                                                CoralAlert.copy(alpha = 0.2f)
                                            uiState.isAllTodayPrayersCompleted -> EmeraldCompleted.copy(alpha = 0.2f)
                                            else -> GoldContainerDark
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.isAllTodayPrayersCompleted) Icons.Filled.Check else Icons.Filled.NotificationsActive,
                                    contentDescription = "Prayer notification icon",
                                    tint = when {
                                        uiState.isIshaMissedAfterMidnight || activeStage == AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW -> CoralAlert
                                        uiState.isAllTodayPrayersCompleted -> EmeraldCompleted
                                        else -> ElegantGold
                                    },
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = uiState.heroHeaderStatusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = if (isTightDisplay) 10.sp else 11.sp,
                                color = when {
                                    uiState.isIshaMissedAfterMidnight || activeStage == AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW -> CoralAlert
                                    uiState.isAllTodayPrayersCompleted -> EmeraldCompleted
                                    else -> ElegantGold
                                },
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            color = GoldContainerDark,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(
                                1.dp,
                                if (uiState.isJamaahTimePassed) RadiantAmberGlow else ElegantGold.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.testTag("minutes_until_jamaah_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.isJamaahTimePassed) RadiantAmberGlow else ElegantGold)
                                )
                                Text(
                                    text = uiState.heroJamaahPillText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = if (isTightDisplay) 10.5.sp else 11.sp,
                                    color = RadiantAmberGlow,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Middle Section: Prayer Name on Left, 4-Phase Status on Right, Full-Width Schedule Summary Below
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "$prayerName Prayer",
                                    style = if (isTightDisplay) {
                                        MaterialTheme.typography.titleLarge
                                    } else {
                                        MaterialTheme.typography.headlineSmall
                                    },
                                    color = CrispOffWhite,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                if (!isTightDisplay) {
                                    Text(
                                        text = prayerArabic,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ElegantGold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = uiState.heroCountdownSubtitleText,
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = if (isTightDisplay) 12.sp else 13.5.sp,
                                color = if (uiState.isJamaahTimePassed) RadiantAmberGlow else ElegantGold,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "Starts at $pstTime • Adhan $adhanTime • Jama'ah $jamaahTime",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = if (isTightDisplay) 11.sp else 12.sp,
                            color = MutedPeriwinkle,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Progress bar showing time remaining / elapsed
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.progressLeftLabelText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = if (isTightDisplay) 10.5.sp else 11.5.sp,
                                color = if (uiState.isJamaahTimePassed) CrispOffWhite else MutedPeriwinkle,
                                fontWeight = if (uiState.isJamaahTimePassed) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.progressRightLabelText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = if (isTightDisplay) 10.5.sp else 11.5.sp,
                                color = ElegantGold,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(DeepSlateDarker)
                                .testTag("notification_progress_bar")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress.coerceIn(0.04f, 1f))
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = when {
                                                uiState.isAllTodayPrayersCompleted -> listOf(EmeraldCompleted, ElegantGold)
                                                activeStage == AlarmTriggerType.STAGE_4_URGENT_END_OF_WINDOW ->
                                                    listOf(RadiantAmberGlow, CoralAlert)
                                                else -> listOf(ElegantGold, RadiantAmberGlow)
                                            }
                                        )
                                    )
                            )
                        }
                    }

                    // Action Buttons: "Mark as Prayed" (locked before Jama'ah) and "Snooze 5m" (only when isSnoozeEnabled)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onMarkAsPrayed,
                            modifier = Modifier
                                .weight(if (uiState.isSnoozeEnabled) 1.15f else 1f)
                                .height(48.dp)
                                .testTag("notification_mark_as_prayed_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = if (!canMarkPrayedNow) BorderStroke(1.dp, SlateStrokeBorder) else null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    uiState.isIshaMissedAfterMidnight -> CoralAlert
                                    uiState.isAllTodayPrayersCompleted -> EmeraldCompleted
                                    canMarkPrayedNow -> ElegantGold
                                    else -> DeepSlateDarker
                                },
                                contentColor = when {
                                    uiState.isIshaMissedAfterMidnight -> CrispOffWhite
                                    canMarkPrayedNow || uiState.isAllTodayPrayersCompleted -> OnGoldDark
                                    else -> MutedPeriwinkle
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (canMarkPrayedNow) Icons.Filled.Check else Icons.Outlined.Lock,
                                contentDescription = "Mark as Prayed",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    uiState.isIshaMissedAfterMidnight -> "Log Isha Make-Up (Qada) ✓"
                                    uiState.isAllTodayPrayersCompleted -> "All 5 Prayed ✓ (Next: $prayerName)"
                                    canMarkPrayedNow -> "Mark $prayerName as Prayed"
                                    else -> "Unlocks at Jama'ah ($jamaahTime)"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = if (isTightDisplay) 12.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (uiState.isSnoozeEnabled) {
                            OutlinedButton(
                                onClick = onSnoozeFiveMinutes,
                                modifier = Modifier
                                    .weight(0.85f)
                                    .height(48.dp)
                                    .testTag("notification_snooze_5m_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, SlateStrokeBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = DeepSlateBlue.copy(alpha = 0.6f),
                                    contentColor = CrispOffWhite
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Snooze,
                                    contentDescription = "Snooze 5 minutes",
                                    tint = ElegantGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.snooze_5m),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = if (isTightDisplay) 12.sp else 13.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Single clean button to test firing the live Android system notification banner (only when prayer window is active)
            OutlinedButton(
                onClick = {
                    if (!uiState.isFeaturedPrayerActiveWindow) {
                        onNotificationTriggered(
                            "$prayerName has not started yet (Starts at $pstTime • ${uiState.heroCountdownSubtitleText})"
                        )
                        return@OutlinedButton
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("trigger_system_notification_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (uiState.isFeaturedPrayerActiveWindow) ElegantGold.copy(alpha = 0.5f) else SlateStrokeBorder
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (uiState.isFeaturedPrayerActiveWindow) ElegantGold else MutedPeriwinkle
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isFeaturedPrayerActiveWindow) {
                        "Send Live System Notification"
                    } else {
                        "Alert Armed for $prayerName ($pstTime)"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
