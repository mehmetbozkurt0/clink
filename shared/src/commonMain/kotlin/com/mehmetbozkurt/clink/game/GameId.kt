package com.mehmetbozkurt.clink.game

import kotlinx.serialization.Serializable

/**
 * [minPlayers] is the smallest local session this game makes sense in; [playsOnline] is false while a game's online
 * rules are still missing, which keeps it out of the lobby picker and greys it out on the mode screen.
 */
@Serializable
enum class GameId(val minPlayers: Int = PlayerNames.MIN, val playsOnline: Boolean = true) {
    TRUTH_OR_DARE,
    SPIN_THE_BOTTLE,
    CUP_PONG,

    /** Pointing at one of two people says nothing, so this one needs three (spec 2026-11-25 §5). */
    MOST_LIKELY_TO(minPlayers = 3, playsOnline = false),
}
