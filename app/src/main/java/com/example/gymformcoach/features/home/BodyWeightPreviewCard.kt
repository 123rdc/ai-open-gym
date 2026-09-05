package com.example.gymformcoach.features.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.BodyWeightRepository
import com.example.gymformcoach.core.data.ProfileRepository
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import java.time.LocalDate

/** Compact preview of §17A body-weight tracking, shown on Home; taps through to the full screen. */
@Composable
fun BodyWeightPreviewCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bodyWeightRepository = remember { BodyWeightRepository(AppDatabase.getInstance(context)) }
    val profileRepository = remember { ProfileRepository(AppDatabase.getInstance(context)) }
    val entries by bodyWeightRepository.getFrom(LocalDate.now().minusMonths(3)).collectAsState(initial = emptyList())
    val profile by profileRepository.observeProfile().collectAsState(initial = null)
    val primaryColor = Primary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Body weight", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                TextButton(onClick = onOpen) { Text("Log", color = Primary) }
            }

            val latest = entries.lastOrNull()
            if (latest == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No entries yet - tap Log to start tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                val previous = entries.getOrNull(entries.size - 2)
                val delta = previous?.let { latest.weightKg - it.weightKg }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${latest.weightKg} kg",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (delta != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${if (delta >= 0) "+" else ""}${"%.1f".format(delta)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                profile?.goalWeightKg?.let { goal ->
                    val remaining = kotlin.math.abs(latest.weightKg - goal)
                    val direction = if (latest.weightKg > goal) "to lose" else "to gain"
                    Text(
                        text = "Goal ${goal.toInt()} kg · ${"%.1f".format(remaining)} kg $direction",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                if (entries.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        val weights = entries.map { it.weightKg }
                        val minW = weights.min()
                        val maxW = weights.max()
                        val range = (maxW - minW).coerceAtLeast(0.1f)
                        val stepX = size.width / (entries.size - 1)
                        val path = androidx.compose.ui.graphics.Path()
                        entries.forEachIndexed { i, e ->
                            val x = stepX * i
                            val y = size.height - ((e.weightKg - minW) / range) * size.height
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = primaryColor, style = Stroke(width = 3f))
                    }
                }
            }
        }
    }
}
