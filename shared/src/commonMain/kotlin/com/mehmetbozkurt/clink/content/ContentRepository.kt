package com.mehmetbozkurt.clink.content

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * Which cards the app uses (spec §6.3): the content downloaded last time if it is valid, otherwise the set bundled
 * in the app. [refresh] downloads newer content in the background for the **next** start, so cards never change in
 * the middle of a session.
 */
class ContentRepository(
    private val cache: ContentCache,
    private val remote: ContentRemote,
    private val bundled: suspend () -> List<Pack>,
) {
    suspend fun load(): ContentLibrary = ContentLibrary(validCache()?.packs ?: bundled())

    /** Never throws: no connection, no permission or broken remote content all leave the cache as it was. */
    suspend fun refresh() {
        try {
            val remoteVersion = remote.version()
            // Any other version means new content: it can also go down, e.g. after the Firebase project is recreated.
            if (remoteVersion == validCache()?.version) return
            val packs = remote.packs()
            if (!isUsable(packs)) return
            cache.write(encode(CachedContent(remoteVersion, packs)))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Keep what we have; the next start tries again.
        }
    }

    private suspend fun validCache(): CachedContent? = try {
        cache.read()?.let(::decode)?.takeIf { isUsable(it.packs) }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun encode(content: CachedContent): String = json.encodeToString(CachedContent.serializer(), content)
        fun decode(text: String): CachedContent = json.decodeFromString(CachedContent.serializer(), text)

        /** The same checks as the bundled file, plus every card in Turkish and English (docs/content-format.md). */
        private fun isUsable(packs: List<Pack>): Boolean =
            packs.isNotEmpty() &&
                runCatching { ContentParser.validate(packs) }.isSuccess &&
                packs.flatMap { it.cards }.all { it.text.keys.containsAll(listOf("tr", "en")) }
    }
}
