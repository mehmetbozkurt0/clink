package com.mehmetbozkurt.clink.game.mostlikely

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.done_button
import com.mehmetbozkurt.clink.resources.game_mostlikely
import com.mehmetbozkurt.clink.resources.ml_chip
import com.mehmetbozkurt.clink.resources.ml_instruction
import com.mehmetbozkurt.clink.resources.ml_no_penalty
import com.mehmetbozkurt.clink.resources.ml_no_prompts
import com.mehmetbozkurt.clink.resources.ml_pass
import com.mehmetbozkurt.clink.resources.ml_summary_title
import com.mehmetbozkurt.clink.resources.ml_who
import com.mehmetbozkurt.clink.resources.ml_wrong_person
import com.mehmetbozkurt.clink.resources.penalty_done
import com.mehmetbozkurt.clink.resources.penalty_title
import com.mehmetbozkurt.clink.LocalAppContainer
import com.mehmetbozkurt.clink.ui.components.EndGameDialog
import com.mehmetbozkurt.clink.ui.components.LevelTag
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.SlapPoster
import com.mehmetbozkurt.clink.ui.components.TopBar
import com.mehmetbozkurt.clink.ui.components.TopBarNav
import com.mehmetbozkurt.clink.ui.components.WallChip
import com.mehmetbozkurt.clink.ui.components.cardTextStyle
import com.mehmetbozkurt.clink.ui.components.rememberPhaseTapGuard
import com.mehmetbozkurt.clink.ui.screens.ScreenColumn
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

/**
 * "Kim Daha Çok?" on one phone (spec 2026-11-25 §7): a prompt, everyone points at once, the phone holder taps the
 * most-pointed name, and that player takes a penalty. The counters on the chips are the running tally, and the
 * game ends on a summary of who was picked most.
 */
