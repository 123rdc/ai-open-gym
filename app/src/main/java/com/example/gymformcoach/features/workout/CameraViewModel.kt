package com.example.gymformcoach.features.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymformcoach.core.analysis.PostSetAnalysisRunner
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.ExerciseSession
import com.example.gymformcoach.core.data.ExerciseSessionRepository
import com.example.gymformcoach.core.ml.PoseUtils
import com.example.gymformcoach.core.utils.PrEstimator
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RepState {
    START, DOWN, BOTTOM, UP
}

data class RepData(
    val repNumber: Int,
    val durationMs: Long,
    val minPrimaryAngleDeg: Double,
    val minBackAngleDeg: Double?,
    val feedback: List<String>
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val exerciseSessionRepository = ExerciseSessionRepository(AppDatabase.getInstance(application))

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount.asStateFlow()

    private val _poseResult = MutableStateFlow<PoseLandmarkerResult?>(null)
    val poseResult: StateFlow<PoseLandmarkerResult?> = _poseResult.asStateFlow()

    private val _formFeedback = MutableStateFlow<String?>(null)
    val formFeedback: StateFlow<String?> = _formFeedback.asStateFlow()

    // Rep counting state
    private var currentRepState = RepState.START
    private var workoutType: String = "Squat"
    private var workoutWeight: Float = 0f
    private var repStartTime: Long = 0
    private var minKneeAngle: Double = 180.0
    private var minBackAngle: Double = 180.0
    private var minElbowAngle: Double = 180.0
    private val currentRepFeedback = mutableListOf<String>()

    // Buffer for recording sessions
    private val landmarkBuffer = mutableListOf<Pair<Long, PoseLandmarkerResult>>()
    private val completedReps = mutableListOf<RepData>()

    fun setWorkoutType(type: String) {
        workoutType = type
    }

    fun setWeight(weight: Float) {
        workoutWeight = weight
    }

    fun startRecording() {
        _isRecording.value = true
        completedReps.clear()
        landmarkBuffer.clear()
    }

    /**
     * Stops recording, persists the completed set, and reports whether it beat the exercise's
     * prior estimated-1RM best. Kicks off post-set LLM analysis in the background (does not block
     * this return). Returns (repsCompleted, isNewPr, exerciseSessionId - empty if no reps logged).
     */
    suspend fun stopRecordingAndLog(): Triple<Int, Boolean, String> {
        _isRecording.value = false
        processSessionResults()
        val reps = _repCount.value
        val repsSnapshot = completedReps.toList()
        val (isPr, sessionId) = if (reps > 0) logExerciseSessionAndCheckPr(reps) else (false to "")
        landmarkBuffer.clear()
        _repCount.value = 0
        currentRepState = RepState.START

        if (sessionId.isNotEmpty()) {
            val application = getApplication<Application>()
            viewModelScope.launch {
                PostSetAnalysisRunner.run(
                    context = application,
                    database = AppDatabase.getInstance(application),
                    sessionId = sessionId,
                    exerciseType = workoutType,
                    weightKg = workoutWeight,
                    reps = repsSnapshot
                )
            }
        }

        return Triple(reps, isPr, sessionId)
    }

    private suspend fun logExerciseSessionAndCheckPr(reps: Int): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val priorSessions = exerciseSessionRepository.getSessionsForExerciseOnce(workoutType)
        val priorBest = priorSessions.maxOfOrNull { PrEstimator.estimatedOneRepMax(it.weightKg, it.reps) } ?: 0f
        val newE1rm = PrEstimator.estimatedOneRepMax(workoutWeight, reps)

        val session = ExerciseSession(
            exerciseId = workoutType,
            date = System.currentTimeMillis(),
            weightKg = workoutWeight,
            reps = reps,
            sets = 1,
            volumeScore = workoutWeight * reps
        )
        exerciseSessionRepository.logSession(session)

        (priorSessions.isNotEmpty() && newE1rm > priorBest) to session.id
    }

    fun onPoseResult(result: PoseLandmarkerResult) {
        _poseResult.value = result
        if (_isRecording.value) {
            landmarkBuffer.add(System.currentTimeMillis() to result)
            processPoseForReps(result)
        }
    }

    private fun processPoseForReps(result: PoseLandmarkerResult) {
        if (result.landmarks().isEmpty()) return
        val landmarks = result.landmarks()[0]

        when (workoutType.lowercase()) {
            "squat" -> processSquat(landmarks)
            "bench press", "pushup" -> processPushup(landmarks)
        }
    }

    private fun processSquat(landmarks: List<NormalizedLandmark>) {
        val hip = landmarks[23]
        val knee = landmarks[25]
        val ankle = landmarks[27]
        val shoulder = landmarks[11]

        val kneeAngle = PoseUtils.calculateAngle(hip, knee, ankle)
        val backAngle = PoseUtils.calculateAngle(shoulder, hip, knee)

        if (kneeAngle < minKneeAngle) minKneeAngle = kneeAngle
        if (backAngle < minBackAngle) minBackAngle = backAngle

        // Basic form checks
        if (backAngle < 45) {
            _formFeedback.value = "Keep your chest up!"
            if (!currentRepFeedback.contains("Leaning too far forward")) {
                currentRepFeedback.add("Leaning too far forward")
            }
        }

        when (currentRepState) {
            RepState.START -> {
                if (kneeAngle < 160) {
                    currentRepState = RepState.DOWN
                    repStartTime = System.currentTimeMillis()
                    minKneeAngle = kneeAngle
                    minBackAngle = backAngle
                    currentRepFeedback.clear()
                    _formFeedback.value = "Descending..."
                }
            }
            RepState.DOWN -> {
                if (kneeAngle < 100) {
                    currentRepState = RepState.BOTTOM
                    _formFeedback.value = "Great depth!"
                }
            }
            RepState.BOTTOM -> {
                if (kneeAngle > 120) {
                    currentRepState = RepState.UP
                    _formFeedback.value = "Driving up!"
                }
            }
            RepState.UP -> {
                if (kneeAngle > 165) {
                    val duration = System.currentTimeMillis() - repStartTime
                    _repCount.value += 1
                    completedReps.add(
                        RepData(
                            repNumber = _repCount.value,
                            durationMs = duration,
                            minPrimaryAngleDeg = minKneeAngle,
                            minBackAngleDeg = minBackAngle,
                            feedback = ArrayList(currentRepFeedback)
                        )
                    )
                    currentRepState = RepState.START
                    _formFeedback.value = "Rep ${_repCount.value} Complete!"
                }
            }
        }
    }

    private fun processPushup(landmarks: List<NormalizedLandmark>) {
        val shoulder = landmarks[11]
        val elbow = landmarks[13]
        val wrist = landmarks[15]

        val elbowAngle = PoseUtils.calculateAngle(shoulder, elbow, wrist)
        if (elbowAngle < minElbowAngle) minElbowAngle = elbowAngle

        when (currentRepState) {
            RepState.START -> {
                if (elbowAngle < 160) {
                    currentRepState = RepState.DOWN
                    repStartTime = System.currentTimeMillis()
                    minElbowAngle = elbowAngle
                    _formFeedback.value = "Lowering..."
                }
            }
            RepState.DOWN -> {
                if (elbowAngle < 90) {
                    currentRepState = RepState.BOTTOM
                    _formFeedback.value = "Good depth!"
                }
            }
            RepState.BOTTOM -> {
                if (elbowAngle > 110) {
                    currentRepState = RepState.UP
                    _formFeedback.value = "Pushing!"
                }
            }
            RepState.UP -> {
                if (elbowAngle > 165) {
                    val duration = System.currentTimeMillis() - repStartTime
                    _repCount.value += 1
                    completedReps.add(
                        RepData(
                            repNumber = _repCount.value,
                            durationMs = duration,
                            minPrimaryAngleDeg = minElbowAngle,
                            minBackAngleDeg = null,
                            feedback = emptyList()
                        )
                    )
                    currentRepState = RepState.START
                    _formFeedback.value = "Rep ${_repCount.value} Done!"
                }
            }
        }
    }

    /**
     * Post-set analysis: Calculates session metrics from the buffered landmarks and completed reps.
     */
    private fun processSessionResults() {
        if (completedReps.isEmpty()) return

        val avgDuration = completedReps.map { it.durationMs }.average()
        val avgDepth = completedReps.map { it.minPrimaryAngleDeg }.average()
        val totalFeedbackCount = completedReps.sumOf { it.feedback.size }

        android.util.Log.i("CameraViewModel", "--- Session Summary ---")
        android.util.Log.i("CameraViewModel", "Total Reps: ${completedReps.size}")
        android.util.Log.i("CameraViewModel", "Avg Rep Speed: ${"%.2f".format(avgDuration / 1000.0)}s")
        android.util.Log.i("CameraViewModel", "Avg Min Angle: ${"%.1f".format(avgDepth)}°")
        android.util.Log.i("CameraViewModel", "Total Form Issues: $totalFeedbackCount")

        if (totalFeedbackCount > 0) {
            val mostCommonIssue = completedReps.flatMap { it.feedback }
                .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            android.util.Log.i("CameraViewModel", "Most Common Issue: $mostCommonIssue")
        }
    }

    fun setFeedback(feedback: String) {
        _formFeedback.value = feedback
    }
}
