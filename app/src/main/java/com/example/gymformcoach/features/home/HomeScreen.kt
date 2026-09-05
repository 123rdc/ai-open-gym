package com.example.gymformcoach.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.RoutineRepository
import com.example.gymformcoach.core.data.RoutineSummary
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.designsystem.components.SearchField
import com.example.gymformcoach.core.designsystem.components.WorkoutCategoryCard
import com.example.gymformcoach.features.workout.ExerciseCatalog

import com.example.gymformcoach.core.designsystem.components.*
import com.example.gymformcoach.core.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeScreen(
    userName: String = "Sarah",
    onBodyPartSelected: (String) -> Unit,
    onNavigateToTab: (String) -> Unit,
    onCreateRoutine: () -> Unit = {},
    onRoutineSelected: (String) -> Unit = {},
    onSeeAllRoutines: () -> Unit = {},
    onStartPlannedRoutine: (String) -> Unit = {},
    onEditWeeklyPlan: () -> Unit = {},
    onOpenBodyWeight: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val bodyParts = ExerciseCatalog.bodyParts
    val context = LocalContext.current
    val routineRepository = remember { RoutineRepository(AppDatabase.getInstance(context)) }
    val routines by routineRepository.getRoutineSummaries().collectAsState(initial = emptyList())
    val planViewModel: com.example.gymformcoach.features.plan.WeeklyPlanViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val planState by planViewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            GymBottomNavigation(
                currentRoute = Screen.Home.route,
                onTabSelected = onNavigateToTab
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Welcome Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hello $userName,",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
                Text(
                    text = "Good Morning",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                // Profile placeholder
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Primary))
            }
        }

        // Search Bar
        SearchField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search for workouts"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // §2.4: today's planned session is the primary surface.
        com.example.gymformcoach.features.plan.TodayWorkoutCard(
            planned = planState.today,
            routineName = planState.todayRoutineName,
            onStart = onStartPlannedRoutine,
            onEditPlan = onEditWeeklyPlan
        )

        Spacer(modifier = Modifier.height(16.dp))

        val sessionRepository = remember { com.example.gymformcoach.core.data.ExerciseSessionRepository(AppDatabase.getInstance(context)) }
        val allSessions by sessionRepository.getAllSessions().collectAsState(initial = emptyList())
        val loggedDates = remember(allSessions) {
            allSessions.map {
                java.time.Instant.ofEpochMilli(it.performedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            }.toSet()
        }

        WeekCalendarStrip(loggedDates = loggedDates)

        Spacer(modifier = Modifier.height(16.dp))

        BodyWeightPreviewCard(onOpen = onOpenBodyWeight)

        Spacer(modifier = Modifier.height(16.dp))

        StreakCard(stats = remember(loggedDates) { StreakCalculator.calculate(loggedDates) })

        Spacer(modifier = Modifier.height(24.dp))

        // My Routines
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Routines",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (routines.isNotEmpty()) {
                Text(text = "See All", color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onSeeAllRoutines() })
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                CreateRoutineCard(onClick = onCreateRoutine)
            }
            items(routines) { routine ->
                RoutineSummaryCard(routine = routine, onClick = { onRoutineSelected(routine.id) })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Workouts
        Text(
            text = "Workouts",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        bodyParts.forEach { (name, imageUrl) ->
            WorkoutBodyPartItem(
                name = name,
                imageUrl = imageUrl,
                onClick = { onBodyPartSelected(name) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
}

@Composable
fun CreateRoutineCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(120.dp),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create Routine",
                color = Primary,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun RoutineSummaryCard(routine: RoutineSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(120.dp),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = routine.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2
            )
            Column {
                Text(
                    text = "${routine.exerciseCount} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                val lastPerformedLabel = routine.lastPerformedDate?.let {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(it)
                } ?: "Not started yet"
                Text(
                    text = lastPerformedLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary
                )
            }
        }
    }
}

@Composable
fun WorkoutBodyPartItem(
    name: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Training Plan", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(modifier = Modifier.weight(1.0f))
            Text(
                text = "View", 
                color = Primary, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    }
}
