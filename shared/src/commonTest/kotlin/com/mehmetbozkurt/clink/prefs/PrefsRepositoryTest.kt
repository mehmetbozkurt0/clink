package com.mehmetbozkurt.clink.prefs

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrefsRepositoryTest {
    private class FakeStore(var text: String? = null) : LocalStore {
        override suspend fun read(): String? = text
        override suspend fun write(text: String) {
            this.text = text
        }
    }

    @Test
    fun emptyStoreGivesDefaults() = runTest {
        val repository = PrefsRepository(FakeStore())
        repository.load()
        assertEquals(LocalPrefs(), repository.prefs.value)
    }

    @Test
    fun corruptFileGivesDefaults() = runTest {
        val repository = PrefsRepository(FakeStore("{not json"))
        repository.load()
        assertEquals(LocalPrefs(), repository.prefs.value)
    }

    @Test
    fun updateIsSavedAndReloaded() = runTest {
        val store = FakeStore()
        PrefsRepository(store).update { it.copy(joinTourSeen = true, guestName = "Ayşe") }

        val reloaded = PrefsRepository(store)
        reloaded.load()
        assertTrue(reloaded.prefs.value.joinTourSeen)
        assertEquals("Ayşe", reloaded.prefs.value.guestName)
    }

    @Test
    fun unknownFieldsAreIgnored() = runTest {
        val repository = PrefsRepository(FakeStore("""{"playTourSeen":true,"later":1}"""))
        repository.load()
        assertTrue(repository.prefs.value.playTourSeen)
    }
}
