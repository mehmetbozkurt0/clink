package com.mehmetbozkurt.clink.game.spinthebottle

import kotlinx.serialization.Serializable
import com.mehmetbozkurt.clink.content.CardType
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Deck
import com.mehmetbozkurt.clink.content.Level

/**
 * The UI animates the bottle toward [lastAngle] (or [lastTarget]'s centre, for states from before stop angles)
 * whenever [spinCount] changes. [lastTarget] is the player whose sector [lastAngle] falls into.
 */
@Serializable
data class SpinState(
    val players: List<String>,
    val level: Level,
    val spinner: Int,
    val phase: SpinPhase,
    val penaltyDeck: Deck,
    val spinCount: Int,
    val lastTarget: Int?,
    val notice: SpinNotice?,
    val lastAngle: Float? = null,
)

@Serializable
sealed interface SpinPhase {
    @Serializable
    data class ReadyToSpin(val landedOnSelf: Boolean) : SpinPhase
    @Serializable
    data class Asking(val asker: Int, val answerer: Int) : SpinPhase
    @Serializable
    data class ShowingPenalty(val answerer: Int, val cardId: String) : SpinPhase
}

@Serializable
enum class SpinNotice { NO_PENALTY_CARDS }

@Serializable
sealed interface SpinAction {
    @Serializable
    data object Spin : SpinAction
    @Serializable
    data object Answered : SpinAction
    @Serializable
    data object Refuse : SpinAction
    @Serializable
    data object PenaltyDone : SpinAction
}

/** Where the bottle points after the last spin; states from before stop angles point at the picked player's centre. */
fun SpinState.stopAngle(): Float? = lastAngle ?: lastTarget?.let { playerAngleDegrees(it, players.size) }

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
