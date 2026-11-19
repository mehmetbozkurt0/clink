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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.back_to_games
import com.mehmetbozkurt.clink.resources.game_over
import com.mehmetbozkurt.clink.resources.play_again
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.SecondaryButton
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun GameOverScreen(game: GameId, onPlayAgain: () -> Unit, onBackToGames: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Text(text = game.title(), color = ClinkColors.Primary, style = MaterialTheme.typography.labelLarge)
        Text(text = stringResource(Res.string.game_over), color = ClinkColors.TextPrimary, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.weight(1f))
        PrimaryButton(text = stringResource(Res.string.play_again), onClick = onPlayAgain, modifier = Modifier.fillMaxWidth())
        SecondaryButton(text = stringResource(Res.string.back_to_games), onClick = onBackToGames, modifier = Modifier.fillMaxWidth())
    }
}
