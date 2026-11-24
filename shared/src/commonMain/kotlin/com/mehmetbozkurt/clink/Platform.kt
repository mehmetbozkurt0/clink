package com.mehmetbozkurt.clink

import com.mehmetbozkurt.clink.account.GoogleIdTokenProvider
import com.mehmetbozkurt.clink.content.ContentCache
import com.mehmetbozkurt.clink.prefs.LanguageSetting
import com.mehmetbozkurt.clink.prefs.LocalStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What only the platform can provide, passed in by MainActivity: file storage, the Google account picker, the share
 * sheet, invite links that opened the app, the app language, the version name, and whether the phone's animations
 * are turned off ([reduceMotion]).
 */
class Platform(
    val contentCache: ContentCache,
    val localStore: LocalStore,
    val googleIdTokens: GoogleIdTokenProvider,
    val share: (String) -> Unit,
    val incomingLinks: IncomingLinks,
    val language: LanguageSetting,
    val versionName: String,
    val reduceMotion: Boolean,
)

/** The latest invite link the app was opened with, until the app has acted on it. */
class IncomingLinks {
    private val _link = MutableStateFlow<String?>(null)
    val link: StateFlow<String?> = _link.asStateFlow()

    fun receive(url: String?) {
        if (url != null) _link.value = url
    }

    fun consume() {
        _link.value = null
    }
}
