package com.example.gymformcoach.features.home

import java.time.LocalDate

data class StreakStats(
    val weekStreak: Int,
    val sessionsThisWeek: Int,
    val weeklyTarget: Int,
    val totalWorkouts: Int
)

/**
 * Pure so it's testable without a device - a week "counts" toward the streak if
 * at least one session was logged in it. Never fabricates a number: with no
 * sessions logged, every field is honestly zero (§18.5).
 */
object StreakCalculator {
    fun calculate(loggedDates: Set<LocalDate>, weeklyTarget: Int = 3): StreakStats {
        if (loggedDates.isEmpty()) return StreakStats(0, 0, weeklyTarget, 0)

        val today = LocalDate.now()
        val weeksWithSessions = loggedDates.map { it.weekStartMonday() }.toSet()

        var streak = 0
        var cursor = today.weekStartMonday()
        while (cursor in weeksWithSessions) {
            streak++
            cursor = cursor.minusWeeks(1)
        }

        val thisWeekStart = today.weekStartMonday()
        val sessionsThisWeek = loggedDates.count { it >= thisWeekStart && it <= today }

        return StreakStats(
            weekStreak = streak,
            sessionsThisWeek = sessionsThisWeek,
            weeklyTarget = weeklyTarget,
            totalWorkouts = loggedDates.size
        )
    }

    private fun LocalDate.weekStartMonday(): LocalDate = minusDays((dayOfWeek.value - 1).toLong())
}
