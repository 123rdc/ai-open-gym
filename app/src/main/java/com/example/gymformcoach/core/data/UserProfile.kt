package com.example.gymformcoach.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val gender: String,
    val age: Int,
    val weightKg: Float,
    val heightCm: Float,
    val fitnessGoal: String,
    val activityLevel: String,
    val trainingExperience: String,
    val equipment: Set<String>,
    val trainingSplit: String,
    val focusAreas: Set<String>,
    val sessionLengthPreference: String,
    val trainingLimitations: String,
    val syncStatus: String = SyncStatus.SYNCED,
    val serverId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
