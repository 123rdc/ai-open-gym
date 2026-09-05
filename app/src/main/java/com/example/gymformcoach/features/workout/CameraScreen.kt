package com.example.gymformcoach.features.workout

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymformcoach.core.data.ExerciseType
import com.example.gymformcoach.core.data.LoadType
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.Background
import com.example.gymformcoach.core.ml.PoseLandmarkerHelper
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    workoutType: String,
    weight: Float,
    targetReps: Int,
    totalSets: Int,
    onBack: () -> Unit,
    onFinishSet: (Int, Boolean, String) -> Unit,
    /** §3.1: shown once at the start of a guided routine session, not per set. */
    promptForBodyWeight: Boolean = false,
    /** §4.2: which set within the (possibly superset) sequence this is, 1-based. */
    currentSetNumber: Int = 1,
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = remember { com.example.gymformcoach.core.utils.PreferenceManager(context) }

    // §3.2: held for the active session only, released on dispose.
    KeepScreenOn(enabled = prefs.keepScreenOnDuringWorkout)

    var bodyWeightPromptShown by rememberSaveable { mutableStateOf(!promptForBodyWeight) }
    if (!bodyWeightPromptShown) {
        BodyWeightPrompt(onDismiss = { bodyWeightPromptShown = true })
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val isRecording by viewModel.isRecording.collectAsState()
    val repCount by viewModel.repCount.collectAsState()
    val poseResult by viewModel.poseResult.collectAsState()
    val feedback by viewModel.formFeedback.collectAsState()
    val exercise by viewModel.exercise.collectAsState()
    val timerElapsedSeconds by viewModel.timerElapsedSeconds.collectAsState()
    val cardioDistanceMeters by viewModel.cardioDistanceMeters.collectAsState()
    val addedWeightKg by viewModel.addedWeightKg.collectAsState()
    val effortValue by viewModel.effortValue.collectAsState()
    val effortTrackingEnabled = remember { com.example.gymformcoach.core.utils.PreferenceManager(context).effortTrackingEnabled }
    val effortScale = remember { com.example.gymformcoach.core.utils.PreferenceManager(context).effortScale }
    val exerciseType = exercise?.exerciseType ?: ExerciseType.REPS
    val loadType = exercise?.loadType ?: LoadType.WEIGHTED

    LaunchedEffect(workoutType) {
        viewModel.setWorkoutType(workoutType)
    }

    LaunchedEffect(weight) {
        viewModel.setWeight(weight)
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val frameCounter = remember { java.util.concurrent.atomic.AtomicInteger(0) }

    // PoseLandmarkerHelper initialization
    val poseLandmarkerHelper = remember {
        PoseLandmarkerHelper(
            context = context,
            listener = object : PoseLandmarkerHelper.LandmarkerListener {
                override fun onError(error: String) {
                    Log.e("CameraScreen", "PoseLandmarker Error: $error")
                }

                override fun onResults(
                    result: PoseLandmarkerResult,
                    inferenceTime: Long,
                    inputImageHeight: Int,
                    inputImageWidth: Int
                ) {
                    viewModel.onPoseResult(result)
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            poseLandmarkerHelper.close()
            analysisExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        previewView = this
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView?.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                        .also {
                            it.setAnalyzer(analysisExecutor) { imageProxy ->
                                val frameNumber = frameCounter.incrementAndGet()
                                Log.d("CameraAnalyzer", "Frame #$frameNumber reaching analyzer at ${System.currentTimeMillis()} (${imageProxy.width}x${imageProxy.height})")
                                poseLandmarkerHelper.detectLiveStream(
                                    imageProxy = imageProxy,
                                    isFrontCamera = false // Change if needed
                                )
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Use case binding failed", e)
                    }
                }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Camera permission is required", color = Color.White)
            }
        }

        // Pose Overlay
        poseResult?.let { result ->
            PoseOverlay(result = result)
        }

        // Feedback Overlay
        feedback?.let { text ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 100.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = text,
                    color = Primary,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .background(Background.copy(alpha = 0.8f), MaterialTheme.shapes.medium)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Background.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = workoutType.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .background(Background.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            val weightLabel = when {
                loadType == LoadType.BODYWEIGHT && addedWeightKg == null -> "BODYWEIGHT"
                loadType == LoadType.BODYWEIGHT -> "+${addedWeightKg}KG"
                else -> "${weight}KG"
            }
            Text(
                text = weightLabel,
                style = MaterialTheme.typography.labelLarge,
                color = Primary,
                modifier = Modifier
                    .background(Background.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Spacer(modifier = Modifier.weight(1.0f))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "SET $currentSetNumber OF $totalSets",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                when (exerciseType) {
                    ExerciseType.TIMED -> Text(
                        text = formatSeconds(timerElapsedSeconds),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    ExerciseType.CARDIO -> Text(
                        text = formatSeconds(timerElapsedSeconds),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    ExerciseType.REPS -> Text(
                        text = "$repCount / $targetReps",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (repCount >= targetReps) Primary else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Progress Bar at the top
        val progress = (repCount.toFloat() / targetReps.toFloat()).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.TopCenter),
            color = Primary,
            trackColor = Color.White.copy(alpha = 0.2f)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // §1.3: bodyweight exercises get an optional added-weight affordance, collapsed by
            // default, shown above the primary control regardless of exercise shape.
            if (loadType == LoadType.BODYWEIGHT && !isRecording) {
                AddedWeightToggle(
                    addedWeightKg = addedWeightKg,
                    onChange = viewModel::setAddedWeight
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // §10: informational-only effort rating, off by default. Only meaningful
            // for REPS sets - nothing reads it, so it's purely a logging convenience.
            if (effortTrackingEnabled && exerciseType == ExerciseType.REPS && !isRecording) {
                EffortPicker(
                    scale = effortScale,
                    selected = effortValue,
                    onSelect = viewModel::setEffortValue
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            when {
                exerciseType == ExerciseType.REPS && viewModel.isPoseTracked -> {
                    // Camera-tracked squat/pushup: unchanged auto rep-counting record button.
                    RecordButton(isRecording = isRecording, onClick = {
                        if (isRecording) {
                            coroutineScope.launch {
                                val (finishedReps, isPr, sessionId) = viewModel.stopRecordingAndLog()
                                onFinishSet(finishedReps, isPr, sessionId)
                            }
                        } else {
                            viewModel.startRecording()
                        }
                    })
                }
                exerciseType == ExerciseType.REPS -> {
                    // §1.2: no pose tracking for this exercise - manual rep entry.
                    ManualRepStepper(
                        repCount = repCount,
                        onIncrement = viewModel::incrementManualReps,
                        onDecrement = viewModel::decrementManualReps,
                        onLogSet = {
                            coroutineScope.launch {
                                val (finishedReps, isPr, sessionId) = viewModel.stopRecordingAndLog()
                                onFinishSet(finishedReps, isPr, sessionId)
                            }
                        }
                    )
                }
                exerciseType == ExerciseType.TIMED -> {
                    // §1.5: work timer, distinct from the rest timer.
                    WorkTimerControl(
                        isRunning = isRecording,
                        onStart = {
                            viewModel.startRecording()
                            viewModel.startTimer()
                        },
                        onStopAndLog = {
                            coroutineScope.launch {
                                val (finishedReps, isPr, sessionId) = viewModel.stopRecordingAndLog()
                                onFinishSet(finishedReps, isPr, sessionId)
                            }
                        }
                    )
                }
                exerciseType == ExerciseType.CARDIO -> {
                    CardioControl(
                        isRunning = isRecording,
                        distanceMeters = cardioDistanceMeters,
                        onDistanceChange = viewModel::setCardioDistance,
                        onStart = {
                            viewModel.startRecording()
                            viewModel.startTimer()
                        },
                        onStopAndLog = {
                            coroutineScope.launch {
                                val (finishedReps, isPr, sessionId) = viewModel.stopRecordingAndLog()
                                onFinishSet(finishedReps, isPr, sessionId)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordButton(isRecording: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(80.dp)
            .border(2.dp, if (isRecording) Color.Red else Primary, CircleShape),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRecording) Color.Red.copy(alpha = 0.3f) else Color.Transparent
        ),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (isRecording) 32.dp else 64.dp)
                .background(if (isRecording) Color.Red else Primary, if (isRecording) MaterialTheme.shapes.extraSmall else CircleShape)
        )
    }
}

@Composable
private fun ManualRepStepper(
    repCount: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onLogSet: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onDecrement,
            modifier = Modifier.background(Background.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrease reps", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = "$repCount",
            style = MaterialTheme.typography.displaySmall,
            color = Primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(64.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.width(20.dp))
        IconButton(
            onClick = onIncrement,
            modifier = Modifier.background(Background.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Increase reps", tint = Color.White)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onLogSet,
        enabled = repCount > 0,
        colors = ButtonDefaults.buttonColors(containerColor = Primary)
    ) {
        Text(text = "Log Set", color = Color.Black, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WorkTimerControl(isRunning: Boolean, onStart: () -> Unit, onStopAndLog: () -> Unit) {
    Button(
        onClick = { if (isRunning) onStopAndLog() else onStart() },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRunning) Color.Red.copy(alpha = 0.8f) else Primary
        )
    ) {
        Text(
            text = if (isRunning) "Stop & Log" else "Start Hold",
            color = if (isRunning) Color.White else Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CardioControl(
    isRunning: Boolean,
    distanceMeters: Float,
    onDistanceChange: (Float) -> Unit,
    onStart: () -> Unit,
    onStopAndLog: () -> Unit
) {
    var distanceText by remember(distanceMeters) { mutableStateOf(if (distanceMeters > 0f) distanceMeters.toInt().toString() else "") }
    OutlinedTextField(
        value = distanceText,
        onValueChange = {
            distanceText = it
            onDistanceChange(it.toFloatOrNull() ?: 0f)
        },
        label = { Text("Distance (m)") },
        singleLine = true,
        modifier = Modifier
            .background(Background.copy(alpha = 0.6f), MaterialTheme.shapes.small)
            .width(200.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = { if (isRunning) onStopAndLog() else onStart() },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRunning) Color.Red.copy(alpha = 0.8f) else Primary
        )
    ) {
        Text(
            text = if (isRunning) "Stop & Log" else "Start",
            color = if (isRunning) Color.White else Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AddedWeightToggle(addedWeightKg: Float?, onChange: (Float?) -> Unit) {
    var expanded by remember { mutableStateOf(addedWeightKg != null) }
    if (!expanded) {
        TextButton(onClick = { expanded = true }) {
            Text(text = "+ Add weight", color = Primary)
        }
    } else {
        var text by remember { mutableStateOf(addedWeightKg?.toString() ?: "") }
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onChange(it.toFloatOrNull())
            },
            label = { Text("Added weight (kg)") },
            singleLine = true,
            modifier = Modifier
                .background(Background.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                .width(200.dp)
        )
    }
}

@Composable
private fun EffortPicker(scale: String, selected: Int?, onSelect: (Int?) -> Unit) {
    // RIR (reps in reserve): 0-5, lower = harder. RPE: 6-10, higher = harder.
    val values = if (scale == "RPE") (6..10).toList() else (0..5).toList()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = scale, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEach { value ->
                val isSelected = selected == value
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isSelected) Primary else Background.copy(alpha = 0.6f),
                            CircleShape
                        )
                        .then(Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = { onSelect(if (isSelected) null else value) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "$value",
                            color = if (isSelected) Color.Black else Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

private fun formatSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun PoseOverlay(result: PoseLandmarkerResult) {
    val primaryColor = Primary
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (result.landmarks().isNotEmpty()) {
            val landmarks = result.landmarks()[0]

            // Draw connections
            com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker.POSE_LANDMARKS.forEach { connection ->
                val start = landmarks[connection.start()]
                val end = landmarks[connection.end()]

                drawLine(
                    color = primaryColor.copy(alpha = 0.7f),
                    start = Offset(start.x() * size.width, start.y() * size.height),
                    end = Offset(end.x() * size.width, end.y() * size.height),
                    strokeWidth = 4f
                )
            }

            // Draw landmarks
            landmarks.forEach { landmark ->
                drawCircle(
                    color = primaryColor,
                    radius = 6f,
                    center = Offset(landmark.x() * size.width, landmark.y() * size.height)
                )
            }
        }
    }
}
