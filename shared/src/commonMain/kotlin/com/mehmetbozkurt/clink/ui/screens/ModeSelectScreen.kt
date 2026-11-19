package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.coming_soon
import com.mehmetbozkurt.clink.resources.mode_local
import com.mehmetbozkurt.clink.resources.mode_local_sub
import com.mehmetbozkurt.clink.resources.mode_online
import com.mehmetbozkurt.clink.resources.mode_title
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

/** Online stays disabled until Plan 4. */
@Composable
fun ModeSelectScreen(game: GameId, onLocal: () -> Unit) {
    ScreenColumn {
        ScreenTitle(stringResource(Res.string.mode_title), overline = game.title())
        ChoiceCard(onClick = onLocal) {
            Text(text = stringResource(Res.string.mode_local), color = ClinkColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
            Text(text = stringResource(Res.string.mode_local_sub), color = ClinkColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
        }
        ChoiceCard(onClick = {}, enabled = false) {
            Text(text = stringResource(Res.string.mode_online), color = ClinkColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
            Text(text = stringResource(Res.string.coming_soon), color = ClinkColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
