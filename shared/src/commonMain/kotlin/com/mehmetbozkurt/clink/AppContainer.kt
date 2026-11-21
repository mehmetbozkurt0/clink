package com.mehmetbozkurt.clink

import androidx.compose.runtime.staticCompositionLocalOf
import com.mehmetbozkurt.clink.account.AuthService
import com.mehmetbozkurt.clink.account.GoogleIdTokenProvider
import com.mehmetbozkurt.clink.account.ProfileRepository
import com.mehmetbozkurt.clink.content.ContentLibrary
import kotlin.random.Random

/** App-wide singletons. Game ViewModels take [library] and [random] from here; account screens take the rest. */
class AppContainer(
    val library: ContentLibrary,
    val auth: AuthService,
    val profiles: ProfileRepository,
    val googleIdTokens: GoogleIdTokenProvider,
    val random: Random = Random.Default,
)

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
