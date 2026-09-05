package com.example.gymformcoach.core.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A one-off change for a specific calendar date (§2.2). Takes precedence over
 * the recurring [WeeklyPlanEntry].
 *
 * A row with a null routineId is an *explicit* rest day for that date — which
 * is why "no override" and "override to rest" have to be distinguishable, and
 * why resolution checks for row existence rather than for a non-null routineId.
 */
@Entity(
    tableName = "plan_overrides",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["epochDay"], unique = true), Index("routineId")]
)
data class PlanOverride(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val epochDay: Long, // LocalDate.toEpochDay()
    val routineId: String? = null, // null explicitly means "rest this date"
    val syncStatus: String = SyncStatus.SYNCED,
    val serverId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
