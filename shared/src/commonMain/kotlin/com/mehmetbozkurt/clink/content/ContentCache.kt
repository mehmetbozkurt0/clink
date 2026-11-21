package com.mehmetbozkurt.clink.content

import kotlinx.serialization.Serializable

/** Content downloaded from Firestore, kept on the phone; [version] is Firestore's `meta/content.version`. */
@Serializable
data class CachedContent(val version: Int, val packs: List<Pack>)

/** Where the downloaded content is stored on the phone, as text (the platform only reads and writes a file). */
interface ContentCache {
    suspend fun read(): String?
    suspend fun write(text: String)
}
