package com.mehmetbozkurt.clink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import com.mehmetbozkurt.clink.ui.theme.color

/** Pill in the level's color. Level names stay as-is in every language (they're part of the brand). */
@Composable
fun LevelTag(level: Level, modifier: Modifier = Modifier) {
    Text(
        text = level.name,
        modifier = modifier
            .background(level.color(), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = ClinkColors.OnPrimary,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp),
    )
}
