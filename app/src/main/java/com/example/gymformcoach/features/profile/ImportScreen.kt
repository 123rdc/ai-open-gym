package com.example.gymformcoach.features.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.designsystem.Error
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.importer.FitNotesImporter
import com.example.gymformcoach.core.importer.HevyImporter
import com.example.gymformcoach.core.importer.ImportCommitResult
import com.example.gymformcoach.core.importer.ImportPipeline
import com.example.gymformcoach.core.importer.ImportPreview
import com.example.gymformcoach.core.importer.StrongImporter
import com.example.gymformcoach.core.importer.WorkoutImporter
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val IMPORTERS: List<WorkoutImporter> = listOf(FitNotesImporter(), StrongImporter(), HevyImporter())

/**
 * §17.2/§17C: pick a file, auto-detect the source by header, preview counts,
 * confirm, commit, show a completion summary including skipped/malformed rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pipeline = remember { ImportPipeline(AppDatabase.getInstance(context)) }

    var preview by remember { mutableStateOf<ImportPreview?>(null) }
    var commitResult by remember { mutableStateOf<ImportCommitResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isBusy = true
        errorMessage = null
        commitResult = null
        scope.launch {
            try {
                val headerLine = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readLine()
                val importer = headerLine?.let { header -> IMPORTERS.firstOrNull { it.canParse(header) } }
                if (importer == null) {
                    errorMessage = "Couldn't identify this file's format. Supported: FitNotes, Strong, Hevy CSV exports."
                } else {
                    val result = context.contentResolver.openInputStream(uri)?.use { importer.parse(it) }
                    if (result == null) {
                        errorMessage = "Couldn't read file."
                    } else {
                        preview = pipeline.preview(importer, result)
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Import failed: ${e.message}"
            }
            isBusy = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Workouts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text(
                text = "Supports FitNotes, Strong, and Hevy CSV exports. Nothing is uploaded - the file stays on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { pickLauncher.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values")) },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Choose file", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
            }

            errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = message, style = MaterialTheme.typography.bodySmall, color = Error)
            }

            preview?.let { p ->
                Spacer(modifier = Modifier.height(20.dp))
                ImportPreviewCard(p)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            isBusy = true
                            scope.launch {
                                commitResult = pipeline.commit(p)
                                preview = null
                                isBusy = false
                            }
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Confirm import", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { preview = null }) { Text("Cancel") }
                }
            }

            commitResult?.let { result ->
                Spacer(modifier = Modifier.height(20.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Import complete", fontWeight = FontWeight.Bold, color = Primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${result.sessionsImported} session(s) imported", style = MaterialTheme.typography.bodyMedium)
                        Text("${result.sessionsSkippedAsDuplicate} duplicate(s) skipped", style = MaterialTheme.typography.bodyMedium)
                        Text("${result.exercisesCreated} new custom exercise(s) created", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportPreviewCard(preview: ImportPreview) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(preview.sourceName, fontWeight = FontWeight.Bold)
                if (preview.isExperimental) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Error.copy(alpha = 0.15f), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("EXPERIMENTAL", style = MaterialTheme.typography.labelSmall, color = Error)
                    }
                }
            }
            if (preview.isExperimental) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This parser hasn't been verified against a real ${preview.sourceName} export - check the results carefully.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("${preview.sessionCount} session(s) found", style = MaterialTheme.typography.bodyMedium)
            preview.dateRange?.let { (from, to) ->
                val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
                Text("${from.format(fmt)} - ${to.format(fmt)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Text(
                "${preview.matchedExerciseCount} sets matched to existing exercises, ${preview.newExerciseNames.size} new exercise(s) will be created",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            if (preview.malformedRowCount > 0) {
                Text(
                    "${preview.malformedRowCount} row(s) skipped as malformed",
                    style = MaterialTheme.typography.bodySmall,
                    color = Error
                )
            }
        }
    }
}
