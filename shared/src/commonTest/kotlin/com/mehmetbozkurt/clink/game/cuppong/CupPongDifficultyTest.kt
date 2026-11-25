package com.mehmetbozkurt.clink.game.cuppong

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the match length (spec §5.8): a simulated average player — aim off by N(0, 2.5°), speed off by
 * N(0, 3.5 %), always going for a random remaining cup — must clear six cups in at most 35 throws on average.
 */
class CupPongDifficultyTest {
    private fun Random.gaussian(): Float {
        val u1 = nextDouble().coerceAtLeast(1e-9)
        return (sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * nextDouble())).toFloat()
    }

    @Test
    fun averagePlayerClearsSixCupsWithin35Throws() {
        val random = Random(42)
        val racks = 300
        var throws = 0
        repeat(racks) {
            var left = CupPongPhysics.ALL_CUPS
            var count = 0
            while (left.isNotEmpty() && count < 300) {
                count++
                val aimed = aimAt(CupPongPhysics.cups[left.random(random)])
                val input = ThrowInput(aimed.yawDegrees + random.gaussian() * 2.5f, aimed.speed * (1f + random.gaussian() * 0.035f))
                simulateThrow(input, left).cupHit?.let { left = left - it }
            }
            throws += count
        }
        val average = throws.toFloat() / racks
        assertTrue(average <= 35f, "an average player needs $average throws to clear six cups")
    }
}