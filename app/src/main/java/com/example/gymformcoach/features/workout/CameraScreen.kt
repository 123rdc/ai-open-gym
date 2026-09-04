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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val isRecording by viewModel.isRecording.collectAsState()
    val repCount by viewModel.repCount.collectAsState()
    val poseResult by viewModel.poseResult.collectAsState()
    val feedback by viewModel.formFeedback.collectAsState()

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
            Text(
                text = "${weight}KG",
                style = MaterialTheme.typography.labelLarge,
                color = Primary,
                modifier = Modifier
                    .background(Background.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Spacer(modifier = Modifier.weight(1.0f))
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "SET 1 OF $totalSets",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "$repCount / $targetReps",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (repCount >= targetReps) Primary else Color.White,
                    fontWeight = FontWeight.Bold
                )
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    if (isRecording) {
                        coroutineScope.launch {
                            val (finishedReps, isPr, sessionId) = viewModel.stopRecordingAndLog()
                            onFinishSet(finishedReps, isPr, sessionId)
                        }
                    } else {
                        viewModel.startRecording()
                    }
                },
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
    }
}

@Composable
fun PoseOverlay(result: PoseLandmarkerResult) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (result.landmarks().isNotEmpty()) {
            val landmarks = result.landmarks()[0]
            
            // Draw connections
            com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker.POSE_LANDMARKS.forEach { connection ->
                val start = landmarks[connection.start()]
                val end = landmarks[connection.end()]
                
                drawLine(
                    color = Primary.copy(alpha = 0.7f),
                    start = Offset(start.x() * size.width, start.y() * size.height),
                    end = Offset(end.x() * size.width, end.y() * size.height),
                    strokeWidth = 4f
                )
            }

            // Draw landmarks
            landmarks.forEach { landmark ->
                drawCircle(
                    color = Primary,
                    radius = 6f,
                    center = Offset(landmark.x() * size.width, landmark.y() * size.height)
                )
            }
        }
    }
}
