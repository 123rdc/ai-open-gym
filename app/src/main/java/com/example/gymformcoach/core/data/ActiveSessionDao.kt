package com.example.gymformcoach.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ActiveSession)

    @Query("SELECT * FROM active_sessions WHERE id = :id LIMIT 1")
    suspend fun getOnce(id: String = ActiveSession.SINGLETON_ID): ActiveSession?

    @Query("SELECT * FROM active_sessions WHERE id = :id LIMIT 1")
    fun observe(id: String = ActiveSession.SINGLETON_ID): Flow<ActiveSession?>

    @Query("DELETE FROM active_sessions")
    suspend fun clear()
}
