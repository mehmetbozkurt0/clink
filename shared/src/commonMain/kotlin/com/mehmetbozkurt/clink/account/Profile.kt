package com.mehmetbozkurt.clink.account

import com.mehmetbozkurt.clink.game.PlayerNames
import kotlinx.serialization.Serializable

/** What the host shows to others (spec §4): a display name and a preset avatar; no photo. Stored at `users/{uid}`. */
@Serializable
data class Profile(val displayName: String, val avatarId: String)

/** The preset avatars (Phosphor icon names); ids are stored in profiles, so they never change. */
object Avatars {
    val ids = listOf("cat", "dog", "bird", "fish", "rabbit", "butterfly", "ghost", "alien", "robot", "rocket", "crown", "cactus")
}

sealed interface ProfileNameResult {
    data class Valid(val name: String) : ProfileNameResult
    data object Blank : ProfileNameResult
}

/** Profile names follow the player-name rule: trimmed, at most [PlayerNames.MAX_LENGTH] characters. */
object ProfileInput {
    fun validate(name: String): ProfileNameResult {
        val clean = name.trim().take(PlayerNames.MAX_LENGTH).trim()
        return if (clean.isEmpty()) ProfileNameResult.Blank else ProfileNameResult.Valid(clean)
    }
}

/** The Google account's name as a starting point for the profile name, cut to the limit; empty when there is none. */
fun suggestedName(googleName: String?): String = googleName?.trim()?.take(PlayerNames.MAX_LENGTH)?.trim() ?: ""
