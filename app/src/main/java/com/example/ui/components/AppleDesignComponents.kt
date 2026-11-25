package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
import com.example.R
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.NoorTheme
import com.example.ui.theme.withTabularDigits
import com.example.viewmodel.ActiveTab

/**
 * Critically damped tactile press feedback (zero toy-like bounce).
 */
fun Modifier.appleSpringClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.985f,
    shape: Shape? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "swiss_press_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(if (shape != null) Modifier.clip(shape) else Modifier)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Razor-sharp 0.5dp Swiss hairline divider.
 */
@Composable
fun AppleHairlineDivider(
    modifier: Modifier = Modifier,
    color: Color = NoorTheme.colors.hairline
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(color)
    )
}

/**
 * Quiet uppercase Swiss editorial section header sitting directly on the canvas.
 */
@Composable
fun SwissSectionLabel(
    title: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    trailingColor: Color = NoorTheme.colors.inkTertiary
) {
    val colors = NoorTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = IbmPlexMonoFontFamily,
                fontSize = 10.5.sp,
                letterSpacing = 1.1.sp,
                fontWeight = FontWeight.Medium
            ),
            color = colors.inkTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelSmall.withTabularDigits().copy(
                    fontFamily = IbmPlexMonoFontFamily,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.2.sp
                ),
                color = trailingColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Single flat grouped inset container with a crisp 0.5dp hairline outline and zero drop shadow.
 */
@Composable
fun SwissInsetGroup(
    modifier: Modifier = Modifier,
    borderColor: Color = NoorTheme.colors.hairline,
    containerColor: Color = NoorTheme.colors.surfaceCard,
    cornerRadius: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(0.5.dp, borderColor),
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Crisp Swiss segmented control with critically damped sliding thumb and zero drop shadow.
 */
@Composable
fun <T> AppleSegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionTagPrefix: String = "segmented_opt"
) {
    val colors = NoorTheme.colors
    val haptics = LocalHapticFeedback.current
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)

    Surface(
        color = colors.segmentedTrack,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, colors.hairline),
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp)
        ) {
            val segmentWidth = maxWidth / options.size.coerceAtLeast(1)
            val thumbOffset by animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "swiss_segmented_thumb"
            )

            Surface(
                color = colors.segmentedThumb,
                shape = RoundedCornerShape(7.5.dp),
                border = BorderStroke(0.5.dp, colors.hairlineStrong.copy(alpha = 0.65f)),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .width(segmentWidth)
                    .fillMaxHeight()
            ) {}

            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                options.forEachIndexed { idx, (value, label) ->
                    val isSelected = idx == selectedIndex
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) colors.inkPrimary else colors.inkSecondary,
                        animationSpec = tween(160),
                        label = "seg_text_color"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(7.5.dp))
                            .clickable {
                                if (!isSelected) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(value)
                                }
                            }
                            .testTag("${optionTagPrefix}_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.5.sp,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private data class DockTabSpec(
    val tab: ActiveTab,
    val label: String,
    val icon: ImageVector,
    val testTag: String,
    val badgeCount: Int = 0,
    val badgeTestTag: String? = null
)

/**
 * Editorial Swiss Minimal floating pill navigation bar.
 * Flat hairline border, monochrome ink contrast, refined gold indicator bar, and zero bounce.
 */
