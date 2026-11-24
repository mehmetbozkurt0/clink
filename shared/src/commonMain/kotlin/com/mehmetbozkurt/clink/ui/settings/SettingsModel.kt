package com.mehmetbozkurt.clink.ui.settings

import com.mehmetbozkurt.clink.account.AuthUser
import com.mehmetbozkurt.clink.account.Avatars
import com.mehmetbozkurt.clink.account.Profile
import com.mehmetbozkurt.clink.account.ProfileInput
import com.mehmetbozkurt.clink.account.ProfileNameResult
import com.mehmetbozkurt.clink.prefs.LocalPrefs

/** Who the settings screen's profile card shows. */
sealed interface ProfileCard {
    /** A Google account: its profile, or the Google name and the first avatar while there is no profile in hand. */
    data class Google(val name: String, val avatarId: String) : ProfileCard

    /** Not signed in, or an anonymous guest: the identity remembered on this phone, if any. */
    data class Guest(val name: String?, val avatarId: String?) : ProfileCard
}

fun profileCard(user: AuthUser?, profile: Profile?, prefs: LocalPrefs): ProfileCard {
    if (user != null && !user.isAnonymous) {
        return ProfileCard.Google(profile?.displayName ?: user.googleName.orEmpty(), profile?.avatarId ?: Avatars.ids.first())
    }
    val name = (ProfileInput.validate(prefs.guestName.orEmpty()) as? ProfileNameResult.Valid)?.name
    return ProfileCard.Guest(name, prefs.guestAvatar?.takeIf { it in Avatars.ids })
}

/** Sign out and delete exist only for a Google account. */
fun showsAccountSection(user: AuthUser?): Boolean = user != null && !user.isAnonymous
