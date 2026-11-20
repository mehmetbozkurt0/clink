package com.mehmetbozkurt.clink.game.beerpong

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A point on the table, in table units. The origin is the thrower's ball, +y points at the target rack
 * and +x to the thrower's right. The rack is always the defender's cups, seen from the thrower.
 */
data class TablePoint(val x: Float, val y: Float)

/** What a throw is, independent of any screen: published as-is in online play so every phone replays it. */
@Serializable
data class ThrowInput(val angleDegrees: Float, val range: Float)

/** One touchdown is a clean landing; two mean the ball hit a rim and bounced once. */
data class ThrowResult(val input: ThrowInput, val touchdowns: List<TablePoint>, val cupHit: Int?)

/** Hand-written throw model (spec §7.3). The constants set the feel and may be tuned on a device. */
object BeerPongPhysics {
    const val CUP_RADIUS = 3f
    const val BALL_RADIUS = 1f
    const val RIM_BOUNCE = 4f
    const val MIN_FLICK = 300f // dp/s
    const val MAX_FLICK = 3500f // dp/s
    const val MIN_RANGE = 70f
    const val MAX_RANGE = 100f
    const val AIM_SCALE = 0.25f // a 20° wobble of the finger turns the throw by only 5°
    const val MAX_FLICK_TILT = 60f // a flick further than this from vertical is a sideways swipe, not a throw

    /** Cup centers by id: back row 0-1-2, middle row 3-4, front cup 5 (a 3-2-1 triangle pointing at the thrower). */
    val cups: List<TablePoint> = listOf(
        TablePoint(-6.2f, 90f), TablePoint(0f, 90f), TablePoint(6.2f, 90f),
        TablePoint(-3.1f, 84.6f), TablePoint(3.1f, 84.6f),
        TablePoint(0f, 79.2f),
    )

    val ALL_CUPS: Set<Int> = cups.indices.toSet()
}

/**
 * Turns a flick into a throw. The direction comes from the whole drag ([dragDx], [dragDy] on screen, dy < 0 is up
 * toward the rack), which is steadier than the release velocity's direction; it is scaled down by
 * [BeerPongPhysics.AIM_SCALE]. The release speed sets the range.
 * Too weak, downward or more than [BeerPongPhysics.MAX_FLICK_TILT] off vertical → null: not a throw.
 */
fun flickToThrow(dragDx: Float, dragDy: Float, speedDpPerSec: Float): ThrowInput? = with(BeerPongPhysics) {
    if (dragDy >= 0f || speedDpPerSec < MIN_FLICK) return null
    val tilt = atan2(dragDx, -dragDy) * 180f / PI.toFloat()
    if (abs(tilt) > MAX_FLICK_TILT) return null
    val t = ((speedDpPerSec - MIN_FLICK) / (MAX_FLICK - MIN_FLICK)).coerceIn(0f, 1f)
    ThrowInput(tilt * AIM_SCALE, MIN_RANGE + t * (MAX_RANGE - MIN_RANGE))
}

/**
 * Where the ball ends up. The nearest remaining cup decides (ties go to the lowest id): inside its inner edge
 * the ball is in; on its rim it bounces once, away from that cup's center, and counts only if it then drops
 * into a cup. Deterministic: the same input and cups always give the same result.
 */
fun simulateThrow(input: ThrowInput, remainingCups: Set<Int>): ThrowResult = with(BeerPongPhysics) {
    val radians = input.angleDegrees * PI.toFloat() / 180f
    val landing = TablePoint(input.range * sin(radians), input.range * cos(radians))
    val inside = CUP_RADIUS - BALL_RADIUS
    val (cup, distance) = nearestCup(landing, remainingCups) ?: return ThrowResult(input, listOf(landing), null)
    when {
        distance <= inside -> ThrowResult(input, listOf(landing), cup)
        distance <= CUP_RADIUS + BALL_RADIUS -> {
            val center = cups[cup]
            val bounce = TablePoint(
                landing.x + (landing.x - center.x) / distance * RIM_BOUNCE,
                landing.y + (landing.y - center.y) / distance * RIM_BOUNCE,
            )
            val second = nearestCup(bounce, remainingCups)?.takeIf { it.second <= inside }?.first
            ThrowResult(input, listOf(landing, bounce), second)
        }
        else -> ThrowResult(input, listOf(landing), null)
    }
}

private fun nearestCup(point: TablePoint, remainingCups: Set<Int>): Pair<Int, Float>? =
    remainingCups.sorted()
        .map { id -> id to hypot(point.x - BeerPongPhysics.cups[id].x, point.y - BeerPongPhysics.cups[id].y) }
        .minByOrNull { it.second }
