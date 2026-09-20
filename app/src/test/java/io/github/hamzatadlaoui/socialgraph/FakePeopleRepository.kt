package io.github.hamzatadlaoui.socialgraph

import io.github.hamzatadlaoui.socialgraph.data.DocumentEntity
import io.github.hamzatadlaoui.socialgraph.data.DocumentTagEntity
import io.github.hamzatadlaoui.socialgraph.data.PeopleRepository
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.data.RelationshipEntity
import io.github.hamzatadlaoui.socialgraph.model.Certainty
import io.github.hamzatadlaoui.socialgraph.model.FuzzyDate
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * A [PeopleRepository] kept in memory, so a view model can be tested without
 * Room or a device. Only people and relationships are backed by real state -
 * the document shelf is untouched by anything under test here.
 */
class FakePeopleRepository(initialPeople: List<PersonEntity> = emptyList()) : PeopleRepository {

    private val people = MutableStateFlow(initialPeople)
    private val relationships = MutableStateFlow<List<RelationshipEntity>>(emptyList())

    override fun people(): Flow<List<PersonEntity>> = people

    override fun search(term: String): Flow<List<PersonEntity>> =
        people.map { list -> list.filter { it.fullName.contains(term, ignoreCase = true) } }

    override fun person(id: String): Flow<PersonEntity?> =
        people.map { list -> list.firstOrNull { it.id == id } }

    override fun me(): Flow<PersonEntity?> = people.map { list -> list.firstOrNull { it.isMe } }

    override fun relationshipsOf(personId: String): Flow<List<RelationshipEntity>> =
        relationships.map { list -> list.filter { it.fromId == personId } }

    override fun allRelationships(): Flow<List<RelationshipEntity>> = relationships

    override suspend fun find(id: String): PersonEntity? = people.value.firstOrNull { it.id == id }

    override suspend fun save(person: PersonEntity): PersonEntity {
        people.value = people.value.filterNot { it.id == person.id } + person
        return person
    }

    override suspend fun delete(person: PersonEntity) {
        people.value = people.value.filterNot { it.id == person.id }
        relationships.value = relationships.value.filterNot {
            it.fromId == person.id || it.toId == person.id
        }
    }

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
        relationships.value = relationships.value + forward + forward.inverse()
    }

    override suspend fun unlink(relationship: RelationshipEntity) {
        relationships.value = relationships.value.filterNot { it.pairId == relationship.pairId }
    }

    override suspend fun clearMe(keep: String?) {
        people.value = people.value.map { it.copy(isMe = it.id == keep) }
    }

    override suspend fun snapshot(): Pair<List<PersonEntity>, List<RelationshipEntity>> =
        people.value to relationships.value

    override suspend fun restore(people: List<PersonEntity>, relationships: List<RelationshipEntity>) {
        this.people.value = people
        this.relationships.value = relationships
    }

    override fun documents(): Flow<List<DocumentEntity>> = MutableStateFlow(emptyList())

    override fun searchDocuments(term: String): Flow<List<DocumentEntity>> = MutableStateFlow(emptyList())

    override fun document(id: String): Flow<DocumentEntity?> = MutableStateFlow(null)

    override fun tagsOn(documentId: String): Flow<List<DocumentTagEntity>> = MutableStateFlow(emptyList())

    override fun documentsOf(personId: String): Flow<List<DocumentEntity>> = MutableStateFlow(emptyList())

    override fun tagsOf(personId: String): Flow<List<DocumentTagEntity>> = MutableStateFlow(emptyList())

    override suspend fun findDocument(id: String): DocumentEntity? = null

    override suspend fun saveDocument(document: DocumentEntity): DocumentEntity = document

    override suspend fun deleteDocument(document: DocumentEntity) = Unit

    override suspend fun tag(tag: DocumentTagEntity) = Unit

    override suspend fun untag(tagId: String) = Unit

    override suspend fun documentSnapshot(): Pair<List<DocumentEntity>, List<DocumentTagEntity>> =
        emptyList<DocumentEntity>() to emptyList()

    override suspend fun restoreDocuments(documents: List<DocumentEntity>, tags: List<DocumentTagEntity>) = Unit
}
