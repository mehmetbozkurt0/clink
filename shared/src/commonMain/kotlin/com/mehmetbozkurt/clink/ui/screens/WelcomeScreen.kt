package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehmetbozkurt.clink.game.FeaturedGames
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.cd_settings
import com.mehmetbozkurt.clink.resources.home_game_count
import com.mehmetbozkurt.clink.resources.ic_settings
import com.mehmetbozkurt.clink.resources.play
import com.mehmetbozkurt.clink.resources.tagline
import com.mehmetbozkurt.clink.resources.welcome_join
import com.mehmetbozkurt.clink.ui.components.LocalWallMotion
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.SecondaryButton
import com.mehmetbozkurt.clink.ui.components.TapeLabel
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import com.mehmetbozkurt.clink.ui.theme.ClinkFonts
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Welcome (spec 2026-11-24-home-and-settings §4): the settings gear top-right, the spray logo and tape tagline, the
 * game count sprayed on the wall, the three featured games as taped posters (decoration only), and PLAY / JOIN at the
 * bottom. The wall behind it has faint tags and a passing light, and its glows breathe while this screen shows —
 * all still when [animated] is false (the phone's animations are off).
 */
@Composable
fun WelcomeScreen(
    animated: Boolean,
    onPlay: () -> Unit,
    onJoin: () -> Unit,
    onSettings: () -> Unit,
) {
    val wallMotion = LocalWallMotion.current
    DisposableEffect(animated) {
        wallMotion.value = animated
        onDispose { wallMotion.value = false }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        HomeWallArt(animated = animated)
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterEnd) {
                val label = stringResource(Res.string.cd_settings)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = label, onClick = onSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_settings),
                        contentDescription = label,
                        modifier = Modifier.size(26.dp),
                        colorFilter = ColorFilter.tint(ClinkColors.Paper.copy(alpha = 0.85f)),
                    )
                }
            }
            Text(
                text = buildAnnotatedString {
                    append("clink")
                    withStyle(SpanStyle(color = ClinkColors.Pink)) { append(".") }
                },
                color = ClinkColors.Acid,
                style = TextStyle(
                    fontFamily = ClinkFonts.spray,
                    fontSize = 60.sp,
                    lineHeight = 64.sp,
                    shadow = Shadow(color = ClinkColors.Acid.copy(alpha = 0.35f), blurRadius = 24f),
                ),
            )
            Drips(modifier = Modifier.padding(start = 18.dp))
            Spacer(Modifier.height(10.dp))
            TapeLabel(stringResource(Res.string.tagline))
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Only a count, not a link: OYNA is the way to the games.
                Text(
                    text = stringResource(Res.string.home_game_count, GameId.entries.size),
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 4.dp).rotate(-3f),
                    color = ClinkColors.Acid,
                    style = TextStyle(fontFamily = ClinkFonts.marker, fontSize = 20.sp),
                )
                HomePosters(
                    games = FeaturedGames.ids,
                    animated = animated,
                    modifier = Modifier.fillMaxSize().padding(top = 34.dp, bottom = 12.dp),
                )
            }
            PrimaryButton(text = stringResource(Res.string.play), onClick = onPlay, modifier = Modifier.fillMaxWidth())
            // Guests with an invite code start here (spec §5.1); no account needed.
            SecondaryButton(
                text = stringResource(Res.string.welcome_join),
                onClick = onJoin,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }
    }
}

/** A few paint drips under the logo. */
@Composable
private fun Drips(modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(12, 22, 8, 17).forEach { length ->
            Box(
                Modifier
                    .size(width = 5.dp, height = length.dp)
                    .background(ClinkColors.Acid.copy(alpha = 0.85f), RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)),
            )
        }
    }
}
