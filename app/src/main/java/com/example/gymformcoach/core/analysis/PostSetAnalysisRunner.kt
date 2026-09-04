package com.example.gymformcoach.core.analysis

import android.content.Context
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.SetAnalysis
import com.example.gymformcoach.core.data.SetAnalysisRepository
import com.example.gymformcoach.core.utils.PreferenceManager
import com.example.gymformcoach.features.workout.RepData
import org.json.JSONArray
import org.json.JSONObject

private const val SYSTEM_PROMPT = """You are a concise strength-training form coach analyzing one completed set from on-device rep sensors. You receive per-rep joint-angle metrics (already computed, units labeled) and a knowledge base of matched issues with likely causes and correctives. Pick the single most important issue for this set (say the set looked solid if the knowledge base list is empty), summarize the trend across reps in one short sentence, and choose up to 3 correctives ONLY from the provided knowledge base entries. Respond with ONLY this JSON, no markdown fences, no extra text: {"main_issue": "...", "trend_summary": "...", "correctives": ["...", "..."]}. Keep every field under 20 words. Never invent knowledge base entries that were not provided."""

/**
 * Orchestrates the post-set diagnosis: matches buffered rep metrics against the local knowledge
 * base, sends a compact JSON payload to the configured AI coach API, and persists the structured
 * result. Runs fully in the background - never blocks the results screen from appearing.
 */
object PostSetAnalysisRunner {

    suspend fun run(
        context: Context,
        database: AppDatabase,
        sessionId: String,
        exerciseType: String,
        weightKg: Float,
        reps: List<RepData>
    ) {
        PostSetAnalysisStatusBus.set(sessionId, PostSetAnalysisStatusBus.State.Loading)

        if (reps.isEmpty()) {
            PostSetAnalysisStatusBus.set(
                sessionId,
                PostSetAnalysisStatusBus.State.Failure("No rep data captured for this set")
            )
            return
        }

        val prefs = PreferenceManager(context)
        val apiUrl = prefs.aiCoachApiUrl
        if (apiUrl.isBlank()) {
            PostSetAnalysisStatusBus.set(
                sessionId,
                PostSetAnalysisStatusBus.State.Failure("AI coach unavailable — set your API endpoint in Settings")
            )
            return
        }

        val issueKeys = detectIssueKeys(exerciseType, reps)
        val matchedEntries = CorrectivesKnowledgeBase.match(context, issueKeys)
        val userContent = buildUserContent(exerciseType, weightKg, reps, matchedEntries)

        val result = AiCoachApiClient.requestSetAnalysis(
            apiUrl = apiUrl,
            apiKey = prefs.aiCoachApiKey,
            model = prefs.aiCoachModel,
            systemPrompt = SYSTEM_PROMPT,
            userContent = userContent
        )

        result.fold(
            onSuccess = { content ->
                val parsed = parseAnalysisJson(content)
                if (parsed == null) {
                    PostSetAnalysisStatusBus.set(
                        sessionId,
                        PostSetAnalysisStatusBus.State.Failure("AI coach response could not be understood")
                    )
                    return@fold
                }
                val analysis = SetAnalysis(
                    exerciseSessionId = sessionId,
                    mainIssue = parsed.first,
                    trendSummary = parsed.second,
                    correctives = parsed.third
                )
                SetAnalysisRepository(database).save(analysis)
                PostSetAnalysisStatusBus.set(sessionId, PostSetAnalysisStatusBus.State.Success(analysis))
            },
            onFailure = {
                PostSetAnalysisStatusBus.set(
                    sessionId,
                    PostSetAnalysisStatusBus.State.Failure("AI coach unavailable — check your API connection")
                )
            }
        )
    }

    private fun detectIssueKeys(exerciseType: String, reps: List<RepData>): List<String> {
        val keys = mutableSetOf<String>()
        when (exerciseType.lowercase()) {
            "squat" -> {
                if (reps.any { it.minPrimaryAngleDeg > 100.0 }) keys.add("insufficient_depth_squat")
                if (reps.any { (it.minBackAngleDeg ?: 180.0) < 45.0 }) keys.add("excessive_torso_lean_squat")
            }
            "bench press", "pushup" -> {
                if (reps.any { it.minPrimaryAngleDeg > 90.0 }) keys.add("insufficient_depth_pushup")
            }
        }
        if (reps.size >= 3) {
            val firstHalfAvg = reps.take(reps.size / 2).map { it.durationMs }.average()
            val secondHalfAvg = reps.takeLast(reps.size / 2).map { it.durationMs }.average()
            if (secondHalfAvg > firstHalfAvg * 1.4) keys.add("fatigue_driven_form_breakdown")
        }
        return keys.toList()
    }

    private fun buildUserContent(
        exerciseType: String,
        weightKg: Float,
        reps: List<RepData>,
        matchedEntries: List<CorrectiveKbEntry>
    ): String {
        val repsArray = JSONArray()
        reps.forEach { rep ->
            repsArray.put(
                JSONObject().apply {
                    put("rep_number", rep.repNumber)
                    put("duration_sec", rep.durationMs / 1000.0)
                    put("min_primary_joint_angle_deg", rep.minPrimaryAngleDeg)
                    put("primary_joint_angle_note", "lower value = deeper range of motion at this joint")
                    rep.minBackAngleDeg?.let {
                        put("min_torso_angle_deg", it)
                        put("torso_angle_note", "angle between shoulder-hip-knee; lower value = more forward lean")
                    }
                }
            )
        }
        val metrics = JSONObject().apply {
            put("exercise", exerciseType)
            put("weight_kg", weightKg)
            put("weight_unit_note", "weight is always in kilograms regardless of the user's display unit")
            put("reps", repsArray)
        }

        val kbArray = JSONArray()
        matchedEntries.forEach { entry ->
            kbArray.put(
                JSONObject().apply {
                    put("issue", entry.issue)
                    put("likely_causes", JSONArray(entry.likelyCauses))
                    put("correctives", JSONArray(entry.correctives))
                }
            )
        }

        return "SET_METRICS:\n${metrics}\n\nKNOWLEDGE_BASE_MATCHES:\n${kbArray}"
    }

    private fun parseAnalysisJson(content: String): Triple<String, String, List<String>>? {
        return try {
            val cleaned = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(cleaned)
            val mainIssue = obj.getString("main_issue")
            val trendSummary = obj.getString("trend_summary")
            val correctivesArray = obj.optJSONArray("correctives") ?: JSONArray()
            val correctives = (0 until correctivesArray.length()).map { correctivesArray.getString(it) }
            Triple(mainIssue, trendSummary, correctives)
        } catch (e: Exception) {
            null
        }
    }
}
