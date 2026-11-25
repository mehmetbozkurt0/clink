package com.mehmetbozkurt.clink.ui.online

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.mehmetbozkurt.clink.LocalAppContainer
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.game.PlayerNames
import com.mehmetbozkurt.clink.online.HostStatus
import com.mehmetbozkurt.clink.online.LobbyExit
import com.mehmetbozkurt.clink.online.LobbySession
import com.mehmetbozkurt.clink.online.LobbySnapshot
import com.mehmetbozkurt.clink.online.LobbyStatus
import com.mehmetbozkurt.clink.online.Member
import com.mehmetbozkurt.clink.online.OnlineAction
import com.mehmetbozkurt.clink.online.hostStatus
import com.mehmetbozkurt.clink.online.inviteLink
import com.mehmetbozkurt.clink.online.isPaused
import com.mehmetbozkurt.clink.online.playerOrder
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.cancel
import com.mehmetbozkurt.clink.resources.host_reconnecting
import com.mehmetbozkurt.clink.resources.lobby_alone_hint
import com.mehmetbozkurt.clink.resources.lobby_close_body
import com.mehmetbozkurt.clink.resources.lobby_close_confirm
import com.mehmetbozkurt.clink.resources.lobby_close_title
import com.mehmetbozkurt.clink.resources.lobby_closed
import com.mehmetbozkurt.clink.resources.lobby_code
import com.mehmetbozkurt.clink.resources.lobby_host_badge
import com.mehmetbozkurt.clink.resources.lobby_host_gone
import com.mehmetbozkurt.clink.resources.lobby_invite
import com.mehmetbozkurt.clink.resources.lobby_invite_text
import com.mehmetbozkurt.clink.resources.lobby_leave_confirm
import com.mehmetbozkurt.clink.resources.lobby_leave_title
import com.mehmetbozkurt.clink.resources.lobby_need_two
import com.mehmetbozkurt.clink.resources.lobby_pick_pair
import com.mehmetbozkurt.clink.resources.lobby_players
import com.mehmetbozkurt.clink.resources.lobby_remove_confirm
import com.mehmetbozkurt.clink.resources.lobby_remove_title
import com.mehmetbozkurt.clink.resources.lobby_removed
import com.mehmetbozkurt.clink.resources.lobby_start
import com.mehmetbozkurt.clink.resources.lobby_title
import com.mehmetbozkurt.clink.resources.lobby_waiting_start
import com.mehmetbozkurt.clink.resources.offline_banner
import com.mehmetbozkurt.clink.resources.ok
import com.mehmetbozkurt.clink.ui.components.AdultConfirmDialog
import com.mehmetbozkurt.clink.ui.components.AvatarBadge
import com.mehmetbozkurt.clink.ui.components.EndGameDialog
import com.mehmetbozkurt.clink.ui.components.PosterDialog
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.SecondaryButton
import com.mehmetbozkurt.clink.ui.components.TopBar
import com.mehmetbozkurt.clink.ui.components.TopBarNav
import com.mehmetbozkurt.clink.ui.components.WaitingLabel
import com.mehmetbozkurt.clink.ui.components.WallChip
import com.mehmetbozkurt.clink.ui.components.needsAdultConfirmation
import com.mehmetbozkurt.clink.ui.screens.ScreenColumn
import com.mehmetbozkurt.clink.ui.screens.ScreenTitle
import com.mehmetbozkurt.clink.ui.screens.icon
import com.mehmetbozkurt.clink.ui.screens.title
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import com.mehmetbozkurt.clink.ui.theme.ClinkFonts
import com.mehmetbozkurt.clink.ui.theme.color
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The online room: the lobby while no game runs, the game while one does (spec §5). Guests follow the host's
 * lobby status, so nobody has to navigate. [onExit] leaves the screen once this phone is out of the lobby.
 */
@Composable
fun OnlineRoomScreen(onExit: () -> Unit) {
    val session = LocalAppContainer.current.lobbies.active.collectAsState().value
    if (session == null) {
        LaunchedEffect(Unit) { onExit() }
        return
    }
    OnlineRoom(session, onExit)
}

