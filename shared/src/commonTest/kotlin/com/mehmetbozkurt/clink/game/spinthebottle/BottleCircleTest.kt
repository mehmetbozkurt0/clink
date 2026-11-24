package com.mehmetbozkurt.clink.game.spinthebottle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BottleCircleTest {
    @Test
    fun firstPlayerIsAtTop() = assertEquals(-90f, playerAngleDegrees(0, 4))

    @Test
    fun playersAreEvenlySpacedClockwise() = assertEquals(0f, playerAngleDegrees(1, 4))

    @Test
    fun twelvePlayers() = assertEquals(240f, playerAngleDegrees(11, 12))

    @Test
    fun weakFlickDoesNotSpin() = assertNull(flickToSpin(299f))

    @Test
    fun flickSpeedMapsToTurnsAndDuration() {
        assertEquals(SpinMotion(turns = 3, durationMillis = 1800), flickToSpin(300f))
        assertEquals(SpinMotion(turns = 8, durationMillis = 3200), flickToSpin(MAX_FLICK_DP_PER_SECOND))
        assertEquals(SpinMotion(turns = 8, durationMillis = 3200), flickToSpin(MAX_FLICK_DP_PER_SECOND * 3))
    }

    @Test
    fun spinEndsWithTheBottlePointingAtTheTarget() {
        for (count in listOf(2, 5, 12)) for (target in 0 until count) for (start in listOf(0f, 37f, 725f, -200f)) {
            val end = spinEndRotation(current = start, targetAngle = playerAngleDegrees(target, count), extraTurns = 3)
            val pointing = bottleAngleDegrees(end)
            val expected = normalized(playerAngleDegrees(target, count))
            assertTrue(kotlin.math.abs(pointing - expected) < 0.01f, "count=$count target=$target start=$start: $pointing vs $expected")
            assertTrue(end - start >= 3 * 360f && end - start < 4 * 360f, "turns for start=$start: ${end - start}")
        }
    }

    @Test
    fun sectorCentresMapToTheirPlayer() {
        for (i in 0 until 4) assertEquals(i, sectorAt(playerAngleDegrees(i, 4), 4))
    }

    @Test
    fun sectorAtTwoPlayers() {
        assertEquals(0, sectorAt(-90f, 2))
        assertEquals(1, sectorAt(90f, 2))
        assertEquals(0, sectorAt(-1f, 2))
        assertEquals(1, sectorAt(1f, 2))
    }

    @Test
    fun borderBelongsToNextSector() = assertEquals(1, sectorAt(-45f, 4))

    @Test
    fun wrapAroundTop() {
        assertEquals(0, sectorAt(269.9f, 4))
        assertEquals(0, sectorAt(-90f + 360f, 4))
        assertEquals(3, sectorAt(224.9f, 4))
    }

    @Test
    fun twelvePlayersCentresAndEdges() {
        val centre = playerAngleDegrees(11, 12)
        assertEquals(11, sectorAt(centre, 12))
        assertEquals(11, sectorAt(centre + 14.9f, 12))
        assertEquals(0, sectorAt(centre + 15f, 12))
    }

    private fun normalized(degrees: Float) = ((degrees % 360f) + 360f) % 360f
}
