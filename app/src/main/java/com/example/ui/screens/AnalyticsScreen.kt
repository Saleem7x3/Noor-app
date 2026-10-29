package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AthkaarHabitUiModel
import com.example.data.DayContributionCell
import com.example.ui.theme.CrispOffWhite
import com.example.ui.theme.DeepSlateBlue
import com.example.ui.theme.DeepSlateDarker
import com.example.ui.theme.ElegantGold
import com.example.ui.theme.ElevatedSlateCard
import com.example.ui.theme.ElevatedSlateSurface
import com.example.ui.theme.EmeraldCompleted
import com.example.ui.theme.GoldContainerDark
import com.example.ui.theme.MutedPeriwinkle
import com.example.ui.theme.RadiantAmberGlow
import com.example.ui.theme.SlateStrokeBorder
import com.example.viewmodel.PrayerDashboardUiState

/**
 * Screen 3: Analytics & Tracking Dashboard
 * Features:
 * - Circular progress ring for today's goals (5 Daily Prayers + 3 Daily Athkaar)
 * - Simple streak statistics (Day Streak, Jama'ah Rate, Consistency Score)
 * - Weekly completion grid (GitHub-style contribution graph across 7 weeks)
 * - Interactive Morning Athkaar, Evening Athkaar, and Bedtime Athkaar (indirectly tracking sleep/bedtime)
 */
@Composable
fun AnalyticsScreenContent(
    uiState: PrayerDashboardUiState,
    onStepAthkaar: (AthkaarHabitUiModel) -> Unit,
    onToggleAthkaarComplete: (AthkaarHabitUiModel) -> Unit,
    modifier: Modifier = Modifier,
    isCompactShowcase: Boolean = false
) {
    val animatedRingProgress by animateFloatAsState(
        targetValue = uiState.todayGoalFraction,
        animationSpec = tween(durationMillis = 600),
        label = "today_goals_ring"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("screen_3_analytics_dashboard"),
        verticalArrangement = Arrangement.spacedBy(if (isCompactShowcase) 12.dp else 16.dp)
    ) {
        // Top Card: Circular Progress Ring for Today's Goals + Streak Statistics
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("todays_goals_ring_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSlateSurface),
            border = BorderStroke(1.dp, ElegantGold.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isCompactShowcase) 14.dp else 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Circular Progress Ring
                    Box(
                        modifier = Modifier.size(if (isCompactShowcase) 92.dp else 104.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(if (isCompactShowcase) 84.dp else 94.dp)) {
                            val strokeW = 9.dp.toPx()
                            drawArc(
                                color = DeepSlateDarker,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        ElegantGold,
                                        RadiantAmberGlow,
                                        EmeraldCompleted,
                                        ElegantGold
                                    )
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * animatedRingProgress.coerceIn(0.05f, 1f),
                                useCenter = false,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(animatedRingProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                color = CrispOffWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.todayPrayersCompleted + uiState.todayAthkaarCompleted}/8",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElegantGold
                            )
                        }
                    }

                    // Goal Breakdown & Summary
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.todays_goals_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = CrispOffWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Fardh Prayers: ${uiState.todayPrayersCompleted}/5 completed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldCompleted
                        )
                        Text(
                            text = "Daily Athkaar: ${uiState.todayAthkaarCompleted}/3 completed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElegantGold
                        )
                    }
                }

                // Simple Streak Statistics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StreakStatPill(
                        icon = Icons.Filled.LocalFireDepartment,
                        value = "${uiState.currentStreakDays}d",
                        label = stringResource(R.string.streak_days_label),
                        accentColor = ElegantGold,
                        modifier = Modifier.weight(1f)
                    )
                    StreakStatPill(
                        icon = Icons.Filled.Mosque,
                        value = "${uiState.jamaahRatePercent}%",
                        label = stringResource(R.string.jamaah_rate_label),
                        accentColor = EmeraldCompleted,
                        modifier = Modifier.weight(1f)
                    )
                    StreakStatPill(
                        icon = Icons.Filled.Verified,
                        value = "${uiState.consistencyPercent}%",
                        label = stringResource(R.string.ontime_score_label),
                        accentColor = RadiantAmberGlow,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Weekly Completion Grid Card (GitHub-style contribution graph)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("weekly_contribution_grid_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSlateSurface),
            border = BorderStroke(1.dp, SlateStrokeBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isCompactShowcase) 14.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.weekly_grid_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = CrispOffWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Less", style = MaterialTheme.typography.labelSmall, color = MutedPeriwinkle)
                        listOf(0.15f, 0.45f, 0.75f, 1.0f).forEach { level ->
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(contributionCellColor(level))
                            )
                        }
                        Text("Full", style = MaterialTheme.typography.labelSmall, color = ElegantGold)
                    }
                }

                GitHubStyleContributionGrid(cells = uiState.weeklyGrid)
            }
        }

        // Athkaar Section: Morning, Evening, and Bedtime Athkaar (Indirect Bedtime Tracker)
        Text(
            text = stringResource(R.string.athkaar_section_title),
            style = MaterialTheme.typography.titleMedium,
            color = CrispOffWhite,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.athkaarList.forEach { habit ->
                AthkaarHabitCard(
                    habit = habit,
                    onStepCounter = { onStepAthkaar(habit) },
                    onToggleComplete = { onToggleAthkaarComplete(habit) },
                    isCompact = isCompactShowcase
                )
            }
        }
    }
}

