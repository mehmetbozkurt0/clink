package com.mehmetbozkurt.clink.game.truthordare

import kotlinx.serialization.Serializable
import com.mehmetbozkurt.clink.content.CardType
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Deck
import com.mehmetbozkurt.clink.content.Level

@Serializable
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

@Serializable
sealed interface TodPhase {
    @Serializable
    data object Choosing : TodPhase
    @Serializable
    data class ShowingCard(val type: CardType, val cardId: String) : TodPhase
    @Serializable
    data class ShowingPenalty(val cardId: String) : TodPhase
}

/** Something the players should be told, e.g. the chosen level has no cards of a type. */
@Serializable
enum class TodNotice { NO_TRUTH_CARDS, NO_DARE_CARDS, NO_PENALTY_CARDS }

@Serializable
sealed interface TodAction {
    @Serializable
    data class Pick(val type: CardType) : TodAction
    @Serializable
    data object Done : TodAction
    @Serializable
    data object Refuse : TodAction
    @Serializable
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
