package com.mehmetbozkurt.clink.account

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountSessionTest {
    private val host = AuthUser("u1", isAnonymous = false, googleName = "Mehmet")
    private val cat = Profile("Mehmet", "cat")

    private class FakeAuth(start: AuthUser?) : AuthService {
        override val user = MutableStateFlow(start)
        override suspend fun signInWithGoogle(idToken: String) = Unit
        override suspend fun signInAnonymously() = Unit
        override suspend fun linkGoogle(idToken: String) = Unit
        override suspend fun reauthenticateWithGoogle(idToken: String) = Unit
        override suspend fun deleteUser() = Unit
        override suspend fun signOut() {
            user.value = null
        }
    }

    private class FakeProfiles(val stored: MutableMap<String, Profile> = mutableMapOf(), val fail: Boolean = false) : ProfileRepository {
        var loads = 0
        override suspend fun load(uid: String): Profile? {
            loads++
            if (fail) error("offline")
            return stored[uid]
        }
        override suspend fun save(uid: String, profile: Profile) {
            stored[uid] = profile
        }
        override suspend fun delete(uid: String) {
            stored.remove(uid)
        }
    }

    @Test
    fun signedOutGoesToSignIn() = runTest {
        assertEquals(PlayGate.SignIn, AccountSession(FakeAuth(null), FakeProfiles()).gate())
    }

    @Test
    fun hostWithStoredProfilePlays() = runTest {
        val session = AccountSession(FakeAuth(host), FakeProfiles(mutableMapOf("u1" to cat)))
        assertEquals(PlayGate.Play, session.gate())
        assertEquals(cat, session.profile)
    }

    @Test
    fun profileIsLoadedOncePerUser() = runTest {
        val profiles = FakeProfiles(mutableMapOf("u1" to cat))
        val session = AccountSession(FakeAuth(host), profiles)
        session.gate()
        session.gate()
        assertEquals(1, profiles.loads)
    }

    @Test
    fun unreadableProfileMeansSetUp() = runTest {
        assertEquals(PlayGate.SetUpProfile, AccountSession(FakeAuth(host), FakeProfiles(fail = true)).gate())
    }

    @Test
    fun savingSetsTheProfile() = runTest {
        val profiles = FakeProfiles()
        val session = AccountSession(FakeAuth(host), profiles)
        session.save(cat)
        assertEquals(cat, profiles.stored["u1"])
        assertEquals(PlayGate.Play, session.gate())
    }

    /** Offline, Firestore keeps the write and only confirms it when the server answers, which may be never. */
    private class OfflineProfiles : ProfileRepository {
        override suspend fun load(uid: String): Profile? = null
        override suspend fun save(uid: String, profile: Profile) = awaitCancellation()
        override suspend fun delete(uid: String) = Unit
    }

    @Test
    fun savingOfflineDoesNotWaitForever() = runTest {
        val session = AccountSession(FakeAuth(host), OfflineProfiles())
        session.save(cat)
        assertEquals(cat, session.profile)
    }

    @Test
    fun signingOutForgetsTheProfile() = runTest {
        val session = AccountSession(FakeAuth(host), FakeProfiles(mutableMapOf("u1" to cat)))
        session.gate()
        session.signOut()
        assertNull(session.profile)
        assertEquals(PlayGate.SignIn, session.gate())
    }
}
