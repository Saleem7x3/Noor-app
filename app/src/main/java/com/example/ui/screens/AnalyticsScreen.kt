package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AthkaarHabitUiModel
import com.example.data.DayContributionCell
import com.example.data.HistoricalDayPrayerUiModel
import com.example.data.PrayerCompletionStatus
import com.example.ui.components.AppleHairlineDivider
import com.example.ui.components.AutoSizeSingleLineText
import com.example.ui.components.SwissInsetGroup
import com.example.ui.components.SwissSectionLabel
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.NoorTheme
import com.example.ui.theme.NoorThemePalette
import com.example.ui.theme.arabicCalligraphyStyle
import com.example.ui.theme.asMonoPrecision
import com.example.ui.theme.withTabularDigits
import com.example.viewmodel.PrayerDashboardUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreenContent(
    uiState: PrayerDashboardUiState,
    onStepAthkaar: (AthkaarHabitUiModel) -> Unit,
    onToggleAthkaarComplete: (AthkaarHabitUiModel) -> Unit,
    onAdjustQazaBacklog: (Int) -> Unit = {},
    onUpdateHistoricalPrayer: (String, String, String, Boolean, Boolean, PrayerCompletionStatus) -> Unit = { _, _, _, _, _, _ -> },
    modifier: Modifier = Modifier,
    isCompactShowcase: Boolean = false
) {
    val colors = NoorTheme.colors
    var selectedGridDateIso by remember { mutableStateOf<String?>(null) }
    val selectedDayCell = remember(selectedGridDateIso, uiState.weeklyGrid) {
        selectedGridDateIso?.let { dateIso ->
            uiState.weeklyGrid.find { it.dateIso == dateIso }
        }
    }
    val animatedTodayProgress by animateFloatAsState(
        targetValue = uiState.todayGoalFraction,
        animationSpec = tween(durationMillis = 480),
        label = "today_goals_progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("screen_3_analytics_dashboard"),
        verticalArrangement = Arrangement.spacedBy(if (isCompactShowcase) 18.dp else 24.dp)
    ) {
        // 1. Open Editorial KPI Strip directly on canvas (no rainbow ring, no nested stat boxes)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .testTag("todays_goals_ring_card"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SwissSectionLabel(
                title = stringResource(R.string.todays_goals_title),
                trailingText = "${uiState.todayPrayersCompleted}/5 FARDH · ${uiState.todayAthkaarCompleted}/3 ATHKAAR"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorialKpiColumn(
                    value = "${(animatedTodayProgress * 100).toInt()}%",
                    label = "TODAY",
                    accentColor = colors.inkPrimary,
                    modifier = Modifier.weight(1f)
                )
                VerticalHairline()
                EditorialKpiColumn(
                    value = "${uiState.currentStreakDays}d",
                    label = "STREAK",
                    accentColor = colors.accentGold,
                    modifier = Modifier.weight(1f)
                )
                VerticalHairline()
                EditorialKpiColumn(
                    value = "${uiState.jamaahRatePercent}%",
                    label = "JAMA'AH",
                    accentColor = colors.accentEmerald,
                    modifier = Modifier.weight(1f)
                )
                VerticalHairline()
                EditorialKpiColumn(
                    value = "${uiState.consistencyPercent}%",
                    label = "ON-TIME",
                    accentColor = colors.inkPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Ultra-thin 2dp progress hairline across full width
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(colors.hairline)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedTodayProgress.coerceIn(0f, 1f))
                        .background(if (animatedTodayProgress >= 0.99f) colors.accentEmerald else colors.accentGold)
                )
            }
        }

        // 2. Qaza Make-Up Section (single flat inset group, zero icon box, zero nested counter card)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SwissSectionLabel(title = "Qaza Make-Up")
            QazaBacklogInsetGroup(
                totalQazaBacklogCount = uiState.totalQazaBacklogCount,
                autoDetectedQazaCount = uiState.autoDetectedQazaCount,
                pendingQazaPrayerNames = uiState.pendingQazaPrayerNames,
                onAdjustQazaBacklog = onAdjustQazaBacklog
            )
        }

        // 3. 7-Week Consistency Grid (flat inset group, Swiss tonal squares, outline Today ring)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SwissSectionLabel(
                title = stringResource(R.string.weekly_grid_title),
                trailingText = "TAP DAY TO EDIT"
            )
            SwissInsetGroup(
                modifier = Modifier.testTag("weekly_contribution_grid_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isCompactShowcase) 14.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GitHubStyleContributionGrid(
                        cells = uiState.weeklyGrid,
                        onDayCellClick = { cell -> selectedGridDateIso = cell.dateIso }
                    )

                    AppleHairlineDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LESS",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily, fontSize = 9.sp),
                            color = colors.inkTertiary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0.15f, 0.45f, 0.75f, 1.0f).forEach { level ->
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(RoundedCornerShape(2.5.dp))
                                        .background(contributionCellColor(level, colors))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FULL",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily, fontSize = 9.sp),
                            color = colors.inkSecondary
                        )
                    }
                }
            }
        }

        // 4. Daily Athkaar (ALL 3 unified inside ONE flat inset group with 0.5dp hairlines)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SwissSectionLabel(title = stringResource(R.string.athkaar_section_title))

            SwissInsetGroup {
                uiState.athkaarList.forEachIndexed { index, habit ->
                    SwissAthkaarRowItem(
                        habit = habit,
                        onStepCounter = { onStepAthkaar(habit) },
                        onToggleComplete = { onToggleAthkaarComplete(habit) },
                        isCompact = isCompactShowcase
                    )
                    if (index < uiState.athkaarList.lastIndex) {
                        AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }

    if (selectedDayCell != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedGridDateIso = null },
            sheetState = sheetState,
            containerColor = colors.surfaceCard,
            contentColor = colors.inkPrimary,
            modifier = Modifier.testTag("historical_day_bottom_sheet")
        ) {
            HistoricalDayEditorSheetContent(
                cell = selectedDayCell,
                onDismiss = { selectedGridDateIso = null },
                onUpdatePrayer = { prayerItem, isPrayed, isInJamaah, status ->
                    onUpdateHistoricalPrayer(
                        selectedDayCell.dateIso,
                        prayerItem.prayerId,
                        prayerItem.nameEnglish,
                        isPrayed,
                        isInJamaah,
                        status
                    )
                }
            )
        }
    }
}

