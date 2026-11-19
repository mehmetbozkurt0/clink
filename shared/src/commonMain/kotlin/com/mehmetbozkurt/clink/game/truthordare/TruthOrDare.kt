package com.mehmetbozkurt.clink.game.truthordare

import com.mehmetbozkurt.clink.content.CardType
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Deck
import com.mehmetbozkurt.clink.content.Level

data class TruthOrDareState(
    val players: List<String>,
    val level: Level,
    val currentPlayer: Int,
    val phase: TodPhase,
    val truthDeck: Deck,
    val dareDeck: Deck,
    val penaltyDeck: Deck,
    val notice: TodNotice?,
)

sealed interface TodPhase {
    data object Choosing : TodPhase
    data class ShowingCard(val type: CardType, val cardId: String) : TodPhase
    data class ShowingPenalty(val cardId: String) : TodPhase
}

/** Something the players should be told, e.g. the chosen level has no cards of a type. */
enum class TodNotice { NO_TRUTH_CARDS, NO_DARE_CARDS, NO_PENALTY_CARDS }

sealed interface TodAction {
    data class Pick(val type: CardType) : TodAction
    data object Done : TodAction
    data object Refuse : TodAction
    data object PenaltyDone : TodAction
}

fun initialTruthOrDareState(players: List<String>, level: Level, library: ContentLibrary) = TruthOrDareState(
    players = players,
    level = level,
    currentPlayer = 0,
    phase = TodPhase.Choosing,
    truthDeck = Deck.of(library.cardIds(CardType.TRUTH, level)),
    dareDeck = Deck.of(library.cardIds(CardType.DARE, level)),
    penaltyDeck = Deck.of(library.cardIds(CardType.PENALTY, level)),
    notice = null,
)
