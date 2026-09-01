package io.github.hamzatadlaoui.socialgraph.export

import io.github.hamzatadlaoui.socialgraph.data.DocumentEntity
import io.github.hamzatadlaoui.socialgraph.data.DocumentStore
import io.github.hamzatadlaoui.socialgraph.data.DocumentTagEntity
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.data.PhotoStore
import io.github.hamzatadlaoui.socialgraph.data.RelationshipEntity
import io.github.hamzatadlaoui.socialgraph.model.Certainty
import io.github.hamzatadlaoui.socialgraph.model.FuzzyDate
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The whole database as one file the user owns: a `backup.json` describing
 * every person, every tie and every document, plus a copy of every photo and
 * every filed document.
 *
 * Written as plain JSON on purpose (section 16 asks for the format to be
 * documented): a backup should still be readable in ten years by something
 * that is not this app.
 *
 * Version 2 added `documents` and `documentTags`. A version 1 file simply has
 * neither, and still restores - which is why every reader here is written to
 * find nothing rather than to fail.
 */
object Backup {

    const val VERSION = 2
    const val JSON_ENTRY = "backup.json"
    const val PHOTOS_ENTRY = "photos/"
    const val DOCUMENTS_ENTRY = "documents/"

    /** What the file is called when the user is asked where to put it. */
    fun fileName(now: Long): String = "social-graph-${DATE.format(java.util.Date(now))}.zip"

    fun toJson(
        people: List<PersonEntity>,
        relationships: List<RelationshipEntity>,
        documents: List<DocumentEntity> = emptyList(),
        tags: List<DocumentTagEntity> = emptyList(),
    ): JSONObject = JSONObject()
        .put(KEY_VERSION, VERSION)
        .put(KEY_PEOPLE, JSONArray().apply { people.forEach { put(it.toJson()) } })
        .put(KEY_TIES, JSONArray().apply { relationships.forEach { put(it.toJson()) } })
        .put(KEY_DOCUMENTS, JSONArray().apply { documents.forEach { put(it.toJson()) } })
        .put(KEY_TAGS, JSONArray().apply { tags.forEach { put(it.toJson()) } })

    fun peopleFrom(json: JSONObject): List<PersonEntity> =
        json.optJSONArray(KEY_PEOPLE).objects().mapNotNull { personFrom(it) }

    fun relationshipsFrom(json: JSONObject): List<RelationshipEntity> =
        json.optJSONArray(KEY_TIES).objects().mapNotNull { relationshipFrom(it) }

    fun documentsFrom(json: JSONObject): List<DocumentEntity> =
        json.optJSONArray(KEY_DOCUMENTS).objects().mapNotNull { documentFrom(it) }

    fun tagsFrom(json: JSONObject): List<DocumentTagEntity> =
        json.optJSONArray(KEY_TAGS).objects().mapNotNull { tagFrom(it) }

