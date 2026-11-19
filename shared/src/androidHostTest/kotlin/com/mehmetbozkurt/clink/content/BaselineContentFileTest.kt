package com.mehmetbozkurt.clink.content

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Checks the real bundled content file on disk (JVM only). */
class BaselineContentFileTest {
    private val packs = ContentParser.parse(File("src/commonMain/composeResources/files/content/baseline.json").readText())

    @Test
    fun everyTypeAndLevelHasAtLeastThreeCards() {
        for (t in CardType.entries) for (l in Level.entries)
            assertTrue(packs.filter { it.type == t && it.level == l }.sumOf { it.cards.size } >= 3, "$t/$l")
    }

    @Test
    fun cardIdsAreUnique() {
        val ids = packs.flatMap { it.cards }.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyCardHasTurkishAndEnglish() {
        packs.flatMap { it.cards }.forEach { assertTrue(it.text.keys.containsAll(setOf("tr", "en")), it.id) }
    }

    @Test
    fun noCardMentionsDrinking() {
        // Whole words only ((?U) makes \b work with ç/ş), so "biraz" and "screenshot" don't match.
        val banned = Regex("(?iU)\\b(drink\\w*|drunk|shots?|beers?|alcohol\\w*|içki\\w*|alkol\\w*|sarhoş\\w*|bira(yı|ya|dan|nın|lar\\w*)?)\\b")
        packs.flatMap { it.cards }.forEach { c -> c.text.values.forEach { t -> assertNull(banned.find(t), "${c.id}: $t") } }
    }
}
