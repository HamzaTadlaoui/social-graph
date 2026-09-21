package io.github.hamzatadlaoui.socialgraph.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FactDao {

    @Query("SELECT * FROM facts WHERE personId = :personId ORDER BY category, createdAt")
    fun factsOf(personId: String): Flow<List<FactEntity>>

    @Query("SELECT * FROM facts")
    suspend fun snapshot(): List<FactEntity>

    @Upsert
    suspend fun upsert(fact: FactEntity)

    @Upsert
    suspend fun upsertAll(facts: List<FactEntity>)

    @Query("DELETE FROM facts WHERE id = :id")
    suspend fun delete(id: String)
}
