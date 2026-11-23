package com.mehmetbozkurt.clink.content

import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Draws every card once in random order, then starts over.
 * The card drawn right after a refill is never the same as the one before it (unless there is only one card).
 */
@Serializable
data class Deck(val all: List<String>, val remaining: List<String>, val last: String?) {

    fun draw(random: Random): Pair<String, Deck>? {
        if (all.isEmpty()) return null
        val pool = remaining.ifEmpty { all }
        val candidates = if (remaining.isEmpty() && all.size > 1) pool.filter { it != last } else pool
        val id = candidates[random.nextInt(candidates.size)]
        val rest = pool.toMutableList().apply { remove(id) }
        return id to Deck(all, rest, id)
    }

    companion object {
        fun of(ids: List<String>): Deck = Deck(all = ids, remaining = ids, last = null)
    }
}
