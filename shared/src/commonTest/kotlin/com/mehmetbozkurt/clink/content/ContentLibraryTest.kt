package com.mehmetbozkurt.clink.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ContentLibraryTest {
    private val lib = ContentLibrary(
        listOf(
            Pack(
                "t-c", CardType.TRUTH, Level.CASUAL, Access.FREE,
                listOf(
                    Card("t1", mapOf("tr" to "Soru", "en" to "Question")),
                    Card("t2", mapOf("tr" to "Sadece TR")),
                ),
            ),
            Pack("t-c2", CardType.TRUTH, Level.CASUAL, Access.FREE, listOf(Card("t3", mapOf("en" to "Q3")))),
            Pack("d-h", CardType.DARE, Level.HARD, Access.FREE, listOf(Card("d1", mapOf("en" to "Dare")))),
        ),
    )

    @Test
    fun cardIds_mergesPacksOfSameTypeAndLevel() =
        assertEquals(listOf("t1", "t2", "t3"), lib.cardIds(CardType.TRUTH, Level.CASUAL))

    @Test
    fun cardIds_emptyWhenNoPack() = assertEquals(emptyList(), lib.cardIds(CardType.DARE, Level.SPICY))

    @Test
    fun text_usesRequestedLanguage() = assertEquals("Soru", lib.text("t1", "tr"))

    @Test
    fun text_fallsBackToEnglish() = assertEquals("Question", lib.text("t1", "de"))

    @Test
    fun text_fallsBackToFirstAvailable() = assertEquals("Sadece TR", lib.text("t2", "en"))

    @Test
    fun text_unknownIdThrows() {
        assertFailsWith<IllegalArgumentException> { lib.text("nope", "en") }
    }
}
