package com.example.gymformcoach.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Insert
    suspend fun insertRoutine(routine: Routine)

    @Insert
    suspend fun insertRoutineExercises(exercises: List<RoutineExercise>)

    /** §17D.3: revising an existing routine exercise in place (swap/adjust-volume diff items). */
    @Update
    suspend fun updateRoutineExercise(exercise: RoutineExercise)

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutine(routineId: String)

    @Query("SELECT * FROM routines WHERE id = :routineId")
    suspend fun getRoutineOnce(routineId: String): Routine?

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY orderIndex ASC")
    suspend fun getRoutineExercisesOnce(routineId: String): List<RoutineExercise>

    @Query("SELECT * FROM routines WHERE id = :routineId")
    fun getRoutine(routineId: String): Flow<Routine?>

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY orderIndex ASC")
    fun getRoutineExercises(routineId: String): Flow<List<RoutineExercise>>

    @Query(
        """
        SELECT r.id as id, r.name as name, r.description as description,
            COUNT(re.id) as exerciseCount,
            (
                SELECT MAX(es.performedAt) FROM exercise_sessions es
                INNER JOIN routine_exercises re2 ON re2.exerciseId = es.exerciseId
                WHERE re2.routineId = r.id
            ) as lastPerformedDate
        FROM routines r
        LEFT JOIN routine_exercises re ON re.routineId = r.id
        GROUP BY r.id
        ORDER BY r.createdAt DESC
        """
    )
    fun getRoutineSummaries(): Flow<List<RoutineSummary>>
}
