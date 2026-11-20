package com.mehmetbozkurt.clink.game.beerpong

import com.mehmetbozkurt.clink.content.Access
import com.mehmetbozkurt.clink.content.Card
import com.mehmetbozkurt.clink.content.CardType
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.content.Pack
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class BeerPongReducerTest {
    private val players = listOf("Ayşe", "Mert")
    private val all = BeerPongPhysics.ALL_CUPS

    private val withPenalty = ContentLibrary(
        listOf(Pack("p", CardType.PENALTY, Level.CASUAL, Access.FREE, listOf(Card("p1", mapOf("en" to "p1"))))),
    )
    private val noPenalty = ContentLibrary(emptyList())

    private val start = initialBeerPongState(players, Level.CASUAL, withPenalty)

    private fun BeerPongState.after(vararg actions: BpAction) =
        actions.fold(this) { state, action -> BeerPongReducer.reduce(state, action, Random(0)) }

    @Test
    fun hitRemovesDefendersCupAndPassesTurn() {
        val state = start.after(BpAction.Throw(at(0f, 90f)))
        assertEquals(all - 1, state.cups[1])
        assertEquals(all, state.cups[0])
        assertEquals(1, state.thrower)
        assertEquals(1, state.throwCount)
        assertEquals(1, state.lastThrow!!.cupHit)
    }

    @Test
    fun missKeepsCupsAndPassesTurn() {
        val state = start.after(BpAction.Throw(at(0f, 60f)))
        assertEquals(listOf(all, all), state.cups)
        assertEquals(1, state.thrower)
        assertEquals(1, state.throwCount)
    }

    @Test
    fun clearingTheLastCupEndsTheMatch() {
        val state = start.copy(cups = listOf(all, setOf(5))).after(BpAction.Throw(at(0f, 79.2f)))
        assertEquals(BpPhase.MatchOver(winner = 0, loser = 1, penaltyCardId = "p1"), state.phase)
        assertEquals(emptySet(), state.cups[1])
    }

    @Test
    fun emptyPenaltyPoolStillEndsTheMatch() {
        val state = initialBeerPongState(players, Level.CASUAL, noPenalty)
            .copy(cups = listOf(all, setOf(5)))
            .after(BpAction.Throw(at(0f, 79.2f)))
        assertEquals(BpPhase.MatchOver(winner = 0, loser = 1, penaltyCardId = null), state.phase)
        assertEquals(BpNotice.NO_PENALTY_CARDS, state.notice)
    }

    @Test
    fun throwsAfterTheMatchAreIgnored() {
        val over = start.copy(cups = listOf(all, setOf(5))).after(BpAction.Throw(at(0f, 79.2f)))
        assertEquals(over, over.after(BpAction.Throw(at(0f, 90f))))
    }

    @Test
    fun secondPlayerCanWin() {
        val state = start.copy(thrower = 1, cups = listOf(setOf(1), all)).after(BpAction.Throw(at(0f, 90f)))
        assertEquals(BpPhase.MatchOver(winner = 1, loser = 0, penaltyCardId = "p1"), state.phase)
    }
}
