package com.example.gymformcoach.core.progression

/**
 * §6 estimated 1RM (Epley).
 *
 * The eligibility rules matter more than the formula: past 12 reps Epley's error
 * grows unacceptably, so those sets are *excluded* rather than extrapolated, and
 * the UI names the source set so the number is auditable rather than magic.
 */
object OneRepMax {

    const val MAX_ELIGIBLE_REPS = 12

    data class Estimate(
        val oneRepMaxKg: Float,
        val sourceWeightKg: Float,
        val sourceReps: Int,
        val performedAt: Long
    )

    fun isEligible(weightKg: Float?, reps: Int?): Boolean {
        val load = weightKg ?: return false
        val r = reps ?: return false
        return load > 0f && r in 1..MAX_ELIGIBLE_REPS
    }

    fun epley(weightKg: Float, reps: Int): Float = weightKg * (1f + reps / 30f)

    /** Returns null above the rep cutoff rather than a bad number (§6.2 calculator). */
    fun calculate(weightKg: Float, reps: Int): Float? =
        if (isEligible(weightKg, reps)) epley(weightKg, reps) else null

    /**
     * The maximum eligible estimate across the window, carrying the set it came
     * from so the UI can say "from 85kg × 6, 12 Aug".
     */
    fun best(history: List<SessionRecord>): Estimate? =
        history.flatMap { session -> session.sets.map { session.performedAt to it } }
            .filter { (_, set) -> isEligible(set.effectiveLoadKg, set.reps) }
            .map { (performedAt, set) ->
                val load = set.effectiveLoadKg!!
                val reps = set.reps!!
                Estimate(epley(load, reps), load, reps, performedAt)
            }
            .maxByOrNull { it.oneRepMaxKg }

    /** §13.3: one point per session for the 1RM curve, sessions with no eligible set omitted. */
    fun series(history: List<SessionRecord>): List<Estimate> =
        history.mapNotNull { session -> best(listOf(session)) }.sortedBy { it.performedAt }
}
