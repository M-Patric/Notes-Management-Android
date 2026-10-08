package com.example.notesmanagement.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query(
        """
        SELECT *
        FROM notes
        ORDER BY updated_at DESC, id DESC
        """
    )
    fun getAllNotes(): Flow<List<Note>>

    @Query(
        """
        SELECT *
        FROM notes
        WHERE title LIKE '%' || :searchQuery || '%'
           OR content LIKE '%' || :searchQuery || '%'
        ORDER BY updated_at DESC, id DESC
        """
    )
    fun searchNotes(searchQuery: String): Flow<List<Note>>
}