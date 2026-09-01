package io.github.hamzatadlaoui.socialgraph.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY addedAt DESC")
    fun all(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun byId(id: String): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun find(id: String): DocumentEntity?

    /** The list's search box: title, original file name and notes all count. */
    @Query(
        """
        SELECT * FROM documents
        WHERE title        LIKE '%' || :term || '%'
           OR originalName LIKE '%' || :term || '%'
           OR notes        LIKE '%' || :term || '%'
        ORDER BY addedAt DESC
        """,
    )
    fun search(term: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents")
    suspend fun snapshot(): List<DocumentEntity>

    @Query("SELECT * FROM document_tags WHERE documentId = :documentId")
    fun tagsOn(documentId: String): Flow<List<DocumentTagEntity>>

    /** Everything this person has been tagged in, newest document first. */
    @Query(
        """
        SELECT d.* FROM documents d
        JOIN document_tags t ON t.documentId = d.id
        WHERE t.personId = :personId
        GROUP BY d.id
        ORDER BY d.addedAt DESC
        """,
    )
    fun documentsOf(personId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM document_tags WHERE personId = :personId")
    fun tagsOf(personId: String): Flow<List<DocumentTagEntity>>

    @Query("SELECT * FROM document_tags")
    suspend fun tagSnapshot(): List<DocumentTagEntity>

    @Upsert
    suspend fun upsert(document: DocumentEntity)

    @Upsert
    suspend fun upsertAll(documents: List<DocumentEntity>)

    @Upsert
    suspend fun upsertTag(tag: DocumentTagEntity)

    @Upsert
    suspend fun upsertTags(tags: List<DocumentTagEntity>)

    @Delete
    suspend fun delete(document: DocumentEntity)

    @Query("DELETE FROM document_tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: String)
}
