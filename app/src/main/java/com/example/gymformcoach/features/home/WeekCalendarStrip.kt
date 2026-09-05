package com.example.gymformcoach.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymformcoach.core.designsystem.Background
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** This-week strip on Home, matching the reference design's Mo-Su calendar card. */
@Composable
fun WeekCalendarStrip(loggedDates: Set<LocalDate>, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val days = (0..6).map { monday.plusDays(it.toLong()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { date ->
                val isToday = date == today
                val hasLog = date in loggedDates
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(if (isToday) Primary else Background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${date.dayOfMonth}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isToday) Background else androidx.compose.ui.graphics.Color.White,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                if (hasLog) Primary else androidx.compose.ui.graphics.Color.Transparent,
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}
