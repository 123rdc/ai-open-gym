package com.example.gymformcoach.features.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val dummyHistory = listOf(
        HistoryItem("Aug 22", "Squat", 12),
        HistoryItem("Aug 20", "Bench Press", 8),
        HistoryItem("Aug 18", "Deadlift", 5),
        HistoryItem("Aug 15", "Squat", 10)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Session History",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(dummyHistory) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = item.date, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                            Text(text = item.type, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        }
                        Text(text = "${item.reps} reps", style = MaterialTheme.typography.headlineMedium, color = Primary)
                    }
                }
            }
        }
    }
}

data class HistoryItem(val date: String, val type: String, val reps: Int)
