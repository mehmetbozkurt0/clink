package com.mehmetbozkurt.clink.game.spinthebottle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.mehmetbozkurt.clink.LocalAppContainer
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.answered
import com.mehmetbozkurt.clink.resources.asks_answers
import com.mehmetbozkurt.clink.resources.end
import com.mehmetbozkurt.clink.resources.landed_on_self
import com.mehmetbozkurt.clink.resources.notice_no_penalty
import com.mehmetbozkurt.clink.resources.penalty_done
import com.mehmetbozkurt.clink.resources.penalty_title
import com.mehmetbozkurt.clink.resources.spin_hint
import com.mehmetbozkurt.clink.resources.take_penalty
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
fun SpinTheBottleScreen(players: List<String>, level: Level, onEnd: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { SpinTheBottleViewModel(players, level, container.library, container.random) }
    val state by viewModel.state.collectAsState()
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmEnd by remember { mutableStateOf(false) }
    var spinning by remember { mutableStateOf(false) }
    // After a configuration change the bottle starts out pointing at the last target again.
    val rotation = remember {
        Animatable(state.lastTarget?.let { spinEndRotation(0f, playerAngleDegrees(it, players.size), 0) } ?: 0f)
    }

    fun flick(speedDpPerSecond: Float) {
        if (spinning || state.phase !is SpinPhase.ReadyToSpin) return
        val motion = flickToSpin(speedDpPerSecond) ?: return
        viewModel.onAction(SpinAction.Spin)
        val target = viewModel.state.value.lastTarget ?: return
        val end = spinEndRotation(rotation.value, playerAngleDegrees(target, players.size), motion.turns)
        spinning = true
        scope.launch {
            rotation.animateTo(end, tween(durationMillis = motion.durationMillis, easing = EaseOutQuart))
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            spinning = false
        }
    }

    val tapGuard = rememberPhaseTapGuard(state.phase)

    // The notice reacts to the tap, so it doesn't repeat after a configuration change.
    fun act(action: SpinAction) = tapGuard {
        viewModel.onAction(action)
        if (viewModel.state.value.notice == SpinNotice.NO_PENALTY_CARDS) {
            scope.launch { snackbar.showSnackbar(getString(Res.string.notice_no_penalty)) }
        }
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        onBackCompleted = { confirmEnd = true },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { confirmEnd = true }) {
                    Text(stringResource(Res.string.end), color = ClinkColors.TextMuted)
                }
            }
            val phase = state.phase
            // The circle takes whatever space the bottom panel leaves (the largest square that fits),
            // so the panel's buttons always fit: small phones, large font scale, landscape on large screens.
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                BottleCircle(
                    players = state.players,
                    spinner = state.spinner,
                    answerer = (phase as? SpinPhase.Asking)?.answerer?.takeIf { !spinning },
                    rotation = rotation.value,
                    flickEnabled = !spinning && phase is SpinPhase.ReadyToSpin,
                    onFlick = ::flick,
                    modifier = Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true),
                )
            }
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!spinning) {
                    PhasePanel(
                        state = state,
                        penaltyText = { container.library.text(it, Locale.current.language) },
                        onAnswered = { act(SpinAction.Answered) },
                        onRefuse = { act(SpinAction.Refuse) },
                        onPenaltyDone = { act(SpinAction.PenaltyDone) },
                    )
                }
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (confirmEnd) {
        EndGameDialog(onConfirm = { confirmEnd = false; onEnd() }, onDismiss = { confirmEnd = false })
    }
}

@Composable
private fun PhasePanel(
    state: SpinState,
    penaltyText: (String) -> String,
    onAnswered: () -> Unit,
    onRefuse: () -> Unit,
    onPenaltyDone: () -> Unit,
) {
    when (val phase = state.phase) {
        is SpinPhase.ReadyToSpin -> {
            if (phase.landedOnSelf) {
                Text(stringResource(Res.string.landed_on_self), color = ClinkColors.Amber, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            }
            Text(
                text = stringResource(Res.string.spin_hint, state.players[state.spinner]),
                color = ClinkColors.TextMuted,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        is SpinPhase.Asking -> {
            Text(
                text = stringResource(Res.string.asks_answers, state.players[phase.asker], state.players[phase.answerer]),
                color = ClinkColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ClinkButton(stringResource(Res.string.answered), onAnswered, Modifier.weight(1f), true, ClinkColors.Mint, ClinkColors.OnPrimary)
                ClinkButton(stringResource(Res.string.take_penalty), onRefuse, Modifier.weight(1f), true, ClinkColors.SurfaceRaised, ClinkColors.Primary)
            }
        }
        is SpinPhase.ShowingPenalty -> {
            ClinkCard(modifier = Modifier.fillMaxWidth()) {
                LevelTag(state.level)
                Text(
                    text = stringResource(Res.string.penalty_title),
                    modifier = Modifier.padding(top = 12.dp),
                    color = ClinkColors.Primary,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "${state.players[phase.answerer]}: ${penaltyText(phase.cardId)}",
                    modifier = Modifier.padding(top = 8.dp),
                    color = ClinkColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            PrimaryButton(text = stringResource(Res.string.penalty_done), onClick = onPenaltyDone, modifier = Modifier.fillMaxWidth())
        }
    }
}
