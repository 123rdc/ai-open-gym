package com.example.gymformcoach.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "exercises",
    indices = [Index(value = ["name"], unique = true)]
)
data class Exercise(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bodyPart: String,
    val muscle: String,
    val category: String, // "Free Weight", "Dumbbell", "Machine", "Cable", "Bodyweight", "Kettlebell"
    val duration: String,
    val difficulty: String,
    val imageUrl: String,
    val about: String,
    val exerciseType: ExerciseType = ExerciseType.REPS,
    val loadType: LoadType = LoadType.WEIGHTED,
    val isUnilateral: Boolean = false,
    val isCustom: Boolean = false,
    val muscleGroups: List<String> = emptyList(),
    val syncStatus: String = SyncStatus.SYNCED,
    val serverId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
