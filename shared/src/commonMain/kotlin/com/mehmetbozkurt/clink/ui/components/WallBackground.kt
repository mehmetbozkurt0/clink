package com.mehmetbozkurt.clink.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.tex_grain
import com.mehmetbozkurt.clink.resources.tex_wall
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.imageResource

/** A tiled texture brush, created once per image. */
@Composable
internal fun rememberTiledBrush(image: ImageBitmap): ShaderBrush =
    remember(image) { ShaderBrush(ImageShader(image, TileMode.Repeated, TileMode.Repeated)) }

/**
 * Set to true by a screen that wants the wall's two glows to breathe (only the home screen, spec
 * 2026-11-24-home-and-settings §4); every other screen keeps the wall still.
 */
val LocalWallMotion = staticCompositionLocalOf<MutableState<Boolean>> { mutableStateOf(false) }

/**
 * The app background (design §3.3, §5): the wall color with a faint brick texture, two small spray glows,
 * and a fine grain over everything — including the content, so every part of the screen has detail.
 * Content is kept clear of the status and navigation bars. While [LocalWallMotion] is on, the glows slowly drift
 * toward the middle and breathe (pink every 9 s, acid every 11 s).
 */
@Composable
fun WallBackground(content: @Composable BoxScope.() -> Unit) {
    val wall = rememberTiledBrush(imageResource(Res.drawable.tex_wall))
    val grain = rememberTiledBrush(imageResource(Res.drawable.tex_grain))
    val motion = remember { mutableStateOf(false) }
    val pink: State<Float>
    val acid: State<Float>
    if (motion.value) {
        val breathing = rememberInfiniteTransition()
        pink = breathing.animateFloat(0f, 1f, infiniteRepeatable(tween(4_500, easing = FastOutSlowInEasing), RepeatMode.Reverse))
        acid = breathing.animateFloat(0f, 1f, infiniteRepeatable(tween(5_500, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    } else {
        pink = remember { mutableFloatStateOf(0f) }
        acid = remember { mutableFloatStateOf(0f) }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClinkColors.Wall)
            .drawBehind {
                drawRect(wall, alpha = 0.10f)
                val radius = size.width * 0.35f
                val p = pink.value
                val a = acid.value
                drawGlow(
                    ClinkColors.Pink,
                    Offset(size.width * (0.92f - 0.10f * p), size.height * 0.06f + size.width * 0.10f * p),
                    radius * (1f + 0.15f * p),
                    alpha = 1f - 0.3f * p,
                )
                drawGlow(
                    ClinkColors.Acid,
                    Offset(size.width * (0.04f + 0.10f * a), size.height * 0.70f - size.width * 0.10f * a),
                    radius * (1f + 0.2f * a),
                    alpha = 1f - 0.3f * a,
                )
            }
            .drawWithContent {
                drawContent()
                drawRect(grain, alpha = 0.06f)
            },
    ) {
        CompositionLocalProvider(LocalWallMotion provides motion) {
            Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), content = content)
        }
    }
}

private fun DrawScope.drawGlow(color: Color, center: Offset, radius: Float, alpha: Float = 1f) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = 0.22f * alpha), Color.Transparent), center, radius),
        radius = radius,
        center = center,
    )
}
