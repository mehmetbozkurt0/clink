package com.mehmetbozkurt.clink.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.adult_body
import com.mehmetbozkurt.clink.resources.adult_title
import com.mehmetbozkurt.clink.resources.adult_yes
import com.mehmetbozkurt.clink.resources.cancel
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

/** Spec §6.1: every Spicy session asks "18 or older?" before it starts. */
fun Level.needsAdultConfirmation(): Boolean = this == Level.SPICY

@Composable
fun AdultConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.adult_title)) },
        text = { Text(stringResource(Res.string.adult_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.adult_yes), color = ClinkColors.Acid) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel), color = ClinkColors.Muted) }
        },
    )
}
