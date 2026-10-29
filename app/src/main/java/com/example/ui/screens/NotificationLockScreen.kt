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
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.PrayerTimeCalculator
import com.example.notification.PrayerNotificationHelper
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
 * Features:
 * - Clean Android Lock Screen clock & Hijri date header
 * - Rich Android notification banner with progress bar showing time remaining until next prayer
 * - Active "Minutes until Jama'ah" countdown badge
 * - Prominent action buttons: "Mark as Prayed" and "Snooze 5m"
 * - Soft Chime alert profile badge + button to trigger live Android system notification
 */
@Composable
fun NotificationLockScreenContent(
    uiState: PrayerDashboardUiState,
    onMarkAsPrayed: () -> Unit,
    onSnoozeFiveMinutes: () -> Unit,
    onNotificationTriggered: (String) -> Unit,
    modifier: Modifier = Modifier,
    isCompactShowcase: Boolean = false
) {
    val context = LocalContext.current
    val featuredPrayer = uiState.featuredPrayer
    val prayerName = featuredPrayer?.nameEnglish ?: "Asr"
    val prayerArabic = featuredPrayer?.nameArabic ?: "العصر"
    val adhanTime = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.adhanMinutes) } ?: "03:45 PM"
    val jamaahTime = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.jamaahMinutes) } ?: "04:00 PM"

    val animatedProgress by animateFloatAsState(
        targetValue = uiState.notificationProgressFraction,
        animationSpec = tween(durationMillis = 450),
        label = "notification_progress_bar"
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            PrayerNotificationHelper.showDynamicPrayerNotification(
                context = context,
                prayerName = prayerName,
                adhanTimeFormatted = adhanTime,
                jamaahTimeFormatted = jamaahTime,
                minutesUntilJamaah = uiState.minutesUntilJamaah,
                progressPercent = (uiState.notificationProgressFraction * 100).toInt()
            )
            onNotificationTriggered("System notification posted with Soft Chime")
        } else {
            onNotificationTriggered("Notification permission denied; preview active on screen")
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("screen_1_notification_lock"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isCompactShowcase) 14.dp else 18.dp)
    ) {
        // Lock Screen Status & Time Display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = if (isCompactShowcase) 6.dp else 12.dp)
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
                        color = MutedPeriwinkle
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = uiState.currentTimeFormatted.substringBefore(" "),
                style = if (isCompactShowcase) {
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
                color = MutedPeriwinkle
            )
        }

        // Rich Android Dynamic Notification Banner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dynamic_notification_banner"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSlateSurface),
            border = BorderStroke(1.2.dp, ElegantGold.copy(alpha = 0.45f)),
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
                    .padding(if (isCompactShowcase) 16.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Notification Top Row: App Brand + Active "Minutes until Jama'ah" Countdown Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(GoldContainerDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "Prayer notification icon",
                                tint = ElegantGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "NOOR PRAYER • NOW",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElegantGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Muslim World League",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedPeriwinkle,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Active "Minutes until Jama'ah" Countdown Badge
                    Surface(
                        color = GoldContainerDark,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.7f)),
                        modifier = Modifier.testTag("minutes_until_jamaah_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(ElegantGold)
                            )
                            Text(
                                text = if (uiState.snoozedExtraMinutes > 0) {
                                    stringResource(
                                        R.string.snoozed_badge_format,
                                        uiState.snoozedExtraMinutes,
                                        uiState.minutesUntilJamaah
                                    )
                                } else {
                                    stringResource(
                                        R.string.jamaah_countdown_format,
                                        uiState.minutesUntilJamaah
                                    )
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = RadiantAmberGlow,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Prayer Name & Schedule Details
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
                                text = "$prayerName Prayer",
                                style = MaterialTheme.typography.headlineMedium,
                                color = CrispOffWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = prayerArabic,
                                style = MaterialTheme.typography.titleMedium,
                                color = ElegantGold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Adhan $adhanTime  •  Jama'ah $jamaahTime",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedPeriwinkle
                        )
                    }

                    Text(
                        text = "-${uiState.minutesUntilNextPrayer}m:${String.format("%02d", uiState.currentClockSeconds)}s",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElegantGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Progress bar showing time remaining until the next prayer
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Time remaining until $prayerName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedPeriwinkle
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}% elapsed",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElegantGold,
                            fontWeight = FontWeight.SemiBold
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
                                .fillMaxWidth(animatedProgress.coerceIn(0.08f, 1f))
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            ElegantGold,
                                            RadiantAmberGlow
                                        )
                                    )
                                )
                        )
                    }
                }

                // Prominent Action Buttons: "Mark as Prayed" and "Snooze 5m"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onMarkAsPrayed,
                        modifier = Modifier
                            .weight(1.15f)
                            .height(48.dp)
                            .testTag("notification_mark_as_prayed_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElegantGold,
                            contentColor = OnGoldDark
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Mark as Prayed",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.mark_as_prayed),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Alert Profile & Live Notification Trigger Card
        Surface(
            color = ElevatedSlateSurface.copy(alpha = 0.85f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SlateStrokeBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
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
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = "Soft chime profile",
                            tint = EmeraldCompleted,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.alert_profile_badge),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrispOffWhite,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Real-time sync",
                        tint = ElegantGold,
                        modifier = Modifier.size(16.dp)
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val sent = PrayerNotificationHelper.showDynamicPrayerNotification(
                                context = context,
                                prayerName = prayerName,
                                adhanTimeFormatted = adhanTime,
                                jamaahTimeFormatted = jamaahTime,
                                minutesUntilJamaah = uiState.minutesUntilJamaah,
                                progressPercent = (uiState.notificationProgressFraction * 100).toInt()
                            )
                            if (!sent) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onNotificationTriggered("System notification posted with Soft Chime")
                            }
                        } else {
                            PrayerNotificationHelper.showDynamicPrayerNotification(
                                context = context,
                                prayerName = prayerName,
                                adhanTimeFormatted = adhanTime,
                                jamaahTimeFormatted = jamaahTime,
                                minutesUntilJamaah = uiState.minutesUntilJamaah,
                                progressPercent = (uiState.notificationProgressFraction * 100).toInt()
                            )
                            onNotificationTriggered("System notification posted with Soft Chime")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("trigger_system_notification_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElegantGold)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.send_system_notification),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
