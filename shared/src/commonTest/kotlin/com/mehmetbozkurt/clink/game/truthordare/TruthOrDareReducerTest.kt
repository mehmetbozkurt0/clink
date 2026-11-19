package com.mehmetbozkurt.clink.game.truthordare

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

class TruthOrDareReducerTest {
    private val players = listOf("Ayşe", "Mert", "Can")

    private fun pack(id: String, type: CardType, level: Level, vararg cardIds: String) =
        Pack(id, type, level, Access.FREE, cardIds.map { Card(it, mapOf("en" to it)) })

    private val casual = ContentLibrary(
        listOf(
            pack("t", CardType.TRUTH, Level.CASUAL, "t1", "t2"),
            pack("d", CardType.DARE, Level.CASUAL, "d1"),
            pack("p", CardType.PENALTY, Level.CASUAL, "p1"),
        ),
    )

    /** Spicy truths only: no Spicy dares, no Spicy penalties. */
    private val spicyTruthsOnly = ContentLibrary(listOf(pack("ts", CardType.TRUTH, Level.SPICY, "s1")))

    private fun TruthOrDareState.after(vararg actions: TodAction, random: Random = Random(0)) =
        actions.fold(this) { state, action -> TruthOrDareReducer.reduce(state, action, random) }

    private val start = initialTruthOrDareState(players, Level.CASUAL, casual)

    @Test
    fun pickTruth_showsTruthCardForCurrentPlayer() {
        val state = start.after(TodAction.Pick(CardType.TRUTH), random = SequenceRandom(0))
        assertEquals(TodPhase.ShowingCard(CardType.TRUTH, "t1"), state.phase)
        assertEquals(0, state.currentPlayer)
    }

    @Test
    fun done_movesToNextPlayerAndChoosing() {
        val state = start.after(TodAction.Pick(CardType.DARE), TodAction.Done)
        assertEquals(1, state.currentPlayer)
        assertEquals(TodPhase.Choosing, state.phase)
    }

    @Test
    fun refuse_showsPenalty_thenPenaltyDoneAdvances() {
        val refused = start.after(TodAction.Pick(CardType.DARE), TodAction.Refuse)
        assertEquals(TodPhase.ShowingPenalty("p1"), refused.phase)
        assertEquals(0, refused.currentPlayer)

        val done = refused.after(TodAction.PenaltyDone)
        assertEquals(1, done.currentPlayer)
        assertEquals(TodPhase.Choosing, done.phase)
    }

    @Test
    fun turnWrapsAfterLastPlayer() {
        val turn = arrayOf(TodAction.Pick(CardType.TRUTH), TodAction.Done)
        val state = start.after(*turn, *turn, *turn)
        assertEquals(0, state.currentPlayer)
        assertEquals(TodPhase.Choosing, state.phase)
    }

    @Test
    fun pickWithEmptyDeck_staysChoosingWithNotice() {
        val state = initialTruthOrDareState(players, Level.SPICY, spicyTruthsOnly).after(TodAction.Pick(CardType.DARE))
        assertEquals(TodPhase.Choosing, state.phase)
        assertEquals(TodNotice.NO_DARE_CARDS, state.notice)
        assertEquals(0, state.currentPlayer)
    }

    @Test
    fun refuseWithEmptyPenaltyDeck_advancesWithNotice() {
        val state = initialTruthOrDareState(players, Level.SPICY, spicyTruthsOnly)
            .after(TodAction.Pick(CardType.TRUTH), TodAction.Refuse)
        assertEquals(1, state.currentPlayer)
        assertEquals(TodPhase.Choosing, state.phase)
        assertEquals(TodNotice.NO_PENALTY_CARDS, state.notice)
    }

    @Test
    fun actionsOutOfPhaseAreIgnored() {
        assertEquals(start, start.after(TodAction.Done))
        assertEquals(start, start.after(TodAction.Refuse))
        assertEquals(start, start.after(TodAction.PenaltyDone))

        val showing = start.after(TodAction.Pick(CardType.TRUTH))
        assertEquals(showing, showing.after(TodAction.Pick(CardType.DARE)))
        assertEquals(showing, showing.after(TodAction.PenaltyDone))
    }

    @Test
    fun pickPenaltyTypeIsIgnored() {
        assertEquals(start, start.after(TodAction.Pick(CardType.PENALTY)))
    }

    @Test
    fun nextSuccessfulActionClearsNotice() {
        val noticed = initialTruthOrDareState(players, Level.SPICY, spicyTruthsOnly).after(TodAction.Pick(CardType.DARE))
        assertEquals(TodNotice.NO_DARE_CARDS, noticed.notice)

        val picked = noticed.after(TodAction.Pick(CardType.TRUTH))
        assertEquals(TodPhase.ShowingCard(CardType.TRUTH, "s1"), picked.phase)
        assertNull(picked.notice)
    }
}
