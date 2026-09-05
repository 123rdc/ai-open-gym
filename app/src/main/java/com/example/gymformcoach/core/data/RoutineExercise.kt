package com.example.gymformcoach.core.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId")]
)
data class RoutineExercise(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val routineId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val targetWeightKg: Float,
    val targetReps: Int,
    val targetSets: Int,
    /** §4.1: UUID shared by grouped exercises; null = standalone. */
    val supersetGroupId: String? = null,
    /** §5.1 resolution: this override → the routine's default → NONE. */
    val progressionRuleOverride: String? = null,
    val incrementKg: Float = 2.5f,
    val repRangeMin: Int = 8,
    val repRangeMax: Int = 12,
    val targetDurationSeconds: Int = 30,
    val syncStatus: String = SyncStatus.SYNCED,
    val serverId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
