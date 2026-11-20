package com.mehmetbozkurt.clink.game.spinthebottle

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
