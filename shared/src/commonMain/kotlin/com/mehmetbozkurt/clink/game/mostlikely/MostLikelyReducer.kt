package com.mehmetbozkurt.clink.game.mostlikely

import com.mehmetbozkurt.clink.core.mvi.Reducer
import kotlin.random.Random

/**
 * ShowingPrompt --Pick(i)------> ShowingPenalty   picks[i] + 1
 * ShowingPrompt --Pass---------> ShowingPrompt    (the next prompt, picks untouched)
 * ShowingPenalty --WrongPerson-> ShowingPrompt    (the same prompt, picks[picked] - 1)
 * ShowingPenalty --PenaltyDone-> ShowingPrompt    (the next prompt)
 * any --End-------------------> Summary
 *
 * Actions that don't fit the current phase leave the state unchanged. A penalty card drawn for a pick is not put
 * back by WrongPerson: the deck reshuffles anyway (spec §12.8).
 */
object MostLikelyReducer : Reducer<MostLikelyState, MlAction> {

    override fun reduce(state: MostLikelyState, action: MlAction, random: Random): MostLikelyState =
        when (action) {
            is MlAction.Pick -> if (state.phase is MlPhase.ShowingPrompt) pick(state, state.phase, action.player, random) else state
            MlAction.Pass -> if (state.phase is MlPhase.ShowingPrompt) state.nextPrompt(random) else state
            MlAction.WrongPerson -> if (state.phase is MlPhase.ShowingPenalty) undo(state, state.phase) else state
            MlAction.PenaltyDone -> if (state.phase is MlPhase.ShowingPenalty) state.nextPrompt(random) else state
            MlAction.End -> state.copy(phase = MlPhase.Summary)
        }

    private fun pick(state: MostLikelyState, phase: MlPhase.ShowingPrompt, player: Int, random: Random): MostLikelyState {
        if (player !in state.players.indices) return state
        val drawn = state.penaltyDeck.draw(random)
        return state.copy(
            phase = MlPhase.ShowingPenalty(player, phase.cardId, drawn?.first),
            penaltyDeck = drawn?.second ?: state.penaltyDeck,
            picks = state.picks.mapIndexed { i, count -> if (i == player) count + 1 else count },
        )
    }

    private fun undo(state: MostLikelyState, phase: MlPhase.ShowingPenalty): MostLikelyState = state.copy(
        phase = MlPhase.ShowingPrompt(phase.promptId),
        picks = state.picks.mapIndexed { i, count -> if (i == phase.picked) (count - 1).coerceAtLeast(0) else count },
    )

    /** Keeps the current phase when the level has no prompts, which is only reachable before the first round. */
    private fun MostLikelyState.nextPrompt(random: Random): MostLikelyState {
        val (cardId, rest) = promptDeck.draw(random) ?: return this
        return copy(phase = MlPhase.ShowingPrompt(cardId), promptDeck = rest)
    }
}
