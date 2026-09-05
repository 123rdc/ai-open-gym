package com.example.gymformcoach.features.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.ExerciseRepository
import com.example.gymformcoach.core.data.ExerciseSessionRepository
import com.example.gymformcoach.core.data.SetLogRepository
import com.example.gymformcoach.core.progression.MuscleFatigue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class EffortWindow(val days: Long?) { THIRTY(30), NINETY(90), YEAR(365), ALL(null) }

data class EffortStats(
    val averageEffort: Double?,
    val percentAtRirThreeOrHarder: Int,
    val ratedSets: Int,
    val totalSets: Int
)

data class MuscleFatigueUiState(
    val fatigueByGroup: List<MuscleFatigue.GroupFatigue> = emptyList(),
    val effortStats: EffortStats = EffortStats(null, 0, 0, 0),
    val effortWindow: EffortWindow = EffortWindow.NINETY
)

/**
 * §13.2 (fatigue axis) + the effort/RIR stats section shown alongside it.
 * Both are real numbers computed from logged sets - no fabricated data (§18.5).
 */
class MuscleFatigueViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val sessionRepository = ExerciseSessionRepository(database)
    private val setLogRepository = SetLogRepository(database)
    private val exerciseRepository = ExerciseRepository(database)

    private val _effortWindow = MutableStateFlow(EffortWindow.NINETY)
    private val _uiState = MutableStateFlow(MuscleFatigueUiState())
    val uiState: StateFlow<MuscleFatigueUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                sessionRepository.getAllSessions(),
                exerciseRepository.getAll(),
                _effortWindow
            ) { sessions, exercises, window ->
                val exerciseByName = exercises.associateBy { it.name }
                val trainedAt = mutableMapOf<String, MutableList<Long>>()
                sessions.forEach { session ->
                    val groups = exerciseByName[session.exerciseId]?.muscleGroups.orEmpty()
                    groups.flatMap { normalizeMuscleGroup(it) }.forEach { region ->
                        trainedAt.getOrPut(region) { mutableListOf() }.add(session.performedAt)
                    }
                }
                val fatigue = MuscleFatigue.calculate(trainedAt)

                val cutoff = window.days?.let { System.currentTimeMillis() - it * 24L * 60 * 60 * 1000 }
                val relevantSessions = if (cutoff != null) sessions.filter { it.performedAt >= cutoff } else sessions
                val allSetLogs = relevantSessions.flatMap { setLogRepository.getForSessionOnce(it.id) }
                val rated = allSetLogs.filter { it.effortValue != null }
                val avgEffort = rated.mapNotNull { it.effortValue }.average().takeIf { rated.isNotEmpty() }
                val atThreeOrHarder = rated.count { log ->
                    // RIR: lower = harder (<=3 is hard). RPE: higher = harder (>=7 maps to RIR<=3).
                    val v = log.effortValue!!
                    if (log.effortScale == "RPE") v >= 7 else v <= 3
                }
                val percent = if (rated.isNotEmpty()) (atThreeOrHarder * 100 / rated.size) else 0

                MuscleFatigueUiState(
                    fatigueByGroup = fatigue,
                    effortStats = EffortStats(avgEffort, percent, rated.size, allSetLogs.size),
                    effortWindow = window
                )
            }.collect { _uiState.value = it }
        }
    }

    fun setEffortWindow(window: EffortWindow) {
        _effortWindow.value = window
    }

    /**
     * `Exercise.muscleGroups` is seeded as free text ("Lats & Lower Back", "Upper
     * Chest") rather than a fixed taxonomy - map it to the diagram's canonical
     * regions by keyword. A string can hit more than one region (e.g. "Chest &
     * Triceps"), and one that hits none is simply not shown, not miscounted.
     */
    private fun normalizeMuscleGroup(raw: String): List<String> {
        val text = raw.lowercase()
        val regions = mutableListOf<String>()
        if ("chest" in text) regions += "Chest"
        if ("shoulder" in text || "delt" in text || "traps" in text) regions += "Shoulders"
        if ("bicep" in text || "brachial" in text) regions += "Biceps"
        if ("tricep" in text) regions += "Triceps"
        if ("ab" in text || "core" in text || "oblique" in text) regions += "Abs"
        if ("back" in text || "lat" in text) regions += "Back"
        if ("quad" in text) regions += "Quads"
        if ("hamstring" in text) regions += "Hamstrings"
        if ("calv" in text) regions += "Calves"
        if ("glute" in text) regions += "Glutes"
        return regions
    }
}
