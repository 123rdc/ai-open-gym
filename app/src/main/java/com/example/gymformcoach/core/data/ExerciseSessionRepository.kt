package com.example.gymformcoach.core.data

import kotlinx.coroutines.flow.Flow

class ExerciseSessionRepository(database: AppDatabase) {
    private val dao = database.exerciseSessionDao()

    suspend fun logSession(session: ExerciseSession) = dao.insert(session)

    fun getSessionsForExercise(exerciseId: String): Flow<List<ExerciseSession>> =
        dao.getSessionsForExercise(exerciseId)

    suspend fun getSessionsForExerciseOnce(exerciseId: String): List<ExerciseSession> =
        dao.getSessionsForExerciseOnce(exerciseId)

    fun getAllSessions(): Flow<List<ExerciseSession>> = dao.getAllSessions()
}
