package com.example.gymformcoach.features.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.notifications.RestTimerScheduler
import kotlinx.coroutines.delay

/** §7.1 presets. */
private val REST_PRESETS = listOf(60, 90, 120, 180)

/**
 * §7: rest timer.
 *
 * The countdown is derived from a wall-clock end timestamp, and completion is
 * scheduled with AlarmManager (§7.2) — a tick-counting coroutine dies with the UI,
 * so returning to the app after 40s of a 90s rest would wrongly show 90s and the
 * alert would never fire with the screen off. The receiver owns the
 * vibration/sound; this composable only draws.
 */
@Composable
fun RestTimerCard(
    initialSeconds: Int,
    onDurationChange: (Int) -> Unit
) {
    val context = LocalContext.current
    var durationSeconds by remember { mutableIntStateOf(initialSeconds) }
    var endAtMillis by remember { mutableLongStateOf(0L) }
    var remainingSeconds by remember { mutableIntStateOf(initialSeconds) }
    var isPaused by remember { mutableStateOf(false) }

    fun startRest(seconds: Int) {
        durationSeconds = seconds
        remainingSeconds = seconds
        isPaused = false
        endAtMillis = RestTimerScheduler.schedule(context, seconds)
    }

    // Auto-starts on entry (§7.1: fires on set completion, and this card is shown
    // by the results screen at exactly that moment).
    LaunchedEffect(Unit) { startRest(initialSeconds) }

    LaunchedEffect(endAtMillis, isPaused) {
        if (isPaused || endAtMillis == 0L) return@LaunchedEffect
        while (true) {
            val left = ((endAtMillis - System.currentTimeMillis()) / 1000L).toInt()
            remainingSeconds = left.coerceAtLeast(0)
            if (left <= 0) {
                endAtMillis = 0L
                break
            }
            delay(250)
        }
    }

    // Leaving the screen must not leave an alarm queued to fire later out of context.
    DisposableEffect(Unit) {
        onDispose { RestTimerScheduler.cancel(context) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "REST",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            // §7.3: largest type on the screen, legible from arm's length.
            Text(
                text = formatRestTime(remainingSeconds),
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                color = if (remainingSeconds == 0) Primary else Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                REST_PRESETS.forEach { preset ->
                    OutlinedButton(
                        onClick = {
                            startRest(preset)
                            onDurationChange(preset)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                    ) {
                        Text("${preset}s")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { if (remainingSeconds > 0) startRest(remainingSeconds + 30) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("+30s")
                }
                OutlinedButton(
                    onClick = {
                        if (isPaused) {
                            isPaused = false
                            endAtMillis = RestTimerScheduler.schedule(context, remainingSeconds)
                        } else {
                            RestTimerScheduler.cancel(context)
                            isPaused = true
                            endAtMillis = 0L
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = remainingSeconds > 0,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(if (isPaused) "Resume" else "Pause")
                }
                Button(
                    onClick = {
                        RestTimerScheduler.cancel(context)
                        endAtMillis = 0L
                        isPaused = false
                        remainingSeconds = 0
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Skip", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatRestTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
