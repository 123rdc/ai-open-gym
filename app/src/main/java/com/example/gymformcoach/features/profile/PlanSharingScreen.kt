package com.example.gymformcoach.features.profile

import android.content.Intent
import android.print.PrintManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.gymformcoach.core.export.PlanPdfDocumentAdapter
import com.example.gymformcoach.core.export.PlanShare
import com.example.gymformcoach.core.export.PlanShareRepository
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** §17B: share/import/print a plan, structure only - never session data. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSharingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { PlanShareRepository(AppDatabase.getInstance(context)) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<PlanShare?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val content = try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            } catch (e: Exception) {
                null
            }
            if (content == null) {
                statusMessage = "Couldn't read file."
                isError = true
                return@launch
            }
            repository.parse(content)
                .onSuccess { pendingImport = it }
                .onFailure {
                    statusMessage = it.message
                    isError = true
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan Sharing", fontWeight = FontWeight.Bold) },
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
                text = "Shares routines and your weekly schedule only - no workout history, no body weight, no personal data.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            val share = repository.buildShare()
                            val uri = repository.writeShareFile(context, share)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share plan"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Share as file", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                ) {
                    Text("Import")
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val share = repository.buildShare()
                            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as PrintManager
                            printManager.print("Workout Plan", PlanPdfDocumentAdapter(share), null)
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                ) {
                    Text("Print PDF")
                }
            }

            statusMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = message, style = MaterialTheme.typography.bodySmall, color = if (isError) Error else Primary)
            }
        }
    }

    pendingImport?.let { share ->
        ImportPreviewDialog(
            share = share,
            onDismiss = { pendingImport = null },
            onConfirm = { adoptDays ->
                scope.launch {
                    val summary = repository.importShare(share, adoptDays)
                    statusMessage = summary
                    isError = false
                    pendingImport = null
                }
            }
        )
    }
}

@Composable
private fun ImportPreviewDialog(
    share: PlanShare,
    onDismiss: () -> Unit,
    onConfirm: (Set<Int>) -> Unit
) {
    // §17B.2: weekly-plan assignments are offered, not applied automatically.
    val adopted = remember { mutableStateOf(setOf<Int>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import ${share.routines.size} routine(s)?") },
        text = {
            Column {
                Text(
                    text = "Added alongside your existing routines - nothing is overwritten. Choose which days to adopt from the shared weekly plan:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(share.weeklyPlan.filter { it.routineName != null }) { assignment ->
                        val dayName = DayOfWeek.of(assignment.dayOfWeek).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        val checked = assignment.dayOfWeek in adopted.value
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    adopted.value = if (it) adopted.value + assignment.dayOfWeek else adopted.value - assignment.dayOfWeek
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Primary)
                            )
                            Text("$dayName - ${assignment.routineName}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(adopted.value) }) { Text("Import", color = Primary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
