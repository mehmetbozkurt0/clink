package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.coming_soon
import com.mehmetbozkurt.clink.resources.games_title
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

/** Beer Pong stays disabled until Plan 2. */
@Composable
fun GameListScreen(onGame: (GameId) -> Unit) {
    ScreenColumn {
        ScreenTitle(stringResource(Res.string.games_title))
        GameId.entries.forEach { game ->
            val available = game != GameId.BEER_PONG
            ChoiceCard(onClick = { onGame(game) }, enabled = available) {
                Text(text = game.title(), color = ClinkColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
                if (!available) {
                    Text(text = stringResource(Res.string.coming_soon), color = ClinkColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
