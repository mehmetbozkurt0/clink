package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.game.PlayerNames
import com.mehmetbozkurt.clink.game.PlayerNamesResult
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.add_player
import com.mehmetbozkurt.clink.resources.continue_button
import com.mehmetbozkurt.clink.resources.err_duplicate
import com.mehmetbozkurt.clink.resources.err_too_few
import com.mehmetbozkurt.clink.resources.err_too_many
import com.mehmetbozkurt.clink.resources.player_hint
import com.mehmetbozkurt.clink.resources.players_title
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.SecondaryButton
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

private val RowsSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)

@Composable
fun PlayerSetupScreen(onContinue: (List<String>) -> Unit) {
    val rows = rememberSaveable(saver = RowsSaver) { mutableStateListOf("", "") }
    var error by remember { mutableStateOf<PlayerNamesResult?>(null) }

    fun swap(a: Int, b: Int) {
        val moved = rows[a]
        rows[a] = rows[b]
        rows[b] = moved
    }

    ScreenColumn {
        ScreenTitle(stringResource(Res.string.players_title))
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rows.forEachIndexed { index, name ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            rows[index] = it.take(PlayerNames.MAX_LENGTH)
                            error = null
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text(stringResource(Res.string.player_hint, index + 1)) },
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ClinkColors.Surface,
                            unfocusedContainerColor = ClinkColors.Surface,
                            focusedBorderColor = ClinkColors.Secondary,
                            unfocusedBorderColor = ClinkColors.SurfaceRaised,
                            cursorColor = ClinkColors.Primary,
                        ),
                    )
                    RowAction("↑", enabled = index > 0) { swap(index, index - 1) }
                    RowAction("↓", enabled = index < rows.lastIndex) { swap(index, index + 1) }
                    RowAction("✕", enabled = rows.size > PlayerNames.MIN) {
                        rows.removeAt(index)
                        error = null
                    }
                }
            }
            if (rows.size < PlayerNames.MAX) {
                SecondaryButton(
                    text = stringResource(Res.string.add_player),
                    onClick = {
                        rows.add("")
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        error?.let { result ->
            errorText(result)?.let { Text(text = it, color = ClinkColors.Amber, style = MaterialTheme.typography.bodyMedium) }
        }
        PrimaryButton(
            text = stringResource(Res.string.continue_button),
            onClick = {
                when (val result = PlayerNames.validate(rows)) {
                    is PlayerNamesResult.Valid -> onContinue(result.names)
                    else -> error = result
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RowAction(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Text(text = symbol, color = if (enabled) ClinkColors.TextMuted else ClinkColors.SurfaceRaised, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun errorText(result: PlayerNamesResult): String? = when (result) {
    PlayerNamesResult.TooFew -> stringResource(Res.string.err_too_few)
    PlayerNamesResult.TooMany -> stringResource(Res.string.err_too_many)
    is PlayerNamesResult.Duplicate -> stringResource(Res.string.err_duplicate, result.name)
    is PlayerNamesResult.Valid -> null
}
