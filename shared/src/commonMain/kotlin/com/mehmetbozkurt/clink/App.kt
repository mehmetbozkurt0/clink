package com.mehmetbozkurt.clink

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.mehmetbozkurt.clink.content.loadBundledContent
import com.mehmetbozkurt.clink.ui.components.ClinkBackground
import com.mehmetbozkurt.clink.ui.navigation.ClinkNavHost
import com.mehmetbozkurt.clink.ui.theme.ClinkTheme

@Composable
fun App() {
    ClinkTheme {
        ClinkBackground {
            // Until the bundled content is loaded, only the background shows.
            val container by produceState<AppContainer?>(initialValue = null) {
                value = AppContainer(loadBundledContent())
            }
            container?.let {
                CompositionLocalProvider(LocalAppContainer provides it) { ClinkNavHost() }
            }
        }
    }
}
