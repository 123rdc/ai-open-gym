package com.example.gymformcoach.features.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gymformcoach.core.data.Routine
import com.example.gymformcoach.core.data.RoutineExercise
import com.example.gymformcoach.core.designsystem.Error
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.Surface
import com.example.gymformcoach.core.designsystem.TextSecondary

/**
 * §3/§5 guided-session set-logging table - the manual counterpart to
 * CameraScreen's pose-tracked flow. Shows the progression engine's reasoning
 * verbatim (§5.3) and lets each set be checked off independently.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetLoggingScreen(
    routineExercise: RoutineExercise,
    routine: Routine?,
    stepNumber: Int,
    totalSteps: Int,
    canMakeSuperset: Boolean,
    onMakeSuperset: () -> Unit,
    onBack: () -> Unit,
    onFinishExercise: () -> Unit,
    viewModel: SetLoggingViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(routineExercise.id) {
        viewModel.load(routineExercise, routine)
    }

    val allCompleted = state.rows.isNotEmpty() && state.rows.all { it.completed }

    Scaffold(
        topBar = {
            Column {
                LinearProgressIndicator(
                    progress = { stepNumber.toFloat() / totalSteps.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = Primary,
                    trackColor = Surface
                )
                TopAppBar(
                    title = {
                        Column {
                            Text(routine?.name ?: "Workout", fontWeight = FontWeight.Bold)
                            Text(
                                text = "Exercise $stepNumber / $totalSteps",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Button(
                    onClick = onFinishExercise,
                    enabled = allCompleted,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(
                        text = if (stepNumber < totalSteps) "Next Exercise" else "Finish Workout",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                if (state.exercise?.imageUrl?.isNotBlank() == true) {
                    AsyncImage(
                        model = state.exercise!!.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Surface, RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (canMakeSuperset) {
                    AssistChip(
                        onClick = onMakeSuperset,
                        label = { Text("Make superset with next") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Primary.copy(alpha = 0.15f),
                            labelColor = Primary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = routineExercise.exerciseId,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.exercise?.muscle?.let { TagChip(it) }
                    state.exercise?.category?.let { TagChip(it) }
                    state.bestWeightKg?.let { TagChip("Best: ${formatKg(it)} kg") }
                }

                state.lastTimeSummary?.let { summary ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Last time: $summary",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // §5.3: the reasoning string, verbatim - never omitted, never paraphrased.
                state.target?.reasoning?.let { reasoning ->
                    Spacer(modifier = Modifier.height(12.dp))
                    val isWarning = reasoning.contains("stall", ignoreCase = true) ||
                        reasoning.contains("Missed", ignoreCase = true) ||
                        reasoning.contains("deload", ignoreCase = true) ||
                        reasoning.contains("days ago", ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                (if (isWarning) Error else Primary).copy(alpha = 0.12f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = reasoning,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isWarning) Error else Primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                SetTableHeader(showEffort = state.effortEnabled, effortScale = state.effortScale)
            }

            items(state.rows, key = { it.id }) { row ->
                val index = state.rows.indexOf(row)
                SetTableRow(
                    displayIndex = if (row.isWarmup) null else state.rows.take(index + 1).count { !it.isWarmup },
                    row = row,
                    showEffort = state.effortEnabled,
                    onWeightChange = { viewModel.updateRow(row.id, weightKg = it) },
                    onRepsChange = { viewModel.updateRow(row.id, reps = it) },
                    onEffortChange = { viewModel.updateRow(row.id, effortValue = it) },
                    onComplete = { viewModel.completeSet(row.id, routineExercise.exerciseId) },
                    onRemove = { viewModel.removeSet(row.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = { viewModel.addWarmupSet() }) {
                        Text("+ Add warm-up set", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { viewModel.addSet() }) {
                        Text("+ Add set", color = Primary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Box(
        modifier = Modifier
            .background(Surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun SetTableHeader(showEffort: Boolean, effortScale: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("SET", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(32.dp))
        Text("WEIGHT (KG)", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.weight(1f))
        Text("REPS", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.weight(1f))
        if (showEffort) {
            Text(effortScale, style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.width(40.dp))
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SetTableRow(
    displayIndex: Int?,
    row: SetRow,
    showEffort: Boolean,
    onWeightChange: (Float) -> Unit,
    onRepsChange: (Int) -> Unit,
    onEffortChange: (Int) -> Unit,
    onComplete: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayIndex?.toString() ?: "W",
            style = MaterialTheme.typography.bodyMedium,
            color = if (row.isWarmup) TextSecondary else Color.White,
            modifier = Modifier.width(32.dp)
        )
        Stepper(
            value = formatKg(row.weightKg),
            onIncrement = { onWeightChange(row.weightKg + 2.5f) },
            onDecrement = { onWeightChange((row.weightKg - 2.5f).coerceAtLeast(0f)) },
            enabled = !row.completed,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Stepper(
            value = "${row.reps}",
            onIncrement = { onRepsChange(row.reps + 1) },
            onDecrement = { onRepsChange((row.reps - 1).coerceAtLeast(0)) },
            enabled = !row.completed,
            modifier = Modifier.weight(1f)
        )
        if (showEffort) {
            Spacer(modifier = Modifier.width(4.dp))
            Stepper(
                value = row.effortValue?.toString() ?: "-",
                onIncrement = { onEffortChange(((row.effortValue ?: 0) + 1).coerceAtMost(10)) },
                onDecrement = { onEffortChange(((row.effortValue ?: 1) - 1).coerceAtLeast(0)) },
                enabled = !row.completed,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (row.completed) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(if (row.wasPr) Primary else Primary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = "Completed", tint = Color.Black, modifier = Modifier.size(18.dp))
            }
        } else {
            IconButton(onClick = onComplete, modifier = Modifier.size(32.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent, CircleShape)
                        .then(Modifier)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Mark set complete",
                        tint = TextSecondary,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                }
            }
        }
    }
    if (row.wasPr) {
        Text(
            text = "New PR",
            style = MaterialTheme.typography.labelSmall,
            color = Primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 32.dp, top = 2.dp)
        )
    }
}

@Composable
private fun Stepper(
    value: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onDecrement, enabled = enabled, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = if (enabled) Color.White else TextSecondary, modifier = Modifier.size(14.dp))
        }
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = onIncrement, enabled = enabled, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Increase", tint = if (enabled) Color.White else TextSecondary, modifier = Modifier.size(14.dp))
        }
    }
}

private fun formatKg(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)
