package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.cd_home_posters
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

private val PosterShape = RoundedCornerShape(4.dp)

/** Each poster's tilt, background and sway phase, in the order of the approved sketch (spec §4): top left, top right, bottom. */
private data class PosterLook(val tilt: Float, val color: Color, val phaseMillis: Int)

private val Looks = listOf(
    PosterLook(-6f, ClinkColors.Paper, 0),
    PosterLook(5f, ClinkColors.Acid, 1_300),
    PosterLook(-2f, ClinkColors.Pink, 2_400),
)

/** Poster height ÷ width (the sketch's 118 × 150). */
private const val ASPECT = 150f / 118f

/**
 * The home screen's three featured games as taped posters (spec 2026-11-24-home-and-settings §4): two on top, one below
 * in the middle overlapping both. Decoration only — not tappable; screen readers hear one line with the game names.
 * They fill the space they are given, shrinking so all three always fit; [animated] lets each sway ±1° every ~4 s.
 */
@Composable
fun HomePosters(games: List<GameId>, animated: Boolean, modifier: Modifier = Modifier) {
    val titles = games.map { it.title() }
    val label = stringResource(Res.string.cd_home_posters, titles.joinToString(", "))
    BoxWithConstraints(modifier = modifier.clearAndSetSemantics { contentDescription = label }) {
        // The cluster is 2.12 poster widths wide and 1.95 poster heights tall.
        val width = min(maxWidth * 0.40f, maxHeight / (1.95f * ASPECT + 0.1f))
        val height = width * ASPECT
        val left = (maxWidth - width * 2.12f) / 2f
        val top = (maxHeight - height * 1.95f) / 2f
        val places = listOf(left to top, left + width * 1.12f to top + height * 0.09f, left + width * 0.56f to top + height * 0.95f)
        val sway = if (animated) rememberInfiniteTransition() else null
        games.take(3).forEachIndexed { i, game ->
            val look = Looks[i]
            val angle = sway?.animateFloat(
                -1f,
                1f,
                infiniteRepeatable(tween(2_000, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(look.phaseMillis, StartOffsetType.FastForward)),
            )
            GamePoster(
                title = titles[i],
                players = game.players(),
                look = look,
                width = width,
                rotation = { look.tilt + (angle?.value ?: 0f) },
                modifier = Modifier.offset(places[i].first, places[i].second),
            )
        }
    }
}

@Composable
private fun GamePoster(title: String, players: String, look: PosterLook, width: Dp, rotation: () -> Float, modifier: Modifier) {
    Box(modifier = modifier.size(width, width * ASPECT).rotateBy(rotation)) {
        Column(
            modifier = Modifier
                .size(width, width * ASPECT)
                .shadow(8.dp, PosterShape)
                .background(look.color, PosterShape)
                .padding(width * 0.085f),
        ) {
            Text(
                text = title.toUpperCase(Locale.current),
                color = ClinkColors.Ink,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = (width.value * 0.14f).sp, lineHeight = (width.value * 0.15f).sp),
            )
            Text(
                text = players,
                modifier = Modifier.padding(top = 4.dp),
                color = if (look.color == ClinkColors.Pink) ClinkColors.Ink else ClinkColors.InkMuted,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = (width.value * 0.085f).sp),
            )
        }
        // A strip of tape holding it to the wall.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-7).dp)
                .size(width * 0.36f, 14.dp)
                .rotate(-4f)
                .background(ClinkColors.Tape.copy(alpha = 0.85f)),
        )
    }
}

/** Rotation read in the graphics layer, so the sway doesn't recompose the poster every frame. */
private fun Modifier.rotateBy(angle: () -> Float): Modifier = graphicsLayer { rotationZ = angle() }
