package com.mehmetbozkurt.clink.game

sealed interface PlayerNamesResult {
    data class Valid(val names: List<String>) : PlayerNamesResult
    /** [min] is how many the game being set up needs, so the message can say the right number. */
    data class TooFew(val min: Int) : PlayerNamesResult
    data object TooMany : PlayerNamesResult
    data class Duplicate(val name: String) : PlayerNamesResult
}

/** Validates the player list of a local session. */
object PlayerNames {
    const val MIN = 2
    const val MAX = 12
    const val MAX_LENGTH = 20

    /**
     * Trims, drops blank rows, caps each name at [MAX_LENGTH], then checks the count and case-insensitive duplicates.
     * [min] comes from the game being set up ([GameId.minPlayers]); most games take the default.
     */
    fun validate(raw: List<String>, min: Int = MIN): PlayerNamesResult {
        val names = raw.map { it.trim().take(MAX_LENGTH).trim() }.filter { it.isNotEmpty() }
        if (names.size < min) return PlayerNamesResult.TooFew(min)
        if (names.size > MAX) return PlayerNamesResult.TooMany
        names.forEachIndexed { index, name ->
            // Char-wise ignoreCase also matches Turkish İ/i and I/ı.
            if (names.take(index).any { it.equals(name, ignoreCase = true) }) return PlayerNamesResult.Duplicate(name)
        }
        return PlayerNamesResult.Valid(names)
    }
}
