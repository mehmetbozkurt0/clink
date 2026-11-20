package com.mehmetbozkurt.clink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 * The app background (design §3.3, §5): the wall color with a faint brick texture, two small spray glows,
 * and a fine grain over everything — including the content, so every part of the screen has detail.
 * Content is kept clear of the status and navigation bars.
 */
@Composable
fun WallBackground(content: @Composable BoxScope.() -> Unit) {
    val wall = rememberTiledBrush(imageResource(Res.drawable.tex_wall))
    val grain = rememberTiledBrush(imageResource(Res.drawable.tex_grain))
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClinkColors.Wall)
            .drawBehind {
                drawRect(wall, alpha = 0.10f)
                val radius = size.width * 0.35f
                drawGlow(ClinkColors.Pink, Offset(size.width * 0.92f, size.height * 0.06f), radius)
                drawGlow(ClinkColors.Acid, Offset(size.width * 0.04f, size.height * 0.70f), radius)
            }
            .drawWithContent {
                drawContent()
                drawRect(grain, alpha = 0.06f)
            },
    ) {
        Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), content = content)
    }
}

private fun DrawScope.drawGlow(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent), center, radius),
        radius = radius,
        center = center,
    )
}
