package com.mehmetbozkurt.clink.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class PhaseTapGuardTest {
    @Test
    fun secondTapOfADoubleTapIsTooSoon() = assertTrue(isTapTooSoon(sincePhaseShown = 80.milliseconds))

    @Test
    fun tapRightAtShowIsTooSoon() = assertTrue(isTapTooSoon(sincePhaseShown = Duration.ZERO))

    @Test
    fun deliberateTapAfterCooldownIsAllowed() = assertFalse(isTapTooSoon(sincePhaseShown = PHASE_TAP_COOLDOWN))
}
