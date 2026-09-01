package io.github.hamzatadlaoui.socialgraph.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.hamzatadlaoui.socialgraph.model.FuzzyDate
import java.util.UUID

/**
 * A file the user has kept: a photograph, a scanned letter, a certificate, a
 * recording. The file itself lives in the app's own document folder; this row
 * is everything the app knows *about* it.
 *
 * The same reasoning as [PersonEntity.photo]: a picked file is copied in rather
 * than pointed at, because a content URI's permission can be withdrawn and a
 * document that vanishes from a dossier is worse than the copy costing space.
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String = newId(),
    /** File name inside the app's own document folder, not a content URI. */
    val fileName: String,
    /** What the file was called when it was picked, shown when there is no title. */
    val originalName: String = "",
    val mimeType: String = "",
    val title: String = "",
    val notes: String = "",
    /** When the document itself is from, as opposed to when it was filed. */
    val dated: FuzzyDate = FuzzyDate.Unknown,
    val sizeBytes: Long = 0L,
    val addedAt: Long = 0L,
) {
    /** What to call it in a list: the title if given, else the original name. */
    val label: String get() = title.ifBlank { originalName }.ifBlank { fileName }

    /** Only images can have a region of them tagged; everything else is tagged whole. */
    val isImage: Boolean get() = mimeType.startsWith("image/")

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
