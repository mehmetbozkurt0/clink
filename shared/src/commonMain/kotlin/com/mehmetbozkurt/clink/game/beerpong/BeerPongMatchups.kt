package com.mehmetbozkurt.clink.game.beerpong

import kotlinx.serialization.Serializable

/** Two players of the session (indexes into the full player list); [first] throws first. */
@Serializable
data class Matchup(val first: Int, val second: Int)

/**
 * The next match. No previous match → the first two players. Otherwise the winner stays on and the next
 * player after the loser (wrapping, never the winner) challenges; the challenger throws first.
 * With two players this is a rematch with the first throw swapped.
 */
fun suggestedMatchup(playerCount: Int, previousWinner: Int?, previousLoser: Int?): Matchup {
    if (previousWinner == null || previousLoser == null) return Matchup(0, 1)
    val challenger = (1..playerCount).map { (previousLoser + it) % playerCount }.first { it != previousWinner }
    return Matchup(first = challenger, second = previousWinner)
}

/** Two picked players: the one who isn't the previous winner throws first; otherwise the lower index does. */
fun matchupFromSelection(selected: List<Int>, previousWinner: Int?): Matchup {
    require(selected.size == 2 && selected[0] != selected[1]) { "Pick two different players: $selected" }
    val (a, b) = if (previousWinner in selected) selected.sortedBy { it == previousWinner } else selected.sorted()
    return Matchup(first = a, second = b)
}
