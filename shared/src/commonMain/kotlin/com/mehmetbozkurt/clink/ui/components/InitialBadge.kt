package com.mehmetbozkurt.clink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehmetbozkurt.clink.ui.theme.ClinkColors

private val BadgeColors = listOf(ClinkColors.Acid, ClinkColors.Pink, ClinkColors.Amber, ClinkColors.Paper)

/** Player badge colors cycle acid, pink, amber, paper by the player's position. */
fun badgeColor(index: Int): Color = BadgeColors[index.mod(BadgeColors.size)]

/**
 * The first letter of [name], capitalized for [language] ("ilker" → "İ" in Turkish), "?" for a blank name.
 * An emoji (two UTF-16 units) is kept whole.
 */
fun initialOf(name: String, language: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    val first = trimmed[0]
    if (first.isHighSurrogate() && trimmed.length > 1) return trimmed.substring(0, 2)
    return when {
        language == "tr" && first == 'i' -> "İ"
        language == "tr" && first == 'ı' -> "I"
        else -> first.uppercase()
    }
}

/** A colored circle with the player's initial. */
@Composable
fun InitialBadge(name: String, index: Int, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Box(
        modifier = modifier.size(size).background(badgeColor(index), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialOf(name, Locale.current.language),
            color = ClinkColors.Ink,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = (size.value * 0.5f).sp),
        )
    }
}
