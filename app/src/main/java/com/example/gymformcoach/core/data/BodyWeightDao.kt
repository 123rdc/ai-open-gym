package com.example.gymformcoach.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyWeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: BodyWeightEntry)

    @Query("SELECT * FROM body_weight_entries ORDER BY epochDay ASC")
    fun getAll(): Flow<List<BodyWeightEntry>>

    @Query("SELECT * FROM body_weight_entries WHERE epochDay >= :fromEpochDay ORDER BY epochDay ASC")
    fun getFrom(fromEpochDay: Long): Flow<List<BodyWeightEntry>>

    @Query("SELECT * FROM body_weight_entries ORDER BY epochDay DESC LIMIT 1")
    suspend fun getLatestOnce(): BodyWeightEntry?

    @Query("DELETE FROM body_weight_entries WHERE epochDay = :epochDay")
    suspend fun delete(epochDay: Long)
}
