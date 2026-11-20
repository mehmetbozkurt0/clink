package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.games_title
import com.mehmetbozkurt.clink.ui.components.TopBar
import com.mehmetbozkurt.clink.ui.components.TopBarNav
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Slight, fixed tilts so the posters look pasted by hand. */
internal val PosterTilts = listOf(-1f, 1f, -0.5f)

/** Game list (design §4): one poster per game with its illustration, name, players and a one-line rule. */
@Composable
fun GameListScreen(onGame: (GameId) -> Unit, onBack: () -> Unit) {
    ScreenColumn {
        TopBar(navigation = TopBarNav.Back, onNavigate = onBack)
        ScreenTitle(stringResource(Res.string.games_title))
        Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            GameId.entries.forEachIndexed { index, game ->
                ChoiceCard(onClick = { onGame(game) }, rotation = PosterTilts[index % PosterTilts.size]) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = game.title().toUpperCase(Locale.current),
                                color = ClinkColors.Ink,
                                style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 28.sp),
                            )
                            Text(
                                text = game.meta(),
                                modifier = Modifier.padding(top = 6.dp),
                                color = ClinkColors.InkMuted,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Image(
                            painter = painterResource(game.art()),
                            contentDescription = null,
                            modifier = Modifier.size(76.dp),
                        )
                    }
                }
                Spacer(Modifier.size(14.dp))
            }
        }
    }
}
