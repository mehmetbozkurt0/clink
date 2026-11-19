package com.mehmetbozkurt.clink.ui.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigateOnceTest {
    private class FakeScreen : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun navigatesFromTheVisibleScreen() {
        val screen = FakeScreen().apply { registry.currentState = Lifecycle.State.RESUMED }
        var navigations = 0
        screen.ifResumed { navigations++ }
        assertEquals(1, navigations)
    }

    @Test
    fun ignoresTheSecondTapAfterTheScreenStartedLeaving() {
        val screen = FakeScreen().apply { registry.currentState = Lifecycle.State.RESUMED }
        var navigations = 0
        screen.ifResumed {
            navigations++
            screen.registry.currentState = Lifecycle.State.STARTED // what navigate() does to the screen being left
        }
        screen.ifResumed { navigations++ }
        assertEquals(1, navigations)
    }
}
