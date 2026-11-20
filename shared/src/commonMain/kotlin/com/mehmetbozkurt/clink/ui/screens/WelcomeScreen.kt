package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.play
import com.mehmetbozkurt.clink.resources.tagline
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.TapeLabel
import com.mehmetbozkurt.clink.ui.components.WallChip
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import com.mehmetbozkurt.clink.ui.theme.ClinkFonts
import org.jetbrains.compose.resources.stringResource

private val GameIconTints = mapOf(
    GameId.TRUTH_OR_DARE to ClinkColors.Acid,
    GameId.SPIN_THE_BOTTLE to ClinkColors.Pink,
    GameId.BEER_PONG to ClinkColors.Amber,
)

/** Welcome (design §4): spray logo with drips, tape tagline, the games as stickers, PLAY at the bottom. No top bar. */
@Composable
fun WelcomeScreen(onPlay: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(56.dp))
            Text(
                text = buildAnnotatedString {
                    append("clink")
                    withStyle(SpanStyle(color = ClinkColors.Pink)) { append(".") }
                },
                color = ClinkColors.Acid,
                style = TextStyle(
                    fontFamily = ClinkFonts.spray,
                    fontSize = 76.sp,
                    lineHeight = 80.sp,
                    shadow = Shadow(color = ClinkColors.Acid.copy(alpha = 0.35f), blurRadius = 24f),
                ),
            )
            Drips(modifier = Modifier.padding(start = 22.dp))
            Spacer(Modifier.height(14.dp))
            TapeLabel(stringResource(Res.string.tagline))
            Spacer(Modifier.height(32.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GameId.entries.forEach { game ->
                    WallChip(
                        text = game.title(),
                        icon = game.icon(),
                        iconTint = GameIconTints.getValue(game),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        PrimaryButton(text = stringResource(Res.string.play), onClick = onPlay, modifier = Modifier.fillMaxWidth())
    }
}

/** A few paint drips under the logo. */
@Composable
private fun Drips(modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(14, 26, 9, 20).forEach { length ->
            Box(
                Modifier
                    .size(width = 5.dp, height = length.dp)
                    .background(ClinkColors.Acid.copy(alpha = 0.85f), RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)),
            )
        }
    }
}
