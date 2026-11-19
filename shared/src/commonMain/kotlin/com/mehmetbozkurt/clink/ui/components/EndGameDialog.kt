package com.mehmetbozkurt.clink.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.cancel
import com.mehmetbozkurt.clink.resources.end
import com.mehmetbozkurt.clink.resources.end_confirm
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

/** Asks before leaving a running game. */
@Composable
fun EndGameDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.end_confirm)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.end), color = ClinkColors.Primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel), color = ClinkColors.TextMuted) }
        },
    )
}
