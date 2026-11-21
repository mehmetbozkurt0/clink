package com.mehmetbozkurt.clink.account

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

enum class DeleteResult { DELETED, CANCELLED, FAILED }

/** Signing in with Google again to confirm a deletion: done, cancelled by the user, or failed (offline, wrong account…). */
enum class ReauthResult { DONE, CANCELLED, FAILED }

private const val SERVER_WAIT_MILLIS = 10_000L

/**
 * Deletes the signed-in user's account in the app (spec §4). The user first confirms by signing in with Google again
 * ([reauth]); that also gives Firebase the fresh sign-in it needs to delete a user, and it needs a connection, so
 * cancelling or being offline stops here with nothing deleted. Then the profile goes (the rules only let the signed-in
 * owner delete it), then the Firebase user, then the session is signed out.
 */
suspend fun deleteAccount(auth: AuthService, profiles: ProfileRepository, reauth: suspend () -> ReauthResult): DeleteResult {
    val uid = auth.user.value?.uid ?: return DeleteResult.FAILED
    when (reauth()) {
        ReauthResult.DONE -> Unit
        ReauthResult.CANCELLED -> return DeleteResult.CANCELLED
        ReauthResult.FAILED -> return DeleteResult.FAILED
    }
    try {
        // Firestore only reports a delete as done when the server answers; don't wait forever if the line drops.
        withTimeout(SERVER_WAIT_MILLIS) { profiles.delete(uid) }
        auth.deleteUser()
    } catch (e: CancellationException) {
        if (e is kotlinx.coroutines.TimeoutCancellationException) return DeleteResult.FAILED
        throw e
    } catch (_: Exception) {
        return DeleteResult.FAILED
    }
    runCatching { auth.signOut() }
    return DeleteResult.DELETED
}
