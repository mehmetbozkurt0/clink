package com.mehmetbozkurt.clink.content

/** The content on the server: a version number that goes up on every import, and the packs. */
interface ContentRemote {
    suspend fun version(): Int
    suspend fun packs(): List<Pack>
}
