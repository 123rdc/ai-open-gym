package com.example.gymformcoach.core.data

import com.example.gymformcoach.core.progression.ExerciseSpec
import com.example.gymformcoach.core.progression.ProgressionConfig
import com.example.gymformcoach.core.progression.ProgressionRuleType
import com.example.gymformcoach.core.progression.ProgressionRules
import com.example.gymformcoach.core.progression.ProgressionTarget
import com.example.gymformcoach.core.progression.SessionRecord
import com.example.gymformcoach.core.progression.SetRecord

/**
 * Bridges Room data to the pure progression engine in `core.progression` -
 * everything here is I/O; everything there is logic (§20.5).
 */
class ProgressionRepository(private val database: AppDatabase) {
    private val sessionDao = database.exerciseSessionDao()
    private val setLogDao = database.setLogDao()
    private val exerciseDao = database.exerciseDao()

    /**
     * §5.1 resolution: exercise override -> routine default -> NONE.
     */
    suspend fun nextTarget(routineExercise: RoutineExercise, routine: Routine?): ProgressionTarget? {
        val exercise = exerciseDao.findByName(routineExercise.exerciseId)
        val ruleType = parseRule(routineExercise.progressionRuleOverride)
            ?: parseRule(routine?.progressionRule)
            ?: ProgressionRuleType.NONE

        val sessions = sessionDao.getSessionsForExerciseOnce(routineExercise.exerciseId)
            .sortedByDescending { it.performedAt }

        val history = sessions.map { session ->
            val logs = setLogDao.getForSessionOnce(session.id)
            val sets = if (logs.isNotEmpty()) {
                logs.map { SetRecord(it.weightKg, it.addedWeightKg, it.reps, it.durationSeconds) }
            } else {
                // Older sessions logged before SetLog existed (or a chunk that
                // never got a SetLog row) still have the aggregate columns -
                // fall back to those rather than losing the history entirely.
                listOf(SetRecord(weightKg = session.weightKg.takeIf { it > 0f }, reps = session.reps))
            }
            SessionRecord(session.performedAt, sets)
        }

        val spec = ExerciseSpec(
            exerciseType = exercise?.exerciseType ?: ExerciseType.REPS,
            loadType = exercise?.loadType ?: LoadType.WEIGHTED,
            isUnilateral = exercise?.isUnilateral ?: false,
            targetReps = routineExercise.targetReps,
            targetSets = routineExercise.targetSets,
            targetDurationSeconds = routineExercise.targetDurationSeconds
        )
        val config = ProgressionConfig(
            incrementKg = routineExercise.incrementKg,
            repRangeMin = routineExercise.repRangeMin,
            repRangeMax = routineExercise.repRangeMax
        )

        return ProgressionRules.of(ruleType).nextTarget(history, config, spec)
    }

    private fun parseRule(value: String?): ProgressionRuleType? =
        value?.let { runCatching { ProgressionRuleType.valueOf(it) }.getOrNull() }
}
