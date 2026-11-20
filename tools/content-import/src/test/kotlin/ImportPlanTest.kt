import kotlin.test.Test
import kotlin.test.assertEquals

class ImportPlanTest {
    @Test
    fun newAndChangedPacksAreUpserted() = assertEquals(setOf("a", "b"), importPlan(setOf("a", "b"), setOf("b")).upsert)

    @Test
    fun packsGoneFromTheRepoAreDeleted() = assertEquals(setOf("old"), importPlan(setOf("a"), setOf("a", "old")).delete)

    // A broken or empty read must never wipe Firestore.
    @Test
    fun emptyLocalFileDeletesNothing() = assertEquals(emptySet<String>(), importPlan(emptySet(), setOf("a")).delete)

    @Test
    fun packBecomesAFirestoreDocument() {
        val json = """{"version":1,"packs":[{"id":"p1","type":"TRUTH","level":"CASUAL","access":"FREE",
            "cards":[{"id":"c1","text":{"tr":"Soru","en":"Question"}}]}]}"""
        val (id, doc) = readPacks(json).single()
        assertEquals("p1", id)
        assertEquals(
            mapOf(
                "type" to "TRUTH", "level" to "CASUAL", "access" to "FREE",
                "cards" to listOf(mapOf("id" to "c1", "text" to mapOf("tr" to "Soru", "en" to "Question"))),
            ),
            doc,
        )
    }
}
