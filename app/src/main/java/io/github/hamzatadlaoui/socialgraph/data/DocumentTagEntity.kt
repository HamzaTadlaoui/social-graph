package io.github.hamzatadlaoui.socialgraph.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.hamzatadlaoui.socialgraph.model.CropBox
import java.util.UUID

/**
 * "This person is in this document" - and, for an image, whereabouts in it.
 *
 * The region is stored as fractions of the image's width and height rather than
 * pixels, so a tag survives the picture being re-encoded at another size, and
 * means the same thing on any screen. A tag with no region ([whole]) covers the
 * document as a whole, which is the only kind a PDF or a recording can have.
 *
 * Deleting either end takes the tag with it, the same way a tie goes when a
 * person does.
 */
@Entity(
    tableName = "document_tags",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("documentId"), Index("personId")],
)
data class DocumentTagEntity(
    @PrimaryKey val id: String = newId(),
    val documentId: String,
    val personId: String,
    /** Fractions of the image, 0..1, left/top/right/bottom. All zero means the whole document. */
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val note: String = "",
) {
    /** True when this tag is about the document rather than a patch of it. */
    val whole: Boolean get() = right <= left || bottom <= top

    companion object {
        fun newId(): String = UUID.randomUUID().toString()

        /**
         * A region built from two corners in any order, clamped to the image and
         * refused if it came out too small to have been meant - a stray tap on
         * the picture should not silently become a tag of one pixel.
         */
        fun region(
            documentId: String,
            personId: String,
            x1: Float,
            y1: Float,
            x2: Float,
            y2: Float,
            id: String = newId(),
        ): DocumentTagEntity? {
            val left = minOf(x1, x2).coerceIn(0f, 1f)
            val right = maxOf(x1, x2).coerceIn(0f, 1f)
            val top = minOf(y1, y2).coerceIn(0f, 1f)
            val bottom = maxOf(y1, y2).coerceIn(0f, 1f)
            if (right - left < MINIMUM || bottom - top < MINIMUM) return null
            return DocumentTagEntity(
                id = id,
                documentId = documentId,
                personId = personId,
                left = left,
                top = top,
                right = right,
                bottom = bottom,
            )
        }

        /** Two per cent of the picture each way: smaller than that was a slip. */
        const val MINIMUM = CropBox.MINIMUM
    }
}
