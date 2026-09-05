package com.example.gymformcoach.core.coach

import com.example.gymformcoach.core.progression.ProgressionRuleType

/**
 * §17D.2/§20.9: built before the renderer. Every field the model can hand back
 * is treated as untrusted input - a model-invented exercise name that silently
 * becomes a custom exercise is a data-quality failure the spec calls out
 * explicitly, so unknown IDs are dropped and counted, never created.
 *
 * Pure, no Android/LLM dependency - testable the same way the progression
 * rules are (§20.5's pattern).
 */
object PlanValidator {

    private const val MIN_SETS = 1
    private const val MAX_SETS = 10
    private const val MIN_REPS = 1
    private const val MAX_REPS = 50

    data class ValidatedPlan(
        val proposal: TrainingPlanProposal,
        val droppedExerciseCount: Int,
        val droppedRoutineNames: List<String>,
        val coercedRuleCount: Int
    ) {
        val isEmpty: Boolean get() = proposal.routines.isEmpty()
    }

    /**
     * @param catalogExerciseIds the exact set of exercise IDs the model was
     *   given - anything outside this set is a hallucination, not a
     *   legitimate new exercise, and is dropped rather than created (§17D.2's
     *   "hard constraint - exercise IDs must come from the supplied catalog").
     */
    fun validate(raw: TrainingPlanProposal, catalogExerciseIds: Set<String>): ValidatedPlan {
        var droppedExercises = 0
        var coercedRules = 0
        val droppedRoutines = mutableListOf<String>()

        val validRoutines = raw.routines.mapNotNull { routine ->
            val seenExerciseIds = mutableSetOf<String>()
            val validExercises = routine.exercises.filter { exercise ->
                when {
                    exercise.exerciseId !in catalogExerciseIds -> {
                        droppedExercises++; false
                    }
                    exercise.exerciseId in seenExerciseIds -> {
                        // Duplicate exercise within one routine (§17D.2 validation list).
                        droppedExercises++; false
                    }
                    exercise.repRangeMin > exercise.repRangeMax -> {
                        droppedExercises++; false
                    }
                    exercise.repRangeMin < MIN_REPS || exercise.repRangeMax > MAX_REPS -> {
                        droppedExercises++; false
                    }
                    else -> {
                        seenExerciseIds += exercise.exerciseId
                        true
                    }
                }
            }.map { it.copy(sets = it.sets.coerceIn(MIN_SETS, MAX_SETS)) }

            if (validExercises.isEmpty()) {
                droppedRoutines += routine.name
                null
            } else {
                val validRule = runCatching { ProgressionRuleType.valueOf(routine.progressionRule) }.isSuccess
                if (!validRule) coercedRules++
                routine.copy(
                    exercises = validExercises,
                    progressionRule = if (validRule) routine.progressionRule else ProgressionRuleType.NONE.name
                )
            }
        }

        val validRoutineNames = validRoutines.map { it.name }.toSet()
        val validWeeklyPlan = raw.weeklyPlan.filter {
            it.dayOfWeek in 1..7 && it.routineName in validRoutineNames
        }

        return ValidatedPlan(
            proposal = raw.copy(routines = validRoutines, weeklyPlan = validWeeklyPlan),
            droppedExerciseCount = droppedExercises,
            droppedRoutineNames = droppedRoutines,
            coercedRuleCount = coercedRules
        )
    }
}
