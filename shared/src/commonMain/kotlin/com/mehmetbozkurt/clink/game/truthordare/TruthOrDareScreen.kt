package com.mehmetbozkurt.clink.game.truthordare

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.mehmetbozkurt.clink.LocalAppContainer
import com.mehmetbozkurt.clink.content.CardType
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.dare
import com.mehmetbozkurt.clink.resources.done
import com.mehmetbozkurt.clink.resources.end
import com.mehmetbozkurt.clink.resources.notice_no_dare
import com.mehmetbozkurt.clink.resources.notice_no_penalty
import com.mehmetbozkurt.clink.resources.notice_no_truth
import com.mehmetbozkurt.clink.resources.penalty_done
import com.mehmetbozkurt.clink.resources.penalty_title
import com.mehmetbozkurt.clink.resources.take_penalty
import com.mehmetbozkurt.clink.resources.truth
import com.mehmetbozkurt.clink.resources.turn_of
import com.mehmetbozkurt.clink.ui.components.ClinkButton
import com.mehmetbozkurt.clink.ui.components.ClinkCard
import com.mehmetbozkurt.clink.ui.components.EndGameDialog
import com.mehmetbozkurt.clink.ui.components.LevelTag
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.rememberPhaseTapGuard
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun TruthOrDareScreen(players: List<String>, level: Level, onEnd: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { TruthOrDareViewModel(players, level, container.library, container.random) }
    val state by viewModel.state.collectAsState()
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmEnd by remember { mutableStateOf(false) }
    val tapGuard = rememberPhaseTapGuard(state.phase)

    // Notices and haptics react to the tap, not to the state, so they don't repeat after a configuration change.
    fun act(action: TodAction) = tapGuard {
        viewModel.onAction(action)
        val after = viewModel.state.value
        if (action == TodAction.Refuse && after.phase is TodPhase.ShowingPenalty) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        after.notice?.let { notice -> scope.launch { snackbar.showSnackbar(getString(notice.message())) } }
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        onBackCompleted = { confirmEnd = true },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.turn_of, state.players[state.currentPlayer]),
                    modifier = Modifier.weight(1f),
                    color = ClinkColors.Paper,
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = { confirmEnd = true }) {
                    Text(stringResource(Res.string.end), color = ClinkColors.Muted)
                }
            }
            Spacer(Modifier.height(16.dp))
            when (val phase = state.phase) {
                TodPhase.Choosing -> Choosing(onPick = { act(TodAction.Pick(it)) })
                is TodPhase.ShowingCard -> ShowingCard(
                    level = state.level,
                    type = phase.type,
                    text = container.library.text(phase.cardId, Locale.current.language),
                    flipKey = phase,
                    onDone = { act(TodAction.Done) },
                    onRefuse = { act(TodAction.Refuse) },
                )
                is TodPhase.ShowingPenalty -> ShowingPenalty(
                    level = state.level,
                    text = container.library.text(phase.cardId, Locale.current.language),
                    flipKey = phase,
                    onDone = { act(TodAction.PenaltyDone) },
                )
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (confirmEnd) {
        EndGameDialog(onConfirm = { confirmEnd = false; onEnd() }, onDismiss = { confirmEnd = false })
    }
}

private fun TodNotice.message() = when (this) {
    TodNotice.NO_TRUTH_CARDS -> Res.string.notice_no_truth
    TodNotice.NO_DARE_CARDS -> Res.string.notice_no_dare
    TodNotice.NO_PENALTY_CARDS -> Res.string.notice_no_penalty
}

@Composable
private fun Choosing(onPick: (CardType) -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
        BigChoice(stringResource(Res.string.truth), ClinkColors.Pink, ClinkColors.Paper) { onPick(CardType.TRUTH) }
        BigChoice(stringResource(Res.string.dare), ClinkColors.Acid, ClinkColors.Ink) { onPick(CardType.DARE) }
    }
}

@Composable
private fun BigChoice(text: String, container: Color, content: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = content, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun ShowingCard(
    level: Level,
    type: CardType,
    text: String,
    flipKey: Any,
    onDone: () -> Unit,
    onRefuse: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FlipCard(flipKey, Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LevelTag(level)
                Text(
                    text = stringResource(if (type == CardType.TRUTH) Res.string.truth else Res.string.dare),
                    modifier = Modifier.background(ClinkColors.WallRaised, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
                    color = ClinkColors.Paper,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            CardText(text)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ClinkButton(stringResource(Res.string.done), onDone, Modifier.weight(1f), true, ClinkColors.Acid, ClinkColors.Ink)
            ClinkButton(stringResource(Res.string.take_penalty), onRefuse, Modifier.weight(1f), true, ClinkColors.WallRaised, ClinkColors.Acid)
        }
    }
}

@Composable
private fun ShowingPenalty(level: Level, text: String, flipKey: Any, onDone: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        FlipCard(flipKey, Modifier.weight(1f)) {
            LevelTag(level)
            Spacer(Modifier.height(16.dp))
            Text(text = stringResource(Res.string.penalty_title), color = ClinkColors.Acid, style = MaterialTheme.typography.headlineMedium)
            CardText(text)
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(text = stringResource(Res.string.penalty_done), onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CardText(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 16.dp),
        color = ClinkColors.Paper,
        style = MaterialTheme.typography.titleLarge,
    )
}

/** A card that flips in (~250 ms) whenever [key] changes. */
@Composable
private fun FlipCard(key: Any, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val rotation = remember(key) { Animatable(90f) }
    LaunchedEffect(key) { rotation.animateTo(0f, tween(durationMillis = 250)) }
    ClinkCard(
        modifier = modifier.fillMaxWidth().graphicsLayer {
            rotationY = rotation.value
            cameraDistance = 12f * density
        },
        content = content,
    )
}
