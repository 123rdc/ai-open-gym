package com.example.gymformcoach.core.export

import android.content.Context
import androidx.core.content.FileProvider
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.Exercise
import com.example.gymformcoach.core.data.RoutineExercise
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.File

class PlanShareRepository(private val database: AppDatabase) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun buildShare(): PlanShare {
        val summaries = database.routineDao().getRoutineSummaries().first()
        val routines = summaries.mapNotNull { summary ->
            val routine = database.routineDao().getRoutineOnce(summary.id) ?: return@mapNotNull null
            val exercises = database.routineDao().getRoutineExercisesOnce(routine.id)
                .sortedBy { it.orderIndex }
                .map {
                    SharedRoutineExercise(
                        it.exerciseId, it.orderIndex, it.targetWeightKg, it.targetReps, it.targetSets,
                        it.supersetGroupId, it.progressionRuleOverride
                    )
                }
            SharedRoutine(routine.name, routine.description, routine.progressionRule, exercises)
        }

        val weekly = database.planDao().getWeeklyPlan().first()
        val weeklyShared = weekly.map { entry ->
            val name = summaries.firstOrNull { it.id == entry.routineId }?.name
            SharedWeeklyAssignment(entry.dayOfWeek, name)
        }

        return PlanShare(sharedAt = System.currentTimeMillis(), routines = routines, weeklyPlan = weeklyShared)
    }

    fun serialize(share: PlanShare): String = json.encodeToString(PlanShare.serializer(), share)

    fun parse(content: String): Result<PlanShare> = runCatching {
        val share = json.decodeFromString(PlanShare.serializer(), content)
        require(share.type == PLAN_SHARE_TYPE) {
            "Expected a plan-share file (type=$PLAN_SHARE_TYPE), found type=${share.type}. " +
                "This looks like a full backup - use Import on the Data screen instead."
        }
        share
    }

    /** Writes to app cache and returns a content:// URI suitable for a share-sheet intent. */
    fun writeShareFile(context: Context, share: PlanShare): android.net.Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "plan_share_${System.currentTimeMillis()}.json")
        file.writeText(serialize(share))
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * §17B.2: additive only - the recipient's existing plan is never overwritten.
     * Name collisions get a suffix rather than a silent replace. Weekly-plan
     * assignments are applied only for the days in [adoptDaysOfWeek] - the
     * caller gets to offer them, not auto-apply them.
     */
    suspend fun importShare(share: PlanShare, adoptDaysOfWeek: Set<Int>): String {
        val existingNames = database.routineDao().getRoutineSummaries().first().map { it.name }.toMutableSet()
        val newRoutineIdByName = mutableMapOf<String, String>()

        for (shared in share.routines) {
            var name = shared.name
            if (name in existingNames) {
                var suffix = 1
                var candidate = "$name (imported)"
                while (candidate in existingNames) {
                    suffix++
                    candidate = "$name (imported $suffix)"
                }
                name = candidate
            }
            existingNames += name

            val exercises = shared.exercises.map { se ->
                ensureExerciseExists(se.exerciseId)
                RoutineExercise(
                    routineId = "", // filled in by saveRoutine
                    exerciseId = se.exerciseId,
                    orderIndex = se.orderIndex,
                    targetWeightKg = se.targetWeightKg,
                    targetReps = se.targetReps,
                    targetSets = se.targetSets,
                    supersetGroupId = se.supersetGroupId,
                    progressionRuleOverride = se.progressionRuleOverride
                )
            }
            val routineId = com.example.gymformcoach.core.data.RoutineRepository(database)
                .saveRoutine(name, shared.description, exercises, shared.progressionRule)
            newRoutineIdByName[shared.name] = routineId
        }

        var adopted = 0
        for (assignment in share.weeklyPlan) {
            if (assignment.dayOfWeek !in adoptDaysOfWeek) continue
            val routineId = assignment.routineName?.let { newRoutineIdByName[it] }
            database.planDao().upsertWeeklyEntry(
                com.example.gymformcoach.core.data.WeeklyPlanEntry(dayOfWeek = assignment.dayOfWeek, routineId = routineId)
            )
            adopted++
        }

        return "Imported ${share.routines.size} routine(s)" + if (adopted > 0) ", $adopted day(s) of the weekly plan." else "."
    }

    /**
     * §17B.2/§17.2: unknown exercises become custom exercises rather than being
     * dropped - the same convention the CSV importers use.
     */
    private suspend fun ensureExerciseExists(name: String) {
        val existing = database.exerciseDao().findByName(name)
        if (existing != null) return
        database.exerciseDao().insert(
            Exercise(
                name = name,
                bodyPart = "Custom",
                muscle = "Custom",
                category = "Bodyweight",
                duration = "20 mins",
                difficulty = "Beginner",
                imageUrl = "",
                about = "Imported from a shared plan.",
                isCustom = true
            )
        )
    }
}
