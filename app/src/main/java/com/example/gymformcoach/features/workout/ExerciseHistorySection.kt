package com.example.gymformcoach.features.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.ExerciseSession
import com.example.gymformcoach.core.data.ExerciseSessionRepository
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val HEATMAP_DAYS = 112 // ~16 weeks

fun startOfDay(millis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

data class DaySummary(val dayStart: Long, val volume: Float, val topWeight: Float, val setCount: Int)

@Composable
fun ExerciseHistorySection(exerciseName: String) {
    val context = LocalContext.current
    val repository = remember { ExerciseSessionRepository(AppDatabase.getInstance(context)) }
    val sessions by repository.getSessionsForExercise(exerciseName).collectAsState(initial = emptyList())

    Column {
        Text(
            text = "Your History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (sessions.isEmpty()) {
            EmptyHistoryPlaceholder()
        } else {
            val daySummaries = sessions
                .groupBy { startOfDay(it.performedAt) }
                .map { (dayStart, daySessions) ->
                    DaySummary(
                        dayStart = dayStart,
                        volume = daySessions.sumOf { it.volumeScore.toDouble() }.toFloat(),
                        topWeight = daySessions.maxOf { it.weightKg },
                        setCount = daySessions.sumOf { it.sets }
                    )
                }
                .sortedBy { it.dayStart }

            ExerciseHeatmap(daySummaries)
            Spacer(modifier = Modifier.height(20.dp))
            ExerciseProgressionChart(daySummaries)
        }
    }
}

@Composable
private fun EmptyHistoryPlaceholder() {
    Column {
        HeatmapGrid(daySummaries = emptyList(), muted = true)
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.25f))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 16.dp)) {
                    val path = Path().apply {
                        moveTo(0f, size.height * 0.6f)
                        lineTo(size.width * 0.3f, size.height * 0.5f)
                        lineTo(size.width * 0.6f, size.height * 0.55f)
                        lineTo(size.width, size.height * 0.4f)
                    }
                    drawPath(path, color = Color.Gray.copy(alpha = 0.3f), style = Stroke(width = 2.dp.toPx()))
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Your progress will show up here after your first session",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun ExerciseHeatmap(daySummaries: List<DaySummary>) {
    var selected by remember { mutableStateOf<DaySummary?>(null) }
    val context = LocalContext.current
    val unit = remember { com.example.gymformcoach.core.utils.PreferenceManager(context).weightUnit }

    HeatmapGrid(daySummaries = daySummaries, muted = false, onCellClick = { selected = it })

    selected?.let { day ->
        Spacer(modifier = Modifier.height(8.dp))
        val dateLabel = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(day.dayStart)
        val topWeightLabel = com.example.gymformcoach.core.utils.WeightUnit.formatKg(day.topWeight, unit)
        Text(
            text = "$dateLabel  •  ${day.setCount} set(s)  •  top $topWeightLabel$unit",
            style = MaterialTheme.typography.bodySmall,
            color = Primary
        )
    }
}

@Composable
fun HeatmapGrid(
    daySummaries: List<DaySummary>,
    muted: Boolean,
    onCellClick: (DaySummary) -> Unit = {}
) {
    val today = startOfDay(System.currentTimeMillis())
    val dayInMillis = 24L * 60 * 60 * 1000
    val summaryByDay = daySummaries.associateBy { it.dayStart }
    val maxVolume = daySummaries.maxOfOrNull { it.volume } ?: 0f

    val oldestDay = today - (HEATMAP_DAYS - 1) * dayInMillis
    val weeks = (0 until HEATMAP_DAYS step 7).map { weekOffset ->
        (0 until 7).map { dayOffset ->
            oldestDay + (weekOffset + dayOffset) * dayInMillis
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        weeks.forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { day ->
                    val summary = summaryByDay[day]
                    val intensity = if (summary != null && maxVolume > 0f) {
                        (0.25f + 0.75f * (summary.volume / maxVolume)).coerceIn(0.25f, 1f)
                    } else 0f

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = when {
                                    muted -> Color.Transparent
                                    summary != null -> Primary.copy(alpha = intensity)
                                    else -> Color.Gray.copy(alpha = 0.12f)
                                },
                                shape = RoundedCornerShape(2.dp)
                            )
                            .then(
                                if (muted) Modifier.border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                else Modifier
                            )
                            .then(
                                if (summary != null) Modifier.clickable { onCellClick(summary) } else Modifier
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseProgressionChart(daySummaries: List<DaySummary>) {
    val context = LocalContext.current
    val unit = remember { com.example.gymformcoach.core.utils.PreferenceManager(context).weightUnit }
    val primaryColor = Primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Top Weight ($unit)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val maxWeight = daySummaries.maxOf { it.topWeight }
                val minWeight = daySummaries.minOf { it.topWeight }
                val range = (maxWeight - minWeight).coerceAtLeast(1f)
                val lastIndex = (daySummaries.size - 1).coerceAtLeast(1)
                val stepX = size.width / lastIndex

                fun pointFor(index: Int): Offset {
                    val weight = daySummaries[index].topWeight
                    val x = stepX * index
                    val y = size.height - ((weight - minWeight) / range) * size.height
                    return Offset(x, y)
                }

                val path = Path()
                daySummaries.indices.forEach { index ->
                    val point = pointFor(index)
                    if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                drawPath(path = path, color = primaryColor, style = Stroke(width = 3.dp.toPx()))

                daySummaries.indices.forEach { index ->
                    drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = pointFor(index))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                Text(text = dateFormat.format(daySummaries.first().dayStart), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(text = dateFormat.format(daySummaries.last().dayStart), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}
