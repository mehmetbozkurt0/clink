package com.mehmetbozkurt.clink.game.spinthebottle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.bottle
import com.mehmetbozkurt.clink.ui.components.WallChip
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

const val MIN_FLICK_DP_PER_SECOND = 300f
const val MAX_FLICK_DP_PER_SECOND = 3000f

/** Where player [index] sits on the circle: 0° points right, clockwise, player 0 at the top. */
fun playerAngleDegrees(index: Int, count: Int): Float = -90f + index * 360f / count

/**
 * The player whose sector [angleDegrees] (in [playerAngleDegrees] terms) falls into. Each sector is centred on its
 * player and spans ±180°/[count]; an angle exactly on a border belongs to the next sector clockwise.
 */
fun sectorAt(angleDegrees: Float, count: Int): Int {
    val sector = 360f / count
    // Shift so sector 0 starts at 0°, then count whole sectors.
    return (normalized(angleDegrees + 90f + sector / 2f) / sector).toInt() % count
}

data class SpinMotion(val turns: Int, val durationMillis: Int)

/** A flick below [MIN_FLICK_DP_PER_SECOND] does nothing; faster flicks spin longer, linearly up to 8 turns and 3.2 s. */
fun flickToSpin(speedDpPerSecond: Float): SpinMotion? {
    if (speedDpPerSecond < MIN_FLICK_DP_PER_SECOND) return null
    val t = ((speedDpPerSecond - MIN_FLICK_DP_PER_SECOND) / (MAX_FLICK_DP_PER_SECOND - MIN_FLICK_DP_PER_SECOND)).coerceIn(0f, 1f)
    return SpinMotion(turns = 3 + (t * 5).roundToInt(), durationMillis = 1800 + (t * 1400).roundToInt())
}

/** The bottle's neck points up at rotation 0. Returns where it points, in [playerAngleDegrees] terms, within [0, 360). */
fun bottleAngleDegrees(rotation: Float): Float = normalized(rotation - 90f)

/** The rotation to animate to: forward from [current], [extraTurns] full turns, ending on [targetAngle]. */
fun spinEndRotation(current: Float, targetAngle: Float, extraTurns: Int): Float =
    current + normalized(targetAngle + 90f - current) + extraTurns * 360f

private fun normalized(degrees: Float) = ((degrees % 360f) + 360f) % 360f

/**
 * Players' names on a circle with the bottle in the middle. The spinner is acid, the answerer pink.
 * Flicking the bottle calls [onFlick] with the release speed in dp/s.
 */
@Composable
fun BottleCircle(
    players: List<String>,
    spinner: Int,
    answerer: Int?,
    rotation: Float,
    flickEnabled: Boolean,
    onFlick: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.aspectRatio(1f)) {
        val radiusPx = constraints.maxWidth / 2f * 0.8f
        Sectors(count = players.size, spinner = spinner, answerer = answerer, modifier = Modifier.matchParentSize())
        players.forEachIndexed { index, name ->
            val angle = playerAngleDegrees(index, players.size) * PI / 180.0
            WallChip(
                text = name,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset((cos(angle) * radiusPx).roundToInt(), (sin(angle) * radiusPx).roundToInt()) }
                    .widthIn(max = 104.dp),
                selected = index == spinner || index == answerer,
                color = if (index == spinner) ClinkColors.Acid else ClinkColors.Pink,
                // Smaller chips from 9 players on, so neighbors at the top and bottom of the circle don't touch.
                compact = players.size > 8,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.55f)
                .pointerInput(flickEnabled) {
                    if (!flickEnabled) return@pointerInput
                    val tracker = VelocityTracker()
                    detectDragGestures(
                        onDragStart = { tracker.resetTracking() },
                        onDrag = { change, _ -> tracker.addPosition(change.uptimeMillis, change.position) },
                        onDragEnd = {
                            val velocity = tracker.calculateVelocity()
                            onFlick(hypot(velocity.x, velocity.y) / density)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.bottle),
                contentDescription = null,
                modifier = Modifier.fillMaxHeight(0.9f).aspectRatio(1f / 3f).graphicsLayer { rotationZ = rotation },
            )
        }
    }
}

/**
 * One wedge per player, centred on their name: two alternating light tones (a third for the last wedge when the count
 * is odd, so no neighbours match), thin dividers and an outer ring. The spinner's wedge is tinted acid, the answerer's pink.
 */
@Composable
private fun Sectors(count: Int, spinner: Int, answerer: Int?, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f * 0.92f
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)
        val sweep = 360f / count
        val line = ClinkColors.Paper.copy(alpha = 0.22f)
        for (i in 0 until count) {
            val start = playerAngleDegrees(i, count) - sweep / 2f
            val base = when {
                count % 2 == 1 && i == count - 1 -> ClinkColors.Paper.copy(alpha = 0.07f)
                i % 2 == 0 -> ClinkColors.Paper.copy(alpha = 0.10f)
                // Paper over the wall, not wall tones: the screen behind is ClinkColors.Wall, so wall-coloured wedges would vanish.
                else -> ClinkColors.Paper.copy(alpha = 0.04f)
            }
            drawArc(base, start, sweep, useCenter = true, topLeft = topLeft, size = arcSize)
            val tint = when (i) {
                spinner -> ClinkColors.Acid.copy(alpha = 0.22f)
                answerer -> ClinkColors.Pink.copy(alpha = 0.28f)
                else -> null
            }
            if (tint != null) drawArc(tint, start, sweep, useCenter = true, topLeft = topLeft, size = arcSize)
        }
        if (count > 1) {
            for (i in 0 until count) {
                val border = (playerAngleDegrees(i, count) - sweep / 2f) * PI.toFloat() / 180f
                drawLine(line, center, center + Offset(cos(border) * radius, sin(border) * radius), strokeWidth = 1.5.dp.toPx())
            }
        }
        drawCircle(line, radius, center, style = Stroke(width = 1.5.dp.toPx()))
    }
}
