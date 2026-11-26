package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.PrayerCalculationConfig
import com.example.domain.PrayerTimeCalculator
import com.example.notification.PrayerNotificationHelper
import com.example.ui.components.AppleHairlineDivider
import com.example.ui.components.AppleSegmentedControl
import com.example.ui.components.AsrMadhabSelectorBlock
import com.example.ui.components.AutoSizeSingleLineText
import com.example.ui.components.CalculationLivePreviewStrip
import com.example.ui.components.CalculationMethodSelectorList
import com.example.ui.components.SwissInsetGroup
import com.example.ui.components.SwissSectionLabel
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.DisplayScaleMode
import com.example.ui.theme.HeroCardStyle
import com.example.ui.theme.NoorTheme
import com.example.ui.theme.asMonoPrecision
import com.example.viewmodel.PrayerDashboardUiState
import java.util.Locale

/**
 * Editorial Swiss Minimal Settings screen structured as clean inset grouped lists
 * with uppercase mono section labels, 0.5dp hairlines, zero icon-tile boxes,
 * and zero nested card clutter.
 */
@Composable
fun ArchitectureBlueprintScreenContent(
    uiState: PrayerDashboardUiState,
    onRequestGpsLocation: () -> Unit,
    onSimulateQadaRollover: () -> Unit = {},
    onMarkFeaturedAsPrayed: () -> Unit = {},
    onUpdateNotificationSettings: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onUpdateAutoJamaahWindow: (Int) -> Unit = {},
    onUpdateAutoSilenceDuringJamaah: (Boolean) -> Unit = {},
    onUpdateThemeMode: (AppThemeMode) -> Unit = {},
    onUpdateHeroCardStyle: (HeroCardStyle) -> Unit = {},
    onUpdateDisplayScaleMode: (DisplayScaleMode) -> Unit = {},
    onUpdateCalculationConfig: (PrayerCalculationConfig) -> Unit = {},
    onUpdateAnalyticsTrackingPreference: (Boolean, String?) -> Unit = { _, _ -> },
    onReopenOnboarding: () -> Unit = {},
    isDeviceLocationEnabled: () -> Boolean = { true },
    onMarkPendingGpsAfterSettings: () -> Unit = {},
    onBannerMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = NoorTheme.colors
    var isMethodPickerExpanded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            PrayerNotificationHelper.stopSoundPreview()
        }
    }

    var activePreviewTier by remember {
        mutableStateOf(PrayerNotificationHelper.getActivePreviewTier())
    }
    var customPickerTargetTier by remember {
        mutableStateOf(PrayerNotificationHelper.TIER_URGENT)
    }

    var adhanSoundMode by remember {
        mutableStateOf(PrayerNotificationHelper.getTierSoundMode(context, PrayerNotificationHelper.TIER_ADHAN))
    }
    var adhanSoundLabel by remember {
        mutableStateOf(PrayerNotificationHelper.resolveTierSoundDisplayLabel(context, PrayerNotificationHelper.TIER_ADHAN))
    }

    var jamaahSoundMode by remember {
        mutableStateOf(PrayerNotificationHelper.getTierSoundMode(context, PrayerNotificationHelper.TIER_JAMAAH))
    }
    var jamaahSoundLabel by remember {
        mutableStateOf(PrayerNotificationHelper.resolveTierSoundDisplayLabel(context, PrayerNotificationHelper.TIER_JAMAAH))
    }

    var urgentSoundMode by remember {
        mutableStateOf(PrayerNotificationHelper.getUrgentSoundMode(context))
    }
    var urgentSoundLabel by remember {
        mutableStateOf(PrayerNotificationHelper.resolveUrgentSoundDisplayLabel(context))
    }

    fun refreshTierSoundState(tier: String) {
        when (tier) {
            PrayerNotificationHelper.TIER_ADHAN -> {
                adhanSoundMode = PrayerNotificationHelper.getTierSoundMode(context, PrayerNotificationHelper.TIER_ADHAN)
                adhanSoundLabel = PrayerNotificationHelper.resolveTierSoundDisplayLabel(context, PrayerNotificationHelper.TIER_ADHAN)
            }
            PrayerNotificationHelper.TIER_JAMAAH -> {
                jamaahSoundMode = PrayerNotificationHelper.getTierSoundMode(context, PrayerNotificationHelper.TIER_JAMAAH)
                jamaahSoundLabel = PrayerNotificationHelper.resolveTierSoundDisplayLabel(context, PrayerNotificationHelper.TIER_JAMAAH)
            }
            else -> {
                urgentSoundMode = PrayerNotificationHelper.getUrgentSoundMode(context)
                urgentSoundLabel = PrayerNotificationHelper.resolveUrgentSoundDisplayLabel(context)
            }
        }
    }

    val customAudioDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val title = runCatching {
                RingtoneManager.getRingtone(context, uri)?.getTitle(context)
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Custom Audio"
            PrayerNotificationHelper.setTierSoundPreference(
                context = context,
                tier = customPickerTargetTier,
                mode = PrayerNotificationHelper.URGENT_SOUND_CUSTOM,
                customUri = uri.toString(),
                customLabel = title
            )
            refreshTierSoundState(customPickerTargetTier)
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (pickedUri != null) {
                val title = runCatching {
                    RingtoneManager.getRingtone(context, pickedUri)?.getTitle(context)
                }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Custom Tone"
                PrayerNotificationHelper.setTierSoundPreference(
                    context = context,
                    tier = customPickerTargetTier,
                    mode = PrayerNotificationHelper.URGENT_SOUND_CUSTOM,
                    customUri = pickedUri.toString(),
                    customLabel = title
                )
                refreshTierSoundState(customPickerTargetTier)
            }
        }
    }

    fun launchCustomTonePickerForTier(tier: String, dialogTitle: String) {
        customPickerTargetTier = tier
        val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_RINGTONE
            )
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, dialogTitle)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                PrayerNotificationHelper.resolveTierSoundUri(context, tier)
            )
        }
        runCatching {
            ringtonePickerLauncher.launch(pickerIntent)
        }.onFailure {
            customAudioDocLauncher.launch(arrayOf("audio/*"))
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted && !isDeviceLocationEnabled()) {
            onMarkPendingGpsAfterSettings()
            onBannerMessage("Turn on Location Services in Android Settings to resolve GPS")
            runCatching {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        } else {
            onRequestGpsLocation()
        }
    }

    val nextPrayerName = uiState.featuredPrayer?.nameEnglish ?: "Fajr"
    val nextPrayerTime = uiState.featuredPrayer?.let {
        PrayerTimeCalculator.formatMinutesTo12Hour(it.pstMinutes)
    } ?: "05:00 AM"

    val testNotifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            PrayerNotificationHelper.showTestNotification(
                context = context,
                nextPrayerName = nextPrayerName,
                nextPrayerTimeFormatted = nextPrayerTime,
                canScheduleExactAlarms = uiState.canScheduleExactAlarms
            )
            onBannerMessage("Test notification sent ✓")
        } else {
            onBannerMessage("Allow notifications in Android Settings to receive prayer alerts")
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .testTag("screen_4_architecture_blueprint"),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Section 1: Appearance
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SwissSectionLabel(title = "Appearance")
            SwissInsetGroup(modifier = Modifier.testTag("settings_appearance_card")) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppleSegmentedControl(
                        options = listOf(
                            AppThemeMode.SYSTEM_AUTO to "Auto",
                            AppThemeMode.APPLE_WHITE_LIGHT to "Apple White",
                            AppThemeMode.APPLE_OLED_DARK to "OLED Dark"
                        ),
                        selected = uiState.themeMode,
                        onSelect = onUpdateThemeMode,
                        optionTagPrefix = "theme_mode_option",
                        modifier = Modifier.testTag("settings_theme_segmented_control")
                    )
                    Text(
                        text = "Follows system appearance or locks to warm light or true OLED black.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary
                    )
                }

                AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Prayer Hero Style",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.inkPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    AppleSegmentedControl(
                        options = listOf(
                            HeroCardStyle.WAVE_CARD to "Wave Card",
                            HeroCardStyle.MINIMAL_EDITORIAL to "Minimal"
                        ),
                        selected = uiState.heroCardStyle,
                        onSelect = onUpdateHeroCardStyle,
                        optionTagPrefix = "hero_style_option",
                        modifier = Modifier.testTag("settings_hero_style_segmented_control")
                    )
                    Text(
                        text = "Framed card with calm animated solar waves or borderless editorial typography.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary
                    )
                }

                AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Display & Font Scale",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.inkPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    AppleSegmentedControl(
                        options = listOf(
                            DisplayScaleMode.ADAPTIVE_CAPPED to "Adaptive",
                            DisplayScaleMode.FIXED_SCALE to "Fixed Scale"
                        ),
                        selected = uiState.displayScaleMode,
                        onSelect = onUpdateDisplayScaleMode,
                        optionTagPrefix = "display_scale_option",
                        modifier = Modifier.testTag("settings_display_scale_segmented_control")
                    )
                    Text(
                        text = "Adaptive caps extreme system scaling cleanly; Fixed Scale locks app typography to 100% ignoring system zoom.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary
                    )
                }
            }
        }

        // Section 2: Pre-Prayer Reminders & Sound
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SwissSectionLabel(title = "Pre-Prayer Reminders")
            SwissInsetGroup(modifier = Modifier.testTag("settings_pre_prayer_reminders_card")) {
                // 2.0 Notification Delivery Verification & Real Test Trigger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Notification Delivery Check",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.inkPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (uiState.canScheduleExactAlarms) {
                                "Exact alarms armed · Verify alerts anytime"
                            } else {
                                "Exact alarm permission needed for on-time alerts"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.canScheduleExactAlarms) colors.inkSecondary else colors.accentCoral
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = {
                            val sent = PrayerNotificationHelper.showTestNotification(
                                context = context,
                                nextPrayerName = nextPrayerName,
                                nextPrayerTimeFormatted = nextPrayerTime,
                                canScheduleExactAlarms = uiState.canScheduleExactAlarms
                            )
                            if (!sent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                testNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else if (sent) {
                                onBannerMessage("Test notification sent ✓")
                            } else {
                                onBannerMessage("Notifications are disabled in system settings")
                            }
                        },
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(0.5.dp, colors.accentGold.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("settings_send_test_notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = colors.accentGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        AutoSizeSingleLineText(
                            text = "Send Test",
                            style = MaterialTheme.typography.labelLarge,
                            maxFontSize = 11.5.sp,
                            minFontSize = 9.5.sp,
                            color = colors.accentGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // 2A. Pre-Adhan
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pre-Adhan Reminder",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.inkPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    SwissChoiceStrip(
                        options = listOf(0, 5, 10, 15).map { mins ->
                            Triple(mins, if (mins == 0) "Off" else "${mins}m", "pre_adhan_chip_${mins}m")
                        },
                        selected = uiState.preAdhanAlertMinutes,
                        onSelect = { mins ->
                            onUpdateNotificationSettings(
                                uiState.nagIntervalMinutes,
                                uiState.urgentEndWindowMinutes,
                                mins,
                                uiState.preJamaahAlertMinutes
                            )
                        }
                    )
                }

                AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // 2B. Pre-Jama'ah
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Pre-Jama'ah Reminder",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.inkPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Enables 5m snooze once fired",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkSecondary
                        )
                    }
                    SwissChoiceStrip(
                        options = listOf(5, 10, 15, 20, 30).map { mins ->
                            Triple(mins, "${mins}m", "pre_jamaah_chip_${mins}m")
                        },
                        selected = uiState.preJamaahAlertMinutes,
                        onSelect = { mins ->
                            onUpdateNotificationSettings(
                                uiState.nagIntervalMinutes,
                                uiState.urgentEndWindowMinutes,
                                uiState.preAdhanAlertMinutes,
                                mins
                            )
                        }
                    )
                }

                AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // 2C. Adhan Sound
                SoundTierSelectorRow(
                    title = "Adhan Alert Sound",
                    displayLabel = adhanSoundLabel,
                    selectedMode = adhanSoundMode,
                    includeSilentOption = true,
                    isPreviewPlaying = activePreviewTier == PrayerNotificationHelper.TIER_ADHAN,
                    previewButtonTestTag = "sound_preview_button_adhan",
                    chipTagPrefix = "adhan_sound_chip",
                    onSelectMode = { modeKey ->
                        if (modeKey == PrayerNotificationHelper.URGENT_SOUND_CUSTOM) {
                            launchCustomTonePickerForTier(
                                PrayerNotificationHelper.TIER_ADHAN,
                                "Select Adhan Notification Sound"
                            )
                        } else {
                            PrayerNotificationHelper.setTierSoundPreference(
                                context = context,
                                tier = PrayerNotificationHelper.TIER_ADHAN,
                                mode = modeKey
                            )
                            refreshTierSoundState(PrayerNotificationHelper.TIER_ADHAN)
                        }
                    },
                    onTogglePreview = {
                        PrayerNotificationHelper.toggleSoundPreview(
                            context = context,
                            tier = PrayerNotificationHelper.TIER_ADHAN,
                            onStateChanged = { playingTier -> activePreviewTier = playingTier }
                        )
                    }
                )

                AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // 2D. Jama'ah Sound
                SoundTierSelectorRow(
                    title = "Jama'ah Alert Sound",
                    displayLabel = jamaahSoundLabel,
                    selectedMode = jamaahSoundMode,
                    includeSilentOption = true,
                    isPreviewPlaying = activePreviewTier == PrayerNotificationHelper.TIER_JAMAAH,
                    previewButtonTestTag = "sound_preview_button_jamaah",
                    chipTagPrefix = "jamaah_sound_chip",
                    onSelectMode = { modeKey ->
                        if (modeKey == PrayerNotificationHelper.URGENT_SOUND_CUSTOM) {
                            launchCustomTonePickerForTier(
                                PrayerNotificationHelper.TIER_JAMAAH,
                                "Select Jama'ah Notification Sound"
                            )
                        } else {
                            PrayerNotificationHelper.setTierSoundPreference(
                                context = context,
                                tier = PrayerNotificationHelper.TIER_JAMAAH,
                                mode = modeKey
                            )
                            refreshTierSoundState(PrayerNotificationHelper.TIER_JAMAAH)
                        }
                    },
                    onTogglePreview = {
                        PrayerNotificationHelper.toggleSoundPreview(
                            context = context,
                            tier = PrayerNotificationHelper.TIER_JAMAAH,
                            onStateChanged = { playingTier -> activePreviewTier = playingTier }
                        )
                    }
                )
            }
        }

        // Section 3: Follow-Up & Urgency
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SwissSectionLabel(title = "Follow-Up & Urgency")
            SwissInsetGroup(modifier = Modifier.testTag("settings_post_jamaah_reminders_card")) {
                // 3A. Post-Jama'ah Repeat Interval
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Unticked Prayer Repeat Interval",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.inkPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Repeats starting +10m after Jama'ah until marked",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkSecondary
                        )
                    }
                    SwissChoiceStrip(
                        options = listOf(5, 10, 12, 15, 20, 30).map { intervalMin ->
                            Triple(intervalMin, "${intervalMin}m", "nag_interval_chip_${intervalMin}m")
                        },
                        selected = uiState.nagIntervalMinutes,
                        onSelect = { intervalMin ->
                            onUpdateNotificationSettings(
                                intervalMin,
                                uiState.urgentEndWindowMinutes,
                                uiState.preAdhanAlertMinutes,
                                uiState.preJamaahAlertMinutes
                            )
                        }
                    )
                }

                AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // 3B. Urgent End-of-Window Warning
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Urgent End-of-Window Threshold",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.inkPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    SwissChoiceStrip(
                        options = listOf(15, 20, 30, 45).map { urgentMin ->
                            Triple(urgentMin, "Last ${urgentMin}m", "urgent_window_chip_${urgentMin}m")
                        },
                        selected = uiState.urgentEndWindowMinutes,
                        activeTextColor = colors.accentCoral,
                        onSelect = { urgentMin ->
                            onUpdateNotificationSettings(
                                uiState.nagIntervalMinutes,
                                urgentMin,
                                uiState.preAdhanAlertMinutes,
                                uiState.preJamaahAlertMinutes
                            )
                        }
                    )
                }

                AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // 3C. Urgent Warning Sound
                SoundTierSelectorRow(
                    title = "Urgent Warning Sound",
                    displayLabel = urgentSoundLabel,
                    selectedMode = urgentSoundMode,
                    includeSilentOption = false,
                    isPreviewPlaying = activePreviewTier == PrayerNotificationHelper.TIER_URGENT,
                    previewButtonTestTag = "sound_preview_button_urgent",
                    chipTagPrefix = "urgent_sound_chip",
                    onSelectMode = { modeKey ->
                        if (modeKey == PrayerNotificationHelper.URGENT_SOUND_CUSTOM) {
                            launchCustomTonePickerForTier(
                                PrayerNotificationHelper.TIER_URGENT,
                                "Select Urgent Prayer Warning Sound"
                            )
                        } else {
                            PrayerNotificationHelper.setUrgentSoundPreference(
                                context = context,
                                mode = modeKey
                            )
                            refreshTierSoundState(PrayerNotificationHelper.TIER_URGENT)
                        }
                    },
                    onTogglePreview = {
                        PrayerNotificationHelper.toggleSoundPreview(
                            context = context,
                            tier = PrayerNotificationHelper.TIER_URGENT,
                            onStateChanged = { playingTier -> activePreviewTier = playingTier }
                        )
                    }
                )
            }
        }

        // Section 4: Jama'ah & Masjid (zero nested card inside card)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SwissSectionLabel(title = "Jama'ah & Masjid")
            SwissInsetGroup(modifier = Modifier.testTag("settings_jamaah_tracking_card")) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Auto-Detect Jama'ah Window",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.inkPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Records Jama'ah automatically when marked near congregation time",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkSecondary
                        )
                    }
                    SwissChoiceStrip(
                        options = listOf(
                            Triple(0, "Manual", "auto_jamaah_chip_0m"),
                            Triple(15, "±15m", "auto_jamaah_chip_15m"),
                            Triple(20, "±20m", "auto_jamaah_chip_20m"),
                            Triple(30, "±30m", "auto_jamaah_chip_30m")
                        ),
                        selected = uiState.autoJamaahWindowMinutes,
                        activeTextColor = colors.accentEmerald,
                        onSelect = onUpdateAutoJamaahWindow
                    )
                }

                AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Masjid Auto-Silence as a clean hairline-separated row in the SAME group
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Masjid Auto-Silence (15m)",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.inkPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Silence sound and vibration for 15 minutes from Jama'ah start",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = uiState.autoSilenceDuringJamaah,
                        onCheckedChange = onUpdateAutoSilenceDuringJamaah,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.accentEmerald,
                            uncheckedThumbColor = colors.inkTertiary,
                            uncheckedTrackColor = colors.segmentedTrack,
                            uncheckedBorderColor = colors.hairlineStrong
                        ),
                        modifier = Modifier.testTag("masjid_auto_silence_toggle")
                    )
                }
            }
        }

        // Section 5: Location & Calculation Method
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SwissSectionLabel(
                title = "Location & Calculation",
                trailingText = uiState.calculationConfig.shortSummaryBadge
            )

            CalculationLivePreviewStrip(
                city = uiState.selectedCity,
                config = uiState.calculationConfig
            )

            SwissInsetGroup(modifier = Modifier.testTag("settings_location_method_card")) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = uiState.selectedCity.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.inkPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.3f", uiState.selectedCity.latitude)}°, ${String.format(Locale.US, "%.3f", uiState.selectedCity.longitude)}°",
                                style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                                color = colors.inkTertiary
                            )
                        }
                    }

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
                            .height(44.dp)
                            .testTag("resolve_fused_gps_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.inkPrimary,
                            contentColor = colors.surfaceCard
                        )
                    ) {
                        Icon(Icons.Rounded.GpsFixed, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        AutoSizeSingleLineText(
                            text = "Refresh Current GPS Location",
                            style = MaterialTheme.typography.labelLarge,
                            maxFontSize = 13.5.sp,
                            minFontSize = 11.sp,
                            color = colors.surfaceCard,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // 5B. Asr Madhab Selector
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Asr Juristic Method (Madhab)",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.inkPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    AsrMadhabSelectorBlock(
                        selectedMadhab = uiState.calculationConfig.asrMadhab,
                        onSelectMadhab = { madhab ->
                            onUpdateCalculationConfig(uiState.calculationConfig.copy(asrMadhab = madhab))
                        }
                    )
                }

                AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // 5C. Calculation Convention & Manual Angles Picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isMethodPickerExpanded = !isMethodPickerExpanded }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("settings_calculation_method_expand_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Calculation Convention",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.inkPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${uiState.calculationConfig.method.displayName} (${uiState.calculationConfig.angleSpecLabel})",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.accentGold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isMethodPickerExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (isMethodPickerExpanded) "Collapse methods" else "Choose calculation method",
                        tint = colors.inkTertiary
                    )
                }
            }

            AnimatedVisibility(visible = isMethodPickerExpanded) {
                CalculationMethodSelectorList(
                    config = uiState.calculationConfig,
                    onUpdateConfig = onUpdateCalculationConfig
                )
            }
        }

        // Section 6: Analytics History & Setup Guide
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SwissSectionLabel(
                title = "Analytics & Setup",
                trailingText = uiState.analyticsStartDateIso ?: "ALL DAYS"
            )
            SwissInsetGroup(modifier = Modifier.testTag("settings_analytics_setup_card")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Ignore Prior Days in Analytics",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.inkPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (uiState.ignorePriorAnalyticsDays) {
                                "Days before ${uiState.analyticsStartDateIso ?: "today"} appear muted and are excluded from stats (still tappable)"
                            } else {
                                "All 7 weeks are open for manual logging"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = uiState.ignorePriorAnalyticsDays,
                        onCheckedChange = { checked ->
                            onUpdateAnalyticsTrackingPreference(checked, uiState.analyticsStartDateIso)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.accentGold,
                            uncheckedThumbColor = colors.inkTertiary,
                            uncheckedTrackColor = colors.segmentedTrack,
                            uncheckedBorderColor = colors.hairlineStrong
                        ),
                        modifier = Modifier.testTag("settings_ignore_prior_days_toggle")
                    )
                }

                AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Box(modifier = Modifier.padding(16.dp)) {
                    OutlinedButton(
                        onClick = onReopenOnboarding,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, colors.hairlineStrong),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("settings_rerun_onboarding_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            tint = colors.inkPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AutoSizeSingleLineText(
                            text = "Re-run First-Time Setup Guide",
                            style = MaterialTheme.typography.labelLarge,
                            maxFontSize = 13.5.sp,
                            minFontSize = 11.sp,
                            color = colors.inkPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean flat Swiss segmented strip for discrete settings choices (replaces bordered pill chips).
 */
@Composable
private fun <T> SwissChoiceStrip(
    options: List<Triple<T, String, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    activeTextColor: Color = NoorTheme.colors.accentGold
) {
    val colors = NoorTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(colors.segmentedTrack)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEach { (value, label, tag) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.5.dp))
                    .background(if (isSelected) colors.segmentedThumb else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 7.dp, horizontal = 2.dp)
                    .testTag(tag),
                contentAlignment = Alignment.Center
            ) {
                AutoSizeSingleLineText(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.asMonoPrecision(),
                    maxFontSize = 11.sp,
                    minFontSize = 8.sp,
                    color = if (isSelected) activeTextColor else colors.inkSecondary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SoundTierSelectorRow(
    title: String,
    displayLabel: String,
    selectedMode: String,
    includeSilentOption: Boolean,
    isPreviewPlaying: Boolean,
    previewButtonTestTag: String,
    chipTagPrefix: String,
    onSelectMode: (String) -> Unit,
    onTogglePreview: () -> Unit
) {
    val colors = NoorTheme.colors
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.inkPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = displayLabel,
                    style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                    color = colors.inkTertiary,
                    maxLines = 2
                )
            }

            if (selectedMode != PrayerNotificationHelper.SOUND_MODE_SILENT) {
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTogglePreview() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag(previewButtonTestTag),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPreviewPlaying) "Stop sound preview" else "Preview sound",
                        tint = if (isPreviewPlaying) colors.accentCoral else colors.accentGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isPreviewPlaying) "Stop" else "Preview",
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 12.sp,
                        color = if (isPreviewPlaying) colors.accentCoral else colors.accentGold,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        val soundOptions = buildList {
            add(Triple(PrayerNotificationHelper.URGENT_SOUND_ALARM, "Alarm", "${chipTagPrefix}_alarm"))
            add(Triple(PrayerNotificationHelper.URGENT_SOUND_NOTIFICATION, "Chime", "${chipTagPrefix}_notification"))
            if (includeSilentOption) {
                add(Triple(PrayerNotificationHelper.SOUND_MODE_SILENT, "Silent", "${chipTagPrefix}_silent"))
            }
            add(Triple(PrayerNotificationHelper.URGENT_SOUND_CUSTOM, "Custom", "${chipTagPrefix}_custom"))
        }

        SwissChoiceStrip(
            options = soundOptions,
            selected = selectedMode,
            onSelect = onSelectMode
        )
    }
}
