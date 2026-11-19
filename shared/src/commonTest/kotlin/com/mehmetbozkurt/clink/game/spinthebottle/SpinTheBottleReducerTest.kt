package com.mehmetbozkurt.clink.game.spinthebottle

import com.mehmetbozkurt.clink.content.Access
import com.mehmetbozkurt.clink.content.Card
import com.mehmetbozkurt.clink.content.CardType
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.content.Pack
import com.mehmetbozkurt.clink.testing.SequenceRandom
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpinTheBottleReducerTest {
    private val players = listOf("Ayşe", "Mert", "Can", "Deniz")

    private val withPenalty = ContentLibrary(
        listOf(Pack("p", CardType.PENALTY, Level.CASUAL, Access.FREE, listOf(Card("p1", mapOf("en" to "p1"))))),
    )
    private val noPenalty = ContentLibrary(emptyList())

    private fun SpinState.after(random: Random, vararg actions: SpinAction) =
        actions.fold(this) { state, action -> SpinTheBottleReducer.reduce(state, action, random) }

    private val start = initialSpinState(players, Level.CASUAL, withPenalty)

    @Test
    fun spin_landsOnOtherPlayer_asksThem() {
        val state = start.after(SequenceRandom(2), SpinAction.Spin)
        assertEquals(SpinPhase.Asking(0, 2), state.phase)
        assertEquals(1, state.spinCount)
        assertEquals(2, state.lastTarget)
    }

    @Test
    fun spin_landsOnSpinner_spinsAgain() {
        val state = start.after(SequenceRandom(0), SpinAction.Spin)
        assertEquals(SpinPhase.ReadyToSpin(landedOnSelf = true), state.phase)
        assertEquals(0, state.spinner)
        assertEquals(1, state.spinCount)
    }

    @Test
    fun answered_answererBecomesSpinner() {
        val state = start.after(SequenceRandom(2), SpinAction.Spin, SpinAction.Answered)
        assertEquals(2, state.spinner)
        assertEquals(SpinPhase.ReadyToSpin(landedOnSelf = false), state.phase)
    }

    @Test
    fun refuse_showsPenalty_thenAnswererSpins() {
        val refused = start.after(SequenceRandom(2, 0), SpinAction.Spin, SpinAction.Refuse)
        assertEquals(SpinPhase.ShowingPenalty(2, "p1"), refused.phase)

        val done = refused.after(Random(0), SpinAction.PenaltyDone)
        assertEquals(2, done.spinner)
        assertEquals(SpinPhase.ReadyToSpin(landedOnSelf = false), done.phase)
    }

    @Test
    fun refuseWithEmptyPenaltyDeck_actsLikeAnswered() {
        val state = initialSpinState(players, Level.CASUAL, noPenalty)
            .after(SequenceRandom(2), SpinAction.Spin, SpinAction.Refuse)
        assertEquals(2, state.spinner)
        assertEquals(SpinPhase.ReadyToSpin(landedOnSelf = false), state.phase)
        assertEquals(SpinNotice.NO_PENALTY_CARDS, state.notice)
    }

    @Test
    fun nextSpinClearsNotice() {
        val state = initialSpinState(players, Level.CASUAL, noPenalty)
            .after(SequenceRandom(2, 1), SpinAction.Spin, SpinAction.Refuse, SpinAction.Spin)
        assertEquals(SpinPhase.Asking(2, 1), state.phase)
        assertNull(state.notice)
    }

    @Test
    fun twoPlayers_alternateAndNeverStick() {
        val random = SequenceRandom(0, 1, 1, 0)
        var state = initialSpinState(listOf("A", "B"), Level.CASUAL, withPenalty)

        state = state.after(random, SpinAction.Spin)
        assertEquals(SpinPhase.ReadyToSpin(landedOnSelf = true), state.phase)
        state = state.after(random, SpinAction.Spin)
        assertEquals(SpinPhase.Asking(0, 1), state.phase)
        state = state.after(random, SpinAction.Answered)
        assertEquals(1, state.spinner)
        state = state.after(random, SpinAction.Spin)
        assertEquals(SpinPhase.ReadyToSpin(landedOnSelf = true), state.phase)
        state = state.after(random, SpinAction.Spin)
        assertEquals(SpinPhase.Asking(1, 0), state.phase)
        assertEquals(4, state.spinCount)
    }

    @Test
    fun actionsOutOfPhaseAreIgnored() {
        assertEquals(start, start.after(Random(0), SpinAction.Answered))
        assertEquals(start, start.after(Random(0), SpinAction.Refuse))
        assertEquals(start, start.after(Random(0), SpinAction.PenaltyDone))

        val asking = start.after(SequenceRandom(2), SpinAction.Spin)
        assertEquals(asking, asking.after(Random(0), SpinAction.Spin))
        assertEquals(asking, asking.after(Random(0), SpinAction.PenaltyDone))
    }
}
