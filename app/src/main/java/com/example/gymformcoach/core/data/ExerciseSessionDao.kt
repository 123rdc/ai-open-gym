package com.example.gymformcoach.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseSessionDao {
    @Insert
    suspend fun insert(session: ExerciseSession)

    @Query("SELECT * FROM exercise_sessions WHERE exerciseId = :exerciseId ORDER BY performedAt ASC")
    fun getSessionsForExercise(exerciseId: String): Flow<List<ExerciseSession>>

    @Query("SELECT * FROM exercise_sessions WHERE exerciseId = :exerciseId")
    suspend fun getSessionsForExerciseOnce(exerciseId: String): List<ExerciseSession>

    @Query("SELECT * FROM exercise_sessions")
    fun getAllSessions(): Flow<List<ExerciseSession>>
}
