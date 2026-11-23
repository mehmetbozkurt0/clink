package com.mehmetbozkurt.clink.online

import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.game.GameId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** In-memory stand-in for the Realtime Database that applies the same rules the app relies on. */
class FakeLobbyRepository : LobbyRepository {
    private val codes = MutableStateFlow<Map<String, Pair<String, CodeInfo>>>(emptyMap()) // code → (hostUid, info)
    private val metas = MutableStateFlow<Map<String, LobbyMeta>>(emptyMap())
    private val members = MutableStateFlow<Map<String, Map<String, Member>>>(emptyMap())
    private val removed = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    private val states = MutableStateFlow<Map<String, SessionState?>>(emptyMap())
    private val actionLists = MutableStateFlow<Map<String, List<ActionRecord>>>(emptyMap())
    private val chats = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    private var nextId = 0
    var publishCount = 0
        private set

    override suspend fun createLobby(host: Member, game: GameId, level: Level, code: String): String? {
        if (code in codes.value) return null
        val id = "lobby${nextId++}"
        codes.value += code to (host.uid to CodeInfo(id, game, level, host.name))
        metas.value += id to LobbyMeta(host.uid, code, game, level, LobbyStatus.LOBBY, hostOnline = true, hostSeenAt = null)
        members.value += id to mapOf(host.uid to host)
        return id
    }

    override suspend fun resolve(code: String): CodeInfo? = codes.value[code]?.second

    override suspend fun join(lobbyId: String, me: Member): Boolean {
        val meta = metas.value[lobbyId] ?: return false
        if (meta.status == LobbyStatus.CLOSED || me.uid in removed.value[lobbyId].orEmpty()) return false
        val list = members.value[lobbyId].orEmpty()
        val joinedAt = list[me.uid]?.joinedAt ?: me.joinedAt
        members.value += lobbyId to (list + (me.uid to me.copy(joinedAt = joinedAt, online = true)))
        return true
    }

    override suspend fun leave(lobbyId: String, uid: String) {
        members.value += lobbyId to (members.value[lobbyId].orEmpty() - uid)
    }

    override suspend fun remove(lobbyId: String, uid: String) {
        removed.value += lobbyId to (removed.value[lobbyId].orEmpty() + uid)
        leave(lobbyId, uid)
    }

    override fun observe(lobbyId: String): Flow<LobbySnapshot> =
        combine(metas, members, removed, states) { m, mem, rem, st ->
            LobbySnapshot(m[lobbyId], mem[lobbyId].orEmpty().values.toList(), rem[lobbyId].orEmpty(), st[lobbyId])
        }

    override fun actions(lobbyId: String): Flow<List<ActionRecord>> = actionLists.map { it[lobbyId].orEmpty() }

    override suspend fun sendAction(lobbyId: String, uid: String, json: String) {
        val list = actionLists.value[lobbyId].orEmpty()
        actionLists.value += lobbyId to (list + ActionRecord("a${list.size}-${nextId++}", uid, json))
    }

    override suspend fun deleteAction(lobbyId: String, key: String) {
        actionLists.value += lobbyId to actionLists.value[lobbyId].orEmpty().filterNot { it.key == key }
    }

    override suspend fun publish(lobbyId: String, session: SessionState?) {
        publishCount++
        states.value += lobbyId to session
    }

    override suspend fun setMeta(lobbyId: String, game: GameId, level: Level, status: LobbyStatus) {
        val meta = metas.value.getValue(lobbyId)
        metas.value += lobbyId to meta.copy(game = game, level = level, status = status)
        val code = meta.code
        codes.value[code]?.let { (host, info) -> codes.value += code to (host to info.copy(game = game, level = level)) }
    }

    override suspend fun close(lobbyId: String) {
        val meta = metas.value[lobbyId] ?: return
        metas.value += lobbyId to meta.copy(status = LobbyStatus.CLOSED)
        codes.value -= meta.code
    }

    override fun chat(lobbyId: String): Flow<List<ChatMessage>> = chats.map { it[lobbyId].orEmpty() }

    override suspend fun sendChat(lobbyId: String, uid: String, name: String, text: String) {
        val list = chats.value[lobbyId].orEmpty()
        chats.value += lobbyId to (list + ChatMessage("m${list.size}", uid, name, text, list.size.toLong()))
    }

    override suspend fun stayOnline(lobbyId: String, uid: String, isHost: Boolean) = Unit

    val isConnected = MutableStateFlow(true)

    override fun connected(): Flow<Boolean> = isConnected

    override suspend fun serverTimeOffset(): Long = 0

    /** Test helper: what presence would do when a phone drops or comes back. */
    fun setOnline(lobbyId: String, uid: String, online: Boolean) {
        val list = members.value[lobbyId].orEmpty()
        list[uid]?.let { members.value += lobbyId to (list + (uid to it.copy(online = online))) }
    }

    fun setHostOnline(lobbyId: String, online: Boolean, seenAt: Long?) {
        metas.value[lobbyId]?.let { metas.value += lobbyId to it.copy(hostOnline = online, hostSeenAt = seenAt) }
    }

    fun session(lobbyId: String): SessionState? = states.value[lobbyId]
    fun meta(lobbyId: String): LobbyMeta? = metas.value[lobbyId]
    fun codeExists(code: String): Boolean = code in codes.value
}
