package com.mehmetbozkurt.clink.content

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.Serializable

/**
 * Reads `meta/content` and `packs/{id}` as written by tools/content-import (reads need a signed-in user). Both reads
 * go to the server: Firestore's own cache could hand back a partial set of packs when the connection drops.
 */
class FirestoreContentRemote(private val db: FirebaseFirestore = Firebase.firestore) : ContentRemote {

    override suspend fun version(): Int =
        db.collection("meta").document("content").get(Source.SERVER).get<Long?>("version")?.toInt() ?: 0

    override suspend fun packs(): List<Pack> =
        db.collection("packs").get(Source.SERVER).documents.map { doc ->
            val fields = doc.data<PackDocument>()
            Pack(doc.id, fields.type, fields.level, fields.access, fields.cards)
        }

    /** A pack document: the pack's fields without its id, which is the document id. */
    @Serializable
    private class PackDocument(val type: CardType, val level: Level, val access: Access, val cards: List<Card>)
}
