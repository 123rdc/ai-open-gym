package com.example.gymformcoach.features.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.ExerciseSessionRepository
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * §18.5: this used to render four hardcoded fake sessions unconditionally -
 * a fresh install with zero logged sets showed fabricated history that
 * looked real. Now backed by real data with a genuine empty state.
 */
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { ExerciseSessionRepository(AppDatabase.getInstance(context)) }
    val sessions by repository.getAllSessions().collectAsState(initial = null)

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

        when {
            sessions == null -> Unit // still loading; nothing to show yet either way
            sessions!!.isEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No sessions logged yet. Your history will show up here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            else -> {
                val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(sessions!!.sortedByDescending { it.performedAt }) { session ->
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
                                    Text(
                                        text = dateFormat.format(java.util.Date(session.performedAt)),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextSecondary
                                    )
                                    Text(text = session.exerciseId, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                                }
                                Text(text = "${session.reps} reps", style = MaterialTheme.typography.headlineMedium, color = Primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
