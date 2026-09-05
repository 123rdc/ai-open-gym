package com.example.gymformcoach.features.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.designsystem.components.GymBottomNavigation
import com.example.gymformcoach.core.navigation.Screen
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * §2.4 plan editor: seven rows, each assigning a routine or rest. A bottom-nav
 * tab destination (Plan), same pattern as Home/Progress/Profile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanScreen(
    onNavigateToTab: (String) -> Unit,
    viewModel: WeeklyPlanViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Plan", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            GymBottomNavigation(currentRoute = Screen.WeeklyPlan.route, onTabSelected = onNavigateToTab)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "WEEK SCHEDULE",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(DayOfWeek.entries.toList()) { day ->
                DayRow(
                    day = day,
                    assignedRoutineId = state.entriesByDay[day.value],
                    hasEntry = state.entriesByDay.containsKey(day.value),
                    routines = state.routines.map { it.id to it.name },
                    onAssign = { routineId -> viewModel.assignDay(day.value, routineId) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ROUTINES",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            if (state.routines.isEmpty()) {
                item {
                    Text(
                        text = "No routines yet. Create one from My Routines.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            items(state.routines) { routine ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = routine.name, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${routine.exerciseCount} exercises",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayRow(
    day: DayOfWeek,
    assignedRoutineId: String?,
    hasEntry: Boolean,
    routines: List<Pair<String, String>>,
    onAssign: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val assignedName = routines.firstOrNull { it.first == assignedRoutineId }?.second

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = day.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(110.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                TextButton(onClick = { expanded = true }) {
                    Text(
                        text = when {
                            assignedName != null -> assignedName
                            hasEntry -> "Rest"
                            else -> "Unassigned"
                        },
                        color = if (assignedName != null) Primary else TextSecondary
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Rest") },
                        onClick = {
                            onAssign(null)
                            expanded = false
                        }
                    )
                    routines.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onAssign(id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
