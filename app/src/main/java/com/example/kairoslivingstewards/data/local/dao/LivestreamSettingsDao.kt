package com.example.kairoslivingstewards.data.local.dao

import androidx.room.*
import com.example.kairoslivingstewards.data.local.entities.LivestreamSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LivestreamSettingsDao {
    @Query("SELECT * FROM livestream_settings WHERE id = 1")
    fun getSettings(): Flow<LivestreamSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: LivestreamSettingsEntity)

    @Update
    suspend fun updateSettings(settings: LivestreamSettingsEntity)
}
