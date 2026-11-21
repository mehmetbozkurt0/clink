package com.mehmetbozkurt.clink.account

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileTest {
    @Test
    fun trimsName() = assertEquals(ProfileNameResult.Valid("Ayşe"), ProfileInput.validate("  Ayşe "))

    @Test
    fun blankIsRejected() = assertEquals(ProfileNameResult.Blank, ProfileInput.validate("   "))

    @Test
    fun longNameIsCut() = assertEquals(ProfileNameResult.Valid("a".repeat(20)), ProfileInput.validate("a".repeat(25)))

    // trim, take(20), trim again
    @Test
    fun googleNameIsSuggestedAndCut() = assertEquals("Mehmet Bozkurt Uzun", suggestedName("  Mehmet Bozkurt Uzun İsim "))

    @Test
    fun noGoogleNameSuggestsNothing() = assertEquals("", suggestedName(null))

    @Test
    fun twelveDistinctAvatars() = assertEquals(12, Avatars.ids.toSet().size)
}
