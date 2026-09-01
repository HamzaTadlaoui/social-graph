package io.github.hamzatadlaoui.socialgraph.ui.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hamzatadlaoui.socialgraph.data.DocumentEntity
import io.github.hamzatadlaoui.socialgraph.data.PeopleRepository
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.data.RelationshipEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One tie, with the person at the other end of it already looked up. */
data class Tie(val relationship: RelationshipEntity, val other: PersonEntity)

class PersonProfileViewModel(
    private val repository: PeopleRepository,
    val personId: String,
) : ViewModel() {

    val person: StateFlow<PersonEntity?> = repository.person(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
