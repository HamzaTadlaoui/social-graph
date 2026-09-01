package io.github.hamzatadlaoui.socialgraph.ui.documents

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hamzatadlaoui.socialgraph.data.DocumentEntity
import io.github.hamzatadlaoui.socialgraph.data.DocumentStore
import io.github.hamzatadlaoui.socialgraph.data.DocumentTagEntity
import io.github.hamzatadlaoui.socialgraph.data.PeopleRepository
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.data.PhotoStore
import io.github.hamzatadlaoui.socialgraph.model.Corner
import io.github.hamzatadlaoui.socialgraph.model.CropBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** One tag, with the person it points at already looked up. */
data class TaggedPerson(val tag: DocumentTagEntity, val person: PersonEntity)

data class DocumentState(
    val document: DocumentEntity? = null,
    val tagged: List<TaggedPerson> = emptyList(),
    val people: List<PersonEntity> = emptyList(),
    val loading: Boolean = true,
)

class DocumentViewModel(
    private val repository: PeopleRepository,
    private val files: DocumentStore,
    private val photos: PhotoStore,
    private val documentId: String,
) : ViewModel() {

    val state: StateFlow<DocumentState> = combine(
        repository.document(documentId),
        repository.tagsOn(documentId),
        repository.people(),
    ) { document, tags, people ->
        val byId = people.associateBy { it.id }
        DocumentState(
            document = document,
            // A tag whose person has since been deleted simply does not appear;
            // the foreign key will have taken it already, but a restore could
            // still hand us one.
            tagged = tags.mapNotNull { tag -> byId[tag.personId]?.let { TaggedPerson(tag, it) } },
            people = people,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DocumentState())

    /**
     * The box currently being worked on: freshly drawn, or an existing tag
     * opened for adjustment. Holding it here rather than in the canvas means a
     * half-adjusted box survives a recomposition, and that "save" is one place.
     */
    private val _draft = MutableStateFlow<Draft?>(null)
    val draft: StateFlow<Draft?> = _draft

    /**
     * A rectangle on its way to being a tag. [tagId] is null until it has been
     * given a name; after that, edits go back to the same row rather than
     * piling up new ones.
     */
    data class Draft(
        val tagId: String? = null,
        val personId: String? = null,
        val box: CropBox = CropBox.EMPTY,
    ) {
        val valid: Boolean get() = box.usable
    }

    /** Starts a new box, drawn from nothing. */
    fun beginRegion(left: Float, top: Float, right: Float, bottom: Float) {
        _draft.value = Draft(box = CropBox(left, top, right, bottom).tidied())
    }

    /** Opens an existing tag for adjustment. */
    fun select(tagged: TaggedPerson) {
        val tag = tagged.tag
        _draft.value = Draft(
            tagId = tag.id,
            personId = tag.personId,
            box = CropBox(tag.left, tag.top, tag.right, tag.bottom),
        )
    }

    /**
     * Where the box is now, as the finger moves it. The minimum size is not
     * enforced here - snapping a box back mid-drag fights whoever is dragging
     * it - only when it is saved.
     */
    fun drawTo(left: Float, top: Float, right: Float, bottom: Float) {
        val current = _draft.value ?: return
        _draft.value = current.copy(box = CropBox(left, top, right, bottom).tidied())
    }

    /** Slides the whole box, stopping at the edges of the picture. */
    fun moveBy(dx: Float, dy: Float) {
        val current = _draft.value ?: return
        _draft.value = current.copy(box = current.box.movedBy(dx, dy))
    }

    /** Drags one corner, leaving the opposite one where it is. */
    fun dragCorner(corner: Corner, x: Float, y: Float) {
        val current = _draft.value ?: return
        _draft.value = current.copy(box = current.box.withCorner(corner, x, y))
    }

    fun clearDraft() {
        _draft.value = null
    }

    /**
     * Writes the adjusted rectangle back. Called when a drag finishes, so an
     * existing tag follows the finger without anyone having to press save.
     */
    fun saveDraft() {
        val current = _draft.value ?: return
        val personId = current.personId ?: return
        if (!current.valid) return

        val tag = DocumentTagEntity.region(
            documentId = documentId,
            personId = personId,
            x1 = current.box.left,
            y1 = current.box.top,
            x2 = current.box.right,
            y2 = current.box.bottom,
            id = current.tagId ?: DocumentTagEntity.newId(),
        ) ?: return

        _draft.value = current.copy(tagId = tag.id)
        viewModelScope.launch { repository.tag(tag) }
    }

    /**
     * Puts a name to the box. A box with no size behind it - the "tag someone
     * in this file" button - becomes a tag about the document as a whole.
     */
    fun assign(personId: String) {
        val current = _draft.value ?: Draft()
        if (!current.valid) {
            viewModelScope.launch {
                repository.tag(
                    DocumentTagEntity(
                        id = current.tagId ?: DocumentTagEntity.newId(),
                        documentId = documentId,
                        personId = personId,
                    ),
                )
            }
            _draft.value = null
            return
        }
        _draft.value = current.copy(personId = personId)
        saveDraft()
    }

    /** Asks for a name with no rectangle: the tag will be about the whole document. */
    fun tagWhole() {
        _draft.value = Draft()
    }

    /** Removes whatever the draft points at, drawn or already saved. */
    fun removeDraft() {
        val tagId = _draft.value?.tagId
        _draft.value = null
        if (tagId != null) viewModelScope.launch { repository.untag(tagId) }
    }

    fun untag(tagId: String) {
        viewModelScope.launch { repository.untag(tagId) }
    }

    fun updateDetails(title: String, notes: String) {
        val current = state.value.document ?: return
        viewModelScope.launch {
            repository.saveDocument(current.copy(title = title, notes = notes))
        }
    }

    /**
     * Cuts the tagged region out of the picture and makes it that person's
     * photograph. This is the one moment a crop is written to disk: the tag
     * itself stays a rectangle, so re-tagging never leaves a stale copy behind.
     */
    fun useAsPhoto(tagged: TaggedPerson, onDone: (Boolean) -> Unit = {}) {
        val document = state.value.document ?: return
        if (!document.isImage || tagged.tag.whole) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) {
                val source = files.file(document.fileName).takeIf { it.isFile }
                    ?: return@withContext null
                val full = runCatching { BitmapFactory.decodeFile(source.path) }.getOrNull()
                    ?: return@withContext null
                runCatching {
                    val tag = tagged.tag
                    val x = (tag.left * full.width).toInt().coerceIn(0, full.width - 1)
                    val y = (tag.top * full.height).toInt().coerceIn(0, full.height - 1)
                    val w = ((tag.right - tag.left) * full.width).toInt()
                        .coerceIn(1, full.width - x)
                    val h = ((tag.bottom - tag.top) * full.height).toInt()
                        .coerceIn(1, full.height - y)

                    val crop = Bitmap.createBitmap(full, x, y, w, h)
                    val fileName = "${UUID.randomUUID()}.jpg"
                    File(photos.file(fileName).also { it.parentFile?.mkdirs() }.path)
                        .outputStream()
                        .use { crop.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                    fileName
                }.getOrNull()
            }
            if (name == null) {
                onDone(false)
                return@launch
            }
            // The old photo goes only once the new one is safely written.
            val previous = tagged.person.photo
            repository.save(tagged.person.copy(photo = name))
            if (previous.isNotEmpty()) photos.delete(previous)
            onDone(true)
        }
    }

    /** Deletes the row and the file behind it; one without the other is litter. */
    fun delete(onDone: () -> Unit) {
        val current = state.value.document ?: return
        viewModelScope.launch {
            repository.deleteDocument(current)
            files.delete(current.fileName)
            onDone()
        }
    }
}
