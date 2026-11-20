package com.mehmetbozkurt.clink.ui.theme

import androidx.compose.ui.graphics.Color
import com.mehmetbozkurt.clink.content.Level
import kotlin.test.Test
import kotlin.test.assertEquals

class LevelColorsTest {
    @Test
    fun levelColors_matchSpec() {
        assertEquals(Color(0xFFC6FF00), Level.CASUAL.color())
        assertEquals(Color(0xFFFFB020), Level.HARD.color())
        assertEquals(Color(0xFFFF2E88), Level.SPICY.color())
    }
}
