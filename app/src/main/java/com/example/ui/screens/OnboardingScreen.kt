package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.core.content.ContextCompat
import com.example.domain.CityLocation
import com.example.domain.PrayerCalculationConfig
import com.example.domain.PrayerTimeCalculator
import com.example.ui.components.AppleHairlineDivider
import com.example.ui.components.AsrMadhabSelectorBlock
import com.example.ui.components.AutoSizeSingleLineText
import com.example.ui.components.CalculationLivePreviewStrip
import com.example.ui.components.CalculationMethodSelectorList
import com.example.ui.components.SwissInsetGroup
import com.example.ui.components.SwissSectionLabel
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.NoorTheme
import com.example.ui.theme.arabicCalligraphyStyle
import com.example.ui.theme.asMonoPrecision
import com.example.ui.theme.withTabularDigits
import com.example.viewmodel.OnboardingCatchUpChoice
import com.example.viewmodel.PrayerDashboardUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class OnboardingCatchUpPrayerRow(
    val prayerId: String,
    val nameEnglish: String,
    val nameArabic: String,
    val pstMinutes: Int
)

private data class OnboardingCatchUpContext(
    val targetDateIso: String,
    val isPastMidnightYesterdayMode: Boolean,
    val heading: String,
    val subtitle: String,
    val prayers: List<OnboardingCatchUpPrayerRow>
)

