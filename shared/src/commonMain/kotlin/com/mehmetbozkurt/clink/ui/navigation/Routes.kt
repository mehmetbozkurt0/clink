package com.mehmetbozkurt.clink.ui.navigation

import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.game.GameId
import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute

@Serializable
data object GameListRoute

@Serializable
data class ModeRoute(val game: GameId)

@Serializable
data class PlayersRoute(val game: GameId)

@Serializable
data class LevelRoute(val game: GameId, val players: List<String>)

@Serializable
data class TruthOrDareRoute(val players: List<String>, val level: Level)

@Serializable
data class SpinTheBottleRoute(val players: List<String>, val level: Level)

/** Picking the two match players when there are more than two (indexes into [players]). */
@Serializable
data class BeerPongSetupRoute(
    val players: List<String>,
    val level: Level,
    val previousWinner: Int? = null,
    val previousLoser: Int? = null,
)

/** One Beer Pong match: [first] and [second] index into the full session list [players]; [first] throws first. */
@Serializable
data class BeerPongRoute(val players: List<String>, val level: Level, val first: Int, val second: Int)

/** [matchWinner] and [matchLoser] (indexes into [players]) let Beer Pong's "Play again" keep the winner on. */
@Serializable
data class GameOverRoute(
    val game: GameId,
    val players: List<String>,
    val level: Level,
    val matchWinner: Int? = null,
    val matchLoser: Int? = null,
)
