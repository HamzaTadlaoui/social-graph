package io.github.hamzatadlaoui.socialgraph.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * One short, categorised line about a person - "favourite food: banana nut
 * muffins", not a paragraph. [category] is free text: a handful of built-in
 * ones always show up on the profile (see `ui/person/FactCategories.kt`), and
 * anything else the person types becomes a category of its own the moment a
 * fact is saved under it. There is no separate table for categories - one
 * would only ever hold a name, and the distinct [category] values already in
 * use are that list.
 *
 * Deleting the person takes their facts with them, the same way a tie or a
 * document tag does.
 */
@Entity(
    tableName = "facts",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("personId")],
)
data class FactEntity(
    @PrimaryKey val id: String = newId(),
    val personId: String,
    val category: String,
    val text: String,
    val createdAt: Long = 0L,
) {
    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
