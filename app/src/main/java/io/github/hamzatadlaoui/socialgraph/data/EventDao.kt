package io.github.hamzatadlaoui.socialgraph.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY addedAt DESC")
    fun all(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun byId(id: String): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun find(id: String): EventEntity?

    /** The list's search box: title, location and description all count. */
    @Query(
        """
        SELECT * FROM events
        WHERE title       LIKE '%' || :term || '%'
           OR location    LIKE '%' || :term || '%'
           OR description LIKE '%' || :term || '%'
        ORDER BY addedAt DESC
        """,
    )
    fun search(term: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events")
    suspend fun snapshot(): List<EventEntity>

    @Upsert
    suspend fun upsert(event: EventEntity)

    @Upsert
    suspend fun upsertAll(events: List<EventEntity>)

    @Delete
    suspend fun delete(event: EventEntity)

    @Query("SELECT * FROM event_attendees WHERE eventId = :eventId")
    fun attendeesOn(eventId: String): Flow<List<EventAttendeeEntity>>

    /** Every event this person was marked present at, newest first. */
    @Query(
        """
        SELECT e.* FROM events e
        JOIN event_attendees a ON a.eventId = e.id
        WHERE a.personId = :personId
        GROUP BY e.id
        ORDER BY e.addedAt DESC
        """,
    )
    fun eventsOf(personId: String): Flow<List<EventEntity>>

    @Upsert
    suspend fun upsertAttendee(attendee: EventAttendeeEntity)

    @Upsert
    suspend fun upsertAttendees(attendees: List<EventAttendeeEntity>)

    @Query("DELETE FROM event_attendees WHERE id = :id")
    suspend fun deleteAttendee(id: String)

    @Query("SELECT * FROM event_attendees")
    suspend fun attendeeSnapshot(): List<EventAttendeeEntity>

    @Query("SELECT * FROM event_photos WHERE eventId = :eventId ORDER BY addedAt")
    fun photosOn(eventId: String): Flow<List<EventPhotoEntity>>

    @Upsert
    suspend fun upsertPhoto(photo: EventPhotoEntity)

    @Upsert
    suspend fun upsertPhotos(photos: List<EventPhotoEntity>)

    @Query("DELETE FROM event_photos WHERE id = :id")
    suspend fun deletePhoto(id: String)

    @Query("SELECT * FROM event_photos")
    suspend fun photoSnapshot(): List<EventPhotoEntity>
}
