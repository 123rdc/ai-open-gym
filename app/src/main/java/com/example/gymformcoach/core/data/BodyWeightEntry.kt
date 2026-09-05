package com.example.gymformcoach.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

object BodyWeightSource {
    const val MANUAL = "MANUAL"
    const val SESSION_PROMPT = "SESSION_PROMPT"
    const val IMPORT = "IMPORT"
}

/**
 * §17A.1. One entry per day maximum — a second weigh-in on the same date is a
 * correction, not a second data point, so [epochDay] is unique and writes
 * replace rather than accumulate.
 */
@Entity(
    tableName = "body_weight_entries",
    indices = [Index(value = ["epochDay"], unique = true)]
)
data class BodyWeightEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val weightKg: Float,
    /** The date the measurement applies to, as LocalDate.toEpochDay(). */
    val epochDay: Long,
    val recordedAt: Long = System.currentTimeMillis(),
    val source: String = BodyWeightSource.MANUAL,
    val syncStatus: String = SyncStatus.SYNCED,
    val serverId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
