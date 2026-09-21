package io.github.hamzatadlaoui.socialgraph.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.hamzatadlaoui.socialgraph.model.FuzzyDate
import java.util.UUID

/**
 * A real occasion - a wedding, a trip, a birthday - rather than a line of
 * free text about someone. Where a fact is typed straight onto a person's
 * profile, an event is its own thing with its own place in the app (the
 * bottom-nav Events tab) and is only ever *fed onto* a profile, the same
 * way a tie is: see [EventAttendeeEntity] and
 * [io.github.hamzatadlaoui.socialgraph.ui.person.ProfileTab.Events].
 */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String = newId(),
    val title: String = "",
    val date: FuzzyDate = FuzzyDate.Unknown,
    val location: String = "",
    val description: String = "",
    val addedAt: Long = 0L,
) {
    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
