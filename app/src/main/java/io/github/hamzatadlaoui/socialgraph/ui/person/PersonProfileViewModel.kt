package io.github.hamzatadlaoui.socialgraph.ui.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hamzatadlaoui.socialgraph.data.DocumentEntity
import io.github.hamzatadlaoui.socialgraph.data.DocumentStore
import io.github.hamzatadlaoui.socialgraph.data.DocumentTagEntity
import io.github.hamzatadlaoui.socialgraph.data.EventEntity
import io.github.hamzatadlaoui.socialgraph.data.FactEntity
import io.github.hamzatadlaoui.socialgraph.data.PeopleRepository
import io.github.hamzatadlaoui.socialgraph.data.PhotoStore
import io.github.hamzatadlaoui.socialgraph.graph.Edge
import io.github.hamzatadlaoui.socialgraph.graph.Kinship
import io.github.hamzatadlaoui.socialgraph.graph.PeopleGraph
import io.github.hamzatadlaoui.socialgraph.graph.impliedKin
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.data.RelationshipEntity
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One tie, with the person at the other end of it already looked up. */
data class Tie(val relationship: RelationshipEntity, val other: PersonEntity)

/** A relation the app worked out, and the person it runs through. */
data class ImpliedTie(val other: PersonEntity, val kinship: Kinship, val throughName: String?)

/** A photo this person is tagged in, with the tag that marks their part of it. */
data class TaggedPhoto(val document: DocumentEntity, val tag: DocumentTagEntity)

class PersonProfileViewModel(
    private val repository: PeopleRepository,
    val personId: String,
    // Null only in tests: both need a real Context to even construct, which
    // a plain JVM test has no way to provide. Everything else on this view
    // model is exercised without them; only useTaggedPhotoAsProfile needs
    // them, and it is the one thing here that touches the filesystem rather
    // than the database, so it is the one thing this cannot cover.
    private val photos: PhotoStore? = null,
    private val files: DocumentStore? = null,
) : ViewModel() {

    val person: StateFlow<PersonEntity?> = repository.person(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * The ties nobody typed in but which follow from the ones that were - two
     * children of the same parent being siblings, and so on. Worked out from the
     * whole database rather than stored, so they can never go stale.
     */
    val implied: StateFlow<List<ImpliedTie>> =
        combine(repository.allRelationships(), repository.people()) { relationships, people ->
            val graph = PeopleGraph(
                relationships.map { Edge(it.pairId, it.fromId, it.toId, it.type) },
            )
            val byId = people.associateBy { it.id }
            impliedKin(graph, personId).mapNotNull { implied ->
                byId[implied.personId]?.let {
                    ImpliedTie(it, implied.kinship, byId[implied.throughId]?.displayName)
                }
            }.sortedWith(compareBy({ it.kinship.ordinal }, { it.other.fullName.lowercase() }))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every document this person has been tagged in - the other end of a tag. */
    val documents: StateFlow<List<DocumentEntity>> = repository.documentsOf(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Short categorised facts about this person - section 4.3. */
    val facts: StateFlow<List<FactEntity>> = repository.factsOf(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every event this person was marked present at - fed onto the profile, not typed onto it. */
    val events: StateFlow<List<EventEntity>> = repository.eventsOf(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Every image this person is tagged in, paired with the tag that marks
     * their part of it - what a "choose a photo from what they're tagged in"
     * picker offers. A document with no image behind it (a PDF, a recording)
     * has nothing to crop a face out of, so it never turns up here.
     */
    val taggedPhotos: StateFlow<List<TaggedPhoto>> =
        combine(repository.tagsOf(personId), repository.documentsOf(personId)) { tags, documents ->
            val byId = documents.associateBy { it.id }
            tags.mapNotNull { tag ->
                byId[tag.documentId]?.takeIf { it.isImage }?.let { TaggedPhoto(it, tag) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ties: StateFlow<List<Tie>> =
        combine(repository.relationshipsOf(personId), repository.people()) { relationships, people ->
            val byId = people.associateBy { it.id }
            relationships.mapNotNull { relationship ->
                byId[relationship.toId]?.let { Tie(relationship, it) }
            }.sortedBy { it.other.fullName.lowercase() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun unlink(tie: Tie) {
        viewModelScope.launch { repository.unlink(tie.relationship) }
    }

    /**
     * Turns a worked-out tie into a recorded one. A parent's partner, a
     * partner's child, and a sibling's parent are each one confirmation away
     * from being a real parent, not a label standing in for one; a sibling
     * worked out through a shared parent is one confirmation away from being
     * a real sibling tie in its own right. Everything else is recorded under
     * the name it was shown as, since the app has no built-in type for it.
     */
    fun confirmImplied(tie: ImpliedTie, label: String) {
        viewModelScope.launch {
            when (tie.kinship) {
                Kinship.PARENTS_PARTNER, Kinship.SIBLINGS_PARENT ->
                    repository.link(tie.other.id, personId, RelationshipType.PARENT_OF)
                Kinship.PARTNERS_CHILD ->
                    repository.link(personId, tie.other.id, RelationshipType.PARENT_OF)
                Kinship.SIBLING, Kinship.HALF_SIBLING ->
                    repository.link(personId, tie.other.id, RelationshipType.SIBLING_OF)
                else ->
                    repository.link(personId, tie.other.id, RelationshipType.CUSTOM, label)
            }
        }
    }

    /** Blank text is a no-op: a fact is a thing that was said, not a placeholder. */
    fun addFact(category: String, text: String) {
        val trimmedCategory = category.trim()
        val trimmedText = text.trim()
        if (trimmedCategory.isEmpty() || trimmedText.isEmpty()) return
        viewModelScope.launch {
            repository.saveFact(
                FactEntity(personId = personId, category = trimmedCategory, text = trimmedText),
            )
        }
    }

    fun deleteFact(id: String) {
        viewModelScope.launch { repository.deleteFact(id) }
    }

    /**
     * Crops the tagged region out of [taggedPhoto] and makes it this
     * person's photograph, the same crop the document shelf's own "use as
     * photo" action performs.
     */
    fun useTaggedPhotoAsProfile(taggedPhoto: TaggedPhoto) {
        val photoStore = photos ?: return
        val fileStore = files ?: return
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) {
                val source = fileStore.file(taggedPhoto.document.fileName).takeIf { it.isFile }
                    ?: return@withContext null
                photoStore.cropFrom(source, taggedPhoto.tag)
            } ?: return@launch
            val current = person.value ?: return@launch
            val previous = current.photo
            repository.save(current.copy(photo = name))
            if (previous.isNotEmpty()) withContext(Dispatchers.IO) { photoStore.delete(previous) }
        }
    }
}
