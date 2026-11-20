package com.mehmetbozkurt.clink.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/** WCAG contrast ratio (1–21) between two opaque colors; AA needs ≥ 4.5 for normal text. */
fun contrastRatio(foreground: Color, background: Color): Double {
    val a = foreground.luminance().toDouble()
    val b = background.luminance().toDouble()
    return (max(a, b) + 0.05) / (min(a, b) + 0.05)
}
