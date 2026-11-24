package com.mehmetbozkurt.clink.ui.components

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * The app's haptics with the settings switch in front: provided as `LocalHapticFeedback`, so every screen's haptic
 * goes through it. [enabled] is read on every call, so a screen that grabbed the haptics earlier still obeys the switch.
 */
class SwitchableHaptics(private val platform: HapticFeedback, private val enabled: () -> Boolean) : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        if (enabled()) platform.performHapticFeedback(hapticFeedbackType)
    }
}
