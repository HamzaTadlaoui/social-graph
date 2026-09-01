package io.github.hamzatadlaoui.socialgraph.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RelationshipDao {

    /**
     * Every tie one person has. Because both directions are stored, this one
     * query answers "who is next to them?" without a UNION.
     */
    @Query("SELECT * FROM relationships WHERE fromId = :personId")
    fun of(personId: String): Flow<List<RelationshipEntity>>

    @Query("SELECT * FROM relationships")
    fun all(): Flow<List<RelationshipEntity>>

    @Query("SELECT * FROM relationships")
    suspend fun snapshot(): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE id = :id")
    suspend fun find(id: String): RelationshipEntity?

    @Insert
    suspend fun insert(relationships: List<RelationshipEntity>)

    @Upsert
    suspend fun upsertAll(relationships: List<RelationshipEntity>)

    /** Removes a tie from both people at once. */
    @Query("DELETE FROM relationships WHERE pairId = :pairId")
    suspend fun deletePair(pairId: String)
}
