package com.mehmetbozkurt.clink.game

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerOrderTest {
    @Test
    fun movesDown() = assertEquals(listOf("b", "a", "c"), listOf("a", "b", "c").moved(0, 1))

    @Test
    fun movesUpToTop() = assertEquals(listOf("c", "a", "b"), listOf("a", "b", "c").moved(2, 0))

    @Test
    fun outOfRangeIsUnchanged() = assertEquals(listOf("a", "b"), listOf("a", "b").moved(0, 5))
}
