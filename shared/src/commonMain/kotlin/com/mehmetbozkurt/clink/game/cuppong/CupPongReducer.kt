package com.mehmetbozkurt.clink.game.cuppong

import com.mehmetbozkurt.clink.core.mvi.Reducer
import kotlin.random.Random

/**
 * Aiming --Throw--> the defender loses the cup that was hit; the turn passes to the defender,
 * or the match is over when the defender has no cups left (the loser draws a penalty).
 * Throws after the match are ignored.
 */
object CupPongReducer : Reducer<CupPongState, CpAction> {

    override fun reduce(state: CupPongState, action: CpAction, random: Random): CupPongState = when (action) {
        is CpAction.Throw -> {
            val input = action.input.sanitized()
            if (state.phase == CpPhase.Aiming && input != null) throwBall(state, input, random) else state
        }
    }

    private fun throwBall(state: CupPongState, input: ThrowInput, random: Random): CupPongState {
        val defender = 1 - state.thrower
        val result = simulateThrow(input, state.cups[defender])
        val defenderCups = state.cups[defender] - setOfNotNull(result.cupHit)
        val thrown = state.copy(
            cups = state.cups.mapIndexed { player, cups -> if (player == defender) defenderCups else cups },
            throwCount = state.throwCount + 1,
            lastThrow = result,
            notice = null,
        )
        if (defenderCups.isNotEmpty()) return thrown.copy(thrower = defender)

        val penalty = state.penaltyDeck.draw(random)
        return thrown.copy(
            phase = CpPhase.MatchOver(winner = state.thrower, loser = defender, penaltyCardId = penalty?.first),
            penaltyDeck = penalty?.second ?: state.penaltyDeck,
            notice = if (penalty == null) CpNotice.NO_PENALTY_CARDS else null,
        )
    }
}
