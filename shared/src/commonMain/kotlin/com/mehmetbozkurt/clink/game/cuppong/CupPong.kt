package com.mehmetbozkurt.clink.game.cuppong

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
data class CupPongState(
    val players: List<String>,
    val level: Level,
    val thrower: Int,
    val cups: List<Set<Int>>,
    val phase: CpPhase,
    val throwCount: Int,
    val lastThrow: ThrowResult?,
    val penaltyDeck: Deck,
    val notice: CpNotice?,
)

@Serializable
sealed interface CpPhase {
    @Serializable
    data object Aiming : CpPhase
    @Serializable
    data class MatchOver(val winner: Int, val loser: Int, val penaltyCardId: String?) : CpPhase
}

@Serializable
enum class CpNotice { NO_PENALTY_CARDS }

@Serializable
sealed interface CpAction {
    @Serializable
    data class Throw(val input: ThrowInput) : CpAction
}

fun initialCupPongState(players: List<String>, level: Level, library: ContentLibrary) = CupPongState(
    players = players,
    level = level,
    thrower = 0,
    cups = listOf(CupPongPhysics.ALL_CUPS, CupPongPhysics.ALL_CUPS),
    phase = CpPhase.Aiming,
    throwCount = 0,
    lastThrow = null,
    penaltyDeck = Deck.of(library.cardIds(CardType.PENALTY, level)),
    notice = null,
)
