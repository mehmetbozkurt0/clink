package com.mehmetbozkurt.clink.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.LocalAppContainer
import com.mehmetbozkurt.clink.account.AccountSession
import com.mehmetbozkurt.clink.account.Avatars
import com.mehmetbozkurt.clink.account.DeleteResult
import com.mehmetbozkurt.clink.account.GoogleIdToken
import com.mehmetbozkurt.clink.account.ReauthResult
import com.mehmetbozkurt.clink.account.deleteAccount
import com.mehmetbozkurt.clink.prefs.AppLanguage
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.account_delete
import com.mehmetbozkurt.clink.resources.account_sign_out
import com.mehmetbozkurt.clink.resources.cancel
import com.mehmetbozkurt.clink.resources.cd_edit_profile
import com.mehmetbozkurt.clink.resources.delete_body
import com.mehmetbozkurt.clink.resources.delete_confirm
import com.mehmetbozkurt.clink.resources.delete_title
import com.mehmetbozkurt.clink.resources.err_delete_failed
import com.mehmetbozkurt.clink.resources.how_to_play
import com.mehmetbozkurt.clink.resources.ic_edit
import com.mehmetbozkurt.clink.resources.language_en
import com.mehmetbozkurt.clink.resources.language_system
import com.mehmetbozkurt.clink.resources.language_tr
import com.mehmetbozkurt.clink.resources.settings_guest
import com.mehmetbozkurt.clink.resources.settings_guest_name
import com.mehmetbozkurt.clink.resources.settings_haptics
import com.mehmetbozkurt.clink.resources.settings_how_to_join
import com.mehmetbozkurt.clink.resources.settings_language
import com.mehmetbozkurt.clink.resources.settings_section_account
import com.mehmetbozkurt.clink.resources.settings_section_game
import com.mehmetbozkurt.clink.resources.settings_section_help
import com.mehmetbozkurt.clink.resources.settings_signed_in_google
import com.mehmetbozkurt.clink.resources.settings_title
import com.mehmetbozkurt.clink.resources.settings_version
import com.mehmetbozkurt.clink.ui.components.AvatarBadge
import com.mehmetbozkurt.clink.ui.components.Poster
import com.mehmetbozkurt.clink.ui.components.PosterDialog
import com.mehmetbozkurt.clink.ui.components.SecondaryButton
import com.mehmetbozkurt.clink.ui.components.TopBar
import com.mehmetbozkurt.clink.ui.components.TopBarNav
import com.mehmetbozkurt.clink.ui.components.WallChip
import com.mehmetbozkurt.clink.ui.screens.ScreenColumn
import com.mehmetbozkurt.clink.ui.screens.ScreenTitle
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Settings (spec 2026-11-24-home-and-settings §5): the profile card, language and haptics, the two tours, and — for a
 * Google account only — sign out and delete (moved unchanged from the old account screen: deleting asks first, then
 * the user confirms by picking their Google account again; cancelling deletes nothing). The version sits at the end.
 */
