package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
    modifier: Modifier = Modifier
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
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
