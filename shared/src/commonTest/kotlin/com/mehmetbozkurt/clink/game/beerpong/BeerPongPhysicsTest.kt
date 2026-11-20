package com.mehmetbozkurt.clink.game.beerpong

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A throw that lands exactly at (x, y) on the table. */
internal fun at(x: Float, y: Float) = ThrowInput(atan2(x, y) * 180f / PI.toFloat(), hypot(x, y))

class BeerPongPhysicsTest {
    private val all = BeerPongPhysics.ALL_CUPS

    private fun TablePoint.rounded() = TablePoint((x * 100).roundToInt() / 100f, (y * 100).roundToInt() / 100f)

    @Test
    fun straightIntoBackMiddleCup() = assertEquals(1, simulateThrow(at(0f, 90f), all).cupHit)

    @Test
    fun straightIntoFrontCup() = assertEquals(5, simulateThrow(at(0f, 79.2f), all).cupHit)

    @Test
    fun angledIntoBackLeftCup() = assertEquals(0, simulateThrow(at(-6.2f, 90f), all).cupHit)

    @Test
    fun justInsideInnerEdgeIsIn() = assertEquals(5, simulateThrow(at(0f, 79.2f - 1.99f), all).cupHit)

    @Test
    fun justOutsideInnerEdgeHitsTheRim() = assertEquals(2, simulateThrow(at(0f, 79.2f - 2.01f), all).touchdowns.size)

    @Test
    fun rimBounceOutIsAMiss() {
        val r = simulateThrow(at(0f, 76.2f), all) // 3 from cup 5, bounces back toward the thrower
        assertEquals(listOf(TablePoint(0f, 76.2f), TablePoint(0f, 72.2f)), r.touchdowns.map { it.rounded() })
        assertNull(r.cupHit)
    }

    @Test
    fun rimBounceCanDropIntoTheNeighbour() {
        val r = simulateThrow(at(0f, 84.6f), all) // 3.1 from cups 3 and 4: tie → cup 3's rim, bounces right into cup 4
        assertEquals(4, r.cupHit)
        assertEquals(2, r.touchdowns.size)
    }

    @Test
    fun removedCupsAreIgnored() = assertNull(simulateThrow(at(0f, 90f), all - 1).cupHit)

    @Test
    fun shortThrowMisses() {
        val r = simulateThrow(at(0f, 60f), all)
        assertNull(r.cupHit)
        assertEquals(1, r.touchdowns.size)
    }

    /** A flick whose whole drag points [degrees] off vertical (positive = right), released at [speed] dp/s. */
    private fun flick(degrees: Float, speed: Float = 1000f): ThrowInput? {
        val radians = degrees * PI.toFloat() / 180f
        return flickToThrow(dragDx = kotlin.math.sin(radians) * 100f, dragDy = -kotlin.math.cos(radians) * 100f, speedDpPerSec = speed)
    }

    @Test
    fun weakFlickIsNotAThrow() = assertNull(flick(0f, speed = 299f))

    @Test
    fun downwardOrSidewaysFlickIsNotAThrow() {
        assertNull(flickToThrow(dragDx = 0f, dragDy = 100f, speedDpPerSec = 1000f))
        assertNull(flickToThrow(dragDx = 100f, dragDy = 0f, speedDpPerSec = 1000f))
    }

    @Test
    fun nearlySidewaysFlickIsNotAThrow() {
        // A real sideways swipe always has a little upward movement; more than 60° off vertical is not a throw.
        assertNull(flick(61f))
        assertNull(flick(-75f))
    }

    @Test
    fun straightUpFlickAimsStraight() = assertEquals(0f, flick(0f)!!.angleDegrees, 0.001f)

    @Test
    fun aimIsDampenedSoAWobblyFingerStillHits() {
        assertEquals(5f, flick(20f)!!.angleDegrees, 0.01f)
        assertEquals(-15f, flick(-60f)!!.angleDegrees, 0.01f)
    }

    @Test
    fun rangeFollowsSpeedAndIsClamped() {
        assertEquals(70f, flick(0f, speed = 300f)!!.range, 0.01f)
        assertEquals(100f, flick(0f, speed = 3500f)!!.range, 0.01f)
        assertEquals(100f, flick(0f, speed = 7000f)!!.range, 0.01f)
    }

    @Test
    fun everyCupIsReachable() {
        // The widest cups (back corners) need about 16° of drag, well inside the 60° limit; the back row's range fits too.
        assertEquals(0, simulateThrow(flick(-15.76f, speed = 300f + (hypot(6.2f, 90f) - 70f) / 30f * 3200f)!!, all).cupHit)
    }
}
