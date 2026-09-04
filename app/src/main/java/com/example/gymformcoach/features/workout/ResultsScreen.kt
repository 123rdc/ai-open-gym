package com.example.gymformcoach.features.workout

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymformcoach.core.analysis.PostSetAnalysisStatusBus
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.SetAnalysis
import com.example.gymformcoach.core.data.SetAnalysisRepository
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.designsystem.components.PrimaryButton
import com.example.gymformcoach.core.utils.PreferenceManager

@Composable
fun ResultsScreen(
    workoutType: String,
    reps: Int,
    isPr: Boolean = false,
    exerciseSessionId: String = "",
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Set Complete",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
        Text(
            text = "$workoutType: $reps reps",
            style = MaterialTheme.typography.displayLarge,
            color = Color.White
        )

        if (isPr) {
            Spacer(modifier = Modifier.height(16.dp))
            PrCelebrationBanner()
        }

        Spacer(modifier = Modifier.height(24.dp))

        RestTimerCard(
            initialSeconds = prefs.restDurationSeconds,
            onDurationChange = { prefs.restDurationSeconds = it }
        )

        if (exerciseSessionId.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "AI COACH ANALYSIS",
                style = MaterialTheme.typography.labelLarge,
                color = Primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            SetAnalysisSection(exerciseSessionId = exerciseSessionId)
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Done",
            onClick = onDone
        )
    }
}

private sealed class AnalysisUiState {
    data object Idle : AnalysisUiState()
    data object Loading : AnalysisUiState()
    data class Unavailable(val message: String) : AnalysisUiState()
    data class Ready(val analysis: SetAnalysis) : AnalysisUiState()
}

@Composable
private fun SetAnalysisSection(exerciseSessionId: String) {
    val context = LocalContext.current
    val repository = remember { SetAnalysisRepository(AppDatabase.getInstance(context)) }
    val dbAnalysis by remember(exerciseSessionId) { repository.observeForSession(exerciseSessionId) }
        .collectAsState(initial = null)
    val busStates by PostSetAnalysisStatusBus.states.collectAsState()

    val uiState: AnalysisUiState = when {
        dbAnalysis != null -> AnalysisUiState.Ready(dbAnalysis!!)
        else -> when (val busState = busStates[exerciseSessionId]) {
            is PostSetAnalysisStatusBus.State.Loading -> AnalysisUiState.Loading
            is PostSetAnalysisStatusBus.State.Failure -> AnalysisUiState.Unavailable(busState.message)
            is PostSetAnalysisStatusBus.State.Success -> AnalysisUiState.Ready(busState.analysis)
            null -> AnalysisUiState.Idle
        }
    }

    when (uiState) {
        is AnalysisUiState.Idle -> Unit
        is AnalysisUiState.Loading -> AnalysisLoadingCard()
        is AnalysisUiState.Unavailable -> AnalysisUnavailableCard(uiState.message)
        is AnalysisUiState.Ready -> AnalysisReadyCard(uiState.analysis)
    }
}

@Composable
private fun AnalysisLoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Primary, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Analyzing your set...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun AnalysisUnavailableCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.25f))
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun AnalysisReadyCard(analysis: SetAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.10f)),
        border = BorderStroke(1.5.dp, Primary.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = analysis.mainIssue,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = analysis.trendSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
            if (analysis.correctives.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "TRY THIS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    analysis.correctives.forEach { corrective ->
                        Row {
                            Text(text = "•  ", color = Primary, fontWeight = FontWeight.Bold)
                            Text(text = corrective, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrCelebrationBanner() {
    var pulseScale by remember { mutableFloatStateOf(0.85f) }
    LaunchedEffect(Unit) { pulseScale = 1f }
    val scale by animateFloatAsState(
        targetValue = pulseScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "prPulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.15f)),
        border = BorderStroke(1.5.dp, Primary),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "NEW PERSONAL RECORD",
                style = MaterialTheme.typography.labelLarge,
                color = Primary,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "You just beat your previous best on this exercise",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
