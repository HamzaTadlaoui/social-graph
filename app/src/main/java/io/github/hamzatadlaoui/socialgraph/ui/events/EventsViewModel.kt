package io.github.hamzatadlaoui.socialgraph.ui.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hamzatadlaoui.socialgraph.data.EventAttendeeEntity
import io.github.hamzatadlaoui.socialgraph.data.EventEntity
import io.github.hamzatadlaoui.socialgraph.data.PeopleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModel(private val repository: PeopleRepository) : ViewModel() {

    var query by mutableStateOf("")
        private set

    private val term = MutableStateFlow("")

    val events: StateFlow<List<EventEntity>> = term
        .flatMapLatest { repository.searchEvents(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        query = value
        term.value = value
    }

    /**
     * Created blank and opened straight into its own screen - an event has
     * nothing that has to be picked first the way a document's file does, so
     * there is nothing to gate creating it on. [attendeeId], when given,
     * marks that person present from the start - the profile's own "Add
     * event" button arrives this way.
     */
    fun createDraft(attendeeId: String? = null, onDone: (EventEntity) -> Unit) {
        viewModelScope.launch {
            val created = repository.saveEvent(EventEntity())
            if (attendeeId != null) {
                repository.attend(EventAttendeeEntity(eventId = created.id, personId = attendeeId))
            }
            onDone(created)
        }
    }
}
