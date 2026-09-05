package com.example.gymformcoach.core.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * The recurring weekly template (§2.2). One row per weekday that has an
 * assignment; a missing row (or a null routineId) means rest.
 *
 * §2.5: a deleted routine must not crash the plan — ON DELETE SET NULL leaves
 * the slot unassigned rather than dangling.
 */
@Entity(
    tableName = "weekly_plan_entries",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["dayOfWeek"], unique = true), Index("routineId")]
)
data class WeeklyPlanEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dayOfWeek: Int, // java.time.DayOfWeek value, 1=Monday..7=Sunday
    val routineId: String? = null, // null = rest day
    val syncStatus: String = SyncStatus.SYNCED,
    val serverId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
