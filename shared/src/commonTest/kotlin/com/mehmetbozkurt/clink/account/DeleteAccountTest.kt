package com.mehmetbozkurt.clink.account

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

    /** [deleteFailures] are thrown by the first deleteUser calls, in order. */
    private class FakeAuth(start: AuthUser?, val deleteFailures: MutableList<AuthFailure> = mutableListOf()) : AuthService {
        override val user = MutableStateFlow(start)
        var deleted = false
        var reauthenticated = false
        override suspend fun signInWithGoogle(idToken: String) = Unit
        override suspend fun signInAnonymously() = Unit
        override suspend fun linkGoogle(idToken: String) = Unit
        override suspend fun reauthenticateWithGoogle(idToken: String) {
            reauthenticated = true
        }
        override suspend fun deleteUser() {
            if (deleteFailures.isNotEmpty()) throw AuthException(deleteFailures.removeAt(0))
            deleted = true
            user.value = null
        }
        override suspend fun signOut() {
            user.value = null
        }
    }

    private class FakeProfiles(val fail: Boolean = false) : ProfileRepository {
        val stored = mutableMapOf("u1" to Profile("Mehmet", "cat"))
        override suspend fun load(uid: String) = stored[uid]
        override suspend fun save(uid: String, profile: Profile) {
            stored[uid] = profile
        }
        override suspend fun delete(uid: String) {
            if (fail) throw AuthException(AuthFailure.NETWORK)
            stored.remove(uid)
        }
    }

    @Test
    fun deletesProfileThenUser() = runTest {
        val auth = FakeAuth(host)
        val profiles = FakeProfiles()
        assertEquals(DeleteResult.DELETED, deleteAccount(auth, profiles, reauth = { true }))
        assertNull(profiles.stored["u1"])
        assertTrue(auth.deleted)
        assertNull(auth.user.value)
    }

    @Test
    fun reauthenticatesOnceWhenRequired() = runTest {
        val auth = FakeAuth(host, mutableListOf(AuthFailure.RECENT_LOGIN_REQUIRED))
        var asked = 0
        assertEquals(DeleteResult.DELETED, deleteAccount(auth, FakeProfiles(), reauth = { asked++; true }))
        assertEquals(1, asked)
        assertTrue(auth.deleted)
    }

    @Test
    fun cancelledReauthKeepsTheAccount() = runTest {
        val auth = FakeAuth(host, mutableListOf(AuthFailure.RECENT_LOGIN_REQUIRED))
        assertEquals(DeleteResult.CANCELLED, deleteAccount(auth, FakeProfiles(), reauth = { false }))
        assertFalse(auth.deleted)
        assertNotNull(auth.user.value)
    }

    @Test
    fun networkFailureReportsFailed() = runTest {
        val auth = FakeAuth(host)
        val profiles = FakeProfiles(fail = true)
        assertEquals(DeleteResult.FAILED, deleteAccount(auth, profiles, reauth = { true }))
        assertFalse(auth.deleted)
        assertNotNull(profiles.stored["u1"])
    }

    @Test
    fun signedOutCannotDelete() = runTest {
        assertEquals(DeleteResult.FAILED, deleteAccount(FakeAuth(null), FakeProfiles(), reauth = { true }))
    }
}
