package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.coming_soon
import com.mehmetbozkurt.clink.resources.mode_local_title
import com.mehmetbozkurt.clink.resources.mode_local_sub
import com.mehmetbozkurt.clink.resources.mode_online_title
import com.mehmetbozkurt.clink.resources.mode_online_sub
import com.mehmetbozkurt.clink.resources.mode_title
import com.mehmetbozkurt.clink.ui.components.TopBar
import com.mehmetbozkurt.clink.ui.components.TopBarNav
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

/** Mode (design §4): two posters, one phone or online (everyone on their own phone). */
@Composable
fun ModeSelectScreen(game: GameId, onLocal: () -> Unit, onOnline: () -> Unit, onBack: () -> Unit) {
    ScreenColumn {
        TopBar(navigation = TopBarNav.Back, onNavigate = onBack)
        ScreenTitle(stringResource(Res.string.mode_title), overline = game.title())
        // Scrolls when a large font doesn't fit.
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ChoiceCard(onClick = onLocal, rotation = PosterTilts[0]) {
                ModeText(stringResource(Res.string.mode_local_title), stringResource(Res.string.mode_local_sub))
            }
            // A game whose online rules aren't written yet shows the card greyed out rather than hiding it.
            ChoiceCard(onClick = onOnline, enabled = game.playsOnline, rotation = PosterTilts[1]) {
                ModeText(
                    title = stringResource(Res.string.mode_online_title),
                    subtitle = if (game.playsOnline) {
                        stringResource(Res.string.mode_online_sub)
                    } else {
                        stringResource(Res.string.coming_soon)
                    },
                )
            }
        }
    }
}

@Composable
private fun ModeText(title: String, subtitle: String) {
    Text(text = title, color = ClinkColors.Ink, style = MaterialTheme.typography.headlineSmall)
    Text(
        text = subtitle,
        modifier = Modifier.padding(top = 6.dp),
        color = ClinkColors.InkMuted,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
    )
}
