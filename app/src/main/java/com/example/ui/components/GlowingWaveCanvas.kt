package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElegantGold
import com.example.ui.theme.EmeraldCompleted
import com.example.ui.theme.RadiantAmberGlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders a clean, subtle glowing animated harmonic wave pattern along the lower portion
 * of the current prayer hero card without any distracting circles or clutter in the top right.
 */
@Composable
fun GlowingWaveCanvas(
    modifier: Modifier = Modifier,
    primaryWaveColor: Color = ElegantGold,
    secondaryWaveColor: Color = RadiantAmberGlow
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glowing_wave_transition")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val secondaryPhase by infiniteTransition.animateFloat(
        initialValue = (2f * PI).toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "secondary_wave_phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // Harmonic Wave 1 (Filled gradient glow + luminous gold crest along the bottom)
        val wave1Path = Path()
        val wave1FillPath = Path()
        val baseline1 = height * 0.76f
        val amplitude1 = height * 0.08f
        val steps = 64

        for (i in 0..steps) {
            val fraction = i.toFloat() / steps
            val x = fraction * width
            val y = baseline1 +
                sin(fraction * 2.2f * PI + phaseShift).toFloat() * amplitude1 +
                cos(fraction * 1.1f * PI - phaseShift * 0.5f).toFloat() * (amplitude1 * 0.35f)

            if (i == 0) {
                wave1Path.moveTo(x, y)
                wave1FillPath.moveTo(x, height)
                wave1FillPath.lineTo(x, y)
            } else {
                wave1Path.lineTo(x, y)
                wave1FillPath.lineTo(x, y)
            }
        }
        wave1FillPath.lineTo(width, height)
        wave1FillPath.close()

        drawPath(
            path = wave1FillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryWaveColor.copy(alpha = 0.20f),
                    primaryWaveColor.copy(alpha = 0.02f)
                ),
                startY = baseline1 - amplitude1,
                endY = height
            )
        )

        // Outer soft glow stroke
        drawPath(
            path = wave1Path,
            color = secondaryWaveColor.copy(alpha = 0.30f),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        // Crisp inner gold crest stroke
        drawPath(
            path = wave1Path,
            color = primaryWaveColor.copy(alpha = 0.75f),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Harmonic Wave 2 (Secondary subtle counterpoint wave)
        val wave2Path = Path()
        val baseline2 = height * 0.83f
        val amplitude2 = height * 0.06f
        for (i in 0..steps) {
            val fraction = i.toFloat() / steps
            val x = fraction * width
            val y = baseline2 +
                sin(fraction * 2.8f * PI + secondaryPhase).toFloat() * amplitude2
            if (i == 0) {
                wave2Path.moveTo(x, y)
            } else {
                wave2Path.lineTo(x, y)
            }
        }

        drawPath(
            path = wave2Path,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    primaryWaveColor.copy(alpha = 0.12f),
                    EmeraldCompleted.copy(alpha = 0.35f),
                    primaryWaveColor.copy(alpha = 0.20f)
                )
            ),
            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
