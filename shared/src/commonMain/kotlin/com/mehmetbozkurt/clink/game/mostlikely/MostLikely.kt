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
    /** True in an online session, where a round is a secret vote instead of the phone holder tapping a name. */
    val voting: Boolean = false,
) {
    /** False until somebody has been picked, so ending early can skip the tally. */
    val hasPicks: Boolean get() = picks.any { it > 0 }

    /** True when closing the game should publish the tally first, so the whole room sees it (spec 2026-11-26 §6). */
    fun endsWithTally(): Boolean = hasPicks && phase !is MlPhase.Summary

    /** True once every seat in [online] has voted; the round reveals itself then. */
    fun allVoted(online: Set<Int>): Boolean {
        val phase = phase as? MlPhase.Voting ?: return false
        return online.isNotEmpty() && online.all { phase.votes.getOrNull(it) != null }
    }
}

@Serializable
sealed interface MlPhase {
    /** The chosen level has no prompts at all, so no round can start. */
    @Serializable
    data object NoPrompts : MlPhase

    @Serializable
    data class ShowingPrompt(val cardId: String) : MlPhase

    /** Online only: [votes] has one slot per seat, null until that player votes, else the seat they voted for. */
    @Serializable
    data class Voting(val cardId: String, val votes: List<Int?>) : MlPhase

    /**
     * [picked] is every player taking the penalty — one on a phone, several when an online vote ties.
     * [cardId] is null when the level has no penalty cards; the round still ends normally.
     * [counts] is the votes each seat received this round, empty on one phone.
     */
    @Serializable
    data class ShowingPenalty(
        val picked: List<Int>,
        val promptId: String,
        val cardId: String?,
        val counts: List<Int> = emptyList(),
    ) : MlPhase

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

    /** The wrong name was tapped: back to the same prompt, the count given back. One phone only. */
    @Serializable
    data object WrongPerson : MlAction

    /**
     * Online: [voter]'s secret vote for [target] on the prompt [promptId]. The referee stamps [voter] with the
     * sender's own seat, and a vote for a prompt that is no longer showing is dropped (spec §6: passing throws
     * the votes away, and a vote still in flight must not land on the prompt that replaced it).
     */
    @Serializable
    data class Vote(val voter: Int, val target: Int, val promptId: String) : MlAction

    /** Online, host only: close the round now with the votes already in. */
    @Serializable
    data object Reveal : MlAction

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

/**
 * What counts as "a new phase" for the tap cooldown. A vote changes the voting phase's value without starting
 * anything new, so the round's prompt is the key; anything else is a real transition.
 */
internal fun MlPhase.tapGuardKey(): Any = if (this is MlPhase.Voting) cardId else this

fun initialMostLikelyState(
    players: List<String>,
    level: Level,
    library: ContentLibrary,
    random: Random,
    voting: Boolean = false,
): MostLikelyState {
    val promptDeck = Deck.of(library.cardIds(CardType.MOST_LIKELY, level))
    val drawn = promptDeck.draw(random)
    val empty = List<Int?>(players.size) { null }
    return MostLikelyState(
        players = players,
        level = level,
        phase = when {
            drawn == null -> MlPhase.NoPrompts
            voting -> MlPhase.Voting(drawn.first, empty)
            else -> MlPhase.ShowingPrompt(drawn.first)
        },
        promptDeck = drawn?.second ?: promptDeck,
        penaltyDeck = Deck.of(library.cardIds(CardType.PENALTY, level)),
        picks = List(players.size) { 0 },
        voting = voting,
    )
}
