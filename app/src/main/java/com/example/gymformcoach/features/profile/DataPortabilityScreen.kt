package com.example.gymformcoach.features.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.gymformcoach.core.export.DataExportRepository
import com.example.gymformcoach.core.export.ImportOutcome
import com.example.gymformcoach.core.utils.PreferenceManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * §17.1: one-tap full export/import via Storage Access Framework - the user
 * picks the location, nothing leaves the device, no telemetry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataPortabilityScreen(
    onBack: () -> Unit,
    onOpenPlanSharing: () -> Unit,
    onOpenImport: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember {
        DataExportRepository(AppDatabase.getInstance(context), PreferenceManager(context))
    }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showOverwriteWarning by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isBusy = true
        scope.launch {
            try {
                val export = repository.exportAll()
                val json = repository.serialize(export)
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                statusMessage = "Exported ${export.routines.size} routines, ${export.exerciseSessions.size} sessions."
                isError = false
            } catch (e: Exception) {
                statusMessage = "Export failed: ${e.message}"
                isError = true
            }
            isBusy = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            // §17.1: validate structure before any write, and warn explicitly
            // before overwriting existing data.
            if (repository.hasExistingData()) {
                pendingImportUri = uri
                showOverwriteWarning = true
            } else {
                runImport(context, repository, uri) { message, error ->
                    statusMessage = message
                    isError = error
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data", fontWeight = FontWeight.Bold) },
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
            Text(text = "Full backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Routines, plan, sessions, sets, custom exercises, body weight, and settings - one JSON file, saved wherever you choose. No telemetry, no external service.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val filename = "gymformcoach_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.json"
                        exportLauncher.launch(filename)
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Export", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    enabled = !isBusy,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                ) {
                    Text("Import")
                }
            }

            statusMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = message, style = MaterialTheme.typography.bodySmall, color = if (isError) Error else Primary)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(text = "Share a plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Send just your routines and weekly schedule to someone else - no workout history, no personal data.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            OutlinedButton(onClick = onOpenPlanSharing, colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)) {
                Text("Plan sharing")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(text = "Import from another app", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Bring in workout history from FitNotes, Strong, or Hevy.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            OutlinedButton(onClick = onOpenImport, colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)) {
                Text("Import workouts")
            }
        }
    }

    if (showOverwriteWarning) {
        AlertDialog(
            onDismissRequest = { showOverwriteWarning = false; pendingImportUri = null },
            title = { Text("Overwrite existing data?") },
            text = {
                Text("You already have routines or logged sessions. Importing will add this file's data alongside what's already here - it won't delete anything, but duplicate routines or sessions are possible if you import the same file twice.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingImportUri
                    showOverwriteWarning = false
                    pendingImportUri = null
                    if (uri != null) {
                        scope.launch {
                            runImport(context, repository, uri) { message, error ->
                                statusMessage = message
                                isError = error
                            }
                        }
                    }
                }) {
                    Text("Import anyway", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverwriteWarning = false; pendingImportUri = null }) { Text("Cancel") }
            }
        )
    }
}

private suspend fun runImport(
    context: android.content.Context,
    repository: DataExportRepository,
    uri: Uri,
    onResult: (String, Boolean) -> Unit
) {
    val content = try {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
    } catch (e: Exception) {
        onResult("Couldn't read file: ${e.message}", true)
        return
    }
    if (content == null) {
        onResult("Couldn't read file.", true)
        return
    }
    val parsed = repository.parse(content)
    parsed.onFailure { e ->
        onResult("Not a valid backup file: ${e.message}", true)
    }
    parsed.onSuccess { export ->
        when (val outcome = repository.importAll(export)) {
            is ImportOutcome.Success -> onResult(outcome.summary, false)
            is ImportOutcome.InvalidFormat -> onResult("Import failed: ${outcome.reason}", true)
            is ImportOutcome.WrongFileType -> onResult("Expected ${outcome.expected}, got ${outcome.found}", true)
        }
    }
}