@Composable
private fun EditorialKpiColumn(
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = NoorTheme.colors
    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        AutoSizeSingleLineText(
            text = value,
            style = MaterialTheme.typography.headlineMedium.withTabularDigits(),
            maxFontSize = 22.sp,
            minFontSize = 15.sp,
            color = accentColor,
            fontWeight = FontWeight.Medium
        )
        AutoSizeSingleLineText(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = IbmPlexMonoFontFamily,
                letterSpacing = 0.7.sp
            ),
            maxFontSize = 9.5.sp,
            minFontSize = 7.5.sp,
            color = colors.inkTertiary
        )
    }
}

@Composable
private fun VerticalHairline() {
    Box(
        modifier = Modifier
            .width(0.5.dp)
            .height(32.dp)
            .background(NoorTheme.colors.hairline)
    )
}

@Composable
private fun QazaBacklogInsetGroup(
    totalQazaBacklogCount: Int,
    autoDetectedQazaCount: Int,
    pendingQazaPrayerNames: List<String>,
    onAdjustQazaBacklog: (Int) -> Unit
) {
    val colors = NoorTheme.colors
    val hasPending = totalQazaBacklogCount > 0

    SwissInsetGroup(
        borderColor = if (hasPending) colors.accentCoral.copy(alpha = 0.4f) else colors.hairline,
        modifier = Modifier.testTag("qaza_backlog_counter_card")
    ) {
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
                    text = if (hasPending) "Pending Make-Up Prayers" else "No Pending Qaza",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.inkPrimary,
                    fontWeight = FontWeight.Medium
                )
                val subtitleText = when {
                    pendingQazaPrayerNames.isNotEmpty() ->
                        "Today: ${pendingQazaPrayerNames.joinToString(", ")}"
                    autoDetectedQazaCount > 0 ->
                        "$autoDetectedQazaCount auto-detected + manual backlog"
                    hasPending ->
                        "Historical missed prayers queued for make-up"
                    else ->
                        "Alhamdulillah — all prayers accounted for"
                }
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasPending) colors.accentCoral else colors.inkSecondary,
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "$totalQazaBacklogCount",
                style = MaterialTheme.typography.headlineMedium.asMonoPrecision(),
                color = if (hasPending) colors.accentCoral else colors.accentEmerald,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("qaza_backlog_count_text")
            )
        }

        AppleHairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onAdjustQazaBacklog(-1) },
                enabled = totalQazaBacklogCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.inkPrimary,
                    contentColor = colors.surfaceCard,
                    disabledContainerColor = colors.surfaceNested,
                    disabledContentColor = colors.inkTertiary
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                modifier = Modifier
                    .weight(1.35f)
                    .height(40.dp)
                    .testTag("qaza_backlog_minus_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Log 1 Qaza Prayed",
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                AutoSizeSingleLineText(
                    text = "Prayed 1 Qaza",
                    style = MaterialTheme.typography.labelLarge,
                    maxFontSize = 12.5.sp,
                    minFontSize = 10.sp,
                    color = if (totalQazaBacklogCount > 0) colors.surfaceCard else colors.inkTertiary
                )
            }

            OutlinedButton(
                onClick = { onAdjustQazaBacklog(1) },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.5.dp, colors.hairlineStrong),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("qaza_backlog_plus_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add 1 Missed Qaza to Backlog",
                    tint = colors.inkPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                AutoSizeSingleLineText(
                    text = "Add 1",
                    style = MaterialTheme.typography.labelLarge,
                    maxFontSize = 12.5.sp,
                    minFontSize = 10.sp,
                    color = colors.inkPrimary
                )
            }
        }
    }
}

