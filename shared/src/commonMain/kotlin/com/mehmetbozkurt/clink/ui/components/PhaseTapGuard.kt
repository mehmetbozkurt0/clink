package com.mehmetbozkurt.clink.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/** How long a newly shown game phase ignores taps (about the card flip time). */
val PHASE_TAP_COOLDOWN: Duration = 300.milliseconds

fun isTapTooSoon(sincePhaseShown: Duration): Boolean = sincePhaseShown < PHASE_TAP_COOLDOWN

/**
 * Returns a wrapper for button actions that ignores taps arriving right after [phase] changed,
 * so the second tap of a double tap can't land on the next phase's button (e.g. "Penalty done").
 */
@Composable
fun rememberPhaseTapGuard(phase: Any): (() -> Unit) -> Unit {
    val shownAt = remember(phase) { TimeSource.Monotonic.markNow() }
    return { action -> if (!isTapTooSoon(shownAt.elapsedNow())) action() }
}
