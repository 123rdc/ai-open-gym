package com.example.gymformcoach.features.workout

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.gymformcoach.core.data.ActiveSession
import com.example.gymformcoach.core.data.ActiveSessionRepository
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import kotlinx.coroutines.launch

/**
 * §3.3: on launch, offer to resume an in-progress session that started recently.
 * Discarding is explicit — an abandoned session is cleared rather than left to
 * re-prompt forever.
 */
@Composable
fun ResumeSessionPrompt(onResume: (routineId: String, exerciseIndex: Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ActiveSessionRepository(AppDatabase.getInstance(context)) }
    var session by remember { mutableStateOf<ActiveSession?>(null) }
    var checked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        session = repository.getResumable()
        checked = true
    }

    val active = session
    if (!checked || active == null) return

    AlertDialog(
        onDismissRequest = { session = null },
        title = { Text("Resume workout?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "You have a session in progress with ${active.completedSetIds.size} set(s) logged.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = {
                session = null
                onResume(active.routineId, active.currentExerciseIndex)
            }) {
                Text("Resume", color = Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                scope.launch { repository.finish() }
                session = null
            }) {
                Text("Discard", color = TextSecondary)
            }
        }
    )
}
