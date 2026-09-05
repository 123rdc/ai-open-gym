package com.example.gymformcoach.features.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.BodyWeightEntry
import com.example.gymformcoach.core.data.BodyWeightRepository
import com.example.gymformcoach.core.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class BodyWeightRange(val days: Long?) {
    MONTH(30), SIX_MONTHS(182), YEAR(365), ALL(null)
}

data class BodyWeightUiState(
    val entries: List<BodyWeightEntry> = emptyList(),
    val goalWeightKg: Float? = null,
    val range: BodyWeightRange = BodyWeightRange.MONTH
)

class BodyWeightViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val bodyWeightRepository = BodyWeightRepository(database)
    private val profileRepository = ProfileRepository(database)

    private val _range = MutableStateFlow(BodyWeightRange.MONTH)

    val uiState: StateFlow<BodyWeightUiState> = combine(
        bodyWeightRepository.getAll(),
        profileRepository.observeProfile(),
        _range
    ) { entries, profile, range ->
        val filtered = range.days?.let { days ->
            val cutoff = LocalDate.now().minusDays(days).toEpochDay()
            entries.filter { it.epochDay >= cutoff }
        } ?: entries
        BodyWeightUiState(entries = filtered, goalWeightKg = profile?.goalWeightKg, range = range)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BodyWeightUiState())

    fun setRange(range: BodyWeightRange) {
        _range.value = range
    }

    fun recordEntry(
        weightKg: Float,
        date: LocalDate = LocalDate.now(),
        source: String = com.example.gymformcoach.core.data.BodyWeightSource.MANUAL
    ) {
        viewModelScope.launch { bodyWeightRepository.record(weightKg, date, source) }
    }

    fun setGoal(goalWeightKg: Float?) {
        viewModelScope.launch { profileRepository.updateGoalWeight(goalWeightKg) }
    }
}
