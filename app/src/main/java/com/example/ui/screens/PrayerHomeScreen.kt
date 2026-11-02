package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.PrayerCompletionStatus
import com.example.data.PrayerItemUiModel
import com.example.data.SolarWarningItemUiModel
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
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SlateStrokeBorder
import com.example.viewmodel.NextPrayerCountdownMode
import com.example.viewmodel.PrayerDashboardUiState
import java.util.Locale

@Composable
fun PrayerHomeScreenContent(
    uiState: PrayerDashboardUiState,
    onSelectCity: (CityLocation) -> Unit,
    onSearchLocationQueryChanged: (String) -> Unit,
    onAddCustomLocation: (String, String, Double, Double) -> Unit,
    onRequestGpsLocation: () -> Unit,
    onTogglePrayerCompleted: (PrayerItemUiModel) -> Unit,
    onEditJamaahClicked: (PrayerItemUiModel) -> Unit,
    onSaveJamaahOffset: (String, Int, Boolean, Int) -> Unit,
    onDismissJamaahDialog: () -> Unit,
    onEditAdhanClicked: (PrayerItemUiModel) -> Unit = onEditJamaahClicked,
    onSavePrayerTimeSettings: (String, Int, Boolean, Int, Int, Boolean, Int) -> Unit = { id, _, _, _, jOff, jFixed, jMin ->
        onSaveJamaahOffset(id, jOff, jFixed, jMin)
    },
    onToggleCountdownMode: () -> Unit = {},
    modifier: Modifier = Modifier,
    isCompactShowcase: Boolean = false
) {
    var showLocationSearchModal by remember { mutableStateOf(false) }
    val featuredPrayer = uiState.featuredPrayer
    val featuredPst = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.pstMinutes) } ?: "03:39 PM"
    val featuredAdhan = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.adhanMinutes) } ?: "03:45 PM"
    val featuredJamaah = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.jamaahMinutes) } ?: "04:00 PM"
    val featuredEnd = featuredPrayer?.let { PrayerTimeCalculator.formatMinutesTo12Hour(it.endMinutes) } ?: "05:00 PM"

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onRequestGpsLocation()
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
            verticalArrangement = Arrangement.spacedBy(if (isTightDisplay) 10.dp else 14.dp)
        ) {
            // Top Bar: Left = Day & Date Badge, Right = Live GPS + City Selector Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ElevatedSlateSurface,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, SlateStrokeBorder.copy(alpha = 0.85f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .testTag("day_date_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = "Current Day and Date",
                            tint = ElegantGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = uiState.dayAndDateBadgeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = if (isTightDisplay) 11.5.sp else 12.5.sp,
                            color = CrispOffWhite,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right Pill: Live GPS Icon (tappable to refresh GPS) + City Name (tappable to search locations)
                Surface(
                    color = ElevatedSlateSurface,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(50))
                        .clickable { showLocationSearchModal = true }
                        .testTag("city_dropdown_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                                .padding(2.dp)
                                .testTag("gps_status_badge"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GpsFixed,
                                contentDescription = "Use Live GPS",
                                tint = EmeraldCompleted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = uiState.selectedCity.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = if (isTightDisplay) 11.5.sp else 12.5.sp,
                            color = ElegantGold,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = stringResource(R.string.select_city),
                            tint = ElegantGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Hero Card: Restored Bubbly 28.dp Corners, Tall Open Sky Gap, and Bottom Cluster Pushed Down
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_current_prayer_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = ElevatedSlateSurface),
                border = BorderStroke(
                    1.4.dp,
                    if (uiState.isUrgentEndOfWindow || uiState.isIshaMissedAfterMidnight) CoralAlert.copy(alpha = 0.85f)
                    else ElegantGold.copy(alpha = 0.65f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (isTightDisplay) 215.dp else 240.dp)
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
                        modifier = Modifier.matchParentSize()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = if (isTightDisplay) 215.dp else 240.dp)
                            .padding(
                                start = if (isTightDisplay) 18.dp else 22.dp,
                                end = if (isTightDisplay) 18.dp else 22.dp,
                                top = if (isTightDisplay) 18.dp else 20.dp,
                                bottom = if (isTightDisplay) 12.dp else 14.dp
                            ),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Row: Status Header ("CURRENT PRAYER" / "NEXT PRAYER") + Prayer Title ("Jumu'ah الجمعة") on Left, Time-Based Solar Badge on Right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = uiState.heroHeaderStatusLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontSize = if (isTightDisplay) 12.sp else 13.5.sp,
                                    color = when {
                                        uiState.isUrgentEndOfWindow || uiState.isIshaMissedAfterMidnight -> CoralAlert
                                        uiState.isAllTodayPrayersCompleted -> EmeraldCompleted
                                        else -> ElegantGold
                                    },
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = featuredPrayer?.nameEnglish ?: "Asr",
                                        style = if (isTightDisplay) {
                                            MaterialTheme.typography.headlineMedium
                                        } else {
                                            MaterialTheme.typography.headlineLarge
                                        },
                                        fontSize = if (isTightDisplay) 28.sp else 34.sp,
                                        color = CrispOffWhite,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = featuredPrayer?.nameArabic ?: "العصر",
                                        style = if (isTightDisplay) {
                                            MaterialTheme.typography.titleLarge
                                        } else {
                                            MaterialTheme.typography.headlineSmall
                                        },
                                        fontSize = if (isTightDisplay) 22.sp else 26.sp,
                                        color = ElegantGold,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Surface(
                                color = DeepSlateDarker.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, SlateStrokeBorder.copy(alpha = 0.9f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.WbSunny,
                                        contentDescription = "Solar cycle badge",
                                        tint = RadiantAmberGlow,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = uiState.heroSolarBadgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = if (isTightDisplay) 11.sp else 12.sp,
                                        color = MutedPeriwinkle,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // Open Sky Gap: Pushes bottom cluster vertically down toward the bottom wave edge
                        Spacer(modifier = Modifier.height(if (isTightDisplay) 52.dp else 68.dp))

                        // Bottom Cluster: Left = Stage Time Display, Right = Plump Jama'ah Pill & Large Countdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Bottom-Left Column: Time Display (STARTS AT / ADHAN TIME / JAMA'AH TIME)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = uiState.heroLeftTimeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = if (isTightDisplay) 10.sp else 11.sp,
                                    color = MutedPeriwinkle,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = uiState.heroLeftTimeValue.ifEmpty { featuredAdhan },
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontSize = if (isTightDisplay) 24.sp else 28.sp,
                                    color = ElegantGold,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Bottom-Right Column: Plump Bubbly Jama'ah Pill + Larger Countdown Text
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = GoldContainerDark.copy(alpha = 0.95f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(
                                        1.2.dp,
                                        if (uiState.isJamaahTimePassed) RadiantAmberGlow else ElegantGold.copy(alpha = 0.75f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            featuredPrayer?.let { onEditJamaahClicked(it) }
                                        }
                                        .testTag("hero_jamaah_pill")
                                ) {
                                    Text(
                                        text = uiState.heroJamaahPillText,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontSize = if (isTightDisplay) 13.sp else 14.5.sp,
                                        color = if (uiState.isJamaahTimePassed) RadiantAmberGlow else ElegantGold,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(
                                            horizontal = if (isTightDisplay) 13.dp else 16.dp,
                                            vertical = if (isTightDisplay) 7.dp else 8.dp
                                        )
                                    )
                                }

                                Text(
                                    text = uiState.heroCountdownSubtitleText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = if (isTightDisplay) 14.sp else 16.5.sp,
                                    color = when {
                                        uiState.isUrgentEndOfWindow || uiState.isIshaMissedAfterMidnight -> CoralAlert
                                        uiState.isJamaahTimePassed -> RadiantAmberGlow
                                        else -> CrispOffWhite
                                    },
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.testTag("hero_countdown_status_text")
                                )
                            }
                        }
                    }
                }
            }

            // Section Header for Daily Prayers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTightDisplay) "Daily Prayers" else stringResource(R.string.daily_prayers_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = if (isTightDisplay) 14.5.sp else 16.sp,
                    color = CrispOffWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (isTightDisplay) 14.dp else 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = "Adhan & Jama'ah",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = if (isTightDisplay) 11.sp else 12.sp,
                        color = ElegantGold,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = stringResource(R.string.status_column),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = if (isTightDisplay) 11.sp else 12.sp,
                        color = MutedPeriwinkle,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Vertical List of the 5 Daily Prayers Interleaved with the 3 Solar / Makruh Rows
            val solarMap = uiState.solarWarnings.associateBy { it.id }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.prayers.forEach { prayer ->
                    PrayerRowCard(
                        prayer = prayer,
                        onToggleCompleted = { onTogglePrayerCompleted(prayer) },
                        onEditAdhan = { onEditAdhanClicked(prayer) },
                        onEditJamaah = { onEditJamaahClicked(prayer) },
                        isCompact = isCompactShowcase
                    )

                    if (prayer.id == "FAJR") {
                        solarMap["SUN_RISING"]?.let { sunRising ->
                            SolarWarningRowCard(
                                item = sunRising,
                                isCompact = isCompactShowcase
                            )
                        }
                        solarMap["TOP_OF_HEAD_SUN"]?.let { topOfHead ->
                            SolarWarningRowCard(
                                item = topOfHead,
                                isCompact = isCompactShowcase
                            )
                        }
                    } else if (prayer.id == "ASR") {
                        solarMap["SUN_SETTING"]?.let { sunSetting ->
                            SolarWarningRowCard(
                                item = sunSetting,
                                isCompact = isCompactShowcase
                            )
                        }
                    }
                }
            }
        }
    }

    // Worldwide Town / Locality / City Search Modal Dialog
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
fun PrayerTimeConfigDialog(
    uiState: PrayerDashboardUiState,
    onSavePrayerTimeSettings: (String, Int, Boolean, Int, Int, Boolean, Int) -> Unit,
    onDismissJamaahDialog: () -> Unit
) {
    // Unified 3-Tier Prayer Time Configuration Dialog (PST Reference + Configurable Adhan & Jama'ah via Clock Time or Offset)
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

        // Which tier is currently being edited in the dialog: "ADHAN" or "JAMAAH"
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
            fixedAdhanMinutesValue.coerceIn(0, 1439)
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
            "JUMUAH" -> "Friday Jumu'ah (الجمعة)"
            "DHUHR" -> "Daily Dhuhr (الظهر)"
            else -> "${editingPrayer.nameEnglish} (${editingPrayer.nameArabic})"
        }

        val pst12h = PrayerTimeCalculator.formatMinutesTo12Hour(pstMin)
        val adhan12h = PrayerTimeCalculator.formatMinutesTo12Hour(resolvedAdhanMinutes)
        val adhan24h = PrayerTimeCalculator.formatMinutesTo24Hour(resolvedAdhanMinutes)
        val jamaah12h = PrayerTimeCalculator.formatMinutesTo12Hour(resolvedJamaahMinutes)
        val jamaah24h = PrayerTimeCalculator.formatMinutesTo24Hour(resolvedJamaahMinutes)

        AlertDialog(
            onDismissRequest = onDismissJamaahDialog,
            containerColor = ElevatedSlateCard,
            titleContentColor = CrispOffWhite,
            textContentColor = MutedPeriwinkle,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Configure $activeDisplayName",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Natural Prayer Start Time (PST): $pst12h",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElegantGold,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Separate Switcher for Friday Jumu'ah vs Daily Dhuhr
                    if (isMiddayPrayer) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DeepSlateDarker)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isJumuahTab = selectedSettingsKey == "JUMUAH"
                            Surface(
                                color = if (isJumuahTab) ElegantGold else DeepSlateDarker,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedSettingsKey = "JUMUAH" }
                                    .testTag("tab_config_jumuah")
                            ) {
                                Text(
                                    text = "Jumu'ah (Fri)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isJumuahTab) OnGoldDark else CrispOffWhite,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp),
                                    maxLines = 1
                                )
                            }

                            Surface(
                                color = if (!isJumuahTab) ElegantGold else DeepSlateDarker,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedSettingsKey = "DHUHR" }
                                    .testTag("tab_config_dhuhr")
                            ) {
                                Text(
                                    text = "Dhuhr (Daily)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (!isJumuahTab) OnGoldDark else CrispOffWhite,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // 2. Three-Tier Live Summary Banner (PST -> Adhan -> Jama'ah)
                    Surface(
                        color = DeepSlateDarker,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateStrokeBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("PST (Solar)", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MutedPeriwinkle)
                                Text(pst12h, style = MaterialTheme.typography.labelMedium, color = CrispOffWhite, fontWeight = FontWeight.SemiBold)
                            }
                            Text("→", color = MutedPeriwinkle)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Mosque Adhan", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = RadiantAmberGlow)
                                Text(adhan12h, style = MaterialTheme.typography.labelMedium, color = RadiantAmberGlow, fontWeight = FontWeight.Bold)
                            }
                            Text("→", color = MutedPeriwinkle)
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Jama'ah", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = ElegantGold)
                                Text(jamaah12h, style = MaterialTheme.typography.labelMedium, color = ElegantGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 3. Tier Selector Tab: Configure Adhan Time vs Configure Jama'ah Time
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DeepSlateDarker)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isAdhanTab = activeTierTab == "ADHAN"
                        Surface(
                            color = if (isAdhanTab) GoldContainerDark else DeepSlateDarker,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isAdhanTab) RadiantAmberGlow else SlateStrokeBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { activeTierTab = "ADHAN" }
                                .testTag("tier_tab_adhan")
                        ) {
                            Text(
                                text = "Adhan Time",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isAdhanTab) RadiantAmberGlow else CrispOffWhite,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 8.dp),
                                maxLines = 1
                            )
                        }

                        Surface(
                            color = if (!isAdhanTab) GoldContainerDark else DeepSlateDarker,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (!isAdhanTab) ElegantGold else SlateStrokeBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { activeTierTab = "JAMAAH" }
                                .testTag("tier_tab_jamaah")
                        ) {
                            Text(
                                text = "Jama'ah Time",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (!isAdhanTab) ElegantGold else CrispOffWhite,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 8.dp),
                                maxLines = 1
                            )
                        }
                    }

                    // 4. Mode Toggle for the Selected Tier: Specific Clock Time vs Offset (+ / - min)
                    val isCurrentTierFixed = if (activeTierTab == "ADHAN") useFixedAdhanMode else useFixedJamaahMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DeepSlateDarker.copy(alpha = 0.7f))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = if (isCurrentTierFixed) GoldContainerDark else DeepSlateDarker,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isCurrentTierFixed) ElegantGold else SlateStrokeBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
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
                                text = "Specific Clock Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrentTierFixed) ElegantGold else CrispOffWhite,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                maxLines = 1
                            )
                        }

                        Surface(
                            color = if (!isCurrentTierFixed) GoldContainerDark else DeepSlateDarker,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (!isCurrentTierFixed) ElegantGold else SlateStrokeBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
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
                                text = if (activeTierTab == "ADHAN") "PST Offset (+min)" else "Adhan Offset (+min)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (!isCurrentTierFixed) ElegantGold else CrispOffWhite,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                maxLines = 1
                            )
                        }
                    }

                    // 5. Primary Active Value Display Card
                    Surface(
                        color = DeepSlateDarker,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (activeTierTab == "ADHAN") adhan12h else jamaah12h,
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (activeTierTab == "ADHAN") RadiantAmberGlow else ElegantGold,
                                fontWeight = FontWeight.Bold
                            )
                            val detailText = if (activeTierTab == "ADHAN") {
                                if (useFixedAdhanMode) {
                                    "$adhan24h • Specific Mosque Adhan Clock Time (PST $pst12h)"
                                } else {
                                    val sign = if (adhanOffsetValue >= 0) "+$adhanOffsetValue min" else "$adhanOffsetValue min"
                                    "$adhan24h • $sign from PST ($pst12h)"
                                }
                            } else {
                                if (useFixedJamaahMode) {
                                    "$jamaah24h • Specific Jama'ah Clock Time (Adhan $adhan12h)"
                                } else {
                                    val sign = if (jamaahOffsetValue >= 0) "+$jamaahOffsetValue min" else "$jamaahOffsetValue min"
                                    "$jamaah24h • $sign from Adhan ($adhan12h)"
                                }
                            }
                            Text(
                                text = detailText,
                                style = MaterialTheme.typography.labelSmall,
                                color = CrispOffWhite
                            )
                        }
                    }

                    // 6. Interactive Controls (Specific Clock Picker Controls OR Offset Controls)
                    val minAllowedClock = if (activeTierTab == "ADHAN") (pstMin - 60).coerceAtLeast(0) else resolvedAdhanMinutes
                    val maxAllowedClock = (pstMin + 240).coerceAtMost(1439)

                    if (isCurrentTierFixed) {
                        // Hour & Minute Direct Clock Adjusters (-1h, -15m, -5m, -1m, +1m, +5m, +15m, +1h)
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
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
                                modifier = Modifier.testTag("jamaah_minus_button")
                            ) {
                                Text("-15m", color = ElegantGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") {
                                        fixedAdhanMinutesValue = (fixedAdhanMinutesValue - 5).coerceAtLeast(minAllowedClock)
                                    } else {
                                        fixedJamaahMinutesValue = (fixedJamaahMinutesValue - 5).coerceAtLeast(minAllowedClock)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SlateStrokeBorder),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text("-5m", color = CrispOffWhite, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") {
                                        fixedAdhanMinutesValue = (fixedAdhanMinutesValue - 1).coerceAtLeast(minAllowedClock)
                                    } else {
                                        fixedJamaahMinutesValue = (fixedJamaahMinutesValue - 1).coerceAtLeast(minAllowedClock)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SlateStrokeBorder),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text("-1m", color = CrispOffWhite, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") {
                                        fixedAdhanMinutesValue = (fixedAdhanMinutesValue + 1).coerceAtMost(maxAllowedClock)
                                    } else {
                                        fixedJamaahMinutesValue = (fixedJamaahMinutesValue + 1).coerceAtMost(maxAllowedClock)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SlateStrokeBorder),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text("+1m", color = CrispOffWhite, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") {
                                        fixedAdhanMinutesValue = (fixedAdhanMinutesValue + 5).coerceAtMost(maxAllowedClock)
                                    } else {
                                        fixedJamaahMinutesValue = (fixedJamaahMinutesValue + 5).coerceAtMost(maxAllowedClock)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SlateStrokeBorder),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text("+5m", color = CrispOffWhite, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") {
                                        fixedAdhanMinutesValue = (fixedAdhanMinutesValue + 15).coerceAtMost(maxAllowedClock)
                                    } else {
                                        fixedJamaahMinutesValue = (fixedJamaahMinutesValue + 15).coerceAtMost(maxAllowedClock)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
                                modifier = Modifier.testTag("jamaah_plus_button")
                            ) {
                                Text("+15m", color = ElegantGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        // Quick Rounded Clock Time Presets around this prayer's natural window
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
                                    color = if (isPresetSelected) GoldContainerDark else DeepSlateDarker,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isPresetSelected) ElegantGold else SlateStrokeBorder
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
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
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isPresetSelected) ElegantGold else CrispOffWhite,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Offset Mode Stepper Controls (-5m, -1m, +1m, +5m)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") {
                                        adhanOffsetValue = (adhanOffsetValue - 5).coerceAtLeast(0)
                                    } else {
                                        jamaahOffsetValue = (jamaahOffsetValue - 5).coerceAtLeast(0)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("jamaah_minus_button")
                            ) {
                                Text("-5m", color = ElegantGold, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") {
                                        adhanOffsetValue = (adhanOffsetValue - 1).coerceAtLeast(0)
                                    } else {
                                        jamaahOffsetValue = (jamaahOffsetValue - 1).coerceAtLeast(0)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SlateStrokeBorder),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("-1m", color = CrispOffWhite, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") {
                                        adhanOffsetValue = (adhanOffsetValue + 1).coerceAtMost(180)
                                    } else {
                                        jamaahOffsetValue = (jamaahOffsetValue + 1).coerceAtMost(180)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SlateStrokeBorder),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("+1m", color = CrispOffWhite, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (activeTierTab == "ADHAN") {
                                        adhanOffsetValue = (adhanOffsetValue + 5).coerceAtMost(180)
                                    } else {
                                        jamaahOffsetValue = (jamaahOffsetValue + 5).coerceAtMost(180)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("jamaah_plus_button")
                            ) {
                                Text("+5m", color = ElegantGold, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Quick Offset Presets
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
                                    color = if (isPresetSelected) GoldContainerDark else DeepSlateDarker,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isPresetSelected) ElegantGold else SlateStrokeBorder
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            if (activeTierTab == "ADHAN") {
                                                adhanOffsetValue = presetOff
                                            } else {
                                                jamaahOffsetValue = presetOff
                                            }
                                        }
                                ) {
                                    Text(
                                        text = if (presetOff == 0) "At PST (0m)" else "+${presetOff}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isPresetSelected) ElegantGold else CrispOffWhite,
                                        fontWeight = FontWeight.SemiBold,
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
private fun LocationSearchModalDialog(
    uiState: PrayerDashboardUiState,
    onDismiss: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSelectLocation: (CityLocation) -> Unit,
    onAddCustomLocation: (String, String, Double, Double) -> Unit,
    onUseLiveGps: () -> Unit
) {
    var showCustomCoordEntry by remember { mutableStateOf(false) }
    var customTownName by remember { mutableStateOf("") }
    var customRegion by remember { mutableStateOf("") }
    var customLat by remember { mutableStateOf("21.4225") }
    var customLon by remember { mutableStateOf("39.8262") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 640.dp)
                .testTag("location_search_modal"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSlateSurface),
            border = BorderStroke(1.2.dp, ElegantGold.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Any City, Town or Area",
                            style = MaterialTheme.typography.titleLarge,
                            color = CrispOffWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.availableCities.size}+ alphabetical world cities + live worldwide place search",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedPeriwinkle
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close location search",
                            tint = MutedPeriwinkle
                        )
                    }
                }

                // Search TextField
                OutlinedTextField(
                    value = uiState.locationSearchQuery,
                    onValueChange = onQueryChanged,
                    placeholder = {
                        Text(
                            text = "Search any city, town, or neighborhood worldwide...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedPeriwinkle
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = ElegantGold
                        )
                    },
                    trailingIcon = {
                        if (uiState.isSearchingLocations) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = ElegantGold
                            )
                        } else if (uiState.locationSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear search",
                                    tint = MutedPeriwinkle,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElegantGold,
                        unfocusedBorderColor = SlateStrokeBorder,
                        focusedTextColor = CrispOffWhite,
                        unfocusedTextColor = CrispOffWhite,
                        focusedContainerColor = DeepSlateDarker,
                        unfocusedContainerColor = DeepSlateDarker
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_search_input")
                )

                // Global Quick Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickPlaces = listOf("Abu Dhabi", "Cairo", "Doha", "Dubai", "Istanbul", "Kuala Lumpur", "London", "Madinah", "Makkah")
                    quickPlaces.forEach { place ->
                        Surface(
                            color = DeepSlateDarker,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onQueryChanged(place) }
                        ) {
                            Text(
                                text = place,
                                style = MaterialTheme.typography.labelSmall,
                                color = ElegantGold,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Action Row: Use Live GPS or Toggle Custom Coordinate Entry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onUseLiveGps,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldCompleted,
                            contentColor = DeepSlateBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Filled.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Use Live GPS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = { showCustomCoordEntry = !showCustomCoordEntry },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = ElegantGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showCustomCoordEntry) "Hide Custom" else "Custom Lat/Lon",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElegantGold,
                            maxLines = 1
                        )
                    }
                }

                if (showCustomCoordEntry) {
                    Surface(
                        color = DeepSlateDarker,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SlateStrokeBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customTownName,
                                    onValueChange = { customTownName = it },
                                    label = { Text("Town / Area Name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customRegion,
                                    onValueChange = { customRegion = it },
                                    label = { Text("State / Country") },
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
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElegantGold,
                                    contentColor = OnGoldDark
                                )
                            ) {
                                Text("Save & Select Custom Town", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Results List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.locationSearchResults, key = { it.id }) { city ->
                        val isSelected = city.id == uiState.selectedCity.id
                        Surface(
                            color = if (isSelected) GoldContainerDark else DeepSlateDarker,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) ElegantGold else SlateStrokeBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectLocation(city) }
                                .testTag("city_option_${city.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = if (isSelected) ElegantGold else EmeraldCompleted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = city.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isSelected) ElegantGold else CrispOffWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${city.country} • ${String.format(Locale.US, "%.3f", city.latitude)}°, ${String.format(Locale.US, "%.3f", city.longitude)}°",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MutedPeriwinkle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = ElegantGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SolarWarningRowCard(
    item: SolarWarningItemUiModel,
    isCompact: Boolean
) {
    val startFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(item.startMinutes)
    val endFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(item.endMinutes)

    Surface(
        color = CoralAlert.copy(alpha = if (item.isActiveNow) 0.22f else 0.14f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (item.isActiveNow) 1.5.dp else 1.dp,
            color = CoralAlert.copy(alpha = if (item.isActiveNow) 0.85f else 0.42f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("solar_warning_row_${item.id.lowercase()}")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val fontScale = LocalDensity.current.fontScale
            val isTightSpace = isCompact || maxWidth < 385.dp || fontScale > 1.03f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isTightSpace) 11.dp else 14.dp,
                        vertical = if (isTightSpace) 8.dp else 10.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isTightSpace) 9.dp else 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = item.title,
                    tint = CoralAlert,
                    modifier = Modifier.size(if (isTightSpace) 20.dp else 22.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = if (isTightSpace) 12.sp else 13.sp,
                        color = MutedPeriwinkle,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = startFormatted,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = if (isTightSpace) 13.sp else 14.sp,
                            color = CrispOffWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                                .height(1.dp)
                                .background(CoralAlert.copy(alpha = 0.45f))
                        )

                        Text(
                            text = endFormatted,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = if (isTightSpace) 13.sp else 14.sp,
                            color = CrispOffWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerRowCard(
    prayer: PrayerItemUiModel,
    onToggleCompleted: () -> Unit,
    onEditAdhan: () -> Unit,
    onEditJamaah: () -> Unit,
    isCompact: Boolean
) {
    val isMissedQada = prayer.completionStatus == PrayerCompletionStatus.MISSED_QADA
    val isHighlighted = prayer.isCurrent || prayer.isNext
    val borderColor = when {
        prayer.isCurrent -> ElegantGold
        prayer.isNext -> ElegantGold.copy(alpha = 0.65f)
        prayer.isPrayed -> EmeraldCompleted.copy(alpha = 0.45f)
        isMissedQada -> CoralAlert.copy(alpha = 0.65f)
        else -> SlateStrokeBorder
    }
    val containerColor = when {
        isHighlighted -> ElevatedSlateCard
        else -> ElevatedSlateSurface
    }

    val pstFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(prayer.pstMinutes)
    val endFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(prayer.displayEndMinutes)
    val adhanFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(prayer.adhanMinutes)
    val jamaahFormatted = PrayerTimeCalculator.formatMinutesTo12Hour(prayer.jamaahMinutes)

    val prayerTimeIcon = when (prayer.id) {
        "FAJR" -> Icons.Outlined.Cloud
        "DHUHR" -> Icons.Outlined.WbSunny
        "ASR" -> Icons.Outlined.Cloud
        "MAGHRIB" -> Icons.Outlined.WbTwilight
        "ISHA" -> Icons.Outlined.NightsStay
        else -> Icons.Outlined.WbSunny
    }

    val prayerIconTint = when {
        isHighlighted -> ElegantGold
        prayer.isPrayed -> EmeraldCompleted
        isMissedQada -> CoralAlert
        else -> MutedPeriwinkle
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(if (isHighlighted) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_row_${prayer.id.lowercase()}")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val fontScale = LocalDensity.current.fontScale
            val isTightSpace = isCompact || maxWidth < 385.dp || fontScale > 1.03f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isTightSpace) 13.dp else 16.dp,
                        vertical = if (isTightSpace) 12.dp else 15.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(if (isTightSpace) 8.dp else 10.dp)
            ) {
                // Top Row: Time-of-Day Icon + English & Arabic Prayer Name on Left; Vertically Stacked Adhan & Jama'ah Pills + Check on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isTightSpace) 8.dp else 11.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isTightSpace) 34.dp else 38.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isHighlighted -> GoldContainerDark.copy(alpha = 0.65f)
                                        prayer.isPrayed -> EmeraldContainer.copy(alpha = 0.35f)
                                        isMissedQada -> CoralAlert.copy(alpha = 0.2f)
                                        else -> DeepSlateDarker.copy(alpha = 0.8f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = prayerTimeIcon,
                                contentDescription = prayer.nameEnglish,
                                tint = prayerIconTint,
                                modifier = Modifier.size(if (isTightSpace) 18.dp else 20.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (isTightSpace) 5.dp else 6.dp)
                        ) {
                            Text(
                                text = prayer.nameEnglish,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = if (isTightSpace) 15.sp else 17.5.sp,
                                color = when {
                                    isHighlighted -> ElegantGold
                                    isMissedQada -> CoralAlert
                                    else -> CrispOffWhite
                                },
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = prayer.nameArabic,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = if (isTightSpace) 13.5.sp else 15.sp,
                                color = if (isHighlighted) ElegantGold else MutedPeriwinkle,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Right: Vertically Stacked Adhan & Jama'ah Pills + Circular Checkmark / Qaza Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isTightSpace) 6.dp else 10.dp)
                    ) {
                        // Vertically Stacked Time Pills (Top = Adhan, Bottom = Jama'ah)
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Top Pill: Adhan
                            Surface(
                                color = DeepSlateDarker.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, SlateStrokeBorder.copy(alpha = 0.85f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(onClick = onEditAdhan)
                                    .testTag("edit_adhan_${prayer.id.lowercase()}")
                            ) {
                                Text(
                                    text = "Adhan $adhanFormatted",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = if (isTightSpace) 10.sp else 11.sp,
                                    color = CrispOffWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(
                                        horizontal = if (isTightSpace) 7.dp else 9.dp,
                                        vertical = 3.dp
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // Bottom Pill: Jama'ah
                            Surface(
                                color = if (prayer.isCurrent) GoldContainerDark else DeepSlateDarker.copy(alpha = 0.95f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(
                                    1.dp,
                                    if (prayer.isCurrent) ElegantGold else ElegantGold.copy(alpha = 0.55f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(onClick = onEditJamaah)
                                    .testTag("edit_jamaah_${prayer.id.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = if (isTightSpace) 7.dp else 9.dp,
                                        vertical = 3.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "Jama'ah $jamaahFormatted",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = if (isTightSpace) 10.sp else 11.sp,
                                        color = ElegantGold,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit times",
                                        tint = ElegantGold,
                                        modifier = Modifier.size(9.dp)
                                    )
                                }
                            }
                        }

                        // Completion Check Button: Unlocked at Prayer Start Time (PST), or logs Qaza make-up
                        val isLockedBeforePst = !prayer.isPrayed && !prayer.canMarkPrayed
                        IconButton(
                            onClick = onToggleCompleted,
                            modifier = Modifier
                                .size(if (isTightSpace) 36.dp else 40.dp)
                                .testTag("toggle_prayer_${prayer.id.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isTightSpace) 28.dp else 32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            prayer.isPrayed -> EmeraldCompleted
                                            isLockedBeforePst -> DeepSlateDarker.copy(alpha = 0.7f)
                                            isMissedQada -> CoralAlert.copy(alpha = 0.25f)
                                            else -> EmeraldContainer.copy(alpha = 0.35f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isMissedQada && !prayer.isPrayed) {
                                    Text(
                                        text = "Qaza",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.5.sp,
                                        color = CoralAlert,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isLockedBeforePst) Icons.Outlined.Lock else Icons.Filled.Check,
                                        contentDescription = when {
                                            prayer.isPrayed -> "${prayer.nameEnglish} completed"
                                            isLockedBeforePst -> "${prayer.nameEnglish} unlocks at Start Time ($pstFormatted)"
                                            else -> "Mark ${prayer.nameEnglish} as completed"
                                        },
                                        tint = when {
                                            prayer.isPrayed -> DeepSlateBlue
                                            isLockedBeforePst -> MutedPeriwinkle.copy(alpha = 0.35f)
                                            else -> MutedPeriwinkle.copy(alpha = 0.7f)
                                        },
                                        modifier = Modifier.size(if (isLockedBeforePst) 14.dp else 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Row: Prayer Start Time ───────── Prayer End Time (matching SolarWarningRowCard)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pstFormatted,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = if (isTightSpace) 12.5.sp else 13.5.sp,
                        color = if (isHighlighted) ElegantGold else CrispOffWhite.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .height(1.dp)
                            .background(
                                when {
                                    isHighlighted -> ElegantGold.copy(alpha = 0.5f)
                                    prayer.isPrayed -> EmeraldCompleted.copy(alpha = 0.4f)
                                    isMissedQada -> CoralAlert.copy(alpha = 0.45f)
                                    else -> SlateStrokeBorder.copy(alpha = 0.85f)
                                }
                            )
                    )

                    Text(
                        text = endFormatted,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = if (isTightSpace) 12.5.sp else 13.5.sp,
                        color = if (isHighlighted) ElegantGold else CrispOffWhite.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
