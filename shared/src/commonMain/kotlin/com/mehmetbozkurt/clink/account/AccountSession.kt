package com.mehmetbozkurt.clink.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The signed-in host and their profile for the screens: decides where Play leads ([gate]) and keeps the profile
 * the Welcome badge and the account screen show. The profile is loaded once per user. A profile that can't be read
 * sends the host to setup this time and is read again next time, so a passing network error never makes them
 * overwrite their real profile. Loads and saves run one at a time, so a slow load can't undo a save.
 */
class AccountSession(private val auth: AuthService, private val profiles: ProfileRepository) {
    var profile: Profile? by mutableStateOf(null)
        private set
    private var loadedFor: String? = null
    private val lock = Mutex()

    suspend fun gate(): PlayGate = lock.withLock {
        val user = auth.user.value
        if (user == null || user.isAnonymous) {
            forget()
        } else if (loadedFor != user.uid) {
            try {
                profile = profiles.load(user.uid)
                loadedFor = user.uid
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                profile = null
            }
        }
        playGate(user, profile)
    }

    /**
     * Saves the signed-in host's profile; throws if there is no host or the write is refused. Firestore applies the
     * write on the phone at once but only reports it done when the server answers; offline that can take forever, so
     * after [SERVER_WAIT_MILLIS] the save counts as done and Firestore sends it once the phone is back online.
     */
    suspend fun save(newProfile: Profile) = lock.withLock {
        val user = auth.user.value ?: throw AuthException(AuthFailure.OTHER)
        withTimeoutOrNull(SERVER_WAIT_MILLIS) { profiles.save(user.uid, newProfile) }
        profile = newProfile
        loadedFor = user.uid
    }

    suspend fun signOut() {
        auth.signOut()
        lock.withLock { forget() }
    }

    /** Forgets the profile so the next [gate] reads it again (after a delete that may or may not have reached the server). */
    fun reset() = forget()

    private fun forget() {
        profile = null
        loadedFor = null
    }

    private companion object {
        const val SERVER_WAIT_MILLIS = 5_000L
    }
}
