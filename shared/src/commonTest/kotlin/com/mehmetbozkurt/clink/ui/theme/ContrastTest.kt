package com.mehmetbozkurt.clink.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContrastTest {
    @Test
    fun blackOnWhiteIs21() = assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.01)

    /** Every text/background pair the screens use; a task that adds a pair adds it here. */
    @Test
    fun everyTextPairMeetsAA() {
        val pairs = listOf(
            ClinkColors.Paper to ClinkColors.Wall, ClinkColors.Paper to ClinkColors.WallRaised,
            ClinkColors.Muted to ClinkColors.Wall, ClinkColors.Muted to ClinkColors.WallRaised,
            ClinkColors.Ink to ClinkColors.Paper, ClinkColors.InkMuted to ClinkColors.Paper, ClinkColors.Ink to ClinkColors.Tape,
            ClinkColors.Ink to ClinkColors.Acid, ClinkColors.Ink to ClinkColors.Pink, ClinkColors.Ink to ClinkColors.Amber,
            ClinkColors.Acid to ClinkColors.Wall, ClinkColors.Amber to ClinkColors.Wall,
        )
        pairs.forEach { (fg, bg) -> assertTrue(contrastRatio(fg, bg) >= 4.5, "$fg on $bg: ${contrastRatio(fg, bg)}") }
    }
}
