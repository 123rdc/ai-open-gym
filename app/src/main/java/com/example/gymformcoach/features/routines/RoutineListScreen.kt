package com.example.gymformcoach.features.routines

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.RoutineRepository
import com.example.gymformcoach.core.data.RoutineSummary
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineListScreen(
    onBack: () -> Unit,
    onCreateRoutine: () -> Unit,
    onRoutineSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val repository = remember { RoutineRepository(database) }
    val routines by repository.getRoutineSummaries().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "My Routines", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onCreateRoutine) {
                        Icon(Icons.Default.Add, contentDescription = "Create Routine", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        if (routines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No routines yet. Create one to get started.",
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(routines, key = { it.id }) { routine ->
                    RoutineListCard(
                        routine = routine,
                        onClick = { onRoutineSelected(routine.id) },
                        onDuplicate = { scope.launch { repository.duplicateRoutine(routine.id) } },
                        onDelete = { scope.launch { repository.deleteRoutine(routine.id) } }
                    )
                }
            }
        }
    }
}

@Composable
fun RoutineListCard(
    routine: RoutineSummary,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = routine.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                if (routine.description.isNotBlank()) {
                    Text(text = routine.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
                }
                Spacer(modifier = Modifier.height(4.dp))
                val lastPerformedLabel = routine.lastPerformedDate?.let {
                    "Last performed ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(it)}"
                } ?: "Not started yet"
                Text(
                    text = "${routine.exerciseCount} exercises  •  $lastPerformedLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary
                )
            }
            IconButton(onClick = onDuplicate) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate routine", tint = TextSecondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete routine", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
