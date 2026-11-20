package com.mehmetbozkurt.clink.game.beerpong

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import com.mehmetbozkurt.clink.resources.bp_hint
import com.mehmetbozkurt.clink.resources.bp_hit
import com.mehmetbozkurt.clink.resources.bp_loser_penalty
import com.mehmetbozkurt.clink.resources.bp_miss
import com.mehmetbozkurt.clink.resources.bp_rim_out
import com.mehmetbozkurt.clink.resources.bp_score
import com.mehmetbozkurt.clink.resources.bp_win
import com.mehmetbozkurt.clink.resources.continue_button
import com.mehmetbozkurt.clink.resources.end
import com.mehmetbozkurt.clink.resources.notice_no_penalty
import com.mehmetbozkurt.clink.resources.penalty_done
import com.mehmetbozkurt.clink.resources.penalty_title
import com.mehmetbozkurt.clink.resources.turn_of
import com.mehmetbozkurt.clink.ui.components.ClinkCard
import com.mehmetbozkurt.clink.ui.components.EndGameDialog
import com.mehmetbozkurt.clink.ui.components.LevelTag
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.rememberPhaseTapGuard
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** A throw being animated: drawn from the thrower's side, with the defender's cups as they were before it. */
private data class Flight(val thrower: Int, val cupsBefore: Set<Int>, val result: ThrowResult)

/**
 * One Beer Pong match on one phone. [players] are the two match players in throwing order;
 * [onMatchOver] gets the winner and loser as 0/1 in that list.
 */
@Composable
fun BeerPongScreen(players: List<String>, level: Level, onMatchOver: (winner: Int, loser: Int) -> Unit, onEnd: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { BeerPongViewModel(players, level, container.library, container.random) }
    val state by viewModel.state.collectAsState()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var confirmEnd by remember { mutableStateOf(false) }
    // Not saved across configuration changes on purpose: after one, the animation is skipped and the current state shows.
    var flight by remember { mutableStateOf<Flight?>(null) }
    var outcome by remember { mutableStateOf<StringResource?>(null) }
    val progress = remember { Animatable(0f) }
    val splash = remember { Animatable(0f) }
    val tapGuard = rememberPhaseTapGuard(state.phase)

    fun throwBall(input: ThrowInput) {
        if (flight != null || state.phase != BpPhase.Aiming) return
        val before = state
        viewModel.onAction(BpAction.Throw(input))
        val after = viewModel.state.value
        val result = after.lastThrow ?: return
        flight = Flight(before.thrower, before.cups[1 - before.thrower], result)
        scope.launch {
            progress.snapTo(0f)
            splash.snapTo(0f)
            val bounced = result.touchdowns.size == 2
            progress.animateTo(1f, tween(durationMillis = if (bounced) 1250 else 900, easing = FastOutSlowInEasing))
            outcome = when {
                result.cupHit != null -> Res.string.bp_hit
                bounced -> Res.string.bp_rim_out
                else -> Res.string.bp_miss
            }
            if (result.cupHit != null) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                splash.animateTo(1f, tween(durationMillis = 450))
            }
            delay(700)
            outcome = null
            flight = null
            if ((viewModel.state.value.phase as? BpPhase.MatchOver)?.penaltyCardId != null) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        onBackCompleted = { confirmEnd = true },
    )

    val shown = flight
    val thrower = shown?.thrower ?: state.thrower
    val defender = 1 - thrower
    val rack = when {
        shown == null -> state.cups[defender]
        splash.value >= 1f -> shown.cupsBefore - setOfNotNull(shown.result.cupHit)
        else -> shown.cupsBefore
    }
    fun cupsLeft(player: Int) = if (player == defender) rack.size else state.cups[player].size

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.turn_of, state.players[thrower]), color = ClinkColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(Res.string.bp_score, state.players[0], cupsLeft(0), state.players[1], cupsLeft(1)),
                    color = ClinkColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(onClick = { confirmEnd = true }) { Text(stringResource(Res.string.end), color = ClinkColors.TextMuted) }
        }
        Spacer(Modifier.height(12.dp))

        val over = state.phase as? BpPhase.MatchOver
        if (over != null && shown == null) {
            MatchOverPanel(
                winner = state.players[over.winner],
                loser = state.players[over.loser],
                level = state.level,
                penaltyText = over.penaltyCardId?.let { container.library.text(it, Locale.current.language) },
                onDone = { tapGuard { onMatchOver(over.winner, over.loser) } },
                modifier = Modifier.weight(1f),
            )
        } else {
            BeerPongTable(
                cups = rack,
                flight = shown?.result,
                progress = progress.value,
                splash = splash.value,
                flickEnabled = shown == null && state.phase == BpPhase.Aiming,
                onFlick = ::throwBall,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            Text(
                text = stringResource(outcome ?: Res.string.bp_hint),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                color = if (outcome == Res.string.bp_hit) ClinkColors.Mint else ClinkColors.TextMuted,
                style = if (outcome != null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (confirmEnd) {
        EndGameDialog(onConfirm = { confirmEnd = false; onEnd() }, onDismiss = { confirmEnd = false })
    }
}

@Composable
private fun MatchOverPanel(
    winner: String,
    loser: String,
    level: Level,
    penaltyText: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
        Text(stringResource(Res.string.bp_win, winner), color = ClinkColors.Mint, style = MaterialTheme.typography.headlineMedium)
        ClinkCard(modifier = Modifier.fillMaxWidth()) {
            LevelTag(level)
            Text(
                stringResource(Res.string.penalty_title),
                modifier = Modifier.padding(top = 12.dp),
                color = ClinkColors.Primary,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                stringResource(Res.string.bp_loser_penalty, loser),
                modifier = Modifier.padding(top = 4.dp),
                color = ClinkColors.TextMuted,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = penaltyText ?: stringResource(Res.string.notice_no_penalty),
                modifier = Modifier.padding(top = 12.dp),
                color = ClinkColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        PrimaryButton(
            text = stringResource(if (penaltyText != null) Res.string.penalty_done else Res.string.continue_button),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
