package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.adult_body
import com.mehmetbozkurt.clink.resources.adult_title
import com.mehmetbozkurt.clink.resources.adult_yes
import com.mehmetbozkurt.clink.resources.cancel
import com.mehmetbozkurt.clink.resources.level_casual_desc
import com.mehmetbozkurt.clink.resources.level_hard_desc
import com.mehmetbozkurt.clink.resources.level_spicy_desc
import com.mehmetbozkurt.clink.resources.level_title
import com.mehmetbozkurt.clink.ui.components.LevelTag
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

/** Spicy asks for the 18+ confirmation every time it is tapped. */
@Composable
fun LevelSelectScreen(onLevel: (Level) -> Unit) {
    var askAdult by remember { mutableStateOf(false) }

    ScreenColumn {
        ScreenTitle(stringResource(Res.string.level_title))
        Level.entries.forEach { level ->
            ChoiceCard(onClick = { if (level == Level.SPICY) askAdult = true else onLevel(level) }) {
                LevelTag(level)
                Spacer(Modifier.height(8.dp))
                Text(text = level.description(), color = ClinkColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    if (askAdult) {
        AlertDialog(
            onDismissRequest = { askAdult = false },
            title = { Text(stringResource(Res.string.adult_title)) },
            text = { Text(stringResource(Res.string.adult_body)) },
            confirmButton = {
                TextButton(onClick = {
                    askAdult = false
                    onLevel(Level.SPICY)
                }) { Text(stringResource(Res.string.adult_yes), color = ClinkColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { askAdult = false }) { Text(stringResource(Res.string.cancel), color = ClinkColors.TextMuted) }
            },
        )
    }
}

@Composable
private fun Level.description(): String = stringResource(
    when (this) {
        Level.CASUAL -> Res.string.level_casual_desc
        Level.HARD -> Res.string.level_hard_desc
        Level.SPICY -> Res.string.level_spicy_desc
    },
)
