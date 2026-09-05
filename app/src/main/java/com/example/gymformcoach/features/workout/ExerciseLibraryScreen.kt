package com.example.gymformcoach.features.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.Exercise
import com.example.gymformcoach.core.data.ExerciseRepository
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.Surface
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.designsystem.components.GymBottomNavigation
import com.example.gymformcoach.core.navigation.Screen
import kotlinx.coroutines.launch

/**
 * §15.1/§15.2: a single searchable, filterable exercise library across every
 * body part, rather than the body-part-first flow WorkoutListScreen uses -
 * this is what "Exercises" means as its own destination.
 */
@Composable
fun ExerciseLibraryScreen(
    onNavigateToTab: (String) -> Unit,
    onExerciseSelected: (Exercise) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exerciseRepository = remember { ExerciseRepository(AppDatabase.getInstance(context)) }
    val allExercises by exerciseRepository.getAll().collectAsState(initial = emptyList())

    var query by remember { mutableStateOf("") }
    var selectedBodyPart by remember { mutableStateOf("All") }
    var selectedEquipment by remember { mutableStateOf("Any equipment") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val bodyParts = remember { listOf("All") + ExerciseCatalog.bodyParts.map { it.first } }
    val equipmentOptions = remember(allExercises) {
        listOf("Any equipment") + allExercises.map { it.category }.distinct().sorted()
    }

    val filtered = remember(allExercises, query, selectedBodyPart, selectedEquipment) {
        allExercises.filter { exercise ->
            (selectedBodyPart == "All" || exercise.bodyPart == selectedBodyPart) &&
                (selectedEquipment == "Any equipment" || exercise.category == selectedEquipment) &&
                (query.isBlank() || exercise.name.contains(query, ignoreCase = true))
        }
    }

    Scaffold(
        bottomBar = {
            GymBottomNavigation(currentRoute = Screen.WorkoutList.route, onTabSelected = onNavigateToTab)
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(text = "Exercises", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "${allExercises.size} exercises",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(bodyParts) { part ->
                        FilterChip(
                            selected = part == selectedBodyPart,
                            onClick = { selectedBodyPart = part },
                            label = { Text(part) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(equipmentOptions) { equipment ->
                        FilterChip(
                            selected = equipment == selectedEquipment,
                            onClick = { selectedEquipment = equipment },
                            label = { Text(equipment) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .then(Modifier)
                            .padding(16.dp)
                            .let { it },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Create your own exercise", fontWeight = FontWeight.Bold)
                            Text(
                                text = "name + body part, no animation",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Create custom exercise", tint = Primary)
                        }
                    }
                }

                if (filtered.isEmpty() && allExercises.isNotEmpty()) {
                    item {
                        Text(
                            text = "No exercises match these filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                }
                items(filtered, key = { it.id }) { exercise ->
                    ExerciseLibraryRow(exercise = exercise, onSelect = { onExerciseSelected(exercise) })
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCustomExerciseDialog(
            bodyParts = ExerciseCatalog.bodyParts.map { it.first },
            onDismiss = { showCreateDialog = false },
            onSave = { name, bodyPart ->
                scope.launch {
                    exerciseRepository.addCustomExercise(
                        Exercise(
                            name = name,
                            bodyPart = bodyPart,
                            muscle = bodyPart,
                            category = "Bodyweight",
                            duration = "20 mins",
                            difficulty = "Beginner",
                            imageUrl = "",
                            about = "Custom exercise."
                        )
                    )
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
private fun CreateCustomExerciseDialog(
    bodyParts: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var bodyPart by remember { mutableStateOf(bodyParts.first()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New exercise") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(bodyPart)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        bodyParts.forEach { part ->
                            DropdownMenuItem(
                                text = { Text(part) },
                                onClick = { bodyPart = part; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), bodyPart) }, enabled = name.isNotBlank()) {
                Text("Add", color = Primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ExerciseLibraryRow(exercise: Exercise, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (exercise.imageUrl.isNotBlank()) {
            AsyncImage(
                model = exercise.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Surface, RoundedCornerShape(10.dp))
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = exercise.name, fontWeight = FontWeight.Bold)
            Text(
                text = "${exercise.bodyPart} · ${exercise.category}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        OutlinedButton(
            onClick = onSelect,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Plan", style = MaterialTheme.typography.labelSmall)
        }
    }
}
