package com.mehmetbozkurt.clink.account

import com.mehmetbozkurt.clink.prefs.LocalPrefs
import com.mehmetbozkurt.clink.prefs.LocalStore
import com.mehmetbozkurt.clink.prefs.PrefsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuestProfileTest {
    private class FakeStore(var text: String? = null) : LocalStore {
        override suspend fun read(): String? = text
        override suspend fun write(text: String) {
            this.text = text
        }
    }

    @Test
    fun guestSaveIsRemembered() = runTest {
        val prefs = PrefsRepository(FakeStore())
        saveGuestProfile(prefs, Profile("Ayşe", Avatars.ids[2]))
        assertEquals("Ayşe", prefs.prefs.value.guestName)
        assertEquals(Avatars.ids[2], prefs.prefs.value.guestAvatar)
    }

    @Test
    fun guestSaveKeepsOtherPrefs() = runTest {
        val prefs = PrefsRepository(FakeStore())
        prefs.update { LocalPrefs(joinTourSeen = true, hapticsOn = false) }
        saveGuestProfile(prefs, Profile("Ayşe", Avatars.ids[2]))
        assertTrue(prefs.prefs.value.joinTourSeen)
        assertFalse(prefs.prefs.value.hapticsOn)
    }
}
