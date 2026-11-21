package com.mehmetbozkurt.clink.account

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseAuthRecentLoginRequiredException
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** [AuthService] on Firebase Authentication (GitLive SDK). The session survives restarts, so signing in is needed once. */
class FirebaseAuthService(scope: CoroutineScope, private val auth: FirebaseAuth = Firebase.auth) : AuthService {

    override val user: StateFlow<AuthUser?> =
        auth.authStateChanged.map { it?.toAuthUser() }.stateIn(scope, SharingStarted.Eagerly, auth.currentUser?.toAuthUser())

    override suspend fun signInWithGoogle(idToken: String) = guarded {
        auth.signInWithCredential(GoogleAuthProvider.credential(idToken, null))
    }

    override suspend fun signInAnonymously() = guarded { auth.signInAnonymously() }

    override suspend fun linkGoogle(idToken: String) = guarded {
        current().linkWithCredential(GoogleAuthProvider.credential(idToken, null))
    }

    override suspend fun reauthenticateWithGoogle(idToken: String) = guarded {
        current().reauthenticate(GoogleAuthProvider.credential(idToken, null))
    }

    override suspend fun deleteUser() = guarded { current().delete() }

    override suspend fun signOut() = guarded { auth.signOut() }

    private fun current(): FirebaseUser = auth.currentUser ?: throw AuthException(AuthFailure.OTHER)

    /** Maps Firebase errors to [AuthFailure]; cancellation passes through. */
    private suspend fun guarded(block: suspend () -> Any?) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: AuthException) {
            throw e
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            throw AuthException(AuthFailure.RECENT_LOGIN_REQUIRED, e)
        } catch (e: FirebaseNetworkException) {
            throw AuthException(AuthFailure.NETWORK, e)
        } catch (e: Exception) {
            throw AuthException(AuthFailure.OTHER, e)
        }
    }
}

private fun FirebaseUser.toAuthUser() = AuthUser(uid = uid, isAnonymous = isAnonymous, googleName = displayName)
