package com.example.gymformcoach.features.coach

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymformcoach.core.coach.CoachResult
import com.example.gymformcoach.core.coach.PlanValidator
import com.example.gymformcoach.core.coach.TrainingCoachRepository
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.utils.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CoachUiState {
    data object Idle : CoachUiState()
    data object Loading : CoachUiState()
    data class ProposalReady(val plan: PlanValidator.ValidatedPlan) : CoachUiState()
    data class RevisionReady(val revision: com.example.gymformcoach.core.coach.PlanRevision) : CoachUiState()
    data class Unavailable(val reason: String) : CoachUiState()
    data class Applied(val message: String) : CoachUiState()
}

class CoachViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TrainingCoachRepository(AppDatabase.getInstance(application), PreferenceManager(application))

    private val _uiState = MutableStateFlow<CoachUiState>(CoachUiState.Idle)
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    fun generatePlan() {
        _uiState.value = CoachUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = repository.generatePlan()) {
                is CoachResult.Ready -> CoachUiState.ProposalReady(result.plan)
                is CoachResult.Unavailable -> CoachUiState.Unavailable(result.reason)
                is CoachResult.Revision -> CoachUiState.Unavailable("Unexpected response type.")
            }
        }
    }

    fun revisePlan() {
        _uiState.value = CoachUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = repository.revisePlan()) {
                is CoachResult.Revision -> CoachUiState.RevisionReady(result.revision)
                is CoachResult.Unavailable -> CoachUiState.Unavailable(result.reason)
                is CoachResult.Ready -> CoachUiState.Unavailable("Unexpected response type.")
            }
        }
    }

    /** §17D.1: the user disposes - applying is always an explicit action, never automatic. */
    fun applyPlan(plan: PlanValidator.ValidatedPlan) {
        viewModelScope.launch {
            repository.applyPlan(plan)
            _uiState.value = CoachUiState.Applied("\"${plan.proposal.planName}\" added to My Routines.")
        }
    }

    /** §17D.3: each item is independently acceptable - only the accepted ones are passed in. */
    fun applyRevisionItems(items: List<com.example.gymformcoach.core.coach.PlanDiffItem>) {
        viewModelScope.launch {
            repository.applyRevision(items)
            _uiState.value = CoachUiState.Applied("${items.size} change(s) applied to your plan.")
        }
    }

    fun dismiss() {
        _uiState.value = CoachUiState.Idle
    }
}
