package com.mehmetbozkurt.clink

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
import com.mehmetbozkurt.clink.ui.components.WallBackground
import com.mehmetbozkurt.clink.ui.navigation.ClinkNavHost
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import com.mehmetbozkurt.clink.ui.theme.ClinkFonts
import com.mehmetbozkurt.clink.ui.theme.ClinkTheme

@Composable
fun App(platform: Platform) {
    ClinkTheme {
        WallBackground {
            val container by produceState<AppContainer?>(initialValue = null) {
                val content = ContentRepository(platform.contentCache, FirestoreContentRemote(), ::loadBundledPacks)
                value = AppContainer(
                    library = content.load(),
                    auth = FirebaseAuthService(scope = this),
                    profiles = FirestoreProfileRepository(),
                    googleIdTokens = platform.googleIdTokens,
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
                CompositionLocalProvider(LocalAppContainer provides loaded) { ClinkNavHost() }
            }
        }
    }
}
