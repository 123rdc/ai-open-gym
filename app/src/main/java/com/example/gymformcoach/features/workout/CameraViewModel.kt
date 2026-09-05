package com.example.gymformcoach.features.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymformcoach.core.analysis.PostSetAnalysisRunner
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.Exercise
import com.example.gymformcoach.core.data.ExerciseRepository
import com.example.gymformcoach.core.data.ExerciseSession
import com.example.gymformcoach.core.data.ExerciseSessionRepository
import com.example.gymformcoach.core.data.ExerciseType
import com.example.gymformcoach.core.data.LoadType
import com.example.gymformcoach.core.data.SetLog
import com.example.gymformcoach.core.data.SetLogRepository
import com.example.gymformcoach.core.ml.PoseUtils
import com.example.gymformcoach.core.progression.PersonalRecords
import com.example.gymformcoach.core.progression.SessionRecord
import com.example.gymformcoach.core.progression.SetRecord
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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

/** Exercise names with real pose-driven rep counting (§1.2 REPS+WEIGHTED, camera-tracked). */
val POSE_TRACKED_EXERCISES = setOf("squat", "bench press", "pushup", "push-ups")

/** §18.2: how long a rep can sit mid-motion (paused, racked, out of frame) before it's aborted. */
private const val STUCK_REP_TIMEOUT_MS = 4000L

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val exerciseSessionRepository = ExerciseSessionRepository(AppDatabase.getInstance(application))
    private val exerciseRepository = ExerciseRepository(AppDatabase.getInstance(application))
    private val setLogRepository = SetLogRepository(AppDatabase.getInstance(application))

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount.asStateFlow()

    private val _poseResult = MutableStateFlow<PoseLandmarkerResult?>(null)
    val poseResult: StateFlow<PoseLandmarkerResult?> = _poseResult.asStateFlow()

    private val _formFeedback = MutableStateFlow<String?>(null)
    val formFeedback: StateFlow<String?> = _formFeedback.asStateFlow()

    /** null while the catalog lookup is in flight; defaults to a REPS/WEIGHTED shape until then. */
    private val _exercise = MutableStateFlow<Exercise?>(null)
    val exercise: StateFlow<Exercise?> = _exercise.asStateFlow()

    private val _timerElapsedSeconds = MutableStateFlow(0)
    val timerElapsedSeconds: StateFlow<Int> = _timerElapsedSeconds.asStateFlow()

    private val _cardioDistanceMeters = MutableStateFlow(0f)
    val cardioDistanceMeters: StateFlow<Float> = _cardioDistanceMeters.asStateFlow()

    private val _addedWeightKg = MutableStateFlow<Float?>(null)
    val addedWeightKg: StateFlow<Float?> = _addedWeightKg.asStateFlow()

    /** §10: optional, off by default, informational only - never read by progression/PR/1RM/LLM. */
    private val _effortValue = MutableStateFlow<Int?>(null)
    val effortValue: StateFlow<Int?> = _effortValue.asStateFlow()

    /** §18.1: true while key landmarks are missing or below the confidence threshold. */
    private val _lowPoseConfidence = MutableStateFlow(false)
    val lowPoseConfidence: StateFlow<Boolean> = _lowPoseConfidence.asStateFlow()

    private var timerJob: Job? = null

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

    val isPoseTracked: Boolean
        get() = workoutType.lowercase() in POSE_TRACKED_EXERCISES

    fun setWorkoutType(type: String) {
        workoutType = type
        _exercise.value = null
        viewModelScope.launch {
            _exercise.value = exerciseRepository.findByName(type)
        }
    }

    fun setWeight(weight: Float) {
        workoutWeight = weight
    }

    fun setAddedWeight(weight: Float?) {
        _addedWeightKg.value = weight
    }

    fun setEffortValue(value: Int?) {
        _effortValue.value = value
    }

    /** Manual rep entry for exercises with no camera pose-tracking (§1.2/§1.3). */
    fun incrementManualReps() {
        _repCount.value += 1
    }

    fun decrementManualReps() {
        _repCount.value = (_repCount.value - 1).coerceAtLeast(0)
    }

    fun setCardioDistance(meters: Float) {
        _cardioDistanceMeters.value = meters
    }

    /** Work timer (§1.5) — distinct from the rest timer (§7). Count-up only for now. */
    fun startTimer() {
        if (timerJob?.isActive == true) return
        _timerElapsedSeconds.value = 0
        timerJob = viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(1000)
                _timerElapsedSeconds.value += 1
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun startRecording() {
        _isRecording.value = true
        completedReps.clear()
        landmarkBuffer.clear()
    }

    /**
     * Stops recording, persists the completed set (branching on the exercise's type/load shape,
     * §1.2), and reports whether it beat the exercise's prior estimated-1RM best. Kicks off
     * post-set LLM analysis in the background (does not block this return, and only for REPS
     * exercises with pose-derived rep data). Returns (repsCompleted, isNewPr, exerciseSessionId -
     * empty if nothing was logged).
     */
    suspend fun stopRecordingAndLog(): Triple<Int, Boolean, String> {
        _isRecording.value = false
        stopTimer()
        processSessionResults()
        val type = _exercise.value?.exerciseType ?: ExerciseType.REPS
        val loadType = _exercise.value?.loadType ?: LoadType.WEIGHTED
        val repsSnapshot = completedReps.toList()

        val (reps, isPr, sessionId) = when (type) {
            ExerciseType.TIMED -> logTimedSet()
            ExerciseType.CARDIO -> logCardioSet()
            ExerciseType.REPS -> logRepsSet(loadType)
        }

        landmarkBuffer.clear()
        _repCount.value = 0
        _timerElapsedSeconds.value = 0
        _cardioDistanceMeters.value = 0f
        _effortValue.value = null
        currentRepState = RepState.START

        if (sessionId.isNotEmpty() && type == ExerciseType.REPS) {
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

    /** Prior sessions for the current exercise, converted for the pure PR/progression engine. */
    private suspend fun sessionHistory(): List<SessionRecord> {
        val sessions = exerciseSessionRepository.getSessionsForExerciseOnce(workoutType)
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

    private suspend fun logRepsSet(loadType: LoadType): Triple<Int, Boolean, String> = withContext(Dispatchers.IO) {
        val reps = _repCount.value
        if (reps <= 0) return@withContext Triple(0, false, "")

        val added = _addedWeightKg.value
        // §5.2.6/§6.1: a bodyweight set with no added load isn't eligible for weighted-1RM
        // comparison - stored as weightKg=0 on the aggregate session either way; SetLog carries
        // the real addedWeightKg for the progression/PR engine to read.
        val effectiveWeight = when (loadType) {
            LoadType.BODYWEIGHT -> added ?: 0f
            LoadType.WEIGHTED -> workoutWeight
        }

        // §8: shape-aware PR criterion (weighted load/1RM, bodyweight reps, or
        // reverts to weighted once a bodyweight set carries added load).
        val history = sessionHistory()
        val candidate = SetRecord(
            weightKg = if (loadType == LoadType.WEIGHTED) workoutWeight else null,
            addedWeightKg = if (loadType == LoadType.BODYWEIGHT) added else null,
            reps = reps
        )
        val isPr = PersonalRecords.isPersonalRecord(history, candidate, ExerciseType.REPS, loadType)

        val session = ExerciseSession(
            exerciseId = workoutType,
            performedAt = System.currentTimeMillis(),
            weightKg = effectiveWeight,
            reps = reps,
            sets = 1,
            volumeScore = effectiveWeight * reps
        )
        exerciseSessionRepository.logSession(session)
        val prefs = com.example.gymformcoach.core.utils.PreferenceManager(getApplication())
        setLogRepository.logSet(
            SetLog(
                exerciseSessionId = session.id,
                setIndex = 0,
                weightKg = if (loadType == LoadType.WEIGHTED) workoutWeight else null,
                addedWeightKg = if (loadType == LoadType.BODYWEIGHT) added else null,
                reps = reps,
                effortValue = _effortValue.value,
                effortScale = _effortValue.value?.let { prefs.effortScale },
                isPr = isPr
            )
        )

        Triple(reps, isPr, session.id)
    }

    private suspend fun logTimedSet(): Triple<Int, Boolean, String> = withContext(Dispatchers.IO) {
        val durationSeconds = _timerElapsedSeconds.value
        if (durationSeconds <= 0) return@withContext Triple(0, false, "")

        // §8.1: timed PR criterion is longest duration held, not weight-based 1RM.
        val history = sessionHistory()
        val candidate = SetRecord(weightKg = _addedWeightKg.value, durationSeconds = durationSeconds)
        val isPr = PersonalRecords.isPersonalRecord(history, candidate, ExerciseType.TIMED, LoadType.BODYWEIGHT)

        // ExerciseSession has no durationSeconds column of its own (Phase A keeps the existing
        // aggregate schema; SetLog is the real source of truth for TIMED data). Store the
        // duration in `reps` so existing "most recent value" reads stay non-garbage until Phase
        // C/E's history views are updated to read SetLog directly for TIMED/CARDIO exercises.
        val session = ExerciseSession(
            exerciseId = workoutType,
            performedAt = System.currentTimeMillis(),
            weightKg = _addedWeightKg.value ?: 0f,
            reps = durationSeconds,
            sets = 1,
            volumeScore = 0f
        )
        exerciseSessionRepository.logSession(session)
        setLogRepository.logSet(
            SetLog(
                exerciseSessionId = session.id,
                setIndex = 0,
                weightKg = _addedWeightKg.value,
                durationSeconds = durationSeconds,
                isPr = isPr
            )
        )

        Triple(0, isPr, session.id)
    }

    private suspend fun logCardioSet(): Triple<Int, Boolean, String> = withContext(Dispatchers.IO) {
        val durationSeconds = _timerElapsedSeconds.value
        val distanceMeters = _cardioDistanceMeters.value
        if (durationSeconds <= 0 && distanceMeters <= 0f) return@withContext Triple(0, false, "")

        // §8.1: cardio is excluded from PR tracking entirely.
        val session = ExerciseSession(
            exerciseId = workoutType,
            performedAt = System.currentTimeMillis(),
            weightKg = 0f,
            reps = 0,
            sets = 1,
            volumeScore = 0f
        )
        exerciseSessionRepository.logSession(session)
        setLogRepository.logSet(
            SetLog(
                exerciseSessionId = session.id,
                setIndex = 0,
                durationSeconds = durationSeconds.takeIf { it > 0 },
                distanceMeters = distanceMeters.takeIf { it > 0f },
                isPr = false
            )
        )

        Triple(0, false, session.id)
    }

    fun onPoseResult(result: PoseLandmarkerResult) {
        _poseResult.value = result
        if (_isRecording.value) {
            landmarkBuffer.add(System.currentTimeMillis() to result)
            processPoseForReps(result)
        }
    }

    private fun processPoseForReps(result: PoseLandmarkerResult) {
        if (result.landmarks().isEmpty()) {
            // §18.1/§18.2: user left frame entirely. Suspend counting and, if this
            // drags on mid-rep, abort the in-progress rep rather than let a stale
            // state machine combine with wherever they land when they return.
            _lowPoseConfidence.value = true
            _formFeedback.value = "Move into frame"
            resetRepIfStuck()
            return
        }
        val landmarks = result.landmarks()[0]

        when (workoutType.lowercase()) {
            "squat" -> processSquat(landmarks)
            "bench press", "pushup", "push-ups" -> processPushup(landmarks)
        }
    }

    /**
     * §18.2: a long pause mid-rep (racking the bar, stopping to adjust, walking
     * out of frame) must not silently produce a malformed rep when motion
     * resumes. Aborts back to START - the partial rep is discarded, never
     * counted.
     */
    private fun resetRepIfStuck() {
        if (currentRepState == RepState.START) return
        val elapsed = System.currentTimeMillis() - repStartTime
        if (elapsed > STUCK_REP_TIMEOUT_MS) {
            currentRepState = RepState.START
            currentRepFeedback.clear()
            minKneeAngle = 180.0
            minBackAngle = 180.0
            minElbowAngle = 180.0
            _formFeedback.value = "Rep interrupted - resetting"
        }
    }

    private fun processSquat(landmarks: List<NormalizedLandmark>) {
        val hip = landmarks[23]
        val knee = landmarks[25]
        val ankle = landmarks[27]
        val shoulder = landmarks[11]

        // §18.1: bad landmarks corrupt every downstream layer (rep counts, metrics,
        // LLM analysis, progression, PRs) - suspend counting rather than trust them.
        if (!PoseUtils.allConfident(hip, knee, ankle, shoulder)) {
            _lowPoseConfidence.value = true
            _formFeedback.value = "Move fully into frame"
            return
        }
        if (_lowPoseConfidence.value) _lowPoseConfidence.value = false
        resetRepIfStuck()

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

        // §18.1: same confidence gate as squat - suspend rather than log garbage reps.
        if (!PoseUtils.allConfident(shoulder, elbow, wrist)) {
            _lowPoseConfidence.value = true
            _formFeedback.value = "Move fully into frame"
            return
        }
        if (_lowPoseConfidence.value) _lowPoseConfidence.value = false
        resetRepIfStuck()

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
