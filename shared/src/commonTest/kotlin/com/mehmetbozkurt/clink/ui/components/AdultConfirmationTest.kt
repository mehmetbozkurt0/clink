package com.mehmetbozkurt.clink.ui.components

import com.mehmetbozkurt.clink.content.Level
import kotlin.test.Test
import kotlin.test.assertEquals

class AdultConfirmationTest {
    @Test
    fun onlySpicyNeedsTheAdultConfirmation() = assertEquals(
        listOf(false, false, true),
        listOf(Level.CASUAL, Level.HARD, Level.SPICY).map { it.needsAdultConfirmation() },
    )
}
