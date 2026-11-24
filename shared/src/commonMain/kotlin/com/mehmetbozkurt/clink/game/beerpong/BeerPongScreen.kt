package com.mehmetbozkurt.clink.game.beerpong

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
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
import com.mehmetbozkurt.clink.resources.notice_no_penalty
import com.mehmetbozkurt.clink.resources.penalty_done
import com.mehmetbozkurt.clink.resources.penalty_title
import com.mehmetbozkurt.clink.ui.components.EndGameDialog
import com.mehmetbozkurt.clink.ui.components.LevelTag
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.SlapPoster
import com.mehmetbozkurt.clink.ui.components.TopBar
import com.mehmetbozkurt.clink.ui.components.TopBarNav
import com.mehmetbozkurt.clink.ui.components.TurnBadge
import com.mehmetbozkurt.clink.ui.components.cardTextStyle
import com.mehmetbozkurt.clink.ui.components.rememberPhaseTapGuard
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** A throw being animated: drawn from the thrower's side, with the defender's cups as they were before it. */
internal data class Flight(val thrower: Int, val cupsBefore: Set<Int>, val result: ThrowResult)

/**
 * One Beer Pong match on one phone. [players] are the two match players in throwing order;
 * [onMatchOver] gets the winner and loser as 0/1 in that list. [playerIndexes] are their positions in the full
 * player list, so their turn badges keep their colors from the Players screen.
 */
@Composable
fun BeerPongScreen(
    players: List<String>,
    level: Level,
    onMatchOver: (winner: Int, loser: Int) -> Unit,
    onEnd: () -> Unit,
    playerIndexes: List<Int>,
) {
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
            val bounced = result.touchedRim
            // The physics sets the pace: the flight replays in real time.
            progress.animateTo(1f, tween(durationMillis = result.durationMillis, easing = LinearEasing))
            outcome = when {
                result.cupHit != null -> Res.string.bp_hit
                bounced -> Res.string.bp_rim_out
                else -> Res.string.bp_miss
            }
            if (result.cupHit != null) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                splash.animateTo(1f, tween(durationMillis = 260))
            }
            delay(400)
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

    val over = state.phase as? BpPhase.MatchOver
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        TopBar(navigation = TopBarNav.Close, onNavigate = { confirmEnd = true }) {
            // No turn on match over: nobody throws any more.
            if (over == null || shown != null) TurnBadge(name = state.players[thrower], index = playerIndexes[thrower])
        }
        Text(
            text = stringResource(Res.string.bp_score, state.players[0], cupsLeft(0), state.players[1], cupsLeft(1)).toUpperCase(Locale.current),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            color = ClinkColors.Muted,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))

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
                color = if (outcome == Res.string.bp_hit) ClinkColors.Acid else ClinkColors.Muted,
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
internal fun MatchOverPanel(
    winner: String,
    loser: String,
    level: Level,
    penaltyText: String?,
    onDone: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = stringResource(Res.string.bp_win, winner).toUpperCase(Locale.current),
                color = ClinkColors.Acid,
                style = MaterialTheme.typography.displaySmall,
            )
            SlapPoster(key = loser, modifier = Modifier.fillMaxWidth(), rotation = -1f) {
                LevelTag(level)
                if (penaltyText != null) {
                    Text(
                        stringResource(Res.string.penalty_title).toUpperCase(Locale.current),
                        modifier = Modifier.padding(top = 12.dp),
                        color = ClinkColors.Ink,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        stringResource(Res.string.bp_loser_penalty, loser),
                        modifier = Modifier.padding(top = 4.dp),
                        color = ClinkColors.InkMuted,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
                // With an empty penalty pool the poster says only that (carried-over copy fix).
                Text(
                    text = penaltyText ?: stringResource(Res.string.notice_no_penalty),
                    modifier = Modifier.padding(top = 12.dp),
                    color = ClinkColors.Ink,
                    style = cardTextStyle(),
                )
            }
        }
        // Online, only the host moves everyone on.
        if (onDone != null) {
            PrimaryButton(
                text = stringResource(if (penaltyText != null) Res.string.penalty_done else Res.string.continue_button),
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
