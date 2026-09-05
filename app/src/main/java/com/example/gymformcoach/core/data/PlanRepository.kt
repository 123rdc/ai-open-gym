package com.example.gymformcoach.core.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** What the app should surface for a given day (§2.2). */
sealed class PlannedDay {
    data class Workout(val routineId: String) : PlannedDay()
    object Rest : PlannedDay()
    object Unplanned : PlannedDay()
}

class PlanRepository(private val database: AppDatabase) {
    private val dao = database.planDao()

    fun getWeeklyPlan(): Flow<List<WeeklyPlanEntry>> = dao.getWeeklyPlan()

    fun getUpcomingOverrides(from: LocalDate = LocalDate.now()): Flow<List<PlanOverride>> =
        dao.getOverridesFrom(from.toEpochDay())

    suspend fun assignDay(dayOfWeek: Int, routineId: String?) {
        dao.upsertWeeklyEntry(WeeklyPlanEntry(dayOfWeek = dayOfWeek, routineId = routineId))
    }

    suspend fun clearDay(dayOfWeek: Int) = dao.deleteWeeklyEntry(dayOfWeek)

    /**
     * §2.2 resolution order: a `PlanOverride` for this date wins (including one
     * with a null routineId, which is an explicit rest day), then the recurring
     * weekly entry, then nothing planned.
     */
    suspend fun resolveDay(date: LocalDate = LocalDate.now()): PlannedDay {
        val override = dao.getOverrideOnce(date.toEpochDay())
        if (override != null) {
            return override.routineId?.let { PlannedDay.Workout(it) } ?: PlannedDay.Rest
        }
        val weekly = dao.getWeeklyEntryOnce(date.dayOfWeek.value)
            ?: return PlannedDay.Unplanned
        return weekly.routineId?.let { PlannedDay.Workout(it) } ?: PlannedDay.Rest
    }

    /**
     * §2.3: moving a session writes *two* overrides — a rest on the original
     * date and the routine on the target date. The recurring `WeeklyPlanEntry`
     * rows are never touched, so next week falls back to the normal pattern
     * automatically.
     */
    suspend fun reschedule(from: LocalDate, to: LocalDate, routineId: String) {
        dao.upsertOverride(PlanOverride(epochDay = from.toEpochDay(), routineId = null))
        dao.upsertOverride(PlanOverride(epochDay = to.toEpochDay(), routineId = routineId))
    }

    suspend fun clearOverride(date: LocalDate) = dao.deleteOverride(date.toEpochDay())
}
