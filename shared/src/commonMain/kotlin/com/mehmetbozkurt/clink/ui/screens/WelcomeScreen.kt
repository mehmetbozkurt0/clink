package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.play
import com.mehmetbozkurt.clink.resources.tagline
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun WelcomeScreen(onPlay: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = buildAnnotatedString {
                append("clink")
                withStyle(SpanStyle(color = ClinkColors.Primary)) { append(".") }
            },
            color = ClinkColors.TextPrimary,
            style = MaterialTheme.typography.displayLarge.copy(letterSpacing = (-0.02).em),
        )
        Text(text = stringResource(Res.string.tagline), color = ClinkColors.TextMuted, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GameId.entries.forEach { game ->
                Text(
                    text = game.title(),
                    modifier = Modifier.background(ClinkColors.Surface, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
                    color = ClinkColors.TextPrimary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(text = stringResource(Res.string.play), onClick = onPlay, modifier = Modifier.fillMaxWidth())
    }
}
