package com.mehmetbozkurt.clink

import androidx.compose.runtime.staticCompositionLocalOf
import com.mehmetbozkurt.clink.content.ContentLibrary
import kotlin.random.Random

/** App-wide singletons. Game ViewModels take [library] and [random] from here. */
class AppContainer(val library: ContentLibrary, val random: Random = Random.Default)

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
