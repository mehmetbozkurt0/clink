package com.mehmetbozkurt.clink.online

import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.game.GameId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Why this phone is out of the lobby. */
enum class LobbyExit { LEFT, CLOSED, REMOVED }

sealed interface JoinResult {
    data class Joined(val session: LobbySession) : JoinResult
    data object NotFound : JoinResult
    data object Refused : JoinResult
    data object Full : JoinResult
    data object Failed : JoinResult
}

/** What a code leads to, before joining. [Failed]: no answer from the database (no connection). */
sealed interface Peek {
    data class Found(val info: CodeInfo) : Peek
    data object NotFound : Peek
    data object Failed : Peek
}

private const val CODE_ATTEMPTS = 5
private const val WRITE_TIMEOUT_MILLIS = 10_000L

/** A dropped connection shows the offline banner only after this long, so a blip doesn't flash it. */
const val OFFLINE_BANNER_DELAY_MILLIS = 2_000L

@OptIn(ExperimentalTime::class)
private fun systemNow(): Long = Clock.System.now().toEpochMilliseconds()

/** Opens and joins lobbies; [active] is the lobby this phone is in, if any. */
class LobbyLauncher(
    private val repo: LobbyRepository,
    private val library: ContentLibrary,
    private val random: Random,
    private val scope: CoroutineScope,
    private val clock: () -> Long = ::systemNow,
    private val newCode: () -> String = { RoomCode.generate(random) },
) {
    private val _active = MutableStateFlow<LobbySession?>(null)
    val active: StateFlow<LobbySession?> = _active.asStateFlow()

    /** A new lobby with [host] in it, or null when it couldn't be created (no connection). */
    suspend fun create(host: Member, game: GameId, level: Level): LobbySession? = guarded(null) {
        repeat(CODE_ATTEMPTS) {
            val code = newCode()
            val id = withTimeoutOrNull(WRITE_TIMEOUT_MILLIS) { repo.createLobby(host, game, level, code) } ?: return@repeat
            return@guarded open(id, code, host, isHost = true)
        }
        null
    }

    /** What a joiner sees before joining: the host's name and the game. */
    suspend fun peek(code: String): Peek = guarded(Peek.Failed) {
        val clean = RoomCode.normalize(code) ?: return@guarded Peek.NotFound
        lookUp(clean)
    }

    suspend fun join(code: String, me: Member): JoinResult = guarded(JoinResult.Failed) {
        val clean = RoomCode.normalize(code) ?: return@guarded JoinResult.NotFound
        val info = when (val found = lookUp(clean)) {
            is Peek.Found -> found.info
            Peek.NotFound -> return@guarded JoinResult.NotFound
            Peek.Failed -> return@guarded JoinResult.Failed
        }
        // Already in this lobby (the invite link tapped again): keep the seat instead of leaving and rejoining.
        _active.value?.takeIf { it.lobbyId == info.lobbyId && it.me.uid == me.uid }?.let { return@guarded JoinResult.Joined(it) }
        if (!(withTimeoutOrNull(WRITE_TIMEOUT_MILLIS) { repo.join(info.lobbyId, me) } ?: return@guarded JoinResult.Failed)) {
            return@guarded JoinResult.Refused
        }
        val members = withTimeoutOrNull(WRITE_TIMEOUT_MILLIS) { repo.observe(info.lobbyId).first { it.meta != null }.members }
            ?: return@guarded JoinResult.Failed
        if (isFull(members, me.uid)) {
            repo.leave(info.lobbyId, me.uid)
            return@guarded JoinResult.Full
        }
        JoinResult.Joined(open(info.lobbyId, clean, me, isHost = false))
    }

    // A missing code and a database that doesn't answer are different messages for the joiner.
    private suspend fun lookUp(code: String): Peek =
        withTimeoutOrNull(WRITE_TIMEOUT_MILLIS) { repo.resolve(code)?.let { Peek.Found(it) } ?: Peek.NotFound } ?: Peek.Failed

    private suspend fun open(lobbyId: String, code: String, me: Member, isHost: Boolean): LobbySession {
        val offset = withTimeoutOrNull(WRITE_TIMEOUT_MILLIS) { repo.serverTimeOffset() } ?: 0L
        _active.value?.leave()
        return LobbySession(repo, library, random, scope, clock, lobbyId, code, me, isHost, offset) { gone ->
            _active.compareAndSet(gone, null)
        }.also { _active.value = it }
    }

    private suspend fun <T> guarded(fallback: T, block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fallback
        }
}

