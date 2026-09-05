package com.example.gymformcoach.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * An in-progress guided session (§3.3). Persisted on every set completion so a
 * session survives process death — Android will kill a backgrounded app with an
 * active camera, and losing a half-finished workout is unacceptable.
 *
 * Only one is live at a time; [SINGLETON_ID] keeps that invariant in the schema
 * rather than relying on callers to enforce it.
 */
@Entity(tableName = "active_sessions")
data class ActiveSession(
    @PrimaryKey val id: String = SINGLETON_ID,
    val routineId: String,
    val startedAt: Long,
    val currentExerciseIndex: Int,
    val completedSetIds: List<String> = emptyList(),
    val syncStatus: String = SyncStatus.SYNCED,
    val serverId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SINGLETON_ID = "active"

        /** §3.3: offer to resume only if it started within this window. */
        const val RESUMABLE_WINDOW_MS = 6 * 60 * 60 * 1000L
    }
}
