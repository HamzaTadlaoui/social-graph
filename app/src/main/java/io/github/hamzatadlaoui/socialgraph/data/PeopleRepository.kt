package io.github.hamzatadlaoui.socialgraph.data

import androidx.room.withTransaction
import io.github.hamzatadlaoui.socialgraph.model.Certainty
import io.github.hamzatadlaoui.socialgraph.model.FuzzyDate
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import kotlinx.coroutines.flow.Flow

/**
 * Everything the screens are allowed to ask of the database.
 *
 * The interface exists so the storage underneath can be swapped without the UI
 * noticing - and, more usefully, so that [link] and [unlink] are the only place
 * in the app that knows a tie is kept as two rows.
 */
interface PeopleRepository {

    fun people(): Flow<List<PersonEntity>>

    fun search(term: String): Flow<List<PersonEntity>>

    fun person(id: String): Flow<PersonEntity?>

    fun me(): Flow<PersonEntity?>

    fun relationshipsOf(personId: String): Flow<List<RelationshipEntity>>

    fun allRelationships(): Flow<List<RelationshipEntity>>

    suspend fun find(id: String): PersonEntity?

    suspend fun save(person: PersonEntity): PersonEntity

    suspend fun delete(person: PersonEntity)

    /** Records a tie, and the same tie seen from the other end. */
    suspend fun link(
        fromId: String,
        toId: String,
        type: RelationshipType,
        customLabel: String = "",
        start: FuzzyDate = FuzzyDate.Unknown,
        end: FuzzyDate = FuzzyDate.Unknown,
        notes: String = "",
        certainty: Certainty = Certainty.SURE,
    )

    /** Removes a tie from both people. */
    suspend fun unlink(relationship: RelationshipEntity)

    /** Takes the "this is you" mark off everyone except [keep]. */
    suspend fun clearMe(keep: String?)

    /** Everything, for writing a backup. */
    suspend fun snapshot(): Pair<List<PersonEntity>, List<RelationshipEntity>>

    /**
     * Puts a backup back. People and ties are matched on the ids they were
     * saved under, so restoring the same file twice leaves one copy, not two.
     */
    suspend fun restore(people: List<PersonEntity>, relationships: List<RelationshipEntity>)

    // The document shelf. Kept on this interface rather than a second one
    // because a tag is a tie between a person and a file, and the screens that
    // ask about one invariably ask about the other.

    fun documents(): Flow<List<DocumentEntity>>

    fun searchDocuments(term: String): Flow<List<DocumentEntity>>

    fun document(id: String): Flow<DocumentEntity?>

    /** Who has been marked in this document, and whereabouts. */
    fun tagsOn(documentId: String): Flow<List<DocumentTagEntity>>

    /** The other direction: every document this person appears in. */
    fun documentsOf(personId: String): Flow<List<DocumentEntity>>

    fun tagsOf(personId: String): Flow<List<DocumentTagEntity>>

    suspend fun findDocument(id: String): DocumentEntity?

    suspend fun saveDocument(document: DocumentEntity): DocumentEntity

    suspend fun deleteDocument(document: DocumentEntity)

    suspend fun tag(tag: DocumentTagEntity)

    suspend fun untag(tagId: String)

    suspend fun documentSnapshot(): Pair<List<DocumentEntity>, List<DocumentTagEntity>>

    suspend fun restoreDocuments(documents: List<DocumentEntity>, tags: List<DocumentTagEntity>)
}

