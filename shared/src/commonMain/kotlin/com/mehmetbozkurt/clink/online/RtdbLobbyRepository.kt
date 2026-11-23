package com.mehmetbozkurt.clink.online

import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.game.GameId
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.DataSnapshot
import dev.gitlive.firebase.database.DatabaseException
import dev.gitlive.firebase.database.FirebaseDatabase
import dev.gitlive.firebase.database.ServerValue
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val CHAT_HISTORY = 100

/**
 * [LobbyRepository] on the Firebase Realtime Database (GitLive SDK). Values are read as plain maps and written as
 * plain values, so the stored layout is exactly the one `firebase/database.rules.json` checks.
 */
class RtdbLobbyRepository(database: () -> FirebaseDatabase = { Firebase.database }) : LobbyRepository {
    // Lazy: without a database URL in google-services.json this throws, and only online play should fail then.
    private val db by lazy(database)

    private fun lobby(id: String) = db.reference("lobbies/$id")

    override suspend fun createLobby(host: Member, game: GameId, level: Level, code: String): String? {
        val id = db.reference("lobbies").push().key ?: return null
        // Meta and code first: the member rule needs the lobby to exist, and the host may write everything after.
        val claimed = permitted {
            db.reference().updateChildren(
                mapOf(
                    "codes/$code" to mapOf(
                        "lobbyId" to id, "game" to game.name, "level" to level.name, "hostName" to host.name, "hostUid" to host.uid,
                    ),
                    "lobbies/$id/meta" to mapOf(
                        "hostUid" to host.uid, "code" to code, "game" to game.name, "level" to level.name,
                        "status" to LobbyStatus.LOBBY.wire, "hostOnline" to true,
                    ),
                ),
            )
        }
        if (!claimed) return null
        lobby(id).child("members/${host.uid}").updateChildren(memberFields(host, firstJoin = true))
        return id
    }

    override suspend fun resolve(code: String): CodeInfo? {
        val raw = db.reference("codes/$code").get().value as? Map<*, *> ?: return null
        return CodeInfo(
            lobbyId = raw["lobbyId"] as? String ?: return null,
            game = enumOrNull<GameId>(raw["game"]) ?: return null,
            level = enumOrNull<Level>(raw["level"]) ?: return null,
            hostName = raw["hostName"] as? String ?: "",
        )
    }

    override suspend fun join(lobbyId: String, me: Member): Boolean {
        val node = lobby(lobbyId).child("members/${me.uid}")
        val rejoin = try {
            node.get().exists
        } catch (e: DatabaseException) {
            false
        }
        return permitted { node.updateChildren(memberFields(me, firstJoin = !rejoin)) }
    }

    override suspend fun leave(lobbyId: String, uid: String) {
        val node = lobby(lobbyId).child("members/$uid")
        node.child("online").onDisconnect().cancel()
        node.removeValue()
    }

    override suspend fun remove(lobbyId: String, uid: String) {
        lobby(lobbyId).updateChildren(mapOf("removed/$uid" to true, "members/$uid" to null))
    }

    override fun observe(lobbyId: String): Flow<LobbySnapshot> {
        val ref = lobby(lobbyId)
        return combine(
            ref.child("meta").valueEvents,
            ref.child("members").valueEvents,
            ref.child("removed").valueEvents,
            ref.child("state").valueEvents,
        ) { meta, members, removed, state ->
            LobbySnapshot(parseMeta(meta.value), members.children.mapNotNull(::parseMember), removed.children.mapNotNull { it.key }.toSet(), parseState(state.value))
        }
    }

    override fun actions(lobbyId: String): Flow<List<ActionRecord>> =
        lobby(lobbyId).child("actions").orderByKey().valueEvents.map { snap ->
            snap.children.mapNotNull { child ->
                val raw = child.value as? Map<*, *> ?: return@mapNotNull null
                ActionRecord(child.key ?: return@mapNotNull null, raw["uid"] as? String ?: return@mapNotNull null, raw["json"] as? String ?: return@mapNotNull null)
            }
        }

    override suspend fun sendAction(lobbyId: String, uid: String, json: String) {
        lobby(lobbyId).child("actions").push().updateChildren(mapOf("uid" to uid, "json" to json))
    }

    override suspend fun deleteAction(lobbyId: String, key: String) {
        lobby(lobbyId).child("actions/$key").removeValue()
    }

    override suspend fun publish(lobbyId: String, session: SessionState?) {
        val state = lobby(lobbyId).child("state")
        if (session == null) state.removeValue() else state.updateChildren(mapOf("seq" to session.seq, "json" to encodeSession(session)))
    }

    override suspend fun setMeta(lobbyId: String, game: GameId, level: Level, status: LobbyStatus) {
        val code = lobby(lobbyId).child("meta/code").get().value as? String
        val update = mutableMapOf<String, Any?>(
            "lobbies/$lobbyId/meta/game" to game.name,
            "lobbies/$lobbyId/meta/level" to level.name,
            "lobbies/$lobbyId/meta/status" to status.wire,
        )
        if (code != null) {
            update["codes/$code/game"] = game.name
            update["codes/$code/level"] = level.name
        }
        db.reference().updateChildren(update)
    }

