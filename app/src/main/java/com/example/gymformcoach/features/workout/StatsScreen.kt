package com.example.gymformcoach.features.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.Surface
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.designsystem.components.GymBottomNavigation
import com.example.gymformcoach.core.navigation.Screen

private enum class StatsTab { MUSCLE_BALANCE, FATIGUE, STRENGTH }

/**
 * §13.2 (fatigue view) + §13.3 (strength view, already-built progression chart)
 * behind one set of tabs, matching the reference design.
 */
@Composable
fun StatsScreen(
    onNavigateToTab: (String) -> Unit,
    viewModel: MuscleFatigueViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var tab by remember { mutableStateOf(StatsTab.FATIGUE) }

    Scaffold(
        bottomBar = { GymBottomNavigation(currentRoute = Screen.Progress.route, onTabSelected = onNavigateToTab) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(text = "Stats", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            TabRow(
                selectedTabIndex = tab.ordinal,
                containerColor = Surface,
                contentColor = Primary
            ) {
                Tab(selected = tab == StatsTab.MUSCLE_BALANCE, onClick = { tab = StatsTab.MUSCLE_BALANCE }, text = { Text("Muscle balance") })
                Tab(selected = tab == StatsTab.FATIGUE, onClick = { tab = StatsTab.FATIGUE }, text = { Text("Fatigue") })
                Tab(selected = tab == StatsTab.STRENGTH, onClick = { tab = StatsTab.STRENGTH }, text = { Text("Strength") })
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (tab) {
                StatsTab.FATIGUE -> FatigueTabContent(state, viewModel)
                StatsTab.MUSCLE_BALANCE -> MuscleBalanceTabContent(state)
                StatsTab.STRENGTH -> {
                    ActivityHeatmapSection()
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(text = "Progressive Overload", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    ProgressChart()
                }
            }
        }
    }
}

@Composable
private fun FatigueTabContent(state: MuscleFatigueUiState, viewModel: MuscleFatigueViewModel) {
    if (state.fatigueByGroup.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            colors = CardDefaults.cardColors(containerColor = Surface)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Log a workout to see fatigue by muscle group.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    MuscleFatigueDiagram(fatigueByGroup = state.fatigueByGroup)
    Spacer(modifier = Modifier.height(8.dp))
    FatigueLegend()
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Fatigue shows how recently each muscle was trained. High means rest.",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
    )

    Spacer(modifier = Modifier.height(24.dp))
    Text(text = "Effort · how close to failure", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EffortWindow.entries.forEach { window ->
            FilterChip(
                selected = state.effortWindow == window,
                onClick = { viewModel.setEffortWindow(window) },
                label = { Text(windowLabel(window)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary)
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))

    val stats = state.effortStats
    if (stats.averageEffort == null) {
        Text(
            text = "No rated sets in this window - enable effort tracking in Settings to see this.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    } else {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    text = "%.1f".format(stats.averageEffort) + " avg effort",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${stats.percentAtRirThreeOrHarder}%",
                style = MaterialTheme.typography.headlineSmall,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "at RIR 3 or harder",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${stats.ratedSets} of ${stats.totalSets} finished sets rated",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun MuscleBalanceTabContent(state: MuscleFatigueUiState) {
    // Reuses the same region data as a simple weekly-volume proxy: groups
    // trained recently vs. not, per §13.2's "names untrained groups" requirement.
    val untrained = state.fatigueByGroup.filter { it.level == com.example.gymformcoach.core.progression.MuscleFatigue.Level.UNTRAINED }
    Text(text = "Muscle balance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
    MuscleFatigueDiagram(fatigueByGroup = state.fatigueByGroup)
    Spacer(modifier = Modifier.height(12.dp))
    if (untrained.isNotEmpty()) {
        Text(
            text = "Not trained recently: ${untrained.joinToString(", ") { it.muscleGroup }}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    } else {
        Text(
            text = "Every tracked muscle group has been trained recently.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

private fun windowLabel(window: EffortWindow) = when (window) {
    EffortWindow.THIRTY -> "30d"
    EffortWindow.NINETY -> "90d"
    EffortWindow.YEAR -> "1Y"
    EffortWindow.ALL -> "All"
}
