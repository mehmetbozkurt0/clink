package com.mehmetbozkurt.clink.game.spinthebottle

import com.mehmetbozkurt.clink.core.mvi.Reducer
import kotlin.random.Random

/**
 * ReadyToSpin --Spin--> Asking(spinner, target), or ReadyToSpin(landedOnSelf = true) if it lands on the spinner.
 * Asking --Answered--> answerer spins next; Asking --Refuse--> ShowingPenalty --PenaltyDone--> answerer spins next.
 * Actions that don't fit the current phase leave the state unchanged.
 */
object SpinTheBottleReducer : Reducer<SpinState, SpinAction> {

    override fun reduce(state: SpinState, action: SpinAction, random: Random): SpinState {
        val phase = state.phase
        return when (action) {
            SpinAction.Spin -> if (phase is SpinPhase.ReadyToSpin) spin(state, random) else state
            SpinAction.Answered -> if (phase is SpinPhase.Asking) state.answererSpins(phase.answerer) else state
            SpinAction.Refuse -> if (phase is SpinPhase.Asking) refuse(state, phase.answerer, random) else state
            SpinAction.PenaltyDone -> if (phase is SpinPhase.ShowingPenalty) state.answererSpins(phase.answerer) else state
        }
    }

    private fun spin(state: SpinState, random: Random): SpinState {
        val target = random.nextInt(state.players.size)
        val phase = if (target == state.spinner) SpinPhase.ReadyToSpin(landedOnSelf = true) else SpinPhase.Asking(state.spinner, target)
        return state.copy(phase = phase, spinCount = state.spinCount + 1, lastTarget = target, notice = null)
    }

    private fun refuse(state: SpinState, answerer: Int, random: Random): SpinState {
        val (cardId, rest) = state.penaltyDeck.draw(random)
            ?: return state.answererSpins(answerer).copy(notice = SpinNotice.NO_PENALTY_CARDS)
        return state.copy(phase = SpinPhase.ShowingPenalty(answerer, cardId), penaltyDeck = rest, notice = null)
    }

    private fun SpinState.answererSpins(answerer: Int) =
        copy(spinner = answerer, phase = SpinPhase.ReadyToSpin(landedOnSelf = false), notice = null)
}
