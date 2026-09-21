package io.github.hamzatadlaoui.socialgraph.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * "This person was there" - the same kind of tie [DocumentTagEntity] makes
 * between a person and a photo, just without a region: an event either
 * had someone present or it didn't, there is nowhere in it to point at.
 *
 * Deleting either end takes the row with it, the same way a tie or a tag
 * does.
 */
@Entity(
    tableName = "event_attendees",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("eventId"), Index("personId")],
)
data class EventAttendeeEntity(
    @PrimaryKey val id: String = newId(),
    val eventId: String,
    val personId: String,
) {
    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
