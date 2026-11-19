package com.mehmetbozkurt.clink.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.ui.theme.ClinkColors

private val ButtonShape = RoundedCornerShape(14.dp)
private val ButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)

/** Main action: coral with dark text, Unbounded 800. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    ClinkButton(text, onClick, modifier, enabled, ClinkColors.Primary, ClinkColors.OnPrimary)
}

/** Secondary action on a raised surface. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    ClinkButton(text, onClick, modifier, enabled, ClinkColors.SurfaceRaised, ClinkColors.TextPrimary)
}

@Composable
private fun ClinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    container: Color,
    content: Color,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.4f),
            disabledContentColor = content.copy(alpha = 0.6f),
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold))
    }
}
