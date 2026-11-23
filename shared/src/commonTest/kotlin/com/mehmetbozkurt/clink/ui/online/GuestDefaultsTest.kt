package com.mehmetbozkurt.clink.ui.online

import com.mehmetbozkurt.clink.account.Avatars
import com.mehmetbozkurt.clink.account.Profile
import com.mehmetbozkurt.clink.prefs.LocalPrefs
import com.mehmetbozkurt.clink.testing.SequenceRandom
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GuestDefaultsTest {
    @Test
    fun profileWinsOverRememberedGuest() {
        val result = guestDefaults(Profile("Mehmet", "dog"), LocalPrefs(guestName = "Emu", guestAvatar = "robot"), Random(1))
        assertEquals(GuestDefaults("Mehmet", "dog"), result)
    }

    @Test
    fun rememberedGuestIsUsed() {
        val result = guestDefaults(null, LocalPrefs(guestName = "Emu", guestAvatar = "robot"), Random(1))
        assertEquals(GuestDefaults("Emu", "robot"), result)
    }

    @Test
    fun rememberedNameIsCleaned() {
        val long = guestDefaults(null, LocalPrefs(guestName = "  " + "x".repeat(30), guestAvatar = "robot"), Random(1))
        assertEquals("x".repeat(20), long.name)
        val blank = guestDefaults(null, LocalPrefs(guestName = "   ", guestAvatar = "robot"), Random(1))
        assertEquals("", blank.name)
    }

    @Test
    fun unknownRememberedAvatarFallsBack() {
        val result = guestDefaults(null, LocalPrefs(guestName = "Emu", guestAvatar = "dragon"), Random(1))
        assertTrue(result.avatarId in Avatars.ids)
    }

    @Test
    fun firstTimeGuestGetsARandomAvatar() {
        val result = guestDefaults(null, LocalPrefs(), SequenceRandom(5))
        assertEquals(GuestDefaults("", Avatars.ids[5]), result)
    }
}
