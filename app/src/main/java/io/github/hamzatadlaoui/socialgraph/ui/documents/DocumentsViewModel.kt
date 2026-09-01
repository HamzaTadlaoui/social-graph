package io.github.hamzatadlaoui.socialgraph.ui.documents

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hamzatadlaoui.socialgraph.data.DocumentEntity
import io.github.hamzatadlaoui.socialgraph.data.DocumentStore
import io.github.hamzatadlaoui.socialgraph.data.PeopleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentsViewModel(
    private val repository: PeopleRepository,
    private val files: DocumentStore,
) : ViewModel() {

    var query by mutableStateOf("")
        private set

    private val term = MutableStateFlow("")

    val documents: StateFlow<List<DocumentEntity>> = term
        .flatMapLatest { repository.searchDocuments(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        query = value
        term.value = value
    }

    /**
     * Copies a picked file into the app's own folder and files it. The copy is
     * done off the main thread; a scanned certificate can be several megabytes.
     */
    fun add(uri: Uri?, onDone: (DocumentEntity?) -> Unit = {}) {
        if (uri == null) return
        viewModelScope.launch {
            val picked = withContext(Dispatchers.IO) { files.save(uri) }
            if (picked == null) {
                onDone(null)
                return@launch
            }
            val saved = repository.saveDocument(
                DocumentEntity(
                    fileName = picked.fileName,
                    originalName = picked.originalName,
                    mimeType = picked.mimeType,
                    sizeBytes = picked.size,
                ),
            )
            onDone(saved)
        }
    }
}
