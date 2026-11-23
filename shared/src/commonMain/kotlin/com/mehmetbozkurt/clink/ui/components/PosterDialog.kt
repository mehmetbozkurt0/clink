package com.mehmetbozkurt.clink.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mehmetbozkurt.clink.ui.theme.ClinkColors

/**
 * A dialog as a paper poster (design §4): Anton title, optional body, a neon confirm button and a paper cancel button.
 * Tapping outside or system back dismisses it.
 */
@Composable
internal fun PosterDialog(
    title: String,
    confirmText: String,
    cancelText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    body: String? = null,
    destructive: Boolean = false,
) {
    Dialog(onDismissRequest = onDismiss) {
        Poster(modifier = Modifier.fillMaxWidth(), rotation = -1f) {
            Text(text = title, color = ClinkColors.Ink, style = MaterialTheme.typography.headlineSmall)
            if (body != null) {
                Spacer(Modifier.height(10.dp))
                Text(text = body, color = ClinkColors.Ink, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (destructive) {
                    SecondaryButton(text = confirmText, onClick = onConfirm, modifier = Modifier.fillMaxWidth())
                } else {
                    PrimaryButton(text = confirmText, onClick = onConfirm, modifier = Modifier.fillMaxWidth())
                }
                if (cancelText != null) ClinkButton(cancelText, onDismiss, Modifier.fillMaxWidth(), true, ClinkColors.Paper, ClinkColors.Ink)
            }
        }
    }
}