@Composable
fun SettingsScreen(
    session: AccountSession,
    onEditProfile: () -> Unit,
    onPlayTour: () -> Unit,
    onJoinTour: () -> Unit,
    onSignedOut: () -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val user by container.auth.user.collectAsState()
    val prefs by container.prefs.prefs.collectAsState()
    val language = remember { container.language.current() }
    var busy by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var deleteFailed by remember { mutableStateOf(false) }

    ScreenColumn {
        TopBar(navigation = TopBarNav.Back, onNavigate = onBack)
        ScreenTitle(stringResource(Res.string.settings_title))
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProfileCardView(profileCard(user, session.profile, prefs), onEdit = onEditProfile, enabled = !busy)

            SectionLabel(Res.string.settings_section_game)
            Text(stringResource(Res.string.settings_language), color = ClinkColors.Paper, style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    AppLanguage.SYSTEM to Res.string.language_system,
                    AppLanguage.TURKISH to Res.string.language_tr,
                    AppLanguage.ENGLISH to Res.string.language_en,
                ).forEach { (option, label) ->
                    WallChip(
                        text = stringResource(label),
                        selected = option == language,
                        onClick = { if (option != language) container.language.set(option) },
                    )
                }
            }
            // The whole row is one labelled switch, so screen readers hear "Haptics, on" and the text is tappable too.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(
                        value = prefs.hapticsOn,
                        role = Role.Switch,
                        onValueChange = { on -> scope.launch { container.prefs.update { it.copy(hapticsOn = on) } } },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.settings_haptics),
                    modifier = Modifier.weight(1f),
                    color = ClinkColors.Paper,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = prefs.hapticsOn,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ClinkColors.Ink,
                        checkedTrackColor = ClinkColors.Acid,
                        uncheckedThumbColor = ClinkColors.Muted,
                        uncheckedTrackColor = ClinkColors.WallRaised,
                        uncheckedBorderColor = ClinkColors.Muted,
                    ),
                )
            }

            SectionLabel(Res.string.settings_section_help)
            LinkRow(Res.string.how_to_play, onPlayTour)
            LinkRow(Res.string.settings_how_to_join, onJoinTour)

            if (showsAccountSection(user)) {
                SectionLabel(Res.string.settings_section_account)
                if (deleteFailed) {
                    Text(stringResource(Res.string.err_delete_failed), color = ClinkColors.Amber, style = MaterialTheme.typography.bodyMedium)
                }
                SecondaryButton(
                    text = stringResource(Res.string.account_sign_out),
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (busy) return@SecondaryButton
                        busy = true
                        scope.launch {
                            runCatching { session.signOut() }
                            busy = false
                            onSignedOut()
                        }
                    },
                )
                // A quiet text button: deleting is rare and shouldn't compete with the main actions.
                Text(
                    text = stringResource(Res.string.account_delete),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(enabled = !busy, role = Role.Button) { askDelete = true }
                        .wrapContentHeight(Alignment.CenterVertically),
                    color = ClinkColors.Amber,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = stringResource(Res.string.settings_version, container.versionName),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                color = ClinkColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (askDelete) {
        PosterDialog(
            title = stringResource(Res.string.delete_title),
            body = stringResource(Res.string.delete_body),
            confirmText = stringResource(Res.string.delete_confirm),
            cancelText = stringResource(Res.string.cancel),
            destructive = true,
            onDismiss = { askDelete = false },
            onConfirm = {
                askDelete = false
                if (busy) return@PosterDialog
                busy = true
                deleteFailed = false
                scope.launch {
                    val result = deleteAccount(container.auth, container.profiles) {
                        when (val token = container.googleIdTokens.request()) {
                            is GoogleIdToken.Ok ->
                                if (runCatching { container.auth.reauthenticateWithGoogle(token.idToken) }.isSuccess) ReauthResult.DONE
                                else ReauthResult.FAILED
                            GoogleIdToken.Cancelled -> ReauthResult.CANCELLED
                            else -> ReauthResult.FAILED
                        }
                    }
                    busy = false
                    when (result) {
                        DeleteResult.DELETED -> onDeleted()
                        DeleteResult.CANCELLED -> Unit
                        DeleteResult.FAILED -> {
                            deleteFailed = true
                            // A delete that timed out may still reach the server; reload the profile on the next Play.
                            session.reset()
                        }
                    }
                }
            },
        )
    }
}

/** Avatar and name on a poster, with who this is underneath and a pencil to edit. */
@Composable
private fun ProfileCardView(card: ProfileCard, onEdit: () -> Unit, enabled: Boolean) {
    val name: String
    val avatar: String?
    val subtitle: StringResource
    when (card) {
        is ProfileCard.Google -> {
            name = card.name
            avatar = card.avatarId
            subtitle = Res.string.settings_signed_in_google
        }
        is ProfileCard.Guest -> {
            name = card.name ?: stringResource(Res.string.settings_guest_name)
            avatar = card.avatarId
            subtitle = Res.string.settings_guest
        }
    }
    Poster(modifier = Modifier.fillMaxWidth(), rotation = -1f) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // A guest without a chosen avatar gets the first one, dimmed.
            AvatarBadge(
                avatarId = avatar ?: Avatars.ids.first(),
                name = name,
                size = 64.dp,
                modifier = if (avatar == null) Modifier.alpha(0.45f) else Modifier,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, color = ClinkColors.Ink, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
                Text(text = stringResource(subtitle), color = ClinkColors.InkMuted, style = MaterialTheme.typography.bodyMedium)
            }
            val label = stringResource(Res.string.cd_edit_profile)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_edit),
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(ClinkColors.Ink),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: StringResource) {
    Text(
        text = stringResource(text),
        modifier = Modifier.padding(top = 14.dp),
        color = ClinkColors.Muted,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun LinkRow(text: StringResource, onClick: () -> Unit) {
    Text(
        text = stringResource(text),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .wrapContentHeight(Alignment.CenterVertically),
        color = ClinkColors.Paper,
        style = MaterialTheme.typography.bodyLarge,
    )
}
