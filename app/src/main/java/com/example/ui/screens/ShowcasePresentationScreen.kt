package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AthkaarHabitUiModel
import com.example.data.PrayerItemUiModel
import com.example.domain.CityLocation
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
import com.example.ui.theme.SlateStrokeBorder
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
    onUpdateRepeatInterval: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> }
) {
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DeepSlateBlue,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (uiState.displayMode == AppDisplayMode.INTERACTIVE_SINGLE_SCREEN) {
                NavigationBar(
                    containerColor = DeepSlateDarker,
                    contentColor = CrispOffWhite,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = uiState.activeTab == ActiveTab.NOTIFICATION_LOCK,
                        onClick = { onSelectTab(ActiveTab.NOTIFICATION_LOCK) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = stringResource(R.string.nav_notification)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_notification),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OnGoldDark,
                            selectedTextColor = ElegantGold,
                            indicatorColor = ElegantGold,
                            unselectedIconColor = MutedPeriwinkle,
                            unselectedTextColor = MutedPeriwinkle
                        ),
                        modifier = Modifier.testTag("nav_tab_notification")
                    )

                    NavigationBarItem(
                        selected = uiState.activeTab == ActiveTab.HOME_PRAYERS,
                        onClick = { onSelectTab(ActiveTab.HOME_PRAYERS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Mosque,
                                contentDescription = stringResource(R.string.nav_home)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_home),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OnGoldDark,
                            selectedTextColor = ElegantGold,
                            indicatorColor = ElegantGold,
                            unselectedIconColor = MutedPeriwinkle,
                            unselectedTextColor = MutedPeriwinkle
                        ),
                        modifier = Modifier.testTag("nav_tab_home")
                    )

                    NavigationBarItem(
                        selected = uiState.activeTab == ActiveTab.ANALYTICS_HABITS,
                        onClick = { onSelectTab(ActiveTab.ANALYTICS_HABITS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Analytics,
                                contentDescription = stringResource(R.string.nav_analytics)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_analytics),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OnGoldDark,
                            selectedTextColor = ElegantGold,
                            indicatorColor = ElegantGold,
                            unselectedIconColor = MutedPeriwinkle,
                            unselectedTextColor = MutedPeriwinkle
                        ),
                        modifier = Modifier.testTag("nav_tab_analytics")
                    )

                    NavigationBarItem(
                        selected = uiState.activeTab == ActiveTab.ARCHITECTURE_BLUEPRINT,
                        onClick = { onSelectTab(ActiveTab.ARCHITECTURE_BLUEPRINT) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.nav_architecture)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_architecture),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OnGoldDark,
                            selectedTextColor = ElegantGold,
                            indicatorColor = ElegantGold,
                            unselectedIconColor = MutedPeriwinkle,
                            unselectedTextColor = MutedPeriwinkle
                        ),
                        modifier = Modifier.testTag("nav_tab_architecture")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DeepSlateDarker,
                            DeepSlateBlue,
                            DeepSlateDarker
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            TopBrandPresentationHeader(
                displayMode = uiState.displayMode,
                onSetDisplayMode = onSetDisplayMode
            )

            AnimatedVisibility(
                visible = uiState.statusBannerMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = GoldContainerDark,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElegantGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("transient_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldCompleted)
                        )
                        Text(
                            text = uiState.statusBannerMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrispOffWhite,
                            fontWeight = FontWeight.SemiBold
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
                    onUpdateRepeatInterval = onUpdateRepeatInterval
                )
            } else {
                val verticalScroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScroll)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 560.dp)) {
                        when (uiState.activeTab) {
                            ActiveTab.NOTIFICATION_LOCK -> {
                                BackHandler { onSelectTab(ActiveTab.HOME_PRAYERS) }
                                NotificationLockScreenContent(
                                    uiState = uiState,
                                    onMarkAsPrayed = onMarkFeaturedAsPrayed,
                                    onSnoozeFiveMinutes = onSnoozeFeaturedFiveMin,
                                    onNotificationTriggered = onBannerMessage,
                                    onToggleCountdownMode = onToggleCountdownMode,
                                    onUpdateRepeatInterval = onUpdateRepeatInterval,
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
                                    onTogglePrayerCompleted = onTogglePrayerCompleted,
                                    onEditJamaahClicked = onEditJamaahClicked,
                                    onEditAdhanClicked = onEditAdhanClicked,
                                    onSaveJamaahOffset = onSaveJamaahOffset,
                                    onSavePrayerTimeSettings = onSavePrayerTimeSettings,
                                    onDismissJamaahDialog = onDismissJamaahDialog,
                                    onToggleCountdownMode = onToggleCountdownMode,
                                    isCompactShowcase = false
                                )
                            }

                            ActiveTab.ANALYTICS_HABITS -> {
                                BackHandler { onSelectTab(ActiveTab.HOME_PRAYERS) }
                                AnalyticsScreenContent(
                                    uiState = uiState,
                                    onStepAthkaar = onStepAthkaar,
                                    onToggleAthkaarComplete = onToggleAthkaarComplete,
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
                                    onUpdateNotificationSettings = onUpdateRepeatInterval
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBrandPresentationHeader(
    displayMode: AppDisplayMode,
    onSetDisplayMode: (AppDisplayMode) -> Unit
) {
    Surface(
        color = DeepSlateDarker.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, SlateStrokeBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GoldContainerDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mosque,
                        contentDescription = null,
                        tint = ElegantGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = CrispOffWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "Muslim World League (18° / 17°)",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElegantGold,
                        maxLines = 1
                    )
                }
            }
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
    onUpdateRepeatInterval: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> }
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
                        screenTitle = "Dynamic Notification & Lock Screen",
                        onExpandClicked = { onOpenScreenFull(ActiveTab.NOTIFICATION_LOCK) },
                        modifier = Modifier.weight(1f)
                    ) {
                        NotificationLockScreenContent(
                            uiState = uiState,
                            onMarkAsPrayed = onMarkFeaturedAsPrayed,
                            onSnoozeFiveMinutes = onSnoozeFeaturedFiveMin,
                            onNotificationTriggered = onBannerMessage,
                            onToggleCountdownMode = onToggleCountdownMode,
                            onUpdateRepeatInterval = onUpdateRepeatInterval,
                            isCompactShowcase = true
                        )
                    }

                    ShowcaseDeviceFrame(
                        screenNumber = "02",
                        screenTitle = "Home & Prayer Dashboard",
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
                            isCompactShowcase = true
                        )
                    }

                    ShowcaseDeviceFrame(
                        screenNumber = "03",
                        screenTitle = "Analytics & Habit Dashboard",
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

                // Technical Architecture Blueprint & Live Alarm Inspector Panel
                ShowcaseDeviceFrame(
                    screenNumber = "ARCH",
                    screenTitle = "Technical Architecture Blueprint & Exact Alarm / FGS Loop Inspector",
                    onExpandClicked = { onOpenScreenFull(ActiveTab.ARCHITECTURE_BLUEPRINT) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ArchitectureBlueprintScreenContent(
                        uiState = uiState,
                        onRequestGpsLocation = onRequestGpsLocation,
                        onSimulateQadaRollover = onSimulateQadaRollover,
                        onMarkFeaturedAsPrayed = onMarkFeaturedAsPrayed,
                        onUpdateNotificationSettings = onUpdateRepeatInterval
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickFocusPill(
                        badge = "Screen 1",
                        title = "Lock & Notification",
                        onClick = { onOpenScreenFull(ActiveTab.NOTIFICATION_LOCK) },
                        testTag = "quick_focus_screen_1"
                    )
                    QuickFocusPill(
                        badge = "Screen 2",
                        title = "Prayer Dashboard",
                        onClick = { onOpenScreenFull(ActiveTab.HOME_PRAYERS) },
                        testTag = "quick_focus_screen_2"
                    )
                    QuickFocusPill(
                        badge = "Screen 3",
                        title = "Analytics & Athkaar",
                        onClick = { onOpenScreenFull(ActiveTab.ANALYTICS_HABITS) },
                        testTag = "quick_focus_screen_3"
                    )
                    QuickFocusPill(
                        badge = "Blueprint",
                        title = "Architecture & Alarms",
                        onClick = { onOpenScreenFull(ActiveTab.ARCHITECTURE_BLUEPRINT) },
                        testTag = "quick_focus_architecture"
                    )
                }

                ShowcaseDeviceFrame(
                    screenNumber = "01",
                    screenTitle = "Screen 1: Dynamic Notification & Lock Screen",
                    onExpandClicked = { onOpenScreenFull(ActiveTab.NOTIFICATION_LOCK) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NotificationLockScreenContent(
                        uiState = uiState,
                        onMarkAsPrayed = onMarkFeaturedAsPrayed,
                        onSnoozeFiveMinutes = onSnoozeFeaturedFiveMin,
                        onNotificationTriggered = onBannerMessage,
                        onToggleCountdownMode = onToggleCountdownMode,
                        onUpdateRepeatInterval = onUpdateRepeatInterval,
                        isCompactShowcase = true
                    )
                }

                ShowcaseDeviceFrame(
                    screenNumber = "02",
                    screenTitle = "Screen 2: Home & Prayer Dashboard",
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
                        isCompactShowcase = true
                    )
                }

                ShowcaseDeviceFrame(
                    screenNumber = "03",
                    screenTitle = "Screen 3: Analytics & Tracking Dashboard",
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
                    screenNumber = "ARCH",
                    screenTitle = "Technical Architecture Blueprint & Alarm Loop Inspector",
                    onExpandClicked = { onOpenScreenFull(ActiveTab.ARCHITECTURE_BLUEPRINT) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ArchitectureBlueprintScreenContent(
                        uiState = uiState,
                        onRequestGpsLocation = onRequestGpsLocation,
                        onSimulateQadaRollover = onSimulateQadaRollover,
                        onMarkFeaturedAsPrayed = onMarkFeaturedAsPrayed,
                        onUpdateNotificationSettings = onUpdateRepeatInterval
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
    Surface(
        color = ElevatedSlateSurface,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.45f)),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = GoldContainerDark,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = ElegantGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = CrispOffWhite
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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
        border = BorderStroke(
            width = if (isCenterFeatured) 1.8.dp else 1.2.dp,
            color = if (isCenterFeatured) ElegantGold.copy(alpha = 0.75f) else SlateStrokeBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCenterFeatured) 14.dp else 8.dp)
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
                    Surface(
                        color = if (isCenterFeatured) ElegantGold else ElevatedSlateCard,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = screenNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCenterFeatured) OnGoldDark else ElegantGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = CrispOffWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = ElevatedSlateSurface,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, SlateStrokeBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onExpandClicked)
                        .testTag("expand_screen_$screenNumber")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Dashboard,
                            contentDescription = "Focus screen $screenNumber",
                            tint = ElegantGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Full View",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElegantGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            content()
        }
    }
}