    /**
     * Writes the zip: the JSON first, then every photo any person refers to and
     * every document on the shelf. Documents are included rather than merely
     * listed, because a backup that restores to a set of dangling entries with
     * no files behind them is not a backup.
     */
    fun write(
        out: OutputStream,
        people: List<PersonEntity>,
        relationships: List<RelationshipEntity>,
        photos: PhotoStore,
        documents: List<DocumentEntity> = emptyList(),
        tags: List<DocumentTagEntity> = emptyList(),
        files: DocumentStore? = null,
    ) {
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_ENTRY))
            zip.write(toJson(people, relationships, documents, tags).toString(2).toByteArray())
            zip.closeEntry()

            for (name in people.map { it.photo }.filter { it.isNotEmpty() }.distinct()) {
                val file = photos.file(name).takeIf { it.isFile } ?: continue
                zip.putNextEntry(ZipEntry(PHOTOS_ENTRY + name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }

            if (files != null) {
                for (name in documents.map { it.fileName }.filter { it.isNotEmpty() }.distinct()) {
                    val file = files.file(name).takeIf { it.isFile } ?: continue
                    zip.putNextEntry(ZipEntry(DOCUMENTS_ENTRY + name))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    /**
     * Reads a zip back, putting the photos where they belong and handing back
     * what the caller should write to the database.
     */
    fun read(input: InputStream, photos: PhotoStore, files: DocumentStore? = null): Restored {
        var json: JSONObject? = null

        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                when {
                    entry.name == JSON_ENTRY -> json = runCatching {
                        JSONObject(zip.readBytes().decodeToString())
                    }.getOrNull()

                    entry.name.startsWith(PHOTOS_ENTRY) && !entry.isDirectory -> {
                        val name = entry.name.removePrefix(PHOTOS_ENTRY)
                        if (safe(name)) photos.write(name, zip.readBytes())
                    }

                    entry.name.startsWith(DOCUMENTS_ENTRY) && !entry.isDirectory -> {
                        val name = entry.name.removePrefix(DOCUMENTS_ENTRY)
                        if (safe(name) && files != null) files.write(name, zip.readBytes())
                    }
                }
                zip.closeEntry()
            }
        }

        val read = json ?: return Restored(emptyList(), emptyList())
        return Restored(
            people = peopleFrom(read),
            relationships = relationshipsFrom(read),
            documents = documentsFrom(read),
            tags = tagsFrom(read),
        )
    }

    /**
     * Only ever write inside the folder we meant, whatever the zip claims a file
     * is called - a name with a separator or a parent reference in it is how an
     * archive talks its way into somewhere else on disk.
     */
    private fun safe(name: String): Boolean =
        name.isNotEmpty() && '/' !in name && '\\' !in name && !name.startsWith("..")

    data class Restored(
        val people: List<PersonEntity>,
        val relationships: List<RelationshipEntity>,
        val documents: List<DocumentEntity> = emptyList(),
        val tags: List<DocumentTagEntity> = emptyList(),
    )

    private fun PersonEntity.toJson() = JSONObject()
        .put("id", id)
        .put("displayName", displayName)
        .put("lastName", lastName)
        .put("nickname", nickname)
        .put("photo", photo)
        .put("notes", notes)
        .put("birth", birth.store())
        .put("death", death.store())
        .put("pronouns", pronouns)
        .put("isMe", isMe)
        .put("isFavourite", isFavourite)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)

    private fun personFrom(json: JSONObject): PersonEntity? {
        val id = json.optString("id").ifEmpty { return null }
        val name = json.optString("displayName").ifEmpty { return null }
        return PersonEntity(
            id = id,
            displayName = name,
            lastName = json.optString("lastName"),
            nickname = json.optString("nickname"),
            photo = json.optString("photo"),
            notes = json.optString("notes"),
            birth = FuzzyDate.parse(json.optString("birth")),
            death = FuzzyDate.parse(json.optString("death")),
            pronouns = json.optString("pronouns"),
            isMe = json.optBoolean("isMe"),
            isFavourite = json.optBoolean("isFavourite"),
            createdAt = json.optLong("createdAt"),
            updatedAt = json.optLong("updatedAt"),
        )
    }

    private fun RelationshipEntity.toJson() = JSONObject()
        .put("id", id)
        .put("pairId", pairId)
        .put("fromId", fromId)
        .put("toId", toId)
        .put("type", type.name)
        .put("customLabel", customLabel)
        .put("start", start.store())
        .put("end", end.store())
        .put("notes", notes)
        .put("certainty", certainty.name)

    private fun relationshipFrom(json: JSONObject): RelationshipEntity? {
        val id = json.optString("id").ifEmpty { return null }
        val fromId = json.optString("fromId").ifEmpty { return null }
        val toId = json.optString("toId").ifEmpty { return null }
        return RelationshipEntity(
            id = id,
            pairId = json.optString("pairId").ifEmpty { id },
            fromId = fromId,
            toId = toId,
            type = RelationshipType.fromName(json.optString("type")) ?: RelationshipType.KNOWS,
            customLabel = json.optString("customLabel"),
            start = FuzzyDate.parse(json.optString("start")),
            end = FuzzyDate.parse(json.optString("end")),
            notes = json.optString("notes"),
            certainty = Certainty.fromName(json.optString("certainty")) ?: Certainty.SURE,
        )
    }

    private fun DocumentEntity.toJson() = JSONObject()
        .put("id", id)
        .put("fileName", fileName)
        .put("originalName", originalName)
        .put("mimeType", mimeType)
        .put("title", title)
        .put("notes", notes)
        .put("dated", dated.store())
        .put("sizeBytes", sizeBytes)
        .put("addedAt", addedAt)

    private fun documentFrom(json: JSONObject): DocumentEntity? {
        val id = json.optString("id").ifEmpty { return null }
        val fileName = json.optString("fileName").ifEmpty { return null }
        return DocumentEntity(
            id = id,
            fileName = fileName,
            originalName = json.optString("originalName"),
            mimeType = json.optString("mimeType"),
            title = json.optString("title"),
            notes = json.optString("notes"),
            dated = FuzzyDate.parse(json.optString("dated")),
            sizeBytes = json.optLong("sizeBytes"),
            addedAt = json.optLong("addedAt"),
        )
    }

    private fun DocumentTagEntity.toJson() = JSONObject()
        .put("id", id)
        .put("documentId", documentId)
        .put("personId", personId)
        .put("left", left.toDouble())
        .put("top", top.toDouble())
        .put("right", right.toDouble())
        .put("bottom", bottom.toDouble())
        .put("note", note)

    private fun tagFrom(json: JSONObject): DocumentTagEntity? {
        val id = json.optString("id").ifEmpty { return null }
        val documentId = json.optString("documentId").ifEmpty { return null }
        val personId = json.optString("personId").ifEmpty { return null }
        return DocumentTagEntity(
            id = id,
            documentId = documentId,
            personId = personId,
            left = json.optDouble("left", 0.0).toFloat(),
            top = json.optDouble("top", 0.0).toFloat(),
            right = json.optDouble("right", 0.0).toFloat(),
            bottom = json.optDouble("bottom", 0.0).toFloat(),
            note = json.optString("note"),
        )
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        (0 until (this?.length() ?: 0)).mapNotNull { this?.optJSONObject(it) }

    private const val KEY_VERSION = "version"
    private const val KEY_PEOPLE = "people"
    private const val KEY_TIES = "relationships"
    private const val KEY_DOCUMENTS = "documents"
    private const val KEY_TAGS = "documentTags"

    private val DATE = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
}
