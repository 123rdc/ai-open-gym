package com.example.gymformcoach.core.progression

import com.example.gymformcoach.core.data.LoadType

/** §5.2.1: pre-fills last session's values verbatim. The app's existing behavior, and the default. */
object NoneRule : ProgressionRule {
    override fun nextTarget(
        history: List<SessionRecord>,
        config: ProgressionConfig,
        exercise: ExerciseSpec
    ): ProgressionTarget? {
        val last = history.firstOrNull() ?: return null
        return ProgressionTarget(
            weightKg = last.topLoadKg().takeIf { it > 0f },
            reps = last.topReps().takeIf { it > 0 } ?: exercise.targetReps,
            sets = exercise.targetSets,
            durationSeconds = last.topDurationSeconds().takeIf { it > 0 },
            isAmrapTopSet = false,
            reasoning = "Same as last session."
        )
    }
}

/** §5.2.2. Never advances on a missed session; deloads after a stall run. */
object LinearRule : ProgressionRule {
    override fun nextTarget(
        history: List<SessionRecord>,
        config: ProgressionConfig,
        exercise: ExerciseSpec
    ): ProgressionTarget? {
        if (history.isEmpty()) return null
        if (isBodyweightUnloaded(history, exercise)) {
            return bodyweightRepProgression(history, config, exercise)
        }
        if (isStale(history, config, System.currentTimeMillis())) {
            return staleTarget(history, config, exercise, System.currentTimeMillis())
        }

        val last = history.first()
        val lastLoad = last.topLoadKg()
        val misses = consecutiveMisses(history, exercise.targetReps)

        if (misses >= config.stallThreshold) {
            val deloaded = lastLoad * (1f - config.deloadPercent)
            return ProgressionTarget(
                weightKg = deloaded,
                reps = exercise.targetReps,
                sets = exercise.targetSets,
                durationSeconds = null,
                isAmrapTopSet = false,
                reasoning = "${misses}${ordinalSuffix(misses)} stall → deload ${(config.deloadPercent * 100).toInt()}% to ${formatKg(deloaded)}kg"
            )
        }

        return if (last.hitTarget(exercise.targetReps)) {
            val next = lastLoad + config.incrementKg
            ProgressionTarget(
                weightKg = next,
                reps = exercise.targetReps,
                sets = exercise.targetSets,
                durationSeconds = null,
                isAmrapTopSet = false,
                reasoning = "Hit ${exercise.targetSets}×${exercise.targetReps} @ ${formatKg(lastLoad)}kg → +${formatKg(config.incrementKg)}kg"
            )
        } else {
            ProgressionTarget(
                weightKg = lastLoad,
                reps = exercise.targetReps,
                sets = exercise.targetSets,
                durationSeconds = null,
                isAmrapTopSet = false,
                reasoning = "Missed reps last session → repeating ${formatKg(lastLoad)}kg"
            )
        }
    }
}

/** §5.2.3: final set is AMRAP; a big AMRAP earns a double jump. */
object GreyskullRule : ProgressionRule {
    override fun nextTarget(
        history: List<SessionRecord>,
        config: ProgressionConfig,
        exercise: ExerciseSpec
    ): ProgressionTarget? {
        if (history.isEmpty()) return null
        if (isBodyweightUnloaded(history, exercise)) {
            return bodyweightRepProgression(history, config, exercise)?.copy(isAmrapTopSet = true)
        }
        if (isStale(history, config, System.currentTimeMillis())) {
            return staleTarget(history, config, exercise, System.currentTimeMillis()).copy(isAmrapTopSet = true)
        }

        val last = history.first()
        val lastLoad = last.topLoadKg()
        // The AMRAP is the final set, so its rep count is what drives the decision.
        val amrapReps = last.sets.lastOrNull()?.reps ?: 0
        val misses = consecutiveMisses(history, exercise.targetReps)

        if (misses >= config.stallThreshold) {
            val deloaded = lastLoad * (1f - config.deloadPercent)
            return ProgressionTarget(
                weightKg = deloaded,
                reps = exercise.targetReps,
                sets = exercise.targetSets,
                durationSeconds = null,
                isAmrapTopSet = true,
                reasoning = "${misses}${ordinalSuffix(misses)} stall → deload ${(config.deloadPercent * 100).toInt()}% to ${formatKg(deloaded)}kg"
            )
        }

        return when {
            amrapReps >= config.doubleJumpThreshold -> {
                val next = lastLoad + config.incrementKg * 2
                ProgressionTarget(
                    weightKg = next,
                    reps = exercise.targetReps,
                    sets = exercise.targetSets,
                    durationSeconds = null,
                    isAmrapTopSet = true,
                    reasoning = "AMRAP hit $amrapReps reps (≥${config.doubleJumpThreshold}) → double jump +${formatKg(config.incrementKg * 2)}kg"
                )
            }
            amrapReps >= exercise.targetReps -> {
                val next = lastLoad + config.incrementKg
                ProgressionTarget(
                    weightKg = next,
                    reps = exercise.targetReps,
                    sets = exercise.targetSets,
                    durationSeconds = null,
                    isAmrapTopSet = true,
                    reasoning = "AMRAP hit $amrapReps reps → +${formatKg(config.incrementKg)}kg"
                )
            }
            else -> ProgressionTarget(
                weightKg = lastLoad,
                reps = exercise.targetReps,
                sets = exercise.targetSets,
                durationSeconds = null,
                isAmrapTopSet = true,
                reasoning = "AMRAP hit $amrapReps of ${exercise.targetReps} → repeating ${formatKg(lastLoad)}kg"
            )
        }
    }
}

