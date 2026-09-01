package io.github.hamzatadlaoui.socialgraph.ui.settings

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hamzatadlaoui.socialgraph.R
import io.github.hamzatadlaoui.socialgraph.data.PeopleRepository
import io.github.hamzatadlaoui.socialgraph.data.PhotoStore
import io.github.hamzatadlaoui.socialgraph.export.Backup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val repository: PeopleRepository,
    private val photos: PhotoStore,
) : ViewModel() {

    /** What just happened, shown once and then cleared. */
    var message by mutableStateOf<Message?>(null)
        private set

    data class Message(@param:StringRes val text: Int, val count: Int = 0)

    fun suggestedFileName(): String = Backup.fileName(System.currentTimeMillis())

    fun exportTo(context: Context, target: Uri?) {
        if (target == null) return
        viewModelScope.launch {
            val (people, relationships) = repository.snapshot()
            val wrote = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(target)?.use { out ->
                        Backup.write(out, people, relationships, photos)
                    } ?: return@runCatching false
                    true
                }.getOrDefault(false)
            }
            message = if (wrote) {
                Message(R.string.backup_written, people.size)
            } else {
                Message(R.string.backup_failed)
            }
        }
    }

    fun restoreFrom(context: Context, source: Uri?) {
        if (source == null) return
        viewModelScope.launch {
            val restored = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(source)?.use { input ->
                        Backup.read(input, photos)
                    }
                }.getOrNull()
            }
            if (restored == null || restored.people.isEmpty()) {
                message = Message(R.string.restore_failed)
                return@launch
            }
            repository.restore(restored.people, restored.relationships)
            message = Message(R.string.restore_done, restored.people.size)
        }
    }

    fun messageShown() {
        message = null
    }
}
