package com.mehmetbozkurt.clink.ui.screens

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
import androidx.compose.ui.Modifier
import com.mehmetbozkurt.clink.LocalAppContainer
import com.mehmetbozkurt.clink.account.AuthException
import com.mehmetbozkurt.clink.account.AuthFailure
import com.mehmetbozkurt.clink.account.AuthService
import com.mehmetbozkurt.clink.account.GoogleIdToken
import com.mehmetbozkurt.clink.account.GoogleIdTokenProvider
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.err_network
import com.mehmetbozkurt.clink.resources.err_no_google_account
import com.mehmetbozkurt.clink.resources.err_signin_failed
import com.mehmetbozkurt.clink.resources.signin_body
import com.mehmetbozkurt.clink.resources.signin_google
import com.mehmetbozkurt.clink.resources.signin_title
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.TopBar
import com.mehmetbozkurt.clink.ui.components.TopBarNav
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Sign-in (spec §4): the host needs a Google account. While the account picker is open the button can't be tapped
 * again; cancelling the picker shows nothing, other failures show a short reason in amber.
 */
@Composable
fun SignInScreen(onSignedIn: () -> Unit, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<StringResource?>(null) }

    ScreenColumn {
        TopBar(navigation = TopBarNav.Back, onNavigate = onBack)
        ScreenTitle(stringResource(Res.string.signin_title))
        Text(text = stringResource(Res.string.signin_body), color = ClinkColors.Paper, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        error?.let { Text(text = stringResource(it), color = ClinkColors.Amber, style = MaterialTheme.typography.bodyMedium) }
        PrimaryButton(
            text = stringResource(Res.string.signin_google),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (busy) return@PrimaryButton
                busy = true
                error = null
                scope.launch {
                    val result = signIn(container.googleIdTokens, container.auth)
                    busy = false
                    when (result) {
                        SignInResult.SignedIn -> onSignedIn()
                        SignInResult.Cancelled -> Unit
                        is SignInResult.Failed -> error = result.message
                    }
                }
            },
        )
    }
}

private sealed interface SignInResult {
    data object SignedIn : SignInResult
    data object Cancelled : SignInResult
    data class Failed(val message: StringResource) : SignInResult
}

private suspend fun signIn(tokens: GoogleIdTokenProvider, auth: AuthService): SignInResult =
    when (val token = tokens.request()) {
        GoogleIdToken.Cancelled -> SignInResult.Cancelled
        GoogleIdToken.NoAccount -> SignInResult.Failed(Res.string.err_no_google_account)
        is GoogleIdToken.Failed -> SignInResult.Failed(Res.string.err_signin_failed)
        is GoogleIdToken.Ok -> try {
            auth.signInWithGoogle(token.idToken)
            SignInResult.SignedIn
        } catch (e: CancellationException) {
            throw e
        } catch (e: AuthException) {
            SignInResult.Failed(if (e.kind == AuthFailure.NETWORK) Res.string.err_network else Res.string.err_signin_failed)
        }
    }