@Composable
fun OnboardingScreenContent(
    uiState: PrayerDashboardUiState,
    onSelectCity: (CityLocation) -> Unit,
    onSearchLocationQueryChanged: (String) -> Unit,
    onAddCustomLocation: (String, String, Double, Double) -> Unit,
    onRequestGpsLocation: () -> Unit,
    isDeviceLocationEnabled: () -> Boolean,
    onMarkPendingGpsAfterSettings: () -> Unit,
    onUpdateCalculationConfig: (PrayerCalculationConfig) -> Unit,
    onCompleteOnboarding: (String?, Map<String, OnboardingCatchUpChoice>, Boolean) -> Unit,
    onBannerMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val colors = NoorTheme.colors

    var currentStep by remember { mutableIntStateOf(0) } // 0..3
    var ignorePriorDaysChoice by remember { mutableStateOf(true) }
    val catchUpSelections = remember { mutableStateMapOf<String, OnboardingCatchUpChoice>() }
    var gpsStatusNote by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = currentStep > 0) {
        currentStep = (currentStep - 1).coerceAtLeast(0)
    }

    fun launchLocationEnableSettings() {
        onMarkPendingGpsAfterSettings()
        gpsStatusNote = "Turn on Device Location in Android Settings, then return here."
        onBannerMessage("Turn on Location Services to resolve your GPS position")
        runCatching {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }.onFailure {
            gpsStatusNote = "Could not open Android Location Settings. Select your city manually below."
        }
    }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            if (!isDeviceLocationEnabled()) {
                launchLocationEnableSettings()
            } else {
                gpsStatusNote = "Acquiring GPS fix..."
                onRequestGpsLocation()
            }
        } else {
            gpsStatusNote = "Location permission denied. You can search or enter your city below."
            onBannerMessage("Location permission denied — select a city manually")
        }
    }

    fun triggerGpsFlow() {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            gpsPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else if (!isDeviceLocationEnabled()) {
            launchLocationEnableSettings()
        } else {
            gpsStatusNote = "Acquiring GPS fix..."
            onRequestGpsLocation()
        }
    }

    // Compute time-aware catch-up context for Step 3 whenever city or calculation method changes
    val catchUpContext = remember(uiState.selectedCity, uiState.calculationConfig, currentStep) {
        computeOnboardingCatchUpContext(uiState.selectedCity, uiState.calculationConfig)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
            .testTag("onboarding_root_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
        ) {
            // Top step progress header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        IconButton(
                            onClick = { currentStep -= 1 },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("onboarding_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Previous step",
                                tint = colors.inkPrimary
                            )
                        }
                    } else {
                        Box(modifier = Modifier.size(40.dp))
                    }

                    Text(
                        text = "STEP ${currentStep + 1} OF 4",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = IbmPlexMonoFontFamily,
                            letterSpacing = 1.2.sp
                        ),
                        color = colors.accentGold,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("onboarding_step_indicator")
                    )

                    Box(modifier = Modifier.size(40.dp))
                }

                // 2dp hairline progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(colors.hairline)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((currentStep + 1) / 4f)
                            .background(colors.accentGold)
                    )
                }
            }

            // Scrollable step body
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn(tween(180)).togetherWith(fadeOut(tween(140)))
                },
                modifier = Modifier.weight(1f),
                label = "onboarding_step_content"
            ) { step ->
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    when (step) {
                        0 -> OnboardingLocationStep(
                            uiState = uiState,
                            gpsStatusNote = gpsStatusNote,
                            onTriggerGps = ::triggerGpsFlow,
                            onSelectCity = { city ->
                                gpsStatusNote = null
                                onSelectCity(city)
                            },
                            onSearchQueryChanged = onSearchLocationQueryChanged,
                            onAddCustomLocation = { name, country, lat, lon ->
                                gpsStatusNote = null
                                onAddCustomLocation(name, country, lat, lon)
                            }
                        )

                        1 -> OnboardingCalculationStep(
                            uiState = uiState,
                            onUpdateCalculationConfig = onUpdateCalculationConfig
                        )

                        2 -> OnboardingCatchUpStep(
                            catchUpContext = catchUpContext,
                            selections = catchUpSelections
                        )

                        3 -> OnboardingAnalyticsStartStep(
                            uiState = uiState,
                            catchUpContext = catchUpContext,
                            loggedCatchUpCount = catchUpSelections.values.count { it != OnboardingCatchUpChoice.UNMARKED },
                            ignorePriorDays = ignorePriorDaysChoice,
                            onSelectIgnorePriorDays = { ignorePriorDaysChoice = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Sticky bottom action footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.canvas)
            ) {
                AppleHairlineDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep == 2 && catchUpContext.prayers.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                catchUpSelections.clear()
                                currentStep = 3
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.5.dp, colors.hairlineStrong),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("onboarding_skip_catchup_button")
                        ) {
                            AutoSizeSingleLineText(
                                text = "Skip",
                                style = MaterialTheme.typography.labelLarge,
                                maxFontSize = 13.sp,
                                minFontSize = 11.sp,
                                color = colors.inkSecondary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < 3) {
                                currentStep += 1
                            } else {
                                onCompleteOnboarding(
                                    if (catchUpContext.prayers.isNotEmpty()) catchUpContext.targetDateIso else null,
                                    catchUpSelections.toMap(),
                                    ignorePriorDaysChoice
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.inkPrimary,
                            contentColor = colors.surfaceCard
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag(if (currentStep < 3) "onboarding_continue_button" else "onboarding_finish_button")
                    ) {
                        AutoSizeSingleLineText(
                            text = if (currentStep < 3) "Continue" else "Finish & Start Tracking",
                            style = MaterialTheme.typography.titleSmall,
                            maxFontSize = 14.5.sp,
                            minFontSize = 12.sp,
                            color = colors.surfaceCard,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (currentStep < 3) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepHeader(
    title: String,
    arabicLabel: String,
    subtitle: String
) {
    val colors = NoorTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.inkPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f, fill = false)
            )
            Text(
                text = arabicLabel,
                style = arabicCalligraphyStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = colors.accentGold
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkSecondary
        )
    }
}

@Composable
private fun OnboardingLocationStep(
    uiState: PrayerDashboardUiState,
    gpsStatusNote: String?,
    onTriggerGps: () -> Unit,
    onSelectCity: (CityLocation) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onAddCustomLocation: (String, String, Double, Double) -> Unit
) {
    val colors = NoorTheme.colors
    var showCustomCoords by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customCountry by remember { mutableStateOf("") }
    var customLat by remember { mutableStateOf("") }
    var customLon by remember { mutableStateOf("") }
    var customError by remember { mutableStateOf<String?>(null) }

    OnboardingStepHeader(
        title = "Choose Your Location",
        arabicLabel = "الموقع",
        subtitle = "Accurate solar prayer times depend on your exact coordinates. Use live GPS or pick any city worldwide."
    )

    // Selected Location Status Card
    SwissInsetGroup(
        borderColor = colors.accentGold.copy(alpha = 0.45f),
        modifier = Modifier.testTag("onboarding_selected_location_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "ACTIVE LOCATION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = IbmPlexMonoFontFamily,
                        letterSpacing = 0.9.sp
                    ),
                    color = colors.accentGold
                )
                Text(
                    text = uiState.selectedCity.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.inkPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${uiState.selectedCity.country} · ${
                        String.format(Locale.US, "%.3f°, %.3f°", uiState.selectedCity.latitude, uiState.selectedCity.longitude)
                    }",
                    style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                    color = colors.inkSecondary
                )
            }
            Icon(
                imageVector = if (uiState.selectedCity.isGpsSimulated) Icons.Rounded.GpsFixed else Icons.Rounded.LocationOn,
                contentDescription = "Selected location",
                tint = colors.accentGold,
                modifier = Modifier.size(22.dp)
            )
        }
    }

    // Option 1: Live GPS
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SwissSectionLabel(title = "Automatic GPS")
        SwissInsetGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Use your phone's GPS to detect your exact town and coordinates. If Location is off, you'll be taken straight to Location Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkSecondary
                )

                Button(
                    onClick = onTriggerGps,
                    enabled = !uiState.isResolvingGps,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentGold,
                        contentColor = colors.onAccentGold
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("onboarding_use_gps_button")
                ) {
                    if (uiState.isResolvingGps) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = colors.onAccentGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Detecting GPS Location...",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.GpsFixed,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AutoSizeSingleLineText(
                            text = "Use Live GPS Location",
                            style = MaterialTheme.typography.labelLarge,
                            maxFontSize = 14.sp,
                            minFontSize = 11.5.sp,
                            color = colors.onAccentGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!gpsStatusNote.isNullOrBlank()) {
                    Text(
                        text = gpsStatusNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.accentGold
                    )
                }
            }
        }
    }

    // Option 2: Manual City Search
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SwissSectionLabel(
            title = "Or Select Manually",
            trailingText = "${uiState.sqliteFallbackCityCount} CITIES"
        )

        OutlinedTextField(
            value = uiState.locationSearchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = {
                Text(
                    text = "Search any city, town, or village...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkTertiary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search city",
                    tint = colors.inkTertiary
                )
            },
            trailingIcon = {
                if (uiState.isSearchingLocations) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colors.accentGold
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accentGold,
                unfocusedBorderColor = colors.hairlineStrong,
                focusedContainerColor = colors.surfaceCard,
                unfocusedContainerColor = colors.surfaceCard,
                focusedTextColor = colors.inkPrimary,
                unfocusedTextColor = colors.inkPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_city_search_input")
        )

        val visibleResults = remember(uiState.locationSearchResults, uiState.selectedCity) {
            uiState.locationSearchResults.take(10)
        }

        if (visibleResults.isNotEmpty()) {
            SwissInsetGroup {
                visibleResults.forEachIndexed { idx, city ->
                    val isSelected = city.id == uiState.selectedCity.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCity(city) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("onboarding_city_item_${city.id}"),
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
                                text = city.country,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkSecondary
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected city",
                                tint = colors.accentGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (idx < visibleResults.lastIndex) {
                        AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }

        // Custom coordinates toggle
        Text(
            text = if (showCustomCoords) "Hide custom coordinates" else "+ Enter custom latitude & longitude",
            style = MaterialTheme.typography.labelLarge,
            color = colors.accentGold,
            modifier = Modifier
                .clickable { showCustomCoords = !showCustomCoords }
                .padding(vertical = 4.dp)
                .testTag("onboarding_toggle_custom_coords")
        )

        AnimatedVisibility(visible = showCustomCoords) {
            SwissInsetGroup {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it; customError = null },
                        label = { Text("Town / City Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customCountry,
                        onValueChange = { customCountry = it },
                        label = { Text("Country / Region") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = customLat,
                            onValueChange = { customLat = it; customError = null },
                            label = { Text("Latitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customLon,
                            onValueChange = { customLon = it; customError = null },
                            label = { Text("Longitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (customError != null) {
                        Text(
                            text = customError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.accentCoral
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val latVal = customLat.trim().toDoubleOrNull()
                            val lonVal = customLon.trim().toDoubleOrNull()
                            if (customName.isBlank() || latVal == null || lonVal == null || latVal !in -65.0..65.0 || lonVal !in -180.0..180.0) {
                                customError = "Enter a valid town name, latitude (-65 to 65), and longitude (-180 to 180)."
                            } else {
                                onAddCustomLocation(customName, customCountry, latVal, lonVal)
                                showCustomCoords = false
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, colors.accentGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Custom Coordinates", color = colors.accentGold)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingCalculationStep(
    uiState: PrayerDashboardUiState,
    onUpdateCalculationConfig: (PrayerCalculationConfig) -> Unit
) {
    OnboardingStepHeader(
        title = "Madhab & Calculation",
        arabicLabel = "طريقة الحساب",
        subtitle = "Choose your Asr juristic school and prayer calculation convention, or set your own custom Fajr & Isha degrees."
    )

    CalculationLivePreviewStrip(
        city = uiState.selectedCity,
        config = uiState.calculationConfig
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SwissSectionLabel(title = "Asr Juristic Method")
        SwissInsetGroup {
            Box(modifier = Modifier.padding(16.dp)) {
                AsrMadhabSelectorBlock(
                    selectedMadhab = uiState.calculationConfig.asrMadhab,
                    onSelectMadhab = { madhab ->
                        onUpdateCalculationConfig(uiState.calculationConfig.copy(asrMadhab = madhab))
                    }
                )
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SwissSectionLabel(
            title = "Calculation Convention",
            trailingText = uiState.calculationConfig.angleSpecLabel
        )
        CalculationMethodSelectorList(
            config = uiState.calculationConfig,
            onUpdateConfig = onUpdateCalculationConfig
        )
    }
}

@Composable
private fun OnboardingCatchUpStep(
    catchUpContext: OnboardingCatchUpContext,
    selections: MutableMap<String, OnboardingCatchUpChoice>
) {
    val colors = NoorTheme.colors

    OnboardingStepHeader(
        title = catchUpContext.heading,
        arabicLabel = "الصلوات",
        subtitle = catchUpContext.subtitle
    )

    if (catchUpContext.prayers.isEmpty()) {
        SwissInsetGroup(borderColor = colors.accentEmerald.copy(alpha = 0.4f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Fresh Start Ahead",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.accentEmerald,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "No prayers have passed yet for today. Continue to pick how you'd like Analytics to track your history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkSecondary
                )
            }
        }
        return
    }

    // Quick Batch Actions
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickBatchPill(
            label = "All Jama'ah",
            accentColor = colors.accentEmerald,
            onClick = {
                catchUpContext.prayers.forEach { p ->
                    selections[p.prayerId] = OnboardingCatchUpChoice.JAMAAH
                }
            },
            modifier = Modifier
                .weight(1f)
                .testTag("onboarding_catchup_all_jamaah")
        )
        QuickBatchPill(
            label = "All Solo",
            accentColor = colors.accentGold,
            onClick = {
                catchUpContext.prayers.forEach { p ->
                    selections[p.prayerId] = OnboardingCatchUpChoice.SOLO
                }
            },
            modifier = Modifier
                .weight(1f)
                .testTag("onboarding_catchup_all_solo")
        )
        QuickBatchPill(
            label = "Clear",
            accentColor = colors.inkSecondary,
            onClick = { selections.clear() },
            modifier = Modifier
                .weight(0.7f)
                .testTag("onboarding_catchup_clear")
        )
    }

    SwissInsetGroup(modifier = Modifier.testTag("onboarding_catchup_prayers_card")) {
        Column(modifier = Modifier.fillMaxWidth()) {
            catchUpContext.prayers.forEachIndexed { idx, row ->
                val current = selections[row.prayerId] ?: OnboardingCatchUpChoice.UNMARKED
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("onboarding_catchup_row_${row.prayerId.lowercase()}"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            Text(
                                text = row.nameEnglish,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.inkPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = row.nameArabic,
                                style = arabicCalligraphyStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = colors.accentGold
                            )
                        }

                        Text(
                            text = PrayerTimeCalculator.formatMinutesTo12Hour(row.pstMinutes),
                            style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                            color = colors.inkTertiary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.segmentedTrack)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        CatchUpOptionChip(
                            label = "Jama'ah",
                            selected = current == OnboardingCatchUpChoice.JAMAAH,
                            activeColor = colors.accentEmerald,
                            onClick = {
                                selections[row.prayerId] = if (current == OnboardingCatchUpChoice.JAMAAH) {
                                    OnboardingCatchUpChoice.UNMARKED
                                } else {
                                    OnboardingCatchUpChoice.JAMAAH
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_chip_jamaah_${row.prayerId.lowercase()}")
                        )
                        CatchUpOptionChip(
                            label = "Solo",
                            selected = current == OnboardingCatchUpChoice.SOLO,
                            activeColor = colors.accentGold,
                            onClick = {
                                selections[row.prayerId] = if (current == OnboardingCatchUpChoice.SOLO) {
                                    OnboardingCatchUpChoice.UNMARKED
                                } else {
                                    OnboardingCatchUpChoice.SOLO
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_chip_solo_${row.prayerId.lowercase()}")
                        )
                        CatchUpOptionChip(
                            label = "Qaza",
                            selected = current == OnboardingCatchUpChoice.QAZA_DONE,
                            activeColor = colors.accentGoldBright,
                            onClick = {
                                selections[row.prayerId] = if (current == OnboardingCatchUpChoice.QAZA_DONE) {
                                    OnboardingCatchUpChoice.UNMARKED
                                } else {
                                    OnboardingCatchUpChoice.QAZA_DONE
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_chip_qaza_${row.prayerId.lowercase()}")
                        )
                        CatchUpOptionChip(
                            label = "Missed",
                            selected = current == OnboardingCatchUpChoice.MISSED,
                            activeColor = colors.accentCoral,
                            onClick = {
                                selections[row.prayerId] = if (current == OnboardingCatchUpChoice.MISSED) {
                                    OnboardingCatchUpChoice.UNMARKED
                                } else {
                                    OnboardingCatchUpChoice.MISSED
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_chip_missed_${row.prayerId.lowercase()}")
                        )
                    }
                }

                if (idx < catchUpContext.prayers.lastIndex) {
                    AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickBatchPill(
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NoorTheme.colors
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(colors.surfaceCard)
            .border(0.5.dp, colors.hairlineStrong, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        AutoSizeSingleLineText(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxFontSize = 11.5.sp,
            minFontSize = 9.sp,
            color = accentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CatchUpOptionChip(
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NoorTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) colors.segmentedThumb else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AutoSizeSingleLineText(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxFontSize = 11.sp,
            minFontSize = 8.5.sp,
            color = if (selected) activeColor else colors.inkSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun OnboardingAnalyticsStartStep(
    uiState: PrayerDashboardUiState,
    catchUpContext: OnboardingCatchUpContext,
    loggedCatchUpCount: Int,
    ignorePriorDays: Boolean,
    onSelectIgnorePriorDays: (Boolean) -> Unit
) {
    val colors = NoorTheme.colors

    OnboardingStepHeader(
        title = "Analytics Starting Point",
        arabicLabel = "الإحصائيات",
        subtitle = "Choose how earlier days and weeks in your 7-week Analytics grid should be handled."
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AnalyticsChoiceCard(
            title = "Start Fresh from Today",
            badge = "RECOMMENDED",
            description = "Ignore prior days and weeks in your streak, Qaza, and consistency stats. Earlier squares appear in a muted color and remain tappable if you ever want to log a past day.",
            selected = ignorePriorDays,
            onClick = { onSelectIgnorePriorDays(true) },
            testTag = "onboarding_analytics_start_fresh_card"
        )

        AnalyticsChoiceCard(
            title = "Mark Prior Days Manually",
            badge = "FULL 7-WEEK GRID",
            description = "Keep all prior days in the 7-week Analytics grid open so you can manually fill in earlier prayers at your own pace.",
            selected = !ignorePriorDays,
            onClick = { onSelectIgnorePriorDays(false) },
            testTag = "onboarding_analytics_manual_prior_card"
        )
    }

    // Concise Setup Summary
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SwissSectionLabel(title = "Ready Summary")
        SwissInsetGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryKeyValueRow("Location", "${uiState.selectedCity.name}, ${uiState.selectedCity.country}")
                AppleHairlineDivider()
                SummaryKeyValueRow("Calculation", uiState.calculationConfig.shortSummaryBadge)
                AppleHairlineDivider()
                SummaryKeyValueRow(
                    label = if (catchUpContext.isPastMidnightYesterdayMode) "Yesterday's Catch-Up" else "Today's Catch-Up",
                    value = if (loggedCatchUpCount > 0) "$loggedCatchUpCount prayers marked" else "Starting fresh"
                )
                AppleHairlineDivider()
                SummaryKeyValueRow(
                    label = "Analytics History",
                    value = if (ignorePriorDays) "Ignore prior days (muted)" else "Manual prior days"
                )
            }
        }
    }
}

@Composable
private fun AnalyticsChoiceCard(
    title: String,
    badge: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = NoorTheme.colors
    SwissInsetGroup(
        borderColor = if (selected) colors.accentGold else colors.hairline,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (selected) colors.accentGold else Color.Transparent)
                    .then(
                        if (!selected) {
                            Modifier.border(1.25.dp, colors.hairlineStrong, CircleShape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = colors.onAccentGold,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.inkPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = IbmPlexMonoFontFamily,
                            fontSize = 9.sp,
                            letterSpacing = 0.6.sp
                        ),
                        color = if (selected) colors.accentGold else colors.inkTertiary
                    )
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkSecondary
                )
            }
        }
    }
}

@Composable
private fun SummaryKeyValueRow(
    label: String,
    value: String
) {
    val colors = NoorTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkSecondary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.withTabularDigits(),
            color = colors.inkPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun computeOnboardingCatchUpContext(
    city: CityLocation,
    config: PrayerCalculationConfig
): OnboardingCatchUpContext {
    val cityTz = PrayerTimeCalculator.getTimeZoneForCity(city)
    val nowCal = Calendar.getInstance(cityTz)
    val nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
    val todayCalc = PrayerTimeCalculator.calculateDailyPrayers(city, nowCal, config)
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = cityTz }
    val friendlyFormatter = SimpleDateFormat("EEEE, d MMM", Locale.US).apply { timeZone = cityTz }

    val isPastMidnightBeforeFajr = nowMinutes < todayCalc.fajrMinutes
    if (isPastMidnightBeforeFajr) {
        val yesterdayCal = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayCalc = PrayerTimeCalculator.calculateDailyPrayers(city, yesterdayCal, config)
        val isYesterdayFriday = yesterdayCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
        val yesterdayIso = dateFormatter.format(yesterdayCal.time)
        val friendlyDate = friendlyFormatter.format(yesterdayCal.time)

        return OnboardingCatchUpContext(
            targetDateIso = yesterdayIso,
            isPastMidnightYesterdayMode = true,
            heading = "Log Yesterday's Prayers",
            subtitle = "It's past midnight before Fajr — record Fajr through Isha for $friendlyDate before the new day begins.",
            prayers = listOf(
                OnboardingCatchUpPrayerRow("FAJR", "Fajr", "الفجر", yesterdayCalc.fajrMinutes),
                OnboardingCatchUpPrayerRow(
                    "DHUHR",
                    if (isYesterdayFriday) "Jumu'ah" else "Dhuhr",
                    if (isYesterdayFriday) "الجمعة" else "الظهر",
                    yesterdayCalc.dhuhrMinutes
                ),
                OnboardingCatchUpPrayerRow("ASR", "Asr", "العصر", yesterdayCalc.asrMinutes),
                OnboardingCatchUpPrayerRow("MAGHRIB", "Maghrib", "المغرب", yesterdayCalc.maghribMinutes),
                OnboardingCatchUpPrayerRow("ISHA", "Isha", "العشاء", yesterdayCalc.ishaMinutes)
            )
        )
    }

    val isTodayFriday = nowCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
    val todayIso = dateFormatter.format(nowCal.time)
    val friendlyDate = friendlyFormatter.format(nowCal.time)
    val allToday = listOf(
        OnboardingCatchUpPrayerRow("FAJR", "Fajr", "الفجر", todayCalc.fajrMinutes),
        OnboardingCatchUpPrayerRow(
            "DHUHR",
            if (isTodayFriday) "Jumu'ah" else "Dhuhr",
            if (isTodayFriday) "الجمعة" else "الظهر",
            todayCalc.dhuhrMinutes
        ),
        OnboardingCatchUpPrayerRow("ASR", "Asr", "العصر", todayCalc.asrMinutes),
        OnboardingCatchUpPrayerRow("MAGHRIB", "Maghrib", "المغرب", todayCalc.maghribMinutes),
        OnboardingCatchUpPrayerRow("ISHA", "Isha", "العشاء", todayCalc.ishaMinutes)
    )
    val passedToday = allToday.filter { it.pstMinutes <= nowMinutes }

    return OnboardingCatchUpContext(
        targetDateIso = todayIso,
        isPastMidnightYesterdayMode = false,
        heading = "Today's Prayers So Far",
        subtitle = "Mark any prayers that have already started on $friendlyDate as Jama'ah, Solo, Qaza, or Missed.",
        prayers = passedToday
    )
}
