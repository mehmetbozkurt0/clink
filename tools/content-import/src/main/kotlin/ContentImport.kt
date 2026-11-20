import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import kotlin.system.exitProcess

/**
 * Pushes the repo's content file (the single source of truth, see docs/content-format.md) to Firestore:
 * every pack to `packs/{id}`, packs no longer in the file are deleted, and `meta/content.version` goes up by one
 * so the app knows to download again. Run with `./gradlew :tools:content-import:run` (runs the content tests first).
 */
fun main(args: Array<String>) {
    val contentFile = File(args.getOrElse(0) { "shared/src/commonMain/composeResources/files/content/baseline.json" })
    val keyFile = File(args.getOrElse(1) { "secrets/firebase-admin.json" })
    if (!keyFile.exists()) fail("Admin key not found at ${keyFile.path} (see docs/firebase-setup.md, step 6).")

    val packs = readPacks(contentFile.readText())
    if (packs.isEmpty()) fail("No packs in ${contentFile.path}; nothing is pushed or deleted.")

    val options = FirebaseOptions.builder().setCredentials(keyFile.inputStream().use { GoogleCredentials.fromStream(it) }).build()
    FirebaseApp.initializeApp(options)
    val db = FirestoreClient.getFirestore()

    val remoteIds = db.collection("packs").listDocuments().map { it.id }.toSet()
    val plan = importPlan(packs.map { it.first }.toSet(), remoteIds)
    val meta = db.collection("meta").document("content")
    val version = (meta.get().get().getLong("version") ?: 0L) + 1

    val batch = db.batch()
    packs.filter { it.first in plan.upsert }.forEach { (id, doc) -> batch.set(db.collection("packs").document(id), doc) }
    plan.delete.forEach { batch.delete(db.collection("packs").document(it)) }
    batch.set(meta, mapOf("version" to version))
    batch.commit().get()

    println("Pushed ${plan.upsert.size} packs, deleted ${plan.delete.size}, content version is now $version.")
}

data class ImportPlan(val upsert: Set<String>, val delete: Set<String>)

/** Every local pack is written; remote packs missing locally are deleted, unless the local set is empty (a broken read). */
fun importPlan(localPackIds: Set<String>, remotePackIds: Set<String>): ImportPlan =
    ImportPlan(upsert = localPackIds, delete = if (localPackIds.isEmpty()) emptySet() else remotePackIds - localPackIds)

/** The packs of a content file as (id, Firestore document) pairs; the document holds every pack field except `id`. */
fun readPacks(json: String): List<Pair<String, Map<String, Any?>>> =
    Json.parseToJsonElement(json).jsonObject.getValue("packs").jsonArray.map { pack ->
        val fields = pack.jsonObject
        fields.getValue("id").jsonPrimitive.content to fields.filterKeys { it != "id" }.mapValues { toFirestore(it.value) }
    }

private fun toFirestore(element: JsonElement): Any? = when (element) {
    is JsonNull -> null
    is JsonObject -> element.mapValues { toFirestore(it.value) }
    is JsonArray -> element.map { toFirestore(it) }
    is JsonPrimitive -> if (element.isString) element.content else element.booleanOrNull ?: element.longOrNull ?: element.content
}

private fun fail(message: String): Nothing {
    System.err.println(message)
    exitProcess(1)
}
