package com.mehmetbozkurt.clink.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.ui.theme.ClinkColors

/** Online: shown instead of the buttons on the phones whose turn it isn't ("Waiting for Ayşe…"). */
@Composable
fun WaitingLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        color = ClinkColors.Muted,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
}
