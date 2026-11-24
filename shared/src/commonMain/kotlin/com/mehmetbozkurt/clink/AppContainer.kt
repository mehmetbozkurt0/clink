package com.mehmetbozkurt.clink

import androidx.compose.runtime.staticCompositionLocalOf
import com.mehmetbozkurt.clink.account.AuthService
import com.mehmetbozkurt.clink.account.GoogleIdTokenProvider
import com.mehmetbozkurt.clink.account.ProfileRepository
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.online.LobbyLauncher
import com.mehmetbozkurt.clink.prefs.LanguageSetting
import com.mehmetbozkurt.clink.prefs.PrefsRepository
import kotlin.random.Random

/**
 * App-wide singletons. Game ViewModels take [library] and [random] from here; account screens take the account parts;
 * online play takes [lobbies], [share] (the share sheet) and [incomingLinks] (invite links that opened the app); settings
 * take [language] and [versionName]; the home screen keeps still when [reduceMotion].
 */
class AppContainer(
    val library: ContentLibrary,
    val auth: AuthService,
    val profiles: ProfileRepository,
    val googleIdTokens: GoogleIdTokenProvider,
    val lobbies: LobbyLauncher,
    val share: (String) -> Unit,
    val incomingLinks: IncomingLinks,
    val prefs: PrefsRepository,
    val language: LanguageSetting,
    val versionName: String,
    val reduceMotion: Boolean,
    val random: Random = Random.Default,
)

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
