package com.example.gymformcoach.core.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseSession::class,
            parentColumns = ["id"],
            childColumns = ["exerciseSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseSessionId")]
)
data class SetLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val exerciseSessionId: String,
    val setIndex: Int,
    val weightKg: Float? = null,       // null for bodyweight without added load, null for cardio
    val addedWeightKg: Float? = null,  // dip belt / vest on a bodyweight exercise
    val reps: Int? = null,             // null for TIMED and CARDIO
    val durationSeconds: Int? = null,  // TIMED: time held. CARDIO: session duration
    val distanceMeters: Float? = null, // CARDIO only
    val effortValue: Int? = null,      // §10, nullable
    val effortScale: String? = null,   // "RIR" | "RPE" | null
    val isPr: Boolean = false,         // computed at write time, §8
    val syncStatus: String = SyncStatus.SYNCED,
    val serverId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
