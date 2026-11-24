package com.mehmetbozkurt.clink.account

import com.mehmetbozkurt.clink.prefs.PrefsRepository

/** A guest's name and avatar from settings, kept on this phone; the next Join starts with them. */
suspend fun saveGuestProfile(prefs: PrefsRepository, profile: Profile) {
    prefs.update { it.copy(guestName = profile.displayName, guestAvatar = profile.avatarId) }
}
