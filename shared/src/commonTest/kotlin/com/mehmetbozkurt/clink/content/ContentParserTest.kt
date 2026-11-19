package com.mehmetbozkurt.clink.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ContentParserTest {
    @Test
    fun parse_readsPacksAndCards() {
        val packs = ContentParser.parse(
            """{"version":1,"packs":[{"id":"p","type":"DARE","level":"SPICY","access":"FREE",
            "cards":[{"id":"c1","text":{"tr":"a","en":"b"}}]}]}""",
        )
        assertEquals(CardType.DARE, packs.single().type)
        assertEquals("b", packs.single().cards.single().text["en"])
    }

    @Test
    fun parse_rejectsUnknownVersion() {
        assertFailsWith<IllegalArgumentException> { ContentParser.parse("""{"version":2,"packs":[]}""") }
    }
}
