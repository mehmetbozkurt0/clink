package com.mehmetbozkurt.clink.account

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore

/** [ProfileRepository] on Firestore; offline reads come from Firestore's local cache, so a known host can play offline. */
class FirestoreProfileRepository(private val db: FirebaseFirestore = Firebase.firestore) : ProfileRepository {

    private fun doc(uid: String) = db.collection("users").document(uid)

    override suspend fun load(uid: String): Profile? = doc(uid).get().let { if (it.exists) it.data<Profile>() else null }

    override suspend fun save(uid: String, profile: Profile) = doc(uid).set(profile)

    override suspend fun delete(uid: String) = doc(uid).delete()
}
