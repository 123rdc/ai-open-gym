package com.example.gymformcoach.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "exercise_sessions")
data class ExerciseSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val exerciseId: String,
    val performedAt: Long,
    val weightKg: Float,
    val reps: Int,
    val sets: Int,
    val volumeScore: Float,
    val syncStatus: String = SyncStatus.SYNCED,
    val serverId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
