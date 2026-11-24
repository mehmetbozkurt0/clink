package com.mehmetbozkurt.clink.ui.settings

import com.mehmetbozkurt.clink.account.AuthUser
import com.mehmetbozkurt.clink.account.Avatars
import com.mehmetbozkurt.clink.account.Profile
import com.mehmetbozkurt.clink.prefs.LocalPrefs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsModelTest {
    private val google = AuthUser("u1", isAnonymous = false, googleName = "Mehmet B")
    private val anon = AuthUser("u2", isAnonymous = true, googleName = null)

    @Test
    fun googleUserWithProfileShowsProfile() =
        assertEquals(ProfileCard.Google("Mehmet", "cat"), profileCard(google, Profile("Mehmet", "cat"), LocalPrefs()))

    @Test
    fun googleUserWithoutProfileShowsGoogleName() =
        assertEquals(ProfileCard.Google("Mehmet B", Avatars.ids.first()), profileCard(google, null, LocalPrefs()))

    @Test
    fun anonymousGuestShowsGuestIdentity() = assertEquals(
        ProfileCard.Guest("Ayşe", Avatars.ids[1]),
        profileCard(anon, null, LocalPrefs(guestName = "Ayşe", guestAvatar = Avatars.ids[1])),
    )

    @Test
    fun signedOutWithNothingIsBlankGuest() = assertEquals(ProfileCard.Guest(null, null), profileCard(null, null, LocalPrefs()))

    @Test
    fun unknownGuestAvatarIsDropped() =
        assertEquals(ProfileCard.Guest("Ayşe", null), profileCard(null, null, LocalPrefs(guestName = "Ayşe", guestAvatar = "nope")))

    @Test
    fun accountSectionOnlyForGoogle() {
        assertTrue(showsAccountSection(google))
        assertFalse(showsAccountSection(anon))
        assertFalse(showsAccountSection(null))
    }
}
