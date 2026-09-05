package com.example.gymformcoach.features.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.Exercise
import com.example.gymformcoach.core.data.ExerciseRepository
import com.example.gymformcoach.core.data.ExerciseSession
import com.example.gymformcoach.core.data.ExerciseSessionRepository
import com.example.gymformcoach.core.data.ExerciseType
import com.example.gymformcoach.core.data.LoadType
import com.example.gymformcoach.core.data.ProgressionRepository
import com.example.gymformcoach.core.data.Routine
import com.example.gymformcoach.core.data.RoutineExercise
import com.example.gymformcoach.core.data.SetLog
import com.example.gymformcoach.core.data.SetLogRepository
import com.example.gymformcoach.core.progression.PersonalRecords
import com.example.gymformcoach.core.progression.ProgressionTarget
import com.example.gymformcoach.core.progression.SessionRecord
import com.example.gymformcoach.core.progression.SetRecord
import com.example.gymformcoach.core.utils.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** One editable row in the set-logging table. */
data class SetRow(
    val id: String = UUID.randomUUID().toString(),
    val weightKg: Float,
    val reps: Int,
    val effortValue: Int? = null,
    val isWarmup: Boolean = false,
    val completed: Boolean = false,
    val wasPr: Boolean = false
)

data class SetLoggingUiState(
    val exercise: Exercise? = null,
    val rows: List<SetRow> = emptyList(),
    val target: ProgressionTarget? = null,
    val lastTimeSummary: String? = null,
    val bestWeightKg: Float? = null,
    val effortScale: String = "RIR",
    val effortEnabled: Boolean = false
)

/**
 * Drives the multi-set logging table for one exercise (§3/§5) - the manual
 * counterpart to CameraViewModel's pose-tracked flow, used for the 111
 * exercises this catalog has no camera tracking for. Pre-fills every row from
 * the progression engine's target and writes each completed row through the
 * same ExerciseSession+SetLog+PR pipeline the camera flow uses.
 */
class SetLoggingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val exerciseSessionRepository = ExerciseSessionRepository(database)
    private val setLogRepository = SetLogRepository(database)
    private val exerciseRepository = ExerciseRepository(database)
    private val progressionRepository = ProgressionRepository(database)
    private val prefs = PreferenceManager(application)

    private val _uiState = MutableStateFlow(SetLoggingUiState())
    val uiState: StateFlow<SetLoggingUiState> = _uiState.asStateFlow()

    fun load(routineExercise: RoutineExercise, routine: Routine?) {
        viewModelScope.launch {
            val exercise = exerciseRepository.findByName(routineExercise.exerciseId)
            val target = progressionRepository.nextTarget(routineExercise, routine)
            val history = sessionHistory(routineExercise.exerciseId)
            val best = PersonalRecords.currentBest(
                history,
                exercise?.exerciseType ?: ExerciseType.REPS,
                exercise?.loadType ?: LoadType.WEIGHTED
            ) as? PersonalRecords.Record.Weight

            val rowWeight = target?.weightKg ?: routineExercise.targetWeightKg
            val rowReps = target?.reps ?: routineExercise.targetReps
            val rows = List(routineExercise.targetSets) {
                SetRow(weightKg = rowWeight, reps = rowReps)
            }

            _uiState.value = SetLoggingUiState(
                exercise = exercise,
                rows = rows,
                target = target,
                lastTimeSummary = lastTimeSummary(history),
                bestWeightKg = best?.weightKg,
                effortScale = prefs.effortScale,
                effortEnabled = prefs.effortTrackingEnabled
            )
        }
    }

    private suspend fun sessionHistory(exerciseName: String): List<SessionRecord> {
        val sessions = exerciseSessionRepository.getSessionsForExerciseOnce(exerciseName)
            .sortedByDescending { it.performedAt }
        return sessions.map { session ->
            val logs = setLogRepository.getForSessionOnce(session.id)
            val sets = if (logs.isNotEmpty()) {
                logs.map { SetRecord(it.weightKg, it.addedWeightKg, it.reps, it.durationSeconds) }
            } else {
                listOf(SetRecord(weightKg = session.weightKg.takeIf { it > 0f }, reps = session.reps))
            }
            SessionRecord(session.performedAt, sets)
        }
    }

    /** §3.1-adjacent convenience: "Last time: 92.5x8, 92.5x8, 92.5x7". */
    private fun lastTimeSummary(history: List<SessionRecord>): String? {
        val last = history.firstOrNull() ?: return null
        val parts = last.sets.joinToString(", ") { set ->
            val weight = set.effectiveLoadKg
            val reps = set.reps
            when {
                weight != null && reps != null -> "${formatWeight(weight)}x$reps"
                reps != null -> "$reps reps"
                set.durationSeconds != null -> "${set.durationSeconds}s"
                else -> "-"
            }
        }
        return parts
    }

    private fun formatWeight(w: Float) = if (w % 1f == 0f) w.toInt().toString() else "%.1f".format(w)

    fun updateRow(rowId: String, weightKg: Float? = null, reps: Int? = null, effortValue: Int? = null) {
        val rows = _uiState.value.rows.map { row ->
            if (row.id == rowId) {
                row.copy(
                    weightKg = weightKg ?: row.weightKg,
                    reps = reps ?: row.reps,
                    effortValue = effortValue ?: row.effortValue
                )
            } else row
        }
        _uiState.value = _uiState.value.copy(rows = rows)
    }

    fun addSet() {
        val last = _uiState.value.rows.lastOrNull()
        val newRow = SetRow(
            weightKg = last?.weightKg ?: 0f,
            reps = last?.reps ?: 0
        )
        _uiState.value = _uiState.value.copy(rows = _uiState.value.rows + newRow)
    }

    fun addWarmupSet() {
        val first = _uiState.value.rows.firstOrNull()
        val warmup = SetRow(
            weightKg = ((first?.weightKg ?: 20f) * 0.5f),
            reps = first?.reps ?: 10,
            isWarmup = true
        )
        _uiState.value = _uiState.value.copy(rows = listOf(warmup) + _uiState.value.rows)
    }

    fun removeSet(rowId: String) {
        _uiState.value = _uiState.value.copy(rows = _uiState.value.rows.filterNot { it.id == rowId })
    }

    /** Marks a row complete and persists it through the same pipeline the camera flow uses. */
    fun completeSet(rowId: String, exerciseName: String) {
        val state = _uiState.value
        val row = state.rows.firstOrNull { it.id == rowId } ?: return
        if (row.completed) return

        viewModelScope.launch {
            val history = sessionHistory(exerciseName)
            val candidate = SetRecord(weightKg = row.weightKg, reps = row.reps)
            val isPr = if (row.isWarmup) false else {
                PersonalRecords.isPersonalRecord(
                    history, candidate,
                    state.exercise?.exerciseType ?: ExerciseType.REPS,
                    state.exercise?.loadType ?: LoadType.WEIGHTED
                )
            }

            val session = ExerciseSession(
                exerciseId = exerciseName,
                performedAt = System.currentTimeMillis(),
                weightKg = row.weightKg,
                reps = row.reps,
                sets = 1,
                volumeScore = row.weightKg * row.reps
            )
            exerciseSessionRepository.logSession(session)
            setLogRepository.logSet(
                SetLog(
                    exerciseSessionId = session.id,
                    setIndex = state.rows.indexOf(row),
                    weightKg = row.weightKg,
                    reps = row.reps,
                    effortValue = row.effortValue,
                    effortScale = row.effortValue?.let { prefs.effortScale },
                    isPr = isPr
                )
            )

            _uiState.value = _uiState.value.copy(
                rows = _uiState.value.rows.map {
                    if (it.id == rowId) it.copy(completed = true, wasPr = isPr) else it
                }
            )
        }
    }
}
