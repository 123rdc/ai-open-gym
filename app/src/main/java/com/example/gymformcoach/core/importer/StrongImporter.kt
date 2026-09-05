package com.example.gymformcoach.core.importer

import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * §17C.2 Strong: CSV export, row-per-set like FitNotes, but with its own
 * column names, a combined date-time field, and a workout-name column
 * FitNotes lacks.
 *
 * §17C.3: EXPERIMENTAL - written against Strong's documented/publicly-known
 * export format, not verified against a real sample file. Ship behind the
 * "experimental" label rather than presenting it as verified; if a real
 * export's columns differ, this parser needs correcting against one.
 */
class StrongImporter : WorkoutImporter {
    override val sourceName = "Strong"
    override val isExperimental = true

    private val expectedColumns = setOf("Date", "Workout Name", "Exercise Name", "Weight", "Reps")

    override fun canParse(header: String): Boolean {
        val cols = splitCsvLine(header).map { it.trim() }
        return expectedColumns.all { it in cols }
    }

    override fun parse(input: InputStream): ImportResult {
        val lines = input.bufferedReader().readLines()
        if (lines.isEmpty()) return ImportResult(emptyList())

        val header = splitCsvLine(lines.first())
        fun col(name: String) = header.indexOf(name).takeIf { it >= 0 }

        val dateIdx = col("Date") ?: return ImportResult(emptyList(), lines.size - 1)
        val exerciseIdx = col("Exercise Name") ?: return ImportResult(emptyList(), lines.size - 1)
        val weightIdx = col("Weight")
        val weightUnitIdx = col("Weight Unit")
        val repsIdx = col("Reps")
        val distanceIdx = col("Distance")
        val secondsIdx = col("Seconds")
        val rpeIdx = col("RPE")

        // Combined date-time, e.g. "2023-01-15 14:30:00" - date portion only matters for a session key.
        val dateFormatters = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE
        )

        val sets = mutableListOf<ImportedSet>()
        var malformed = 0

        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val row = splitCsvLine(line)
            try {
                val rawDate = row[dateIdx]
                val date = parseDate(rawDate, dateFormatters) ?: throw IllegalArgumentException("unparseable date: $rawDate")
                val exercise = row[exerciseIdx]
                if (exercise.isBlank()) {
                    malformed++
                    continue
                }

                val rawWeight = weightIdx?.let { row.getOrNull(it)?.toFloatOrNull() }
                val unit = weightUnitIdx?.let { row.getOrNull(it) } ?: "kg"
                val weightKg = rawWeight?.let { if (unit.equals("lb", true) || unit.equals("lbs", true)) lbsToKg(it) else it }
                val reps = repsIdx?.let { row.getOrNull(it)?.toIntOrNull() }
                val distance = distanceIdx?.let { row.getOrNull(it)?.toFloatOrNull() }
                val seconds = secondsIdx?.let { row.getOrNull(it)?.toIntOrNull() }
                val rpe = rpeIdx?.let { row.getOrNull(it)?.toFloatOrNull()?.toInt() }

                val isCardio = (distance != null || seconds != null) && weightKg == null && reps == null

                sets += ImportedSet(
                    exerciseName = exercise,
                    performedAt = date,
                    weightKg = weightKg,
                    reps = reps,
                    distanceMeters = distance,
                    durationSeconds = seconds,
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

    private fun parseDate(raw: String, formatters: List<DateTimeFormatter>): LocalDate? {
        for (f in formatters) {
            try {
                return if (f == DateTimeFormatter.ISO_LOCAL_DATE) LocalDate.parse(raw, f)
                else java.time.LocalDateTime.parse(raw, f).toLocalDate()
            } catch (_: Exception) { }
        }
        return null
    }
}
