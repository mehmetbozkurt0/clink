package com.mehmetbozkurt.clink.game.beerpong

import com.mehmetbozkurt.clink.core.mvi.Reducer
import kotlin.random.Random

/**
 * Aiming --Throw--> the defender loses the cup that was hit; the turn passes to the defender,
 * or the match is over when the defender has no cups left (the loser draws a penalty).
 * Throws after the match are ignored.
 */
object BeerPongReducer : Reducer<BeerPongState, BpAction> {

    override fun reduce(state: BeerPongState, action: BpAction, random: Random): BeerPongState = when (action) {
        is BpAction.Throw -> {
            val input = action.input.sanitized()
            if (state.phase == BpPhase.Aiming && input != null) throwBall(state, input, random) else state
        }
    }

    private fun throwBall(state: BeerPongState, input: ThrowInput, random: Random): BeerPongState {
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
            phase = BpPhase.MatchOver(winner = state.thrower, loser = defender, penaltyCardId = penalty?.first),
            penaltyDeck = penalty?.second ?: state.penaltyDeck,
            notice = if (penalty == null) BpNotice.NO_PENALTY_CARDS else null,
        )
    }
}
