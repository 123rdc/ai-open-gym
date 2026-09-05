package com.example.gymformcoach.core.export

import kotlinx.serialization.Serializable

/**
 * §17B: structure only - routines and the weekly schedule. Explicitly excludes
 * session logs, sets, PRs, body-weight entries, analysis records, and settings.
 * The distinct `type` discriminator (vs. [FULL_EXPORT_TYPE]) is what lets the
 * importer reject a full-data export handed to the wrong entry point, and
 * vice versa - a training log leaking into what someone thinks is a plan
 * share would be a privacy failure, not a bug.
 */
const val PLAN_SHARE_TYPE = "PLAN_SHARE"

@Serializable
data class SharedRoutineExercise(
    val exerciseId: String,
    val orderIndex: Int,
    val targetWeightKg: Float,
    val targetReps: Int,
    val targetSets: Int,
    val supersetGroupId: String? = null,
    val progressionRuleOverride: String? = null
)

@Serializable
data class SharedRoutine(
    val name: String,
    val description: String,
    val progressionRule: String,
    val exercises: List<SharedRoutineExercise>
)

@Serializable
data class SharedWeeklyAssignment(val dayOfWeek: Int, val routineName: String?)

@Serializable
data class PlanShare(
    val type: String = PLAN_SHARE_TYPE,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val sharedAt: Long,
    val routines: List<SharedRoutine>,
    val weeklyPlan: List<SharedWeeklyAssignment>
)
