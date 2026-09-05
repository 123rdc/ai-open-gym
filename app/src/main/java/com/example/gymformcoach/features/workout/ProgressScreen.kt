package com.example.gymformcoach.features.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary

/** Used by StatsScreen's Strength tab. */
@Composable
fun ProgressChart() {
    val primaryColor = Primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Weight Lifted (kg)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(16.dp))

            Canvas(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
                val path = Path().apply {
                    moveTo(0f, size.height * 0.8f)
                    lineTo(size.width * 0.2f, size.height * 0.7f)
                    lineTo(size.width * 0.4f, size.height * 0.4f)
                    lineTo(size.width * 0.6f, size.height * 0.1f)
                    lineTo(size.width * 0.8f, size.height * 0.3f)
                    lineTo(size.width, size.height * 0.05f)
                }

                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx())
                )

                drawCircle(primaryColor, radius = 4.dp.toPx(), center = Offset(size.width * 0.4f, size.height * 0.4f))
                drawCircle(primaryColor, radius = 4.dp.toPx(), center = Offset(size.width * 0.8f, size.height * 0.3f))
            }
        }
    }
}
