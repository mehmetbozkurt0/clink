package com.mehmetbozkurt.clink.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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

    /** Saves the signed-in host's profile; throws if there is no host or the write fails. */
    suspend fun save(newProfile: Profile) {
        val user = auth.user.value ?: throw AuthException(AuthFailure.OTHER)
        profiles.save(user.uid, newProfile)
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
}
