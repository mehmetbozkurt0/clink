package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.back_to_games
import com.mehmetbozkurt.clink.resources.game_over
import com.mehmetbozkurt.clink.resources.play_again
import com.mehmetbozkurt.clink.ui.components.AdultConfirmDialog
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.needsAdultConfirmation
import com.mehmetbozkurt.clink.ui.components.SecondaryButton
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

/** "Play again" starts a new session, so a Spicy one asks the 18+ question again. */
@Composable
fun GameOverScreen(game: GameId, level: Level, onPlayAgain: () -> Unit, onBackToGames: () -> Unit) {
    var askAdult by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Text(text = game.title(), color = ClinkColors.Acid, style = MaterialTheme.typography.labelLarge)
        Text(text = stringResource(Res.string.game_over), color = ClinkColors.Paper, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(Res.string.play_again),
            onClick = { if (level.needsAdultConfirmation()) askAdult = true else onPlayAgain() },
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(text = stringResource(Res.string.back_to_games), onClick = onBackToGames, modifier = Modifier.fillMaxWidth())
    }

    if (askAdult) {
        AdultConfirmDialog(
            onConfirm = {
                askAdult = false
                onPlayAgain()
            },
            onDismiss = { askAdult = false },
        )
    }
}
