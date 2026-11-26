package com.mehmetbozkurt.clink.ui.online

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.mehmetbozkurt.clink.content.Level
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.LocalAppContainer
import com.mehmetbozkurt.clink.game.cuppong.CupPongState
import com.mehmetbozkurt.clink.game.cuppong.CupPongTable
import com.mehmetbozkurt.clink.game.cuppong.CpPhase
import com.mehmetbozkurt.clink.game.cuppong.Flight
import com.mehmetbozkurt.clink.game.cuppong.MatchOverPanel
import com.mehmetbozkurt.clink.game.mostlikely.MlAction
import com.mehmetbozkurt.clink.game.mostlikely.MlPhase
import com.mehmetbozkurt.clink.game.mostlikely.compactChips
import com.mehmetbozkurt.clink.game.mostlikely.tapGuardKey
import com.mehmetbozkurt.clink.game.spinthebottle.BottleCircle
import com.mehmetbozkurt.clink.game.spinthebottle.SpinAction
import com.mehmetbozkurt.clink.game.spinthebottle.SpinMotion
import com.mehmetbozkurt.clink.game.spinthebottle.SpinNotice
import com.mehmetbozkurt.clink.game.spinthebottle.SpinPhase
import com.mehmetbozkurt.clink.game.spinthebottle.flickToSpin
import com.mehmetbozkurt.clink.game.spinthebottle.stopAngle
import com.mehmetbozkurt.clink.game.spinthebottle.spinEndRotation
import com.mehmetbozkurt.clink.game.truthordare.Choosing
import com.mehmetbozkurt.clink.game.truthordare.ShowingCard
import com.mehmetbozkurt.clink.game.truthordare.ShowingPenalty
import com.mehmetbozkurt.clink.game.truthordare.TodAction
import com.mehmetbozkurt.clink.game.truthordare.TodPhase
import com.mehmetbozkurt.clink.game.truthordare.message
import com.mehmetbozkurt.clink.online.OnlineAction
import com.mehmetbozkurt.clink.online.SPIN_TEXT_MAX_LENGTH
import com.mehmetbozkurt.clink.online.SessionState
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.done_button
import com.mehmetbozkurt.clink.resources.ml_chip
import com.mehmetbozkurt.clink.resources.ml_no_penalty
import com.mehmetbozkurt.clink.resources.ml_no_prompts
import com.mehmetbozkurt.clink.resources.ml_pass
import com.mehmetbozkurt.clink.resources.ml_pick
import com.mehmetbozkurt.clink.resources.ml_reveal
import com.mehmetbozkurt.clink.resources.ml_summary_title
import com.mehmetbozkurt.clink.resources.ml_takes_penalty
import com.mehmetbozkurt.clink.resources.ml_voted
import com.mehmetbozkurt.clink.resources.penalty_done
import com.mehmetbozkurt.clink.resources.answered
import com.mehmetbozkurt.clink.resources.asks_answers
import com.mehmetbozkurt.clink.resources.cp_hint
import com.mehmetbozkurt.clink.resources.cp_hit
import com.mehmetbozkurt.clink.resources.cp_miss
import com.mehmetbozkurt.clink.resources.cp_rim_out
import com.mehmetbozkurt.clink.resources.cp_score
import com.mehmetbozkurt.clink.resources.card_missing
import com.mehmetbozkurt.clink.resources.landed_on_self
import com.mehmetbozkurt.clink.resources.notice_no_penalty
import com.mehmetbozkurt.clink.resources.online_reconnecting_player
import com.mehmetbozkurt.clink.resources.online_waiting_for
import com.mehmetbozkurt.clink.resources.online_waiting_host
import com.mehmetbozkurt.clink.resources.online_watching
import com.mehmetbozkurt.clink.resources.penalty_done
import com.mehmetbozkurt.clink.resources.penalty_title
import com.mehmetbozkurt.clink.resources.spin_answer_hint
import com.mehmetbozkurt.clink.resources.spin_answer_label
import com.mehmetbozkurt.clink.resources.spin_ask
import com.mehmetbozkurt.clink.resources.spin_hint
import com.mehmetbozkurt.clink.resources.spin_question_hint
import com.mehmetbozkurt.clink.resources.spin_question_label
import com.mehmetbozkurt.clink.resources.spin_waiting_answer
import com.mehmetbozkurt.clink.resources.spin_waiting_question
import com.mehmetbozkurt.clink.resources.take_penalty
import com.mehmetbozkurt.clink.ui.components.ClinkTextField
import com.mehmetbozkurt.clink.ui.components.LevelTag
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.SecondaryButton
import com.mehmetbozkurt.clink.ui.components.SlapPoster
import com.mehmetbozkurt.clink.ui.components.TopBar
import com.mehmetbozkurt.clink.ui.components.TopBarNav
import com.mehmetbozkurt.clink.ui.components.TurnBadge
import com.mehmetbozkurt.clink.ui.components.WaitingLabel
import com.mehmetbozkurt.clink.ui.components.cardTextStyle
import com.mehmetbozkurt.clink.ui.components.rememberPhaseTapGuard
import com.mehmetbozkurt.clink.ui.screens.ScreenColumn
import com.mehmetbozkurt.clink.ui.components.LevelTag
import com.mehmetbozkurt.clink.ui.components.WallChip
import com.mehmetbozkurt.clink.ui.components.WaitingLabel
import com.mehmetbozkurt.clink.ui.screens.title
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.semantics.Role
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** Every phone spins the bottle the same way, so the spinner's flick speed isn't sent. */
private val OnlineSpin = SpinMotion(turns = 5, durationMillis = 2600)

