package com.example.gymformcoach.features.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.ExerciseRepository
import com.example.gymformcoach.core.data.ExerciseType
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.designsystem.components.PrimaryButton
import com.example.gymformcoach.features.workout.IntensityInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineBuilderScreen(
    viewModel: RoutineBuilderViewModel,
    onBack: () -> Unit,
    onPickExercise: () -> Unit,
    onSaved: (String) -> Unit,
    onStart: (String) -> Unit
) {
    val context = LocalContext.current
    val exerciseRepository = remember { ExerciseRepository(AppDatabase.getInstance(context)) }
    val allExercises by exerciseRepository.getAll().collectAsState(initial = emptyList())
    // §4.4: cardio can't take part in a superset (no meaningful round structure).
    val cardioNames = remember(allExercises) {
        allExercises.filter { it.exerciseType == ExerciseType.CARDIO }.map { it.name }.toSet()
    }

    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }

    // §4.3: grouped exercises render with a connecting rail and a shared
    // "Superset X" header. Group letters are assigned by first appearance.
    val groupLetters = remember(viewModel.exercises.map { it.supersetGroupId }) {
        viewModel.exercises.mapNotNull { it.supersetGroupId }.distinct()
            .withIndex().associate { (i, id) -> id to ('A' + i) }
    }

    fun exitSelectionMode() {
        selectionMode = false
        selectedIds.clear()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectionMode) "${selectedIds.size} selected" else "New Routine",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (selectionMode) exitSelectionMode() else onBack() }) {
                        Icon(
                            if (selectionMode) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(
                            onClick = {
                                val indices = selectedIds.mapNotNull { id ->
                                    viewModel.exercises.indexOfFirst { it.exerciseId == id }.takeIf { it >= 0 }
                                }.toSet()
                                if (viewModel.groupAsSuperset(indices, cardioNames)) {
                                    exitSelectionMode()
                                }
                            },
                            enabled = selectedIds.size >= 2
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Group as superset", tint = Primary)
                        }
                    } else if (viewModel.exercises.size >= 2) {
                        IconButton(onClick = { selectionMode = true }) {
                            Icon(Icons.Default.Link, contentDescription = "Group as superset")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.routineName,
                        onValueChange = { viewModel.routineName = it },
                        label = { Text("Routine name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            cursorColor = Primary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.routineDescription,
                        onValueChange = { viewModel.routineDescription = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            cursorColor = Primary
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onPickExercise,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                    ) {
                        Text("+ Add Exercise")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(viewModel.exercises, key = { it.exerciseId }) { draft ->
                    val index = viewModel.exercises.indexOf(draft)
                    val groupId = draft.supersetGroupId
                    val isFirstInGroup = groupId != null &&
                        viewModel.exercises.getOrNull(index - 1)?.supersetGroupId != groupId

                    if (groupId != null && isFirstInGroup) {
                        Text(
                            text = "Superset ${groupLetters[groupId]}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                        )
                    }

                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        if (groupId != null) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(Primary, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        RoutineExerciseRow(
                            draft = draft,
                            selectionMode = selectionMode,
                            selected = draft.exerciseId in selectedIds,
                            onToggleSelected = {
                                if (draft.exerciseId in selectedIds) selectedIds.remove(draft.exerciseId)
                                else selectedIds.add(draft.exerciseId)
                            },
                            onMoveUp = { viewModel.moveUp(index) },
                            onMoveDown = { viewModel.moveDown(index) },
                            onRemove = { viewModel.removeExercise(index) },
                            onUngroup = { groupId?.let { viewModel.ungroupSuperset(it) } },
                            canMoveUp = index > 0,
                            canMoveDown = index < viewModel.exercises.size - 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                PrimaryButton(
                    text = "Save Routine",
                    onClick = { viewModel.saveRoutine(onSaved) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.saveRoutine(onStart) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                    enabled = viewModel.canSave
                ) {
                    Text("Start Routine", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RoutineExerciseRow(
    draft: DraftRoutineExercise,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onToggleSelected: () -> Unit = {},
    onUngroup: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (selectionMode) Modifier else Modifier
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        onClick = if (selectionMode) onToggleSelected else ({})
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onToggleSelected() },
                        colors = CheckboxDefaults.colors(checkedColor = Primary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                AsyncImage(
                    model = draft.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = draft.exerciseId, fontWeight = FontWeight.Bold)
                    Text(text = draft.muscle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                if (!selectionMode) {
                    if (draft.supersetGroupId != null) {
                        IconButton(onClick = onUngroup) {
                            Icon(Icons.Default.LinkOff, contentDescription = "Ungroup", tint = TextSecondary)
                        }
                    }
                    Column {
                        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", tint = if (canMoveUp) Color.White else Color.Gray.copy(alpha = 0.3f))
                        }
                        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", tint = if (canMoveDown) Color.White else Color.Gray.copy(alpha = 0.3f))
                        }
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (!selectionMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntensityInput(
                        label = "Weight (kg)",
                        value = draft.targetWeightKg,
                        onValueChange = { draft.targetWeightKg = it },
                        modifier = Modifier.weight(1f)
                    )
                    IntensityInput(
                        label = "Reps",
                        value = draft.targetReps,
                        onValueChange = { draft.targetReps = it },
                        modifier = Modifier.weight(1f)
                    )
                    IntensityInput(
                        label = "Sets",
                        value = draft.targetSets,
                        onValueChange = { draft.targetSets = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
