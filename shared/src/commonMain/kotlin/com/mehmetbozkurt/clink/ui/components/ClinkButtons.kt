package com.mehmetbozkurt.clink.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mehmetbozkurt.clink.ui.theme.ClinkColors

private val ButtonShape = RoundedCornerShape(14.dp)
private val ShadowOffset = 4.dp

/** Main action: an acid neon button with a pink shadow. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    ClinkButton(text, onClick, modifier, enabled, ClinkColors.Acid, ClinkColors.Ink, shadow = ClinkColors.Pink)
}

/** Second action: a pink neon button with an acid shadow. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    ClinkButton(text, onClick, modifier, enabled, ClinkColors.Pink, ClinkColors.Ink, shadow = ClinkColors.Acid)
}

/**
 * A neon button (design §3.3): flat fill, a 2 dp ink outline and a hard offset shadow; while pressed it sinks onto
 * its shadow. Fills the width it is given.
 */
@Composable
internal fun ClinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    container: Color,
    content: Color,
    shadow: Color = ClinkColors.Ink,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed) ShadowOffset else 0.dp, tween(durationMillis = 90))
    Box(modifier = modifier.alpha(if (enabled) 1f else 0.45f)) {
        Box(
            Modifier
                .matchParentSize()
                .padding(start = ShadowOffset, top = ShadowOffset)
                .background(shadow, ButtonShape),
        )
        Box(
            modifier = Modifier
                .offset(sink, sink)
                .padding(end = ShadowOffset, bottom = ShadowOffset)
                .fillMaxWidth()
                .clip(ButtonShape)
                .background(container)
                .border(2.dp, ClinkColors.Ink, ButtonShape)
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                // Locale-aware: in Turkish "çekti" becomes "ÇEKTİ", not "ÇEKTI".
                text = text.toUpperCase(Locale.current),
                color = content,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 19.sp, letterSpacing = 0.04.em),
            )
        }
    }
}