@Composable
fun AppleFloatingGlassDock(
    activeTab: ActiveTab,
    prayersQazaBadgeCount: Int,
    analyticsQazaBadgeCount: Int,
    onSelectTab: (ActiveTab) -> Unit,
    modifier: Modifier = Modifier,
    applyOuterInsetsAndPadding: Boolean = true
) {
    val colors = NoorTheme.colors
    val haptics = LocalHapticFeedback.current

    val items = listOf(
        DockTabSpec(
            tab = ActiveTab.NOTIFICATION_LOCK,
            label = stringResource(R.string.nav_notification),
            icon = Icons.Rounded.NotificationsActive,
            testTag = "nav_tab_notification"
        ),
        DockTabSpec(
            tab = ActiveTab.HOME_PRAYERS,
            label = stringResource(R.string.nav_home),
            icon = Icons.Rounded.Mosque,
            testTag = "nav_tab_home",
            badgeCount = prayersQazaBadgeCount,
            badgeTestTag = "nav_badge_prayers_qaza"
        ),
        DockTabSpec(
            tab = ActiveTab.ANALYTICS_HABITS,
            label = stringResource(R.string.nav_analytics),
            icon = Icons.Rounded.AutoGraph,
            testTag = "nav_tab_analytics",
            badgeCount = analyticsQazaBadgeCount,
            badgeTestTag = "nav_badge_analytics_qaza"
        ),
        DockTabSpec(
            tab = ActiveTab.ARCHITECTURE_BLUEPRINT,
            label = stringResource(R.string.nav_architecture),
            icon = Icons.Rounded.Tune,
            testTag = "nav_tab_architecture"
        )
    )

    val selectedIndex = items.indexOfFirst { it.tab == activeTab }.coerceAtLeast(0)

    val outerModifier = if (applyOuterInsetsAndPadding) {
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    } else {
        modifier.fillMaxWidth()
    }

    Box(
        modifier = outerModifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = colors.dockGlassSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(0.5.dp, colors.dockGlassBorder),
            shadowElevation = if (colors.isDark) 0.dp else 2.dp,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .testTag("bottom_navigation_bar")
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                val itemWidth = maxWidth / items.size
                val indicatorOffset by animateDpAsState(
                    targetValue = itemWidth * selectedIndex + (itemWidth - 20.dp) / 2f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "dock_hairline_indicator"
                )

                // Whisper-thin gold editorial accent line at bottom of active tab
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = indicatorOffset)
                        .width(20.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(colors.accentGold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { spec ->
                        val isSelected = spec.tab == activeTab
                        val contentTint by animateColorAsState(
                            targetValue = if (isSelected) colors.inkPrimary else colors.inkTertiary,
                            animationSpec = tween(160),
                            label = "dock_content_tint"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectTab(spec.tab)
                                }
                                .testTag(spec.testTag),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            BadgedBox(
                                badge = {
                                    if (spec.badgeCount > 0) {
                                        Badge(
                                            containerColor = colors.accentCoral,
                                            contentColor = Color.White,
                                            modifier = Modifier.then(
                                                if (spec.badgeTestTag != null) Modifier.testTag(spec.badgeTestTag) else Modifier
                                            )
                                        ) {
                                            Text(
                                                text = "${spec.badgeCount}",
                                                style = MaterialTheme.typography.labelSmall.withTabularDigits(),
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = spec.icon,
                                    contentDescription = spec.label,
                                    tint = contentTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = spec.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 10.sp,
                                color = contentTint,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                letterSpacing = 0.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Priority-ordered state for the unified Dynamic Island-style floating squircle
 * hovering directly above the Prayers (Tab 2) and Analytics (Tab 3) dock buttons.
 */
sealed interface BottomSquircleAlertState {
    data object Hidden : BottomSquircleAlertState
    data class UndoUncheck(
        val prayerName: String,
        val secondsRemaining: Int
    ) : BottomSquircleAlertState

    data class StatusNotice(
        val message: String,
        val isWarning: Boolean
    ) : BottomSquircleAlertState

    data class QazaPending(
        val totalCount: Int,
        val pendingNames: List<String>
    ) : BottomSquircleAlertState
}

/**
 * Dynamic Island-style floating squircle anchored directly above the center two dock buttons
 * (Prayers -> Analytics). Morphs smoothly in place between Undo, Locked Prayer / Status alerts,
 * and persistent Qaza Pending reminders without shifting top page layout.
 */
@Composable
fun BottomSquircleAlertIsland(
    state: BottomSquircleAlertState,
    onLogOneQaza: () -> Unit,
    onUndoUncheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NoorTheme.colors
    val haptics = LocalHapticFeedback.current
    val isVisible = state !is BottomSquircleAlertState.Hidden

    val borderColor by animateColorAsState(
        targetValue = when (state) {
            is BottomSquircleAlertState.UndoUncheck -> colors.accentGold.copy(alpha = 0.65f)
            is BottomSquircleAlertState.QazaPending -> colors.accentCoral.copy(alpha = 0.55f)
            is BottomSquircleAlertState.StatusNotice -> if (state.isWarning) {
                colors.accentCoral.copy(alpha = 0.55f)
            } else {
                colors.hairlineStrong
            }
            BottomSquircleAlertState.Hidden -> colors.hairline
        },
        animationSpec = tween(220),
        label = "squircle_border_color"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) { fullHeight -> fullHeight / 2 } + fadeIn(tween(180)) + scaleIn(
            initialScale = 0.94f,
            animationSpec = tween(200)
        ),
        exit = slideOutVertically(
            animationSpec = tween(170)
        ) { fullHeight -> fullHeight / 2 } + fadeOut(tween(150)) + scaleOut(
            targetScale = 0.94f,
            animationSpec = tween(160)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Exact middle 2 tabs (Prayers -> Analytics) span 50% of the 4-tab dock width.
            // At rest (Qaza), the squircle spans closely across Tabs 2 & 3; when morphed for
            // longer notices (e.g. locked prayer messages), it expands smoothly in place
            // while staying centered above Tabs 2 & 3.
            val dockWidth = maxWidth
            val targetWidth = when (state) {
                is BottomSquircleAlertState.QazaPending -> {
                    (dockWidth * 0.56f).coerceIn(186.dp, 240.dp)
                }
                is BottomSquircleAlertState.UndoUncheck -> {
                    (dockWidth * 0.72f).coerceIn(224.dp, 316.dp)
                }
                is BottomSquircleAlertState.StatusNotice -> {
                    if (state.message.length > 34) {
                        (dockWidth * 0.86f).coerceIn(252.dp, 368.dp)
                    } else {
                        (dockWidth * 0.68f).coerceIn(216.dp, 300.dp)
                    }
                }
                BottomSquircleAlertState.Hidden -> (dockWidth * 0.54f).coerceIn(180.dp, 232.dp)
            }

            val animatedWidth by animateDpAsState(
                targetValue = targetWidth,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "squircle_island_width"
            )

            Surface(
                color = colors.dockGlassSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(0.75.dp, borderColor),
                shadowElevation = if (colors.isDark) 0.dp else 3.dp,
                modifier = Modifier
                    .width(animatedWidth)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                    .testTag("bottom_alert_squircle")
            ) {
                AnimatedContent(
                    targetState = state,
                    contentKey = { target ->
                        when (target) {
                            is BottomSquircleAlertState.UndoUncheck -> "undo_${target.prayerName}"
                            is BottomSquircleAlertState.StatusNotice -> "status_${target.message}"
                            is BottomSquircleAlertState.QazaPending -> "qaza"
                            BottomSquircleAlertState.Hidden -> "hidden"
                        }
                    },
                    transitionSpec = {
                        (fadeIn(tween(180)) + scaleIn(initialScale = 0.96f, animationSpec = tween(180)))
                            .togetherWith(fadeOut(tween(130)) + scaleOut(targetScale = 0.96f, animationSpec = tween(130)))
                    },
                    label = "squircle_dynamic_island_morph"
                ) { activeState ->
                    when (activeState) {
                        is BottomSquircleAlertState.UndoUncheck -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 13.dp, end = 8.dp, top = 7.dp, bottom = 7.dp)
                                    .testTag("undo_uncheck_snackbar"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Undo,
                                        contentDescription = "Undo uncheck",
                                        tint = colors.accentGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "${activeState.prayerName} unmarked · ${activeState.secondsRemaining}s",
                                        style = MaterialTheme.typography.labelMedium.withTabularDigits(),
                                        color = colors.inkPrimary,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(
                                    color = colors.accentGoldWash,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .appleSpringClickable(shape = RoundedCornerShape(10.dp)) {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onUndoUncheck()
                                        }
                                        .testTag("undo_uncheck_action_button")
                                ) {
                                    Text(
                                        text = "Undo",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontSize = 12.sp,
                                        color = colors.accentGold,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        is BottomSquircleAlertState.StatusNotice -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("transient_status_banner"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (activeState.isWarning) colors.accentCoral else colors.accentGold
                                        )
                                )
                                Text(
                                    text = activeState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp,
                                    color = colors.inkPrimary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        is BottomSquircleAlertState.QazaPending -> {
                            val subText = if (activeState.pendingNames.isNotEmpty()) {
                                activeState.pendingNames.joinToString(", ")
                            } else {
                                "Make-up pending"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                                    .testTag("persistent_qaza_top_banner"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.5.dp)
                                            .clip(CircleShape)
                                            .background(colors.accentCoral)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                        Text(
                                            text = "Qaza · ${activeState.totalCount}",
                                            style = MaterialTheme.typography.labelLarge.withTabularDigits(),
                                            fontSize = 12.sp,
                                            color = colors.accentCoral,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = subText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = colors.inkSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(
                                    color = colors.accentEmeraldWash,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .appleSpringClickable(shape = RoundedCornerShape(10.dp)) {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onLogOneQaza()
                                        }
                                        .testTag("qaza_top_banner_log_one_button")
                                ) {
                                    Text(
                                        text = "Log 1",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontSize = 11.5.sp,
                                        color = colors.accentEmerald,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        BottomSquircleAlertState.Hidden -> {
                            Spacer(modifier = Modifier.height(1.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calm multi-layered animated solar wave canvas drawn behind the Wave Hero Card.
 * Adapts smoothly to both Apple White Light and true OLED Dark themes.
 */
@Composable
fun HeroWaveBackdropCanvas(
    accentColor: Color,
    secondaryColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_wave_motion")
    val primaryPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hero_wave_primary_phase"
    )
    val secondaryPhase by infiniteTransition.animateFloat(
        initialValue = (2.0 * PI).toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hero_wave_secondary_phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Soft vertical atmospheric tint
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (isDark) 0.08f else 0.06f),
                    Color.Transparent,
                    accentColor.copy(alpha = if (isDark) 0.10f else 0.07f)
                )
            )
        )

        fun buildWavePath(
            baseYFraction: Float,
            amplitudeFraction: Float,
            frequency: Float,
            phase: Float,
            closeBottom: Boolean
        ): Path {
            val path = Path()
            val baseY = h * baseYFraction
            val amp = h * amplitudeFraction
            val steps = 42
            for (i in 0..steps) {
                val x = w * (i.toFloat() / steps.toFloat())
                val normX = i.toFloat() / steps.toFloat()
                val y = baseY + sin(normX * frequency * 2f * PI.toFloat() + phase) * amp
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            if (closeBottom) {
                path.lineTo(w, h)
                path.lineTo(0f, h)
                path.close()
            }
            return path
        }

        // Back translucent filled wave
        val backWave = buildWavePath(
            baseYFraction = 0.64f,
            amplitudeFraction = 0.065f,
            frequency = 1.15f,
            phase = secondaryPhase,
            closeBottom = true
        )
        drawPath(
            path = backWave,
            brush = Brush.verticalGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = if (isDark) 0.13f else 0.10f),
                    Color.Transparent
                ),
                startY = h * 0.52f,
                endY = h
            )
        )

        // Front translucent filled wave
        val frontWave = buildWavePath(
            baseYFraction = 0.74f,
            amplitudeFraction = 0.055f,
            frequency = 1.45f,
            phase = primaryPhase,
            closeBottom = true
        )
        drawPath(
            path = frontWave,
            brush = Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (isDark) 0.16f else 0.12f),
                    accentColor.copy(alpha = if (isDark) 0.03f else 0.02f)
                ),
                startY = h * 0.64f,
                endY = h
            )
        )

        // Subtle crisp wave crest hairlines
        val crestOne = buildWavePath(
            baseYFraction = 0.64f,
            amplitudeFraction = 0.065f,
            frequency = 1.15f,
            phase = secondaryPhase,
            closeBottom = false
        )
        drawPath(
            path = crestOne,
            color = secondaryColor.copy(alpha = if (isDark) 0.24f else 0.18f),
            style = Stroke(width = 1.dp.toPx())
        )

        val crestTwo = buildWavePath(
            baseYFraction = 0.74f,
            amplitudeFraction = 0.055f,
            frequency = 1.45f,
            phase = primaryPhase,
            closeBottom = false
        )
        drawPath(
            path = crestTwo,
            color = accentColor.copy(alpha = if (isDark) 0.30f else 0.22f),
            style = Stroke(width = 1.2.dp.toPx())
        )
    }
}

