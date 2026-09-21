package io.github.hamzatadlaoui.socialgraph.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * One photo filed under an event, copied in through [PhotoStore.save] the
 * same way a person's own photo is - it lives in the same `files/photos`
 * folder, there is simply more than one of them per event.
 */
@Entity(
    tableName = "event_photos",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("eventId")],
)
data class EventPhotoEntity(
    @PrimaryKey val id: String = newId(),
    val eventId: String,
    val fileName: String,
    val addedAt: Long = 0L,
) {
    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