@Composable
private fun OnlineRoom(session: LobbySession, onExit: () -> Unit) {
    val snapshot by session.snapshot.collectAsState()
    val exit by session.exit.collectAsState()
    val offline by session.offline.collectAsState()
    val messages by session.chat.collectAsState()
    val scope = rememberCoroutineScope()
    var chatOpen by rememberSaveable { mutableStateOf(false) }
    // By id, not count: the chat keeps only the last 100 messages, so the count stops growing.
    var lastRead by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }
    // Ticks once a second so the host-reconnecting countdown counts down.
    val now by produceState(session.now()) {
        while (true) {
            delay(1_000)
            value = session.now()
        }
    }
    val lastMessage = messages.lastOrNull()?.id
    if (chatOpen) lastRead = lastMessage

    val snap = snapshot
    val meta = snap?.meta
    val host = if (meta == null || session.isHost) HostStatus.Online else hostStatus(meta.hostOnline, meta.hostSeenAt, now)
    val playing = meta?.status == LobbyStatus.PLAYING && snap.session != null

    LaunchedEffect(exit) { if (exit == LobbyExit.LEFT) onExit() }
    // A member already in the room is asked "18+?" too when the host switches to Spicy; saying no leaves.
    var adultOk by remember(session) { mutableStateOf(session.adultConfirmed) }
    val askAdult = !session.isHost && meta?.level?.needsAdultConfirmation() == true && !adultOk
    val farewell: StringResource? = when {
        exit == LobbyExit.CLOSED && !session.isHost -> Res.string.lobby_closed
        exit == LobbyExit.REMOVED -> Res.string.lobby_removed
        host == HostStatus.Gone -> Res.string.lobby_host_gone
        else -> null
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        onBackCompleted = {
            when {
                chatOpen -> chatOpen = false
                playing && session.isHost -> confirmEnd = true
                else -> confirmLeave = true
            }
        },
    )

    val chatButton: @Composable RowScope.() -> Unit = {
        ChatButton(unread = lastMessage != null && lastMessage != lastRead) { chatOpen = true }
    }
    val avatars = snap?.members.orEmpty().associate { it.uid to it.avatarId }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            snap == null || meta == null -> Unit
            playing -> {
                val game = snap.session!!
                val seat = game.playerUids.indexOf(session.me.uid).takeIf { it >= 0 }
                val me = OnlineSeat(seat, session.isHost)
                val send: (OnlineAction) -> Unit = { action -> scope.launch { session.send(action) } }
                val onClose = { if (session.isHost) confirmEnd = true else confirmLeave = true }
                when (game.game) {
                    GameId.TRUTH_OR_DARE -> OnlineTruthOrDare(game, me, send, onClose, chatButton)
                    GameId.SPIN_THE_BOTTLE -> OnlineSpinTheBottle(game, me, send, onClose, chatButton)
                    GameId.CUP_PONG -> {
                        val online = snap.members.filter { it.online }.map { it.uid }.toSet()
                        OnlineCupPong(
                            session = game,
                            me = me,
                            paused = isPaused(game, online),
                            send = send,
                            onBackToLobby = { scope.launch { session.endGame() } },
                            onClose = onClose,
                            topBarExtras = chatButton,
                        )
                    }
                }
            }
            else -> Lobby(session, snap, chatButton, onLeave = { confirmLeave = true })
        }

        // Status chips stack under the top bar: this phone's connection first, then the host's.
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (offline) StatusChip(stringResource(Res.string.offline_banner))
            if (host is HostStatus.Reconnecting) StatusChip(stringResource(Res.string.host_reconnecting, host.secondsLeft))
        }

        AnimatedVisibility(visible = chatOpen, enter = fadeIn() + slideInVertically { it / 3 }, exit = fadeOut() + slideOutVertically { it / 3 }) {
            ChatPanel(
                messages = messages,
                myUid = session.me.uid,
                avatarOf = { avatars[it] },
                onSend = { text -> scope.launch { session.sendChat(text) } },
                onClose = { chatOpen = false },
            )
        }
    }

    if (farewell != null) {
        PosterDialog(
            title = stringResource(farewell),
            confirmText = stringResource(Res.string.ok),
            cancelText = null,
            onConfirm = { session.leave() },
            onDismiss = { session.leave() },
        )
    } else if (askAdult) {
        AdultConfirmDialog(
            onConfirm = {
                session.adultConfirmed = true
                adultOk = true
            },
            onDismiss = { session.leave() },
        )
    } else if (confirmLeave) {
        PosterDialog(
            title = stringResource(if (session.isHost) Res.string.lobby_close_title else Res.string.lobby_leave_title),
            body = if (session.isHost) stringResource(Res.string.lobby_close_body) else null,
            confirmText = stringResource(if (session.isHost) Res.string.lobby_close_confirm else Res.string.lobby_leave_confirm),
            cancelText = stringResource(Res.string.cancel),
            onConfirm = { confirmLeave = false; session.leave() },
            onDismiss = { confirmLeave = false },
            destructive = true,
        )
    } else if (confirmEnd) {
        EndGameDialog(onConfirm = { confirmEnd = false; scope.launch { session.endGame() } }, onDismiss = { confirmEnd = false })
    }
}