    override suspend fun close(lobbyId: String) {
        val meta = lobby(lobbyId).child("meta")
        meta.onDisconnect().cancel()
        val code = meta.child("code").get().value as? String
        val update = mutableMapOf<String, Any?>("lobbies/$lobbyId/meta/status" to LobbyStatus.CLOSED.wire)
        if (code != null) update["codes/$code"] = null
        db.reference().updateChildren(update)
    }

    override fun chat(lobbyId: String): Flow<List<ChatMessage>> =
        lobby(lobbyId).child("chat").orderByKey().limitToLast(CHAT_HISTORY).valueEvents.map { snap ->
            snap.children.mapNotNull { child ->
                val raw = child.value as? Map<*, *> ?: return@mapNotNull null
                ChatMessage(
                    id = child.key ?: return@mapNotNull null,
                    uid = raw["uid"] as? String ?: return@mapNotNull null,
                    name = raw["name"] as? String ?: "",
                    text = raw["text"] as? String ?: return@mapNotNull null,
                    at = (raw["at"] as? Number)?.toLong() ?: 0L,
                )
            }
        }

    override suspend fun sendChat(lobbyId: String, uid: String, name: String, text: String) {
        lobby(lobbyId).child("chat").push().updateChildren(mapOf("uid" to uid, "name" to name, "text" to text, "at" to ServerValue.TIMESTAMP))
    }

    override suspend fun stayOnline(lobbyId: String, uid: String, isHost: Boolean) {
        val online = lobby(lobbyId).child("members/$uid/online")
        val meta = lobby(lobbyId).child("meta")
        // Re-armed on every reconnect: the server drops onDisconnect handlers once they have run.
        connected().collect { connected ->
            if (!connected) return@collect
            try {
                online.onDisconnect().setValue(false)
                online.setValue(true)
                if (isHost) {
                    meta.onDisconnect().updateChildren(mapOf("hostOnline" to false, "hostSeenAt" to ServerValue.TIMESTAMP))
                    meta.updateChildren(mapOf("hostOnline" to true, "hostSeenAt" to null))
                }
            } catch (e: DatabaseException) {
                // Removed or closed meanwhile: the rules refuse, and the lobby screen is already on its way out.
            }
        }
    }

    override fun connected(): Flow<Boolean> = db.reference(".info/connected").valueEvents.map { it.value as? Boolean ?: false }

    override suspend fun serverTimeOffset(): Long =
        (db.reference(".info/serverTimeOffset").valueEvents.first().value as? Number)?.toLong() ?: 0L

    /** Runs a write the rules may refuse; false when they do. Other failures are thrown. */
    private suspend fun permitted(write: suspend () -> Unit): Boolean =
        try {
            write()
            true
        } catch (e: DatabaseException) {
            if (e.message?.contains("permission", ignoreCase = true) == true) false else throw e
        }

    private fun memberFields(member: Member, firstJoin: Boolean): Map<String, Any?> {
        val fields = mutableMapOf<String, Any?>("name" to member.name, "avatarId" to member.avatarId, "online" to true)
        // Server time, so the seat order doesn't depend on how each phone's clock is set.
        if (firstJoin) fields["joinedAt"] = ServerValue.TIMESTAMP
        return fields
    }

    private fun parseMeta(raw: Any?): LobbyMeta? {
        val map = raw as? Map<*, *> ?: return null
        return LobbyMeta(
            hostUid = map["hostUid"] as? String ?: return null,
            code = map["code"] as? String ?: "",
            game = enumOrNull<GameId>(map["game"]) ?: return null,
            level = enumOrNull<Level>(map["level"]) ?: return null,
            status = LobbyStatus.entries.firstOrNull { it.wire == map["status"] } ?: LobbyStatus.LOBBY,
            hostOnline = map["hostOnline"] as? Boolean ?: true,
            hostSeenAt = (map["hostSeenAt"] as? Number)?.toLong(),
        )
    }

    private fun parseMember(snap: DataSnapshot): Member? {
        val map = snap.value as? Map<*, *> ?: return null
        return Member(
            uid = snap.key ?: return null,
            name = map["name"] as? String ?: return null,
            avatarId = map["avatarId"] as? String ?: "",
            joinedAt = (map["joinedAt"] as? Number)?.toLong() ?: Long.MAX_VALUE,
            online = map["online"] as? Boolean ?: false,
        )
    }

    private fun parseState(raw: Any?): SessionState? {
        val json = (raw as? Map<*, *>)?.get("json") as? String ?: return null
        return try {
            decodeSession(json)
        } catch (e: Exception) {
            null
        }
    }

    private inline fun <reified E : Enum<E>> enumOrNull(raw: Any?): E? = enumValues<E>().firstOrNull { it.name == raw }
}

/** How a status is stored: the same lowercase words the rules compare against. */
private val LobbyStatus.wire: String
    get() = name.lowercase()
