package com.example.gymformcoach.features.routines

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.Exercise
import com.example.gymformcoach.core.data.ExerciseRepository
import com.example.gymformcoach.core.data.RoutineExercise
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.designsystem.components.PrimaryButton
import com.example.gymformcoach.features.workout.toWorkout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    viewModel: RoutineDetailViewModel,
    routineId: String,
    onBack: () -> Unit,
    onStartRoutine: () -> Unit
) {
    LaunchedEffect(routineId) { viewModel.load(routineId) }
    val routine by viewModel.routine.collectAsState()
    val exercises by viewModel.exercises.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = routine?.name ?: "Routine", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            routine?.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (exercises.isEmpty()) {
                // §18.5: designed empty state, never a blank scroll region.
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No exercises in this routine yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(exercises, key = { it.id }) { exercise ->
                        RoutineDetailExerciseRow(exercise)
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                PrimaryButton(
                    text = "Start Routine",
                    onClick = onStartRoutine,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun RoutineDetailExerciseRow(exercise: RoutineExercise) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val exerciseRepository = remember { ExerciseRepository(AppDatabase.getInstance(context)) }
    var catalogEntry by remember(exercise.exerciseId) { mutableStateOf<Exercise?>(null) }
    LaunchedEffect(exercise.exerciseId) {
        catalogEntry = exerciseRepository.findByName(exercise.exerciseId)
    }
    val workout = catalogEntry?.toWorkout()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (workout != null) {
                AsyncImage(
                    model = workout.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = exercise.exerciseId, fontWeight = FontWeight.Bold)
                if (workout != null) {
                    Text(text = workout.muscle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            val context = androidx.compose.ui.platform.LocalContext.current
            val unit = remember { com.example.gymformcoach.core.utils.PreferenceManager(context).weightUnit }
            Text(
                text = "${exercise.targetSets}×${exercise.targetReps} @ ${com.example.gymformcoach.core.utils.WeightUnit.formatKg(exercise.targetWeightKg, unit)}$unit",
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
