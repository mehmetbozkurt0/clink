package com.mehmetbozkurt.clink.account

/** Asks the platform for a Google ID token (Android: Credential Manager's account picker). */
fun interface GoogleIdTokenProvider {
    suspend fun request(): GoogleIdToken
}

sealed interface GoogleIdToken {
    data class Ok(val idToken: String) : GoogleIdToken
    data object Cancelled : GoogleIdToken
    data object NoAccount : GoogleIdToken
    data class Failed(val message: String) : GoogleIdToken
}
