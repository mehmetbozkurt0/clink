package com.mehmetbozkurt.clink.game

sealed interface PlayerNamesResult {
    data class Valid(val names: List<String>) : PlayerNamesResult
    data object TooFew : PlayerNamesResult
    data object TooMany : PlayerNamesResult
    data class Duplicate(val name: String) : PlayerNamesResult
}

/** Validates the player list of a local session. */
object PlayerNames {
    const val MIN = 2
    const val MAX = 12
    const val MAX_LENGTH = 20

    /** Trims, drops blank rows, caps each name at [MAX_LENGTH], then checks the count and case-insensitive duplicates. */
    fun validate(raw: List<String>): PlayerNamesResult {
        val names = raw.map { it.trim().take(MAX_LENGTH).trim() }.filter { it.isNotEmpty() }
        if (names.size < MIN) return PlayerNamesResult.TooFew
        if (names.size > MAX) return PlayerNamesResult.TooMany
        names.forEachIndexed { index, name ->
            // Char-wise ignoreCase also matches Turkish İ/i and I/ı.
            if (names.take(index).any { it.equals(name, ignoreCase = true) }) return PlayerNamesResult.Duplicate(name)
        }
        return PlayerNamesResult.Valid(names)
    }
}
