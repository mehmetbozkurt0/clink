package com.mehmetbozkurt.clink.online

import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.game.PlayerNames
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Someone in a lobby. [uid] is the key of `members/{uid}`; [online] is kept up to date by presence. */
data class Member(val uid: String, val name: String, val avatarId: String, val joinedAt: Long, val online: Boolean = true)

@Serializable
enum class LobbyStatus {
    @SerialName("lobby") LOBBY,
    @SerialName("playing") PLAYING,
    @SerialName("closed") CLOSED,
}

/** `lobbies/{id}/meta`: written by the host only. */
data class LobbyMeta(
    val hostUid: String,
    val code: String,
    val game: GameId,
    val level: Level,
    val status: LobbyStatus,
    val hostOnline: Boolean,
    val hostSeenAt: Long?,
)

/** `codes/{code}`: what a joiner sees before joining (the lobby itself is readable only by members). */
data class CodeInfo(val lobbyId: String, val game: GameId, val level: Level, val hostName: String)

data class ChatMessage(val id: String, val uid: String, val name: String, val text: String, val at: Long)

const val CHAT_MAX_LENGTH = 300
const val HOST_TIMEOUT_MILLIS = 60_000L

sealed interface HostStatus {
    data object Online : HostStatus
    data class Reconnecting(val secondsLeft: Int) : HostStatus
    data object Gone : HostStatus
}

/**
 * Spec §5.2: when the host drops, the others see "host reconnecting"; after 60 s the lobby closes. A host that is
 * offline without a known time (it never reached the server) counts from [now].
 */
fun hostStatus(hostOnline: Boolean, hostSeenAt: Long?, now: Long): HostStatus {
    if (hostOnline) return HostStatus.Online
    val elapsed = (now - (hostSeenAt ?: now)).coerceAtLeast(0)
    if (elapsed >= HOST_TIMEOUT_MILLIS) return HostStatus.Gone
    return HostStatus.Reconnecting(((HOST_TIMEOUT_MILLIS - elapsed + 999) / 1000).toInt())
}

/** Who plays, in seat order: by join time (uid breaks ties), at most [PlayerNames.MAX]. */
fun playerOrder(members: List<Member>): List<Member> =
    members.sortedWith(compareBy({ it.joinedAt }, { it.uid })).take(PlayerNames.MAX)

/** A member who joined after the lobby already had [PlayerNames.MAX] players. */
fun isFull(members: List<Member>, uid: String): Boolean =
    members.any { it.uid == uid } && playerOrder(members).none { it.uid == uid }
