package com.example.gymformcoach.features.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymformcoach.core.data.BodyWeightEntry
import com.example.gymformcoach.core.data.BodyWeightSource
import com.example.gymformcoach.core.designsystem.Error
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.importer.AppleHealthImporter
import kotlinx.coroutines.launch

/**
 * §17A: body weight is its own tracked metric, not just a session-start prompt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyWeightScreen(
    onBack: () -> Unit,
    viewModel: BodyWeightViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showEntryDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var importStatus by remember { mutableStateOf<String?>(null) }
    var importIsError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // §17C.2: Apple Health export.zip, body weight only, stream-parsed (never
    // forced through the CSV import path).
    val appleHealthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val entries = context.contentResolver.openInputStream(uri)?.use {
                    AppleHealthImporter.parseWeightFromZip(it)
                } ?: emptyList()
                entries.forEach { viewModel.recordEntry(it.weightKg, it.date, BodyWeightSource.IMPORT) }
                importStatus = "Imported ${entries.size} weight entries from Apple Health (experimental parser - check results)."
                importIsError = false
            } catch (e: Exception) {
                importStatus = "Import failed: ${e.message}"
                importIsError = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Body Weight", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { appleHealthLauncher.launch(arrayOf("application/zip")) }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import from Apple Health")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showEntryDialog = true }, containerColor = Primary) {
                Icon(Icons.Default.Add, contentDescription = "Add entry", tint = Color.Black)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            importStatus?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (importIsError) Error else Primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BodyWeightRange.entries.forEach { range ->
                    FilterChip(
                        selected = state.range == range,
                        onClick = { viewModel.setRange(range) },
                        label = { Text(range.label()) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.entries.isEmpty()) {
                // §18.5: designed empty state, never a fabricated chart.
                Card(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No entries yet. Tap + to log your weight.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                BodyWeightChart(entries = state.entries, goalWeightKg = state.goalWeightKg)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { showGoalDialog = true }) {
                Text(
                    text = state.goalWeightKg?.let { "Goal: ${it}kg" } ?: "Set a goal weight",
                    color = Primary
                )
            }
        }
    }

    if (showEntryDialog) {
        BodyWeightEntryDialog(
            onDismiss = { showEntryDialog = false },
            onSave = { weight ->
                viewModel.recordEntry(weight)
                showEntryDialog = false
            }
        )
    }

    if (showGoalDialog) {
        GoalWeightDialog(
            currentGoal = state.goalWeightKg,
            onDismiss = { showGoalDialog = false },
            onSave = { goal ->
                viewModel.setGoal(goal)
                showGoalDialog = false
            }
        )
    }
}

private fun BodyWeightRange.label() = when (this) {
    BodyWeightRange.MONTH -> "Month"
    BodyWeightRange.SIX_MONTHS -> "6 Months"
    BodyWeightRange.YEAR -> "Year"
    BodyWeightRange.ALL -> "All"
}

/**
 * §17A.2: point-to-point deltas are colored by direction *relative to the goal*,
 * never hardcoded "down = green" - losing weight is favorable only when the goal
 * is below the current value.
 */
@Composable
private fun BodyWeightChart(entries: List<BodyWeightEntry>, goalWeightKg: Float?) {
    val favorable = Color(0xFF4CAF50)
    val unfavorable = Color(0xFFE57373)
    val neutral = TextSecondary
    val primaryColor = Primary

    Card(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val weights = entries.map { it.weightKg }
            val minW = minOf(weights.min(), goalWeightKg ?: weights.min())
            val maxW = maxOf(weights.max(), goalWeightKg ?: weights.max())
            val range = (maxW - minW).coerceAtLeast(0.1f)
            val stepX = if (entries.size > 1) size.width / (entries.size - 1) else 0f

            fun yFor(weight: Float) = size.height - ((weight - minW) / range) * size.height

            if (goalWeightKg != null) {
                val goalY = yFor(goalWeightKg)
                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = Offset(0f, goalY),
                    end = Offset(size.width, goalY),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }

            for (i in 1 until entries.size) {
                val prev = entries[i - 1]
                val curr = entries[i]
                val color = if (goalWeightKg == null) {
                    neutral
                } else {
                    val movingTowardGoal = when {
                        goalWeightKg < prev.weightKg -> curr.weightKg < prev.weightKg
                        goalWeightKg > prev.weightKg -> curr.weightKg > prev.weightKg
                        else -> true
                    }
                    if (movingTowardGoal) favorable else unfavorable
                }
                drawLine(
                    color = color,
                    start = Offset(stepX * (i - 1), yFor(prev.weightKg)),
                    end = Offset(stepX * i, yFor(curr.weightKg)),
                    strokeWidth = 3f
                )
            }

            entries.forEachIndexed { i, entry ->
                drawCircle(color = primaryColor, radius = 4f, center = Offset(stepX * i, yFor(entry.weightKg)))
            }
        }
    }
}

@Composable
private fun BodyWeightEntryDialog(onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log weight") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { text.toFloatOrNull()?.let(onSave) }) {
                Text("Save", color = Primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun GoalWeightDialog(currentGoal: Float?, onDismiss: () -> Unit, onSave: (Float?) -> Unit) {
    var text by remember { mutableStateOf(currentGoal?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Goal weight") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Goal (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.toFloatOrNull()) }) {
                Text("Save", color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = { onSave(null) }) { Text("Clear goal") }
        }
    )
}
