package com.noto.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.noto.app.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room database operations on notes,
 * supporting soft-delete Recycle Bin and active note filtering.
 */
@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 AND isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashNotes(): Flow<List<Note>>

    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 1")
    fun getTrashCount(): Flow<Int>

    @Query(
        """
        SELECT * FROM notes 
        WHERE isDeleted = 0 AND (title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%') 
        ORDER BY isPinned DESC, updatedAt DESC
        """
    )
    fun searchNotes(query: String): Flow<List<Note>>

    @Query(
        """
        SELECT * FROM notes 
        WHERE isFavorite = 1 AND isDeleted = 0 AND (title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%') 
        ORDER BY isPinned DESC, updatedAt DESC
        """
    )
    fun searchFavoriteNotes(query: String): Flow<List<Note>>

    @Query(
        """
        SELECT * FROM notes 
        WHERE isDeleted = 1 AND (title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%') 
        ORDER BY deletedAt DESC
        """
    )
    fun searchTrashNotes(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: Long): Flow<Note?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdOnce(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun moveToTrash(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreNote(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun permanentlyDeleteNote(id: Long)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("UPDATE notes SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePinned(id: Long, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())
}
