package com.example.gymformcoach.core.importer

import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.Exercise
import com.example.gymformcoach.core.data.ExerciseSession
import com.example.gymformcoach.core.data.SetLog
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

data class ImportPreview(
    val sourceName: String,
    val isExperimental: Boolean,
    val sessionCount: Int,
    val dateRange: Pair<LocalDate, LocalDate>?,
    val matchedExerciseCount: Int,
    val newExerciseNames: Set<String>,
    val malformedRowCount: Int,
    val sets: List<ImportedSet>
)

data class ImportCommitResult(
    val sessionsImported: Int,
    val sessionsSkippedAsDuplicate: Int,
    val exercisesCreated: Int
)

/**
 * §17C.1's shared path: exercise-name matching -> unit normalization (done in
 * the parser) -> duplicate detection -> preview -> commit. Every source
 * (FitNotes, Strong, Hevy) flows through this; only [WorkoutImporter.parse]
 * differs per source.
 */
class ImportPipeline(private val database: AppDatabase) {

    suspend fun preview(importer: WorkoutImporter, result: ImportResult): ImportPreview {
        val catalog = database.exerciseDao().getAll().first()
        val matched = result.sets.count { findMatch(it.exerciseName, catalog) != null }
        val newNames = result.sets.map { it.exerciseName }.distinct()
            .filter { findMatch(it, catalog) == null }
            .toSet()

        val dates = result.sets.map { it.performedAt }
        return ImportPreview(
            sourceName = importer.sourceName,
            isExperimental = importer.isExperimental,
            sessionCount = result.sets.groupBy { it.exerciseName to it.performedAt }.size,
            dateRange = if (dates.isNotEmpty()) dates.min() to dates.max() else null,
            matchedExerciseCount = matched,
            newExerciseNames = newNames,
            malformedRowCount = result.malformedRowCount,
            sets = result.sets
        )
    }

    /**
     * §17.2: each row is one set, grouped by Date+Exercise into a session,
     * preserving set order. A session already existing for that date+exercise
     * is skipped, not merged - re-importing the same file is a no-op.
     * §15.2/§17.2: unmatched exercise names become custom exercises - nothing
     * in the file is ever dropped.
     */
    suspend fun commit(preview: ImportPreview): ImportCommitResult {
        val catalog = database.exerciseDao().getAll().first().toMutableList()
        var created = 0

        // §15.2: unmatched names become custom exercises up front.
        for (name in preview.newExerciseNames) {
            val exercise = Exercise(
                name = name,
                bodyPart = "Imported",
                muscle = "Imported",
                category = "Bodyweight",
                duration = "20 mins",
                difficulty = "Beginner",
                imageUrl = "",
                about = "Imported from ${preview.sourceName}.",
                isCustom = true
            )
            database.exerciseDao().insert(exercise)
            catalog += exercise
            created++
        }

        val existingSessions = database.exerciseSessionDao().getAllSessions().first()
        val existingKeys = existingSessions.map {
            it.exerciseId to java.time.Instant.ofEpochMilli(it.performedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSet()

        var imported = 0
        var skipped = 0

        val grouped = preview.sets.groupBy { it.exerciseName to it.performedAt }
        for ((key, setsInSession) in grouped) {
            val (rawName, date) = key
            val matchedName = findMatch(rawName, catalog)?.name ?: rawName

            if ((matchedName to date) in existingKeys) {
                skipped++
                continue
            }

            val performedAtMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val topWeight = setsInSession.mapNotNull { it.weightKg }.maxOrNull() ?: 0f
            val totalReps = setsInSession.sumOf { it.reps ?: 0 }
            val session = ExerciseSession(
                exerciseId = matchedName,
                performedAt = performedAtMillis,
                weightKg = topWeight,
                reps = if (setsInSession.size == 1) (setsInSession.first().reps ?: 0) else totalReps,
                sets = setsInSession.size,
                volumeScore = setsInSession.sumOf { ((it.weightKg ?: 0f) * (it.reps ?: 0)).toDouble() }.toFloat()
            )
            database.exerciseSessionDao().insert(session)

            setsInSession.forEachIndexed { index, set ->
                database.setLogDao().insert(
                    SetLog(
                        exerciseSessionId = session.id,
                        setIndex = index,
                        weightKg = set.weightKg,
                        reps = set.reps,
                        distanceMeters = set.distanceMeters,
                        durationSeconds = set.durationSeconds,
                        effortValue = set.effortValue,
                        effortScale = set.effortScale
                    )
                )
            }
            imported++
        }

        return ImportCommitResult(imported, skipped, created)
    }

    /**
     * §17.2: case-insensitive, whitespace-trimmed, with handling for common
     * variants ("Bench Press (Barbell)" <-> "Barbell Bench Press") - matched by
     * comparing the normalized, word-sorted token set rather than requiring an
     * exact string match.
     */
    private fun findMatch(name: String, catalog: List<Exercise>): Exercise? {
        val target = normalize(name)
        return catalog.firstOrNull { normalize(it.name) == target }
    }

    companion object {
        /**
         * Exposed for testing - the actual normalization the pipeline matches
         * on ("Bench Press (Barbell)" <-> "Barbell Bench Press", §17.2).
         */
        internal fun normalize(name: String): String =
            name.lowercase()
                .replace(Regex("[()]"), " ")
                .split(Regex("[\\s,]+"))
                .filter { it.isNotBlank() }
                .sorted()
                .joinToString(" ")
    }
}
