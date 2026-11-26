package com.mehmetbozkurt.clink.game.mostlikely

import com.mehmetbozkurt.clink.content.CardType
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Deck
import com.mehmetbozkurt.clink.content.Level
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * "Kim Daha Çok?" (spec 2026-11-25-most-likely-to): a prompt is read out, the group counts to three and everyone
 * points at once, and the most-pointed player takes a penalty. [picks] counts how many times each player has been
 * picked tonight and is as long as [players].
 */
@Serializable
data class MostLikelyState(
    val players: List<String>,
    val level: Level,
    val phase: MlPhase,
    val promptDeck: Deck,
    val penaltyDeck: Deck,
    val picks: List<Int>,
) {
    /** False until somebody has been picked, so ending early can skip the tally. */
    val hasPicks: Boolean get() = picks.any { it > 0 }
}

@Serializable
sealed interface MlPhase {
    /** The chosen level has no prompts at all, so no round can start. */
    @Serializable
    data object NoPrompts : MlPhase

    @Serializable
    data class ShowingPrompt(val cardId: String) : MlPhase

    /** [cardId] is null when the level has no penalty cards; the round still ends normally. */
    @Serializable
    data class ShowingPenalty(val picked: Int, val promptId: String, val cardId: String?) : MlPhase

    /** Who was picked most tonight, shown when the players end the game. */
    @Serializable
    data object Summary : MlPhase
}

@Serializable
sealed interface MlAction {
    @Serializable
    data class Pick(val player: Int) : MlAction

    /** The prompt does not suit the room: draw another, nobody is counted. */
    @Serializable
    data object Pass : MlAction

    /** The wrong name was tapped: back to the same prompt, the count given back. */
    @Serializable
    data object WrongPerson : MlAction

    @Serializable
    data object PenaltyDone : MlAction

    @Serializable
    data object End : MlAction
}

/**
 * Smaller name chips once the list is long, so twelve of them still fit above the buttons without scrolling.
 * Eight is where two columns of full-size chips stop fitting on a 360 dp wide screen.
 */
internal fun compactChips(playerCount: Int): Boolean = playerCount > 8

fun initialMostLikelyState(
    players: List<String>,
    level: Level,
    library: ContentLibrary,
    random: Random,
): MostLikelyState {
    val promptDeck = Deck.of(library.cardIds(CardType.MOST_LIKELY, level))
    val drawn = promptDeck.draw(random)
    return MostLikelyState(
        players = players,
        level = level,
        phase = drawn?.let { MlPhase.ShowingPrompt(it.first) } ?: MlPhase.NoPrompts,
        promptDeck = drawn?.second ?: promptDeck,
        penaltyDeck = Deck.of(library.cardIds(CardType.PENALTY, level)),
        picks = List(players.size) { 0 },
    )
}
