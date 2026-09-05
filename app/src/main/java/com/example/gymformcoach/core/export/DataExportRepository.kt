package com.example.gymformcoach.core.export

import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.BodyWeightEntry
import com.example.gymformcoach.core.data.Exercise
import com.example.gymformcoach.core.data.ExerciseSession
import com.example.gymformcoach.core.data.ExerciseType
import com.example.gymformcoach.core.data.LoadType
import com.example.gymformcoach.core.data.PlanOverride
import com.example.gymformcoach.core.data.Routine
import com.example.gymformcoach.core.data.RoutineExercise
import com.example.gymformcoach.core.data.SetLog
import com.example.gymformcoach.core.data.UserProfile
import com.example.gymformcoach.core.data.WeeklyPlanEntry
import com.example.gymformcoach.core.utils.PreferenceManager
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * §17.1: one-tap full export/import - everything, and the backup mechanism the
 * spec explicitly wants built first so every other importer (§17C) has
 * something safe to test against.
 */
class DataExportRepository(
    private val database: AppDatabase,
    private val preferenceManager: PreferenceManager
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportAll(): FullExport {
        val routines = database.routineDao().let { dao ->
            // No "get all routines" query exists yet; summaries carry id/name/description.
            dao.getRoutineSummaries().first()
        }
        val allRoutineExercises = mutableListOf<RoutineExerciseDto>()
        val routineDtos = mutableListOf<RoutineDto>()
        for (summary in routines) {
            val routine = database.routineDao().getRoutineOnce(summary.id) ?: continue
            routineDtos += routine.toDto()
            allRoutineExercises += database.routineDao().getRoutineExercisesOnce(routine.id).map { it.toDto() }
        }

        val sessions = database.exerciseSessionDao().getAllSessions().first()
        val setLogs = sessions.flatMap { database.setLogDao().getForSessionOnce(it.id) }
        val customExercises = database.exerciseDao().getAll().first().filter { it.isCustom }
        val bodyWeight = database.bodyWeightDao().getAll().first()
        val profile = database.userProfileDao().getProfileOnce()
        val weeklyPlan = database.planDao().getWeeklyPlan().first()
        val overrides = database.planDao().getOverridesFrom(Long.MIN_VALUE).first()

        return FullExport(
            exportedAt = System.currentTimeMillis(),
            routines = routineDtos,
            routineExercises = allRoutineExercises,
            weeklyPlan = weeklyPlan.map { WeeklyPlanEntryDto(it.id, it.dayOfWeek, it.routineId) },
            planOverrides = overrides.map { PlanOverrideDto(it.id, it.epochDay, it.routineId) },
            exerciseSessions = sessions.map { it.toDto() },
            setLogs = setLogs.map { it.toDto() },
            customExercises = customExercises.map { it.toDto() },
            bodyWeightEntries = bodyWeight.map { it.toDto() },
            userProfile = profile?.toDto(),
            settings = SettingsDto(
                weightUnit = preferenceManager.weightUnit,
                restDurationSeconds = preferenceManager.restDurationSeconds,
                keepScreenOnDuringWorkout = preferenceManager.keepScreenOnDuringWorkout,
                effortTrackingEnabled = preferenceManager.effortTrackingEnabled,
                effortScale = preferenceManager.effortScale,
                themeMode = preferenceManager.themeMode,
                accent = preferenceManager.accent
            )
        )
    }

    fun serialize(export: FullExport): String = json.encodeToString(FullExport.serializer(), export)

    /** Parses and validates structure only - no write happens here (§17.1). */
    fun parse(content: String): Result<FullExport> = runCatching {
        val export = json.decodeFromString(FullExport.serializer(), content)
        require(export.type == FULL_EXPORT_TYPE) {
            "Expected a full export file (type=$FULL_EXPORT_TYPE), found type=${export.type}"
        }
        export
    }

    /**
     * Whether importing would overwrite anything, so the caller can warn
     * explicitly before proceeding (§17.1).
     */
    suspend fun hasExistingData(): Boolean {
        val hasRoutines = database.routineDao().getRoutineSummaries().first().isNotEmpty()
        val hasSessions = database.exerciseSessionDao().getAllSessions().first().isNotEmpty()
        return hasRoutines || hasSessions
    }

    /** Writes everything in the export. Caller is responsible for the overwrite warning (§17.1). */
    suspend fun importAll(export: FullExport): ImportOutcome {
        return try {
            database.withTransaction {
                export.routines.forEach { database.routineDao().insertRoutine(it.toEntity()) }
                database.routineDao().insertRoutineExercises(export.routineExercises.map { it.toEntity() })
                export.weeklyPlan.forEach {
                    database.planDao().upsertWeeklyEntry(WeeklyPlanEntry(it.id, it.dayOfWeek, it.routineId))
                }
                export.planOverrides.forEach {
                    database.planDao().upsertOverride(PlanOverride(it.id, it.epochDay, it.routineId))
                }
                export.exerciseSessions.forEach { database.exerciseSessionDao().insert(it.toEntity()) }
                export.setLogs.forEach { database.setLogDao().insert(it.toEntity()) }
                export.customExercises.forEach { database.exerciseDao().insert(it.toEntity()) }
                export.bodyWeightEntries.forEach { database.bodyWeightDao().upsert(it.toEntity()) }
                export.userProfile?.let { dto ->
                    val existing = database.userProfileDao().getProfileOnce()
                    val entity = dto.toEntity(existing?.id)
                    if (existing == null) database.userProfileDao().insert(entity) else database.userProfileDao().update(entity)
                }
                export.settings?.let { s ->
                    preferenceManager.weightUnit = s.weightUnit
                    preferenceManager.restDurationSeconds = s.restDurationSeconds
                    preferenceManager.keepScreenOnDuringWorkout = s.keepScreenOnDuringWorkout
                    preferenceManager.effortTrackingEnabled = s.effortTrackingEnabled
                    preferenceManager.effortScale = s.effortScale
                    preferenceManager.themeMode = s.themeMode
                    preferenceManager.accent = s.accent
                }
            }
            ImportOutcome.Success(
                "Imported ${export.routines.size} routines, ${export.exerciseSessions.size} sessions, " +
                    "${export.bodyWeightEntries.size} body-weight entries."
            )
        } catch (e: Exception) {
            ImportOutcome.InvalidFormat(e.message ?: "Import failed")
        }
    }

    // ---- entity <-> dto ----

    private fun Routine.toDto() = RoutineDto(id, name, description, progressionRule, createdAt, updatedAt)
    private fun RoutineDto.toEntity() = Routine(id, name, description, progressionRule, createdAt = createdAt, updatedAt = updatedAt)

    private fun RoutineExercise.toDto() = RoutineExerciseDto(
        id, routineId, exerciseId, orderIndex, targetWeightKg, targetReps, targetSets,
        supersetGroupId, progressionRuleOverride, incrementKg, repRangeMin, repRangeMax, targetDurationSeconds
    )
    private fun RoutineExerciseDto.toEntity() = RoutineExercise(
        id, routineId, exerciseId, orderIndex, targetWeightKg, targetReps, targetSets,
        supersetGroupId = supersetGroupId, progressionRuleOverride = progressionRuleOverride,
        incrementKg = incrementKg, repRangeMin = repRangeMin, repRangeMax = repRangeMax,
        targetDurationSeconds = targetDurationSeconds
    )

    private fun ExerciseSession.toDto() = ExerciseSessionDto(id, exerciseId, performedAt, weightKg, reps, sets, volumeScore, createdAt)
    private fun ExerciseSessionDto.toEntity() = ExerciseSession(id, exerciseId, performedAt, weightKg, reps, sets, volumeScore, createdAt = createdAt)

    private fun SetLog.toDto() = SetLogDto(id, exerciseSessionId, setIndex, weightKg, addedWeightKg, reps, durationSeconds, distanceMeters, effortValue, effortScale, isPr)
    private fun SetLogDto.toEntity() = SetLog(id, exerciseSessionId, setIndex, weightKg, addedWeightKg, reps, durationSeconds, distanceMeters, effortValue, effortScale, isPr)

    private fun Exercise.toDto() = CustomExerciseDto(id, name, bodyPart, muscle, category, exerciseType.name, loadType.name, isUnilateral, muscleGroups)
    private fun CustomExerciseDto.toEntity() = Exercise(
        id = id, name = name, bodyPart = bodyPart, muscle = muscle, category = category,
        duration = "20 mins", difficulty = "Beginner", imageUrl = "", about = "Custom exercise.",
        exerciseType = runCatching { ExerciseType.valueOf(exerciseType) }.getOrDefault(ExerciseType.REPS),
        loadType = runCatching { LoadType.valueOf(loadType) }.getOrDefault(LoadType.WEIGHTED),
        isUnilateral = isUnilateral, isCustom = true, muscleGroups = muscleGroups
    )

    private fun BodyWeightEntry.toDto() = BodyWeightEntryDto(id, weightKg, epochDay, recordedAt, source)
    private fun BodyWeightEntryDto.toEntity() = BodyWeightEntry(id, weightKg, epochDay, recordedAt, source)

    private fun UserProfile.toDto() = UserProfileDto(
        gender, age, weightKg, heightCm, fitnessGoal, activityLevel, trainingExperience,
        equipment, trainingSplit, focusAreas, sessionLengthPreference, trainingLimitations, goalWeightKg
    )
    private fun UserProfileDto.toEntity(existingId: String?) = UserProfile(
        id = existingId ?: java.util.UUID.randomUUID().toString(),
        gender = gender, age = age, weightKg = weightKg, heightCm = heightCm,
        fitnessGoal = fitnessGoal, activityLevel = activityLevel, trainingExperience = trainingExperience,
        equipment = equipment, trainingSplit = trainingSplit, focusAreas = focusAreas,
        sessionLengthPreference = sessionLengthPreference, trainingLimitations = trainingLimitations,
        goalWeightKg = goalWeightKg
    )
}
