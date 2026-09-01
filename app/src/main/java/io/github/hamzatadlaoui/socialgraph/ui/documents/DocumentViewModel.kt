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

    /** The tag the user has just drawn but not yet given a name to. */
    private val pending = MutableStateFlow<FloatArray?>(null)
    val pendingRegion: StateFlow<FloatArray?> = pending

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

    /** Remembers the rectangle just drawn, so the picker knows where to put the name. */
    fun regionDrawn(left: Float, top: Float, right: Float, bottom: Float) {
        pending.value = floatArrayOf(left, top, right, bottom)
    }

    fun cancelRegion() {
        pending.value = null
    }

    /**
     * Asks for a name with no rectangle attached - the tag will be about the
     * document as a whole. An empty array rather than null, because null is
     * what "nothing is pending" already means.
     */
    fun tagWhole() {
        pending.value = FloatArray(0)
    }

    /**
     * Attaches [personId] to the pending rectangle, or to the document as a
     * whole when nothing has been drawn - which is the only kind of tag a PDF
     * or a recording can have.
     */
    fun tagPending(personId: String) {
        val region = pending.value
        viewModelScope.launch {
            val tag = if (region == null || region.size < 4) {
                DocumentTagEntity(documentId = documentId, personId = personId)
            } else {
                DocumentTagEntity.region(
                    documentId = documentId,
                    personId = personId,
                    x1 = region[0],
                    y1 = region[1],
                    x2 = region[2],
                    y2 = region[3],
                ) ?: return@launch
            }
            repository.tag(tag)
            pending.value = null
        }
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
