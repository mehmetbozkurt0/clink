package com.mehmetbozkurt.clink.ui.components

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.test.Test
import kotlin.test.assertEquals

class SwitchableHapticsTest {
    private class Recording : HapticFeedback {
        val calls = mutableListOf<HapticFeedbackType>()
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            calls += hapticFeedbackType
        }
    }

    @Test
    fun offIsSilent() {
        val platform = Recording()
        SwitchableHaptics(platform) { false }.performHapticFeedback(HapticFeedbackType.LongPress)
        assertEquals(0, platform.calls.size)
    }

    @Test
    fun onForwards() {
        val platform = Recording()
        SwitchableHaptics(platform) { true }.performHapticFeedback(HapticFeedbackType.LongPress)
        assertEquals(listOf(HapticFeedbackType.LongPress), platform.calls)
    }

    @Test
    fun toggleIsReadAtCallTime() {
        val platform = Recording()
        var on = true
        val haptics = SwitchableHaptics(platform) { on }
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        on = false
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        assertEquals(1, platform.calls.size)
    }
}
