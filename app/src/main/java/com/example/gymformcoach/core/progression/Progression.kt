package com.example.gymformcoach.core.progression

import com.example.gymformcoach.core.data.ExerciseType
import com.example.gymformcoach.core.data.LoadType

/**
 * §5 progression engine.
 *
 * Deliberately free of Android and Room dependencies so every rule is a pure
 * function that can be unit-tested without a device (§20.5). This is the one
 * place in the app where a silent logic error produces wrong numbers that still
 * look plausible, so the rules are testable in isolation and every target
 * carries its own explanation.
 */

/** One logged set, flattened out of SetLog for the rules to read. */
data class SetRecord(
    val weightKg: Float? = null,
    val addedWeightKg: Float? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null
) {
    /** §5.2.6: a bodyweight set counts as loaded only when extra weight was actually added. */
    val effectiveLoadKg: Float?
        get() = weightKg ?: addedWeightKg
}

/** One exercise's sets on one day. */
data class SessionRecord(
    val performedAt: Long,
    val sets: List<SetRecord>
)

/** What the exercise being progressed is, independent of the Room entity. */
data class ExerciseSpec(
    val exerciseType: ExerciseType = ExerciseType.REPS,
    val loadType: LoadType = LoadType.WEIGHTED,
    val isUnilateral: Boolean = false,
    val targetReps: Int = 10,
    val targetSets: Int = 3,
    val targetDurationSeconds: Int = 30
)

data class ProgressionConfig(
    val incrementKg: Float = 2.5f,
    val deloadPercent: Float = 0.10f,
    val repRangeMin: Int = 8,
    val repRangeMax: Int = 12,
    val stallThreshold: Int = 3,
    val doubleJumpThreshold: Int = 10,
    val incrementSeconds: Int = 5,
    val staleAfterDays: Int = 30,
    /** §1.3: past this, progression adds a set instead of a rep. */
    val repCeiling: Int = 20
)

data class ProgressionTarget(
    val weightKg: Float?,
    val reps: Int?,
    val sets: Int,
    val durationSeconds: Int?,
    val isAmrapTopSet: Boolean,
    /** §5.3: shown verbatim in the UI. Non-negotiable - a pre-filled number with
     *  no explanation is indistinguishable from a bug. */
    val reasoning: String
)

enum class ProgressionRuleType { NONE, LINEAR, GREYSKULL_LP, DOUBLE_PROGRESSION, TIME_BASED }

interface ProgressionRule {
    /**
     * @param history most recent first.
     * @return null when there's no basis for a target at all (§5.4: first time
     *   performing an exercise - the UI prompts for a working weight rather than
     *   the engine guessing a starting load).
     */
    fun nextTarget(
        history: List<SessionRecord>,
        config: ProgressionConfig,
        exercise: ExerciseSpec
    ): ProgressionTarget?
}

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

internal fun formatKg(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)

/** §5.4: last performed long enough ago that repeating the old load is unwise. */
internal fun isStale(history: List<SessionRecord>, config: ProgressionConfig, now: Long): Boolean {
    val last = history.firstOrNull() ?: return false
    return (now - last.performedAt) > config.staleAfterDays * MILLIS_PER_DAY
}

/** A session "hit" its target when every set reached the target reps. */
internal fun SessionRecord.hitTarget(targetReps: Int): Boolean =
    sets.isNotEmpty() && sets.all { (it.reps ?: 0) >= targetReps }

internal fun SessionRecord.topLoadKg(): Float =
    sets.mapNotNull { it.effectiveLoadKg }.maxOrNull() ?: 0f

internal fun SessionRecord.topReps(): Int = sets.mapNotNull { it.reps }.maxOrNull() ?: 0

internal fun SessionRecord.topDurationSeconds(): Int =
    sets.mapNotNull { it.durationSeconds }.maxOrNull() ?: 0

/** Consecutive most-recent sessions that missed the target. */
internal fun consecutiveMisses(history: List<SessionRecord>, targetReps: Int): Int =
    history.takeWhile { !it.hitTarget(targetReps) }.size

/**
 * §5.2.6: for an unloaded bodyweight exercise every rule progresses reps rather
 * than weight, subject to §1.3's rep ceiling. Applies regardless of which rule
 * was selected, which is why it lives outside the individual rules.
 */
internal fun bodyweightRepProgression(
    history: List<SessionRecord>,
    config: ProgressionConfig,
    exercise: ExerciseSpec
): ProgressionTarget? {
    val last = history.firstOrNull() ?: return null
    val lastReps = last.topReps()
    val hit = last.hitTarget(exercise.targetReps)

    if (!hit) {
        return ProgressionTarget(
            weightKg = null,
            reps = exercise.targetReps,
            sets = exercise.targetSets,
            durationSeconds = null,
            isAmrapTopSet = false,
            reasoning = "Missed ${exercise.targetReps} reps last session → repeating ${exercise.targetReps}"
        )
    }

    // §1.3: once a rep target would exceed the ceiling, add a set instead of a rep.
    val nextReps = lastReps + if (exercise.isUnilateral) 2 else 1
    return if (nextReps > config.repCeiling) {
        ProgressionTarget(
            weightKg = null,
            reps = config.repCeiling,
            sets = exercise.targetSets + 1,
            durationSeconds = null,
            isAmrapTopSet = false,
            reasoning = "Hit rep ceiling (${config.repCeiling}) → adding a set (${exercise.targetSets + 1}×${config.repCeiling})"
        )
    } else {
        ProgressionTarget(
            weightKg = null,
            reps = nextReps,
            sets = exercise.targetSets,
            durationSeconds = null,
            isAmrapTopSet = false,
            reasoning = "Bodyweight: hit $lastReps reps → $nextReps next"
        )
    }
}

/** §5.4: sparse history - suggest one deload step down, naming the gap. */
internal fun staleTarget(
    history: List<SessionRecord>,
    config: ProgressionConfig,
    exercise: ExerciseSpec,
    now: Long
): ProgressionTarget {
    val last = history.first()
    val daysAgo = ((now - last.performedAt) / MILLIS_PER_DAY).toInt()
    val reduced = last.topLoadKg() * (1f - config.deloadPercent)
    return ProgressionTarget(
        weightKg = reduced,
        reps = exercise.targetReps,
        sets = exercise.targetSets,
        durationSeconds = null,
        isAmrapTopSet = false,
        reasoning = "Last performed $daysAgo days ago → easing back to ${formatKg(reduced)}kg"
    )
}
