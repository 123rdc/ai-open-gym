package com.example.gymformcoach.core.importer

import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * §17C.2 Hevy: CSV export, row-per-set, includes workout title, exercise
 * notes, and an RPE column that maps directly onto §10's effort field.
 *
 * §17C.3: EXPERIMENTAL - same caveat as [StrongImporter]. Written against
 * Hevy's documented/publicly-known export columns, not a verified sample.
 */
class HevyImporter : WorkoutImporter {
    override val sourceName = "Hevy"
    override val isExperimental = true

    private val expectedColumns = setOf("title", "start_time", "exercise_title", "weight_kg", "reps")

    override fun canParse(header: String): Boolean {
        val cols = splitCsvLine(header).map { it.trim() }
        return expectedColumns.all { it in cols }
    }

    override fun parse(input: InputStream): ImportResult {
        val lines = input.bufferedReader().readLines()
        if (lines.isEmpty()) return ImportResult(emptyList())

        val header = splitCsvLine(lines.first())
        fun col(name: String) = header.indexOf(name).takeIf { it >= 0 }

        val startTimeIdx = col("start_time") ?: return ImportResult(emptyList(), lines.size - 1)
        val exerciseIdx = col("exercise_title") ?: return ImportResult(emptyList(), lines.size - 1)
        val weightIdx = col("weight_kg")
        val repsIdx = col("reps")
        val distanceIdx = col("distance_km")
        val durationIdx = col("duration_seconds")
        val rpeIdx = col("rpe")

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val sets = mutableListOf<ImportedSet>()
        var malformed = 0

        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val row = splitCsvLine(line)
            try {
                val date = java.time.LocalDateTime.parse(row[startTimeIdx], formatter).toLocalDate()
                val exercise = row[exerciseIdx]
                if (exercise.isBlank()) {
                    malformed++
                    continue
                }

                val weightKg = weightIdx?.let { row.getOrNull(it)?.toFloatOrNull() }
                val reps = repsIdx?.let { row.getOrNull(it)?.toIntOrNull() }
                val distanceKm = distanceIdx?.let { row.getOrNull(it)?.toFloatOrNull() }
                val duration = durationIdx?.let { row.getOrNull(it)?.toIntOrNull() }
                val rpe = rpeIdx?.let { row.getOrNull(it)?.toFloatOrNull()?.toInt() }

                val isCardio = (distanceKm != null || duration != null) && weightKg == null && reps == null

                sets += ImportedSet(
                    exerciseName = exercise,
                    performedAt = date,
                    weightKg = weightKg,
                    reps = reps,
                    distanceMeters = distanceKm?.let { it * 1000f },
                    durationSeconds = duration,
                    effortValue = rpe,
                    effortScale = if (rpe != null) "RPE" else null,
                    isCardio = isCardio
                )
            } catch (e: Exception) {
                malformed++
            }
        }

        return ImportResult(sets, malformed)
    }
}
