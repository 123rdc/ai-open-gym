package com.example.gymformcoach.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SetLogDao {
    @Insert
    suspend fun insert(setLog: SetLog)

    @Insert
    suspend fun insertAll(setLogs: List<SetLog>)

    @Query("SELECT * FROM set_logs WHERE exerciseSessionId = :exerciseSessionId ORDER BY setIndex ASC")
    fun getForSession(exerciseSessionId: String): Flow<List<SetLog>>

    @Query("SELECT * FROM set_logs WHERE exerciseSessionId = :exerciseSessionId ORDER BY setIndex ASC")
    suspend fun getForSessionOnce(exerciseSessionId: String): List<SetLog>
}
