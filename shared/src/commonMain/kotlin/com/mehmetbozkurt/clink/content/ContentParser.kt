package com.mehmetbozkurt.clink.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Reads the content file format described in docs/content-format.md. */
object ContentParser {
    private const val SUPPORTED_VERSION = 1

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private class ContentFile(val version: Int, val packs: List<Pack>)

    fun parse(json: String): List<Pack> {
        val file = this.json.decodeFromString<ContentFile>(json)
        require(file.version == SUPPORTED_VERSION) { "Unsupported content format version: ${file.version}" }
        return file.packs
    }
}
