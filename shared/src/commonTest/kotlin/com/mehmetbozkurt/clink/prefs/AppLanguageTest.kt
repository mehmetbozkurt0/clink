package com.mehmetbozkurt.clink.prefs

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageTest {
    @Test
    fun tagsRoundTrip() = AppLanguage.entries.forEach { assertEquals(it, AppLanguage.fromTag(it.tag)) }

    @Test
    fun regionIsIgnored() = assertEquals(AppLanguage.TURKISH, AppLanguage.fromTag("tr-TR"))

    @Test
    fun unknownIsSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("de"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(null))
    }
}
