package com.mehmetbozkurt.clink

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mehmetbozkurt.clink.ui.components.ClinkBackground
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import com.mehmetbozkurt.clink.ui.theme.ClinkTheme

@Composable
fun App() {
    ClinkTheme {
        ClinkBackground {
            Text(
                text = "clink.",
                modifier = Modifier.align(Alignment.Center),
                color = ClinkColors.TextPrimary,
                style = MaterialTheme.typography.displayMedium,
            )
        }
    }
}
