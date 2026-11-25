package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AthkaarHabitUiModel
import com.example.data.PrayerCompletionStatus
import com.example.data.PrayerItemUiModel
import com.example.domain.CityLocation
import com.example.ui.components.AppleFloatingGlassDock
import com.example.ui.components.SwissInsetGroup
import com.example.ui.components.appleSpringClickable
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.NoorTheme
import com.example.ui.theme.withTabularDigits
import com.example.viewmodel.ActiveTab
import com.example.viewmodel.AppDisplayMode
import com.example.viewmodel.PrayerDashboardUiState

@Composable
fun NoorPrayerAppRoot(
    uiState: PrayerDashboardUiState,
    onSetDisplayMode: (AppDisplayMode) -> Unit,
    onSelectTab: (ActiveTab) -> Unit,
    onSelectCity: (CityLocation) -> Unit,
    onSearchLocationQueryChanged: (String) -> Unit,
    onAddCustomLocation: (String, String, Double, Double) -> Unit,
    onRequestGpsLocation: () -> Unit,
    onMarkFeaturedAsPrayed: () -> Unit,
    onSnoozeFeaturedFiveMin: () -> Unit,
    onSimulateQadaRollover: () -> Unit,
    onTogglePrayerCompleted: (PrayerItemUiModel) -> Unit,
    onEditJamaahClicked: (PrayerItemUiModel) -> Unit,
    onEditAdhanClicked: (PrayerItemUiModel) -> Unit = onEditJamaahClicked,
    onSaveJamaahOffset: (String, Int, Boolean, Int) -> Unit,
    onSavePrayerTimeSettings: (String, Int, Boolean, Int, Int, Boolean, Int) -> Unit = { _, _, _, _, _, _, _ -> },
    onDismissJamaahDialog: () -> Unit,
    onStepAthkaar: (AthkaarHabitUiModel) -> Unit,
    onToggleAthkaarComplete: (AthkaarHabitUiModel) -> Unit,
    onBannerMessage: (String) -> Unit,
    onToggleCountdownMode: () -> Unit = {},
    onUpdateRepeatInterval: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onMarkFeaturedAsPrayedWithMode: (Boolean?) -> Unit = { _ -> onMarkFeaturedAsPrayed() },
    onTogglePrayerCompletedWithMode: (PrayerItemUiModel, Boolean?) -> Unit = { p, _ -> onTogglePrayerCompleted(p) },
    onSetPrayerJamaahStatus: (PrayerItemUiModel, Boolean) -> Unit = { _, _ -> },
    onUpdateHijriOffset: (Int) -> Unit = {},
    onUpdateAutoJamaahWindow: (Int) -> Unit = {},
    onAdjustQazaBacklog: (Int) -> Unit = {},
    onUpdateHistoricalPrayer: (String, String, String, Boolean, Boolean, PrayerCompletionStatus) -> Unit = { _, _, _, _, _, _ -> },
    onUpdateAutoSilenceDuringJamaah: (Boolean) -> Unit = {},
    onUpdateThemeMode: (AppThemeMode) -> Unit = {},
    onCycleThemeMode: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val colors = NoorTheme.colors

    BackHandler(
        enabled = uiState.displayMode == AppDisplayMode.INTERACTIVE_SINGLE_SCREEN ||
            uiState.activeTab != ActiveTab.HOME_PRAYERS
    ) {
        if (uiState.displayMode == AppDisplayMode.INTERACTIVE_SINGLE_SCREEN &&
            uiState.activeTab != ActiveTab.HOME_PRAYERS
        ) {
            onSelectTab(ActiveTab.HOME_PRAYERS)
        } else {
            onSetDisplayMode(AppDisplayMode.SHOWCASE_3_SCREENS)
        }
    }

    val prayersTabQazaBadgeCount = if (uiState.todayQadaCount > 0) {
        uiState.todayQadaCount
    } else {
        uiState.totalQazaBacklogCount
    }
    val analyticsTabQazaBadgeCount = uiState.totalQazaBacklogCount

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Distilled editorial Qaza reminder strip (flat hairline, zero icon-tile clutter)
            val hasPendingQaza = uiState.todayQadaCount > 0 || uiState.totalQazaBacklogCount > 0
            AnimatedVisibility(
                visible = hasPendingQaza,
                enter = expandVertically() + fadeIn(tween(180)),
                exit = shrinkVertically() + fadeOut(tween(150))
            ) {
                val pendingNamesStr = if (uiState.pendingQazaPrayerNames.isNotEmpty()) {
                    "Today: ${uiState.pendingQazaPrayerNames.joinToString(", ")}"
                } else {
                    "${uiState.totalQazaBacklogCount} missed prayer${if (uiState.totalQazaBacklogCount == 1) "" else "s"} pending"
                }
                Surface(
                    color = colors.surfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, colors.accentCoral.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                        .testTag("persistent_qaza_top_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(colors.accentCoral)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = "Qaza Pending · ${uiState.totalQazaBacklogCount}",
                                    style = MaterialTheme.typography.labelLarge.withTabularDigits(),
                                    color = colors.accentCoral,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = pendingNamesStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.inkSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = "Log 1",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.accentEmerald,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .appleSpringClickable(shape = RoundedCornerShape(8.dp)) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onAdjustQazaBacklog(-1)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("qaza_top_banner_log_one_button"),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Quiet flat status feedback strip
            AnimatedVisibility(
                visible = uiState.statusBannerMessage != null,
                enter = expandVertically() + fadeIn(tween(180)),
                exit = shrinkVertically() + fadeOut(tween(150))
            ) {
                Surface(
                    color = colors.surfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, colors.hairlineStrong),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                        .testTag("transient_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.accentGold)
                        )
                        Text(
                            text = uiState.statusBannerMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (uiState.displayMode == AppDisplayMode.SHOWCASE_3_SCREENS) {
                ThreeScreenShowcaseDeck(
                    uiState = uiState,
                    onOpenScreenFull = { tab ->
                        onSelectTab(tab)
                        onSetDisplayMode(AppDisplayMode.INTERACTIVE_SINGLE_SCREEN)
                    },
                    onSelectCity = onSelectCity,
                    onSearchLocationQueryChanged = onSearchLocationQueryChanged,
                    onAddCustomLocation = onAddCustomLocation,
                    onRequestGpsLocation = onRequestGpsLocation,
                    onMarkFeaturedAsPrayed = onMarkFeaturedAsPrayed,
                    onSnoozeFeaturedFiveMin = onSnoozeFeaturedFiveMin,
                    onSimulateQadaRollover = onSimulateQadaRollover,
                    onTogglePrayerCompleted = onTogglePrayerCompleted,
                    onEditJamaahClicked = onEditJamaahClicked,
                    onEditAdhanClicked = onEditAdhanClicked,
                    onSaveJamaahOffset = onSaveJamaahOffset,
                    onSavePrayerTimeSettings = onSavePrayerTimeSettings,
                    onDismissJamaahDialog = onDismissJamaahDialog,
                    onStepAthkaar = onStepAthkaar,
                    onToggleAthkaarComplete = onToggleAthkaarComplete,
                    onBannerMessage = onBannerMessage,
                    onToggleCountdownMode = onToggleCountdownMode,
                    onUpdateRepeatInterval = onUpdateRepeatInterval,
                    onUpdateThemeMode = onUpdateThemeMode,
                    onCycleThemeMode = onCycleThemeMode
                )
            } else {
                // Critically damped Swiss horizontal glide between tabs
                AnimatedContent(
                    targetState = uiState.activeTab,
                    transitionSpec = {
                        val forward = targetState.ordinal >= initialState.ordinal
                        val slideSpec = spring<IntOffset>(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                        val slideIn = slideInHorizontally(animationSpec = slideSpec) { fullWidth ->
                            if (forward) fullWidth / 10 else -fullWidth / 10
                        }
                        val slideOut = slideOutHorizontally(animationSpec = tween(150)) { fullWidth ->
                            if (forward) -fullWidth / 12 else fullWidth / 12
                        }
                        (slideIn + fadeIn(animationSpec = tween(180)))
                            .togetherWith(slideOut + fadeOut(animationSpec = tween(140)))
                    },
                    modifier = Modifier.weight(1f),
                    label = "bottom_tab_transition"
                ) { targetTab ->
                    val verticalScroll = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScroll)
                            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 108.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(modifier = Modifier.widthIn(max = 560.dp)) {
                            when (targetTab) {
                                ActiveTab.NOTIFICATION_LOCK -> {
                                    BackHandler { onSelectTab(ActiveTab.HOME_PRAYERS) }
                                    NotificationLockScreenContent(
                                        uiState = uiState,
                                        onMarkAsPrayed = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onMarkFeaturedAsPrayed()
                                        },
                                        onMarkAsPrayedWithMode = { isInJamaah ->
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onMarkFeaturedAsPrayedWithMode(isInJamaah)
                                        },
                                        onSnoozeFiveMinutes = onSnoozeFeaturedFiveMin,
                                        onNotificationTriggered = onBannerMessage,
                                        onEditJamaahClicked = onEditJamaahClicked,
                                        onSavePrayerTimeSettings = onSavePrayerTimeSettings,
                                        onDismissJamaahDialog = onDismissJamaahDialog,
                                        onToggleCountdownMode = onToggleCountdownMode,
                                        onUpdateRepeatInterval = onUpdateRepeatInterval,
                                        onUpdateHijriOffset = { newOffset ->
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onUpdateHijriOffset(newOffset)
                                        },
                                        isCompactShowcase = false
                                    )
                                }

                                ActiveTab.HOME_PRAYERS -> {
                                    PrayerHomeScreenContent(
                                        uiState = uiState,
                                        onSelectCity = onSelectCity,
                                        onSearchLocationQueryChanged = onSearchLocationQueryChanged,
                                        onAddCustomLocation = onAddCustomLocation,
                                        onRequestGpsLocation = onRequestGpsLocation,
                                        onTogglePrayerCompleted = { prayer ->
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onTogglePrayerCompleted(prayer)
                                        },
                                        onTogglePrayerCompletedWithMode = { prayer, isInJamaah ->
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onTogglePrayerCompletedWithMode(prayer, isInJamaah)
                                        },
                                        onSetPrayerJamaahStatus = { prayer, isInJamaah ->
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSetPrayerJamaahStatus(prayer, isInJamaah)
                                        },
                                        onEditJamaahClicked = onEditJamaahClicked,
                                        onEditAdhanClicked = onEditAdhanClicked,
                                        onSaveJamaahOffset = onSaveJamaahOffset,
                                        onSavePrayerTimeSettings = onSavePrayerTimeSettings,
                                        onDismissJamaahDialog = onDismissJamaahDialog,
                                        onToggleCountdownMode = onToggleCountdownMode,
                                        onCycleThemeMode = onCycleThemeMode,
                                        isCompactShowcase = false
                                    )
                                }

                                ActiveTab.ANALYTICS_HABITS -> {
                                    BackHandler { onSelectTab(ActiveTab.HOME_PRAYERS) }
                                    AnalyticsScreenContent(
                                        uiState = uiState,
                                        onStepAthkaar = { habit ->
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onStepAthkaar(habit)
                                        },
                                        onToggleAthkaarComplete = { habit ->
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onToggleAthkaarComplete(habit)
                                        },
                                        onAdjustQazaBacklog = { delta ->
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onAdjustQazaBacklog(delta)
                                        },
                                        onUpdateHistoricalPrayer = { dateIso, pId, pName, isPrayed, inJamaah, status ->
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onUpdateHistoricalPrayer(dateIso, pId, pName, isPrayed, inJamaah, status)
                                        },
                                        isCompactShowcase = false
                                    )
                                }

                                ActiveTab.ARCHITECTURE_BLUEPRINT -> {
                                    BackHandler { onSelectTab(ActiveTab.HOME_PRAYERS) }
                                    ArchitectureBlueprintScreenContent(
                                        uiState = uiState,
                                        onRequestGpsLocation = onRequestGpsLocation,
                                        onSimulateQadaRollover = onSimulateQadaRollover,
                                        onMarkFeaturedAsPrayed = onMarkFeaturedAsPrayed,
                                        onUpdateNotificationSettings = { nag, urgent, preAdhan, preJamaah ->
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onUpdateRepeatInterval(nag, urgent, preAdhan, preJamaah)
                                        },
                                        onUpdateAutoJamaahWindow = { windowMin ->
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onUpdateAutoJamaahWindow(windowMin)
                                        },
                                        onUpdateAutoSilenceDuringJamaah = { enabled ->
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onUpdateAutoSilenceDuringJamaah(enabled)
                                        },
                                        onUpdateThemeMode = onUpdateThemeMode
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Refined Swiss minimal floating navigation dock
        if (uiState.displayMode == AppDisplayMode.INTERACTIVE_SINGLE_SCREEN) {
            AppleFloatingGlassDock(
                activeTab = uiState.activeTab,
                prayersQazaBadgeCount = prayersTabQazaBadgeCount,
                analyticsQazaBadgeCount = analyticsTabQazaBadgeCount,
                onSelectTab = onSelectTab,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun ThreeScreenShowcaseDeck(
    uiState: PrayerDashboardUiState,
    onOpenScreenFull: (ActiveTab) -> Unit,
    onSelectCity: (CityLocation) -> Unit,
    onSearchLocationQueryChanged: (String) -> Unit,
    onAddCustomLocation: (String, String, Double, Double) -> Unit,
    onRequestGpsLocation: () -> Unit,
    onMarkFeaturedAsPrayed: () -> Unit,
    onSnoozeFeaturedFiveMin: () -> Unit,
    onSimulateQadaRollover: () -> Unit,
    onTogglePrayerCompleted: (PrayerItemUiModel) -> Unit,
    onEditJamaahClicked: (PrayerItemUiModel) -> Unit,
    onEditAdhanClicked: (PrayerItemUiModel) -> Unit = onEditJamaahClicked,
    onSaveJamaahOffset: (String, Int, Boolean, Int) -> Unit,
    onSavePrayerTimeSettings: (String, Int, Boolean, Int, Int, Boolean, Int) -> Unit = { _, _, _, _, _, _, _ -> },
    onDismissJamaahDialog: () -> Unit,
    onStepAthkaar: (AthkaarHabitUiModel) -> Unit,
    onToggleAthkaarComplete: (AthkaarHabitUiModel) -> Unit,
    onBannerMessage: (String) -> Unit,
    onToggleCountdownMode: () -> Unit = {},
    onUpdateRepeatInterval: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onUpdateThemeMode: (AppThemeMode) -> Unit = {},
    onCycleThemeMode: () -> Unit = {}
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWidePresentation = maxWidth >= 860.dp
        val verticalScroll = rememberScrollState()

        if (isWidePresentation) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    ShowcaseDeviceFrame(
                        screenNumber = "01",
                        screenTitle = "Alerts & Lock Screen",
                        onExpandClicked = { onOpenScreenFull(ActiveTab.NOTIFICATION_LOCK) },
                        modifier = Modifier.weight(1f)
                    ) {
                        NotificationLockScreenContent(
                            uiState = uiState,
                            onMarkAsPrayed = onMarkFeaturedAsPrayed,
                            onSnoozeFiveMinutes = onSnoozeFeaturedFiveMin,
                            onNotificationTriggered = onBannerMessage,
                            onEditJamaahClicked = onEditJamaahClicked,
                            onSavePrayerTimeSettings = onSavePrayerTimeSettings,
                            onDismissJamaahDialog = onDismissJamaahDialog,
                            onToggleCountdownMode = onToggleCountdownMode,
                            onUpdateRepeatInterval = onUpdateRepeatInterval,
                            isCompactShowcase = true
                        )
                    }

                    ShowcaseDeviceFrame(
                        screenNumber = "02",
                        screenTitle = "Prayers",
                        onExpandClicked = { onOpenScreenFull(ActiveTab.HOME_PRAYERS) },
                        isCenterFeatured = true,
                        modifier = Modifier.weight(1.08f)
                    ) {
                        PrayerHomeScreenContent(
                            uiState = uiState,
                            onSelectCity = onSelectCity,
                            onSearchLocationQueryChanged = onSearchLocationQueryChanged,
                            onAddCustomLocation = onAddCustomLocation,
                            onRequestGpsLocation = onRequestGpsLocation,
                            onTogglePrayerCompleted = onTogglePrayerCompleted,
                            onEditJamaahClicked = onEditJamaahClicked,
                            onEditAdhanClicked = onEditAdhanClicked,
                            onSaveJamaahOffset = onSaveJamaahOffset,
                            onSavePrayerTimeSettings = onSavePrayerTimeSettings,
                            onDismissJamaahDialog = onDismissJamaahDialog,
                            onToggleCountdownMode = onToggleCountdownMode,
                            onCycleThemeMode = onCycleThemeMode,
                            isCompactShowcase = true
                        )
                    }

                    ShowcaseDeviceFrame(
                        screenNumber = "03",
                        screenTitle = "Analytics",
                        onExpandClicked = { onOpenScreenFull(ActiveTab.ANALYTICS_HABITS) },
                        modifier = Modifier.weight(1f)
                    ) {
                        AnalyticsScreenContent(
                            uiState = uiState,
                            onStepAthkaar = onStepAthkaar,
                            onToggleAthkaarComplete = onToggleAthkaarComplete,
                            isCompactShowcase = true
                        )
                    }
                }

                ShowcaseDeviceFrame(
                    screenNumber = "04",
                    screenTitle = "Settings",
                    onExpandClicked = { onOpenScreenFull(ActiveTab.ARCHITECTURE_BLUEPRINT) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ArchitectureBlueprintScreenContent(
                        uiState = uiState,
                        onRequestGpsLocation = onRequestGpsLocation,
                        onSimulateQadaRollover = onSimulateQadaRollover,
                        onMarkFeaturedAsPrayed = onMarkFeaturedAsPrayed,
                        onUpdateNotificationSettings = onUpdateRepeatInterval,
                        onUpdateThemeMode = onUpdateThemeMode
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickFocusPill(
                        badge = "01",
                        title = "Alerts",
                        onClick = { onOpenScreenFull(ActiveTab.NOTIFICATION_LOCK) },
                        testTag = "quick_focus_screen_1"
                    )
                    QuickFocusPill(
                        badge = "02",
                        title = "Prayers",
                        onClick = { onOpenScreenFull(ActiveTab.HOME_PRAYERS) },
                        testTag = "quick_focus_screen_2"
                    )
                    QuickFocusPill(
                        badge = "03",
                        title = "Analytics",
                        onClick = { onOpenScreenFull(ActiveTab.ANALYTICS_HABITS) },
                        testTag = "quick_focus_screen_3"
                    )
                    QuickFocusPill(
                        badge = "04",
                        title = "Settings",
                        onClick = { onOpenScreenFull(ActiveTab.ARCHITECTURE_BLUEPRINT) },
                        testTag = "quick_focus_architecture"
                    )
                }

                ShowcaseDeviceFrame(
                    screenNumber = "01",
                    screenTitle = "Alerts & Lock Screen",
                    onExpandClicked = { onOpenScreenFull(ActiveTab.NOTIFICATION_LOCK) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NotificationLockScreenContent(
                        uiState = uiState,
                        onMarkAsPrayed = onMarkFeaturedAsPrayed,
                        onSnoozeFiveMinutes = onSnoozeFeaturedFiveMin,
                        onNotificationTriggered = onBannerMessage,
                        onEditJamaahClicked = onEditJamaahClicked,
                        onSavePrayerTimeSettings = onSavePrayerTimeSettings,
                        onDismissJamaahDialog = onDismissJamaahDialog,
                        onToggleCountdownMode = onToggleCountdownMode,
                        onUpdateRepeatInterval = onUpdateRepeatInterval,
                        isCompactShowcase = true
                    )
                }

                ShowcaseDeviceFrame(
                    screenNumber = "02",
                    screenTitle = "Prayers",
                    onExpandClicked = { onOpenScreenFull(ActiveTab.HOME_PRAYERS) },
                    isCenterFeatured = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PrayerHomeScreenContent(
                        uiState = uiState,
                        onSelectCity = onSelectCity,
                        onSearchLocationQueryChanged = onSearchLocationQueryChanged,
                        onAddCustomLocation = onAddCustomLocation,
                        onRequestGpsLocation = onRequestGpsLocation,
                        onTogglePrayerCompleted = onTogglePrayerCompleted,
                        onEditJamaahClicked = onEditJamaahClicked,
                        onEditAdhanClicked = onEditAdhanClicked,
                        onSaveJamaahOffset = onSaveJamaahOffset,
                        onSavePrayerTimeSettings = onSavePrayerTimeSettings,
                        onDismissJamaahDialog = onDismissJamaahDialog,
                        onToggleCountdownMode = onToggleCountdownMode,
                        onCycleThemeMode = onCycleThemeMode,
                        isCompactShowcase = true
                    )
                }

                ShowcaseDeviceFrame(
                    screenNumber = "03",
                    screenTitle = "Analytics",
                    onExpandClicked = { onOpenScreenFull(ActiveTab.ANALYTICS_HABITS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnalyticsScreenContent(
                        uiState = uiState,
                        onStepAthkaar = onStepAthkaar,
                        onToggleAthkaarComplete = onToggleAthkaarComplete,
                        isCompactShowcase = true
                    )
                }

                ShowcaseDeviceFrame(
                    screenNumber = "04",
                    screenTitle = "Settings",
                    onExpandClicked = { onOpenScreenFull(ActiveTab.ARCHITECTURE_BLUEPRINT) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ArchitectureBlueprintScreenContent(
                        uiState = uiState,
                        onRequestGpsLocation = onRequestGpsLocation,
                        onSimulateQadaRollover = onSimulateQadaRollover,
                        onMarkFeaturedAsPrayed = onMarkFeaturedAsPrayed,
                        onUpdateNotificationSettings = onUpdateRepeatInterval,
                        onUpdateThemeMode = onUpdateThemeMode
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun QuickFocusPill(
    badge: String,
    title: String,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = NoorTheme.colors
    Surface(
        color = colors.surfaceCard,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, colors.hairline),
        modifier = Modifier
            .appleSpringClickable(shape = RoundedCornerShape(10.dp), onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                color = colors.accentGold,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = colors.inkPrimary
            )
        }
    }
}

@Composable
private fun ShowcaseDeviceFrame(
    screenNumber: String,
    screenTitle: String,
    onExpandClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isCenterFeatured: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = NoorTheme.colors
    SwissInsetGroup(
        modifier = modifier,
        containerColor = colors.canvasElevated,
        borderColor = if (isCenterFeatured) colors.hairlineStrong else colors.hairline,
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = screenNumber,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                        color = colors.accentGold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.inkPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onExpandClicked)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("expand_screen_$screenNumber"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = "Focus screen $screenNumber",
                        tint = colors.inkSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Open",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.inkSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            content()
        }
    }
}
