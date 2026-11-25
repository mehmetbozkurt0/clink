package com.mehmetbozkurt.clink.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PosterAccentTest {
    @Test
    fun findsTheWordInAnEnglishTitle() = assertEquals(9..12, accentRange("TRUTH OR DARE", "DARE"))

    @Test
    fun findsTheWordInATurkishTitle() = assertEquals(12..18, accentRange("DOĞRULUK MU CESARET Mİ", "CESARET"))

    @Test
    fun findsAWordWithDottedCapitalI() = assertEquals(0..3, accentRange("ŞİŞE ÇEVİRMECE", "ŞİŞE"))

    @Test
    fun missingWordHasNoAccent() = assertNull(accentRange("BEER PONG", "DARE"))

    @Test
    fun blankWordHasNoAccent() = assertNull(accentRange("BEER PONG", " "))
}
