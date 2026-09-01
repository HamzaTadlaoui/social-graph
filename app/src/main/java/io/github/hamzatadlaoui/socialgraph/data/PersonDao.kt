package io.github.hamzatadlaoui.socialgraph.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Query("SELECT * FROM people ORDER BY displayName COLLATE NOCASE")
    fun all(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE id = :id")
    fun byId(id: String): Flow<PersonEntity?>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun find(id: String): PersonEntity?

    /** The list's search box: name, surname and nickname all count (section 8). */
    @Query(
        """
        SELECT * FROM people
        WHERE displayName LIKE '%' || :term || '%'
           OR lastName    LIKE '%' || :term || '%'
           OR nickname    LIKE '%' || :term || '%'
        ORDER BY displayName COLLATE NOCASE
        """,
    )
    fun search(term: String): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people ORDER BY createdAt DESC LIMIT :count")
    fun recentlyAdded(count: Int): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE isMe = 1 LIMIT 1")
    fun me(): Flow<PersonEntity?>

    @Query("SELECT * FROM people")
    suspend fun snapshot(): List<PersonEntity>

    @Query("UPDATE people SET isMe = 0 WHERE id != :keep")
    suspend fun clearMe(keep: String)

    @Upsert
    suspend fun upsert(person: PersonEntity)

    @Upsert
    suspend fun upsertAll(people: List<PersonEntity>)

    @Delete
    suspend fun delete(person: PersonEntity)
}
