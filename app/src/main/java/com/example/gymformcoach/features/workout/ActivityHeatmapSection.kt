package com.example.gymformcoach.features.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.ExerciseSessionRepository
import com.example.gymformcoach.core.designsystem.Primary
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * §13.1: full-year, GitHub-style grid across *all* training, not one exercise -
 * distinct from the per-exercise heatmap in [ExerciseHistorySection].
 *
 * Shade is meant to represent time spent training. `ExerciseSession` doesn't
 * carry a duration for weighted REPS sets (only `SetLog.durationSeconds` does,
 * and only for TIMED/CARDIO), so this uses set count per day as the closest
 * available proxy until Phase C/E thread real durations through the history
 * views. Empty state renders as a muted outline grid, never zeros passed off
 * as real data (§18.5).
 */
@Composable
fun ActivityHeatmapSection() {
    val context = LocalContext.current
    val repository = remember { ExerciseSessionRepository(AppDatabase.getInstance(context)) }
    val sessions by repository.getAllSessions().collectAsState(initial = emptyList())
    var selected by remember { mutableStateOf<DaySummary?>(null) }

    Column {
        Text(
            text = "Activity",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (sessions.isEmpty()) {
            HeatmapGrid(daySummaries = emptyList(), muted = true)
        } else {
            val daySummaries = sessions
                .groupBy { startOfDay(it.performedAt) }
                .map { (dayStart, daySessions) ->
                    DaySummary(
                        dayStart = dayStart,
                        volume = daySessions.size.toFloat(),
                        topWeight = daySessions.maxOf { it.weightKg },
                        setCount = daySessions.sumOf { it.sets }
                    )
                }
                .sortedBy { it.dayStart }

            HeatmapGrid(daySummaries = daySummaries, muted = false, onCellClick = { selected = it })

            selected?.let { day ->
                Spacer(modifier = Modifier.height(8.dp))
                val dateLabel = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(day.dayStart)
                Text(
                    text = "$dateLabel  •  ${day.setCount} set(s) logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary
                )
            }
        }
    }
}
