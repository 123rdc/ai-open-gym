package com.example.gymformcoach.core.export

import kotlinx.serialization.Serializable

/**
 * §17.1: DTOs deliberately decoupled from the Room entities, so the export
 * schema can evolve on its own version number instead of tracking the DB
 * schema 1:1 - a future column rename shouldn't silently change what old
 * export files mean.
 */

const val FULL_EXPORT_TYPE = "FULL_EXPORT"
const val CURRENT_SCHEMA_VERSION = 1

@Serializable
data class RoutineDto(
    val id: String,
    val name: String,
    val description: String,
    val progressionRule: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class RoutineExerciseDto(
    val id: String,
    val routineId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val targetWeightKg: Float,
    val targetReps: Int,
    val targetSets: Int,
    val supersetGroupId: String? = null,
    val progressionRuleOverride: String? = null,
    val incrementKg: Float = 2.5f,
    val repRangeMin: Int = 8,
    val repRangeMax: Int = 12,
    val targetDurationSeconds: Int = 30
)

@Serializable
data class WeeklyPlanEntryDto(val id: String, val dayOfWeek: Int, val routineId: String?)

@Serializable
data class PlanOverrideDto(val id: String, val epochDay: Long, val routineId: String?)

@Serializable
data class ExerciseSessionDto(
    val id: String,
    val exerciseId: String,
    val performedAt: Long,
    val weightKg: Float,
    val reps: Int,
    val sets: Int,
    val volumeScore: Float,
    val createdAt: Long
)

@Serializable
data class SetLogDto(
    val id: String,
    val exerciseSessionId: String,
    val setIndex: Int,
    val weightKg: Float? = null,
    val addedWeightKg: Float? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Float? = null,
    val effortValue: Int? = null,
    val effortScale: String? = null,
    val isPr: Boolean = false
)

@Serializable
data class CustomExerciseDto(
    val id: String,
    val name: String,
    val bodyPart: String,
    val muscle: String,
    val category: String,
    val exerciseType: String,
    val loadType: String,
    val isUnilateral: Boolean,
    val muscleGroups: List<String>
)

@Serializable
data class BodyWeightEntryDto(
    val id: String,
    val weightKg: Float,
    val epochDay: Long,
    val recordedAt: Long,
    val source: String
)

@Serializable
data class UserProfileDto(
    val gender: String,
    val age: Int,
    val weightKg: Float,
    val heightCm: Float,
    val fitnessGoal: String,
    val activityLevel: String,
    val trainingExperience: String,
    val equipment: Set<String>,
    val trainingSplit: String,
    val focusAreas: Set<String>,
    val sessionLengthPreference: String,
    val trainingLimitations: String,
    val goalWeightKg: Float? = null
)

@Serializable
data class SettingsDto(
    val weightUnit: String,
    val restDurationSeconds: Int,
    val keepScreenOnDuringWorkout: Boolean,
    val effortTrackingEnabled: Boolean,
    val effortScale: String,
    val themeMode: String,
    val accent: String
)

/** §17.1: everything - the full backup, and the safety net for testing every importer. */
@Serializable
data class FullExport(
    val type: String = FULL_EXPORT_TYPE,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long,
    val routines: List<RoutineDto> = emptyList(),
    val routineExercises: List<RoutineExerciseDto> = emptyList(),
    val weeklyPlan: List<WeeklyPlanEntryDto> = emptyList(),
    val planOverrides: List<PlanOverrideDto> = emptyList(),
    val exerciseSessions: List<ExerciseSessionDto> = emptyList(),
    val setLogs: List<SetLogDto> = emptyList(),
    val customExercises: List<CustomExerciseDto> = emptyList(),
    val bodyWeightEntries: List<BodyWeightEntryDto> = emptyList(),
    val userProfile: UserProfileDto? = null,
    val settings: SettingsDto? = null
)

sealed class ImportOutcome {
    data class Success(val summary: String) : ImportOutcome()
    data class InvalidFormat(val reason: String) : ImportOutcome()
    data class WrongFileType(val expected: String, val found: String) : ImportOutcome()
}
