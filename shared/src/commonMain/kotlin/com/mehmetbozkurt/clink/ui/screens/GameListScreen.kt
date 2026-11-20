package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.games_title
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun GameListScreen(onGame: (GameId) -> Unit) {
    ScreenColumn {
        ScreenTitle(stringResource(Res.string.games_title))
        GameId.entries.forEach { game ->
            ChoiceCard(onClick = { onGame(game) }) {
                Text(text = game.title(), color = ClinkColors.Ink, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
