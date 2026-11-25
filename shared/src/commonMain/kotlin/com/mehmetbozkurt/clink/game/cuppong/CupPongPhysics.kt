package com.mehmetbozkurt.clink.game.cuppong

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A point or direction in the Cup Pong world, in centimetres: x to the thrower's right, y away from the thrower
 * along the table, z up. The table's near edge is y = 0.
 */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(k: Float) = Vec3(x * k, y * k, z * k)
    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun length() = sqrt(this dot this)
    fun normalized() = this * (1f / length())
}

/** The world and the ball's physics (spec 2026-11-24-cup-pong-3d §5.1–5.2). The numbers set the feel. */
object CupPongPhysics {
    const val GRAVITY = 980f
    const val BALL_RADIUS = 2f
    const val CUP_TOP_RADIUS = 5.0f
    const val CUP_BOTTOM_RADIUS = 3.5f
    const val CUP_HEIGHT = 12f
    const val TABLE_HALF_WIDTH = 30.5f
    const val TABLE_LENGTH = 262f
    const val PITCH_DEGREES = 50f
    const val MIN_SPEED = 300f
    const val MAX_SPEED = 400f
    const val MAX_YAW = 45f
    const val RIM_TUBE = 0.3f
    const val SAMPLES_PER_SECOND = 60
    const val STEPS_PER_SECOND = 240
    const val MAX_SECONDS = 4

    /** Where every throw starts: the ball resting on the thrower's end of the table. */
    val REST = Vec3(0f, 100f, BALL_RADIUS)

    /** Cup positions by id: back row 0-1-2, middle row 3-4, front cup 5 (a triangle pointing at the thrower). */
    val cups: List<Vec3> = listOf(
        Vec3(-10.4f, 228f, 0f), Vec3(0f, 228f, 0f), Vec3(10.4f, 228f, 0f),
        Vec3(-5.2f, 219f, 0f), Vec3(5.2f, 219f, 0f),
        Vec3(0f, 210f, 0f),
    )

    val ALL_CUPS: Set<Int> = cups.indices.toSet()

    /** The cup's wall radius at height [z] (it widens toward the top). */
    fun cupRadiusAt(z: Float) = CUP_BOTTOM_RADIUS + (CUP_TOP_RADIUS - CUP_BOTTOM_RADIUS) * (z / CUP_HEIGHT).coerceIn(0f, 1f)
}

/** A throw, independent of any screen: published as-is in online play. [yawDegrees] 0 = straight down the table. */
@Serializable
data class ThrowInput(val yawDegrees: Float, val speed: Float) {
    /**
     * Online a member's phone sends this, so it is checked before simulating: null if a value isn't a number,
     * otherwise held to ±[CupPongPhysics.MAX_YAW] and [CupPongPhysics.MIN_SPEED]…[CupPongPhysics.MAX_SPEED].
     */
    fun sanitized(): ThrowInput? {
        if (!yawDegrees.isFinite() || !speed.isFinite()) return null
        return ThrowInput(
            yawDegrees.coerceIn(-CupPongPhysics.MAX_YAW, CupPongPhysics.MAX_YAW),
            speed.coerceIn(CupPongPhysics.MIN_SPEED, CupPongPhysics.MAX_SPEED),
        )
    }
}

/** The ball touched cup [cup]'s rim at path sample [sample] (the cup wobbles from there). */
@Serializable
data class RimContact(val sample: Int, val cup: Int)

/**
 * A simulated throw. [path] is the ball's position 60 times a second as x, y, z triples in whole tenths of a
 * centimetre, so online every phone replays exactly the host's flight.
 */
@Serializable
data class ThrowResult(
    val input: ThrowInput,
    val cupHit: Int?,
    val touchedRim: Boolean,
    val rimContacts: List<RimContact>,
    val path: List<Int>,
) {
    fun points(): List<Vec3> = path.chunked(3) { Vec3(it[0] / 10f, it[1] / 10f, it[2] / 10f) }

    val durationMillis: Int
        get() = ((path.size / 3 - 1) * 1000 / CupPongPhysics.SAMPLES_PER_SECOND).coerceAtLeast(1)
}

/**
 * The ball as a real projectile (spec §5.2): gravity, cup rims (forgiving over the opening), inner and outer cup walls
 * and table bounces, at a fixed step. Deterministic: the same input and cups always give the same result. A rim bounce
 * that ends in another cup counts for that cup.
 */
