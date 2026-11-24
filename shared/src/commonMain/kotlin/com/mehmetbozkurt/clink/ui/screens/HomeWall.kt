package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import com.mehmetbozkurt.clink.ui.theme.ClinkFonts

/**
 * The home screen's wall art (spec 2026-11-24-home-and-settings §4, sketch W3), drawn behind everything: faint spray
 * tags, a crown and a star in the gaps, and — when [animated] — a soft light band that crosses the wall during the
 * last 3.5 s of every 12 s, like a passing car's headlight. The tags are wall art, the same in every language.
 */
@Composable
fun HomeWallArt(animated: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        Tag("wow", ClinkColors.Pink, 48.sp, -10f, w * 0.56f, h * 0.10f)
        Tag("gg", ClinkColors.Paper, 40.sp, 8f, w * 0.04f, h * 0.71f)
        Tag("cheers", ClinkColors.Acid, 30.sp, -6f, w * 0.66f, h * 0.72f)
        Doodle(w * 0.86f, h * 0.20f, 34.dp) { s -> crown(s) }
        Doodle(w * 0.04f, h * 0.25f, 26.dp) { s -> star(s) }

        val sweep: State<Float> = if (animated) {
            rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(SWEEP_CYCLE_MILLIS, easing = LinearEasing), RepeatMode.Restart))
        } else {
            remember { mutableFloatStateOf(0f) }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = sweep.value
            if (!animated || t < SWEEP_START) return@Canvas
            val progress = (t - SWEEP_START) / (1f - SWEEP_START)
            val band = size.width * 0.4f
            val left = -band * 1.5f + (size.width + band * 3f) * progress
            rotate(18f, pivot = center) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, ClinkColors.Paper.copy(alpha = 0.06f), Color.Transparent),
                        startX = left,
                        endX = left + band,
                    ),
                    topLeft = Offset(left, -size.height * 0.5f),
                    size = Size(band, size.height * 2f),
                )
            }
        }
    }
}

/** 12 s per cycle; the band moves only in the last 3.5 s. */
private const val SWEEP_CYCLE_MILLIS = 12_000
private const val SWEEP_START = 8.5f / 12f

@Composable
private fun Tag(text: String, color: Color, size: TextUnit, angle: Float, x: Dp, y: Dp) {
    Text(
        text = text,
        // Wall art: screen readers skip it.
        modifier = Modifier.offset(x, y).rotate(angle).clearAndSetSemantics {},
        color = color.copy(alpha = 0.09f),
        style = TextStyle(fontFamily = ClinkFonts.spray, fontSize = size),
        maxLines = 1,
        softWrap = false,
    )
}

@Composable
private fun Doodle(x: Dp, y: Dp, size: Dp, shape: (Float) -> Pair<Path, Color>) {
    Canvas(modifier = Modifier.offset(x, y).size(size)) {
        val (path, color) = shape(this.size.minDimension)
        drawPath(path, color.copy(alpha = 0.10f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** A three-pointed crown, sprayed in acid. */
private fun crown(s: Float): Pair<Path, Color> = Path().apply {
    moveTo(0.1f * s, 0.85f * s)
    lineTo(0.2f * s, 0.25f * s)
    lineTo(0.4f * s, 0.6f * s)
    lineTo(0.5f * s, 0.12f * s)
    lineTo(0.6f * s, 0.6f * s)
    lineTo(0.8f * s, 0.25f * s)
    lineTo(0.9f * s, 0.85f * s)
    close()
} to ClinkColors.Acid

/** A five-pointed star outline in paper. */
private fun star(s: Float): Pair<Path, Color> = Path().apply {
    val points = (0 until 10).map { i ->
        val r = if (i % 2 == 0) 0.48f * s else 0.2f * s
        val a = -kotlin.math.PI / 2 + i * kotlin.math.PI / 5
        Offset(0.5f * s + r * kotlin.math.cos(a).toFloat(), 0.5f * s + r * kotlin.math.sin(a).toFloat())
    }
    moveTo(points[0].x, points[0].y)
    points.drop(1).forEach { lineTo(it.x, it.y) }
    close()
} to ClinkColors.Paper
