package com.mehmetbozkurt.clink.online

import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.game.GameId
import kotlinx.coroutines.flow.Flow

/** One entry of `lobbies/{id}/actions`: [uid] is checked by the database rules, so the host can trust it. */
data class ActionRecord(val key: String, val uid: String, val json: String)

/** What a member can see of a lobby at one moment; [meta] is null while it hasn't loaded or doesn't exist. */
data class LobbySnapshot(val meta: LobbyMeta?, val members: List<Member>, val removed: Set<String>, val session: SessionState?)

/**
 * The lobby's data in the Realtime Database (spec §10.4). Writes the rules forbid fail with an exception; the host-only
 * calls are made by the host only.
 */
interface LobbyRepository {
    /** Claims [code] and opens a lobby with [host] in it. Returns the lobby id, or null when the code is taken. */
    suspend fun createLobby(host: Member, game: GameId, level: Level, code: String): String?

    /** The lobby behind [code]; null when there is none (never existed, or closed). */
    suspend fun resolve(code: String): CodeInfo?

    /** Adds [me] to the lobby; a rejoin keeps the first join time. False when the lobby refuses (closed, or removed). */
    suspend fun join(lobbyId: String, me: Member): Boolean

    suspend fun leave(lobbyId: String, uid: String)

    /** Host: takes [uid] out and keeps them out. */
    suspend fun remove(lobbyId: String, uid: String)

    fun observe(lobbyId: String): Flow<LobbySnapshot>

    /** Host: the queued member actions, oldest first. */
    fun actions(lobbyId: String): Flow<List<ActionRecord>>

    suspend fun sendAction(lobbyId: String, uid: String, json: String)

    suspend fun deleteAction(lobbyId: String, key: String)

    /** Host: the game everyone sees; null when there is no game running. */
    suspend fun publish(lobbyId: String, session: SessionState?)

    suspend fun setMeta(lobbyId: String, game: GameId, level: Level, status: LobbyStatus)

    /** Host: marks the lobby closed and frees its code. */
    suspend fun close(lobbyId: String)

    fun chat(lobbyId: String): Flow<List<ChatMessage>>

    suspend fun sendChat(lobbyId: String, uid: String, name: String, text: String)

    /** Keeps [uid] (and, for the host, the lobby's host flags) marked online while this runs; never returns. */
    suspend fun stayOnline(lobbyId: String, uid: String, isHost: Boolean)

    /** Whether this phone is connected to the database right now. */
    fun connected(): Flow<Boolean>

    /** Server time minus this phone's time, so countdowns agree across phones. */
    suspend fun serverTimeOffset(): Long
}
