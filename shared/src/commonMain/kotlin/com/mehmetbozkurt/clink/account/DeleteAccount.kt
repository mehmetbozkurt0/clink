package com.mehmetbozkurt.clink.account

import kotlinx.coroutines.CancellationException

enum class DeleteResult { DELETED, CANCELLED, FAILED }

/**
 * Deletes the signed-in user's account in the app (spec §4): first the profile (the rules only let a signed-in owner
 * delete it), then the Firebase user, then signs out. When Firebase wants a recent sign-in, [reauth] asks the user to
 * sign in with Google again once (false = they cancelled). If that is cancelled, the account stays but the profile is
 * already gone; the next Play just asks for a profile again.
 */
suspend fun deleteAccount(auth: AuthService, profiles: ProfileRepository, reauth: suspend () -> Boolean): DeleteResult {
    val uid = auth.user.value?.uid ?: return DeleteResult.FAILED
    try {
        profiles.delete(uid)
        try {
            auth.deleteUser()
        } catch (e: AuthException) {
            if (e.kind != AuthFailure.RECENT_LOGIN_REQUIRED) throw e
            if (!reauth()) return DeleteResult.CANCELLED
            auth.deleteUser()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        return DeleteResult.FAILED
    }
    runCatching { auth.signOut() }
    return DeleteResult.DELETED
}
