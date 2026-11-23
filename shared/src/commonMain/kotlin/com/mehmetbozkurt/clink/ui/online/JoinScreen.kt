package com.mehmetbozkurt.clink.ui.online

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.mehmetbozkurt.clink.LocalAppContainer
import com.mehmetbozkurt.clink.account.Avatars
import com.mehmetbozkurt.clink.account.Profile
import com.mehmetbozkurt.clink.account.ProfileInput
import com.mehmetbozkurt.clink.account.ProfileNameResult
import com.mehmetbozkurt.clink.game.PlayerNames
import com.mehmetbozkurt.clink.online.CodeInfo
import com.mehmetbozkurt.clink.online.JoinResult
import com.mehmetbozkurt.clink.online.Member
import com.mehmetbozkurt.clink.online.Peek
import com.mehmetbozkurt.clink.online.RoomCode
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.err_code_invalid
import com.mehmetbozkurt.clink.resources.err_join_failed
import com.mehmetbozkurt.clink.resources.err_lobby_full
import com.mehmetbozkurt.clink.resources.err_lobby_not_found
import com.mehmetbozkurt.clink.resources.err_lobby_refused
import com.mehmetbozkurt.clink.resources.err_profile_name_blank
import com.mehmetbozkurt.clink.resources.join_button
import com.mehmetbozkurt.clink.resources.join_code_hint
import com.mehmetbozkurt.clink.resources.join_continue
import com.mehmetbozkurt.clink.resources.join_lobby_of
import com.mehmetbozkurt.clink.resources.join_title
import com.mehmetbozkurt.clink.resources.profile_avatar
import com.mehmetbozkurt.clink.resources.profile_name_hint
import com.mehmetbozkurt.clink.ui.components.AdultConfirmDialog
import com.mehmetbozkurt.clink.ui.components.ClinkTextField
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.TopBar
import com.mehmetbozkurt.clink.ui.components.TopBarNav
import com.mehmetbozkurt.clink.ui.components.needsAdultConfirmation
import com.mehmetbozkurt.clink.ui.screens.AvatarGrid
import com.mehmetbozkurt.clink.ui.screens.ScreenColumn
import com.mehmetbozkurt.clink.ui.screens.ScreenTitle
import com.mehmetbozkurt.clink.ui.screens.title
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import com.mehmetbozkurt.clink.ui.theme.ClinkFonts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val USER_WAIT_MILLIS = 5_000L

/** Room for spaces or a dash someone types between the code's halves. */
private const val CODE_INPUT_MAX = 12

/**
 * Joining a lobby (spec §5.1): the code (typed, or from an invite link), then a nickname and avatar. Guests without an
 * account are signed in anonymously; a signed-in user's profile is the starting point. A Spicy lobby asks "18+?".
 */
@Composable
fun JoinScreen(initialCode: String?, profile: Profile?, onJoined: () -> Unit, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf(initialCode.orEmpty()) }
    var name by rememberSaveable { mutableStateOf(profile?.displayName.orEmpty()) }
    var avatar by rememberSaveable { mutableStateOf(profile?.avatarId ?: Avatars.ids.first()) }
    var lobby by remember { mutableStateOf<CodeInfo?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<StringResource?>(null) }
    var askAdult by remember { mutableStateOf(false) }

    /** Runs [block] signed in (anonymously if needed); a failed sign-in shows the connection error. */
    fun withUser(block: suspend (uid: String) -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                val uid = container.auth.user.value?.uid ?: run {
                    container.auth.signInAnonymously()
                    // The user arrives through Firebase's auth events a moment later.
                    withTimeoutOrNull(USER_WAIT_MILLIS) { container.auth.user.first { it != null } }?.uid
                }
                if (uid == null) error = Res.string.err_join_failed else block(uid)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = Res.string.err_join_failed
            }
            busy = false
        }
    }

    fun findLobby() {
        val clean = RoomCode.normalize(code) ?: run { error = Res.string.err_code_invalid; return }
        withUser {
            when (val found = container.lobbies.peek(clean)) {
                is Peek.Found -> lobby = found.info
                Peek.NotFound -> error = Res.string.err_lobby_not_found
                Peek.Failed -> error = Res.string.err_join_failed
            }
        }
    }

    fun join() {
        withUser { uid ->
            val clean = ProfileInput.validate(name)
            if (clean !is ProfileNameResult.Valid) {
                error = Res.string.err_profile_name_blank
                return@withUser
            }
            when (val result = container.lobbies.join(code, Member(uid, clean.name, avatar, joinedAt = 0))) {
                is JoinResult.Joined -> {
                    if (lobby?.level?.needsAdultConfirmation() == true) result.session.adultConfirmed = true
                    onJoined()
                }
                JoinResult.NotFound -> error = Res.string.err_lobby_not_found
                JoinResult.Refused -> error = Res.string.err_lobby_refused
                JoinResult.Full -> error = Res.string.err_lobby_full
                JoinResult.Failed -> error = Res.string.err_join_failed
            }
        }
    }

    // On the name step, system back goes back to the code like the top bar's ‹ does.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = lobby != null,
        onBackCompleted = {
            lobby = null
            error = null
        },
    )

    // An invite link opens straight on the lobby.
    LaunchedEffect(initialCode) { if (initialCode != null && RoomCode.normalize(initialCode) != null) findLobby() }

    ScreenColumn {
        TopBar(navigation = TopBarNav.Back, onNavigate = { if (lobby != null) { lobby = null; error = null } else onBack() })
        val found = lobby
        ScreenTitle(stringResource(Res.string.join_title))
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (found == null) {
                ClinkTextField(
                    value = code,
                    // Not rewritten while typing (that fights the keyboard); RoomCode.normalize cleans it up on submit.
                    onChange = { code = it.take(CODE_INPUT_MAX); error = null },
                    hint = stringResource(Res.string.join_code_hint),
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Go,
                    onImeAction = ::findLobby,
                    textStyle = TextStyle(fontFamily = ClinkFonts.spray, fontSize = 32.sp),
                )
            } else {
                Text(
                    text = stringResource(Res.string.join_lobby_of, found.hostName, found.game.title()),
                    color = ClinkColors.Acid,
                    style = MaterialTheme.typography.titleMedium,
                )
                ClinkTextField(
                    value = name,
                    onChange = { name = it.take(PlayerNames.MAX_LENGTH); error = null },
                    hint = stringResource(Res.string.profile_name_hint),
                    capitalization = KeyboardCapitalization.Words,
                )
                Text(text = stringResource(Res.string.profile_avatar), color = ClinkColors.Muted, style = MaterialTheme.typography.labelLarge)
                AvatarGrid(selected = avatar, name = name, onSelect = { avatar = it })
            }
        }
        error?.let { Text(text = stringResource(it), color = ClinkColors.Amber, style = MaterialTheme.typography.bodyMedium) }
        PrimaryButton(
            text = stringResource(if (found == null) Res.string.join_continue else Res.string.join_button),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                when {
                    found == null -> findLobby()
                    found.level.needsAdultConfirmation() -> askAdult = true
                    else -> join()
                }
            },
        )
    }

    if (askAdult) {
        AdultConfirmDialog(onConfirm = { askAdult = false; join() }, onDismiss = { askAdult = false })
    }
}
