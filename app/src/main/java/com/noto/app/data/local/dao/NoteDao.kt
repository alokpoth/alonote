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
 * Data Access Object for Room database operations on notes.
 */
@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<Note>>

    @Query(
        """
        SELECT * FROM notes 
        WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' 
        ORDER BY updatedAt DESC
        """
    )
    fun searchNotes(query: String): Flow<List<Note>>

    @Query(
        """
        SELECT * FROM notes 
        WHERE isFavorite = 1 AND (title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%') 
        ORDER BY updatedAt DESC
        """
    )
    fun searchFavoriteNotes(query: String): Flow<List<Note>>

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

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("UPDATE notes SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())
}
