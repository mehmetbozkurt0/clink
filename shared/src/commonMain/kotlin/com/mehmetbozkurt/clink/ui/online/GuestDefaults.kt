package com.mehmetbozkurt.clink.ui.online

import com.mehmetbozkurt.clink.account.Avatars
import com.mehmetbozkurt.clink.account.Profile
import com.mehmetbozkurt.clink.account.ProfileInput
import com.mehmetbozkurt.clink.account.ProfileNameResult
import com.mehmetbozkurt.clink.prefs.LocalPrefs
import kotlin.random.Random

data class GuestDefaults(val name: String, val avatarId: String)

/**
 * The starting name and avatar on the Join screen: a signed-in profile wins, then the guest identity remembered from the
 * last join, then an empty name with a random avatar.
 */
fun guestDefaults(profile: Profile?, prefs: LocalPrefs, random: Random): GuestDefaults {
    if (profile != null) return GuestDefaults(profile.displayName, profile.avatarId)
    val name = (ProfileInput.validate(prefs.guestName.orEmpty()) as? ProfileNameResult.Valid)?.name.orEmpty()
    val avatar = prefs.guestAvatar?.takeIf { it in Avatars.ids } ?: Avatars.ids[random.nextInt(Avatars.ids.size)]
    return GuestDefaults(name, avatar)
}