/** This phone's place in a running online game: [seat] in [SessionState.playerUids], null for a spectator. */
internal class OnlineSeat(val seat: Int?, val isHost: Boolean)

/** A card's text on this phone, in its own language (spec §10.4: only card ids are shared). */
@Composable
private fun cardText(cardId: String): String =
    LocalAppContainer.current.library.textOrNull(cardId, Locale.current.language) ?: stringResource(Res.string.card_missing)

@Composable
internal fun OnlineTruthOrDare(
    session: SessionState,
    me: OnlineSeat,
    send: (OnlineAction) -> Unit,
    onClose: () -> Unit,
    topBarExtras: @Composable RowScope.() -> Unit,
) {
    val state = session.tod ?: return
    val snackbar = remember { SnackbarHostState() }
    val tapGuard = rememberPhaseTapGuard(state.phase)
    val myTurn = me.seat == state.currentPlayer
    val current = state.players[state.currentPlayer]
    val waitingFor = if (myTurn) null else stringResource(Res.string.online_waiting_for, current)
    fun act(action: TodAction) = tapGuard { send(OnlineAction.Tod(action)) }

    // The notice belongs to the published state, so it shows once per state, on the phone that acted.
    LaunchedEffect(session.seq) {
        if (myTurn) state.notice?.let { snackbar.showSnackbar(getString(it.message())) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScreenColumn {
            TopBar(navigation = TopBarNav.Close, onNavigate = onClose) {
                topBarExtras()
                TurnBadge(name = current, index = state.currentPlayer)
            }
            when (val phase = state.phase) {
                TodPhase.Choosing ->
                    if (myTurn) {
                        Choosing(onPick = { act(TodAction.Pick(it)) })
                    } else {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { WaitingLabel(waitingFor.orEmpty()) }
                    }
                is TodPhase.ShowingCard -> ShowingCard(
                    level = state.level,
                    type = phase.type,
                    text = cardText(phase.cardId),
                    slapKey = phase,
                    onDone = { act(TodAction.Done) },
                    onRefuse = { act(TodAction.Refuse) },
                    waitingFor = waitingFor,
                )
                is TodPhase.ShowingPenalty -> ShowingPenalty(
                    level = state.level,
                    text = cardText(phase.cardId),
                    slapKey = phase,
                    onDone = { act(TodAction.PenaltyDone) },
                    waitingFor = waitingFor,
                )
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
internal fun OnlineSpinTheBottle(
    session: SessionState,
    me: OnlineSeat,
    send: (OnlineAction) -> Unit,
    onClose: () -> Unit,
    topBarExtras: @Composable RowScope.() -> Unit,
) {
    val state = session.spin ?: return
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    var spinning by remember { mutableStateOf(false) }
    // Spins already shown, so a configuration change doesn't replay the last one.
    var shownSpins by rememberSaveable { mutableStateOf(state.spinCount) }
    // Between the flick and the host's answer the bottle can't be flicked again.
    var awaitingSpin by remember(state.spinCount) { mutableStateOf(false) }
    val rotation = remember {
        Animatable(state.stopAngle()?.let { spinEndRotation(0f, it, 0) } ?: 0f)
    }
    val tapGuard = rememberPhaseTapGuard(state.phase)

    LaunchedEffect(state.spinCount) {
        if (state.spinCount == shownSpins) return@LaunchedEffect
        shownSpins = state.spinCount
        val stop = state.stopAngle() ?: return@LaunchedEffect
        spinning = true
        try {
            val end = spinEndRotation(rotation.value, stop, OnlineSpin.turns)
            rotation.animateTo(end, tween(durationMillis = OnlineSpin.durationMillis, easing = EaseOutQuart))
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } finally {
            spinning = false
        }
    }
    LaunchedEffect(session.seq) {
        val answerer = (state.phase as? SpinPhase.ShowingPenalty)?.answerer
        if (state.notice == SpinNotice.NO_PENALTY_CARDS && me.seat == answerer) {
            snackbar.showSnackbar(getString(Res.string.notice_no_penalty))
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxPanelHeight = maxHeight * 0.6f
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
            TopBar(navigation = TopBarNav.Close, onNavigate = onClose) {
                topBarExtras()
                TurnBadge(name = state.players[state.spinner], index = state.spinner, color = ClinkColors.Acid)
            }
            val phase = state.phase
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                BottleCircle(
                    players = state.players,
                    spinner = state.spinner,
                    answerer = (phase as? SpinPhase.Asking)?.answerer?.takeIf { !spinning },
                    rotation = rotation.value,
                    flickEnabled = !spinning && !awaitingSpin && phase is SpinPhase.ReadyToSpin && me.seat == state.spinner,
                    onFlick = { speed ->
                        if (flickToSpin(speed) != null) {
                            awaitingSpin = true
                            send(OnlineAction.Spin(SpinAction.Spin))
                        }
                    },
                    modifier = Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true),
                )
            }
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = maxPanelHeight).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!spinning) SpinPanel(session, me, send = { tapGuard { send(it) } })
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** Spec §7.2 online: the asker types the question, it shows as a large card everywhere; the answerer types the answer. */
@Composable
private fun SpinPanel(session: SessionState, me: OnlineSeat, send: (OnlineAction) -> Unit) {
    val state = session.spin ?: return
    val question = session.spinQuestion
    val answer = session.spinAnswer
    when (val phase = state.phase) {
        is SpinPhase.ReadyToSpin -> {
            if (question != null && answer != null) QuestionPoster(question, answer, key = state.spinCount)
            if (phase.landedOnSelf) {
                Text(stringResource(Res.string.landed_on_self), color = ClinkColors.Amber, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            }
            Text(
                text = stringResource(Res.string.spin_hint, state.players[state.spinner]),
                color = ClinkColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        is SpinPhase.Asking -> {
            Text(
                text = stringResource(Res.string.asks_answers, state.players[phase.asker], state.players[phase.answerer]),
                color = ClinkColors.Paper,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            if (question == null) {
                if (me.seat == phase.asker) {
                    TextEntry(hint = stringResource(Res.string.spin_question_hint), button = stringResource(Res.string.spin_ask), key = phase) {
                        send(OnlineAction.AskQuestion(it))
                    }
                } else {
                    WaitingLabel(stringResource(Res.string.spin_waiting_question, state.players[phase.asker]))
                }
            } else {
                QuestionPoster(question, answer = null, key = question)
                if (me.seat == phase.answerer) {
                    TextEntry(hint = stringResource(Res.string.spin_answer_hint), button = stringResource(Res.string.answered), key = question) {
                        send(OnlineAction.AnswerQuestion(it))
                    }
                } else {
                    WaitingLabel(stringResource(Res.string.spin_waiting_answer, state.players[phase.answerer]))
                }
            }
            if (me.seat == phase.answerer) {
                SecondaryButton(stringResource(Res.string.take_penalty), { send(OnlineAction.Spin(SpinAction.Refuse)) }, Modifier.fillMaxWidth())
            }
        }
        is SpinPhase.ShowingPenalty -> {
            SlapPoster(key = phase, modifier = Modifier.fillMaxWidth(), rotation = -1f) {
                LevelTag(state.level)
                Text(
                    text = stringResource(Res.string.penalty_title).toUpperCase(Locale.current),
                    modifier = Modifier.padding(top = 12.dp),
                    color = ClinkColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "${state.players[phase.answerer]}: ${cardText(phase.cardId)}",
                    modifier = Modifier.padding(top = 8.dp),
                    color = ClinkColors.Ink,
                    style = cardTextStyle(),
                )
            }
            if (me.seat == phase.answerer) {
                PrimaryButton(stringResource(Res.string.penalty_done), { send(OnlineAction.Spin(SpinAction.PenaltyDone)) }, Modifier.fillMaxWidth())
            } else {
                WaitingLabel(stringResource(Res.string.online_waiting_for, state.players[phase.answerer]))
            }
        }
    }
}

@Composable
private fun QuestionPoster(question: String, answer: String?, key: Any) {
    SlapPoster(key = key, modifier = Modifier.fillMaxWidth(), rotation = 1f) {
        Text(stringResource(Res.string.spin_question_label).toUpperCase(Locale.current), color = ClinkColors.InkMuted, style = MaterialTheme.typography.labelLarge)
        Text(text = question, modifier = Modifier.padding(top = 6.dp), color = ClinkColors.Ink, style = cardTextStyle())
        if (answer != null) {
            Text(
                stringResource(Res.string.spin_answer_label).toUpperCase(Locale.current),
                modifier = Modifier.padding(top = 14.dp),
                color = ClinkColors.InkMuted,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(text = answer, modifier = Modifier.padding(top = 6.dp), color = ClinkColors.Ink, style = cardTextStyle())
        }
    }
}

/** A short line to type and send; cleared when [key] changes (a new question or round). */
@Composable
private fun TextEntry(hint: String, button: String, key: Any, onSend: (String) -> Unit) {
    var text by rememberSaveable(key.toString()) { mutableStateOf("") }
    var sent by remember(key) { mutableStateOf(false) }
    fun submit() {
        if (sent || text.isBlank()) return
        sent = true
        onSend(text.trim())
    }
    ClinkTextField(
        value = text,
        onChange = { text = it.take(SPIN_TEXT_MAX_LENGTH) },
        hint = hint,
        imeAction = ImeAction.Send,
        onImeAction = ::submit,
        singleLine = false,
    )
    PrimaryButton(text = button, onClick = ::submit, enabled = !sent && text.isNotBlank(), modifier = Modifier.fillMaxWidth())
}

@Composable
internal fun OnlineCupPong(
    session: SessionState,
    me: OnlineSeat,
    paused: Boolean,
    send: (OnlineAction) -> Unit,
    onBackToLobby: () -> Unit,
    onClose: () -> Unit,
    topBarExtras: @Composable RowScope.() -> Unit,
) {
    val state = session.cupPong ?: return
    val haptics = LocalHapticFeedback.current
    var flight by remember { mutableStateOf<Flight?>(null) }
    var outcome by remember { mutableStateOf<StringResource?>(null) }
    val progress = remember { Animatable(0f) }
    val splash = remember { Animatable(0f) }
    var shownThrows by rememberSaveable { mutableStateOf(state.throwCount) }
    // The match as it was before the newest throw, so everyone replays that throw from the right side.
    var before by remember { mutableStateOf(state) }
    var awaitingThrow by remember(state.throwCount) { mutableStateOf(false) }
    val tapGuard = rememberPhaseTapGuard(state.phase)

    LaunchedEffect(state.throwCount) {
        val result = state.lastThrow
        if (state.throwCount == shownThrows || result == null) {
            shownThrows = state.throwCount
            before = state
            return@LaunchedEffect
        }
        shownThrows = state.throwCount
        flight = Flight(before.thrower, before.cups[1 - before.thrower], result)
        try {
            progress.snapTo(0f)
            splash.snapTo(0f)
            val bounced = result.touchedRim
            // The physics sets the pace: the flight replays in real time.
            progress.animateTo(1f, tween(durationMillis = result.durationMillis, easing = LinearEasing))
            outcome = when {
                result.cupHit != null -> Res.string.cp_hit
                bounced -> Res.string.cp_rim_out
                else -> Res.string.cp_miss
            }
            if (result.cupHit != null) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                splash.animateTo(1f, tween(durationMillis = 260))
            }
            delay(400)
        } finally {
            outcome = null
            flight = null
            before = state
        }
    }

    val shown = flight
    val thrower = shown?.thrower ?: state.thrower
    val defender = 1 - thrower
    val rack = when {
        shown == null -> state.cups[defender]
        splash.value >= 1f -> shown.cupsBefore - setOfNotNull(shown.result.cupHit)
        else -> shown.cupsBefore
    }
    fun cupsLeft(player: Int) = if (player == defender) rack.size else state.cups[player].size
    val myThrow = me.seat == state.thrower
    val over = state.phase as? CpPhase.MatchOver

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        TopBar(navigation = TopBarNav.Close, onNavigate = onClose) {
            topBarExtras()
            if (over == null || shown != null) TurnBadge(name = state.players[thrower], index = thrower)
        }
        Text(
            text = stringResource(Res.string.cp_score, state.players[0], cupsLeft(0), state.players[1], cupsLeft(1)).toUpperCase(Locale.current),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            color = ClinkColors.Muted,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
        if (me.seat == null) {
            Text(
                text = stringResource(Res.string.online_watching).toUpperCase(Locale.current),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                color = ClinkColors.Pink,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(12.dp))

        if (over != null && shown == null) {
            MatchOverPanel(
                winner = state.players[over.winner],
                loser = state.players[over.loser],
                level = state.level,
                penaltyText = over.penaltyCardId?.let { cardText(it) },
                onDone = if (me.isHost) ({ tapGuard(onBackToLobby) }) else null,
                modifier = Modifier.weight(1f),
            )
            if (!me.isHost) WaitingLabel(stringResource(Res.string.online_waiting_host))
        } else {
            CupPongTable(
                cups = rack,
                flight = shown?.result,
                progress = progress.value,
                splash = splash.value,
                flickEnabled = shown == null && !awaitingThrow && !paused && myThrow && state.phase == CpPhase.Aiming,
                onFlick = { input ->
                    awaitingThrow = true
                    send(OnlineAction.Throw(input))
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            val hint = when {
                outcome != null -> stringResource(outcome!!)
                paused -> stringResource(Res.string.online_reconnecting_player, state.players[state.thrower])
                myThrow -> stringResource(Res.string.cp_hint)
                else -> stringResource(Res.string.online_waiting_for, state.players[state.thrower])
            }
            Text(
                text = hint,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                color = if (outcome == Res.string.cp_hit) ClinkColors.Acid else ClinkColors.Muted,
                style = if (outcome != null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * "Kim Daha Çok?" online (spec 2026-11-26 §6): everyone votes in secret, the result opens with the split, and the
 * player or players with the most votes take a penalty. No screen ever shows who voted for whom.
 */
@Composable
internal fun OnlineMostLikely(
    session: SessionState,
    me: OnlineSeat,
    send: (OnlineAction) -> Unit,
    onClose: () -> Unit,
    onBackToLobby: () -> Unit,
    topBarExtras: @Composable RowScope.() -> Unit,
) {
    val state = session.mostLikely ?: return
    // Keyed on the round, not the phase value: a vote from anyone changes the phase without starting a new round.
    val tapGuard = rememberPhaseTapGuard(state.phase.tapGuardKey())
    fun act(action: MlAction) = tapGuard { send(OnlineAction.MostLikely(action)) }

    ScreenColumn {
        TopBar(
            navigation = TopBarNav.Close,
            // Closing publishes the night's tally to every phone first; the host's Done on it leaves (spec §6).
            onNavigate = { if (me.isHost && state.endsWithTally()) act(MlAction.End) else onClose() },
            title = session.game.title(),
            trailing = topBarExtras,
        )
        when (val phase = state.phase) {
            MlPhase.NoPrompts -> WaitingLabel(stringResource(Res.string.ml_no_prompts))
            // One-phone phases: an online session never publishes them.
            is MlPhase.ShowingPrompt -> WaitingLabel(stringResource(Res.string.ml_pick))
            is MlPhase.Voting -> OnlineVoting(
                level = state.level,
                text = cardText(phase.cardId),
                players = state.players,
                myVote = me.seat?.let { phase.votes.getOrNull(it) },
                voted = phase.votes.count { it != null },
                slapKey = phase.cardId,
                mySeat = me.seat,
                isHost = me.isHost,
                onVote = { seat, target -> act(MlAction.Vote(seat, target, phase.cardId)) },
                onReveal = { act(MlAction.Reveal) },
                onPass = { act(MlAction.Pass) },
            )
            is MlPhase.ShowingPenalty -> OnlineResult(
                level = state.level,
                players = state.players,
                counts = phase.counts,
                takers = phase.picked,
                text = phase.cardId?.let { cardText(it) },
                slapKey = phase,
                mine = me.seat in phase.picked || me.isHost,
                onDone = { act(MlAction.PenaltyDone) },
            )
            MlPhase.Summary -> OnlineSummary(state.players, state.picks, me.isHost, onDone = onBackToLobby)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.OnlineVoting(
    level: Level,
    text: String,
    players: List<String>,
    myVote: Int?,
    voted: Int,
    slapKey: Any,
    mySeat: Int?,
    isHost: Boolean,
    onVote: (Int, Int) -> Unit,
    onReveal: () -> Unit,
    onPass: () -> Unit,
) {
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        SlapPoster(key = slapKey, modifier = Modifier.fillMaxWidth(), rotation = -1f) {
            LevelTag(level)
            Text(text = text, modifier = Modifier.padding(top = 14.dp), color = ClinkColors.Ink, style = cardTextStyle())
        }
        Text(text = stringResource(Res.string.ml_pick), color = ClinkColors.Muted, style = MaterialTheme.typography.bodyMedium)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        players.forEachIndexed { index, name ->
            WallChip(
                text = name,
                compact = compactChips(players.size),
                selected = myVote == index,
                onClick = if (mySeat != null) ({ onVote(mySeat, index) }) else null,
            )
        }
    }
    Text(
        text = stringResource(Res.string.ml_voted, voted, players.size),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        color = ClinkColors.Muted,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
    if (isHost) {
        if (voted > 0) OnlineTextAction(stringResource(Res.string.ml_reveal), onReveal)
        OnlineTextAction(stringResource(Res.string.ml_pass), onPass)
    }
}

@Composable
private fun ColumnScope.OnlineResult(
    level: Level,
    players: List<String>,
    counts: List<Int>,
    takers: List<Int>,
    text: String?,
    slapKey: Any,
    mine: Boolean,
    onDone: () -> Unit,
) {
    val names = takers.mapNotNull { players.getOrNull(it) }
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        // Counts only: who voted for whom is never shown, to anybody (spec 2026-11-26 §4).
        players.indices.filter { counts.getOrElse(it) { 0 } > 0 }.sortedByDescending { counts[it] }.forEach { seat ->
            Text(
                text = stringResource(Res.string.ml_chip, players[seat], counts[seat]),
                color = ClinkColors.Paper,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        Text(
            text = stringResource(Res.string.ml_takes_penalty, names.joinToString(" · ")).toUpperCase(Locale.current),
            modifier = Modifier.padding(top = 6.dp),
            color = ClinkColors.Pink,
            style = MaterialTheme.typography.titleLarge,
        )
        if (text == null) {
            Text(stringResource(Res.string.ml_no_penalty), color = ClinkColors.Muted, style = MaterialTheme.typography.bodyLarge)
        } else {
            SlapPoster(key = slapKey, modifier = Modifier.fillMaxWidth(), rotation = -1f) {
                LevelTag(level)
                Text(text = text, modifier = Modifier.padding(top = 14.dp), color = ClinkColors.Ink, style = cardTextStyle())
            }
        }
    }
    if (mine) {
        PrimaryButton(text = stringResource(Res.string.penalty_done), onClick = onDone, modifier = Modifier.fillMaxWidth())
    } else {
        WaitingLabel(stringResource(Res.string.online_waiting_for, names.joinToString(" · ")))
    }
}

@Composable
private fun ColumnScope.OnlineSummary(players: List<String>, picks: List<Int>, isHost: Boolean, onDone: () -> Unit) {
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(Res.string.ml_summary_title).toUpperCase(Locale.current),
            color = ClinkColors.Acid,
            style = MaterialTheme.typography.displaySmall,
        )
        players.indices.filter { picks.getOrElse(it) { 0 } > 0 }.sortedByDescending { picks[it] }.forEach { seat ->
            Text(
                text = stringResource(Res.string.ml_chip, players[seat], picks[seat]),
                color = ClinkColors.Paper,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
    if (isHost) {
        PrimaryButton(text = stringResource(Res.string.done_button), onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}

/** The same quiet action the one-phone game uses, for the host's two extra controls. */
@Composable
private fun OnlineTextAction(text: String, onClick: () -> Unit) {
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
