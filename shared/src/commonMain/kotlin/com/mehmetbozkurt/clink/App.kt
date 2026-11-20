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
import com.mehmetbozkurt.clink.content.loadBundledContent
import com.mehmetbozkurt.clink.ui.components.WallBackground
import com.mehmetbozkurt.clink.ui.navigation.ClinkNavHost
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import com.mehmetbozkurt.clink.ui.theme.ClinkFonts
import com.mehmetbozkurt.clink.ui.theme.ClinkTheme

@Composable
fun App() {
    ClinkTheme {
        WallBackground {
            val container by produceState<AppContainer?>(initialValue = null) {
                value = AppContainer(loadBundledContent())
            }
            val loaded = container
            if (loaded == null) {
                // While the bundled content loads, show the logo instead of an empty wall.
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