/** §5.2.4: climb reps inside a range at fixed weight, then add weight and reset to the bottom. */
object DoubleProgressionRule : ProgressionRule {
    override fun nextTarget(
        history: List<SessionRecord>,
        config: ProgressionConfig,
        exercise: ExerciseSpec
    ): ProgressionTarget? {
        if (history.isEmpty()) return null
        if (isBodyweightUnloaded(history, exercise)) {
            return bodyweightRepProgression(history, config, exercise)
        }
        if (isStale(history, config, System.currentTimeMillis())) {
            return staleTarget(history, config, exercise, System.currentTimeMillis())
        }

        val last = history.first()
        val lastLoad = last.topLoadKg()
        val allAtTop = last.sets.isNotEmpty() && last.sets.all { (it.reps ?: 0) >= config.repRangeMax }

        return if (allAtTop) {
            val next = lastLoad + config.incrementKg
            ProgressionTarget(
                weightKg = next,
                reps = config.repRangeMin,
                sets = exercise.targetSets,
                durationSeconds = null,
                isAmrapTopSet = false,
                reasoning = "Hit ${exercise.targetSets}×${config.repRangeMax} (top of range) → +${formatKg(config.incrementKg)}kg, back to ${config.repRangeMin} reps"
            )
        } else {
            val achieved = last.sets.mapNotNull { it.reps }.minOrNull() ?: config.repRangeMin
            val nextReps = (achieved + 1).coerceAtMost(config.repRangeMax)
            ProgressionTarget(
                weightKg = lastLoad,
                reps = nextReps,
                sets = exercise.targetSets,
                durationSeconds = null,
                isAmrapTopSet = false,
                reasoning = "Hit $achieved reps @ ${formatKg(lastLoad)}kg → $nextReps next"
            )
        }
    }
}

/** §5.2.5: for TIMED exercises, progress held duration instead of load. */
object TimeBasedRule : ProgressionRule {
    override fun nextTarget(
        history: List<SessionRecord>,
        config: ProgressionConfig,
        exercise: ExerciseSpec
    ): ProgressionTarget? {
        val last = history.firstOrNull() ?: return null
        val lastDuration = last.topDurationSeconds()
        val hit = lastDuration >= exercise.targetDurationSeconds

        // Consecutive sessions that fell short of the target hold.
        val misses = history.takeWhile { it.topDurationSeconds() < exercise.targetDurationSeconds }.size
        if (misses >= config.stallThreshold) {
            val deloaded = (exercise.targetDurationSeconds * (1f - config.deloadPercent)).toInt()
            return ProgressionTarget(
                weightKg = null,
                reps = null,
                sets = exercise.targetSets,
                durationSeconds = deloaded,
                isAmrapTopSet = false,
                reasoning = "${misses}${ordinalSuffix(misses)} stall → deload to ${deloaded}s"
            )
        }

        return if (hit) {
            val next = lastDuration + config.incrementSeconds
            ProgressionTarget(
                weightKg = null,
                reps = null,
                sets = exercise.targetSets,
                durationSeconds = next,
                isAmrapTopSet = false,
                reasoning = "Held ${lastDuration}s → +${config.incrementSeconds}s to ${next}s"
            )
        } else {
            ProgressionTarget(
                weightKg = null,
                reps = null,
                sets = exercise.targetSets,
                durationSeconds = exercise.targetDurationSeconds,
                isAmrapTopSet = false,
                reasoning = "Held ${lastDuration}s of ${exercise.targetDurationSeconds}s → repeating ${exercise.targetDurationSeconds}s"
            )
        }
    }
}

/**
 * §5.2.6: the override applies when the exercise is bodyweight *and* the last
 * session carried no added load. A set with a dip belt reverts to normal weight
 * progression, and §5.4 warns not to assume continuity between the two.
 */
private fun isBodyweightUnloaded(history: List<SessionRecord>, exercise: ExerciseSpec): Boolean {
    if (exercise.loadType != LoadType.BODYWEIGHT) return false
    val last = history.firstOrNull() ?: return true
    return last.sets.none { (it.effectiveLoadKg ?: 0f) > 0f }
}

private fun ordinalSuffix(n: Int): String = when {
    n % 100 in 11..13 -> "th"
    n % 10 == 1 -> "st"
    n % 10 == 2 -> "nd"
    n % 10 == 3 -> "rd"
    else -> "th"
}

object ProgressionRules {
    fun of(type: ProgressionRuleType): ProgressionRule = when (type) {
        ProgressionRuleType.NONE -> NoneRule
        ProgressionRuleType.LINEAR -> LinearRule
        ProgressionRuleType.GREYSKULL_LP -> GreyskullRule
        ProgressionRuleType.DOUBLE_PROGRESSION -> DoubleProgressionRule
        ProgressionRuleType.TIME_BASED -> TimeBasedRule
    }
}