@Composable
private fun HistoricalDayEditorSheetContent(
    cell: DayContributionCell,
    onDismiss: () -> Unit,
    onUpdatePrayer: (HistoricalDayPrayerUiModel, Boolean, Boolean, PrayerCompletionStatus) -> Unit
) {
    val colors = NoorTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (cell.isToday) "Today · ${cell.dateIso}" else cell.dateIso,
                    style = MaterialTheme.typography.titleLarge.withTabularDigits(),
                    color = colors.inkPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${cell.prayersCompleted}/5 Fardh prayers completed",
                    style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                    color = colors.inkSecondary
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_historical_day_sheet_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close day log",
                    tint = colors.inkTertiary
                )
            }
        }

        AppleHairlineDivider()

        // Hairline-divided list (no nested cards inside modal sheet)
        Column(modifier = Modifier.fillMaxWidth()) {
            cell.prayerStatuses.forEachIndexed { idx, item ->
                val isLockedUpcoming = cell.isToday && !item.canMarkPrayed && !item.isPrayed
                val isJamaah = item.isPrayed && item.isInJamaah && item.completionStatus == PrayerCompletionStatus.PRAYED_ON_TIME
                val isIndividual = item.isPrayed && !item.isInJamaah && item.completionStatus == PrayerCompletionStatus.PRAYED_ON_TIME
                val isQazaMakeup = item.isPrayed && item.completionStatus == PrayerCompletionStatus.PRAYED_LATE
                val isMissed = !item.isPrayed && !isLockedUpcoming

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp)
                        .testTag("sheet_prayer_row_${item.prayerId.lowercase()}"),
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
                                text = item.nameEnglish,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isLockedUpcoming) colors.inkSecondary else colors.inkPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = item.nameArabic,
                                style = arabicCalligraphyStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = if (isLockedUpcoming) colors.inkTertiary else colors.accentGold
                            )
                        }

                        val statusBadgeText = when {
                            isLockedUpcoming -> "Starts ${item.pstFormatted}"
                            isJamaah -> "Jama'ah"
                            isIndividual -> "Individual"
                            isQazaMakeup -> "Qaza Done"
                            else -> "Missed"
                        }
                        val statusBadgeColor = when {
                            isLockedUpcoming -> colors.inkTertiary
                            isJamaah -> colors.accentEmerald
                            isIndividual -> colors.accentGold
                            isQazaMakeup -> colors.accentGoldBright
                            else -> colors.accentCoral
                        }

                        Text(
                            text = statusBadgeText,
                            style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                            color = statusBadgeColor
                        )
                    }

                    if (isLockedUpcoming) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sheet_locked_upcoming_${item.prayerId.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Locked until Prayer Start Time",
                                tint = colors.inkTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Unlocks at ${item.pstFormatted}",
                                style = MaterialTheme.typography.bodySmall.withTabularDigits(),
                                color = colors.inkTertiary
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.segmentedTrack)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            HistoricalStatusOption(
                                label = "Jama'ah",
                                selected = isJamaah,
                                activeColor = colors.accentEmerald,
                                onClick = { onUpdatePrayer(item, true, true, PrayerCompletionStatus.PRAYED_ON_TIME) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sheet_chip_jamaah_${item.prayerId.lowercase()}")
                            )
                            HistoricalStatusOption(
                                label = "Solo",
                                selected = isIndividual,
                                activeColor = colors.accentGold,
                                onClick = { onUpdatePrayer(item, true, false, PrayerCompletionStatus.PRAYED_ON_TIME) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sheet_chip_individual_${item.prayerId.lowercase()}")
                            )
                            HistoricalStatusOption(
                                label = "Qaza",
                                selected = isQazaMakeup,
                                activeColor = colors.accentGoldBright,
                                onClick = { onUpdatePrayer(item, true, false, PrayerCompletionStatus.PRAYED_LATE) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sheet_chip_qaza_done_${item.prayerId.lowercase()}")
                            )
                            HistoricalStatusOption(
                                label = "Missed",
                                selected = isMissed,
                                activeColor = colors.accentCoral,
                                onClick = { onUpdatePrayer(item, false, false, PrayerCompletionStatus.MISSED_QADA) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sheet_chip_missed_${item.prayerId.lowercase()}")
                            )
                        }
                    }
                }
                if (idx < cell.prayerStatuses.lastIndex) {
                    AppleHairlineDivider()
                }
            }
        }
    }
}

