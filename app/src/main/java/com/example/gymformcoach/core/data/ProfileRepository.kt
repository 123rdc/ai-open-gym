package com.example.gymformcoach.core.data

import com.example.gymformcoach.core.utils.PreferenceManager
import kotlinx.coroutines.flow.Flow

class ProfileRepository(database: AppDatabase) {
    private val dao = database.userProfileDao()

    fun observeProfile(): Flow<UserProfile?> = dao.getProfile()

    suspend fun getProfile(): UserProfile? = dao.getProfileOnce()

    /**
     * Snapshots the setup-flow fields out of SharedPreferences into the syncable Room profile.
     * Called once onboarding/setup finishes (and again if the user redoes it later).
     */
    suspend fun syncFromPreferences(preferenceManager: PreferenceManager) {
        val existing = dao.getProfileOnce()
        val snapshot = UserProfile(
            id = existing?.id ?: java.util.UUID.randomUUID().toString(),
            gender = preferenceManager.gender,
            age = preferenceManager.age,
            weightKg = preferenceManager.weightKg,
            heightCm = preferenceManager.heightCm,
            fitnessGoal = preferenceManager.fitnessGoal,
            activityLevel = preferenceManager.activityLevel,
            trainingExperience = preferenceManager.trainingExperience,
            equipment = preferenceManager.equipment,
            trainingSplit = preferenceManager.trainingSplit,
            focusAreas = preferenceManager.focusAreas,
            sessionLengthPreference = preferenceManager.sessionLengthPreference,
            trainingLimitations = preferenceManager.trainingLimitations,
            syncStatus = existing?.syncStatus ?: SyncStatus.SYNCED,
            serverId = existing?.serverId,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        if (existing == null) dao.insert(snapshot) else dao.update(snapshot)
    }
}
