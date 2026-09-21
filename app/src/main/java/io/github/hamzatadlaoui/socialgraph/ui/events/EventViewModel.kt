package io.github.hamzatadlaoui.socialgraph.ui.events

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hamzatadlaoui.socialgraph.data.EventAttendeeEntity
import io.github.hamzatadlaoui.socialgraph.data.EventEntity
import io.github.hamzatadlaoui.socialgraph.data.EventPhotoEntity
import io.github.hamzatadlaoui.socialgraph.data.PeopleRepository
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.data.PhotoStore
import io.github.hamzatadlaoui.socialgraph.model.FuzzyDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One attendee row, with the person it points at already looked up. */
data class Attendee(val attendeeId: String, val person: PersonEntity)

class EventViewModel(
    private val repository: PeopleRepository,
    private val eventId: String,
    // Null only in tests, the same reason PersonProfileViewModel's are: a
    // real Context is needed to construct either and a plain JVM test has
    // no way to provide one.
    private val photos: PhotoStore? = null,
) : ViewModel() {

    // A local draft rather than reading straight from the [event] flow into
    // each text field, so a field being typed into is never reset mid-edit
    // by a recomposition of its own flow - the same reasoning
    // PersonEditViewModel's form already follows. Seeded once, written
    // through to the repository on every change rather than behind a Save
    // button, since there is nothing here to gate saving on.
    var title by mutableStateOf("")
        private set
    var dateText by mutableStateOf("")
        private set
    var location by mutableStateOf("")
        private set
    var description by mutableStateOf("")
        private set

    init {
        viewModelScope.launch {
            val loaded = repository.event(eventId).first() ?: return@launch
            title = loaded.title
            dateText = loaded.date.store()
            location = loaded.location
            description = loaded.description
        }
    }

    val event: StateFlow<EventEntity?> = repository.event(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val attendees: StateFlow<List<Attendee>> =
        combine(repository.attendeesOn(eventId), repository.people()) { rows, people ->
            val byId = people.associateBy { it.id }
            rows.mapNotNull { row -> byId[row.personId]?.let { Attendee(row.id, it) } }
                .sortedBy { it.person.fullName.lowercase() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Everyone not already marked present - who is left to add. */
    val candidates: StateFlow<List<PersonEntity>> =
        combine(repository.people(), attendees) { people, attending ->
            val attendingIds = attending.map { it.person.id }.toSet()
            people.filterNot { it.id in attendingIds }.sortedBy { it.fullName.lowercase() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val photosOnEvent: StateFlow<List<EventPhotoEntity>> = repository.photosOn(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun save(change: EventEntity.() -> EventEntity) {
        viewModelScope.launch {
            val current = event.value ?: repository.findEvent(eventId) ?: return@launch
            repository.saveEvent(current.change())
        }
    }

    fun onTitleChange(text: String) {
        title = text
        save { copy(title = text) }
    }

    fun onDateChange(text: String) {
        dateText = text
        save { copy(date = FuzzyDate.parse(text)) }
    }

    fun onLocationChange(text: String) {
        location = text
        save { copy(location = text) }
    }

    fun onDescriptionChange(text: String) {
        description = text
        save { copy(description = text) }
    }

    fun addAttendee(personId: String) {
        viewModelScope.launch {
            repository.attend(EventAttendeeEntity(eventId = eventId, personId = personId))
        }
    }

    /** The other half of adding someone new mid-event: create them and mark them present in one step. */
    fun createAndAddAttendee(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val created = repository.save(PersonEntity(displayName = trimmed))
            repository.attend(EventAttendeeEntity(eventId = eventId, personId = created.id))
        }
    }

    fun removeAttendee(attendeeId: String) {
        viewModelScope.launch { repository.unattend(attendeeId) }
    }

    /** Copies each picked photo in off the main thread; several at once can be a few megabytes. */
    fun addPhotos(uris: List<Uri>) {
        val store = photos ?: return
        viewModelScope.launch {
            for (uri in uris) {
                val name = withContext(Dispatchers.IO) { store.save(uri) } ?: continue
                repository.addEventPhoto(EventPhotoEntity(eventId = eventId, fileName = name))
            }
        }
    }

    fun removePhoto(photo: EventPhotoEntity) {
        viewModelScope.launch {
            repository.removeEventPhoto(photo.id)
            withContext(Dispatchers.IO) { photos?.delete(photo.fileName) }
        }
    }

    /** Takes its photo files with it - the database rows go on their own, through the foreign key. */
    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            val current = event.value ?: repository.findEvent(eventId) ?: return@launch
            val files = photosOnEvent.value
            repository.deleteEvent(current)
            withContext(Dispatchers.IO) { files.forEach { photos?.delete(it.fileName) } }
            onDone()
        }
    }
}
