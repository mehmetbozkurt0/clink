package com.mehmetbozkurt.clink.ui.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

/**
 * Runs [action] only while this screen is the visible (resumed) one. navigate() moves the screen it leaves
 * out of RESUMED right away, so the second tap of a double tap can't open the next screen twice.
 */
fun LifecycleOwner.ifResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}
