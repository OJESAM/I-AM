package com.example.kairoslivingstewards.data.local.dao

import androidx.room.*
import com.example.kairoslivingstewards.data.local.entities.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE targetId = :targetId ORDER BY timestamp DESC")
    fun getNotesForTarget(targetId: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)
}
