package com.example.gymformcoach.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SetAnalysisDao {
    @Insert
    suspend fun insert(analysis: SetAnalysis)

    @Query("SELECT * FROM set_analyses WHERE exerciseSessionId = :sessionId LIMIT 1")
    fun getForSession(sessionId: String): Flow<SetAnalysis?>

    @Query("SELECT * FROM set_analyses ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentOnce(limit: Int): List<SetAnalysis>
}