@Composable
private fun HistoricalStatusOption(
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
            .padding(vertical = 6.dp, horizontal = 4.dp),
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
private fun GitHubStyleContributionGrid(
    cells: List<DayContributionCell>,
    onDayCellClick: (DayContributionCell) -> Unit
) {
    val colors = NoorTheme.colors
    val weeks = cells.chunked(7)
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayLabels.forEach { d ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = d,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily, fontSize = 9.5.sp),
                        color = colors.inkTertiary
                    )
                }
            }
        }

        weeks.forEach { weekRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                weekRow.forEach { cell ->
                    val bgColor = contributionCellColor(cell.totalScore, colors)
                    val isStrongFill = cell.totalScore >= 0.65f
                    val textColor = when {
                        cell.totalScore >= 0.88f -> colors.onAccentGold
                        cell.totalScore >= 0.65f -> colors.onAccentEmerald
                        else -> colors.inkSecondary
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(bgColor)
                            .then(
                                if (cell.isToday) {
                                    Modifier.border(1.25.dp, colors.inkPrimary, RoundedCornerShape(4.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onDayCellClick(cell) }
                            .testTag("grid_day_cell_${cell.dateIso}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${cell.prayersCompleted}",
                            style = MaterialTheme.typography.labelSmall.asMonoPrecision(),
                            fontSize = 10.sp,
                            color = textColor,
                            fontWeight = if (isStrongFill || cell.isToday) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

private fun contributionCellColor(score: Float, colors: NoorThemePalette): Color {
    return when {
        score >= 0.88f -> colors.accentGold
        score >= 0.65f -> colors.accentEmerald
        score >= 0.40f -> colors.accentGold.copy(alpha = if (colors.isDark) 0.34f else 0.24f)
        else -> colors.surfaceNested
    }
}

@Composable
private fun SwissAthkaarRowItem(
    habit: AthkaarHabitUiModel,
    onStepCounter: () -> Unit,
    onToggleComplete: () -> Unit,
    isCompact: Boolean
) {
    val colors = NoorTheme.colors
    val haptic = LocalHapticFeedback.current
    val progress = (habit.currentCount.toFloat() / habit.targetCount.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val willComplete = !habit.isCompleted && (habit.currentCount + 11 >= habit.targetCount)
                haptic.performHapticFeedback(
                    if (willComplete) HapticFeedbackType.LongPress
                    else HapticFeedbackType.TextHandleMove
                )
                onStepCounter()
            }
            .testTag("athkaar_card_${habit.id.lowercase()}")
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.inkPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = habit.arabicSubtitle,
                        style = arabicCalligraphyStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = colors.accentGold
                    )
                }

                if (habit.isBedtimeTracker) {
                    val sleepNote = if (habit.completedTimeFormatted != null) {
                        stringResource(R.string.bedtime_logged_format, habit.completedTimeFormatted)
                    } else {
                        stringResource(R.string.bedtime_not_logged)
                    }
                    Text(
                        text = sleepNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (habit.isCompleted) colors.accentEmerald else colors.inkSecondary
                    )
                } else {
                    Text(
                        text = habit.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary,
                        maxLines = 3
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${habit.currentCount}/${habit.targetCount}",
                    style = MaterialTheme.typography.labelMedium.asMonoPrecision(),
                    color = if (habit.isCompleted) colors.accentEmerald else colors.inkSecondary
                )

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleComplete()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("toggle_athkaar_${habit.id.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (habit.isCompleted) colors.accentEmerald else Color.Transparent)
                            .then(
                                if (!habit.isCompleted) {
                                    Modifier.border(1.25.dp, colors.hairlineStrong, CircleShape)
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (habit.isCompleted) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Toggle ${habit.title}",
                                tint = colors.onAccentEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Whisper-thin 1.5dp progress hairline along bottom of Athkaar row
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(colors.hairline)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(if (habit.isCompleted) colors.accentEmerald else colors.accentGold)
                )
            }
        }
    }
}
