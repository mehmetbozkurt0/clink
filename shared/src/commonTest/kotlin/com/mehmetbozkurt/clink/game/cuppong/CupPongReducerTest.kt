package com.mehmetbozkurt.clink.game.cuppong

import com.mehmetbozkurt.clink.content.Access
import com.mehmetbozkurt.clink.content.Card
import com.mehmetbozkurt.clink.content.CardType
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.content.Pack
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class CupPongReducerTest {
    private val players = listOf("Ayşe", "Mert")
    private val all = CupPongPhysics.ALL_CUPS

    private val withPenalty = ContentLibrary(
        listOf(Pack("p", CardType.PENALTY, Level.CASUAL, Access.FREE, listOf(Card("p1", mapOf("en" to "p1"))))),
    )
    private val noPenalty = ContentLibrary(emptyList())

    private val start = initialCupPongState(players, Level.CASUAL, withPenalty)

    private fun CupPongState.after(vararg actions: CpAction) =
        actions.fold(this) { state, action -> CupPongReducer.reduce(state, action, Random(0)) }

    @Test
    fun hitRemovesDefendersCupAndPassesTurn() {
        val state = start.after(CpAction.Throw(aimAt(CupPongPhysics.cups[5])))
        assertEquals(all - 5, state.cups[1])
        assertEquals(all, state.cups[0])
        assertEquals(1, state.thrower)
        assertEquals(1, state.throwCount)
        assertEquals(5, state.lastThrow!!.cupHit)
    }

    @Test
    fun missKeepsCupsAndPassesTurn() {
        val state = start.after(CpAction.Throw(ThrowInput(0f, 300f)))
        assertEquals(listOf(all, all), state.cups)
        assertEquals(1, state.thrower)
        assertEquals(1, state.throwCount)
    }

    @Test
    fun clearingTheLastCupEndsTheMatch() {
        val state = start.copy(cups = listOf(all, setOf(5))).after(CpAction.Throw(aimAt(CupPongPhysics.cups[5])))
        assertEquals(CpPhase.MatchOver(winner = 0, loser = 1, penaltyCardId = "p1"), state.phase)
        assertEquals(emptySet(), state.cups[1])
    }

    @Test
    fun emptyPenaltyPoolStillEndsTheMatch() {
        val state = initialCupPongState(players, Level.CASUAL, noPenalty)
            .copy(cups = listOf(all, setOf(5)))
            .after(CpAction.Throw(aimAt(CupPongPhysics.cups[5])))
        assertEquals(CpPhase.MatchOver(winner = 0, loser = 1, penaltyCardId = null), state.phase)
        assertEquals(CpNotice.NO_PENALTY_CARDS, state.notice)
    }

    @Test
    fun throwsAfterTheMatchAreIgnored() {
        val over = start.copy(cups = listOf(all, setOf(5))).after(CpAction.Throw(aimAt(CupPongPhysics.cups[5])))
        assertEquals(over, over.after(CpAction.Throw(aimAt(CupPongPhysics.cups[1]))))
    }

    @Test
    fun secondPlayerCanWin() {
        val state = start.copy(thrower = 1, cups = listOf(setOf(1), all)).after(CpAction.Throw(aimAt(CupPongPhysics.cups[1])))
        assertEquals(CpPhase.MatchOver(winner = 1, loser = 0, penaltyCardId = "p1"), state.phase)
    }
    // A member's phone can send anything online: a broken number is ignored, an extreme one is held to the limits.
    @Test
    fun nonFiniteThrowIsIgnored() {
        assertEquals(start, start.after(CpAction.Throw(ThrowInput(Float.NaN, 350f))))
        assertEquals(start, start.after(CpAction.Throw(ThrowInput(0f, Float.POSITIVE_INFINITY))))
    }

    @Test
    fun outOfRangeThrowIsClamped() {
        assertEquals(ThrowInput(45f, 400f), start.after(CpAction.Throw(ThrowInput(80f, 5000f))).lastThrow!!.input)
    }
}