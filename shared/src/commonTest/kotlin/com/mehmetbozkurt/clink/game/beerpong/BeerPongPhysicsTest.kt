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

    @Test
    fun weakFlickIsNotAThrow() = assertNull(flickToThrow(0f, -299f))

    @Test
    fun downwardOrSidewaysFlickIsNotAThrow() {
        assertNull(flickToThrow(0f, 1000f))
        assertNull(flickToThrow(1000f, 0f))
    }

    @Test
    fun straightUpFlickAimsStraight() = assertEquals(0f, flickToThrow(0f, -1000f)!!.angleDegrees, 0.001f)

    @Test
    fun rangeFollowsSpeedAndIsClamped() {
        assertEquals(60f, flickToThrow(0f, -300f)!!.range, 0.01f)
        assertEquals(110f, flickToThrow(0f, -3000f)!!.range, 0.01f)
        assertEquals(110f, flickToThrow(0f, -6000f)!!.range, 0.01f)
    }

    @Test
    fun angleIsClamped() = assertEquals(-30f, flickToThrow(-1000f, -1000f)!!.angleDegrees, 0.001f)

    @Test
    fun nearlySidewaysFlickIsNotAThrow() {
        // A real sideways swipe always has a little upward speed; more than 60° off vertical is not a throw.
        assertNull(flickToThrow(1000f, -100f))
        assertNull(flickToThrow(-1000f, -500f))
    }
}
