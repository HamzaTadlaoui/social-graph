package io.github.hamzatadlaoui.socialgraph.ui.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hamzatadlaoui.socialgraph.data.DocumentEntity
import io.github.hamzatadlaoui.socialgraph.data.PeopleRepository
import io.github.hamzatadlaoui.socialgraph.graph.Edge
import io.github.hamzatadlaoui.socialgraph.graph.Kinship
import io.github.hamzatadlaoui.socialgraph.graph.PeopleGraph
import io.github.hamzatadlaoui.socialgraph.graph.impliedKin
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.data.RelationshipEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One tie, with the person at the other end of it already looked up. */
data class Tie(val relationship: RelationshipEntity, val other: PersonEntity)

/** A relation the app worked out, and the person it runs through. */
data class ImpliedTie(val other: PersonEntity, val kinship: Kinship, val throughName: String?)

class PersonProfileViewModel(
    private val repository: PeopleRepository,
    val personId: String,
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
}
