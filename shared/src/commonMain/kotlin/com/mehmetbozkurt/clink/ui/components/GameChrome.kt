package com.mehmetbozkurt.clink.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.turn_of
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

/** Whose turn it is (design §6): the player's initial badge and "SIRA: MERT". [index] keeps the badge color from the Players screen. */
@Composable
fun TurnBadge(name: String, index: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InitialBadge(name = name, index = index)
        Text(
            text = stringResource(Res.string.turn_of, name).toUpperCase(Locale.current),
            color = ClinkColors.Paper,
            style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.5.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A small dark pill next to the level tag, e.g. "DOĞRULUK". */
@Composable
fun TypePill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.toUpperCase(Locale.current),
        modifier = modifier.background(ClinkColors.Ink, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
        color = ClinkColors.Paper,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp),
    )
}

/**
 * A poster that "slaps" onto the wall (design §3.4) whenever [key] changes: it lands from 1.04× to its size in
 * about 200 ms, keeping its [rotation] tilt.
 */
@Composable
fun SlapPoster(key: Any, modifier: Modifier = Modifier, rotation: Float = 0f, content: @Composable ColumnScope.() -> Unit) {
    val scale = remember(key) { Animatable(1.04f) }
    LaunchedEffect(key) { scale.animateTo(1f, tween(durationMillis = 200, easing = FastOutSlowInEasing)) }
    Poster(
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        rotation = rotation,
        content = content,
    )
}

/** Card questions and penalties (design §4): Archivo 800, large. */
@Composable
fun cardTextStyle() = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 27.sp)
