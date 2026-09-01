package io.github.hamzatadlaoui.socialgraph.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.hamzatadlaoui.socialgraph.model.FuzzyDate
import java.util.UUID

/**
 * A person, as the database keeps them.
 *
 * Only [displayName] is ever required. Everything else is filled in if and when
 * it is known, which is the whole of section 3.3: adding someone should take
 * seconds, and the app should never hold a name hostage to a birth date.
 */
@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: String = newId(),
    val displayName: String,
    val lastName: String = "",
    val nickname: String = "",
    /** File name inside the app's own photo folder, not a content URI. */
    val photo: String = "",
    val notes: String = "",
    val birth: FuzzyDate = FuzzyDate.Unknown,
    val death: FuzzyDate = FuzzyDate.Unknown,
    val pronouns: String = "",
    /** Marks the one person the app calls "you", for "how do I know them?". */
    val isMe: Boolean = false,
    val isFavourite: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    /** Name and nickname the way the list and the graph label a node. */
    val fullName: String get() = listOf(displayName, lastName).filter { it.isNotBlank() }.joinToString(" ")

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
