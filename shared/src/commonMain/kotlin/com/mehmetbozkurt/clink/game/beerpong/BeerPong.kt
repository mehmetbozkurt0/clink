package com.mehmetbozkurt.clink.game.beerpong

import kotlinx.serialization.Serializable
import com.mehmetbozkurt.clink.content.CardType
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Deck
import com.mehmetbozkurt.clink.content.Level

/**
 * One 1v1 match. [players] holds exactly the two match players (index 0 throws first);
 * [cups] are the cups each player still has, i.e. the ones the other player aims at.
 * The UI animates [lastThrow] whenever [throwCount] changes.
 */
@Serializable
data class BeerPongState(
    val players: List<String>,
    val level: Level,
    val thrower: Int,
    val cups: List<Set<Int>>,
    val phase: BpPhase,
    val throwCount: Int,
    val lastThrow: ThrowResult?,
    val penaltyDeck: Deck,
    val notice: BpNotice?,
)

@Serializable
sealed interface BpPhase {
    @Serializable
    data object Aiming : BpPhase
    @Serializable
    data class MatchOver(val winner: Int, val loser: Int, val penaltyCardId: String?) : BpPhase
}

@Serializable
enum class BpNotice { NO_PENALTY_CARDS }

@Serializable
sealed interface BpAction {
    @Serializable
    data class Throw(val input: ThrowInput) : BpAction
}

fun initialBeerPongState(players: List<String>, level: Level, library: ContentLibrary) = BeerPongState(
    players = players,
    level = level,
    thrower = 0,
    cups = listOf(BeerPongPhysics.ALL_CUPS, BeerPongPhysics.ALL_CUPS),
    phase = BpPhase.Aiming,
    throwCount = 0,
    lastThrow = null,
    penaltyDeck = Deck.of(library.cardIds(CardType.PENALTY, level)),
    notice = null,
)
