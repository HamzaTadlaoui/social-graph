package io.github.hamzatadlaoui.socialgraph.ui.people

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hamzatadlaoui.socialgraph.data.PeopleRepository
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.data.PhotoStore
import io.github.hamzatadlaoui.socialgraph.model.FuzzyDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The state of the one form in the app. [born] is held as the text the user
 * typed and only turned into a [FuzzyDate] on the way to the database, so that
 * "1974" is as easy to enter as a full date - section 7.
 */
data class PersonForm(
    val displayName: String = "",
    val lastName: String = "",
    val nickname: String = "",
    val pronouns: String = "",
    val born: String = "",
    val notes: String = "",
    val photo: String = "",
    val isMe: Boolean = false,
) {
    val canSave: Boolean get() = displayName.isNotBlank()
}

class PersonEditViewModel(
    private val repository: PeopleRepository,
    private val photos: PhotoStore,
    private val personId: String?,
) : ViewModel() {

    var form by mutableStateOf(PersonForm())
        private set

    /** Null until an existing person has been read; new people never load one. */
    private var existing: PersonEntity? = null

    val isNew: Boolean get() = personId == null

    init {
        if (personId != null) {
            viewModelScope.launch {
                repository.find(personId)?.let { person ->
                    existing = person
                    form = PersonForm(
                        displayName = person.displayName,
                        lastName = person.lastName,
                        nickname = person.nickname,
                        pronouns = person.pronouns,
                        born = person.birth.store(),
                        notes = person.notes,
                        photo = person.photo,
                        isMe = person.isMe,
                    )
                }
            }
        }
    }

    fun update(change: (PersonForm) -> PersonForm) {
        form = change(form)
    }

    fun onPhotoPicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) { photos.save(uri) } ?: return@launch
            val previous = form.photo
            form = form.copy(photo = name)
            // The old file is only dropped once the new one is safely copied in.
            if (previous.isNotEmpty()) withContext(Dispatchers.IO) { photos.delete(previous) }
        }
    }

    fun save(onSaved: (String) -> Unit) {
        if (!form.canSave) return
        viewModelScope.launch {
            // Only one person can be "you", so marking someone unmarks whoever
            // held it before.
            if (form.isMe) {
                repository.clearMe(existing?.id)
            }
            val person = (existing ?: PersonEntity(displayName = "")).copy(
                displayName = form.displayName.trim(),
                lastName = form.lastName.trim(),
                nickname = form.nickname.trim(),
                pronouns = form.pronouns.trim(),
                birth = FuzzyDate.parse(form.born),
                notes = form.notes,
                photo = form.photo,
                isMe = form.isMe,
            )
            onSaved(repository.save(person).id)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val person = existing ?: return onDeleted()
        viewModelScope.launch {
            repository.delete(person)
            withContext(Dispatchers.IO) { photos.delete(person.photo) }
            onDeleted()
        }
    }
}