class RoomPeopleRepository(
    private val db: SocialGraphDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) : PeopleRepository {

    private val people = db.people()
    private val relationships = db.relationships()
    private val documents = db.documents()

    override fun people(): Flow<List<PersonEntity>> = people.all()

    override fun search(term: String): Flow<List<PersonEntity>> =
        if (term.isBlank()) people.all() else people.search(term.trim())

    override fun person(id: String): Flow<PersonEntity?> = people.byId(id)

    override fun me(): Flow<PersonEntity?> = people.me()

    override fun relationshipsOf(personId: String): Flow<List<RelationshipEntity>> =
        relationships.of(personId)

    override fun allRelationships(): Flow<List<RelationshipEntity>> = relationships.all()

    override suspend fun find(id: String): PersonEntity? = people.find(id)

    override suspend fun save(person: PersonEntity): PersonEntity {
        val stamped = person.copy(
            createdAt = if (person.createdAt == 0L) now() else person.createdAt,
            updatedAt = now(),
        )
        people.upsert(stamped)
        return stamped
    }

    override suspend fun delete(person: PersonEntity) = people.delete(person)

    override suspend fun link(
        fromId: String,
        toId: String,
        type: RelationshipType,
        customLabel: String,
        start: FuzzyDate,
        end: FuzzyDate,
        notes: String,
        certainty: Certainty,
    ) {
        // Nobody is their own sibling.
        if (fromId == toId) return

        val forward = RelationshipEntity(
            fromId = fromId,
            toId = toId,
            type = type,
            customLabel = customLabel,
            start = start,
            end = end,
            notes = notes,
            certainty = certainty,
        )
        // One transaction, so a half-written tie can never be read back.
        db.withTransaction {
            relationships.insert(listOf(forward, forward.inverse()))
        }
    }

    override suspend fun unlink(relationship: RelationshipEntity) {
        relationships.deletePair(relationship.pairId)
    }

    override suspend fun clearMe(keep: String?) = people.clearMe(keep.orEmpty())

    override suspend fun snapshot(): Pair<List<PersonEntity>, List<RelationshipEntity>> =
        db.withTransaction { people.snapshot() to relationships.snapshot() }

    override suspend fun restore(
        people: List<PersonEntity>,
        relationships: List<RelationshipEntity>,
    ) {
        db.withTransaction {
            // People first: a tie whose ends are missing would be refused by
            // the foreign keys.
            this@RoomPeopleRepository.people.upsertAll(people)
            val known = people.map { it.id }.toSet() +
                this@RoomPeopleRepository.people.snapshot().map { it.id }
            this@RoomPeopleRepository.relationships.upsertAll(
                relationships.filter { it.fromId in known && it.toId in known },
            )
        }
    }

    override fun documents(): Flow<List<DocumentEntity>> = documents.all()

    override fun searchDocuments(term: String): Flow<List<DocumentEntity>> =
        if (term.isBlank()) documents.all() else documents.search(term.trim())

    override fun document(id: String): Flow<DocumentEntity?> = documents.byId(id)

    override fun tagsOn(documentId: String): Flow<List<DocumentTagEntity>> =
        documents.tagsOn(documentId)

    override fun documentsOf(personId: String): Flow<List<DocumentEntity>> =
        documents.documentsOf(personId)

    override fun tagsOf(personId: String): Flow<List<DocumentTagEntity>> =
        documents.tagsOf(personId)

    override suspend fun findDocument(id: String): DocumentEntity? = documents.find(id)

    override suspend fun saveDocument(document: DocumentEntity): DocumentEntity {
        val stamped = document.copy(addedAt = if (document.addedAt == 0L) now() else document.addedAt)
        documents.upsert(stamped)
        return stamped
    }

    override suspend fun deleteDocument(document: DocumentEntity) = documents.delete(document)

    override suspend fun tag(tag: DocumentTagEntity) = documents.upsertTag(tag)

    override suspend fun untag(tagId: String) = documents.deleteTag(tagId)

    override suspend fun documentSnapshot(): Pair<List<DocumentEntity>, List<DocumentTagEntity>> =
        db.withTransaction { documents.snapshot() to documents.tagSnapshot() }

    override suspend fun restoreDocuments(
        documents: List<DocumentEntity>,
        tags: List<DocumentTagEntity>,
    ) {
        db.withTransaction {
            this@RoomPeopleRepository.documents.upsertAll(documents)
            // A tag needs both ends to exist, the same as a tie does.
            val files = this@RoomPeopleRepository.documents.snapshot().map { it.id }.toSet()
            val known = this@RoomPeopleRepository.people.snapshot().map { it.id }.toSet()
            this@RoomPeopleRepository.documents.upsertTags(
                tags.filter { it.documentId in files && it.personId in known },
            )
        }
    }
}
