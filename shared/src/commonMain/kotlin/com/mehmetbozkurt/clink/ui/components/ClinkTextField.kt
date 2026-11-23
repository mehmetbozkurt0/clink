package com.mehmetbozkurt.clink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.ui.theme.ClinkColors

private val FieldShape = RoundedCornerShape(12.dp)

/** The app's text field: a raised wall tile with an acid line under it while focused. */
@Composable
internal fun ClinkTextField(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    singleLine: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 4,
        textStyle = textStyle.copy(color = ClinkColors.Paper),
        cursorBrush = SolidColor(ClinkColors.Acid),
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        interactionSource = interaction,
        decorationBox = { field ->
            Column(
                Modifier
                    .background(ClinkColors.WallRaised, FieldShape)
                    .border(1.dp, ClinkColors.Paper.copy(alpha = 0.14f), FieldShape)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Box {
                    if (value.isEmpty()) Text(hint, color = ClinkColors.Muted, style = textStyle.copy(fontWeight = FontWeight.Normal))
                    field()
                }
                Box(
                    Modifier.padding(top = 4.dp).fillMaxWidth().height(2.dp)
                        .background(if (focused) ClinkColors.Acid else ClinkColors.WallRaised),
                )
            }
        },
    )
}
