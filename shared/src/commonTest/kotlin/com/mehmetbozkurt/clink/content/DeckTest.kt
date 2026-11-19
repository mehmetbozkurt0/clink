package com.mehmetbozkurt.clink.content

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeckTest {
    @Test
    fun draw_emptyDeckReturnsNull() = assertNull(Deck.of(emptyList()).draw(Random(1)))

    @Test
    fun draw_coversEveryCardBeforeRepeating() {
        var deck = Deck.of(listOf("a", "b", "c", "d"))
        val seen = (1..4).map { val (id, next) = deck.draw(Random(7))!!; deck = next; id }
        assertEquals(setOf("a", "b", "c", "d"), seen.toSet())
    }

    @Test
    fun draw_firstPassIsShuffled() {
        val firstDraws = (0 until 50).map { seed -> Deck.of(listOf("a", "b", "c", "d")).draw(Random(seed))!!.first }
        assertTrue(firstDraws.toSet().size > 1, "first draw was always ${firstDraws.first()}")
    }

    @Test
    fun draw_afterExhaustion_reshufflesWithoutImmediateRepeat() {
        repeat(200) { seed ->
            var deck = Deck.of(listOf("a", "b", "c"))
            val random = Random(seed)
            val draws = (1..9).map { val (id, next) = deck.draw(random)!!; deck = next; id }
            draws.zipWithNext().forEach { (x, y) -> assertNotEquals(x, y, "seed $seed: $draws") }
        }
    }

    @Test
    fun draw_singleCardDeckRepeatsThatCard() {
        var deck = Deck.of(listOf("only"))
        repeat(3) { val (id, next) = deck.draw(Random(0))!!; assertEquals("only", id); deck = next }
    }
}
