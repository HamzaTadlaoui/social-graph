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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Which of the situations worth double-checking a [FollowUpPrompt] is about. */
enum class FollowUpKind { CHILD_OF_PARTNER, PARTNER_PARENT_OF, PARENT_OF_SIBLING }

/**
 * A question worth asking right after a tie is recorded, because the tie
 * just added makes another one likely. Answering yes records that one too;
 * answering no simply moves on - nothing is inferred without being asked.
 */
data class FollowUpPrompt(
    val kind: FollowUpKind,
    val candidateName: String,
    val newPersonName: String,
    /** Recorded as `repository.link(fromId, toId, PARENT_OF)` if confirmed. */
    val fromId: String,
    val toId: String,
)

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

    private val _pendingPrompts = MutableStateFlow<List<FollowUpPrompt>>(emptyList())
    val pendingPrompts: StateFlow<List<FollowUpPrompt>> = _pendingPrompts.asStateFlow()

    private var onAllResolved: (() -> Unit)? = null

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
            finishLinking(otherId, onLinked)
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
            finishLinking(created.id, onLinked)
        }
    }

    private suspend fun finishLinking(newPersonId: String, onLinked: () -> Unit) {
        val prompts = gatherFollowUps(newPersonId)
        if (prompts.isEmpty()) {
            onLinked()
        } else {
            onAllResolved = onLinked
            _pendingPrompts.value = prompts
        }
    }

    /**
     * The tie just recorded makes one more worth asking about, in the three
     * cases where adding it usually leaves a gap: a new child whose other
     * parent might be the subject's partner, a new partner who might also
     * parent the subject's children, or a new sibling who shares the
     * subject's recorded parent too.
     */
    private suspend fun gatherFollowUps(newPersonId: String): List<FollowUpPrompt> {
        val newPerson = repository.find(newPersonId) ?: return emptyList()
        val subjectTies = repository.relationshipsOf(personId).first()
        val newPersonTies = repository.relationshipsOf(newPersonId).first()

        fun candidates(subjectType: RelationshipType, alreadyType: RelationshipType) =
            subjectTies.filter { it.type == subjectType }
                .map { it.toId }
                .distinct()
                .filterNot { id -> newPersonTies.any { it.type == alreadyType && it.toId == id } }

        return when (type) {
            RelationshipType.PARENT_OF -> candidates(RelationshipType.PARTNER_OF, RelationshipType.CHILD_OF)
                .mapNotNull { partnerId ->
                    val partner = repository.find(partnerId) ?: return@mapNotNull null
                    FollowUpPrompt(
                        kind = FollowUpKind.CHILD_OF_PARTNER,
                        candidateName = partner.displayName,
                        newPersonName = newPerson.displayName,
                        fromId = partnerId,
                        toId = newPersonId,
                    )
                }
            RelationshipType.PARTNER_OF -> candidates(RelationshipType.PARENT_OF, RelationshipType.PARENT_OF)
                .mapNotNull { childId ->
                    val child = repository.find(childId) ?: return@mapNotNull null
                    FollowUpPrompt(
                        kind = FollowUpKind.PARTNER_PARENT_OF,
                        candidateName = child.displayName,
                        newPersonName = newPerson.displayName,
                        fromId = newPersonId,
                        toId = childId,
                    )
                }
            RelationshipType.SIBLING_OF -> candidates(RelationshipType.CHILD_OF, RelationshipType.CHILD_OF)
                .mapNotNull { parentId ->
                    val parent = repository.find(parentId) ?: return@mapNotNull null
                    FollowUpPrompt(
                        kind = FollowUpKind.PARENT_OF_SIBLING,
                        candidateName = parent.displayName,
                        newPersonName = newPerson.displayName,
                        fromId = parentId,
                        toId = newPersonId,
                    )
                }
            else -> emptyList()
        }
    }

    fun confirmPrompt(prompt: FollowUpPrompt) {
        viewModelScope.launch {
            repository.link(prompt.fromId, prompt.toId, RelationshipType.PARENT_OF)
            advancePrompt()
        }
    }

    fun declinePrompt(prompt: FollowUpPrompt) {
        advancePrompt()
    }

    private fun advancePrompt() {
        val remaining = _pendingPrompts.value.drop(1)
        _pendingPrompts.value = remaining
        if (remaining.isEmpty()) {
            onAllResolved?.invoke()
            onAllResolved = null
        }
    }
}
