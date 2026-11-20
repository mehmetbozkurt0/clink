package com.mehmetbozkurt.clink.ui.theme

import androidx.compose.ui.graphics.Color
import com.mehmetbozkurt.clink.content.Level

fun Level.color(): Color = when (this) {
    Level.CASUAL -> ClinkColors.Acid
    Level.HARD -> ClinkColors.Amber
    Level.SPICY -> ClinkColors.Pink
}
