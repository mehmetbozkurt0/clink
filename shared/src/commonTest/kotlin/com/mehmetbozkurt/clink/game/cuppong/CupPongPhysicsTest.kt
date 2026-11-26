package com.mehmetbozkurt.clink.game.cuppong

import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CupPongPhysicsTest {
    private val all = CupPongPhysics.ALL_CUPS
    private val cups = CupPongPhysics.cups

    @Test
    fun straightThrowAtTheFrontCupGoesIn() {
        assertEquals(5, simulateThrow(aimAt(cups[5]), all).cupHit)
    }

    @Test
    fun everyCupIsReachable() {
        for (id in 0..5) assertEquals(id, simulateThrow(aimAt(cups[id]), setOf(id)).cupHit, "cup $id")
    }

    @Test
    fun tooShortMisses() {
        assertNull(simulateThrow(aimAt(cups[5], speedFactor = 0.85f), all).cupHit)
    }

    @Test
    fun tooLongMisses() {
        assertNull(simulateThrow(aimAt(cups[1], speedFactor = 1.25f), all).cupHit)
    }

    @Test
    fun ballOverTheOpeningDropsIn() {
        // 3 cm off-centre clips the rim; it drops in only because a ball already over the opening isn't bounced
        // (without that rule the limit is about 2.7 cm).
        assertEquals(5, simulateThrow(aimAt(cups[5], sideOffset = 3.0f), setOf(5)).cupHit)
    }

    @Test
    fun nearRimHitIsRecorded() {
        val result = simulateThrow(aimAt(cups[5], sideOffset = 5.3f), setOf(5))
        assertTrue(result.touchedRim)
        assertEquals(5, result.rimContacts.first().cup)
    }

    @Test
    fun rimBounceCanEndInAnotherCup() {
        var found = false
        for (id in 0..5) {
            for (yawStep in -12..12) for (speedStep in -4..4) {
                val aimed = aimAt(cups[id], speedFactor = 1f + speedStep * 0.01f)
                val result = simulateThrow(ThrowInput(aimed.yawDegrees + yawStep * 0.25f, aimed.speed), all)
                val hit = result.cupHit
                if (hit != null && result.rimContacts.any { it.cup != hit }) found = true
            }
        }
        assertTrue(found, "no throw bounced off one cup's rim into another cup")
    }

    @Test
    fun removedCupsAreIgnored() {
        assertNotEquals(5, simulateThrow(aimAt(cups[5]), all - 5).cupHit)
    }

    @Test
    fun offTheSideTheBallFalls() {
        val result = simulateThrow(ThrowInput(40f, 400f), all)
        assertNull(result.cupHit)
        assertTrue(result.points().last().z < 0f)
    }

    @Test
    fun slowRollerEnds() {
        val result = simulateThrow(ThrowInput(0f, 300f), all)
        assertTrue(result.durationMillis <= 4000)
        assertNull(result.cupHit)
    }

    @Test
    fun sameThrowSameResult() {
        assertEquals(simulateThrow(aimAt(cups[3]), all), simulateThrow(aimAt(cups[3]), all))
    }

    @Test
    fun pathIsInTenthsOfACentimetre() {
        val result = simulateThrow(aimAt(cups[5]), all)
        assertEquals(0, result.path.size % 3)
        val end = result.points().last()
        assertTrue(hypot(end.x - cups[5].x, end.y - cups[5].y) < 5f)
    }
    /**
     * A ball that clips a rim must leave it. The softened rim (spec 2026-11-26 §7.2) made it possible for a ball to
     * be pushed back onto the rim shell every step and hover there for the whole flight, buzzing the cup, while the
     * turn waited on it.
     */
    @Test
    fun noThrowGetsStuckOnARim() {
        var worstSamples = 0
        var worstContacts = 0
        var worstInput = ThrowInput(0f, 0f)
        for (yawTenths in -120..120 step 2) {
            for (speedTenths in 3350..3750 step 5) {
                val input = ThrowInput(yawTenths / 10f, speedTenths / 10f)
                val r = simulateThrow(input, all)
                val samples = r.path.size / 3
                if (samples > worstSamples || r.rimContacts.size > worstContacts) worstInput = input
                worstSamples = maxOf(worstSamples, samples)
                worstContacts = maxOf(worstContacts, r.rimContacts.size)
            }
        }
        assertTrue(worstSamples <= 150, "a throw took $worstSamples samples ($worstInput)")
        assertTrue(worstContacts <= 20, "a throw hit a rim $worstContacts times ($worstInput)")
    }
}
