package com.mehmetbozkurt.clink.game.truthordare

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.mehmetbozkurt.clink.resources.notice_no_dare
import com.mehmetbozkurt.clink.resources.notice_no_penalty
import com.mehmetbozkurt.clink.resources.notice_no_truth
import com.mehmetbozkurt.clink.resources.penalty_done
import com.mehmetbozkurt.clink.resources.penalty_title
import com.mehmetbozkurt.clink.resources.take_penalty
import com.mehmetbozkurt.clink.resources.tod_hint
import com.mehmetbozkurt.clink.resources.tod_hint_dare
import com.mehmetbozkurt.clink.resources.truth
import com.mehmetbozkurt.clink.ui.components.EndGameDialog
import com.mehmetbozkurt.clink.ui.components.LevelTag
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.SecondaryButton
import com.mehmetbozkurt.clink.ui.components.SlapPoster
import com.mehmetbozkurt.clink.ui.components.TopBar
import com.mehmetbozkurt.clink.ui.components.TopBarNav
import com.mehmetbozkurt.clink.ui.components.TurnBadge
import com.mehmetbozkurt.clink.ui.components.TypePill
import com.mehmetbozkurt.clink.ui.components.WaitingLabel
import com.mehmetbozkurt.clink.ui.components.cardTextStyle
import com.mehmetbozkurt.clink.ui.components.rememberPhaseTapGuard
import com.mehmetbozkurt.clink.ui.screens.ScreenColumn
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import com.mehmetbozkurt.clink.ui.theme.ClinkFonts
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
        ScreenColumn {
            TopBar(navigation = TopBarNav.Close, onNavigate = { confirmEnd = true }) {
                TurnBadge(name = state.players[state.currentPlayer], index = state.currentPlayer)
            }
            when (val phase = state.phase) {
                TodPhase.Choosing -> Choosing(onPick = { act(TodAction.Pick(it)) })
                is TodPhase.ShowingCard -> ShowingCard(
                    level = state.level,
                    type = phase.type,
                    text = container.library.text(phase.cardId, Locale.current.language),
                    slapKey = phase,
                    onDone = { act(TodAction.Done) },
                    onRefuse = { act(TodAction.Refuse) },
                )
                is TodPhase.ShowingPenalty -> ShowingPenalty(
                    level = state.level,
                    text = container.library.text(phase.cardId, Locale.current.language),
                    slapKey = phase,
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

internal fun TodNotice.message() = when (this) {
    TodNotice.NO_TRUTH_CARDS -> Res.string.notice_no_truth
    TodNotice.NO_DARE_CARDS -> Res.string.notice_no_dare
    TodNotice.NO_PENALTY_CARDS -> Res.string.notice_no_penalty
}

/** Two large neon panels (design §4): Truth acid, Dare pink. */
@Composable
internal fun ColumnScope.Choosing(onPick: (CardType) -> Unit) {
    Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        NeonPanel(stringResource(Res.string.truth), ClinkColors.Acid, ClinkColors.Pink) { onPick(CardType.TRUTH) }
        NeonPanel(stringResource(Res.string.dare), ClinkColors.Pink, ClinkColors.Acid) { onPick(CardType.DARE) }
    }
}

private val PanelShape = RoundedCornerShape(18.dp)
private val PanelShadow = 6.dp

/** A big neon button that fills its share of the column; sinks onto its shadow while pressed, like the neon buttons. */
@Composable
private fun ColumnScope.NeonPanel(text: String, container: Color, shadow: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed) PanelShadow else 0.dp, tween(durationMillis = 90))
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        Box(Modifier.matchParentSize().padding(start = PanelShadow, top = PanelShadow).background(shadow, PanelShape))
        Box(
            modifier = Modifier
                .offset(sink, sink)
                .padding(end = PanelShadow, bottom = PanelShadow)
                .fillMaxSize()
                .clip(PanelShape)
                .background(container)
                .border(2.dp, ClinkColors.Ink, PanelShape)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text.toUpperCase(Locale.current), color = ClinkColors.Ink, style = MaterialTheme.typography.displayMedium)
        }
    }
}

@Composable
internal fun ColumnScope.ShowingCard(
    level: Level,
    type: CardType,
    text: String,
    slapKey: Any,
    onDone: () -> Unit,
    onRefuse: () -> Unit,
    waitingFor: String? = null,
) {
    CardArea {
        SlapPoster(key = slapKey, modifier = Modifier.fillMaxWidth(), rotation = 1f) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LevelTag(level)
                TypePill(stringResource(if (type == CardType.TRUTH) Res.string.truth else Res.string.dare))
            }
            Text(text = text, modifier = Modifier.padding(top = 16.dp), color = ClinkColors.Ink, style = cardTextStyle())
            Text(
                text = stringResource(if (type == CardType.TRUTH) Res.string.tod_hint else Res.string.tod_hint_dare),
                modifier = Modifier.padding(top = 18.dp),
                color = ClinkColors.InkMuted,
                style = TextStyle(fontFamily = ClinkFonts.marker, fontSize = 18.sp),
            )
        }
    }
    // Online, only the player whose turn it is gets the buttons.
    if (waitingFor != null) {
        WaitingLabel(waitingFor)
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(stringResource(Res.string.done), onDone, Modifier.weight(1f))
            SecondaryButton(stringResource(Res.string.take_penalty), onRefuse, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun ColumnScope.ShowingPenalty(level: Level, text: String, slapKey: Any, onDone: () -> Unit, waitingFor: String? = null) {
    CardArea {
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
    if (waitingFor != null) {
        WaitingLabel(waitingFor)
    } else {
        PrimaryButton(text = stringResource(Res.string.penalty_done), onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}

/** The card sits in the middle of the free space at its own height (design §4); a long card scrolls. */
@Composable
private fun ColumnScope.CardArea(content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp), content = content)
    }
}
