package io.github.hamzatadlaoui.socialgraph

import io.github.hamzatadlaoui.socialgraph.data.DocumentEntity
import io.github.hamzatadlaoui.socialgraph.data.DocumentTagEntity
import io.github.hamzatadlaoui.socialgraph.data.EventAttendeeEntity
import io.github.hamzatadlaoui.socialgraph.data.EventEntity
import io.github.hamzatadlaoui.socialgraph.data.EventPhotoEntity
import io.github.hamzatadlaoui.socialgraph.data.FactEntity
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
 * Room or a device. People, relationships, the document shelf and facts are
 * all backed by real state, so a view model under test sees the same joins
 * and cascades the real repository would give it.
 */
class FakePeopleRepository(initialPeople: List<PersonEntity> = emptyList()) : PeopleRepository {

    private val people = MutableStateFlow(initialPeople)
    private val relationships = MutableStateFlow<List<RelationshipEntity>>(emptyList())
    private val documents = MutableStateFlow<List<DocumentEntity>>(emptyList())
    private val documentTags = MutableStateFlow<List<DocumentTagEntity>>(emptyList())
    private val facts = MutableStateFlow<List<FactEntity>>(emptyList())
    private val events = MutableStateFlow<List<EventEntity>>(emptyList())
    private val eventAttendees = MutableStateFlow<List<EventAttendeeEntity>>(emptyList())
    private val eventPhotos = MutableStateFlow<List<EventPhotoEntity>>(emptyList())

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

    override fun documents(): Flow<List<DocumentEntity>> = documents

    override fun searchDocuments(term: String): Flow<List<DocumentEntity>> =
        documents.map { list -> list.filter { it.title.contains(term, ignoreCase = true) } }

    override fun document(id: String): Flow<DocumentEntity?> =
        documents.map { list -> list.firstOrNull { it.id == id } }

    override fun tagsOn(documentId: String): Flow<List<DocumentTagEntity>> =
        documentTags.map { list -> list.filter { it.documentId == documentId } }

    override fun documentsOf(personId: String): Flow<List<DocumentEntity>> = documentTags.map { tags ->
        val taggedIn = tags.filter { it.personId == personId }.map { it.documentId }.toSet()
        documents.value.filter { it.id in taggedIn }
    }

    override fun tagsOf(personId: String): Flow<List<DocumentTagEntity>> =
        documentTags.map { list -> list.filter { it.personId == personId } }

    override suspend fun findDocument(id: String): DocumentEntity? =
        documents.value.firstOrNull { it.id == id }

    override suspend fun saveDocument(document: DocumentEntity): DocumentEntity {
        documents.value = documents.value.filterNot { it.id == document.id } + document
        return document
    }

    override suspend fun deleteDocument(document: DocumentEntity) {
        documents.value = documents.value.filterNot { it.id == document.id }
        documentTags.value = documentTags.value.filterNot { it.documentId == document.id }
    }

    override suspend fun tag(tag: DocumentTagEntity) {
        documentTags.value = documentTags.value.filterNot { it.id == tag.id } + tag
    }

    override suspend fun untag(tagId: String) {
        documentTags.value = documentTags.value.filterNot { it.id == tagId }
    }

    override suspend fun documentSnapshot(): Pair<List<DocumentEntity>, List<DocumentTagEntity>> =
        documents.value to documentTags.value

    override suspend fun restoreDocuments(documents: List<DocumentEntity>, tags: List<DocumentTagEntity>) {
        this.documents.value = documents
        this.documentTags.value = tags
    }

    override fun factsOf(personId: String): Flow<List<FactEntity>> =
        facts.map { list -> list.filter { it.personId == personId } }

    override suspend fun saveFact(fact: FactEntity): FactEntity {
        facts.value = facts.value.filterNot { it.id == fact.id } + fact
        return fact
    }

    override suspend fun deleteFact(id: String) {
        facts.value = facts.value.filterNot { it.id == id }
    }

    override suspend fun factSnapshot(): List<FactEntity> = facts.value

    override suspend fun restoreFacts(facts: List<FactEntity>) {
        this.facts.value = facts
    }

    override fun events(): Flow<List<EventEntity>> = events

    override fun searchEvents(term: String): Flow<List<EventEntity>> =
        events.map { list -> list.filter { it.title.contains(term, ignoreCase = true) } }

    override fun event(id: String): Flow<EventEntity?> =
        events.map { list -> list.firstOrNull { it.id == id } }

    override fun attendeesOn(eventId: String): Flow<List<EventAttendeeEntity>> =
        eventAttendees.map { list -> list.filter { it.eventId == eventId } }

    override fun eventsOf(personId: String): Flow<List<EventEntity>> = eventAttendees.map { rows ->
        val attending = rows.filter { it.personId == personId }.map { it.eventId }.toSet()
        events.value.filter { it.id in attending }
    }

    override fun photosOn(eventId: String): Flow<List<EventPhotoEntity>> =
        eventPhotos.map { list -> list.filter { it.eventId == eventId } }

    override suspend fun findEvent(id: String): EventEntity? = events.value.firstOrNull { it.id == id }

    override suspend fun saveEvent(event: EventEntity): EventEntity {
        events.value = events.value.filterNot { it.id == event.id } + event
        return event
    }

    override suspend fun deleteEvent(event: EventEntity) {
        events.value = events.value.filterNot { it.id == event.id }
        eventAttendees.value = eventAttendees.value.filterNot { it.eventId == event.id }
        eventPhotos.value = eventPhotos.value.filterNot { it.eventId == event.id }
    }

    override suspend fun attend(attendee: EventAttendeeEntity) {
        eventAttendees.value = eventAttendees.value.filterNot { it.id == attendee.id } + attendee
    }

    override suspend fun unattend(attendeeId: String) {
        eventAttendees.value = eventAttendees.value.filterNot { it.id == attendeeId }
    }

    override suspend fun addEventPhoto(photo: EventPhotoEntity) {
        eventPhotos.value = eventPhotos.value.filterNot { it.id == photo.id } + photo
    }

    override suspend fun removeEventPhoto(photoId: String) {
        eventPhotos.value = eventPhotos.value.filterNot { it.id == photoId }
    }

    override suspend fun eventSnapshot():
        Triple<List<EventEntity>, List<EventAttendeeEntity>, List<EventPhotoEntity>> =
        Triple(events.value, eventAttendees.value, eventPhotos.value)

    override suspend fun restoreEvents(
        events: List<EventEntity>,
        attendees: List<EventAttendeeEntity>,
        photos: List<EventPhotoEntity>,
    ) {
        this.events.value = events
        this.eventAttendees.value = attendees
        this.eventPhotos.value = photos
    }
}
