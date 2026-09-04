package com.example.gymformcoach.features.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.gymformcoach.core.designsystem.Background
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.components.PrimaryButton
import com.example.gymformcoach.core.utils.PreferenceManager

@Composable
fun SetupBaseScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(48.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            content()
        }

        PrimaryButton(
            text = "Next",
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun GenderScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    var selectedGender by remember { mutableStateOf(prefs.gender) }
    SetupBaseScreen(
        title = "Tell Us About Yourself",
        subtitle = "To give you a better experience we need to know your gender",
        onBack = onBack,
        onNext = { prefs.gender = selectedGender; onNext() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            GenderItem("Male", selectedGender == "Male") { selectedGender = "Male" }
            GenderItem("Female", selectedGender == "Female") { selectedGender = "Female" }
        }
    }
}

@Composable
fun GenderItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(
                if (isSelected) Primary else Color.Transparent,
                shape = MaterialTheme.shapes.extraLarge
            )
            .clickable { onClick() }
            .padding(2.dp)
            .background(
                if (isSelected) Primary else Color.Gray.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.extraLarge
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AgeScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    var age by remember { mutableFloatStateOf(prefs.age.toFloat()) }
    SetupBaseScreen(
        title = "How Old Are You?",
        subtitle = "Age helps us personalize your fitness plan",
        onBack = onBack,
        onNext = { prefs.age = age.toInt(); onNext() }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = age.toInt().toString(),
                style = MaterialTheme.typography.displayLarge,
                color = Primary,
                fontWeight = FontWeight.Black
            )
            Slider(
                value = age,
                onValueChange = { age = it },
                valueRange = 10f..80f,
                colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
            )
        }
    }
}

@Composable
fun WeightScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    var weight by remember { mutableFloatStateOf(prefs.weightKg) }
    SetupBaseScreen(
        title = "What's Your Weight?",
        subtitle = "You can change this later in settings",
        onBack = onBack,
        onNext = { prefs.weightKg = weight; onNext() }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${weight.toInt()} kg",
                style = MaterialTheme.typography.displayLarge,
                color = Primary,
                fontWeight = FontWeight.Black
            )
            Slider(
                value = weight,
                onValueChange = { weight = it },
                valueRange = 30f..150f,
                colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
            )
        }
    }
}

@Composable
fun HeightScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    var height by remember { mutableFloatStateOf(prefs.heightCm) }
    SetupBaseScreen(
        title = "What's Your Height?",
        subtitle = "Height is used for calculating your BMI",
        onBack = onBack,
        onNext = { prefs.heightCm = height; onNext() }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${height.toInt()} cm",
                style = MaterialTheme.typography.displayLarge,
                color = Primary,
                fontWeight = FontWeight.Black
            )
            Slider(
                value = height,
                onValueChange = { height = it },
                valueRange = 100f..220f,
                colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
            )
        }
    }
}

@Composable
fun GoalScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    val goals = listOf("Gain Weight", "Lose Weight", "Get Fitter", "Stay Healthy")
    var selectedGoal by remember { mutableStateOf(prefs.fitnessGoal.takeIf { it in goals } ?: goals[0]) }
    SetupBaseScreen(
        title = "What is Your Goal?",
        subtitle = "Help us tailor the experience to your needs",
        onBack = onBack,
        onNext = { prefs.fitnessGoal = selectedGoal; onNext() }
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(goals) { goal ->
                GoalItem(goal, goal == selectedGoal) { selectedGoal = goal }
            }
        }
    }
}

@Composable
fun GoalItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Primary else Color.Gray.copy(alpha = 0.1f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (isSelected) Color.Black else Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ActivityLevelScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    val levels = listOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Extra Active")
    var selectedLevel by remember { mutableStateOf(prefs.activityLevel.takeIf { it in levels } ?: levels[2]) }
    SetupBaseScreen(
        title = "Activity Level",
        subtitle = "How active are you on a daily basis?",
        onBack = onBack,
        onNext = { prefs.activityLevel = selectedLevel; onNext() }
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(levels) { level ->
                GoalItem(level, level == selectedLevel) { selectedLevel = level }
            }
        }
    }
}

