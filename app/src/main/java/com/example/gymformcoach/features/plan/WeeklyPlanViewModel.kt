package com.example.gymformcoach.features.plan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.PlanRepository
import com.example.gymformcoach.core.data.PlannedDay
import com.example.gymformcoach.core.data.RoutineRepository
import com.example.gymformcoach.core.data.RoutineSummary
import com.example.gymformcoach.core.data.WeeklyPlanEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class WeeklyPlanUiState(
    val entriesByDay: Map<Int, String?> = emptyMap(), // dayOfWeek -> routineId (null = rest)
    val routines: List<RoutineSummary> = emptyList(),
    val today: PlannedDay = PlannedDay.Unplanned,
    val todayRoutineName: String? = null
)

class WeeklyPlanViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val planRepository = PlanRepository(database)
    private val routineRepository = RoutineRepository(database)

    private val _uiState = MutableStateFlow(WeeklyPlanUiState())
    val uiState: StateFlow<WeeklyPlanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                planRepository.getWeeklyPlan(),
                routineRepository.getRoutineSummaries()
            ) { entries: List<WeeklyPlanEntry>, routines: List<RoutineSummary> ->
                entries to routines
            }.collect { (entries, routines) ->
                val today = planRepository.resolveDay()
                val todayName = (today as? PlannedDay.Workout)
                    ?.let { planned -> routines.firstOrNull { it.id == planned.routineId }?.name }
                _uiState.value = WeeklyPlanUiState(
                    entriesByDay = entries.associate { it.dayOfWeek to it.routineId },
                    routines = routines,
                    today = today,
                    todayRoutineName = todayName
                )
            }
        }
    }

    fun assignDay(dayOfWeek: Int, routineId: String?) {
        viewModelScope.launch { planRepository.assignDay(dayOfWeek, routineId) }
    }

    fun clearDay(dayOfWeek: Int) {
        viewModelScope.launch { planRepository.clearDay(dayOfWeek) }
    }

    /** §2.3: writes two overrides, never mutates the recurring template. */
    fun reschedule(to: LocalDate, routineId: String) {
        viewModelScope.launch { planRepository.reschedule(LocalDate.now(), to, routineId) }
    }
}
