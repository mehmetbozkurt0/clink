package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.account.AccountSession
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.account_edit
import com.mehmetbozkurt.clink.resources.account_sign_out
import com.mehmetbozkurt.clink.resources.account_title
import com.mehmetbozkurt.clink.ui.components.AvatarBadge
import com.mehmetbozkurt.clink.ui.components.Poster
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.SecondaryButton
import com.mehmetbozkurt.clink.ui.components.TopBar
import com.mehmetbozkurt.clink.ui.components.TopBarNav
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** The host's account: profile on a poster, edit, sign out. Account deletion is added in Task 6. */
@Composable
fun AccountScreen(session: AccountSession, onEdit: () -> Unit, onSignedOut: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
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
        PrimaryButton(text = stringResource(Res.string.account_edit), onClick = onEdit, modifier = Modifier.fillMaxWidth())
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
    }
}
