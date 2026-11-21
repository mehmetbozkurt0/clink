package com.mehmetbozkurt.clink.account

import kotlinx.coroutines.flow.StateFlow

/** The signed-in user. Anonymous users are guests (Plan 4); they can't host. */
data class AuthUser(val uid: String, val isAnonymous: Boolean, val googleName: String?)

enum class AuthFailure { NETWORK, CANCELLED, RECENT_LOGIN_REQUIRED, OTHER }

class AuthException(val kind: AuthFailure, cause: Throwable? = null) : Exception(kind.name, cause)

/** Sign-in and account operations (spec §4). Every failure surfaces as an [AuthException]. */
interface AuthService {
    val user: StateFlow<AuthUser?>
    suspend fun signInWithGoogle(idToken: String)
    suspend fun signInAnonymously()

    /** Turns the current anonymous guest into a Google account, keeping the same uid. */
    suspend fun linkGoogle(idToken: String)
    suspend fun reauthenticateWithGoogle(idToken: String)
    suspend fun deleteUser()
    suspend fun signOut()
}