/**
 * This phone's seat in one lobby. On the host it also runs the game (spec §10.4): it applies its own and the members'
 * actions through [HostReferee], skips players who drop and publishes every new state. Members only send actions.
 */
class LobbySession internal constructor(
    private val repo: LobbyRepository,
    private val library: ContentLibrary,
    private val random: Random,
    private val parent: CoroutineScope,
    private val clock: () -> Long,
    val lobbyId: String,
    val code: String,
    val me: Member,
    val isHost: Boolean,
    private val serverOffset: Long,
    private val onGone: (LobbySession) -> Unit,
) {
    private val scope = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    private val lostAccess = MutableStateFlow(false)
    private val left = MutableStateFlow(false)
    private val mutex = Mutex()
    private var current: SessionState? = null
    private val toPublish = MutableStateFlow<Pending?>(null)

    /** Spec §6.1: this member said "18+" for a Spicy lobby (on joining, or when the host switched to Spicy). */
    var adultConfirmed: Boolean = false

    val snapshot: StateFlow<LobbySnapshot?> =
        repo.observe(lobbyId).catch { lostAccess.value = true }.stateIn(scope, SharingStarted.Eagerly, null)

    val chat: StateFlow<List<ChatMessage>> =
        repo.chat(lobbyId).catch { }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** True once this phone has had no connection for [OFFLINE_BANNER_DELAY_MILLIS]; false again as soon as it's back. */
    val offline: StateFlow<Boolean> = repo.connected().catch { }.transformLatest { connected ->
        if (connected) {
            emit(false)
        } else {
            delay(OFFLINE_BANNER_DELAY_MILLIS)
            emit(true)
        }
    }.stateIn(scope, SharingStarted.Eagerly, false)

    /** Set once this phone is out of the lobby; a removed member loses read access, which also counts as removed. */
    val exit: StateFlow<LobbyExit?> = combine(snapshot, lostAccess, left) { snap, lost, gone ->
        when {
            gone -> LobbyExit.LEFT
            snap?.meta?.status == LobbyStatus.CLOSED -> LobbyExit.CLOSED
            lost -> LobbyExit.REMOVED
            snap?.meta != null && snap.members.none { it.uid == me.uid } -> LobbyExit.REMOVED
            else -> null
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    init {
        scope.launch { quietly { repo.stayOnline(lobbyId, me.uid, isHost) } }
        if (isHost) {
            scope.launch { toPublish.filterNotNull().collect { quietly { repo.publish(lobbyId, it.session) } } }
            scope.launch { applyMemberActions() }
            scope.launch { snapshot.filterNotNull().collect { mutex.withLock { current?.let { update(it) } } } }
            scope.launch { closeAfterLongOutage() }
        }
    }

    /** Server time, so every phone counts the host's reconnect window the same way. */
    fun now(): Long = clock() + serverOffset

    fun hostStatus(snap: LobbySnapshot): HostStatus {
        val meta = snap.meta ?: return HostStatus.Online
        return if (isHost) HostStatus.Online else hostStatus(meta.hostOnline, meta.hostSeenAt, now())
    }

    /** Host: starts [LobbyMeta.game] with the online members (Cup Pong: the two in [players]). False if it can't. */
    suspend fun startGame(players: List<String>? = null): Boolean {
        if (!isHost) return false
        val snap = snapshot.value ?: return false
        val meta = snap.meta ?: return false
        val ready = playerOrder(snap.members).filter { it.online || it.uid == me.uid }
        val chosen = if (players != null) ready.filter { it.uid in players } else ready
        val enough = if (meta.game == GameId.CUP_PONG) chosen.size == 2 else chosen.size >= 2
        if (!enough) return false
        mutex.withLock {
            val next = HostReferee.start(meta.game, meta.level, chosen, library, random, seq = (current?.seq ?: 0) + 1)
            if (!write { repo.setMeta(lobbyId, meta.game, meta.level, LobbyStatus.PLAYING) }) return false
            update(next)
        }
        return true
    }

    /** Host: everyone goes back to the lobby. */
    suspend fun endGame() {
        if (!isHost) return
        val meta = snapshot.value?.meta ?: return
        mutex.withLock {
            current = null
            toPublish.value = Pending(null)
            write { repo.setMeta(lobbyId, meta.game, meta.level, LobbyStatus.LOBBY) }
        }
    }

    /** Host, between games: picks the next game and level. */
    suspend fun setGame(game: GameId, level: Level) {
        if (!isHost || current != null) return
        write { repo.setMeta(lobbyId, game, level, LobbyStatus.LOBBY) }
    }

    suspend fun send(action: OnlineAction) {
        if (isHost) {
            mutex.withLock { current?.let { s -> HostReferee.apply(s, me.uid, action, random)?.let { update(it) } } }
        } else {
            write { repo.sendAction(lobbyId, me.uid, encodeAction(action)) }
        }
    }

    suspend fun removePlayer(uid: String) {
        if (isHost && uid != me.uid) write { repo.remove(lobbyId, uid) }
    }

    suspend fun sendChat(text: String) {
        val clean = text.trim().take(CHAT_MAX_LENGTH).trim()
        if (clean.isNotEmpty()) write { repo.sendChat(lobbyId, me.uid, me.name, clean) }
    }

    /** Leaves the lobby (the host closes it). Runs on the app's scope, so it finishes after the screen is gone. */
    fun leave() {
        if (left.value) return
        left.value = true
        onGone(this)
        parent.launch {
            write { if (isHost) repo.close(lobbyId) else repo.leave(lobbyId, me.uid) }
            scope.cancel()
        }
    }

    /** Applies [next], skips players who are offline, and queues it for publishing. Call with [mutex] held. */
    private fun update(next: SessionState) {
        val online = onlineUids()
        val settled = HostReferee.skipOffline(next, online, random) ?: next
        if (settled == current) return
        current = settled
        toPublish.value = Pending(settled)
    }

    private fun onlineUids(): Set<String> =
        snapshot.value?.members.orEmpty().filter { it.online }.map { it.uid }.toSet() + me.uid

    private suspend fun applyMemberActions() {
        val handled = mutableSetOf<String>()
        repo.actions(lobbyId).catch { }.collect { records ->
            handled.retainAll(records.map { it.key }.toSet())
            for (record in records) {
                if (!handled.add(record.key)) continue
                val action = try {
                    decodeAction(record.json)
                } catch (e: Exception) {
                    null
                }
                if (action != null) {
                    mutex.withLock { current?.let { s -> HostReferee.apply(s, record.uid, action, random)?.let { update(it) } } }
                }
                scope.launch { write { repo.deleteAction(lobbyId, record.key) } }
            }
        }
    }

    /** Spec §5.2: a host away for [HOST_TIMEOUT_MILLIS] finds the lobby closed; members already left by then. */
    private suspend fun closeAfterLongOutage() {
        var lostAt: Long? = null
        repo.connected().catch { }.collect { connected ->
            if (!connected) {
                if (lostAt == null) lostAt = clock()
            } else {
                val since = lostAt
                lostAt = null
                if (since != null && clock() - since >= HOST_TIMEOUT_MILLIS) leave()
            }
        }
    }

    private suspend fun write(block: suspend () -> Unit): Boolean = quietly { withTimeoutOrNull(WRITE_TIMEOUT_MILLIS) { block() } != null } ?: false

    private suspend fun <T> quietly(block: suspend () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    // Identity, not equality: publishing the same state twice must still go out.
    private class Pending(val session: SessionState?)
}
