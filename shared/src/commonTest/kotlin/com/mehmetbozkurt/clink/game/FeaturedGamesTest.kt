package com.mehmetbozkurt.clink.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeaturedGamesTest {
    @Test
    fun threeDistinctRealGames() {
        assertEquals(3, FeaturedGames.ids.distinct().size)
        assertTrue(GameId.entries.containsAll(FeaturedGames.ids))
    }
}
