package com.mehmetbozkurt.clink.game.truthordare

import com.mehmetbozkurt.clink.content.CardType
import com.mehmetbozkurt.clink.core.mvi.Reducer
import kotlin.random.Random

/**
 * Choosing --Pick(TRUTH|DARE)--> ShowingCard --Done--> next player
 *                                ShowingCard --Refuse--> ShowingPenalty --PenaltyDone--> next player
 * Actions that don't fit the current phase leave the state unchanged.
 */
object TruthOrDareReducer : Reducer<TruthOrDareState, TodAction> {

    override fun reduce(state: TruthOrDareState, action: TodAction, random: Random): TruthOrDareState =
        when (action) {
            is TodAction.Pick -> if (state.phase == TodPhase.Choosing) pick(state, action.type, random) else state
            TodAction.Done -> if (state.phase is TodPhase.ShowingCard) state.nextTurn() else state
            TodAction.Refuse -> if (state.phase is TodPhase.ShowingCard) refuse(state, random) else state
            TodAction.PenaltyDone -> if (state.phase is TodPhase.ShowingPenalty) state.nextTurn() else state
        }

    private fun pick(state: TruthOrDareState, type: CardType, random: Random): TruthOrDareState {
        val deck = when (type) {
            CardType.TRUTH -> state.truthDeck
            CardType.DARE -> state.dareDeck
            // Truth or Dare only draws these two; any other type is not an action this game can take.
            CardType.PENALTY, CardType.MOST_LIKELY -> return state
        }
        val (cardId, rest) = deck.draw(random)
            ?: return state.copy(notice = if (type == CardType.TRUTH) TodNotice.NO_TRUTH_CARDS else TodNotice.NO_DARE_CARDS)
        val shown = state.copy(phase = TodPhase.ShowingCard(type, cardId), notice = null)
        return if (type == CardType.TRUTH) shown.copy(truthDeck = rest) else shown.copy(dareDeck = rest)
    }

    private fun refuse(state: TruthOrDareState, random: Random): TruthOrDareState {
        val (cardId, rest) = state.penaltyDeck.draw(random)
            ?: return state.nextTurn().copy(notice = TodNotice.NO_PENALTY_CARDS)
        return state.copy(phase = TodPhase.ShowingPenalty(cardId), penaltyDeck = rest, notice = null)
    }

    private fun TruthOrDareState.nextTurn() =
        copy(currentPlayer = (currentPlayer + 1) % players.size, phase = TodPhase.Choosing, notice = null)
}
