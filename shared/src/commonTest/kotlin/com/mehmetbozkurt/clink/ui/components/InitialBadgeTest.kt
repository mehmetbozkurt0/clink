package com.mehmetbozkurt.clink.ui.components

import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import kotlin.test.Test
import kotlin.test.assertEquals

class InitialBadgeTest {
    @Test
    fun badgeColorsCycle() = assertEquals(
        listOf(ClinkColors.Acid, ClinkColors.Pink, ClinkColors.Amber, ClinkColors.Paper, ClinkColors.Acid),
        (0..4).map { badgeColor(it) },
    )

    @Test
    fun initialUsesTurkishCapitalI() {
        assertEquals("İ", initialOf("ilker", "tr"))
        assertEquals("I", initialOf("ılgaz", "tr"))
        assertEquals("I", initialOf("ian", "en"))
    }

    @Test
    fun initialOfBlankIsQuestionMark() = assertEquals("?", initialOf("  ", "en"))

    @Test
    fun initialKeepsAWholeEmoji() = assertEquals("😀", initialOf("😀 Can", "en"))
}