@Composable
fun TrainingExperienceScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    val levels = listOf("Beginner (<6 months)", "Intermediate (6mo-2yr)", "Advanced (2yr+)")
    var selected by remember { mutableStateOf(prefs.trainingExperience.takeIf { it in levels } ?: levels[0]) }
    SetupBaseScreen(
        title = "Training Experience",
        subtitle = "This helps us calibrate suggestions and how much explanation you'll see",
        onBack = onBack,
        onNext = { prefs.trainingExperience = selected; onNext() }
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(levels) { level ->
                GoalItem(level, level == selected) { selected = level }
            }
        }
    }
}

@Composable
fun EquipmentScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    val options = listOf("Full gym", "Dumbbells only", "Barbell + rack", "Bodyweight only", "Resistance bands")
    val selected = remember { mutableStateListOf<String>().apply { addAll(prefs.equipment.filter { it in options }) } }
    SetupBaseScreen(
        title = "Available Equipment",
        subtitle = "Select everything you have access to — we'll filter suggestions to match",
        onBack = onBack,
        onNext = { prefs.equipment = selected.toSet(); onNext() }
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(options) { option ->
                MultiSelectItem(option, option in selected) {
                    if (option in selected) selected.remove(option) else selected.add(option)
                }
            }
        }
    }
}

@Composable
fun TrainingSplitScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    val splits = listOf("Full body", "Upper-lower", "Push-pull-legs", "Bro split", "No preference")
    var selected by remember { mutableStateOf(prefs.trainingSplit.takeIf { it in splits } ?: splits[4]) }
    SetupBaseScreen(
        title = "Training Split Preference",
        subtitle = "We'll use this to seed suggested routine templates",
        onBack = onBack,
        onNext = { prefs.trainingSplit = selected; onNext() }
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(splits) { split ->
                GoalItem(split, split == selected) { selected = split }
            }
        }
    }
}

@Composable
fun FocusAreasScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    val bodyParts = listOf("Chest", "Back", "Shoulders", "Legs", "Bicep", "Tricep", "Core")
    val selected = remember { mutableStateListOf<String>().apply { addAll(prefs.focusAreas.filter { it in bodyParts }) } }
    SetupBaseScreen(
        title = "Priority Areas",
        subtitle = "Which areas do you want to prioritize?",
        onBack = onBack,
        onNext = { prefs.focusAreas = selected.toSet(); onNext() }
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(bodyParts) { part ->
                MultiSelectItem(part, part in selected) {
                    if (part in selected) selected.remove(part) else selected.add(part)
                }
            }
        }
    }
}

@Composable
fun SessionLengthScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    val lengths = listOf("15-30 min", "30-45 min", "45-60 min", "60+ min")
    var selected by remember { mutableStateOf(prefs.sessionLengthPreference.takeIf { it in lengths } ?: lengths[1]) }
    SetupBaseScreen(
        title = "Session Length",
        subtitle = "How long do you usually train for?",
        onBack = onBack,
        onNext = { prefs.sessionLengthPreference = selected; onNext() }
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(lengths) { length ->
                GoalItem(length, length == selected) { selected = length }
            }
        }
    }
}

@Composable
fun TrainingLimitationsScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    var limitations by remember { mutableStateOf(prefs.trainingLimitations) }
    SetupBaseScreen(
        title = "Movements to Avoid",
        subtitle = "Optional — list any exercises or movements you'd rather skip",
        onBack = onBack,
        onNext = { prefs.trainingLimitations = limitations; onNext() }
    ) {
        OutlinedTextField(
            value = limitations,
            onValueChange = { limitations = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. overhead pressing, deep lunges") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                cursorColor = Primary
            )
        )
    }
}

@Composable
fun MultiSelectItem(label: String, isSelected: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Primary else Color.Gray.copy(alpha = 0.1f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = if (isSelected) Color.Black else Color.White,
                fontWeight = FontWeight.Bold
            )
            if (isSelected) {
                Text(text = "✓", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
