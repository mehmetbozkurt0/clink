package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.LocalAppContainer
import com.mehmetbozkurt.clink.account.AccountSession
import com.mehmetbozkurt.clink.account.DeleteResult
import com.mehmetbozkurt.clink.account.GoogleIdToken
import com.mehmetbozkurt.clink.account.ReauthResult
import com.mehmetbozkurt.clink.account.deleteAccount
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.account_delete
import com.mehmetbozkurt.clink.resources.account_edit
import com.mehmetbozkurt.clink.resources.account_sign_out
import com.mehmetbozkurt.clink.resources.account_title
import com.mehmetbozkurt.clink.resources.cancel
import com.mehmetbozkurt.clink.resources.delete_body
import com.mehmetbozkurt.clink.resources.delete_confirm
import com.mehmetbozkurt.clink.resources.delete_title
import com.mehmetbozkurt.clink.resources.err_delete_failed
import com.mehmetbozkurt.clink.ui.components.AvatarBadge
import com.mehmetbozkurt.clink.ui.components.Poster
import com.mehmetbozkurt.clink.ui.components.PosterDialog
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.SecondaryButton
import com.mehmetbozkurt.clink.ui.components.TopBar
import com.mehmetbozkurt.clink.ui.components.TopBarNav
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The host's account: profile on a poster, edit, sign out, and delete (spec §4: in-app deletion). Deleting asks
 * first, then the user confirms by picking their Google account again; cancelling deletes nothing.
 */
@Composable
fun AccountScreen(
    session: AccountSession,
    onEdit: () -> Unit,
    onSignedOut: () -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var deleteFailed by remember { mutableStateOf(false) }
    val profile = session.profile

    ScreenColumn {
        TopBar(navigation = TopBarNav.Back, onNavigate = onBack)
        ScreenTitle(stringResource(Res.string.account_title))
        if (profile != null) {
            Poster(modifier = Modifier.fillMaxWidth(), rotation = -1f) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AvatarBadge(avatarId = profile.avatarId, name = profile.displayName, size = 64.dp)
                    Text(text = profile.displayName, color = ClinkColors.Ink, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (deleteFailed) {
            Text(stringResource(Res.string.err_delete_failed), color = ClinkColors.Amber, style = MaterialTheme.typography.bodyMedium)
        }
        PrimaryButton(text = stringResource(Res.string.account_edit), onClick = onEdit, modifier = Modifier.fillMaxWidth(), enabled = !busy)
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
