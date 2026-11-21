package com.mehmetbozkurt.clink.account

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeleteAccountTest {
    private val host = AuthUser("u1", isAnonymous = false, googleName = "Mehmet")

    private class FakeAuth(start: AuthUser?, val deleteFailure: AuthFailure? = null) : AuthService {
        override val user = MutableStateFlow(start)
        var deleted = false
        override suspend fun signInWithGoogle(idToken: String) = Unit
        override suspend fun signInAnonymously() = Unit
        override suspend fun linkGoogle(idToken: String) = Unit
        override suspend fun reauthenticateWithGoogle(idToken: String) = Unit
        override suspend fun deleteUser() {
            if (deleteFailure != null) throw AuthException(deleteFailure)
            deleted = true
            user.value = null
        }
        override suspend fun signOut() {
            user.value = null
        }
    }

    private class FakeProfiles(val fail: Boolean = false, val hang: Boolean = false) : ProfileRepository {
        val stored = mutableMapOf("u1" to Profile("Mehmet", "cat"))
        override suspend fun load(uid: String) = stored[uid]
        override suspend fun save(uid: String, profile: Profile) {
            stored[uid] = profile
        }
        override suspend fun delete(uid: String) {
            if (fail) throw AuthException(AuthFailure.NETWORK)
            if (hang) awaitCancellation()
            stored.remove(uid)
        }
    }

    @Test
    fun confirmsThenDeletesProfileAndUser() = runTest {
        val auth = FakeAuth(host)
        val profiles = FakeProfiles()
        var asked = 0
        assertEquals(DeleteResult.DELETED, deleteAccount(auth, profiles) { asked++; ReauthResult.DONE })
        assertEquals(1, asked)
        assertNull(profiles.stored["u1"])
        assertTrue(auth.deleted)
        assertNull(auth.user.value)
    }

    // Cancelling the account picker (or being offline, where it fails) must leave everything as it was.
    @Test
    fun cancelledConfirmationTouchesNothing() = runTest {
        val auth = FakeAuth(host)
        val profiles = FakeProfiles()
        assertEquals(DeleteResult.CANCELLED, deleteAccount(auth, profiles) { ReauthResult.CANCELLED })
        assertNotNull(profiles.stored["u1"])
        assertFalse(auth.deleted)
    }

    @Test
    fun failedConfirmationTouchesNothing() = runTest {
        val auth = FakeAuth(host)
        val profiles = FakeProfiles()
        assertEquals(DeleteResult.FAILED, deleteAccount(auth, profiles) { ReauthResult.FAILED })
        assertNotNull(profiles.stored["u1"])
        assertFalse(auth.deleted)
    }

    @Test
    fun profileDeleteThatNeverFinishesFails() = runTest {
        val auth = FakeAuth(host)
        assertEquals(DeleteResult.FAILED, deleteAccount(auth, FakeProfiles(hang = true)) { ReauthResult.DONE })
        assertFalse(auth.deleted)
    }

    @Test
    fun networkFailureReportsFailed() = runTest {
        val auth = FakeAuth(host)
        val profiles = FakeProfiles(fail = true)
        assertEquals(DeleteResult.FAILED, deleteAccount(auth, profiles) { ReauthResult.DONE })
        assertFalse(auth.deleted)
        assertNotNull(profiles.stored["u1"])
    }

    @Test
    fun userDeleteFailureReportsFailed() = runTest {
        val auth = FakeAuth(host, deleteFailure = AuthFailure.OTHER)
        assertEquals(DeleteResult.FAILED, deleteAccount(auth, FakeProfiles()) { ReauthResult.DONE })
    }

    @Test
    fun signedOutCannotDelete() = runTest {
        var asked = 0
        assertEquals(DeleteResult.FAILED, deleteAccount(FakeAuth(null), FakeProfiles()) { asked++; ReauthResult.DONE })
        assertEquals(0, asked)
    }
}
