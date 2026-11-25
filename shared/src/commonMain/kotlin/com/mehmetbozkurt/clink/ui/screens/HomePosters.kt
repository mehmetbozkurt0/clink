package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
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
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val PosterShape = RoundedCornerShape(4.dp)

/**
 * Each poster's tilt, background and sway phase, in the order of the approved sketch (spec §4): top left, top right,
 * bottom; plus the highlight band and its text color, and whether the stamp sits bottom-right (spec
 * 2026-11-24-home-poster-text: the top-right poster's bottom-left corner is under the bottom poster).
 */
private data class PosterLook(
    val tilt: Float,
    val color: Color,
    val phaseMillis: Int,
    val band: Color,
    val accentText: Color,
    val stampEnd: Boolean,
)

private val Looks = listOf(
    PosterLook(-6f, ClinkColors.Paper, 0, band = ClinkColors.Pink, accentText = ClinkColors.Ink, stampEnd = false),
    PosterLook(5f, ClinkColors.Acid, 1_300, band = ClinkColors.Ink, accentText = ClinkColors.Acid, stampEnd = true),
    PosterLook(-2f, ClinkColors.Pink, 2_400, band = ClinkColors.Acid, accentText = ClinkColors.Ink, stampEnd = false),
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
                accent = game.accent(),
                art = game.posterArt(),
                players = game.players(),
                look = look,
                width = width,
                rotation = { look.tilt + (angle?.value ?: 0f) },
                modifier = Modifier.offset(places[i].first, places[i].second),
            )
        }
    }
}

/**
 * One poster (spec 2026-11-24-home-poster-text, sketch C): the title with one word on a tilted band, the game's ink
 * drawing filling whatever space the title leaves, and the players in a tilted stamp at the bottom.
 */
@Composable
private fun GamePoster(
    title: String,
    accent: String,
    art: DrawableResource,
    players: String,
    look: PosterLook,
    width: Dp,
    rotation: () -> Float,
    modifier: Modifier,
) {
    Box(modifier = modifier.size(width, width * ASPECT).rotateBy(rotation)) {
        Column(
            modifier = Modifier
                .size(width, width * ASPECT)
                .shadow(8.dp, PosterShape)
                .background(look.color, PosterShape)
                .padding(width * 0.085f),
        ) {
            AccentTitle(title, accent, look, width)
            Image(
                painter = painterResource(art),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = width * 0.03f),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = players.toUpperCase(Locale.current),
                modifier = Modifier
                    .align(if (look.stampEnd) Alignment.End else Alignment.Start)
                    // The bottom poster covers the lowest ~14 % of the top-right one; keep its stamp above that.
                    .padding(bottom = if (look.stampEnd) width * ASPECT * 0.14f else 0.dp)
                    .rotate(-5f)
                    // Scales with the poster (≈ 2.5 dp on a phone-sized one), so small posters keep a readable stamp.
                    .border(width * 0.017f, ClinkColors.Ink)
                    .padding(horizontal = width * 0.045f, vertical = width * 0.02f),
                color = ClinkColors.Ink,
                maxLines = 1,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = (width.value * 0.085f).sp, lineHeight = (width.value * 0.11f).sp),
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

/**
 * The upper-cased title with [accent] in [PosterLook.accentText] on a [PosterLook.band] tilted −2°. The band is drawn
 * from the text layout, one piece per line the word touches; if the word isn't in the title, there is no band.
 */
@Composable
private fun AccentTitle(title: String, accent: String, look: PosterLook, width: Dp) {
    val shown = title.toUpperCase(Locale.current)
    val range = accentRange(shown, accent.toUpperCase(Locale.current))
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = buildAnnotatedString {
            append(shown)
            if (range != null) addStyle(SpanStyle(color = look.accentText), range.first, range.last + 1)
        },
        modifier = Modifier.drawBehind {
            val text = layout ?: return@drawBehind
            if (range == null) return@drawBehind
            val pad = width.toPx() * 0.025f
            for (line in text.getLineForOffset(range.first)..text.getLineForOffset(range.last)) {
                val from = maxOf(range.first, text.getLineStart(line))
                val to = minOf(range.last, text.getLineEnd(line, visibleEnd = true) - 1)
                if (from > to) continue
                val box = (from..to).map { text.getBoundingBox(it) }.reduce { a, b ->
                    Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom))
                }
                // Full line height, so Turkish marks (the dot of İ, the tail of Ş) stay on the band.
                val band = Rect(box.left - pad, box.top, box.right + pad, box.bottom)
                rotate(-2f, pivot = band.center) { drawRect(look.band, band.topLeft, band.size) }
            }
        },
        color = ClinkColors.Ink,
        onTextLayout = { layout = it },
        style = MaterialTheme.typography.headlineSmall.copy(fontSize = (width.value * 0.14f).sp, lineHeight = (width.value * 0.15f).sp),
    )
}

/** Rotation read in the graphics layer, so the sway doesn't recompose the poster every frame. */
private fun Modifier.rotateBy(angle: () -> Float): Modifier = graphicsLayer { rotationZ = angle() }
