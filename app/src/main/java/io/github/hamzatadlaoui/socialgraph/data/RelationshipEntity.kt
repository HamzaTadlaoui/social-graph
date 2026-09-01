package io.github.hamzatadlaoui.socialgraph.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.hamzatadlaoui.socialgraph.model.Certainty
import io.github.hamzatadlaoui.socialgraph.model.FuzzyDate
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import java.util.UUID

/**
 * One direction of one tie between two people.
 *
 * Both directions are always stored, as two rows sharing a [pairId] - see
 * [inverse]. It costs a row and buys a great deal: every question the app asks
 * ("who is next to this person?") is one flat `WHERE fromId = ?`, the graph
 * never has to reason about which way an edge was typed in, and removing a tie
 * is one delete by [pairId].
 */
@Entity(
    tableName = "relationships",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["toId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("fromId"), Index("toId"), Index("pairId")],
)
data class RelationshipEntity(
    @PrimaryKey val id: String = newId(),
    /** Shared by the two rows that are the same tie seen from either end. */
    val pairId: String = newId(),
    val fromId: String,
    val toId: String,
    val type: RelationshipType,
    /** Only meaningful for [RelationshipType.CUSTOM]. */
    val customLabel: String = "",
    val start: FuzzyDate = FuzzyDate.Unknown,
    val end: FuzzyDate = FuzzyDate.Unknown,
    val notes: String = "",
    val certainty: Certainty = Certainty.SURE,
) {
    /**
     * The same tie written from the other person's side: ends swapped, type
     * inverted, everything else carried over untouched. The caller supplies
     * [id] so that making a pair stays a pure, testable act.
     */
    fun inverse(id: String = newId()): RelationshipEntity = copy(
        id = id,
        pairId = pairId,
        fromId = toId,
        toId = fromId,
        type = type.inverse,
    )

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
