package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.art_pong
import com.mehmetbozkurt.clink.resources.art_spin
import com.mehmetbozkurt.clink.resources.art_tod
import com.mehmetbozkurt.clink.resources.game_beerpong
import com.mehmetbozkurt.clink.resources.game_pong_meta
import com.mehmetbozkurt.clink.resources.game_pong_players
import com.mehmetbozkurt.clink.resources.game_spin
import com.mehmetbozkurt.clink.resources.game_spin_meta
import com.mehmetbozkurt.clink.resources.game_spin_players
import com.mehmetbozkurt.clink.resources.game_tod
import com.mehmetbozkurt.clink.resources.game_tod_meta
import com.mehmetbozkurt.clink.resources.game_tod_players
import com.mehmetbozkurt.clink.resources.ic_game_pong
import com.mehmetbozkurt.clink.resources.ic_game_spin
import com.mehmetbozkurt.clink.resources.ic_game_tod
import org.jetbrains.compose.resources.DrawableResource
import com.mehmetbozkurt.clink.ui.components.Poster
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

/** Shared layout pieces of the flow screens. */
@Composable
internal fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

/**
 * A poster-style screen title (design §4): Anton, upper case for the current language, last word in acid
 * ("NE OYNUYORUZ?"). [overline] is an optional small acid line above it.
 */
@Composable
internal fun ScreenTitle(text: String, overline: String? = null) {
    val upper = text.toUpperCase(Locale.current)
    val split = upper.lastIndexOf(' ')
    Column(modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)) {
        if (overline != null) {
            Text(text = overline.toUpperCase(Locale.current), color = ClinkColors.Acid, style = MaterialTheme.typography.labelLarge)
        }
        Text(
            text = buildAnnotatedString {
                append(if (split > 0) upper.substring(0, split + 1) else "")
                withStyle(SpanStyle(color = ClinkColors.Acid)) { append(if (split > 0) upper.substring(split + 1) else upper) }
            },
            color = ClinkColors.Paper,
            style = MaterialTheme.typography.headlineLarge.copy(lineHeight = 38.sp),
        )
    }
}

@Composable
internal fun GameId.icon(): DrawableResource = when (this) {
    GameId.TRUTH_OR_DARE -> Res.drawable.ic_game_tod
    GameId.SPIN_THE_BOTTLE -> Res.drawable.ic_game_spin
    GameId.BEER_PONG -> Res.drawable.ic_game_pong
}

/** The game's illustration on its poster (design §7; placeholders until the commissioned art arrives). */
internal fun GameId.art(): DrawableResource = when (this) {
    GameId.TRUTH_OR_DARE -> Res.drawable.art_tod
    GameId.SPIN_THE_BOTTLE -> Res.drawable.art_spin
    GameId.BEER_PONG -> Res.drawable.art_pong
}

/** Players and a one-line rule, e.g. "2–12 kişi · Kart çek, yap ya da ceza". */
@Composable
internal fun GameId.meta(): String = stringResource(
    when (this) {
        GameId.TRUTH_OR_DARE -> Res.string.game_tod_meta
        GameId.SPIN_THE_BOTTLE -> Res.string.game_spin_meta
        GameId.BEER_PONG -> Res.string.game_pong_meta
    },
)

/** Just the players, for the home screen's posters, e.g. "2–12 kişi". */
@Composable
internal fun GameId.players(): String = stringResource(
    when (this) {
        GameId.TRUTH_OR_DARE -> Res.string.game_tod_players
        GameId.SPIN_THE_BOTTLE -> Res.string.game_spin_players
        GameId.BEER_PONG -> Res.string.game_pong_players
    },
)

/** A tappable poster; disabled ones are dimmed and ignore taps. Text inside defaults to ink. */
@Composable
internal fun ChoiceCard(
    onClick: () -> Unit,
    enabled: Boolean = true,
    rotation: Float = 0f,
    content: @Composable ColumnScope.() -> Unit,
) {
    Poster(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick),
        rotation = rotation,
        content = content,
    )
}

@Composable
internal fun GameId.title(): String = stringResource(
    when (this) {
        GameId.TRUTH_OR_DARE -> Res.string.game_tod
        GameId.SPIN_THE_BOTTLE -> Res.string.game_spin
        GameId.BEER_PONG -> Res.string.game_beerpong
    },
)
