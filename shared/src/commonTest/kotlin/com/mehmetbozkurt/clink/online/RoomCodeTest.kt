package com.mehmetbozkurt.clink.online

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomCodeTest {
    @Test
    fun generatesSixCharsFromTheAlphabet() {
        val random = Random(1)
        repeat(50) {
            val code = RoomCode.generate(random)
            assertEquals(6, code.length)
            assertTrue(code.all { it in RoomCode.ALPHABET }, code)
        }
    }

    @Test
    fun normalizesSpacesDashesAndCase() = assertEquals("ABC2D3", RoomCode.normalize(" abc-2d 3 "))

    // 0/O and 1/I/L are left out of codes, so they never match.
    @Test
    fun rejectsAmbiguousCharacters() = assertNull(RoomCode.normalize("ABC10O"))

    @Test
    fun rejectsWrongLength() {
        assertNull(RoomCode.normalize("ABC"))
        assertNull(RoomCode.normalize("ABCDEFG"))
    }

    @Test
    fun linkRoundTrip() = assertEquals("ABC234", codeFromLink(inviteLink("ABC234")))

    @Test
    fun linkWithQueryAndSlashWorks() = assertEquals("ABC234", codeFromLink("https://clink-game.com/join/abc234/?utm=x"))

    @Test
    fun foreignLinkIsNull() = assertNull(codeFromLink("https://example.com/join/ABC234"))
}
