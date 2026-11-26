package com.mehmetbozkurt.clink.game.cuppong

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The table seen from behind the thrower, drawn from 3D (spec §5.4): [cups] are the defender's cups. [flight] (with
 * [progress] 0→1) is replayed along its path; [splash] 0→1 rings the cup it hit. A flick that starts on the lower part
 * of the table becomes a throw through [flickToThrow]; the ball itself never follows the finger.
 */
@Composable
fun CupPongTable(
    cups: Set<Int>,
    flight: ThrowResult?,
    progress: Float,
    splash: Float,
    flickEnabled: Boolean,
    onFlick: (ThrowInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val points = remember(flight) { flight?.points() }
    // The faint streak the finger leaves while it drags (spec 2026-11-26 §7.4): it shows the gesture, never the throw.
    // Keyed on flickEnabled: when the turn changes Compose cancels the gesture without onDragCancel, so a
    // trail remembered across that would keep drawing through the flight and the other player's turn.
    val trail = remember(flickEnabled) { mutableStateListOf<Offset>() }
    Canvas(
        modifier = modifier.pointerInput(flickEnabled) {
            if (!flickEnabled) return@pointerInput
            val tracker = VelocityTracker()
            var fromBallArea = false
            var finger = Offset.Zero
            detectDragGestures(
                onDragStart = { start ->
                    tracker.resetTracking()
                    finger = start
                    fromBallArea = start.y > size.height * 0.55f
                    trail.clear()
                    if (fromBallArea) trail.add(start)
                },
                onDrag = { change, amount ->
                    tracker.addPointerInputChange(change)
                    finger += amount
                    if (fromBallArea) {
                        trail.add(finger)
                        if (trail.size > TRAIL_POINTS) trail.removeAt(0)
                    }
                },
                onDragEnd = {
                    if (fromBallArea) {
                        val v = tracker.calculateVelocity()
                        val camera = CupPongCamera(canvasSize.width, canvasSize.height)
                        flickToThrow(camera, finger.x, finger.y, v.x, v.y, hypot(v.x, v.y) / density)?.let(onFlick)
                    }
                    trail.clear()
                },
                onDragCancel = { trail.clear() },
            )
        },
    ) {
        canvasSize = size
        val camera = CupPongCamera(size.width, size.height)
        val ball = ballAt(points, flight?.cupHit, progress)
        // A ball that dropped below the table top (off an edge) is behind the board: drawn first, the table hides it.
        val underTable = ball != null && ball.z < 0f
        if (underTable) drawBall(camera, ball!!)
        drawBoard(camera)
        if (ball != null && !underTable && abs(ball.x) <= CupPongPhysics.TABLE_HALF_WIDTH && ball.y in 0f..CupPongPhysics.TABLE_LENGTH) {
            drawBallShadow(camera, ball)
        }
        // Painter's order: far things first.
        val items = cups.map { id ->
            val wobble = wobble(flight, points, progress, id)
            val center = CupPongPhysics.cups[id].let { Vec3(it.x + wobble, it.y, it.z) }
            center.y to { drawCup(camera, center, if (id == flight?.cupHit) splash else 0f) }
        } + listOfNotNull(ball?.takeIf { !underTable }?.let { b -> b.y - 0.01f to { drawBall(camera, b) } })
        items.sortedByDescending { it.first }.forEach { it.second() }
        // Last, so it is visible over the table, but faint enough not to hide a cup.
        drawFingerTrail(trail)
    }
}

/** The ball at [progress] along the path; at rest before a throw, gone once it has dropped into a cup. */
private fun ballAt(points: List<Vec3>?, cupHit: Int?, progress: Float): Vec3? {
    if (points.isNullOrEmpty()) return CupPongPhysics.REST
    if (progress >= 1f && cupHit != null) return null
    if (points.size == 1) return points[0]
    val f = progress.coerceIn(0f, 1f) * (points.size - 1)
    val i = f.toInt().coerceIn(0, points.size - 2)
    return points[i] + (points[i + 1] - points[i]) * (f - i)
}

/** A cup the ball just clipped on the rim rocks sideways for a moment (±0.9 cm, fading over 18 samples). */
private fun wobble(flight: ThrowResult?, points: List<Vec3>?, progress: Float, cup: Int): Float {
    if (flight == null || points.isNullOrEmpty()) return 0f
    val sample = progress * (points.size - 1)
    val contact = flight.rimContacts.lastOrNull { it.cup == cup && it.sample <= sample } ?: return 0f
    val since = sample - contact.sample
    if (since > WOBBLE_SAMPLES) return 0f
    return 0.9f * (1f - since / WOBBLE_SAMPLES) * sin(since * 1.1f)
}

private const val WOBBLE_SAMPLES = 18f

private fun Triple<Float, Float, Float>.offset() = Offset(first, second)

private fun DrawScope.polygon(points: List<Offset>) = Path().apply {
    moveTo(points[0].x, points[0].y)
    points.drop(1).forEach { lineTo(it.x, it.y) }
    close()
}

/** The board in tape colour, darker toward the far end (depth), with an ink border, centre line and far-edge face. */
private fun DrawScope.drawBoard(camera: CupPongCamera) {
    val w = CupPongPhysics.TABLE_HALF_WIDTH
    val l = CupPongPhysics.TABLE_LENGTH
    val corners = listOf(Vec3(-w, 0f, 0f), Vec3(w, 0f, 0f), Vec3(w, l, 0f), Vec3(-w, l, 0f)).map { camera.project(it).offset() }
    val board = polygon(corners)
    drawPath(board, Brush.verticalGradient(listOf(lerp(ClinkColors.Tape, ClinkColors.Ink, 0.18f), ClinkColors.Tape), startY = corners[2].y, endY = size.height))
    drawLine(ClinkColors.Ink.copy(alpha = 0.25f), camera.project(Vec3(0f, 0f, 0f)).offset(), camera.project(Vec3(0f, l, 0f)).offset(), strokeWidth = 1.5.dp.toPx())
    drawPath(board, ClinkColors.Ink, style = Stroke(width = 3.dp.toPx()))
    for (x in listOf(-w, w)) {
        drawLine(ClinkColors.Ink, camera.project(Vec3(x, l, 0f)).offset(), camera.project(Vec3(x, l, -4f)).offset(), strokeWidth = 2.dp.toPx())
    }
}

/** An empty cup: contact shadow, a left-lit pink body, a dark opening, a paper rim, and a splash ring on a hit. */
private fun DrawScope.drawCup(camera: CupPongCamera, c: Vec3, splash: Float) {
    fun ring(radius: Float, z: Float) = (0 until CUP_SEGMENTS).map { i ->
        val a = 2f * PI.toFloat() * i / CUP_SEGMENTS
        camera.project(Vec3(c.x + cos(a) * radius, c.y + sin(a) * radius, z)).offset()
    }
    drawPath(polygon(ring(CupPongPhysics.CUP_BOTTOM_RADIUS * 1.45f, 0f)), ClinkColors.Ink.copy(alpha = 0.38f))
    val rim = ring(CupPongPhysics.CUP_TOP_RADIUS, CupPongPhysics.CUP_HEIGHT)
    val outline = convexHull(rim + ring(CupPongPhysics.CUP_BOTTOM_RADIUS, 0f))
    val body = polygon(outline)
    val dark = lerp(ClinkColors.Pink, ClinkColors.Ink, 0.45f)
    val light = lerp(ClinkColors.Pink, Color.White, 0.35f)
    drawPath(
        body,
        Brush.horizontalGradient(0f to dark, 0.3f to light, 0.55f to ClinkColors.Pink, 1f to dark, startX = outline.minOf { it.x }, endX = outline.maxOf { it.x }),
    )
    drawPath(body, ClinkColors.Ink, style = Stroke(width = 1.5.dp.toPx()))
    val opening = polygon(rim)
    drawPath(
        opening,
        Brush.verticalGradient(
            listOf(lerp(ClinkColors.Pink, ClinkColors.Ink, 0.75f), lerp(ClinkColors.Pink, ClinkColors.Ink, 0.55f)),
            startY = rim.minOf { it.y },
            endY = rim.maxOf { it.y },
        ),
    )
    clipPath(opening) {
        drawPath(polygon(ring(CupPongPhysics.CUP_TOP_RADIUS * 0.8f, CupPongPhysics.CUP_HEIGHT - 5f)), lerp(ClinkColors.Pink, ClinkColors.Ink, 0.62f))
    }
    val rimWidth = CupPongPhysics.CUP_TOP_RADIUS * camera.focal / camera.project(c).third * 0.14f
    drawPath(opening, ClinkColors.Paper, style = Stroke(width = rimWidth.coerceAtLeast(1.5f)))
    if (splash > 0f) {
        val ripple = polygon(ring(CupPongPhysics.CUP_TOP_RADIUS * (1f + splash * 1.3f), CupPongPhysics.CUP_HEIGHT))
        drawPath(ripple, ClinkColors.Acid.copy(alpha = 1f - splash), style = Stroke(width = 3.dp.toPx()))
    }
}

private const val CUP_SEGMENTS = 40

private fun DrawScope.drawBallShadow(camera: CupPongCamera, ball: Vec3) {
    val (x, y, depth) = camera.project(Vec3(ball.x, ball.y, 0f))
    val r = CupPongPhysics.BALL_RADIUS * camera.focal / depth
    val fade = (1f - ball.z / 80f).coerceIn(0.15f, 1f)
    drawOval(ClinkColors.Ink.copy(alpha = 0.4f * fade), Offset(x - r, y - r * 0.35f), Size(r * 2f, r * 0.7f))
}

/** A paper sphere lit from the upper left, with an ink edge. */
private fun DrawScope.drawBall(camera: CupPongCamera, ball: Vec3) {
    val (x, y, depth) = camera.project(ball)
    val r = CupPongPhysics.BALL_RADIUS * camera.focal / depth
    val center = Offset(x, y)
    drawCircle(
        Brush.radialGradient(
            0f to Color.White,
            0.45f to ClinkColors.Paper,
            1f to lerp(ClinkColors.Paper, ClinkColors.Ink, 0.35f),
            center = Offset(x - r * 0.35f, y - r * 0.4f),
            radius = r * 1.5f,
        ),
        radius = r,
        center = center,
    )
    drawCircle(ClinkColors.Ink, radius = r, center = center, style = Stroke(width = 1.5.dp.toPx()))
}

/** The outline around a cup's rim and base circles (Andrew's monotone chain). */
private fun convexHull(points: List<Offset>): List<Offset> {
    val sorted = points.sortedWith(compareBy({ it.x }, { it.y }))
    fun turn(o: Offset, a: Offset, b: Offset) = (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)
    fun half(input: List<Offset>): List<Offset> {
        val out = mutableListOf<Offset>()
        for (p in input) {
            while (out.size >= 2 && turn(out[out.size - 2], out.last(), p) <= 0f) out.removeAt(out.lastIndex)
            out.add(p)
        }
        return out
    }
    return half(sorted).dropLast(1) + half(sorted.asReversed()).dropLast(1)
}

/** How many of the finger's latest positions the streak keeps. */
private const val TRAIL_POINTS = 24

/** A short streak along the finger's path, oldest end faded out, drawn over the table but barely there. */
private fun DrawScope.drawFingerTrail(trail: List<Offset>) {
    if (trail.size < 2) return
    for (i in 1 until trail.size) {
        val age = i.toFloat() / (trail.size - 1)
        drawLine(
            color = ClinkColors.Paper.copy(alpha = 0.26f * age),
            start = trail[i - 1],
            end = trail[i],
            strokeWidth = 2.dp.toPx() + 2.dp.toPx() * age,
            cap = StrokeCap.Round,
        )
    }
}
