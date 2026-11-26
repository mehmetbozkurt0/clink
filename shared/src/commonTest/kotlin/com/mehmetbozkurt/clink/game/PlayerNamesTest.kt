package com.mehmetbozkurt.clink.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlayerNamesTest {
    @Test
    fun trimsAndDropsBlankRows() =
        assertEquals(PlayerNamesResult.Valid(listOf("Ayşe", "Mert")), PlayerNames.validate(listOf("  Ayşe ", "", "   ", "Mert")))

    @Test
    fun tooFewAfterDroppingBlanks() = assertEquals(PlayerNamesResult.TooFew(2), PlayerNames.validate(listOf("Ayşe", " ")))

    @Test
    fun twoPlayersAreTooFewWhenAGameWantsThree() =
        assertEquals(PlayerNamesResult.TooFew(3), PlayerNames.validate(listOf("Mert", "Ayşe"), min = 3))

    @Test
    fun threePlayersPassWhenAGameWantsThree() = assertEquals(
        PlayerNamesResult.Valid(listOf("Mert", "Ayşe", "Can")),
        PlayerNames.validate(listOf("Mert", "Ayşe", "Can"), min = 3),
    )

    @Test
    fun tooMany() = assertEquals(PlayerNamesResult.TooMany, PlayerNames.validate((1..13).map { "P$it" }))

    @Test
    fun twelveIsAllowed() {
        assertIs<PlayerNamesResult.Valid>(PlayerNames.validate((1..12).map { "P$it" }))
    }

    @Test
    fun duplicatesAreCaseInsensitive() =
        assertEquals(PlayerNamesResult.Duplicate("mert"), PlayerNames.validate(listOf("Mert", "mert")))

    @Test
    fun duplicatesIgnoreTurkishDottedI() =
        assertEquals(PlayerNamesResult.Duplicate("irem"), PlayerNames.validate(listOf("İrem", "irem")))

    @Test
    fun longNamesAreCappedAt20() {
        val result = PlayerNames.validate(listOf("A".repeat(25), "Can")) as PlayerNamesResult.Valid
        assertEquals(20, result.names[0].length)
    }
}
