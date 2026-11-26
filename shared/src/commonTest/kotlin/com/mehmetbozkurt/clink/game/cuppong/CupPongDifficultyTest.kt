package com.mehmetbozkurt.clink.game.cuppong

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards that the game is winnable for a real player (spec 2026-11-26-cup-pong-easier §1). The player picks a cup but
 * has to **guess** the flick: the error is a share of what a finger can express — aim N(0, 4°), strength N(0, 15 %) of
 * the whole speed range — not a small percentage of an answer they already knew.
 */
class CupPongDifficultyTest {
    private fun Random.gaussian(): Float {
        val u1 = nextDouble().coerceAtLeast(1e-9)
        return (sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * nextDouble())).toFloat()
    }

    /** What a player who aims at [target] actually throws, clamped the way the game clamps it. */
    private fun Random.guessedThrow(target: Vec3): ThrowInput {
        val aimed = aimAt(target)
        val spread = (CupPongPhysics.MAX_SPEED - CupPongPhysics.MIN_SPEED) * 0.15f
        return ThrowInput(aimed.yawDegrees + gaussian() * 4f, aimed.speed + gaussian() * spread).sanitized()!!
    }

    @Test
    fun aGuessingPlayerScoresAboutFortyPercent() {
        val random = Random(42)
        val throws = 2000
        var hits = 0
        repeat(throws) {
            val target = CupPongPhysics.cups[CupPongPhysics.ALL_CUPS.random(random)]
            if (simulateThrow(random.guessedThrow(target), CupPongPhysics.ALL_CUPS).cupHit != null) hits++
        }
        val rate = hits.toFloat() / throws
        println("Cup Pong: a guessing player scores $rate of their throws")
        assertTrue(rate in 0.35f..0.50f, "a guessing player scores $rate of their throws")
    }

    /**
     * Six cups at a 40 % hit rate cannot take 15 throws: the last cups are the hard ones, so about 21 is what that
     * rate buys. The ceiling is set from the rate, not wished for independently of it.
     */
    @Test
    fun aGuessingPlayerClearsSixCupsWithin24Throws() {
        val random = Random(42)
        val racks = 300
        var throws = 0
        repeat(racks) {
            var left = CupPongPhysics.ALL_CUPS
            var count = 0
            while (left.isNotEmpty() && count < 300) {
                count++
                val input = random.guessedThrow(CupPongPhysics.cups[left.random(random)])
                simulateThrow(input, left).cupHit?.let { left = left - it }
            }
            throws += count
        }
        val average = throws.toFloat() / racks
        println("Cup Pong: a guessing player needs $average throws per rack")
        assertTrue(average <= 24f, "a guessing player needs $average throws to clear six cups")
    }

    /**
     * How forgiving one intent is. A straight line down the table can never score continuously — the six-cup triangle
     * has no cup on the centre at mid-depth — so what matters is the stretch of strength that still scores *something*
     * when a player aims at a cup and flicks a bit soft or a bit hard. That is the window they learn the feel of.
     */
    @Test
    fun aimingAtACupForgivesAWideStretchOfStrength() {
        val band = CupPongPhysics.MAX_SPEED - CupPongPhysics.MIN_SPEED
        val steps = 40
        val runs = CupPongPhysics.ALL_CUPS.sorted().map { id ->
            val aimed = aimAt(CupPongPhysics.cups[id])
            val scores = (0..steps).map { i ->
                val offset = (i.toFloat() / steps - 0.5f) * band
                val input = ThrowInput(aimed.yawDegrees, aimed.speed + offset).sanitized()!!
                simulateThrow(input, CupPongPhysics.ALL_CUPS).cupHit != null
            }
            var longest = 0
            var run = 0
            scores.forEach { hit -> if (hit) { run++; longest = maxOf(longest, run) } else run = 0 }
            id to longest
        }
        println("Cup Pong: forgiving stretch per cup, out of ${steps + 1}: " + runs.joinToString { "${it.first}=${it.second}" })
        val worst = runs.minOf { it.second }
        assertTrue(worst >= 9, "aiming at cup ${runs.first { it.second == worst }.first} forgives only $worst of ${steps + 1}")
    }
}
