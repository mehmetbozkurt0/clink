package com.mehmetbozkurt.clink.online

import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.game.beerpong.BeerPongState
import com.mehmetbozkurt.clink.game.spinthebottle.SpinState
import com.mehmetbozkurt.clink.game.truthordare.TruthOrDareState
import kotlinx.serialization.Serializable

/**
 * What the host publishes at `lobbies/{id}/state` (spec §10.4): the running game's state (card ids only, never text),
 * which member uid sits in which player seat, and Spin the Bottle's typed texts. [seq] goes up on every change.
 * For Beer Pong [playerUids] are the two match players; everyone else watches.
 */
@Serializable
data class SessionState(
    val seq: Int,
    val game: GameId,
    val level: Level,
    val playerUids: List<String>,
    val tod: TruthOrDareState? = null,
    val spin: SpinState? = null,
    val spinQuestion: String? = null,
    val spinAnswer: String? = null,
    val beerPong: BeerPongState? = null,
)

fun encodeSession(session: SessionState): String = OnlineJson.encodeToString(SessionState.serializer(), session)

fun decodeSession(json: String): SessionState = OnlineJson.decodeFromString(SessionState.serializer(), json)
