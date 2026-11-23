package com.mehmetbozkurt.clink.ui.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TourStateTest {
    @Test
    fun nextMovesOnAndCompletesOnLastPage() {
        val last = TourState(0, 3).next().next()
        assertEquals(2, last.page)
        assertNull(last.exit)
        assertEquals(TourExit.COMPLETED, last.next().exit)
    }

    @Test
    fun backOnFirstCardLeaves() {
        assertEquals(TourExit.BACKED_OUT, TourState(0, 4).back().exit)
    }

    @Test
    fun backGoesToPreviousCard() {
        val state = TourState(2, 4).back()
        assertEquals(1, state.page)
        assertNull(state.exit)
    }

    @Test
    fun skipEndsFromAnyCard() {
        assertEquals(TourExit.SKIPPED, TourState(1, 3).skip().exit)
    }

    @Test
    fun exitIsFinal() {
        assertEquals(TourExit.SKIPPED, TourState(0, 3).skip().back().exit)
        assertEquals(TourExit.COMPLETED, TourState(2, 3).next().skip().exit)
        assertEquals(0, TourState(0, 3).skip().goTo(2).page)
    }

    @Test
    fun goToIsClamped() {
        assertEquals(2, TourState(0, 3).goTo(9).page)
    }
}
