package com.example.gymformcoach.core.chat

import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.ExerciseSessionRepository
import com.example.gymformcoach.core.data.SetAnalysisRepository
import com.example.gymformcoach.core.utils.PreferenceManager
import kotlinx.coroutines.flow.first

/**
 * Builds the system prompt for Coach Chat: a short structured summary, not a dump of raw
 * history, so it stays within a local model's context window and keeps responses fast (§4).
 */
object ChatContextBuilder {

    private const val RECENT_SESSION_COUNT = 5
    private const val RECENT_ANALYSIS_COUNT = 8

    suspend fun build(database: AppDatabase, preferenceManager: PreferenceManager): String {
        val profile = database.userProfileDao().getProfileOnce()
        val recentSessions = ExerciseSessionRepository(database).getAllSessions().first()
            .sortedByDescending { it.performedAt }
            .take(RECENT_SESSION_COUNT)
        val recurringIssues = SetAnalysisRepository(database).getRecentOnce(RECENT_ANALYSIS_COUNT)
            .groupingBy { it.mainIssue }
            .eachCount()
            .filter { it.value > 1 }
            .keys

        val experience = profile?.trainingExperience ?: preferenceManager.trainingExperience
        val goal = profile?.fitnessGoal ?: preferenceManager.fitnessGoal
        val equipment = (profile?.equipment ?: preferenceManager.equipment).joinToString(", ").ifBlank { "unspecified" }
        val limitations = (profile?.trainingLimitations ?: preferenceManager.trainingLimitations).ifBlank { "none noted" }

        val sessionLines = if (recentSessions.isEmpty()) {
            "No sessions logged yet."
        } else {
            recentSessions.joinToString("\n") { "- ${it.exerciseId}: ${it.weightKg}kg x ${it.reps} (${it.sets} sets)" }
        }

        val issueLines = if (recurringIssues.isEmpty()) "None flagged." else recurringIssues.joinToString(", ")

        return """
            You are a fitness coach chatting with the user inside their workout app. Ground every
            answer in the summary below - never invent data you weren't given, and say plainly
            when you don't have enough information.
            Never give medical, injury-rehabilitation, or diagnostic advice.
            You may explain what a plan change (swapping an exercise, changing volume) would
            involve, but never claim to have made one yourself - a plan change only happens
            through the Coach's proposal screen, which the user opens separately.

            USER PROFILE:
            - Experience: $experience
            - Goal: $goal
            - Equipment: $equipment
            - Limitations: $limitations

            RECENT SESSIONS (most recent first):
            $sessionLines

            RECURRING FORM ISSUES:
            $issueLines
        """.trimIndent()
    }
}
