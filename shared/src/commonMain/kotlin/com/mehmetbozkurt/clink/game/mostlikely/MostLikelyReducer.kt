package com.mehmetbozkurt.clink.game.mostlikely

import com.mehmetbozkurt.clink.core.mvi.Reducer
import kotlin.random.Random

/**
 * One phone (spec 2026-11-25 §7):
 *   ShowingPrompt --Pick(i)------> ShowingPenalty   picks[i] + 1
 *   ShowingPrompt --Pass---------> ShowingPrompt    (the next prompt, picks untouched)
 *   ShowingPenalty --WrongPerson-> ShowingPrompt    (the same prompt, picks[picked] - 1)
 *
 * Online (spec 2026-11-26 §6):
 *   Voting --Vote(v,t)----------> Voting            (slot v = t; reveals once every slot is filled)
 *   Voting --Reveal-------------> ShowingPenalty    (everyone on the most votes; no votes at all → the next prompt)
 *   Voting --Pass---------------> Voting            (the next prompt, every slot cleared)
 *
 * Both: ShowingPenalty --PenaltyDone--> the next round, and any --End--> Summary.
 * Actions that don't fit the current phase leave the state unchanged. A penalty card drawn for a pick is not put
 * back by WrongPerson: the deck reshuffles anyway (spec 2026-11-25 §12.8).
 */
object MostLikelyReducer : Reducer<MostLikelyState, MlAction> {

    override fun reduce(state: MostLikelyState, action: MlAction, random: Random): MostLikelyState =
        when (action) {
            is MlAction.Pick -> if (state.phase is MlPhase.ShowingPrompt) pick(state, state.phase, listOf(action.player), random) else state
            is MlAction.Vote -> if (state.phase is MlPhase.Voting) vote(state, state.phase, action, random) else state
            MlAction.Reveal -> if (state.phase is MlPhase.Voting) reveal(state, state.phase, random) else state
            MlAction.Pass -> if (state.phase is MlPhase.Voting || state.phase is MlPhase.ShowingPrompt) state.nextRound(random) else state
            MlAction.WrongPerson -> if (state.phase is MlPhase.ShowingPenalty) undo(state, state.phase) else state
            MlAction.PenaltyDone -> if (state.phase is MlPhase.ShowingPenalty) state.nextRound(random) else state
            MlAction.End -> state.copy(phase = MlPhase.Summary)
        }

    private fun vote(state: MostLikelyState, phase: MlPhase.Voting, action: MlAction.Vote, random: Random): MostLikelyState {
        if (action.promptId != phase.cardId) return state
        if (action.voter !in state.players.indices || action.target !in state.players.indices) return state
        val votes = phase.votes.mapIndexed { seat, vote -> if (seat == action.voter) action.target else vote }
        val filled = state.copy(phase = phase.copy(votes = votes))
        return if (votes.none { it == null }) reveal(filled, filled.phase as MlPhase.Voting, random) else filled
    }

    /** Everyone on the highest count takes the penalty; with no votes at all there is nothing to reveal. */
    private fun reveal(state: MostLikelyState, phase: MlPhase.Voting, random: Random): MostLikelyState {
        val counts = List(state.players.size) { seat -> phase.votes.count { it == seat } }
        val most = counts.max()
        if (most == 0) return state.nextRound(random)
        return pick(state, phase.cardId, counts.indices.filter { counts[it] == most }, random, counts)
    }

    private fun pick(
        state: MostLikelyState,
        phase: MlPhase.ShowingPrompt,
        picked: List<Int>,
        random: Random,
    ): MostLikelyState = pick(state, phase.cardId, picked, random, emptyList())

    private fun pick(
        state: MostLikelyState,
        promptId: String,
        picked: List<Int>,
        random: Random,
        counts: List<Int>,
    ): MostLikelyState {
        if (picked.any { it !in state.players.indices }) return state
        val drawn = state.penaltyDeck.draw(random)
        return state.copy(
            phase = MlPhase.ShowingPenalty(picked, promptId, drawn?.first, counts),
            penaltyDeck = drawn?.second ?: state.penaltyDeck,
            picks = state.picks.mapIndexed { i, count -> if (i in picked) count + 1 else count },
        )
    }

    private fun undo(state: MostLikelyState, phase: MlPhase.ShowingPenalty): MostLikelyState = state.copy(
        phase = MlPhase.ShowingPrompt(phase.promptId),
        picks = state.picks.mapIndexed { i, count -> if (i in phase.picked) (count - 1).coerceAtLeast(0) else count },
    )

    /** The next prompt, in whichever shape this session uses. Keeps the phase when the level has no prompts. */
    private fun MostLikelyState.nextRound(random: Random): MostLikelyState {
        val (cardId, rest) = promptDeck.draw(random) ?: return this
        val next = if (voting) MlPhase.Voting(cardId, List(players.size) { null }) else MlPhase.ShowingPrompt(cardId)
        return copy(phase = next, promptDeck = rest)
    }
}
