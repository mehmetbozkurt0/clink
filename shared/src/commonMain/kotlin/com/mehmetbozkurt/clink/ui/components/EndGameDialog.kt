package com.mehmetbozkurt.clink.ui.components

import androidx.compose.runtime.Composable
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.cancel
import com.mehmetbozkurt.clink.resources.end
import com.mehmetbozkurt.clink.resources.end_confirm
import org.jetbrains.compose.resources.stringResource

/** Asks before leaving a running game. */
@Composable
fun EndGameDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    PosterDialog(
        title = stringResource(Res.string.end_confirm),
        confirmText = stringResource(Res.string.end),
        cancelText = stringResource(Res.string.cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        destructive = true,
    )
}
