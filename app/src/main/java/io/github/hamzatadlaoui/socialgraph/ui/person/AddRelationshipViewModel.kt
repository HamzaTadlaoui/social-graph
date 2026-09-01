package io.github.hamzatadlaoui.socialgraph.ui.person

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hamzatadlaoui.socialgraph.data.PeopleRepository
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AddRelationshipViewModel(
    private val repository: PeopleRepository,
    private val personId: String,
) : ViewModel() {

    var type by mutableStateOf(RelationshipType.FRIEND_OF)
        private set

    var customLabel by mutableStateOf("")
        private set

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Everyone but the person whose page we came from: nobody is their own cousin. */
    val matches: StateFlow<List<PersonEntity>> = _query
        .flatMapLatest { repository.search(it) }
        .map { people -> people.filterNot { it.id == personId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onTypeChange(type: RelationshipType) {
        this.type = type
    }

    fun onCustomLabelChange(label: String) {
        customLabel = label
    }

    fun onQueryChange(text: String) {
        _query.value = text
    }

    fun linkTo(otherId: String, onLinked: () -> Unit) {
        viewModelScope.launch {
            repository.link(personId, otherId, type, customLabel.trim())
            onLinked()
        }
    }

    /**
     * The other half of "adding a family should take one pass": if the person
     * is not in the database yet, they are created from the search box and tied
     * on in the same step.
     */
    fun createAndLink(name: String, onLinked: () -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val created = repository.save(PersonEntity(displayName = trimmed))
            repository.link(personId, created.id, type, customLabel.trim())
            onLinked()
        }
    }
}
