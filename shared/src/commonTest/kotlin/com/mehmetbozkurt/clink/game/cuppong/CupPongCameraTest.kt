package com.mehmetbozkurt.clink.game.cuppong

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CupPongCameraTest {
    private val camera = CupPongCamera(1000f, 1600f)
    private val ball = camera.project(CupPongPhysics.REST)

    private fun flick(vx: Float, vy: Float, speed: Float) = flickToThrow(camera, ball.first, ball.second, vx, vy, speed)

    @Test
    fun straightUpFlickAimsStraight() {
        assertTrue(abs(flick(0f, -2000f, 1500f)!!.yawDegrees) < 0.5f)
    }

    @Test
    fun rightwardFlickAimsRight() {
        assertTrue(flick(300f, -2000f, 1500f)!!.yawDegrees > 0f)
    }

    @Test
    fun slowFlickIsNotAThrow() = assertNull(flick(0f, -2000f, 299f))

    @Test
    fun downwardFlickIsNotAThrow() = assertNull(flick(0f, 500f, 1500f))

    @Test
    fun tooSidewaysIsNotAThrow() = assertNull(flick(2000f, -200f, 1500f))

    @Test
    fun speedMapsToTheBand() {
        assertEquals(CupPongPhysics.MIN_SPEED, flick(0f, -2000f, 300f)!!.speed, 0.01f)
        assertEquals(CupPongPhysics.MAX_SPEED, flick(0f, -2000f, 3000f)!!.speed, 0.01f)
        assertEquals(CupPongPhysics.MAX_SPEED, flick(0f, -2000f, 6000f)!!.speed, 0.01f)
    }

    @Test
    fun projectionRoundTrips() {
        val cup = CupPongPhysics.cups[5]
        val screen = camera.project(cup)
        val back = assertNotNull(camera.onTable(screen.first, screen.second))
        assertTrue(hypot(back.x - cup.x, back.y - cup.y) < 0.1f)
    }
    // A 16:9 phone (short canvas): the ball you flick must still be on screen.
    @Test
    fun restingBallIsOnAShortScreen() {
        val short = CupPongCamera(320f, 400f)
        val (_, y, depth) = short.project(CupPongPhysics.REST)
        val r = CupPongPhysics.BALL_RADIUS * short.focal / depth
        assertTrue(y + r <= 400f, "ball bottom at ${y + r}")
    }

    @Test
    fun tallScreenKeepsTheApprovedFraming() = assertEquals(2000f, CupPongCamera(1000f, 1700f).focal, 0.01f)

    @Test
    fun veryHardFlickIsAFullSpeedThrow() {
        val input = assertNotNull(flick(0f, -40_000f, 14_000f))
        assertEquals(CupPongPhysics.MAX_SPEED, input.speed, 0.01f)
        assertTrue(abs(input.yawDegrees) < 0.5f)
    }

    /** Spec 2026-11-26 §1: someone who swipes as hard as they can must still be making a throw that can score. */
    @Test
    fun aFullForceFlickCanStillScore() {
        val input = assertNotNull(flick(0f, -40_000f, 14_000f))
        assertNotNull(simulateThrow(input, CupPongPhysics.ALL_CUPS).cupHit, "a full-force flick cannot score")
    }

    /**
     * The gentlest flick the game accepts need not score — a feeble flick should fall short, that is what makes the
     * control honest — but it must arrive at the cups rather than die on the near half. Measured at where the ball
     * first comes down, not how far it ever travels: a miss rolls off the far end, which would pass from any speed.
     */
    @Test
    fun theGentlestFlickArrivesAtTheCups() {
        val input = assertNotNull(flick(0f, -2000f, 300f))
        val landing = simulateThrow(input, CupPongPhysics.ALL_CUPS).points()
            .first { it.y > 150f && it.z <= CupPongPhysics.BALL_RADIUS + 0.2f }
        assertTrue(landing.y > 200f, "the gentlest flick lands at ${landing.y}, short of the front cup")
    }
}