package com.mehmetbozkurt.clink.ui.theme

import androidx.compose.ui.graphics.Color
import com.mehmetbozkurt.clink.content.Level
import kotlin.test.Test
import kotlin.test.assertEquals

class LevelColorsTest {
    @Test
    fun levelColors_matchSpec() {
        assertEquals(Color(0xFF5EE2C4), Level.CASUAL.color())
        assertEquals(Color(0xFFFFC65C), Level.HARD.color())
        assertEquals(Color(0xFFFF7A6B), Level.SPICY.color())
    }
}
