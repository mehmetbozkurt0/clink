package com.mehmetbozkurt.clink.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameIdTest {
    @Test
    fun theNewGameNeedsThreePlayersAndPlaysOnline() {
        assertEquals(3, GameId.MOST_LIKELY_TO.minPlayers)
        assertTrue(GameId.MOST_LIKELY_TO.playsOnline)
    }

    @Test
    fun theOtherGamesAreUnchanged() {
        listOf(GameId.TRUTH_OR_DARE, GameId.SPIN_THE_BOTTLE, GameId.CUP_PONG).forEach {
            assertEquals(2, it.minPlayers)
            assertTrue(it.playsOnline)
        }
    }
}
