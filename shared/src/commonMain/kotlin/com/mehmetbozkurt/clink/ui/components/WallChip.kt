package com.mehmetbozkurt.clink.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val ChipShape = RoundedCornerShape(12.dp)

/**
 * A sticker on the wall: raised wall color with a hairline border, or filled with [color] (acid by default)
 * when [selected]. An optional icon sits before the text. [compact] is a smaller chip for crowded layouts
 * (the Spin circle with many players).
 */
@Composable
fun WallChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    color: Color? = null,
    icon: DrawableResource? = null,
    iconTint: Color = ClinkColors.Acid,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val fill = if (selected) color ?: ClinkColors.Acid else ClinkColors.WallRaised
    val textColor = if (selected) ClinkColors.Ink else ClinkColors.Paper
    Row(
        modifier = modifier
            .clip(ChipShape)
            .background(fill)
            .border(1.dp, if (selected) ClinkColors.Ink else ClinkColors.Paper.copy(alpha = 0.14f), ChipShape)
            // A tappable chip is a pick (Cup Pong picker), so screen readers also hear whether it is selected.
            .let { if (onClick != null) it.selectable(selected = selected, role = Role.Checkbox, onClick = onClick) else it }
            .padding(horizontal = if (compact) 9.dp else 12.dp, vertical = if (compact) 5.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                colorFilter = ColorFilter.tint(if (selected) ClinkColors.Ink else iconTint),
            )
        }
        Text(
            text = text,
            color = textColor,
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
