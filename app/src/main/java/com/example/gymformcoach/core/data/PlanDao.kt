package com.example.gymformcoach.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeeklyEntry(entry: WeeklyPlanEntry)

    @Query("DELETE FROM weekly_plan_entries WHERE dayOfWeek = :dayOfWeek")
    suspend fun deleteWeeklyEntry(dayOfWeek: Int)

    @Query("SELECT * FROM weekly_plan_entries ORDER BY dayOfWeek ASC")
    fun getWeeklyPlan(): Flow<List<WeeklyPlanEntry>>

    @Query("SELECT * FROM weekly_plan_entries WHERE dayOfWeek = :dayOfWeek LIMIT 1")
    suspend fun getWeeklyEntryOnce(dayOfWeek: Int): WeeklyPlanEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(override: PlanOverride)

    @Query("DELETE FROM plan_overrides WHERE epochDay = :epochDay")
    suspend fun deleteOverride(epochDay: Long)

    @Query("SELECT * FROM plan_overrides WHERE epochDay = :epochDay LIMIT 1")
    suspend fun getOverrideOnce(epochDay: Long): PlanOverride?

    @Query("SELECT * FROM plan_overrides WHERE epochDay >= :fromEpochDay ORDER BY epochDay ASC")
    fun getOverridesFrom(fromEpochDay: Long): Flow<List<PlanOverride>>
}
