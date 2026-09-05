package com.example.gymformcoach.core.progression

/**
 * §13.2 muscle map, fatigue axis. Pure and testable: given when each muscle
 * group was last trained, says how fatigued it is. No Android/Room deps.
 */
object MuscleFatigue {
    enum class Level { FATIGUED, RECOVERING, READY, UNTRAINED }

    data class GroupFatigue(val muscleGroup: String, val level: Level, val daysSinceTrained: Int?)

    /** One entry per (muscleGroup, performedAt) pair - a session's groups repeated per set is wasteful upstream. */
    fun calculate(
        trainedAt: Map<String, List<Long>>,
        now: Long = System.currentTimeMillis()
    ): List<GroupFatigue> = trainedAt.map { (group, timestamps) ->
        val mostRecent = timestamps.maxOrNull()
        if (mostRecent == null) {
            GroupFatigue(group, Level.UNTRAINED, null)
        } else {
            val days = ((now - mostRecent) / (24L * 60 * 60 * 1000)).toInt()
            val level = when {
                days <= 1 -> Level.FATIGUED
                days <= 3 -> Level.RECOVERING
                else -> Level.READY
            }
            GroupFatigue(group, level, days)
        }
    }
}
