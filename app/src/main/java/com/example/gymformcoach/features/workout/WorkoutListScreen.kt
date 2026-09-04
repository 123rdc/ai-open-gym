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

    fun allWorkouts(): List<Workout> = bodyParts.flatMap { (bodyPart, _) -> getWorkoutsForBodyPart(bodyPart) }

    fun findByName(name: String): Workout? = allWorkouts().firstOrNull { it.name == name }
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
    val allWorkouts = getWorkoutsForBodyPart(bodyPart)
    val workouts = if (selectionMode) {
        val equipment = remember { com.example.gymformcoach.core.utils.PreferenceManager(context).equipment }
        remember(allWorkouts, equipment) {
            com.example.gymformcoach.core.recommendation.DefaultRecommendationEngine()
                .filterExercisesByEquipment(allWorkouts, equipment)
        }
    } else {
        allWorkouts
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "$bodyPart Workouts", fontWeight = FontWeight.Bold) },
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

fun getWorkoutsForBodyPart(bodyPart: String): List<Workout> {
    return when (bodyPart) {
        "Back" -> listOf(
            Workout("Deadlift", "45 mins", "Advanced", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Lats & Lower Back", "Free Weight", "The king of all exercises. Focus on keeping your back straight and driving through your heels."),
            Workout("Lat Pulldown", "30 mins", "Beginner", "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&q=80&w=500", "Lats", "Machine", "Pull the bar down to your upper chest while leaning back slightly. Squeeze your shoulder blades together."),
            Workout("T-Bar Row", "35 mins", "Intermediate", "https://images.unsplash.com/photo-1590239098509-e010d88db81a?auto=format&fit=crop&q=80&w=500", "Upper Back", "Free Weight", "A great mass builder for the mid-back. Keep your chest up and elbows tucked."),
            Workout("Seated Cable Row", "30 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Mid Back", "Cable", "Focus on the stretch and the squeeze. Don't use momentum to pull the weight."),
            Workout("Pull-ups", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500", "Lats", "Bodyweight", "A fundamental bodyweight exercise for back width. Use a wide grip for maximum lat engagement."),
            Workout("Single Arm DB Row", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Lats & Rhomboids", "Dumbbell", "Perform one arm at a time to correct muscle imbalances and improve core stability."),
            Workout("Barbell Row", "35 mins", "Intermediate", "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500", "Back", "Free Weight", "Hinge at the hips and pull the bar to your lower ribs. Keep your core tight."),
            Workout("Straight Arm Pulldown", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Lats", "Cable", "Keep your arms straight and pull the bar to your thighs. Excellent for lat isolation."),
            Workout("Face Pulls", "20 mins", "Beginner", "https://images.unsplash.com/photo-1591741535018-d042766c62eb?auto=format&fit=crop&q=80&w=500", "Rear Delts & Traps", "Cable", "Pull the rope towards your forehead, flaring your elbows out. Focus on the rear delts."),
            Workout("Hyperextensions", "15 mins", "Beginner", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500", "Lower Back", "Bodyweight", "Hinge at the hips on a 45-degree bench. Do not overextend your spine at the top."),
            Workout("Chest Supported Row", "30 mins", "Beginner", "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&q=80&w=500", "Mid Back", "Machine", "Lying face down on an incline bench, row dumbbells or use a machine to isolate the back."),
            Workout("Dumbbell Pullover", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Lats & Serratus", "Dumbbell", "Lying on a bench, lower a dumbbell behind your head and pull it back over your chest."),
            Workout("Renegade Row", "30 mins", "Advanced", "https://images.unsplash.com/photo-1598971639058-fab3c32f850b?auto=format&fit=crop&q=80&w=500", "Back & Core", "Dumbbell", "Perform a row from a plank position. Great for stability and back strength."),
            Workout("Wide Grip Lat Pulldown", "30 mins", "Beginner", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=500", "Lats", "Machine", "Focuses on the outer lats to build a wide V-taper."),
            Workout("Reverse Fly", "20 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Rear Delts", "Dumbbell", "Bend forward and fly weights out to the side to target the upper back and rear delts."),
            Workout("Rack Pulls", "35 mins", "Intermediate", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Lower Back & Traps", "Free Weight", "A partial deadlift performed from the power rack. Allows for heavier loads."),
            Workout("Meadows Row", "30 mins", "Advanced", "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500", "Back", "Free Weight", "A unique rowing variation using a landmine setup for intense back activation.")
        )
        "Chest" -> listOf(
            Workout("Bench Press", "40 mins", "Intermediate", "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?auto=format&fit=crop&q=80&w=500", "Chest", "Free Weight", "The standard for chest strength. Keep your feet planted and arch your lower back slightly."),
            Workout("Incline DB Press", "35 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Upper Chest", "Dumbbell", "Targets the upper pectoral muscles. Use a 30-45 degree incline."),
            Workout("Chest Fly", "30 mins", "Beginner", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=500", "Chest", "Machine", "Isolates the chest muscles. Focus on a wide arc and a deep stretch."),
            Workout("Push-ups", "20 mins", "Beginner", "https://images.unsplash.com/photo-1598971639058-fab3c32f850b?auto=format&fit=crop&q=80&w=500", "Chest & Triceps", "Bodyweight", "The classic bodyweight chest builder. Keep your body in a straight line."),
            Workout("Pec Deck", "25 mins", "Beginner", "https://images.unsplash.com/photo-1546483875-ad9014c88eba?auto=format&fit=crop&q=80&w=500", "Chest", "Machine", "Provides constant tension on the chest. Great for finishing off a chest workout."),
            Workout("Dips", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1583454110551-21f2fa29617b?auto=format&fit=crop&q=80&w=500", "Lower Chest", "Bodyweight", "Lean forward to target the chest more than the triceps."),
            Workout("Decline Barbell Press", "35 mins", "Intermediate", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=500", "Lower Chest", "Free Weight", "Targets the lower part of the chest. Helps build overall chest thickness."),
            Workout("Cable Crossover", "30 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Chest", "Cable", "Provides constant tension. Cross your hands at the bottom for a peak contraction."),
            Workout("Incline Barbell Press", "35 mins", "Intermediate", "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?auto=format&fit=crop&q=80&w=500", "Upper Chest", "Free Weight", "Great for adding mass to the upper chest area."),
            Workout("Dumbbell Fly", "25 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Chest", "Dumbbell", "Isolate the pecs by maintaining a slight bend in the elbows throughout."),
            Workout("Landmine Press", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Upper Chest", "Free Weight", "Excellent for shoulder health and targeting the upper chest and inner pecs."),
            Workout("Hammer Strength Press", "30 mins", "Beginner", "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&q=80&w=500", "Chest", "Machine", "Allows for heavy weight with more stability than free weights."),
            Workout("Diamond Push-ups", "20 mins", "Intermediate", "https://images.unsplash.com/photo-1598971639058-fab3c32f850b?auto=format&fit=crop&q=80&w=500", "Chest & Triceps", "Bodyweight", "A push-up variation with hands forming a diamond to emphasize triceps and inner chest."),
            Workout("Low-to-High Cable Fly", "25 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Upper Chest", "Cable", "Targets the upper chest by pulling from a low position to head height."),
            Workout("Floor Press", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Chest & Triceps", "Dumbbell", "Lying on the floor limits the range of motion, focusing on the lockout."),
            Workout("Svend Press", "20 mins", "Beginner", "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?auto=format&fit=crop&q=80&w=500", "Chest", "Free Weight", "Press two plates together in front of your chest to create intense tension.")
        )
        "Shoulders" -> listOf(
            Workout("Overhead Press", "40 mins", "Intermediate", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Shoulders", "Free Weight", "The foundational vertical press for shoulder mass and strength."),
            Workout("Lateral Raise", "25 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Side Delts", "Dumbbell", "Essential for building shoulder width. Keep your pinky fingers slightly higher."),
            Workout("Front Raise", "20 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Front Delts", "Dumbbell", "Isolate the front of the shoulders. Avoid using momentum."),
            Workout("Arnold Press", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Shoulders", "Dumbbell", "A rotating press that targets all three heads of the deltoids."),
            Workout("Rear Delt Fly", "25 mins", "Beginner", "https://images.unsplash.com/photo-1546483875-ad9014c88eba?auto=format&fit=crop&q=80&w=500", "Rear Delts", "Machine", "Focus on the back of the shoulder. Don't let your traps take over."),
            Workout("Face Pull", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Rear Delts", "Cable", "Excellent for shoulder health and posture. Pull towards your face."),
            Workout("Smith Machine Press", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&q=80&w=500", "Shoulders", "Machine", "Allows for heavy weight with controlled movement."),
            Workout("Upright Row", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500", "Traps & Side Delts", "Free Weight", "Pull the bar up towards your chin. Keep it close to your body."),
            Workout("Cable Lateral Raise", "25 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Side Delts", "Cable", "Provides constant tension throughout the whole range of motion."),
            Workout("Shrugs", "20 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Traps", "Dumbbell", "Build the upper traps by shrugging your shoulders towards your ears."),
            Workout("Pike Push-ups", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1598971639058-fab3c32f850b?auto=format&fit=crop&q=80&w=500", "Shoulders", "Bodyweight", "A bodyweight alternative to the overhead press. Form a 'V' shape."),
            Workout("Military Press", "35 mins", "Advanced", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Shoulders", "Free Weight", "Strict standing overhead press with feet together."),
            Workout("Bus Drivers", "15 mins", "Beginner", "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?auto=format&fit=crop&q=80&w=500", "Shoulders", "Free Weight", "Hold a plate in front of you and rotate it like a steering wheel."),
            Workout("Push Press", "40 mins", "Advanced", "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500", "Shoulders & Legs", "Free Weight", "Use leg drive to help press heavy weight overhead."),
            Workout("Cable Front Raise", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Front Delts", "Cable", "Provides consistent tension for isolating the front deltoids."),
            Workout("Machine Shoulder Press", "30 mins", "Beginner", "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&q=80&w=500", "Shoulders", "Machine", "Safe and effective way to target the entire shoulder complex.")
        )
        "Legs" -> listOf(
            Workout("Squat", "50 mins", "Intermediate", "https://images.unsplash.com/photo-1574680096145-d05b474e2155?auto=format&fit=crop&q=80&w=500", "Quads & Glutes", "Free Weight", "The foundation of lower body training. Sit back into your heels and keep your chest up."),
            Workout("Leg Press", "35 mins", "Beginner", "https://images.unsplash.com/photo-1574680676196-932109470a00?auto=format&fit=crop&q=80&w=500", "Quads", "Machine", "A great alternative to squats. Don't lock your knees at the top."),
            Workout("Leg Extension", "25 mins", "Beginner", "https://images.unsplash.com/photo-1434682881908-b43d0467b798?auto=format&fit=crop&q=80&w=500", "Quads", "Machine", "Isolates the quadriceps. Focus on the squeeze at the top."),
            Workout("Lying Leg Curl", "25 mins", "Beginner", "https://images.unsplash.com/photo-1590239098509-e010d88db81a?auto=format&fit=crop&q=80&w=500", "Hamstrings", "Machine", "Targets the hamstrings. Keep your hips pressed into the pad."),
            Workout("Romanian Deadlift", "35 mins", "Intermediate", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Hamstrings & Glutes", "Free Weight", "Focus on the hinge at your hips. Keep the bar close to your shins."),
            Workout("Calf Raises", "20 mins", "Beginner", "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?auto=format&fit=crop&q=80&w=500", "Calves", "Machine", "Fully stretch and contract your calves. Hold at the top for a second."),
            Workout("Lunges", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1574680096145-d05b474e2155?auto=format&fit=crop&q=80&w=500", "Quads & Glutes", "Dumbbell", "Step forward and lower your hips until both knees are bent at a 90-degree angle."),
            Workout("Bulgarian Split Squat", "35 mins", "Intermediate", "https://images.unsplash.com/photo-1583454110551-21f2fa29617b?auto=format&fit=crop&q=80&w=500", "Quads & Glutes", "Dumbbell", "Elevate one foot behind you and squat. Incredible for building leg strength."),
            Workout("Goblet Squat", "30 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Quads", "Dumbbell", "Hold a dumbbell at your chest. Great for learning proper squat form."),
            Workout("Calf Press", "20 mins", "Beginner", "https://images.unsplash.com/photo-1574680676196-932109470a00?auto=format&fit=crop&q=80&w=500", "Calves", "Machine", "Perform calf raises using the leg press machine for high volume."),
            Workout("Glute Ham Raise", "25 mins", "Advanced", "https://images.unsplash.com/photo-1598971639058-fab3c32f850b?auto=format&fit=crop&q=80&w=500", "Hamstrings & Glutes", "Bodyweight", "A powerful movement for the entire posterior chain."),
            Workout("Step-ups", "25 mins", "Beginner", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500", "Quads & Glutes", "Bodyweight", "Step up onto a bench or box. Focus on pushing through the heel."),
            Workout("Hack Squat", "40 mins", "Intermediate", "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&q=80&w=500", "Quads", "Machine", "Allows you to target the quads with a fixed range of motion."),
            Workout("Box Jumps", "20 mins", "Intermediate", "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500", "Quads & Calves", "Bodyweight", "Explosive movement to build power in the legs."),
            Workout("Seated Leg Curl", "25 mins", "Beginner", "https://images.unsplash.com/photo-1434682881908-b43d0467b798?auto=format&fit=crop&q=80&w=500", "Hamstrings", "Machine", "Isolate the hamstrings from a seated position."),
            Workout("Hip Thrusts", "40 mins", "Intermediate", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Glutes", "Free Weight", "The best exercise for building glute mass and strength.")
        )
        "Bicep" -> listOf(
            Workout("Bicep Curls", "20 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Biceps", "Free Weight", "The foundational bicep exercise. Don't swing your body."),
            Workout("Hammer Curls", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Brachialis", "Dumbbell", "Targets the brachialis for thicker-looking arms. Keep palms facing each other."),
            Workout("Preacher Curl", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Biceps", "Machine", "Eliminates momentum. Focus on the squeeze at the top."),
            Workout("Concentration Curl", "20 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Biceps", "Dumbbell", "Performed while seated. Helps focus entirely on the bicep peak."),
            Workout("Chin-ups", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500", "Biceps & Back", "Bodyweight", "A compound movement that heavily involves the biceps. Use an underhand grip."),
            Workout("Cable Curl", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=500", "Biceps", "Cable", "Provides constant tension throughout the entire range of motion."),
            Workout("EZ Bar Curl", "25 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Biceps", "Free Weight", "The angled bar is easier on the wrists than a straight bar."),
            Workout("Incline DB Curl", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Biceps", "Dumbbell", "Lying on an incline bench puts the biceps in a stretched position."),
            Workout("Spider Curl", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Biceps", "Free Weight", "Lying chest-down on an incline bench isolates the biceps completely."),
            Workout("Zottman Curl", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Biceps & Forearms", "Dumbbell", "A rotation curl that targets both the biceps and the forearms."),
            Workout("Drag Curl", "20 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Biceps", "Free Weight", "Drag the bar up your body to target the long head of the bicep."),
            Workout("Reverse EZ Bar Curl", "20 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Brachioradialis", "Free Weight", "Overhand grip targets the forearms and outer biceps."),
            Workout("High Cable Curl", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=500", "Biceps", "Cable", "Flexing your arms from a high position to target the bicep peak."),
            Workout("Barbell 21s", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Biceps", "Free Weight", "7 partial reps from bottom, 7 from top, and 7 full range of motion reps."),
            Workout("Machine Bicep Curl", "25 mins", "Beginner", "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&q=80&w=500", "Biceps", "Machine", "Consistent resistance throughout the movement for maximum growth."),
            Workout("Rope Cable Curl", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Biceps", "Cable", "Using a rope allows for a more natural wrist position and better squeeze.")
        )
        "Tricep" -> listOf(
            Workout("Tricep Dips", "20 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Triceps", "Bodyweight", "Focus on the triceps by keeping your body upright."),
            Workout("Skull Crushers", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1597452485669-2c7bb5fef90d?auto=format&fit=crop&q=80&w=500", "Triceps", "Free Weight", "Lowers the weight to your forehead. Keep your elbows tucked in."),
            Workout("Rope Pushdown", "20 mins", "Beginner", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Triceps", "Cable", "Pull the rope down and apart at the bottom for a full contraction."),
            Workout("Overhead DB Extension", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Triceps Long Head", "Dumbbell", "Great for stretching the long head of the tricep. Keep your core tight."),
            Workout("Close Grip Bench", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=500", "Triceps", "Free Weight", "A heavy compound movement for tricep thickness. Keep hands shoulder-width apart."),
            Workout("Kickbacks", "20 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Triceps", "Dumbbell", "Isolates the tricep. Keep your upper arm parallel to the floor."),
            Workout("Bench Dips", "15 mins", "Beginner", "https://images.unsplash.com/photo-1598971639058-fab3c32f850b?auto=format&fit=crop&q=80&w=500", "Triceps", "Bodyweight", "A beginner-friendly way to target the triceps using a bench or chair."),
            Workout("Straight Bar Pushdown", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Triceps", "Cable", "Allows for heavier loads than the rope attachment."),
            Workout("Single Arm Extension", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Triceps", "Cable", "Isolate each tricep to correct imbalances and improve mind-muscle connection."),
            Workout("JM Press", "30 mins", "Advanced", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Triceps", "Free Weight", "A hybrid between a close-grip bench and a skull crusher."),
            Workout("French Press", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Triceps", "Free Weight", "Seated or standing extension using an EZ bar behind the head."),
            Workout("Diamond Push-ups", "20 mins", "Intermediate", "https://images.unsplash.com/photo-1598971639058-fab3c32f850b?auto=format&fit=crop&q=80&w=500", "Triceps", "Bodyweight", "Targets the triceps intensely with a narrow hand position."),
            Workout("Tate Press", "25 mins", "Advanced", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Triceps", "Dumbbell", "Dumbbell extension focusing on the lateral head of the tricep."),
            Workout("Machine Tricep Press", "30 mins", "Beginner", "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&q=80&w=500", "Triceps", "Machine", "Similar to a dip but performed in a seated machine for stability."),
            Workout("Overhead Cable Extension", "25 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Triceps", "Cable", "Provides a deep stretch to the long head of the tricep."),
            Workout("Bodyweight Extension", "20 mins", "Advanced", "https://images.unsplash.com/photo-1598971639058-fab3c32f850b?auto=format&fit=crop&q=80&w=500", "Triceps", "Bodyweight", "Lowering your head under a bar to target triceps using your body weight.")
        )
        "Core" -> listOf(
            Workout("Plank", "15 mins", "Beginner", "https://images.unsplash.com/photo-1566241142559-40e1bfc26eb7?auto=format&fit=crop&q=80&w=500", "Abs", "Bodyweight", "Maintain a straight line from head to heels. Don't let your hips sag."),
            Workout("Russian Twists", "20 mins", "Intermediate", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500", "Obliques", "Bodyweight", "Rotate your torso from side to side. Use a weight for extra challenge."),
            Workout("Hanging Leg Raise", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500", "Lower Abs", "Bodyweight", "One of the best lower ab exercises. Don't swing your legs."),
            Workout("Cable Crunch", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=500", "Upper Abs", "Cable", "Kneel down and crunch your elbows toward your knees."),
            Workout("Ab Wheel", "20 mins", "Advanced", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500", "Abs", "Bodyweight", "Roll forward slowly and pull back using your abs. Don't overextend."),
            Workout("Bicycle Curls", "20 mins", "Beginner", "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?auto=format&fit=crop&q=80&w=500", "Abs & Obliques", "Bodyweight", "Focus on bringing your elbow to the opposite knee."),
            Workout("Mountain Climbers", "15 mins", "Beginner", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500", "Core & Cardio", "Bodyweight", "Drive your knees towards your chest in a plank position."),
            Workout("V-Sits", "20 mins", "Advanced", "https://images.unsplash.com/photo-1566241142559-40e1bfc26eb7?auto=format&fit=crop&q=80&w=500", "Abs", "Bodyweight", "Lift your legs and torso simultaneously to form a 'V' shape."),
            Workout("Dead Bug", "15 mins", "Beginner", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500", "Core Stability", "Bodyweight", "Lower opposite arm and leg while keeping your back flat on the floor."),
            Workout("Bird Dog", "15 mins", "Beginner", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500", "Lower Back & Core", "Bodyweight", "Extend opposite arm and leg from a hands-and-knees position."),
            Workout("Woodchoppers", "20 mins", "Intermediate", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Obliques", "Cable", "Rotate your torso against resistance, pulling from high to low or low to high."),
            Workout("Leg Raises", "20 mins", "Beginner", "https://images.unsplash.com/photo-1566241142559-40e1bfc26eb7?auto=format&fit=crop&q=80&w=500", "Lower Abs", "Bodyweight", "Lying on your back, lift your legs to 90 degrees and lower them slowly."),
            Workout("Flutter Kicks", "15 mins", "Beginner", "https://images.unsplash.com/photo-1566241142559-40e1bfc26eb7?auto=format&fit=crop&q=80&w=500", "Lower Abs", "Bodyweight", "Keep your legs straight and kick them in a small range of motion."),
            Workout("Side Plank", "15 mins", "Beginner", "https://images.unsplash.com/photo-1566241142559-40e1bfc26eb7?auto=format&fit=crop&q=80&w=500", "Obliques", "Bodyweight", "Support your body on one forearm and the side of your foot."),
            Workout("Toe Touches", "20 mins", "Beginner", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500", "Abs", "Bodyweight", "Reach for your toes with your legs straight in the air."),
            Workout("Sit-ups", "20 mins", "Beginner", "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?auto=format&fit=crop&q=80&w=500", "Abs", "Bodyweight", "The classic core exercise. Focus on using your abs, not your neck.")
        )
        else -> emptyList()
    }
}
