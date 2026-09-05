package com.example.gymformcoach.core.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class RoutineRepository(private val database: AppDatabase) {
    private val dao = database.routineDao()

    suspend fun saveRoutine(
        name: String,
        description: String,
        exercises: List<RoutineExercise>,
        progressionRule: String = "NONE"
    ): String {
        return database.withTransaction {
            val routine = Routine(name = name, description = description, progressionRule = progressionRule)
            dao.insertRoutine(routine)
            dao.insertRoutineExercises(exercises.map { it.copy(routineId = routine.id) })
            routine.id
        }
    }

    suspend fun duplicateRoutine(routineId: String) {
        database.withTransaction {
            val original = dao.getRoutineOnce(routineId)
            if (original != null) {
                val originalExercises = dao.getRoutineExercisesOnce(routineId)
                val newRoutine = original.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    name = "${original.name} (Copy)",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.SYNCED,
                    serverId = null
                )
                dao.insertRoutine(newRoutine)
                dao.insertRoutineExercises(
                    originalExercises.map {
                        it.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            routineId = newRoutine.id,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            syncStatus = SyncStatus.SYNCED,
                            serverId = null
                        )
                    }
                )
            }
        }
    }

    suspend fun deleteRoutine(routineId: String) {
        dao.deleteRoutine(routineId)
    }

    fun getRoutineSummaries(): Flow<List<RoutineSummary>> = dao.getRoutineSummaries()

    fun getRoutine(routineId: String): Flow<Routine?> = dao.getRoutine(routineId)

    fun getRoutineExercises(routineId: String): Flow<List<RoutineExercise>> = dao.getRoutineExercises(routineId)
}
