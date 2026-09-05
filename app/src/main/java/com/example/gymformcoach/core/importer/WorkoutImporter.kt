package com.example.gymformcoach.core.importer

import java.io.InputStream
import java.time.LocalDate

/** One set, as read from an external source before any matching/normalization. */
data class ImportedSet(
    val exerciseName: String,
    val performedAt: LocalDate,
    val weightKg: Float? = null,
    val reps: Int? = null,
    val distanceMeters: Float? = null,
    val durationSeconds: Int? = null,
    val effortValue: Int? = null,
    val effortScale: String? = null,
    /** True when the row's shape (distance/time, no weight/reps) marks it cardio. */
    val isCardio: Boolean = false
)

data class ImportResult(
    val sets: List<ImportedSet>,
    val malformedRowCount: Int = 0
)

/**
 * §17C.1: one pipeline, one parser per source. Only [parse] differs between
 * FitNotes/Strong/Hevy - everything downstream (matching, unit normalization,
 * duplicate detection, preview, commit) is shared in [ImportPipeline].
 */
interface WorkoutImporter {
    val sourceName: String

    /**
     * §17C.3: true when this parser was written against documented/known column
     * names rather than a verified real export - the UI must show this plainly
     * rather than presenting an unverified guess as equally trustworthy.
     */
    val isExperimental: Boolean get() = false

    /** Sniffs the file's header line to decide whether this parser applies. */
    fun canParse(header: String): Boolean

    fun parse(input: InputStream): ImportResult
}

fun lbsToKg(lbs: Float): Float = lbs * 0.45359237f