@Composable
fun MostLikelyScreen(players: List<String>, level: Level, onEnd: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { MostLikelyViewModel(players, level, container.library, container.random) }
    val state by viewModel.state.collectAsState()
    val haptics = LocalHapticFeedback.current
    var confirmEnd by remember { mutableStateOf(false) }
    val tapGuard = rememberPhaseTapGuard(state.phase)

    fun act(action: MlAction) = tapGuard {
        viewModel.onAction(action)
        if (action is MlAction.Pick) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        onBackCompleted = { confirmEnd = true },
    )

    ScreenColumn {
        TopBar(
            navigation = TopBarNav.Close,
            onNavigate = { confirmEnd = true },
            title = stringResource(Res.string.game_mostlikely),
        )
        when (val phase = state.phase) {
            MlPhase.NoPrompts -> Notice(stringResource(Res.string.ml_no_prompts), onDone = onEnd)
            is MlPhase.ShowingPrompt -> Prompt(
                level = state.level,
                text = container.library.text(phase.cardId, Locale.current.language),
                players = state.players,
                picks = state.picks,
                slapKey = phase,
                onPick = { act(MlAction.Pick(it)) },
                onPass = { act(MlAction.Pass) },
            )
            is MlPhase.ShowingPenalty -> Penalty(
                level = state.level,
                name = state.players[phase.picked],
                text = phase.cardId?.let { container.library.text(it, Locale.current.language) },
                slapKey = phase,
                onDone = { act(MlAction.PenaltyDone) },
                onWrongPerson = { act(MlAction.WrongPerson) },
            )
            MlPhase.Summary -> Summary(state.players, state.picks, onDone = onEnd)
        }
    }

    if (confirmEnd) {
        EndGameDialog(
            onConfirm = {
                confirmEnd = false
                // Nothing to show when nobody has been picked yet, and the summary is already the last screen.
                if (state.hasPicks && state.phase != MlPhase.Summary) viewModel.onAction(MlAction.End) else onEnd()
            },
            onDismiss = { confirmEnd = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.Prompt(
    level: Level,
    text: String,
    players: List<String>,
    picks: List<Int>,
    slapKey: Any,
    onPick: (Int) -> Unit,
    onPass: () -> Unit,
) {
    // The card sits in the middle of the free space at its own height, as the other games' cards do.
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        SlapPoster(key = slapKey, modifier = Modifier.fillMaxWidth(), rotation = -1f) {
            LevelTag(level)
            Text(text = text, modifier = Modifier.padding(top = 14.dp), color = ClinkColors.Ink, style = cardTextStyle())
        }
        Text(
            text = stringResource(Res.string.ml_instruction),
            color = ClinkColors.Muted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    // The names live outside the scrolling area: with twelve players the prompt gives way, never the chips.
    Text(
        text = stringResource(Res.string.ml_who),
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
        color = ClinkColors.Acid,
        style = MaterialTheme.typography.titleMedium,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        players.forEachIndexed { index, name ->
            val count = picks.getOrElse(index) { 0 }
            WallChip(
                text = name,
                compact = compactChips(players.size),
                role = Role.Button,
                // Its own element, so a long name is what gives way, not the count.
                badge = if (count > 0) count.toString() else null,
                onClick = { onPick(index) },
            )
        }
    }
    TextAction(stringResource(Res.string.ml_pass), onPass)
}

@Composable
private fun ColumnScope.Penalty(
    level: Level,
    name: String,
    text: String?,
    slapKey: Any,
    onDone: () -> Unit,
    onWrongPerson: () -> Unit,
) {
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = name.toUpperCase(Locale.current),
            modifier = Modifier.fillMaxWidth(),
            color = ClinkColors.Pink,
            style = MaterialTheme.typography.displaySmall,
        )
        if (text == null) {
            // No card to show, so no card: a penalty poster saying there are no penalties reads as a contradiction.
            Text(
                text = stringResource(Res.string.ml_no_penalty),
                color = ClinkColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            SlapPoster(key = slapKey, modifier = Modifier.fillMaxWidth(), rotation = -1f) {
                LevelTag(level)
                Text(
                    text = stringResource(Res.string.penalty_title).toUpperCase(Locale.current),
                    modifier = Modifier.padding(top = 14.dp),
                    color = ClinkColors.Ink,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(text = text, modifier = Modifier.padding(top = 8.dp), color = ClinkColors.Ink, style = cardTextStyle())
            }
        }
    }
    PrimaryButton(text = stringResource(Res.string.penalty_done), onClick = onDone, modifier = Modifier.fillMaxWidth())
    TextAction(stringResource(Res.string.ml_wrong_person), onWrongPerson)
}

@Composable
private fun ColumnScope.Summary(players: List<String>, picks: List<Int>, onDone: () -> Unit) {
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(Res.string.ml_summary_title).toUpperCase(Locale.current),
            color = ClinkColors.Acid,
            style = MaterialTheme.typography.displaySmall,
        )
        players.indices
            .filter { picks.getOrElse(it) { 0 } > 0 }
            // Most picked first; players level on count keep the order they were set up in.
            .sortedByDescending { picks[it] }
            .forEach { index ->
                Text(
                    text = stringResource(Res.string.ml_chip, players[index], picks[index]),
                    color = ClinkColors.Paper,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
    }
    PrimaryButton(text = stringResource(Res.string.done_button), onClick = onDone, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ColumnScope.Notice(text: String, onDone: () -> Unit) {
    Spacer(Modifier.weight(1f))
    Text(text = text, color = ClinkColors.Muted, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.weight(1f))
    PrimaryButton(text = stringResource(Res.string.done_button), onClick = onDone, modifier = Modifier.fillMaxWidth())
}

/** A quiet escape under the main button: readable, but never the thing the eye lands on first. */
@Composable
private fun TextAction(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .wrapContentHeight(Alignment.CenterVertically),
        textAlign = TextAlign.Center,
        color = ClinkColors.Muted,
        style = MaterialTheme.typography.bodyMedium,
    )
}
