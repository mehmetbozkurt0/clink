package com.mehmetbozkurt.clink.account

/** Profiles at `users/{uid}`; only the owner can read or write theirs (firebase/firestore.rules). */
interface ProfileRepository {
    suspend fun load(uid: String): Profile?
    suspend fun save(uid: String, profile: Profile)
    suspend fun delete(uid: String)
}
