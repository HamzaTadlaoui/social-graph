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
}

class RoomPeopleRepository(
    private val db: SocialGraphDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) : PeopleRepository {

    private val people = db.people()
    private val relationships = db.relationships()

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
}