@Composable
private fun StreakStatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DeepSlateDarker,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SlateStrokeBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = CrispOffWhite,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MutedPeriwinkle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GitHubStyleContributionGrid(cells: List<DayContributionCell>) {
    val weeks = cells.chunked(7)
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Day of week header row
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
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedPeriwinkle
                    )
                }
            }
        }

        // 7 weeks of 7 days matrix
        weeks.forEach { weekRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                weekRow.forEach { cell ->
                    val bgColor = contributionCellColor(cell.totalScore)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(bgColor)
                            .then(
                                if (cell.isToday) {
                                    Modifier.background(
                                        Brush.linearGradient(
                                            listOf(ElegantGold, EmeraldCompleted)
                                        )
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${cell.prayersCompleted}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (cell.totalScore >= 0.7f || cell.isToday) DeepSlateBlue else CrispOffWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun contributionCellColor(score: Float): Color {
    return when {
        score >= 0.88f -> ElegantGold
        score >= 0.65f -> EmeraldCompleted.copy(alpha = 0.85f)
        score >= 0.40f -> ElegantGold.copy(alpha = 0.45f)
        else -> DeepSlateDarker
    }
}

@Composable
private fun AthkaarHabitCard(
    habit: AthkaarHabitUiModel,
    onStepCounter: () -> Unit,
    onToggleComplete: () -> Unit,
    isCompact: Boolean
) {
    val icon = when (habit.id) {
        "MORNING" -> Icons.Outlined.WbSunny
        "EVENING" -> Icons.Outlined.WbTwilight
        else -> Icons.Filled.Bedtime
    }
    val progress = (habit.currentCount.toFloat() / habit.targetCount.toFloat()).coerceIn(0f, 1f)

    Surface(
        color = ElevatedSlateSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (habit.isCompleted) EmeraldCompleted.copy(alpha = 0.55f) else SlateStrokeBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onStepCounter)
            .testTag("athkaar_card_${habit.id.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = if (isCompact) 10.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (habit.isBedtimeTracker) GoldContainerDark else DeepSlateDarker),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = habit.title,
                            tint = if (habit.isCompleted) EmeraldCompleted else ElegantGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = habit.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = CrispOffWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = habit.arabicSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ElegantGold
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
                                style = MaterialTheme.typography.labelSmall,
                                color = if (habit.isCompleted) EmeraldCompleted else RadiantAmberGlow
                            )
                        } else {
                            Text(
                                text = habit.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedPeriwinkle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Counter Badge + Toggle Complete Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = DeepSlateDarker,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SlateStrokeBorder)
                    ) {
                        Text(
                            text = "${habit.currentCount}/${habit.targetCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (habit.isCompleted) EmeraldCompleted else ElegantGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleComplete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("toggle_athkaar_${habit.id.lowercase()}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (habit.isCompleted) EmeraldCompleted else ElevatedSlateCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Toggle ${habit.title}",
                                tint = if (habit.isCompleted) DeepSlateBlue else MutedPeriwinkle,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (habit.isCompleted) EmeraldCompleted else ElegantGold,
                trackColor = DeepSlateDarker
            )
        }
    }
}
