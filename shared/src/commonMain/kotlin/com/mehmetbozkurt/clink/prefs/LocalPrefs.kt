package com.mehmetbozkurt.clink.prefs

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Where the local preferences are stored on the phone, as text (the platform only reads and writes a file). */
interface LocalStore {
    suspend fun read(): String?
    suspend fun write(text: String)
}

/** Things the app remembers on this phone only: which tours were seen, and the guest's name and avatar. */
@Serializable
data class LocalPrefs(
    val joinTourSeen: Boolean = false,
    val playTourSeen: Boolean = false,
    val guestName: String? = null,
    val guestAvatar: String? = null,
)

/** Holds [LocalPrefs] in memory for the UI and saves every change; a missing or broken file simply means defaults. */
class PrefsRepository(private val store: LocalStore) {
    private val _prefs = MutableStateFlow(LocalPrefs())
    val prefs: StateFlow<LocalPrefs> = _prefs.asStateFlow()

    suspend fun load() {
        _prefs.value = try {
            store.read()?.let { json.decodeFromString(LocalPrefs.serializer(), it) } ?: LocalPrefs()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            LocalPrefs()
        }
    }

    suspend fun update(change: (LocalPrefs) -> LocalPrefs) {
        val next = change(_prefs.value)
        _prefs.value = next
        try {
            store.write(json.encodeToString(LocalPrefs.serializer(), next))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Not saved: the choice still holds for this session, and the next change tries again.
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
