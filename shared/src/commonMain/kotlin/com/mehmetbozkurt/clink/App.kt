package com.mehmetbozkurt.clink

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalHapticFeedback
import com.mehmetbozkurt.clink.ui.components.SwitchableHaptics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.mehmetbozkurt.clink.account.FirebaseAuthService
import com.mehmetbozkurt.clink.account.FirestoreProfileRepository
import com.mehmetbozkurt.clink.content.ContentRepository
import com.mehmetbozkurt.clink.content.FirestoreContentRemote
import com.mehmetbozkurt.clink.content.loadBundledPacks
import com.mehmetbozkurt.clink.online.LobbyLauncher
import com.mehmetbozkurt.clink.online.RtdbLobbyRepository
import com.mehmetbozkurt.clink.prefs.PrefsRepository
import com.mehmetbozkurt.clink.ui.components.WallBackground
import com.mehmetbozkurt.clink.ui.navigation.ClinkNavHost
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import com.mehmetbozkurt.clink.ui.theme.ClinkFonts
import com.mehmetbozkurt.clink.ui.theme.ClinkTheme
import com.mehmetbozkurt.clink.content.ContentLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.random.Random

@Composable
fun App(platform: Platform) {
    ClinkTheme {
        WallBackground {
            val container by produceState<AppContainer?>(initialValue = null) {
                val content = ContentRepository(platform.contentCache, FirestoreContentRemote(), ::loadBundledPacks)
                val library = content.load()
                // Loaded before the container is published, so screens never see default prefs by mistake.
                val prefs = PrefsRepository(platform.localStore).also { it.load() }
                value = AppContainer(
                    library = library,
                    auth = FirebaseAuthService(scope = this),
                    profiles = FirestoreProfileRepository(),
                    googleIdTokens = platform.googleIdTokens,
                    lobbies = OnlineProcess.launcher(library),
                    share = platform.share,
                    incomingLinks = platform.incomingLinks,
                    prefs = prefs,
                    language = platform.language,
                    versionName = platform.versionName,
                    reduceMotion = platform.reduceMotion,
                )
                // Newer content is downloaded in the background and used from the next start on.
                content.refresh()
            }
            val loaded = container
            if (loaded == null) {
                // While the content loads, show the logo instead of an empty wall.
                Text(
                    text = buildAnnotatedString {
                        append("clink")
                        withStyle(SpanStyle(color = ClinkColors.Pink)) { append(".") }
                    },
                    modifier = Modifier.align(Alignment.Center),
                    color = ClinkColors.Acid,
                    style = TextStyle(fontFamily = ClinkFonts.spray, fontSize = 56.sp),
                )
            } else {
                // The settings switch sits in front of the phone's haptics for every screen.
                val platformHaptics = LocalHapticFeedback.current
                val haptics = remember(platformHaptics, loaded) { SwitchableHaptics(platformHaptics) { loaded.prefs.prefs.value.hapticsOn } }
                CompositionLocalProvider(LocalAppContainer provides loaded, LocalHapticFeedback provides haptics) { ClinkNavHost() }
            }
        }
    }
}

/**
 * One lobby launcher for the whole process. The Activity (and this composition) is rebuilt on a theme, language or
 * font change; a running lobby must survive that, and leaving it must finish even after its screen is gone.
 */
private object OnlineProcess {
    private var launcher: LobbyLauncher? = null

    fun launcher(library: ContentLibrary): LobbyLauncher =
        launcher ?: LobbyLauncher(RtdbLobbyRepository(), library, Random.Default, CoroutineScope(SupervisorJob() + Dispatchers.Main))
            .also { launcher = it }
}
