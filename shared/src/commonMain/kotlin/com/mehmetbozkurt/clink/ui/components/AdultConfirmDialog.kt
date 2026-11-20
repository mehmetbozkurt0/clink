package com.mehmetbozkurt.clink.ui.components

import androidx.compose.runtime.Composable
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.adult_body
import com.mehmetbozkurt.clink.resources.adult_title
import com.mehmetbozkurt.clink.resources.adult_yes
import com.mehmetbozkurt.clink.resources.cancel
import org.jetbrains.compose.resources.stringResource

/** Spec §6.1: every Spicy session asks "18 or older?" before it starts. */
fun Level.needsAdultConfirmation(): Boolean = this == Level.SPICY

@Composable
fun AdultConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    PosterDialog(
        title = stringResource(Res.string.adult_title),
        body = stringResource(Res.string.adult_body),
        confirmText = stringResource(Res.string.adult_yes),
        cancelText = stringResource(Res.string.cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
