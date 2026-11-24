package com.mehmetbozkurt.clink.game.beerpong

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BeerPongCameraTest {
    private val camera = BeerPongCamera(1000f, 1600f)
    private val ball = camera.project(BeerPongPhysics.REST)

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
        assertEquals(300f, flick(0f, -2000f, 300f)!!.speed, 0.01f)
        assertEquals(400f, flick(0f, -2000f, 3000f)!!.speed, 0.01f)
        assertEquals(400f, flick(0f, -2000f, 6000f)!!.speed, 0.01f)
    }

    @Test
    fun projectionRoundTrips() {
        val cup = BeerPongPhysics.cups[5]
        val screen = camera.project(cup)
        val back = assertNotNull(camera.onTable(screen.first, screen.second))
        assertTrue(hypot(back.x - cup.x, back.y - cup.y) < 0.1f)
    }
    // A 16:9 phone (short canvas): the ball you flick must still be on screen.
    @Test
    fun restingBallIsOnAShortScreen() {
        val short = BeerPongCamera(320f, 400f)
        val (_, y, depth) = short.project(BeerPongPhysics.REST)
        val r = BeerPongPhysics.BALL_RADIUS * short.focal / depth
        assertTrue(y + r <= 400f, "ball bottom at ${y + r}")
    }

    @Test
    fun tallScreenKeepsTheApprovedFraming() = assertEquals(2000f, BeerPongCamera(1000f, 1700f).focal, 0.01f)

    @Test
    fun veryHardFlickIsAFullSpeedThrow() {
        val input = assertNotNull(flick(0f, -40_000f, 14_000f))
        assertEquals(400f, input.speed, 0.01f)
        assertTrue(abs(input.yawDegrees) < 0.5f)
    }
}