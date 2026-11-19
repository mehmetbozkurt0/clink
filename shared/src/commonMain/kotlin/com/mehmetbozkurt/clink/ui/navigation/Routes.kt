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

@Serializable
data class GameOverRoute(val game: GameId, val players: List<String>, val level: Level)
