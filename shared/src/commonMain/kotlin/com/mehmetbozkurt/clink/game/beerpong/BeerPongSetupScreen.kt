package com.mehmetbozkurt.clink.game.beerpong

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.bp_setup_hint
import com.mehmetbozkurt.clink.resources.bp_setup_title
import com.mehmetbozkurt.clink.resources.bp_start_match
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.screens.ScreenColumn
import com.mehmetbozkurt.clink.ui.screens.ScreenTitle
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

private val SelectionSaver = listSaver<SnapshotStateList<Int>, Int>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)

/**
 * With more than two players: pick who plays this match. [suggested] is preselected (winner stays on);
 * tapping a third player replaces the one picked first.
 */
@Composable
fun BeerPongSetupScreen(players: List<String>, suggested: Matchup, previousWinner: Int?, onStart: (Matchup) -> Unit) {
    val selected = rememberSaveable(saver = SelectionSaver) { mutableStateListOf(suggested.first, suggested.second) }

    ScreenColumn {
        ScreenTitle(stringResource(Res.string.bp_setup_title))
        Text(stringResource(Res.string.bp_setup_hint), color = ClinkColors.TextMuted, style = MaterialTheme.typography.bodyLarge)
        // Scrolls, so 12 long names or a large font never push "Start match" off the screen.
        FlowRow(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            players.forEachIndexed { index, name ->
                val isSelected = index in selected
                Text(
                    text = name,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) ClinkColors.Primary else ClinkColors.Surface)
                        .clickable {
                            when {
                                isSelected -> selected.remove(index)
                                selected.size < 2 -> selected.add(index)
                                else -> {
                                    selected.removeAt(0)
                                    selected.add(index)
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (isSelected) ClinkColors.OnPrimary else ClinkColors.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        PrimaryButton(
            text = stringResource(Res.string.bp_start_match),
            // Re-checked here: `enabled` only updates on the next frame.
            onClick = { if (selected.size == 2) onStart(matchupFromSelection(selected.toList(), previousWinner)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = selected.size == 2,
        )
    }
}
