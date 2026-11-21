package com.mehmetbozkurt.clink.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The signed-in host and their profile for the screens: decides where Play leads ([gate]) and keeps the profile
 * the Welcome badge and the account screen show. The profile is loaded once per user; a profile that can't be read
 * counts as missing, so the host sets one up again instead of being stuck.
 */
class AccountSession(private val auth: AuthService, private val profiles: ProfileRepository) {
    var profile: Profile? by mutableStateOf(null)
        private set
    private var loadedFor: String? = null

    suspend fun gate(): PlayGate {
        val user = auth.user.value
        if (user == null || user.isAnonymous) {
            forget()
        } else if (loadedFor != user.uid) {
            profile = try {
                profiles.load(user.uid)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            loadedFor = user.uid
        }
        return playGate(user, profile)
    }

    /**
     * Saves the signed-in host's profile; throws if there is no host or the write is refused. Firestore applies the
     * write on the phone at once but only reports it done when the server answers; offline that can take forever, so
     * after [SERVER_WAIT_MILLIS] the save counts as done and Firestore sends it once the phone is back online.
     */
    suspend fun save(newProfile: Profile) {
        val user = auth.user.value ?: throw AuthException(AuthFailure.OTHER)
        withTimeoutOrNull(SERVER_WAIT_MILLIS) { profiles.save(user.uid, newProfile) }
        profile = newProfile
        loadedFor = user.uid
    }

    suspend fun signOut() {
        auth.signOut()
        forget()
    }

    private fun forget() {
        profile = null
        loadedFor = null
    }

    private companion object {
        const val SERVER_WAIT_MILLIS = 5_000L
    }
}
