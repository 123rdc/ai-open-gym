package com.example.gymformcoach.features.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.Exercise
import com.example.gymformcoach.core.data.ExerciseRepository
import kotlinx.coroutines.launch
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.designsystem.Background

data class Workout(
    val name: String,
    val duration: String,
    val difficulty: String,
    val imageUrl: String,
    val muscle: String,
    val category: String, // "Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight", "Kettlebell"
    val about: String
)

/**
 * The exercise catalog is now DB-backed (`Exercise`, seeded by
 * `ExerciseSeedData` in `MIGRATION_5_6`) - §1/§20.3. `Workout` remains the
 * UI-layer display shape so the existing card Composables don't need to
 * change; this maps the persisted entity down to it.
 */
fun Exercise.toWorkout(): Workout = Workout(
    name = name,
    duration = duration,
    difficulty = difficulty,
    imageUrl = imageUrl,
    muscle = muscle,
    category = category,
    about = about
)

object ExerciseCatalog {
    val bodyParts = listOf(
        "Back" to "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500",
        "Chest" to "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?auto=format&fit=crop&q=80&w=500",
        "Tricep" to "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=500",
        "Bicep" to "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500",
        "Legs" to "https://images.unsplash.com/photo-1574680096145-d05b474e2155?auto=format&fit=crop&q=80&w=500",
        "Shoulders" to "https://images.unsplash.com/photo-1532029837206-abbe2b7620e3?auto=format&fit=crop&q=80&w=500",
        "Core" to "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutListScreen(
    bodyPart: String,
    onBack: () -> Unit,
    onStartWorkout: (String, Float, Int, Int) -> Unit,
    selectionMode: Boolean = false,
    onExerciseSelected: (Workout) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val exerciseRepository = remember { ExerciseRepository(AppDatabase.getInstance(context)) }
    val exercises by exerciseRepository.getByBodyPart(bodyPart).collectAsState(initial = emptyList())
    val allWorkouts = remember(exercises) { exercises.map { it.toWorkout() } }
    val workouts = if (selectionMode) {
        val equipment = remember { com.example.gymformcoach.core.utils.PreferenceManager(context).equipment }
        remember(allWorkouts, equipment) {
            com.example.gymformcoach.core.recommendation.DefaultRecommendationEngine()
                .filterExercisesByEquipment(allWorkouts, equipment)
        }
    } else {
        allWorkouts
    }
    var showAddCustom by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "$bodyPart Workouts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // §15.2: custom exercises behave identically to built-ins everywhere -
                    // the only difference is isCustom=true and no demo animation.
                    IconButton(onClick = { showAddCustom = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add custom exercise")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(workouts) { workout ->
                if (selectionMode) {
                    SelectableWorkoutCard(workout = workout, onSelect = onExerciseSelected)
                } else {
                    WorkoutCard(workout = workout, onStart = onStartWorkout)
                }
            }
        }
    }

    if (showAddCustom) {
        AddCustomExerciseDialog(
            bodyPart = bodyPart,
            onDismiss = { showAddCustom = false },
            onSave = { name ->
                scope.launch {
                    exerciseRepository.addCustomExercise(
                        com.example.gymformcoach.core.data.Exercise(
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
                    showAddCustom = false
                }
            }
        )
    }
}

@Composable
private fun AddCustomExerciseDialog(
    bodyPart: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New $bodyPart exercise") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Exercise name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim()) }, enabled = name.isNotBlank()) {
                Text("Add", color = Primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SelectableWorkoutCard(
    workout: Workout,
    onSelect: (Workout) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = { onSelect(workout) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = workout.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorkoutTag(text = workout.muscle)
                    WorkoutTag(text = workout.category)
                }
            }
            Icon(Icons.Default.Add, contentDescription = "Add to routine", tint = Primary)
        }
    }
}

@Composable
fun WorkoutCard(
    workout: Workout,
    onStart: (String, Float, Int, Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val unit = remember { com.example.gymformcoach.core.utils.PreferenceManager(context).weightUnit }
    var weightDisplay by remember { mutableStateOf(com.example.gymformcoach.core.utils.WeightUnit.formatKg(60f, unit)) }
    var targetReps by remember { mutableStateOf("12") }
    var sets by remember { mutableStateOf("3") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = { expanded = !expanded }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = workout.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            WorkoutTag(text = workout.muscle)
                            WorkoutTag(text = workout.category)
                        }
                        Text(
                            text = workout.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${workout.difficulty} | ${workout.duration}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }

            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // About Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "FORM GUIDE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Primary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = workout.about,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Justify
                        )
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    ExerciseHistorySection(exerciseName = workout.name)

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        IntensityInput(
                            label = "Weight ($unit)",
                            value = weightDisplay,
                            onValueChange = { weightDisplay = it },
                            modifier = Modifier.weight(1f)
                        )
                        IntensityInput(
                            label = "Reps",
                            value = targetReps,
                            onValueChange = { targetReps = it },
                            modifier = Modifier.weight(1f)
                        )
                        IntensityInput(
                            label = "Sets",
                            value = sets,
                            onValueChange = { sets = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val w = com.example.gymformcoach.core.utils.WeightUnit.displayToKg(weightDisplay.toFloatOrNull() ?: 0f, unit)
                            val r = targetReps.toIntOrNull() ?: 0
                            val s = sets.toIntOrNull() ?: 0
                            onStart(workout.name, w, r, s)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "START WORKOUT",
                            color = Background,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutTag(text: String) {
    Surface(
        color = Primary.copy(alpha = 0.9f),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Background
        )
    }
}

@Composable
fun IntensityInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                cursorColor = Primary
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}
