package com.mehmetbozkurt.clink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.mehmetbozkurt.clink.ui.theme.ClinkColors

/**
 * Full-screen wall color with two soft glows (pink top-right, acid lower-left). Replaced by WallBackground in the next step.
 * Glows are radial gradients, not Modifier.blur, which does nothing below Android 12.
 * Content is kept clear of the status and navigation bars.
 */
@Composable
fun ClinkBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ClinkColors.Wall, ClinkColors.Wall)))
            .drawBehind {
                val radius = size.minDimension * 0.6f
                drawGlow(ClinkColors.Pink, Offset(size.width * 0.9f, size.height * 0.08f), radius)
                drawGlow(ClinkColors.Acid, Offset(size.width * 0.05f, size.height * 0.62f), radius)
            },
    ) {
        Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), content = content)
    }
}

private fun DrawScope.drawGlow(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent), center, radius),
        radius = radius,
        center = center,
    )
}
