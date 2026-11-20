package com.mehmetbozkurt.clink.game.beerpong

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.tex_paper
import com.mehmetbozkurt.clink.ui.components.rememberTiledBrush
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.imageResource
import kotlin.math.hypot

private const val TABLE_HALF_WIDTH = 20f
private const val TABLE_LENGTH = 100f
private const val FAR_SCALE = 0.55f

/** Simple perspective: table y runs from the bottom of the canvas (0) to the top (TABLE_LENGTH), far things are smaller. */
private class TableProjection(size: Size) {
    private val bottom = size.height * 0.92f
    private val top = size.height * 0.1f
    private val centerX = size.width / 2f
    private val unit = size.width * 0.45f / TABLE_HALF_WIDTH
    val height = size.height

    fun scale(y: Float) = lerp(1f, FAR_SCALE, y / TABLE_LENGTH)
    fun toScreen(p: TablePoint) = Offset(centerX + p.x * unit * scale(p.y), bottom - p.y / TABLE_LENGTH * (bottom - top))
    fun length(units: Float, y: Float) = units * unit * scale(y)
}

/**
 * The table seen from the thrower: [cups] are the defender's cups. [flight] (with [progress] 0→1) is animated
 * along its touchdowns; [splash] 0→1 rings the cup that was hit. A flick that starts on the lower part of the
 * table is turned into a throw with [flickToThrow]; anything that isn't a throw is ignored.
 */
@Composable
fun BeerPongTable(
    cups: Set<Int>,
    flight: ThrowResult?,
    progress: Float,
    splash: Float,
    flickEnabled: Boolean,
    onFlick: (ThrowInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    val grain = rememberTiledBrush(imageResource(Res.drawable.tex_paper))
    Canvas(
        modifier = modifier.pointerInput(flickEnabled) {
            if (!flickEnabled) return@pointerInput
            val tracker = VelocityTracker()
            var fromBallArea = false
            var drag = Offset.Zero
            detectDragGestures(
                onDragStart = { start ->
                    tracker.resetTracking()
                    drag = Offset.Zero
                    fromBallArea = start.y > size.height * 0.55f
                },
                onDrag = { change, dragAmount ->
                    tracker.addPointerInputChange(change)
                    drag += dragAmount
                },
                onDragEnd = {
                    if (fromBallArea) {
                        val velocity = tracker.calculateVelocity()
                        val speed = hypot(velocity.x, velocity.y) / density
                        flickToThrow(drag.x, drag.y, speed)?.let(onFlick)
                    }
                },
            )
        },
    ) {
        val projection = TableProjection(size)
        drawTable(projection, grain)
        cups.sorted().forEach { drawCup(projection, BeerPongPhysics.cups[it]) }
        val hit = flight?.cupHit
        if (hit != null && splash > 0f) drawSplash(projection, BeerPongPhysics.cups[hit], splash)
        drawBall(projection, flight, progress)
    }
}

/** A plywood board on the wall (design §4): tape-colored with a faint paper grain, an ink border and an ink center line. */
private fun DrawScope.drawTable(projection: TableProjection, grain: Brush) {
    val corners = listOf(
        TablePoint(-TABLE_HALF_WIDTH, 0f), TablePoint(TABLE_HALF_WIDTH, 0f),
        TablePoint(TABLE_HALF_WIDTH, TABLE_LENGTH), TablePoint(-TABLE_HALF_WIDTH, TABLE_LENGTH),
    ).map(projection::toScreen)
    val path = Path().apply {
        moveTo(corners[0].x, corners[0].y)
        corners.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(path, ClinkColors.Tape)
    drawPath(path, grain, alpha = 0.12f)
    val midStart = projection.toScreen(TablePoint(0f, 0f))
    val midEnd = projection.toScreen(TablePoint(0f, TABLE_LENGTH))
    drawLine(ClinkColors.Ink.copy(alpha = 0.25f), midStart, midEnd, strokeWidth = 1.5.dp.toPx())
    drawPath(path, ClinkColors.Ink, style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round))
}

/** An empty cup seen from above at an angle: dark inside, an acid rim with an ink edge so it reads on the light board. */
private fun DrawScope.drawCup(projection: TableProjection, center: TablePoint) {
    val c = projection.toScreen(center)
    val r = projection.length(BeerPongPhysics.CUP_RADIUS, center.y)
    val size = Size(r * 2f, r * 1.2f)
    val topLeft = Offset(c.x - r, c.y - r * 0.6f)
    drawOval(ClinkColors.WallRaised, topLeft, size)
    drawOval(ClinkColors.Ink, topLeft, size, style = Stroke(width = r * 0.36f))
    drawOval(ClinkColors.Acid, topLeft, size, style = Stroke(width = r * 0.2f))
}

private fun DrawScope.drawSplash(projection: TableProjection, center: TablePoint, splash: Float) {
    val c = projection.toScreen(center)
    val r = projection.length(BeerPongPhysics.CUP_RADIUS, center.y) * (1f + splash * 1.2f)
    val topLeft = Offset(c.x - r, c.y - r * 0.6f)
    val size = Size(r * 2f, r * 1.2f)
    drawOval(ClinkColors.Ink.copy(alpha = 1f - splash), topLeft, size, style = Stroke(width = 4.dp.toPx()))
    drawOval(ClinkColors.Acid.copy(alpha = 1f - splash), topLeft, size, style = Stroke(width = 2.5.dp.toPx()))
}

/**
 * The ball: at the launch point when idle; during a flight it follows launch → touchdown 1 (→ touchdown 2 after a
 * rim hit), lifted by an arc, with a shadow on the table. Once it drops into a cup it is no longer drawn.
 */
private fun DrawScope.drawBall(projection: TableProjection, flight: ThrowResult?, progress: Float) {
    val launch = TablePoint(0f, 0f)
    val ballColor = ClinkColors.Paper
    if (flight == null) {
        drawBallAt(projection, launch, lift = 0f, color = ballColor)
        return
    }
    if (progress >= 1f && flight.cupHit != null) return
    val bounced = flight.touchdowns.size == 2
    val firstShare = if (bounced) 0.72f else 1f
    val (from, to, t, arc) = if (progress <= firstShare) {
        Segment(launch, flight.touchdowns[0], progress / firstShare, projection.height * 0.32f)
    } else {
        Segment(flight.touchdowns[0], flight.touchdowns[1], (progress - firstShare) / (1f - firstShare), projection.height * 0.05f)
    }
    val ground = TablePoint(lerp(from.x, to.x, t), lerp(from.y, to.y, t))
    drawBallAt(projection, ground, lift = 4f * arc * t * (1f - t), color = ballColor)
}

private data class Segment(val from: TablePoint, val to: TablePoint, val t: Float, val arc: Float)

private fun DrawScope.drawBallAt(projection: TableProjection, ground: TablePoint, lift: Float, color: Color) {
    val g = projection.toScreen(ground)
    val r = projection.length(BeerPongPhysics.BALL_RADIUS * 1.4f, ground.y) * (1f + lift / projection.height * 0.8f)
    // Hard shadow on the board (no blur), then the ball with an ink outline so it reads on the light board.
    drawOval(ClinkColors.Ink.copy(alpha = 0.45f), Offset(g.x - r, g.y - r * 0.4f), Size(r * 2f, r * 0.8f))
    val ball = Offset(g.x, g.y - r - lift)
    drawCircle(color, radius = r, center = ball)
    drawCircle(ClinkColors.Ink, radius = r, center = ball, style = Stroke(width = 2.dp.toPx()))
}
