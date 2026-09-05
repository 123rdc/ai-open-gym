package com.example.gymformcoach.core.progression

import com.example.gymformcoach.core.data.ExerciseType
import com.example.gymformcoach.core.data.LoadType

/**
 * §8 personal records. The criterion depends on the exercise's shape - a
 * bodyweight set's PR is a rep count, a plank's is a duration, and cardio is
 * excluded entirely. Computed at set-write time so history views never
 * recompute on render.
 */
object PersonalRecords {

    sealed class Record {
        data class Weight(val weightKg: Float, val reps: Int) : Record()
        data class OneRepMax(val estimateKg: Float) : Record()
        data class Reps(val reps: Int) : Record()
        data class Duration(val seconds: Int) : Record()
    }

    /**
     * Whether [candidate] beats everything in [history] for this exercise shape.
     *
     * With no history at all this returns false: the first ever set of an
     * exercise is a baseline, not an achievement, and celebrating it would make
     * the badge meaningless.
     */
    fun isPersonalRecord(
        history: List<SessionRecord>,
        candidate: SetRecord,
        exerciseType: ExerciseType,
        loadType: LoadType
    ): Boolean {
        if (history.isEmpty()) return false
        val priorSets = history.flatMap { it.sets }
        if (priorSets.isEmpty()) return false

        return when (exerciseType) {
            // §8.1: cardio is excluded from PR tracking.
            ExerciseType.CARDIO -> false

            ExerciseType.TIMED -> {
                val best = priorSets.mapNotNull { it.durationSeconds }.maxOrNull() ?: return false
                (candidate.durationSeconds ?: 0) > best
            }

            ExerciseType.REPS -> when (loadType) {
                LoadType.BODYWEIGHT -> {
                    // Loaded bodyweight sets are judged as weighted; unloaded ones on reps.
                    if ((candidate.effectiveLoadKg ?: 0f) > 0f) {
                        beatsWeighted(priorSets, candidate)
                    } else {
                        val best = priorSets.mapNotNull { it.reps }.maxOrNull() ?: return false
                        (candidate.reps ?: 0) > best
                    }
                }
                LoadType.WEIGHTED -> beatsWeighted(priorSets, candidate)
            }
        }
    }

    /** §8.1: weighted reps PR on either raw load or estimated 1RM. */
    private fun beatsWeighted(priorSets: List<SetRecord>, candidate: SetRecord): Boolean {
        val load = candidate.effectiveLoadKg ?: return false
        val bestLoad = priorSets.mapNotNull { it.effectiveLoadKg }.maxOrNull() ?: 0f
        if (load > bestLoad) return true

        if (!OneRepMax.isEligible(load, candidate.reps)) return false
        val candidateE1rm = OneRepMax.epley(load, candidate.reps!!)
        val bestE1rm = priorSets
            .filter { OneRepMax.isEligible(it.effectiveLoadKg, it.reps) }
            .maxOfOrNull { OneRepMax.epley(it.effectiveLoadKg!!, it.reps!!) } ?: 0f
        return candidateE1rm > bestE1rm
    }

    /** Current best for display on the Profile screen (§8.2). */
    fun currentBest(
        history: List<SessionRecord>,
        exerciseType: ExerciseType,
        loadType: LoadType
    ): Record? {
        val sets = history.flatMap { it.sets }
        if (sets.isEmpty()) return null

        return when (exerciseType) {
            ExerciseType.CARDIO -> null
            ExerciseType.TIMED -> sets.mapNotNull { it.durationSeconds }.maxOrNull()?.let { Record.Duration(it) }
            ExerciseType.REPS -> {
                val loaded = sets.filter { (it.effectiveLoadKg ?: 0f) > 0f }
                if (loadType == LoadType.BODYWEIGHT && loaded.isEmpty()) {
                    sets.mapNotNull { it.reps }.maxOrNull()?.let { Record.Reps(it) }
                } else {
                    val top = loaded.maxByOrNull { it.effectiveLoadKg ?: 0f } ?: return null
                    Record.Weight(top.effectiveLoadKg ?: 0f, top.reps ?: 0)
                }
            }
        }
    }
}
