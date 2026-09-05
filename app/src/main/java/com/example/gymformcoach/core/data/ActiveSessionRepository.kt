package com.example.gymformcoach.core.data

import kotlinx.coroutines.flow.Flow

class ActiveSessionRepository(database: AppDatabase) {
    private val dao = database.activeSessionDao()

    fun observe(): Flow<ActiveSession?> = dao.observe()

    suspend fun start(routineId: String) {
        dao.upsert(
            ActiveSession(
                routineId = routineId,
                startedAt = System.currentTimeMillis(),
                currentExerciseIndex = 0
            )
        )
    }

    /** §3.3: called on every set completion, so process death loses nothing. */
    suspend fun recordSetCompleted(exerciseIndex: Int, setId: String) {
        val current = dao.getOnce() ?: return
        dao.upsert(
            current.copy(
                currentExerciseIndex = exerciseIndex,
                completedSetIds = current.completedSetIds + setId,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * The session to offer for resume, or null. A session older than
     * [ActiveSession.RESUMABLE_WINDOW_MS] is stale — clear it rather than
     * prompting to resume yesterday's abandoned workout.
     */
    suspend fun getResumable(): ActiveSession? {
        val session = dao.getOnce() ?: return null
        val age = System.currentTimeMillis() - session.startedAt
        if (age > ActiveSession.RESUMABLE_WINDOW_MS) {
            dao.clear()
            return null
        }
        return session
    }

    suspend fun finish() = dao.clear()
}
