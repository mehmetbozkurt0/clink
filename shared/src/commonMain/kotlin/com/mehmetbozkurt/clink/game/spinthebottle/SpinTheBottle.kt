package com.mehmetbozkurt.clink.game.spinthebottle

import com.mehmetbozkurt.clink.content.CardType
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Deck
import com.mehmetbozkurt.clink.content.Level

/** The UI animates the bottle toward [lastTarget] whenever [spinCount] changes. */
data class SpinState(
    val players: List<String>,
    val level: Level,
    val spinner: Int,
    val phase: SpinPhase,
    val penaltyDeck: Deck,
    val spinCount: Int,
    val lastTarget: Int?,
    val notice: SpinNotice?,
)

sealed interface SpinPhase {
    data class ReadyToSpin(val landedOnSelf: Boolean) : SpinPhase
    data class Asking(val asker: Int, val answerer: Int) : SpinPhase
    data class ShowingPenalty(val answerer: Int, val cardId: String) : SpinPhase
}

enum class SpinNotice { NO_PENALTY_CARDS }

sealed interface SpinAction {
    data object Spin : SpinAction
    data object Answered : SpinAction
    data object Refuse : SpinAction
    data object PenaltyDone : SpinAction
}

fun initialSpinState(players: List<String>, level: Level, library: ContentLibrary) = SpinState(
    players = players,
    level = level,
    spinner = 0,
    phase = SpinPhase.ReadyToSpin(landedOnSelf = false),
    penaltyDeck = Deck.of(library.cardIds(CardType.PENALTY, level)),
    spinCount = 0,
    lastTarget = null,
    notice = null,
)
