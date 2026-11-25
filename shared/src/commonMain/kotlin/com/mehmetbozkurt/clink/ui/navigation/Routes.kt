package com.mehmetbozkurt.clink.ui.navigation

import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.game.GameId
import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute

/** Google sign-in for the host (Plan 3). */
@Serializable
data object SignInRoute

/** First profile setup ([editing] false) or editing it from the account screen. */
@Serializable
data class ProfileSetupRoute(val editing: Boolean)

/** Settings (spec 2026-11-24-home-and-settings §5), from the home screen's gear. */
@Serializable
data object SettingsRoute

/** A guest's name and avatar, edited from settings and kept on this phone. */
@Serializable
data object GuestProfileRoute

/** The Join tour on its own, replayed from settings. */
@Serializable
data object JoinTourRoute

@Serializable
data object GameListRoute

@Serializable
data class ModeRoute(val game: GameId)

@Serializable
data class PlayersRoute(val game: GameId)

/** Online (Plan 4): the host picks the level, then the lobby is created. */
@Serializable
data class OnlineLevelRoute(val game: GameId)

/** The lobby this phone is in ([com.mehmetbozkurt.clink.online.LobbyLauncher.active]) and its games. */
@Serializable
data object OnlineRoomRoute

/** Joining with a code; [code] is filled in when an invite link opened the app. */
@Serializable
data class JoinRoute(val code: String? = null)

@Serializable
data class LevelRoute(val game: GameId, val players: List<String>)

@Serializable
data class TruthOrDareRoute(val players: List<String>, val level: Level)

@Serializable
data class SpinTheBottleRoute(val players: List<String>, val level: Level)

/** Picking the two match players when there are more than two (indexes into [players]). */
@Serializable
data class CupPongSetupRoute(
    val players: List<String>,
    val level: Level,
    val previousWinner: Int? = null,
    val previousLoser: Int? = null,
)

/** One Cup Pong match: [first] and [second] index into the full session list [players]; [first] throws first. */
@Serializable
data class CupPongRoute(val players: List<String>, val level: Level, val first: Int, val second: Int)

/** [matchWinner] and [matchLoser] (indexes into [players]) let Cup Pong's "Play again" keep the winner on. */
@Serializable
data class GameOverRoute(
    val game: GameId,
    val players: List<String>,
    val level: Level,
    val matchWinner: Int? = null,
    val matchLoser: Int? = null,
)

/** The Play onboarding tour (spec §5.4); [replay] when opened from "How to play?" rather than the first Play tap. */
@Serializable
data class PlayTourRoute(val replay: Boolean)