@Composable
private fun StatusChip(text: String) {
    Text(
        text = text,
        modifier = Modifier.background(ClinkColors.Amber, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
        color = ClinkColors.Ink,
        style = MaterialTheme.typography.labelLarge,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Lobby(session: LobbySession, snap: LobbySnapshot, chatButton: @Composable RowScope.() -> Unit, onLeave: () -> Unit) {
    val container = LocalAppContainer.current
    val meta = snap.meta ?: return
    val scope = rememberCoroutineScope()
    val players = playerOrder(snap.members)
    var pair by rememberSaveable { mutableStateOf(listOf<String>()) }
    var removing by remember { mutableStateOf<Member?>(null) }
    var askAdult by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }
    var startFailed by remember { mutableStateOf(false) }
    // Cup Pong: the two picked players, if both are still here and online.
    val livePair = pair.filter { uid -> players.any { it.uid == uid && (it.online || it.uid == session.me.uid) } }
    val ready = players.count { it.online || it.uid == session.me.uid }
    val canStart = if (meta.game == GameId.CUP_PONG) livePair.size == 2 || (ready == 2 && pair.isEmpty()) else ready >= 2
    // The invite: the code and the link that opens the app (spec §5.1).
    val invite = stringResource(Res.string.lobby_invite_text, session.code, inviteLink(session.code))

    ScreenColumn {
        TopBar(navigation = TopBarNav.Close, onNavigate = onLeave, trailing = chatButton)
        ScreenTitle(meta.game.title(), overline = stringResource(Res.string.lobby_title))
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (session.isHost && players.size == 1) {
                Text(stringResource(Res.string.lobby_alone_hint), color = ClinkColors.Amber, style = MaterialTheme.typography.bodyMedium)
            }
            CodeCard(code = session.code, onShare = { container.share(invite) })
            if (session.isHost) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameId.entries.forEach { game ->
                        WallChip(
                            text = game.title(),
                            icon = game.icon(),
                            selected = game == meta.game,
                            compact = true,
                            onClick = { scope.launch { session.setGame(game, meta.level) } },
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Level.entries.forEach { level ->
                        WallChip(
                            text = level.name,
                            selected = level == meta.level,
                            color = level.color(),
                            compact = true,
                            onClick = {
                                if (level.needsAdultConfirmation()) askAdult = true else scope.launch { session.setGame(meta.game, level) }
                            },
                        )
                    }
                }
            } else {
                WallChip(text = meta.level.name, selected = true, color = meta.level.color(), compact = true)
            }
            Text(
                text = stringResource(Res.string.lobby_players, players.size, PlayerNames.MAX).toUpperCase(Locale.current),
                color = ClinkColors.Muted,
                style = MaterialTheme.typography.labelLarge,
            )
            if (session.isHost && meta.game == GameId.CUP_PONG && players.size > 2) {
                Text(stringResource(Res.string.lobby_pick_pair), color = ClinkColors.Amber, style = MaterialTheme.typography.bodyMedium)
            }
            players.forEach { member ->
                val picked = member.uid in livePair
                MemberRow(
                    member = member,
                    isHost = member.uid == meta.hostUid,
                    isMe = member.uid == session.me.uid,
                    picked = picked,
                    onPick = if (session.isHost && meta.game == GameId.CUP_PONG) {
                        { pair = if (picked) pair - member.uid else (livePair + member.uid).takeLast(2) }
                    } else {
                        null
                    },
                    onRemove = if (session.isHost && member.uid != session.me.uid) ({ removing = member }) else null,
                )
            }
        }
        if (session.isHost) {
            if (startFailed || !canStart) {
                Text(
                    text = stringResource(if (meta.game == GameId.CUP_PONG && ready > 2) Res.string.lobby_pick_pair else Res.string.lobby_need_two),
                    color = ClinkColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            PrimaryButton(
                text = stringResource(Res.string.lobby_start),
                enabled = canStart && !starting,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    starting = true
                    scope.launch {
                        val chosen = if (meta.game == GameId.CUP_PONG && livePair.size == 2) livePair else null
                        startFailed = !session.startGame(chosen)
                        starting = false
                    }
                },
            )
        } else {
            WaitingLabel(stringResource(Res.string.lobby_waiting_start))
        }
    }

    removing?.let { member ->
        PosterDialog(
            title = stringResource(Res.string.lobby_remove_title, member.name),
            confirmText = stringResource(Res.string.lobby_remove_confirm),
            cancelText = stringResource(Res.string.cancel),
            onConfirm = { removing = null; scope.launch { session.removePlayer(member.uid) } },
            onDismiss = { removing = null },
            destructive = true,
        )
    }
    if (askAdult) {
        AdultConfirmDialog(
            onConfirm = { askAdult = false; scope.launch { session.setGame(meta.game, Level.SPICY) } },
            onDismiss = { askAdult = false },
        )
    }
}

@Composable
internal fun CodeCard(code: String, onShare: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClinkColors.WallRaised, RoundedCornerShape(16.dp))
            .border(2.dp, ClinkColors.Acid, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(Res.string.lobby_code).toUpperCase(Locale.current), color = ClinkColors.Muted, style = MaterialTheme.typography.labelLarge)
        Text(
            text = code.toList().joinToString(" "),
            color = ClinkColors.Acid,
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = ClinkFonts.spray, fontSize = 40.sp),
            textAlign = TextAlign.Center,
        )
        SecondaryButton(text = stringResource(Res.string.lobby_invite), onClick = onShare, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun MemberRow(
    member: Member,
    isHost: Boolean,
    isMe: Boolean,
    picked: Boolean,
    onPick: (() -> Unit)?,
    onRemove: (() -> Unit)?,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (picked) ClinkColors.Acid.copy(alpha = 0.18f) else ClinkColors.WallRaised)
            .border(2.dp, if (picked) ClinkColors.Acid else Color.Transparent, shape)
            .then(if (onPick != null) Modifier.clickable(role = Role.Checkbox, onClick = onPick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box {
            AvatarBadge(avatarId = member.avatarId, name = member.name, size = 40.dp)
            // Presence dot: acid when online, grey when the phone dropped.
            Box(
                Modifier.align(Alignment.BottomEnd).size(12.dp)
                    .background(if (member.online || isMe) ClinkColors.Acid else ClinkColors.Muted, CircleShape)
                    .border(2.dp, ClinkColors.WallRaised, CircleShape),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(member.name, color = ClinkColors.Paper, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            if (isHost) Text(stringResource(Res.string.lobby_host_badge), color = ClinkColors.Amber, style = MaterialTheme.typography.labelMedium)
        }
        if (onRemove != null) {
            Text(
                text = "×",
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onRemove).padding(top = 4.dp),
                color = ClinkColors.Muted,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