fun simulateThrow(input: ThrowInput, remainingCups: Set<Int>): ThrowResult = with(CupPongPhysics) {
    val step = 1f / STEPS_PER_SECOND
    val samplesEvery = STEPS_PER_SECOND / SAMPLES_PER_SECOND
    val yaw = input.yawDegrees * PI.toFloat() / 180f
    val pitch = PITCH_DEGREES * PI.toFloat() / 180f
    var p = REST
    var v = Vec3(sin(yaw) * cos(pitch), cos(yaw) * cos(pitch), sin(pitch)) * input.speed
    val path = mutableListOf(p)
    val rimContacts = mutableListOf<RimContact>()
    var touchedRim = false
    val cupIds = remainingCups.sorted()

    fun result(hit: Int?) = ThrowResult(input, hit, touchedRim, rimContacts, path.flatMap { listOf(tenths(it.x), tenths(it.y), tenths(it.z)) })

    fun sink(cup: Vec3, id: Int): ThrowResult {
        repeat(6) { i ->
            val t = (i + 1) / 6f
            path.add(Vec3(p.x + (cup.x - p.x) * t, p.y + (cup.y - p.y) * t, (p.z - (i + 1) * 0.8f).coerceAtLeast(BALL_RADIUS)))
        }
        return result(id)
    }

    for (n in 1..STEPS_PER_SECOND * MAX_SECONDS) {
        v = Vec3(v.x, v.y, v.z - GRAVITY * step)
        p = p + v * step
        for (id in cupIds) {
            val c = cups[id]
            val dx = p.x - c.x
            val dy = p.y - c.y
            val horizontal = hypot(dx, dy)
            if (horizontal < 0.001f) {
                if (p.z < CUP_HEIGHT - 4f) return sink(c, id)
                continue
            }
            val nx = dx / horizontal
            val ny = dy / horizontal
            // Rim, a thin torus. Forgiving: a ball whose centre is already over the opening drops in instead.
            val overOpening = horizontal < CUP_TOP_RADIUS - 0.6f
            val rim = Vec3(c.x + nx * CUP_TOP_RADIUS, c.y + ny * CUP_TOP_RADIUS, CUP_HEIGHT)
            val off = p - rim
            val dist = off.length()
            if (!overOpening && dist < BALL_RADIUS + RIM_TUBE && dist > 0.001f) {
                val normal = off * (1f / dist)
                val into = v dot normal
                if (into < 0f) {
                    // The part into the rim bounces back softly; most of the sliding part is kept.
                    val along = normal * into
                    v = (v - along) * 0.9f - along * 0.35f
                    if (rimContacts.lastOrNull()?.sample != path.size) rimContacts.add(RimContact(path.size, id))
                    touchedRim = true
                }
                p = rim + normal * (BALL_RADIUS + RIM_TUBE)
            }
            val wall = cupRadiusAt(p.z)
            if (p.z < CUP_HEIGHT && horizontal < wall) {
                // Inside: the inner wall knocks the ball back toward the middle (the rattle), then it drops.
                val limit = wall - BALL_RADIUS
                if (horizontal > limit) {
                    val out = v.x * nx + v.y * ny
                    if (out > 0f) v = Vec3(v.x - 1.5f * out * nx, v.y - 1.5f * out * ny, v.z)
                    p = Vec3(c.x + nx * limit, c.y + ny * limit, p.z)
                }
                if (p.z < CUP_HEIGHT - 4f) return sink(c, id)
            } else if (p.z < CUP_HEIGHT && horizontal < wall + BALL_RADIUS) {
                // Outer wall: push the ball back out and bounce horizontally.
                val into = v.x * nx + v.y * ny
                if (into < 0f) v = Vec3(v.x - 1.4f * into * nx, v.y - 1.4f * into * ny, v.z)
                p = Vec3(c.x + nx * (wall + BALL_RADIUS), c.y + ny * (wall + BALL_RADIUS), p.z)
            }
        }
        // Table: bounce with some loss; once it no longer hops, it rolls with friction.
        val onTable = abs(p.x) <= TABLE_HALF_WIDTH && p.y in 0f..TABLE_LENGTH
        if (onTable && p.z <= BALL_RADIUS) {
            p = Vec3(p.x, p.y, BALL_RADIUS)
            if (v.z < 0f) v = Vec3(v.x * 0.88f, v.y * 0.88f, if (-v.z < 60f) 0f else -v.z * 0.6f)
            v = Vec3(v.x * (1f - 1.2f * step), v.y * (1f - 1.2f * step), v.z)
        }
        if (n % samplesEvery == 0) path.add(p)
        val stopped = onTable && p.z <= BALL_RADIUS + 0.01f && hypot(v.x, v.y) < 20f
        if (p.z < -40f || stopped || p.y > TABLE_LENGTH + 80f) break
    }
    if (path.last() != p) path.add(p)
    result(null)
}

private fun tenths(value: Float) = (value * 10f).roundToInt()
