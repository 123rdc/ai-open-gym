package com.example.gymformcoach.core.importer

import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * §17.2. Column set per spec:
 * Date, Exercise, Category, Weight (kg), Weight (lbs), Reps, Distance, Distance Unit, Time, Comment
 *
 * Each row is one set, not one workout - grouping into sessions happens in
 * [ImportPipeline], not here. Only one of Weight (kg)/Weight (lbs) is
 * populated per row; lbs converts at import so nothing is ever stored mixed.
 */
class FitNotesImporter : WorkoutImporter {
    override val sourceName = "FitNotes"

    override fun canParse(header: String): Boolean {
        val cols = splitCsvLine(header).map { it.trim() }
        return "Date" in cols && "Exercise" in cols && cols.any { it.startsWith("Weight") }
    }

    override fun parse(input: InputStream): ImportResult {
        val lines = input.bufferedReader().readLines()
        if (lines.isEmpty()) return ImportResult(emptyList())

        val header = splitCsvLine(lines.first())
        fun col(name: String) = header.indexOf(name).takeIf { it >= 0 }

        val dateIdx = col("Date") ?: return ImportResult(emptyList(), lines.size - 1)
        val exerciseIdx = col("Exercise") ?: return ImportResult(emptyList(), lines.size - 1)
        val weightKgIdx = col("Weight (kg)")
        val weightLbsIdx = col("Weight (lbs)")
        val repsIdx = col("Reps")
        val distanceIdx = col("Distance")
        val timeIdx = col("Time")

        val formatter = DateTimeFormatter.ISO_LOCAL_DATE // YYYY-MM-DD
        val sets = mutableListOf<ImportedSet>()
        var malformed = 0

        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val row = splitCsvLine(line)
            try {
                val date = LocalDate.parse(row[dateIdx], formatter)
                val exercise = row[exerciseIdx]
                if (exercise.isBlank()) {
                    malformed++
                    continue
                }

                val weightKg = weightKgIdx?.let { row.getOrNull(it)?.toFloatOrNull() }
                val weightLbs = weightLbsIdx?.let { row.getOrNull(it)?.toFloatOrNull() }
                val reps = repsIdx?.let { row.getOrNull(it)?.toIntOrNull() }
                val distance = distanceIdx?.let { row.getOrNull(it)?.toFloatOrNull() }
                val timeSeconds = timeIdx?.let { parseDurationToSeconds(row.getOrNull(it)) }

                // §17.2: distance/time populated instead of weight/reps -> cardio, not a zero-weight strength set.
                val isCardio = (distance != null || timeSeconds != null) && weightKg == null && weightLbs == null && reps == null

                sets += ImportedSet(
                    exerciseName = exercise,
                    performedAt = date,
                    weightKg = weightKg ?: weightLbs?.let { lbsToKg(it) },
                    reps = reps,
                    distanceMeters = distance,
                    durationSeconds = timeSeconds,
                    isCardio = isCardio
                )
            } catch (e: Exception) {
                malformed++
            }
        }

        return ImportResult(sets, malformed)
    }

    /** FitNotes' Time column is typically HH:mm:ss or mm:ss. */
    private fun parseDurationToSeconds(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            2 -> parts[0] * 60 + parts[1]
            1 -> parts[0]
            else -> null
        }
    }
}
