package com.example.gymformcoach.features.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.progression.MuscleFatigue

private val FatigueColor = Color(0xFFE05B4E)
private val RecoveringColor = Color(0xFFE0A93E)
private val ReadyColor = Color(0xFF6E6E73)

private fun colorFor(level: MuscleFatigue.Level) = when (level) {
    MuscleFatigue.Level.FATIGUED -> FatigueColor
    MuscleFatigue.Level.RECOVERING -> RecoveringColor
    MuscleFatigue.Level.READY, MuscleFatigue.Level.UNTRAINED -> ReadyColor
}

/** Region layout as fractional (x, y, w, h) within the canvas - a simplified front-view figure. */
private val REGIONS: Map<String, FloatArray> = mapOf(
    "Shoulders" to floatArrayOf(0.30f, 0.10f, 0.40f, 0.08f),
    "Chest" to floatArrayOf(0.32f, 0.19f, 0.36f, 0.12f),
    "Biceps" to floatArrayOf(0.15f, 0.20f, 0.14f, 0.16f),
    "Triceps" to floatArrayOf(0.71f, 0.20f, 0.14f, 0.16f),
    "Abs" to floatArrayOf(0.36f, 0.32f, 0.28f, 0.16f),
    "Back" to floatArrayOf(0.32f, 0.19f, 0.36f, 0.12f),
    "Quads" to floatArrayOf(0.30f, 0.50f, 0.18f, 0.28f),
    "Hamstrings" to floatArrayOf(0.52f, 0.50f, 0.18f, 0.28f),
    "Calves" to floatArrayOf(0.32f, 0.80f, 0.14f, 0.16f),
    "Glutes" to floatArrayOf(0.54f, 0.80f, 0.14f, 0.16f)
)

/**
 * A simplified, honestly-labeled body diagram (not anatomical art) - regions are
 * rounded rectangles positioned to roughly match a figure, shaded by fatigue.
 * Real vector art can replace REGIONS' shapes later without touching the
 * fatigue calculation.
 */
@Composable
fun MuscleFatigueDiagram(fatigueByGroup: List<MuscleFatigue.GroupFatigue>, modifier: Modifier = Modifier) {
    val levelByGroup = fatigueByGroup.associate { it.muscleGroup to it.level }

    Canvas(modifier = modifier.fillMaxWidth().height(280.dp)) {
        // Silhouette outline
        val bodyColor = Color.White.copy(alpha = 0.06f)
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(size.width * 0.25f, size.height * 0.02f),
            size = Size(size.width * 0.5f, size.height * 0.06f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.08f)
        ) // head
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(size.width * 0.28f, size.height * 0.09f),
            size = Size(size.width * 0.44f, size.height * 0.88f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f)
        ) // torso+legs silhouette base

        REGIONS.forEach { (group, rect) ->
            val level = levelByGroup[group] ?: MuscleFatigue.Level.UNTRAINED
            drawRoundRect(
                color = colorFor(level).copy(alpha = if (level == MuscleFatigue.Level.UNTRAINED) 0.25f else 0.85f),
                topLeft = Offset(size.width * rect[0], size.height * rect[1]),
                size = Size(size.width * rect[2], size.height * rect[3]),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f),
                style = Fill
            )
        }
    }
}

@Composable
fun FatigueLegend(modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendDot(FatigueColor, "Fatigued")
        LegendDot(RecoveringColor, "Recovering")
        LegendDot(ReadyColor, "Ready")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) { drawCircle(color) }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
