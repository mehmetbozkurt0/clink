package com.mehmetbozkurt.clink.game.beerpong

import kotlin.test.Test
import kotlin.test.assertEquals

class BeerPongMatchupsTest {
    @Test
    fun firstMatchIsTheFirstTwoPlayers() = assertEquals(Matchup(0, 1), suggestedMatchup(5, null, null))

    @Test
    fun twoPlayersRematchSwapsTheFirstThrow() {
        assertEquals(Matchup(1, 0), suggestedMatchup(2, previousWinner = 0, previousLoser = 1))
        assertEquals(Matchup(0, 1), suggestedMatchup(2, previousWinner = 1, previousLoser = 0))
    }

    @Test
    fun winnerStaysOnAndTheNextPlayerChallenges() =
        assertEquals(Matchup(2, 0), suggestedMatchup(4, previousWinner = 0, previousLoser = 1))

    @Test
    fun challengerWrapsAndSkipsTheWinner() =
        assertEquals(Matchup(1, 0), suggestedMatchup(4, previousWinner = 0, previousLoser = 3))

    @Test
    fun challengerSkipsTheWinnerInTheMiddle() =
        assertEquals(Matchup(0, 2), suggestedMatchup(3, previousWinner = 2, previousLoser = 1))

    @Test
    fun selectionPutsTheChallengerFirst() =
        assertEquals(Matchup(1, 3), matchupFromSelection(listOf(3, 1), previousWinner = 3))

    @Test
    fun selectionWithoutWinnerPutsLowerIndexFirst() =
        assertEquals(Matchup(1, 3), matchupFromSelection(listOf(3, 1), previousWinner = null))
}
