package com.mehmetbozkurt.clink.content

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContentRepositoryTest {
    private fun pack(cardId: String, tr: String = "Soru", en: String? = "Question") = Pack(
        "p-$cardId", CardType.TRUTH, Level.CASUAL, Access.FREE,
        listOf(Card(cardId, buildMap { put("tr", tr); if (en != null) put("en", en) })),
    )

    private val bundled = listOf(pack("bundled-1"))

    private class FakeCache(var text: String? = null) : ContentCache {
        override suspend fun read(): String? = text
        override suspend fun write(text: String) {
            this.text = text
        }
    }

    private class FakeRemote(
        val version: Int,
        val packs: List<Pack>,
        val fail: Boolean = false,
    ) : ContentRemote {
        var packsCalls = 0
        override suspend fun version(): Int = if (fail) error("offline") else version
        override suspend fun packs(): List<Pack> {
            packsCalls++
            return packs
        }
    }

    private fun repository(cache: ContentCache, remote: ContentRemote = FakeRemote(0, emptyList())) =
        ContentRepository(cache, remote, bundled = { bundled })

    private fun cached(version: Int, vararg packs: Pack) = ContentRepository.encode(CachedContent(version, packs.toList()))

    @Test
    fun firstStartUsesBundled() = runTest {
        assertEquals(listOf("bundled-1"), repository(FakeCache()).load().cardIds(CardType.TRUTH, Level.CASUAL))
    }

    @Test
    fun validCacheWinsOverBundled() = runTest {
        val cache = FakeCache(cached(3, pack("cached-1")))
        assertEquals(listOf("cached-1"), repository(cache).load().cardIds(CardType.TRUTH, Level.CASUAL))
    }

    @Test
    fun brokenCacheFallsBackToBundled() = runTest {
        val cache = FakeCache(cached(3, pack("blank", tr = " ")))
        assertEquals(listOf("bundled-1"), repository(cache).load().cardIds(CardType.TRUTH, Level.CASUAL))
    }

    @Test
    fun unreadableCacheFallsBackToBundled() = runTest {
        assertEquals(listOf("bundled-1"), repository(FakeCache("{not json")).load().cardIds(CardType.TRUTH, Level.CASUAL))
    }

    @Test
    fun refreshStoresNewerRemoteContent() = runTest {
        val cache = FakeCache(cached(1, pack("old")))
        repository(cache, FakeRemote(2, listOf(pack("new")))).refresh()
        assertEquals(CachedContent(2, listOf(pack("new"))), ContentRepository.decode(cache.text!!))
    }

    @Test
    fun refreshSkipsSameVersion() = runTest {
        val remote = FakeRemote(2, listOf(pack("new")))
        repository(FakeCache(cached(2, pack("old"))), remote).refresh()
        assertEquals(0, remote.packsCalls)
    }

    @Test
    fun refreshIgnoresInvalidRemote() = runTest {
        val cache = FakeCache(cached(1, pack("old")))
        repository(cache, FakeRemote(2, listOf(pack("blank", tr = "")))).refresh()
        assertEquals(1, ContentRepository.decode(cache.text!!).version)
    }

    @Test
    fun refreshIgnoresRemoteMissingALanguage() = runTest {
        val cache = FakeCache(cached(1, pack("old")))
        repository(cache, FakeRemote(2, listOf(pack("tr-only", en = null)))).refresh()
        assertEquals(1, ContentRepository.decode(cache.text!!).version)
    }

    @Test
    fun refreshSurvivesRemoteFailure() = runTest {
        val cache = FakeCache(cached(1, pack("old")))
        repository(cache, FakeRemote(2, listOf(pack("new")), fail = true)).refresh()
        assertEquals(1, ContentRepository.decode(cache.text!!).version)
    }

    @Test
    fun refreshIgnoresEmptyRemote() = runTest {
        val cache = FakeCache()
        repository(cache, FakeRemote(2, emptyList())).refresh()
        assertNull(cache.text)
    }
}
