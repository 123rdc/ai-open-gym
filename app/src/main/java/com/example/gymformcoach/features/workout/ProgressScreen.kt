package com.example.gymformcoach.features.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.designsystem.components.GymBottomNavigation
import com.example.gymformcoach.core.navigation.Screen

@Composable
fun ProgressScreen(
    onNavigateToTab: (String) -> Unit
) {
    Scaffold(
        bottomBar = {
            GymBottomNavigation(
                currentRoute = Screen.Progress.route,
                onTabSelected = onNavigateToTab
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            CalendarHeader()

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Progressive Overload",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            ProgressChart()

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Recent Stats",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            StatItem("Total Weight Lifted", "45,200 kg", "+12% this month")
            Spacer(modifier = Modifier.height(12.dp))
            StatItem("Workouts Completed", "18", "On track")
            Spacer(modifier = Modifier.height(12.dp))
            StatItem("Average Intensity", "8.5/10", "High")
        }
    }
}

@Composable
fun CalendarHeader() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "August 2026", fontWeight = FontWeight.Bold)
            Text(text = "Today", color = Primary)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items((20..26).toList()) { day ->
                val isToday = day == 24
                Column(
                    modifier = Modifier
                        .size(width = 45.dp, height = 70.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isToday) Primary else MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val dayName = when(day % 7) {
                        0 -> "Sun"
                        1 -> "Mon"
                        2 -> "Tue"
                        3 -> "Wed"
                        4 -> "Thu"
                        5 -> "Fri"
                        else -> "Sat"
                    }
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isToday) Color.Black else TextSecondary
                    )
                    Text(
                        text = day.toString(),
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressChart() {
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
                    color = Primary,
                    style = Stroke(width = 3.dp.toPx())
                )
                
                // Draw data points
                drawCircle(Primary, radius = 4.dp.toPx(), center = Offset(size.width * 0.4f, size.height * 0.4f))
                drawCircle(Primary, radius = 4.dp.toPx(), center = Offset(size.width * 0.8f, size.height * 0.3f))
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, trend: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(text = trend, color = if (trend.contains("+")) Primary else TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
